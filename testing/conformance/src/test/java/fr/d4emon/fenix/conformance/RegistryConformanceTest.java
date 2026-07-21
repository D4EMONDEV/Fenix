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
 * Registers real content into real Minecraft, through the whole loader.
 *
 * <p>This is the check behind the registrar's central claim: that a mod's
 * blocks and items get every pass vanilla performs around its <em>own</em>
 * registration. Each of those passes, skipped, is a crash that surfaces far
 * from its cause — a player kicked with "Can't find id for Block{…}", a
 * creative tab dying with "Stack size must be exactly 1". None of it can be
 * caught by a unit test, because all of it depends on real vanilla bootstrap
 * order.
 *
 * <p>So this drives the actual pipeline: a mod jar in a mods directory, the
 * loader discovering it, mixins firing {@code onRegister} at the moment the
 * registries are open, and a probe running as the game to inspect the result.
 * The probe throws on failure and the loader propagates it here.
 */
class RegistryConformanceTest {

    /** The probe classes, compiled by this module and repackaged as a mod. */
    private static final List<String> PROBE_CLASSES = List.of(
            "fr/d4emon/fenix/probe/ProbeContent.class",
            "fr/d4emon/fenix/probe/ProbeMod.class",
            "fr/d4emon/fenix/probe/RegistryProbe.class");

    private static final String MOD_METADATA = """
            {
              "schema": 1,
              "id": "probemod",
              "version": "1.0.0",
              "depends": { "fenix": ">=0.1.0", "fenix-api-registry": ">=0.1.0" }
            }
            """;

    /** The processor would write this; the probe is repackaged, so it is written by hand. */
    private static final String MOD_INDEX = """
            { "schema": 1, "mods": { "probemod": "fr.d4emon.fenix.probe.ProbeMod" } }
            """;

    /**
     * Never cleaned up: a real launch keeps its classloader — and so the mod
     * jars — open for the life of the process, which is right for a game and
     * fatal for a directory Windows is trying to delete.
     */
    @TempDir(cleanup = CleanupMode.NEVER)
    Path gameDir;

    @Test
    @DisplayName("a mod's block and item survive every pass vanilla makes around registration")
    void contentIsRegisteredCompletely() throws IOException {
        Path clientJar = requiredFile("fenix.test.clientJar");
        Path registryJar = requiredFile("fenix.test.registryJar");

        Path mods = Files.createDirectories(gameDir.resolve("mods"));
        Files.copy(registryJar, mods.resolve(registryJar.getFileName()));
        writeProbeMod(mods.resolve("probemod.jar"));

        assertDoesNotThrow(() -> Launch.run(new String[] {
                "--fenix.gameJar", clientJar.toAbsolutePath().toString(),
                "--fenix.gameMain", "fr.d4emon.fenix.probe.RegistryProbe",
                "--fenix.gameDir", gameDir.toAbsolutePath().toString(),
        }), "the probe reports a failed check by throwing");
    }

    /** Packages the compiled probe classes into a jar shaped like a real mod. */
    private void writeProbeMod(Path jar) throws IOException {
        try (ZipOutputStream out = new ZipOutputStream(Files.newOutputStream(jar))) {
            for (String className : PROBE_CLASSES) {
                put(out, className, resourceBytes("/" + className));
            }
            put(out, "fenix.mod.json", MOD_METADATA.getBytes(StandardCharsets.UTF_8));
            put(out, "fenix.index.json", MOD_INDEX.getBytes(StandardCharsets.UTF_8));
        }
    }

    private static void put(ZipOutputStream out, String name, byte[] bytes) throws IOException {
        out.putNextEntry(new ZipEntry(name));
        out.write(bytes);
        out.closeEntry();
    }

    private byte[] resourceBytes(String path) throws IOException {
        try (InputStream in = getClass().getResourceAsStream(path)) {
            assertNotNull(in, "missing compiled probe class " + path);
            return in.readAllBytes();
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
