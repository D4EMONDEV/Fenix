package fr.d4emon.fenix.probe;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.exceptions.CommandSyntaxException;

import java.util.Collection;
import java.util.List;

/**
 * A command argument of the mod's own, for the conformance check.
 *
 * <p>Deliberately trivial: what is being checked is not what it parses but
 * whether the game can describe it to a joining client.
 */
public final class ProbeArgument implements ArgumentType<String> {

    /** Built by the argument info, and by a command that uses it. */
    public static ProbeArgument ore() {
        return new ProbeArgument();
    }

    @Override
    public String parse(StringReader reader) throws CommandSyntaxException {
        return reader.readUnquotedString();
    }

    @Override
    public Collection<String> getExamples() {
        return List.of("ruby", "deepslate_ruby");
    }
}
