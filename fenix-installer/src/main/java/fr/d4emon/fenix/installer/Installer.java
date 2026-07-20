package fr.d4emon.fenix.installer;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;
import java.util.Objects;

/**
 * Writes a Fenix profile into a {@code .minecraft} directory.
 *
 * <p>Three artefacts, all of them things the vanilla launcher already
 * understands — Fenix adds no launcher of its own:
 *
 * <ol>
 * <li>the loader's jars, laid out Maven-style under {@code libraries/};</li>
 * <li>a version JSON that <em>inherits from</em> the installed vanilla
 *     version, swapping only the main class for Fenix's {@code Launch} and
 *     adding the loader libraries — the launcher merges everything else
 *     (classpath, assets, JVM flags) from vanilla;</li>
 * <li>a profile entry in {@code launcher_profiles.json} so the version shows
 *     up in the launcher's dropdown.</li>
 * </ol>
 *
 * <p>The vanilla version must already be installed: Fenix never downloads the
 * game. And nothing here ever touches accounts, tokens or settings — the file
 * edit is limited to adding one profile entry, preserving everything else.
 */
public final class Installer {

    /** The launcher-profiles key, stable so a reinstall updates rather than duplicates. */
    static final String PROFILE_KEY = "fenix";

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();

    private Installer() {
    }

    /**
     * A loader jar to install.
     *
     * @param group    the Maven group id, for example {@code fr.d4emon.fenix}
     * @param artifact the artifact id, for example {@code fenix-loader}
     * @param version  the version
     * @param file     the jar on disk
     */
    public record Library(String group, String artifact, String version, Path file) {

        /**
         * Checks that every component is present.
         *
         * @throws NullPointerException if any component is {@code null}
         */
        public Library {
            Objects.requireNonNull(group, "group");
            Objects.requireNonNull(artifact, "artifact");
            Objects.requireNonNull(version, "version");
            Objects.requireNonNull(file, "file");
        }

        String mavenName() {
            return group + ":" + artifact + ":" + version;
        }

        String mavenPath() {
            return group.replace('.', '/') + "/" + artifact + "/" + version
                    + "/" + artifact + "-" + version + ".jar";
        }
    }

    /**
     * What an installation wrote, for reporting.
     *
     * @param versionId   the launcher version id, {@code fenix-<loader>-<mc>}
     * @param versionJson the version manifest that was written
     * @param libraries   every library jar that was copied
     * @param profiles    the launcher profiles file that was updated
     */
    public record Report(String versionId, Path versionJson, List<Path> libraries, Path profiles) {
    }

    /**
     * Installs Fenix into a {@code .minecraft} directory.
     *
     * @param minecraftDir     the {@code .minecraft} directory
     * @param minecraftVersion the installed vanilla version to inherit from
     * @param fenixVersion     the loader version being installed
     * @param libraries        the loader jars to place under {@code libraries/}
     * @return what was written
     * @throws InstallException     if the directory or the vanilla version is missing
     * @throws NullPointerException if any argument is {@code null}
     */
    public static Report install(Path minecraftDir, String minecraftVersion, String fenixVersion,
                                 List<Library> libraries) {
        Objects.requireNonNull(minecraftDir, "minecraftDir");
        Objects.requireNonNull(minecraftVersion, "minecraftVersion");
        Objects.requireNonNull(fenixVersion, "fenixVersion");
        Objects.requireNonNull(libraries, "libraries");

        if (!Files.isDirectory(minecraftDir)) {
            throw new InstallException(minecraftDir + " is not a directory — point the installer at your "
                    + ".minecraft directory with --dir");
        }
        if (!Files.isRegularFile(minecraftDir.resolve("versions")
                .resolve(minecraftVersion).resolve(minecraftVersion + ".json"))) {
            throw new InstallException("Minecraft " + minecraftVersion + " is not installed in " + minecraftDir
                    + " — install it from the launcher and run it once, then retry");
        }

        try {
            List<Path> installedLibraries = copyLibraries(minecraftDir, libraries);
            String versionId = "fenix-" + fenixVersion + "-" + minecraftVersion;
            Path versionJson = writeVersion(minecraftDir, versionId, minecraftVersion, libraries);
            Path profiles = writeProfile(minecraftDir, versionId, minecraftVersion);
            return new Report(versionId, versionJson, installedLibraries, profiles);
        } catch (IOException e) {
            throw new InstallException("installing into " + minecraftDir + " failed: " + e.getMessage(), e);
        }
    }

    private static List<Path> copyLibraries(Path minecraftDir, List<Library> libraries) throws IOException {
        List<Path> installed = new ArrayList<>(libraries.size());
        for (Library library : libraries) {
            Path target = minecraftDir.resolve("libraries").resolve(library.mavenPath());
            Files.createDirectories(target.getParent());
            Files.copy(library.file(), target, StandardCopyOption.REPLACE_EXISTING);
            installed.add(target);
        }
        return installed;
    }

    private static Path writeVersion(Path minecraftDir, String versionId, String minecraftVersion,
                                     List<Library> libraries) throws IOException {
        JsonObject version = new JsonObject();
        version.addProperty("id", versionId);
        version.addProperty("inheritsFrom", minecraftVersion);
        version.addProperty("type", "release");
        String now = Instant.now().truncatedTo(ChronoUnit.SECONDS).toString();
        version.addProperty("time", now);
        version.addProperty("releaseTime", now);
        version.addProperty("mainClass", "fr.d4emon.fenix.loader.launch.Launch");

        // Entries carry name + sha1 + size and no URL: the jars are already on
        // disk, and there is no public repository to point at yet. The launcher
        // validates the local file by hash and moves on.
        JsonArray libraryArray = new JsonArray();
        for (Library library : libraries) {
            JsonObject entry = new JsonObject();
            entry.addProperty("name", library.mavenName());
            entry.addProperty("sha1", sha1(library.file()));
            entry.addProperty("size", Files.size(library.file()));
            libraryArray.add(entry);
        }
        version.add("libraries", libraryArray);

        Path file = minecraftDir.resolve("versions").resolve(versionId).resolve(versionId + ".json");
        Files.createDirectories(file.getParent());
        try (Writer writer = Files.newBufferedWriter(file, StandardCharsets.UTF_8)) {
            GSON.toJson(version, writer);
        }
        return file;
    }

    /**
     * Adds (or refreshes) the Fenix entry in {@code launcher_profiles.json},
     * leaving every other profile byte-for-byte in place structurally — the
     * file is parsed, one key is set, and it is written back whole.
     */
    private static Path writeProfile(Path minecraftDir, String versionId, String minecraftVersion)
            throws IOException {
        Path file = minecraftDir.resolve("launcher_profiles.json");

        JsonObject root;
        if (Files.isRegularFile(file)) {
            try (Reader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
                root = JsonParser.parseReader(reader).getAsJsonObject();
            }
        } else {
            root = new JsonObject();
        }
        if (!(root.get("profiles") instanceof JsonObject)) {
            root.add("profiles", new JsonObject());
        }

        JsonObject profile = new JsonObject();
        String now = Instant.now().truncatedTo(ChronoUnit.SECONDS).toString();
        profile.addProperty("name", "Fenix " + minecraftVersion);
        profile.addProperty("type", "custom");
        profile.addProperty("icon", "Furnace");
        profile.addProperty("created", now);
        profile.addProperty("lastUsed", now);
        profile.addProperty("lastVersionId", versionId);
        root.getAsJsonObject("profiles").add(PROFILE_KEY, profile);

        try (Writer writer = Files.newBufferedWriter(file, StandardCharsets.UTF_8)) {
            GSON.toJson(root, writer);
        }
        return file;
    }

    private static String sha1(Path file) throws IOException {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-1");
            return HexFormat.of().formatHex(digest.digest(Files.readAllBytes(file)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("every JVM ships SHA-1", e);
        }
    }
}
