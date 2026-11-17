package de.wolfmod.creachreportfix.mixin;

import de.wolfmod.creachreportfix.support.FieldLookup;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * FramedBlocks reads {@code baseModel} in {@code useAmbientOcclusion()} while the
 * model is still being baked. Returning {@code false} instead of throwing keeps
 * the reload alive.
 *
 * <p>{@code m_7541_} is the SRG name of {@code BakedModel.useAmbientOcclusion()}.
 * The field is resolved reflectively so a renamed field can never break the mixin.</p>
 */
@Pseudo
@Mixin(targets = "xfacthd.framedblocks.api.model.BakedModelProxy", remap = false)
public final class FramedBlocksBakedModelProxyMixin {
    @Unique
    private boolean creachfix$baseModelSeen;

    @Inject(method = "m_7541_", at = @At("HEAD"), cancellable = true, remap = false, require = 0)
    private void creachfix$guardUseAmbientOcclusion(CallbackInfoReturnable<Boolean> cir) {
        if (this.creachfix$baseModelSeen) {
            return;
        }

        if (FieldLookup.hasValue(this, "baseModel")) {
            this.creachfix$baseModelSeen = true;
        } else {
            cir.setReturnValue(false);
        }
    }
}
