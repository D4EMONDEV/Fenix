package fr.d4emon.fenix.event.client;

import fr.d4emon.fenix.event.Event;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import org.jspecify.annotations.Nullable;

import java.util.List;

/**
 * What an item says when the pointer rests on it.
 *
 * <pre>{@code
 * ItemTooltipEvents.BUILD.register(tooltip -> {
 *     if (tooltip.stack().is(ModItems.RUBY.get())) {
 *         tooltip.lines().add(Component.translatable("tooltip.mymod.ruby"));
 *     }
 * });
 * }</pre>
 *
 * <p>Client only: a tooltip is drawn, never sent, so there is nothing for a
 * server to have an opinion about.
 */
public final class ItemTooltipEvents {

    /**
     * A tooltip being built.
     *
     * <p>{@code lines} is the live list, already holding the name and whatever
     * vanilla adds. Add to it, or edit it — the first line is the item's name,
     * so a mod appending is the ordinary case and inserting at 1 puts a line
     * directly under the name.
     *
     * @param stack  the stack being described
     * @param player who is looking, or {@code null} on a screen with no player
     * @param flag   whether advanced tooltips are on, which is F3+H
     * @param lines  the tooltip so far, live and writable
     */
    public record Build(ItemStack stack, @Nullable Player player, TooltipFlag flag,
                        List<Component> lines) {
    }

    /** Fires as a tooltip is assembled, before it is drawn. */
    public static final Event<Build> BUILD = Event.create();

    private ItemTooltipEvents() {
    }
}
