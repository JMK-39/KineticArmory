package dev.xyat.kineticarmory.armorsets.predicate.client;

import dev.xyat.kineticcore.api.client.gui.overlay.KineticOverlays;
import dev.xyat.kineticcore.api.client.gui.page.KineticPage;
import dev.xyat.kineticcore.api.client.gui.render.KineticGraphics;
import dev.xyat.kineticcore.api.client.gui.selector.KineticSelectors;
import dev.xyat.kineticcore.api.client.gui.theme.KineticTheme;
import dev.xyat.kineticcore.api.client.gui.ui.KineticUi;
import dev.xyat.kineticcore.api.client.gui.ui.NumberType;
import dev.xyat.kineticcore.api.client.gui.widget.*;
import dev.xyat.kineticcore.api.client.gui.widget.list.*;

import dev.xyat.kineticarmory.armorsets.predicate.ConditionData;
import dev.xyat.kineticarmory.armorsets.predicate.ConditionTypeUtil;
import dev.xyat.kineticcore.api.registry.KineticRegistries;
import dev.xyat.kineticcore.api.text.KineticI18n;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.HashSet;
import java.util.Set;

public class ConditionEditPage extends KineticPage {
    private static final int FIELD_WIDTH = 240;
    // Rows from the panel top: title, type label, type field, then one label + field per parameter, then the buttons.
    // The panel height follows from the parameter count and the panel is centred in the page both ways.
    private static final int TITLE_Y = 10;
    private static final int TYPE_LABEL_Y = 28;
    private static final int TYPE_FIELD_Y = 40;
    private static final int FIRST_PARAM_Y = 72;
    private static final int PARAM_STEP = 45;
    private static final int PARAM_FIELD_OFFSET = 12;
    private static final int BOTTOM_PADDING = 10;
    private static final int PANEL_WIDTH = 280;
    private static final int BACK_WIDTH = 55;
    private static final int BACK_Y = TYPE_LABEL_Y - 20 - 2;
    private final List<ConditionData> parentList;
    private final ConditionData data;
    private boolean isNew;

    private KineticAutoCompleteField typeInput;
    private String lastTickType = "";

    private List<ConditionTypeUtil.ParamDef> currentSchema = new ArrayList<>();
    private final List<KineticControl> dynamicWidgets = new ArrayList<>();
    private final List<KineticAutoCompleteField> dynamicAcBoxes = new ArrayList<>();
    private final Map<String, String> currentParamValues = new HashMap<>();
    private final Set<String> invalidParams = new HashSet<>();

    private KineticButton saveBtn;
    private KineticButton backBtn;
    private int dynamicPanelHeight = 120;

    public ConditionEditPage(List<ConditionData> parentList, ConditionData data, boolean isNew) {
        super(KineticI18n.translatable("gui.kineticarmory.predicate.edit_title"));
        this.parentList = parentList;
        this.data = data;
        this.isNew = isNew;
        if (data.params != null) this.currentParamValues.putAll(data.params);
    }

    @Override protected void build(KineticUi ui) {
        int cx = width() / 2;

        typeInput = ui().autoComplete(cx - FIELD_WIDTH / 2, panelTop() + TYPE_FIELD_Y, FIELD_WIDTH, ConditionTypeUtil::getSuggestions).firstShownTextAsDefault().build();
        typeInput.setTextValue((data.type != null && !data.type.isEmpty()) ? data.type.toUpperCase() : "");

        saveBtn = ui().button(cx - 60, panelTop() + buttonsOffset(currentSchema.size()), 55).text(KineticI18n.translatable("gui.kineticarmory.predicate.save")).onClick(b -> save()).build();
        backBtn = ui().button(cx - PANEL_WIDTH / 2 + 12, panelTop() + BACK_Y, BACK_WIDTH).text(KineticI18n.translatable("gui.kineticarmory.predicate.back")).onClick(b -> {
            navigateBack();
        }).build();
        lastTickType = ConditionTypeUtil.getRawType(typeInput.textValue());
        rebuildParamsUI(lastTickType);
    }

    @Override
    protected void onTick() {
        String currentType = ConditionTypeUtil.getRawType(typeInput.textValue());
        if (!currentType.equals(lastTickType)) {
            lastTickType = currentType;
            rebuildParamsUI(currentType);
        }
    }

    private void rebuildParamsUI(String newType) {
        for (KineticControl control : dynamicWidgets) ui().remove(control);
        dynamicWidgets.clear();
        dynamicAcBoxes.clear();
        invalidParams.clear();

        currentSchema = ConditionTypeUtil.getParamSchema(newType);
        int cx = width() / 2;
        int top = panelTop();
        typeInput.moveControlY(top + TYPE_FIELD_Y);
        int currentY = top + FIRST_PARAM_Y;

        for (ConditionTypeUtil.ParamDef def : currentSchema) {
            String initialVal = currentParamValues.getOrDefault(def.key(), def.defaultVal());

            if (isSecondsParam(newType, def.key())) {
                boolean allowEmpty = "TIME_RANGE".equals(newType);
                double minimum = allowEmpty ? 0.0D : 0.05D;
                double maximum = allowEmpty ? 1199.95D : Integer.MAX_VALUE / 20.0D;
                var builder = ui().numberField(cx - FIELD_WIDTH / 2, currentY + PARAM_FIELD_OFFSET, FIELD_WIDTH, NumberType.DECIMAL).allowNegative(false).range(minimum, maximum).firstShownTextAsDefault();
                // A time range may leave either end empty (no limit), so an empty field is not an error there.
                if (allowEmpty) builder.optional();
                KineticNumberField box = builder.build();
                box.limitTextLength(32);
                box.setTextValue(secondsDisplayValue(initialVal, allowEmpty));
                box.onTextChange(value -> updateSecondsParam(def.key(), box, allowEmpty));
                updateSecondsParam(def.key(), box, allowEmpty);
                dynamicWidgets.add(box);
            } else if (def.type() == ConditionTypeUtil.ParamDataType.ITEM) {
                String displayStr = initialVal.isEmpty() ? KineticI18n.translatable("gui.kineticarmory.predicate.select_item").getString() : initialVal;
                KineticButton btn = ui().button(cx - FIELD_WIDTH / 2, currentY + PARAM_FIELD_OFFSET, FIELD_WIDTH).text(Component.literal(displayStr)).onClick(b -> {
                    syncCurrentValues();
                    KineticSelectors.openItemSelector(selection -> {
                        if (!selection.isItem()) return;
                        String itemId = selectedItemId(selection.stack());
                        if (!itemId.isEmpty()) currentParamValues.put(def.key(), itemId);
                    });
                }).build();
                dynamicWidgets.add(btn);
            }
            else if (def.type() == ConditionTypeUtil.ParamDataType.NUMBER || def.type() == ConditionTypeUtil.ParamDataType.STRING) {
                KineticTextField box = ui().textField(cx - FIELD_WIDTH / 2, currentY + PARAM_FIELD_OFFSET, FIELD_WIDTH).firstShownTextAsDefault().build();
                box.setTextValue(initialVal);

                if (def.key().equals("stage")) {
                    box.onTextChange(s -> {
                        String parsed = s.replace("，", ",").replaceAll("[^a-zA-Z0-9_,]", "");
                        if (!parsed.equals(s)) {
                            box.setTextValue(parsed);
                        }
                        currentParamValues.put(def.key(), parsed);
                    });
                } else {
                    box.onTextChange(s -> currentParamValues.put(def.key(), s));
                }

                dynamicWidgets.add(box);
            }
            else {
                KineticAutoCompleteField acBox = ui().autoComplete(cx - FIELD_WIDTH / 2, currentY + PARAM_FIELD_OFFSET, FIELD_WIDTH, () -> ConditionTypeUtil.getSuggestionsFor(def.type())).firstShownTextAsDefault().build();
                acBox.setTextValue(initialVal);
                acBox.onTextChange(s -> currentParamValues.put(def.key(), ConditionTypeUtil.extractValue(s)));
                dynamicAcBoxes.add(acBox); dynamicWidgets.add(acBox);
            }
            currentY += PARAM_STEP;
        }

        saveBtn.moveControlY(top + buttonsOffset(currentSchema.size()));
        backBtn.moveControlY(top + BACK_Y);
        dynamicPanelHeight = panelHeight(currentSchema.size());
    }

    private String selectedItemId(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return "";
        var id = KineticRegistries.items().id(stack.getItem());
        return id == null ? "" : id.toString();
    }

    private void syncCurrentValues() {
        data.type = ConditionTypeUtil.getRawType(typeInput.textValue());
        data.params.clear();
        data.params.putAll(currentParamValues);
    }

    private void save() {
        String typeStr = ConditionTypeUtil.getRawType(typeInput.textValue());
        if (typeStr.isEmpty()) { KineticOverlays.toast(KineticI18n.translatable("msg.kineticarmory.predicate.empty_type")); return; }

        if ("STAGE".equals(typeStr)) {
            String stageVal = currentParamValues.getOrDefault("stage", "");
            if (stageVal.isEmpty()) {
                KineticOverlays.toast(KineticI18n.translatable("msg.kineticarmory.predicate.invalid_stage"));
                return;
            }
        }

        if (!invalidParams.isEmpty()) {
            KineticOverlays.toast(KineticI18n.translatable("msg.kineticarmory.predicate.invalid_seconds"));
            return;
        }

        syncCurrentValues();
        if (isNew) {
            parentList.add(data);
            isNew = false;
        }
    }

    /** Offset of the Save / Back row from the panel top: 10 px below the last field, or below the type field. */
    private static int buttonsOffset(int params) {
        return params == 0 ? TYPE_FIELD_Y + 30 : FIRST_PARAM_Y + (params - 1) * PARAM_STEP + PARAM_FIELD_OFFSET + 30;
    }

    private static int panelHeight(int params) {
        return buttonsOffset(params) + 20 + BOTTOM_PADDING;
    }

    private int panelTop() {
        return (height() - panelHeight(currentSchema.size())) / 2;
    }

    @Override protected void renderBackground(KineticGraphics g, int mx, int my, float pt) {
        int cx = width() / 2;
        int panelY = panelTop();
        KineticTheme.panel(g, cx - PANEL_WIDTH / 2, panelY, PANEL_WIDTH, dynamicPanelHeight);

        int titleLeft = cx - PANEL_WIDTH / 2 + 12 + BACK_WIDTH + 2;
        int titleRight = cx + PANEL_WIDTH / 2 - 12;
        g.scrollingTextCentered(title(), (titleLeft + titleRight) / 2, panelY + TITLE_Y,
                Math.max(0, titleRight - titleLeft), 0xFFFFFF, true);
        g.scrollingText(KineticI18n.translatable("gui.kineticarmory.predicate.type"), cx - FIELD_WIDTH / 2, panelY + TYPE_LABEL_Y, FIELD_WIDTH, 0xAAAAAA, true);
    }

    @Override protected void renderForeground(KineticGraphics g, int mx, int my, float pt) {
        int cx = width() / 2;

        for (int i = 0; i < currentSchema.size(); i++) {
            ConditionTypeUtil.ParamDef def = currentSchema.get(i);
            int y = panelTop() + FIRST_PARAM_Y + i * PARAM_STEP;

            String label = ConditionTypeUtil.getTranslatedParamName(def.key());
            if (isSecondsParam(ConditionTypeUtil.getRawType(typeInput.textValue()), def.key())) {
                label += " " + KineticI18n.translatable("gui.kineticarmory.predicate.unit.seconds").getString();
            }
            g.scrollingText(KineticI18n.translatable("gui.kineticarmory.predicate.param.label", label, def.key()), cx - FIELD_WIDTH / 2 + 2, y, FIELD_WIDTH - 6, 0xFFFFFF, true);

            if (mx >= cx - FIELD_WIDTH / 2 && mx <= cx + FIELD_WIDTH / 2 && my >= y && my <= y + 10) {
                String hint = ConditionTypeUtil.getTranslatedParamHint(def.key());
                if (!hint.isEmpty() && !hint.startsWith("gui.")) {
                    showTooltip(KineticI18n.translatable("gui.kineticarmory.predicate.param.hint", hint));
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
            return NumberType.DECIMAL.format(ticks / 20.0D);
        } catch (NumberFormatException ignored) {
            return allowEmpty ? "" : "0.05";
        }
    }

    private void updateSecondsParam(String key, KineticNumberField box, boolean allowEmpty) {
        String raw = box.textValue().trim();
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
