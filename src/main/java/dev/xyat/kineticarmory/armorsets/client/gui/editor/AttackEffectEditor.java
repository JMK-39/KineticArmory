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

public class AttackEffectEditor extends KineticScreen {
    private final AutoCompleteBoxGroup inputGroup =
            new AutoCompleteBoxGroup();
    private final KineticScreen parent; private final ArmorDataConfig config; private final ArmorDataConfig.AttackEffectData data; private final boolean isNew;
    private AutoCompleteBox idInput;
    private NumericAutoCompleteBox durInput, lvlInput, chanceInput;
    private String oldTip = null;
    private String tempId = null, tempDur = null, tempLvl = null, tempChance = null;

    public AttackEffectEditor(KineticScreen p, ArmorDataConfig c, ArmorDataConfig.AttackEffectData d) {
        super(ColorText.translatable("gui.kineticarmory.armorsets.editor.attack.title"));
        useResponsiveCanvas(
                640f,
                360f,
                6
        );
        parent = p; config = c; isNew = (d == null); data = isNew ? new ArmorDataConfig.AttackEffectData() : d;
        if (!isNew) oldTip = ArmorTipGenerator.genAtkTip(data);
    }

    @Override
    public void tick() {
        super.tick();
        if (idInput != null) tempId = idInput.getValue();
        if (durInput != null) tempDur = durInput.getValue();
        if (lvlInput != null) tempLvl = lvlInput.getValue();
        if (chanceInput != null) tempChance = chanceInput.getValue();
    }

    @Override protected void buildUi() {
        int cx = canvasWidth() / 2; int cy = canvasHeight() / 2 - 50;
        idInput = addAutoCompleteField(cx - 100, cy - 35, 200, Component.empty(), KineticSearch::getPotionDict, null);
        idInput.setValue(tempId != null ? tempId : (isNew ? "" : (data.effectId != null ? data.effectId : "")));

        int w = 60; int gap = 10; int startX = cx - 100;
        durInput = addDecimalAutoCompleteField(startX, cy - 10, w, Component.empty(), ArrayList::new, true, null, null, null);
        durInput.setValue(tempDur != null ? tempDur : (isNew ? "" : String.valueOf(data.duration)));

        lvlInput = addIntegerAutoCompleteField(startX + w + gap, cy - 10, w, Component.empty(), ArrayList::new, true, null, null, null);
        lvlInput.setValue(tempLvl != null ? tempLvl : (isNew ? "" : String.valueOf(data.amplifier)));

        chanceInput = addDecimalAutoCompleteField(startX + (w + gap) * 2, cy - 10, w, Component.empty(), ArrayList::new, true, null, null, null);
        chanceInput.setValue(tempChance != null ? tempChance : (isNew ? "" : String.valueOf(data.chance)));

        addButton(cx - 100, cy + 15, 200, ColorText.translatable("gui.kineticarmory.armorsets.editor.conditions", data.conditions.size()), null, b -> {
            if (syncToData()) return;
            if (minecraft != null) minecraft.setScreen(new ConditionListScreen(this, data));
        });

        addButton(cx - 60, cy + 45, 55, ColorText.translatable("gui.kineticarmory.armorsets.save"), null, b -> {
            if (syncToData()) return;
            if (data.effectId.isEmpty()) { GuiOverlay.toast(ColorText.translatable("msg.kineticarmory.armorsets.empty_field")); return; }
            if (!isNew && oldTip != null) config.tips.remove(oldTip);
            if (isNew) config.attackEffects.add(data);
            config.tips.add(ArmorTipGenerator.genAtkTip(data));
            if (minecraft != null) navigateBack();
        });
        addButton(cx + 5, cy + 45, 55, ColorText.translatable("gui.kineticarmory.armorsets.back"), null, b -> { if (minecraft != null) navigateBack(); });


        inputGroup.set(
                idInput,
                durInput,
                lvlInput,
                chanceInput
        );
    }

    private boolean syncToData() {
        if (idInput != null) tempId = idInput.getValue();
        if (durInput != null) tempDur = durInput.getValue();
        if (lvlInput != null) tempLvl = lvlInput.getValue();
        if (chanceInput != null) tempChance = chanceInput.getValue();

        data.effectId =
                AutoCompleteBox.normalizeValue(
                        tempId
                );

        Double duration = durInput == null ? null : durInput.getDoubleValue();
        Integer amplifier = lvlInput == null ? null : lvlInput.getIntValue();
        Double chance = chanceInput == null ? null : chanceInput.getDoubleValue();

        if (duration == null || amplifier == null || chance == null) {
            GuiOverlay.toast(
                    ColorText.translatable("msg.kineticarmory.common.invalid_number")
            );
            return true;
        }

        data.duration = duration;
        data.amplifier = amplifier;
        data.chance = chance;
        return false;
    }

    private void renderInputHint(GuiGraphics g, AutoCompleteBox box, String key) {
        renderTextFieldPlaceholder(g, box, ColorText.translatable(key));
    }

    @Override protected void renderCanvasBackground(@NotNull GuiGraphics g, int mx, int my, float pt) {
        int cx = canvasWidth() / 2; int cy = canvasHeight() / 2 - 50;
        GuiTheme.panel(g, cx - 120, cy - 70, 240, 150);
        g.drawCenteredString(font, title, cx, cy - 60, 0xFFFFFF);
    }
    @Override protected void renderCanvasForeground(@NotNull GuiGraphics g, int mx, int my, float pt) {
        renderInputHint(g, idInput, "gui.kineticarmory.armorsets.input.id");
        renderInputHint(g, durInput, "gui.kineticarmory.armorsets.input.duration");
        renderInputHint(g, lvlInput, "gui.kineticarmory.armorsets.input.level");
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
