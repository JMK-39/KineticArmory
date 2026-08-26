package dev.xyat.kineticarmory.armorsets.client.gui;

import dev.xyat.kineticarmory.armorsets.client.ArmorCache;
import dev.xyat.kineticarmory.armorsets.data.ArmorDataConfig;
import dev.xyat.kineticarmory.armorsets.data.ArmorTipGenerator;
import dev.xyat.kineticcore.api.client.GuiRenderUtil;
import dev.xyat.kineticcore.api.client.GuiToastUtil;
import dev.xyat.kineticcore.api.client.ScaledScreen;
import dev.xyat.kineticcore.api.client.ScrollUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.ObjectSelectionList;
import net.minecraft.client.gui.screens.Screen;
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

public class ArmorTipEditorScreen extends ScaledScreen {
    private final ScaledScreen parent;
    private final ArmorDataConfig config;
    private final List<TipRow> displayRows = new ArrayList<>();
    private TipListWidget listWidget;
    private EditBox input;

    private int editingIndex = -1;
    private int draggingIndex = -1;
    private String draggingText = null;
    private int hoverTargetIndex = -1;
    private String tempInput = null;

    private Button btnAdd;
    private Button btnModify;
    private Button btnCancel;

    private static final int[] COLORS = { 0x000000, 0x0000AA, 0x00AA00, 0x00AAAA, 0xAA0000, 0xAA00AA, 0xFFAA00, 0x5555FF, 0x55FF55, 0x55FFFF, 0xFF5555, 0xFF55FF, 0xFFFF55, 0xFFFFFF };
    private static final String[] CODES = {"0", "1", "2", "3", "4", "5", "6", "9", "a", "b", "c", "d", "e", "f"};

    public ArmorTipEditorScreen(ScaledScreen parent, ArmorDataConfig config) {
        super(Component.translatable("gui.kineticarmory.armorsets.btn_edit_tips"));
        this.parent = parent;
        this.config = config;
        this.config.initNullFields();
        ensureManualTipLayout();
        configureResponsiveCanvas(
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
    protected void initScaled() {
        int cx = this.vWidth / 2;
        int guiW = this.vWidth - 20;
        int x0 = cx - guiW / 2;
        int y0 = 35;

        int inputW = guiW - 120;

        this.input = new EditBox(this.font, x0, y0, inputW, 20, Component.empty()) {
            @Override
            public void renderWidget(@NotNull GuiGraphics g, int mx, int my, float pt) {
                super.renderWidget(g, mx, my, pt);
                if (!this.isFocused() && this.getValue().isEmpty()) {
                    String editHint = Component.translatable("gui.kineticarmory.armorsets.tips.edit_hint").getString();
                    g.drawString(Minecraft.getInstance().font, Minecraft.getInstance().font.plainSubstrByWidth(editHint, this.getWidth() - 8), this.getX() + 4, this.getY() + 6, 0x888888, false);
                }
            }
        };
        this.input.setMaxLength(1024);
        if (tempInput != null) this.input.setValue(tempInput);
        this.addRenderableWidget(input);

        this.btnAdd = Button.builder(Component.translatable("gui.kineticarmory.armorsets.tips.add"), b -> {
            ensureManualTipLayout();
            if (!input.getValue().trim().isEmpty()) {
                config.tipLayout.add(ArmorDataConfig.TipLineData.text(input.getValue()));
                input.setValue("");
                listWidget.refresh();
            }
        }).bounds(x0 + guiW - 115, y0, 115, 20).build();
        this.addRenderableWidget(btnAdd);

        this.btnModify = Button.builder(Component.translatable("gui.kineticarmory.armorsets.commands.save_edit"), b -> {
            ensureManualTipLayout();
            String value = input.getValue().trim();
            if (value.isEmpty()) return;
            ArmorDataConfig.TipLineData data = getTipLayoutEntry(editingIndex);
            if (data != null) {
                data.text = input.getValue();
                cancelEdit();
                listWidget.refresh();
                GuiToastUtil.showToast(Component.translatable("msg.kineticarmory.common.saved"));
            }
        }).bounds(x0 + guiW - 115, y0, 55, 20).build();
        this.btnModify.visible = false;
        this.addRenderableWidget(btnModify);

        this.btnCancel = Button.builder(Component.translatable("gui.kineticarmory.armorsets.commands.cancel_edit"), b -> cancelEdit()).bounds(x0 + guiW - 55, y0, 55, 20).build();
        this.btnCancel.visible = false;
        this.addRenderableWidget(btnCancel);

        int cX = cx - (COLORS.length * 14) / 2;
        int cY = y0 + 25;
        for (int i = 0; i < COLORS.length; i++) {
            final String code = "§" + CODES[i];
            int finalI = i;
            this.addRenderableWidget(new Button(cX, cY, 12, 12, Component.empty(), b -> insert(code), (b) -> Component.empty()) {
                @Override
                public void renderWidget(@NotNull GuiGraphics g, int mx, int my, float pt) {
                    g.fill(getX(), getY(), getX() + width, getY() + height, 0xFF000000);
                    g.fill(getX() + 1, getY() + 1, getX() + width - 1, getY() + height - 1, COLORS[finalI] | 0xFF000000);
                    if (this.isHoveredOrFocused()) g.renderOutline(getX(), getY(), width, height, 0xFFFFFFFF);
                }
            });
            cX += 14;
        }

        int listTop = y0 + 45;
        int listBottom = this.vHeight - 35;
        this.listWidget = new TipListWidget(this.minecraft, guiW, listBottom - listTop, listTop, listBottom, 22);
        this.listWidget.setLeftPos(x0);
        this.addWidget(listWidget);

        int actionBtnW = 80;
        int bottomBtnY = this.vHeight - 25;

        this.addRenderableWidget(Button.builder(Component.translatable("gui.kineticarmory.armorsets.save"), b -> GuiToastUtil.showToast(Component.translatable("msg.kineticarmory.common.saved"))).bounds(cx - actionBtnW - 5, bottomBtnY, actionBtnW, 20).build());
        this.addRenderableWidget(Button.builder(Component.translatable("gui.kineticarmory.armorsets.back"), b -> {
            if (minecraft != null) minecraft.setScreen(parent);
        }).bounds(cx + 5, bottomBtnY, actionBtnW, 20).build());
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
        this.input.setFocused(true);
        this.setFocused(this.input);

        this.btnAdd.visible = false;
        this.btnModify.visible = true;
        this.btnCancel.visible = true;
    }

    private void cancelEdit() {
        this.editingIndex = -1;
        this.input.setValue("");

        this.btnAdd.visible = true;
        this.btnModify.visible = false;
        this.btnCancel.visible = false;
    }

    private void insert(String s) {
        if (input == null) return;
        input.setFocused(true);
        int p = input.getCursorPosition();
        String old = input.getValue();
        if (p > old.length()) p = old.length();
        input.setValue(old.substring(0, p) + s + old.substring(p));
        input.setCursorPosition(p + 2);
    }

    @Override
    protected boolean universalMouseDragged(double mx, double my, int btn, double dx, double dy) {
        if (draggingIndex != -1 && Screen.hasControlDown()) {
            return true;
        }
        return super.universalMouseDragged(mx, my, btn, dx, dy);
    }

    @Override
    protected boolean universalMouseReleased(double mx, double my, int btn) {
        if (draggingIndex != -1 && btn == 0) {
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
        return super.universalMouseReleased(mx, my, btn);
    }

    private int resolveLayoutInsertIndex(int displayIndex) {
        ensureManualTipLayout();
        if (displayIndex < 0) return -1;
        return Math.min(displayIndex, config.tipLayout.size());
    }

    @Override
    protected boolean universalMouseClicked(double mx, double my, int btn) {
        if (this.input != null) {
            if (!this.input.isMouseOver(mx, my)) {
                this.input.setFocused(false);
                if (this.getFocused() == this.input) this.setFocused(null);
            } else {
                this.setFocused(this.input);
            }
        }
        return super.universalMouseClicked(mx, my, btn);
    }

    @Override
    protected void renderScaledBackground(@NotNull GuiGraphics g, int mx, int my, float pt) {
        int cx = this.vWidth / 2;
        int guiW = this.vWidth - 20;
        int x0 = cx - guiW / 2;

        GuiRenderUtil.drawStandardPanel(g, x0 - 5, 5, guiW + 10, this.vHeight - 10);
        g.drawCenteredString(this.font, this.title, cx, 8, 0xFFFFFF);

        String dragHint = Component.translatable("gui.kineticarmory.armorsets.tips.drag_hint").getString();
        g.drawString(this.font, dragHint, x0 + 5, 20, 0xFFFFFF);

        renderScissorCorrectedList(listWidget, g, mx, my, pt);
    }

    @Override
    protected void renderScaledForeground(@NotNull GuiGraphics g, int mx, int my, float pt) {
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
                g.fill(listWidget.getLeft(), lineY - 1, listWidget.getLeft() + listWidget.getRowWidth(), lineY + 1, 0xFF55FF55);
            }

            int floatX = mx - 50;
            int floatY = my - 10;
            int w = listWidget.getRowWidth();

            g.fill(floatX, floatY, floatX + w, floatY + 20, 0xFF111111);
            g.renderOutline(floatX, floatY, w, 20, 0xFF55FF55);

            renderTextWithIcons(g, this.font, draggingText, floatX + 4, floatY + 6, w - 45);
        }
    }

    private void renderTextWithIcons(GuiGraphics g, Font font, String text, int x, int y, int maxW) {
        text = text.replaceAll("\\n\\s*(§[0-9a-fk-or])?", "");

        Pattern pattern = Pattern.compile("\\[(item|effect):([^]]+)]");
        Matcher matcher = pattern.matcher(text);

        String cleanText = text.replaceAll("\\[(item|effect):([^]]+)]", "");
        String dispClean = cleanText;

        if (font.width(cleanText) > maxW) {
            dispClean = font.plainSubstrByWidth(cleanText, maxW - 10) + "...";
        }
        g.drawString(font, dispClean, x, y, 16777215, false);

        int maxPixelX = x + maxW - 10;

        matcher.reset();
        while (matcher.find()) {
            String type = matcher.group(1);
            String id = matcher.group(2);

            String textBefore = text.substring(0, matcher.start());
            String cleanBefore = textBefore.replaceAll("\\[(item|effect):([^]]+)]", "");

            int iconX = x + font.width(cleanBefore);
            int iconY = y - 2;

            if (iconX > maxPixelX) continue;

            if (type.equals("item")) {
                net.minecraft.resources.ResourceLocation rl = net.minecraft.resources.ResourceLocation.tryParse(id);
                if (rl != null) {
                    net.minecraft.world.item.Item item = net.minecraftforge.registries.ForgeRegistries.ITEMS.getValue(rl);
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
    }

    private record TipRow(String text, int layoutIndex, boolean iconLine) {}

    private boolean isEditingRow(TipRow row) {
        return row != null && editingIndex == row.layoutIndex();
    }

    class TipListWidget extends ObjectSelectionList<TipListWidget.Entry> {
        private final int listTop, listBottom;

        public TipListWidget(Minecraft mc, int w, int h, int t, int b, int ih) {
            super(mc, w, h, t, b, ih);
            this.listTop = t;
            this.listBottom = b;
            setRenderBackground(false);
            setRenderTopAndBottom(false);
            setRenderSelection(false);
            refresh();
        }

        @Override
        public void render(@NotNull GuiGraphics g, int mx, int my, float pt) {
            super.render(g, mx, my, pt);
            if (this.getMaxScroll() > 0) {
                int barX = this.getScrollbarPosition();
                int height = listBottom - listTop;
                int thumbH = Math.max(20, (int) ((float) height * height / this.getMaxPosition()));
                ScrollUtil.renderScrollbar(
                        g,
                        mx,
                        my,
                        barX,
                        listTop,
                        6,
                        height,
                        thumbH,
                        (int) Math.ceil(this.getMaxScroll()),
                        (int) Math.round(this.getScrollAmount()),
                        false
                );
            }
        }

        public void refresh() {
            clearEntries();
            rebuildDisplayRows();
            for (TipRow displayRow : displayRows) addEntry(new Entry(displayRow));
        }

        @Override public int getRowWidth() { return this.width - 20; }
        @Override protected int getScrollbarPosition() { return this.getLeft() + this.width - 2; }

        class Entry extends ObjectSelectionList.Entry<Entry> {
            private static final int DELETE_BUTTON_W = 44;
            private static final int DELETE_BUTTON_H = 18;

            private final TipRow row;
            private final Button deleteButton;
            protected int lastT;

            public Entry(TipRow row) {
                this.row = row;
                this.deleteButton = Button.builder(Component.translatable("gui.kineticarmory.armorsets.delete"), b -> deleteRow())
                        .bounds(0, 0, DELETE_BUTTON_W, DELETE_BUTTON_H)
                        .build();
            }

            @Override
            public void render(@NotNull GuiGraphics g, int index, int t, int l, int w, int h, int mx, int my, boolean hv, float pt) {
                this.lastT = t;

                if (draggingIndex == row.layoutIndex()) {
                    g.fill(l, t, l + w, t + 20, 0x44000000);
                    g.renderOutline(l, t, w, 20, 0x88555555);
                    return;
                }

                boolean editing = isEditingRow(row);
                int bgColor;
                if (!row.iconLine()) {
                    bgColor = editing ? 0xAA115511 : (hv ? 0x66555555 : 0x55333333);
                } else {
                    bgColor = editing ? 0xAA115511 : (hv ? 0x88777777 : ((index % 2 == 0) ? 0x88444444 : 0x88222222));
                }

                g.fill(l, t, l + w, t + 20, bgColor);

                if (editing) {
                    g.renderOutline(l, t, w, 20, 0xFFFFFFFF);
                } else {
                    g.renderOutline(l, t, w, 20, 0xFF555555);
                }

                int deleteX = l + w - DELETE_BUTTON_W - 5;
                int maxW = Math.max(0, deleteX - l - 8);
                renderTextWithIcons(g, Minecraft.getInstance().font, row.text(), l + 4, t + 6, maxW);

                deleteButton.setX(deleteX);
                deleteButton.setY(t + 1);
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

                if (Screen.hasControlDown() && btn == 0) {
                    draggingIndex = row.layoutIndex();
                    draggingText = row.text();
                    return true;
                }

                if (btn == 0) {
                    startEdit(row);
                    return true;
                }
                return false;
            }

            @Override public @NotNull Component getNarration() { return Component.empty(); }
        }
    }
}
