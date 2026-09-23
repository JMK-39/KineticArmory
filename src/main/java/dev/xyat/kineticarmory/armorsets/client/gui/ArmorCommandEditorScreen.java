package dev.xyat.kineticarmory.armorsets.client.gui;

import dev.xyat.kineticarmory.armorsets.data.ArmorDataConfig;
import dev.xyat.kineticarmory.armorsets.predicate.client.ConditionListScreen;
import dev.xyat.kineticcore.api.client.input.KineticMouseButtons;
import dev.xyat.kineticcore.api.client.command.KineticCommandSuggestions;
import dev.xyat.kineticcore.api.client.overlay.KineticOverlays;
import dev.xyat.kineticcore.api.client.screen.KineticScreen;
import dev.xyat.kineticcore.api.client.text.KineticText;
import dev.xyat.kineticcore.api.client.theme.GuiTheme;
import dev.xyat.kineticcore.api.client.widget.KineticWidgets;
import dev.xyat.kineticcore.api.client.widget.button.KineticButtons.StateButton;
import dev.xyat.kineticcore.api.client.widget.input.KineticTextFields.KineticEditBox;
import dev.xyat.kineticcore.api.client.widget.scroll.KineticScroll.SmoothEntry;
import dev.xyat.kineticcore.api.client.widget.scroll.KineticScroll.SmoothSelectionList;
import dev.xyat.kineticcore.api.runtime.KineticClientRuntime;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class ArmorCommandEditorScreen extends KineticScreen {
    private final ArmorDataConfig config;
    private KineticEditBox input;
    private KineticCommandSuggestions.Session commandSuggestions;
    private CommandListWidget activeList;
    private CommandListWidget deactiveList;

    private StateButton btnPrimary;
    private StateButton btnSecondary;

    private List<ArmorDataConfig.CommandData> editingTargetList;
    private int editingIndex = -1;
    private String tempInput;

    public ArmorCommandEditorScreen(KineticScreen parent, ArmorDataConfig config) {
        super(Component.translatable("gui.kineticarmory.armorsets.commands.title"));
        setParentScreen(parent);
        this.config = config;
    }

    @Override
    protected void canvasTick() {
        if (input != null) tempInput = input.getValue();
    }

    @Override
    protected void buildUi() {
        int cx = canvasWidth() / 2;
        int guiW = Math.min(canvasWidth() - 20, 800);
        int guiH = canvasHeight() - 55;
        int x0 = cx - guiW / 2;
        int y0 = 20;
        int inputY = y0 + guiH - 28;

        input = addTextField(x0 + 10, inputY, guiW - 160, Component.empty());
        input.setPlaceholder(Component.translatable("gui.kineticarmory.armorsets.commands.hint"));
        input.setCanLoseFocus(false);
        input.setMaxLength(2048);
        input.setValue(tempInput == null || tempInput.isBlank() ? "/" : tempInput);
        focusControl(input);

        btnPrimary = addButtonWithHandler(
                x0 + guiW - 145,
                inputY,
                65,
                Component.translatable("gui.kineticarmory.armorsets.commands.add_active"),
                null,
                b -> handlePrimaryAction()
        );
        btnSecondary = addButtonWithHandler(
                x0 + guiW - 75,
                inputY,
                65,
                Component.translatable("gui.kineticarmory.armorsets.commands.add_deactive"),
                null,
                b -> handleSecondaryAction()
        );
        updateButtonMode();

        int listW = guiW / 2 - 15;
        activeList = new CommandListWidget(listW, guiH - 60, y0 + 25, y0 + guiH - 35, 20, config.activationCommands);
        activeList.setLeftPos(x0 + 10);
        addSmoothSelectionList(activeList);

        deactiveList = new CommandListWidget(listW, guiH - 60, y0 + 25, y0 + guiH - 35, 20, config.deactivationCommands);
        deactiveList.setLeftPos(cx + 5);
        addSmoothSelectionList(deactiveList);

        int actionBtnW = 80;
        int bottomBtnY = canvasHeight() - 25;
        addButtonWithHandler(
                cx - actionBtnW - 5,
                bottomBtnY,
                actionBtnW,
                Component.translatable("gui.kineticarmory.armorsets.save"),
                null,
                b -> KineticOverlays.toast(Component.translatable("msg.kineticarmory.common.saved"))
        );
        addButtonWithHandler(
                cx + 5,
                bottomBtnY,
                actionBtnW,
                Component.translatable("gui.kineticarmory.armorsets.back"),
                null,
                b -> navigateBack()
        );

        commandSuggestions = KineticCommandSuggestions.create(
                input,
                canvasWidth(),
                canvasHeight(),
                KineticCommandSuggestions.Options.fieldAligned(false, false, 10, 0xD0000000)
        );
        commandSuggestions.setAllowSuggestions(true);
        input.setResponder(value -> commandSuggestions.update());
        commandSuggestions.update();
    }

    public void startEdit(List<ArmorDataConfig.CommandData> targetList, int index, String text) {
        editingTargetList = targetList;
        editingIndex = index;
        input.setValue(text);
        focusControl(input);
        updateButtonMode();
    }

    private void cancelEdit() {
        editingTargetList = null;
        editingIndex = -1;
        input.setValue("/");
        focusControl(input);
        updateButtonMode();
    }

    private void updateButtonMode() {
        boolean editing = editingTargetList != null;
        if (btnPrimary != null) {
            btnPrimary.setText(Component.translatable(editing
                    ? "gui.kineticarmory.armorsets.commands.save_edit"
                    : "gui.kineticarmory.armorsets.commands.add_active"));
        }
        if (btnSecondary != null) {
            btnSecondary.setText(Component.translatable(editing
                    ? "gui.kineticarmory.armorsets.commands.cancel_edit"
                    : "gui.kineticarmory.armorsets.commands.add_deactive"));
        }
    }

    private void handlePrimaryAction() {
        if (editingTargetList != null) {
            if (editingIndex >= 0 && editingIndex < editingTargetList.size()) {
                editingTargetList.get(editingIndex).command = input.getValue();
                cancelEdit();
                activeList.refresh();
                deactiveList.refresh();
                KineticOverlays.toast(Component.translatable("msg.kineticarmory.common.saved"));
            }
            return;
        }
        addCommand(config.activationCommands, activeList);
    }

    private void handleSecondaryAction() {
        if (editingTargetList != null) {
            cancelEdit();
            return;
        }
        addCommand(config.deactivationCommands, deactiveList);
    }

    private void addCommand(List<ArmorDataConfig.CommandData> targetList, CommandListWidget targetWidget) {
        if (input.getValue().trim().isEmpty()) return;
        ArmorDataConfig.CommandData data = new ArmorDataConfig.CommandData();
        data.command = input.getValue();
        targetList.add(data);
        input.setValue("/");
        focusControl(input);
        targetWidget.refresh();
    }

    @Override
    protected boolean canvasKeyPressed(int keyCode, int scanCode, int modifiers) {
        return commandSuggestions != null && commandSuggestions.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    protected boolean canvasMouseClicked(double mouseX, double mouseY, int button) {
        if (commandSuggestions != null && commandSuggestions.mouseClicked(mouseX, mouseY, button)) return true;
        return super.canvasMouseClicked(mouseX, mouseY, button);
    }

    @Override
    protected boolean canvasMouseScrolled(double mouseX, double mouseY, double delta) {
        if (commandSuggestions != null && commandSuggestions.mouseScrolled(Mth.clamp(delta, -1.0D, 1.0D))) return true;
        return super.canvasMouseScrolled(mouseX, mouseY, delta);
    }

    @Override
    protected void renderCanvasBackground(@NotNull GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        int cx = canvasWidth() / 2;
        int guiW = Math.min(canvasWidth() - 20, 800);
        int guiH = canvasHeight() - 55;
        int x0 = cx - guiW / 2;
        int y0 = 20;

        GuiTheme.panel(graphics, x0, y0, guiW, guiH);
        graphics.drawCenteredString(font, title, cx, 5, 0xFFFFFF);
        graphics.drawCenteredString(font, Component.translatable("gui.kineticarmory.armorsets.commands.activation_label"), x0 + guiW / 4, y0 + 10, 0xFFFFFF);
        graphics.drawCenteredString(font, Component.translatable("gui.kineticarmory.armorsets.commands.deactivation_label"), x0 + guiW * 3 / 4, y0 + 10, 0xFFFFFF);
        GuiTheme.verticalSeparator(graphics, cx, y0 + 25, guiH - 60);

        renderSmoothSelectionList(activeList, graphics, mouseX, mouseY, partialTick);
        renderSmoothSelectionList(deactiveList, graphics, mouseX, mouseY, partialTick);
    }

    @Override
    protected void renderCanvasForeground(@NotNull GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        if (commandSuggestions != null) {
            commandSuggestions.render(graphics, mouseX, mouseY);
        }
    }

    class CommandListWidget extends SmoothSelectionList<CommandListWidget.Entry> {
        private final List<ArmorDataConfig.CommandData> commandList;

        public CommandListWidget(
                int width,
                int height,
                int top,
                int bottom,
                int itemHeight,
                List<ArmorDataConfig.CommandData> commandList
        ) {
            super(width, height, top, bottom, itemHeight);
            this.commandList = commandList;
            setRenderBackground(false);
            setRenderTopAndBottom(false);
            refresh();
        }

        public void refresh() {
            clearEntries();
            for (int i = 0; i < commandList.size(); i++) {
                addEntry(new Entry(i, commandList.get(i).command, commandList));
            }
        }

        @Override
        public int getRowWidth() {
            return width;
        }

        class Entry extends SmoothEntry<Entry> {
            private static final int ROW_BUTTON_W = 42;
            private static final int ROW_BUTTON_H = 18;
            private static final int ROW_BUTTON_GAP = 2;

            private final int index;
            private final String text;
            private final List<ArmorDataConfig.CommandData> targetList;
            private final StateButton deleteButton;
            private final StateButton conditionsButton;
            private int lastTop;

            public Entry(int index, String text, List<ArmorDataConfig.CommandData> targetList) {
                this.index = index;
                this.text = text;
                this.targetList = targetList;
                deleteButton = KineticWidgets.createCompactButton(
                        0,
                        0,
                        ROW_BUTTON_W,
                        Component.translatable("gui.kineticarmory.armorsets.delete"),
                        null,
                        this::deleteEntry
                );
                conditionsButton = KineticWidgets.createCompactButton(
                        0,
                        0,
                        ROW_BUTTON_W,
                        Component.translatable("gui.kineticarmory.armorsets.conditions"),
                        null,
                        this::openConditions
                );
            }

            @Override
            public void render(
                    @NotNull GuiGraphics graphics,
                    int rowIndex,
                    int top,
                    int left,
                    int rowWidth,
                    int rowHeight,
                    int mouseX,
                    int mouseY,
                    boolean hovered,
                    float partialTick
            ) {
                lastTop = top;
                boolean selected = editingTargetList == targetList && editingIndex == index;
                GuiTheme.stateSurface(
                        graphics,
                        left,
                        top,
                        rowWidth,
                        rowHeight - 2,
                        GuiTheme.Surface.PANEL_ALT,
                        selected,
                        hovered,
                        false
                );

                int deleteX = left + rowWidth - ROW_BUTTON_W - 5;
                int conditionsX = deleteX - ROW_BUTTON_W - ROW_BUTTON_GAP;
                int maxWidth = Math.max(0, conditionsX - left - 8);
                KineticText.drawScrollingLeft(
                        graphics,
                        KineticClientRuntime.font(),
                        Component.literal(text),
                        left + 4,
                        top + 5,
                        maxWidth,
                        0xFFFFFF,
                        false
                );

                conditionsButton.setX(conditionsX);
                conditionsButton.setY(top);
                conditionsButton.render(graphics, mouseX, mouseY, partialTick);
                deleteButton.setX(deleteX);
                deleteButton.setY(top);
                deleteButton.render(graphics, mouseX, mouseY, partialTick);
            }

            private void deleteEntry() {
                if (index < 0 || index >= targetList.size()) return;
                targetList.remove(index);
                if (editingTargetList == targetList && editingIndex == index) cancelEdit();
                refresh();
            }

            private void openConditions() {
                if (index < 0 || index >= targetList.size()) return;
                if (targetList.get(index).conditions == null) targetList.get(index).conditions = new java.util.ArrayList<>();
                KineticClientRuntime.openScreen(new ConditionListScreen(ArmorCommandEditorScreen.this, targetList.get(index)));
            }

            @Override
            public boolean mouseClicked(double mouseX, double mouseY, int button) {
                if (mouseY < lastTop || mouseY >= lastTop + ROW_BUTTON_H) return false;
                if (deleteButton.mouseClicked(mouseX, mouseY, button)) return true;
                if (conditionsButton.mouseClicked(mouseX, mouseY, button)) return true;
                if (KineticMouseButtons.isPrimary(button)) {
                    ArmorCommandEditorScreen.this.startEdit(targetList, index, text);
                    return true;
                }
                return false;
            }

            @Override
            public @NotNull Component getNarration() {
                return Component.empty();
            }
        }
    }
}
