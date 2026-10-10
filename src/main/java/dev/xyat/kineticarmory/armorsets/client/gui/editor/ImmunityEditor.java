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

public class ImmunityEditor extends KineticPage {
    // The panel width bounds the centered title with 4 px padding on either side.
    private static final int PANEL_WIDTH = 240;
    private final ArmorDataConfig config;
    private final ArmorDataConfig.DamageImmunityData data; private boolean isNew;
    private KineticAutoCompleteField idInput;
    private KineticNumberAutoCompleteField valInput;
    private String oldTip = null;
    private String tempId = null, tempVal = null;

    public ImmunityEditor(ArmorDataConfig c, ArmorDataConfig.DamageImmunityData d) {
        super(KineticI18n.translatable("gui.kineticarmory.armorsets.editor.immunity.title"));
        config = c; isNew = (d == null); data = isNew ? new ArmorDataConfig.DamageImmunityData() : d;
        if (!isNew) oldTip = ArmorTipGenerator.genImmTip(data);
    }

    @Override
    protected void onTick() {
        if (idInput != null) tempId = idInput.textValue();
        if (valInput != null) tempVal = valInput.textValue();
    }

    @Override protected void build(KineticUi ui) {
        EditorPanel p = panel(); int cx = p.centerX;
        idInput = ui().autoComplete(cx - 100, p.rowY(0), 200, KineticSearch::damageDictionary).placeholder(KineticI18n.translatable("gui.kineticarmory.armorsets.input.id_or_tag")).firstShownTextAsDefault().build();
        idInput.setTextValue(tempId != null ? tempId : (isNew ? "" : (data.damageType != null ? data.damageType : "")));

        valInput = ui().numberAutoComplete(cx - 100, p.rowY(1), 200, NumberType.DECIMAL, ArrayList::new).allowNegative(true).firstShownTextAsDefault().build();
        valInput.setPlaceholder(KineticI18n.translatable("gui.kineticarmory.armorsets.input.multiplier"));
        valInput.setTextValue(tempVal != null ? tempVal : (isNew ? "" : String.valueOf(data.multiplier)));

        ui().button(cx - 100, p.rowY(2), 200).text(KineticI18n.translatable("gui.kineticarmory.armorsets.editor.conditions", data.conditions.size())).onClick(b -> {
            if (syncToData()) return;
            openChild(new ConditionListPage(data));
        }).build();

        ui().button(cx - 60, p.buttonY(), 55).text(KineticI18n.translatable("gui.kineticarmory.armorsets.save")).onClick(b -> {
            if (syncToData()) return;
            if (data.damageType.isEmpty()) { KineticOverlays.toast(KineticI18n.translatable("msg.kineticarmory.armorsets.empty_field")); return; }
            if (oldTip != null) config.tips.remove(oldTip);
            if (isNew) {
                config.damageImmunities.add(data);
                isNew = false;
            }
            String newTip = ArmorTipGenerator.genImmTip(data);
            config.tips.add(newTip);
            oldTip = newTip;
        }).build();
        ui().button(p.backX(), p.backY(), 55).text(KineticI18n.translatable("gui.kineticarmory.armorsets.back")).onClick(b -> { navigateBack(); }).build();
    }

    private boolean syncToData() {
        if (idInput != null) tempId = idInput.textValue();
        if (valInput != null) tempVal = valInput.textValue();

        data.damageType =
                (tempId == null ? "" : tempId.trim());

        Double multiplier = valInput == null ? null : valInput.getDoubleValue();

        if (multiplier == null) {
            KineticOverlays.toast(
                    KineticI18n.translatable("msg.kineticarmory.common.invalid_number")
            );
            return true;
        }

        data.multiplier = multiplier;
        return false;
    }

    @Override protected void renderBackground(KineticGraphics g, int mx, int my, float pt) {
        panel().render(g, title());
    }

    private EditorPanel panel() {
        return new EditorPanel(width(), height(), PANEL_WIDTH, 3);
    }
}
