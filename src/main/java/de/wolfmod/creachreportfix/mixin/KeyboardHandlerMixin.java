package de.wolfmod.creachreportfix.mixin;

import net.minecraft.client.KeyboardHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Blocks the manual resource reload shortcuts (F3+T and F3+S).
 *
 * <p>On this heavily modded client a manual reload while models are still being
 * baked is one of the triggers for the {@code ModelBakery} crash, so the
 * shortcut is swallowed and the player gets a short hint instead.</p>
 *
 * <p>{@code m_90932_} is the SRG name of {@code KeyboardHandler.handleDebugKeys(int)}.</p>
 */
@Mixin(KeyboardHandler.class)
public abstract class KeyboardHandlerMixin {
    private static final int KEY_RELOAD_RESOURCES = 84; // T
    private static final int KEY_RELOAD_RESOURCE_PACKS = 83; // S

    @Inject(method = "m_90932_", at = @At("HEAD"), cancellable = true, remap = false, require = 0)
    private void creachfix$blockManualReloadKeys(int key, CallbackInfoReturnable<Boolean> cir) {
        if (key != KEY_RELOAD_RESOURCES && key != KEY_RELOAD_RESOURCE_PACKS) {
            return;
        }

        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player != null) {
            minecraft.player.displayClientMessage(
                    Component.literal("[CreachFix] F3+T/F3+S ist blockiert (Crash-Schutz)."), true);
        }
        cir.setReturnValue(true);
    }
}
