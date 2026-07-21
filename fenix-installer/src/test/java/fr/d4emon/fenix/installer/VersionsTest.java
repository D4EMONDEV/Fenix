package fr.d4emon.fenix.installer;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

/** What the version list offers, and what it refuses to offer. */
class VersionsTest {

    @TempDir
    Path minecraftDir;

    /** Writes a version the way the launcher does, once it has been run. */
    private void installed(String version) throws IOException {
        Path directory = Files.createDirectories(minecraftDir.resolve("versions").resolve(version));
        Files.writeString(directory.resolve(version + ".json"), "{}");
    }

    /** A version the launcher has downloaded but never run: no manifest yet. */
    private void halfInstalled(String version) throws IOException {
        Files.createDirectories(minecraftDir.resolve("versions").resolve(version));
    }

    @Test
    @DisplayName("only versions that are both installed and supported are offered")
    void offersTheIntersection() throws IOException {
        installed("26.2");
        installed("1.21.11");

        assertEquals(List.of("26.2"),
                Versions.installable(minecraftDir, List.of("26.2", "26.3")));
    }

    @Test
    @DisplayName("a version the launcher never ran is not offered")
    void skipsAVersionWithNoManifest() throws IOException {
        halfInstalled("26.2");

        // The same condition the install itself needs. Offering it would put
        // the old typed-in error back, one click further along.
        assertEquals(List.of(), Versions.installable(minecraftDir, List.of("26.2")));
    }

    @Test
    @DisplayName("a folder with no versions at all is empty, not an error")
    void emptyFolderIsEmpty() {
        assertEquals(List.of(), Versions.installable(minecraftDir, List.of("26.2")));
        assertEquals(List.of(),
                Versions.installable(minecraftDir.resolve("nowhere"), List.of("26.2")));
    }

    @Test
    @DisplayName("versions sort newest first, counting numbers as numbers")
    void sortsNumerically() throws IOException {
        installed("26.2");
        installed("26.10");
        installed("26.1");

        // "26.10" before "26.2" is what a plain string sort gives, and it is the
        // sort of thing a player notices at once and trusts the installer less
        // for.
        assertEquals(List.of("26.10", "26.2", "26.1"),
                Versions.installable(minecraftDir, List.of("26.1", "26.2", "26.10")));
    }

    @Test
    @DisplayName("Fenix versions follow the game version chosen")
    void fenixVersionsFollowTheGame() {
        assertEquals(List.of("0.1.0"),
                Versions.forMinecraft("26.2", "0.1.0", List.of("26.2")));
        assertEquals(List.of(),
                Versions.forMinecraft("26.3", "0.1.0", List.of("26.2")));
    }
}
