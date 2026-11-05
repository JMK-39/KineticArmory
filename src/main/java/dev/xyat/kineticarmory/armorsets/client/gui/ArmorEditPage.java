package dev.xyat.kineticarmory.armorsets.client.gui;

import dev.xyat.kineticcore.api.client.gui.input.MouseInput;
import dev.xyat.kineticcore.api.client.gui.overlay.KineticOverlays;
import dev.xyat.kineticcore.api.client.gui.page.KineticPage;
import dev.xyat.kineticcore.api.client.gui.render.KineticGraphics;
import dev.xyat.kineticcore.api.client.gui.selector.KineticSelectors;
import dev.xyat.kineticcore.api.client.gui.theme.KineticTheme;
import dev.xyat.kineticcore.api.client.gui.ui.KineticUi;
import dev.xyat.kineticcore.api.client.gui.widget.*;
import dev.xyat.kineticcore.api.client.gui.widget.list.*;

import dev.xyat.kineticcore.api.client.input.KineticMouseButtons;

import dev.xyat.kineticarmory.armorsets.Network.ArmorNetwork;
import dev.xyat.kineticarmory.armorsets.client.ArmorClientSnapshot;
import dev.xyat.kineticarmory.armorsets.data.ArmorDataConfig;
import dev.xyat.kineticarmory.armorsets.json.ArmorLoader;

import dev.xyat.kineticcore.api.runtime.KineticClientRuntime;
import dev.xyat.kineticcore.api.registry.KineticRegistries;
import dev.xyat.kineticcore.api.text.KineticI18n;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import top.theillusivec4.curios.api.CuriosApi;

import java.util.ArrayList;
import java.util.List;

public class ArmorEditPage extends KineticPage {

    // Standard item slots (the same size in every add-on), 2 px apart.
    private static final int SLOT_SIZE = dev.xyat.kineticcore.api.client.gui.theme.KineticSizes.ITEM_SLOT;
    private static final int PITCH = dev.xyat.kineticcore.api.client.gui.theme.KineticSizes.ITEM_PITCH;
    private static final int TEXT_GAP = 4;

    
    private final ArmorDataConfig config;
    private String savedId;
    private String pendingSavedId;
    private boolean savePending;

    private KineticAutoCompleteField idBox, nameBox;
    private Component warningMessage;
    private boolean idInputError;
    private boolean nameInputError;
    private String tempId = null;
    private String tempName = null;

    private final String[] vanillaSlots = {"head", "chest", "legs", "feet", "mainhand", "offhand"};
    // Accessory sections use the full page width and scroll when more accessories are added; one empty slot always follows the list.
    private static final int EXT_COLS = 25, EXT_ROWS = 4, EXT_BAR_GAP = 3, EXT_BAR_W = 4;
    private static final int EQUIPMENT_Y = 104, CURIO_Y = 144, REJECTED_Y = 256;
    private final dev.xyat.kineticcore.api.client.gui.scroll.KineticScrollController curioBar = new dev.xyat.kineticcore.api.client.gui.scroll.KineticScrollController();
    private final dev.xyat.kineticcore.api.client.gui.scroll.KineticScrollController rejectedBar = new dev.xyat.kineticcore.api.client.gui.scroll.KineticScrollController();
    private dev.xyat.kineticcore.api.client.gui.scroll.KineticScrollController draggingBar;

    public ArmorEditPage(ArmorDataConfig config) {
        super(KineticI18n.translatable("gui.kineticarmory.armorsets.edit.title"));
        useCanvas(CANVAS_WIDTH, CANVAS_HEIGHT, 4);
        this.config = config;
        this.savedId = config.id;

        if (this.config.equipment == null) this.config.equipment = new java.util.HashMap<>();
        if (this.config.equipmentVariants == null) this.config.equipmentVariants = new java.util.HashMap<>();
        this.config.normalizeEquipmentVariants();
        if (this.config.curios == null) this.config.curios = new ArrayList<>();
        if (this.config.rejectedCurios == null) this.config.rejectedCurios = new ArrayList<>();
        if (this.config.attributes == null) this.config.attributes = new ArrayList<>();
        if (this.config.potionEffects == null) this.config.potionEffects = new ArrayList<>();
        if (this.config.damageImmunities == null) this.config.damageImmunities = new ArrayList<>();
        if (this.config.effectImmunities == null) this.config.effectImmunities = new ArrayList<>();
        if (this.config.attackEffects == null) this.config.attackEffects = new ArrayList<>();
        if (this.config.damageConversions == null) this.config.damageConversions = new ArrayList<>();
        if (this.config.damageMultipliers == null) this.config.damageMultipliers = new ArrayList<>();
        if (this.config.attackDamageMultipliers == null) this.config.attackDamageMultipliers = new ArrayList<>();
        if (this.config.allowedEntityTypes == null) this.config.allowedEntityTypes = new ArrayList<>();
        if (this.config.tips == null) this.config.tips = new ArrayList<>();
        if (this.config.activationCommands == null) this.config.activationCommands = new ArrayList<>();
        if (this.config.deactivationCommands == null) this.config.deactivationCommands = new ArrayList<>();
        if (this.config.tipKey == null) this.config.tipKey = "shift";
        if (this.config.minimumPieces < 1) this.config.minimumPieces = 2;
        if (this.config.flightRequiredPieces < 0) this.config.flightRequiredPieces = 0;
        if (this.config.flightConditions == null) this.config.flightConditions = new ArrayList<>();
        if (this.config.pieceBonusGroups == null) this.config.pieceBonusGroups = new ArrayList<>();

        configureStandaloneDraft(
                config::copyForEdit,
                snapshot -> {
                    config.restoreFromEditCopy(snapshot);
                    tempId = config.id;
                    tempName = config.displayName;
                    if (idBox != null) {
                        idBox.setTextValue(tempId == null ? "" : tempId);
                        idBox.setValidationError(false);
                    }
                    if (nameBox != null) {
                        nameBox.setTextValue(tempName == null ? "" : tempName);
                        nameBox.setValidationError(false);
                    }
                    idInputError = false;
                    nameInputError = false;
                }
        );
    }

    @Override
    protected void onTick() {
        if (idBox != null) tempId = idBox.textValue();
        if (nameBox != null) tempName = nameBox.textValue();
    }

    @Override
    protected void build(KineticUi ui) {
        int panelWidth = width() - 8;
        int leftX = 4;
        int topY = 28, inputW = (panelWidth - 4) / 2;

        this.idBox = ui().autoComplete(leftX, topY, inputW, ArrayList::new).placeholder(KineticI18n.translatable("gui.kineticarmory.armorsets.label.id")).firstShownTextAsDefault().build();
        this.idBox.limitTextLength(ArmorLoader.MAX_SET_ID_LENGTH);
        this.idBox.filterText(value -> value.matches("[\\p{L}\\p{N}_.-]*"));
        this.idBox.setTextValue(tempId != null ? tempId : (config.id != null ? config.id : ""));
        this.idBox.onTextChange(value -> {
            tempId = value;
            if (idInputError) {
                idInputError = false;
                idBox.setValidationError(false);
            }
        });

        this.nameBox = ui().autoComplete(leftX + inputW + 4, topY, inputW, ArrayList::new).placeholder(KineticI18n.translatable("gui.kineticarmory.armorsets.label.name")).firstShownTextAsDefault().build();
        this.nameBox.setTextValue(tempName != null ? tempName : (config.displayName != null ? config.displayName : ""));
        this.nameBox.onTextChange(value -> {
            tempName = value;
            if (nameInputError) {
                nameInputError = false;
                nameBox.setValidationError(false);
            }
        });

        int btnW = 155;
        int gap = 4;
        int startX = 4;
        int row1Y = 52, row2Y = 72;

        ui().button(startX, row1Y, btnW).text(KineticI18n.translatable(config.playerOnly ? "gui.kineticarmory.armorsets.player_only.true" : "gui.kineticarmory.armorsets.player_only.false")).tooltip(KineticI18n.translatable("gui.kineticarmory.armorsets.tooltip.player_only")).onClick(b -> {
            config.playerOnly = !config.playerOnly;
            b.setText(KineticI18n.translatable(config.playerOnly ? "gui.kineticarmory.armorsets.player_only.true" : "gui.kineticarmory.armorsets.player_only.false"));
        }).build();

        ui().button(startX + btnW + gap, row1Y, btnW).text(KineticI18n.translatable("gui.kineticarmory.armorsets.tipkey", config.tipKey.toUpperCase())).tooltip(KineticI18n.translatable("gui.kineticarmory.armorsets.tooltip.tipkey")).onClick(b -> {
            config.tipKey = config.tipKey.equals("shift") ? "ctrl" : (config.tipKey.equals("ctrl") ? "alt" : (config.tipKey.equals("alt") ? "none" : "shift"));
            b.setText(KineticI18n.translatable("gui.kineticarmory.armorsets.tipkey", config.tipKey.toUpperCase()));
        }).build();

        ui().button(startX + (btnW + gap) * 2, row1Y, btnW).text(KineticI18n.translatable("gui.kineticarmory.armorsets.btn_edit_tips")).tooltip(KineticI18n.translatable("gui.kineticarmory.armorsets.tooltip.tips")).onClick(b -> {
            openChild(new ArmorTipEditorPage(config));
        }).build();

        ui().button(startX + (btnW + gap) * 3, row1Y, btnW).text(KineticI18n.translatable("gui.kineticarmory.armorsets.btn_import_equipped")).tooltip(KineticI18n.translatable("gui.kineticarmory.armorsets.tooltip.import")).onClick(b -> importEquipped()).build();

        ui().button(startX, row2Y, btnW).text(KineticI18n.translatable("gui.kineticarmory.armorsets.btn_edit_effects")).tooltip(KineticI18n.translatable("gui.kineticarmory.armorsets.tooltip.effects")).onClick(b -> {
            openChild(new ArmorDetailPage(config));
        }).build();

        ui().button(startX + btnW + gap, row2Y, btnW).text(KineticI18n.translatable("gui.kineticarmory.armorsets.btn_edit_commands")).tooltip(KineticI18n.translatable("gui.kineticarmory.armorsets.tooltip.commands")).onClick(b -> {
            openChild(new ArmorCommandEditorPage(config));
        }).build();

        ui().button(startX + (btnW + gap) * 2, row2Y, btnW).text(KineticI18n.translatable("gui.kineticarmory.armorsets.btn_piece_bonuses", config.getPieceBonusGroupCount())).tooltip(KineticI18n.translatable("gui.kineticarmory.armorsets.tooltip.piece_bonuses")).onClick(b -> {
            openChild(new ArmorPieceBonusPage(config));
        }).build();

        ui().button(startX + (btnW + gap) * 3, row2Y, btnW).text(KineticI18n.translatable("gui.kineticarmory.armorsets.entity_filter.set_button")).tooltip(KineticI18n.translatable("gui.kineticarmory.armorsets.entity_filter.set_button.tooltip")).onClick(b -> {
            openChild(new ArmorEntityFilterPage(config));
        }).build();

        ui().button(width() - 44, 4, 40).text(KineticI18n.translatable("gui.kineticarmory.armorsets.save")).onClick(b -> save()).build();
        ui().button(4, 4, 40).text(KineticI18n.translatable("gui.kineticarmory.armorsets.back")).onClick(b -> {
            this.navigateBack();
        }).build();
    }


    private ArmorDataConfig.ItemReq createReq(ItemStack stack) {
        ArmorDataConfig.ItemReq req = ArmorDataConfig.ItemReq.create(getId(stack));
        req.setItemData(dev.xyat.kineticarmory.armorsets.data.ArmorItemData.format(stack));
        return req;
    }

    private void importEquipped() {
        Player p = KineticClientRuntime.localPlayer(); if (p == null) return;
        config.setSingleEquipmentVariant("head", createReq(p.getItemBySlot(EquipmentSlot.HEAD)));
        config.setSingleEquipmentVariant("chest", createReq(p.getItemBySlot(EquipmentSlot.CHEST)));
        config.setSingleEquipmentVariant("legs", createReq(p.getItemBySlot(EquipmentSlot.LEGS)));
        config.setSingleEquipmentVariant("feet", createReq(p.getItemBySlot(EquipmentSlot.FEET)));
        config.setSingleEquipmentVariant("mainhand", createReq(p.getMainHandItem()));
        config.setSingleEquipmentVariant("offhand", createReq(p.getOffhandItem()));

        config.curios.clear();
        CuriosApi.getCuriosInventory(p).ifPresent(handler -> handler.getCurios().values().forEach(stackHandler -> {
            var stacks = stackHandler.getStacks();
            for (int i = 0; i < stacks.getSlots(); i++) {
                ItemStack stack = stacks.getStackInSlot(i);
                if (!stack.isEmpty()) config.curios.add(createReq(stack));
            }
        }));
    }

    private String getId(ItemStack stack) {
        if (stack.isEmpty()) return "minecraft:air";
        var id = KineticRegistries.items().id(stack.getItem());
        return id == null ? "minecraft:air" : id.toString();
    }

    private void save() {
        if (savePending) return;

        idInputError = false;
        nameInputError = false;
        idBox.setValidationError(false);
        nameBox.setValidationError(false);

        String newDisplayName = nameBox.textValue().trim();
        if (newDisplayName.isEmpty()) {
            this.warningMessage = KineticI18n.translatable("gui.kineticarmory.armorsets.edit.warn_no_name");
            nameInputError = true;
            nameBox.setValidationError(true);
            focus(this.nameBox); return;
        }
        String newId = idBox.textValue().trim();
        if (newId.isEmpty()) {
            this.warningMessage = KineticI18n.translatable("gui.kineticarmory.armorsets.edit.warn_no_id");
            idInputError = true;
            idBox.setValidationError(true);
            focus(this.idBox); return;
        }
        if (!ArmorLoader.isSafeSetId(newId)) {
            this.warningMessage = KineticI18n.translatable("gui.kineticarmory.armorsets.edit.warn_invalid_id");
            idInputError = true;
            idBox.setValidationError(true);
            focus(this.idBox); return;
        }

        long equipmentCount = config.countEquipmentRequirements();
        long curioCount = config.curios.stream().filter(req -> req != null && !req.id.equals("minecraft:air")).count();
        if (equipmentCount + curioCount == 0) {
            this.warningMessage = KineticI18n.translatable("gui.kineticarmory.armorsets.edit.warn_no_items");
            return;
        }

        this.warningMessage = null;
        String oldIdForPacket = null;
        if (!savedId.equals(newId)) {
            oldIdForPacket = this.savedId;
            ArmorClientSnapshot.remove(this.savedId);
        }

        config.id = newId; config.displayName = newDisplayName;
        config.preparePieceBonusData();
        ArmorClientSnapshot.put(config);
        pendingSavedId = newId;
        savePending = true;
        ArmorNetwork.saveArmorSet(config, oldIdForPacket);
    }

    public void handleSaveResult(boolean success) {
        if (!savePending) return;

        savePending = false;
        if (success && pendingSavedId != null) {
            savedId = pendingSavedId;
            commitDraft();
        }
        pendingSavedId = null;
    }

    @Override
    protected void renderBackground(KineticGraphics g, int mx, int my, float pt) {
        KineticTheme.panel(g, 0, 0, width(), height());
    }

    @Override
    protected void renderForeground(KineticGraphics g, int mx, int my, float pt) {
        int cx = width() / 2;
        if (warningMessage != null) g.scrollingTextCentered(warningMessage, cx, height() - 16, width() - 8, 0xFFFFFF, true);
        g.scrollingTextCentered(title(), cx, 8, width() - 96, 0xFFFFFF, true);

        int vanillaStartX = cx - (PITCH * 6 - 2) / 2;
        int extStartX = extStartX(cx);
        int sectionTextWidth = extWidth() - 2 * TEXT_GAP;
        int vanillaY = EQUIPMENT_Y, curioY = CURIO_Y, rejectedY = REJECTED_Y;

        for (int i = 0; i < 6; i++) renderSlot(g, vanillaStartX + i * PITCH, vanillaY, mx, my, vanillaSlots[i], i, 0);
        g.scrollingTextCentered(KineticI18n.translatable("gui.kineticarmory.armorsets.section.curio"), cx, curioY - 12, sectionTextWidth, 0xFFAA00, true);
        renderExtSection(g, extStartX, curioY, mx, my, "curio", 1, curioBar, config.curios.size());
        g.scrollingTextCentered(KineticI18n.translatable("gui.kineticarmory.armorsets.section.rejected"), cx, rejectedY - 12, sectionTextWidth, 0xFF5555, true);
        renderExtSection(g, extStartX, rejectedY, mx, my, "rejected", 2, rejectedBar, config.rejectedCurios.size());
    }

    private void renderSlot(KineticGraphics g, int x, int y, int mx, int my, String slotKey, int index, int type) {
        KineticTheme.itemSlot(g, x, y, SLOT_SIZE, 4, false);
        KineticTheme.indicatorOutline(
                g,
                x,
                y,
                SLOT_SIZE,
                SLOT_SIZE,
                type == 2 ? KineticTheme.Indicator.DANGER : type == 1 ? KineticTheme.Indicator.WARNING : KineticTheme.Indicator.INFO
        );

        ArmorDataConfig.ItemReq req = type == 1 ? (index < config.curios.size() ? config.curios.get(index) : ArmorDataConfig.ItemReq.create("minecraft:air")) :
                (type == 2 ? (index < config.rejectedCurios.size() ? config.rejectedCurios.get(index) : ArmorDataConfig.ItemReq.create("minecraft:air")) :
                        config.getDisplayedEquipmentReq(slotKey, System.currentTimeMillis()));
        int variantCount = type == 0 ? ArmorDataConfig.countPieceRequirements(config.getEquipmentVariants(slotKey)) : 0;

        boolean hover = mx >= x && mx < x + SLOT_SIZE && my >= y && my < y + SLOT_SIZE;
        String reqId = req.id == null ? "minecraft:air" : req.id;
        boolean isEmptyState = reqId.equals("EMPTY");
        boolean isAnyState = reqId.equals("ANY");
        boolean isAir = reqId.equals("minecraft:air");

        if (isEmptyState) {
            g.centeredText("X", x + 9, y + 5, 0xFF5555, true);
            if (hover) {
                List<Component> t = new ArrayList<>();
                t.add(KineticI18n.translatable("gui.kineticarmory.armorsets.slot_state.empty"));
                if (type == 0) t.add(KineticI18n.translatable("gui.kineticarmory.armorsets.variant.tooltip.open_list", variantCount));
                else t.add(KineticI18n.translatable("gui.kineticarmory.armorsets.tooltip.midclick_clear"));
                showTooltip(t);
            }
        } else if (isAnyState) {
            g.centeredText("?", x + 9, y + 5, 0x55FF55, true);
            if (hover) {
                List<Component> t = new ArrayList<>();
                t.add(KineticI18n.translatable("gui.kineticarmory.armorsets.slot_state.any"));
                if (type == 0) t.add(KineticI18n.translatable("gui.kineticarmory.armorsets.variant.tooltip.open_list", variantCount));
                else t.add(KineticI18n.translatable("gui.kineticarmory.armorsets.tooltip.midclick_clear"));
                showTooltip(t);
            }
        } else if (!isAir) {
            ItemStack stack = req.createDisplayStack();
            if (!stack.isEmpty()) {
                KineticTheme.item(g, stack, x, y, SLOT_SIZE, 1.0F, false);
                String nbtStr = "WEAK".equals(req.getDataMode()) ? "W" : ("STRONG".equals(req.getDataMode()) ? "S" : "");
                if (!nbtStr.isEmpty()) g.text(nbtStr, x + SLOT_SIZE - 3 - g.textWidth(nbtStr), y + SLOT_SIZE - 11, 0xFFFFFFFF, true);
                if (variantCount > 1) {
                    ArmorVariantMarker.draw(g, x, y);
                }
                if (hover) {
                    showItemTooltip(stack);
                }
            }
        } else if (hover) {
            List<Component> t = new ArrayList<>();
            Component slotNameComp = type == 2 ? KineticI18n.translatable("gui.kineticarmory.armorsets.slot.rejected") : (type == 1 ? KineticI18n.translatable("gui.kineticarmory.armorsets.slot.curio") : KineticI18n.translatable("gui.kineticarmory.armorsets.slot." + slotKey));
            t.add(KineticI18n.translatable("gui.kineticarmory.armorsets.tooltip.empty_slot", slotNameComp));
            showTooltip(t);
        }
    }

    @Override
    protected boolean onMouseClick(MouseInput input) {
        double mx = input.x(), my = input.y(); int btn = input.rawButton();

        int cx = width() / 2;
        int vanillaStartX = cx - (PITCH * 6 - 2) / 2;
        int extStartX = extStartX(cx);
        int vanillaY = EQUIPMENT_Y, curioY = CURIO_Y, rejectedY = REJECTED_Y;

        for (int i = 0; i < 6; i++) if (handleSlotClick(vanillaStartX + i * PITCH, vanillaY, mx, my, btn, vanillaSlots[i], i, 0)) return true;
        int barX = extStartX + EXT_COLS * PITCH - 2 + EXT_BAR_GAP;
        for (var bar : List.of(curioBar, rejectedBar)) {
            int y = bar == curioBar ? curioY : rejectedY;
            if (bar.canScroll() && bar.beginDrag(mx, my, input.button(), barX, y, EXT_BAR_W, EXT_ROWS * PITCH - 2, 10, 2)) { draggingBar = bar; return true; }
        }
        int curioStart = curioBar.offset() * EXT_COLS, rejectedStart = rejectedBar.offset() * EXT_COLS;
        for (int i = 0; i < EXT_COLS * EXT_ROWS; i++) if (handleSlotClick(extStartX + (i % EXT_COLS) * PITCH, curioY + (i / EXT_COLS) * PITCH, mx, my, btn, "curio", curioStart + i, 1)) return true;
        for (int i = 0; i < EXT_COLS * EXT_ROWS; i++) if (handleSlotClick(extStartX + (i % EXT_COLS) * PITCH, rejectedY + (i / EXT_COLS) * PITCH, mx, my, btn, "rejected", rejectedStart + i, 2)) return true;
        return false;
    }

    private static int extWidth() { return EXT_COLS * PITCH - 2 + EXT_BAR_GAP + EXT_BAR_W; }
    private static int extStartX(int cx) { return cx - extWidth() / 2; }

    private void renderExtSection(KineticGraphics g, int x, int y, int mx, int my, String key, int type,
            dev.xyat.kineticcore.api.client.gui.scroll.KineticScrollController bar, int size) {
        bar.update(Math.max(EXT_ROWS, size / EXT_COLS + 1), EXT_ROWS);
        int start = bar.offset() * EXT_COLS;
        for (int i = 0; i < EXT_COLS * EXT_ROWS; i++) renderSlot(g, x + (i % EXT_COLS) * PITCH, y + (i / EXT_COLS) * PITCH, mx, my, key, start + i, type);
        if (bar.canScroll()) bar.render(g, mx, my, x + EXT_COLS * PITCH - 2 + EXT_BAR_GAP, y, EXT_BAR_W, EXT_ROWS * PITCH - 2, 10);
    }

    @Override
    protected boolean onMouseScroll(dev.xyat.kineticcore.api.client.gui.input.ScrollInput input) {
        int cx = width() / 2;
        int x = extStartX(cx); int curioY = CURIO_Y, rejectedY = REJECTED_Y;
        if (input.x() < x || input.x() >= x + extWidth()) return false;
        if (input.y() >= curioY && input.y() < curioY + EXT_ROWS * PITCH) return curioBar.scroll(input.deltaY());
        if (input.y() >= rejectedY && input.y() < rejectedY + EXT_ROWS * PITCH) return rejectedBar.scroll(input.deltaY());
        return false;
    }

    @Override
    protected boolean onMouseDrag(dev.xyat.kineticcore.api.client.gui.input.MouseDragInput input) {
        if (draggingBar == null) return false;
        int curioY = CURIO_Y;
        return draggingBar.drag(input.y(), draggingBar == curioBar ? curioY : REJECTED_Y, EXT_ROWS * PITCH - 2, 10);
    }

    @Override
    protected boolean onMouseRelease(MouseInput input) {
        if (draggingBar == null) return false;
        draggingBar.release(input.button()); draggingBar = null; return true;
    }

    private boolean handleSlotClick(int x, int y, double mx, double my, int btn, String slotKey, int index, int type) {
        if (mx >= x && mx < x + SLOT_SIZE && my >= y && my < y + SLOT_SIZE) {
            ArmorDataConfig.ItemReq req;
            if (type == 1) { while (config.curios.size() <= index) config.curios.add(ArmorDataConfig.ItemReq.create("minecraft:air")); req = config.curios.get(index); }
            else if (type == 2) { while (config.rejectedCurios.size() <= index) config.rejectedCurios.add(ArmorDataConfig.ItemReq.create("minecraft:air")); req = config.rejectedCurios.get(index); }
            else { req = config.equipment.computeIfAbsent(slotKey, k -> ArmorDataConfig.ItemReq.create("minecraft:air")); }

            if (type == 0) {
                if (KineticMouseButtons.isPrimary(btn)) {
                    Component slotNameComp = KineticI18n.translatable("gui.kineticarmory.armorsets.slot." + slotKey);
                    openChild(new ArmorEquipmentVariantPage(config, slotKey, slotNameComp));
                }
                return true;
            }

            if (KineticMouseButtons.isPrimary(btn)) {
                if (req.id.equals("EMPTY") || req.id.equals("ANY")) return true;

                if (KineticClientRuntime.shiftModifierDown()) {
                    if (!req.id.equals("minecraft:air")) {
                        String initNbt = (req.getItemData() != null && !req.getItemData().trim().isEmpty()) ? req.getItemData() : "";
                        ArmorItemDataEditor.open(ArmorEditPage.this, req.id, initNbt, savedNbt -> {
                            req.setItemData(savedNbt);
                            if (req.getDataMode() == null || req.getDataMode().equals("NONE")) req.setDataMode("WEAK");
                            KineticOverlays.toast(KineticI18n.translatable("msg.kineticarmory.common.saved"));
                        });
                    }
                } else {
                    KineticSelectors.openItemSelector(selection -> {
                        if (!selection.isItem()) return;
                        ItemStack stack = selection.stack();
                        req.id = getId(stack);
                        req.setItemData(dev.xyat.kineticarmory.armorsets.data.ArmorItemData.format(stack));
                        if (req.getDataMode() == null) req.setDataMode("NONE");
                        cleanCurios();
                        cleanRejectedCurios();
                        rebuild();
                    });
                }
                return true;
            } else if (KineticMouseButtons.isSecondary(btn) && !req.id.equals("minecraft:air")) {
                req.setDataMode(("NONE".equals(req.getDataMode()) || req.getDataMode() == null) ? "WEAK" : ("WEAK".equals(req.getDataMode()) ? "STRONG" : "NONE"));
                return true;
            } else if (KineticMouseButtons.isMiddle(btn)) {
                req.id = "minecraft:air"; req.setDataMode("NONE"); req.setItemData(dev.xyat.kineticarmory.armorsets.data.ArmorItemData.emptyData());
                cleanCurios(); cleanRejectedCurios(); return true;
            }
        }
        return false;
    }

    private void cleanCurios() { for (int i = config.curios.size() - 1; i >= 0; i--) { if (config.curios.get(i).id.equals("minecraft:air")) config.curios.remove(i); else break; } }
    private void cleanRejectedCurios() { for (int i = config.rejectedCurios.size() - 1; i >= 0; i--) { if (config.rejectedCurios.get(i).id.equals("minecraft:air")) config.rejectedCurios.remove(i); else break; } }
}
