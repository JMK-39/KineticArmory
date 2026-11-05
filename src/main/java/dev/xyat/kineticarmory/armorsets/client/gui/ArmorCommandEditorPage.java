package dev.xyat.kineticarmory.armorsets.client.gui;

import dev.xyat.kineticcore.api.text.KineticI18n;
import dev.xyat.kineticcore.api.client.gui.command.KineticCommandAssist;
import dev.xyat.kineticcore.api.client.gui.input.KeyInput;
import dev.xyat.kineticcore.api.client.gui.input.ScrollInput;
import dev.xyat.kineticcore.api.client.gui.input.MouseInput;
import dev.xyat.kineticcore.api.client.gui.overlay.KineticOverlays;
import dev.xyat.kineticcore.api.client.gui.page.KineticPage;
import dev.xyat.kineticcore.api.client.gui.render.KineticGraphics;
import dev.xyat.kineticcore.api.client.gui.theme.KineticTheme;
import dev.xyat.kineticcore.api.client.gui.ui.KineticUi;
import dev.xyat.kineticcore.api.client.gui.widget.*;
import dev.xyat.kineticcore.api.client.gui.widget.list.*;

import dev.xyat.kineticarmory.armorsets.data.ArmorDataConfig;
import dev.xyat.kineticarmory.armorsets.predicate.client.ConditionListPage;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;

public class ArmorCommandEditorPage extends KineticPage {
    private final ArmorDataConfig config;
    private KineticTextField input;
    private KineticCommandAssist commandSuggestions;
    private CommandListWidget activeList;
    private CommandListWidget deactiveList;
    private CommandListWidget selectedCommandList;

    private KineticButton btnPrimary;
    private KineticButton btnSecondary;

    private List<ArmorDataConfig.CommandData> editingTargetList;
    private int editingIndex = -1;
    private String tempInput;

    public ArmorCommandEditorPage(ArmorDataConfig config) {
        super(KineticI18n.translatable("gui.kineticarmory.armorsets.commands.title"));
        this.config = config;
        useCanvas(CANVAS_WIDTH, CANVAS_HEIGHT, 4);
        bindDeleteAction(() -> selectedCommandList != null && selectedCommandList.selectedItem() != null,
                () -> selectedCommandList.deleteEntry(selectedCommandList.selectedItem().index()));
    }

    @Override
    protected void onTick() {
        if (input != null) tempInput = input.textValue();

    }

    @Override
    protected void build(KineticUi ui) {
        int cx = width() / 2;
        int guiW = width() - 8;
        int guiH = height() - 28;
        int x0 = cx - guiW / 2;
        int y0 = 24;
        int inputY = 28;

        input = ui().textField(x0, inputY, guiW - 138).firstShownTextAsDefault().build();
        input.setPlaceholder(KineticI18n.translatable("gui.kineticarmory.armorsets.commands.hint"));
        input.limitTextLength(2048);
        input.setTextValue(tempInput == null || tempInput.isBlank() ? "/" : tempInput);
        focus(input);

        btnPrimary = ui().button(x0 + guiW - 134, inputY, 65).text(KineticI18n.translatable("gui.kineticarmory.armorsets.commands.add_active")).onClick(b -> handlePrimaryAction()).build();
        btnSecondary = ui().button(x0 + guiW - 65, inputY, 65).text(KineticI18n.translatable("gui.kineticarmory.armorsets.commands.add_deactive")).onClick(b -> handleSecondaryAction()).build();
        updateButtonMode();

        int listW = (guiW - 4) / 2;
        activeList = ui().add(new CommandListWidget(x0, 66, listW, height() - 70, config.activationCommands));
        deactiveList = ui().add(new CommandListWidget(cx + 2, 66, listW, height() - 70, config.deactivationCommands));

        int actionBtnW = 80;
        int bottomBtnY = height() - 25;
        ui().button(x0 + guiW - 40, 4, 40).text(KineticI18n.translatable("gui.kineticarmory.armorsets.save")).onClick(b -> KineticOverlays.toast(KineticI18n.translatable("msg.kineticarmory.common.saved"))).build();
        ui().button(x0, 4, 40).text(KineticI18n.translatable("gui.kineticarmory.armorsets.back")).onClick(b -> navigateBack()).build();

        commandSuggestions = KineticCommandAssist.attach(input, width(), height(), false, 10, null);
    }

    public void startEdit(List<ArmorDataConfig.CommandData> targetList, int index, String text) {
        editingTargetList = targetList;
        editingIndex = index;
        input.setTextValue(text);
        focus(input);
        updateButtonMode();
    }

    private void cancelEdit() {
        editingTargetList = null;
        editingIndex = -1;
        input.setTextValue("/");
        focus(input);
        updateButtonMode();
    }

    private void updateButtonMode() {
        boolean editing = editingTargetList != null;
        if (btnPrimary != null) {
            btnPrimary.setText(KineticI18n.translatable(editing
                    ? "gui.kineticarmory.armorsets.commands.save_edit"
                    : "gui.kineticarmory.armorsets.commands.add_active"));
        }
        if (btnSecondary != null) {
            btnSecondary.setText(KineticI18n.translatable(editing
                    ? "gui.kineticarmory.armorsets.commands.cancel_edit"
                    : "gui.kineticarmory.armorsets.commands.add_deactive"));
        }
    }

    private void handlePrimaryAction() {
        if (editingTargetList != null) {
            if (editingIndex >= 0 && editingIndex < editingTargetList.size()) {
                editingTargetList.get(editingIndex).command = input.textValue();
                cancelEdit();
                activeList.refresh();
                deactiveList.refresh();
                KineticOverlays.toast(KineticI18n.translatable("msg.kineticarmory.common.saved"));
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
        if (input.textValue().trim().isEmpty()) return;
        ArmorDataConfig.CommandData data = new ArmorDataConfig.CommandData();
        data.command = input.textValue();
        targetList.add(data);
        input.setTextValue("/");
        focus(input);
        targetWidget.refresh();
    }

    @Override
    protected boolean onKeyPress(KeyInput input) {
        return commandSuggestions != null && commandSuggestions.keyPress(input);
    }

    @Override
    protected boolean onMouseClickCapture(MouseInput input) {
        return commandSuggestions != null && commandSuggestions.mouseClick(input);
    }

    @Override
    protected boolean onMouseScroll(ScrollInput input) {
        return commandSuggestions != null && commandSuggestions.mouseScroll(input);
    }

    @Override
    protected void renderBackground(KineticGraphics graphics, int mouseX, int mouseY, float partialTick) {
        int cx = width() / 2;
        int guiW = width() - 8;
        int guiH = height() - 28;
        int x0 = cx - guiW / 2;
        int y0 = 24;

        // The outer frame holds the header (Go Back, title) and Save; the inner one holds the two command lists.
        KineticTheme.panel(graphics, 0, 0, width(), height());
        int titleLeft = 48;
        int titleRight = x0 + guiW - 44;
        graphics.scrollingTextCentered(title(), (titleLeft + titleRight) / 2, 7,
                Math.max(0, titleRight - titleLeft), 0xFFFFFF, true);
        graphics.scrollingTextCentered(KineticI18n.translatable("gui.kineticarmory.armorsets.commands.activation_label"), x0 + guiW / 4, 54, guiW / 2 - 20, 0xFFFFFF, true);
        graphics.scrollingTextCentered(KineticI18n.translatable("gui.kineticarmory.armorsets.commands.deactivation_label"), x0 + guiW * 3 / 4, 54, guiW / 2 - 20, 0xFFFFFF, true);
        KineticTheme.verticalSeparator(graphics, cx, 66, height() - 70);
    }

    @Override
    protected void renderForeground(KineticGraphics graphics, int mouseX, int mouseY, float partialTick) {
        if (commandSuggestions != null) {
            commandSuggestions.render(graphics, mouseX, mouseY);
        }
    }

    /** 一行命令 / One command row. */
    private record CommandRow(int index, String text) {
    }

    /**
     * 命令列表：左键编辑，行内「条件」「删除」按钮。
     * Command list: left click edits; inline "conditions" and "delete" buttons.
     */
    class CommandListWidget extends KineticRowList<CommandRow> {
        // The row frame is ROW_H - 2 tall; its 16 px buttons sit 3 px inside it, clear of both frame lines.
        private static final int ROW_H = 24;
        private static final int ROW_BUTTON_Y = 3;
        private static final int ROW_BUTTON_W = 42;
        private static final int DELETE_BUTTON_W = 40;
        private static final int ROW_BUTTON_GAP = 2;

        private final List<ArmorDataConfig.CommandData> commandList;

        CommandListWidget(int x, int y, int width, int height, List<ArmorDataConfig.CommandData> commandList) {
            super(x, y, width, height, ROW_H);
            this.commandList = commandList;
            refresh();
        }

        public void refresh() {
            List<CommandRow> rows = new ArrayList<>();
            for (int i = 0; i < commandList.size(); i++) {
                rows.add(new CommandRow(i, commandList.get(i).command));
            }
            setItems(rows);
        }

        private int deleteX(int left, int rowWidth) {
            return left + rowWidth - DELETE_BUTTON_W - 5;
        }

        private int conditionsX(int left, int rowWidth) {
            return deleteX(left, rowWidth) - ROW_BUTTON_W - ROW_BUTTON_GAP;
        }

        private boolean over(int x, int y, int width) {
            return mouseX() >= x && mouseX() < x + width && mouseY() >= y && mouseY() < y + KineticPage.CONTROL_HEIGHT;
        }

        @Override
        protected void renderRowBackground(KineticGraphics graphics, int index, int x, int y, int width, int height,
                                           boolean hovered, boolean selected) {
            boolean editing = editingTargetList == commandList && editingIndex == index;
            KineticTheme.stateSurface(graphics, x, y, width, height - 2, KineticTheme.Surface.PANEL_ALT, editing, hovered, false);
        }

        @Override
        protected void renderRow(KineticGraphics graphics, CommandRow row, int index, int left, int top, int rowWidth,
                                 int rowHeight, boolean hovered, boolean selected) {
            int deleteX = deleteX(left, rowWidth);
            int conditionsX = conditionsX(left, rowWidth);
            int maxWidth = Math.max(0, conditionsX - left - 8);
            graphics.scrollingText(Component.literal(row.text()), left + 4, top + (rowHeight - 2 - 8) / 2, maxWidth, 0xFFFFFF, false);
            int buttonY = top + ROW_BUTTON_Y;

            KineticTheme.button(graphics, conditionsX, buttonY, ROW_BUTTON_W, KineticPage.CONTROL_HEIGHT,
                    KineticI18n.translatable("gui.kineticarmory.armorsets.conditions"), over(conditionsX, buttonY, ROW_BUTTON_W), true, false);
            KineticTheme.button(graphics, deleteX, buttonY, DELETE_BUTTON_W, KineticPage.CONTROL_HEIGHT,
                    KineticI18n.translatable("gui.kineticarmory.armorsets.delete"), over(deleteX, buttonY, DELETE_BUTTON_W), true, false);
        }

        private void deleteEntry(int index) {
            if (index < 0 || index >= commandList.size()) return;
            commandList.remove(index);
            if (editingTargetList == commandList && editingIndex == index) cancelEdit();
            refresh();
        }

        private void openConditions(int index) {
            if (index < 0 || index >= commandList.size()) return;
            if (commandList.get(index).conditions == null) commandList.get(index).conditions = new ArrayList<>();
            openChild(new ConditionListPage(commandList.get(index)));
        }

        @Override
        protected boolean onRowClick(CommandRow row, int index, MouseInput input) {
            int top = rowTop(index);
            if (input.y() < top || input.y() >= top + ROW_H - 2) return false;
            int buttonY = top + ROW_BUTTON_Y;
            if (!input.isLeft()) return false;
            selectedCommandList = this;setSelectedIndex(index);
            int left = controlX();
            int rowWidth = rowsWidth();
            if (input.inside(deleteX(left, rowWidth), buttonY, DELETE_BUTTON_W, KineticPage.CONTROL_HEIGHT)) {
                deleteEntry(row.index());
                return true;
            }
            if (input.inside(conditionsX(left, rowWidth), buttonY, ROW_BUTTON_W, KineticPage.CONTROL_HEIGHT)) {
                openConditions(row.index());
                return true;
            }
            ArmorCommandEditorPage.this.startEdit(commandList, row.index(), row.text());
            return true;
        }
    }
}
