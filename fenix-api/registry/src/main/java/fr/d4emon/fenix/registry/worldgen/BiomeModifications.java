package fr.d4emon.fenix.registry.worldgen;

import fr.d4emon.fenix.mixin.registry.BiomeGenerationSettingsMixin;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.levelgen.GenerationStep;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
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

    /**
     * Registered from mod threads and read while datapacks load, which are not
     * the same thread.
     */
    private static final List<Addition> ADDITIONS = new CopyOnWriteArrayList<>();

    private record Addition(BiomeSelector where, GenerationStep.Decoration step,
                            ResourceKey<PlacedFeature> feature) {
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
        if (ADDITIONS.isEmpty()) {
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
                // Loudly, and here. The alternative is an ore that simply never
                // generates, which a player reports as bad luck and an author
                // spends an evening looking for in the wrong place.
                throw new IllegalStateException("no placed feature named "
                        + addition.feature().identifier()
                        + " — check that the mod ships data/<namespace>/worldgen/placed_feature/"
                        + ", or that whatever provides it is installed");
            }
            for (Holder.Reference<Biome> biome : biomes.get().listElements().toList()) {
                if (addition.where().test(new BiomeSelector.Context(biome.key(), biome))) {
                    BiomeGenerationSettingsMixin settings =
                            (BiomeGenerationSettingsMixin) (Object) biome.value().getGenerationSettings();
                    settings.fenix$addFeature(addition.step().ordinal(), feature.get());
                }
            }
        }
    }
}
