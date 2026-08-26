package dev.xyat.kineticarmory.armorsets.client.gui.editor;

import dev.xyat.kineticarmory.util.ColorText;
import dev.xyat.kineticarmory.armorsets.data.ArmorDataConfig;
import dev.xyat.kineticarmory.armorsets.data.ArmorTipGenerator;
import dev.xyat.kineticarmory.armorsets.predicate.client.ConditionListScreen;
import dev.xyat.kineticcore.api.client.GuiRenderUtil;
import dev.xyat.kineticcore.api.client.GuiToastUtil;
import dev.xyat.kineticcore.api.client.RegistryDictUtil;
import dev.xyat.kineticcore.api.client.ScaledScreen;
import dev.xyat.kineticcore.api.client.gui.AutoCompleteBox;
import dev.xyat.kineticcore.api.client.gui.AutoCompleteBoxGroup;
import dev.xyat.kineticcore.api.client.gui.NumericAutoCompleteBox;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;
import java.util.ArrayList;

public class AttackEffectEditor extends ScaledScreen {
    private final AutoCompleteBoxGroup inputGroup =
            new AutoCompleteBoxGroup();
    private final ScaledScreen parent; private final ArmorDataConfig config; private final ArmorDataConfig.AttackEffectData data; private final boolean isNew;
    private AutoCompleteBox idInput;
    private NumericAutoCompleteBox durInput, lvlInput, chanceInput;
    private String oldTip = null;
    private String tempId = null, tempDur = null, tempLvl = null, tempChance = null;

    public AttackEffectEditor(ScaledScreen p, ArmorDataConfig c, ArmorDataConfig.AttackEffectData d) {
        super(ColorText.translatable("gui.kineticarmory.armorsets.editor.attack.title"));
        configureResponsiveCanvas(
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

    @Override protected void initScaled() {
        int cx = vWidth / 2; int cy = vHeight / 2 - 50;
        idInput = new AutoCompleteBox(font, cx - 100, cy - 35, 200, 20, Component.empty(), RegistryDictUtil::getPotionDict);
        idInput.setValue(tempId != null ? tempId : (isNew ? "" : (data.effectId != null ? data.effectId : "")));

        int w = 60; int gap = 10; int startX = cx - 100;
        durInput = NumericAutoCompleteBox.decimal(font, startX, cy - 10, w, 20, Component.empty(), ArrayList::new, true, null, null);
        durInput.setValue(tempDur != null ? tempDur : (isNew ? "" : String.valueOf(data.duration)));

        lvlInput = NumericAutoCompleteBox.integer(font, startX + w + gap, cy - 10, w, 20, Component.empty(), ArrayList::new, true, null, null);
        lvlInput.setValue(tempLvl != null ? tempLvl : (isNew ? "" : String.valueOf(data.amplifier)));

        chanceInput = NumericAutoCompleteBox.decimal(font, startX + (w + gap) * 2, cy - 10, w, 20, Component.empty(), ArrayList::new, true, null, null);
        chanceInput.setValue(tempChance != null ? tempChance : (isNew ? "" : String.valueOf(data.chance)));

        addRenderableWidget(Button.builder(ColorText.translatable("gui.kineticarmory.armorsets.editor.conditions", data.conditions.size()), b -> {
            if (syncToData()) return;
            if (minecraft != null) minecraft.setScreen(new ConditionListScreen(this, data));
        }).bounds(cx - 100, cy + 15, 200, 20).build());

        addRenderableWidget(Button.builder(ColorText.translatable("gui.kineticarmory.armorsets.save"), b -> {
            if (syncToData()) return;
            if (data.effectId.isEmpty()) { GuiToastUtil.showToast(ColorText.translatable("msg.kineticarmory.armorsets.empty_field")); return; }
            if (!isNew && oldTip != null) config.tips.remove(oldTip);
            if (isNew) config.attackEffects.add(data);
            config.tips.add(ArmorTipGenerator.genAtkTip(data));
            if (minecraft != null) minecraft.setScreen(parent);
        }).bounds(cx - 60, cy + 45, 55, 20).build());
        addRenderableWidget(Button.builder(ColorText.translatable("gui.kineticarmory.armorsets.back"), b -> { if (minecraft != null) minecraft.setScreen(parent); }).bounds(cx + 5, cy + 45, 55, 20).build());

        addRenderableWidget(idInput); addRenderableWidget(durInput); addRenderableWidget(lvlInput); addRenderableWidget(chanceInput);

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
            GuiToastUtil.showToast(
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
        if (box != null && !box.isFocused() && box.getValue().isEmpty()) {
            String text = ColorText.translatable(key).getString();
            g.drawString(font, font.plainSubstrByWidth(text, box.getWidth() - 8), box.getX() + 4, box.getY() + 6, 0x999999, false);
        }
    }

    @Override protected void renderScaledBackground(@NotNull GuiGraphics g, int mx, int my, float pt) {
        int cx = vWidth / 2; int cy = vHeight / 2 - 50;
        GuiRenderUtil.drawStandardPanel(g, cx - 120, cy - 70, 240, 150);
        g.drawCenteredString(font, title, cx, cy - 60, 0xFFFFFF);
    }
    @Override protected void renderScaledForeground(@NotNull GuiGraphics g, int mx, int my, float pt) {
        renderInputHint(g, idInput, "gui.kineticarmory.armorsets.input.id");
        renderInputHint(g, durInput, "gui.kineticarmory.armorsets.input.duration");
        renderInputHint(g, lvlInput, "gui.kineticarmory.armorsets.input.level");
        renderInputHint(g, chanceInput, "gui.kineticarmory.armorsets.input.chance");
        inputGroup.renderSuggestions(g, mx, my);
    }
    @Override
    protected boolean universalMouseScrolled(
            double mouseX,
            double mouseY,
            double delta
    ) {
        if (inputGroup.handleMouseScrolled(delta)) {
            return true;
        }

        return super.universalMouseScrolled(
                mouseX,
                mouseY,
                delta
        );
    }

    @Override
    protected boolean universalMouseClicked(
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
                super.universalMouseClicked(
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
    protected boolean universalMouseDragged(
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

        return super.universalMouseDragged(
                mouseX,
                mouseY,
                button,
                dragX,
                dragY
        );
    }

    @Override
    protected boolean universalMouseReleased(
            double mouseX,
            double mouseY,
            int button
    ) {
        if (inputGroup.handleMouseReleased(button)) {
            return true;
        }

        return super.universalMouseReleased(
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
