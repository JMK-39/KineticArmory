package dev.xyat.kineticarmory.armorsets.client.gui.editor;

import dev.xyat.kineticcore.api.client.gui.overlay.KineticOverlays;
import dev.xyat.kineticcore.api.client.gui.page.KineticPage;
import dev.xyat.kineticcore.api.client.gui.render.KineticGraphics;
import dev.xyat.kineticcore.api.client.gui.theme.KineticTheme;
import dev.xyat.kineticcore.api.client.gui.ui.KineticUi;
import dev.xyat.kineticcore.api.client.gui.ui.NumberType;
import dev.xyat.kineticcore.api.client.gui.widget.*;
import dev.xyat.kineticcore.api.client.gui.widget.list.*;

import dev.xyat.kineticarmory.armorsets.data.ArmorDataConfig;
import dev.xyat.kineticarmory.armorsets.data.ArmorTipGenerator;
import dev.xyat.kineticarmory.armorsets.predicate.client.ConditionListPage;
import dev.xyat.kineticcore.api.client.search.KineticSearch;
import dev.xyat.kineticcore.api.text.KineticI18n;
import java.util.ArrayList;

public class DamageConversionEditor extends KineticPage {
    // The panel width bounds the centered title with 4 px padding on either side.
    private static final int PANEL_WIDTH = 280;
    private final ArmorDataConfig config; private final ArmorDataConfig.DamageConversionData data; private boolean isNew;
    private KineticAutoCompleteField srcInput, tgtInput;
    private KineticNumberAutoCompleteField ratioInput, chanceInput;
    private String oldTip = null;
    private String tempSrc = null, tempTgt = null, tempRatio = null, tempChance = null;

    public DamageConversionEditor(ArmorDataConfig c, ArmorDataConfig.DamageConversionData d) {
        super(KineticI18n.translatable("gui.kineticarmory.armorsets.editor.dmg_convert.title"));
        config = c; isNew = (d == null); data = isNew ? new ArmorDataConfig.DamageConversionData() : d;
        if (!isNew) oldTip = ArmorTipGenerator.genConvTip(data);
    }

    @Override
    protected void onTick() {
        if (srcInput != null) tempSrc = srcInput.textValue();
        if (tgtInput != null) tempTgt = tgtInput.textValue();
        if (ratioInput != null) tempRatio = ratioInput.textValue();
        if (chanceInput != null) tempChance = chanceInput.textValue();
    }

    @Override protected void build(KineticUi ui) {
        int cx = width() / 2; int cy = height() / 2 - 50;
        srcInput = ui().autoComplete(cx - 125, cy - 30, 110, KineticSearch::damageDictionary).placeholder(KineticI18n.translatable("gui.kineticarmory.armorsets.input.source_type")).firstShownTextAsDefault().build();
        srcInput.setTextValue(tempSrc != null ? tempSrc : (isNew ? "" : (data.sourceType != null ? data.sourceType : "")));

        tgtInput = ui().autoComplete(cx + 15, cy - 30, 110, KineticSearch::specificDamageDictionary).placeholder(KineticI18n.translatable("gui.kineticarmory.armorsets.input.target_type")).firstShownTextAsDefault().build();
        tgtInput.setTextValue(tempTgt != null ? tempTgt : (isNew ? "" : (data.targetType != null ? data.targetType : "")));

        ratioInput = ui().numberAutoComplete(cx - 125, cy - 5, 110, NumberType.DECIMAL, ArrayList::new).allowNegative(true).firstShownTextAsDefault().build();
        ratioInput.setPlaceholder(KineticI18n.translatable("gui.kineticarmory.armorsets.input.ratio"));
        ratioInput.setTextValue(tempRatio != null ? tempRatio : (isNew ? "" : String.valueOf(data.ratio)));

        chanceInput = ui().numberAutoComplete(cx + 15, cy - 5, 110, NumberType.DECIMAL, ArrayList::new).allowNegative(true).firstShownTextAsDefault().build();
        chanceInput.setPlaceholder(KineticI18n.translatable("gui.kineticarmory.armorsets.input.chance"));
        chanceInput.setTextValue(tempChance != null ? tempChance : (isNew ? "" : String.valueOf(data.chance)));

        ui().button(cx - 100, cy + 20, 200).text(KineticI18n.translatable("gui.kineticarmory.armorsets.editor.conditions", data.conditions.size())).onClick(b -> {
            if (syncToData()) return;
            openChild(new ConditionListPage(data));
        }).build();

        ui().button(cx - 60, cy + 50, 55).text(KineticI18n.translatable("gui.kineticarmory.armorsets.save")).onClick(b -> {
            if (syncToData()) return;
            if (data.sourceType.isEmpty() || data.targetType.isEmpty()) {
                KineticOverlays.toast(KineticI18n.translatable("msg.kineticarmory.armorsets.empty_field")); return;
            }
            if (data.targetType.startsWith("#") || data.targetType.equalsIgnoreCase("all")) {
                KineticOverlays.toast(KineticI18n.translatable("msg.kineticarmory.armorsets.convert_target_error")); return;
            }

            if (oldTip != null) config.tips.remove(oldTip);
            if (isNew) {
                config.damageConversions.add(data);
                isNew = false;
            }
            String newTip = ArmorTipGenerator.genConvTip(data);
            config.tips.add(newTip);
            oldTip = newTip;
        }).build();
        ui().button(cx + 5, cy + 50, 55).text(KineticI18n.translatable("gui.kineticarmory.armorsets.back")).onClick(b -> { navigateBack(); }).build();
    }

    private boolean syncToData() {
        if (srcInput != null) tempSrc = srcInput.textValue();
        if (tgtInput != null) tempTgt = tgtInput.textValue();
        if (ratioInput != null) tempRatio = ratioInput.textValue();
        if (chanceInput != null) tempChance = chanceInput.textValue();

        data.sourceType =
                (tempSrc == null ? "" : tempSrc.trim());

        data.targetType =
                (tempTgt == null ? "" : tempTgt.trim());

        Double ratio = ratioInput == null ? null : ratioInput.getDoubleValue();
        Double chance = chanceInput == null ? null : chanceInput.getDoubleValue();

        if (ratio == null || chance == null) {
            KineticOverlays.toast(
                    KineticI18n.translatable("msg.kineticarmory.common.invalid_number")
            );
            return true;
        }

        data.ratio = ratio;
        data.chance = chance;
        return false;
    }

    @Override protected void renderBackground(KineticGraphics g, int mx, int my, float pt) {
        int cx = width() / 2; int cy = height() / 2 - 50; KineticTheme.panel(g, cx - PANEL_WIDTH / 2, cy - 60, PANEL_WIDTH, 145);
        g.scrollingTextCentered(title(), cx, cy - 50, PANEL_WIDTH - 8, 0xFFFFFF, true);
    }

}
