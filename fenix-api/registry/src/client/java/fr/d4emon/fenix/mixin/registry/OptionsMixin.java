package fr.d4emon.fenix.mixin.registry;

import fr.d4emon.fenix.registry.client.KeyBindings;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Options;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

/**
 * Adds the mod's key bindings to the game's own list.
 *
 * <p>Vanilla builds that list once, from a field initialiser naming its own
 * mappings one by one, and nothing reads it again. A mapping missing from it
 * never appears in the controls screen and is never loaded from or saved to
 * {@code options.txt} — so it works until the player restarts, and then quietly
 * goes back to its default.
 *
 * <p>Appended at the constructor's return, which is after the initialiser has
 * run and before anything reads the array. Mods have long since registered:
 * {@code onRegister} fires during bootstrap, and {@code Options} is built later
 * in Minecraft's constructor.
 */
@Mixin(Options.class)
public abstract class OptionsMixin {

    /**
     * Vanilla's list.
     *
     * <p>{@code final}, and an array rather than a list, so extending it means
     * replacing it — which is what {@code @Mutable} is for.
     */
    @Mutable
    @Shadow
    @Final
    public KeyMapping[] keyMappings;

    @Inject(method = "<init>", at = @At("RETURN"))
    private void fenix$addModKeyBindings(CallbackInfo info) {
        List<KeyMapping> registered = KeyBindings.fenix$registered();
        if (registered.isEmpty()) {
            return;
        }

        KeyMapping[] extended = new KeyMapping[keyMappings.length + registered.size()];
        System.arraycopy(keyMappings, 0, extended, 0, keyMappings.length);
        for (int i = 0; i < registered.size(); i++) {
            extended[keyMappings.length + i] = registered.get(i);
        }
        keyMappings = extended;
    }
}
