package fr.d4emon.fenix.example.content;

import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.projectile.ItemSupplier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

/**
 * A drifting mote that looks like a ruby.
 *
 * <p>Deliberately not alive: a {@code LivingEntity} would need default
 * attributes and a model, and the point here is the smallest thing that shows
 * an entity registered, saved and drawn. Summon it with
 * {@code /summon example-mod:ruby_wisp}.
 *
 * <p>It supplies an {@link ItemStack} so vanilla's own item renderer can draw
 * it, which is what saves this example from needing a model file.
 */
public final class RubyWisp extends Entity implements ItemSupplier {

    /**
     * @param type  its registered type
     * @param level the level it is in
     */
    public RubyWisp(EntityType<? extends RubyWisp> type, Level level) {
        super(type, level);
    }

    @Override
    public ItemStack getItem() {
        return new ItemStack(ModItems.RUBY.get());
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        // Nothing to keep in step between server and client: what it looks like
        // never changes.
    }

    @Override
    public boolean hurtServer(ServerLevel level, DamageSource source, float amount) {
        return false;
    }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput output) {
    }
}
