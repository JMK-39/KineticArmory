package dev.xyat.kineticarmory.armorsets.client.gui.editor;

import dev.xyat.kineticcore.api.client.gui.overlay.KineticOverlays;
import dev.xyat.kineticcore.api.client.gui.page.KineticPage;
import dev.xyat.kineticcore.api.client.gui.render.KineticGraphics;
import dev.xyat.kineticcore.api.client.gui.theme.KineticTheme;
import dev.xyat.kineticcore.api.client.gui.ui.KineticUi;
import dev.xyat.kineticcore.api.client.gui.widget.*;
import dev.xyat.kineticcore.api.client.gui.widget.list.*;

import dev.xyat.kineticarmory.armorsets.data.ArmorDataConfig;
import dev.xyat.kineticarmory.armorsets.data.ArmorTipGenerator;
import dev.xyat.kineticarmory.armorsets.predicate.client.ConditionListPage;
import dev.xyat.kineticcore.api.client.search.KineticSearch;
import dev.xyat.kineticcore.api.runtime.KineticClientRuntime;
import dev.xyat.kineticcore.api.text.KineticI18n;
import net.minecraft.network.chat.Component;

public class PotionImmunityEditor extends KineticPage {
    private final ArmorDataConfig config;
    private final ArmorDataConfig.EffectImmunityData data; private boolean isNew;
    private KineticAutoCompleteField idInput; private String oldTip = null;
    private String tempId = null;

    public PotionImmunityEditor(ArmorDataConfig c, ArmorDataConfig.EffectImmunityData d) {
        super(KineticI18n.translatable("gui.kineticarmory.armorsets.editor.effect_immunity.title"));
        config = c; isNew = (d == null); data = isNew ? new ArmorDataConfig.EffectImmunityData() : d;
        if (!isNew) oldTip = ArmorTipGenerator.genEffImmTip(data);
    }

    @Override
    protected void onTick() {
        if (idInput != null) tempId = idInput.textValue();
    }

    @Override protected void build(KineticUi ui) {
        int cx = width() / 2; int cy = height() / 2 - 50;
        idInput = ui().autoComplete(cx - 100, cy - 30, 200, KineticSearch::potionDictionary).placeholder(KineticI18n.translatable("gui.kineticarmory.armorsets.input.id")).firstShownTextAsDefault().build();
        idInput.setTextValue(tempId != null ? tempId : (isNew ? "" : (data.effectId != null ? data.effectId : "")));

        ui().button(cx - 100, cy - 5, 200).text(KineticI18n.translatable("gui.kineticarmory.armorsets.editor.conditions", data.conditions.size())).onClick(b -> {
            syncToData();
            openChild(new ConditionListPage(data));
        }).build();

        ui().button(cx - 60, cy + 25, 55).text(KineticI18n.translatable("gui.kineticarmory.armorsets.save")).onClick(b -> {
            syncToData();
            if (data.effectId.isEmpty()) { KineticOverlays.toast(KineticI18n.translatable("msg.kineticarmory.armorsets.empty_field")); return; }
            if (oldTip != null) config.tips.remove(oldTip);
            if (isNew) {
                config.effectImmunities.add(data);
                isNew = false;
            }
            String newTip = ArmorTipGenerator.genEffImmTip(data);
            config.tips.add(newTip);
            oldTip = newTip;
        }).build();
        ui().button(cx + 5, cy + 25, 55).text(KineticI18n.translatable("gui.kineticarmory.armorsets.back")).onClick(b -> { navigateBack(); }).build();
    }

    private void syncToData() {
        if (idInput != null) {
            tempId = idInput.textValue();
        }

        data.effectId =
                (tempId == null ? "" : tempId.trim());
    }

    @Override protected void renderBackground(KineticGraphics g, int mx, int my, float pt) {
        int cx = width() / 2; int cy = height() / 2 - 50; KineticTheme.panel(g, cx - 120, cy - 60, 240, 115);
        g.centeredText(title(), cx, cy - 50, 0xFFFFFF, true);
    }

}
