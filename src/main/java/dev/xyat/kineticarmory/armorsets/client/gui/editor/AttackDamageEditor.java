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
import dev.xyat.kineticcore.api.text.KineticI18n;
import java.util.ArrayList;

public class AttackDamageEditor extends KineticPage {
    private final ArmorDataConfig config;
    private final ArmorDataConfig.AttackDamageMultiplierData data; private boolean isNew;
    private KineticNumberAutoCompleteField valInput;
    private String oldTip = null;
    private String tempVal = null;

    public AttackDamageEditor(ArmorDataConfig c, ArmorDataConfig.AttackDamageMultiplierData d) {
        super(KineticI18n.translatable("gui.kineticarmory.armorsets.editor.attack_damage.title"));
        config = c; isNew = (d == null); data = isNew ? new ArmorDataConfig.AttackDamageMultiplierData() : d;
        if (!isNew) oldTip = ArmorTipGenerator.genAtkDmgTip(data);
    }

    @Override
    protected void onTick() {
        if (valInput != null) tempVal = valInput.textValue();
    }

    @Override protected void build(KineticUi ui) {
        int cx = width() / 2; int cy = height() / 2 - 50;
        valInput = ui().numberAutoComplete(cx - 100, cy - 30, 200, NumberType.DECIMAL, ArrayList::new).allowNegative(true).firstShownTextAsDefault().build();
        valInput.setPlaceholder(KineticI18n.translatable("gui.kineticarmory.armorsets.input.attack_multiplier"));
        valInput.setTextValue(tempVal != null ? tempVal : (isNew ? "" : String.valueOf(data.multiplier)));

        ui().button(cx - 100, cy - 5, 200).text(KineticI18n.translatable("gui.kineticarmory.armorsets.editor.conditions", data.conditions.size())).onClick(b -> {
            if (syncToData()) return;
            openChild(new ConditionListPage(data));
        }).build();

        ui().button(cx - 60, cy + 25, 55).text(KineticI18n.translatable("gui.kineticarmory.armorsets.save")).onClick(b -> {
            if (syncToData()) return;
            if (valInput.textValue().trim().isEmpty()) { KineticOverlays.toast(KineticI18n.translatable("msg.kineticarmory.armorsets.empty_field")); return; }
            if (oldTip != null) config.tips.remove(oldTip);
            if (isNew) {
                config.attackDamageMultipliers.add(data);
                isNew = false;
            }
            String newTip = ArmorTipGenerator.genAtkDmgTip(data);
            config.tips.add(newTip);
            oldTip = newTip;
        }).build();
        ui().button(cx + 5, cy + 25, 55).text(KineticI18n.translatable("gui.kineticarmory.armorsets.back")).onClick(b -> { navigateBack(); }).build();
    }

    private boolean syncToData() {
        if (valInput != null) tempVal = valInput.textValue();

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
        int cx = width() / 2; int cy = height() / 2 - 50; KineticTheme.panel(g, cx - 120, cy - 60, 240, 115);
        g.centeredText(title(), cx, cy - 50, 0xFFFFFF, true);
    }

}
