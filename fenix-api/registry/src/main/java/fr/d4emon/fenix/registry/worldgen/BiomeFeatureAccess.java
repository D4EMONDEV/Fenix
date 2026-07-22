package fr.d4emon.fenix.registry.worldgen;

import net.minecraft.core.Holder;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;

/**
 * Adds a feature to a biome's generation settings, from outside them.
 *
 * <p>Not a mixin, and deliberately not in the mixin package either. A mixin
 * class is a template: Mixin merges its members into the target and then
 * refuses to load the template itself, so casting to one gets
 * {@code IllegalClassLoadError} at the moment it runs. Mixin owns the whole
 * package a config declares, not merely the classes it lists, so an ordinary
 * interface put next to a mixin is refused for the same reason.
 *
 * <p>Internal. A mod has {@code BiomeModifications} and no reason to touch
 * this.
 *
 * <p>So the mixin implements this, Mixin gives the interface to
 * {@code BiomeGenerationSettings} along with the method, and a caller casts to
 * the interface. The target has no idea it implements anything.
 */
public interface BiomeFeatureAccess {

    /**
     * Adds a feature at a generation step.
     *
     * @param step    the step's index — {@code GenerationStep.Decoration.ordinal()}
     * @param feature the feature to add
     */
    void fenix$addFeature(int step, Holder<PlacedFeature> feature);
}
