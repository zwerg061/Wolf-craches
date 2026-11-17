package de.wolfmod.creachreportfix.mixin;

import de.wolfmod.creachreportfix.support.RegistrySnapshots;
import net.minecraft.client.resources.model.ModelBakery;
import net.minecraft.core.DefaultedRegistry;
import net.minecraft.resources.ResourceLocation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.util.Set;

/**
 * {@code ModelBakery} iterates the live key set of the item registry while
 * building models (line 183: {@code for (ResourceLocation id : Registry.ITEM.keySet())}).
 *
 * <p>Other mods registering items at the same time caused a
 * {@code ConcurrentModificationException} in every reported crash report. The
 * redirect replaces the live view with a snapshot so the loop cannot fail.</p>
 *
 * <p>The parameter type is {@code DefaultedRegistry} because that is the owner of
 * the {@code keySet()} call in the bytecode; {@link RegistrySnapshots} then handles
 * both the vanilla and the Forge registry implementation.</p>
 */
@Mixin(ModelBakery.class)
public final class ModelBakeryMixin {
    @Redirect(
            method = "<init>",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/core/DefaultedRegistry;m_6566_()Ljava/util/Set;"
            ),
            remap = false,
            require = 0
    )
    private Set<ResourceLocation> creachfix$snapshotKeySet(DefaultedRegistry<?> registry) {
        return RegistrySnapshots.keySetOf(registry);
    }
}
