package dev.xyat.kineticarmory.armorsets.client.gui;

import dev.xyat.kineticcore.api.client.gui.input.KeyInput;
import dev.xyat.kineticcore.api.client.gui.input.ScrollInput;
import dev.xyat.kineticcore.api.client.gui.input.MouseDragInput;
import dev.xyat.kineticcore.api.client.gui.input.MouseInput;
import dev.xyat.kineticcore.api.client.gui.overlay.KineticOverlays;
import dev.xyat.kineticcore.api.client.gui.page.KineticPage;
import dev.xyat.kineticcore.api.client.gui.render.KineticGraphics;
import dev.xyat.kineticcore.api.client.gui.scroll.KineticScrollController;
import dev.xyat.kineticcore.api.client.gui.theme.KineticTheme;
import dev.xyat.kineticcore.api.client.gui.ui.KineticUi;
import dev.xyat.kineticcore.api.client.gui.ui.NumberType;
import dev.xyat.kineticcore.api.client.gui.widget.*;
import dev.xyat.kineticcore.api.client.gui.widget.list.*;

import dev.xyat.kineticcore.api.client.input.KineticMouseButtons;
import dev.xyat.kineticcore.api.client.gui.input.MouseButton;
import dev.xyat.kineticarmory.armorsets.data.ArmorDataConfig;
import dev.xyat.kineticarmory.armorsets.data.ArmorTipGenerator;
import dev.xyat.kineticcore.api.client.input.KineticKeyBindings;
import dev.xyat.kineticcore.api.text.KineticI18n;
import net.minecraft.network.chat.Component;
import dev.xyat.kineticcore.api.registry.KineticRegistries;
import dev.xyat.kineticcore.api.resource.KineticResourceIds;
import net.minecraft.world.effect.MobEffect;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.regex.Pattern;

public class ArmorPieceBonusPage extends KineticPage {
    private static final Pattern ICON_PATTERN = Pattern.compile("\\[(item|effect):([^]]+)]");
    private static final Pattern COLOR_PATTERN = Pattern.compile("§[0-9a-fk-or]", Pattern.CASE_INSENSITIVE);
    private static final int PANEL_PADDING = 14;
    private static final int ROW_H = 25;
    private static final int VISIBLE_ROWS = 10;
    private static final int SCROLL_W = 4;
    private static final int TEXT_GAP = 4;
    // The existing right-aligned tier value column reserves 132 pixels.
    private static final int TIER_VALUE_W = 132;

    
    private final ArmorDataConfig config;
    private final List<PieceEffectEntry> effects = new ArrayList<>();

    private KineticNumberField pieceInput;
    private KineticNumberField valueInput;
    private PieceEffectEntry selectedEffect;
    private int selectedPieces = -1;
    private final KineticScrollController effectScroll = new KineticScrollController();
    private final KineticScrollController tierScroll = new KineticScrollController();
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

    public ArmorPieceBonusPage(ArmorDataConfig config) {
        super(KineticI18n.translatable("gui.kineticarmory.armorsets.piece.bonus.title"));
        this.config = config;
        this.config.initNullFields();
        this.config.preparePieceBonusData();
    }

    @Override
    protected void onTick() {
        if (pieceInput != null) tempPieceInput = pieceInput.textValue();
        if (valueInput != null) tempValueInput = valueInput.textValue();
    }

    @Override
    protected void build(KineticUi ui) {
        config.initNullFields();
        config.preparePieceBonusData();
        rebuildEffects();

        int panelW = width() - PANEL_PADDING * 2;
        int panelH = height() - PANEL_PADDING * 2;
        int controlY = PANEL_PADDING + 36;
        int inputX = PANEL_PADDING + 12;

        if (selectedEffect == null && !effects.isEmpty()) selectedEffect = effects.get(0);
        if (selectedPieces < 2) selectedPieces = findFirstConfiguredPiecesForSelectedEffect();
        if (selectedPieces < 2) selectedPieces = 2;
        if (tempPieceInput == null) tempPieceInput = String.valueOf(selectedPieces);

        pieceInput = ui().numberField(inputX, controlY, 54, NumberType.INT).firstShownTextAsDefault().build();
        pieceInput.limitTextLength(3);
        pieceInput.setPlaceholder(KineticI18n.translatable("gui.kineticarmory.armorsets.piece.bonus.input_hint"));
        pieceInput.setTextValue(tempPieceInput);
        pieceInput.setTooltip(KineticI18n.translatable("gui.kineticarmory.armorsets.piece.bonus.tooltip.piece_input"));
int saveTierX = inputX + 62;
        ui().button(saveTierX, controlY, 82).text(KineticI18n.translatable("gui.kineticarmory.armorsets.piece.bonus.save_tier")).tooltip(KineticI18n.translatable("gui.kineticarmory.armorsets.piece.bonus.tooltip.save_tier")).layer(1).onClick(this::saveTierForSelectedEffect).build();

        int valueX = saveTierX + 98;
        valueInput = ui().numberField(valueX, controlY, 90, NumberType.DECIMAL).allowNegative(true).firstShownTextAsDefault().build();
        valueInput.limitTextLength(32);
        valueInput.setPlaceholder(KineticI18n.translatable("gui.kineticarmory.armorsets.piece.bonus.value_hint"));
        valueInput.setTooltip(KineticI18n.translatable("gui.kineticarmory.armorsets.piece.bonus.tooltip.value_input"));
        if (tempValueInput != null) valueInput.setTextValue(tempValueInput);
ui().button(valueX + 98, controlY, 74).text(KineticI18n.translatable("gui.kineticarmory.armorsets.piece.bonus.value_save")).tooltip(KineticI18n.translatable("gui.kineticarmory.armorsets.piece.bonus.tooltip.value_save")).layer(1).onClick(this::saveSelectedValue).build();
        ui().button(valueX + 178, controlY, 74).text(KineticI18n.translatable("gui.kineticarmory.armorsets.piece.bonus.value_clear")).tooltip(KineticI18n.translatable("gui.kineticarmory.armorsets.piece.bonus.tooltip.value_clear")).layer(1).onClick(this::clearSelectedValue).build();
        ui().button(inputX, PANEL_PADDING + 8, 80).text(KineticI18n.translatable("gui.kineticarmory.armorsets.piece.bonus.confirm")).tooltip(KineticI18n.translatable("gui.kineticarmory.armorsets.piece.bonus.tooltip.confirm")).layer(1).onClick(this::closeToParent).build();

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
        if (config.potionEffects != null) for (ArmorDataConfig.PotionEffectData d : config.potionEffects) effects.add(PieceEffectEntry.potion(config.keyOf(d), ArmorTipGenerator.singleLine(() -> ArmorTipGenerator.genPotTip(d)), d.amplifier, true, d.effectId));
        if (config.attributes != null) for (ArmorDataConfig.AttributeModifierData d : config.attributes) effects.add(new PieceEffectEntry(config.keyOf(d), ArmorTipGenerator.singleLine(() -> ArmorTipGenerator.genAttrTip(d)), d.amount, true));
        if (config.damageImmunities != null) for (ArmorDataConfig.DamageImmunityData d : config.damageImmunities) effects.add(new PieceEffectEntry(config.keyOf(d), ArmorTipGenerator.singleLine(() -> ArmorTipGenerator.genImmTip(d)), d.multiplier, true));
        if (config.effectImmunities != null) for (ArmorDataConfig.EffectImmunityData d : config.effectImmunities) effects.add(PieceEffectEntry.potion(config.keyOf(d), ArmorTipGenerator.singleLine(() -> ArmorTipGenerator.genEffImmTip(d)), 0.0, false, d.effectId));
        if (config.attackEffects != null) for (ArmorDataConfig.AttackEffectData d : config.attackEffects) effects.add(PieceEffectEntry.potion(config.keyOf(d), ArmorTipGenerator.singleLine(() -> ArmorTipGenerator.genAtkTip(d)), d.amplifier, true, d.effectId));
        if (config.damageConversions != null) for (ArmorDataConfig.DamageConversionData d : config.damageConversions) effects.add(new PieceEffectEntry(config.keyOf(d), ArmorTipGenerator.singleLine(() -> ArmorTipGenerator.genConvTip(d)), d.ratio, true));
        if (config.damageMultipliers != null) for (ArmorDataConfig.DamageMultiplierData d : config.damageMultipliers) effects.add(new PieceEffectEntry(config.keyOf(d), ArmorTipGenerator.singleLine(() -> ArmorTipGenerator.genDmgMulTip(d)), d.multiplier, true));
        if (config.attackDamageMultipliers != null) for (ArmorDataConfig.AttackDamageMultiplierData d : config.attackDamageMultipliers) effects.add(new PieceEffectEntry(config.keyOf(d), ArmorTipGenerator.singleLine(() -> ArmorTipGenerator.genAtkDmgTip(d)), d.multiplier, true));
        if (config.allowFlight) effects.add(new PieceEffectEntry(config.keyOfFlight(), ArmorTipGenerator.singleLine(() -> ArmorTipGenerator.genFlightTip(config.flightConditions, config.flightConditionMatchMode, config.flightConditionMinCount)), 0.0, false));

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
            warningMessage = KineticI18n.translatable("gui.kineticarmory.armorsets.piece.bonus.warn_select_effect");
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
        KineticOverlays.toast(KineticI18n.translatable("msg.kineticarmory.common.saved"));
    }

    private boolean putSelectedValue(ArmorDataConfig.PieceBonusGroup group) {
        if (valueInput == null || valueInput.textValue().trim().isEmpty()) {
            warningMessage = KineticI18n.translatable("gui.kineticarmory.armorsets.piece.bonus.warn_value_number");
            return false;
        }
        Double value = valueInput.getDoubleValue();
        if (value == null) {
            warningMessage = KineticI18n.translatable(
                    "gui.kineticarmory.armorsets.piece.bonus.warn_value_number"
            );
            return false;
        }

        if (group.effectValues == null) group.effectValues = new HashMap<>();
        group.effectValues.put(selectedEffect.key(), value);
        tempValueInput = NumberType.DECIMAL.format(value);
        return true;
    }

    private void clearSelectedValue() {
        if (selectedEffect == null) {
            warningMessage = KineticI18n.translatable("gui.kineticarmory.armorsets.piece.bonus.warn_select_effect");
            return;
        }
        ArmorDataConfig.PieceBonusGroup group = findSelectedGroup();
        if (group == null) {
            warningMessage = KineticI18n.translatable("gui.kineticarmory.armorsets.piece.bonus.warn_select_tier");
            return;
        }
        removeSelectedEffect(group);
        pruneEmptyGroups();
        config.preparePieceBonusData();
        loadSelectedValue();
        clampScrolls();
        warningMessage = null;
        KineticOverlays.toast(KineticI18n.translatable("msg.kineticarmory.common.saved"));
    }

    private int parsePiecesFromInput() {
        String text = pieceInput == null ? "" : pieceInput.textValue().trim();
        if (text.isEmpty()) {
            warningMessage = KineticI18n.translatable("gui.kineticarmory.armorsets.piece.bonus.warn_number");
            return -1;
        }
        Integer parsedPieces = pieceInput.getIntValue();
        if (parsedPieces == null) {
            warningMessage = KineticI18n.translatable(
                    "gui.kineticarmory.armorsets.piece.bonus.warn_number"
            );
            return -1;
        }

        int pieces = parsedPieces;
        int total = Math.max(1, config.getTotalPieceCount());
        if (pieces < 2) {
            warningMessage = KineticI18n.translatable("gui.kineticarmory.armorsets.piece.bonus.warn_min");
            return -1;
        }
        if (pieces > total) {
            warningMessage = KineticI18n.translatable("gui.kineticarmory.armorsets.piece.bonus.warn_full", total);
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
        if (!containsEffect(group, effect)) return KineticI18n.translatable("gui.kineticarmory.armorsets.piece.bonus.tier_unset").getString();
        if (!effect.valueEditable()) return KineticI18n.translatable("gui.kineticarmory.armorsets.piece.bonus.tier_enabled").getString();
        return KineticI18n.translatable("gui.kineticarmory.armorsets.piece.bonus.tier_value", fmt(getGroupValue(group, effect))).getString();
    }

    private void selectEffect(PieceEffectEntry effect) {
        selectedEffect = effect;
        int first = findFirstConfiguredPiecesForSelectedEffect();
        selectedPieces = Math.max(first, 2);
        if (pieceInput != null) pieceInput.setTextValue(String.valueOf(selectedPieces));
        loadSelectedValue();
    }

    private void selectTier(TierOption option) {
        if (option == null) return;
        selectedPieces = option.pieces();
        if (pieceInput != null) pieceInput.setTextValue(String.valueOf(selectedPieces));
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
            if (pieceInput != null) pieceInput.setTextValue(String.valueOf(selectedPieces));
            loadSelectedValue();
            clampScrolls();
            warningMessage = null;
            KineticOverlays.toast(KineticI18n.translatable("msg.kineticarmory.common.saved"));
            return;
        }
        selectedPieces = option.pieces();
        if (pieceInput != null) pieceInput.setTextValue(String.valueOf(selectedPieces));
        saveSelectedValue();
    }

    private void loadSelectedValue() {
        if (valueInput == null || selectedEffect == null) return;
        valueInput.setEnabled(selectedEffect.valueEditable());
        if (!selectedEffect.valueEditable()) {
            valueInput.setTextValue("");
            tempValueInput = "";
            return;
        }
        ArmorDataConfig.PieceBonusGroup group = findSelectedGroup();
        double value = selectedEffect.baseValue();
        if (group != null) value = getGroupValue(group, selectedEffect);
        String text = fmt(value);
        valueInput.setTextValue(text);
        tempValueInput = text;
    }

    private String fmt(double d) {
        return d == (long)d ? String.valueOf((long)d) : String.valueOf(d);
    }

    private void closeToParent() {
        pruneEmptyGroups();
        config.preparePieceBonusData();
        navigateBack();
    }

    @Override
    protected void renderBackground(KineticGraphics g, int mx, int my, float pt) {
        KineticTheme.panel(g, PANEL_PADDING, PANEL_PADDING, width() - PANEL_PADDING * 2, height() - PANEL_PADDING * 2);
        int titleLeft = PANEL_PADDING + 12 + 80 + 2;
        int titleRight = width() - PANEL_PADDING - TEXT_GAP;
        g.scrollingTextCentered(title(), (titleLeft + titleRight) / 2, PANEL_PADDING + 10,
                Math.max(0, titleRight - titleLeft), 0xFFFFFF, true);
        KineticTheme.panelAlt(g, leftX, leftY, leftW, leftH);
        KineticTheme.panelAlt(g, rightX, rightY, rightW, rightH);
        renderEffectRows(g, mx, my);
        renderTierRows(g, mx, my);
    }

    @Override
    protected void renderForeground(KineticGraphics g, int mx, int my, float pt) {
        if (pieceInput != null) {
            g.scrollingText(KineticI18n.translatable("gui.kineticarmory.armorsets.piece.bonus.piece_input_label"), pieceInput.controlX(), pieceInput.controlY() - 11, pieceInput.controlWidth() - TEXT_GAP, 0xFFFFAA00, false);
        }
        if (valueInput != null) {
            g.scrollingText(KineticI18n.translatable("gui.kineticarmory.armorsets.piece.bonus.value_input_label"), valueInput.controlX(), valueInput.controlY() - 11, valueInput.controlWidth() - TEXT_GAP, 0xFFFFAA00, false);
        }
        g.scrollingText(KineticI18n.translatable("gui.kineticarmory.armorsets.piece.bonus.effect_list_title"), leftX, leftY - 13, leftW - TEXT_GAP, 0xFFFFAA00, false);
        g.scrollingText(KineticI18n.translatable("gui.kineticarmory.armorsets.piece.bonus.tier_list_title"), rightX, rightY - 13, rightW - TEXT_GAP, 0xFFFFAA00, false);
        if (warningMessage != null) {
            // Warnings share the input-caption row; retain their center without crossing the value caption.
            int warningLeft = valueInput == null ? PANEL_PADDING + TEXT_GAP
                    : valueInput.controlX() + valueInput.controlWidth() + TEXT_GAP;
            int warningWidth = Math.max(0, 2 * Math.min(width() / 2 - warningLeft,
                    width() - PANEL_PADDING - TEXT_GAP - width() / 2));
            g.scrollingTextCentered(warningMessage, width() / 2, PANEL_PADDING + 24, warningWidth, 0xFFFF5555, true);
        }
    }

    private void renderEffectRows(KineticGraphics g, int mx, int my) {
        int visible = Math.max(1, leftH / ROW_H);
        effectScroll.update(effects.size(), visible);
        if (effects.isEmpty()) {
            g.scrollingTextCentered(KineticI18n.translatable("gui.kineticarmory.armorsets.piece.bonus.empty_effects"), leftX + leftW / 2, leftY + 16, leftW - 2 * TEXT_GAP, 0xFFAAAAAA, true);
            return;
        }
        int first = effectScroll.smoothIndexOffset();
        int shift = effectScroll.visualShift(ROW_H);
        int count = Math.min(visible + 1, effects.size() - first);
        g.scissor(leftX, leftY, leftX + leftW - 10, leftY + leftH);
        for (int i = 0; i < count; i++) {
            int index = first + i;
            PieceEffectEntry effect = effects.get(index);
            int y = leftY + i * ROW_H - shift;
            boolean hover = isInside(mx, my, leftX, y, leftW - 10, ROW_H);
            boolean selected = effect == selectedEffect;
            KineticTheme.stateSurface(
                    g,
                    leftX + 1,
                    y + 1,
                    leftW - 11,
                    ROW_H - 2,
                    KineticTheme.Surface.PANEL_ALT,
                    selected,
                    hover,
                    false
            );
            String text = cleanDisplayText(effect.text());
            int textX = leftX + 6;
            int textWidth = leftW - 22;
            if (effect.effect() != null) {
                g.effectIcon(effect.effect(), textX, y + 4, 16);
                textX += 20;
                textWidth -= 20;
            }
            drawTrimmedText(g, text, textX, y + 5, textWidth, 0xFFFFFFFF);
            String summary = buildEffectSummary(effect);
            if (!summary.isEmpty()) drawTrimmedText(g, summary, textX, y + 16, textWidth, 0xFF55FF55);
        }
        g.endScissor();
        effectScroll.render(
                g, mx, my,
                leftX + leftW - SCROLL_W - 2,
                leftY + 2,
                SCROLL_W,
                leftH - 4,
                18
        );
    }

    private void renderTierRows(KineticGraphics g, int mx, int my) {
        List<TierOption> tiers = buildTierOptions();
        int visible = Math.max(1, rightH / ROW_H);
        tierScroll.update(tiers.size(), visible);
        if (tiers.isEmpty()) {
            g.scrollingTextCentered(KineticI18n.translatable("gui.kineticarmory.armorsets.piece.bonus.empty_tiers"), rightX + rightW / 2, rightY + 16, rightW - 2 * TEXT_GAP, 0xFFAAAAAA, true);
            return;
        }
        int first = tierScroll.smoothIndexOffset();
        int shift = tierScroll.visualShift(ROW_H);
        int count = Math.min(visible + 1, tiers.size() - first);
        g.scissor(rightX, rightY, rightX + rightW - 10, rightY + rightH);
        for (int i = 0; i < count; i++) {
            int index = first + i;
            TierOption option = tiers.get(index);
            ArmorDataConfig.PieceBonusGroup group = option.group();
            int y = rightY + i * ROW_H - shift;
            boolean hover = isInside(mx, my, rightX, y, rightW - 10, ROW_H);
            boolean rowSelected = option.pieces() == selectedPieces;
            boolean enabled = selectedEffect != null && containsEffect(group, selectedEffect);
            KineticTheme.stateSurface(
                    g,
                    rightX + 1,
                    y + 1,
                    rightW - 11,
                    ROW_H - 2,
                    KineticTheme.Surface.PANEL_ALT,
                    rowSelected,
                    hover,
                    false
            );
            if (enabled) {
                KineticTheme.indicatorOutline(
                        g,
                        rightX + 1,
                        y + 1,
                        rightW - 11,
                        ROW_H - 2,
                        KineticTheme.Indicator.SUCCESS
                );
            }
            g.text(enabled ? "[x]" : "[ ]", rightX + 6, y + 9, enabled ? 0xFF55FF55 : 0xFFAAAAAA, false);
            Component label = KineticI18n.translatable("gui.kineticarmory.armorsets.piece.bonus.tier_row", option.pieces());
            int labelX = rightX + 36;
            int valueRight = rightX + rightW - 18;
            int valueWidth = Math.min(TIER_VALUE_W, Math.max(0, valueRight - labelX - TEXT_GAP));
            int labelRight = selectedEffect == null ? valueRight : valueRight - valueWidth;
            g.scrollingText(label, labelX, y + 9, Math.max(0, labelRight - labelX - TEXT_GAP), 0xFFFFFFFF, false);
            if (selectedEffect != null) {
                String valueText = getTierValueText(group, selectedEffect);
                g.scrollingTextRight(Component.literal(valueText), valueRight, y + 9, valueWidth, enabled ? 0xFFFFFF55 : 0xFFAAAAAA, false);
            }
        }
        g.endScissor();
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

    private void drawTrimmedText(KineticGraphics g, String text, int x, int y, int maxWidth, int color) {
        g.scrollingText(Component.literal(text == null ? "" : text), x, y, Math.max(0, maxWidth), color, false);
    }

    private boolean isInside(double mx, double my, int x, int y, int w, int h) {
        return KineticTheme.hovering(mx, my, x, y, w, h);
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
    protected boolean onKeyPress(KeyInput input) {
        int keyCode = input.keyCode(), scanCode = input.scanCode(), modifiers = input.modifiers();
        if (KineticKeyBindings.matchesKeyCode(KineticKeyBindings.Key.ENTER, keyCode)
                || KineticKeyBindings.matchesKeyCode(KineticKeyBindings.Key.KP_ENTER, keyCode)) {
            if (pieceInput != null && isFocused(pieceInput)) {
                saveTierForSelectedEffect();
                return true;
            }
            if (valueInput != null && isFocused(valueInput)) {
                saveSelectedValue();
                return true;
            }
        }
        return false;
    }

    @Override
    protected boolean onMouseClick(MouseInput input) {
        double mx = input.x(), my = input.y(); int btn = input.rawButton();
        if (tryStartScrollDrag(mx, my, input.button(), leftX + leftW - SCROLL_W - 2, leftY + 2, leftH - 4, true)) return true;
        if (tryStartScrollDrag(mx, my, input.button(), rightX + rightW - SCROLL_W - 2, rightY + 2, rightH - 4, false)) return true;

        if (isInside(mx, my, leftX, leftY, leftW - 10, leftH)) {
            int row = effectScroll.smoothIndexOffset()
                    + (int) ((my - leftY + effectScroll.visualShift(ROW_H)) / ROW_H);
            if (row >= 0 && row < effects.size()) {
                selectEffect(effects.get(row));
                return true;
            }
        }

        if (isInside(mx, my, rightX, rightY, rightW - 10, rightH)) {
            List<TierOption> tiers = buildTierOptions();
            int row = tierScroll.smoothIndexOffset()
                    + (int) ((my - rightY + tierScroll.visualShift(ROW_H)) / ROW_H);
            if (row >= 0 && row < tiers.size()) {
                TierOption option = tiers.get(row);
                selectTier(option);
                if (KineticMouseButtons.isSecondary(btn)) toggleSelectedEffectInTier(option);
                return true;
            }
        }

        return false;
    }

    private boolean tryStartScrollDrag(double mx, double my, MouseButton button, int x, int y, int h, boolean effectList) {
        int visible = Math.max(1, (effectList ? leftH : rightH) / ROW_H);
        int total = effectList ? effects.size() : buildTierOptions().size();
        KineticScrollController controller = effectList ? effectScroll : tierScroll;
        controller.update(total, visible);
        return controller.beginDrag(
                mx, my, button,
                x, y,
                SCROLL_W, h,
                18, 4
        );
    }

    @Override
    protected boolean onMouseDrag(MouseDragInput input) {
        double mx = input.x(), my = input.y(); int btn = input.rawButton(); double dx = input.deltaX(), dy = input.deltaY();
        if (effectScroll.drag(my, leftY + 2, leftH - 4, 18)) return true;
        if (tierScroll.drag(my, rightY + 2, rightH - 4, 18)) return true;
        return false;
    }

    @Override
    protected boolean onMouseRelease(MouseInput input) {
        double mx = input.x(), my = input.y(); int btn = input.rawButton();
        boolean handled = effectScroll.release(input.button());
        handled = tierScroll.release(input.button()) || handled;
        return handled || false;
    }

    @Override
    protected boolean onMouseScroll(ScrollInput input) {
        double mx = input.x(), my = input.y(), delta = input.deltaY();
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

        return false;
    }

    private record PieceEffectEntry(String key, String text, double baseValue, boolean valueEditable, MobEffect effect) {
        PieceEffectEntry(String key, String text, double baseValue, boolean valueEditable) {
            this(key, text, baseValue, valueEditable, null);
        }

        static PieceEffectEntry potion(String key, String text, double baseValue, boolean valueEditable, String effectId) {
            var id = effectId == null ? null : KineticResourceIds.tryParse(effectId);
            var effect = id == null ? null : KineticRegistries.mobEffects().get(id);
            return new PieceEffectEntry(key, effect == null ? text : text + " §7(" + id + ")", baseValue, valueEditable, effect);
        }
    }
    private record TierOption(int pieces, ArmorDataConfig.PieceBonusGroup group) {}
}
