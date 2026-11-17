package de.wolfmod.creachreportfix.support;

import com.mojang.logging.LogUtils;
import net.minecraftforge.fml.loading.FMLPaths;
import org.slf4j.Logger;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Turns off the two ModernFix mixins that are known to cause reload crashes
 * when combined with other client mods.
 *
 * <p>Only existing settings are respected: if the user already configured a
 * key, it is never overwritten. If ModernFix is not installed, nothing happens.</p>
 */
public final class ModernFixOverrides {
    private static final String LOG_PREFIX = "[creachreportfix]";
    private static final Logger LOGGER = LogUtils.getLogger();

    private static final List<String> SAFE_SETTINGS = List.of(
            "mixin.perf.dedicated_reload_executor=false",
            "mixin.perf.thread_priorities=false"
    );

    private ModernFixOverrides() {
    }

    public static void applySafeDefaults() {
        Path config = FMLPaths.GAMEDIR.get().resolve("config").resolve("modernfix-mixins.properties");
        if (!Files.exists(config)) {
            return;
        }

        try {
            List<String> lines = Files.readAllLines(config, StandardCharsets.UTF_8);
            List<String> missing = SAFE_SETTINGS.stream()
                    .filter(setting -> !containsSetting(lines, keyOf(setting)))
                    .collect(Collectors.toList());

            if (missing.isEmpty()) {
                return;
            }

            List<String> updated = new ArrayList<>(lines);
            if (!updated.isEmpty() && !updated.get(updated.size() - 1).isBlank()) {
                updated.add("");
            }
            updated.add("# Added by creachreportfix to reduce manual reload crash risk.");
            updated.addAll(missing);

            Files.write(config, updated, StandardCharsets.UTF_8);
            LOGGER.info("{} Applied ModernFix override(s) in {}", LOG_PREFIX, config);
        } catch (IOException ex) {
            LOGGER.warn("{} Could not apply ModernFix override(s).", LOG_PREFIX, ex);
        }
    }

    private static String keyOf(String setting) {
        int separator = setting.indexOf('=');
        return separator < 0 ? setting : setting.substring(0, separator);
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
}
