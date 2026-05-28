package de.wolfmod.creachreportfix.mixin;

import net.minecraft.world.item.Item;
import net.minecraftforge.registries.IForgeRegistry;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.util.ArrayList;
import java.util.Collection;

@Pseudo
@Mixin(targets = "com.mrcrayfish.guns.client.MetaLoader", remap = false)
public final class CgmMetaLoaderMixin {
    @Redirect(
            method = "getResourceSuppliers",
            at = @At(value = "INVOKE", target = "Lnet/minecraftforge/registries/IForgeRegistry;getValues()Ljava/util/Collection;"),
            require = 0
    )
    private Collection<Item> creachfix$snapshotItemRegistry(IForgeRegistry<Item> registry) {
        return new ArrayList<>(registry.getValues());
    }
}
