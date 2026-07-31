package fr.d4emon.fenix.mixin.registry;

import com.mojang.brigadier.arguments.ArgumentType;
import net.minecraft.commands.synchronization.ArgumentTypeInfo;
import net.minecraft.commands.synchronization.ArgumentTypeInfos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.Map;

/**
 * Reaches the table that says how an argument type travels to a client.
 *
 * <p>Registering a custom argument in the registry is only half of it. The other
 * half is this map, keyed by the Brigadier class rather than by id, and vanilla
 * fills it from a private method during bootstrap. Its own
 * {@code ArgumentTypeInfos.byClass} throws
 * {@code Unrecognized argument type} when a class is missing — and that call
 * happens while the command tree is being written for a joining player, so a mod
 * that skipped it works alone and fails the moment anyone connects, with a
 * message naming a Brigadier class and nothing about a mod.
 */
@Mixin(ArgumentTypeInfos.class)
public interface ArgumentTypeInfosAccessor {

    /** {@return the live class-to-info map, which is how the tree is serialised} */
    @Accessor("BY_CLASS")
    static Map<Class<?>, ArgumentTypeInfo<?, ?>> fenix$byClass() {
        throw new AssertionError("mixin did not apply");
    }
}
