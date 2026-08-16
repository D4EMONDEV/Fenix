package fr.d4emon.fenix.probe;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.advancements.predicates.ContextAwarePredicate;
import net.minecraft.advancements.triggers.SimpleCriterionTrigger;
import net.minecraft.server.level.ServerPlayer;

import java.util.Optional;

/**
 * An advancement trigger of the mod's own.
 *
 * <p>Vanilla ships around eighty and they describe what vanilla does. This one
 * describes something only this mod knows about — how many of its blocks a
 * player has broken — which is the case a mod cannot express any other way.
 */
public final class ProbeTrigger extends SimpleCriterionTrigger<ProbeTrigger.Instance> {

    /** Built once and registered. */
    public ProbeTrigger() {
    }

    /**
     * What an advancement writes to say when this counts.
     *
     * @param player  the optional player predicate every criterion may carry
     * @param atLeast how many are needed
     */
    public record Instance(Optional<ContextAwarePredicate> player, int atLeast)
            implements SimpleCriterionTrigger.SimpleInstance {

        /** How the criterion's conditions are read from an advancement. */
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
     * Says it happened, for one player.
     *
     * <p>The registered trigger is only half of it: one nothing ever fires is
     * an advancement nobody can earn, which reads as conditions that are too
     * hard rather than as a mod that forgot to call this.
     *
     * @param player who did it
     * @param count  how many they have broken now
     */
    public void fire(ServerPlayer player, int count) {
        trigger(player, instance -> count >= instance.atLeast());
    }
}
