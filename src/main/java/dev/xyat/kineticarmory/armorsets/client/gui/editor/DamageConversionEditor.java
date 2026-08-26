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

public class DamageConversionEditor extends ScaledScreen {
    private final AutoCompleteBoxGroup inputGroup =
            new AutoCompleteBoxGroup();
    private final ScaledScreen parent; private final ArmorDataConfig config; private final ArmorDataConfig.DamageConversionData data; private final boolean isNew;
    private AutoCompleteBox srcInput, tgtInput;
    private NumericAutoCompleteBox ratioInput, chanceInput;
    private String oldTip = null;
    private String tempSrc = null, tempTgt = null, tempRatio = null, tempChance = null;

    public DamageConversionEditor(ScaledScreen p, ArmorDataConfig c, ArmorDataConfig.DamageConversionData d) {
        super(ColorText.translatable("gui.kineticarmory.armorsets.editor.dmg_convert.title"));
        configureResponsiveCanvas(
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

    @Override protected void initScaled() {
        int cx = vWidth / 2; int cy = vHeight / 2 - 50;
        srcInput = new AutoCompleteBox(font, cx - 125, cy - 30, 110, 20, Component.empty(), RegistryDictUtil::getDamageDict);
        srcInput.setValue(tempSrc != null ? tempSrc : (isNew ? "" : (data.sourceType != null ? data.sourceType : "")));

        tgtInput = new AutoCompleteBox(font, cx + 15, cy - 30, 110, 20, Component.empty(), RegistryDictUtil::getSpecificDamageDict);
        tgtInput.setValue(tempTgt != null ? tempTgt : (isNew ? "" : (data.targetType != null ? data.targetType : "")));

        ratioInput = NumericAutoCompleteBox.decimal(font, cx - 125, cy - 5, 110, 20, Component.empty(), ArrayList::new, true, null, null);
        ratioInput.setValue(tempRatio != null ? tempRatio : (isNew ? "" : String.valueOf(data.ratio)));

        chanceInput = NumericAutoCompleteBox.decimal(font, cx + 15, cy - 5, 110, 20, Component.empty(), ArrayList::new, true, null, null);
        chanceInput.setValue(tempChance != null ? tempChance : (isNew ? "" : String.valueOf(data.chance)));

        addRenderableWidget(Button.builder(ColorText.translatable("gui.kineticarmory.armorsets.editor.conditions", data.conditions.size()), b -> {
            if (syncToData()) return;
            if (minecraft != null) minecraft.setScreen(new ConditionListScreen(this, data));
        }).bounds(cx - 100, cy + 20, 200, 20).build());

        addRenderableWidget(Button.builder(ColorText.translatable("gui.kineticarmory.armorsets.save"), b -> {
            if (syncToData()) return;
            if (data.sourceType.isEmpty() || data.targetType.isEmpty()) {
                GuiToastUtil.showToast(ColorText.translatable("msg.kineticarmory.armorsets.empty_field")); return;
            }
            if (data.targetType.startsWith("#") || data.targetType.equalsIgnoreCase("all")) {
                GuiToastUtil.showToast(ColorText.translatable("msg.kineticarmory.armorsets.convert_target_error")); return;
            }

            if (!isNew && oldTip != null) config.tips.remove(oldTip);
            if (isNew) config.damageConversions.add(data);
            config.tips.add(ArmorTipGenerator.genConvTip(data));
            if (minecraft != null) minecraft.setScreen(parent);
        }).bounds(cx - 60, cy + 50, 55, 20).build());
        addRenderableWidget(Button.builder(ColorText.translatable("gui.kineticarmory.armorsets.back"), b -> { if (minecraft != null) minecraft.setScreen(parent); }).bounds(cx + 5, cy + 50, 55, 20).build());

        addRenderableWidget(srcInput); addRenderableWidget(tgtInput); addRenderableWidget(ratioInput); addRenderableWidget(chanceInput);

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
            GuiToastUtil.showToast(
                    ColorText.translatable("msg.kineticarmory.common.invalid_number")
            );
            return true;
        }

        data.ratio = ratio;
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
        int cx = vWidth / 2; int cy = vHeight / 2 - 50; GuiRenderUtil.drawStandardPanel(g, cx - 140, cy - 60, 280, 145);
        g.drawCenteredString(font, title, cx, cy - 50, 0xFFFFFF);
    }
    @Override protected void renderScaledForeground(@NotNull GuiGraphics g, int mx, int my, float pt) {
        renderInputHint(g, srcInput, "gui.kineticarmory.armorsets.input.source_type");
        renderInputHint(g, tgtInput, "gui.kineticarmory.armorsets.input.target_type");
        renderInputHint(g, ratioInput, "gui.kineticarmory.armorsets.input.ratio");
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
