package fr.d4emon.fenix.mixin.command;

import fr.d4emon.fenix.command.CommandEvents;
import fr.d4emon.fenix.command.FenixCommand;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.Commands;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Opens the command tree to mods, once vanilla has filled it.
 *
 * <p>At the tail of the constructor, so a mod's command can override a vanilla
 * one by name — which is the only order that lets a mod do that at all.
 */
@Mixin(Commands.class)
public abstract class CommandsMixin {

    /** Never called — a mixin's constructors are discarded when it is merged. */
    CommandsMixin() {
    }

    @Inject(method = "<init>", at = @At("TAIL"))
    private void fenix$openTheTree(Commands.CommandSelection selection,
                                   CommandBuildContext context, CallbackInfo info) {
        // Fenix's own command first, so a mod that wants to replace /fenix can.
        // No API module has an entrypoint to register it from, and giving one
        // just for this would make every mod wait on it.
        FenixCommand.register(((Commands) (Object) this).getDispatcher());

        CommandEvents.REGISTER.fire(new CommandEvents.Registration(
                ((Commands) (Object) this).getDispatcher(), context, selection));
    }
}
