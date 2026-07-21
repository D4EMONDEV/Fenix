package fr.d4emon.fenix.loader.metadata;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;
import fr.d4emon.fenix.api.ModInfo;
import fr.d4emon.fenix.api.Version;
import fr.d4emon.fenix.api.VersionRange;

import java.io.Reader;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Reads {@code fenix.mod.json}.
 *
 * <p>The JSON tree is walked by hand rather than bound to the record
 * reflectively. It is more code, but every failure can then name the field and
 * the jar it came from — and a broken metadata file is something a mod author
 * has to fix from an error message alone.
 *
 * <p>Gson comes from the vanilla classpath, so Fenix does not ship a second copy
 * of it. That is also why parsing must not rely on Gson's reflective binding,
 * which would tie the metadata format to whichever version the game happens to
 * bundle.
 */
public final class ModMetadataReader {

    /** The name of the metadata file, at the root of every mod jar. */
    public static final String FILE_NAME = "fenix.mod.json";

    /** The metadata schema version this loader understands. */
    public static final int SUPPORTED_SCHEMA = 1;

    private ModMetadataReader() {
    }

    /**
     * Reads metadata from a stream.
     *
     * @param reader the JSON to read; not closed by this method
     * @param source where it came from, used in error messages — typically a jar file name
     * @return the parsed metadata
     * @throws InvalidMetadataException if the metadata is malformed or incomplete
     * @throws NullPointerException     if either argument is {@code null}
     */
    public static ModMetadata read(Reader reader, String source) {
        Objects.requireNonNull(reader, "reader");
        Objects.requireNonNull(source, "source");

        JsonObject root = parseObject(reader, source);
        requireSupportedSchema(root, source);

        String id = requireString(root, "id", source);
        if (!ModInfo.isValidId(id)) {
            throw new InvalidMetadataException(source, "'" + id + "' is not a valid mod id (expected 2 to 64 "
                    + "characters: lowercase letters, digits and hyphens, starting with a letter)");
        }

        return new ModMetadata(
                id,
                readVersion(root, source),
                optionalString(root, "name", source),
                optionalString(root, "description", source),
                readStringArray(root, "authors", source),
                optionalString(root, "license", source),
                readStringMap(root, "contact", source),
                readSide(root, source),
                readDepends(root, source),
                readStringArray(root, "mixins", source),
                readStringArray(root, "accessible", source));
    }

    /**
     * Reads metadata from text.
     *
     * @param json   the JSON to read
     * @param source where it came from, used in error messages
     * @return the parsed metadata
     * @throws InvalidMetadataException if the metadata is malformed or incomplete
     * @throws NullPointerException     if either argument is {@code null}
     */
    public static ModMetadata read(String json, String source) {
        Objects.requireNonNull(json, "json");
        return read(new StringReader(json), source);
    }

    private static JsonObject parseObject(Reader reader, String source) {
        JsonElement root;
        try {
            root = JsonParser.parseReader(reader);
        } catch (JsonParseException e) {
            throw new InvalidMetadataException(source, "is not valid JSON: " + e.getMessage(), e);
        }

        if (root == null || !root.isJsonObject()) {
            throw new InvalidMetadataException(source, "must contain a JSON object");
        }
        return root.getAsJsonObject();
    }

    private static void requireSupportedSchema(JsonObject root, String source) {
        if (!root.has("schema")) {
            throw new InvalidMetadataException(source, "missing required field 'schema'");
        }

        JsonElement element = root.get("schema");
        if (!element.isJsonPrimitive() || !element.getAsJsonPrimitive().isNumber()) {
            throw new InvalidMetadataException(source, "'schema' must be a number");
        }

        int schema = element.getAsInt();
        if (schema != SUPPORTED_SCHEMA) {
            String hint = schema > SUPPORTED_SCHEMA
                    ? " — this mod needs a newer version of Fenix"
                    : " — this mod was built for an older version of Fenix";
            throw new InvalidMetadataException(source, "declares metadata schema " + schema
                    + ", but this loader understands " + SUPPORTED_SCHEMA + hint);
        }
    }

    private static Version readVersion(JsonObject root, String source) {
        String text = requireString(root, "version", source);
        try {
            return Version.parse(text);
        } catch (IllegalArgumentException e) {
            throw new InvalidMetadataException(source, "'version' is not a version: '" + text + "'", e);
        }
    }

    private static ModSide readSide(JsonObject root, String source) {
        String text = optionalString(root, "side", source);
        if (text == null) {
            return ModSide.BOTH;
        }
        try {
            return ModSide.parse(text);
        } catch (IllegalArgumentException e) {
            throw new InvalidMetadataException(source,
                    "'side' must be 'client', 'server' or 'both', not '" + text + "'", e);
        }
    }

    private static List<ModDependency> readDepends(JsonObject root, String source) {
        if (!root.has("depends")) {
            return List.of();
        }

        JsonElement element = root.get("depends");
        if (!element.isJsonObject()) {
            throw new InvalidMetadataException(source,
                    "'depends' must be an object mapping mod ids to version constraints");
        }

        List<ModDependency> dependencies = new ArrayList<>();
        for (Map.Entry<String, JsonElement> entry : element.getAsJsonObject().entrySet()) {
            String id = entry.getKey();
            if (!ModInfo.isValidId(id)) {
                throw new InvalidMetadataException(source, "'depends' contains '" + id + "', which is not a mod id");
            }

            String constraint = requireStringValue(entry.getValue(), "depends." + id, source);
            try {
                dependencies.add(new ModDependency(id, VersionRange.parse(constraint)));
            } catch (IllegalArgumentException e) {
                throw new InvalidMetadataException(source,
                        "'depends." + id + "' is not a version constraint: '" + constraint + "'", e);
            }
        }
        return List.copyOf(dependencies);
    }

    private static List<String> readStringArray(JsonObject root, String field, String source) {
        if (!root.has(field)) {
            return List.of();
        }

        JsonElement element = root.get(field);
        if (!element.isJsonArray()) {
            throw new InvalidMetadataException(source, "'" + field + "' must be an array of strings");
        }

        JsonArray array = element.getAsJsonArray();
        List<String> values = new ArrayList<>(array.size());
        for (int index = 0; index < array.size(); index++) {
            values.add(requireStringValue(array.get(index), field + "[" + index + "]", source));
        }
        return List.copyOf(values);
    }

    private static Map<String, String> readStringMap(JsonObject root, String field, String source) {
        if (!root.has(field)) {
            return Map.of();
        }

        JsonElement element = root.get(field);
        if (!element.isJsonObject()) {
            throw new InvalidMetadataException(source, "'" + field + "' must be an object");
        }

        Map<String, String> values = new LinkedHashMap<>();
        for (Map.Entry<String, JsonElement> entry : element.getAsJsonObject().entrySet()) {
            values.put(entry.getKey(), requireStringValue(entry.getValue(), field + "." + entry.getKey(), source));
        }
        return Map.copyOf(values);
    }

    private static String requireString(JsonObject root, String field, String source) {
        if (!root.has(field)) {
            throw new InvalidMetadataException(source, "missing required field '" + field + "'");
        }
        return requireStringValue(root.get(field), field, source);
    }

    private static String optionalString(JsonObject root, String field, String source) {
        return root.has(field) ? requireStringValue(root.get(field), field, source) : null;
    }

    private static String requireStringValue(JsonElement element, String field, String source) {
        if (!element.isJsonPrimitive() || !element.getAsJsonPrimitive().isString()) {
            throw new InvalidMetadataException(source, "'" + field + "' must be a string");
        }
        return element.getAsString();
    }
}
