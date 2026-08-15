package fr.d4emon.fenix.example.entity;

import fr.d4emon.fenix.example.registry.ModContent;

import fr.d4emon.fenix.registry.EntityAttributes;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

/**
 * A small creature that wanders, looks at people, and carries a charge.
 *
 * <p>Here to be the thing the wisp could not be. {@code RubyWisp} is a thrown
 * projectile — a plain {@link net.minecraft.world.entity.Entity} — so it has no
 * attributes, no goals and no health, and every part of the API that deals with
 * living things went undemonstrated. This is a {@link PathfinderMob}: it has a
 * brain, a health bar, and a place to hang
 * {@link ModContent#RUBY_CHARGE the mod's own attribute}.
 *
 * <p>Deliberately simple. Four goals is what a passive animal has, and anything
 * more would be a mob tutorial rather than a demonstration of the loader.
 */
public class RubySprite extends PathfinderMob {

    /** Called by the game through the entity type. */
    public RubySprite(EntityType<? extends RubySprite> type, Level level) {
        super(type, level);
    }

    /**
     * {@return what a sprite is worth before any modifier}
     *
     * <p>Handed to {@code Registrar.attributes}, which records it rather than
     * building it: the custom attribute below is still an unbound holder while a
     * mod registers, and reading it here would read it too early.
     */
    public static AttributeSupplier.Builder attributes() {
        return Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 10.0)
                .add(Attributes.MOVEMENT_SPEED, 0.25)
                .add(Attributes.FOLLOW_RANGE, 16.0)
                // The mod's own. An attribute that is registered but never added
                // to an entity is an attribute nothing has, which reads exactly
                // like the attribute not working.
                //
                // EntityAttributes.holder bridges Fenix's holder to the game's,
                // which the builder wants. They cannot be one type: Fenix hands
                // its own back before the attribute exists, so content can be
                // declared in a field.
                .add(EntityAttributes.holder(ModContent.RUBY_CHARGE), 3.0);
    }

    @Override
    protected void registerGoals() {
        // Priority is the order they are asked in, lowest first. Floating comes
        // before anything else because a mob that drowns while deciding where to
        // walk is a mob that looks broken.
        goalSelector.addGoal(0, new FloatGoal(this));
        goalSelector.addGoal(1, new WaterAvoidingRandomStrollGoal(this, 1.0));
        goalSelector.addGoal(2, new LookAtPlayerGoal(this, Player.class, 8.0f));
        goalSelector.addGoal(3, new RandomLookAroundGoal(this));
    }
}
