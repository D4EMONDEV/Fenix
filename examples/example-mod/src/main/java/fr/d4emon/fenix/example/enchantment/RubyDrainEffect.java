package fr.d4emon.fenix.example.enchantment;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.enchantment.EnchantedItemInUse;
import net.minecraft.world.item.enchantment.effects.EnchantmentEntityEffect;
import net.minecraft.world.phys.Vec3;

import fr.d4emon.fenix.example.registry.ModContent;

/**
 * An enchantment effect of the mod's own: it gives what it takes.
 *
 * <p>Vanilla ships effects for damage, ignition, status effects and a handful
 * more, and an enchantment file composes them. What it cannot compose is an
 * effect that reads the mod's own state and acts on it — so this drains the
 * target and hands the glimmer to whoever swung.
 *
 * <p>The registered thing is the {@link #CODEC}, not this class. An enchantment
 * file naming {@code example-mod:ruby_drain} is asking for the codec, which
 * builds one of these with the fields the file supplied.
 *
 * @param seconds how long the glimmer lasts, before the enchantment's level
 */
public record RubyDrainEffect(int seconds) implements EnchantmentEntityEffect {

    /** What an enchantment file may configure. */
    public static final MapCodec<RubyDrainEffect> CODEC = RecordCodecBuilder.mapCodec(
            instance -> instance.group(
                    com.mojang.serialization.Codec.INT.fieldOf("seconds")
                            .forGetter(RubyDrainEffect::seconds)
            ).apply(instance, RubyDrainEffect::new));

    @Override
    public void apply(ServerLevel level, int enchantLevel, EnchantedItemInUse item,
                      Entity target, Vec3 at) {
        // The enchantment's level multiplies the duration the file asked for,
        // which is the usual shape: the file says how strong one level is and
        // the game says how many levels there are.
        if (item.owner() instanceof LivingEntity owner) {
            owner.addEffect(new MobEffectInstance(
                    BuiltInRegistries.MOB_EFFECT.wrapAsHolder(ModContent.RUBY_GLIMMER.get()),
                    20 * seconds * enchantLevel));
        }
        if (target instanceof LivingEntity living) {
            // Taken from the target, so the effect is a transfer rather than a
            // gift — an enchantment that only gives is a buff with extra steps.
            living.removeEffect(
                    BuiltInRegistries.MOB_EFFECT.wrapAsHolder(ModContent.RUBY_GLIMMER.get()));
        }
    }

    @Override
    public MapCodec<RubyDrainEffect> codec() {
        return CODEC;
    }
}
