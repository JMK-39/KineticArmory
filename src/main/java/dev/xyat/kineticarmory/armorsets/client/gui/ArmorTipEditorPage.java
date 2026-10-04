package dev.xyat.kineticarmory.armorsets.client.gui;

import dev.xyat.kineticcore.api.text.KineticI18n;
import dev.xyat.kineticcore.api.client.gui.input.MouseDragInput;
import dev.xyat.kineticcore.api.client.gui.input.MouseInput;
import dev.xyat.kineticcore.api.client.gui.overlay.KineticOverlays;
import dev.xyat.kineticcore.api.client.gui.page.KineticPage;
import dev.xyat.kineticcore.api.client.gui.render.KineticGraphics;
import dev.xyat.kineticcore.api.client.gui.text.KineticText;
import dev.xyat.kineticcore.api.client.gui.theme.KineticTheme;
import dev.xyat.kineticcore.api.client.gui.ui.KineticUi;
import dev.xyat.kineticcore.api.client.gui.widget.*;
import dev.xyat.kineticcore.api.client.gui.widget.list.*;

import dev.xyat.kineticarmory.armorsets.client.ArmorCache;
import dev.xyat.kineticarmory.armorsets.data.ArmorDataConfig;
import dev.xyat.kineticarmory.armorsets.data.ArmorTipGenerator;
import dev.xyat.kineticcore.api.client.input.KineticMouseButtons;
import dev.xyat.kineticcore.api.registry.KineticRegistries;
import dev.xyat.kineticcore.api.resource.KineticResourceIds;
import dev.xyat.kineticcore.api.runtime.KineticClientRuntime;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ArmorTipEditorPage extends KineticPage {
    
    private final ArmorDataConfig config;
    private final List<TipRow> displayRows = new ArrayList<>();
    private TipListWidget listWidget;
    private KineticTextField input;

    private int editingIndex = -1;
    private int draggingIndex = -1;
    private String draggingText = null;
    private int hoverTargetIndex = -1;
    private String tempInput = null;

    private KineticButton btnAdd;
    private KineticButton btnModify;
    private KineticButton btnCancel;

    private static final int ADD_BUTTON_WIDTH = 55;
    private static final int EDIT_BUTTON_WIDTH = 55;
    private static final int CANCEL_BUTTON_WIDTH = 55;
    private static final int ACTION_BUTTON_GAP = 4;
    // Keep header text clear of the action buttons and panel edges.
    private static final int TEXT_GAP = 4;
    private static final int[] COLORS = { 0x000000, 0x0000AA, 0x00AA00, 0x00AAAA, 0xAA0000, 0xAA00AA, 0xFFAA00, 0x5555FF, 0x55FF55, 0x55FFFF, 0xFF5555, 0xFF55FF, 0xFFFF55, 0xFFFFFF };
    private static final String[] CODES = {"0", "1", "2", "3", "4", "5", "6", "9", "a", "b", "c", "d", "e", "f"};

    public ArmorTipEditorPage(ArmorDataConfig config) {
        super(KineticI18n.translatable("gui.kineticarmory.armorsets.btn_edit_tips"));
        this.config = config;
        this.config.initNullFields();
        ensureManualTipLayout();
    }

    @Override
    protected void onTick() {
        if (input != null) tempInput = input.textValue();
    }

    @Override
    protected void build(KineticUi ui) {
        int cx = this.width() / 2;
        int guiW = this.width() - 20;
        int x0 = cx - guiW / 2;
        int y0 = 35;

        int inputW = guiW;
        int actionY = 9;

        this.input = ui().textField(x0, y0, inputW).firstShownTextAsDefault().build();
        this.input.limitTextLength(1024);
        this.input.setPlaceholder(KineticI18n.translatable("gui.kineticarmory.armorsets.tips.edit_hint"));
        if (tempInput != null) this.input.setTextValue(tempInput);
        int actionRight = x0 + guiW + 1;
        this.btnAdd = ui().button(actionRight - ADD_BUTTON_WIDTH, actionY, ADD_BUTTON_WIDTH).text(KineticI18n.translatable("gui.kineticarmory.armorsets.tips.add")).onClick(b -> {
            ensureManualTipLayout();
            if (!input.textValue().trim().isEmpty()) {
                config.tipLayout.add(ArmorDataConfig.TipLineData.text(input.textValue()));
                input.setTextValue("");
                listWidget.refresh();
            }
        }).build();
        this.btnModify = ui().button(actionRight - EDIT_BUTTON_WIDTH, actionY, EDIT_BUTTON_WIDTH).text(KineticI18n.translatable("gui.kineticarmory.armorsets.commands.save_edit")).onClick(b -> {
            ensureManualTipLayout();
            String value = input.textValue().trim();
            if (value.isEmpty()) return;
            ArmorDataConfig.TipLineData data = getTipLayoutEntry(editingIndex);
            if (data != null) {
                data.text = input.textValue();
                cancelEdit();
                listWidget.refresh();
                KineticOverlays.toast(KineticI18n.translatable("msg.kineticarmory.common.saved"));
            }
        }).build();
        this.btnCancel = ui().button(actionRight - CANCEL_BUTTON_WIDTH, actionY, CANCEL_BUTTON_WIDTH).text(KineticI18n.translatable("gui.kineticarmory.armorsets.commands.cancel_edit")).onClick(b -> cancelEdit()).build();
        updateActionButtonLayout();
        int swatchSize = KineticPage.CONTROL_HEIGHT;
        int swatchGap = 2;
        int paletteWidth = COLORS.length * swatchSize + (COLORS.length - 1) * swatchGap;
        int cX = cx - paletteWidth / 2;
        int cY = y0 + 25;
        for (int i = 0; i < COLORS.length; i++) {
            final String code = "§" + CODES[i];
            int finalI = i;
            ui().colorSwatch(cX + i * (swatchSize + swatchGap), cY, COLORS[finalI])
                    .onClick(() -> insert(code))
                    .build();
        }

        int listTop = cY + swatchSize + 10;
        int listBottom = this.height() - 35;
        this.listWidget = ui().add(new TipListWidget(x0, listTop, guiW, listBottom - listTop));

        int actionBtnW = 80;
        int bottomBtnY = this.height() - 25;

        ui().button(cx - actionBtnW - 5, bottomBtnY, actionBtnW).text(KineticI18n.translatable("gui.kineticarmory.armorsets.save")).onClick(b -> KineticOverlays.toast(KineticI18n.translatable("msg.kineticarmory.common.saved"))).build();
        ui().button(cx + 5, bottomBtnY, actionBtnW).text(KineticI18n.translatable("gui.kineticarmory.armorsets.back")).onClick(b -> {
            navigateBack();
        }).build();
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
        this.input.setTextValue(data != null && data.text != null && !data.text.isBlank() ? data.text : row.text());
        focus(this.input);

        updateActionButtonLayout();
    }

    private void cancelEdit() {
        this.editingIndex = -1;
        this.input.setTextValue("");

        updateActionButtonLayout();
    }

    private void updateActionButtonLayout() {
        if (btnAdd == null || btnModify == null || btnCancel == null) return;

        int guiW = width() - 20;
        int x0 = width() / 2 - guiW / 2;
        int actionRight = x0 + guiW + 1;
        boolean editing = editingIndex >= 0;

        btnAdd.moveControlX(editing
                ? actionRight - ADD_BUTTON_WIDTH - 2 * EDIT_BUTTON_WIDTH - 2 * ACTION_BUTTON_GAP
                : actionRight - ADD_BUTTON_WIDTH - EDIT_BUTTON_WIDTH - ACTION_BUTTON_GAP);
        btnModify.moveControlX(editing
                ? actionRight - CANCEL_BUTTON_WIDTH - ACTION_BUTTON_GAP - EDIT_BUTTON_WIDTH
                : actionRight - EDIT_BUTTON_WIDTH);
        btnCancel.moveControlX(editing
                ? actionRight - CANCEL_BUTTON_WIDTH
                : actionRight - ADD_BUTTON_WIDTH - EDIT_BUTTON_WIDTH - CANCEL_BUTTON_WIDTH - 2 * ACTION_BUTTON_GAP);

        btnAdd.setControlVisible(true);
        btnAdd.setEnabled(!editing);
        btnModify.setControlVisible(true);
        btnModify.setEnabled(editing);
        btnCancel.setControlVisible(editing);
    }

    private void insert(String s) {
        if (input == null) return;
        focus(input);
        int p = input.cursorIndex();
        String old = input.textValue();
        if (p > old.length()) p = old.length();
        input.setTextValue(old.substring(0, p) + s + old.substring(p));
        input.setCursorIndex(p + 2);
    }

    @Override
    protected boolean onMouseDrag(MouseDragInput input) {
        double mx = input.x(), my = input.y(); int btn = input.rawButton(); double dx = input.deltaX(), dy = input.deltaY();
        if (draggingIndex != -1 && KineticClientRuntime.controlModifierDown()) {
            return true;
        }
        return false;
    }

    @Override
    protected boolean onMouseRelease(MouseInput input) {
        double mx = input.x(), my = input.y(); int btn = input.rawButton();
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
        return false;
    }

    private int resolveLayoutInsertIndex(int displayIndex) {
        ensureManualTipLayout();
        if (displayIndex < 0) return -1;
        return Math.min(displayIndex, config.tipLayout.size());
    }

    @Override
    protected void renderBackground(KineticGraphics g, int mx, int my, float pt) {
        int cx = this.width() / 2;
        int guiW = this.width() - 20;
        int x0 = cx - guiW / 2;

        KineticTheme.panel(g, x0 - 5, 5, guiW + 10, this.height() - 10);
        int textRight = btnAdd.controlX() - TEXT_GAP;
        int titleWidth = Math.max(0, Math.min(cx - x0, textRight - cx) * 2);
        g.scrollingTextCentered(title(), cx, 8, titleWidth, 0xFFFFFF, true);

        g.scrollingText(KineticI18n.translatable("gui.kineticarmory.armorsets.tips.drag_hint"),
                x0 + 5, 20, Math.max(0, textRight - x0 - 5), 0xFFFFFF, true);

    }

    @Override
    protected void renderForeground(KineticGraphics g, int mx, int my, float pt) {
        if (draggingIndex != -1 && draggingText != null) {
            hoverTargetIndex = -1;
            int listTop = listWidget.controlY();
            int listBottom = listTop + listWidget.controlHeight();
            int rowCount = listWidget.items().size();
            if (my >= listTop && my <= listBottom) {
                for (int i = 0; i < rowCount; i++) {
                    int rowTop = listWidget.rowTop(i);
                    if (my < rowTop + 11) {
                        hoverTargetIndex = i;
                        break;
                    } else if (my < rowTop + 22) {
                        hoverTargetIndex = i + 1;
                        break;
                    }
                }
                if (hoverTargetIndex == -1 && rowCount > 0) {
                    hoverTargetIndex = rowCount;
                }
            }

            if (hoverTargetIndex != -1) {
                int lineY;
                if (hoverTargetIndex < rowCount) {
                    lineY = listWidget.rowTop(hoverTargetIndex);
                } else if (rowCount > 0) {
                    lineY = listWidget.rowTop(rowCount - 1) + 22;
                } else {
                    lineY = listTop + 2;
                }
                KineticTheme.separator(g, listWidget.controlX(), lineY, listWidget.rowWidth());
            }

            int w = Math.min(listWidget.rowWidth(), Math.max(0, width() - 2 * TEXT_GAP));
            int floatX = Math.max(TEXT_GAP, Math.min(mx - 50, width() - w - TEXT_GAP));
            int floatY = Math.max(TEXT_GAP, Math.min(my - 10, height() - 20 - TEXT_GAP));

            KineticTheme.stateSurface(
                    g,
                    floatX,
                    floatY,
                    w,
                    20,
                    KineticTheme.Surface.PANEL_ALT,
                    true,
                    false,
                    false
            );

            renderTextWithIcons(g, draggingText, floatX + TEXT_GAP, floatY + 6, Math.max(0, w - TEXT_GAP * 2));
        }
    }

    private void renderTextWithIcons(KineticGraphics g, String text, int x, int y, int maxWidth) {
        text = text.replaceAll("\\n\\s*(§[0-9a-fk-or])?", "");

        Pattern pattern = Pattern.compile("\\[(item|effect):([^]]+)]");
        Matcher measure = pattern.matcher(text);
        int contentWidth = 0;
        int lastEnd = 0;
        while (measure.find()) {
            contentWidth += KineticText.width(formattingBefore(text, lastEnd) + text.substring(lastEnd, measure.start()));
            contentWidth += 12;
            lastEnd = measure.end();
        }
        contentWidth += KineticText.width(formattingBefore(text, lastEnd) + text.substring(lastEnd));

        int offset = KineticText.scrollOffset(contentWidth, maxWidth);
        g.scissor(x, y - 2, x + maxWidth, y + KineticText.lineHeight() + 3);
        try {
            Matcher matcher = pattern.matcher(text);
            int currentX = x - offset;
            lastEnd = 0;
            while (matcher.find()) {
                String plain = formattingBefore(text, lastEnd) + text.substring(lastEnd, matcher.start());
                g.text(plain, currentX, y, 0xFFFFFF, false);
                currentX += KineticText.width(plain);

                String type = matcher.group(1);
                String id = matcher.group(2);
                if (type.equals("item")) {
                    net.minecraft.resources.ResourceLocation rl = KineticResourceIds.tryParse(id);
                    if (rl != null) {
                        net.minecraft.world.item.Item item = KineticRegistries.items().get(rl);
                        if (item != null && item != net.minecraft.world.item.Items.AIR) {
                            g.push();
                            g.translate(currentX, y - 2);
                            g.scale(0.7f, 0.7f);
                            g.item(new net.minecraft.world.item.ItemStack(item), 0, 0);
                            g.pop();
                        }
                    }
                }

                lastEnd = matcher.end();
                currentX += 12;
            }
            g.text(formattingBefore(text, lastEnd) + text.substring(lastEnd), currentX, y, 0xFFFFFF, false);
        } finally {
            g.endScissor();
        }
    }

    // Retain legacy colors and font styles when an icon splits a single authored line.
    private static String formattingBefore(String text, int end) {
        Matcher formats = Pattern.compile("(?i)§[0-9a-fk-or]").matcher(text.substring(0, end));
        StringBuilder codes = new StringBuilder();
        while (formats.find()) codes.append(formats.group());
        return codes.toString();
    }

    private record TipRow(String text, int layoutIndex, boolean iconLine) {}

    private boolean isEditingRow(TipRow row) {
        return row != null && editingIndex == row.layoutIndex();
    }

    /**
     * 提示行列表：Ctrl+左键拖动排序、左键编辑、行内删除按钮。
     * Tip row list: Ctrl+left-drag to reorder, left click to edit, inline delete button.
     */
    class TipListWidget extends KineticRowList<TipRow> {
        private static final int ROW_H = 22;
        private static final int ROW_INSET = 12;
        private static final int DELETE_BUTTON_W = 44;
        private static final int DELETE_BUTTON_H = KineticPage.CONTROL_HEIGHT;

        TipListWidget(int x, int y, int width, int height) {
            super(x, y, width, height, ROW_H);
            refresh();
        }

        public void refresh() {
            rebuildDisplayRows();
            setItems(displayRows);
        }

        /** 行左侧 / Row left edge. */
        int rowLeft() {
            return controlX() + ROW_INSET;
        }

        /** 行宽 / Row width. */
        int rowWidth() {
            return Math.max(1, controlWidth() - 20);
        }

        private int deleteX() {
            return rowLeft() + rowWidth() - DELETE_BUTTON_W - 5;
        }

        private int deleteY(int rowTop) {
            return rowTop + (20 - DELETE_BUTTON_H) / 2;
        }

        @Override
        protected void renderRowBackground(KineticGraphics g, int index, int x, int y, int width, int height,
                                           boolean hovered, boolean selected) {
            // 背景在 renderRow 中按编辑/拖拽状态绘制 / Drawn in renderRow according to edit/drag state.
        }

        @Override
        protected void renderRow(KineticGraphics g, TipRow row, int index, int x, int t, int width, int height,
                                 boolean hv, boolean selected) {
            int l = rowLeft();
            int w = rowWidth();
            if (draggingIndex == row.layoutIndex()) {
                KineticTheme.stateSurface(g, l, t, w, 20, KineticTheme.Surface.PANEL_ALT, true, true, false);
                return;
            }

            boolean editing = isEditingRow(row);
            KineticTheme.stateSurface(
                    g,
                    l,
                    t,
                    w,
                    20,
                    row.iconLine() ? KineticTheme.Surface.PANEL : KineticTheme.Surface.PANEL_ALT,
                    editing,
                    hv,
                    false
            );

            int deleteX = deleteX();
            int maxW = Math.max(0, deleteX - l - 8);
            renderTextWithIcons(g, row.text(), l + 4, t + 6, maxW);

            int deleteY = deleteY(t);
            boolean deleteHovered = mouseX() >= deleteX && mouseX() < deleteX + DELETE_BUTTON_W
                    && mouseY() >= deleteY && mouseY() < deleteY + DELETE_BUTTON_H;
            KineticTheme.button(g, deleteX, deleteY, DELETE_BUTTON_W, DELETE_BUTTON_H,
                    KineticI18n.translatable("gui.kineticarmory.armorsets.delete"), deleteHovered, true, false);
        }

        private void deleteRow(TipRow row) {
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
        protected boolean onRowClick(TipRow row, int index, MouseInput input) {
            int t = rowTop(index);
            if (input.y() < t || input.y() >= t + 20) return false;
            if (input.isLeft() && input.inside(deleteX(), deleteY(t), DELETE_BUTTON_W, DELETE_BUTTON_H)) {
                deleteRow(row);
                return true;
            }

            if (KineticClientRuntime.controlModifierDown() && input.isLeft()) {
                draggingIndex = row.layoutIndex();
                draggingText = row.text();
                return true;
            }

            if (input.isLeft()) {
                startEdit(row);
                return true;
            }
            return false;
        }
    }
}
