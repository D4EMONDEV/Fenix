package fr.d4emon.fenix.example.advancement;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.advancements.predicates.ContextAwarePredicate;
import net.minecraft.advancements.triggers.SimpleCriterionTrigger;
import net.minecraft.server.level.ServerPlayer;

import java.util.Optional;

/**
 * Fires when a player has swung the ruby hammer often enough.
 *
 * <p>Vanilla ships around eighty triggers and every one of them describes
 * something vanilla knows: an item picked up, a block placed, a mob killed.
 * None of them can see a number this mod keeps on the player itself, so an
 * advancement about that number cannot be written without a trigger of the
 * mod's own. That is the whole reason this class exists.
 *
 * <p>Two halves, and both are needed. The class is registered so advancements
 * may name it, and {@link #fire} is called from the code that changes the
 * number. A trigger that is registered and never fired is an advancement
 * nobody can earn, which looks from the game like conditions that are too
 * hard rather than like a mod that forgot to say when.
 */
public final class SwingsTrigger extends SimpleCriterionTrigger<SwingsTrigger.Instance> {

    /** Built once, in {@link ModTriggers}. */
    public SwingsTrigger() {
    }

    /**
     * What an advancement writes under {@code conditions}.
     *
     * @param player  the player predicate every criterion may carry
     * @param atLeast how many swings are needed
     */
    public record Instance(Optional<ContextAwarePredicate> player, int atLeast)
            implements SimpleCriterionTrigger.SimpleInstance {

        /** How those conditions are read back out of the advancement file. */
        public static final Codec<Instance> CODEC = RecordCodecBuilder.create(
                instance -> instance.group(
                        ContextAwarePredicate.CODEC.optionalFieldOf("player")
                                .forGetter(Instance::player),
                        Codec.INT.fieldOf("at_least").forGetter(Instance::atLeast)
                ).apply(instance, Instance::new));
    }

    @Override
    public Codec<Instance> codec() {
        return Instance.CODEC;
    }

    /**
     * Tells the advancement system this player is now at {@code swings}.
     *
     * <p>The predicate is asked once per advancement still listening, so this
     * is cheap to call on every swing rather than only on the round numbers.
     *
     * @param player who swung
     * @param swings their running total
     */
    public void fire(ServerPlayer player, int swings) {
        trigger(player, instance -> swings >= instance.atLeast());
    }
}
