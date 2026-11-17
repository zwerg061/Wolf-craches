package de.wolfmod.creachreportfix.mixin;

import de.wolfmod.creachreportfix.support.FieldLookup;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.client.model.data.ModelData;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import javax.annotation.Nullable;
import java.util.Collections;
import java.util.List;

/**
 * Guards {@code FramedBlockModel#getQuads} against a not yet initialised
 * {@code baseModel}. Without the guard FramedBlocks throws a
 * {@code NullPointerException} during the first frames after a reload.
 *
 * <p>The targeted overload is the Forge one (with {@link ModelData} and
 * {@link RenderType}); Forge-added methods keep their readable name at runtime,
 * which is why {@code remap = false} is correct here.</p>
 */
@Pseudo
@Mixin(targets = "xfacthd.framedblocks.api.model.FramedBlockModel", remap = false)
public final class FramedBlocksModelMixin {
    /** Set once the base model was seen non-null; the field is not cleared afterwards. */
    @Unique
    private boolean creachfix$baseModelSeen;

    @Inject(
            method = "getQuads(Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/core/Direction;"
                    + "Lnet/minecraft/util/RandomSource;Lnet/minecraftforge/client/model/data/ModelData;"
                    + "Lnet/minecraft/client/renderer/RenderType;)Ljava/util/List;",
            at = @At("HEAD"),
            cancellable = true,
            remap = false,
            require = 0
    )
    private void creachfix$guardGetQuads(@Nullable BlockState state, @Nullable Direction side, RandomSource random,
                                         ModelData modelData, RenderType renderType,
                                         CallbackInfoReturnable<List<BakedQuad>> cir) {
        if (this.creachfix$baseModelSeen) {
            return;
        }

        if (FieldLookup.hasValue(this, "baseModel")) {
            this.creachfix$baseModelSeen = true;
        } else {
            cir.setReturnValue(Collections.emptyList());
        }
    }
}
