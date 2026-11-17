package de.wolfmod.creachreportfix.mixin;

import de.wolfmod.creachreportfix.support.RegistrySnapshots;
import net.minecraft.world.item.Item;
import net.minecraftforge.registries.IForgeRegistry;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.util.Collection;

/**
 * MrCrayfish's Gun Mod iterates the live Forge item registry while loading its
 * metadata. Other mods registering items at the same time cause a
 * {@link java.util.ConcurrentModificationException}; a snapshot copy avoids it.
 */
@Pseudo
@Mixin(targets = "com.mrcrayfish.guns.client.MetaLoader", remap = false)
public final class CgmMetaLoaderMixin {
    @Redirect(
            method = "getResourceSuppliers",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraftforge/registries/IForgeRegistry;getValues()Ljava/util/Collection;"
            ),
            remap = false,
            require = 0
    )
    private Collection<Item> creachfix$snapshotItemRegistry(IForgeRegistry<Item> registry) {
        return RegistrySnapshots.snapshot(registry.getValues());
    }
}
