package fr.d4emon.fenix.probe;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.sensing.Sensor;

import java.util.Set;

/**
 * A sensor that senses nothing, so that a sensor type can be registered.
 *
 * <p>What is being checked is the registration, not the sensing: the type's
 * constructor is private in the game and reaching it proves the widening
 * works. A sensor with real behaviour would prove the same thing and take
 * longer to be wrong in.
 */
public final class ProbeSensor extends Sensor<LivingEntity> {

    /** Built by the sensor type, once per mob that has one. */
    public ProbeSensor() {
    }

    @Override
    protected void doTick(ServerLevel level, LivingEntity entity) {
        entity.getBrain().setMemory(ProbeContent.PROBE_MEMORY.get(), 1);
    }

    @Override
    public Set<net.minecraft.world.entity.ai.memory.MemoryModuleType<?>> requires() {
        return Set.of(ProbeContent.PROBE_MEMORY.get());
    }
}
