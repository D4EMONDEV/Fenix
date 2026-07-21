package fr.d4emon.fenix.command;

import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.server.permissions.Permission;
import net.minecraft.server.permissions.PermissionCheck;
import net.minecraft.server.permissions.PermissionProviderCheck;
import net.minecraft.server.permissions.Permissions;

/**
 * Commands, without the parts of Brigadier nobody enjoys.
 *
 * <pre>{@code
 * CommandEvents.REGISTER.register(registration -> registration.dispatcher().register(
 *         literal("home")
 *                 .requires(Commands.operator())
 *                 .then(argument("name", StringArgumentType.word())
 *                         .executes(run(context -> teleport(context.getSource().getPlayerOrException(),
 *                                 StringArgumentType.getString(context, "name")))))));
 * }</pre>
 *
 * <p>Fenix's own, not vanilla's: use this and you never need the other. It is a
 * shortcut over Brigadier, never a wall in front of it — every builder here is
 * Brigadier's own, so anything not covered is still reachable.
 *
 * <p>What it removes is the {@code return 1}. Brigadier's {@code executes} wants
 * an int nobody reads, and forgetting it is a compile error whose message says
 * nothing about commands. {@link #run} takes a body that returns nothing.
 */
public final class Commands {

    private Commands() {
    }

    /**
     * Starts a literal, like the {@code home} in {@code /home}.
     *
     * @param name the word a player types
     * @return Brigadier's builder
     */
    public static LiteralArgumentBuilder<CommandSourceStack> literal(String name) {
        return net.minecraft.commands.Commands.literal(name);
    }

    /**
     * Starts an argument.
     *
     * @param <T>  what it parses to
     * @param name what to call it when reading it back out
     * @param type how to parse it
     * @return Brigadier's builder
     */
    public static <T> RequiredArgumentBuilder<CommandSourceStack, T> argument(
            String name, ArgumentType<T> type) {
        return net.minecraft.commands.Commands.argument(name, type);
    }

    /**
     * Wraps a command body that returns nothing.
     *
     * @param body what the command does
     * @return what {@code executes} wants
     */
    public static com.mojang.brigadier.Command<CommandSourceStack> run(Body body) {
        return context -> {
            body.run(context);
            // Brigadier reads this as "how many things did you affect", and
            // nothing in vanilla does anything with it but check it is not zero.
            return com.mojang.brigadier.Command.SINGLE_SUCCESS;
        };
    }

    /**
     * {@return a requirement that the caller may run operator commands}
     *
     * <p>The same permission {@code /gamemode} asks for. Minecraft 26.2 replaced
     * the old numeric levels with named permissions, so this is a name rather
     * than the {@code hasPermission(2)} that appears in every older mod and
     * means nothing on sight.
     */
    public static PermissionProviderCheck<CommandSourceStack> operator() {
        return requires(Permissions.COMMANDS_GAMEMASTER);
    }

    /**
     * {@return a requirement that the caller holds a permission}
     *
     * @param permission one of vanilla's, or one a server's permission plugin
     *                   provides
     */
    public static PermissionProviderCheck<CommandSourceStack> requires(Permission permission) {
        return net.minecraft.commands.Commands.hasPermission(
                new PermissionCheck.Require(permission));
    }

    /**
     * A command body.
     *
     * <p>Allowed to throw {@code CommandSyntaxException} because the methods
     * that read arguments back out do, and because Brigadier catches it and
     * shows the player what went wrong. Wrapping every one of those calls in a
     * try/catch would only turn a good message into a crash.
     */
    @FunctionalInterface
    public interface Body {

        /**
         * Runs the command.
         *
         * @param context what was typed
         * @throws CommandSyntaxException if an argument cannot be read
         */
        void run(CommandContext<CommandSourceStack> context) throws CommandSyntaxException;
    }
}
