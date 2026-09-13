package dev.xyat.kineticarmory.armorsets.client.gui.editor;

import dev.xyat.kineticarmory.util.ColorText;
import dev.xyat.kineticarmory.armorsets.data.ArmorDataConfig;
import dev.xyat.kineticarmory.armorsets.data.ArmorTipGenerator;
import dev.xyat.kineticarmory.armorsets.predicate.client.ConditionListScreen;
import dev.xyat.kineticcore.api.client.theme.GuiTheme;
import dev.xyat.kineticcore.api.client.overlay.GuiOverlay;
import dev.xyat.kineticcore.api.client.search.KineticSearch;
import dev.xyat.kineticcore.api.client.screen.KineticScreen;
import dev.xyat.kineticcore.api.client.widget.KineticWidgets.AutoCompleteBox;
import dev.xyat.kineticcore.api.client.widget.KineticWidgets.AutoCompleteBoxGroup;
import dev.xyat.kineticcore.api.client.widget.KineticWidgets.NumericAutoCompleteBox;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;
import java.util.ArrayList;

public class DamageConversionEditor extends KineticScreen {
    private final AutoCompleteBoxGroup inputGroup =
            new AutoCompleteBoxGroup();
    private final KineticScreen parent; private final ArmorDataConfig config; private final ArmorDataConfig.DamageConversionData data; private final boolean isNew;
    private AutoCompleteBox srcInput, tgtInput;
    private NumericAutoCompleteBox ratioInput, chanceInput;
    private String oldTip = null;
    private String tempSrc = null, tempTgt = null, tempRatio = null, tempChance = null;

    public DamageConversionEditor(KineticScreen p, ArmorDataConfig c, ArmorDataConfig.DamageConversionData d) {
        super(ColorText.translatable("gui.kineticarmory.armorsets.editor.dmg_convert.title"));
        useResponsiveCanvas(
                640f,
                360f,
                6
        );
        parent = p; config = c; isNew = (d == null); data = isNew ? new ArmorDataConfig.DamageConversionData() : d;
        if (!isNew) oldTip = ArmorTipGenerator.genConvTip(data);
    }

    @Override
    public void tick() {
        super.tick();
        if (srcInput != null) tempSrc = srcInput.getValue();
        if (tgtInput != null) tempTgt = tgtInput.getValue();
        if (ratioInput != null) tempRatio = ratioInput.getValue();
        if (chanceInput != null) tempChance = chanceInput.getValue();
    }

    @Override protected void buildUi() {
        int cx = canvasWidth() / 2; int cy = canvasHeight() / 2 - 50;
        srcInput = addAutoCompleteField(cx - 125, cy - 30, 110, Component.empty(), KineticSearch::getDamageDict, null);
        srcInput.setValue(tempSrc != null ? tempSrc : (isNew ? "" : (data.sourceType != null ? data.sourceType : "")));

        tgtInput = addAutoCompleteField(cx + 15, cy - 30, 110, Component.empty(), KineticSearch::getSpecificDamageDict, null);
        tgtInput.setValue(tempTgt != null ? tempTgt : (isNew ? "" : (data.targetType != null ? data.targetType : "")));

        ratioInput = addDecimalAutoCompleteField(cx - 125, cy - 5, 110, Component.empty(), ArrayList::new, true, null, null, null);
        ratioInput.setValue(tempRatio != null ? tempRatio : (isNew ? "" : String.valueOf(data.ratio)));

        chanceInput = addDecimalAutoCompleteField(cx + 15, cy - 5, 110, Component.empty(), ArrayList::new, true, null, null, null);
        chanceInput.setValue(tempChance != null ? tempChance : (isNew ? "" : String.valueOf(data.chance)));

        addButton(cx - 100, cy + 20, 200, ColorText.translatable("gui.kineticarmory.armorsets.editor.conditions", data.conditions.size()), null, b -> {
            if (syncToData()) return;
            if (minecraft != null) minecraft.setScreen(new ConditionListScreen(this, data));
        });

        addButton(cx - 60, cy + 50, 55, ColorText.translatable("gui.kineticarmory.armorsets.save"), null, b -> {
            if (syncToData()) return;
            if (data.sourceType.isEmpty() || data.targetType.isEmpty()) {
                GuiOverlay.toast(ColorText.translatable("msg.kineticarmory.armorsets.empty_field")); return;
            }
            if (data.targetType.startsWith("#") || data.targetType.equalsIgnoreCase("all")) {
                GuiOverlay.toast(ColorText.translatable("msg.kineticarmory.armorsets.convert_target_error")); return;
            }

            if (!isNew && oldTip != null) config.tips.remove(oldTip);
            if (isNew) config.damageConversions.add(data);
            config.tips.add(ArmorTipGenerator.genConvTip(data));
            if (minecraft != null) navigateBack();
        });
        addButton(cx + 5, cy + 50, 55, ColorText.translatable("gui.kineticarmory.armorsets.back"), null, b -> { if (minecraft != null) navigateBack(); });


        inputGroup.set(
                srcInput,
                tgtInput,
                ratioInput,
                chanceInput
        );
    }

    private boolean syncToData() {
        if (srcInput != null) tempSrc = srcInput.getValue();
        if (tgtInput != null) tempTgt = tgtInput.getValue();
        if (ratioInput != null) tempRatio = ratioInput.getValue();
        if (chanceInput != null) tempChance = chanceInput.getValue();

        data.sourceType =
                AutoCompleteBox.normalizeValue(
                        tempSrc
                );

        data.targetType =
                AutoCompleteBox.normalizeValue(
                        tempTgt
                );

        Double ratio = ratioInput == null ? null : ratioInput.getDoubleValue();
        Double chance = chanceInput == null ? null : chanceInput.getDoubleValue();

        if (ratio == null || chance == null) {
            GuiOverlay.toast(
                    ColorText.translatable("msg.kineticarmory.common.invalid_number")
            );
            return true;
        }

        data.ratio = ratio;
        data.chance = chance;
        return false;
    }

    private void renderInputHint(GuiGraphics g, AutoCompleteBox box, String key) {
        renderTextFieldPlaceholder(g, box, ColorText.translatable(key));
    }

    @Override protected void renderCanvasBackground(@NotNull GuiGraphics g, int mx, int my, float pt) {
        int cx = canvasWidth() / 2; int cy = canvasHeight() / 2 - 50; GuiTheme.panel(g, cx - 140, cy - 60, 280, 145);
        g.drawCenteredString(font, title, cx, cy - 50, 0xFFFFFF);
    }
    @Override protected void renderCanvasForeground(@NotNull GuiGraphics g, int mx, int my, float pt) {
        renderInputHint(g, srcInput, "gui.kineticarmory.armorsets.input.source_type");
        renderInputHint(g, tgtInput, "gui.kineticarmory.armorsets.input.target_type");
        renderInputHint(g, ratioInput, "gui.kineticarmory.armorsets.input.ratio");
        renderInputHint(g, chanceInput, "gui.kineticarmory.armorsets.input.chance");
        inputGroup.renderSuggestions(g, mx, my);
    }
    @Override
    protected boolean canvasMouseScrolled(
            double mouseX,
            double mouseY,
            double delta
    ) {
        if (inputGroup.handleMouseScrolled(delta)) {
            return true;
        }

        return super.canvasMouseScrolled(
                mouseX,
                mouseY,
                delta
        );
    }

    @Override
    protected boolean canvasMouseClicked(
            double mouseX,
            double mouseY,
            int button
    ) {
        if (inputGroup.handleSuggestionClick(
                mouseX,
                mouseY
        )) {
            return true;
        }

        boolean handled =
                super.canvasMouseClicked(
                        mouseX,
                        mouseY,
                        button
                );

        inputGroup.clearFocusOutside(
                mouseX,
                mouseY
        );

        return handled;
    }

    @Override
    protected boolean canvasMouseDragged(
            double mouseX,
            double mouseY,
            int button,
            double dragX,
            double dragY
    ) {
        if (inputGroup.handleMouseDragged(
                mouseX,
                mouseY
        )) {
            return true;
        }

        return super.canvasMouseDragged(
                mouseX,
                mouseY,
                button,
                dragX,
                dragY
        );
    }

    @Override
    protected boolean canvasMouseReleased(
            double mouseX,
            double mouseY,
            int button
    ) {
        if (inputGroup.handleMouseReleased(button)) {
            return true;
        }

        return super.canvasMouseReleased(
                mouseX,
                mouseY,
                button
        );
    }

    @Override
    public boolean keyPressed(
            int keyCode,
            int scanCode,
            int modifiers
    ) {
        return inputGroup.handleKeyPressed(keyCode)
                || super.keyPressed(
                        keyCode,
                        scanCode,
                        modifiers
                );
    }

}
