package dev.xyat.kineticarmory.armorsets.client.gui;

import dev.xyat.kineticcore.api.client.text.KineticText;
import dev.xyat.kineticarmory.armorsets.data.ArmorDataConfig;
import dev.xyat.kineticarmory.armorsets.predicate.client.ConditionListScreen;
import dev.xyat.kineticcore.api.client.theme.GuiTheme;
import dev.xyat.kineticcore.api.client.overlay.GuiOverlay;
import dev.xyat.kineticcore.api.client.screen.KineticScreen;
import dev.xyat.kineticcore.api.client.widget.KineticWidgets;
import dev.xyat.kineticcore.api.client.widget.KineticWidgets.Scroll;
import dev.xyat.kineticcore.api.client.widget.KineticWidgets.SmoothSelectionList;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.CommandSuggestions;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.ObjectSelectionList;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class ArmorCommandEditorScreen extends KineticScreen {
    private final KineticScreen parent;
    private final ArmorDataConfig config;
    private EditBox input;
    private CommandSuggestions commandSuggestions;
    private CommandListWidget activeList;
    private CommandListWidget deactiveList;

    private Button btnAddActive;
    private Button btnAddDeactive;
    private Button btnSaveEdit;
    private Button btnCancelEdit;

    private List<ArmorDataConfig.CommandData> editingTargetList = null;
    private int editingIndex = -1;
    private String tempInput = null;

    public ArmorCommandEditorScreen(KineticScreen parent, ArmorDataConfig config) {
        super(Component.translatable("gui.kineticarmory.armorsets.commands.title"));
        this.parent = parent;
        this.config = config;
        useResponsiveCanvas(
                640f,
                360f,
                6
        );
    }

    @Override
    public void tick() {
        super.tick();
        if (input != null) tempInput = input.getValue();
    }

    @Override
    protected void buildUi() {
        int cx = this.canvasWidth() / 2;
        int guiW = Math.min(this.canvasWidth() - 20, 800);
        int guiH = this.canvasHeight() - 55;
        int x0 = cx - guiW / 2;
        int y0 = 20;

        int inputY = y0 + guiH - 28;

        this.input = addTextField(
                x0 + 10,
                inputY,
                guiW - 160,
                Component.empty()
        );
        this.input.setMaxLength(2048);
        if (tempInput != null) this.input.setValue(tempInput);
        this.input.setResponder(this::onEdited);
this.btnAddActive = addButton(x0 + guiW - 145, inputY, 65, Component.translatable("gui.kineticarmory.armorsets.commands.add_active"), null, b -> {
            if (!input.getValue().trim().isEmpty()) {
                ArmorDataConfig.CommandData cd = new ArmorDataConfig.CommandData();
                cd.command = input.getValue();
                config.activationCommands.add(cd);
                input.setValue(""); activeList.refresh();
            }
        });
this.btnAddDeactive = addButton(x0 + guiW - 75, inputY, 65, Component.translatable("gui.kineticarmory.armorsets.commands.add_deactive"), null, b -> {
            if (!input.getValue().trim().isEmpty()) {
                ArmorDataConfig.CommandData cd = new ArmorDataConfig.CommandData();
                cd.command = input.getValue();
                config.deactivationCommands.add(cd);
                input.setValue(""); deactiveList.refresh();
            }
        });
this.btnSaveEdit = addButton(x0 + guiW - 145, inputY, 65, Component.translatable("gui.kineticarmory.armorsets.commands.save_edit"), null, b -> {
            if (editingTargetList != null && editingIndex >= 0 && editingIndex < editingTargetList.size()) {
                editingTargetList.get(editingIndex).command = input.getValue();
                cancelEdit();
                activeList.refresh();
                deactiveList.refresh();
                GuiOverlay.toast(Component.translatable("msg.kineticarmory.common.saved"));
            }
        });
this.btnCancelEdit = addButton(x0 + guiW - 75, inputY, 65, Component.translatable("gui.kineticarmory.armorsets.commands.cancel_edit"), null, b -> cancelEdit());
updateButtonVisibility();

        int listW = guiW / 2 - 15;
        this.activeList = new CommandListWidget(this.minecraft, listW, guiH - 60, y0 + 25, y0 + guiH - 35, 20, config.activationCommands);
        this.activeList.setLeftPos(x0 + 10);
        this.addEventListWidget(activeList);

        this.deactiveList = new CommandListWidget(this.minecraft, listW, guiH - 60, y0 + 25, y0 + guiH - 35, 20, config.deactivationCommands);
        this.deactiveList.setLeftPos(cx + 5);
        this.addEventListWidget(deactiveList);

        int actionBtnW = 80;
        int bottomBtnY = this.canvasHeight() - 25;

        addButton(cx - actionBtnW - 5, bottomBtnY, actionBtnW, Component.translatable("gui.kineticarmory.armorsets.save"), null, b -> {
            GuiOverlay.toast(Component.translatable("msg.kineticarmory.common.saved"));
            if (minecraft != null) navigateBack();
        });

        addButton(cx + 5, bottomBtnY, actionBtnW, Component.translatable("gui.kineticarmory.armorsets.back"), null, b -> {
            if (minecraft != null) navigateBack();
        });

        Screen dummyScreen = new Screen(Component.empty()) {};
        if (this.minecraft != null) {
            dummyScreen.init(this.minecraft, this.canvasWidth(), inputY + 12);
        }

        if (this.minecraft != null) {
            this.commandSuggestions = new CommandSuggestions(this.minecraft, dummyScreen, this.input, this.font, false, true, 0, 7, true, Integer.MIN_VALUE);
            this.commandSuggestions.setAllowSuggestions(true);
            this.commandSuggestions.updateCommandInfo();
        }
    }

    public void startEdit(List<ArmorDataConfig.CommandData> targetList, int index, String text) {
        this.editingTargetList = targetList;
        this.editingIndex = index;
        this.input.setValue(text);
        this.input.setFocused(true);
        this.setFocused(this.input);
        updateButtonVisibility();
    }

    private void cancelEdit() {
        this.editingTargetList = null;
        this.editingIndex = -1;
        this.input.setValue("");
        updateButtonVisibility();
    }

    private void updateButtonVisibility() {
        boolean editing = this.editingTargetList != null;
        if (this.btnAddActive != null) this.btnAddActive.visible = !editing;
        if (this.btnAddDeactive != null) this.btnAddDeactive.visible = !editing;
        if (this.btnSaveEdit != null) this.btnSaveEdit.visible = editing;
        if (this.btnCancelEdit != null) this.btnCancelEdit.visible = editing;
    }

    private void onEdited(String text) {
        if (this.commandSuggestions != null) {
            this.commandSuggestions.setAllowSuggestions(true);
            this.commandSuggestions.updateCommandInfo();
        }
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (this.commandSuggestions != null && this.commandSuggestions.keyPressed(keyCode, scanCode, modifiers)) {
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    protected boolean canvasMouseClicked(double mx, double my, int btn) {
        if (this.commandSuggestions != null && this.commandSuggestions.mouseClicked(mx, my, btn)) {
            return true;
        }
        return super.canvasMouseClicked(mx, my, btn);
    }

    @Override
    protected void renderCanvasForeground(@NotNull GuiGraphics g, int mx, int my, float pt) {
        renderTextFieldPlaceholder(
                g,
                input,
                Component.translatable("gui.kineticarmory.armorsets.commands.hint")
        );
    }

    @Override
    protected boolean canvasMouseScrolled(double mx, double my, double d) {
        if (this.commandSuggestions != null && this.commandSuggestions.mouseScrolled(Mth.clamp(d, -1.0, 1.0))) {
            return true;
        }
        return super.canvasMouseScrolled(mx, my, d);
    }

    @Override
    protected void renderCanvasBackground(@NotNull GuiGraphics g, int mx, int my, float pt) {
        int cx = this.canvasWidth() / 2;
        int guiW = Math.min(this.canvasWidth() - 20, 800);
        int guiH = this.canvasHeight() - 55;
        int x0 = cx - guiW / 2;
        int y0 = 20;

        GuiTheme.panel(g, x0, y0, guiW, guiH);
        g.drawCenteredString(this.font, this.title, cx, 5, 0xFFFFFF);

        g.drawCenteredString(this.font, Component.translatable("gui.kineticarmory.armorsets.commands.activation_label"), x0 + guiW/4, y0 + 10, 0xFFFFFF);
        g.drawCenteredString(this.font, Component.translatable("gui.kineticarmory.armorsets.commands.deactivation_label"), x0 + guiW*3/4, y0 + 10, 0xFFFFFF);

        g.fill(cx - 1, y0 + 25, cx + 1, y0 + guiH - 35, 0xFF555555);

        renderScaledList(activeList, g, mx, my, pt);
        renderScaledList(deactiveList, g, mx, my, pt);

        if (this.commandSuggestions != null) {
            g.pose().pushPose();
            g.pose().translate(0, 0, 400);
            this.commandSuggestions.render(g, mx, my);
            g.pose().popPose();
        }
    }

    class CommandListWidget extends SmoothSelectionList<CommandListWidget.Entry> {
        private final int listTop, listBottom;
        private final List<ArmorDataConfig.CommandData> commandList;

        public CommandListWidget(Minecraft mc, int w, int h, int t, int b, int ih, List<ArmorDataConfig.CommandData> commandList) {
            super(mc, w, h, t, b, ih); this.listTop = t; this.listBottom = b; this.commandList = commandList;
            setRenderBackground(false); setRenderTopAndBottom(false); refresh();
        }

        @Override
        public void render(@NotNull GuiGraphics g, int mx, int my, float pt) {
            super.render(g, mx, my, pt);
            if (this.getMaxScroll() > 0) {
                int barX = this.getScrollbarPosition();
                int height = listBottom - listTop;
                int thumbH = Math.max(20, (int) ((float) height * height / this.getMaxPosition()));
                Scroll.renderScrollbar(
                        g,
                        mx,
                        my,
                        barX + 2,
                        listTop,
                        4,
                        height,
                        thumbH,
                        (int) Math.ceil(this.getMaxScroll()),
                        this.getScrollAmount(),
                        false
                );
            }
        }
        public void refresh() { clearEntries(); for (int i = 0; i < commandList.size(); i++) addEntry(new Entry(i, commandList.get(i).command, commandList)); }
        @Override public int getRowWidth() { return this.width; }
        @Override protected int getScrollbarPosition() { return this.x0 + this.width - 6; }

        class Entry extends ObjectSelectionList.Entry<Entry> {
            private static final int ROW_BUTTON_W = 42;
            private static final int ROW_BUTTON_H = 18;
            private static final int ROW_BUTTON_GAP = 2;

            private final int index;
            private final String text;
            private final List<ArmorDataConfig.CommandData> targetList;
            private final Button deleteButton;
            private final Button conditionsButton;
            private int lastTop;

            public Entry(int index, String text, List<ArmorDataConfig.CommandData> targetList) {
                this.index = index;
                this.text = text;
                this.targetList = targetList;
                this.deleteButton = KineticWidgets.createCompactButton(0, 0, ROW_BUTTON_W, Component.translatable("gui.kineticarmory.armorsets.delete"), null, b -> deleteEntry());
                this.conditionsButton = KineticWidgets.createCompactButton(0, 0, ROW_BUTTON_W, Component.translatable("gui.kineticarmory.armorsets.conditions"), null, b -> openConditions());
            }

            @Override
            public void render(@NotNull GuiGraphics g, int index, int t, int l, int w, int h, int mx, int my, boolean hv, float pt) {
                this.lastTop = t;
                int bgColor = hv ? 0x88777777 : ((index % 2 == 0) ? 0x88444444 : 0x88222222);
                if (editingTargetList == targetList && editingIndex == this.index) {
                    bgColor = 0xAA228822;
                }
                g.fill(l, t, l + w, t + h - 2, bgColor);
                g.renderOutline(l, t, w, h - 2, 0xFF555555);

                int deleteX = l + w - ROW_BUTTON_W - 5;
                int conditionsX = deleteX - ROW_BUTTON_W - ROW_BUTTON_GAP;
                int maxW = Math.max(0, conditionsX - l - 8);
                KineticText.drawScrollingLeft(
                        g, Minecraft.getInstance().font, text, l + 4, t + 5, maxW, 0xFFFFFF, false
                );

                conditionsButton.setX(conditionsX);
                conditionsButton.setY(t);
                conditionsButton.render(g, mx, my, pt);

                deleteButton.setX(deleteX);
                deleteButton.setY(t);
                deleteButton.render(g, mx, my, pt);
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
                if (ArmorCommandEditorScreen.this.minecraft != null) {
                    ArmorCommandEditorScreen.this.minecraft.setScreen(new ConditionListScreen(ArmorCommandEditorScreen.this, targetList.get(index)));
                }
            }

            @Override
            public boolean mouseClicked(double mx, double my, int btn) {
                if (my < lastTop || my >= lastTop + ROW_BUTTON_H) return false;
                if (deleteButton.mouseClicked(mx, my, btn)) return true;
                if (conditionsButton.mouseClicked(mx, my, btn)) return true;
                if (btn == 0) {
                    ArmorCommandEditorScreen.this.startEdit(this.targetList, this.index, this.text);
                    return true;
                }
                return false;
            }

            @Override public @NotNull Component getNarration() { return Component.empty(); }
        }
    }
}
