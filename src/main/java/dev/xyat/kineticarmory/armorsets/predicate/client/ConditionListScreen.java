package dev.xyat.kineticarmory.armorsets.predicate.client;

import javax.annotation.Nonnull;

import dev.xyat.kineticarmory.armorsets.predicate.ConditionData;
import dev.xyat.kineticarmory.armorsets.predicate.ConditionTypeUtil;
import dev.xyat.kineticarmory.armorsets.predicate.IConditionOwner;
import dev.xyat.kineticarmory.util.ColorText;
import dev.xyat.kineticcore.api.client.input.KineticMouseButtons;
import dev.xyat.kineticcore.api.client.screen.KineticScreen;
import dev.xyat.kineticcore.api.client.theme.GuiTheme;
import dev.xyat.kineticcore.api.client.widget.KineticWidgets;
import dev.xyat.kineticcore.api.client.widget.button.KineticButtons.StateButton;
import dev.xyat.kineticcore.api.client.widget.button.KineticButtons.ToggleButton;
import dev.xyat.kineticcore.api.client.widget.input.KineticTextFields.KineticEditBox;
import dev.xyat.kineticcore.api.client.widget.scroll.KineticScroll.SmoothEntry;
import dev.xyat.kineticcore.api.client.widget.scroll.KineticScroll.SmoothSelectionList;
import dev.xyat.kineticcore.api.runtime.KineticClientRuntime;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

public class ConditionListScreen extends KineticScreen {
    private final IConditionOwner owner;
    private final List<ConditionData> conditions;
    protected ConditionListWidget listWidget;

    private KineticEditBox minCountInput;
    private List<Component> delayedTooltip;

    public ConditionListScreen(KineticScreen parent, IConditionOwner owner) {
        super(ColorText.translatable("gui.kineticarmory.predicate.list_title"));
        setParentScreen(parent);
        this.owner = owner;
        this.conditions = owner.getConditions();
    }

    private void updateModeUI(StateButton modeBtn) {
        boolean isMin = "MIN".equals(owner.getMatchMode());
        modeBtn.setWidth(isMin ? 60 : 90);
        if (minCountInput != null) {
            minCountInput.setVisible(isMin);
            if (isMin && minCountInput.getValue().isEmpty()) {
                minCountInput.setValue(String.valueOf(Math.max(1, owner.getMinCount())));
            }
        }

        if ("ALL".equals(owner.getMatchMode())) {
            modeBtn.setText(ColorText.translatable("gui.kineticarmory.predicate.mode.all"));
        } else if ("MIN".equals(owner.getMatchMode())) {
            modeBtn.setText(ColorText.translatable("gui.kineticarmory.predicate.mode.min_btn"));
        } else {
            modeBtn.setText(ColorText.translatable("gui.kineticarmory.predicate.mode.any"));
        }
    }

    @Override
    protected void buildUi() {
        int cx = canvasWidth() / 2;
        int cy = canvasHeight() / 2;
        int guiW = 400;
        int guiH = 220;
        int y0 = cy - guiH / 2;

        this.listWidget = new ConditionListWidget(guiW, guiH - 60, y0 + 30, y0 + guiH - 30, 24);
        this.listWidget.setLeftPos(cx - guiW / 2);
        addSmoothSelectionList(listWidget);

        int btnW = 90;
        int gap = 15;
        int startX = cx - (btnW * 3 + gap * 2) / 2;
        int bottomY = y0 + guiH - 25;

        addButtonWithHandler(startX, bottomY, btnW, ColorText.translatable("gui.kineticarmory.predicate.add"), null, b -> {
            ConditionData newCond = new ConditionData();
            KineticClientRuntime.openScreen(new ConditionEditScreen(this, conditions, newCond, true));
        });

        this.minCountInput = addTextField(startX + btnW + gap + 65, bottomY, 25, Component.empty());
        this.minCountInput.setValue(String.valueOf(owner.getMinCount()));
        this.minCountInput.setResponder(s -> {
            try {
                int val = Integer.parseInt(s);
                owner.setMinCount(Math.max(1, val));
            } catch (NumberFormatException ignored) {
            }
        });

        StateButton modeBtn = addButtonWithHandler(
                startX + btnW + gap,
                bottomY,
                btnW,
                Component.empty(),
                ColorText.translatable("gui.kineticarmory.predicate.mode.tooltip"),
                b -> {
                    String currentMode = owner.getMatchMode();
                    if ("ANY".equals(currentMode)) {
                        owner.setMatchMode("ALL");
                    } else if ("ALL".equals(currentMode)) {
                        owner.setMatchMode("MIN");
                    } else {
                        owner.setMatchMode("ANY");
                    }
                    updateModeUI(b);
                }
        );
        updateModeUI(modeBtn);

        addButtonWithHandler(
                startX + (btnW + gap) * 2,
                bottomY,
                btnW,
                ColorText.translatable("gui.kineticarmory.predicate.back"),
                null,
                b -> navigateBack()
        );
    }

    @Override
    protected void renderCanvasBackground(@NotNull GuiGraphics g, int mx, int my, float pt) {
        int cx = canvasWidth() / 2;
        int cy = canvasHeight() / 2;
        int guiW = 400;
        int guiH = 220;
        GuiTheme.panel(g, cx - guiW / 2 - 10, cy - guiH / 2 - 10, guiW + 20, guiH + 20);
        g.drawCenteredString(font, title, cx, cy - guiH / 2 + 5, 0xFFFFFF);
        renderSmoothSelectionList(listWidget, g, mx, my, pt);
        if (conditions.isEmpty()) {
            g.drawCenteredString(font, ColorText.translatable("gui.kineticarmory.predicate.empty"), cx, cy, 0xAAAAAA);
        }
    }

    @Override
    protected void renderCanvasForeground(@NotNull GuiGraphics g, int mx, int my, float pt) {
        if (delayedTooltip != null && !delayedTooltip.isEmpty()) {
            showTooltip(delayedTooltip, null);
            delayedTooltip = null;
        }
    }

    class ConditionListWidget extends SmoothSelectionList<ConditionListWidget.Entry> {
        public ConditionListWidget(int width, int height, int top, int bottom, int itemHeight) {
            super(width, height, top, bottom, itemHeight);
            setRenderBackground(false);
            setRenderTopAndBottom(false);
            refresh();
        }

        public void refresh() {
            clearEntries();
            for (ConditionData condition : conditions) {
                addEntry(new Entry(condition));
            }
        }

        @Override
        public int getRowWidth() {
            return this.width;
        }

        class Entry extends SmoothEntry<Entry> {
            private final ConditionData data;
            private final StateButton deleteButton;
            private final ToggleButton invertButton;

            public Entry(ConditionData data) {
                this.data = data;
                this.deleteButton = KineticWidgets.createCompactButton(
                        0,
                        0,
                        35,
                        ColorText.translatable("gui.kineticarmory.predicate.delete"),
                        null,
                        () -> {
                            conditions.remove(data);
                            refresh();
                        }
                );
                this.deleteButton.setError(true);
                this.invertButton = KineticWidgets.createCompactToggleButton(
                        0,
                        0,
                        35,
                        data.invert,
                        ColorText.translatable("gui.kineticarmory.predicate.invert.true"),
                        ColorText.translatable("gui.kineticarmory.predicate.invert.false"),
                        null,
                        value -> {
                            data.invert = value;
                            refresh();
                        }
                );
            }

            @Override
            public void render(
                    @Nonnull GuiGraphics g,
                    int index,
                    int top,
                    int left,
                    int width,
                    int height,
                    int mouseX,
                    int mouseY,
                    boolean hovered,
                    float partialTick
            ) {
                int contentHeight = height - 2;
                GuiTheme.stateSurface(
                        g,
                        left,
                        top,
                        width,
                        contentHeight,
                        index % 2 == 0 ? GuiTheme.Surface.PANEL : GuiTheme.Surface.PANEL_ALT,
                        false,
                        hovered,
                        false
                );

                String typeStr = (data.type == null || data.type.isEmpty()) ? "empty_type" : data.type.toLowerCase();
                String typeName = ConditionTypeUtil.getTranslatedName(typeStr);
                Component typeLine = data.invert
                        ? ColorText.translatable(
                                "gui.kineticarmory.predicate.list.type.inverted",
                                ColorText.translatable("gui.kineticarmory.predicate.invert.prefix"),
                                typeName
                        )
                        : ColorText.translatable("gui.kineticarmory.predicate.list.type", typeName);
                g.drawString(KineticClientRuntime.font(), typeLine, left + 5, top + 5, 0xFFFFFF);

                int deleteWidth = 35;
                int deleteX = left + width - deleteWidth - 5;
                int controlY = top + Math.max(0, (contentHeight - deleteButton.getHeight()) / 2);
                deleteButton.setX(deleteX);
                deleteButton.setY(controlY);
                deleteButton.setWidth(deleteWidth);
                KineticWidgets.renderControl(deleteButton, g, mouseX, mouseY, partialTick);

                int invertWidth = 35;
                int invertX = deleteX - invertWidth - 5;
                invertButton.setX(invertX);
                invertButton.setY(controlY);
                invertButton.setWidth(invertWidth);
                invertButton.setValue(data.invert);
                KineticWidgets.renderControl(invertButton, g, mouseX, mouseY, partialTick);

                if (invertButton.isMouseOver(mouseX, mouseY)) {
                    List<Component> tooltip = new ArrayList<>();
                    tooltip.add(ColorText.translatable("gui.kineticarmory.predicate.invert.tooltip.title"));
                    tooltip.add(ColorText.translatable("gui.kineticarmory.predicate.invert.tooltip.is"));
                    tooltip.add(ColorText.translatable("gui.kineticarmory.predicate.invert.tooltip.not"));
                    ConditionListScreen.this.delayedTooltip = tooltip;
                }
            }

            @Override
            public boolean mouseClicked(double mouseX, double mouseY, int button) {
                if (!KineticMouseButtons.isPrimary(button)) return false;

                if (deleteButton.mouseClicked(mouseX, mouseY, button)) return true;
                if (invertButton.mouseClicked(mouseX, mouseY, button)) return true;

                KineticClientRuntime.openScreen(new ConditionEditScreen(ConditionListScreen.this, conditions, data, false));
                return true;
            }

            @Override
            public @NotNull Component getNarration() {
                return Component.empty();
            }
        }
    }
}
