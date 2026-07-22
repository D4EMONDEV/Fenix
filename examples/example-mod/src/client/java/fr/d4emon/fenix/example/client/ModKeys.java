package fr.d4emon.fenix.example.client;

import com.mojang.blaze3d.platform.InputConstants;
import fr.d4emon.fenix.event.client.ClientEvents;
import fr.d4emon.fenix.example.content.ModContent;
import fr.d4emon.fenix.registry.client.KeyBindings;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.phys.AABB;

/**
 * The mod's key bindings.
 *
 * <p>Client-only by nature: a key exists on a keyboard, and a dedicated server
 * has none. Everything here lives in {@code src/client}, which is what keeps
 * the common half from naming it by accident.
 */
public final class ModKeys {

    /** A category of the mod's own, worth it only once there are several keys. */
    private static final KeyMapping.Category CATEGORY =
            KeyBindings.category(Identifier.parse("example-mod:example_mod"));

    /** Counts the wisps within sixteen blocks. */
    public static final KeyMapping COUNT_WISPS = KeyBindings.register(
            Identifier.parse("example-mod:count_wisps"), InputConstants.KEY_K, CATEGORY);

    private ModKeys() {
    }

    /** Starts listening. Called from the client mod class. */
    static void listen() {
        ClientEvents.TICK_END.register(tick -> {
            // A loop, not an `if`: a key pressed twice between two ticks
            // reports twice, and asking once would drop the second press.
            while (COUNT_WISPS.consumeClick()) {
                count(tick.client());
            }
        });
    }

    private static void count(Minecraft client) {
        if (client.player == null || client.level == null) {
            return;
        }
        AABB nearby = client.player.getBoundingBox().inflate(16);
        long wisps = client.level.getEntities(client.player, nearby, entity ->
                entity.getType() == ModContent.RUBY_WISP.get()).size();

        client.player.sendSystemMessage(Component.literal("Wisps within 16 blocks: " + wisps));
    }
}
