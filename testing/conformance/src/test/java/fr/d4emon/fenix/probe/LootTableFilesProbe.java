package fr.d4emon.fenix.probe;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.JsonOps;
import net.minecraft.SharedConstants;
import net.minecraft.data.registries.VanillaRegistries;
import net.minecraft.resources.RegistryOps;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.level.storage.loot.LootTable;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;

/**
 * Runs as the game: parses the loot tables Ember wrote with Minecraft's own
 * codec.
 *
 * <p>A loot table is pure data, and data is where a generator's mistakes hide.
 * A misspelled function, a condition nested one level wrong, an enchantment
 * named as a string where an object was wanted — none of that fails the build,
 * fails startup, or logs. The entry is dropped and the block simply drops
 * nothing, which reads as a missing table rather than a malformed one.
 *
 * <p>Parsing with the real codec is the whole check. It is also the only thing
 * that would notice the format changing under Fenix in a game update.
 */
public final class LootTableFilesProbe {

    private LootTableFilesProbe() {
    }

    /**
     * @param args the directory holding the generated block loot tables
     */
    public static void main(String[] args) throws Exception {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();

        Path directory = Path.of(args[0]);
        List<Path> files;
        try (Stream<Path> found = Files.list(directory)) {
            files = found.filter(path -> path.toString().endsWith(".json")).sorted().toList();
        }
        require(!files.isEmpty(), "there should be loot tables to check in " + directory);

        // An ore table names enchantments, and enchantments are datapack data —
        // so plain JsonOps has no registry to resolve silk_touch and fortune
        // against and the parse fails on the reference rather than on the file.
        // This is the lookup vanilla's own data generators use, which builds the
        // datapack registries from code.
        DynamicOps<JsonElement> ops = RegistryOps.create(
                JsonOps.INSTANCE, VanillaRegistries.createLookup());

        for (Path file : files) {
            JsonElement json = JsonParser.parseString(Files.readString(file, StandardCharsets.UTF_8));
            // The mod that owns these items is not loaded here — this process
            // has vanilla and nothing else — so every name in its namespace is
            // swapped for a real one first. That is the one part of the file
            // this cannot check, and the one part that cannot be interestingly
            // wrong: the names come straight from Holder.id(), which the
            // registry check already proves. Everything else — the function
            // names, the condition shapes, the nesting — is parsed as written.
            int swapped = substituteModNames(json);
            require(swapped > 0 || !file.getFileName().toString().contains("ruby"),
                    file.getFileName() + " names nothing of the mod's, which is not what was expected");
            // DIRECT_CODEC, not CODEC: the outer one resolves the table by id
            // against a registry of tables. The direct one parses the definition
            // itself, which is what the file holds.
            LootTable.DIRECT_CODEC.parse(ops, json)
                    .getOrThrow(message -> new AssertionError(
                            "loot conformance failed: " + file.getFileName()
                                    + " did not parse: " + message));
        }

        System.out.println("loot table files: " + files.size() + " parsed");
    }

    /**
     * Replaces every {@code "name"} outside Minecraft's namespace with a vanilla
     * item, and {@return how many were replaced}.
     *
     * @param element the parsed file, edited in place
     */
    private static int substituteModNames(JsonElement element) {
        int swapped = 0;
        if (element.isJsonObject()) {
            JsonObject object = element.getAsJsonObject();
            for (String key : List.copyOf(object.keySet())) {
                JsonElement value = object.get(key);
                if (key.equals("name") && value.isJsonPrimitive()
                        && value.getAsString().contains(":")
                        && !value.getAsString().startsWith("minecraft:")) {
                    object.addProperty("name", "minecraft:stone");
                    swapped++;
                } else if (key.equals("block") && value.isJsonPrimitive()
                        && value.getAsString().contains(":")
                        && !value.getAsString().startsWith("minecraft:")) {
                    // A block_state_property condition names a block and then
                    // names one of its properties, and the codec checks the
                    // second against the first. So the stand-in cannot be any
                    // block: it has to be one that has the property, or the
                    // check would fail on the substitution rather than on the
                    // file. Chosen by the property, which is the only thing
                    // here that says what kind of block this is.
                    object.addProperty("block", standInFor(object));
                    swapped++;
                } else {
                    swapped += substituteModNames(value);
                }
            }
        } else if (element.isJsonArray()) {
            for (JsonElement child : element.getAsJsonArray()) {
                swapped += substituteModNames(child);
            }
        }
        return swapped;
    }

    /**
     * A vanilla block carrying the same property the condition asks about.
     *
     * @param condition the {@code block_state_property} condition being edited
     * @return the id of a block that has that property
     */
    private static String standInFor(JsonObject condition) {
        JsonElement properties = condition.get("properties");
        if (properties != null && properties.isJsonObject()) {
            JsonObject asked = properties.getAsJsonObject();
            if (asked.has("type")) {
                return "minecraft:oak_slab";
            }
            if (asked.has("half")) {
                return "minecraft:oak_door";
            }
            if (asked.has("facing")) {
                return "minecraft:furnace";
            }
        }
        // No properties named: any block will do, and this one has none to
        // contradict.
        return "minecraft:stone";
    }

    private static void require(boolean condition, String what) {
        if (!condition) {
            throw new AssertionError("loot conformance failed: " + what);
        }
    }
}
