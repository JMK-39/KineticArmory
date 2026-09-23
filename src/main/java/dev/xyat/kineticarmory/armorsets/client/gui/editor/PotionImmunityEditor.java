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
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;

public class PotionImmunityEditor extends KineticScreen {
    private final KineticScreen parent; private final ArmorDataConfig config;
    private final ArmorDataConfig.EffectImmunityData data; private boolean isNew;
    private AutoCompleteBox idInput; private String oldTip = null;
    private String tempId = null;

    public PotionImmunityEditor(KineticScreen p, ArmorDataConfig c, ArmorDataConfig.EffectImmunityData d) {
        super(ColorText.translatable("gui.kineticarmory.armorsets.editor.effect_immunity.title"));
        setParentScreen(p);
        parent = p; config = c; isNew = (d == null); data = isNew ? new ArmorDataConfig.EffectImmunityData() : d;
        if (!isNew) oldTip = ArmorTipGenerator.genEffImmTip(data);
    }

    @Override
    protected void canvasTick() {
        if (idInput != null) tempId = idInput.getValue();
    }

    @Override protected void buildUi() {
        int cx = canvasWidth() / 2; int cy = canvasHeight() / 2 - 50;
        idInput = addAutoCompleteField(cx - 100, cy - 30, 200, Component.empty(), ColorText.translatable("gui.kineticarmory.armorsets.input.id"), KineticSearch::potionDictionary, null);
        idInput.setValue(tempId != null ? tempId : (isNew ? "" : (data.effectId != null ? data.effectId : "")));

        addButtonWithHandler(cx - 100, cy - 5, 200, ColorText.translatable("gui.kineticarmory.armorsets.editor.conditions", data.conditions.size()), null, b -> {
            syncToData();
            KineticClientRuntime.openScreen(new ConditionListScreen(this, data));
        });

        addButtonWithHandler(cx - 60, cy + 25, 55, ColorText.translatable("gui.kineticarmory.armorsets.save"), null, b -> {
            syncToData();
            if (data.effectId.isEmpty()) { KineticOverlays.toast(ColorText.translatable("msg.kineticarmory.armorsets.empty_field")); return; }
            if (oldTip != null) config.tips.remove(oldTip);
            if (isNew) {
                config.effectImmunities.add(data);
                isNew = false;
            }
            String newTip = ArmorTipGenerator.genEffImmTip(data);
            config.tips.add(newTip);
            oldTip = newTip;
        });
        addButtonWithHandler(cx + 5, cy + 25, 55, ColorText.translatable("gui.kineticarmory.armorsets.back"), null, b -> { navigateBack(); });
    }

    private void syncToData() {
        if (idInput != null) {
            tempId = idInput.getValue();
        }

        data.effectId =
                (tempId == null ? "" : tempId.trim());
    }

    @Override protected void renderCanvasBackground(@NotNull GuiGraphics g, int mx, int my, float pt) {
        int cx = canvasWidth() / 2; int cy = canvasHeight() / 2 - 50; GuiTheme.panel(g, cx - 120, cy - 60, 240, 115);
        g.drawCenteredString(font, title, cx, cy - 50, 0xFFFFFF);
    }

}
