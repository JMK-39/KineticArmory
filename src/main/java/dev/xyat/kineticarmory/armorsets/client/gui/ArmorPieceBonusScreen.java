package dev.xyat.kineticarmory.armorsets.client.gui;

import dev.xyat.kineticarmory.util.ColorText;
import dev.xyat.kineticarmory.armorsets.data.ArmorDataConfig;
import dev.xyat.kineticarmory.armorsets.data.ArmorTipGenerator;
import dev.xyat.kineticcore.api.client.GuiRenderUtil;
import dev.xyat.kineticcore.api.client.GuiToastUtil;
import dev.xyat.kineticcore.api.client.ScaledScreen;
import dev.xyat.kineticcore.api.client.gui.HighZButton;
import dev.xyat.kineticcore.api.client.gui.NumericEditBox;
import dev.xyat.kineticcore.api.client.gui.GridScrollController;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.regex.Pattern;

public class ArmorPieceBonusScreen extends ScaledScreen {
    private static final Pattern ICON_PATTERN = Pattern.compile("\\[(item|effect):([^]]+)]");
    private static final Pattern COLOR_PATTERN = Pattern.compile("§[0-9a-fk-or]", Pattern.CASE_INSENSITIVE);
    private static final int PANEL_PADDING = 14;
    private static final int ROW_H = 25;
    private static final int VISIBLE_ROWS = 10;
    private static final int SCROLL_W = 6;

    private final ScaledScreen parent;
    private final ArmorDataConfig config;
    private final List<PieceEffectEntry> effects = new ArrayList<>();

    private NumericEditBox pieceInput;
    private NumericEditBox valueInput;
    private PieceEffectEntry selectedEffect;
    private int selectedPieces = -1;
    private final GridScrollController effectScroll = new GridScrollController();
    private final GridScrollController tierScroll = new GridScrollController();
    private Component warningMessage;
    private String tempPieceInput;
    private String tempValueInput;

    private int leftX;
    private int leftY;
    private int leftW;
    private int leftH;
    private int rightX;
    private int rightY;
    private int rightW;
    private int rightH;

    public ArmorPieceBonusScreen(ScaledScreen parent, ArmorDataConfig config) {
        super(ColorText.translatable("gui.kineticarmory.armorsets.piece.bonus.title"));
        this.parent = parent;
        this.config = config;
        configureResponsiveCanvas(
                640f,
                360f,
                6
        );
        this.config.initNullFields();
        this.config.preparePieceBonusData();
    }

    @Override
    public void tick() {
        super.tick();
        if (pieceInput != null) tempPieceInput = pieceInput.getValue();
        if (valueInput != null) tempValueInput = valueInput.getValue();
    }

    @Override
    protected void initScaled() {
        config.initNullFields();
        config.preparePieceBonusData();
        rebuildEffects();

        int panelW = vWidth - PANEL_PADDING * 2;
        int panelH = vHeight - PANEL_PADDING * 2;
        int controlY = PANEL_PADDING + 36;
        int inputX = PANEL_PADDING + 12;
        int doneX = PANEL_PADDING + panelW - 92;

        if (selectedEffect == null && !effects.isEmpty()) selectedEffect = effects.get(0);
        if (selectedPieces < 2) selectedPieces = findFirstConfiguredPiecesForSelectedEffect();
        if (selectedPieces < 2) selectedPieces = 2;
        if (tempPieceInput == null) tempPieceInput = String.valueOf(selectedPieces);

        pieceInput = NumericEditBox.integer(
                font, inputX, controlY, 54, 20,
                Component.empty(), false, null, null
        );
        pieceInput.setMaxLength(3);
        pieceInput.setValue(tempPieceInput);
        pieceInput.setTooltip(Tooltip.create(ColorText.translatable("gui.kineticarmory.armorsets.piece.bonus.tooltip.piece_input")));
        addRenderableWidget(pieceInput);

        int saveTierX = inputX + 62;
        addRenderableWidget(new HighZButton(saveTierX, controlY, 82, 20, ColorText.translatable("gui.kineticarmory.armorsets.piece.bonus.save_tier"), b -> saveTierForSelectedEffect(), Tooltip.create(ColorText.translatable("gui.kineticarmory.armorsets.piece.bonus.tooltip.save_tier"))));

        int valueX = saveTierX + 98;
        valueInput = NumericEditBox.decimal(
                font, valueX, controlY, 90, 20,
                Component.empty(), true, null, null
        );
        valueInput.setMaxLength(32);
        valueInput.setTooltip(Tooltip.create(ColorText.translatable("gui.kineticarmory.armorsets.piece.bonus.tooltip.value_input")));
        if (tempValueInput != null) valueInput.setValue(tempValueInput);
        addRenderableWidget(valueInput);

        addRenderableWidget(new HighZButton(valueX + 98, controlY, 74, 20, ColorText.translatable("gui.kineticarmory.armorsets.piece.bonus.value_save"), b -> saveSelectedValue(), Tooltip.create(ColorText.translatable("gui.kineticarmory.armorsets.piece.bonus.tooltip.value_save"))));
        addRenderableWidget(new HighZButton(valueX + 178, controlY, 74, 20, ColorText.translatable("gui.kineticarmory.armorsets.piece.bonus.value_clear"), b -> clearSelectedValue(), Tooltip.create(ColorText.translatable("gui.kineticarmory.armorsets.piece.bonus.tooltip.value_clear"))));
        addRenderableWidget(new HighZButton(doneX, controlY, 80, 20, ColorText.translatable("gui.kineticarmory.armorsets.piece.bonus.confirm"), b -> closeToParent(), Tooltip.create(ColorText.translatable("gui.kineticarmory.armorsets.piece.bonus.tooltip.confirm"))));

        leftX = PANEL_PADDING + 12;
        leftY = controlY + 36;
        leftW = 250;
        int listBottom = PANEL_PADDING + panelH - 10;
        int maxListH = Math.max(ROW_H * 4, listBottom - leftY);
        leftH = Math.min(ROW_H * VISIBLE_ROWS, maxListH);
        rightX = leftX + leftW + 18;
        rightY = leftY;
        rightW = panelW - leftW - 42;
        rightH = leftH;

        loadSelectedValue();
        clampScrolls();
    }

    private void rebuildEffects() {
        String keepKey = selectedEffect == null ? null : selectedEffect.key();
        effects.clear();
        if (config.potionEffects != null) for (ArmorDataConfig.PotionEffectData d : config.potionEffects) effects.add(new PieceEffectEntry(config.keyOf(d), ArmorTipGenerator.genPotTip(d), d.amplifier, true));
        if (config.attributes != null) for (ArmorDataConfig.AttributeModifierData d : config.attributes) effects.add(new PieceEffectEntry(config.keyOf(d), ArmorTipGenerator.genAttrTip(d), d.amount, true));
        if (config.damageImmunities != null) for (ArmorDataConfig.DamageImmunityData d : config.damageImmunities) effects.add(new PieceEffectEntry(config.keyOf(d), ArmorTipGenerator.genImmTip(d), d.multiplier, true));
        if (config.effectImmunities != null) for (ArmorDataConfig.EffectImmunityData d : config.effectImmunities) effects.add(new PieceEffectEntry(config.keyOf(d), ArmorTipGenerator.genEffImmTip(d), 0.0, false));
        if (config.attackEffects != null) for (ArmorDataConfig.AttackEffectData d : config.attackEffects) effects.add(new PieceEffectEntry(config.keyOf(d), ArmorTipGenerator.genAtkTip(d), d.amplifier, true));
        if (config.damageConversions != null) for (ArmorDataConfig.DamageConversionData d : config.damageConversions) effects.add(new PieceEffectEntry(config.keyOf(d), ArmorTipGenerator.genConvTip(d), d.ratio, true));
        if (config.damageMultipliers != null) for (ArmorDataConfig.DamageMultiplierData d : config.damageMultipliers) effects.add(new PieceEffectEntry(config.keyOf(d), ArmorTipGenerator.genDmgMulTip(d), d.multiplier, true));
        if (config.attackDamageMultipliers != null) for (ArmorDataConfig.AttackDamageMultiplierData d : config.attackDamageMultipliers) effects.add(new PieceEffectEntry(config.keyOf(d), ArmorTipGenerator.genAtkDmgTip(d), d.multiplier, true));
        if (config.allowFlight) effects.add(new PieceEffectEntry(config.keyOfFlight(), ArmorTipGenerator.genFlightTip(config.flightConditions, config.flightConditionMatchMode, config.flightConditionMinCount), 0.0, false));

        selectedEffect = null;
        if (keepKey != null) {
            for (PieceEffectEntry effect : effects) {
                if (keepKey.equals(effect.key())) {
                    selectedEffect = effect;
                    break;
                }
            }
        }
        if (selectedEffect == null && !effects.isEmpty()) selectedEffect = effects.get(0);
    }

    private List<ArmorDataConfig.PieceBonusGroup> sortedGroups() {
        config.preparePieceBonusData();
        List<ArmorDataConfig.PieceBonusGroup> groups = new ArrayList<>(config.pieceBonusGroups);
        groups.removeIf(group -> group == null || group.pieces < 2 || group.pieces > Math.max(1, config.getTotalPieceCount()));
        groups.sort(Comparator.comparingInt(group -> group.pieces));
        return groups;
    }

    private List<TierOption> buildTierOptions() {
        int total = Math.max(1, config.getTotalPieceCount());
        List<ArmorDataConfig.PieceBonusGroup> groups = sortedGroups();
        List<TierOption> result = new ArrayList<>();
        for (int pieces = 2; pieces <= total; pieces++) {
            result.add(new TierOption(pieces, findGroup(groups, pieces)));
        }
        return result;
    }

    private ArmorDataConfig.PieceBonusGroup findGroup(List<ArmorDataConfig.PieceBonusGroup> groups, int pieces) {
        for (ArmorDataConfig.PieceBonusGroup group : groups) {
            if (group != null && group.pieces == pieces) return group;
        }
        return null;
    }

    private int findFirstConfiguredPiecesForSelectedEffect() {
        if (selectedEffect == null) return -1;
        for (ArmorDataConfig.PieceBonusGroup group : sortedGroups()) {
            if (groupContainsSelectedEffect(group)) return group.pieces;
        }
        return 2;
    }

    private void saveTierForSelectedEffect() {
        saveSelectedValue();
    }

    private void saveSelectedValue() {
        if (selectedEffect == null) {
            warningMessage = ColorText.translatable("gui.kineticarmory.armorsets.piece.bonus.warn_select_effect");
            return;
        }
        int pieces = parsePiecesFromInput();
        if (pieces < 0) return;
        ArmorDataConfig.PieceBonusGroup group = getOrCreateGroup(pieces);
        selectedPieces = pieces;
        putSelectedEffect(group, selectedEffect.baseValue());
        if (selectedEffect.valueEditable()) {
            if (!putSelectedValue(group)) return;
        }
        config.flexiblePieces = true;
        config.preparePieceBonusData();
        loadSelectedValue();
        clampScrolls();
        warningMessage = null;
        GuiToastUtil.showToast(ColorText.translatable("msg.kineticarmory.common.saved"));
    }

    private boolean putSelectedValue(ArmorDataConfig.PieceBonusGroup group) {
        if (valueInput == null || valueInput.getValue().trim().isEmpty()) {
            warningMessage = ColorText.translatable("gui.kineticarmory.armorsets.piece.bonus.warn_value_number");
            return false;
        }
        Double value = valueInput.getDoubleValue();
        if (value == null) {
            warningMessage = ColorText.translatable(
                    "gui.kineticarmory.armorsets.piece.bonus.warn_value_number"
            );
            return false;
        }

        if (group.effectValues == null) group.effectValues = new HashMap<>();
        group.effectValues.put(selectedEffect.key(), value);
        tempValueInput = NumericEditBox.format(value);
        return true;
    }

    private void clearSelectedValue() {
        if (selectedEffect == null) {
            warningMessage = ColorText.translatable("gui.kineticarmory.armorsets.piece.bonus.warn_select_effect");
            return;
        }
        ArmorDataConfig.PieceBonusGroup group = findSelectedGroup();
        if (group == null) {
            warningMessage = ColorText.translatable("gui.kineticarmory.armorsets.piece.bonus.warn_select_tier");
            return;
        }
        removeSelectedEffect(group);
        pruneEmptyGroups();
        config.preparePieceBonusData();
        loadSelectedValue();
        clampScrolls();
        warningMessage = null;
        GuiToastUtil.showToast(ColorText.translatable("msg.kineticarmory.common.saved"));
    }

    private int parsePiecesFromInput() {
        String text = pieceInput == null ? "" : pieceInput.getValue().trim();
        if (text.isEmpty()) {
            warningMessage = ColorText.translatable("gui.kineticarmory.armorsets.piece.bonus.warn_number");
            return -1;
        }
        Integer parsedPieces = pieceInput.getIntValue();
        if (parsedPieces == null) {
            warningMessage = ColorText.translatable(
                    "gui.kineticarmory.armorsets.piece.bonus.warn_number"
            );
            return -1;
        }

        int pieces = parsedPieces;
        int total = Math.max(1, config.getTotalPieceCount());
        if (pieces < 2) {
            warningMessage = ColorText.translatable("gui.kineticarmory.armorsets.piece.bonus.warn_min");
            return -1;
        }
        if (pieces > total) {
            warningMessage = ColorText.translatable("gui.kineticarmory.armorsets.piece.bonus.warn_full", total);
            return -1;
        }
        return pieces;
    }

    private ArmorDataConfig.PieceBonusGroup getOrCreateGroup(int pieces) {
        if (config.pieceBonusGroups == null) config.pieceBonusGroups = new ArrayList<>();
        for (ArmorDataConfig.PieceBonusGroup group : config.pieceBonusGroups) {
            if (group != null && group.pieces == pieces) {
                ensureGroupFields(group);
                return group;
            }
        }
        ArmorDataConfig.PieceBonusGroup group = ArmorDataConfig.PieceBonusGroup.create(pieces);
        ensureGroupFields(group);
        config.pieceBonusGroups.add(group);
        return group;
    }

    private ArmorDataConfig.PieceBonusGroup findSelectedGroup() {
        if (selectedPieces < 2 || config.pieceBonusGroups == null) return null;
        for (ArmorDataConfig.PieceBonusGroup group : config.pieceBonusGroups) {
            if (group != null && group.pieces == selectedPieces) return group;
        }
        return null;
    }

    private void ensureGroupFields(ArmorDataConfig.PieceBonusGroup group) {
        if (group.effectKeys == null) group.effectKeys = new ArrayList<>();
        if (group.effectValues == null) group.effectValues = new HashMap<>();
    }

    private void putSelectedEffect(ArmorDataConfig.PieceBonusGroup group, double baseValue) {
        if (selectedEffect == null || selectedEffect.key().isBlank()) return;
        ensureGroupFields(group);
        if (!group.effectKeys.contains(selectedEffect.key())) group.effectKeys.add(selectedEffect.key());
        if (selectedEffect.valueEditable() && !group.effectValues.containsKey(selectedEffect.key())) group.effectValues.put(selectedEffect.key(), baseValue);
    }

    private void removeSelectedEffect(ArmorDataConfig.PieceBonusGroup group) {
        if (selectedEffect == null || group == null) return;
        ensureGroupFields(group);
        group.effectKeys.remove(selectedEffect.key());
        group.effectValues.remove(selectedEffect.key());
    }

    private void pruneEmptyGroups() {
        if (config.pieceBonusGroups == null) return;
        Iterator<ArmorDataConfig.PieceBonusGroup> iterator = config.pieceBonusGroups.iterator();
        while (iterator.hasNext()) {
            ArmorDataConfig.PieceBonusGroup group = iterator.next();
            if (group == null) {
                iterator.remove();
                continue;
            }
            boolean noKeys = group.effectKeys == null || group.effectKeys.isEmpty();
            boolean noValues = group.effectValues == null || group.effectValues.isEmpty();
            if (noKeys && noValues) iterator.remove();
        }
    }

    private boolean groupContainsSelectedEffect(ArmorDataConfig.PieceBonusGroup group) {
        if (selectedEffect == null || group == null) return false;
        String key = selectedEffect.key();
        return (group.effectKeys != null && group.effectKeys.contains(key)) || (group.effectValues != null && group.effectValues.containsKey(key));
    }

    private double getGroupValue(ArmorDataConfig.PieceBonusGroup group, PieceEffectEntry effect) {
        if (group != null && group.effectValues != null && group.effectValues.containsKey(effect.key())) {
            Double value = group.effectValues.get(effect.key());
            if (value != null && Double.isFinite(value)) return value;
        }
        return effect.baseValue();
    }

    private String getTierValueText(ArmorDataConfig.PieceBonusGroup group, PieceEffectEntry effect) {
        if (effect == null) return "";
        if (!containsEffect(group, effect)) return ColorText.translatable("gui.kineticarmory.armorsets.piece.bonus.tier_unset").getString();
        if (!effect.valueEditable()) return ColorText.translatable("gui.kineticarmory.armorsets.piece.bonus.tier_enabled").getString();
        return ColorText.translatable("gui.kineticarmory.armorsets.piece.bonus.tier_value", fmt(getGroupValue(group, effect))).getString();
    }

    private void selectEffect(PieceEffectEntry effect) {
        selectedEffect = effect;
        int first = findFirstConfiguredPiecesForSelectedEffect();
        selectedPieces = Math.max(first, 2);
        if (pieceInput != null) pieceInput.setValue(String.valueOf(selectedPieces));
        loadSelectedValue();
    }

    private void selectTier(TierOption option) {
        if (option == null) return;
        selectedPieces = option.pieces();
        if (pieceInput != null) pieceInput.setValue(String.valueOf(selectedPieces));
        loadSelectedValue();
    }

    private void toggleSelectedEffectInTier(TierOption option) {
        if (selectedEffect == null || option == null) return;
        ArmorDataConfig.PieceBonusGroup group = option.group();
        if (group != null && groupContainsSelectedEffect(group)) {
            removeSelectedEffect(group);
            pruneEmptyGroups();
            config.preparePieceBonusData();
            selectedPieces = option.pieces();
            if (pieceInput != null) pieceInput.setValue(String.valueOf(selectedPieces));
            loadSelectedValue();
            clampScrolls();
            warningMessage = null;
            GuiToastUtil.showToast(ColorText.translatable("msg.kineticarmory.common.saved"));
            return;
        }
        selectedPieces = option.pieces();
        if (pieceInput != null) pieceInput.setValue(String.valueOf(selectedPieces));
        saveSelectedValue();
    }

    private void loadSelectedValue() {
        if (valueInput == null || selectedEffect == null) return;
        valueInput.active = selectedEffect.valueEditable();
        if (!selectedEffect.valueEditable()) {
            valueInput.setValue("");
            tempValueInput = "";
            return;
        }
        ArmorDataConfig.PieceBonusGroup group = findSelectedGroup();
        double value = selectedEffect.baseValue();
        if (group != null) value = getGroupValue(group, selectedEffect);
        String text = fmt(value);
        valueInput.setValue(text);
        tempValueInput = text;
    }

    private String fmt(double d) {
        return d == (long)d ? String.valueOf((long)d) : String.valueOf(d);
    }

    private void closeToParent() {
        pruneEmptyGroups();
        config.preparePieceBonusData();
        if (minecraft != null) minecraft.setScreen(parent);
    }

    @Override
    protected void renderScaledBackground(@NotNull GuiGraphics g, int mx, int my, float pt) {
        drawStrongPanel(g, PANEL_PADDING, PANEL_PADDING, vWidth - PANEL_PADDING * 2, vHeight - PANEL_PADDING * 2, 0xFF1C1C1C);
        g.drawCenteredString(font, title, vWidth / 2, PANEL_PADDING + 10, 0xFFFFFF);
        drawStrongPanel(g, leftX, leftY, leftW, leftH, 0xDD050505);
        drawStrongPanel(g, rightX, rightY, rightW, rightH, 0xDD050505);
        renderEffectRows(g, mx, my);
        renderTierRows(g, mx, my);
    }

    @Override
    protected void renderScaledForeground(@NotNull GuiGraphics g, int mx, int my, float pt) {
        if (pieceInput != null && !pieceInput.isFocused() && pieceInput.getValue().isEmpty()) {
            String hint = ColorText.translatable("gui.kineticarmory.armorsets.piece.bonus.input_hint").getString();
            g.drawString(font, font.plainSubstrByWidth(hint, pieceInput.getWidth() - 8), pieceInput.getX() + 4, pieceInput.getY() + 6, 0xFFAAAAAA, false);
        }
        if (valueInput != null && !valueInput.isFocused() && valueInput.getValue().isEmpty() && valueInput.active) {
            String hint = ColorText.translatable("gui.kineticarmory.armorsets.piece.bonus.value_hint").getString();
            g.drawString(font, font.plainSubstrByWidth(hint, valueInput.getWidth() - 8), valueInput.getX() + 4, valueInput.getY() + 6, 0xFFAAAAAA, false);
        }
        if (pieceInput != null) {
            g.drawString(font, ColorText.translatable("gui.kineticarmory.armorsets.piece.bonus.piece_input_label"), pieceInput.getX(), pieceInput.getY() - 11, 0xFFFFAA00, false);
        }
        if (valueInput != null) {
            g.drawString(font, ColorText.translatable("gui.kineticarmory.armorsets.piece.bonus.value_input_label"), valueInput.getX(), valueInput.getY() - 11, 0xFFFFAA00, false);
        }
        g.drawString(font, ColorText.translatable("gui.kineticarmory.armorsets.piece.bonus.effect_list_title"), leftX, leftY - 13, 0xFFFFAA00, false);
        g.drawString(font, ColorText.translatable("gui.kineticarmory.armorsets.piece.bonus.tier_list_title"), rightX, rightY - 13, 0xFFFFAA00, false);
        if (warningMessage != null) g.drawCenteredString(font, warningMessage, vWidth / 2, PANEL_PADDING + 24, 0xFFFF5555);
    }

    private void drawStrongPanel(GuiGraphics g, int x, int y, int w, int h, int bgColor) {
        GuiRenderUtil.drawPanel(g, x, y, w, h, bgColor, 0xFF8A8A8A);
        g.renderOutline(x + 1, y + 1, w - 2, h - 2, 0xFF3A3A3A);
    }

    private void drawRowOutline(GuiGraphics g, int x, int y, int w, int color) {
        g.renderOutline(x, y, w, ROW_H - 2, color);
        g.renderOutline(x + 1, y + 1, w - 2, ROW_H - 4, 0xFF2E2E2E);
    }

    private void renderEffectRows(GuiGraphics g, int mx, int my) {
        int visible = Math.max(1, leftH / ROW_H);
        effectScroll.update(effects.size(), visible);
        if (effects.isEmpty()) {
            g.drawCenteredString(font, ColorText.translatable("gui.kineticarmory.armorsets.piece.bonus.empty_effects"), leftX + leftW / 2, leftY + 16, 0xFFAAAAAA);
            return;
        }
        int count = Math.min(visible, effects.size() - effectScroll.offset());
        for (int i = 0; i < count; i++) {
            int index = effectScroll.offset() + i;
            PieceEffectEntry effect = effects.get(index);
            int y = leftY + i * ROW_H;
            boolean hover = isInside(mx, my, leftX, y, leftW - 10, ROW_H);
            boolean selected = effect == selectedEffect;
            g.fill(leftX + 1, y + 1, leftX + leftW - 10, y + ROW_H - 1, selected ? 0xAA775500 : (hover ? 0x88444444 : 0x88222222));
            drawRowOutline(g, leftX + 1, y + 1, leftW - 11, selected ? 0xFFFFB000 : 0xFF707070);
            String text = cleanDisplayText(effect.text());
            drawTrimmedText(g, text, leftX + 6, y + 5, leftW - 22, selected ? 0xFFFFFFFF : 0xFFDDDDDD);
            String summary = buildEffectSummary(effect);
            if (!summary.isEmpty()) drawTrimmedText(g, summary, leftX + 6, y + 16, leftW - 22, 0xFF55FF55);
        }
        effectScroll.render(
                g, mx, my,
                leftX + leftW - SCROLL_W - 2,
                leftY + 2,
                SCROLL_W,
                leftH - 4,
                18
        );
    }

    private void renderTierRows(GuiGraphics g, int mx, int my) {
        List<TierOption> tiers = buildTierOptions();
        int visible = Math.max(1, rightH / ROW_H);
        tierScroll.update(tiers.size(), visible);
        if (tiers.isEmpty()) {
            g.drawCenteredString(font, ColorText.translatable("gui.kineticarmory.armorsets.piece.bonus.empty_tiers"), rightX + rightW / 2, rightY + 16, 0xFFAAAAAA);
            return;
        }
        int count = Math.min(visible, tiers.size() - tierScroll.offset());
        for (int i = 0; i < count; i++) {
            int index = tierScroll.offset() + i;
            TierOption option = tiers.get(index);
            ArmorDataConfig.PieceBonusGroup group = option.group();
            int y = rightY + i * ROW_H;
            boolean hover = isInside(mx, my, rightX, y, rightW - 10, ROW_H);
            boolean rowSelected = option.pieces() == selectedPieces;
            boolean enabled = selectedEffect != null && containsEffect(group, selectedEffect);
            int bg = rowSelected ? 0xAA775500 : (hover ? 0x88444444 : 0x88222222);
            g.fill(rightX + 1, y + 1, rightX + rightW - 10, y + ROW_H - 1, bg);
            drawRowOutline(g, rightX + 1, y + 1, rightW - 11, enabled ? 0xFF55FF55 : (rowSelected ? 0xFFFFB000 : 0xFF707070));
            g.drawString(font, enabled ? "[x]" : "[ ]", rightX + 6, y + 9, enabled ? 0xFF55FF55 : 0xFFAAAAAA, false);
            Component label = ColorText.translatable("gui.kineticarmory.armorsets.piece.bonus.tier_row", option.pieces());
            g.drawString(font, label, rightX + 36, y + 9, rowSelected ? 0xFFFFFFFF : 0xFFDDDDDD, false);
            if (selectedEffect != null) {
                String valueText = getTierValueText(group, selectedEffect);
                int valueW = Math.min(132, font.width(valueText));
                g.drawString(font, font.plainSubstrByWidth(valueText, valueW), rightX + rightW - valueW - 18, y + 9, enabled ? 0xFFFFFF55 : 0xFFAAAAAA, false);
            }
        }
        tierScroll.render(
                g, mx, my,
                rightX + rightW - SCROLL_W - 2,
                rightY + 2,
                SCROLL_W,
                rightH - 4,
                18
        );
    }

    private String buildEffectSummary(PieceEffectEntry effect) {
        if (effect == null) return "";
        StringBuilder builder = new StringBuilder();
        for (ArmorDataConfig.PieceBonusGroup group : sortedGroups()) {
            if (!containsEffect(group, effect)) continue;
            if (!builder.isEmpty()) builder.append("  ");
            builder.append(group.pieces).append("=");
            if (effect.valueEditable()) builder.append(fmt(getGroupValue(group, effect)));
            else builder.append("+");
        }
        return builder.toString();
    }

    private boolean containsEffect(ArmorDataConfig.PieceBonusGroup group, PieceEffectEntry effect) {
        if (group == null || effect == null) return false;
        return (group.effectKeys != null && group.effectKeys.contains(effect.key())) || (group.effectValues != null && group.effectValues.containsKey(effect.key()));
    }

    private String cleanDisplayText(String text) {
        if (text == null) return "";
        return COLOR_PATTERN.matcher(ICON_PATTERN.matcher(text).replaceAll("")).replaceAll("").replace('\n', ' ').trim();
    }

    private void drawTrimmedText(GuiGraphics g, String text, int x, int y, int maxWidth, int color) {
        g.drawString(font, font.plainSubstrByWidth(text == null ? "" : text, Math.max(0, maxWidth)), x, y, color, false);
    }

    private boolean isInside(double mx, double my, int x, int y, int w, int h) {
        return GuiRenderUtil.isHovering(mx, my, x, y, w, h);
    }

    private void clampScrolls() {
        effectScroll.update(
                effects.size(),
                Math.max(1, leftH / ROW_H)
        );
        tierScroll.update(
                buildTierOptions().size(),
                Math.max(1, rightH / ROW_H)
        );
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (pieceInput != null && pieceInput.isFocused()) {
            if (keyCode == 257 || keyCode == 335) {
                saveTierForSelectedEffect();
                return true;
            }
            if (pieceInput.keyPressed(keyCode, scanCode, modifiers)) return true;
        }
        if (valueInput != null && valueInput.isFocused()) {
            if (keyCode == 257 || keyCode == 335) {
                saveSelectedValue();
                return true;
            }
            if (valueInput.keyPressed(keyCode, scanCode, modifiers)) return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean charTyped(char codePoint, int modifiers) {
        if (pieceInput != null && pieceInput.isFocused() && Character.isDigit(codePoint)) return pieceInput.charTyped(codePoint, modifiers);
        if (valueInput != null && valueInput.isFocused()) return valueInput.charTyped(codePoint, modifiers);
        return super.charTyped(codePoint, modifiers);
    }

    @Override
    protected boolean universalMouseClicked(double mx, double my, int btn) {
        if (pieceInput != null && pieceInput.isMouseOver(mx, my)) {
            setFocused(pieceInput);
            pieceInput.setFocused(true);
            return pieceInput.mouseClicked(mx, my, btn);
        } else if (pieceInput != null) {
            pieceInput.setFocused(false);
        }
        if (valueInput != null && valueInput.isMouseOver(mx, my)) {
            setFocused(valueInput);
            valueInput.setFocused(true);
            return valueInput.mouseClicked(mx, my, btn);
        } else if (valueInput != null) {
            valueInput.setFocused(false);
        }

        if (tryStartScrollDrag(mx, my, leftX + leftW - SCROLL_W - 2, leftY + 2, leftH - 4, true)) return true;
        if (tryStartScrollDrag(mx, my, rightX + rightW - SCROLL_W - 2, rightY + 2, rightH - 4, false)) return true;

        if (isInside(mx, my, leftX, leftY, leftW - 10, leftH)) {
            int row = (int)((my - leftY) / ROW_H) + effectScroll.offset();
            if (row >= 0 && row < effects.size()) {
                selectEffect(effects.get(row));
                return true;
            }
        }

        if (isInside(mx, my, rightX, rightY, rightW - 10, rightH)) {
            List<TierOption> tiers = buildTierOptions();
            int row = (int)((my - rightY) / ROW_H) + tierScroll.offset();
            if (row >= 0 && row < tiers.size()) {
                TierOption option = tiers.get(row);
                selectTier(option);
                if (btn == 1) toggleSelectedEffectInTier(option);
                return true;
            }
        }

        return super.universalMouseClicked(mx, my, btn);
    }

    private boolean tryStartScrollDrag(double mx, double my, int x, int y, int h, boolean effectList) {
        int visible = Math.max(1, (effectList ? leftH : rightH) / ROW_H);
        int total = effectList ? effects.size() : buildTierOptions().size();
        GridScrollController controller = effectList ? effectScroll : tierScroll;
        controller.update(total, visible);
        return controller.beginDrag(
                mx, my,
                x, y,
                SCROLL_W, h,
                18, 4
        );
    }

    @Override
    protected boolean universalMouseDragged(double mx, double my, int btn, double dx, double dy) {
        if (effectScroll.drag(my, leftY + 2, leftH - 4, 18)) return true;
        if (tierScroll.drag(my, rightY + 2, rightH - 4, 18)) return true;
        return super.universalMouseDragged(mx, my, btn, dx, dy);
    }

    @Override
    protected boolean universalMouseReleased(double mx, double my, int btn) {
        boolean handled = effectScroll.release(btn);
        handled = tierScroll.release(btn) || handled;
        return handled || super.universalMouseReleased(mx, my, btn);
    }

    @Override
    protected boolean universalMouseScrolled(double mx, double my, double delta) {
        if (isInside(mx, my, leftX, leftY, leftW, leftH)) {
            effectScroll.update(
                    effects.size(),
                    Math.max(1, leftH / ROW_H)
            );
            if (effectScroll.scroll(delta)) return true;
        }

        if (isInside(mx, my, rightX, rightY, rightW, rightH)) {
            tierScroll.update(
                    buildTierOptions().size(),
                    Math.max(1, rightH / ROW_H)
            );
            if (tierScroll.scroll(delta)) return true;
        }

        return super.universalMouseScrolled(mx, my, delta);
    }

    private record PieceEffectEntry(String key, String text, double baseValue, boolean valueEditable) {}
    private record TierOption(int pieces, ArmorDataConfig.PieceBonusGroup group) {}
}
