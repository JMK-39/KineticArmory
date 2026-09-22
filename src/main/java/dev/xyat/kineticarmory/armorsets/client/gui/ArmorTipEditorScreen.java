package dev.xyat.kineticarmory.armorsets.client.gui;

import dev.xyat.kineticarmory.armorsets.client.ArmorCache;
import dev.xyat.kineticarmory.armorsets.data.ArmorDataConfig;
import dev.xyat.kineticarmory.armorsets.data.ArmorTipGenerator;
import dev.xyat.kineticcore.api.client.input.KineticMouseButtons;
import dev.xyat.kineticcore.api.client.text.KineticText;
import dev.xyat.kineticcore.api.client.theme.GuiTheme;
import dev.xyat.kineticcore.api.client.overlay.KineticOverlays;
import dev.xyat.kineticcore.api.client.screen.KineticScreen;
import dev.xyat.kineticcore.api.client.widget.KineticWidgets;
import dev.xyat.kineticcore.api.client.widget.button.KineticButtons.StateButton;
import dev.xyat.kineticcore.api.client.widget.input.KineticTextFields.KineticEditBox;
import dev.xyat.kineticcore.api.client.widget.scroll.KineticScroll.SmoothEntry;
import dev.xyat.kineticcore.api.client.widget.scroll.KineticScroll.SmoothSelectionList;
import dev.xyat.kineticcore.api.registry.KineticRegistries;
import dev.xyat.kineticcore.api.resource.KineticResourceIds;
import dev.xyat.kineticcore.api.runtime.KineticClientRuntime;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ArmorTipEditorScreen extends KineticScreen {
    private final KineticScreen parent;
    private final ArmorDataConfig config;
    private final List<TipRow> displayRows = new ArrayList<>();
    private TipListWidget listWidget;
    private KineticEditBox input;

    private int editingIndex = -1;
    private int draggingIndex = -1;
    private String draggingText = null;
    private int hoverTargetIndex = -1;
    private String tempInput = null;

    private StateButton btnAdd;
    private StateButton btnModify;
    private StateButton btnCancel;

    private static final int[] COLORS = { 0x000000, 0x0000AA, 0x00AA00, 0x00AAAA, 0xAA0000, 0xAA00AA, 0xFFAA00, 0x5555FF, 0x55FF55, 0x55FFFF, 0xFF5555, 0xFF55FF, 0xFFFF55, 0xFFFFFF };
    private static final String[] CODES = {"0", "1", "2", "3", "4", "5", "6", "9", "a", "b", "c", "d", "e", "f"};

    public ArmorTipEditorScreen(KineticScreen parent, ArmorDataConfig config) {
        super(Component.translatable("gui.kineticarmory.armorsets.btn_edit_tips"));
        setParentScreen(parent);
        this.parent = parent;
        this.config = config;
        this.config.initNullFields();
        ensureManualTipLayout();
    }

    @Override
    protected void canvasTick() {
        if (input != null) tempInput = input.getValue();
    }

    @Override
    protected void buildUi() {
        int cx = this.canvasWidth() / 2;
        int guiW = this.canvasWidth() - 20;
        int x0 = cx - guiW / 2;
        int y0 = 35;

        int inputW = guiW - 120;

        this.input = addTextField(
                x0,
                y0,
                inputW,
                Component.empty()
        );
        this.input.setMaxLength(1024);
        this.input.setPlaceholder(Component.translatable("gui.kineticarmory.armorsets.tips.edit_hint"));
        if (tempInput != null) this.input.setValue(tempInput);
this.btnAdd = addButtonWithHandler(x0 + guiW - 115, y0, 115, Component.translatable("gui.kineticarmory.armorsets.tips.add"), null, b -> {
            ensureManualTipLayout();
            if (!input.getValue().trim().isEmpty()) {
                config.tipLayout.add(ArmorDataConfig.TipLineData.text(input.getValue()));
                input.setValue("");
                listWidget.refresh();
            }
        });
this.btnModify = addButtonWithHandler(x0 + guiW - 115, y0, 55, Component.translatable("gui.kineticarmory.armorsets.commands.save_edit"), null, b -> {
            ensureManualTipLayout();
            String value = input.getValue().trim();
            if (value.isEmpty()) return;
            ArmorDataConfig.TipLineData data = getTipLayoutEntry(editingIndex);
            if (data != null) {
                data.text = input.getValue();
                cancelEdit();
                listWidget.refresh();
                KineticOverlays.toast(Component.translatable("msg.kineticarmory.common.saved"));
            }
        });
        this.btnModify.setVisible(false);
this.btnCancel = addButtonWithHandler(x0 + guiW - 55, y0, 55, Component.translatable("gui.kineticarmory.armorsets.commands.cancel_edit"), null, b -> cancelEdit());
        this.btnCancel.setVisible(false);
        int swatchSize = KineticScreen.COMPACT_CONTROL_HEIGHT;
        int swatchGap = 2;
        int paletteWidth = COLORS.length * swatchSize + (COLORS.length - 1) * swatchGap;
        int cX = cx - paletteWidth / 2;
        int cY = y0 + 25;
        for (int i = 0; i < COLORS.length; i++) {
            final String code = "§" + CODES[i];
            int finalI = i;
            addColorSwatchButton(
                    cX + i * (swatchSize + swatchGap),
                    cY,
                    COLORS[finalI],
                    null,
                    () -> insert(code)
            );
        }

        int listTop = cY + swatchSize + 10;
        int listBottom = this.canvasHeight() - 35;
        this.listWidget = new TipListWidget(guiW, listBottom - listTop, listTop, listBottom, 22);
        this.listWidget.setLeftPos(x0);
        this.addSmoothSelectionList(listWidget);

        int actionBtnW = 80;
        int bottomBtnY = this.canvasHeight() - 25;

        addButtonWithHandler(cx - actionBtnW - 5, bottomBtnY, actionBtnW, Component.translatable("gui.kineticarmory.armorsets.save"), null, b -> KineticOverlays.toast(Component.translatable("msg.kineticarmory.common.saved")));
        addButtonWithHandler(cx + 5, bottomBtnY, actionBtnW, Component.translatable("gui.kineticarmory.armorsets.back"), null, b -> {
            if (minecraft != null) navigateBack();
        });
    }

    private void ensureTips() {
        if (config.tips == null) config.tips = new ArrayList<>();
    }

    private void ensureTipLayout() {
        if (config.tipLayout == null) config.tipLayout = new ArrayList<>();
    }

    private void ensureHiddenTipKeys() {
        if (config.hiddenTipKeys == null) config.hiddenTipKeys = new ArrayList<>();
    }

    private void ensureManualTipLayout() {
        ensureTips();
        ensureTipLayout();
        ensureHiddenTipKeys();

        boolean flexible = config.flexiblePieces && config.pieceBonusGroups != null && !config.pieceBonusGroups.isEmpty();
        int total = Math.max(1, config.getTotalPieceCount());
        int pieceCount = ArmorCache.getSetPieceCount(config.id);
        List<ArmorTipGenerator.TooltipLine> generatedLines = ArmorTipGenerator.buildGeneratedLayoutSource(config, total, flexible, flexible, pieceCount);
        Map<String, ArmorTipGenerator.TooltipLine> generatedByKey = new LinkedHashMap<>();
        for (ArmorTipGenerator.TooltipLine line : generatedLines) {
            if (line != null && line.overrideKey() != null && !line.overrideKey().isBlank()) {
                generatedByKey.putIfAbsent(line.overrideKey(), line);
            }
        }

        if (!config.manualTips) {
            config.tipLayout.clear();
            appendLegacyTextTips();
            appendMissingGeneratedLines(generatedLines, new HashSet<>());
            config.tips.clear();
            config.manualTips = true;
            return;
        }

        if (!config.tips.isEmpty()) {
            appendLegacyTextTips();
            config.tips.clear();
        }

        Set<String> existingKeys = new HashSet<>();
        for (ArmorDataConfig.TipLineData data : config.tipLayout) {
            if (data == null) continue;
            normalizeTipLineData(data);
            if (data.isGenerated()) {
                existingKeys.add(data.key);
                ArmorTipGenerator.TooltipLine source = generatedByKey.get(data.key);
                if (source != null) syncGeneratedMetadata(data, source);
            }
        }
        appendMissingGeneratedLines(generatedLines, existingKeys);
    }

    private void appendLegacyTextTips() {
        for (String tip : config.tips) {
            if (tip == null || tip.isBlank()) continue;
            for (String text : splitDisplayLines(tip)) {
                if (!text.isBlank()) config.tipLayout.add(ArmorDataConfig.TipLineData.text(text));
            }
        }
    }

    private void appendMissingGeneratedLines(List<ArmorTipGenerator.TooltipLine> generatedLines, Set<String> existingKeys) {
        for (ArmorTipGenerator.TooltipLine source : generatedLines) {
            if (source == null || source.overrideKey() == null || source.overrideKey().isBlank()) continue;
            String key = source.overrideKey();
            if (existingKeys.contains(key) || config.hiddenTipKeys.contains(key)) continue;
            config.tipLayout.add(createGeneratedLine(source));
            existingKeys.add(key);
        }
    }

    private ArmorDataConfig.TipLineData createGeneratedLine(ArmorTipGenerator.TooltipLine source) {
        ArmorDataConfig.TipLineData data = ArmorDataConfig.TipLineData.generated(source.overrideKey());
        syncGeneratedMetadata(data, source);
        return data;
    }

    private void syncGeneratedMetadata(ArmorDataConfig.TipLineData data, ArmorTipGenerator.TooltipLine source) {
        if (data == null || source == null) return;
        data.type = "generated";
        data.key = source.overrideKey() == null ? "" : source.overrideKey();
        data.iconLine = source.iconLine();
        data.fullOnly = source.fullOnly();
        data.setActivePieces(source.activePieces());
        if (data.iconLine) {
            data.role = "effect";
        } else if (data.key.startsWith("text|current_pieces")) {
            data.role = "info";
        } else {
            data.role = "title";
        }
    }

    private void normalizeTipLineData(ArmorDataConfig.TipLineData data) {
        if (data.type == null || data.type.isBlank()) data.type = data.key == null || data.key.isBlank() ? "text" : "generated";
        if (data.key == null) data.key = "";
        if (data.text == null) data.text = "";
        if (data.role == null || data.role.isBlank()) data.role = data.iconLine ? "effect" : "title";
        if (data.activePieces == null) data.activePieces = new ArrayList<>();
    }

    private List<String> splitDisplayLines(String text) {
        List<String> result = new ArrayList<>();
        if (text == null || text.isBlank()) return result;
        String[] parts = text.split("\\n");
        for (String part : parts) {
            if (part != null && !part.isBlank()) result.add(part);
        }
        return result;
    }

    private ArmorDataConfig.TipLineData getTipLayoutEntry(int index) {
        ensureTipLayout();
        if (index < 0 || index >= config.tipLayout.size()) return null;
        return config.tipLayout.get(index);
    }

    private void rebuildDisplayRows() {
        ensureManualTipLayout();
        displayRows.clear();
        boolean showPieceCounter = config.flexiblePieces && config.pieceBonusGroups != null && !config.pieceBonusGroups.isEmpty();
        int pieceCount = ArmorCache.getSetPieceCount(config.id);
        List<ArmorTipGenerator.TooltipLine> lines = ArmorTipGenerator.buildTooltipLines(config, pieceCount, showPieceCounter);
        for (ArmorTipGenerator.TooltipLine line : lines) {
            if (line.text() == null || line.text().isBlank()) continue;
            int layoutIndex = line.customIndex();
            displayRows.add(new TipRow(line.text(), Math.max(0, layoutIndex), line.iconLine()));
        }
    }

    private void startEdit(TipRow row) {
        ensureManualTipLayout();
        this.editingIndex = row.layoutIndex();
        ArmorDataConfig.TipLineData data = getTipLayoutEntry(row.layoutIndex());
        this.input.setValue(data != null && data.text != null && !data.text.isBlank() ? data.text : row.text());
        focusControl(this.input);

        this.btnAdd.setVisible(false);
        this.btnModify.setVisible(true);
        this.btnCancel.setVisible(true);
    }

    private void cancelEdit() {
        this.editingIndex = -1;
        this.input.setValue("");

        this.btnAdd.setVisible(true);
        this.btnModify.setVisible(false);
        this.btnCancel.setVisible(false);
    }

    private void insert(String s) {
        if (input == null) return;
        focusControl(input);
        int p = input.getCursorPosition();
        String old = input.getValue();
        if (p > old.length()) p = old.length();
        input.setValue(old.substring(0, p) + s + old.substring(p));
        input.setCursorPosition(p + 2);
    }

    @Override
    protected boolean canvasMouseDragged(double mx, double my, int btn, double dx, double dy) {
        if (draggingIndex != -1 && KineticClientRuntime.controlModifierDown()) {
            return true;
        }
        return super.canvasMouseDragged(mx, my, btn, dx, dy);
    }

    @Override
    protected boolean canvasMouseReleased(double mx, double my, int btn) {
        if (draggingIndex != -1 && KineticMouseButtons.isPrimary(btn)) {
            int insertAt = resolveLayoutInsertIndex(hoverTargetIndex);
            ensureManualTipLayout();
            if (insertAt >= 0 && draggingIndex >= 0 && draggingIndex < config.tipLayout.size()) {
                ArmorDataConfig.TipLineData temp = config.tipLayout.remove(draggingIndex);
                if (insertAt > draggingIndex) insertAt--;
                insertAt = Math.max(0, Math.min(insertAt, config.tipLayout.size()));
                config.tipLayout.add(insertAt, temp);
                listWidget.refresh();
            }
            draggingIndex = -1;
            draggingText = null;
            hoverTargetIndex = -1;
            return true;
        }
        return super.canvasMouseReleased(mx, my, btn);
    }

    private int resolveLayoutInsertIndex(int displayIndex) {
        ensureManualTipLayout();
        if (displayIndex < 0) return -1;
        return Math.min(displayIndex, config.tipLayout.size());
    }

    @Override
    protected void renderCanvasBackground(@NotNull GuiGraphics g, int mx, int my, float pt) {
        int cx = this.canvasWidth() / 2;
        int guiW = this.canvasWidth() - 20;
        int x0 = cx - guiW / 2;

        GuiTheme.panel(g, x0 - 5, 5, guiW + 10, this.canvasHeight() - 10);
        g.drawCenteredString(this.font, this.title, cx, 8, 0xFFFFFF);

        String dragHint = Component.translatable("gui.kineticarmory.armorsets.tips.drag_hint").getString();
        g.drawString(this.font, dragHint, x0 + 5, 20, 0xFFFFFF);

        renderSmoothSelectionList(listWidget, g, mx, my, pt);
    }

    @Override
    protected void renderCanvasForeground(@NotNull GuiGraphics g, int mx, int my, float pt) {
        if (draggingIndex != -1 && draggingText != null) {
            hoverTargetIndex = -1;
            if (my >= listWidget.getTop() && my <= listWidget.getBottom()) {
                for (int i = 0; i < listWidget.children().size(); i++) {
                    TipListWidget.Entry entry = listWidget.children().get(i);
                    if (my < entry.lastT + 11) {
                        hoverTargetIndex = i;
                        break;
                    } else if (my < entry.lastT + 22) {
                        hoverTargetIndex = i + 1;
                        break;
                    }
                }
                if (hoverTargetIndex == -1 && !listWidget.children().isEmpty()) {
                    hoverTargetIndex = listWidget.children().size();
                }
            }

            if (hoverTargetIndex != -1) {
                int lineY;
                if (hoverTargetIndex < listWidget.children().size()) {
                    lineY = listWidget.children().get(hoverTargetIndex).lastT;
                } else if (!listWidget.children().isEmpty()) {
                    lineY = listWidget.children().get(listWidget.children().size() - 1).lastT + 22;
                } else {
                    lineY = listWidget.getTop() + 2;
                }
                GuiTheme.separator(g, listWidget.getLeft(), lineY, listWidget.getRowWidth());
            }

            int floatX = mx - 50;
            int floatY = my - 10;
            int w = listWidget.getRowWidth();

            GuiTheme.stateSurface(
                    g,
                    floatX,
                    floatY,
                    w,
                    20,
                    GuiTheme.Surface.PANEL_ALT,
                    true,
                    false,
                    false
            );

            renderTextWithIcons(g, this.font, draggingText, floatX + 4, floatY + 6, w - 45);
        }
    }

    private void renderTextWithIcons(GuiGraphics g, Font font, String text, int x, int y, int maxW) {
        text = text.replaceAll("\\n\\s*(§[0-9a-fk-or])?", "");

        Pattern pattern = Pattern.compile("\\[(item|effect):([^]]+)]");
        Matcher matcher = pattern.matcher(text);
        String cleanText = text.replaceAll("\\[(item|effect):([^]]+)]", "");

        int contentWidth = font.width(cleanText);
        while (matcher.find()) {
            String textBefore = text.substring(0, matcher.start());
            String cleanBefore = textBefore.replaceAll("\\[(item|effect):([^]]+)]", "");
            contentWidth = Math.max(contentWidth, font.width(cleanBefore) + 12);
        }

        int offset = KineticText.scrollOffset(contentWidth, maxW);
        enableUiScissor(g, x, y - 2, x + maxW, y + font.lineHeight + 3);
        try {
            g.drawString(font, cleanText, x - offset, y, 16777215, false);

            matcher.reset();
            while (matcher.find()) {
                String type = matcher.group(1);
                String id = matcher.group(2);
                String textBefore = text.substring(0, matcher.start());
                String cleanBefore = textBefore.replaceAll("\\[(item|effect):([^]]+)]", "");
                int iconX = x - offset + font.width(cleanBefore);
                int iconY = y - 2;

                if (type.equals("item")) {
                    net.minecraft.resources.ResourceLocation rl = KineticResourceIds.tryParse(id);
                    if (rl != null) {
                        net.minecraft.world.item.Item item = KineticRegistries.items().get(rl);
                        if (item != null && item != net.minecraft.world.item.Items.AIR) {
                            g.pose().pushPose();
                            g.pose().translate(iconX, iconY, 0);
                            g.pose().scale(0.7f, 0.7f, 1.0f);
                            g.renderItem(new net.minecraft.world.item.ItemStack(item), 0, 0);
                            g.pose().popPose();
                        }
                    }
                }
            }
        } finally {
            disableUiScissor(g);
        }
    }

    private record TipRow(String text, int layoutIndex, boolean iconLine) {}

    private boolean isEditingRow(TipRow row) {
        return row != null && editingIndex == row.layoutIndex();
    }

    class TipListWidget extends SmoothSelectionList<TipListWidget.Entry> {
        private final int listTop, listBottom;

        public TipListWidget(int w, int h, int t, int b, int ih) {
            super(w, h, t, b, ih);
            this.listTop = t;
            this.listBottom = b;
            setRenderBackground(false);
            setRenderTopAndBottom(false);
            setRenderSelection(false);
            refresh();
        }

        public void refresh() {
            clearEntries();
            rebuildDisplayRows();
            for (TipRow displayRow : displayRows) addEntry(new Entry(displayRow));
        }

        @Override public int getRowWidth() { return this.width - 20; }

        class Entry extends SmoothEntry<Entry> {
            private static final int DELETE_BUTTON_W = 44;
            private static final int DELETE_BUTTON_H = KineticScreen.STANDARD_CONTROL_HEIGHT;

            private final TipRow row;
            private final StateButton deleteButton;
            protected int lastT;

            public Entry(TipRow row) {
                this.row = row;
                this.deleteButton = KineticWidgets.createCompactButton(0, 0, DELETE_BUTTON_W, Component.translatable("gui.kineticarmory.armorsets.delete"), null, this::deleteRow);
            }

            @Override
            public void render(@NotNull GuiGraphics g, int index, int t, int l, int w, int h, int mx, int my, boolean hv, float pt) {
                this.lastT = t;

                if (draggingIndex == row.layoutIndex()) {
                    GuiTheme.stateSurface(g, l, t, w, 20, GuiTheme.Surface.PANEL_ALT, true, true, false);
                    return;
                }

                boolean editing = isEditingRow(row);
                GuiTheme.stateSurface(
                        g,
                        l,
                        t,
                        w,
                        20,
                        row.iconLine() ? GuiTheme.Surface.PANEL : GuiTheme.Surface.PANEL_ALT,
                        editing,
                        hv,
                        false
                );

                int deleteX = l + w - DELETE_BUTTON_W - 5;
                int maxW = Math.max(0, deleteX - l - 8);
                renderTextWithIcons(g, KineticClientRuntime.font(), row.text(), l + 4, t + 6, maxW);

                deleteButton.setX(deleteX);
                deleteButton.setY(t + (20 - DELETE_BUTTON_H) / 2);
                deleteButton.render(g, mx, my, pt);
            }

            private void deleteRow() {
                ensureManualTipLayout();
                if (row.layoutIndex() < 0 || row.layoutIndex() >= config.tipLayout.size()) return;
                ArmorDataConfig.TipLineData removed = config.tipLayout.remove(row.layoutIndex());
                ensureHiddenTipKeys();
                if (removed != null && removed.isGenerated() && removed.key != null && !removed.key.isBlank() && !config.hiddenTipKeys.contains(removed.key)) {
                    config.hiddenTipKeys.add(removed.key);
                }
                cancelEdit();
                refresh();
            }

            @Override
            public boolean mouseClicked(double mx, double my, int btn) {
                if (my < lastT || my >= lastT + 20) return false;
                if (deleteButton.mouseClicked(mx, my, btn)) return true;

                if (KineticClientRuntime.controlModifierDown() && KineticMouseButtons.isPrimary(btn)) {
                    draggingIndex = row.layoutIndex();
                    draggingText = row.text();
                    return true;
                }

                if (KineticMouseButtons.isPrimary(btn)) {
                    startEdit(row);
                    return true;
                }
                return false;
            }

            @Override public @NotNull Component getNarration() { return Component.empty(); }
        }
    }
}
