package dev.xyat.kineticarmory.armorsets.client.gui.editor;

import dev.xyat.kineticcore.api.client.gui.render.KineticGraphics;
import dev.xyat.kineticcore.api.client.gui.theme.KineticTheme;
import net.minecraft.network.chat.Component;

/**
 * Layout of the small set-effect editors: a title row, rows of 20 px controls 5 px apart and a button row. The panel
 * height follows from the number of rows and the panel is centred in the page both ways, so every editor sits in the
 * same place whatever its size.
 */
final class EditorPanel {
    private static final int PADDING = 10;
    private static final int TITLE_HEIGHT = 9;
    private static final int TITLE_GAP = 10;
    private static final int CONTROL_HEIGHT = 20;
    private static final int ROW_STEP = CONTROL_HEIGHT + 5;
    private static final int BUTTON_GAP = 10;

    final int centerX;
    final int left;
    final int top;
    final int width;
    final int height;
    private final int rows;

    EditorPanel(int pageWidth, int pageHeight, int width, int rows) {
        this.width = width;
        this.rows = rows;
        this.height = PADDING + TITLE_HEIGHT + TITLE_GAP + rows * ROW_STEP + BUTTON_GAP - (ROW_STEP - CONTROL_HEIGHT)
                + CONTROL_HEIGHT + PADDING;
        this.centerX = pageWidth / 2;
        this.left = centerX - width / 2;
        this.top = (pageHeight - height) / 2;
    }

    /** Top of control row {@code row}, counted from 0. */
    int rowY(int row) {
        return top + PADDING + TITLE_HEIGHT + TITLE_GAP + row * ROW_STEP;
    }

    /** Top of the Save button row. */
    int buttonY() {
        return rowY(rows) - (ROW_STEP - CONTROL_HEIGHT) + BUTTON_GAP;
    }

    int backX() { return left + PADDING; }

    int backY() { return rowY(0) - CONTROL_HEIGHT - 2; }

    /** Draws the title within the header space to the right of Back. */
    void render(KineticGraphics graphics, Component title) {
        KineticTheme.panel(graphics, left, top, width, height);
        int textLeft = backX() + 55 + 2;
        int textRight = left + width - PADDING;
        graphics.scrollingTextCentered(title, (textLeft + textRight) / 2, top + PADDING,
                Math.max(0, textRight - textLeft), 0xFFFFFF, true);
    }
}
