package dev.xyat.kineticarmory.armorsets.client.gui.editor;

import dev.xyat.kineticcore.api.client.gui.render.KineticGraphics;
import dev.xyat.kineticcore.api.client.gui.theme.KineticTheme;
import net.minecraft.network.chat.Component;

/**
 * Layout of the small set-effect editors: a compact header and rows of controls four pixels apart. The panel
 * height follows from the number of rows and the panel is centred in the page both ways, so every editor sits in the
 * same place whatever its size.
 */
final class EditorPanel {
    private static final int PADDING = 4;
    private static final int CONTROL_HEIGHT = dev.xyat.kineticcore.api.client.gui.page.KineticPage.CONTROL_HEIGHT;
    private static final int ROW_STEP = CONTROL_HEIGHT + 4;

    final int centerX;
    final int left;
    final int top;
    final int width;
    final int height;

    EditorPanel(int pageWidth, int pageHeight, int width, int rows) {
        this.width = width;
        this.height = CONTROL_HEIGHT + PADDING * 2 + rows * ROW_STEP;
        this.centerX = pageWidth / 2;
        this.left = centerX - width / 2;
        this.top = (pageHeight - height) / 2;
    }

    /** Top of control row {@code row}, counted from 0. */
    int rowY(int row) {
        return top + CONTROL_HEIGHT + PADDING * 2 + row * ROW_STEP;
    }

    /** Top of the Save button row. */
    int buttonY() {
        return top + PADDING;
    }

    int backX() { return left + PADDING; }

    int backY() { return top + PADDING; }

    int saveX() { return left + width - PADDING - 40; }

    /** Draws the title within the header space to the right of Back. */
    void render(KineticGraphics graphics, Component title) {
        KineticTheme.panel(graphics, left, top, width, height);
        int textLeft = backX() + 40 + 4;
        int textRight = saveX() - 4;
        graphics.scrollingTextCentered(title, (textLeft + textRight) / 2, top + PADDING + 6,
                Math.max(0, textRight - textLeft), 0xFFFFFF, true);
    }
}
