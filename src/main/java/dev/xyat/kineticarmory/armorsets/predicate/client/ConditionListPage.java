package dev.xyat.kineticarmory.armorsets.predicate.client;

import dev.xyat.kineticcore.api.client.gui.page.KineticPage;
import dev.xyat.kineticcore.api.client.gui.render.KineticGraphics;
import dev.xyat.kineticcore.api.client.gui.theme.KineticTheme;
import dev.xyat.kineticcore.api.client.gui.ui.KineticUi;
import dev.xyat.kineticcore.api.client.gui.widget.*;
import dev.xyat.kineticcore.api.client.gui.widget.list.*;
import dev.xyat.kineticcore.api.text.KineticI18n;

import dev.xyat.kineticarmory.armorsets.predicate.ConditionData;
import dev.xyat.kineticarmory.armorsets.predicate.ConditionTypeUtil;
import dev.xyat.kineticarmory.armorsets.predicate.IConditionOwner;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;

public class ConditionListPage extends KineticPage {
    private static final int GUI_WIDTH = 400;
    private final IConditionOwner owner;
    private final List<ConditionData> conditions;
    private KineticToggleActionList listWidget;

    private KineticTextField minCountInput;

    public ConditionListPage(IConditionOwner owner) {
        super(KineticI18n.translatable("gui.kineticarmory.predicate.list_title"));
        this.owner = owner;
        this.conditions = owner.getConditions();
    }

    private void updateModeUI(KineticButton modeBtn) {
        boolean isMin = "MIN".equals(owner.getMatchMode());
        modeBtn.resizeControlWidth(isMin ? 60 : 90);
        if (minCountInput != null) {
            minCountInput.setControlVisible(isMin);
            if (isMin && minCountInput.textValue().isEmpty()) {
                minCountInput.setTextValue(String.valueOf(Math.max(1, owner.getMinCount())));
            }
        }

        if ("ALL".equals(owner.getMatchMode())) {
            modeBtn.setText(KineticI18n.translatable("gui.kineticarmory.predicate.mode.all"));
        } else if ("MIN".equals(owner.getMatchMode())) {
            modeBtn.setText(KineticI18n.translatable("gui.kineticarmory.predicate.mode.min_btn"));
        } else {
            modeBtn.setText(KineticI18n.translatable("gui.kineticarmory.predicate.mode.any"));
        }
    }

    @Override
    protected void build(KineticUi ui) {
        int cx = width() / 2;
        int cy = height() / 2;
        int guiW = GUI_WIDTH;
        int guiH = 220;
        int y0 = cy - guiH / 2;

        this.listWidget = ui.toggleActionList(cx - guiW / 2, y0 + 30, guiW, guiH - 60, listItems())
                .selected(-1)
                .toggleWidth(35)
                .actionWidth(35)
                .onSelect(index -> {
                    if (index < 0 || index >= conditions.size()) return;
                    listWidget.setSelectedIndex(-1);
                    openChild(new ConditionEditPage(conditions, conditions.get(index), false));
                })
                .onToggle((index, value) -> {
                    if (index < 0 || index >= conditions.size()) return;
                    conditions.get(index).invert = value;
                    refreshList();
                })
                .onAction(index -> {
                    if (index < 0 || index >= conditions.size()) return;
                    conditions.remove((int) index);
                    refreshList();
                })
                .build();

        int btnW = 90;
        int gap = 15;
        int startX = cx - (btnW * 3 + gap * 2) / 2;
        int bottomY = y0 + guiH - 25;

        ui.button(startX, bottomY, btnW).text(KineticI18n.translatable("gui.kineticarmory.predicate.add")).onClick(b -> {
            ConditionData newCond = new ConditionData();
            openChild(new ConditionEditPage(conditions, newCond, true));
        }).build();

        this.minCountInput = ui.textField(startX + btnW + gap + 65, bottomY, 25).firstShownTextAsDefault().build();
        this.minCountInput.setTextValue(String.valueOf(owner.getMinCount()));
        this.minCountInput.onTextChange(s -> {
            try {
                int val = Integer.parseInt(s);
                owner.setMinCount(Math.max(1, val));
            } catch (NumberFormatException ignored) {
            }
        });

        KineticButton modeBtn = ui.button(startX + btnW + gap, bottomY, btnW).text(Component.empty())
                .tooltip(KineticI18n.translatable("gui.kineticarmory.predicate.mode.tooltip"))
                .onClick(b -> {
                    String currentMode = owner.getMatchMode();
                    if ("ANY".equals(currentMode)) {
                        owner.setMatchMode("ALL");
                    } else if ("ALL".equals(currentMode)) {
                        owner.setMatchMode("MIN");
                    } else {
                        owner.setMatchMode("ANY");
                    }
                    updateModeUI(b);
                }).build();
        updateModeUI(modeBtn);

        ui.button(cx - guiW / 2, y0, btnW).text(KineticI18n.translatable("gui.kineticarmory.predicate.back")).onClick(b -> navigateBack()).build();
    }

    private void refreshList() {
        if (listWidget != null) listWidget.setItems(listItems());
    }

    private List<ToggleActionItem> listItems() {
        Component invertTooltip = Component.empty()
                .append(KineticI18n.translatable("gui.kineticarmory.predicate.invert.tooltip.title")).append("\n")
                .append(KineticI18n.translatable("gui.kineticarmory.predicate.invert.tooltip.is")).append("\n")
                .append(KineticI18n.translatable("gui.kineticarmory.predicate.invert.tooltip.not"));
        List<ToggleActionItem> items = new ArrayList<>(conditions.size());
        for (ConditionData data : conditions) {
            String typeStr = (data.type == null || data.type.isEmpty()) ? "empty_type" : data.type.toLowerCase();
            String typeName = ConditionTypeUtil.getTranslatedName(typeStr);
            Component typeLine = data.invert
                    ? KineticI18n.translatable(
                            "gui.kineticarmory.predicate.list.type.inverted",
                            KineticI18n.translatable("gui.kineticarmory.predicate.invert.prefix"),
                            typeName
                    )
                    : KineticI18n.translatable("gui.kineticarmory.predicate.list.type", typeName);
            items.add(new ToggleActionItem(
                    typeLine, null, null, true, false,
                    data.invert,
                    KineticI18n.translatable("gui.kineticarmory.predicate.invert.true"),
                    KineticI18n.translatable("gui.kineticarmory.predicate.invert.false"),
                    invertTooltip, true,
                    KineticI18n.translatable("gui.kineticarmory.predicate.delete"), null, true, true));
        }
        return items;
    }

    @Override
    protected void renderBackground(KineticGraphics g, int mx, int my, float pt) {
        int cx = width() / 2;
        int cy = height() / 2;
        int guiW = GUI_WIDTH;
        int guiH = 220;
        KineticTheme.panel(g, cx - guiW / 2 - 10, cy - guiH / 2 - 10, guiW + 20, guiH + 20);
        int titleLeft = cx - guiW / 2 + 90 + 2;
        int titleRight = cx + guiW / 2 - 2;
        g.scrollingTextCentered(title(), (titleLeft + titleRight) / 2, cy - guiH / 2 + 5,
                Math.max(0, titleRight - titleLeft), 0xFFFFFF, true);
    }

    @Override
    protected void renderForeground(KineticGraphics g, int mx, int my, float pt) {
        if (conditions.isEmpty()) {
            g.scrollingTextCentered(KineticI18n.translatable("gui.kineticarmory.predicate.empty"), width() / 2, height() / 2, GUI_WIDTH - 4, 0xAAAAAA, true);
        }
    }
}
