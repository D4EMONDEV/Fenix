package fr.d4emon.fenix.registry.worldgen;

import com.mojang.logging.LogUtils;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.levelgen.GenerationStep;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.level.biome.MobSpawnSettings;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;

import org.slf4j.Logger;

import java.util.List;
import java.util.Objects;
import java.util.function.Supplier;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Adds features to biomes that already exist — an ore, a plant, a spring.
 *
 * <pre>{@code
 * BiomeModifications.addFeature(BiomeSelectors.overworld(),
 *         GenerationStep.Decoration.UNDERGROUND_ORES,
 *         ResourceKey.create(Registries.PLACED_FEATURE, Identifier.parse("mymod:ruby_ore")));
 * }</pre>
 *
 * <p>The alternative is overriding whole biome files in a datapack, and that
 * does not compose: two mods each adding an ore to the plains would overwrite
 * one another, and the player would see whichever loaded last.
 *
 * <p>The feature itself is data — a {@code configured_feature} saying what to
 * place and a {@code placed_feature} saying where, both of which
 * {@code EmberOreProvider} generates. This only says which biomes get it.
 *
 * <p>Call it from {@code onRegister}. Modifications are applied each time
 * datapacks load, which is what makes them survive {@code /reload} and apply to
 * whatever world is opened next.
 */
public final class BiomeModifications {

    /** The game's own logger, so this lands in the log a player already sends. */
    private static final Logger LOG = LogUtils.getLogger();

    /**
     * Registered from mod threads and read while datapacks load, which are not
     * the same thread.
     */
    private static final List<Addition> ADDITIONS = new CopyOnWriteArrayList<>();

    /** Features already reported, so the line is said once rather than per load. */
    private static final Set<ResourceKey<PlacedFeature>> APPLIED = ConcurrentHashMap.newKeySet();

    private record Addition(BiomeSelector where, GenerationStep.Decoration step,
                            ResourceKey<PlacedFeature> feature) {
    }

    /** Registered the same way features are, and applied on the same pass. */
    private static final List<Spawn> SPAWNS = new CopyOnWriteArrayList<>();

    /** Spawns already reported, so the line is said once rather than per load. */
    private static final Set<EntityType<?>> SPAWNS_APPLIED = ConcurrentHashMap.newKeySet();

    /** Removals, applied after every addition — see {@link #removeSpawn}. */
    private static final List<Removal> REMOVALS = new CopyOnWriteArrayList<>();

    /** Removals already reported, so the line is said once rather than per load. */
    private static final Set<EntityType<?>> REMOVALS_APPLIED = ConcurrentHashMap.newKeySet();

    private record Removal(BiomeSelector where, MobCategory category,
                           Supplier<EntityType<?>> entity) {
    }

    private record Spawn(BiomeSelector where, MobCategory category,
                         Supplier<EntityType<?>> entity, int weight, int minGroup, int maxGroup) {
    }

    private BiomeModifications() {
    }

    /**
     * Adds a placed feature to every biome a selector matches.
     *
     * @param where   which biomes
     * @param step    when during generation — {@code UNDERGROUND_ORES} for ores
     * @param feature the placed feature, by id; it does not have to exist yet
     * @throws NullPointerException if any argument is {@code null}
     */
    public static void addFeature(BiomeSelector where, GenerationStep.Decoration step,
                                  ResourceKey<PlacedFeature> feature) {
        Objects.requireNonNull(where, "where");
        Objects.requireNonNull(step, "step");
        Objects.requireNonNull(feature, "feature");
        ADDITIONS.add(new Addition(where, step, feature));
    }

    /**
     * Makes a mob spawn naturally in every biome a selector matches.
     *
     * <p>Registering an entity type is what lets it exist; this is what makes
     * the world put it there. A mod that does the first and not the second has
     * a mob that can only be spawned by hand, and nothing anywhere says why —
     * which is the commonest reason a new mob is never seen.
     *
     * <p>The weight is relative to everything else in the same category in that
     * biome, so it only means anything next to vanilla's numbers: a sheep is 12
     * in a plain, a zombie 95 in most places. The category decides which cap it
     * counts against and how far from a player it may appear;
     * {@code CREATURE} is the passive daylight one, {@code MONSTER} the hostile
     * one that despawns.
     *
     * <p>Call it from {@code onRegister}. Like a feature, it is applied each
     * time datapacks load, so it survives {@code /reload}.
     *
     * <pre>{@code
     * BiomeModifications.addSpawn(BiomeSelectors.overworld(), MobCategory.CREATURE,
     *         ModContent.RUBY_WISP.get(), 8, 1, 3);
     * }</pre>
     *
     * @param where    which biomes
     * @param category the spawning group it belongs to
     * @param entity   the mob
     * @param weight   how likely, against the others in that category
     * @param minGroup fewest that appear at once, at least one
     * @param maxGroup most that appear at once
     * @throws NullPointerException     if any reference argument is {@code null}
     * @throws IllegalArgumentException if the weight or the group size cannot work
     */
    public static void addSpawn(BiomeSelector where, MobCategory category, EntityType<?> entity,
                                int weight, int minGroup, int maxGroup) {
        Objects.requireNonNull(entity, "entity");
        addSpawn(where, category, () -> entity, weight, minGroup, maxGroup);
    }

    /**
     * Makes a mob spawn, naming it by its holder rather than by the type.
     *
     * <p>This is the overload to reach for. A holder is what {@code Registrar}
     * hands back, and it is not bound until the registrar is applied — so the
     * other overload has to be called after {@code apply()}, and calling it
     * before throws about content read too early. That is a real trap: the
     * natural place to say where a mob spawns is beside the line that
     * registers it, which is exactly where it does not yet work.
     *
     * <p>Here the holder is read when biomes load, long after everything is
     * bound, so the order the calls are written in stops mattering.
     *
     * @param where    which biomes
     * @param category the spawning group it belongs to
     * @param entity   the mob, as the registrar returned it
     * @param weight   how likely, against the others in that category
     * @param minGroup fewest that appear at once, at least one
     * @param maxGroup most that appear at once
     */
    public static void addSpawn(BiomeSelector where, MobCategory category,
                                fr.d4emon.fenix.registry.Holder<? extends EntityType<?>> entity,
                                int weight, int minGroup, int maxGroup) {
        Objects.requireNonNull(entity, "entity");
        addSpawn(where, category, entity::get, weight, minGroup, maxGroup);
    }

    private static void addSpawn(BiomeSelector where, MobCategory category,
                                 Supplier<EntityType<?>> entity, int weight, int minGroup,
                                 int maxGroup) {
        Objects.requireNonNull(where, "where");
        Objects.requireNonNull(category, "category");
        // Checked here rather than left to the game: a weight of zero is
        // accepted everywhere and simply never picked, so the mob does not
        // appear and the mod author has a correct-looking line to stare at.
        if (weight < 1) {
            throw new IllegalArgumentException(
                    "weight must be at least 1; a weight of 0 is never picked");
        }
        if (minGroup < 1 || maxGroup < minGroup) {
            throw new IllegalArgumentException(
                    "group size must be at least 1 and maxGroup at least minGroup, got "
                            + minGroup + ".." + maxGroup);
        }
        SPAWNS.add(new Spawn(where, category, entity, weight, minGroup, maxGroup));
    }


    /**
     * Stops a mob spawning in every biome a selector matches.
     *
     * <p>The counterpart to {@link #addSpawn}, and what a mod that reshapes the
     * world rather than adding to it needs: taking zombies out of one biome, or
     * a passive mob out of everywhere so a mod's own replaces it.
     *
     * <p>Removals are applied after every addition, so a removal wins against
     * another mod adding the same mob. That ordering is a choice — the opposite
     * would mean a mod could quietly undo a removal by registering later — and
     * it is worth knowing about if two mods disagree.
     *
     * <p>Removing something that was never there is not an error and not a
     * warning: a selector covering many biomes will match plenty that never had
     * the mob. The log says how many entries actually went, which is the number
     * worth reading.
     *
     * <pre>{@code
     * BiomeModifications.removeSpawn(BiomeSelectors.only(Biomes.PLAINS),
     *         MobCategory.MONSTER, EntityType.ZOMBIE);
     * }</pre>
     *
     * @param where    which biomes
     * @param category the spawning group to look in
     * @param entity   the mob to remove
     * @throws NullPointerException if any argument is {@code null}
     */
    public static void removeSpawn(BiomeSelector where, MobCategory category,
                                   EntityType<?> entity) {
        Objects.requireNonNull(entity, "entity");
        removeSpawn(where, category, () -> entity);
    }

    /**
     * Stops a mob spawning, naming it by its holder.
     *
     * <p>For a mod's own mob, for the same reason {@link #addSpawn} has this
     * overload: a holder is not bound until the registrar is applied.
     *
     * @param where    which biomes
     * @param category the spawning group to look in
     * @param entity   the mob, as the registrar returned it
     */
    public static void removeSpawn(BiomeSelector where, MobCategory category,
                                   fr.d4emon.fenix.registry.Holder<? extends EntityType<?>> entity) {
        Objects.requireNonNull(entity, "entity");
        removeSpawn(where, category, entity::get);
    }

    private static void removeSpawn(BiomeSelector where, MobCategory category,
                                    Supplier<EntityType<?>> entity) {
        Objects.requireNonNull(where, "where");
        Objects.requireNonNull(category, "category");
        REMOVALS.add(new Removal(where, category, entity));
    }

    /**
     * Applies every registered modification to a freshly loaded set of
     * registries.
     *
     * <p>Called by Fenix once datapacks have loaded — late enough that biome
     * tags are bound, so a selector can ask about them, and early enough that
     * no chunk has been generated.
     *
     * @param registries the registries just loaded
     */
    public static void fenix$apply(RegistryAccess registries) {
        if (ADDITIONS.isEmpty() && SPAWNS.isEmpty() && REMOVALS.isEmpty()) {
            return;
        }
        Optional<Registry<Biome>> biomes = registries.lookup(Registries.BIOME);
        Optional<Registry<PlacedFeature>> features = registries.lookup(Registries.PLACED_FEATURE);
        if (biomes.isEmpty() || features.isEmpty()) {
            // Not the biome-carrying load — the client gets several.
            return;
        }

        for (Addition addition : ADDITIONS) {
            Optional<Holder.Reference<PlacedFeature>> feature = features.get().get(addition.feature());
            if (feature.isEmpty()) {
                // Not a problem in itself, and not worth a warning. The game
                // loads its worldgen registries more than once — the
                // world-creation screen loads them before it has even found the
                // mod's datapack — so a pass without a mod's data is the normal
                // first half of a normal launch. Warning here cried wolf on
                // every start, and throwing here took the screen down with it.
                continue;
            }

            int changed = 0;
            for (Holder.Reference<Biome> biome : biomes.get().listElements().toList()) {
                if (addition.where().test(new BiomeSelector.Context(biome.key(), biome))) {
                    // The interface, never the mixin class: Mixin merges a
                    // mixin into its target and then refuses to load the mixin
                    // itself, so casting to one fails where it runs rather than
                    // where it compiles.
                    BiomeFeatureAccess settings =
                            (BiomeFeatureAccess) (Object) biome.value().getGenerationSettings();
                    settings.fenix$addFeature(addition.step().ordinal(), feature.get());
                    changed++;
                }
            }

            // Said once, on the first load that actually carries the feature.
            // This is the line that answers "is my ore live?" — and its absence
            // is the tell when nothing generates, which is a question a log
            // could not answer before.
            if (APPLIED.add(addition.feature())) {
                LOG.info("Fenix: {} added to {} biomes at {}", addition.feature().identifier(),
                        changed, addition.step().getName());
            }
        }

        for (Spawn spawn : SPAWNS) {
            int changed = 0;
            for (Holder.Reference<Biome> biome : biomes.get().listElements().toList()) {
                if (spawn.where().test(new BiomeSelector.Context(biome.key(), biome))) {
                    BiomeSpawnAccess settings =
                            (BiomeSpawnAccess) (Object) biome.value().getMobSettings();
                    settings.fenix$addSpawn(spawn.category(),
                            new MobSpawnSettings.SpawnerData(spawn.entity().get(),
                                    spawn.minGroup(), spawn.maxGroup()),
                            spawn.weight());
                    changed++;
                }
            }

            // The same line the feature pass writes, and for the same reason:
            // its absence is the answer to "why is my mob never anywhere".
            if (SPAWNS_APPLIED.add(spawn.entity().get())) {
                LOG.info("Fenix: {} spawns in {} biomes as {} (weight {})",
                        BuiltInRegistries.ENTITY_TYPE.getKey(spawn.entity().get()), changed,
                        spawn.category().getName(), spawn.weight());
            }
        }

        // Last, so a removal wins against an addition of the same mob — see
        // removeSpawn for why that way round.
        for (Removal removal : REMOVALS) {
            EntityType<?> entity = removal.entity().get();
            int removed = 0;
            int biomesChanged = 0;
            for (Holder.Reference<Biome> biome : biomes.get().listElements().toList()) {
                if (removal.where().test(new BiomeSelector.Context(biome.key(), biome))) {
                    BiomeSpawnAccess settings =
                            (BiomeSpawnAccess) (Object) biome.value().getMobSettings();
                    int went = settings.fenix$removeSpawn(removal.category(), entity);
                    removed += went;
                    if (went > 0) {
                        biomesChanged++;
                    }
                }
            }

            // The count is the useful part: a selector covering many biomes
            // will match plenty that never had the mob, so "0 entries" is the
            // answer to "did my removal do anything", and it is not an error.
            if (REMOVALS_APPLIED.add(entity)) {
                LOG.info("Fenix: {} no longer spawns as {} - {} entries removed from {} biomes",
                        BuiltInRegistries.ENTITY_TYPE.getKey(entity), removal.category().getName(),
                        removed, biomesChanged);
            }
        }
    }
}
