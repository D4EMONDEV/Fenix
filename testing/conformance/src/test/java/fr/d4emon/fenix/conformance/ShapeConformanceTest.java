package fr.d4emon.fenix.conformance;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Compares every generated block shape against the vanilla block it is modelled
 * on.
 *
 * <p>A blockstate is a table of "in this state, draw this model, turned this
 * far". Getting a rotation wrong is not a crash and not a missing texture: the
 * block renders, facing somewhere else, and only somebody standing in the right
 * place ever notices. Two of them <em>were</em> wrong when this was written — a
 * furnace and a button are drawn facing north, not south — and nothing but a
 * comparison would have caught it.
 *
 * <p>So the check is the comparison. Model names are normalised away, since
 * {@code ruby_stairs_inner} and {@code oak_stairs_inner} should differ; what is
 * left is the shape of the table and every rotation in it, which should not
 * differ at all.
 */
class ShapeConformanceTest {

    /** What Ember generated, and the vanilla blockstate it should match. */
    private static final Map<String, String> SHAPES = Map.of(
            "ruby_slab", "oak_slab",
            "ruby_stairs", "oak_stairs",
            "ruby_fence", "oak_fence",
            "ruby_gate", "oak_fence_gate",
            "ruby_wall", "cobblestone_wall",
            "ruby_trapdoor", "oak_trapdoor",
            "ruby_button", "oak_button",
            "ruby_plate", "oak_pressure_plate",
            "ruby_door", "oak_door");

    @Test
    @DisplayName("every generated shape has vanilla's own states and rotations")
    void shapesMatchVanilla() throws IOException {
        Path blockstates = requiredDir("fenix.test.exampleGenerated")
                .resolve("assets/example-mod/blockstates");
        assertTrue(Files.isDirectory(blockstates), "example-mod should have generated blockstates");

        String jar = System.getProperty("fenix.test.clientJar");
        assertNotNull(jar, "fenix.test.clientJar names the game to compare against");

        List<String> problems = new ArrayList<>();
        try (ZipFile game = new ZipFile(jar)) {
            for (Map.Entry<String, String> shape : SHAPES.entrySet()) {
                Path file = blockstates.resolve(shape.getKey() + ".json");
                assertTrue(Files.isRegularFile(file),
                        shape.getKey() + " was not generated; the demo should cover every shape");

                JsonObject mine = normalise(
                        JsonParser.parseString(Files.readString(file, StandardCharsets.UTF_8))
                                .getAsJsonObject(),
                        shape.getKey());
                JsonObject theirs = normalise(vanilla(game, shape.getValue()), shape.getValue());

                // A doubled slab points at the block it was cut from, which is a
                // different block in each case and the one thing that should differ.
                if (shape.getKey().equals("ruby_slab")) {
                    mine.getAsJsonObject("variants").getAsJsonObject("type=double")
                            .addProperty("model", "the source block");
                    theirs.getAsJsonObject("variants").getAsJsonObject("type=double")
                            .addProperty("model", "the source block");
                }

                if (!mine.equals(theirs)) {
                    problems.add(shape.getKey() + " does not match " + shape.getValue()
                            + "\n  generated: " + mine + "\n  vanilla:   " + theirs);
                }
            }
        }

        assertTrue(problems.isEmpty(), "generated shapes differing from vanilla:\n"
                + String.join("\n", problems));
    }

    /** Generated loot tables, and the vanilla table each should be shaped like. */
    private static final Map<String, String> LOOT = Map.of(
            "ruby_slab", "oak_slab",
            "ruby_door", "oak_door");

    @Test
    @DisplayName("a slab pays out twice and a door only once, as vanilla's do")
    void lootMatchesVanilla() throws IOException {
        Path tables = requiredDir("fenix.test.exampleGenerated")
                .resolve("data/example-mod/loot_table/blocks");
        assertTrue(Files.isDirectory(tables), "example-mod should have generated loot tables");

        String jar = System.getProperty("fenix.test.clientJar");
        assertNotNull(jar, "fenix.test.clientJar names the game to compare against");

        List<String> problems = new ArrayList<>();
        try (ZipFile game = new ZipFile(jar)) {
            for (Map.Entry<String, String> table : LOOT.entrySet()) {
                Path file = tables.resolve(table.getKey() + ".json");
                assertTrue(Files.isRegularFile(file), table.getKey() + " has no loot table");

                JsonObject mine = blank(
                        JsonParser.parseString(Files.readString(file, StandardCharsets.UTF_8))
                                .getAsJsonObject());
                JsonObject theirs = blank(vanillaLoot(game, table.getValue()));

                // The shape is the whole point: how many the block owes, and
                // under which state. Both are conditions and functions nested
                // several levels down, and both are silent when wrong — a slab
                // that short-changes the player, a door that pays twice.
                if (!mine.equals(theirs)) {
                    problems.add(table.getKey() + " is not shaped like " + table.getValue()
                            + "\n  generated: " + mine + "\n  vanilla:   " + theirs);
                }
            }
        }

        assertTrue(problems.isEmpty(), "loot tables differing from vanilla:\n"
                + String.join("\n", problems));
    }

    /**
     * Replaces every block and item name with a constant, so two tables for two
     * different blocks compare as the same shape.
     */
    private static JsonObject blank(JsonObject table) {
        JsonObject copy = table.deepCopy();
        blankIn(copy);
        copy.addProperty("random_sequence", "the block");
        return copy;
    }

    private static void blankIn(JsonElement element) {
        if (element.isJsonArray()) {
            element.getAsJsonArray().forEach(ShapeConformanceTest::blankIn);
            return;
        }
        if (!element.isJsonObject()) {
            return;
        }
        JsonObject object = element.getAsJsonObject();
        for (String key : List.of("name", "block")) {
            if (object.has(key) && object.get(key).isJsonPrimitive()) {
                object.addProperty(key, "the block");
            }
        }
        object.entrySet().forEach(entry -> blankIn(entry.getValue()));
    }

    private static JsonObject vanillaLoot(ZipFile game, String name) throws IOException {
        ZipEntry entry = game.getEntry("data/minecraft/loot_table/blocks/" + name + ".json");
        assertNotNull(entry, "the game should carry a loot table for " + name);
        try (var in = game.getInputStream(entry)) {
            return JsonParser.parseString(new String(in.readAllBytes(), StandardCharsets.UTF_8))
                    .getAsJsonObject();
        }
    }

    @Test
    @DisplayName("the demo covers every shape the model provider can write")
    void everyShapeIsCovered() throws IOException {
        // A shape nobody generates is a shape nobody compares, and the point of
        // the list above is that it is complete. Reading the provider's own
        // methods is the only way to notice a new one that was never demoed.
        Path source = Path.of("..", "..", "ember", "src", "main", "java", "fr", "d4emon",
                "fenix", "ember", "EmberModelProvider.java").toAbsolutePath().normalize();
        assertTrue(Files.isRegularFile(source), "the model provider should be readable at " + source);

        String text = Files.readString(source, StandardCharsets.UTF_8);
        List<String> uncovered = new ArrayList<>();
        for (String shape : List.of("slab", "stairs", "fence", "fenceGate", "wall",
                "trapdoor", "button", "pressurePlate", "door")) {
            if (!text.contains("void " + shape + "(")) {
                uncovered.add(shape + " is compared here but no longer exists");
            }
        }
        assertEquals(List.of(), uncovered, "the comparison names a shape the provider dropped");
    }

    /** {@return the vanilla blockstate of that name} */
    private static JsonObject vanilla(ZipFile game, String name) throws IOException {
        ZipEntry entry = game.getEntry("assets/minecraft/blockstates/" + name + ".json");
        assertNotNull(entry, "the game should carry a blockstate for " + name);
        try (InputStream in = game.getInputStream(entry)) {
            return JsonParser.parseString(new String(in.readAllBytes(), StandardCharsets.UTF_8))
                    .getAsJsonObject();
        }
    }

    /**
     * {@return the blockstate with every model name reduced to its suffix}
     *
     * <p>{@code example-mod:block/ruby_stairs_inner} and
     * {@code minecraft:block/oak_stairs_inner} both become {@code _inner}, so
     * what is compared is which model each state picks and how it is turned —
     * not what the blocks are called.
     */
    private static JsonObject normalise(JsonObject blockstate, String blockName) {
        JsonObject out = new JsonObject();
        for (String key : blockstate.keySet()) {
            out.add(key, normalise(blockstate.get(key), blockName));
        }
        return out;
    }

    private static JsonElement normalise(JsonElement element, String blockName) {
        if (element.isJsonObject()) {
            JsonObject object = element.getAsJsonObject();
            JsonObject out = new JsonObject();
            for (String key : object.keySet()) {
                if (key.equals("model")) {
                    String model = object.get(key).getAsString();
                    int at = model.indexOf(blockName);
                    String suffix = at == -1 ? model : model.substring(at + blockName.length());
                    out.addProperty(key, suffix.isEmpty() ? "(base)" : suffix);
                } else {
                    out.add(key, normalise(object.get(key), blockName));
                }
            }
            return out;
        }
        if (element.isJsonArray()) {
            JsonArray out = new JsonArray();
            for (JsonElement child : element.getAsJsonArray()) {
                out.add(normalise(child, blockName));
            }
            return out;
        }
        return element;
    }

    private static Path requiredDir(String property) {
        String value = System.getProperty(property);
        assertNotNull(value, property + " should be set by the build");
        Path path = Path.of(value);
        assertTrue(Files.isDirectory(path), path + " should be a directory");
        return path;
    }
}
