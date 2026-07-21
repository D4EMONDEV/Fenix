package fr.d4emon.fenix.installer;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class InstallerTest {

    private static final String MC = "26.2";
    private static final String FENIX = "0.1.0";

    @TempDir
    Path minecraftDir;

    private Path loaderJar;
    private Path apiJar;

    @BeforeEach
    void fakeMinecraftInstall() throws IOException {
        Path vanilla = minecraftDir.resolve("versions").resolve(MC);
        Files.createDirectories(vanilla);
        Files.writeString(vanilla.resolve(MC + ".json"), "{\"id\": \"" + MC + "\"}");

        loaderJar = Files.write(minecraftDir.resolve("payload-loader.jar"),
                "loader bytes".getBytes(StandardCharsets.UTF_8));
        apiJar = Files.write(minecraftDir.resolve("payload-api.jar"),
                "api bytes".getBytes(StandardCharsets.UTF_8));
    }

    private List<Installer.Library> libraries() {
        return List.of(
                new Installer.Library("fr.d4emon.fenix", "fenix-loader", FENIX, loaderJar),
                new Installer.Library("fr.d4emon.fenix", "fenix-api-core", FENIX, apiJar));
    }

    private Installer.Report install() {
        return Installer.install(minecraftDir, MC, FENIX, libraries());
    }

    private JsonObject readJson(Path file) throws IOException {
        return JsonParser.parseString(Files.readString(file, StandardCharsets.UTF_8)).getAsJsonObject();
    }

    private static String sha1(Path file) throws IOException, NoSuchAlgorithmException {
        return HexFormat.of().formatHex(
                MessageDigest.getInstance("SHA-1").digest(Files.readAllBytes(file)));
    }

    @Nested
    @DisplayName("what gets written")
    class Artefacts {

        @Test
        void laysLibrariesOutMavenStyle() throws IOException {
            install();

            Path expected = minecraftDir.resolve(
                    "libraries/fr/d4emon/fenix/fenix-loader/" + FENIX + "/fenix-loader-" + FENIX + ".jar");
            assertTrue(Files.isRegularFile(expected));
            assertEquals("loader bytes", Files.readString(expected, StandardCharsets.UTF_8));
        }

        @Test
        void writesAVersionInheritingFromVanilla() throws IOException {
            Installer.Report report = install();

            assertEquals("fenix-" + FENIX + "-" + MC, report.versionId());
            JsonObject version = readJson(report.versionJson());
            assertEquals(MC, version.get("inheritsFrom").getAsString());
            assertEquals("fr.d4emon.fenix.loader.launch.Launch", version.get("mainClass").getAsString());
            assertEquals(2, version.getAsJsonArray("libraries").size());
        }

        @Test
        @DisplayName("library entries carry the hash the launcher validates against")
        void hashesEveryLibrary() throws IOException, NoSuchAlgorithmException {
            Installer.Report report = install();

            JsonObject first = readJson(report.versionJson())
                    .getAsJsonArray("libraries").get(0).getAsJsonObject();
            assertEquals("fr.d4emon.fenix:fenix-loader:" + FENIX, first.get("name").getAsString());
            assertEquals(sha1(loaderJar), first.get("sha1").getAsString());
            assertEquals(Files.size(loaderJar), first.get("size").getAsLong());
        }

        @Test
        void addsALauncherProfilePointingAtTheVersion() throws IOException {
            Installer.Report report = install();

            JsonObject profile = readJson(report.profiles())
                    .getAsJsonObject("profiles").getAsJsonObject(Installer.PROFILE_KEY);
            assertEquals("fenix-" + FENIX + "-" + MC, profile.get("lastVersionId").getAsString());
            assertEquals("Fenix " + MC, profile.get("name").getAsString());
        }
    }

    @Nested
    @DisplayName("respect for what is already there")
    class Preservation {

        @Test
        @DisplayName("other launcher profiles survive an install untouched")
        void keepsForeignProfiles() throws IOException {
            Files.writeString(minecraftDir.resolve("launcher_profiles.json"), """
                    {
                      "profiles": {
                        "vanilla-latest": {"name": "Latest Release", "lastVersionId": "latest-release"}
                      },
                      "settings": {"keepLauncherOpen": true}
                    }
                    """);

            JsonObject root = readJson(install().profiles());

            assertEquals("latest-release", root.getAsJsonObject("profiles")
                    .getAsJsonObject("vanilla-latest").get("lastVersionId").getAsString());
            assertTrue(root.getAsJsonObject("settings").get("keepLauncherOpen").getAsBoolean(),
                    "unrelated settings must survive");
            assertTrue(root.getAsJsonObject("profiles").has(Installer.PROFILE_KEY));
        }

        @Test
        @DisplayName("installing twice updates the profile instead of duplicating it")
        void isIdempotent() throws IOException {
            install();
            JsonObject profiles = readJson(install().profiles()).getAsJsonObject("profiles");

            assertEquals(1, profiles.keySet().stream()
                    .filter(key -> key.equals(Installer.PROFILE_KEY)).count());
        }

        @Test
        void createsTheProfilesFileWhenTheLauncherNeverRan() {
            Installer.Report report = install();

            assertTrue(Files.isRegularFile(report.profiles()));
        }
    }

    @Nested
    @DisplayName("refusals")
    class Refusals {

        @Test
        @DisplayName("no vanilla install, no Fenix — with the fix spelled out")
        void requiresTheVanillaVersion() {
            InstallException failure = assertThrows(InstallException.class,
                    () -> Installer.install(minecraftDir, "99.9", FENIX, libraries()));

            assertTrue(failure.getMessage().contains("99.9"), failure.getMessage());
            assertTrue(failure.getMessage().contains("run it once"), failure.getMessage());
        }

        @Test
        void requiresARealDirectory() {
            assertThrows(InstallException.class, () -> Installer.install(
                    minecraftDir.resolve("nope"), MC, FENIX, libraries()));
        }
    }
}
