package de.wolfmod.creachreportfix;

import com.mojang.logging.LogUtils;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.loading.FMLEnvironment;
import net.minecraftforge.fml.loading.FMLPaths;
import org.slf4j.Logger;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@Mod(CreachReportFixMod.MODID)
public final class CreachReportFixMod {
    public static final String MODID = "creachreportfix";
    private static final Logger LOGGER = LogUtils.getLogger();

    public CreachReportFixMod() {
        if (FMLEnvironment.dist == Dist.CLIENT) {
            enableSafeConcurrencyMode();
            applyModernFixOverrides();
            installGlobalCrashHintHandler();
        }
    }

    private static void enableSafeConcurrencyMode() {
        setPropertyIfMissing("java.util.concurrent.ForkJoinPool.common.parallelism", "1");
        setPropertyIfMissing("max.bg.threads", String.valueOf(calculateBackgroundThreadLimit()));
        LOGGER.info("[{}] Safe concurrency mode enabled.", MODID);
    }

    private static int calculateBackgroundThreadLimit() {
        int cores = Runtime.getRuntime().availableProcessors();
        return Math.min(3, Math.max(1, cores / 4));
    }

    private static void setPropertyIfMissing(String key, String value) {
        String current = System.getProperty(key);
        if (current == null || current.isBlank()) {
            System.setProperty(key, value);
            LOGGER.info("[{}] Applied JVM property {}={}", MODID, key, value);
        }
    }

    private static void applyModernFixOverrides() {
        Path config = FMLPaths.GAMEDIR.get().resolve("config").resolve("modernfix-mixins.properties");
        if (!Files.exists(config)) {
            return;
        }

        try {
            List<String> lines = Files.readAllLines(config, StandardCharsets.UTF_8);
            boolean hasDedicatedReloadExecutor = containsSetting(lines, "mixin.perf.dedicated_reload_executor");
            boolean hasThreadPriorities = containsSetting(lines, "mixin.perf.thread_priorities");

            if (hasDedicatedReloadExecutor && hasThreadPriorities) {
                return;
            }

            List<String> updated = new ArrayList<>(lines);
            updated.add("");
            updated.add("# Added by creachreportfix to reduce manual reload crash risk.");
            if (!hasDedicatedReloadExecutor) {
                updated.add("mixin.perf.dedicated_reload_executor=false");
            }
            if (!hasThreadPriorities) {
                updated.add("mixin.perf.thread_priorities=false");
            }

            Files.write(config, updated, StandardCharsets.UTF_8);
            LOGGER.info("[{}] Applied ModernFix override(s) in {}", MODID, config);
        } catch (IOException ex) {
            LOGGER.warn("[{}] Could not apply ModernFix override(s).", MODID, ex);
        }
    }

    private static boolean containsSetting(List<String> lines, String key) {
        String prefix = key + "=";
        for (String line : lines) {
            String trimmed = line.trim();
            if (!trimmed.isEmpty() && !trimmed.startsWith("#") && trimmed.startsWith(prefix)) {
                return true;
            }
        }
        return false;
    }

    private static void installGlobalCrashHintHandler() {
        Thread.UncaughtExceptionHandler previous = Thread.getDefaultUncaughtExceptionHandler();
        Thread.setDefaultUncaughtExceptionHandler((thread, throwable) -> {
            try {
                LOGGER.error("[{}] Uncaught client exception on thread {}.", MODID, thread.getName(), throwable);
                String message = throwable.toString().toLowerCase(Locale.ROOT);
                if (message.contains("concurrentmodificationexception")) {
                    LOGGER.error("[{}] Crash hint: likely resource reload concurrency conflict between client mods.", MODID);
                }
            } catch (Throwable ignored) {
            }

            if (previous != null) {
                previous.uncaughtException(thread, throwable);
            }
        });
    }
}
