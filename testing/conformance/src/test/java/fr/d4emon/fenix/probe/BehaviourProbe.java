package fr.d4emon.fenix.probe;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.JsonOps;
import com.google.gson.JsonElement;
import fr.d4emon.fenix.registry.worldgen.BiomeSpawnAccess;
import net.minecraft.SharedConstants;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.level.biome.MobSpawnSettings;
import net.minecraft.world.level.gamerules.GameRules;

import java.util.List;

/**
 * Checks that two registrations do something, rather than merely existing.
 *
 * <p>Everywhere else in this module the question is whether a thing was
 * registered. These two are different: both are wiring that can be perfectly
 * in place and still have no effect, and in both cases the symptom is the
 * absence of something.
 *
 * <ul>
 *   <li>A removed spawn that was not really removed looks like a mob that is
 *       rare today. Nobody files that.
 *   <li>A game rule that does not survive a save looks like a player who
 *       forgot they changed it. They will insist they did, and be right.
 * </ul>
 *
 * <p>Neither is reachable from a unit test: the first needs the mixin actually
 * applied to a real {@code MobSpawnSettings}, the second needs the rule in
 * vanilla's own frozen registry.
 */
public final class BehaviourProbe {

    private BehaviourProbe() {
    }

    /**
     * @param args unused; the probe is driven by the loader, not the caller
     */
    public static void main(String[] args) {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();

        spawnRemovalTakesEffect();
        gameRulesSurviveASave();

        System.out.println("behaviour conformance: all checks passed");
    }

    /**
     * A biome built with bats in it has none after a removal, and has the mob
     * that was added.
     *
     * <p>Against a table built by vanilla's own builder, because that is what
     * the mixin will meet at biome load: a map that has already been made
     * immutable. Mutating it in place would throw, and returning a modified
     * copy nobody keeps would silently do nothing — which is the failure this
     * is here for.
     */
    private static void spawnRemovalTakesEffect() {
        MobSpawnSettings settings = new MobSpawnSettings.Builder()
                .addSpawn(MobCategory.AMBIENT, 10,
                        new MobSpawnSettings.SpawnerData(EntityTypes.BAT, 8, 8))
                .addSpawn(MobCategory.CREATURE, 10,
                        new MobSpawnSettings.SpawnerData(EntityTypes.PIG, 4, 4))
                .build();
        BiomeSpawnAccess access = (BiomeSpawnAccess) settings;

        require(contains(settings, MobCategory.AMBIENT, EntityTypes.BAT),
                "the test biome should start with bats, or the removal proves nothing");

        int removed = access.fenix$removeSpawn(MobCategory.AMBIENT, EntityTypes.BAT);
        require(removed == 1, "removing bats should have reported one entry gone, not " + removed);
        require(!contains(settings, MobCategory.AMBIENT, EntityTypes.BAT),
                "bats are still in the spawn table after being removed");

        // The other category is untouched. Replacing the whole map is easy to
        // get wrong in the direction of emptying it.
        require(contains(settings, MobCategory.CREATURE, EntityTypes.PIG),
                "removing bats should not have disturbed the creature spawns");

        require(access.fenix$removeSpawn(MobCategory.AMBIENT, EntityTypes.BAT) == 0,
                "removing a mob that is already gone should report nothing removed");

        access.fenix$addSpawn(MobCategory.AMBIENT,
                new MobSpawnSettings.SpawnerData(EntityTypes.BEE, 1, 2), 30);
        require(contains(settings, MobCategory.AMBIENT, EntityTypes.BEE),
                "an added mob is not in the spawn table");
    }

    /**
     * A rule the mod registered, changed and written out, still reads back.
     *
     * <p>Through {@link GameRules#codec}, which is the same codec the world
     * save uses — so this is the round trip a world does, minus the disk.
     */
    private static void gameRulesSurviveASave() {
        GameRules rules = new GameRules(FeatureFlags.VANILLA_SET);
        require(!rules.get(ProbeContent.PROBE_FLAG),
                "a fresh rule set should hold the default the mod declared");
        require(rules.get(ProbeContent.PROBE_LIMIT) == 7,
                "the integer rule should default to 7, not " + rules.get(ProbeContent.PROBE_LIMIT));

        rules.set(ProbeContent.PROBE_FLAG, true, null);
        rules.set(ProbeContent.PROBE_LIMIT, 42, null);

        Codec<GameRules> codec = GameRules.codec(FeatureFlags.VANILLA_SET);
        JsonElement written = codec.encodeStart(JsonOps.INSTANCE, rules)
                .getOrThrow(message -> new AssertionError(
                        "behaviour conformance failed: the rules would not save: " + message));

        require(written.toString().contains("probe_flag"),
                "the mod's rule was not written at all: " + written);

        DataResult<GameRules> read = codec.parse(JsonOps.INSTANCE, written);
        GameRules loaded = read.getOrThrow(message -> new AssertionError(
                "behaviour conformance failed: the saved rules would not load: " + message));

        require(loaded.get(ProbeContent.PROBE_FLAG),
                "the boolean rule came back at its default, so the change was not saved");
        require(loaded.get(ProbeContent.PROBE_LIMIT) == 42,
                "the integer rule came back as " + loaded.get(ProbeContent.PROBE_LIMIT)
                        + " rather than the 42 that was saved");

        // A vanilla rule nobody touched still reads its own default, which is
        // what says the mod's rules joined the set rather than replaced it.
        require(loaded.get(GameRules.KEEP_INVENTORY) == GameRules.KEEP_INVENTORY.defaultValue(),
                "loading changed a vanilla rule the mod never touched");
    }

    private static boolean contains(MobSpawnSettings settings, MobCategory category,
                                    net.minecraft.world.entity.EntityType<?> entity) {
        List<?> entries = settings.getMobs(category).unwrap();
        return entries.stream()
                .map(weighted -> ((net.minecraft.util.random.Weighted<?>) weighted).value())
                .anyMatch(value -> ((MobSpawnSettings.SpawnerData) value).type() == entity);
    }

    private static void require(boolean condition, String what) {
        if (!condition) {
            throw new AssertionError("behaviour conformance failed: " + what);
        }
    }
}
