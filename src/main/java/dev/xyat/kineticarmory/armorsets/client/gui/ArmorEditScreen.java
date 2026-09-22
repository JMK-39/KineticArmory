package dev.xyat.kineticarmory.armorsets.client.gui;

import dev.xyat.kineticcore.api.client.input.KineticMouseButtons;
import dev.xyat.kineticcore.api.client.screen.KineticScreen;
import dev.xyat.kineticcore.api.client.theme.GuiTheme;
import dev.xyat.kineticcore.api.client.overlay.KineticOverlays;
import dev.xyat.kineticcore.api.client.selector.KineticSelectors;

import dev.xyat.kineticarmory.util.ColorText;
import dev.xyat.kineticarmory.armorsets.Network.ArmorNetwork;
import dev.xyat.kineticarmory.armorsets.client.ArmorClientSnapshot;
import dev.xyat.kineticarmory.armorsets.data.ArmorDataConfig;
import dev.xyat.kineticarmory.armorsets.json.ArmorLoader;

import dev.xyat.kineticcore.api.client.widget.input.KineticAutoComplete.AutoCompleteBox;
import dev.xyat.kineticcore.api.runtime.KineticClientRuntime;
import dev.xyat.kineticcore.api.registry.KineticRegistries;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;
import top.theillusivec4.curios.api.CuriosApi;

import java.util.ArrayList;
import java.util.List;

public class ArmorEditScreen extends KineticScreen {

    private static final int SLOT_SIZE = 18;

    private final KineticScreen parent;
    private final ArmorDataConfig config;
    private final String originalId;

    private AutoCompleteBox idBox, nameBox;
    private Component warningMessage;
    private boolean idInputError;
    private boolean nameInputError;
    private String tempId = null;
    private String tempName = null;

    private final String[] vanillaSlots = {"head", "chest", "legs", "feet", "mainhand", "offhand"};

    public ArmorEditScreen(KineticScreen parent, ArmorDataConfig config) {
        super(ColorText.translatable("gui.kineticarmory.armorsets.edit.title"));
        setParentScreen(parent);
        this.parent = parent;
        this.config = config;
        this.originalId = config.id;

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
                        idBox.setValue(tempId == null ? "" : tempId);
                        idBox.setValidationError(false);
                    }
                    if (nameBox != null) {
                        nameBox.setValue(tempName == null ? "" : tempName);
                        nameBox.setValidationError(false);
                    }
                    idInputError = false;
                    nameInputError = false;
                }
        );
    }

    @Override
    protected void canvasTick() {
        if (idBox != null) tempId = idBox.getValue();
        if (nameBox != null) tempName = nameBox.getValue();
    }

    @Override
    protected void buildUi() {
        int cx = this.canvasWidth() / 2; int cy = this.canvasHeight() / 2;
        int panelWidth = Math.min(this.canvasWidth() - 40, 360);
        int leftX = cx - panelWidth / 2;
        int topY = cy - 110; int inputW = (panelWidth - 10) / 2;

        this.idBox = addAutoCompleteField(
                leftX, topY, inputW, Component.empty(),
                ColorText.translatable("gui.kineticarmory.armorsets.label.id"),
                ArrayList::new, null
        );
        this.idBox.setMaxLength(ArmorLoader.MAX_SET_ID_LENGTH);
        this.idBox.setFilter(value -> value.matches("[\\p{L}\\p{N}_.-]*"));
        this.idBox.setValue(tempId != null ? tempId : (config.id != null ? config.id : ""));
        this.idBox.setResponder(value -> {
            tempId = value;
            if (idInputError) {
                idInputError = false;
                idBox.setValidationError(false);
            }
        });

        this.nameBox = addAutoCompleteField(
                leftX + inputW + 10, topY, inputW, Component.empty(),
                ColorText.translatable("gui.kineticarmory.armorsets.label.name"),
                ArrayList::new, null
        );
        this.nameBox.setValue(tempName != null ? tempName : (config.displayName != null ? config.displayName : ""));
        this.nameBox.setResponder(value -> {
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

        addButtonWithHandler(startX, row1Y, btnW, ColorText.translatable(config.playerOnly ? "gui.kineticarmory.armorsets.player_only.true" : "gui.kineticarmory.armorsets.player_only.false"), ColorText.translatable("gui.kineticarmory.armorsets.tooltip.player_only"), b -> {
            config.playerOnly = !config.playerOnly;
            b.setText(ColorText.translatable(config.playerOnly ? "gui.kineticarmory.armorsets.player_only.true" : "gui.kineticarmory.armorsets.player_only.false"));
        });

        addButtonWithHandler(startX + btnW + gap, row1Y, btnW, ColorText.translatable("gui.kineticarmory.armorsets.tipkey", config.tipKey.toUpperCase()), ColorText.translatable("gui.kineticarmory.armorsets.tooltip.tipkey"), b -> {
            config.tipKey = config.tipKey.equals("shift") ? "ctrl" : (config.tipKey.equals("ctrl") ? "alt" : (config.tipKey.equals("alt") ? "none" : "shift"));
            b.setText(ColorText.translatable("gui.kineticarmory.armorsets.tipkey", config.tipKey.toUpperCase()));
        });

        addButtonWithHandler(startX + (btnW + gap) * 2, row1Y, btnW, ColorText.translatable("gui.kineticarmory.armorsets.btn_edit_tips"), ColorText.translatable("gui.kineticarmory.armorsets.tooltip.tips"), b -> {
            KineticClientRuntime.openScreen(new ArmorTipEditorScreen(this, config));
        });

        addButtonWithHandler(startX, row2Y, btnW, ColorText.translatable("gui.kineticarmory.armorsets.btn_import_equipped"), ColorText.translatable("gui.kineticarmory.armorsets.tooltip.import"), b -> importEquipped());

        addButtonWithHandler(startX + btnW + gap, row2Y, btnW, ColorText.translatable("gui.kineticarmory.armorsets.btn_edit_effects"), ColorText.translatable("gui.kineticarmory.armorsets.tooltip.effects"), b -> {
            KineticClientRuntime.openScreen(new ArmorDetailScreen(this, config));
        });

        addButtonWithHandler(startX + (btnW + gap) * 2, row2Y, btnW, ColorText.translatable("gui.kineticarmory.armorsets.btn_edit_commands"), ColorText.translatable("gui.kineticarmory.armorsets.tooltip.commands"), b -> {
            KineticClientRuntime.openScreen(new ArmorCommandEditorScreen(this, config));
        });

        addButtonWithHandler(startX, row3Y, btnW, ColorText.translatable("gui.kineticarmory.armorsets.btn_piece_bonuses", config.getPieceBonusGroupCount()), ColorText.translatable("gui.kineticarmory.armorsets.tooltip.piece_bonuses"), b -> {
            KineticClientRuntime.openScreen(new ArmorPieceBonusScreen(this, config));
        });

        addButtonWithHandler(startX + btnW + gap, row3Y, btnW, ColorText.translatable("gui.kineticarmory.armorsets.entity_filter.set_button"), ColorText.translatable("gui.kineticarmory.armorsets.entity_filter.set_button.tooltip"), b -> {
            KineticClientRuntime.openScreen(new ArmorEntityFilterScreen(this, config));
        });

        int bottomBtnY = topY + 225; int actionBtnW = 80;
        addButtonWithHandler(cx - actionBtnW - 5, bottomBtnY, actionBtnW, ColorText.translatable("gui.kineticarmory.armorsets.save"), null, b -> saveAndClose());
        addButtonWithHandler(cx + 5, bottomBtnY, actionBtnW, ColorText.translatable("gui.kineticarmory.armorsets.back"), null, b -> {
            if (this.minecraft != null) this.navigateBack();
        });
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

    private void saveAndClose() {
        idInputError = false;
        nameInputError = false;
        idBox.setValidationError(false);
        nameBox.setValidationError(false);

        String newDisplayName = nameBox.getValue().trim();
        if (newDisplayName.isEmpty()) {
            this.warningMessage = ColorText.translatable("gui.kineticarmory.armorsets.edit.warn_no_name");
            nameInputError = true;
            nameBox.setValidationError(true);
            focusControl(this.nameBox); return;
        }
        String newId = idBox.getValue().trim();
        if (newId.isEmpty()) {
            this.warningMessage = ColorText.translatable("gui.kineticarmory.armorsets.edit.warn_no_id");
            idInputError = true;
            idBox.setValidationError(true);
            focusControl(this.idBox); return;
        }
        if (!ArmorLoader.isSafeSetId(newId)) {
            this.warningMessage = ColorText.translatable("gui.kineticarmory.armorsets.edit.warn_invalid_id");
            idInputError = true;
            idBox.setValidationError(true);
            focusControl(this.idBox); return;
        }

        long equipmentCount = config.countEquipmentRequirements();
        long curioCount = config.curios.stream().filter(req -> req != null && !req.id.equals("minecraft:air")).count();
        if (equipmentCount + curioCount == 0) {
            this.warningMessage = ColorText.translatable("gui.kineticarmory.armorsets.edit.warn_no_items");
            return;
        }

        this.warningMessage = null;
        String oldIdForPacket = null;
        if (!originalId.equals(newId)) {
            oldIdForPacket = this.originalId;
            ArmorClientSnapshot.remove(this.originalId);
        }

        config.id = newId; config.displayName = newDisplayName;
        config.preparePieceBonusData();
        ArmorClientSnapshot.put(config);
        commitDraft();
        ArmorNetwork.saveArmorSet(config, oldIdForPacket);

        if (this.minecraft != null) this.navigateBack();
    }

    @Override
    protected void renderCanvasBackground(@NotNull GuiGraphics g, int mx, int my, float pt) {
        int cx = this.canvasWidth() / 2; int topY = this.canvasHeight() / 2 - 110;
        int panelWidth = Math.min(this.canvasWidth() - 40, 360);
        int panelX = cx - panelWidth / 2 - 15; int panelY = topY - 30;
        if (this.warningMessage != null) panelY -= 15;
        GuiTheme.panel(g, panelX, panelY, panelWidth + 30, (topY + 225 + 30) - panelY);
    }

    @Override
    protected void renderCanvasForeground(@NotNull GuiGraphics g, int mx, int my, float pt) {
        int cx = this.canvasWidth() / 2; int topY = this.canvasHeight() / 2 - 110;

        int titleY = topY - 23;
        if (this.warningMessage != null) {
            g.drawCenteredString(this.font, this.warningMessage, cx, titleY - 12, 0xFFFFFF);
        }

        g.drawCenteredString(this.font, this.title, cx, titleY, 0xFFFFFF);

        int vanillaStartX = cx - ((18 + 2) * 6 - 2) / 2;
        int extStartX = cx - ((18 + 2) * 18 - 2) / 2;
        int vanillaY = topY + 95; int curioY = vanillaY + 34; int rejectedY = curioY + 54;

        for (int i = 0; i < 6; i++) renderSlot(g, vanillaStartX + i * 20, vanillaY, mx, my, vanillaSlots[i], i, 0);
        g.drawCenteredString(this.font, ColorText.translatable("gui.kineticarmory.armorsets.section.curio"), cx, curioY - 12, 0xFFAA00);
        for (int i = 0; i < 36; i++) renderSlot(g, extStartX + (i % 18) * 20, curioY + (i / 18) * 20, mx, my, "curio", i, 1);
        g.drawCenteredString(this.font, ColorText.translatable("gui.kineticarmory.armorsets.section.rejected"), cx, rejectedY - 12, 0xFF5555);
        for (int i = 0; i < 36; i++) renderSlot(g, extStartX + (i % 18) * 20, rejectedY + (i / 18) * 20, mx, my, "rejected", i, 2);

        idBox.renderSuggestions(g, mx, my);
        nameBox.renderSuggestions(g, mx, my);
    }

    private void renderSlot(GuiGraphics g, int x, int y, int mx, int my, String slotKey, int index, int type) {
        GuiTheme.itemSlot(g, x, y, SLOT_SIZE, 4, false);
        GuiTheme.indicatorOutline(
                g,
                x,
                y,
                SLOT_SIZE,
                SLOT_SIZE,
                type == 2 ? GuiTheme.Indicator.DANGER : type == 1 ? GuiTheme.Indicator.WARNING : GuiTheme.Indicator.INFO
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
            g.drawCenteredString(this.font, "X", x + 9, y + 5, 0xFF5555);
            if (hover) {
                List<Component> t = new ArrayList<>();
                t.add(ColorText.translatable("gui.kineticarmory.armorsets.slot_state.empty").withStyle(ChatFormatting.RED));
                if (type == 0) t.add(ColorText.translatable("gui.kineticarmory.armorsets.variant.tooltip.open_list", variantCount));
                else t.add(ColorText.translatable("gui.kineticarmory.armorsets.tooltip.midclick_clear"));
                showTooltip(t, null);
            }
        } else if (isAnyState) {
            g.drawCenteredString(this.font, "?", x + 9, y + 5, 0x55FF55);
            if (hover) {
                List<Component> t = new ArrayList<>();
                t.add(ColorText.translatable("gui.kineticarmory.armorsets.slot_state.any").withStyle(ChatFormatting.GREEN));
                if (type == 0) t.add(ColorText.translatable("gui.kineticarmory.armorsets.variant.tooltip.open_list", variantCount));
                else t.add(ColorText.translatable("gui.kineticarmory.armorsets.tooltip.midclick_clear"));
                showTooltip(t, null);
            }
        } else if (!isAir) {
            ItemStack stack = req.createDisplayStack();
            if (!stack.isEmpty()) {
                g.renderItem(stack, x + 1, y + 1);
                String nbtStr = "WEAK".equals(req.nbtMode) ? "W" : ("STRONG".equals(req.nbtMode) ? "S" : "");
                if (!nbtStr.isEmpty()) g.renderItemDecorations(this.font, stack, x + 1, y + 1, nbtStr);
                if (variantCount > 1) {
                    GuiTheme.indicatorFill(g, x + 10, y + 10, 8, 8, GuiTheme.Indicator.SUCCESS, 0.80F);
                    g.drawString(this.font, "+", x + 12, y + 9, 0xFF55FF55, false);
                }
                if (hover) {
                    List<Component> t = new ArrayList<>();
                    t.add(stack.getHoverName());
                    t.add(ColorText.translatable("gui.kineticarmory.armorsets.nbt_prefix", ColorText.translatable("gui.kineticarmory.armorsets.nbt." + (req.nbtMode == null ? "none" : req.nbtMode.toLowerCase()))));
                    if (type == 0) {
                        t.add(ColorText.translatable("gui.kineticarmory.armorsets.variant.tooltip.open_list", variantCount));
                    } else {
                        t.add(ColorText.translatable("gui.kineticarmory.armorsets.tooltip.lclick"));
                        t.add(ColorText.translatable("gui.kineticarmory.armorsets.tooltip.rclick"));
                        t.add(ColorText.translatable("gui.kineticarmory.armorsets.tooltip.shift_edit_nbt"));
                    }
                    if (type != 0) t.add(ColorText.translatable("gui.kineticarmory.armorsets.tooltip.midclick_clear"));
                    showTooltip(t, null);
                }
            }
        } else if (hover) {
            List<Component> t = new ArrayList<>();
            Component slotNameComp = type == 2 ? ColorText.translatable("gui.kineticarmory.armorsets.slot.rejected") : (type == 1 ? ColorText.translatable("gui.kineticarmory.armorsets.slot.curio") : ColorText.translatable("gui.kineticarmory.armorsets.slot." + slotKey));
            t.add(ColorText.translatable("gui.kineticarmory.armorsets.tooltip.empty_slot", slotNameComp));
            showTooltip(t, null);
        }
    }

    @Override
    protected boolean canvasMouseClicked(double mx, double my, int btn) {
        if (super.canvasMouseClicked(mx, my, btn)) return true;

        int cx = this.canvasWidth() / 2; int topY = this.canvasHeight() / 2 - 110;
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
                    Component slotNameComp = ColorText.translatable("gui.kineticarmory.armorsets.slot." + slotKey);
                    KineticClientRuntime.openScreen(new ArmorEquipmentVariantScreen(this, config, slotKey, slotNameComp));
                }
                return true;
            }

            if (KineticMouseButtons.isPrimary(btn)) {
                if (req.id.equals("EMPTY") || req.id.equals("ANY")) return true;

                if (hasShiftDown()) {
                    if (!req.id.equals("minecraft:air")) {
                        String initNbt = (req.nbtTag != null && !req.nbtTag.trim().isEmpty()) ? req.nbtTag : "";
                        KineticSelectors.openNbtEditor(this, initNbt, savedNbt -> {
                            req.nbtTag = savedNbt;
                            if (req.nbtMode == null || req.nbtMode.equals("NONE")) req.nbtMode = "WEAK";
                            KineticOverlays.toast(ColorText.translatable("msg.kineticarmory.common.saved"));
                        });
                    }
                } else {
                    KineticSelectors.openItemSelector(this, selection -> {
                        if (!selection.isItem()) return;
                        ItemStack stack = selection.stack();
                        req.id = getId(stack);
                        req.nbtTag = stack.hasTag() && stack.getTag() != null ? stack.getTag().toString() : "{}";
                        if (req.nbtMode == null) req.nbtMode = "NONE";
                        cleanCurios();
                        cleanRejectedCurios();
                        rebuildUi();
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
