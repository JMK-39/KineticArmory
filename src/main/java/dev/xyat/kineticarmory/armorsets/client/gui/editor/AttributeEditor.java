package dev.xyat.kineticarmory.armorsets.client.gui.editor;

import dev.xyat.kineticarmory.util.ColorText;
import dev.xyat.kineticarmory.armorsets.data.ArmorDataConfig;
import dev.xyat.kineticarmory.armorsets.data.ArmorTipGenerator;
import dev.xyat.kineticarmory.armorsets.predicate.client.ConditionListScreen;
import dev.xyat.kineticcore.api.client.theme.GuiTheme;
import dev.xyat.kineticcore.api.client.overlay.KineticOverlays;
import dev.xyat.kineticcore.api.client.search.KineticSearch;
import dev.xyat.kineticcore.api.client.screen.KineticScreen;
import dev.xyat.kineticcore.api.runtime.KineticClientRuntime;
import dev.xyat.kineticcore.api.client.widget.input.KineticAutoComplete.AutoCompleteBox;
import dev.xyat.kineticcore.api.client.widget.input.KineticAutoComplete.NumericAutoCompleteBox;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;
import java.util.ArrayList;
import java.util.UUID;

public class AttributeEditor extends KineticScreen {
    private final KineticScreen parent; private final ArmorDataConfig config;
    private final ArmorDataConfig.AttributeModifierData data; private boolean isNew;
    private AutoCompleteBox idInput;
    private NumericAutoCompleteBox amountInput;
    private String currentOp;
    private String oldTip = null;
    private String tempId = null, tempAmount = null;

    public AttributeEditor(KineticScreen p, ArmorDataConfig c, ArmorDataConfig.AttributeModifierData d) {
        super(ColorText.translatable("gui.kineticarmory.armorsets.editor.attr.title"));
        setParentScreen(p);
        parent = p; config = c; isNew = (d == null); data = isNew ? new ArmorDataConfig.AttributeModifierData() : d;
        if (isNew && data.uuid == null) { data.uuid = UUID.randomUUID().toString(); data.operation = "ADDITION"; }
        if (!isNew) { oldTip = ArmorTipGenerator.genAttrTip(data); }
        currentOp = data.operation;
        if ("MULTIPLY_BASE".equals(currentOp)) currentOp = "MULTIPLY_TOTAL";
    }

    @Override
    protected void canvasTick() {
        if (idInput != null) tempId = idInput.getValue();
        if (amountInput != null) tempAmount = amountInput.getValue();
    }

    @Override protected void buildUi() {
        int cx = canvasWidth() / 2; int cy = canvasHeight() / 2 - 50;
        idInput = addAutoCompleteField(cx - 100, cy - 35, 200, Component.empty(), ColorText.translatable("gui.kineticarmory.armorsets.input.id"), KineticSearch::attributeDictionary, null);
        idInput.setValue(tempId != null ? tempId : (isNew ? "" : (data.attribute != null ? data.attribute : "")));

        amountInput = addDecimalAutoCompleteField(cx - 100, cy - 10, 95, Component.empty(), ArrayList::new, true, null, null, null, null);
        amountInput.setPlaceholder(ColorText.translatable("gui.kineticarmory.armorsets.input.amount"));
        amountInput.setValue(tempAmount != null ? tempAmount : (isNew ? "" : String.valueOf(data.amount)));

        addButtonWithHandler(cx + 5, cy - 10, 95, ColorText.translatable("gui.kineticarmory.armorsets.op." + currentOp.toLowerCase()), null, b -> {
            currentOp = currentOp.equals("ADDITION") ? "MULTIPLY_TOTAL" : (currentOp.equals("MULTIPLY_TOTAL") ? "SET" : "ADDITION");
            b.setText(ColorText.translatable("gui.kineticarmory.armorsets.op." + currentOp.toLowerCase()));
            data.operation = currentOp;
        });

        addButtonWithHandler(cx - 100, cy + 15, 200, ColorText.translatable("gui.kineticarmory.armorsets.editor.conditions", data.conditions.size()), null, b -> {
            if (syncToData()) return;
            KineticClientRuntime.openScreen(new ConditionListScreen(this, data));
        });

        addButtonWithHandler(cx - 60, cy + 45, 55, ColorText.translatable("gui.kineticarmory.armorsets.save"), null, b -> {
            if (syncToData()) return;
            if (data.attribute.isEmpty()) { KineticOverlays.toast(ColorText.translatable("msg.kineticarmory.armorsets.empty_field")); return; }
            if (oldTip != null) config.tips.remove(oldTip);
            if (isNew) {
                config.attributes.add(data);
                isNew = false;
            }
            String newTip = ArmorTipGenerator.genAttrTip(data);
            config.tips.add(newTip);
            oldTip = newTip;
        });

        addButtonWithHandler(cx + 5, cy + 45, 55, ColorText.translatable("gui.kineticarmory.armorsets.back"), null, b -> { navigateBack(); });
    }

    private boolean syncToData() {
        if (idInput != null) tempId = idInput.getValue();
        if (amountInput != null) tempAmount = amountInput.getValue();

        data.attribute =
                (tempId == null ? "" : tempId.trim());
        data.operation = currentOp;

        Double amount = amountInput == null ? null : amountInput.getDoubleValue();

        if (amount == null) {
            KineticOverlays.toast(
                    ColorText.translatable("msg.kineticarmory.common.invalid_number")
            );
            return true;
        }

        data.amount = amount;
        return false;
    }

    @Override protected void renderCanvasBackground(@NotNull GuiGraphics g, int mx, int my, float pt) {
        int cx = canvasWidth() / 2; int cy = canvasHeight() / 2 - 50; GuiTheme.panel(g, cx - 120, cy - 70, 240, 150);
        g.drawCenteredString(font, title, cx, cy - 60, 0xFFFFFF);
    }

}
