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

public class ImmunityEditor extends KineticScreen {
    private final KineticScreen parent; private final ArmorDataConfig config;
    private final ArmorDataConfig.DamageImmunityData data; private final boolean isNew;
    private AutoCompleteBox idInput;
    private NumericAutoCompleteBox valInput;
    private String oldTip = null;
    private String tempId = null, tempVal = null;

    public ImmunityEditor(KineticScreen p, ArmorDataConfig c, ArmorDataConfig.DamageImmunityData d) {
        super(ColorText.translatable("gui.kineticarmory.armorsets.editor.immunity.title"));
        setParentScreen(p);
        parent = p; config = c; isNew = (d == null); data = isNew ? new ArmorDataConfig.DamageImmunityData() : d;
        if (!isNew) oldTip = ArmorTipGenerator.genImmTip(data);
    }

    @Override
    protected void canvasTick() {
        if (idInput != null) tempId = idInput.getValue();
        if (valInput != null) tempVal = valInput.getValue();
    }

    @Override protected void buildUi() {
        int cx = canvasWidth() / 2; int cy = canvasHeight() / 2 - 50;
        idInput = addAutoCompleteField(cx - 100, cy - 35, 200, Component.empty(), ColorText.translatable("gui.kineticarmory.armorsets.input.id_or_tag"), KineticSearch::damageDictionary, null);
        idInput.setValue(tempId != null ? tempId : (isNew ? "" : (data.damageType != null ? data.damageType : "")));

        valInput = addDecimalAutoCompleteField(cx - 100, cy - 10, 200, Component.empty(), ArrayList::new, true, null, null, null, null);
        valInput.setPlaceholder(ColorText.translatable("gui.kineticarmory.armorsets.input.multiplier"));
        valInput.setValue(tempVal != null ? tempVal : (isNew ? "" : String.valueOf(data.multiplier)));

        addButtonWithHandler(cx - 100, cy + 15, 200, ColorText.translatable("gui.kineticarmory.armorsets.editor.conditions", data.conditions.size()), null, b -> {
            if (syncToData()) return;
            KineticClientRuntime.openScreen(new ConditionListScreen(this, data));
        });

        addButtonWithHandler(cx - 60, cy + 45, 55, ColorText.translatable("gui.kineticarmory.armorsets.save"), null, b -> {
            if (syncToData()) return;
            if (data.damageType.isEmpty()) { KineticOverlays.toast(ColorText.translatable("msg.kineticarmory.armorsets.empty_field")); return; }
            if (!isNew && oldTip != null) config.tips.remove(oldTip);
            if (isNew) config.damageImmunities.add(data);
            config.tips.add(ArmorTipGenerator.genImmTip(data));
            navigateBack();
        });
        addButtonWithHandler(cx + 5, cy + 45, 55, ColorText.translatable("gui.kineticarmory.armorsets.back"), null, b -> { navigateBack(); });
    }

    private boolean syncToData() {
        if (idInput != null) tempId = idInput.getValue();
        if (valInput != null) tempVal = valInput.getValue();

        data.damageType =
                (tempId == null ? "" : tempId.trim());

        Double multiplier = valInput == null ? null : valInput.getDoubleValue();

        if (multiplier == null) {
            KineticOverlays.toast(
                    ColorText.translatable("msg.kineticarmory.common.invalid_number")
            );
            return true;
        }

        data.multiplier = multiplier;
        return false;
    }

    @Override protected void renderCanvasBackground(@NotNull GuiGraphics g, int mx, int my, float pt) {
        int cx = canvasWidth() / 2; int cy = canvasHeight() / 2 - 50; GuiTheme.panel(g, cx - 120, cy - 70, 240, 150);
        g.drawCenteredString(font, title, cx, cy - 60, 0xFFFFFF);
    }

}
