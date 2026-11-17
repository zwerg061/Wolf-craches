package de.wolfmod.creachreportfix.mixin;

import com.mojang.logging.LogUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.protocol.game.ClientboundResourcePackPacket;
import net.minecraft.network.protocol.game.ServerboundResourcePackPacket;
import org.slf4j.Logger;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Declines server resource packs before the client starts downloading them.
 *
 * <p><b>Inactive by default.</b> This class is intentionally not listed in
 * {@code creachreportfix.mixins.json}: blocking a required server resource pack
 * can get the player kicked. To enable it, add {@code "ClientPacketListenerMixin"}
 * to the {@code client} array of the config.</p>
 *
 * <p>The three injections target the same method under different names because
 * the obfuscated name differs between mappings.</p>
 */
@Mixin(ClientPacketListener.class)
public final class ClientPacketListenerMixin {
    private static final Logger LOGGER = LogUtils.getLogger();

    @Inject(method = "m_104583_", at = @At("HEAD"), cancellable = true, remap = false, require = 0)
    private void creachfix$blockServerResourcePackSrg(ClientboundResourcePackPacket packet, CallbackInfo ci) {
        LOGGER.info("[creachreportfix] Intercepted resource pack packet (srg) - sending DECLINED.");
        creachfix$sendDeclined();
        ci.cancel();
    }

    @Inject(
            method = "handleResourcePack(Lnet/minecraft/network/protocol/game/ClientboundResourcePackPacket;)V",
            at = @At("HEAD"),
            cancellable = true,
            remap = false,
            require = 0
    )
    private void creachfix$blockServerResourcePackNamed(ClientboundResourcePackPacket packet, CallbackInfo ci) {
        LOGGER.info("[creachreportfix] Intercepted resource pack packet (named) - sending DECLINED.");
        creachfix$sendDeclined();
        ci.cancel();
    }

    private static void creachfix$sendDeclined() {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft == null || minecraft.getConnection() == null) {
            return;
        }

        minecraft.getConnection().send(
                new ServerboundResourcePackPacket(ServerboundResourcePackPacket.Action.DECLINED));
    }
}
