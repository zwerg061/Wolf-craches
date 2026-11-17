package de.wolfmod.creachreportfix.mixin;

import de.wolfmod.creachreportfix.support.ConcurrencyTuning;
import net.minecraft.Util;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Applies the background thread limit before {@code Util} creates its worker
 * pool. Doing this in the static initialiser is the only way to make the limit
 * effective, a later call would be too late.
 */
@Mixin(Util.class)
public final class UtilMixin {
    @Inject(method = "<clinit>", at = @At("HEAD"))
    private static void creachfix$limitBackgroundThreadsEarly(CallbackInfo ci) {
        ConcurrencyTuning.applyBackgroundThreadLimitIfMissing();
    }
}
