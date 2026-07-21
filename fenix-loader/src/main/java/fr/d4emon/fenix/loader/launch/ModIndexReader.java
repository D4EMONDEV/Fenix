package fr.d4emon.fenix.loader.launch;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;
import fr.d4emon.fenix.api.ModInfo;
import fr.d4emon.fenix.loader.metadata.InvalidMetadataException;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

/**
 * Reads {@code fenix.index.json}, the file the annotation processor writes into
 * a mod jar at compile time.
 *
 * <p>The index maps mod ids to the binary names of their {@code @Mod} classes.
 * A jar without one is a mod with no code — a resource pack in mod clothing —
 * which is perfectly legal.
 */
public final class ModIndexReader {

    /** The index file, at the root of the jar. Written by {@code fenix-processor}. */
    public static final String FILE_NAME = "fenix.index.json";

    /**
     * The client-only index, written from a mod's client source set.
     *
     * <p>Kept apart rather than merged so a dedicated server is never even told
     * that the class exists. Nothing there can load on a server: a class naming
     * {@code net.minecraft.client} types has no chance of resolving against a
     * jar that ships none of them.
     */
    public static final String CLIENT_FILE_NAME = "fenix.index.client.json";

    /** The index schema this loader understands. */
    public static final int SUPPORTED_SCHEMA = 1;

    private ModIndexReader() {
    }

    /**
     * Reads an index from a jar.
     *
     * @param jar the mod jar
     * @return mod id to entry class binary name; empty if the jar has no index
     * @throws InvalidMetadataException if the jar or its index cannot be read
     * @throws NullPointerException     if the jar is {@code null}
     */
    public static Map<String, String> readFromJar(Path jar) {
        return readFromJar(jar, FILE_NAME);
    }

    /**
     * Reads one of a jar's indexes.
     *
     * @param jar  the mod jar
     * @param file {@link #FILE_NAME} or {@link #CLIENT_FILE_NAME}
     * @return mod id to entry class binary name; empty if the jar has no such index
     * @throws InvalidMetadataException if the jar or its index cannot be read
     * @throws NullPointerException     if either argument is {@code null}
     */
    public static Map<String, String> readFromJar(Path jar, String file) {
        Objects.requireNonNull(jar, "jar");
        Objects.requireNonNull(file, "file");
        String source = jar.getFileName().toString();

        try (JarFile jarFile = new JarFile(jar.toFile())) {
            JarEntry entry = jarFile.getJarEntry(file);
            if (entry == null) {
                return Map.of();
            }
            try (Reader reader = new InputStreamReader(jarFile.getInputStream(entry), StandardCharsets.UTF_8)) {
                return read(reader, source);
            }
        } catch (IOException e) {
            throw new InvalidMetadataException(source, "cannot be opened as a jar: " + e.getMessage(), e);
        }
    }

    /**
     * Reads an index.
     *
     * @param reader the JSON to read; not closed by this method
     * @param source where it came from, for error messages
     * @return mod id to entry class binary name
     * @throws InvalidMetadataException if the index is malformed
     * @throws NullPointerException     if either argument is {@code null}
     */
    public static Map<String, String> read(Reader reader, String source) {
        Objects.requireNonNull(reader, "reader");
        Objects.requireNonNull(source, "source");

        JsonElement root;
        try {
            root = JsonParser.parseReader(reader);
        } catch (JsonParseException e) {
            throw new InvalidMetadataException(source, FILE_NAME + " is not valid JSON: " + e.getMessage(), e);
        }
        if (root == null || !root.isJsonObject()) {
            throw new InvalidMetadataException(source, FILE_NAME + " must contain a JSON object");
        }
        JsonObject object = root.getAsJsonObject();

        JsonElement schema = object.get("schema");
        if (schema == null || !schema.isJsonPrimitive() || !schema.getAsJsonPrimitive().isNumber()) {
            throw new InvalidMetadataException(source, FILE_NAME + " has no numeric 'schema' field");
        }
        if (schema.getAsInt() != SUPPORTED_SCHEMA) {
            throw new InvalidMetadataException(source, FILE_NAME + " declares schema " + schema.getAsInt()
                    + ", but this loader understands " + SUPPORTED_SCHEMA);
        }

        JsonElement mods = object.get("mods");
        if (mods == null || !mods.isJsonObject()) {
            throw new InvalidMetadataException(source, FILE_NAME + " has no 'mods' object");
        }

        Map<String, String> index = new LinkedHashMap<>();
        for (Map.Entry<String, JsonElement> entry : mods.getAsJsonObject().entrySet()) {
            if (!ModInfo.isValidId(entry.getKey())) {
                throw new InvalidMetadataException(source,
                        FILE_NAME + " indexes '" + entry.getKey() + "', which is not a mod id");
            }
            JsonElement value = entry.getValue();
            if (!value.isJsonPrimitive() || !value.getAsJsonPrimitive().isString()
                    || value.getAsString().isBlank()) {
                throw new InvalidMetadataException(source,
                        FILE_NAME + " entry '" + entry.getKey() + "' must name a class");
            }
            index.put(entry.getKey(), value.getAsString());
        }
        return Map.copyOf(index);
    }

    /**
     * Convenience for tests and tools: reads an index from text.
     *
     * @param json   the JSON to read
     * @param source where it came from, for error messages
     * @return mod id to entry class binary name
     * @throws InvalidMetadataException if the index is malformed
     */
    public static Map<String, String> read(String json, String source) {
        Objects.requireNonNull(json, "json");
        return read(new StringReader(json), source);
    }
}
