package fr.d4emon.fenix.mixin.network;

import fr.d4emon.fenix.network.Envelope;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

import java.util.ArrayList;
import java.util.List;

/**
 * Puts Fenix's two envelope types into vanilla's payload table.
 *
 * <p>Vanilla builds that table eagerly, once, from the list handed to this
 * method. Adding to the list on the way in is the whole trick, and it works
 * precisely because the two types being added are constants: this runs when
 * the class is transformed, which is always before whatever static initialiser
 * calls the method. There is no window in which a registration could be late.
 *
 * <p>Everything a mod sends travels inside one of those two, so this is the
 * only injection Fenix needs on the encoding side.
 */
@Mixin(CustomPacketPayload.class)
public interface CustomPayloadCodecMixin {

    @ModifyVariable(method = "codec(Lnet/minecraft/network/protocol/common/custom/CustomPacketPayload$FallbackProvider;Ljava/util/List;)Lnet/minecraft/network/codec/StreamCodec;",
            at = @At("HEAD"), argsOnly = true)
    private static List<CustomPacketPayload.TypeAndCodec<? super FriendlyByteBuf, ?>> fenix$addEnvelopes(
            List<CustomPacketPayload.TypeAndCodec<? super FriendlyByteBuf, ?>> types) {
        // Copied rather than added to: vanilla passes a List.of in one place and
        // a mutable list in another, and which is which is not our business.
        List<CustomPacketPayload.TypeAndCodec<? super FriendlyByteBuf, ?>> all = new ArrayList<>(types);
        all.add(new CustomPacketPayload.TypeAndCodec<>(Envelope.TO_SERVER, Envelope.TO_SERVER_CODEC));
        all.add(new CustomPacketPayload.TypeAndCodec<>(Envelope.TO_CLIENT, Envelope.TO_CLIENT_CODEC));
        return all;
    }
}
