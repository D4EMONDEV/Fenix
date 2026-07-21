package fr.d4emon.fenix.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;
import fr.d4emon.fenix.api.Fenix;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * A mod's settings, as a record.
 *
 * <pre>{@code
 * public record Settings(boolean spawnWisps, int maxWisps, Difficulty floor) {
 * }
 *
 * private static final Config<Settings> CONFIG =
 *         Config.of(fenix, new Settings(true, 20, Difficulty.EASY));
 *
 * if (CONFIG.get().spawnWisps()) { … }
 * }</pre>
 *
 * <p>The record is the schema, the defaults and the documentation at once: its
 * component names are the file's keys, its types decide what a value may be,
 * and the instance you pass is what a missing setting falls back to. There is
 * no separate spec to keep in step with it.
 *
 * <p>Validation belongs in the record's compact constructor, where it can be
 * written once and cannot be skipped:
 *
 * <pre>{@code
 * public record Settings(int maxWisps) {
 *     public Settings {
 *         if (maxWisps < 1) {
 *             throw new IllegalArgumentException("maxWisps must be at least 1");
 *         }
 *     }
 * }
 * }</pre>
 *
 * <p>That message reaches the player prefixed with the file and field, rather
 * than as a stack trace.
 *
 * <p>The file is rewritten after every load, so a setting added by an update
 * appears with its default rather than staying invisible until somebody reads
 * the changelog.
 *
 * @param <T> the record holding the settings
 */
public final class Config<T extends Record> {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private final Path file;
    private final T defaults;
    private final Fenix fenix;

    private volatile T current;

    private Config(Fenix fenix, Path file, T defaults) {
        this.fenix = fenix;
        this.file = file;
        this.defaults = defaults;
        this.current = defaults;
    }

    /**
     * Loads a mod's settings from {@code config.json}.
     *
     * @param <T>      the record type
     * @param fenix    the mod's context, which knows where its files live
     * @param defaults every setting, with the value to use when the file is
     *                 silent about it
     * @return the handle, already loaded
     */
    public static <T extends Record> Config<T> of(Fenix fenix, T defaults) {
        return of(fenix, "config", defaults);
    }

    /**
     * Loads a named settings file, for a mod with more than one.
     *
     * @param <T>      the record type
     * @param fenix    the mod's context
     * @param name     the file name, without {@code .json}
     * @param defaults every setting, with its fallback value
     * @return the handle, already loaded
     */
    public static <T extends Record> Config<T> of(Fenix fenix, String name, T defaults) {
        Objects.requireNonNull(fenix, "fenix");
        Objects.requireNonNull(defaults, "defaults");
        Path file = fenix.configDir().resolve(Objects.requireNonNull(name, "name") + ".json");

        Config<T> config = new Config<>(fenix, file, defaults);
        config.reload();
        return config;
    }

    /**
     * {@return the settings as they were last read}
     *
     * <p>Safe to call from any thread and cheap enough to call in a loop: it
     * reads a field, and the record it returns cannot change underneath.
     */
    public T get() {
        return current;
    }

    /**
     * {@return where the file is, for a mod that wants to say so}
     */
    public Path file() {
        return file;
    }

    /**
     * Reads the file again, and writes it back complete.
     *
     * <p>Called once by {@link #of}; call it again after a command that lets a
     * player edit the file and reload without restarting.
     *
     * @throws ConfigException if the file exists and cannot be understood
     */
    public void reload() {
        JsonObject json = readFile();
        List<String> unknown = new ArrayList<>();
        T loaded = Records.read(json, defaults, unknown, "");

        for (String key : unknown) {
            // Named, never dropped in silence. A key that does nothing is
            // almost always a typo, and the player has no way to tell.
            fenix.logger().warn("{} has no setting called '{}' — it is ignored", file, key);
        }
        current = loaded;
        writeFile(Records.write(loaded));
    }

    private JsonObject readFile() {
        if (!Files.isRegularFile(file)) {
            return new JsonObject();
        }
        try {
            String text = Files.readString(file, StandardCharsets.UTF_8);
            return JsonParser.parseString(text).getAsJsonObject();
        } catch (IOException e) {
            throw new ConfigException(file + " cannot be read: " + e.getMessage(), e);
        } catch (JsonParseException | IllegalStateException e) {
            throw new ConfigException(file + " is not valid JSON: " + e.getMessage(), e);
        }
    }

    private void writeFile(JsonObject json) {
        try {
            Files.createDirectories(file.getParent());
            // Through a temporary name: a crash or a full disk halfway through
            // would otherwise leave a truncated file that the next launch
            // refuses, losing settings the player had already chosen.
            Path partial = file.resolveSibling(file.getFileName() + ".partial");
            Files.writeString(partial, GSON.toJson(json) + System.lineSeparator(),
                    StandardCharsets.UTF_8);
            Files.move(partial, file, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            throw new UncheckedIOException("cannot write " + file, e);
        }
    }
}
