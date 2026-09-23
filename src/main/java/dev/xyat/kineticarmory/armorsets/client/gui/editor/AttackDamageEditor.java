package dev.xyat.kineticarmory.armorsets.client.gui.editor;

import dev.xyat.kineticarmory.util.ColorText;
import dev.xyat.kineticarmory.armorsets.data.ArmorDataConfig;
import dev.xyat.kineticarmory.armorsets.data.ArmorTipGenerator;
import dev.xyat.kineticarmory.armorsets.predicate.client.ConditionListScreen;
import dev.xyat.kineticcore.api.client.theme.GuiTheme;
import dev.xyat.kineticcore.api.client.overlay.KineticOverlays;
import dev.xyat.kineticcore.api.client.screen.KineticScreen;
import dev.xyat.kineticcore.api.runtime.KineticClientRuntime;
import dev.xyat.kineticcore.api.client.widget.input.KineticAutoComplete.NumericAutoCompleteBox;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;
import java.util.ArrayList;

public class AttackDamageEditor extends KineticScreen {
    private final KineticScreen parent; private final ArmorDataConfig config;
    private final ArmorDataConfig.AttackDamageMultiplierData data; private boolean isNew;
    private NumericAutoCompleteBox valInput;
    private String oldTip = null;
    private String tempVal = null;

    public AttackDamageEditor(KineticScreen p, ArmorDataConfig c, ArmorDataConfig.AttackDamageMultiplierData d) {
        super(ColorText.translatable("gui.kineticarmory.armorsets.editor.attack_damage.title"));
        setParentScreen(p); parent = p; config = c; isNew = (d == null); data = isNew ? new ArmorDataConfig.AttackDamageMultiplierData() : d;
        if (!isNew) oldTip = ArmorTipGenerator.genAtkDmgTip(data);
    }

    @Override
    protected void canvasTick() {
        if (valInput != null) tempVal = valInput.getValue();
    }

    @Override protected void buildUi() {
        int cx = canvasWidth() / 2; int cy = canvasHeight() / 2 - 50;
        valInput = addDecimalAutoCompleteField(cx - 100, cy - 30, 200, Component.empty(), ArrayList::new, true, null, null, null, null);
        valInput.setPlaceholder(ColorText.translatable("gui.kineticarmory.armorsets.input.attack_multiplier"));
        valInput.setValue(tempVal != null ? tempVal : (isNew ? "" : String.valueOf(data.multiplier)));

        addButtonWithHandler(cx - 100, cy - 5, 200, ColorText.translatable("gui.kineticarmory.armorsets.editor.conditions", data.conditions.size()), null, b -> {
            if (syncToData()) return;
            KineticClientRuntime.openScreen(new ConditionListScreen(this, data));
        });

        addButtonWithHandler(cx - 60, cy + 25, 55, ColorText.translatable("gui.kineticarmory.armorsets.save"), null, b -> {
            if (syncToData()) return;
            if (valInput.getValue().trim().isEmpty()) { KineticOverlays.toast(ColorText.translatable("msg.kineticarmory.armorsets.empty_field")); return; }
            if (oldTip != null) config.tips.remove(oldTip);
            if (isNew) {
                config.attackDamageMultipliers.add(data);
                isNew = false;
            }
            String newTip = ArmorTipGenerator.genAtkDmgTip(data);
            config.tips.add(newTip);
            oldTip = newTip;
        });
        addButtonWithHandler(cx + 5, cy + 25, 55, ColorText.translatable("gui.kineticarmory.armorsets.back"), null, b -> { navigateBack(); });
    }

    private boolean syncToData() {
        if (valInput != null) tempVal = valInput.getValue();

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
        int cx = canvasWidth() / 2; int cy = canvasHeight() / 2 - 50; GuiTheme.panel(g, cx - 120, cy - 60, 240, 115);
        g.drawCenteredString(font, title, cx, cy - 50, 0xFFFFFF);
    }

}
