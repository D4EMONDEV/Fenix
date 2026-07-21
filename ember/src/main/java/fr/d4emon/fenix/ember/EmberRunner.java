package fr.d4emon.fenix.ember;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.SharedConstants;
import net.minecraft.server.Bootstrap;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Runs the generators. This is the game's main class for a generation run.
 *
 * <p>It boots a real game, headlessly, before generating anything — which is
 * the whole trick. By the time a generator runs, the registries are populated
 * and the mod's own content is registered, so a generator refers to a block by
 * the object it registered rather than by repeating its name.
 *
 * <p>Generators are found the same way mods are: from the index the annotation
 * processor wrote into each jar. Nothing here is configured by naming a class
 * in a build file.
 *
 * <p>It has to run as the game, from a jar the Fenix classloader loaded, or its
 * {@code net.minecraft} references would resolve against a second, untouched
 * copy of the game — which is why Ember ships as a mod.
 */
public final class EmberRunner {

    private EmberRunner() {
    }

    /**
     * Generates every mod's resources.
     *
     * @param args one argument: the directory to write into
     * @throws Exception if a generator fails
     */
    public static void main(String[] args) throws Exception {
        if (args.length != 1) {
            throw new IllegalArgumentException("usage: EmberRunner <output directory>");
        }
        Path output = Path.of(args[0]);

        // Registries must be live: a generator asks content for its id and its
        // translation key, and content does not exist until this has run.
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();

        int generated = 0;
        for (ModIndex index : readIndexes()) {
            EmberWriter writer = new EmberWriter(index.modId(), output);
            for (String className : index.generators()) {
                generatorFor(className).collect(writer);
                generated++;
            }
            writer.finish();
        }
        System.out.println("Ember: ran " + generated + " generator(s) into " + output.toAbsolutePath());
    }

    private static EmberGenerator generatorFor(String className) throws ReflectiveOperationException {
        Class<?> type = Class.forName(className, true, EmberRunner.class.getClassLoader());
        Object instance = type.getConstructor().newInstance();
        if (!(instance instanceof EmberGenerator generator)) {
            throw new IllegalStateException(className + " is marked @Generator but does not implement "
                    + EmberGenerator.class.getName());
        }
        return generator;
    }

    /** One mod's index: which mod it is, and what it generates. */
    private record ModIndex(String modId, List<String> generators) {
    }

    private static List<ModIndex> readIndexes() throws Exception {
        List<ModIndex> indexes = new ArrayList<>();

        for (URL url : Collections.list(
                EmberRunner.class.getClassLoader().getResources("fenix.index.json"))) {
            try (InputStream in = url.openStream();
                 Reader reader = new InputStreamReader(in, StandardCharsets.UTF_8)) {

                JsonObject root = JsonParser.parseReader(reader).getAsJsonObject();
                if (!root.has("generators") || !root.has("mods")) {
                    continue;
                }
                JsonObject mods = root.getAsJsonObject("mods");
                if (mods.keySet().isEmpty()) {
                    continue;
                }
                // One jar is one mod, so its generators belong to it.
                String modId = mods.keySet().iterator().next();

                List<String> generators = new ArrayList<>();
                for (JsonElement element : root.getAsJsonArray("generators")) {
                    generators.add(element.getAsString());
                }
                if (!generators.isEmpty()) {
                    indexes.add(new ModIndex(modId, generators));
                }
            }
        }
        return indexes;
    }
}
