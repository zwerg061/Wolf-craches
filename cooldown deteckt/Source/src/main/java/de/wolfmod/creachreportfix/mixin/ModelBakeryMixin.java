package de.wolfmod.creachreportfix.mixin;

import com.google.common.collect.ImmutableSet;
import net.minecraft.client.resources.model.ModelBakery;
import net.minecraft.core.DefaultedRegistry;
import net.minecraft.resources.ResourceLocation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.util.Set;

@Mixin(ModelBakery.class)
public final class ModelBakeryMixin {
    @Redirect(
            method = "<init>",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/core/DefaultedRegistry;m_6566_()Ljava/util/Set;", remap = false),
            remap = false,
            require = 0
    )
    private Set<ResourceLocation> creachfix$snapshotKeySet(DefaultedRegistry<?> registry) {
        return ImmutableSet.copyOf(registry.keySet());
    }
}
