package fr.d4emon.fenix.loader.game;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import fr.d4emon.fenix.api.Side;
import fr.d4emon.fenix.api.Version;

import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;
import java.util.Optional;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

/**
 * Finds Minecraft.
 *
 * <p>When the vanilla launcher starts a Fenix profile, the game jar is already
 * on the application classpath — the profile inherits from the vanilla version,
 * so the launcher assembles the vanilla classpath and merely swaps the main
 * class for Fenix's. The loader therefore does not download or configure
 * anything; it only has to recognise which classpath entry is the game.
 *
 * <p>A jar is the game when it contains a known Minecraft main class. The
 * client jar also contains the dedicated server's classes, so the client main
 * is checked first — a client jar must never be mistaken for a server.
 *
 * <p>Since Minecraft 26.1 the jar is unobfuscated and self-describing: its
 * {@code version.json} carries the game version, which is what lets mods
 * declare {@code "minecraft": "~26.2"} and have it checked.
 */
public final class GameLocator {

    /**
     * A located game.
     *
     * @param jar       the game jar
     * @param mainClass the game's real main class, the one Fenix hands over to
     * @param side      which side this jar launches
     * @param version   the game version from {@code version.json}, when readable
     */
    public record Game(Path jar, String mainClass, Side side, Optional<Version> version) {

        /**
         * Checks that every component is present.
         *
         * @throws NullPointerException if any component is {@code null}
         */
        public Game {
            Objects.requireNonNull(jar, "jar");
            Objects.requireNonNull(mainClass, "mainClass");
            Objects.requireNonNull(side, "side");
            Objects.requireNonNull(version, "version");
        }
    }

    private static final String CLIENT_MAIN_ENTRY = "net/minecraft/client/main/Main.class";
    private static final String SERVER_MAIN_ENTRY = "net/minecraft/server/Main.class";
    private static final String VERSION_ENTRY = "version.json";

    private GameLocator() {
    }

    /**
     * Scans a classpath for the game.
     *
     * @param classpath a {@link File#pathSeparator}-joined classpath, typically
     *                  {@code System.getProperty("java.class.path")}
     * @return the first entry that is a Minecraft jar
     * @throws NullPointerException if the classpath is {@code null}
     */
    public static Optional<Game> locate(String classpath) {
        Objects.requireNonNull(classpath, "classpath");

        for (String entry : classpath.split(File.pathSeparator)) {
            if (entry.isBlank()) {
                continue;
            }
            Optional<Game> game = inspect(Path.of(entry));
            if (game.isPresent()) {
                return game;
            }
        }
        return Optional.empty();
    }

    /**
     * Checks whether one jar is the game.
     *
     * @param jar the jar to inspect; a missing or unreadable file is simply not
     *            the game, never an error — classpaths are full of both
     * @return the game, if this jar is one
     * @throws NullPointerException if the jar is {@code null}
     */
    public static Optional<Game> inspect(Path jar) {
        Objects.requireNonNull(jar, "jar");

        if (!Files.isRegularFile(jar)) {
            return Optional.empty();
        }
        try (JarFile jarFile = new JarFile(jar.toFile())) {
            String mainClass;
            Side side;
            if (jarFile.getJarEntry(CLIENT_MAIN_ENTRY) != null) {
                mainClass = "net.minecraft.client.main.Main";
                side = Side.CLIENT;
            } else if (jarFile.getJarEntry(SERVER_MAIN_ENTRY) != null) {
                mainClass = "net.minecraft.server.Main";
                side = Side.SERVER;
            } else {
                return Optional.empty();
            }
            return Optional.of(new Game(jar, mainClass, side, readVersion(jarFile)));
        } catch (IOException e) {
            return Optional.empty();
        }
    }

    /**
     * Reads the game version out of the jar's {@code version.json}. Absent or
     * unreadable is empty rather than an error: an unrecognised future format
     * should degrade to "version unknown", not refuse to launch.
     */
    private static Optional<Version> readVersion(JarFile jarFile) {
        JarEntry entry = jarFile.getJarEntry(VERSION_ENTRY);
        if (entry == null) {
            return Optional.empty();
        }
        try (Reader reader = new InputStreamReader(jarFile.getInputStream(entry), StandardCharsets.UTF_8)) {
            JsonElement root = JsonParser.parseReader(reader);
            if (root.isJsonObject() && root.getAsJsonObject().get("id") instanceof JsonElement id
                    && id.isJsonPrimitive()) {
                return Optional.of(Version.parse(id.getAsString()));
            }
        } catch (IOException | RuntimeException e) {
            // Fall through: located, version unknown.
        }
        return Optional.empty();
    }
}
