package de.wolfmod.creachreportfix.crash;

/**
 * A single problem detected inside a crash report.
 *
 * @param title  short description shown in bold in the crash screen
 * @param advice what the player can do about it
 */
public record DetectedIssue(String title, String advice) {
}
