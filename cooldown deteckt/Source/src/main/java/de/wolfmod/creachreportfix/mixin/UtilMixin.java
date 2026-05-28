package de.wolfmod.creachreportfix.mixin;

import net.minecraft.Util;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Util.class)
public final class UtilMixin {
    @Inject(method = "<clinit>", at = @At("HEAD"))
    private static void creachfix$limitBgThreadsEarly(CallbackInfo ci) {
        if (System.getProperty("max.bg.threads") == null) {
            int cores = Runtime.getRuntime().availableProcessors();
            int limit = Math.min(3, Math.max(1, cores / 4));
            System.setProperty("max.bg.threads", String.valueOf(limit));
        }
    }
}
