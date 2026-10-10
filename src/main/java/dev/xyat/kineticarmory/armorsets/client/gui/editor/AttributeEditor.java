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
import java.util.UUID;

public class AttributeEditor extends KineticPage {
    // The panel width bounds the centered title with 4 px padding on either side.
    private static final int PANEL_WIDTH = 240;
    private final ArmorDataConfig config;
    private final ArmorDataConfig.AttributeModifierData data; private boolean isNew;
    private KineticAutoCompleteField idInput;
    private KineticNumberAutoCompleteField amountInput;
    private String currentOp;
    private String oldTip = null;
    private String tempId = null, tempAmount = null;

    public AttributeEditor(ArmorDataConfig c, ArmorDataConfig.AttributeModifierData d) {
        super(KineticI18n.translatable("gui.kineticarmory.armorsets.editor.attr.title"));
        config = c; isNew = (d == null); data = isNew ? new ArmorDataConfig.AttributeModifierData() : d;
        if (isNew && data.uuid == null) { data.uuid = UUID.randomUUID().toString(); data.operation = "ADDITION"; }
        if (!isNew) { oldTip = ArmorTipGenerator.genAttrTip(data); }
        currentOp = data.operation;
        if ("MULTIPLY_BASE".equals(currentOp)) currentOp = "MULTIPLY_TOTAL";
    }

    @Override
    protected void onTick() {
        if (idInput != null) tempId = idInput.textValue();
        if (amountInput != null) tempAmount = amountInput.textValue();
    }

    @Override protected void build(KineticUi ui) {
        EditorPanel p = panel(); int cx = p.centerX;
        idInput = ui().autoComplete(cx - 100, p.rowY(0), 200, KineticSearch::attributeDictionary).placeholder(KineticI18n.translatable("gui.kineticarmory.armorsets.input.id")).firstShownTextAsDefault().build();
        idInput.setTextValue(tempId != null ? tempId : (isNew ? "" : (data.attribute != null ? data.attribute : "")));

        amountInput = ui().numberAutoComplete(cx - 100, p.rowY(1), 95, NumberType.DECIMAL, ArrayList::new).allowNegative(true).firstShownTextAsDefault().build();
        amountInput.setPlaceholder(KineticI18n.translatable("gui.kineticarmory.armorsets.input.amount"));
        amountInput.setTextValue(tempAmount != null ? tempAmount : (isNew ? "" : String.valueOf(data.amount)));

        ui().button(cx + 5, p.rowY(1), 95).text(KineticI18n.translatable("gui.kineticarmory.armorsets.op." + currentOp.toLowerCase())).onClick(b -> {
            currentOp = currentOp.equals("ADDITION") ? "MULTIPLY_TOTAL" : (currentOp.equals("MULTIPLY_TOTAL") ? "SET" : "ADDITION");
            b.setText(KineticI18n.translatable("gui.kineticarmory.armorsets.op." + currentOp.toLowerCase()));
            data.operation = currentOp;
        }).build();

        ui().button(cx - 100, p.rowY(2), 200).text(KineticI18n.translatable("gui.kineticarmory.armorsets.editor.conditions", data.conditions.size())).onClick(b -> {
            if (syncToData()) return;
            openChild(new ConditionListPage(data));
        }).build();

        ui().button(cx - 60, p.buttonY(), 55).text(KineticI18n.translatable("gui.kineticarmory.armorsets.save")).onClick(b -> {
            if (syncToData()) return;
            if (data.attribute.isEmpty()) { KineticOverlays.toast(KineticI18n.translatable("msg.kineticarmory.armorsets.empty_field")); return; }
            if (oldTip != null) config.tips.remove(oldTip);
            if (isNew) {
                config.attributes.add(data);
                isNew = false;
            }
            String newTip = ArmorTipGenerator.genAttrTip(data);
            config.tips.add(newTip);
            oldTip = newTip;
        }).build();

        ui().button(p.backX(), p.backY(), 55).text(KineticI18n.translatable("gui.kineticarmory.armorsets.back")).onClick(b -> { navigateBack(); }).build();
    }

    private boolean syncToData() {
        if (idInput != null) tempId = idInput.textValue();
        if (amountInput != null) tempAmount = amountInput.textValue();

        data.attribute =
                (tempId == null ? "" : tempId.trim());
        data.operation = currentOp;

        Double amount = amountInput == null ? null : amountInput.getDoubleValue();

        if (amount == null) {
            KineticOverlays.toast(
                    KineticI18n.translatable("msg.kineticarmory.common.invalid_number")
            );
            return true;
        }

        data.amount = amount;
        return false;
    }

    @Override protected void renderBackground(KineticGraphics g, int mx, int my, float pt) {
        panel().render(g, title());
    }

    private EditorPanel panel() {
        return new EditorPanel(width(), height(), PANEL_WIDTH, 3);
    }
}
