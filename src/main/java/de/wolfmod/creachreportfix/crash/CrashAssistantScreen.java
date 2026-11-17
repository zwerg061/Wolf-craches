package de.wolfmod.creachreportfix.crash;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.Util;
import net.minecraft.client.gui.GuiComponent;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;

import java.util.ArrayList;
import java.util.List;

/**
 * Crash screen shown on the next start after a crash. Lists the detected cause,
 * what the player can do and which mods were involved.
 */
public final class CrashAssistantScreen extends Screen {
    private static final int LINE_HEIGHT = 10;
    private static final int PADDING = 12;

    private static final int PANEL_BG = 0xF0101014;
    private static final int ACCENT = 0xFFE5484D;
    private static final int TITLE_COLOR = 0xFFFFFFFF;
    private static final int MUTED_COLOR = 0xFF9BA1AB;
    private static final int TEXT_COLOR = 0xFFDDE1E6;
    private static final int WARN_COLOR = 0xFFFFC857;
    private static final int OK_COLOR = 0xFF56D364;

    private record Line(FormattedCharSequence text, int color, int indent) {
    }

    private final Screen parent;
    private final CrashAnalysis analysis;
    private final List<Line> lines = new ArrayList<>();

    private int scrollOffset;
    private int viewportHeight;
    private long copiedAt = -1L;

    public CrashAssistantScreen(Screen parent, CrashAnalysis analysis) {
        super(Component.literal("Crash Assistant"));
        this.parent = parent;
        this.analysis = analysis;
        CrashAssistant.markShown();
    }

    @Override
    protected void init() {
        this.scrollOffset = 0;
        this.buildLines();
        this.viewportHeight = this.panelHeight() - 62;

        int buttonY = this.panelTop() + this.panelHeight() - 26;
        int gap = 6;
        int usable = this.panelWidth() - PADDING * 2;
        int buttonWidth = (usable - gap * 2) / 3;
        int x = this.panelLeft() + PADDING;

        this.addRenderableWidget(new Button(x, buttonY, buttonWidth, 20,
                Component.literal("Report-Ordner"), button -> creachfix$openReportsFolder()));
        this.addRenderableWidget(new Button(x + buttonWidth + gap, buttonY, buttonWidth, 20,
                Component.literal("Diagnose kopieren"), button -> creachfix$copyDiagnosis()));
        this.addRenderableWidget(new Button(x + (buttonWidth + gap) * 2, buttonY, buttonWidth, 20,
                Component.literal("Schließen"), button -> this.onClose()));
    }

    private void buildLines() {
        this.lines.clear();
        int wrapWidth = this.panelWidth() - PADDING * 2 - 8;

        if (!this.analysis.description().isEmpty()) {
            this.addWrapped("Beschreibung: " + this.analysis.description(), MUTED_COLOR, 0, wrapWidth);
        }
        if (!this.analysis.exception().isEmpty()) {
            this.addWrapped(this.analysis.exception(), WARN_COLOR, 0, wrapWidth);
        }
        this.addWrapped("", TEXT_COLOR, 0, wrapWidth);

        this.addWrapped("Erkannte Probleme", MUTED_COLOR, 0, wrapWidth);
        for (DetectedIssue issue : this.analysis.issues()) {
            this.addWrapped("- " + issue.title(), TEXT_COLOR, 0, wrapWidth);
            this.addWrapped(issue.advice(), MUTED_COLOR, 8, wrapWidth - 8);
        }

        this.addWrapped("", TEXT_COLOR, 0, wrapWidth);
        this.addWrapped("Verdächtige Mods", MUTED_COLOR, 0, wrapWidth);
        if (this.analysis.suspects().isEmpty()) {
            this.addWrapped("Keine Mod eindeutig zuordenbar.", TEXT_COLOR, 8, wrapWidth - 8);
        } else {
            for (String suspect : this.analysis.suspects()) {
                this.addWrapped("- " + suspect, TEXT_COLOR, 8, wrapWidth - 8);
            }
        }
    }

    private void addWrapped(String text, int color, int indent, int width) {
        if (text.isEmpty()) {
            this.lines.add(new Line(FormattedCharSequence.EMPTY, color, indent));
            return;
        }

        for (FormattedCharSequence part : this.font.split(Component.literal(text), Math.max(40, width))) {
            this.lines.add(new Line(part, color, indent));
        }
    }

    @Override
    public void render(PoseStack poseStack, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(poseStack);

        int left = this.panelLeft();
        int top = this.panelTop();
        int panelWidth = this.panelWidth();
        int panelHeight = this.panelHeight();

        GuiComponent.fill(poseStack, left, top, left + panelWidth, top + panelHeight, PANEL_BG);
        GuiComponent.fill(poseStack, left, top, left + panelWidth, top + 3, ACCENT);

        GuiComponent.drawString(poseStack, this.font, "Crash erkannt", left + PADDING, top + 14, TITLE_COLOR);
        GuiComponent.drawString(poseStack, this.font, this.analysis.fileName(), left + PADDING, top + 26,
                MUTED_COLOR);

        int contentTop = top + 42;
        int contentBottom = top + panelHeight - 36;
        int y = contentTop - this.scrollOffset;

        for (Line line : this.lines) {
            if (y >= contentTop - LINE_HEIGHT && y + LINE_HEIGHT <= contentBottom + LINE_HEIGHT) {
                GuiComponent.drawString(poseStack, this.font, line.text(), left + PADDING + line.indent(), y,
                        line.color());
            }
            y += LINE_HEIGHT;
        }

        int statusY = contentBottom + 6;
        if (this.canScroll()) {
            GuiComponent.drawString(poseStack, this.font, "Mausrad zum Scrollen", left + PADDING, statusY,
                    MUTED_COLOR);
        } else if (this.copiedAt > 0 && Util.getMillis() - this.copiedAt < 2500L) {
            GuiComponent.drawString(poseStack, this.font, "Diagnose kopiert.", left + PADDING, statusY,
                    OK_COLOR);
        }

        super.render(poseStack, mouseX, mouseY, partialTick);
    }

    private boolean canScroll() {
        return this.lines.size() * LINE_HEIGHT > this.viewportHeight;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        int maxScroll = Math.max(0, this.lines.size() * LINE_HEIGHT - this.viewportHeight);
        this.scrollOffset = Mth.clamp((int) (this.scrollOffset - delta * LINE_HEIGHT * 2), 0, maxScroll);
        return true;
    }

    @Override
    public void onClose() {
        if (this.minecraft != null) {
            this.minecraft.setScreen(this.parent);
        }
    }

    private void creachfix$copyDiagnosis() {
        if (this.minecraft == null) {
            return;
        }
        this.minecraft.keyboardHandler.setClipboard(this.analysis.toShareText());
        this.copiedAt = Util.getMillis();
    }

    private void creachfix$openReportsFolder() {
        try {
            Util.getPlatform().openFile(CrashAssistant.crashReportsDir().toFile());
        } catch (Throwable ignored) {
            // Opening the folder is a convenience, never a reason to crash.
        }
    }

    private int panelWidth() {
        return Math.min(this.width - 40, 560);
    }

    private int panelHeight() {
        return Math.min(this.height - 40, 400);
    }

    private int panelLeft() {
        return (this.width - this.panelWidth()) / 2;
    }

    private int panelTop() {
        return (this.height - this.panelHeight()) / 2;
    }
}
