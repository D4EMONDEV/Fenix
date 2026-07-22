package fr.d4emon.fenix.mixin.registry;

import com.google.common.base.Suppliers;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.tags.FeatureTags;
import net.minecraft.world.level.biome.BiomeGenerationSettings;
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.function.Supplier;
import java.util.stream.Collectors;

/**
 * Lets a feature be added to a biome after the biome has been loaded.
 *
 * <p>A biome's feature list arrives from a datapack and is final, which is
 * right: it is read on every chunk. The alternative to reaching in here is
 * overriding whole biome files, and two mods that both add an ore to the plains
 * would then silently erase each other.
 *
 * <p>The two derived views have to be rebuilt as well as the list. They are
 * memoised over the <em>constructor's</em> list rather than over the field, so
 * replacing the field alone would leave bone meal and {@code hasFeature}
 * answering from the old one — the kind of half-applied change that works until
 * somebody uses bone meal on a modded flower.
 */
@Mixin(BiomeGenerationSettings.class)
public abstract class BiomeGenerationSettingsMixin {

    @Mutable
    @Shadow
    @Final
    private List<HolderSet<PlacedFeature>> features;

    @Mutable
    @Shadow
    @Final
    private Supplier<List<ConfiguredFeature<?, ?>>> boneMealFeatures;

    @Mutable
    @Shadow
    @Final
    private Supplier<Set<PlacedFeature>> featureSet;

    /**
     * Adds a feature at a generation step.
     *
     * @param step    the step's index — {@code GenerationStep.Decoration.ordinal()}
     * @param feature the feature to add
     */
    public void fenix$addFeature(int step, Holder<PlacedFeature> feature) {
        List<HolderSet<PlacedFeature>> steps = new ArrayList<>(features);
        // A biome only carries steps up to the last one it uses, so a mod
        // adding to a later step has to grow the list first.
        while (steps.size() <= step) {
            steps.add(HolderSet.direct(List.of()));
        }

        List<Holder<PlacedFeature>> atStep = new ArrayList<>(steps.get(step).stream().toList());
        atStep.add(feature);
        steps.set(step, HolderSet.direct(atStep));

        List<HolderSet<PlacedFeature>> replacement = List.copyOf(steps);
        features = replacement;
        boneMealFeatures = Suppliers.memoize(() -> replacement.stream()
                .flatMap(HolderSet::stream)
                .flatMap(placed -> placed.value().getFeatures())
                .filter(configured -> configured.is(FeatureTags.CAN_SPAWN_FROM_BONE_MEAL))
                .map(Holder::value)
                .collect(Collectors.toUnmodifiableList()));
        featureSet = Suppliers.memoize(() -> replacement.stream()
                .flatMap(HolderSet::stream)
                .map(Holder::value)
                .collect(Collectors.toUnmodifiableSet()));
    }
}
