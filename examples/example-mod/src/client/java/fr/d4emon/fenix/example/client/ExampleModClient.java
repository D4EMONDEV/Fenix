package fr.d4emon.fenix.example.client;

import fr.d4emon.fenix.example.client.render.ModEntityModels;
import fr.d4emon.fenix.example.client.render.RubySpriteRenderer;
import fr.d4emon.fenix.example.client.screen.RubyReforgingScreen;
import fr.d4emon.fenix.example.client.screen.RubySafeScreen;

import fr.d4emon.fenix.api.Fenix;
import fr.d4emon.fenix.api.FenixMod;
import fr.d4emon.fenix.api.Mod;
import fr.d4emon.fenix.event.client.ClientEvents;
import fr.d4emon.fenix.event.client.HudRenderEvents;
import fr.d4emon.fenix.event.client.ItemTooltipEvents;
import fr.d4emon.fenix.registry.attachment.Attachments;
import fr.d4emon.fenix.example.registry.ModContent;
import fr.d4emon.fenix.example.registry.ModItems;
import fr.d4emon.fenix.example.network.ModPayloads;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import fr.d4emon.fenix.example.client.render.RubyTallyRenderer;
import fr.d4emon.fenix.example.registry.ModBlocks;
import fr.d4emon.fenix.event.Flow;
import fr.d4emon.fenix.event.client.ClientBlockEvents;
import fr.d4emon.fenix.registry.client.BlockEntityRendering;
import fr.d4emon.fenix.registry.client.EntityRendering;
import fr.d4emon.fenix.registry.client.FluidRendering;
import fr.d4emon.fenix.registry.client.MenuScreens;
import fr.d4emon.fenix.registry.client.ParticleRendering;
import net.minecraft.client.particle.GlowParticle;
import net.minecraft.client.renderer.entity.ThrownItemRenderer;

/**
 * The client half of the mod.
 *
 * <p>Same {@code @Mod} annotation and same interface as the common half — what
 * makes this one client-only is where it lives. Everything under
 * {@code src/client/java} is indexed apart, and a dedicated server is never
 * told the class exists.
 *
 * <p>This half may use the common half; the reverse is a compile error. That is
 * the right way round: a mod is written in {@code src/main} and reached into
 * from here, mostly to say how it looks.
 */
@Mod("example-mod")
public final class ExampleModClient implements FenixMod {

    /** Instantiated by the loader. */
    public ExampleModClient() {
    }

    @Override
    public void onRegister(Fenix fenix) {
        // Runs after the common half, so the entity type is already bound.
        // Vanilla's item renderer is all a wisp needs — no model file.
        EntityRendering.register(ModContent.RUBY_WISP, ThrownItemRenderer::new);
        // A layer to bake from, then the renderer that bakes it. The order
        // matters: baking a layer nobody declared throws while the client
        // loads.
        ModEntityModels.register();
        EntityRendering.register(ModContent.RUBY_SPRITE, RubySpriteRenderer::new);

        // A particle type with no provider is spawned and never drawn: the
        // lookup finds nothing and the effect silently does not happen.
        // Vanilla's spark provider is all a ruby spark needs.
        ParticleRendering.register(ModContent.RUBY_SPARK, GlowParticle.ElectricSparkProvider::new);

        // A fluid with no rendering shows as the missing-texture checkerboard.
        // This reuses water's own sprites — no new textures to ship — and tints
        // them ruby red, so the brine is water-shaped and unmistakably not water.
        //
        // 0xFF and not six digits: the tint is ARGB and is multiplied into
        // every sprite, so an alpha of zero renders the fluid invisible while
        // everything else about it keeps working.
        FluidRendering.register(ModContent.RUBY_BRINE,
                Identifier.withDefaultNamespace("block/water_still"),
                Identifier.withDefaultNamespace("block/water_flow"),
                null, 0xFFC8203A);

        // Keys are client-only, and registered here rather than beside the
        // content: onRegister runs before the game builds its options, which
        // is exactly when the list has to be complete.
        ModKeys.listen();

        // The other half of a menu: the server opens the window, this says what
        // the player sees when it opens.
        // A block entity that draws something of its own. Every other thing
        // this mod draws goes through a different door; this is the one a
        // block entity uses, and until now the demo never opened it.
        BlockEntityRendering.register(ModContent.RUBY_TALLY, RubyTallyRenderer::new);

        // Client-side block events. They fire on the client's own prediction,
        // before the server is asked, which is the only place a mod can stop an
        // interaction without the block first appearing to break and come back.
        ClientBlockEvents.ATTACK.register(attack -> {
            boolean glowing = attack.player().level().getBlockState(attack.pos())
                    .is(ModBlocks.GLOWING_RUBY_BLOCK.get());
            if (!glowing || attack.player().isCreative()) {
                return Flow.CONTINUE;
            }
            // Above the hotbar rather than in chat: this fires on every swing
            // at the block, and chat would fill with it in a second.
            attack.player().sendOverlayMessage(
                    Component.translatable("message.example-mod.too_bright"));
            return Flow.CANCEL;
        });

        MenuScreens.register(ModContent.RUBY_SAFE_MENU, RubySafeScreen::new);
        MenuScreens.register(ModContent.RUBY_REFORGING_MENU, RubyReforgingScreen::new);

        // A line under the ruby's name, to show a tooltip being built. The list
        // handed over is live: index 0 is the item's own name, so inserting at 1
        // puts this directly beneath it.
        ItemTooltipEvents.BUILD.register(tooltip -> {
            if (tooltip.stack().is(ModItems.RUBY.get())) {
                tooltip.lines().add(1, Component.translatable("tooltip.example-mod.ruby")
                        .withStyle(ChatFormatting.DARK_RED));
            }
        });

        // Per-world client state belongs discarded here, or a mod carries it
        // into the next world and is quietly wrong about it.
        ClientEvents.DISCONNECTED.register(left -> ModKeys.forget());

        // Drawn over vanilla's hotbar and health, once a frame, only while a
        // world is on screen.
        HudRenderEvents.RENDER.register(hud -> {
            Minecraft client = Minecraft.getInstance();
            if (client.player == null || !client.player.getMainHandItem().is(ModItems.RUBY_HAMMER.get())) {
                return;
            }
            int swings = Attachments.get(client.player, ModContent.TOTAL_SWINGS);
            // text, not drawString: 26.2 records what to draw rather than
            // drawing it, and the methods were renamed to match.
            hud.graphics().text(client.font, "Swings: " + swings, 4, 4, 0xFFC8203A);
        });

        // The other half of the tally block. Showing it needs the client, so
        // the handler belongs here rather than beside the channel.
        ModPayloads.TALLY.receive(tally -> {
            var player = Minecraft.getInstance().player;
            if (player != null) {
                player.sendSystemMessage(Component.literal(
                        "Tally at " + tally.pos().toShortString() + ": " + tally.count()));
            }
        });
    }
}
