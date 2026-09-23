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

public class DamageConversionEditor extends KineticScreen {
    private final KineticScreen parent; private final ArmorDataConfig config; private final ArmorDataConfig.DamageConversionData data; private boolean isNew;
    private AutoCompleteBox srcInput, tgtInput;
    private NumericAutoCompleteBox ratioInput, chanceInput;
    private String oldTip = null;
    private String tempSrc = null, tempTgt = null, tempRatio = null, tempChance = null;

    public DamageConversionEditor(KineticScreen p, ArmorDataConfig c, ArmorDataConfig.DamageConversionData d) {
        super(ColorText.translatable("gui.kineticarmory.armorsets.editor.dmg_convert.title"));
        setParentScreen(p);
        parent = p; config = c; isNew = (d == null); data = isNew ? new ArmorDataConfig.DamageConversionData() : d;
        if (!isNew) oldTip = ArmorTipGenerator.genConvTip(data);
    }

    @Override
    protected void canvasTick() {
        if (srcInput != null) tempSrc = srcInput.getValue();
        if (tgtInput != null) tempTgt = tgtInput.getValue();
        if (ratioInput != null) tempRatio = ratioInput.getValue();
        if (chanceInput != null) tempChance = chanceInput.getValue();
    }

    @Override protected void buildUi() {
        int cx = canvasWidth() / 2; int cy = canvasHeight() / 2 - 50;
        srcInput = addAutoCompleteField(cx - 125, cy - 30, 110, Component.empty(), ColorText.translatable("gui.kineticarmory.armorsets.input.source_type"), KineticSearch::damageDictionary, null);
        srcInput.setValue(tempSrc != null ? tempSrc : (isNew ? "" : (data.sourceType != null ? data.sourceType : "")));

        tgtInput = addAutoCompleteField(cx + 15, cy - 30, 110, Component.empty(), ColorText.translatable("gui.kineticarmory.armorsets.input.target_type"), KineticSearch::specificDamageDictionary, null);
        tgtInput.setValue(tempTgt != null ? tempTgt : (isNew ? "" : (data.targetType != null ? data.targetType : "")));

        ratioInput = addDecimalAutoCompleteField(cx - 125, cy - 5, 110, Component.empty(), ArrayList::new, true, null, null, null, null);
        ratioInput.setPlaceholder(ColorText.translatable("gui.kineticarmory.armorsets.input.ratio"));
        ratioInput.setValue(tempRatio != null ? tempRatio : (isNew ? "" : String.valueOf(data.ratio)));

        chanceInput = addDecimalAutoCompleteField(cx + 15, cy - 5, 110, Component.empty(), ArrayList::new, true, null, null, null, null);
        chanceInput.setPlaceholder(ColorText.translatable("gui.kineticarmory.armorsets.input.chance"));
        chanceInput.setValue(tempChance != null ? tempChance : (isNew ? "" : String.valueOf(data.chance)));

        addButtonWithHandler(cx - 100, cy + 20, 200, ColorText.translatable("gui.kineticarmory.armorsets.editor.conditions", data.conditions.size()), null, b -> {
            if (syncToData()) return;
            KineticClientRuntime.openScreen(new ConditionListScreen(this, data));
        });

        addButtonWithHandler(cx - 60, cy + 50, 55, ColorText.translatable("gui.kineticarmory.armorsets.save"), null, b -> {
            if (syncToData()) return;
            if (data.sourceType.isEmpty() || data.targetType.isEmpty()) {
                KineticOverlays.toast(ColorText.translatable("msg.kineticarmory.armorsets.empty_field")); return;
            }
            if (data.targetType.startsWith("#") || data.targetType.equalsIgnoreCase("all")) {
                KineticOverlays.toast(ColorText.translatable("msg.kineticarmory.armorsets.convert_target_error")); return;
            }

            if (oldTip != null) config.tips.remove(oldTip);
            if (isNew) {
                config.damageConversions.add(data);
                isNew = false;
            }
            String newTip = ArmorTipGenerator.genConvTip(data);
            config.tips.add(newTip);
            oldTip = newTip;
        });
        addButtonWithHandler(cx + 5, cy + 50, 55, ColorText.translatable("gui.kineticarmory.armorsets.back"), null, b -> { navigateBack(); });
    }

    private boolean syncToData() {
        if (srcInput != null) tempSrc = srcInput.getValue();
        if (tgtInput != null) tempTgt = tgtInput.getValue();
        if (ratioInput != null) tempRatio = ratioInput.getValue();
        if (chanceInput != null) tempChance = chanceInput.getValue();

        data.sourceType =
                (tempSrc == null ? "" : tempSrc.trim());

        data.targetType =
                (tempTgt == null ? "" : tempTgt.trim());

        Double ratio = ratioInput == null ? null : ratioInput.getDoubleValue();
        Double chance = chanceInput == null ? null : chanceInput.getDoubleValue();

        if (ratio == null || chance == null) {
            KineticOverlays.toast(
                    ColorText.translatable("msg.kineticarmory.common.invalid_number")
            );
            return true;
        }

        data.ratio = ratio;
        data.chance = chance;
        return false;
    }

    @Override protected void renderCanvasBackground(@NotNull GuiGraphics g, int mx, int my, float pt) {
        int cx = canvasWidth() / 2; int cy = canvasHeight() / 2 - 50; GuiTheme.panel(g, cx - 140, cy - 60, 280, 145);
        g.drawCenteredString(font, title, cx, cy - 50, 0xFFFFFF);
    }

}
