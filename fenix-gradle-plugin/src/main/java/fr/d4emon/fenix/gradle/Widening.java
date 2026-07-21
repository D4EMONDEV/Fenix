package fr.d4emon.fenix.gradle;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

/**
 * The {@code accessible} declarations a project makes about Minecraft.
 *
 * <p>Widening at run time is only half of it. The loader can open a door, but a
 * mod still has to be able to <em>write down</em> what is behind it, and that
 * is decided by the jar it compiles against. So the same declarations are
 * applied to the compile-time copy of Minecraft, and the two cannot disagree
 * because both read the one file the mod already ships.
 */
final class Widening {

    private Widening() {
    }

    /**
     * {@return what a project declared, or an empty list if it declared nothing}
     *
     * @param manifest the project's {@code fenix.mod.json}
     */
    static List<String> declarations(File manifest) {
        if (manifest == null || !manifest.isFile()) {
            return List.of();
        }
        try {
            String text = Files.readString(manifest.toPath(), StandardCharsets.UTF_8);
            JsonObject root = JsonParser.parseString(text).getAsJsonObject();
            JsonElement accessible = root.get("accessible");
            if (accessible == null || !accessible.isJsonArray()) {
                return List.of();
            }
            List<String> declarations = new ArrayList<>();
            accessible.getAsJsonArray().forEach(entry -> declarations.add(entry.getAsString()));
            return List.copyOf(declarations);
        } catch (IOException e) {
            throw new UncheckedIOException("cannot read " + manifest, e);
        } catch (RuntimeException e) {
            // A malformed manifest is the mod's problem to fix, but saying which
            // file is the difference between a minute and an afternoon.
            throw new IllegalStateException(manifest + " is not valid JSON", e);
        }
    }
}
