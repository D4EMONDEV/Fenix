package fr.d4emon.fenix.example.enchantment;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.enchantment.effects.EnchantmentValueEffect;

/**
 * A curve that rises and then falls away.
 *
 * <p>Vanilla offers add, multiply and a few straight lines. This is the shape
 * they cannot make: strongest in the middle of the level range and weaker at
 * either end, so the best level is not simply the highest one.
 *
 * @param peak     the level the curve is strongest at
 * @param strength how much it adds at that level
 */
public record RubyRisingValue(float peak, float strength) implements EnchantmentValueEffect {

    /** What an enchantment file may configure. */
    public static final MapCodec<RubyRisingValue> CODEC = RecordCodecBuilder.mapCodec(
            instance -> instance.group(
                    Codec.FLOAT.fieldOf("peak").forGetter(RubyRisingValue::peak),
                    Codec.FLOAT.fieldOf("strength").forGetter(RubyRisingValue::strength)
            ).apply(instance, RubyRisingValue::new));

    @Override
    public float process(int level, RandomSource random, float value) {
        // A triangle around the peak, never below zero. Deliberately simple:
        // the point is that the shape is the mod's, not that it is clever.
        float distance = Math.abs(level - peak);
        float share = Math.max(0f, 1f - distance / Math.max(1f, peak));
        return value + strength * share;
    }

    @Override
    public MapCodec<? extends EnchantmentValueEffect> codec() {
        return CODEC;
    }
}
