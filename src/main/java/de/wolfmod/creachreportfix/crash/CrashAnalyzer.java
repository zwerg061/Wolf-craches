package de.wolfmod.creachreportfix.crash;

import com.mojang.logging.LogUtils;
import net.minecraftforge.forgespi.language.IModInfo;
import net.minecraftforge.fml.ModList;
import org.slf4j.Logger;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Reads a Minecraft crash report and turns it into a short, human readable
 * diagnosis: what broke, which mods are involved, what the player can do.
 */
public final class CrashAnalyzer {
    private static final Logger LOGGER = LogUtils.getLogger();

    private static final Pattern JAR_NAME = Pattern.compile("([A-Za-z0-9_.+\\-]+\\.jar)");
    private static final int MAX_STACK_LINES = 160;
    private static final int MAX_CAUSE_LINES = 30;
    private static final int MAX_SUSPECTS = 8;

    private CrashAnalyzer() {
    }

    public static CrashAnalysis analyze(Path report) {
        List<String> lines;
        try {
            lines = Files.readAllLines(report, StandardCharsets.UTF_8);
        } catch (IOException ex) {
            LOGGER.warn("[creachreportfix] Could not read crash report {}", report, ex);
            lines = List.of();
        }

        String description = "";
        String exception = "";
        int stackStart = -1;

        for (int i = 0; i < lines.size(); i++) {
            String line = lines.get(i);
            if (description.isEmpty() && line.startsWith("Description: ")) {
                description = line.substring("Description: ".length()).trim();
            }
            if (exception.isEmpty() && looksLikeException(line)) {
                exception = line.trim();
            }
            if (stackStart < 0 && line.startsWith("\tat ")) {
                stackStart = i;
            }
        }

        String stackText = stackText(lines, stackStart);
        // Only the deepest cause block is relevant; the rest of the report lists
        // every installed mod and would produce false positives.
        String causeText = causeText(lines, stackStart);
        List<String> suspects = findSuspects(causeText.isEmpty() ? stackText : causeText);
        String detail = (description + "\n" + exception + "\n"
                + (causeText.isEmpty() ? stackText : causeText)).toLowerCase(Locale.ROOT);
        List<DetectedIssue> issues = detect(detail, suspects);

        return new CrashAnalysis(report, description, exception, suspects, issues);
    }

    private static boolean looksLikeException(String line) {
        if (line.isEmpty() || Character.isWhitespace(line.charAt(0))) {
            return false;
        }
        if (line.startsWith("Description:") || line.startsWith("Caused by:") || line.startsWith("Suppressed:")) {
            return false;
        }

        String name = line;
        int colon = name.indexOf(':');
        if (colon > 0) {
            name = name.substring(0, colon);
        }
        name = name.trim();

        int dot = name.lastIndexOf('.');
        if (dot >= 0) {
            name = name.substring(dot + 1);
        }
        return name.endsWith("Exception") || name.endsWith("Error") || name.endsWith("Throwable");
    }

    private static String stackText(List<String> lines, int stackStart) {
        if (stackStart < 0) {
            return "";
        }
        int end = Math.min(lines.size(), stackStart + MAX_STACK_LINES);
        return String.join("\n", lines.subList(stackStart, end));
    }

    /**
     * The frames of the deepest {@code Caused by} block. That is where the mod
     * that actually broke shows up, not the wrapper exception on top.
     */
    private static String causeText(List<String> lines, int stackStart) {
        if (stackStart < 0) {
            return "";
        }

        int start = stackStart;
        for (int i = stackStart; i < Math.min(lines.size(), stackStart + MAX_STACK_LINES); i++) {
            if (lines.get(i).startsWith("Caused by:") || lines.get(i).startsWith("Suppressed:")) {
                start = i + 1;
            }
        }

        int end = Math.min(lines.size(), start + MAX_CAUSE_LINES);
        return String.join("\n", lines.subList(start, end));
    }

    private static List<String> findSuspects(String text) {
        Set<String> jars = new LinkedHashSet<>();
        Matcher matcher = JAR_NAME.matcher(text);
        while (matcher.find() && jars.size() < MAX_SUSPECTS * 3) {
            String jar = matcher.group(1);
            if (!isLibraryJar(jar)) {
                jars.add(jar);
            }
        }

        List<String> suspects = new ArrayList<>();
        for (String jar : jars) {
            if (suspects.size() >= MAX_SUSPECTS) {
                break;
            }
            suspects.add(displayNameOf(jar));
        }
        return suspects;
    }

    /** Vanilla, Forge and Minecraft libraries are never the culprit. */
    private static boolean isLibraryJar(String jar) {
        String name = jar.toLowerCase(Locale.ROOT);
        if (name.startsWith("client-") || name.startsWith("server-") || name.contains("forge-1.19")) {
            return true;
        }
        if (name.contains("creach-report-fix") || name.contains("creachreportfix")) {
            // Our own jar shows up in every crash report because of its mixins.
            return true;
        }
        for (String prefix : new String[]{
                "fmlloader", "modlauncher", "bootstraplauncher", "mixin", "eventbus", "accesstransformers",
                "guava", "gson", "netty", "lwjgl", "log4j", "slf4j", "authlib", "brigadier",
                "datafixerupper", "jopt", "oshi", "jarjar", "asm", "commons-", "jna", "icu4j"
        }) {
            if (name.startsWith(prefix)) {
                return true;
            }
        }
        return false;
    }

    private static String displayNameOf(String jarFile) {
        try {
            ModList modList = ModList.get();
            if (modList == null) {
                return jarFile;
            }

            for (IModInfo info : modList.getMods()) {
                try {
                    String fileName = info.getOwningFile().getFile().getFileName();
                    if (fileName.equalsIgnoreCase(jarFile)) {
                        return info.getDisplayName() + " (" + jarFile + ")";
                    }
                } catch (Throwable ignored) {
                    // Some mod infos have no owning file - ignore them.
                }
            }
        } catch (Throwable ignored) {
            // Mod list not ready (or running outside the game).
        }
        return jarFile;
    }

    private static List<DetectedIssue> detect(String text, List<String> suspects) {
        List<DetectedIssue> issues = new ArrayList<>();
        String suspect = suspects.isEmpty() ? "" : suspects.get(0);

        if (text.contains("concurrentmodificationexception")) {
            if (text.contains("modelbakery")) {
                issues.add(new DetectedIssue(
                        "Ressourcen-Reload kollidiert (Model Bakery)",
                        "Genau dagegen schützt diese Mod: die Item-Registry wird beim Modell-Laden nur noch "
                                + "als stabile Kopie gelesen. Falls es weiterhin passiert: keine Resourcepacks "
                                + "im laufenden Spiel wechseln und Mods aktualisieren, die Items oder Blöcke "
                                + "spät registrieren." + suspectHint(suspect)));
            } else if (text.contains("metaloader") || text.contains("mrcrayfish")) {
                issues.add(new DetectedIssue(
                        "MrCrayfish's Gun Mod kollidiert beim Laden",
                        "Die Item-Liste wird hier durch eine stabile Kopie ersetzt, damit der Reload nicht "
                                + "mehr abstürzt. Gun Mod und 'Framework' aktuell halten."
                                + suspectHint(suspect)));
            } else {
                issues.add(new DetectedIssue(
                        "Zwei Mods greifen gleichzeitig auf eine Registry zu",
                        "Meist registriert eine Mod Inhalte während eines Reloads. Die betroffene Mod "
                                + "aktualisieren oder testweise entfernen." + suspectHint(suspect)));
            }
        }

        if (text.contains("worldschematic") && text.contains("classcastexception")) {
            issues.add(new DetectedIssue(
                    "Litematica/Forgematica passt nicht zu einer anderen Mod",
                    "Eine Schematic-Welt wird als echte Welt interpretiert. Litematica/Forgematica "
                            + "aktualisieren oder das Schematic-Rendering abschalten (M -> Render Layers)."));
        }

        if (text.contains("embeddium") && (text.contains("sprite") || text.contains("fluid"))) {
            issues.add(new DetectedIssue(
                    "Embeddium-Flüssigkeitsrenderer ohne Sprite",
                    "Der Flüssigkeits-Cache lief, bevor die Texturen geladen waren. Embeddium aktualisieren "
                            + "oder in den Embeddium-Optionen das Fluid-Rendering auf 'Vanilla' stellen."));
        }

        if (text.contains("collectingneighborupdater")
                || (text.contains("nosuchelementexception") && text.contains("neighbor"))) {
            issues.add(new DetectedIssue(
                    "Block-Update-Konflikt (Redstone oder Block aus einer Mod)",
                    "Ein Block hat während eines Updates weitere Updates ausgelöst. Den zuletzt platzierten "
                            + "Block entfernen und die Mod aktualisieren." + suspectHint(suspect)));
        }

        if (text.contains("outofmemoryerror") || text.contains("java heap space")) {
            issues.add(new DetectedIssue(
                    "Zu wenig Arbeitsspeicher",
                    "Im Launcher mehr RAM zuweisen (6-8 GB) und Resourcepacks/Shaders reduzieren."));
        }

        if (text.contains("noclassdeffounderror") || text.contains("classnotfoundexception")) {
            issues.add(new DetectedIssue(
                    "Fehlende Abhängigkeit",
                    "Eine benötigte Mod oder Bibliothek fehlt. Im Log steht meist dabei, welche Mod sie "
                            + "verlangt." + suspectHint(suspect)));
        }

        if (text.contains("invalidmixinexception") || text.contains("mixin apply failed")
                || text.contains("mixinapplyerror")) {
            issues.add(new DetectedIssue(
                    "Zwei Mods patchen dieselbe Stelle",
                    "Mixin-Konflikt zwischen zwei Mods. Eine der beiden Mods aktualisieren oder entfernen."
                            + suspectHint(suspect)));
        }

        if (text.contains("ticking entity") || text.contains("ticking block entity")) {
            issues.add(new DetectedIssue(
                    "Eine Entity oder ein Block tickt fehlerhaft",
                    "Die betroffene Entity mit /kill entfernen bzw. die Mod aktualisieren."
                            + suspectHint(suspect)));
        }

        if (text.contains("opengl") || text.contains("access_violation") || text.contains("glfw")
                || text.contains("nvoglv") || text.contains("atio6") || text.contains("igxelpicd")) {
            issues.add(new DetectedIssue(
                    "Grafiktreiber-Problem",
                    "Grafiktreiber aktualisieren, Shader deaktivieren und testweise ohne OptiFine oder "
                            + "Shaderpack starten."));
        }

        if (text.contains("duplicatemodsfoundexception") || text.contains("found duplicate mods")) {
            issues.add(new DetectedIssue(
                    "Mod doppelt installiert",
                    "Zwei Versionen derselben Mod liegen im mods-Ordner. Die ältere JAR löschen."));
        }

        if (issues.isEmpty()) {
            issues.add(new DetectedIssue(
                    "Unbekannter Fehler",
                    "Tritt es häufiger auf: 'Diagnose kopieren' nutzen und den Text an den Server-Admin "
                            + "schicken." + suspectHint(suspect)));
        }

        return issues;
    }

    private static String suspectHint(String suspect) {
        return suspect.isEmpty() ? "" : " Betroffen: " + suspect + ".";
    }
}
