package fr.d4emon.fenix.command;

import com.mojang.brigadier.CommandDispatcher;
import fr.d4emon.fenix.event.Event;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;

/**
 * When to add commands.
 *
 * <pre>{@code
 * CommandEvents.REGISTER.register(registration ->
 *         registration.dispatcher().register(literal("hello").executes(run(context -> {
 *             context.getSource().sendSuccess(() -> Component.literal("hi"), false);
 *         }))));
 * }</pre>
 *
 * <p>Fired once per command tree, which the server builds on start and rebuilds
 * whenever datapacks reload — so a listener runs more than once per session and
 * must add rather than accumulate.
 */
public final class CommandEvents {

    /**
     * The command tree is open for additions.
     *
     * <p>Server-side by definition: commands are the server's, and a client
     * only ever sees the tree it is sent.
     */
    public static final Event<Registration> REGISTER = Event.create();

    private CommandEvents() {
    }

    /**
     * What a listener is given.
     *
     * @param dispatcher where commands are added
     * @param context    registry access, for argument types that need it
     * @param selection  which kind of server this is; a command that only makes
     *                   sense on a dedicated server can check rather than guess
     */
    public record Registration(CommandDispatcher<CommandSourceStack> dispatcher,
                               CommandBuildContext context,
                               net.minecraft.commands.Commands.CommandSelection selection) {
    }
}
