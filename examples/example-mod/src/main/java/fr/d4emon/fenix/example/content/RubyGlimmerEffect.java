package fr.d4emon.fenix.example.content;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.LivingEntity;

/**
 * Heals a sliver every second, to show a status effect that does something.
 *
 * <p>The two halves of an effect: this class is what it <em>does</em>, and
 * {@link ModContent#RUBY_GLIMMER} is what registers it. An effect also needs an
 * icon at {@code textures/mob_effect/<name>.png} and a translation, or the
 * player's inventory shows a missing texture and a raw key.
 */
public final class RubyGlimmerEffect extends MobEffect {

    /** Beneficial, and ruby-coloured — the colour tints the effect's bar. */
    public RubyGlimmerEffect() {
        super(MobEffectCategory.BENEFICIAL, 0xC43042);
    }

    @Override
    public boolean shouldApplyEffectTickThisTick(int tickCount, int amplification) {
        // Once a second at amplifier 0, twice at 1, and so on. Vanilla's
        // regeneration works the same way; ticking every tick would heal a
        // player faster than anything could hurt them.
        int period = Math.max(20 >> amplification, 1);
        return tickCount % period == 0;
    }

    @Override
    public boolean applyEffectTick(ServerLevel level, LivingEntity entity, int amplification) {
        if (entity.getHealth() < entity.getMaxHealth()) {
            entity.heal(1f);
        }
        // false would end the effect early; it runs for its whole duration.
        return true;
    }
}
