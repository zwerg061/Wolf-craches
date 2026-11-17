package de.wolfmod.creachreportfix.crash;

import java.nio.file.Path;
import java.util.List;

/**
 * Result of analysing one crash report.
 */
public final class CrashAnalysis {
    private final Path report;
    private final String description;
    private final String exception;
    private final List<String> suspects;
    private final List<DetectedIssue> issues;

    CrashAnalysis(Path report, String description, String exception,
                  List<String> suspects, List<DetectedIssue> issues) {
        this.report = report;
        this.description = description;
        this.exception = exception;
        this.suspects = List.copyOf(suspects);
        this.issues = List.copyOf(issues);
    }

    public Path report() {
        return this.report;
    }

    public String fileName() {
        return this.report.getFileName().toString();
    }

    public String description() {
        return this.description;
    }

    public String exception() {
        return this.exception;
    }

    public List<String> suspects() {
        return this.suspects;
    }

    public List<DetectedIssue> issues() {
        return this.issues;
    }

    /** Text put into the clipboard by the button in the crash screen. */
    public String toShareText() {
        StringBuilder builder = new StringBuilder();
        builder.append("=== Creach Report Fix - Crash-Diagnose ===\n");
        builder.append("Report: ").append(fileName()).append('\n');
        if (!this.description.isEmpty()) {
            builder.append("Beschreibung: ").append(this.description).append('\n');
        }
        if (!this.exception.isEmpty()) {
            builder.append("Fehler: ").append(this.exception).append('\n');
        }
        if (!this.issues.isEmpty()) {
            builder.append("\nErkannte Probleme:\n");
            for (DetectedIssue issue : this.issues) {
                builder.append("- ").append(issue.title()).append('\n');
                builder.append("  ").append(issue.advice()).append('\n');
            }
        }
        builder.append("\nVerdächtige Mods:\n");
        if (this.suspects.isEmpty()) {
            builder.append("- keine eindeutig zuordenbar\n");
        } else {
            for (String suspect : this.suspects) {
                builder.append("- ").append(suspect).append('\n');
            }
        }
        builder.append("\nReport-Datei: ").append(this.report.toAbsolutePath()).append('\n');
        return builder.toString();
    }
}
