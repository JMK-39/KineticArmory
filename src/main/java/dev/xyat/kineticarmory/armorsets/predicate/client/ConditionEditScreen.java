package dev.xyat.kineticarmory.armorsets.predicate.client;

import dev.xyat.kineticarmory.util.ColorText;
import dev.xyat.kineticarmory.armorsets.predicate.ConditionData;
import dev.xyat.kineticarmory.armorsets.predicate.ConditionTypeUtil;
import dev.xyat.kineticcore.api.client.theme.GuiTheme;
import dev.xyat.kineticcore.api.client.overlay.KineticOverlays;
import dev.xyat.kineticcore.api.client.selector.KineticSelectors;
import dev.xyat.kineticcore.api.client.screen.KineticScreen;
import dev.xyat.kineticcore.api.client.widget.KineticControl;
import dev.xyat.kineticcore.api.client.widget.button.KineticButtons.StateButton;
import dev.xyat.kineticcore.api.client.widget.input.KineticNumericFields;
import dev.xyat.kineticcore.api.client.widget.input.KineticTextFields.KineticEditBox;
import dev.xyat.kineticcore.api.registry.KineticRegistries;
import dev.xyat.kineticcore.api.client.widget.input.KineticAutoComplete.AutoCompleteBox;
import dev.xyat.kineticcore.api.client.widget.input.KineticNumericFields.NumericEditBox;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
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
    private boolean isNew;

    private AutoCompleteBox typeInput;
    private String lastTickType = "";

    private List<ConditionTypeUtil.ParamDef> currentSchema = new ArrayList<>();
    private final List<KineticControl> dynamicWidgets = new ArrayList<>();
    private final List<AutoCompleteBox> dynamicAcBoxes = new ArrayList<>();
    private final Map<String, String> currentParamValues = new HashMap<>();
    private final Set<String> invalidParams = new HashSet<>();

    private StateButton saveBtn;
    private StateButton backBtn;
    private int dynamicPanelHeight = 120;

    public ConditionEditScreen(ConditionListScreen parent, List<ConditionData> parentList, ConditionData data, boolean isNew) {
        super(ColorText.translatable("gui.kineticarmory.predicate.edit_title"));
        setParentScreen(parent);
        this.parent = parent;
        this.parentList = parentList;
        this.data = data;
        this.isNew = isNew;
        if (data.params != null) this.currentParamValues.putAll(data.params);
    }

    @Override protected void buildUi() {
        int cx = canvasWidth() / 2; int cy = canvasHeight() / 2;

        typeInput = addAutoCompleteField(cx - 120, cy - 50, 240, Component.empty(), null, ConditionTypeUtil::getSuggestions, null);
        typeInput.setValue((data.type != null && !data.type.isEmpty()) ? data.type.toUpperCase() : "");

        saveBtn = addButtonWithHandler(cx - 60, cy + 30, 55, ColorText.translatable("gui.kineticarmory.predicate.save"), null, b -> save());
        backBtn = addButtonWithHandler(cx + 5, cy + 30, 55, ColorText.translatable("gui.kineticarmory.predicate.back"), null, b -> {
            navigateBack();
        });
        lastTickType = ConditionTypeUtil.getRawType(typeInput.getValue());
        rebuildParamsUI(lastTickType);
    }

    @Override
    protected void canvasTick() {
        String currentType = ConditionTypeUtil.getRawType(typeInput.getValue());
        if (!currentType.equals(lastTickType)) {
            lastTickType = currentType;
            rebuildParamsUI(currentType);
        }
    }

    private void rebuildParamsUI(String newType) {
        for (KineticControl control : dynamicWidgets) removeKineticControl(control);
        dynamicWidgets.clear();
        dynamicAcBoxes.clear();
        invalidParams.clear();

        currentSchema = ConditionTypeUtil.getParamSchema(newType);
        int cx = canvasWidth() / 2;
        int currentY = (canvasHeight() / 2) - 10;

        for (ConditionTypeUtil.ParamDef def : currentSchema) {
            String initialVal = currentParamValues.getOrDefault(def.key(), def.defaultVal());

            if (isSecondsParam(newType, def.key())) {
                boolean allowEmpty = "TIME_RANGE".equals(newType);
                double minimum = allowEmpty ? 0.0D : 0.05D;
                double maximum = allowEmpty ? 1199.95D : Integer.MAX_VALUE / 20.0D;
                NumericEditBox box = addDecimalField(cx - 120, currentY + 12, 240, Component.empty(), false, minimum, maximum, null);
                box.setMaxLength(32);
                box.setValue(secondsDisplayValue(initialVal, allowEmpty));
                box.setResponder(value -> updateSecondsParam(def.key(), box, allowEmpty));
                updateSecondsParam(def.key(), box, allowEmpty);
                dynamicWidgets.add(box);
            } else if (def.type() == ConditionTypeUtil.ParamDataType.ITEM) {
                String displayStr = initialVal.isEmpty() ? ColorText.translatable("gui.kineticarmory.predicate.select_item").getString() : initialVal;
                StateButton btn = addButtonWithHandler(cx - 120, currentY + 12, 240, Component.literal(displayStr), null, b -> {
                    syncCurrentValues();
                    KineticSelectors.openItemSelector(this, selection -> {
                        if (!selection.isItem()) return;
                        String itemId = selectedItemId(selection.stack());
                        if (!itemId.isEmpty()) currentParamValues.put(def.key(), itemId);
                    });
                });
                dynamicWidgets.add(btn);
            }
            else if (def.type() == ConditionTypeUtil.ParamDataType.NUMBER || def.type() == ConditionTypeUtil.ParamDataType.STRING) {
                KineticEditBox box = addTextField(cx - 120, currentY + 12, 240, Component.empty());
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

                dynamicWidgets.add(box);
            }
            else {
                AutoCompleteBox acBox = addAutoCompleteField(cx - 120, currentY + 12, 240, Component.empty(), null, () -> ConditionTypeUtil.getSuggestionsFor(def.type()), null);
                acBox.setValue(initialVal);
                acBox.setResponder(s -> currentParamValues.put(def.key(), ConditionTypeUtil.extractValue(s)));
                dynamicAcBoxes.add(acBox); dynamicWidgets.add(acBox);
            }
            currentY += 45;
        }

        saveBtn.setY(currentY + 10);
        backBtn.setY(currentY + 10);

        int panelStartY = (canvasHeight() / 2) - 70;
        int buttonsBottomY = saveBtn.getY() + saveBtn.getHeight();
        dynamicPanelHeight = (buttonsBottomY + 15) - panelStartY;
    }

    private String selectedItemId(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return "";
        var id = KineticRegistries.items().id(stack.getItem());
        return id == null ? "" : id.toString();
    }

    private void syncCurrentValues() {
        data.type = ConditionTypeUtil.getRawType(typeInput.getValue());
        data.params.clear();
        data.params.putAll(currentParamValues);
    }

    private void save() {
        String typeStr = ConditionTypeUtil.getRawType(typeInput.getValue());
        if (typeStr.isEmpty()) { KineticOverlays.toast(ColorText.translatable("msg.kineticarmory.predicate.empty_type")); return; }

        if ("STAGE".equals(typeStr)) {
            String stageVal = currentParamValues.getOrDefault("stage", "");
            if (stageVal.isEmpty()) {
                KineticOverlays.toast(ColorText.translatable("msg.kineticarmory.predicate.invalid_stage"));
                return;
            }
        }

        if (!invalidParams.isEmpty()) {
            KineticOverlays.toast(ColorText.translatable("msg.kineticarmory.predicate.invalid_seconds"));
            return;
        }

        syncCurrentValues();
        if (isNew) {
            parentList.add(data);
            isNew = false;
        }
        parent.listWidget.refresh();
    }

    @Override protected void renderCanvasBackground(@NotNull GuiGraphics g, int mx, int my, float pt) {
        int cx = canvasWidth() / 2; int cy = canvasHeight() / 2;
        int panelY = cy - 70;
        GuiTheme.panel(g, cx - 140, panelY, 280, dynamicPanelHeight);

        g.drawCenteredString(font, title, cx, panelY + 10, 0xFFFFFF);
        g.drawString(font, ColorText.translatable("gui.kineticarmory.predicate.type"), cx - 120, cy - 62, 0xAAAAAA);
    }

    @Override protected void renderCanvasForeground(@NotNull GuiGraphics g, int mx, int my, float pt) {
        int cx = canvasWidth() / 2;

        for (int i = 0; i < currentSchema.size(); i++) {
            ConditionTypeUtil.ParamDef def = currentSchema.get(i);
            int y = (canvasHeight() / 2) - 10 + i * 45;

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
                    showTooltipLine(ColorText.translatable("gui.kineticarmory.predicate.param.hint", hint));
                }
            }
        }

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
            return KineticNumericFields.formatDecimal(ticks / 20.0D);
        } catch (NumberFormatException ignored) {
            return allowEmpty ? "" : "0.05";
        }
    }

    private void updateSecondsParam(String key, NumericEditBox box, boolean allowEmpty) {
        String raw = box.getValue().trim();
        if (allowEmpty && raw.isEmpty()) {
            currentParamValues.put(key, "");
            invalidParams.remove(key);
            return;
        }
        Double seconds = box.getDoubleValue();
        if (seconds == null) {
            invalidParams.add(key);
            return;
        }
        long ticks = Math.round(seconds * 20.0D);
        if (!allowEmpty) ticks = Math.max(1L, ticks);
        currentParamValues.put(key, Long.toString(ticks));
        invalidParams.remove(key);
    }
}
