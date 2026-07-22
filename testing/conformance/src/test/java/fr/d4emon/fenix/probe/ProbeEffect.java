package fr.d4emon.fenix.probe;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;

/** A status effect that does nothing, so that registering one is exercised. */
public final class ProbeEffect extends MobEffect {

    /** Beneficial and pale gold, which is as much as a probe needs to decide. */
    public ProbeEffect() {
        super(MobEffectCategory.BENEFICIAL, 0xE0C060);
    }
}
