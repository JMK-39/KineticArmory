package dev.xyat.kineticarmory.armorsets.client.gui;

import dev.xyat.kineticarmory.util.ColorText;
import dev.xyat.kineticarmory.armorsets.data.ArmorDataConfig;
import dev.xyat.kineticcore.api.client.AdaptiveItemGridRenderer;
import dev.xyat.kineticcore.api.client.GuiRenderUtil;
import dev.xyat.kineticcore.api.client.GuiToastUtil;
import dev.xyat.kineticcore.api.client.ItemCache;
import dev.xyat.kineticcore.api.client.ItemSelectorScreen;
import dev.xyat.kineticcore.api.client.ScaledScreen;
import dev.xyat.kineticcore.api.client.gui.NbtEditorScreen;
import dev.xyat.kineticcore.api.client.gui.GridScrollController;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.ForgeRegistries;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class ArmorEquipmentVariantScreen extends ScaledScreen {
    private static final int PANEL_COLOR = 0xFF1C1C1C;
    private static final int PANEL_OUTLINE = 0xFF8A8A8A;
    private static final int LIST_COLOR = 0xCC050505;
    private static final int LIST_OUTLINE = 0xFF6A6A6A;
    private static final int ROW_H = 36;
    private static final int ROW_GAP = 4;
    private static final int ICON_BOX = 24;
    private static final int SCROLL_W = 6;
    private static final int ROW_BUTTON_W = 48;
    private static final int ROW_BUTTON_H = 18;
    private static final int ROW_BUTTON_GAP = 6;

    private final ScaledScreen parent;
    private final ArmorDataConfig config;
    private final String slotKey;

    private Button addItemButton;
    private Button addEquippedButton;
    private Button slotModeButton;
    private Button clearButton;
    private Button backButton;
    private final List<Button> modeButtons = new ArrayList<>();
    private final List<Button> nbtButtons = new ArrayList<>();
    private final List<Button> replaceButtons = new ArrayList<>();
    private final List<Button> deleteButtons = new ArrayList<>();

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
    private final GridScrollController listScroll = new GridScrollController();
    private int selectedIndex;

    public ArmorEquipmentVariantScreen(ScaledScreen parent, ArmorDataConfig config, String slotKey, Component slotName) {
        super(ColorText.translatable("gui.kineticarmory.armorsets.variant.title", slotName));
        this.parent = parent;
        this.config = config;
        this.slotKey = slotKey;
        configureResponsiveCanvas(
                640f,
                360f,
                6
        );
        this.selectedIndex = 0;
    }

    @Override
    public void tick() {
        super.tick();
        clampSelectionAndScroll();
        updateActionButtons();
    }

    @Override
    protected void initScaled() {
        config.getEquipmentVariants(slotKey);
        calculateLayout();

        int btnW = 96;
        int gap = 6;
        int controlY = panelY + 36;
        int startX = vWidth / 2 - (btnW * 5 + gap * 4) / 2;

        slotModeButton = Button.builder(getSlotModeButtonText(), b -> toggleSlotRequirementMode())
                .bounds(startX, controlY, btnW, 20)
                .build();
        addRenderableWidget(slotModeButton);

        addItemButton = Button.builder(ColorText.translatable("gui.kineticarmory.armorsets.variant.add_item"), b -> addFromSelector())
                .bounds(startX + (btnW + gap), controlY, btnW, 20)
                .build();
        addRenderableWidget(addItemButton);

        addEquippedButton = Button.builder(ColorText.translatable("gui.kineticarmory.armorsets.variant.add_equipped"), b -> addEquipped())
                .bounds(startX + (btnW + gap) * 2, controlY, btnW, 20)
                .build();
        addRenderableWidget(addEquippedButton);

        clearButton = Button.builder(ColorText.translatable("gui.kineticarmory.armorsets.variant.clear"), b -> clearAll())
                .bounds(startX + (btnW + gap) * 3, controlY, btnW, 20)
                .build();
        addRenderableWidget(clearButton);

        backButton = Button.builder(ColorText.translatable("gui.kineticarmory.armorsets.back"), b -> {
            if (minecraft != null) minecraft.setScreen(parent);
        }).bounds(startX + (btnW + gap) * 4, controlY, btnW, 20).build();
        addRenderableWidget(backButton);

        createRowButtons();
        clampSelectionAndScroll();
        updateActionButtons();
    }

    private void createRowButtons() {
        modeButtons.clear();
        nbtButtons.clear();
        replaceButtons.clear();
        deleteButtons.clear();

        for (int i = 0; i < visibleRows; i++) {
            int row = i;
            int rowY = rowY(row);
            int deleteX = listX + rowW - ROW_BUTTON_W - 7;
            int replaceX = deleteX - ROW_BUTTON_W - ROW_BUTTON_GAP;
            int nbtX = replaceX - ROW_BUTTON_W - ROW_BUTTON_GAP;
            int modeX = nbtX - ROW_BUTTON_W - ROW_BUTTON_GAP;
            int buttonY = rowY + (ROW_H - ROW_BUTTON_H) / 2;

            Button mode = Button.builder(ColorText.translatable("gui.kineticarmory.armorsets.variant.mode"), b -> toggleVisibleRowNbtMode(row))
                    .bounds(modeX, buttonY, ROW_BUTTON_W, ROW_BUTTON_H)
                    .build();
            modeButtons.add(mode);
            addRenderableWidget(mode);

            Button nbt = Button.builder(ColorText.translatable("gui.kineticarmory.armorsets.variant.nbt"), b -> editVisibleRowNbt(row))
                    .bounds(nbtX, buttonY, ROW_BUTTON_W, ROW_BUTTON_H)
                    .build();
            nbtButtons.add(nbt);
            addRenderableWidget(nbt);

            Button replace = Button.builder(ColorText.translatable("gui.kineticarmory.armorsets.variant.edit"), b -> replaceVisibleRowFromSelector(row))
                    .bounds(replaceX, buttonY, ROW_BUTTON_W, ROW_BUTTON_H)
                    .build();
            replaceButtons.add(replace);
            addRenderableWidget(replace);

            Button delete = Button.builder(ColorText.translatable("gui.kineticarmory.armorsets.delete"), b -> deleteVisibleRow(row))
                    .bounds(deleteX, buttonY, ROW_BUTTON_W, ROW_BUTTON_H)
                    .build();
            deleteButtons.add(delete);
            addRenderableWidget(delete);
        }
    }

    private void calculateLayout() {
        panelW = Math.min(vWidth - 30, 620);
        panelH = vHeight - 32;
        panelX = vWidth / 2 - panelW / 2;
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
            case EMPTY -> ColorText.translatable("gui.kineticarmory.armorsets.slot_mode.empty");
            case ANY -> ColorText.translatable("gui.kineticarmory.armorsets.slot_mode.any");
            default -> ColorText.translatable("gui.kineticarmory.armorsets.slot_mode.normal");
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
        GuiToastUtil.showToast(ColorText.translatable("msg.kineticarmory.common.saved"));
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
        return listScroll.offset() + visibleRow;
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
        return stack.isEmpty() ? "minecraft:air" : Objects.requireNonNull(ForgeRegistries.ITEMS.getKey(stack.getItem())).toString();
    }

    private void addFromSelector() {
        if (isSpecialSlotMode() || minecraft == null) return;
        ItemCache.prepareCache(() -> minecraft.setScreen(new ItemSelectorScreen(this, selection -> {
            if (!selection.isItem()) return;
            ItemStack stack = selection.stack();
            if (stack.isEmpty()) return;
            variants().add(createReq(stack));
            selectedIndex = variants().size() - 1;
            config.normalizeEquipmentVariants();
            GuiToastUtil.showToast(ColorText.translatable("msg.kineticarmory.common.saved"));
            clampSelectionAndScroll();
        })));
    }

    private void addEquipped() {
        if (isSpecialSlotMode()) return;
        ItemStack stack = getEquippedStackForSlot();
        if (stack.isEmpty()) {
            GuiToastUtil.showToast(ColorText.translatable("msg.kineticarmory.armorsets.variant.empty_equipped"));
            return;
        }
        variants().add(createReq(stack));
        selectedIndex = variants().size() - 1;
        config.normalizeEquipmentVariants();
        GuiToastUtil.showToast(ColorText.translatable("msg.kineticarmory.common.saved"));
        clampSelectionAndScroll();
    }

    private ItemStack getEquippedStackForSlot() {
        Player player = Minecraft.getInstance().player;
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
        GuiToastUtil.showToast(ColorText.translatable("msg.kineticarmory.common.saved"));
        updateActionButtons();
    }

    private void replaceVisibleRowFromSelector(int visibleRow) {
        int index = indexOfVisibleRow(visibleRow);
        selectIndex(index);
        replaceFromSelector(selectedReq());
    }

    private void replaceFromSelector(ArmorDataConfig.ItemReq req) {
        if (minecraft == null || req == null) return;
        ItemCache.prepareCache(() -> minecraft.setScreen(new ItemSelectorScreen(this, selection -> {
            if (!selection.isItem()) return;
            ItemStack stack = selection.stack();
            if (stack.isEmpty()) return;
            ArmorDataConfig.ItemReq newReq = createReq(stack);
            req.id = newReq.id;
            req.nbtTag = newReq.nbtTag;
            if (req.nbtMode == null) req.nbtMode = "NONE";
            config.normalizeEquipmentVariants();
            GuiToastUtil.showToast(ColorText.translatable("msg.kineticarmory.common.saved"));
            clampSelectionAndScroll();
        })));
    }

    private void editVisibleRowNbt(int visibleRow) {
        int index = indexOfVisibleRow(visibleRow);
        selectIndex(index);
        editNbt(selectedReq());
    }

    private void editNbt(ArmorDataConfig.ItemReq req) {
        if (!canEditNbt(req) || minecraft == null) return;
        String initNbt = (req.nbtTag != null && !req.nbtTag.trim().isEmpty()) ? req.nbtTag : "";
        minecraft.setScreen(new NbtEditorScreen(initNbt, savedNbt -> {
            req.nbtTag = savedNbt;
            if (req.nbtMode == null || req.nbtMode.equals("NONE")) req.nbtMode = "WEAK";
            config.normalizeEquipmentVariants();
            GuiToastUtil.showToast(ColorText.translatable("msg.kineticarmory.common.saved"));
        }, this));
    }

    private void toggleVisibleRowNbtMode(int visibleRow) {
        int index = indexOfVisibleRow(visibleRow);
        selectIndex(index);
        toggleNbtMode(selectedReq());
    }

    private void toggleNbtMode(ArmorDataConfig.ItemReq req) {
        if (!canEditNbt(req)) return;
        req.nbtMode = ("NONE".equals(req.nbtMode) || req.nbtMode == null) ? "WEAK" : ("WEAK".equals(req.nbtMode) ? "STRONG" : "NONE");
        GuiToastUtil.showToast(ColorText.translatable("msg.kineticarmory.common.saved"));
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
        GuiToastUtil.showToast(ColorText.translatable("msg.kineticarmory.common.deleted"));
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
        return ColorText.translatable("gui.kineticarmory.armorsets.nbt." + normalizeNbtMode(mode).toLowerCase());
    }

    private String normalizeNbtMode(String mode) {
        if (mode == null) return "NONE";
        String value = mode.toUpperCase();
        if ("WEAK".equals(value) || "STRONG".equals(value)) return value;
        return "NONE";
    }

    private List<Component> getNbtModeHelp(String mode) {
        return List.of(
                ColorText.translatable("gui.kineticarmory.armorsets.nbt_prefix", getNbtModeName(mode)),
                ColorText.translatable("gui.kineticarmory.armorsets.tooltip.rclick"),
                ColorText.translatable("gui.kineticarmory.armorsets.tooltip.shift_edit_nbt")
        );
    }

    private void updateActionButtons() {
        List<ArmorDataConfig.ItemReq> list = variants();
        SlotRequirementMode mode = getSlotRequirementMode();
        boolean normalMode = mode == SlotRequirementMode.NORMAL;
        if (slotModeButton != null) slotModeButton.setMessage(getSlotModeButtonText());
        if (addItemButton != null) addItemButton.active = normalMode;
        if (addEquippedButton != null) addEquippedButton.active = normalMode;
        if (clearButton != null) clearButton.active = !normalMode || !list.isEmpty();
        for (int i = 0; i < visibleRows; i++) {
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

    private void setButtonState(List<Button> buttons, int index, boolean visible, boolean active) {
        if (index < 0 || index >= buttons.size()) return;
        Button button = buttons.get(index);
        button.visible = visible;
        button.active = active;
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
    protected void renderScaledBackground(@NotNull GuiGraphics g, int mx, int my, float pt) {
        calculateLayout();
        clampSelectionAndScroll();
        updateActionButtons();
        GuiRenderUtil.drawPanel(g, panelX, panelY, panelW, panelH, PANEL_COLOR, PANEL_OUTLINE);
        g.renderOutline(panelX + 1, panelY + 1, panelW - 2, panelH - 2, 0xFF3A3A3A);
        renderListArea(g, mx, my);
    }

    @Override
    protected void renderScaledForeground(@NotNull GuiGraphics g, int mx, int my, float pt) {
        g.drawCenteredString(font, title, vWidth / 2, panelY + 11, 0xFFFFFFFF);
    }

    @Override
    protected void renderTooltips(GuiGraphics g, int scaledMouseX, int scaledMouseY, int mouseX, int mouseY) {
        List<Component> tooltip = getTooltipAt(scaledMouseX, scaledMouseY);
        if (!tooltip.isEmpty()) g.renderComponentTooltip(font, tooltip, mouseX, mouseY);
    }

    private void renderListArea(GuiGraphics g, int mx, int my) {
        GuiRenderUtil.drawPanel(g, listX, listY, listW, listH, LIST_COLOR, LIST_OUTLINE);
        g.renderOutline(listX + 1, listY + 1, listW - 2, listH - 2, 0xFF3A3A3A);

        SlotRequirementMode mode = getSlotRequirementMode();
        if (mode == SlotRequirementMode.EMPTY) {
            g.drawCenteredString(font, ColorText.translatable("gui.kineticarmory.armorsets.variant.mode_empty_note"), listX + listW / 2, listY + 18, 0xFFFF5555);
            return;
        }
        if (mode == SlotRequirementMode.ANY) {
            g.drawCenteredString(font, ColorText.translatable("gui.kineticarmory.armorsets.variant.mode_any_note"), listX + listW / 2, listY + 18, 0xFF55FF55);
            return;
        }
        if (variants().isEmpty()) {
            g.drawCenteredString(font, ColorText.translatable("gui.kineticarmory.armorsets.variant.empty"), listX + listW / 2, listY + 18, 0xFFAAAAAA);
        }

        renderRows(g, mx, my);
        renderScrollbar(g, mx, my);
    }

    private void renderRows(GuiGraphics g, int mx, int my) {
        List<ArmorDataConfig.ItemReq> list = variants();
        int start = listScroll.offset();
        int end = Math.min(list.size(), start + visibleRows);
        for (int index = start; index < end; index++) {
            int visibleRow = index - start;
            int rowY = rowY(visibleRow);
            int rowX = listX + 6;
            ArmorDataConfig.ItemReq req = list.get(index);
            boolean selected = index == selectedIndex;
            boolean hover = GuiRenderUtil.isHovering(mx, my, rowX, rowY, rowW, ROW_H);
            int bg = selected ? 0xAA775500 : (hover ? 0x88444444 : ((index % 2 == 0) ? 0x88333333 : 0x88222222));
            int outline = selected ? 0xFFFFB000 : (hover ? 0xFF55FFFF : 0xFF707070);

            g.fill(rowX, rowY, rowX + rowW, rowY + ROW_H, bg);
            g.renderOutline(rowX, rowY, rowW, ROW_H, outline);

            int iconX = rowX + 8;
            int iconY = rowY + 6;
            AdaptiveItemGridRenderer.drawSlot(g, iconX, iconY, ICON_BOX, 4, false);
            renderReqIcon(g, req, iconX + 4, iconY + 4);

            int textX = iconX + ICON_BOX + 10;
            int firstButtonX = listX + rowW - 7 - ROW_BUTTON_W * 4 - ROW_BUTTON_GAP * 3;
            int textW = Math.max(20, firstButtonX - textX - 8);
            drawTrimmedText(g, getReqName(req).getString(), textX, rowY + 6, textW);
            drawInfoLine(g, req, textX, rowY + 20, textW);
        }
    }

    private void drawInfoLine(GuiGraphics g, ArmorDataConfig.ItemReq req, int x, int y, int maxWidth) {
        String idLabel = "ID: ";
        String id = req == null || req.id == null ? "" : req.id;
        String nbtLabel = "  " + ColorText.translatable("gui.kineticarmory.armorsets.variant.nbt").getString().replace("§b", "") + ": ";
        String nbt = getNbtModeName(req == null ? "NONE" : req.nbtMode).getString();
        String full = idLabel + id + nbtLabel + nbt;
        String trimmed = font.plainSubstrByWidth(full, Math.max(0, maxWidth));
        int cursor = x;
        if (trimmed.startsWith(idLabel)) {
            g.drawString(font, idLabel, cursor, y, 0xFF55FFFF, false);
            cursor += font.width(idLabel);
            String rest = trimmed.substring(idLabel.length());
            int nbtStart = rest.indexOf(nbtLabel);
            if (nbtStart >= 0) {
                String idPart = rest.substring(0, nbtStart);
                g.drawString(font, idPart, cursor, y, 0xFFDDDDDD, false);
                cursor += font.width(idPart);
                g.drawString(font, nbtLabel, cursor, y, 0xFFFFFF55, false);
                cursor += font.width(nbtLabel);
                g.drawString(font, rest.substring(nbtStart + nbtLabel.length()), cursor, y, 0xFFDDDDDD, false);
            } else {
                g.drawString(font, rest, cursor, y, 0xFFDDDDDD, false);
            }
        } else {
            g.drawString(font, trimmed, x, y, 0xFFDDDDDD, false);
        }
    }

    private void renderScrollbar(GuiGraphics g, int mx, int my) {
        listScroll.update(variants().size(), visibleRows);
        listScroll.render(
                g, mx, my,
                scrollX, scrollY,
                SCROLL_W, scrollH,
                18
        );
    }



    private void renderReqIcon(GuiGraphics g, ArmorDataConfig.ItemReq req, int x, int y) {
        if (req == null || req.id == null) return;
        if ("EMPTY".equalsIgnoreCase(req.id)) {
            g.drawCenteredString(font, "X", x + 8, y + 4, 0xFFFF5555);
            return;
        }
        if ("ANY".equalsIgnoreCase(req.id)) {
            g.drawCenteredString(font, "?", x + 8, y + 4, 0xFF55FF55);
            return;
        }

        ItemStack stack = req.createDisplayStack();
        if (!stack.isEmpty()) {
            g.renderItem(stack, x, y);
            String nbtStr = "WEAK".equals(req.nbtMode) ? "W" : ("STRONG".equals(req.nbtMode) ? "S" : "");
            if (!nbtStr.isEmpty()) g.renderItemDecorations(font, stack, x, y, nbtStr);
        }
    }

    private Component getReqName(ArmorDataConfig.ItemReq req) {
        if (req == null || req.id == null) return ColorText.translatable("gui.kineticarmory.armorsets.variant.invalid");
        if ("EMPTY".equalsIgnoreCase(req.id)) return ColorText.translatable("gui.kineticarmory.armorsets.slot_state.empty").withStyle(ChatFormatting.RED);
        if ("ANY".equalsIgnoreCase(req.id)) return ColorText.translatable("gui.kineticarmory.armorsets.slot_state.any").withStyle(ChatFormatting.GREEN);
        ItemStack stack = req.createDisplayStack();
        return stack.isEmpty() ? Component.literal(req.id) : stack.getHoverName();
    }

    private void drawTrimmedText(GuiGraphics g, String text, int x, int y, int maxWidth) {
        g.drawString(font, font.plainSubstrByWidth(text == null ? "" : text, Math.max(0, maxWidth)), x, y, -1, false);
    }

    private List<Component> getTooltipAt(int mx, int my) {
        List<Component> buttonTooltip = getButtonTooltip(mx, my);
        if (!buttonTooltip.isEmpty()) return buttonTooltip;
        return getRowTooltip(mx, my);
    }

    private List<Component> getButtonTooltip(int mx, int my) {
        if (isButtonHovered(slotModeButton, mx, my)) return List.of(ColorText.translatable("gui.kineticarmory.armorsets.variant.tooltip.slot_mode"));
        if (isButtonHovered(addItemButton, mx, my)) return isSpecialSlotMode() ? List.of(ColorText.translatable("gui.kineticarmory.armorsets.variant.tooltip.special_no_add")) : List.of(ColorText.translatable("gui.kineticarmory.armorsets.variant.tooltip.add_item"));
        if (isButtonHovered(addEquippedButton, mx, my)) return isSpecialSlotMode() ? List.of(ColorText.translatable("gui.kineticarmory.armorsets.variant.tooltip.special_no_add")) : List.of(ColorText.translatable("gui.kineticarmory.armorsets.variant.tooltip.add_equipped"));
        if (isButtonHovered(clearButton, mx, my)) return List.of(ColorText.translatable("gui.kineticarmory.armorsets.variant.tooltip.clear"));
        if (isButtonHovered(backButton, mx, my)) return List.of(ColorText.translatable("gui.kineticarmory.armorsets.back"));
        for (int i = 0; i < visibleRows; i++) {
            ArmorDataConfig.ItemReq req = reqOfVisibleRow(i);
            if (isButtonHovered(modeButtons, i, mx, my)) return getNbtModeHelp(req == null ? "NONE" : req.nbtMode);
            if (isButtonHovered(nbtButtons, i, mx, my)) return List.of(ColorText.translatable("gui.kineticarmory.armorsets.tooltip.shift_edit_nbt"));
            if (isButtonHovered(replaceButtons, i, mx, my)) return List.of(ColorText.translatable("gui.kineticarmory.armorsets.tooltip.lclick"));
            if (isButtonHovered(deleteButtons, i, mx, my)) return List.of(ColorText.translatable("gui.kineticarmory.armorsets.delete"));
        }
        return List.of();
    }

    private boolean isButtonHovered(List<Button> buttons, int index, int mx, int my) {
        return index >= 0 && index < buttons.size() && isButtonHovered(buttons.get(index), mx, my);
    }

    private boolean isButtonHovered(Button button, int mx, int my) {
        return button != null && button.visible && button.isMouseOver(mx, my);
    }

    private List<Component> getRowTooltip(int mx, int my) {
        int index = getRowIndexAt(mx, my);
        List<ArmorDataConfig.ItemReq> list = variants();
        if (index < 0 || index >= list.size()) return List.of();
        ArmorDataConfig.ItemReq req = list.get(index);
        List<Component> tooltip = new ArrayList<>();
        tooltip.add(getReqName(req));
        tooltip.add(ColorText.translatable("gui.kineticarmory.armorsets.variant.row_sub", req.id, getNbtModeName(req.nbtMode)));
        tooltip.add(ColorText.translatable("gui.kineticarmory.armorsets.tooltip.lclick"));
        tooltip.add(ColorText.translatable("gui.kineticarmory.armorsets.tooltip.rclick"));
        if (canEditNbt(req)) tooltip.add(ColorText.translatable("gui.kineticarmory.armorsets.tooltip.shift_edit_nbt"));
        return tooltip;
    }

    private int getRowIndexAt(double mx, double my) {
        int rowX = listX + 6;
        if (mx < rowX || mx >= rowX + rowW || my < listY + 6 || my >= listY + listH - 6) return -1;
        int localY = (int)(my - listY - 6);
        int step = ROW_H + ROW_GAP;
        int visibleRow = localY / step;
        if (visibleRow < 0 || visibleRow >= visibleRows) return -1;
        if (localY % step >= ROW_H) return -1;
        return listScroll.offset() + visibleRow;
    }

    @Override
    protected boolean universalMouseClicked(double mx, double my, int btn) {
        if (tryStartScrollbarDrag(mx, my, btn)) return true;
        if (super.universalMouseClicked(mx, my, btn)) return true;

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

    private boolean tryStartScrollbarDrag(double mx, double my, int btn) {
        if (btn != 0 || isSpecialSlotMode()) return false;
        listScroll.update(variants().size(), visibleRows);
        return listScroll.beginDrag(
                mx, my,
                scrollX, scrollY,
                SCROLL_W, scrollH,
                18, 5
        );
    }

    @Override
    protected boolean universalMouseDragged(double mx, double my, int btn, double dx, double dy) {
        if (listScroll.drag(my, scrollY, scrollH, 18)) {
            updateActionButtons();
            return true;
        }
        return super.universalMouseDragged(mx, my, btn, dx, dy);
    }

    @Override
    protected boolean universalMouseReleased(double mx, double my, int btn) {
        if (listScroll.release(btn)) return true;
        return super.universalMouseReleased(mx, my, btn);
    }

    @Override
    protected boolean universalMouseScrolled(double mx, double my, double delta) {
        if (!isSpecialSlotMode()
                && GuiRenderUtil.isHovering(mx, my, listX, listY, listW, listH)) {
            listScroll.update(variants().size(), visibleRows);
            if (listScroll.scroll(delta)) {
                updateActionButtons();
                return true;
            }
        }
        return super.universalMouseScrolled(mx, my, delta);
    }

}
