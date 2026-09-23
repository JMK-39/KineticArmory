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

public class PotionEditor extends KineticScreen {
    private final KineticScreen parent; private final ArmorDataConfig config;
    private final ArmorDataConfig.PotionEffectData data; private boolean isNew;
    private AutoCompleteBox idInput;
    private NumericAutoCompleteBox lvlInput, timeInput;
    private String oldTip = null;
    private String tempId = null, tempLvl = null, tempTime = null;

    public PotionEditor(KineticScreen p, ArmorDataConfig c, ArmorDataConfig.PotionEffectData d) {
        super(ColorText.translatable("gui.kineticarmory.armorsets.editor.potion.title"));
        setParentScreen(p);
        parent = p; config = c; isNew = (d == null); data = isNew ? new ArmorDataConfig.PotionEffectData() : d;
        if (!isNew) oldTip = ArmorTipGenerator.genPotTip(data);
    }

    @Override
    protected void canvasTick() {
        if (idInput != null) tempId = idInput.getValue();
        if (lvlInput != null) tempLvl = lvlInput.getValue();
        if (timeInput != null) tempTime = timeInput.getValue();
    }

    @Override protected void buildUi() {
        int cx = canvasWidth() / 2; int cy = canvasHeight() / 2 - 50;
        idInput = addAutoCompleteField(cx - 100, cy - 35, 200, Component.empty(), ColorText.translatable("gui.kineticarmory.armorsets.input.id"), KineticSearch::potionDictionary, null);
        idInput.setValue(tempId != null ? tempId : (isNew ? "" : (data.effectId != null ? data.effectId : "")));

        lvlInput = addIntegerAutoCompleteField(cx - 100, cy - 10, 95, Component.empty(), ArrayList::new, true, null, null, null, null);
        lvlInput.setPlaceholder(ColorText.translatable("gui.kineticarmory.armorsets.input.level"));
        lvlInput.setValue(tempLvl != null ? tempLvl : (isNew ? "" : String.valueOf(data.amplifier)));

        timeInput = addIntegerAutoCompleteField(cx + 5, cy - 10, 95, Component.empty(), ArrayList::new, true, null, null, null, null);
        timeInput.setPlaceholder(ColorText.translatable("gui.kineticarmory.armorsets.input.duration"));
        timeInput.setValue(tempTime != null ? tempTime : (isNew ? "" : String.valueOf(data.duration)));

        addButtonWithHandler(cx - 100, cy + 15, 200, ColorText.translatable("gui.kineticarmory.armorsets.editor.conditions", data.conditions.size()), null, b -> {
            if (syncToData()) return;
            KineticClientRuntime.openScreen(new ConditionListScreen(this, data));
        });

        addButtonWithHandler(cx - 60, cy + 45, 55, ColorText.translatable("gui.kineticarmory.armorsets.save"), null, b -> {
            if (syncToData()) return;
            if (data.effectId.isEmpty()) { KineticOverlays.toast(ColorText.translatable("msg.kineticarmory.armorsets.empty_field")); return; }
            if (oldTip != null) config.tips.remove(oldTip);
            if (isNew) {
                config.potionEffects.add(data);
                isNew = false;
            }
            String newTip = ArmorTipGenerator.genPotTip(data);
            config.tips.add(newTip);
            oldTip = newTip;
        });

        addButtonWithHandler(cx + 5, cy + 45, 55, ColorText.translatable("gui.kineticarmory.armorsets.back"), null, b -> { navigateBack(); });
    }

    private boolean syncToData() {
        if (idInput != null) tempId = idInput.getValue();
        if (lvlInput != null) tempLvl = lvlInput.getValue();
        if (timeInput != null) tempTime = timeInput.getValue();

        data.effectId =
                (tempId == null ? "" : tempId.trim());

        Integer amplifier = lvlInput == null ? null : lvlInput.getIntValue();
        Integer duration = timeInput == null ? null : timeInput.getIntValue();

        if (amplifier == null || duration == null) {
            KineticOverlays.toast(
                    ColorText.translatable("msg.kineticarmory.common.invalid_number")
            );
            return true;
        }

        data.amplifier = amplifier;
        data.duration = duration;
        return false;
    }

    @Override protected void renderCanvasBackground(@NotNull GuiGraphics g, int mx, int my, float pt) {
        int cx = canvasWidth() / 2; int cy = canvasHeight() / 2 - 50; GuiTheme.panel(g, cx - 120, cy - 70, 240, 150);
        g.drawCenteredString(font, title, cx, cy - 60, 0xFFFFFF);
    }

}
