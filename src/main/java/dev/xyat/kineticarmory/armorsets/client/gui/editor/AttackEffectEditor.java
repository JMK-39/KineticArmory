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
import dev.xyat.kineticcore.api.runtime.KineticClientRuntime;
import dev.xyat.kineticcore.api.text.KineticI18n;
import net.minecraft.network.chat.Component;
import java.util.ArrayList;

public class AttackEffectEditor extends KineticPage {
    private final ArmorDataConfig config; private final ArmorDataConfig.AttackEffectData data; private boolean isNew;
    private KineticAutoCompleteField idInput;
    private KineticNumberAutoCompleteField durInput, lvlInput, chanceInput;
    private String oldTip = null;
    private String tempId = null, tempDur = null, tempLvl = null, tempChance = null;

    public AttackEffectEditor(ArmorDataConfig c, ArmorDataConfig.AttackEffectData d) {
        super(KineticI18n.translatable("gui.kineticarmory.armorsets.editor.attack.title"));
        config = c; isNew = (d == null); data = isNew ? new ArmorDataConfig.AttackEffectData() : d;
        if (!isNew) oldTip = ArmorTipGenerator.genAtkTip(data);
    }

    @Override
    protected void onTick() {
        if (idInput != null) tempId = idInput.textValue();
        if (durInput != null) tempDur = durInput.textValue();
        if (lvlInput != null) tempLvl = lvlInput.textValue();
        if (chanceInput != null) tempChance = chanceInput.textValue();
    }

    @Override protected void build(KineticUi ui) {
        int cx = width() / 2; int cy = height() / 2 - 50;
        idInput = ui().autoComplete(cx - 100, cy - 35, 200, KineticSearch::potionDictionary).placeholder(KineticI18n.translatable("gui.kineticarmory.armorsets.input.id")).firstShownTextAsDefault().build();
        idInput.setTextValue(tempId != null ? tempId : (isNew ? "" : (data.effectId != null ? data.effectId : "")));

        int w = 60; int gap = 10; int startX = cx - 100;
        durInput = ui().numberAutoComplete(startX, cy - 10, w, NumberType.DECIMAL, ArrayList::new).allowNegative(true).firstShownTextAsDefault().build();
        durInput.setPlaceholder(KineticI18n.translatable("gui.kineticarmory.armorsets.input.duration"));
        durInput.setTextValue(tempDur != null ? tempDur : (isNew ? "" : String.valueOf(data.duration)));

        lvlInput = ui().numberAutoComplete(startX + w + gap, cy - 10, w, NumberType.INT, ArrayList::new).allowNegative(true).firstShownTextAsDefault().build();
        lvlInput.setPlaceholder(KineticI18n.translatable("gui.kineticarmory.armorsets.input.level"));
        lvlInput.setTextValue(tempLvl != null ? tempLvl : (isNew ? "" : String.valueOf(data.amplifier)));

        chanceInput = ui().numberAutoComplete(startX + (w + gap) * 2, cy - 10, w, NumberType.DECIMAL, ArrayList::new).allowNegative(true).firstShownTextAsDefault().build();
        chanceInput.setPlaceholder(KineticI18n.translatable("gui.kineticarmory.armorsets.input.chance"));
        chanceInput.setTextValue(tempChance != null ? tempChance : (isNew ? "" : String.valueOf(data.chance)));

        ui().button(cx - 100, cy + 15, 200).text(KineticI18n.translatable("gui.kineticarmory.armorsets.editor.conditions", data.conditions.size())).onClick(b -> {
            if (syncToData()) return;
            openChild(new ConditionListPage(data));
        }).build();

        ui().button(cx - 60, cy + 45, 55).text(KineticI18n.translatable("gui.kineticarmory.armorsets.save")).onClick(b -> {
            if (syncToData()) return;
            if (data.effectId.isEmpty()) { KineticOverlays.toast(KineticI18n.translatable("msg.kineticarmory.armorsets.empty_field")); return; }
            if (oldTip != null) config.tips.remove(oldTip);
            if (isNew) {
                config.attackEffects.add(data);
                isNew = false;
            }
            String newTip = ArmorTipGenerator.genAtkTip(data);
            config.tips.add(newTip);
            oldTip = newTip;
        }).build();
        ui().button(cx + 5, cy + 45, 55).text(KineticI18n.translatable("gui.kineticarmory.armorsets.back")).onClick(b -> { navigateBack(); }).build();
    }

    private boolean syncToData() {
        if (idInput != null) tempId = idInput.textValue();
        if (durInput != null) tempDur = durInput.textValue();
        if (lvlInput != null) tempLvl = lvlInput.textValue();
        if (chanceInput != null) tempChance = chanceInput.textValue();

        data.effectId =
                (tempId == null ? "" : tempId.trim());

        Double duration = durInput == null ? null : durInput.getDoubleValue();
        Integer amplifier = lvlInput == null ? null : lvlInput.getIntValue();
        Double chance = chanceInput == null ? null : chanceInput.getDoubleValue();

        if (duration == null || amplifier == null || chance == null) {
            KineticOverlays.toast(
                    KineticI18n.translatable("msg.kineticarmory.common.invalid_number")
            );
            return true;
        }

        data.duration = duration;
        data.amplifier = amplifier;
        data.chance = chance;
        return false;
    }

    @Override protected void renderBackground(KineticGraphics g, int mx, int my, float pt) {
        int cx = width() / 2; int cy = height() / 2 - 50;
        KineticTheme.panel(g, cx - 120, cy - 70, 240, 150);
        g.centeredText(title(), cx, cy - 60, 0xFFFFFF, true);
    }

}
