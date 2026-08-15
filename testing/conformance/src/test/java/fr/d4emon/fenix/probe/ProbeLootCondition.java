package fr.d4emon.fenix.probe;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;

/**
 * A loot condition of the mod's own, to prove one can be registered.
 *
 * <p>It asks something vanilla has no condition for — whether a number the mod
 * cares about is above a threshold — because a condition that duplicates a
 * vanilla one would prove the registration and not the point of it.
 *
 * @param threshold the number to beat
 */
public record ProbeLootCondition(int threshold) implements LootItemCondition {

    /**
     * How the condition is read from and written to JSON.
     *
     * <p>The same instance is registered and returned from {@link #codec()}:
     * the registry maps an id to a codec, and the class points back at the
     * codec, which is how the game gets from {@code "condition": "…"} to here.
     */
    public static final MapCodec<ProbeLootCondition> CODEC =
            RecordCodecBuilder.mapCodec(instance -> instance.group(
                    Codec.INT.fieldOf("threshold").forGetter(ProbeLootCondition::threshold)
            ).apply(instance, ProbeLootCondition::new));

    @Override
    public MapCodec<? extends LootItemCondition> codec() {
        return CODEC;
    }

    @Override
    public boolean test(LootContext context) {
        // Deterministic, so the check is about registration rather than luck.
        return threshold <= 0;
    }
}
