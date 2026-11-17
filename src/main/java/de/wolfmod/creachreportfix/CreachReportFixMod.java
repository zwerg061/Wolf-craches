package de.wolfmod.creachreportfix;

import com.mojang.logging.LogUtils;
import de.wolfmod.creachreportfix.crash.CrashAssistant;
import de.wolfmod.creachreportfix.support.ConcurrencyTuning;
import de.wolfmod.creachreportfix.support.ModernFixOverrides;
import de.wolfmod.creachreportfix.support.UncaughtCrashHints;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.loading.FMLEnvironment;
import org.slf4j.Logger;

/**
 * Client-side crash fix for the "Wolf" game mode.
 *
 * <p>The mod only applies global tweaks while starting up, the actual crash guards
 * live in the mixins under {@code de.wolfmod.creachreportfix.mixin}.</p>
 */
@Mod(CreachReportFixMod.MODID)
public final class CreachReportFixMod {
    public static final String MODID = "creachreportfix";
    public static final Logger LOGGER = LogUtils.getLogger();

    public CreachReportFixMod() {
        if (FMLEnvironment.dist != Dist.CLIENT) {
            return;
        }

        ConcurrencyTuning.applySafeDefaults();
        ModernFixOverrides.applySafeDefaults();
        UncaughtCrashHints.install();
        CrashAssistant.init();
    }
}
