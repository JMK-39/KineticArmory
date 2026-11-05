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

    private static final int ADD_BUTTON_WIDTH = 55;
    private static final int EDIT_BUTTON_WIDTH = 55;
    private static final int ACTION_BUTTON_GAP = 4;
    // Keep header text clear of the action buttons and panel edges.
    private static final int TEXT_GAP = 4;
    private static final int[] COLORS = { 0x000000, 0x0000AA, 0x00AA00, 0x00AAAA, 0xAA0000, 0xAA00AA, 0xFFAA00, 0x5555FF, 0x55FF55, 0x55FFFF, 0xFF5555, 0xFF55FF, 0xFFFF55, 0xFFFFFF };
    private static final String[] CODES = {"0", "1", "2", "3", "4", "5", "6", "9", "a", "b", "c", "d", "e", "f"};

    public ArmorTipEditorPage(ArmorDataConfig config) {
        super(KineticI18n.translatable("gui.kineticarmory.armorsets.btn_edit_tips"));
        useCanvas(CANVAS_WIDTH, CANVAS_HEIGHT, 4);
        bindDeleteAction(() -> listWidget != null && listWidget.selectedItem() != null,
                () -> listWidget.deleteRow(listWidget.selectedItem()));
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
        // Header: Back, the colour squares and Reset centred on the screen, then Add (Save Edit while a row is being
        // edited) and Save at the right. The text field sits under the header and the rows fill the rest.
        int guiW = this.width() - 8;
        int x0 = 4;
        int actionY = 4;
        ui().button(x0, actionY, 40).text(KineticI18n.translatable("gui.kineticarmory.armorsets.back")).onClick(b -> {
            if (editingIndex >= 0) cancelEdit(); else navigateBack();
        }).build();
        int saveX = x0 + guiW - 40;
        ui().button(saveX, actionY, 40).text(KineticI18n.translatable("gui.kineticarmory.armorsets.save")).onClick(b -> KineticOverlays.toast(KineticI18n.translatable("msg.kineticarmory.common.saved"))).build();
        int actionX = saveX - ACTION_BUTTON_GAP - ADD_BUTTON_WIDTH;
        this.btnAdd = ui().button(actionX, actionY, ADD_BUTTON_WIDTH).text(KineticI18n.translatable("gui.kineticarmory.armorsets.tips.add")).onClick(b -> {
            ensureManualTipLayout();
            if (!input.textValue().trim().isEmpty()) {
                config.tipLayout.add(ArmorDataConfig.TipLineData.text(input.textValue()));
                input.setTextValue("");
                listWidget.refresh();
            }
        }).build();
        this.btnModify = ui().button(actionX, actionY, EDIT_BUTTON_WIDTH).text(KineticI18n.translatable("gui.kineticarmory.armorsets.commands.save_edit")).onClick(b -> {
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
        updateActionButtonLayout();

        int swatchSize = KineticPage.CONTROL_HEIGHT;
        int swatchGap = 2;
        int paletteWidth = (COLORS.length + 1) * (swatchSize + swatchGap) - swatchGap;
        int cX = this.width() / 2 - paletteWidth / 2;
        for (int i = 0; i < COLORS.length; i++) {
            final String code = "§" + CODES[i];
            ui().colorSwatch(cX + i * (swatchSize + swatchGap), actionY, COLORS[i])
                    .tooltip(net.minecraft.network.chat.Component.literal(code.replace("§", "&")))
                    .onClick(() -> insert(code))
                    .build();
        }
        ui().button(cX + COLORS.length * (swatchSize + swatchGap), actionY, swatchSize).text(net.minecraft.network.chat.Component.literal("R"))
                .tooltip(KineticI18n.translatable("gui.kineticarmory.armorsets.tips.reset_color")).onClick(b -> insert("§r")).build();

        int y0 = 28;
        this.input = ui().textField(x0, y0, guiW).firstShownTextAsDefault().build();
        this.input.limitTextLength(1024);
        this.input.setTooltip(KineticI18n.translatable("gui.kineticarmory.armorsets.tips.drag_hint"));
        this.input.setPlaceholder(KineticI18n.translatable("gui.kineticarmory.armorsets.tips.edit_hint"));
        if (tempInput != null) this.input.setTextValue(tempInput);

        int listTop = y0 + swatchSize + 4;
        int listBottom = this.height() - 4;
        this.listWidget = ui().add(new TipListWidget(x0, listTop, guiW, listBottom - listTop));
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

    /** Add and Save Edit share one place: Save Edit replaces Add while a row is being edited; Back ends the edit. */
    private void updateActionButtonLayout() {
        if (btnAdd == null || btnModify == null) return;
        boolean editing = editingIndex >= 0;
        btnAdd.setControlVisible(!editing);
        btnModify.setControlVisible(editing);
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
        int guiW = this.width() - 8;
        int x0 = cx - guiW / 2;

        KineticTheme.panel(g, 0, 0, width(), height());
        KineticTheme.separator(g, 4, 24, width() - 8);



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
                KineticTheme.separator(g, listWidget.rowLeft(), lineY, listWidget.rowWidth());
            }

            // The dragged row stays over the list it reorders, and its opaque backing keeps the rows underneath
            // from showing through the translucent surface.
            int w = listWidget.rowWidth();
            int floatX = listWidget.rowLeft();
            int floatY = Math.max(listTop, Math.min(my - 10, listBottom - 22));

            g.fill(floatX, floatY, floatX + w, floatY + 22, 0xFF000000 | KineticTheme.current().panelAlt());
            KineticTheme.stateSurface(
                    g,
                    floatX,
                    floatY,
                    w,
                    22,
                    KineticTheme.Surface.PANEL_ALT,
                    true,
                    false,
                    false
            );

            renderTextWithIcons(g, draggingText, floatX + TEXT_GAP, floatY + 7, Math.max(0, w - TEXT_GAP * 2));
        }
    }

    private void renderTextWithIcons(KineticGraphics g, String text, int x, int y, int maxWidth) {
        renderTextWithIcons(g, text, x, y, maxWidth, true);
    }

    private void renderTextWithIcons(KineticGraphics g, String text, int x, int y, int maxWidth, boolean scroll) {
        text = text.replaceAll("\\n\\s*(§[0-9a-fk-or])?", "");

        Pattern pattern = Pattern.compile("\\[(item|effect):([^]]+)]");
        Matcher measure = pattern.matcher(text);
        int contentWidth = 0;
        int lastEnd = 0;
        while (measure.find()) {
            contentWidth += KineticText.width(formattingBefore(text, lastEnd) + text.substring(lastEnd, measure.start()));
            contentWidth += measure.group(1).equals("item") ? 20 : 12;
            lastEnd = measure.end();
        }
        contentWidth += KineticText.width(formattingBefore(text, lastEnd) + text.substring(lastEnd));

        int offset = scroll ? KineticText.scrollOffset(contentWidth, maxWidth) : 0;
        g.scissor(x, y - 4, x + maxWidth, y + 12);
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
                            KineticTheme.itemSlot(g, currentX + 2, y - 4, 16, false);
                            KineticTheme.item(g, new net.minecraft.world.item.ItemStack(item), currentX + 2, y - 4, 16, 0.625F, false);
                        }
                    }
                }

                lastEnd = matcher.end();
                currentX += type.equals("item") ? 20 : 12;
            }
            g.text(formattingBefore(text, lastEnd) + text.substring(lastEnd), currentX, y, 0xFFFFFF, false);
        } finally {
            g.endScissor();
        }
    }

    // Retain legacy colors and font styles when an icon splits a single authored line.
    static String formattingBefore(String text, int end) {
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
        // Row frames are ROW_FRAME_H tall; the 16 px Delete button and the text sit centred, 3 px clear of the frame lines.
        private static final int ROW_H = 24;
        private static final int ROW_FRAME_H = 22;
        private static final int ROW_INSET = 0;
        private static final int DELETE_BUTTON_W = 40;
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
            return rowsWidth();
        }

        private int deleteX() {
            return rowLeft() + rowWidth() - DELETE_BUTTON_W - 5;
        }

        private int deleteY(int rowTop) {
            return rowTop + (ROW_FRAME_H - DELETE_BUTTON_H) / 2;
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
                KineticTheme.stateSurface(g, l, t, w, ROW_FRAME_H, KineticTheme.Surface.PANEL_ALT, true, true, false);
                return;
            }

            boolean editing = isEditingRow(row);
            KineticTheme.stateSurface(
                    g,
                    l,
                    t,
                    w,
                    ROW_FRAME_H,
                    row.iconLine() ? KineticTheme.Surface.PANEL : KineticTheme.Surface.PANEL_ALT,
                    editing,
                    hv,
                    false
            );

            int deleteX = deleteX();
            int maxW = Math.max(0, deleteX - l - 8);
            renderTextWithIcons(g, row.text(), l + 4, t + (ROW_FRAME_H - 8) / 2, maxW, hv);

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
            if (input.y() < t || input.y() >= t + ROW_FRAME_H) return false;
            setSelectedIndex(index);
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
