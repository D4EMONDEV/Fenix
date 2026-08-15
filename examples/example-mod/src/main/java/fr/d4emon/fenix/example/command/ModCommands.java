package fr.d4emon.fenix.example.command;

import fr.d4emon.fenix.example.registry.ModContent;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import fr.d4emon.fenix.command.CommandEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;

import static fr.d4emon.fenix.command.Commands.argument;
import static fr.d4emon.fenix.command.Commands.literal;
import static fr.d4emon.fenix.command.Commands.operator;
import static fr.d4emon.fenix.command.Commands.run;

/** The mod's commands. */
public final class ModCommands {

    private ModCommands() {
    }

    /** Adds them. Called from {@code onInit}. */
    public static void register() {
        CommandEvents.REGISTER.register(registration -> registration.dispatcher().register(
                literal("wisp")
                        .requires(operator())
                        .then(argument("count", IntegerArgumentType.integer(1, 20))
                                .executes(run(context -> {
                                    int count = IntegerArgumentType.getInteger(context, "count");
                                    spawn(context.getSource(), count);
                                })))
                        .executes(run(context -> spawn(context.getSource(), 1)))));

        // A command taking an argument type of the mod's own: it completes as
        // you type and refuses an unknown name by itself. That it works for a
        // player who *joined* rather than only in single player is the half
        // Registrar.commandArgument exists for.
        CommandEvents.REGISTER.register(registration -> registration.dispatcher().register(
                literal("rubyore")
                        .requires(operator())
                        .then(argument("which", OreArgument.ore())
                                .executes(run(context -> {
                                    var ore = OreArgument.getOre(context, "which");
                                    context.getSource().sendSuccess(
                                            () -> Component.literal("That ore is " + ore.id()), false);
                                })))));
    }

    private static void spawn(net.minecraft.commands.CommandSourceStack source, int count) {
        BlockPos pos = BlockPos.containing(source.getPosition());
        for (int i = 0; i < count; i++) {
            ModContent.RUBY_WISP.get().spawn(source.getLevel(), pos, null);
        }
        // A supplier, not a component: vanilla only builds the message if
        // anybody is actually going to read it.
        source.sendSuccess(() -> Component.literal("Spawned " + count + " wisp(s)"), true);
    }
}
