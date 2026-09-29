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

    private static final int SLOT_SIZE = 18;

    
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

    public ArmorEditPage(ArmorDataConfig config) {
        super(KineticI18n.translatable("gui.kineticarmory.armorsets.edit.title"));
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
        int cx = this.width() / 2; int cy = this.height() / 2;
        int panelWidth = Math.min(this.width() - 40, 360);
        int leftX = cx - panelWidth / 2;
        int topY = cy - 110; int inputW = (panelWidth - 10) / 2;

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

        this.nameBox = ui().autoComplete(leftX + inputW + 10, topY, inputW, ArrayList::new).placeholder(KineticI18n.translatable("gui.kineticarmory.armorsets.label.name")).firstShownTextAsDefault().build();
        this.nameBox.setTextValue(tempName != null ? tempName : (config.displayName != null ? config.displayName : ""));
        this.nameBox.onTextChange(value -> {
            tempName = value;
            if (nameInputError) {
                nameInputError = false;
                nameBox.setValidationError(false);
            }
        });

        int btnW = 105;
        int gap = 8;
        int startX = cx - (btnW * 3 + gap * 2) / 2;
        int row1Y = topY + 25;
        int row2Y = row1Y + 25;
        int row3Y = row2Y + 25;

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

        ui().button(startX, row2Y, btnW).text(KineticI18n.translatable("gui.kineticarmory.armorsets.btn_import_equipped")).tooltip(KineticI18n.translatable("gui.kineticarmory.armorsets.tooltip.import")).onClick(b -> importEquipped()).build();

        ui().button(startX + btnW + gap, row2Y, btnW).text(KineticI18n.translatable("gui.kineticarmory.armorsets.btn_edit_effects")).tooltip(KineticI18n.translatable("gui.kineticarmory.armorsets.tooltip.effects")).onClick(b -> {
            openChild(new ArmorDetailPage(config));
        }).build();

        ui().button(startX + (btnW + gap) * 2, row2Y, btnW).text(KineticI18n.translatable("gui.kineticarmory.armorsets.btn_edit_commands")).tooltip(KineticI18n.translatable("gui.kineticarmory.armorsets.tooltip.commands")).onClick(b -> {
            openChild(new ArmorCommandEditorPage(config));
        }).build();

        ui().button(startX, row3Y, btnW).text(KineticI18n.translatable("gui.kineticarmory.armorsets.btn_piece_bonuses", config.getPieceBonusGroupCount())).tooltip(KineticI18n.translatable("gui.kineticarmory.armorsets.tooltip.piece_bonuses")).onClick(b -> {
            openChild(new ArmorPieceBonusPage(config));
        }).build();

        ui().button(startX + btnW + gap, row3Y, btnW).text(KineticI18n.translatable("gui.kineticarmory.armorsets.entity_filter.set_button")).tooltip(KineticI18n.translatable("gui.kineticarmory.armorsets.entity_filter.set_button.tooltip")).onClick(b -> {
            openChild(new ArmorEntityFilterPage(config));
        }).build();

        int bottomBtnY = topY + 225; int actionBtnW = 80;
        ui().button(cx - actionBtnW - 5, bottomBtnY, actionBtnW).text(KineticI18n.translatable("gui.kineticarmory.armorsets.save")).onClick(b -> save()).build();
        ui().button(cx + 5, bottomBtnY, actionBtnW).text(KineticI18n.translatable("gui.kineticarmory.armorsets.back")).onClick(b -> {
            this.navigateBack();
        }).build();
    }


    private ArmorDataConfig.ItemReq createReq(ItemStack stack) {
        ArmorDataConfig.ItemReq req = ArmorDataConfig.ItemReq.create(getId(stack));
        if (!stack.isEmpty() && stack.hasTag() && stack.getTag() != null) req.nbtTag = stack.getTag().toString();
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
        int cx = this.width() / 2; int topY = this.height() / 2 - 110;
        int panelWidth = Math.min(this.width() - 40, 360);
        int panelX = cx - panelWidth / 2 - 15; int panelY = topY - 30;
        if (this.warningMessage != null) panelY -= 15;
        KineticTheme.panel(g, panelX, panelY, panelWidth + 30, (topY + 225 + 30) - panelY);
    }

    @Override
    protected void renderForeground(KineticGraphics g, int mx, int my, float pt) {
        int cx = this.width() / 2; int topY = this.height() / 2 - 110;

        int titleY = topY - 23;
        if (this.warningMessage != null) {
            g.centeredText(this.warningMessage, cx, titleY - 12, 0xFFFFFF, true);
        }

        g.centeredText(title(), cx, titleY, 0xFFFFFF, true);

        int vanillaStartX = cx - ((18 + 2) * 6 - 2) / 2;
        int extStartX = cx - ((18 + 2) * 18 - 2) / 2;
        int vanillaY = topY + 95; int curioY = vanillaY + 34; int rejectedY = curioY + 54;

        for (int i = 0; i < 6; i++) renderSlot(g, vanillaStartX + i * 20, vanillaY, mx, my, vanillaSlots[i], i, 0);
        g.centeredText(KineticI18n.translatable("gui.kineticarmory.armorsets.section.curio"), cx, curioY - 12, 0xFFAA00, true);
        for (int i = 0; i < 36; i++) renderSlot(g, extStartX + (i % 18) * 20, curioY + (i / 18) * 20, mx, my, "curio", i, 1);
        g.centeredText(KineticI18n.translatable("gui.kineticarmory.armorsets.section.rejected"), cx, rejectedY - 12, 0xFF5555, true);
        for (int i = 0; i < 36; i++) renderSlot(g, extStartX + (i % 18) * 20, rejectedY + (i / 18) * 20, mx, my, "rejected", i, 2);
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

        boolean hover = mx >= x && mx < x + 18 && my >= y && my < y + 18;
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
                g.item(stack, x + 1, y + 1);
                String nbtStr = "WEAK".equals(req.nbtMode) ? "W" : ("STRONG".equals(req.nbtMode) ? "S" : "");
                if (!nbtStr.isEmpty()) g.itemDecorations(stack, x + 1, y + 1, nbtStr);
                if (variantCount > 1) {
                    KineticTheme.indicatorFill(g, x + 10, y + 10, 8, 8, KineticTheme.Indicator.SUCCESS, 0.80F);
                    g.text("+", x + 12, y + 9, 0xFF55FF55, false);
                }
                if (hover) {
                    List<Component> t = new ArrayList<>();
                    t.add(stack.getHoverName());
                    t.add(KineticI18n.translatable("gui.kineticarmory.armorsets.nbt_prefix", KineticI18n.translatable("gui.kineticarmory.armorsets.nbt." + (req.nbtMode == null ? "none" : req.nbtMode.toLowerCase()))));
                    if (type == 0) {
                        t.add(KineticI18n.translatable("gui.kineticarmory.armorsets.variant.tooltip.open_list", variantCount));
                    } else {
                        t.add(KineticI18n.translatable("gui.kineticarmory.armorsets.tooltip.lclick"));
                        t.add(KineticI18n.translatable("gui.kineticarmory.armorsets.tooltip.rclick"));
                        t.add(KineticI18n.translatable("gui.kineticarmory.armorsets.tooltip.shift_edit_nbt"));
                    }
                    if (type != 0) t.add(KineticI18n.translatable("gui.kineticarmory.armorsets.tooltip.midclick_clear"));
                    showTooltip(t);
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

        int cx = this.width() / 2; int topY = this.height() / 2 - 110;
        int vanillaStartX = cx - ((18 + 2) * 6 - 2) / 2;
        int extStartX = cx - ((18 + 2) * 18 - 2) / 2;
        int vanillaY = topY + 95; int curioY = vanillaY + 34; int rejectedY = curioY + 54;

        for (int i = 0; i < 6; i++) if (handleSlotClick(vanillaStartX + i * 20, vanillaY, mx, my, btn, vanillaSlots[i], i, 0)) return true;
        for (int i = 0; i < 36; i++) if (handleSlotClick(extStartX + (i % 18) * 20, curioY + (i / 18) * 20, mx, my, btn, "curio", i, 1)) return true;
        for (int i = 0; i < 36; i++) if (handleSlotClick(extStartX + (i % 18) * 20, rejectedY + (i / 18) * 20, mx, my, btn, "rejected", i, 2)) return true;
        return false;
    }

    private boolean handleSlotClick(int x, int y, double mx, double my, int btn, String slotKey, int index, int type) {
        if (mx >= x && mx < x + 18 && my >= y && my < y + 18) {
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
                        String initNbt = (req.nbtTag != null && !req.nbtTag.trim().isEmpty()) ? req.nbtTag : "";
                        KineticSelectors.openNbtEditor(initNbt, savedNbt -> {
                            req.nbtTag = savedNbt;
                            if (req.nbtMode == null || req.nbtMode.equals("NONE")) req.nbtMode = "WEAK";
                            KineticOverlays.toast(KineticI18n.translatable("msg.kineticarmory.common.saved"));
                        });
                    }
                } else {
                    KineticSelectors.openItemSelector(selection -> {
                        if (!selection.isItem()) return;
                        ItemStack stack = selection.stack();
                        req.id = getId(stack);
                        req.nbtTag = stack.hasTag() && stack.getTag() != null ? stack.getTag().toString() : "{}";
                        if (req.nbtMode == null) req.nbtMode = "NONE";
                        cleanCurios();
                        cleanRejectedCurios();
                        rebuild();
                    });
                }
                return true;
            } else if (KineticMouseButtons.isSecondary(btn) && !req.id.equals("minecraft:air")) {
                req.nbtMode = ("NONE".equals(req.nbtMode) || req.nbtMode == null) ? "WEAK" : ("WEAK".equals(req.nbtMode) ? "STRONG" : "NONE");
                return true;
            } else if (KineticMouseButtons.isMiddle(btn)) {
                req.id = "minecraft:air"; req.nbtMode = "NONE"; req.nbtTag="{}";
                cleanCurios(); cleanRejectedCurios(); return true;
            }
        }
        return false;
    }

    private void cleanCurios() { for (int i = config.curios.size() - 1; i >= 0; i--) { if (config.curios.get(i).id.equals("minecraft:air")) config.curios.remove(i); else break; } }
    private void cleanRejectedCurios() { for (int i = config.rejectedCurios.size() - 1; i >= 0; i--) { if (config.rejectedCurios.get(i).id.equals("minecraft:air")) config.rejectedCurios.remove(i); else break; } }
}
