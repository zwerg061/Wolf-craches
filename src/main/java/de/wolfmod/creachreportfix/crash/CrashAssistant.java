package de.wolfmod.creachreportfix.crash;

import com.mojang.logging.LogUtils;
import net.minecraftforge.fml.loading.FMLPaths;
import org.slf4j.Logger;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

/**
 * Finds crash reports that were written by the previous game session and keeps
 * the newest one ready for {@link CrashAssistantScreen}.
 *
 * <p>This mirrors how Crash Assistant works: the report is analysed after the
 * crash, on the next start, so nothing has to run while the game is dying.</p>
 */
public final class CrashAssistant {
    private static final String LOG_PREFIX = "[creachreportfix]";
    private static final Logger LOGGER = LogUtils.getLogger();

    private static final String CONFIG_FILE = "creachreportfix-client.properties";
    private static final String STATE_DIR = "creachreportfix";
    private static final String STATE_FILE = "last-crash-shown.txt";

    private static boolean enabled = true;
    private static int maxAgeDays = 30;

    private static CrashAnalysis pending;
    private static String pendingMarker;

    private CrashAssistant() {
    }

    /** Called once while the client starts. Never throws. */
    public static void init() {
        try {
            loadConfig();
            if (enabled) {
                scan();
            }
        } catch (Throwable throwable) {
            LOGGER.warn("{} Crash assistant disabled: {}", LOG_PREFIX, throwable.toString());
            pending = null;
        }
    }

    private static void scan() {
        Path report = newestReport();
        if (report == null) {
            return;
        }

        long modified;
        try {
            modified = Files.getLastModifiedTime(report).toMillis();
        } catch (IOException ex) {
            return;
        }

        long ageDays = (System.currentTimeMillis() - modified) / 86_400_000L;
        if (ageDays > maxAgeDays) {
            return;
        }

        String marker = report.getFileName() + "@" + modified;
        if (marker.equals(readState())) {
            return;
        }

        pending = CrashAnalyzer.analyze(report);
        pendingMarker = marker;
        LOGGER.info("{} Crash report found: {}", LOG_PREFIX, report.getFileName());
    }

    private static Path newestReport() {
        Path dir = crashReportsDir();
        if (!Files.isDirectory(dir)) {
            return null;
        }

        Path newest = null;
        long newestTime = Long.MIN_VALUE;

        try (DirectoryStream<Path> stream = Files.newDirectoryStream(dir, "crash-*.txt")) {
            for (Path candidate : stream) {
                try {
                    long time = Files.getLastModifiedTime(candidate).toMillis();
                    if (time > newestTime) {
                        newestTime = time;
                        newest = candidate;
                    }
                } catch (IOException ignored) {
                    // Report was deleted in the meantime.
                }
            }
        } catch (IOException ex) {
            LOGGER.warn("{} Could not list crash reports", LOG_PREFIX, ex);
            return null;
        }

        return newest;
    }

    public static Path crashReportsDir() {
        return FMLPaths.GAMEDIR.get().resolve("crash-reports");
    }

    public static boolean hasPending() {
        return pending != null;
    }

    public static CrashAnalysis pending() {
        return pending;
    }

    /** Remembers that this report was shown, so the screen appears only once. */
    public static void markShown() {
        String marker = pendingMarker;
        pending = null;
        pendingMarker = null;
        if (marker == null) {
            return;
        }

        try {
            Path dir = stateDir();
            Files.createDirectories(dir);
            Files.writeString(dir.resolve(STATE_FILE), marker, StandardCharsets.UTF_8);
        } catch (IOException ex) {
            LOGGER.debug("{} Could not store crash screen state", LOG_PREFIX, ex);
        }
    }

    private static String readState() {
        try {
            Path file = stateDir().resolve(STATE_FILE);
            if (!Files.exists(file)) {
                return "";
            }
            return Files.readString(file, StandardCharsets.UTF_8).trim();
        } catch (IOException ex) {
            return "";
        }
    }

    private static Path stateDir() {
        return FMLPaths.CONFIGDIR.get().resolve(STATE_DIR);
    }

    private static Path configFile() {
        return FMLPaths.CONFIGDIR.get().resolve(CONFIG_FILE);
    }

    private static void loadConfig() {
        Path file = configFile();
        Properties properties = new Properties();

        if (Files.exists(file)) {
            try (InputStream in = Files.newInputStream(file)) {
                properties.load(in);
            } catch (IOException ex) {
                LOGGER.warn("{} Could not read {}", LOG_PREFIX, file);
            }
        } else {
            properties.setProperty("crash-screen-enabled", "true");
            properties.setProperty("crash-report-max-age-days", "30");
            writeDefaults(file, properties);
        }

        enabled = Boolean.parseBoolean(properties.getProperty("crash-screen-enabled", "true").trim());
        maxAgeDays = parseInt(properties.getProperty("crash-report-max-age-days", "30"), 30);
    }

    private static void writeDefaults(Path file, Properties properties) {
        try {
            Files.createDirectories(file.getParent());
            try (OutputStream out = Files.newOutputStream(file)) {
                properties.store(out, "Creach Report Fix - Crash Assistant");
            }
        } catch (IOException ex) {
            LOGGER.debug("{} Could not write default config", LOG_PREFIX, ex);
        }
    }

    private static int parseInt(String value, int fallback) {
        try {
            return Integer.parseInt(value.trim());
        } catch (NumberFormatException ex) {
            return fallback;
        }
    }
}
