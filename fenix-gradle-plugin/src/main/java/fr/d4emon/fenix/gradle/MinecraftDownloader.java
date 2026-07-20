package fr.d4emon.fenix.gradle;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;
import java.util.Locale;

/**
 * Fetches a Minecraft version from Mojang and lays it out in the Fenix cache.
 *
 * <p>Only the client jar is downloaded by hand — it is not on any Maven
 * repository. The libraries are returned as coordinates for Gradle to resolve
 * from {@code libraries.minecraft.net}, which also gives caching and
 * deduplication for free.
 *
 * <p>Everything is content-addressed by SHA-1 and cached under
 * {@code <gradleUserHome>/caches/fenix/<version>/}, so a second build does no
 * network at all. The game jar is never redistributed; it only ever lives in
 * the developer's own cache.
 */
public final class MinecraftDownloader {

    private static final String MANIFEST_URL =
            "https://piston-meta.mojang.com/mc/game/version_manifest_v2.json";

    private final HttpClient http = HttpClient.newHttpClient();
    private final Path cacheRoot;

    /**
     * @param cacheRoot the {@code caches/fenix} directory in the Gradle user home
     */
    public MinecraftDownloader(Path cacheRoot) {
        this.cacheRoot = cacheRoot;
    }

    /**
     * Ensures a version is present and returns what a build needs from it.
     *
     * @param version the Minecraft version, for example {@code 26.2}
     * @return the client jar and the library coordinates
     * @throws RuntimeException if the version is unknown or a download fails
     */
    public MinecraftLibraries resolve(String version) {
        try {
            Path versionDir = Files.createDirectories(cacheRoot.resolve(version));
            JsonObject versionJson = fetchVersionJson(version, versionDir);

            Path clientJar = downloadClient(versionJson, versionDir);
            String assetIndex = versionJson.getAsJsonObject("assetIndex").get("id").getAsString();
            int javaVersion = versionJson.getAsJsonObject("javaVersion").get("majorVersion").getAsInt();

            List<String> compileLibs = new ArrayList<>();
            List<String> nativeLibs = new ArrayList<>();
            for (JsonElement element : versionJson.getAsJsonArray("libraries")) {
                JsonObject library = element.getAsJsonObject();
                if (!isAllowed(library)) {
                    continue;
                }
                String name = library.get("name").getAsString();
                (isNative(name) ? nativeLibs : compileLibs).add(name);
            }
            return new MinecraftLibraries(clientJar, compileLibs, nativeLibs, assetIndex, javaVersion);
        } catch (IOException | InterruptedException e) {
            if (e instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            throw new RuntimeException("failed to prepare Minecraft " + version + ": " + e.getMessage(), e);
        }
    }

    private JsonObject fetchVersionJson(String version, Path versionDir) throws IOException, InterruptedException {
        Path cached = versionDir.resolve(version + ".json");
        if (Files.isRegularFile(cached)) {
            return JsonParser.parseString(Files.readString(cached, StandardCharsets.UTF_8)).getAsJsonObject();
        }

        JsonObject manifest = JsonParser.parseString(getString(MANIFEST_URL)).getAsJsonObject();
        String versionUrl = null;
        for (JsonElement element : manifest.getAsJsonArray("versions")) {
            JsonObject entry = element.getAsJsonObject();
            if (entry.get("id").getAsString().equals(version)) {
                versionUrl = entry.get("url").getAsString();
                break;
            }
        }
        if (versionUrl == null) {
            throw new IOException("Minecraft " + version + " is not in Mojang's version manifest");
        }

        String json = getString(versionUrl);
        Files.writeString(cached, json, StandardCharsets.UTF_8);
        return JsonParser.parseString(json).getAsJsonObject();
    }

    private Path downloadClient(JsonObject versionJson, Path versionDir) throws IOException, InterruptedException {
        JsonObject client = versionJson.getAsJsonObject("downloads").getAsJsonObject("client");
        String sha1 = client.get("sha1").getAsString();
        String url = client.get("url").getAsString();

        Path jar = versionDir.resolve("client-" + sha1 + ".jar");
        if (Files.isRegularFile(jar) && sha1(jar).equals(sha1)) {
            return jar;
        }
        downloadTo(url, jar);
        String actual = sha1(jar);
        if (!actual.equals(sha1)) {
            Files.deleteIfExists(jar);
            throw new IOException("client jar checksum mismatch: expected " + sha1 + ", got " + actual);
        }
        return jar;
    }

    /**
     * Evaluates a library's {@code rules} against the current OS, matching
     * Mojang's own semantics: no rules means allowed; otherwise the last rule
     * whose OS clause matches decides, defaulting to disallowed.
     */
    private static boolean isAllowed(JsonObject library) {
        if (!library.has("rules")) {
            return true;
        }
        boolean allowed = false;
        for (JsonElement element : library.getAsJsonArray("rules")) {
            JsonObject rule = element.getAsJsonObject();
            if (ruleMatchesOs(rule)) {
                allowed = rule.get("action").getAsString().equals("allow");
            }
        }
        return allowed;
    }

    private static boolean ruleMatchesOs(JsonObject rule) {
        if (!rule.has("os")) {
            return true;
        }
        JsonObject os = rule.getAsJsonObject("os");
        return !os.has("name") || os.get("name").getAsString().equals(currentOsName());
    }

    /** A native library carries a {@code natives-<os>} classifier as its fourth coordinate. */
    private static boolean isNative(String coordinate) {
        int lastColon = coordinate.lastIndexOf(':');
        return lastColon > 0 && coordinate.indexOf(':') != lastColon
                && coordinate.substring(lastColon + 1).startsWith("natives-");
    }

    private static String currentOsName() {
        String os = System.getProperty("os.name", "").toLowerCase(Locale.ROOT);
        if (os.contains("win")) {
            return "windows";
        }
        if (os.contains("mac") || os.contains("osx")) {
            return "osx";
        }
        return "linux";
    }

    private String getString(String url) throws IOException, InterruptedException {
        HttpResponse<String> response = http.send(
                HttpRequest.newBuilder(URI.create(url)).GET().build(),
                HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        if (response.statusCode() != 200) {
            throw new IOException("GET " + url + " returned " + response.statusCode());
        }
        return response.body();
    }

    private void downloadTo(String url, Path target) throws IOException, InterruptedException {
        Path temp = Files.createTempFile(target.getParent(), "download-", ".part");
        try {
            HttpResponse<Path> response = http.send(
                    HttpRequest.newBuilder(URI.create(url)).GET().build(),
                    HttpResponse.BodyHandlers.ofFile(temp));
            if (response.statusCode() != 200) {
                throw new IOException("GET " + url + " returned " + response.statusCode());
            }
            Files.move(temp, target, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
        } finally {
            Files.deleteIfExists(temp);
        }
    }

    private static String sha1(Path file) throws IOException {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-1");
            try (InputStream in = Files.newInputStream(file)) {
                byte[] buffer = new byte[1 << 16];
                int read;
                while ((read = in.read(buffer)) != -1) {
                    digest.update(buffer, 0, read);
                }
            }
            return HexFormat.of().formatHex(digest.digest());
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("every JVM ships SHA-1", e);
        }
    }
}
