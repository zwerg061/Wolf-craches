package de.wolfmod.creachreportfix.mixin;

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
import java.lang.reflect.Field;
import java.util.Collections;
import java.util.List;

@Pseudo
@Mixin(targets = "xfacthd.framedblocks.api.model.FramedBlockModel", remap = false)
public final class FramedBlocksModelMixin {
    @Unique
    private static volatile Field creachfix$baseModelField;

    @Unique
    private boolean creachfix$baseModelOk;

    @Unique
    private static Field creachfix$resolveField(Object instance) {
        Field field = creachfix$baseModelField;
        if (field != null) {
            return field;
        }

        Class<?> current = instance.getClass();
        while (current != null) {
            try {
                field = current.getDeclaredField("baseModel");
                field.setAccessible(true);
                creachfix$baseModelField = field;
                return field;
            } catch (NoSuchFieldException ignored) {
                current = current.getSuperclass();
            }
        }
        return null;
    }

    @Inject(
            method = "getQuads(Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/core/Direction;Lnet/minecraft/util/RandomSource;Lnet/minecraftforge/client/model/data/ModelData;Lnet/minecraft/client/renderer/RenderType;)Ljava/util/List;",
            at = @At("HEAD"),
            cancellable = true,
            remap = false,
            require = 0
    )
    private void creachfix$guardGetQuads(@Nullable BlockState state, @Nullable Direction side, RandomSource rand, ModelData extraData, RenderType renderType, CallbackInfoReturnable<List<BakedQuad>> cir) {
        if (this.creachfix$baseModelOk) {
            return;
        }

        try {
            Field field = creachfix$resolveField(this);
            if (field == null) {
                return;
            }

            if (field.get(this) == null) {
                cir.setReturnValue(Collections.emptyList());
            } else {
                this.creachfix$baseModelOk = true;
            }
        } catch (Exception ignored) {
        }
    }
}
