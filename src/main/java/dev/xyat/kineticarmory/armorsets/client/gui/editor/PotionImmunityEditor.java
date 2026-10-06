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
import dev.xyat.kineticcore.api.text.KineticI18n;

public class PotionImmunityEditor extends KineticPage {
    // The panel width bounds the centered title with 4 px padding on either side.
    private static final int PANEL_WIDTH = 240;
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
        EditorPanel p = panel(); int cx = p.centerX;
        idInput = ui().autoComplete(cx - 100, p.rowY(0), 200, KineticSearch::potionDictionary).placeholder(KineticI18n.translatable("gui.kineticarmory.armorsets.input.id")).firstShownTextAsDefault().build();
        idInput.setTextValue(tempId != null ? tempId : (isNew ? "" : (data.effectId != null ? data.effectId : "")));

        ui().button(cx - 100, p.rowY(1), 200).text(KineticI18n.translatable("gui.kineticarmory.armorsets.editor.conditions", data.conditions.size())).onClick(b -> {
            syncToData();
            openChild(new ConditionListPage(data));
        }).build();

        ui().button(cx - 60, p.buttonY(), 55).text(KineticI18n.translatable("gui.kineticarmory.armorsets.save")).onClick(b -> {
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
        ui().button(cx + 5, p.buttonY(), 55).text(KineticI18n.translatable("gui.kineticarmory.armorsets.back")).onClick(b -> { navigateBack(); }).build();
    }

    private void syncToData() {
        if (idInput != null) {
            tempId = idInput.textValue();
        }

        data.effectId =
                (tempId == null ? "" : tempId.trim());
    }

    @Override protected void renderBackground(KineticGraphics g, int mx, int my, float pt) {
        panel().render(g, title());
    }

    private EditorPanel panel() {
        return new EditorPanel(width(), height(), PANEL_WIDTH, 2);
    }
}
