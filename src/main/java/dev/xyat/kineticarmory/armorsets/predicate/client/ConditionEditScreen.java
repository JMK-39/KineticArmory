package dev.xyat.kineticarmory.armorsets.predicate.client;

import dev.xyat.kineticarmory.util.ColorText;
import dev.xyat.kineticarmory.armorsets.predicate.ConditionData;
import dev.xyat.kineticarmory.armorsets.predicate.ConditionTypeUtil;
import dev.xyat.kineticcore.api.client.theme.GuiTheme;
import dev.xyat.kineticcore.api.client.overlay.GuiOverlay;
import dev.xyat.kineticcore.api.client.selector.ItemSelectorScreen;
import dev.xyat.kineticcore.api.client.screen.KineticScreen;
import dev.xyat.kineticcore.api.client.widget.KineticWidgets.AutoCompleteBox;
import dev.xyat.kineticcore.api.client.widget.KineticWidgets.NumericEditBox;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.ForgeRegistries;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.HashSet;
import java.util.Set;

public class ConditionEditScreen extends KineticScreen {
    private final ConditionListScreen parent;
    private final List<ConditionData> parentList;
    private final ConditionData data;
    private final boolean isNew;

    private AutoCompleteBox typeInput;
    private String lastTickType = "";

    private List<ConditionTypeUtil.ParamDef> currentSchema = new ArrayList<>();
    private final List<AbstractWidget> dynamicWidgets = new ArrayList<>();
    private final List<AutoCompleteBox> dynamicAcBoxes = new ArrayList<>();
    private final Map<String, String> currentParamValues = new HashMap<>();
    private final Set<String> invalidParams = new HashSet<>();

    private Button saveBtn;
    private Button backBtn;
    private int dynamicPanelHeight = 120;

    public ConditionEditScreen(ConditionListScreen parent, List<ConditionData> parentList, ConditionData data, boolean isNew) {
        super(ColorText.translatable("gui.kineticarmory.predicate.edit_title"));
        this.parent = parent;
        this.parentList = parentList;
        this.data = data;
        this.isNew = isNew;
        useCanvas(
                640f,
                360f,
                6
        );
        if (data.params != null) this.currentParamValues.putAll(data.params);
    }

    @Override protected void buildUi() {
        int cx = canvasWidth / 2; int cy = canvasHeight / 2;

        typeInput = new AutoCompleteBox(font, cx - 120, cy - 50, 240, 20, Component.empty(), ConditionTypeUtil::getSuggestions);
        typeInput.setValue((data.type != null && !data.type.isEmpty()) ? data.type.toUpperCase() : "");
        addRenderableWidget(typeInput);

        saveBtn = Button.builder(ColorText.translatable("gui.kineticarmory.predicate.save"), b -> saveAndClose())
                .bounds(cx - 60, cy + 30, 55, 20).build();
        backBtn = Button.builder(ColorText.translatable("gui.kineticarmory.predicate.back"), b -> {
            if (minecraft != null) minecraft.setScreen(parent);
        }).bounds(cx + 5, cy + 30, 55, 20).build();

        addRenderableWidget(saveBtn);
        addRenderableWidget(backBtn);

        lastTickType = ConditionTypeUtil.getRawType(typeInput.getValue());
        rebuildParamsUI(lastTickType);
    }

    @Override
    public void tick() {
        super.tick();
        String currentType = ConditionTypeUtil.getRawType(typeInput.getValue());
        if (!currentType.equals(lastTickType)) {
            lastTickType = currentType;
            rebuildParamsUI(currentType);
        }
    }

    private void rebuildParamsUI(String newType) {
        for (AbstractWidget w : dynamicWidgets) removeWidget(w);
        dynamicWidgets.clear();
        dynamicAcBoxes.clear();
        invalidParams.clear();

        currentSchema = ConditionTypeUtil.getParamSchema(newType);
        int cx = canvasWidth / 2;
        int currentY = (canvasHeight / 2) - 10;

        for (ConditionTypeUtil.ParamDef def : currentSchema) {
            String initialVal = currentParamValues.getOrDefault(def.key(), def.defaultVal());

            if (isSecondsParam(newType, def.key())) {
                boolean allowEmpty = "TIME_RANGE".equals(newType);
                double minimum = allowEmpty ? 0.0D : 0.05D;
                double maximum = allowEmpty ? 1199.95D : Integer.MAX_VALUE / 20.0D;
                NumericEditBox box = NumericEditBox.decimal(
                        font, cx - 120, currentY + 12, 240, 20,
                        Component.empty(), false, minimum, maximum
                );
                box.setMaxLength(32);
                box.setValue(secondsDisplayValue(initialVal, allowEmpty));
                box.setResponder(value -> updateSecondsParam(def.key(), box, allowEmpty));
                updateSecondsParam(def.key(), box, allowEmpty);
                dynamicWidgets.add(box);
                addRenderableWidget(box);
            }
            else if (def.type() == ConditionTypeUtil.ParamDataType.ITEM) {
                String displayStr = initialVal.isEmpty() ? ColorText.translatable("gui.kineticarmory.predicate.select_item").getString() : initialVal;
                Button btn = Button.builder(Component.literal(displayStr), b -> {
                    syncCurrentValues();
                    if (minecraft != null) {
                        minecraft.setScreen(new ItemSelectorScreen(this, selection -> {
                            if (!selection.isItem()) return;
                            String itemId = selectedItemId(selection.stack());
                            if (!itemId.isEmpty()) currentParamValues.put(def.key(), itemId);
                            minecraft.setScreen(this);
                        }));
                    }
                }).bounds(cx - 120, currentY + 12, 240, 20).build();
                dynamicWidgets.add(btn); addRenderableWidget(btn);
            }
            else if (def.type() == ConditionTypeUtil.ParamDataType.NUMBER || def.type() == ConditionTypeUtil.ParamDataType.STRING) {
                EditBox box = new EditBox(font, cx - 120, currentY + 12, 240, 20, Component.empty());
                box.setValue(initialVal);

                if (def.key().equals("stage")) {
                    box.setResponder(s -> {
                        String parsed = s.replace("，", ",").replaceAll("[^a-zA-Z0-9_,]", "");
                        if (!parsed.equals(s)) {
                            box.setValue(parsed);
                        }
                        currentParamValues.put(def.key(), parsed);
                    });
                } else {
                    box.setResponder(s -> currentParamValues.put(def.key(), s));
                }

                dynamicWidgets.add(box); addRenderableWidget(box);
            }
            else {
                AutoCompleteBox acBox = new AutoCompleteBox(font, cx - 120, currentY + 12, 240, 20, Component.empty(), () -> ConditionTypeUtil.getSuggestionsFor(def.type()));
                acBox.setValue(initialVal);
                acBox.setResponder(s -> currentParamValues.put(def.key(), ConditionTypeUtil.extractValue(s)));
                dynamicAcBoxes.add(acBox); dynamicWidgets.add(acBox); addRenderableWidget(acBox);
            }
            currentY += 45;
        }

        saveBtn.setY(currentY + 10);
        backBtn.setY(currentY + 10);

        int panelStartY = (canvasHeight / 2) - 70;
        int buttonsBottomY = saveBtn.getY() + saveBtn.getHeight();
        dynamicPanelHeight = (buttonsBottomY + 15) - panelStartY;
    }

    private String selectedItemId(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return "";
        ResourceLocation id = ForgeRegistries.ITEMS.getKey(stack.getItem());
        return id == null ? "" : id.toString();
    }

    private void syncCurrentValues() {
        data.type = ConditionTypeUtil.getRawType(typeInput.getValue());
        data.params.clear();
        data.params.putAll(currentParamValues);
    }

    private void saveAndClose() {
        String typeStr = ConditionTypeUtil.getRawType(typeInput.getValue());
        if (typeStr.isEmpty()) { GuiOverlay.toast(ColorText.translatable("msg.kineticarmory.predicate.empty_type")); return; }

        if ("STAGE".equals(typeStr)) {
            String stageVal = currentParamValues.getOrDefault("stage", "");
            if (stageVal.isEmpty()) {
                GuiOverlay.toast(ColorText.translatable("msg.kineticarmory.predicate.invalid_stage"));
                return;
            }
        }

        if (!invalidParams.isEmpty()) {
            GuiOverlay.toast(ColorText.translatable("msg.kineticarmory.predicate.invalid_seconds"));
            return;
        }

        syncCurrentValues();
        if (isNew) parentList.add(data);
        parent.listWidget.refresh();
        if (minecraft != null) minecraft.setScreen(parent);
    }

    @Override protected void renderCanvasBackground(@NotNull GuiGraphics g, int mx, int my, float pt) {
        int cx = canvasWidth / 2; int cy = canvasHeight / 2;
        int panelY = cy - 70;
        GuiTheme.panel(g, cx - 140, panelY, 280, dynamicPanelHeight);

        g.drawCenteredString(font, title, cx, panelY + 10, 0xFFFFFF);
        g.drawString(font, ColorText.translatable("gui.kineticarmory.predicate.type"), cx - 120, cy - 62, 0xAAAAAA);
    }

    @Override protected void renderCanvasForeground(@NotNull GuiGraphics g, int mx, int my, float pt) {
        int cx = canvasWidth / 2;

        for (int i = 0; i < currentSchema.size(); i++) {
            ConditionTypeUtil.ParamDef def = currentSchema.get(i);
            int y = (canvasHeight / 2) - 10 + i * 45;

            String label = ConditionTypeUtil.getTranslatedParamName(def.key());
            if (isSecondsParam(ConditionTypeUtil.getRawType(typeInput.getValue()), def.key())) {
                label += " " + ColorText.translatable("gui.kineticarmory.predicate.unit.seconds").getString();
            }
            g.drawString(
                    font,
                    ColorText.translatable("gui.kineticarmory.predicate.param.label", label, def.key()),
                    cx - 118,
                    y,
                    0xFFFFFF
            );

            if (mx >= cx - 120 && mx <= cx + 120 && my >= y && my <= y + 10) {
                String hint = ConditionTypeUtil.getTranslatedParamHint(def.key());
                if (!hint.isEmpty() && !hint.startsWith("gui.")) {
                    GuiOverlay.requestTooltip(ColorText.translatable("gui.kineticarmory.predicate.param.hint", hint), mx, my);
                }
            }
        }

        typeInput.renderSuggestions(g, mx, my);
        for (int i = dynamicAcBoxes.size() - 1; i >= 0; i--) {
            dynamicAcBoxes.get(i).renderSuggestions(g, mx, my);
        }
    }

    @Override protected boolean canvasMouseScrolled(double x, double y, double d) {
        if(typeInput.handleMouseScrolled(d)) return true;
        for (AutoCompleteBox box : dynamicAcBoxes) if (box.visible && box.handleMouseScrolled(d)) return true;
        return super.canvasMouseScrolled(x, y, d);
    }

    @Override protected boolean canvasMouseDragged(double mx, double my, int btn, double dx, double dy) {
        if (typeInput != null && typeInput.handleMouseDragged(mx, my)) return true;
        for (AutoCompleteBox box : dynamicAcBoxes) if (box.visible && box.handleMouseDragged(mx, my)) return true;
        return super.canvasMouseDragged(mx, my, btn, dx, dy);
    }

    @Override protected boolean canvasMouseReleased(double mx, double my, int btn) {
        if (typeInput != null && typeInput.handleMouseReleased(btn)) return true;
        for (AutoCompleteBox box : dynamicAcBoxes) if (box.visible && box.handleMouseReleased(btn)) return true;
        return super.canvasMouseReleased(mx, my, btn);
    }

    @Override protected boolean canvasMouseClicked(double x, double y, int b) {
        boolean handled = (typeInput != null && typeInput.handleMouseClick(x, y)) ||
                dynamicAcBoxes.stream().anyMatch(box -> box.visible && box.handleMouseClick(x, y));

        boolean res = super.canvasMouseClicked(x, y, b);

        boolean clickedInput = (typeInput != null && typeInput.isMouseOver(x, y)) ||
                dynamicAcBoxes.stream().anyMatch(box -> box.visible && box.isMouseOver(x, y)) ||
                dynamicWidgets.stream().anyMatch(w -> (w instanceof EditBox || w instanceof Button) && w.isMouseOver(x, y));

        if (!clickedInput && !handled) {
            if (typeInput != null) typeInput.setFocused(false);
            dynamicAcBoxes.forEach(box -> box.setFocused(false));
            this.setFocused(null);
        }

        return handled || res;
    }

    @Override public boolean keyPressed(int k, int s, int m) {
        if (typeInput.handleKeyPressed(k)) return true;
        for (AutoCompleteBox box : dynamicAcBoxes) if (box.visible && box.handleKeyPressed(k)) return true;
        return super.keyPressed(k, s, m);
    }

    private static boolean isSecondsParam(String conditionType, String key) {
        return (("MOUSE_LEFT_HOLD".equals(conditionType) || "MOUSE_RIGHT_HOLD".equals(conditionType))
                && "ticks".equals(key))
                || ("TIME_RANGE".equals(conditionType) && ("min".equals(key) || "max".equals(key)));
    }

    private static String secondsDisplayValue(String storedTicks, boolean allowEmpty) {
        if (storedTicks == null || storedTicks.isBlank()) return allowEmpty ? "" : "0.05";
        try {
            double ticks = Double.parseDouble(storedTicks.trim());
            return NumericEditBox.format(ticks / 20.0D);
        } catch (NumberFormatException ignored) {
            return allowEmpty ? "" : "0.05";
        }
    }

    private void updateSecondsParam(String key, NumericEditBox box, boolean allowEmpty) {
        String raw = box.getValue().trim();
        if (allowEmpty && raw.isEmpty()) {
            currentParamValues.put(key, "");
            invalidParams.remove(key);
            box.setTextColor(0xFFE0E0E0);
            return;
        }
        Double seconds = box.getDoubleValue();
        if (seconds == null) {
            invalidParams.add(key);
            box.setTextColor(0xFFFF5555);
            return;
        }
        long ticks = Math.round(seconds * 20.0D);
        if (!allowEmpty) ticks = Math.max(1L, ticks);
        currentParamValues.put(key, Long.toString(ticks));
        invalidParams.remove(key);
        box.setTextColor(0xFFE0E0E0);
    }
}
