package de.wolfmod.creachreportfix.mixin;

import net.minecraft.client.resources.model.BakedModel;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Pseudo
@Mixin(targets = "xfacthd.framedblocks.api.model.BakedModelProxy", remap = false)
public final class FramedBlocksBakedModelProxyMixin {
    @Shadow(remap = false)
    @Final
    protected BakedModel baseModel;

    @Inject(method = "m_7541_", at = @At("HEAD"), cancellable = true, remap = false, require = 0)
    private void creachfix$guardUseAmbientOcclusion(CallbackInfoReturnable<Boolean> cir) {
        if (this.baseModel == null) {
            cir.setReturnValue(false);
        }
    }
}
