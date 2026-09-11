package dev.xyat.kineticarmory.armorsets.predicate.client;

import dev.xyat.kineticcore.api.client.overlay.GuiOverlay;
import dev.xyat.kineticarmory.util.ColorText;
import dev.xyat.kineticarmory.armorsets.predicate.ConditionData;
import dev.xyat.kineticarmory.armorsets.predicate.ConditionTypeUtil;
import dev.xyat.kineticarmory.armorsets.predicate.IConditionOwner;
import dev.xyat.kineticcore.api.client.theme.GuiTheme;
import dev.xyat.kineticcore.api.client.screen.KineticScreen;
import dev.xyat.kineticcore.api.client.widget.KineticWidgets.Scroll;
import dev.xyat.kineticcore.api.client.widget.KineticWidgets.SmoothSelectionList;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.ObjectSelectionList;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

public class ConditionListScreen extends KineticScreen {
    private final KineticScreen parent;
    private final IConditionOwner owner;
    private final List<ConditionData> conditions;
    protected ConditionListWidget listWidget;

    private EditBox minCountInput;

    private List<Component> delayedTooltip = null;
    private int delayedTooltipX = 0;
    private int delayedTooltipY = 0;

    public ConditionListScreen(KineticScreen parent, IConditionOwner owner) {
        super(ColorText.translatable("gui.kineticarmory.predicate.list_title"));
        this.parent = parent;
        this.owner = owner;
        this.conditions = owner.getConditions();
        useCanvas(
                640f,
                360f,
                6
        );
    }

    private void updateModeUI(Button modeBtn) {
        boolean isMin = "MIN".equals(owner.getMatchMode());
        modeBtn.setWidth(isMin ? 60 : 90);
        if (minCountInput != null) {
            minCountInput.visible = isMin;
            if (isMin && minCountInput.getValue().isEmpty()) {
                minCountInput.setValue(String.valueOf(Math.max(1, owner.getMinCount())));
            }
        }

        if ("ALL".equals(owner.getMatchMode())) {
            modeBtn.setMessage(ColorText.translatable("gui.kineticarmory.predicate.mode.all"));
        } else if ("MIN".equals(owner.getMatchMode())) {
            modeBtn.setMessage(ColorText.translatable("gui.kineticarmory.predicate.mode.min_btn"));
        } else {
            modeBtn.setMessage(ColorText.translatable("gui.kineticarmory.predicate.mode.any"));
        }
    }

    @Override protected void buildUi() {
        int cx = canvasWidth / 2; int cy = canvasHeight / 2; int guiW = 400; int guiH = 220; int y0 = cy - guiH / 2;

        this.listWidget = new ConditionListWidget(this.minecraft, guiW, guiH - 60, y0 + 30, y0 + guiH - 30, 24);
        this.listWidget.setLeftPos(cx - guiW / 2); this.addWidget(listWidget);

        int btnW = 90;
        int gap = 15;
        int startX = cx - (btnW * 3 + gap * 2) / 2;
        int bottomY = y0 + guiH - 25;

        this.addRenderableWidget(Button.builder(ColorText.translatable("gui.kineticarmory.predicate.add"), b -> {
            ConditionData newCond = new ConditionData();
            if (minecraft != null) minecraft.setScreen(new ConditionEditScreen(this, conditions, newCond, true));
        }).bounds(startX, bottomY, btnW, 20).build());

        this.minCountInput = new EditBox(font, startX + btnW + gap + 65, bottomY, 25, 20, Component.empty());
        this.minCountInput.setValue(String.valueOf(owner.getMinCount()));
        this.minCountInput.setResponder(s -> {
            try {
                int val = Integer.parseInt(s);
                owner.setMinCount(Math.max(1, val));
            } catch (NumberFormatException ignored) {}
        });
        this.addRenderableWidget(minCountInput);

        Button modeBtn = Button.builder(Component.empty(), b -> {
            String currentMode = owner.getMatchMode();
            if ("ANY".equals(currentMode)) { owner.setMatchMode("ALL"); }
            else if ("ALL".equals(currentMode)) { owner.setMatchMode("MIN"); }
            else { owner.setMatchMode("ANY"); }
            updateModeUI(b);
        }).bounds(startX + btnW + gap, bottomY, btnW, 20).tooltip(Tooltip.create(ColorText.translatable("gui.kineticarmory.predicate.mode.tooltip"))).build();
        this.addRenderableWidget(modeBtn);

        updateModeUI(modeBtn);

        this.addRenderableWidget(Button.builder(ColorText.translatable("gui.kineticarmory.predicate.back"), b -> {
            if (minecraft != null) minecraft.setScreen(parent);
        }).bounds(startX + (btnW + gap) * 2, bottomY, btnW, 20).build());
    }

    @Override protected void renderCanvasBackground(@NotNull GuiGraphics g, int mx, int my, float pt) {
        int cx = canvasWidth / 2; int cy = canvasHeight / 2; int guiW = 400; int guiH = 220;
        GuiTheme.panel(g, cx - guiW / 2 - 10, cy - guiH / 2 - 10, guiW + 20, guiH + 20);
        g.drawCenteredString(font, title, cx, cy - guiH / 2 + 5, 0xFFFFFF);
        renderScaledList(listWidget, g, mx, my, pt);
        if (conditions.isEmpty()) g.drawCenteredString(font, ColorText.translatable("gui.kineticarmory.predicate.empty"), cx, cy, 0xAAAAAA);
    }

    @Override protected void renderCanvasForeground(@NotNull GuiGraphics g, int mx, int my, float pt) {
        if (delayedTooltip != null && !delayedTooltip.isEmpty()) {
            GuiOverlay.requestTooltip(delayedTooltip, delayedTooltipX, delayedTooltipY);
            delayedTooltip = null;
        }
    }

    @Override protected boolean canvasMouseClicked(double mx, double my, int btn) {
        if (minCountInput != null && minCountInput.visible) {
            if (minCountInput.mouseClicked(mx, my, btn)) {
                this.setFocused(minCountInput);
                return true;
            } else if (minCountInput.isFocused() && !minCountInput.isMouseOver(mx, my)) {
                minCountInput.setFocused(false);
                this.setFocused(null);
            }
        }
        return super.canvasMouseClicked(mx, my, btn);
    }

    @Override public boolean keyPressed(int k, int s, int m) {
        if (minCountInput != null && minCountInput.visible && minCountInput.isFocused()) {
            if (minCountInput.keyPressed(k, s, m)) return true;
        }
        return super.keyPressed(k, s, m);
    }

    @Override public boolean charTyped(char c, int m) {
        if (minCountInput != null && minCountInput.visible && minCountInput.isFocused()) {
            if (minCountInput.charTyped(c, m)) return true;
        }
        return super.charTyped(c, m);
    }

    class ConditionListWidget extends SmoothSelectionList<ConditionListWidget.Entry> {
        private final int listTop;
        private final int listBottom;

        public ConditionListWidget(Minecraft mc, int w, int h, int t, int b, int ih) {
            super(mc, w, h, t, b, ih);
            this.listTop = t;
            this.listBottom = b;
            setRenderBackground(false);
            setRenderTopAndBottom(false);
            refresh();
        }

        @Override
        public void render(@NotNull GuiGraphics g, int mx, int my, float pt) {
            super.render(g, mx, my, pt);
            if (this.getMaxScroll() > 0) {
                int height = Math.max(1, listBottom - listTop);
                int thumbH = Math.max(20, (int) ((float) height * height / this.getMaxPosition()));
                Scroll.renderScrollbar(
                        g,
                        mx,
                        my,
                        this.getScrollbarPosition() + 2,
                        listTop,
                        4,
                        height,
                        thumbH,
                        (int) Math.ceil(this.getMaxScroll()),
                        this.getScrollAmount(),
                        false
                );
            }
        }

        public void refresh() { clearEntries(); for (ConditionData cond : conditions) addEntry(new Entry(cond)); }
        @Override public int getRowWidth() { return this.width; }
        @Override protected int getScrollbarPosition() { return this.x0 + this.width - 6; }

        class Entry extends ObjectSelectionList.Entry<Entry> {
            private final ConditionData data;
            public Entry(ConditionData data) { this.data = data; }

            @Override public void render(GuiGraphics g, int index, int t, int l, int w, int h, int mx, int my, boolean hv, float pt) {
                int bgColor = hv ? 0x88777777 : ((index % 2 == 0) ? 0x88444444 : 0x88222222);
                g.fill(l, t, l + w, t + h - 2, bgColor); g.renderOutline(l, t, w, h - 2, 0xFF555555);

                String typeStr = (data.type == null || data.type.isEmpty()) ? "empty_type" : data.type.toLowerCase();
                String typeName = ConditionTypeUtil.getTranslatedName(typeStr);
                Component typeLine = data.invert
                        ? ColorText.translatable(
                                "gui.kineticarmory.predicate.list.type.inverted",
                                ColorText.translatable("gui.kineticarmory.predicate.invert.prefix"),
                                typeName
                        )
                        : ColorText.translatable("gui.kineticarmory.predicate.list.type", typeName);
                g.drawString(Minecraft.getInstance().font, typeLine, l + 5, t + 5, 0xFFFFFF);

                int delW = 35; int delX = l + w - delW - 5;
                boolean delHover = mx >= delX && mx < delX + delW && my >= t + 2 && my < t + h - 4;
                g.fill(delX, t + 2, delX + delW, t + h - 4, delHover ? 0xFFFF5555 : 0xFFAA0000);
                g.drawCenteredString(Minecraft.getInstance().font, ColorText.translatable("gui.kineticarmory.predicate.delete"), delX + delW / 2, t + 6, 0xFFFFFF);

                int invW = 35; int invX = delX - invW - 5;
                boolean invHover = mx >= invX && mx < invX + invW && my >= t + 2 && my < t + h - 4;
                g.fill(invX, t + 2, invX + invW, t + h - 4, invHover ? (data.invert ? 0xFF884444 : 0xFF448844) : (data.invert ? 0xFF552222 : 0xFF225522));
                String invStr = data.invert ? ColorText.translatable("gui.kineticarmory.predicate.invert.true").getString() : ColorText.translatable("gui.kineticarmory.predicate.invert.false").getString();
                g.drawCenteredString(Minecraft.getInstance().font, invStr, invX + invW / 2, t + 6, data.invert ? 0xFFFF5555 : 0xFF55FF55);

                if (invHover) {
                    List<Component> tooltip = new ArrayList<>();
                    tooltip.add(ColorText.translatable("gui.kineticarmory.predicate.invert.tooltip.title"));
                    tooltip.add(ColorText.translatable("gui.kineticarmory.predicate.invert.tooltip.is"));
                    tooltip.add(ColorText.translatable("gui.kineticarmory.predicate.invert.tooltip.not"));
                    ConditionListScreen.this.delayedTooltip = tooltip;
                    ConditionListScreen.this.delayedTooltipX = mx;
                    ConditionListScreen.this.delayedTooltipY = my;
                }
            }
            @Override public boolean mouseClicked(double mx, double my, int btn) {
                if (btn == 0) {
                    int w = getRowWidth();
                    int delW = 35; int delX = getLeft() + w - delW - 5;
                    int invW = 35; int invX = delX - invW - 5;

                    if (mx >= delX && mx < delX + delW) { conditions.remove(data); refresh(); }
                    else if (mx >= invX && mx < invX + invW) { data.invert = !data.invert; refresh(); }
                    else {
                        minecraft.setScreen(new ConditionEditScreen(ConditionListScreen.this, conditions, data, false));
                    }
                    return true;
                } return false;
            }
            @Override public @NotNull Component getNarration() { return Component.empty(); }
        }
    }
}
