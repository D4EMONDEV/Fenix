package fr.d4emon.fenix.conformance;

import fr.d4emon.fenix.loader.launch.Launch;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.CleanupMode;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Checks that a mod can add to a loot table without erasing what is there.
 *
 * <p>Run through the loader rather than as a plain unit test because rebuilding
 * a loot table needs its private constructor, which Fenix widens in the jar the
 * game actually loads. Nothing about that transformation is visible on an
 * ordinary classpath, so a test that did not go through the loader would prove
 * only that the code compiles.
 */
class LootConformanceTest {

    /**
     * Never cleaned up: a real launch keeps its classloader — and so the mod
     * jars — open for the life of the process.
     */
    @TempDir(cleanup = CleanupMode.NEVER)
    Path gameDir;

    private static final String MOD_METADATA = """
            {
              "schema": 1,
              "id": "lootprobe",
              "version": "1.0.0",
              "depends": { "fenix": ">=0.1.0", "fenix-api-event": ">=0.1.0" }
            }
            """;

    @Test
    @DisplayName("a mod adds a pool to a loot table and keeps the pools already there")
    void poolsAreAddedNotReplaced() throws IOException {
        Path clientJar = requiredFile("fenix.test.clientJar");
        Path eventJar = requiredFile("fenix.test.eventJar");

        Path mods = Files.createDirectories(gameDir.resolve("mods"));
        Files.copy(eventJar, mods.resolve(eventJar.getFileName()));
        // Packaged as a mod rather than left on the test classpath: a class the
        // Fenix classloader does not own is loaded by the app loader, and then
        // so is everything it touches — including a LootTable that no mixin has
        // been applied to. The failure looks like a broken mixin and is not.
        writeProbeMod(mods.resolve("lootprobe.jar"));

        assertDoesNotThrow(() -> Launch.run(new String[] {
                "--fenix.gameJar", clientJar.toAbsolutePath().toString(),
                "--fenix.gameMain", "fr.d4emon.fenix.probe.LootProbe",
                "--fenix.gameDir", gameDir.toAbsolutePath().toString(),
        }), "the probe reports a failed check by throwing");
    }

    /** Packages the compiled probe into a jar shaped like a real mod. */
    private void writeProbeMod(Path jar) throws IOException {
        try (ZipOutputStream out = new ZipOutputStream(Files.newOutputStream(jar))) {
            for (String className : List.of(
                    "fr/d4emon/fenix/probe/LootProbe.class",
                    "fr/d4emon/fenix/probe/LootProbe$1.class")) {
                byte[] bytes = resourceBytes("/" + className);
                if (bytes == null) {
                    // The lambda's class is only there if javac made one.
                    continue;
                }
                out.putNextEntry(new ZipEntry(className));
                out.write(bytes);
                out.closeEntry();
            }
            out.putNextEntry(new ZipEntry("fenix.mod.json"));
            out.write(MOD_METADATA.getBytes(StandardCharsets.UTF_8));
            out.closeEntry();
        }
    }

    private byte[] resourceBytes(String path) throws IOException {
        try (InputStream in = getClass().getResourceAsStream(path)) {
            return in == null ? null : in.readAllBytes();
        }
    }

    private static Path requiredFile(String property) {
        String value = System.getProperty(property);
        assertNotNull(value, "the build must set -D" + property);
        Path path = Path.of(value);
        assertTrue(Files.isRegularFile(path), value + " does not exist");
        return path;
    }
}
