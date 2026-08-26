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
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;

public class PotionImmunityEditor extends ScaledScreen {
    private final AutoCompleteBoxGroup inputGroup =
            new AutoCompleteBoxGroup();
    private final ScaledScreen parent; private final ArmorDataConfig config;
    private final ArmorDataConfig.EffectImmunityData data; private final boolean isNew;
    private AutoCompleteBox idInput; private String oldTip = null;
    private String tempId = null;

    public PotionImmunityEditor(ScaledScreen p, ArmorDataConfig c, ArmorDataConfig.EffectImmunityData d) {
        super(ColorText.translatable("gui.kineticarmory.armorsets.editor.effect_immunity.title"));
        configureResponsiveCanvas(
                640f,
                360f,
                6
        );
        parent = p; config = c; isNew = (d == null); data = isNew ? new ArmorDataConfig.EffectImmunityData() : d;
        if (!isNew) oldTip = ArmorTipGenerator.genEffImmTip(data);
    }

    @Override
    public void tick() {
        super.tick();
        if (idInput != null) tempId = idInput.getValue();
    }

    @Override protected void initScaled() {
        int cx = vWidth / 2; int cy = vHeight / 2 - 50;
        idInput = new AutoCompleteBox(font, cx - 100, cy - 30, 200, 20, Component.empty(), RegistryDictUtil::getPotionDict);
        idInput.setValue(tempId != null ? tempId : (isNew ? "" : (data.effectId != null ? data.effectId : "")));

        addRenderableWidget(Button.builder(ColorText.translatable("gui.kineticarmory.armorsets.editor.conditions", data.conditions.size()), b -> {
            syncToData();
            if (minecraft != null) minecraft.setScreen(new ConditionListScreen(this, data));
        }).bounds(cx - 100, cy - 5, 200, 20).build());

        addRenderableWidget(Button.builder(ColorText.translatable("gui.kineticarmory.armorsets.save"), b -> {
            syncToData();
            if (data.effectId.isEmpty()) { GuiToastUtil.showToast(ColorText.translatable("msg.kineticarmory.armorsets.empty_field")); return; }
            if (!isNew && oldTip != null) config.tips.remove(oldTip);
            if (isNew) config.effectImmunities.add(data);
            config.tips.add(ArmorTipGenerator.genEffImmTip(data));
            if (minecraft != null) minecraft.setScreen(parent);
        }).bounds(cx - 60, cy + 25, 55, 20).build());
        addRenderableWidget(Button.builder(ColorText.translatable("gui.kineticarmory.armorsets.back"), b -> { if (minecraft != null) minecraft.setScreen(parent); }).bounds(cx + 5, cy + 25, 55, 20).build());

        addRenderableWidget(idInput);

        inputGroup.set(
                idInput
        );
    }

    private void syncToData() {
        if (idInput != null) {
            tempId = idInput.getValue();
        }

        data.effectId =
                AutoCompleteBox.normalizeValue(
                        tempId
                );
    }

    private void renderInputHint(GuiGraphics g, AutoCompleteBox box) {
        if (box != null && !box.isFocused() && box.getValue().isEmpty()) {
            String text = ColorText.translatable("gui.kineticarmory.armorsets.input.id").getString();
            g.drawString(font, font.plainSubstrByWidth(text, box.getWidth() - 8), box.getX() + 4, box.getY() + 6, 0x999999, false);
        }
    }

    @Override protected void renderScaledBackground(@NotNull GuiGraphics g, int mx, int my, float pt) {
        int cx = vWidth / 2; int cy = vHeight / 2 - 50; GuiRenderUtil.drawStandardPanel(g, cx - 120, cy - 60, 240, 115);
        g.drawCenteredString(font, title, cx, cy - 50, 0xFFFFFF);
    }
    @Override protected void renderScaledForeground(@NotNull GuiGraphics g, int mx, int my, float pt) {
        renderInputHint(g, idInput);
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
