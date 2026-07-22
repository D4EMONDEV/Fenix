package fr.d4emon.fenix.probe;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.JsonOps;
import net.minecraft.SharedConstants;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;
import net.minecraft.world.level.levelgen.placement.PlacementModifier;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Runs as the game: reads the worldgen files Ember wrote and parses them with
 * Minecraft's own codecs.
 *
 * <p>Worldgen is the one part of a mod that is pure data, and data is where a
 * generator's mistakes hide. A misspelled field or a renamed enum does not fail
 * the build, does not fail startup, and does not log: the datapack entry is
 * dropped and the ore is simply never anywhere. A player reports bad luck.
 *
 * <p>Parsing with the real codecs is the whole check. It is also the only thing
 * that would notice the format changing under Fenix in a game update.
 */
public final class WorldgenProbe {

    private WorldgenProbe() {
    }

    /**
     * @param args the configured feature file, then the placed feature file
     */
    public static void main(String[] args) throws Exception {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();

        JsonElement configured = read(Path.of(args[0]));
        JsonElement placed = read(Path.of(args[1]));

        // Every block the file names has to be in the registry, and the mod
        // that owns them is not loaded here — this process has vanilla and
        // nothing else. So the names are swapped for a real block first.
        //
        // That is the one part of the file this cannot check, and the one part
        // that cannot be interestingly wrong: the names come straight from
        // Holder.id(), which the registry check already proves. Everything else
        // — field names, the dispatch key, the nested shapes — is parsed as
        // written.
        for (JsonElement target : configured.getAsJsonObject()
                .getAsJsonObject("config").getAsJsonArray("targets")) {
            JsonObject state = target.getAsJsonObject().getAsJsonObject("state");
            String name = state.get("Name").getAsString();
            require(name.startsWith("example-mod:") && name.length() > "example-mod:".length(),
                    "each target should name a block in the mod's namespace, not " + name);
            state.addProperty("Name", "minecraft:iron_ore");
        }

        // DIRECT_CODEC, not CODEC: the outer one resolves a registry reference,
        // which needs a loaded datapack. The direct one parses the definition
        // itself, which is what the file holds.
        require(ConfiguredFeature.DIRECT_CODEC.parse(JsonOps.INSTANCE, configured),
                "the configured feature");

        // The placed feature's own codec names its configured feature by id, so
        // it cannot be parsed whole without a registry. Its placement list is
        // the part Ember writes by hand, and the part worth checking.
        require(PlacementModifier.CODEC.listOf()
                        .parse(JsonOps.INSTANCE, placed.getAsJsonObject().get("placement")),
                "the placement modifiers");

        require(placed.getAsJsonObject().get("feature").getAsString().equals("example-mod:ruby_ore"),
                "the placed feature should name the configured feature written beside it");

        System.out.println("worldgen conformance: all checks passed");
    }

    private static JsonElement read(Path file) throws Exception {
        return JsonParser.parseString(Files.readString(file, StandardCharsets.UTF_8));
    }

    private static void require(DataResult<?> result, String what) {
        result.getOrThrow(message -> new AssertionError(
                "worldgen conformance failed: " + what + " did not parse: " + message));
    }

    private static void require(boolean condition, String what) {
        if (!condition) {
            throw new AssertionError("worldgen conformance failed: " + what);
        }
    }
}
