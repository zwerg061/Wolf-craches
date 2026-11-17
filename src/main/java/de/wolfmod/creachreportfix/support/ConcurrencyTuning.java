package de.wolfmod.creachreportfix.support;

import com.mojang.logging.LogUtils;
import org.slf4j.Logger;

/**
 * Limits the number of threads Minecraft uses for background work (resource
 * reload, model baking, chunk light updates). Most of the reported crashes are
 * concurrent-modification exceptions caused by too many workers touching the
 * same registries.
 */
public final class ConcurrencyTuning {
    /** Property read by several performance mods to cap background workers. */
    public static final String MAX_BG_THREADS = "max.bg.threads";
    /** ForkJoinPool property used by the vanilla reload/model pipeline. */
    public static final String FORK_JOIN_PARALLELISM = "java.util.concurrent.ForkJoinPool.common.parallelism";

    private static final String LOG_PREFIX = "[creachreportfix]";
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final int HARD_THREAD_CAP = 3;

    private ConcurrencyTuning() {
    }

    /**
     * Computes the background worker limit for this machine: a quarter of the
     * available cores, clamped to {@code 1..3}.
     */
    public static int backgroundThreadLimit() {
        int cores = Runtime.getRuntime().availableProcessors();
        return Math.min(HARD_THREAD_CAP, Math.max(1, cores / 4));
    }

    /** Applies both properties unless the user already set them. */
    public static void applySafeDefaults() {
        boolean forkJoinChanged = setPropertyIfMissing(FORK_JOIN_PARALLELISM, "1");
        boolean backgroundChanged = applyBackgroundThreadLimitIfMissing();

        if (forkJoinChanged || backgroundChanged) {
            LOGGER.info("{} Safe concurrency mode enabled (background threads: {}).",
                    LOG_PREFIX, backgroundThreadLimit());
        }
    }

    /**
     * Only sets the background thread limit.
     *
     * <p>This variant is called from {@code Util}'s static initialiser, which runs
     * before the mod class is loaded, therefore it must not log or touch other
     * mod classes.</p>
     *
     * @return {@code true} if the property was applied
     */
    public static boolean applyBackgroundThreadLimitIfMissing() {
        return setPropertyIfMissing(MAX_BG_THREADS, String.valueOf(backgroundThreadLimit()));
    }

    private static boolean setPropertyIfMissing(String key, String value) {
        String current = System.getProperty(key);
        if (current != null && !current.isBlank()) {
            return false;
        }

        System.setProperty(key, value);
        return true;
    }
}
