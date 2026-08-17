package fr.d4emon.fenix.probe;

import com.google.gson.JsonArray;
import java.util.ArrayList;
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
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.dimension.DimensionType;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.level.block.entity.BannerPattern;
import net.minecraft.world.item.Instrument;
import net.minecraft.world.entity.decoration.painting.PaintingVariant;
import net.minecraft.world.item.JukeboxSong;
import net.minecraft.world.item.trading.VillagerTrade;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureProcessorType;
import net.minecraft.world.item.equipment.trim.TrimPattern;
import net.minecraft.world.item.equipment.trim.TrimMaterial;
import net.minecraft.world.entity.animal.cow.CowVariant;
import net.minecraft.world.level.levelgen.NoiseGeneratorSettings;
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
            parseBiomes(Path.of(args[6]).resolve("worldgen/biome"), ops);
            parseDimensions(Path.of(args[6]), ops);
            parseStructures(Path.of(args[6]), ops, args.length > 7 ? Path.of(args[7]) : null);
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

        int modTriggers = 0;
        int parsed = 0;
        for (Path file : files) {
            JsonElement json = JsonParser.parseString(
                    Files.readString(file, StandardCharsets.UTF_8));

            // The same substitution the loot tables need: this process has
            // vanilla and nothing else, so a mod's own item in a criterion is
            // a registry key it has never heard of. The trigger names, the
            // condition shapes and the requirement lists — everything that can
            // actually be wrong here — are parsed as written.
            substituteModNames(json);

            // Except a trigger the mod registered itself, which has no vanilla
            // stand-in at all. Swapping one in would not check it: it would
            // check some other trigger's conditions against this trigger's
            // JSON, and pass whatever was written. So those are counted and
            // left out here. They are not unchecked — Ember reads every
            // advancement back with this same codec as it writes it, inside a
            // game that does have the mod, which is stricter than this can be.
            int skipped = removeModTriggers(json);
            modTriggers += skipped;
            if (skipped > 0 && json.getAsJsonObject().getAsJsonObject("criteria").isEmpty()) {
                // Nothing vanilla left to parse. Counted above, so the file is
                // not silently forgotten.
                continue;
            }

            Advancement.CODEC.parse(ops, json)
                    .getOrThrow(message -> new AssertionError(
                            "advancement conformance failed: " + file.getFileName()
                                    + " did not parse: " + message));
            parsed++;
        }

        require(!files.isEmpty(), "the demo should have advancements to check");
        // The demo is expected to have one, and this is the check that says so:
        // without it, a mod trigger that stopped being used would look like a
        // clean run rather than like coverage that quietly went away.
        require(modTriggers > 0,
                "the demo should use at least one trigger of its own, and none was found");
        System.out.println("advancement files: " + parsed + " of " + files.size()
                + " parsed here, " + modTriggers
                + " criteria on the mod's own triggers left to Ember");
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

        int modEffects = 0;
        for (Path file : files) {
            JsonElement json = JsonParser.parseString(
                    Files.readString(file, StandardCharsets.UTF_8));

            // An effect kind the mod invented has no vanilla stand-in — the
            // registry key simply is not in this process. Those entries are
            // counted and dropped rather than swapped for some other effect,
            // which would check a different effect's fields against these.
            // Ember reads the whole file back with this same codec as it
            // writes it, in a game that does have the mod.
            modEffects += removeModEffects(json);

            Enchantment.DIRECT_CODEC.parse(ops, json)
                    .getOrThrow(message -> new AssertionError(
                            "enchantment conformance failed: " + file.getFileName()
                                    + " did not parse: " + message));
        }

        require(!files.isEmpty(), "the demo should have enchantments to check");
        // Without this, an effect that stopped being used reads as a clean run.
        require(modEffects > 0,
                "the demo should use at least one enchantment effect of its own, "
                        + "and none was found");
        System.out.println("enchantment files: " + files.size() + " parsed, "
                + modEffects + " effect(s) of the mod's own left to Ember");
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

        // The trims. A pattern names a texture and a material names a palette,
        // and neither is checked by anything else: a misspelled asset draws as
        // no trim at all rather than as a missing texture, because a trim that
        // resolves to nothing is simply not drawn.
        int trims = parseEach(root.resolve("trim_pattern"), ops,
                TrimPattern.DIRECT_CODEC, "trim pattern");
        trims += parseEach(root.resolve("trim_material"), ops,
                TrimMaterial.DIRECT_CODEC, "trim material");

        // The animal variants. The codec checks the shape — an unknown model
        // type, a condition of the wrong form — and that is worth having: a
        // variant that fails to parse never appears at all.
        //
        // What it does not check is which biome a condition names. Tags are
        // resolved long after this, so a spawn condition naming a tag that
        // does not exist parses cleanly here and produces a variant nothing
        // ever spawns. Tried, and it passed; saying so is better than leaving
        // the impression that it is covered.
        int variants = parseEach(root.resolve("cow_variant"), ops,
                CowVariant.DIRECT_CODEC, "cow variant");

        // And the noise settings, which decide what a dimension is made of.
        // Fifteen density functions are required and every one of them is a
        // tree the codec walks; a router missing a field produces a dimension
        // that fails to load with a message naming the field, but only when
        // someone travels there.
        int noise = parseEach(root.resolve("worldgen/noise_settings"), ops,
                NoiseGeneratorSettings.DIRECT_CODEC, "noise settings");
        require(noise > 0, "the demo should ship noise settings of its own");

        require(trims > 0, "the demo should ship an armour trim");
        require(variants > 0, "the demo should ship an animal variant");
        System.out.println("cosmetics: " + trims + " trim file(s), "
                + variants + " variant(s) parsed");
    }

    /**
     * Parses every biome Ember wrote.
     *
     * <p>A biome is the longest file a mod ships and the most positional: the
     * features are an array of arrays whose index is the generation step.
     *
     * <p>The codec does <em>not</em> mind a short one — ten steps parse as
     * happily as eleven, and everything after the missing one has quietly
     * moved. That was checked by writing ten and watching this pass. So the
     * length is asserted here, by hand, because the codec will not do it.
     *
     * @param directory where Ember wrote them
     * @param ops       the ops the loot tables were parsed with
     */
    private static void parseBiomes(Path directory, DynamicOps<JsonElement> ops)
            throws Exception {
        List<Path> files = jsonIn(directory);
        if (files.isEmpty()) {
            System.out.println("biome files: none written");
            return;
        }

        for (Path file : files) {
            JsonElement json = JsonParser.parseString(
                    Files.readString(file, StandardCharsets.UTF_8));

            // Features, carvers and spawn types are the mod's own, and this
            // process has vanilla only. Unlike everywhere else these are bare
            // strings in arrays rather than values under a key, so they need
            // their own pass.
            substituteInArray(json, "features", "minecraft:ore_coal_upper");
            substituteInArray(json, "carvers", "minecraft:cave");
            substituteModNames(json);

            Biome.DIRECT_CODEC.parse(ops, json)
                    .getOrThrow(message -> new AssertionError(
                            "biome conformance failed: " + file.getFileName()
                                    + " did not parse: " + message));

            JsonElement steps = json.getAsJsonObject().get("features");
            require(steps != null && steps.isJsonArray()
                            && steps.getAsJsonArray().size() == 11,
                    file.getFileName() + " has "
                            + (steps == null || !steps.isJsonArray()
                                    ? "no features array"
                                    : steps.getAsJsonArray().size() + " generation steps")
                            + " rather than 11; the array is positional, so a short one "
                            + "moves every step after the gap and the codec does not mind");
        }

        System.out.println("biome files: " + files.size() + " parsed");
    }

    /**
     * Replaces modded ids inside a named array, however deeply it is nested.
     *
     * @param element   the parsed file, edited in place
     * @param key       the array's name
     * @param standIn   a vanilla id of the same kind
     */
    private static void substituteInArray(JsonElement element, String key, String standIn) {
        if (element.isJsonArray()) {
            element.getAsJsonArray().forEach(child -> substituteInArray(child, key, standIn));
            return;
        }
        if (!element.isJsonObject()) {
            return;
        }
        JsonObject object = element.getAsJsonObject();
        JsonElement found = object.get(key);
        if (found != null && found.isJsonArray()) {
            replaceModded(found.getAsJsonArray(), standIn);
        }
        object.entrySet().forEach(entry -> substituteInArray(entry.getValue(), key, standIn));
    }

    /** Swaps modded strings for a vanilla one, walking nested arrays. */
    private static void replaceModded(com.google.gson.JsonArray array, String standIn) {
        for (int i = 0; i < array.size(); i++) {
            JsonElement value = array.get(i);
            if (value.isJsonArray()) {
                replaceModded(value.getAsJsonArray(), standIn);
            } else if (value.isJsonPrimitive() && value.getAsString().contains(":")
                    && !value.getAsString().startsWith("minecraft:")) {
                array.set(i, new com.google.gson.JsonPrimitive(standIn));
            }
        }
    }

    /**
     * Parses the dimension types and the dimensions built from them.
     *
     * <p>The type is checked with its own codec, which does mind a height that
     * is not a multiple of sixteen. The dimension itself is checked only for
     * shape: {@code LevelStem.CODEC} resolves the type and the biome against
     * registries this process does not have the mod's half of, and swapping
     * them for vanilla ids would leave a check that proves the stand-ins parse.
     *
     * @param root the mod's data directory
     * @param ops  the ops the loot tables were parsed with
     */
    private static void parseDimensions(Path root, DynamicOps<JsonElement> ops)
            throws Exception {
        int parsed = 0;

        for (Path file : jsonIn(root.resolve("dimension_type"))) {
            JsonElement json = JsonParser.parseString(
                    Files.readString(file, StandardCharsets.UTF_8));
            DimensionType.DIRECT_CODEC.parse(ops, json)
                    .getOrThrow(message -> new AssertionError(
                            "dimension type conformance failed: " + file.getFileName()
                                    + " did not parse: " + message));
            parsed++;
        }

        for (Path file : jsonIn(root.resolve("dimension"))) {
            JsonObject json = JsonParser.parseString(
                    Files.readString(file, StandardCharsets.UTF_8)).getAsJsonObject();

            // What can actually be wrong here, and what nothing else would say:
            // a dimension naming a type that was never written, or a generator
            // with no biome source, is a dimension the game refuses to make
            // and a /execute in that answers "unknown dimension".
            require(json.has("type"), file.getFileName() + " names no dimension type");
            String type = json.get("type").getAsString();
            String local = type.substring(type.indexOf(':') + 1);
            require(Files.isRegularFile(root.resolve("dimension_type").resolve(local + ".json")),
                    file.getFileName() + " names the type " + type
                            + ", and no such dimension type was written");

            JsonElement generator = json.get("generator");
            require(generator != null && generator.isJsonObject()
                            && generator.getAsJsonObject().has("biome_source"),
                    file.getFileName() + " has no biome source, so it generates nothing");
            parsed++;
        }

        if (parsed > 0) {
            System.out.println("dimension files: " + parsed + " parsed");
        }
    }

    /**
     * Checks the four files a structure is made of.
     *
     * <p>Only the set has a codec that can be used here: the structure resolves
     * its start pool and its biomes against registries this process does not
     * have, and the pool resolves the templates it names. So the links are
     * checked by hand, which is the part that actually goes wrong:
     *
     * <ul>
     *   <li>a structure with no set is never placed by the world, and
     *       {@code /place} still works, so it looks finished
     *   <li>a set naming a structure that is not there places nothing, silently
     *   <li>a pool naming a template with no {@code .nbt} generates empty air
     * </ul>
     *
     * @param root      the mod's generated data directory
     * @param ops       the ops the loot tables were parsed with
     * @param resources the mod's hand-written data directory, where templates
     *                  live, or {@code null} if it was not given
     */
    private static void parseStructures(Path root, DynamicOps<JsonElement> ops, Path resources)
            throws Exception {
        Path structures = root.resolve("worldgen/structure");
        Path sets = root.resolve("worldgen/structure_set");
        Path pools = root.resolve("worldgen/template_pool");
        Path lists = root.resolve("worldgen/processor_list");

        List<Path> found = jsonIn(structures);
        if (found.isEmpty()) {
            System.out.println("structure files: none written");
            return;
        }

        // Which structures a set places, so an unplaced one can be named.
        java.util.Set<String> placed = new java.util.HashSet<>();
        for (Path file : jsonIn(sets)) {
            JsonObject set = JsonParser.parseString(
                    Files.readString(file, StandardCharsets.UTF_8)).getAsJsonObject();

            JsonElement placement = set.get("placement");
            require(placement != null && placement.isJsonObject(),
                    file.getFileName() + " has no placement, so it places nothing");

            JsonElement list = set.get("structures");
            require(list != null && list.isJsonArray() && !list.getAsJsonArray().isEmpty(),
                    file.getFileName() + " names no structures");

            for (JsonElement entry : list.getAsJsonArray()) {
                String id = entry.getAsJsonObject().get("structure").getAsString();
                placed.add(id);
                String local = id.substring(id.indexOf(':') + 1);
                require(Files.isRegularFile(structures.resolve(local + ".json")),
                        file.getFileName() + " places " + id
                                + ", and no such structure was written");
            }
        }

        for (Path file : found) {
            JsonObject structure = JsonParser.parseString(
                    Files.readString(file, StandardCharsets.UTF_8)).getAsJsonObject();
            String local = file.getFileName().toString().replace(".json", "");

            require(structure.has("start_pool"),
                    file.getFileName() + " has no start pool, so it has no first piece");
            String pool = structure.get("start_pool").getAsString();
            String poolLocal = pool.substring(pool.indexOf(':') + 1);
            require(Files.isRegularFile(pools.resolve(poolLocal + ".json")),
                    file.getFileName() + " starts from " + pool
                            + ", and no such template pool was written");

            require(placed.stream().anyMatch(id -> id.endsWith(":" + local)),
                    local + " is a structure no structure set places, so the world will "
                            + "never generate it - /place would still work, which is why "
                            + "this is easy to miss");
        }

        // Every piece a pool names has to have its .nbt, or the structure
        // generates as nothing and reports nothing.
        for (Path file : jsonIn(pools)) {
            JsonObject pool = JsonParser.parseString(
                    Files.readString(file, StandardCharsets.UTF_8)).getAsJsonObject();
            JsonElement elements = pool.get("elements");
            require(elements != null && elements.isJsonArray()
                            && !elements.getAsJsonArray().isEmpty(),
                    file.getFileName() + " has no pieces, so it generates empty air");

            if (resources == null) {
                continue;
            }
            for (JsonElement entry : elements.getAsJsonArray()) {
                JsonElement element = entry.getAsJsonObject().get("element");
                JsonElement location = element.getAsJsonObject().get("location");
                if (location == null) {
                    continue;
                }
                String id = location.getAsString();
                String template = id.substring(id.indexOf(':') + 1);
                require(Files.isRegularFile(resources.resolve("structure")
                                .resolve(template + ".nbt")),
                        file.getFileName() + " names the template " + id
                                + ", and no such .nbt was shipped");

                // And the processor list it is placed through. Naming one that
                // was never written is not an error the game reports: the
                // structure generates, unprocessed, looking exactly like a
                // processor list that was written and does nothing.
                JsonElement processors = element.getAsJsonObject().get("processors");
                if (processors != null && processors.isJsonPrimitive()) {
                    String list = processors.getAsString();
                    if (!list.equals("minecraft:empty")) {
                        String listLocal = list.substring(list.indexOf(':') + 1);
                        require(Files.isRegularFile(
                                        lists.resolve(listLocal + ".json")),
                                file.getFileName() + " places " + id + " through " + list
                                        + ", and no such processor list was written");
                    }
                }
            }
        }

        // Parsed with the game's own codec, which is the only thing that reads
        // a processor's fields. A misspelled processor_type, an integrity that
        // is a string, a rule missing its predicate — none of that stops a
        // world loading. The structure is placed unprocessed.
        int processorLists = parseEach(lists, ops,
                StructureProcessorType.DIRECT_CODEC, "processor list");

        System.out.println("structure files: " + found.size() + " structure(s) checked, "
                + processorLists + " processor list(s) parsed");
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
     * Drops every enchantment effect entry naming a type this process has
     * never heard of.
     *
     * @param element the enchantment, changed in place
     * @return how many entries were dropped
     */
    private static int removeModEffects(JsonElement element) {
        JsonObject effects = element.getAsJsonObject().getAsJsonObject("effects");
        if (effects == null) {
            return 0;
        }

        int dropped = 0;
        for (String component : List.copyOf(effects.keySet())) {
            JsonElement value = effects.get(component);
            if (!value.isJsonArray()) {
                continue;
            }
            JsonArray kept = new JsonArray();
            for (JsonElement entry : value.getAsJsonArray()) {
                JsonElement effect = entry.isJsonObject()
                        ? entry.getAsJsonObject().get("effect") : null;
                String type = effect != null && effect.isJsonObject()
                        && effect.getAsJsonObject().has("type")
                        ? effect.getAsJsonObject().get("type").getAsString() : "minecraft:";
                if (type.startsWith("minecraft:")) {
                    kept.add(entry);
                } else {
                    dropped++;
                }
            }
            // A component whose list is now empty is removed: an empty list is
            // not what the codec expects, and leaving one would fail the parse
            // on this edit rather than on the file.
            if (kept.isEmpty()) {
                effects.remove(component);
            } else {
                effects.add(component, kept);
            }
        }
        return dropped;
    }

    /**
     * Drops every criterion naming a trigger this process does not have, and
     * every requirement that named one.
     *
     * <p>A criterion left in a requirement list after its criterion is gone is
     * exactly the error this probe exists to find, so removing one without the
     * other would make the probe fail on its own edit rather than on the file.
     *
     * @param element the advancement, changed in place
     * @return how many criteria were dropped
     */
    private static int removeModTriggers(JsonElement element) {
        JsonObject criteria = element.getAsJsonObject().getAsJsonObject("criteria");
        if (criteria == null) {
            return 0;
        }

        List<String> dropped = new ArrayList<>();
        for (String key : List.copyOf(criteria.keySet())) {
            String trigger = criteria.getAsJsonObject(key).get("trigger").getAsString();
            if (!trigger.startsWith("minecraft:")) {
                criteria.remove(key);
                dropped.add(key);
            }
        }
        if (dropped.isEmpty()) {
            return 0;
        }

        JsonArray requirements = element.getAsJsonObject().getAsJsonArray("requirements");
        if (requirements != null) {
            JsonArray kept = new JsonArray();
            for (JsonElement group : requirements) {
                JsonArray names = new JsonArray();
                for (JsonElement name : group.getAsJsonArray()) {
                    if (!dropped.contains(name.getAsString())) {
                        names.add(name);
                    }
                }
                if (!names.isEmpty()) {
                    kept.add(names);
                }
            }
            element.getAsJsonObject().add("requirements", kept);
        }
        return dropped.size();
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
                } else if (key.equals("type") && value.isJsonPrimitive()
                        && value.getAsString().contains(":")
                        && !value.getAsString().startsWith("minecraft:")) {
                    // A biome's spawner entries name an entity type here.
                    object.addProperty("type", "minecraft:pig");
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
                } else if (key.equals("Name") && value.isJsonPrimitive()
                        && value.getAsString().contains(":")
                        && !value.getAsString().startsWith("minecraft:")) {
                    // A block state, which is how noise settings name their
                    // default block and how a processor rule names its output.
                    // Capitalised, unlike every other name in a datapack.
                    object.addProperty("Name", "minecraft:stone");
                    swapped++;
                } else if (key.equals("biomes") && value.isJsonArray()) {
                    // A variant's spawn condition names biomes as a holder set.
                    // The mod's own biome is not in this process, so a vanilla
                    // one stands in — what is being checked here is the shape
                    // of the condition, not which biome it names.
                    JsonArray stood = new JsonArray();
                    for (JsonElement biome : value.getAsJsonArray()) {
                        stood.add(biome.isJsonPrimitive()
                                && !biome.getAsString().startsWith("minecraft:")
                                ? new com.google.gson.JsonPrimitive("minecraft:plains")
                                : biome);
                    }
                    object.add("biomes", stood);
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
