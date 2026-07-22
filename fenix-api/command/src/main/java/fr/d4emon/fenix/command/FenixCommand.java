package fr.d4emon.fenix.command;

import com.mojang.brigadier.CommandDispatcher;
import fr.d4emon.fenix.api.ModInfo;
import fr.d4emon.fenix.loader.launch.FenixHooks;
import fr.d4emon.fenix.loader.launch.FenixVersion;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;

import java.util.List;

import static fr.d4emon.fenix.command.Commands.argument;
import static fr.d4emon.fenix.command.Commands.literal;
import static fr.d4emon.fenix.command.Commands.run;

/**
 * {@code /fenix}, which answers the first question asked of any broken world.
 *
 * <p>Nothing in the game knows what Fenix loaded. A player reporting a problem
 * is asked which mods they have and has to go and read a folder; a server
 * administrator has to trust that the folder matches what started. Both are
 * answerable in one line, and neither was.
 *
 * <p>Open to everyone rather than to operators. The list is not a secret — the
 * server already sends every mod's namespace to every client that joins, so
 * withholding it would only inconvenience the person asking.
 */
public final class FenixCommand {

    private FenixCommand() {
    }

    /**
     * Adds the command to a dispatcher.
     *
     * <p>Called by Fenix itself when the command tree opens, not by a mod.
     *
     * @param dispatcher the tree being built
     */
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(literal("fenix")
                .executes(run(context -> summary(context.getSource())))
                .then(literal("mods")
                        .executes(run(context -> list(context.getSource())))
                        .then(argument("id", com.mojang.brigadier.arguments.StringArgumentType.string())
                                .executes(run(context -> detail(context.getSource(),
                                        com.mojang.brigadier.arguments.StringArgumentType
                                                .getString(context, "id")))))));
    }

    private static void summary(CommandSourceStack source) {
        List<ModInfo> mods = FenixHooks.loadedMods();
        source.sendSuccess(() -> Component.literal("Fenix " + FenixVersion.current())
                .withStyle(ChatFormatting.GOLD)
                .append(Component.literal(" — " + mods.size()
                                + (mods.size() == 1 ? " mod loaded" : " mods loaded"))
                        .withStyle(ChatFormatting.GRAY)), false);
    }

    private static void list(CommandSourceStack source) {
        List<ModInfo> mods = FenixHooks.loadedMods();
        if (mods.isEmpty()) {
            source.sendSuccess(() -> Component.literal("No mods are loaded.")
                    .withStyle(ChatFormatting.GRAY), false);
            return;
        }

        summary(source);
        // In load order, which is the order that matters when two mods disagree
        // and somebody is trying to work out which one went first.
        for (ModInfo mod : mods) {
            source.sendSuccess(() -> Component.literal("  " + mod.id())
                    .withStyle(ChatFormatting.WHITE)
                    .append(Component.literal(" " + mod.version())
                            .withStyle(ChatFormatting.GRAY)), false);
        }
    }

    private static void detail(CommandSourceStack source, String id) {
        ModInfo mod = FenixHooks.loadedMods().stream()
                .filter(candidate -> candidate.id().equals(id))
                .findFirst()
                .orElse(null);

        if (mod == null) {
            source.sendFailure(Component.literal("No mod with id '" + id + "' is loaded."));
            return;
        }

        source.sendSuccess(() -> Component.literal(mod.name() + " " + mod.version())
                .withStyle(ChatFormatting.GOLD), false);
        line(source, "id", mod.id());
        if (!mod.description().isBlank()) {
            line(source, "description", mod.description());
        }
        if (!mod.authors().isEmpty()) {
            line(source, "authors", String.join(", ", mod.authors()));
        }
        if (!mod.license().isBlank()) {
            line(source, "license", mod.license());
        }
    }

    private static void line(CommandSourceStack source, String label, String value) {
        source.sendSuccess(() -> Component.literal("  " + label + ": ")
                .withStyle(ChatFormatting.GRAY)
                .append(Component.literal(value).withStyle(ChatFormatting.WHITE)), false);
    }
}
