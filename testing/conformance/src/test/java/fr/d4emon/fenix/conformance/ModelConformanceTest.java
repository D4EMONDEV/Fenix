package fr.d4emon.fenix.conformance;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Checks that every model Ember wrote is actually there.
 *
 * <p>A blockstate naming a model that does not exist is the one modelling
 * mistake the game says nothing about: it draws the missing-model cube, which
 * looks like a texture problem and is a filename problem. Nothing is logged at a
 * level anyone reads, and the block is otherwise perfectly registered.
 *
 * <p>Reads what shipped rather than calling the generator, for the same reason
 * the worldgen check does: the files are what a player loads.
 */
class ModelConformanceTest {

    @Test
    @DisplayName("every blockstate names a model that exists, and a pillar covers all three axes")
    void blockstatesNameRealModels() throws IOException {
        Path generated = requiredDir("fenix.test.exampleGenerated");
        Path assets = generated.resolve("assets/example-mod");
        Path blockstates = assets.resolve("blockstates");
        assertTrue(Files.isDirectory(blockstates), "example-mod should have generated blockstates");

        List<String> problems = new ArrayList<>();
        try (Stream<Path> files = Files.list(blockstates)) {
            for (Path file : files.toList()) {
                JsonObject root = JsonParser
                        .parseString(Files.readString(file, StandardCharsets.UTF_8))
                        .getAsJsonObject();

                // A blockstate is one of two shapes. `variants` picks one model
                // per state; `multipart` adds models up, which is how a fence
                // is a post plus an arm per neighbour. Reading only the first
                // was not a lenient check, it was a crash the moment a fence
                // was generated.
                if (root.has("multipart")) {
                    for (JsonElement part : root.getAsJsonArray("multipart")) {
                        checkModel(assets, file, "multipart",
                                part.getAsJsonObject().getAsJsonObject("apply"), problems);
                    }
                    continue;
                }

                JsonObject variants = root.getAsJsonObject("variants");
                assertNotNull(variants, file.getFileName() + " has neither variants nor multipart");

                for (String key : variants.keySet()) {
                    checkModel(assets, file, key, variants.getAsJsonObject(key), problems);
                }

                // A pillar turns rather than having a model per direction, so
                // its blockstate has to answer for every axis the block can be
                // placed on. A missing one renders as the missing model.
                if (variants.keySet().stream().anyMatch(key -> key.startsWith("axis="))) {
                    assertTrue(variants.keySet().containsAll(Set.of("axis=x", "axis=y", "axis=z")),
                            file.getFileName() + " has axis variants but not all three: "
                                    + variants.keySet());
                }
            }
        }

        assertTrue(problems.isEmpty(), "blockstates naming models that do not exist: " + problems);
    }

    /** Records a problem if {@code placement} names a model that was never written. */
    private static void checkModel(Path assets, Path file, String where, JsonObject placement,
                                   List<String> problems) {
        String model = placement.get("model").getAsString();
        // "example-mod:block/ruby_log" -> assets/example-mod/models/block/ruby_log.json
        String path = model.substring(model.indexOf(':') + 1);
        if (!Files.isRegularFile(assets.resolve("models/" + path + ".json"))) {
            problems.add(file.getFileName() + " '" + where + "' names " + model
                    + ", which was never written");
        }
    }

    @Test
    @DisplayName("every translation in another language answers a key English also has")
    void translationsAnswerRealKeys() throws IOException {
        Path lang = requiredDir("fenix.test.exampleGenerated")
                .resolve("assets/example-mod/lang");
        assertTrue(Files.isRegularFile(lang.resolve("en_us.json")),
                "en_us is the language every key is defined in");

        JsonObject english = read(lang.resolve("en_us.json"));

        try (Stream<Path> files = Files.list(lang)) {
            for (Path file : files.toList()) {
                String name = file.getFileName().toString();
                if (name.equals("en_us.json")) {
                    continue;
                }
                // A key that exists only in a translation is one the game never
                // asks for: it displays nothing different, and the author is the
                // one person who cannot notice, because they see their own
                // language. Usually it is a key renamed on one side only.
                List<String> stray = read(file).keySet().stream()
                        .filter(key -> !english.has(key))
                        .sorted()
                        .toList();
                assertTrue(stray.isEmpty(),
                        name + " translates keys that en_us does not define, so they never "
                                + "display: " + stray);
            }
        }
    }

    private static JsonObject read(Path file) throws IOException {
        return JsonParser.parseString(Files.readString(file, StandardCharsets.UTF_8))
                .getAsJsonObject();
    }

    private static Path requiredDir(String property) {
        String value = System.getProperty(property);
        assertNotNull(value, "the build must set -D" + property);
        Path path = Path.of(value);
        assertTrue(Files.isDirectory(path), value + " does not exist");
        return path;
    }
}
