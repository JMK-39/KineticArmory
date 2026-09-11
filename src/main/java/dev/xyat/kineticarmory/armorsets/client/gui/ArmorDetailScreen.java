package dev.xyat.kineticarmory.armorsets.client.gui;

import dev.xyat.kineticarmory.armorsets.client.gui.editor.*;
import dev.xyat.kineticarmory.armorsets.data.ArmorDataConfig;
import dev.xyat.kineticarmory.armorsets.data.ArmorTipGenerator;
import dev.xyat.kineticarmory.armorsets.predicate.ConditionData;
import dev.xyat.kineticarmory.armorsets.predicate.IConditionOwner;
import dev.xyat.kineticarmory.armorsets.predicate.client.ConditionListScreen;
import dev.xyat.kineticcore.api.client.theme.GuiTheme;
import dev.xyat.kineticcore.api.client.screen.KineticScreen;
import dev.xyat.kineticcore.api.client.widget.KineticWidgets.Scroll;
import dev.xyat.kineticcore.api.client.widget.KineticWidgets.SmoothSelectionList;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.ObjectSelectionList;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ArmorDetailScreen extends KineticScreen {
    private final KineticScreen parent;
    private final ArmorDataConfig config;
    private DetailListWidget listWidget;

    private double savedScrollAmount = 0;

    public ArmorDetailScreen(KineticScreen parent, ArmorDataConfig config) {
        super(Component.translatable("gui.kineticarmory.armorsets.detail.title"));
        this.parent = parent;
        this.config = config;
        useCanvas(
                640f,
                360f,
                6
        );
    }

    @Override
    public void tick() {
        super.tick();
        if (listWidget != null) {
            savedScrollAmount = listWidget.getScrollAmount();
        }
    }

    @Override
    protected void buildUi() {
        int cx = canvasWidth / 2;
        int padding = 15;

        int btnW = 105;
        int gap = 6;

        int y0 = padding + 25;
        int row2Y = y0 + 24;

        int startX1 = cx - (btnW * 5 + gap * 4) / 2;

        this.addRenderableWidget(Button.builder(Component.translatable("gui.kineticarmory.armorsets.detail.add_attr"), b -> {
            if (minecraft != null) minecraft.setScreen(new AttributeEditor(this, config, null));
        }).bounds(startX1, y0, btnW, 20).build());

        this.addRenderableWidget(Button.builder(Component.translatable("gui.kineticarmory.armorsets.detail.add_potion"), b -> {
            if (minecraft != null) minecraft.setScreen(new PotionEditor(this, config, null));
        }).bounds(startX1 + btnW + gap, y0, btnW, 20).build());

        this.addRenderableWidget(Button.builder(Component.translatable("gui.kineticarmory.armorsets.detail.add_immunity"), b -> {
            if (minecraft != null) minecraft.setScreen(new ImmunityEditor(this, config, null));
        }).bounds(startX1 + (btnW + gap) * 2, y0, btnW, 20).build());

        this.addRenderableWidget(Button.builder(Component.translatable("gui.kineticarmory.armorsets.detail.add_attack"), b -> {
            if (minecraft != null) minecraft.setScreen(new AttackEffectEditor(this, config, null));
        }).bounds(startX1 + (btnW + gap) * 3, y0, btnW, 20).build());

        this.addRenderableWidget(Button.builder(Component.translatable("gui.kineticarmory.armorsets.detail.add_effect_immunity"), b -> {
            if (minecraft != null) minecraft.setScreen(new PotionImmunityEditor(this, config, null));
        }).bounds(startX1 + (btnW + gap) * 4, y0, btnW, 20).build());

        int startX2 = cx - (btnW * 3 + gap * 2) / 2;

        this.addRenderableWidget(Button.builder(Component.translatable("gui.kineticarmory.armorsets.detail.add_dmg_convert"), b -> {
            if (minecraft != null) minecraft.setScreen(new DamageConversionEditor(this, config, null));
        }).bounds(startX2, row2Y, btnW, 20).build());

        this.addRenderableWidget(Button.builder(Component.translatable("gui.kineticarmory.armorsets.detail.add_attack_damage"), b -> {
            if (minecraft != null) minecraft.setScreen(new AttackDamageEditor(this, config, null));
        }).bounds(startX2 + btnW + gap, row2Y, btnW, 20).build());

        this.addRenderableWidget(Button.builder(Component.translatable("gui.kineticarmory.armorsets.detail.add_flight"), b -> {
            config.allowFlight = true;
            if (config.flightConditions == null) config.flightConditions = new java.util.ArrayList<>();
            IConditionOwner flightOwner = new IConditionOwner() {
                @Override public List<ConditionData> getConditions() { return config.flightConditions; }
                @Override public String getMatchMode() { return config.flightConditionMatchMode == null ? "ANY" : config.flightConditionMatchMode; }
                @Override public void setMatchMode(String mode) { config.flightConditionMatchMode = mode; }
                @Override public int getMinCount() { return Math.max(config.flightConditionMinCount, 1); }
                @Override public void setMinCount(int count) { config.flightConditionMinCount = count; }
            };
            if (minecraft != null) minecraft.setScreen(new ConditionListScreen(this, flightOwner));
        }).bounds(startX2 + (btnW + gap) * 2, row2Y, btnW, 20).build());

        int listTop = row2Y + 30;
        int listBottom = canvasHeight - padding - 35;
        int listWidth = (canvasWidth - padding * 2) - 20;

        listWidget = new DetailListWidget(this.minecraft, listWidth, listBottom - listTop, listTop, 22);
        listWidget.setLeftPos(cx - listWidth / 2);
        this.addWidget(listWidget);

        addRenderableWidget(Button.builder(Component.translatable("gui.kineticarmory.armorsets.back"), b -> {
            if (minecraft != null) minecraft.setScreen(parent);
        }).bounds(cx - 50, canvasHeight - padding - 25, 100, 20).build());

        refreshList();
        listWidget.setScrollAmount(savedScrollAmount);
    }

    private void refreshList() {
        listWidget.clearAllEntries();

        config.potionEffects.forEach(d -> listWidget.addDetailEntry(new DetailEntry(ArmorTipGenerator.genPotTip(d), () -> {
            if (minecraft != null) minecraft.setScreen(new PotionEditor(this, config, d));
        }, () -> {
            config.tips.remove(ArmorTipGenerator.genPotTip(d));
            config.potionEffects.remove(d);
            refreshList();
        })));

        config.attributes.forEach(d -> listWidget.addDetailEntry(new DetailEntry(ArmorTipGenerator.genAttrTip(d), () -> {
            if (minecraft != null) minecraft.setScreen(new AttributeEditor(this, config, d));
        }, () -> {
            config.tips.remove(ArmorTipGenerator.genAttrTip(d));
            config.attributes.remove(d);
            refreshList();
        })));

        config.damageImmunities.forEach(d -> listWidget.addDetailEntry(new DetailEntry(ArmorTipGenerator.genImmTip(d), () -> {
            if (minecraft != null) minecraft.setScreen(new ImmunityEditor(this, config, d));
        }, () -> {
            config.tips.remove(ArmorTipGenerator.genImmTip(d));
            config.damageImmunities.remove(d);
            refreshList();
        })));

        config.effectImmunities.forEach(d -> listWidget.addDetailEntry(new DetailEntry(ArmorTipGenerator.genEffImmTip(d), () -> {
            if (minecraft != null) minecraft.setScreen(new PotionImmunityEditor(this, config, d));
        }, () -> {
            config.tips.remove(ArmorTipGenerator.genEffImmTip(d));
            config.effectImmunities.remove(d);
            refreshList();
        })));

        config.attackEffects.forEach(d -> listWidget.addDetailEntry(new DetailEntry(ArmorTipGenerator.genAtkTip(d), () -> {
            if (minecraft != null) minecraft.setScreen(new AttackEffectEditor(this, config, d));
        }, () -> {
            config.tips.remove(ArmorTipGenerator.genAtkTip(d));
            config.attackEffects.remove(d);
            refreshList();
        })));

        config.damageConversions.forEach(d -> listWidget.addDetailEntry(new DetailEntry(ArmorTipGenerator.genConvTip(d), () -> {
            if (minecraft != null) minecraft.setScreen(new DamageConversionEditor(this, config, d));
        }, () -> {
            config.tips.remove(ArmorTipGenerator.genConvTip(d));
            config.damageConversions.remove(d);
            refreshList();
        })));

        config.attackDamageMultipliers.forEach(d -> listWidget.addDetailEntry(new DetailEntry(ArmorTipGenerator.genAtkDmgTip(d), () -> {
            if (minecraft != null) minecraft.setScreen(new AttackDamageEditor(this, config, d));
        }, () -> {
            config.tips.remove(ArmorTipGenerator.genAtkDmgTip(d));
            config.attackDamageMultipliers.remove(d);
            refreshList();
        })));

        if (config.allowFlight) {
            listWidget.addDetailEntry(new DetailEntry(ArmorTipGenerator.genFlightTip(config.flightConditions, config.flightConditionMatchMode, config.flightConditionMinCount), () -> {
                IConditionOwner flightOwner = new IConditionOwner() {
                    @Override public List<ConditionData> getConditions() { return config.flightConditions; }
                    @Override public String getMatchMode() { return config.flightConditionMatchMode == null ? "ANY" : config.flightConditionMatchMode; }
                    @Override public void setMatchMode(String mode) { config.flightConditionMatchMode = mode; }
                    @Override public int getMinCount() { return Math.max(config.flightConditionMinCount, 1); }
                    @Override public void setMinCount(int count) { config.flightConditionMinCount = count; }
                };
                if (minecraft != null) minecraft.setScreen(new ConditionListScreen(this, flightOwner));
            }, () -> {
                config.tips.remove(ArmorTipGenerator.genFlightTip(config.flightConditions, config.flightConditionMatchMode, config.flightConditionMinCount));
                config.allowFlight = false;
                if (config.flightConditions != null) config.flightConditions.clear();
                refreshList();
            }));
        }
    }

    @Override
    protected void renderCanvasBackground(@NotNull GuiGraphics g, int mx, int my, float pt) {
        int cx = canvasWidth / 2;
        int padding = 15;
        int panelWidth = canvasWidth - padding * 2;
        int panelHeight = canvasHeight - padding * 2;

        GuiTheme.panel(g, cx - panelWidth / 2, padding, panelWidth, panelHeight);
        g.drawCenteredString(font, title, cx, padding + 10, 0xFFFFFF);

        renderScaledList(listWidget, g, mx, my, pt);
    }

    public static class DetailListWidget extends SmoothSelectionList<DetailEntry> {
        private final int listTop;
        private final int listBottom;

        public DetailListWidget(Minecraft mc, int w, int h, int t, int itemH) {
            super(mc, w, h, t, t + h, itemH);
            this.listTop = t;
            this.listBottom = t + h;
            this.setRenderBackground(false);
            this.setRenderHeader(false, 0);
            this.setRenderTopAndBottom(false);
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

        public void clearAllEntries() {
            this.clearEntries();
        }

        public void addDetailEntry(DetailEntry entry) {
            this.addEntry(entry);
        }

        @Override public int getRowWidth() { return this.width - 20; }
        @Override protected int getScrollbarPosition() { return this.getLeft() + this.width - 6; }
    }

    public static class DetailEntry extends ObjectSelectionList.Entry<DetailEntry> {
        private static final int DELETE_BUTTON_W = 44;
        private static final int DELETE_BUTTON_H = 18;

        private final String text;
        private final Runnable onEdit;
        private final Button deleteButton;

        private int lastT;

        public DetailEntry(String text, Runnable onEdit, Runnable onDelete) {
            this.text = text;
            this.onEdit = onEdit;
            this.deleteButton = Button.builder(Component.translatable("gui.kineticarmory.armorsets.delete"), b -> onDelete.run())
                    .bounds(0, 0, DELETE_BUTTON_W, DELETE_BUTTON_H)
                    .build();
        }

        @Override
        public void render(@NotNull GuiGraphics g, int index, int t, int l, int w, int h, int mx, int my, boolean hv, float pt) {
            this.lastT = t;
            int bgColor = hv ? 0x88777777 : ((index % 2 == 0) ? 0x88444444 : 0x88222222);

            g.fill(l, t, l + w, t + 20, bgColor);
            g.renderOutline(l, t, w, 20, 0xFF555555);

            int deleteX = l + w - 16 - DELETE_BUTTON_W;
            renderTextWithIcons(g, Minecraft.getInstance().font, text, l + 5, t + 6, deleteX - l - 10);

            deleteButton.setX(deleteX);
            deleteButton.setY(t + 1);
            deleteButton.render(g, mx, my, pt);
        }

        private void renderTextWithIcons(GuiGraphics g, Font font, String text, int x, int y, int maxWidth) {
            text = text.replaceAll("\\n\\s*(§[0-9a-fk-or])?", "");

            Pattern pattern = Pattern.compile("\\[(item|effect):([^]]+)]");
            Matcher matcher = pattern.matcher(text);
            int currentX = x;
            int lastEnd = 0;
            while (matcher.find()) {
                String plain = text.substring(lastEnd, matcher.start());
                String clipped = font.plainSubstrByWidth(plain, Math.max(0, x + maxWidth - currentX));
                g.drawString(font, clipped, currentX, y, 0xFFFFFF);
                currentX += font.width(clipped);
                if (clipped.length() < plain.length() || currentX + 12 > x + maxWidth) return;

                String type = matcher.group(1);
                String id = matcher.group(2);
                if (type.equals("item")) {
                    net.minecraft.resources.ResourceLocation rl = net.minecraft.resources.ResourceLocation.tryParse(id);
                    if (rl != null) {
                        net.minecraft.world.item.Item item = net.minecraftforge.registries.ForgeRegistries.ITEMS.getValue(rl);
                        if (item != null && item != net.minecraft.world.item.Items.AIR) {
                            g.pose().pushPose();
                            g.pose().translate(currentX, y - 2, 0);
                            g.pose().scale(0.7f, 0.7f, 1.0f);
                            g.renderItem(new net.minecraft.world.item.ItemStack(item), 0, 0);
                            g.pose().popPose();
                        }
                    }
                }

                lastEnd = matcher.end();
                currentX += 12;
            }
            String tail = text.substring(lastEnd);
            g.drawString(font, font.plainSubstrByWidth(tail, Math.max(0, x + maxWidth - currentX)), currentX, y, 0xFFFFFF);
        }

        @Override
        public boolean mouseClicked(double mx, double my, int btn) {
            if (my < lastT || my >= lastT + 20) return false;
            if (deleteButton.mouseClicked(mx, my, btn)) return true;
            if (btn == 0) {
                onEdit.run();
                return true;
            }
            return false;
        }

        @Override public @NotNull Component getNarration() { return Component.empty(); }
    }
}