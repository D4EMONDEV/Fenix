package fr.d4emon.fenix.loader.launch;

import fr.d4emon.fenix.loader.metadata.InvalidMetadataException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ModIndexReaderTest {

    @TempDir
    Path tempDir;

    @Test
    void readsAnIndex() {
        Map<String, String> index = ModIndexReader.read("""
                {"schema": 1, "mods": {"testmod": "fr.example.TestMod"}}
                """, "testmod.jar");

        assertEquals(Map.of("testmod", "fr.example.TestMod"), index);
    }

    @Test
    @DisplayName("a jar without an index is a data-only mod, not an error")
    void aJarWithoutAnIndexIsEmpty() throws IOException {
        Path jar = tempDir.resolve("resources-only.jar");
        try (ZipOutputStream out = new ZipOutputStream(Files.newOutputStream(jar))) {
            out.putNextEntry(new ZipEntry("assets/whatever.png"));
            out.closeEntry();
        }

        assertEquals(Map.of(), ModIndexReader.readFromJar(jar));
    }

    @Test
    void readsAnIndexOutOfAJar() throws IOException {
        Path jar = tempDir.resolve("real.jar");
        try (ZipOutputStream out = new ZipOutputStream(Files.newOutputStream(jar))) {
            out.putNextEntry(new ZipEntry(ModIndexReader.FILE_NAME));
            out.write("""
                    {"schema": 1, "mods": {"real": "fr.example.RealMod"}}
                    """.getBytes(StandardCharsets.UTF_8));
            out.closeEntry();
        }

        assertEquals(Map.of("real", "fr.example.RealMod"), ModIndexReader.readFromJar(jar));
    }

    @Test
    void rejectsBrokenJson() {
        InvalidMetadataException failure = assertThrows(InvalidMetadataException.class,
                () -> ModIndexReader.read("{ nope", "bad.jar"));

        assertTrue(failure.getMessage().contains("bad.jar"), failure.getMessage());
    }

    @Test
    void rejectsAMissingOrForeignSchema() {
        assertThrows(InvalidMetadataException.class,
                () -> ModIndexReader.read("""
                        {"mods": {}}
                        """, "bad.jar"));

        InvalidMetadataException failure = assertThrows(InvalidMetadataException.class,
                () -> ModIndexReader.read("""
                        {"schema": 99, "mods": {}}
                        """, "bad.jar"));
        assertTrue(failure.getMessage().contains("99"), failure.getMessage());
    }

    @Test
    void rejectsBadEntries() {
        assertThrows(InvalidMetadataException.class, () -> ModIndexReader.read("""
                {"schema": 1, "mods": {"Bad_Id": "fr.example.X"}}
                """, "bad.jar"));

        assertThrows(InvalidMetadataException.class, () -> ModIndexReader.read("""
                {"schema": 1, "mods": {"fine": 42}}
                """, "bad.jar"));

        assertThrows(InvalidMetadataException.class, () -> ModIndexReader.read("""
                {"schema": 1, "mods": {"fine": ""}}
                """, "bad.jar"));
    }
}
