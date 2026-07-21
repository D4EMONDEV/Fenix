package fr.d4emon.fenix.probe;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/**
 * A living entity, for the conformance check.
 *
 * <p>Living is the point: it is the constructor of {@code LivingEntity} that
 * asks vanilla for the default attributes, so only something that lives can
 * prove they were registered.
 */
public final class ProbeCritter extends Animal {

    /**
     * @param type  its registered type
     * @param level the level it is in
     */
    public ProbeCritter(EntityType<? extends Animal> type, Level level) {
        super(type, level);
    }

    @Override
    public boolean isFood(ItemStack stack) {
        return false;
    }

    @Override
    public AgeableMob getBreedOffspring(ServerLevel level, AgeableMob partner) {
        return null;
    }
}
