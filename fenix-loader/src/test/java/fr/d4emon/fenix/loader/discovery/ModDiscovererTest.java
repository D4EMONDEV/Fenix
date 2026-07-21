package fr.d4emon.fenix.loader.discovery;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ModDiscovererTest {

    @TempDir
    Path modsDir;

    private void writeJar(String fileName, String metadataJson) throws IOException {
        try (ZipOutputStream out = new ZipOutputStream(Files.newOutputStream(modsDir.resolve(fileName)))) {
            if (metadataJson != null) {
                out.putNextEntry(new ZipEntry("fenix.mod.json"));
                out.write(metadataJson.getBytes(StandardCharsets.UTF_8));
                out.closeEntry();
            } else {
                out.putNextEntry(new ZipEntry("whatever.txt"));
                out.write("not a mod".getBytes(StandardCharsets.UTF_8));
                out.closeEntry();
            }
        }
    }

    private static String metadata(String id) {
        return """
                {"schema": 1, "id": "%s", "version": "1.0.0"}
                """.formatted(id);
    }

    /** A container jar carrying other mod jars under META-INF/jars. */
    private void writeBundle(String fileName, String id, String... nestedIds) throws IOException {
        try (ZipOutputStream out = new ZipOutputStream(Files.newOutputStream(modsDir.resolve(fileName)))) {
            out.putNextEntry(new ZipEntry("fenix.mod.json"));
            out.write(metadata(id).getBytes(StandardCharsets.UTF_8));
            out.closeEntry();
            for (String nested : nestedIds) {
                out.putNextEntry(new ZipEntry("META-INF/jars/" + nested + ".jar"));
                out.write(jarBytes(metadata(nested)));
                out.closeEntry();
            }
        }
    }

    private static byte[] jarBytes(String metadataJson) throws IOException {
        var bytes = new java.io.ByteArrayOutputStream();
        try (ZipOutputStream out = new ZipOutputStream(bytes)) {
            out.putNextEntry(new ZipEntry("fenix.mod.json"));
            out.write(metadataJson.getBytes(StandardCharsets.UTF_8));
            out.closeEntry();
        }
        return bytes.toByteArray();
    }

    @Nested
    @DisplayName("jars carried inside jars")
    class Nested_ {

        @TempDir
        Path unpackDir;

        @Test
        @DisplayName("a bundle is found, and so is everything it carries")
        void unpacksNestedJars() throws IOException {
            writeBundle("fenix-api.jar", "fenix-api", "fenix-api-event", "fenix-api-registry");

            DiscoveryResult result = ModDiscoverer.scan(modsDir, unpackDir);

            assertEquals(List.of("fenix-api", "fenix-api-event", "fenix-api-registry"),
                    result.mods().stream().map(ModCandidate::id).sorted().toList());
            assertEquals(List.of(), result.problems());
        }

        @Test
        @DisplayName("unpacking twice reuses what is already there")
        void reusesWhatIsUnpacked() throws IOException {
            writeBundle("fenix-api.jar", "fenix-api", "fenix-api-event");

            ModDiscoverer.scan(modsDir, unpackDir);
            Path unpacked = unpackDir.resolve("fenix-api").resolve("fenix-api-event.jar");
            long first = Files.getLastModifiedTime(unpacked).toMillis();

            ModDiscoverer.scan(modsDir, unpackDir);

            // Rewriting on every launch would mean copying the whole API before
            // the game starts, every time, for nothing.
            assertEquals(first, Files.getLastModifiedTime(unpacked).toMillis());
        }
    }

    @Nested
    @DisplayName("the happy path")
    class Valid {

        @Test
        void readsAModJar() throws IOException {
            writeJar("example-mod.jar", metadata("example-mod"));

            DiscoveryResult result = ModDiscoverer.scan(modsDir);

            assertFalse(result.hasProblems());
            assertEquals(1, result.mods().size());

            ModCandidate candidate = result.mods().getFirst();
            assertEquals("example-mod", candidate.id());
            assertEquals("example-mod.jar", candidate.fileName());
            assertEquals(modsDir.resolve("example-mod.jar"), candidate.path());
        }

        @Test
        @DisplayName("a missing mods directory is a first launch, not an error")
        void toleratesAMissingDirectory() {
            DiscoveryResult result = ModDiscoverer.scan(modsDir.resolve("does-not-exist"));

            assertEquals(List.of(), result.mods());
            assertFalse(result.hasProblems());
        }

        @Test
        void toleratesAnEmptyDirectory() {
            DiscoveryResult result = ModDiscoverer.scan(modsDir);

            assertEquals(List.of(), result.mods());
            assertFalse(result.hasProblems());
        }

        @Test
        @DisplayName("only top-level .jar files are considered")
        void ignoresEverythingThatIsNotAJar() throws IOException {
            Files.writeString(modsDir.resolve("README.txt"), "hello");
            Files.writeString(modsDir.resolve("old-mod.jar.disabled"), "ignored");
            Files.createDirectory(modsDir.resolve("nested"));
            writeJar("nested/hidden.jar", metadata("hidden"));
            writeJar("real.jar", metadata("real"));

            DiscoveryResult result = ModDiscoverer.scan(modsDir);

            assertFalse(result.hasProblems());
            assertEquals(1, result.mods().size());
            assertEquals("real", result.mods().getFirst().id());
        }

        @Test
        @DisplayName("results come back in file-name order, so logs are stable")
        void sortsByFileName() throws IOException {
            writeJar("Bravo.jar", metadata("bravo"));
            writeJar("alpha.jar", metadata("alpha"));
            writeJar("charlie.jar", metadata("charlie"));

            DiscoveryResult result = ModDiscoverer.scan(modsDir);

            assertEquals(List.of("alpha", "bravo", "charlie"),
                    result.mods().stream().map(ModCandidate::id).toList());
        }
    }

    @Nested
    @DisplayName("bad jars are reported, never silently dropped")
    class Problems {

        @Test
        void reportsAJarWithoutMetadata() throws IOException {
            writeJar("fabric-mod.jar", null);

            DiscoveryResult result = ModDiscoverer.scan(modsDir);

            assertEquals(1, result.problems().size());
            String problem = result.problems().getFirst();
            assertTrue(problem.contains("fabric-mod.jar"), problem);
            assertTrue(problem.contains("fenix.mod.json"), problem);
            assertTrue(problem.contains("different loader"), problem);
        }

        @Test
        void reportsAFileThatIsNotAnArchive() throws IOException {
            Files.writeString(modsDir.resolve("corrupt.jar"), "this is not a zip");

            DiscoveryResult result = ModDiscoverer.scan(modsDir);

            String problem = result.problems().getFirst();
            assertTrue(problem.contains("corrupt.jar"), problem);
            assertTrue(problem.contains("cannot be opened"), problem);
        }

        @Test
        void reportsBrokenMetadataWithItsFieldAndFile() throws IOException {
            writeJar("broken.jar", """
                    {"schema": 1, "id": "broken"}
                    """);

            DiscoveryResult result = ModDiscoverer.scan(modsDir);

            String problem = result.problems().getFirst();
            assertTrue(problem.contains("broken.jar"), problem);
            assertTrue(problem.contains("'version'"), problem);
        }

        @Test
        @DisplayName("one bad jar does not hide the good ones")
        void keepsScanningPastAProblem() throws IOException {
            writeJar("aaa-broken.jar", "{ not json");
            writeJar("zzz-fine.jar", metadata("fine"));

            DiscoveryResult result = ModDiscoverer.scan(modsDir);

            assertEquals(1, result.mods().size());
            assertEquals("fine", result.mods().getFirst().id());
            assertEquals(1, result.problems().size());
            assertTrue(result.problems().getFirst().contains("aaa-broken.jar"));
        }
    }
}
