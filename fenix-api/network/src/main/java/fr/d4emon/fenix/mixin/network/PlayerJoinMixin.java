package fr.d4emon.fenix.mixin.network;

import fr.d4emon.fenix.network.RegistryCheck;
import net.minecraft.network.Connection;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.CommonListenerCookie;
import net.minecraft.server.players.PlayerList;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * States what this server has, to every player as they arrive.
 *
 * <p>At the tail, so the player's connection is in the play phase and can carry
 * a payload. Earlier would be tidier and would not work.
 */
@Mixin(PlayerList.class)
public class PlayerJoinMixin {

    /** Never called — a mixin's constructors are discarded when it is merged. */
    PlayerJoinMixin() {
    }

    @Inject(method = "placeNewPlayer", at = @At("TAIL"))
    private void fenix$stateRegistries(Connection connection, ServerPlayer player,
                                       CommonListenerCookie cookie, CallbackInfo info) {
        RegistryCheck.greet(player);
    }
}
