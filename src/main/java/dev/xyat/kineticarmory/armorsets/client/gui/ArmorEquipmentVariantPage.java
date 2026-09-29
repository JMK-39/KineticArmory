package dev.xyat.kineticarmory.armorsets.client.gui;

import dev.xyat.kineticcore.api.client.gui.input.ScrollInput;
import dev.xyat.kineticcore.api.client.gui.input.MouseDragInput;
import dev.xyat.kineticcore.api.client.gui.input.MouseInput;
import dev.xyat.kineticcore.api.client.gui.overlay.KineticOverlays;
import dev.xyat.kineticcore.api.client.gui.page.KineticPage;
import dev.xyat.kineticcore.api.client.gui.render.KineticGraphics;
import dev.xyat.kineticcore.api.client.gui.scroll.KineticScrollController;
import dev.xyat.kineticcore.api.client.gui.selector.KineticSelectors;
import dev.xyat.kineticcore.api.client.gui.theme.KineticTheme;
import dev.xyat.kineticcore.api.client.gui.ui.KineticUi;
import dev.xyat.kineticcore.api.client.gui.widget.*;
import dev.xyat.kineticcore.api.client.gui.widget.list.*;

import dev.xyat.kineticcore.api.client.input.KineticMouseButtons;
import dev.xyat.kineticarmory.armorsets.data.ArmorDataConfig;
import dev.xyat.kineticcore.api.registry.KineticRegistries;
import dev.xyat.kineticcore.api.runtime.KineticClientRuntime;
import dev.xyat.kineticcore.api.client.gui.input.MouseButton;
import dev.xyat.kineticcore.api.text.KineticI18n;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

public class ArmorEquipmentVariantPage extends KineticPage {
    private static final int PANEL_COLOR = 0xFF1C1C1C;
    private static final int PANEL_OUTLINE = 0xFF8A8A8A;
    private static final int LIST_COLOR = 0xCC050505;
    private static final int LIST_OUTLINE = 0xFF6A6A6A;
    private static final int ROW_H = 36;
    private static final int ROW_GAP = 4;
    private static final int ICON_BOX = 24;
    private static final int SCROLL_W = 4;
    private static final int ROW_BUTTON_W = 48;
    private static final int ROW_BUTTON_H = 18;
    private static final int ROW_BUTTON_GAP = 6;

    
    private final ArmorDataConfig config;
    private final String slotKey;

    private KineticButton addItemButton;
    private KineticButton addEquippedButton;
    private KineticButton slotModeButton;
    private KineticButton clearButton;
    private KineticButton backButton;
    private final List<KineticButton> modeButtons = new ArrayList<>();
    private final List<KineticButton> nbtButtons = new ArrayList<>();
    private final List<KineticButton> replaceButtons = new ArrayList<>();
    private final List<KineticButton> deleteButtons = new ArrayList<>();

    private int panelX;
    private int panelY;
    private int panelW;
    private int panelH;
    private int listX;
    private int listY;
    private int listW;
    private int listH;
    private int rowW;
    private int scrollX;
    private int scrollY;
    private int scrollH;
    private int visibleRows;
    private final KineticScrollController listScroll = new KineticScrollController();
    private int selectedIndex;

    public ArmorEquipmentVariantPage(ArmorDataConfig config, String slotKey, Component slotName) {
        super(KineticI18n.translatable("gui.kineticarmory.armorsets.variant.title", slotName));
        this.config = config;
        this.slotKey = slotKey;
        this.selectedIndex = 0;
    }

    @Override
    protected void onTick() {
        clampSelectionAndScroll();
        updateActionButtons();
    }

    @Override
    protected void build(KineticUi ui) {
        config.getEquipmentVariants(slotKey);
        calculateLayout();

        int btnW = 96;
        int gap = 6;
        int controlY = panelY + 36;
        int startX = width() / 2 - (btnW * 5 + gap * 4) / 2;

        slotModeButton = ui().button(startX, controlY, btnW).text(getSlotModeButtonText()).onClick(b -> toggleSlotRequirementMode()).build();
addItemButton = ui().button(startX + (btnW + gap), controlY, btnW).text(KineticI18n.translatable("gui.kineticarmory.armorsets.variant.add_item")).onClick(b -> addFromSelector()).build();
addEquippedButton = ui().button(startX + (btnW + gap) * 2, controlY, btnW).text(KineticI18n.translatable("gui.kineticarmory.armorsets.variant.add_equipped")).onClick(b -> addEquipped()).build();
clearButton = ui().button(startX + (btnW + gap) * 3, controlY, btnW).text(KineticI18n.translatable("gui.kineticarmory.armorsets.variant.clear")).onClick(b -> clearAll()).build();
backButton = ui().button(startX + (btnW + gap) * 4, controlY, btnW).text(KineticI18n.translatable("gui.kineticarmory.armorsets.back")).onClick(b -> navigateBack()).build();
createRowButtons();
        clampSelectionAndScroll();
        updateActionButtons();
    }

    private void createRowButtons() {
        modeButtons.clear();
        nbtButtons.clear();
        replaceButtons.clear();
        deleteButtons.clear();

        for (int i = 0; i <= visibleRows; i++) {
            int row = i;
            int rowY = rowY(row);
            int deleteX = listX + rowW - ROW_BUTTON_W - 7;
            int replaceX = deleteX - ROW_BUTTON_W - ROW_BUTTON_GAP;
            int nbtX = replaceX - ROW_BUTTON_W - ROW_BUTTON_GAP;
            int modeX = nbtX - ROW_BUTTON_W - ROW_BUTTON_GAP;
            int buttonY = rowY + (ROW_H - ROW_BUTTON_H) / 2;

            KineticButton mode = ui().button(modeX, buttonY, ROW_BUTTON_W).text(KineticI18n.translatable("gui.kineticarmory.armorsets.variant.mode")).onClick(b -> toggleVisibleRowNbtMode(row)).build();
            modeButtons.add(mode);
KineticButton nbt = ui().button(nbtX, buttonY, ROW_BUTTON_W).text(KineticI18n.translatable("gui.kineticarmory.armorsets.variant.nbt")).onClick(b -> editVisibleRowNbt(row)).build();
            nbtButtons.add(nbt);
KineticButton replace = ui().button(replaceX, buttonY, ROW_BUTTON_W).text(KineticI18n.translatable("gui.kineticarmory.armorsets.variant.edit")).onClick(b -> replaceVisibleRowFromSelector(row)).build();
            replaceButtons.add(replace);
KineticButton delete = ui().button(deleteX, buttonY, ROW_BUTTON_W).text(KineticI18n.translatable("gui.kineticarmory.armorsets.delete")).onClick(b -> deleteVisibleRow(row)).build();
            deleteButtons.add(delete);
}
    }

    private void calculateLayout() {
        panelW = Math.min(width() - 30, 620);
        panelH = height() - 32;
        panelX = width() / 2 - panelW / 2;
        panelY = 16;

        listX = panelX + 18;
        listY = panelY + 66;
        listW = panelW - 36;
        listH = panelH - 84;
        rowW = listW - SCROLL_W - 12;

        scrollX = listX + listW - SCROLL_W - 5;
        scrollY = listY + 6;
        scrollH = Math.max(1, listH - 12);
        visibleRows = Math.max(1, (listH - 12 + ROW_GAP) / (ROW_H + ROW_GAP));
    }

    private int rowY(int visibleRow) {
        return listY + 6 + visibleRow * (ROW_H + ROW_GAP);
    }

    private List<ArmorDataConfig.ItemReq> variants() {
        return config.getEquipmentVariants(slotKey);
    }

    private enum SlotRequirementMode {
        NORMAL,
        EMPTY,
        ANY
    }

    private SlotRequirementMode getSlotRequirementMode() {
        List<ArmorDataConfig.ItemReq> list = variants();
        if (ArmorDataConfig.isExclusiveSlotState(list, "EMPTY")) return SlotRequirementMode.EMPTY;
        if (ArmorDataConfig.isExclusiveSlotState(list, "ANY")) return SlotRequirementMode.ANY;
        return SlotRequirementMode.NORMAL;
    }

    private boolean isSpecialSlotMode() {
        return getSlotRequirementMode() != SlotRequirementMode.NORMAL;
    }

    private Component getSlotModeButtonText() {
        return getSlotModeName(getSlotRequirementMode());
    }

    private Component getSlotModeName(SlotRequirementMode mode) {
        return switch (mode) {
            case EMPTY -> KineticI18n.translatable("gui.kineticarmory.armorsets.slot_mode.empty");
            case ANY -> KineticI18n.translatable("gui.kineticarmory.armorsets.slot_mode.any");
            default -> KineticI18n.translatable("gui.kineticarmory.armorsets.slot_mode.normal");
        };
    }

    private void toggleSlotRequirementMode() {
        SlotRequirementMode mode = getSlotRequirementMode();
        if (mode == SlotRequirementMode.NORMAL) {
            setSlotRequirementMode(SlotRequirementMode.EMPTY);
        } else if (mode == SlotRequirementMode.EMPTY) {
            setSlotRequirementMode(SlotRequirementMode.ANY);
        } else {
            setSlotRequirementMode(SlotRequirementMode.NORMAL);
        }
        KineticOverlays.toast(KineticI18n.translatable("msg.kineticarmory.common.saved"));
        clampSelectionAndScroll();
        updateActionButtons();
    }

    private void setSlotRequirementMode(SlotRequirementMode mode) {
        if (mode == SlotRequirementMode.EMPTY) {
            ArmorDataConfig.ItemReq req = ArmorDataConfig.ItemReq.create("EMPTY");
            req.nbtMode = "NONE";
            req.nbtTag = "{}";
            config.setSingleEquipmentVariant(slotKey, req);
            selectedIndex = 0;
        } else if (mode == SlotRequirementMode.ANY) {
            ArmorDataConfig.ItemReq req = ArmorDataConfig.ItemReq.create("ANY");
            req.nbtMode = "NONE";
            req.nbtTag = "{}";
            config.setSingleEquipmentVariant(slotKey, req);
            selectedIndex = 0;
        } else {
            variants().clear();
            if (config.equipment != null) config.equipment.remove(slotKey);
            if (config.equipmentVariants != null) config.equipmentVariants.remove(slotKey);
            selectedIndex = -1;
            listScroll.reset();
        }
        config.normalizeEquipmentVariants();
    }

    private ArmorDataConfig.ItemReq selectedReq() {
        List<ArmorDataConfig.ItemReq> list = variants();
        if (selectedIndex < 0 || selectedIndex >= list.size()) return null;
        return list.get(selectedIndex);
    }

    private int indexOfVisibleRow(int visibleRow) {
        return listScroll.smoothIndexOffset() + visibleRow;
    }

    private ArmorDataConfig.ItemReq reqOfVisibleRow(int visibleRow) {
        int index = indexOfVisibleRow(visibleRow);
        List<ArmorDataConfig.ItemReq> list = variants();
        if (index < 0 || index >= list.size()) return null;
        return list.get(index);
    }

    private ArmorDataConfig.ItemReq createReq(ItemStack stack) {
        ArmorDataConfig.ItemReq req = ArmorDataConfig.ItemReq.create(getId(stack));
        if (!stack.isEmpty() && stack.hasTag() && stack.getTag() != null) req.nbtTag = stack.getTag().toString();
        return req;
    }

    private String getId(ItemStack stack) {
        return stack.isEmpty() ? "minecraft:air" : KineticRegistries.items().id(stack.getItem()).toString();
    }

    private void addFromSelector() {
        if (isSpecialSlotMode()) return;
        KineticSelectors.openItemSelector(selection -> {
            if (!selection.isItem()) return;
            ItemStack stack = selection.stack();
            if (stack.isEmpty()) return;
            variants().add(createReq(stack));
            selectedIndex = variants().size() - 1;
            config.normalizeEquipmentVariants();
            KineticOverlays.toast(KineticI18n.translatable("msg.kineticarmory.common.saved"));
            clampSelectionAndScroll();
        });
    }

    private void addEquipped() {
        if (isSpecialSlotMode()) return;
        ItemStack stack = getEquippedStackForSlot();
        if (stack.isEmpty()) {
            KineticOverlays.toast(KineticI18n.translatable("msg.kineticarmory.armorsets.variant.empty_equipped"));
            return;
        }
        variants().add(createReq(stack));
        selectedIndex = variants().size() - 1;
        config.normalizeEquipmentVariants();
        KineticOverlays.toast(KineticI18n.translatable("msg.kineticarmory.common.saved"));
        clampSelectionAndScroll();
    }

    private ItemStack getEquippedStackForSlot() {
        Player player = KineticClientRuntime.localPlayer();
        if (player == null) return ItemStack.EMPTY;
        return switch (slotKey) {
            case "head" -> player.getItemBySlot(EquipmentSlot.HEAD);
            case "chest" -> player.getItemBySlot(EquipmentSlot.CHEST);
            case "legs" -> player.getItemBySlot(EquipmentSlot.LEGS);
            case "feet" -> player.getItemBySlot(EquipmentSlot.FEET);
            case "mainhand" -> player.getMainHandItem();
            case "offhand" -> player.getOffhandItem();
            default -> ItemStack.EMPTY;
        };
    }

    private void clearAll() {
        setSlotRequirementMode(SlotRequirementMode.NORMAL);
        KineticOverlays.toast(KineticI18n.translatable("msg.kineticarmory.common.saved"));
        updateActionButtons();
    }

    private void replaceVisibleRowFromSelector(int visibleRow) {
        int index = indexOfVisibleRow(visibleRow);
        selectIndex(index);
        replaceFromSelector(selectedReq());
    }

    private void replaceFromSelector(ArmorDataConfig.ItemReq req) {
        if (req == null) return;
        KineticSelectors.openItemSelector(selection -> {
            if (!selection.isItem()) return;
            ItemStack stack = selection.stack();
            if (stack.isEmpty()) return;
            ArmorDataConfig.ItemReq newReq = createReq(stack);
            req.id = newReq.id;
            req.nbtTag = newReq.nbtTag;
            if (req.nbtMode == null) req.nbtMode = "NONE";
            config.normalizeEquipmentVariants();
            KineticOverlays.toast(KineticI18n.translatable("msg.kineticarmory.common.saved"));
            clampSelectionAndScroll();
        });
    }

    private void editVisibleRowNbt(int visibleRow) {
        int index = indexOfVisibleRow(visibleRow);
        selectIndex(index);
        editNbt(selectedReq());
    }

    private void editNbt(ArmorDataConfig.ItemReq req) {
        if (!canEditNbt(req)) return;
        String initNbt = (req.nbtTag != null && !req.nbtTag.trim().isEmpty()) ? req.nbtTag : "";
        KineticSelectors.openNbtEditor(initNbt, savedNbt -> {
            req.nbtTag = savedNbt;
            if (req.nbtMode == null || req.nbtMode.equals("NONE")) req.nbtMode = "WEAK";
            config.normalizeEquipmentVariants();
            KineticOverlays.toast(KineticI18n.translatable("msg.kineticarmory.common.saved"));
        });
    }

    private void toggleVisibleRowNbtMode(int visibleRow) {
        int index = indexOfVisibleRow(visibleRow);
        selectIndex(index);
        toggleNbtMode(selectedReq());
    }

    private void toggleNbtMode(ArmorDataConfig.ItemReq req) {
        if (!canEditNbt(req)) return;
        req.nbtMode = ("NONE".equals(req.nbtMode) || req.nbtMode == null) ? "WEAK" : ("WEAK".equals(req.nbtMode) ? "STRONG" : "NONE");
        KineticOverlays.toast(KineticI18n.translatable("msg.kineticarmory.common.saved"));
    }

    private void deleteVisibleRow(int visibleRow) {
        deleteAt(indexOfVisibleRow(visibleRow));
    }

    private void deleteAt(int index) {
        List<ArmorDataConfig.ItemReq> list = variants();
        if (index < 0 || index >= list.size()) return;

        list.remove(index);

        if (list.isEmpty()) {
            if (config.equipmentVariants != null) config.equipmentVariants.remove(slotKey);
            if (config.equipment != null) config.equipment.remove(slotKey);
        } else if (config.equipment != null) {
            config.equipment.put(slotKey, list.get(0));
        }

        selectedIndex = Math.min(index, list.size() - 1);
        config.normalizeEquipmentVariants();
        KineticOverlays.toast(KineticI18n.translatable("msg.kineticarmory.common.deleted"));
        clampSelectionAndScroll();
        updateActionButtons();
    }

    private void selectIndex(int index) {
        List<ArmorDataConfig.ItemReq> list = variants();
        if (index < 0 || index >= list.size()) return;
        selectedIndex = index;
        clampSelectionAndScroll();
        updateActionButtons();
    }

    private boolean canEditNbt(ArmorDataConfig.ItemReq req) {
        return ArmorDataConfig.isPieceRequirement(req) && !"EMPTY".equalsIgnoreCase(req.id) && !"ANY".equalsIgnoreCase(req.id);
    }

    private Component getNbtModeName(String mode) {
        return KineticI18n.translatable("gui.kineticarmory.armorsets.nbt." + normalizeNbtMode(mode).toLowerCase());
    }

    private String normalizeNbtMode(String mode) {
        if (mode == null) return "NONE";
        String value = mode.toUpperCase();
        if ("WEAK".equals(value) || "STRONG".equals(value)) return value;
        return "NONE";
    }

    private List<Component> getNbtModeHelp(String mode) {
        return List.of(
                KineticI18n.translatable("gui.kineticarmory.armorsets.nbt_prefix", getNbtModeName(mode)),
                KineticI18n.translatable("gui.kineticarmory.armorsets.tooltip.rclick"),
                KineticI18n.translatable("gui.kineticarmory.armorsets.tooltip.shift_edit_nbt")
        );
    }

    private void updateActionButtons() {
        List<ArmorDataConfig.ItemReq> list = variants();
        SlotRequirementMode mode = getSlotRequirementMode();
        boolean normalMode = mode == SlotRequirementMode.NORMAL;
        if (slotModeButton != null) slotModeButton.setText(getSlotModeButtonText());
        if (addItemButton != null) addItemButton.setEnabled(normalMode);
        if (addEquippedButton != null) addEquippedButton.setEnabled(normalMode);
        if (clearButton != null) clearButton.setEnabled(!normalMode || !list.isEmpty());
        for (int i = 0; i <= visibleRows; i++) {
            int index = indexOfVisibleRow(i);
            boolean hasItem = normalMode && index >= 0 && index < list.size();
            ArmorDataConfig.ItemReq req = hasItem ? list.get(index) : null;
            boolean nbtAllowed = canEditNbt(req);
            setButtonState(modeButtons, i, hasItem, nbtAllowed);
            setButtonState(nbtButtons, i, hasItem, nbtAllowed);
            setButtonState(replaceButtons, i, hasItem, hasItem);
            setButtonState(deleteButtons, i, hasItem, hasItem);
        }
    }

    private void updateRowButtonPositions() {
        int shift = listScroll.visualShift(ROW_H + ROW_GAP);
        for (int i = 0; i < modeButtons.size(); i++) {
            int buttonY = rowY(i) - shift + (ROW_H - ROW_BUTTON_H) / 2;
            boolean inside = buttonY >= listY + 2 && buttonY + ROW_BUTTON_H <= listY + listH - 2;
            setRowButtonY(modeButtons, i, buttonY, inside);
            setRowButtonY(nbtButtons, i, buttonY, inside);
            setRowButtonY(replaceButtons, i, buttonY, inside);
            setRowButtonY(deleteButtons, i, buttonY, inside);
        }
    }

    private void setRowButtonY(List<KineticButton> buttons, int index, int y, boolean inside) {
        if (index < 0 || index >= buttons.size()) return;
        KineticButton button = buttons.get(index);
        button.moveControlY(y);
        button.setControlVisible(button.controlVisible() && inside);
    }

    private void setButtonState(List<KineticButton> buttons, int index, boolean visible, boolean active) {
        if (index < 0 || index >= buttons.size()) return;
        KineticButton button = buttons.get(index);
        button.setControlVisible(visible);
        button.setEnabled(active);
    }

    private void clampSelectionAndScroll() {
        calculateLayout();
        if (isSpecialSlotMode()) {
            selectedIndex = 0;
            listScroll.reset();
            return;
        }
        List<ArmorDataConfig.ItemReq> list = variants();
        if (list.isEmpty()) {
            selectedIndex = -1;
            listScroll.reset();
            return;
        }
        if (selectedIndex < 0) selectedIndex = 0;
        if (selectedIndex >= list.size()) selectedIndex = list.size() - 1;
        listScroll.update(list.size(), visibleRows);
        if (selectedIndex < listScroll.offset()) {
            listScroll.setOffset(selectedIndex);
        }
        if (selectedIndex >= listScroll.offset() + visibleRows) {
            listScroll.setOffset(selectedIndex - visibleRows + 1);
        }
    }

    @Override
    protected void renderBackground(KineticGraphics g, int mx, int my, float pt) {
        calculateLayout();
        clampSelectionAndScroll();
        updateActionButtons();
        updateRowButtonPositions();
        KineticTheme.panel(g, panelX, panelY, panelW, panelH);
        renderListArea(g, mx, my);
    }

    @Override
    protected void renderForeground(KineticGraphics g, int mx, int my, float pt) {
        g.centeredText(title(), width() / 2, panelY + 11, 0xFFFFFFFF, true);
    }

    @Override
    protected void renderTooltips(int mouseX, int mouseY) {
        List<Component> tooltip = getTooltipAt(mouseX, mouseY);
        if (!tooltip.isEmpty()) showTooltip(tooltip);
    }

    private void renderListArea(KineticGraphics g, int mx, int my) {
        KineticTheme.panelAlt(g, listX, listY, listW, listH);

        SlotRequirementMode mode = getSlotRequirementMode();
        if (mode == SlotRequirementMode.EMPTY) {
            g.centeredText(KineticI18n.translatable("gui.kineticarmory.armorsets.variant.mode_empty_note"), listX + listW / 2, listY + 18, 0xFFFF5555, true);
            return;
        }
        if (mode == SlotRequirementMode.ANY) {
            g.centeredText(KineticI18n.translatable("gui.kineticarmory.armorsets.variant.mode_any_note"), listX + listW / 2, listY + 18, 0xFF55FF55, true);
            return;
        }
        if (variants().isEmpty()) {
            g.centeredText(KineticI18n.translatable("gui.kineticarmory.armorsets.variant.empty"), listX + listW / 2, listY + 18, 0xFFAAAAAA, true);
        }

        renderRows(g, mx, my);
        renderScrollbar(g, mx, my);
    }

    private void renderRows(KineticGraphics g, int mx, int my) {
        List<ArmorDataConfig.ItemReq> list = variants();
        int start = listScroll.smoothIndexOffset();
        int shift = listScroll.visualShift(ROW_H + ROW_GAP);
        int end = Math.min(list.size(), start + visibleRows + 1);
                g.scissor(listX + 6, listY + 6, listX + listW - 6, listY + listH - 6);
        try {
for (int index = start; index < end; index++) {
            int visibleRow = index - start;
            int rowY = rowY(visibleRow) - shift;
            int rowX = listX + 6;
            ArmorDataConfig.ItemReq req = list.get(index);
            boolean selected = index == selectedIndex;
            boolean hover = KineticTheme.hovering(mx, my, rowX, rowY, rowW, ROW_H);
            KineticTheme.stateSurface(
                    g,
                    rowX,
                    rowY,
                    rowW,
                    ROW_H,
                    KineticTheme.Surface.PANEL_ALT,
                    selected,
                    hover,
                    false
            );

            int iconX = rowX + 8;
            int iconY = rowY + 6;
            KineticTheme.itemSlot(g, iconX, iconY, ICON_BOX, 4, false);
            renderReqIcon(g, req, iconX + 4, iconY + 4);

            int textX = iconX + ICON_BOX + 10;
            int firstButtonX = listX + rowW - 7 - ROW_BUTTON_W * 4 - ROW_BUTTON_GAP * 3;
            int textW = Math.max(20, firstButtonX - textX - 8);
            drawTrimmedText(g, getReqName(req).getString(), textX, rowY + 6, textW);
            drawInfoLine(g, req, textX, rowY + 20, textW);
        }
        } finally {
            g.endScissor();
        }
    }

    private void drawInfoLine(KineticGraphics g, ArmorDataConfig.ItemReq req, int x, int y, int maxWidth) {
        String id = req == null || req.id == null ? "" : req.id;
        Component line = KineticI18n.translatable(
                "gui.kineticarmory.armorsets.variant.row_sub",
                id,
                getNbtModeName(req == null ? "NONE" : req.nbtMode)
        );
        g.scrollingText(line, x, y, maxWidth, 0xFFFFFFFF, false);
    }

    private void renderScrollbar(KineticGraphics g, int mx, int my) {
        listScroll.update(variants().size(), visibleRows);
        listScroll.render(
                g, mx, my,
                scrollX, scrollY,
                SCROLL_W, scrollH,
                18
        );
    }



    private void renderReqIcon(KineticGraphics g, ArmorDataConfig.ItemReq req, int x, int y) {
        if (req == null || req.id == null) return;
        if ("EMPTY".equalsIgnoreCase(req.id)) {
            g.centeredText("X", x + 8, y + 4, 0xFFFF5555, true);
            return;
        }
        if ("ANY".equalsIgnoreCase(req.id)) {
            g.centeredText("?", x + 8, y + 4, 0xFF55FF55, true);
            return;
        }

        ItemStack stack = req.createDisplayStack();
        if (!stack.isEmpty()) {
            g.item(stack, x, y);
            String nbtStr = "WEAK".equals(req.nbtMode) ? "W" : ("STRONG".equals(req.nbtMode) ? "S" : "");
            if (!nbtStr.isEmpty()) g.itemDecorations(stack, x, y, nbtStr);
        }
    }

    private Component getReqName(ArmorDataConfig.ItemReq req) {
        if (req == null || req.id == null) return KineticI18n.translatable("gui.kineticarmory.armorsets.variant.invalid");
        if ("EMPTY".equalsIgnoreCase(req.id)) return KineticI18n.translatable("gui.kineticarmory.armorsets.slot_state.empty");
        if ("ANY".equalsIgnoreCase(req.id)) return KineticI18n.translatable("gui.kineticarmory.armorsets.slot_state.any");
        ItemStack stack = req.createDisplayStack();
        return stack.isEmpty() ? Component.literal(req.id) : stack.getHoverName();
    }

    private void drawTrimmedText(KineticGraphics g, String text, int x, int y, int maxWidth) {
        g.scrollingText(Component.literal(text == null ? "" : text), x, y, Math.max(0, maxWidth), -1, false);
    }

    private List<Component> getTooltipAt(int mx, int my) {
        List<Component> buttonTooltip = getButtonTooltip(mx, my);
        if (!buttonTooltip.isEmpty()) return buttonTooltip;
        return getRowTooltip(mx, my);
    }

    private List<Component> getButtonTooltip(int mx, int my) {
        if (isButtonHovered(slotModeButton, mx, my)) return List.of(KineticI18n.translatable("gui.kineticarmory.armorsets.variant.tooltip.slot_mode"));
        if (isButtonHovered(addItemButton, mx, my)) return isSpecialSlotMode() ? List.of(KineticI18n.translatable("gui.kineticarmory.armorsets.variant.tooltip.special_no_add")) : List.of(KineticI18n.translatable("gui.kineticarmory.armorsets.variant.tooltip.add_item"));
        if (isButtonHovered(addEquippedButton, mx, my)) return isSpecialSlotMode() ? List.of(KineticI18n.translatable("gui.kineticarmory.armorsets.variant.tooltip.special_no_add")) : List.of(KineticI18n.translatable("gui.kineticarmory.armorsets.variant.tooltip.add_equipped"));
        if (isButtonHovered(clearButton, mx, my)) return List.of(KineticI18n.translatable("gui.kineticarmory.armorsets.variant.tooltip.clear"));
        if (isButtonHovered(backButton, mx, my)) return List.of(KineticI18n.translatable("gui.kineticarmory.armorsets.back"));
        for (int i = 0; i <= visibleRows; i++) {
            ArmorDataConfig.ItemReq req = reqOfVisibleRow(i);
            if (isButtonHovered(modeButtons, i, mx, my)) return getNbtModeHelp(req == null ? "NONE" : req.nbtMode);
            if (isButtonHovered(nbtButtons, i, mx, my)) return List.of(KineticI18n.translatable("gui.kineticarmory.armorsets.tooltip.shift_edit_nbt"));
            if (isButtonHovered(replaceButtons, i, mx, my)) return List.of(KineticI18n.translatable("gui.kineticarmory.armorsets.tooltip.lclick"));
            if (isButtonHovered(deleteButtons, i, mx, my)) return List.of(KineticI18n.translatable("gui.kineticarmory.armorsets.delete"));
        }
        return List.of();
    }

    private boolean isButtonHovered(List<KineticButton> buttons, int index, int mx, int my) {
        return index >= 0 && index < buttons.size() && isButtonHovered(buttons.get(index), mx, my);
    }

    private boolean isButtonHovered(KineticButton button, int mx, int my) {
        return button != null && button.controlVisible() && button.contains(mx, my);
    }

    private List<Component> getRowTooltip(int mx, int my) {
        int index = getRowIndexAt(mx, my);
        List<ArmorDataConfig.ItemReq> list = variants();
        if (index < 0 || index >= list.size()) return List.of();
        ArmorDataConfig.ItemReq req = list.get(index);
        List<Component> tooltip = new ArrayList<>();
        tooltip.add(getReqName(req));
        tooltip.add(KineticI18n.translatable("gui.kineticarmory.armorsets.variant.row_sub", req.id, getNbtModeName(req.nbtMode)));
        tooltip.add(KineticI18n.translatable("gui.kineticarmory.armorsets.tooltip.lclick"));
        tooltip.add(KineticI18n.translatable("gui.kineticarmory.armorsets.tooltip.rclick"));
        if (canEditNbt(req)) tooltip.add(KineticI18n.translatable("gui.kineticarmory.armorsets.tooltip.shift_edit_nbt"));
        return tooltip;
    }

    private int getRowIndexAt(double mx, double my) {
        int rowX = listX + 6;
        if (mx < rowX || mx >= rowX + rowW || my < listY + 6 || my >= listY + listH - 6) return -1;
        int step = ROW_H + ROW_GAP;
        int localY = (int)(my - listY - 6) + listScroll.visualShift(step);
        int visibleRow = localY / step;
        if (visibleRow < 0 || visibleRow >= visibleRows) return -1;
        if (localY % step >= ROW_H) return -1;
        return listScroll.smoothIndexOffset() + visibleRow;
    }

    @Override
    protected boolean onMouseClick(MouseInput input) {
        double mx = input.x(), my = input.y(); int btn = input.rawButton();
        if (tryStartScrollbarDrag(mx, my, input.button())) return true;

        int index = getRowIndexAt(mx, my);
        List<ArmorDataConfig.ItemReq> list = variants();
        if (index >= 0 && index < list.size()) {
            selectIndex(index);
            clampSelectionAndScroll();
            updateActionButtons();
            return true;
        }

        return false;
    }

    private boolean tryStartScrollbarDrag(double mx, double my, MouseButton btn) {
        if (btn != MouseButton.LEFT || isSpecialSlotMode()) return false;
        listScroll.update(variants().size(), visibleRows);
        return listScroll.beginDrag(
                mx, my, btn,
                scrollX, scrollY,
                SCROLL_W, scrollH,
                18, 5
        );
    }

    @Override
    protected boolean onMouseDrag(MouseDragInput input) {
        double mx = input.x(), my = input.y(); int btn = input.rawButton(); double dx = input.deltaX(), dy = input.deltaY();
        if (listScroll.drag(my, scrollY, scrollH, 18)) {
            updateActionButtons();
            return true;
        }
        return false;
    }

    @Override
    protected boolean onMouseRelease(MouseInput input) {
        double mx = input.x(), my = input.y(); int btn = input.rawButton();
        if (listScroll.release(input.button())) return true;
        return false;
    }

    @Override
    protected boolean onMouseScroll(ScrollInput input) {
        double mx = input.x(), my = input.y(), delta = input.deltaY();
        if (!isSpecialSlotMode()
                && KineticTheme.hovering(mx, my, listX, listY, listW, listH)) {
            listScroll.update(variants().size(), visibleRows);
            if (listScroll.scroll(delta)) {
                updateActionButtons();
                return true;
            }
        }
        return false;
    }

}
