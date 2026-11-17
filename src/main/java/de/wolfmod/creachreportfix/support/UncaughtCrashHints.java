package de.wolfmod.creachreportfix.support;

import com.mojang.logging.LogUtils;
import org.slf4j.Logger;

import java.util.ConcurrentModificationException;

/**
 * Adds a readable hint to the log when a client thread dies from a known crash
 * pattern, then hands the throwable over to the previously installed handler so
 * normal crash reporting keeps working.
 */
public final class UncaughtCrashHints {
    private static final String LOG_PREFIX = "[creachreportfix]";
    private static final Logger LOGGER = LogUtils.getLogger();

    private UncaughtCrashHints() {
    }

    public static void install() {
        Thread.UncaughtExceptionHandler previous = Thread.getDefaultUncaughtExceptionHandler();
        Thread.setDefaultUncaughtExceptionHandler((thread, throwable) -> {
            logHint(thread, throwable);
            if (previous != null) {
                previous.uncaughtException(thread, throwable);
            }
        });
    }

    private static void logHint(Thread thread, Throwable throwable) {
        try {
            LOGGER.error("{} Uncaught client exception on thread {}.", LOG_PREFIX, thread.getName(), throwable);
            if (isConcurrencyCrash(throwable)) {
                LOGGER.error("{} Crash hint: likely a resource reload concurrency conflict between client mods.",
                        LOG_PREFIX);
            }
        } catch (Throwable ignored) {
            // A crash handler must never crash the game itself.
        }
    }

    private static boolean isConcurrencyCrash(Throwable throwable) {
        for (Throwable current = throwable; current != null; current = current.getCause()) {
            if (current instanceof ConcurrentModificationException) {
                return true;
            }
        }
        return false;
    }
}
