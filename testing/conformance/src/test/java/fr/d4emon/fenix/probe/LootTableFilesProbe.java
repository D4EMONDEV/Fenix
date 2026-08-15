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
import net.minecraft.advancements.Advancement;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.level.block.entity.BannerPattern;
import net.minecraft.world.item.Instrument;
import net.minecraft.world.entity.decoration.painting.PaintingVariant;
import net.minecraft.world.item.JukeboxSong;
import net.minecraft.world.item.trading.VillagerTrade;
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
        // Walk, not list: blocks, entities and chests are separate directories
        // under loot_table, and checking only the first meant an entity table
        // with a comma for a decimal point reached a committed file.
        try (Stream<Path> found = Files.walk(directory)) {
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

        if (args.length > 1) {
            parseAdvancements(Path.of(args[1]), ops);
        }
        if (args.length > 2) {
            parseDamageTypes(Path.of(args[2]), ops);
        }
        if (args.length > 3) {
            parseEnchantments(Path.of(args[3]), ops);
        }
        if (args.length > 5) {
            parseTrades(Path.of(args[4]), Path.of(args[5]), ops);
        }
        if (args.length > 6) {
            parseCosmetics(Path.of(args[6]), ops);
        }
    }

    /**
     * Parses every advancement Ember wrote, with the game's own codec.
     *
     * <p>An advancement is the most forgiving file in a datapack and the least
     * forgiving to get wrong: a trigger that does not exist, or a requirement
     * naming a criterion that is not there, loads without complaint and can
     * never be earned. The codec is the only thing that says so.
     *
     * @param directory where Ember wrote them
     * @param ops       the ops the loot tables were parsed with
     */
    private static void parseAdvancements(Path directory, DynamicOps<JsonElement> ops)
            throws Exception {
        if (!Files.isDirectory(directory)) {
            System.out.println("advancement files: none written");
            return;
        }

        List<Path> files;
        try (Stream<Path> found = Files.walk(directory)) {
            files = found.filter(file -> file.toString().endsWith(".json")).toList();
        }

        for (Path file : files) {
            JsonElement json = JsonParser.parseString(
                    Files.readString(file, StandardCharsets.UTF_8));

            // The same substitution the loot tables need: this process has
            // vanilla and nothing else, so a mod's own item in a criterion is
            // a registry key it has never heard of. The trigger names, the
            // condition shapes and the requirement lists — everything that can
            // actually be wrong here — are parsed as written.
            substituteModNames(json);

            Advancement.CODEC.parse(ops, json)
                    .getOrThrow(message -> new AssertionError(
                            "advancement conformance failed: " + file.getFileName()
                                    + " did not parse: " + message));
        }

        require(!files.isEmpty(), "the demo should have advancements to check");
        System.out.println("advancement files: " + files.size() + " parsed");
    }

    /**
     * Parses every damage type Ember wrote.
     *
     * <p>Short files, and wrong in ways that are invisible: a scaling or an
     * effects value the game does not know is a damage type that fails to
     * load, and a mod whose damage silently stops working.
     *
     * @param directory where Ember wrote them
     * @param ops       the ops the loot tables were parsed with
     */
    private static void parseDamageTypes(Path directory, DynamicOps<JsonElement> ops)
            throws Exception {
        if (!Files.isDirectory(directory)) {
            System.out.println("damage type files: none written");
            return;
        }

        List<Path> files;
        try (Stream<Path> found = Files.walk(directory)) {
            files = found.filter(file -> file.toString().endsWith(".json")).toList();
        }

        for (Path file : files) {
            JsonElement json = JsonParser.parseString(
                    Files.readString(file, StandardCharsets.UTF_8));
            // DIRECT_CODEC: the other one resolves a reference against a
            // registry of damage types, and the file holds the definition.
            DamageType.DIRECT_CODEC.parse(ops, json)
                    .getOrThrow(message -> new AssertionError(
                            "damage type conformance failed: " + file.getFileName()
                                    + " did not parse: " + message));
        }

        require(!files.isEmpty(), "the demo should have damage types to check");
        System.out.println("damage type files: " + files.size() + " parsed");
    }

    /**
     * Parses every enchantment Ember wrote.
     *
     * <p>An enchantment is the widest data file a mod ships: its effects are a
     * language of their own, and a shape that is one level off loads as an
     * enchantment that exists, can be applied, and does nothing.
     *
     * @param directory where Ember wrote them
     * @param ops       the ops the loot tables were parsed with
     */
    private static void parseEnchantments(Path directory, DynamicOps<JsonElement> ops)
            throws Exception {
        if (!Files.isDirectory(directory)) {
            System.out.println("enchantment files: none written");
            return;
        }

        List<Path> files;
        try (Stream<Path> found = Files.walk(directory)) {
            files = found.filter(file -> file.toString().endsWith(".json")).toList();
        }

        for (Path file : files) {
            JsonElement json = JsonParser.parseString(
                    Files.readString(file, StandardCharsets.UTF_8));
            Enchantment.DIRECT_CODEC.parse(ops, json)
                    .getOrThrow(message -> new AssertionError(
                            "enchantment conformance failed: " + file.getFileName()
                                    + " did not parse: " + message));
        }

        require(!files.isEmpty(), "the demo should have enchantments to check");
        System.out.println("enchantment files: " + files.size() + " parsed");
    }

    /**
     * Parses the villager trades and the sets that draw from them.
     *
     * <p>Two halves that fail differently and both quietly: a trade nothing
     * names is a file the game loads and never offers, and a set naming a
     * trade that is not there is a villager with fewer offers than intended.
     *
     * @param tradeDir where the trades were written
     * @param setDir   where the sets were written
     * @param ops      the ops the loot tables were parsed with
     */
    private static void parseTrades(Path tradeDir, Path setDir, DynamicOps<JsonElement> ops)
            throws Exception {
        int parsed = 0;

        for (Path file : jsonIn(tradeDir)) {
            JsonElement json = JsonParser.parseString(
                    Files.readString(file, StandardCharsets.UTF_8));
            substituteModNames(json);
            VillagerTrade.CODEC.parse(ops, json)
                    .getOrThrow(message -> new AssertionError(
                            "trade conformance failed: " + file.getFileName()
                                    + " did not parse: " + message));
            parsed++;
        }

        // Not with TradeSet.CODEC. It resolves each name against a registry
        // of trades, and this process has vanilla and nothing else, so every
        // name in the mod's own namespace fails — including the correct ones.
        //
        // The check that matters is the one the codec cannot do here anyway: a
        // set naming a trade that was never written is a villager quietly
        // offering fewer things, and that is a file on disk either being there
        // or not.
        for (Path file : jsonIn(setDir)) {
            JsonObject set = JsonParser.parseString(
                    Files.readString(file, StandardCharsets.UTF_8)).getAsJsonObject();

            require(set.has("amount"), file.getFileName() + " has no amount");
            JsonElement trades = set.get("trades");
            require(trades != null && trades.isJsonArray(),
                    file.getFileName() + " names no trades");

            for (JsonElement named : trades.getAsJsonArray()) {
                String id = named.getAsString();
                String path = id.substring(id.indexOf(':') + 1);
                require(Files.isRegularFile(tradeDir.resolve(path + ".json")),
                        file.getFileName() + " names " + id + ", and no such trade was written");
            }
            parsed++;
        }

        require(parsed > 0, "the demo should have trades to check");
        System.out.println("trade files: " + parsed + " parsed");
    }

    /**
     * Parses the four presentation-only data kinds, each with its own codec.
     *
     * <p>Small files, and wrong in ways nothing reports: a song whose length
     * disagrees with its sound leaves the jukebox silent at the end, a
     * painting whose size disagrees with its texture draws stretched.
     *
     * @param root the mod's data directory
     * @param ops  the ops the loot tables were parsed with
     */
    private static void parseCosmetics(Path root, DynamicOps<JsonElement> ops) throws Exception {
        int parsed = 0;
        parsed += parseEach(root.resolve("jukebox_song"), ops,
                JukeboxSong.DIRECT_CODEC, "jukebox song");
        parsed += parseEach(root.resolve("painting_variant"), ops,
                PaintingVariant.DIRECT_CODEC, "painting");
        parsed += parseEach(root.resolve("instrument"), ops,
                Instrument.DIRECT_CODEC, "instrument");
        parsed += parseEach(root.resolve("banner_pattern"), ops,
                BannerPattern.DIRECT_CODEC, "banner pattern");

        require(parsed > 0, "the demo should have cosmetic data to check");
        System.out.println("cosmetic files: " + parsed + " parsed");
    }

    /** Parses every file in one directory with one codec. */
    private static <T> int parseEach(Path directory, DynamicOps<JsonElement> ops,
                                     com.mojang.serialization.Codec<T> codec, String kind)
            throws Exception {
        int parsed = 0;
        for (Path file : jsonIn(directory)) {
            JsonElement json = JsonParser.parseString(
                    Files.readString(file, StandardCharsets.UTF_8));
            substituteModNames(json);
            codec.parse(ops, json).getOrThrow(message -> new AssertionError(
                    kind + " conformance failed: " + file.getFileName()
                            + " did not parse: " + message));
            parsed++;
        }
        return parsed;
    }

    /** {@return every JSON file under a directory, or nothing if it is absent} */
    private static List<Path> jsonIn(Path directory) throws Exception {
        if (!Files.isDirectory(directory)) {
            return List.of();
        }
        try (Stream<Path> found = Files.walk(directory)) {
            return found.filter(file -> file.toString().endsWith(".json")).toList();
        }
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
                } else if (key.equals("sound_event") && value.isJsonPrimitive()
                        && value.getAsString().contains(":")
                        && !value.getAsString().startsWith("minecraft:")) {
                    // A jukebox song and an instrument each name a sound the
                    // mod registered, and this process has vanilla only.
                    object.addProperty("sound_event", "minecraft:block.note_block.chime");
                    swapped++;
                } else if (key.equals("id") && value.isJsonPrimitive()
                        && value.getAsString().contains(":")
                        && !value.getAsString().startsWith("minecraft:")) {
                    // An advancement's icon, and a recipe's result, name an
                    // item under "id" rather than "name".
                    object.addProperty("id", "minecraft:stone");
                    swapped++;
                } else if (key.equals("items") && value.isJsonPrimitive()
                        && value.getAsString().contains(":")
                        && !value.getAsString().startsWith("minecraft:")
                        && !value.getAsString().startsWith("#minecraft:")) {
                    // An advancement criterion names items directly.
                    object.addProperty("items", "minecraft:stone");
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
