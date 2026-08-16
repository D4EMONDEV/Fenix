package fr.d4emon.fenix.example.item;

import fr.d4emon.fenix.example.registry.ModContent;

import fr.d4emon.fenix.registry.attachment.Attachments;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;

/**
 * A hammer that counts its own swings, sparks, and leaves a glimmer behind.
 *
 * <p>Three things at once on purpose: a data component holding state on the
 * stack, a particle sent to whoever can see it, and a status effect applied to
 * the player. Each is one line; what is worth reading is where each one runs.
 */
public final class RubyHammer extends Item {

    /** Every fifth swing is the one that glimmers. */
    private static final int SWINGS_PER_GLIMMER = 5;

    /**
     * @param properties the properties, already carrying the item's id
     */
    public RubyHammer(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Player player = context.getPlayer();
        if (!(context.getLevel() instanceof ServerLevel level) || player == null) {
            // The client is told what happened; deciding it here as well would
            // count every swing twice and spark for nobody else.
            return InteractionResult.SUCCESS;
        }

        ItemStack hammer = context.getItemInHand();
        int swings = hammer.getOrDefault(ModContent.SWINGS.get(), 0) + 1;
        hammer.set(ModContent.SWINGS.get(), swings);

        // On the stack above; on the player here. This one outlives the hammer,
        // and because it is persistent it survives logging out — set once, read
        // back next session.
        int total = Attachments.get(player, ModContent.TOTAL_SWINGS) + 1;
        Attachments.set(player, ModContent.TOTAL_SWINGS, total);

        // And say so, every time. The advancement holding the number decides
        // whether this one counts; guessing the threshold here would put the
        // same number in two places and let them drift apart.
        if (player instanceof ServerPlayer server) {
            ModContent.SWINGS_TRIGGER.fire(server, total);
        }

        // sendParticles rather than addParticle: the server has no particles of
        // its own, it tells the clients that can see the spot.
        var pos = context.getClickedPos().above();
        level.sendParticles(ModContent.RUBY_SPARK.get(),
                pos.getX() + 0.5, pos.getY() + 0.2, pos.getZ() + 0.5,
                8, 0.25, 0.1, 0.25, 0.02);

        if (swings % SWINGS_PER_GLIMMER == 0) {
            // wrapAsHolder because MobEffectInstance wants vanilla's Holder,
            // not Fenix's: the two are different types with the same name.
            player.addEffect(new MobEffectInstance(
                    BuiltInRegistries.MOB_EFFECT.wrapAsHolder(ModContent.RUBY_GLIMMER.get()),
                    20 * 10));
            player.sendSystemMessage(
                    Component.translatable("message.example-mod.glimmer", swings, total));
        }

        hammer.hurtAndBreak(1, player, context.getHand());
        return InteractionResult.SUCCESS;
    }
}
