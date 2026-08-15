package fr.d4emon.fenix.example.command;

import fr.d4emon.fenix.example.registry.ModBlocks;
import fr.d4emon.fenix.example.registry.ModContent;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import fr.d4emon.fenix.registry.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.Block;

import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * A command argument naming one of this mod's ores.
 *
 * <p>Written as an argument type rather than a literal for the reason argument
 * types exist: it completes as you type, and the error when you get it wrong
 * names what was expected instead of failing the whole command silently.
 *
 * <p>Registering it takes two halves — see {@code ModContent.ORE_ARGUMENT}.
 */
public final class OreArgument implements ArgumentType<Holder<Block>> {

    private static final SimpleCommandExceptionType UNKNOWN =
            new SimpleCommandExceptionType(Component.literal("Unknown ruby ore"));

    /** The ores by the name a player types. */
    private static Map<String, Holder<Block>> ores() {
        return Map.of(
                "stone", ModBlocks.RUBY_ORE,
                "deepslate", ModBlocks.DEEPSLATE_RUBY_ORE);
    }

    /** Built by the argument info at registration, and by a command using it. */
    public static OreArgument ore() {
        return new OreArgument();
    }

    /**
     * {@return the ore a parsed argument named}
     *
     * @param context the command being run
     * @param name    the argument's name
     */
    @SuppressWarnings("unchecked")
    public static Holder<Block> getOre(CommandContext<?> context, String name) {
        return context.getArgument(name, Holder.class);
    }

    @Override
    public Holder<Block> parse(StringReader reader) throws CommandSyntaxException {
        String name = reader.readUnquotedString().toLowerCase(Locale.ROOT);
        Holder<Block> ore = ores().get(name);
        if (ore == null) {
            throw UNKNOWN.createWithContext(reader);
        }
        return ore;
    }

    @Override
    public <S> CompletableFuture<Suggestions> listSuggestions(CommandContext<S> context,
                                                              SuggestionsBuilder builder) {
        String typed = builder.getRemaining().toLowerCase(Locale.ROOT);
        ores().keySet().stream()
                .filter(name -> name.startsWith(typed))
                .sorted()
                .forEach(builder::suggest);
        return builder.buildFuture();
    }

    @Override
    public Collection<String> getExamples() {
        return List.of("stone", "deepslate");
    }
}
