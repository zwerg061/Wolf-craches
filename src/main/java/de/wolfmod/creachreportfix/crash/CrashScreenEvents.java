package de.wolfmod.creachreportfix.crash;

import de.wolfmod.creachreportfix.CreachReportFixMod;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Swaps the title screen for the crash assistant when the previous session ended
 * in a crash.
 */
@Mod.EventBusSubscriber(modid = CreachReportFixMod.MODID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class CrashScreenEvents {
    private CrashScreenEvents() {
    }

    @SubscribeEvent
    public static void onScreenOpening(ScreenEvent.Opening event) {
        try {
            if (!(event.getNewScreen() instanceof TitleScreen)) {
                return;
            }
            if (!CrashAssistant.hasPending()) {
                return;
            }
            event.setNewScreen(new CrashAssistantScreen(event.getNewScreen(), CrashAssistant.pending()));
        } catch (Throwable throwable) {
            CreachReportFixMod.LOGGER.warn("[creachreportfix] Could not open the crash screen.", throwable);
        }
    }
}
