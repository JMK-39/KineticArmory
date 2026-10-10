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

public class PotionEditor extends KineticPage {
    // The panel width bounds the centered title with 4 px padding on either side.
    private static final int PANEL_WIDTH = 240;
    private final ArmorDataConfig config;
    private final ArmorDataConfig.PotionEffectData data; private boolean isNew;
    private KineticAutoCompleteField idInput;
    private KineticNumberAutoCompleteField lvlInput, timeInput;
    private String oldTip = null;
    private String tempId = null, tempLvl = null, tempTime = null;

    public PotionEditor(ArmorDataConfig c, ArmorDataConfig.PotionEffectData d) {
        super(KineticI18n.translatable("gui.kineticarmory.armorsets.editor.potion.title"));
        config = c; isNew = (d == null); data = isNew ? new ArmorDataConfig.PotionEffectData() : d;
        if (!isNew) oldTip = ArmorTipGenerator.genPotTip(data);
    }

    @Override
    protected void onTick() {
        if (idInput != null) tempId = idInput.textValue();
        if (lvlInput != null) tempLvl = lvlInput.textValue();
        if (timeInput != null) tempTime = timeInput.textValue();
    }

    @Override protected void build(KineticUi ui) {
        EditorPanel p = panel(); int cx = p.centerX;
        idInput = ui().autoComplete(cx - 100, p.rowY(0), 200, KineticSearch::potionDictionary).placeholder(KineticI18n.translatable("gui.kineticarmory.armorsets.input.id")).firstShownTextAsDefault().build();
        idInput.setTextValue(tempId != null ? tempId : (isNew ? "" : (data.effectId != null ? data.effectId : "")));

        lvlInput = ui().numberAutoComplete(cx - 100, p.rowY(1), 95, NumberType.INT, ArrayList::new).allowNegative(true).firstShownTextAsDefault().build();
        lvlInput.setPlaceholder(KineticI18n.translatable("gui.kineticarmory.armorsets.input.level"));
        lvlInput.setTextValue(tempLvl != null ? tempLvl : (isNew ? "" : String.valueOf(data.amplifier)));

        timeInput = ui().numberAutoComplete(cx + 5, p.rowY(1), 95, NumberType.INT, ArrayList::new).allowNegative(true).firstShownTextAsDefault().build();
        timeInput.setPlaceholder(KineticI18n.translatable("gui.kineticarmory.armorsets.input.duration"));
        timeInput.setTextValue(tempTime != null ? tempTime : (isNew ? "" : String.valueOf(data.duration)));

        ui().button(cx - 100, p.rowY(2), 200).text(KineticI18n.translatable("gui.kineticarmory.armorsets.editor.conditions", data.conditions.size())).onClick(b -> {
            if (syncToData()) return;
            openChild(new ConditionListPage(data));
        }).build();

        ui().button(cx - 60, p.buttonY(), 55).text(KineticI18n.translatable("gui.kineticarmory.armorsets.save")).onClick(b -> {
            if (syncToData()) return;
            if (data.effectId.isEmpty()) { KineticOverlays.toast(KineticI18n.translatable("msg.kineticarmory.armorsets.empty_field")); return; }
            if (oldTip != null) config.tips.remove(oldTip);
            if (isNew) {
                config.potionEffects.add(data);
                isNew = false;
            }
            String newTip = ArmorTipGenerator.genPotTip(data);
            config.tips.add(newTip);
            oldTip = newTip;
        }).build();

        ui().button(p.backX(), p.backY(), 55).text(KineticI18n.translatable("gui.kineticarmory.armorsets.back")).onClick(b -> { navigateBack(); }).build();
    }

    private boolean syncToData() {
        if (idInput != null) tempId = idInput.textValue();
        if (lvlInput != null) tempLvl = lvlInput.textValue();
        if (timeInput != null) tempTime = timeInput.textValue();

        data.effectId =
                (tempId == null ? "" : tempId.trim());

        Integer amplifier = lvlInput == null ? null : lvlInput.getIntValue();
        Integer duration = timeInput == null ? null : timeInput.getIntValue();

        if (amplifier == null || duration == null) {
            KineticOverlays.toast(
                    KineticI18n.translatable("msg.kineticarmory.common.invalid_number")
            );
            return true;
        }

        data.amplifier = amplifier;
        data.duration = duration;
        return false;
    }

    @Override protected void renderBackground(KineticGraphics g, int mx, int my, float pt) {
        panel().render(g, title());
    }

    private EditorPanel panel() {
        return new EditorPanel(width(), height(), PANEL_WIDTH, 3);
    }
}
