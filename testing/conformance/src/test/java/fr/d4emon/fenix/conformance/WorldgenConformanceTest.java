package fr.d4emon.fenix.conformance;

import fr.d4emon.fenix.loader.launch.Launch;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.CleanupMode;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Checks that the worldgen files Ember writes are files the game accepts.
 *
 * <p>Worldgen is the one part of a mod that is pure data, and data is where a
 * generator's mistakes hide: a misspelled field or a renamed enum fails no
 * build, fails no startup and logs nothing — the entry is dropped and the ore
 * is never anywhere. The player reports bad luck, and the author looks at their
 * spawn weights.
 *
 * <p>So the files are parsed with Minecraft's own codecs, which is both the
 * only honest check and the only thing that would notice the format changing
 * in a game update.
 */
class WorldgenConformanceTest {

    /** A real launch keeps its classloader open for the life of the process. */
    @TempDir(cleanup = CleanupMode.NEVER)
    Path gameDir;

    @Test
    @DisplayName("the ore files Ember wrote parse with Minecraft's own codecs")
    void generatedWorldgenParses() throws Exception {
        Path clientJar = requiredFile("fenix.test.clientJar");
        Path worldgen = Path.of(requiredProperty("fenix.test.worldgenDir"));

        Path configured = worldgen.resolve("configured_feature").resolve("ruby_ore.json");
        Path placed = worldgen.resolve("placed_feature").resolve("ruby_ore.json");
        assertTrue(Files.isRegularFile(configured), configured + " — run :example-mod:ember");
        assertTrue(Files.isRegularFile(placed), placed + " — run :example-mod:ember");

        Files.createDirectories(gameDir.resolve("mods"));

        assertDoesNotThrow(() -> Launch.run(new String[] {
                "--fenix.gameJar", clientJar.toAbsolutePath().toString(),
                "--fenix.gameMain", "fr.d4emon.fenix.probe.WorldgenProbe",
                "--fenix.gameDir", gameDir.toAbsolutePath().toString(),
                configured.toAbsolutePath().toString(),
                placed.toAbsolutePath().toString(),
        }), "the probe reports a failed check by throwing");
    }

    private static String requiredProperty(String name) {
        String value = System.getProperty(name);
        assertNotNull(value, "the build must set -D" + name);
        return value;
    }

    private static Path requiredFile(String property) {
        Path path = Path.of(requiredProperty(property));
        assertTrue(Files.isRegularFile(path), path + " does not exist");
        return path;
    }
}
