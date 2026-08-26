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
import java.util.UUID;

public class AttributeEditor extends ScaledScreen {
    private final AutoCompleteBoxGroup inputGroup =
            new AutoCompleteBoxGroup();
    private final ScaledScreen parent; private final ArmorDataConfig config;
    private final ArmorDataConfig.AttributeModifierData data; private final boolean isNew;
    private AutoCompleteBox idInput;
    private NumericAutoCompleteBox amountInput;
    private String currentOp;
    private String oldTip = null;
    private String tempId = null, tempAmount = null;

    public AttributeEditor(ScaledScreen p, ArmorDataConfig c, ArmorDataConfig.AttributeModifierData d) {
        super(ColorText.translatable("gui.kineticarmory.armorsets.editor.attr.title"));
        configureResponsiveCanvas(
                640f,
                360f,
                6
        );
        parent = p; config = c; isNew = (d == null); data = isNew ? new ArmorDataConfig.AttributeModifierData() : d;
        if (isNew && data.uuid == null) { data.uuid = UUID.randomUUID().toString(); data.operation = "ADDITION"; }
        if (!isNew) { oldTip = ArmorTipGenerator.genAttrTip(data); }
        currentOp = data.operation;
        if ("MULTIPLY_BASE".equals(currentOp)) currentOp = "MULTIPLY_TOTAL";
    }

    @Override
    public void tick() {
        super.tick();
        if (idInput != null) tempId = idInput.getValue();
        if (amountInput != null) tempAmount = amountInput.getValue();
    }

    @Override protected void initScaled() {
        int cx = vWidth / 2; int cy = vHeight / 2 - 50;
        idInput = new AutoCompleteBox(font, cx - 100, cy - 35, 200, 20, Component.empty(), RegistryDictUtil::getAttributeDict);
        idInput.setValue(tempId != null ? tempId : (isNew ? "" : (data.attribute != null ? data.attribute : "")));

        amountInput = NumericAutoCompleteBox.decimal(font, cx - 100, cy - 10, 95, 20, Component.empty(), ArrayList::new, true, null, null);
        amountInput.setValue(tempAmount != null ? tempAmount : (isNew ? "" : String.valueOf(data.amount)));

        Button opBtn = Button.builder(ColorText.translatable("gui.kineticarmory.armorsets.op." + currentOp.toLowerCase()), b -> {
            currentOp = currentOp.equals("ADDITION") ? "MULTIPLY_TOTAL" : (currentOp.equals("MULTIPLY_TOTAL") ? "SET" : "ADDITION");
            b.setMessage(ColorText.translatable("gui.kineticarmory.armorsets.op." + currentOp.toLowerCase()));
            data.operation = currentOp;
        }).bounds(cx + 5, cy - 10, 95, 20).build();

        addRenderableWidget(Button.builder(ColorText.translatable("gui.kineticarmory.armorsets.editor.conditions", data.conditions.size()), b -> {
            if (syncToData()) return;
            if (minecraft != null) minecraft.setScreen(new ConditionListScreen(this, data));
        }).bounds(cx - 100, cy + 15, 200, 20).build());

        addRenderableWidget(Button.builder(ColorText.translatable("gui.kineticarmory.armorsets.save"), b -> {
            if (syncToData()) return;
            if (data.attribute.isEmpty()) { GuiToastUtil.showToast(ColorText.translatable("msg.kineticarmory.armorsets.empty_field")); return; }
            if (!isNew && oldTip != null) config.tips.remove(oldTip);
            if (isNew) config.attributes.add(data);
            config.tips.add(ArmorTipGenerator.genAttrTip(data));
            if (minecraft != null) minecraft.setScreen(parent);
        }).bounds(cx - 60, cy + 45, 55, 20).build());

        addRenderableWidget(Button.builder(ColorText.translatable("gui.kineticarmory.armorsets.back"), b -> { if (minecraft != null) minecraft.setScreen(parent); }).bounds(cx + 5, cy + 45, 55, 20).build());
        addRenderableWidget(opBtn);
        addRenderableWidget(idInput); addRenderableWidget(amountInput);

        inputGroup.set(
                idInput,
                amountInput
        );
    }

    private boolean syncToData() {
        if (idInput != null) tempId = idInput.getValue();
        if (amountInput != null) tempAmount = amountInput.getValue();

        data.attribute =
                AutoCompleteBox.normalizeValue(
                        tempId
                );
        data.operation = currentOp;

        Double amount = amountInput == null ? null : amountInput.getDoubleValue();

        if (amount == null) {
            GuiToastUtil.showToast(
                    ColorText.translatable("msg.kineticarmory.common.invalid_number")
            );
            return true;
        }

        data.amount = amount;
        return false;
    }

    private void renderInputHint(GuiGraphics g, AutoCompleteBox box, String key) {
        if (box != null && !box.isFocused() && box.getValue().isEmpty()) {
            String text = ColorText.translatable(key).getString();
            g.drawString(font, font.plainSubstrByWidth(text, box.getWidth() - 8), box.getX() + 4, box.getY() + 6, 0x888888, false);
        }
    }

    @Override protected void renderScaledBackground(@NotNull GuiGraphics g, int mx, int my, float pt) {
        int cx = vWidth / 2; int cy = vHeight / 2 - 50; GuiRenderUtil.drawStandardPanel(g, cx - 120, cy - 70, 240, 150);
        g.drawCenteredString(font, title, cx, cy - 60, 0xFFFFFF);
    }
    @Override protected void renderScaledForeground(@NotNull GuiGraphics g, int mx, int my, float pt) {
        renderInputHint(g, idInput, "gui.kineticarmory.armorsets.input.id");
        renderInputHint(g, amountInput, "gui.kineticarmory.armorsets.input.amount");
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
