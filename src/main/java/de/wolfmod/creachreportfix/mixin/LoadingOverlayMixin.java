package de.wolfmod.creachreportfix.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.LoadingOverlay;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.server.packs.resources.ReloadInstance;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Optional;
import java.util.function.Consumer;

/**
 * While joining a server, the server resource pack triggers a second resource
 * reload. Vanilla keeps the frozen "Loading world" overlay on top of everything
 * and pauses the game, which looks like (and often becomes) a crash.
 *
 * <p>For that specific reload the overlay is turned into a pass-through: the
 * underlying screen keeps rendering and the game is not paused.</p>
 */
@Mixin(LoadingOverlay.class)
public abstract class LoadingOverlayMixin {
    /*
     * The shadow fields must carry the runtime (SRG) names, otherwise the mixin
     * cannot find them after reobfuscation. remap = false keeps them untouched.
     */
    @Shadow(remap = false)
    @Final
    private Minecraft f_96163_;

    @Shadow(remap = false)
    @Final
    private ReloadInstance f_96164_;

    @Shadow(remap = false)
    @Final
    private Consumer<Optional<Throwable>> f_96165_;

    @Unique
    private boolean creachfix$finished;

    @Inject(method = "m_6305_", at = @At("HEAD"), cancellable = true, remap = false)
    private void creachfix$hideReloadOverlay(PoseStack poseStack, int mouseX, int mouseY, float partialTick,
                                             CallbackInfo ci) {
        if (!creachfix$isServerResourcePackReload()) {
            return;
        }

        Screen screen = this.f_96163_.screen;
        if (screen != null) {
            screen.render(poseStack, mouseX, mouseY, partialTick);
        }

        if (!this.creachfix$finished && this.f_96164_.isDone()) {
            creachfix$finishReload();
        }

        ci.cancel();
    }

    @Inject(method = "m_7859_", at = @At("HEAD"), cancellable = true, remap = false)
    private void creachfix$doNotPauseWhileReloading(CallbackInfoReturnable<Boolean> cir) {
        if (creachfix$isServerResourcePackReload()) {
            cir.setReturnValue(false);
        }
    }

    @Unique
    private boolean creachfix$isServerResourcePackReload() {
        return this.f_96163_.level != null
                && this.f_96163_.player != null
                && this.f_96163_.getResourcePackRepository().getSelectedIds().contains("server");
    }

    @Unique
    private void creachfix$finishReload() {
        this.creachfix$finished = true;

        try {
            this.f_96164_.done();
            this.f_96165_.accept(Optional.empty());
        } catch (Throwable throwable) {
            this.f_96165_.accept(Optional.of(throwable));
        }

        this.f_96163_.setOverlay(null);

        Screen screen = this.f_96163_.screen;
        if (screen != null) {
            screen.init(this.f_96163_, this.f_96163_.getWindow().getGuiScaledWidth(),
                    this.f_96163_.getWindow().getGuiScaledHeight());
        }
    }
}
