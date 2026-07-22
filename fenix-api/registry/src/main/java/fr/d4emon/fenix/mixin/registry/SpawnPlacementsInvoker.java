package fr.d4emon.fenix.mixin.registry;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.SpawnPlacementType;
import net.minecraft.world.entity.SpawnPlacements;
import net.minecraft.world.level.levelgen.Heightmap;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

/**
 * Reaches vanilla's spawn placement table, whose {@code register} is private.
 *
 * <p>Without an entry an entity can be summoned and spawned by an egg, and
 * never appears on its own — which looks like a broken spawn weight rather than
 * a missing registration, and is the sort of thing a mod author debugs for an
 * evening.
 */
@Mixin(SpawnPlacements.class)
public interface SpawnPlacementsInvoker {

    /**
     * Records where and when an entity may spawn.
     *
     * @param <T>           the mob class
     * @param type          the entity type
     * @param placementType what it needs underfoot — ground, water, no
     *                      restrictions
     * @param heightmap     which surface the spawn position is measured from
     * @param predicate     the final say: light, difficulty, whatever the mob
     *                      cares about
     */
    @Invoker("register")
    static <T extends Mob> void fenix$register(EntityType<T> type, SpawnPlacementType placementType,
                                               Heightmap.Types heightmap,
                                               SpawnPlacements.SpawnPredicate<T> predicate) {
        throw new AssertionError("replaced by Mixin");
    }
}
