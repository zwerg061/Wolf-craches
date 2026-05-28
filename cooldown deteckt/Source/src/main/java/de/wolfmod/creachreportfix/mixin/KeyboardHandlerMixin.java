package de.wolfmod.creachreportfix.mixin;

import net.minecraft.client.KeyboardHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(KeyboardHandler.class)
public abstract class KeyboardHandlerMixin {
    @Inject(method = {"m_90830_", "handleDebugKeys"}, at = @At("HEAD"), cancellable = true, remap = false, require = 0)
    private void creachreportfix$blockManualReloadKeys(int key, CallbackInfoReturnable<Boolean> cir) {
        if (key != 84 && key != 83) {
            return;
        }

        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player != null) {
            minecraft.player.displayClientMessage(Component.literal("[CreachFix] F3+T/F3+S ist blockiert (Crash-Schutz)."), true);
        }
        cir.setReturnValue(true);
    }
}
