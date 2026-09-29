package dev.xyat.kineticarmory.armorsets.client.gui;

import dev.xyat.kineticcore.api.text.KineticI18n;
import dev.xyat.kineticcore.api.client.gui.input.MouseInput;
import dev.xyat.kineticcore.api.client.gui.page.KineticPage;
import dev.xyat.kineticcore.api.client.gui.render.KineticGraphics;
import dev.xyat.kineticcore.api.client.gui.text.KineticText;
import dev.xyat.kineticcore.api.client.gui.theme.KineticTheme;
import dev.xyat.kineticcore.api.client.gui.ui.KineticUi;
import dev.xyat.kineticcore.api.client.gui.widget.*;
import dev.xyat.kineticcore.api.client.gui.widget.list.*;

import dev.xyat.kineticarmory.armorsets.client.gui.editor.*;
import dev.xyat.kineticarmory.armorsets.data.ArmorDataConfig;
import dev.xyat.kineticarmory.armorsets.data.ArmorTipGenerator;
import dev.xyat.kineticarmory.armorsets.predicate.ConditionData;
import dev.xyat.kineticarmory.armorsets.predicate.IConditionOwner;
import dev.xyat.kineticarmory.armorsets.predicate.client.ConditionListPage;
import dev.xyat.kineticcore.api.client.input.KineticMouseButtons;
import dev.xyat.kineticcore.api.registry.KineticRegistries;
import dev.xyat.kineticcore.api.resource.KineticResourceIds;
import dev.xyat.kineticcore.api.runtime.KineticClientRuntime;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ArmorDetailPage extends KineticPage {
    
    private final ArmorDataConfig config;
    private DetailListWidget listWidget;

    private int savedScrollOffset = 0;

    public ArmorDetailPage(ArmorDataConfig config) {
        super(KineticI18n.translatable("gui.kineticarmory.armorsets.detail.title"));
        this.config = config;
    }

    @Override
    protected void onTick() {
        if (listWidget != null) {
            savedScrollOffset = listWidget.scrollOffset();
        }
    }

    @Override
    protected void build(KineticUi ui) {
        int cx = width() / 2;
        int padding = 15;

        int btnW = 105;
        int gap = 6;

        int y0 = padding + 25;
        int row2Y = y0 + 24;

        int startX1 = cx - (btnW * 5 + gap * 4) / 2;

        ui().button(startX1, y0, btnW).text(KineticI18n.translatable("gui.kineticarmory.armorsets.detail.add_attr")).onClick(b -> {
            openChild(new AttributeEditor(config, null));
        }).build();

        ui().button(startX1 + btnW + gap, y0, btnW).text(KineticI18n.translatable("gui.kineticarmory.armorsets.detail.add_potion")).onClick(b -> {
            openChild(new PotionEditor(config, null));
        }).build();

        ui().button(startX1 + (btnW + gap) * 2, y0, btnW).text(KineticI18n.translatable("gui.kineticarmory.armorsets.detail.add_immunity")).onClick(b -> {
            openChild(new ImmunityEditor(config, null));
        }).build();

        ui().button(startX1 + (btnW + gap) * 3, y0, btnW).text(KineticI18n.translatable("gui.kineticarmory.armorsets.detail.add_attack")).onClick(b -> {
            openChild(new AttackEffectEditor(config, null));
        }).build();

        ui().button(startX1 + (btnW + gap) * 4, y0, btnW).text(KineticI18n.translatable("gui.kineticarmory.armorsets.detail.add_effect_immunity")).onClick(b -> {
            openChild(new PotionImmunityEditor(config, null));
        }).build();

        int startX2 = cx - (btnW * 3 + gap * 2) / 2;

        ui().button(startX2, row2Y, btnW).text(KineticI18n.translatable("gui.kineticarmory.armorsets.detail.add_dmg_convert")).onClick(b -> {
            openChild(new DamageConversionEditor(config, null));
        }).build();

        ui().button(startX2 + btnW + gap, row2Y, btnW).text(KineticI18n.translatable("gui.kineticarmory.armorsets.detail.add_attack_damage")).onClick(b -> {
            openChild(new AttackDamageEditor(config, null));
        }).build();

        ui().button(startX2 + (btnW + gap) * 2, row2Y, btnW).text(KineticI18n.translatable("gui.kineticarmory.armorsets.detail.add_flight")).onClick(b -> {
            config.allowFlight = true;
            if (config.flightConditions == null) config.flightConditions = new java.util.ArrayList<>();
            IConditionOwner flightOwner = new IConditionOwner() {
                @Override public List<ConditionData> getConditions() { return config.flightConditions; }
                @Override public String getMatchMode() { return config.flightConditionMatchMode == null ? "ANY" : config.flightConditionMatchMode; }
                @Override public void setMatchMode(String mode) { config.flightConditionMatchMode = mode; }
                @Override public int getMinCount() { return Math.max(config.flightConditionMinCount, 1); }
                @Override public void setMinCount(int count) { config.flightConditionMinCount = count; }
            };
            openChild(new ConditionListPage(flightOwner));
        }).build();

        int listTop = row2Y + 30;
        int listBottom = height() - padding - 35;
        int listWidth = (width() - padding * 2) - 20;

        listWidget = ui.add(new DetailListWidget(cx - listWidth / 2, listTop, listWidth, listBottom - listTop));

        ui().button(cx - 50, height() - padding - 25, 100).text(KineticI18n.translatable("gui.kineticarmory.armorsets.back")).onClick(b -> {
            navigateBack();
        }).build();

        refreshList();
        listWidget.setScrollOffset(savedScrollOffset);
    }

    private void refreshList() {
        List<DetailEntry> entries = new java.util.ArrayList<>();

        config.potionEffects.forEach(d -> entries.add(new DetailEntry(ArmorTipGenerator.genPotTip(d), () -> {
            openChild(new PotionEditor(config, d));
        }, () -> {
            config.tips.remove(ArmorTipGenerator.genPotTip(d));
            config.potionEffects.remove(d);
            refreshList();
        })));

        config.attributes.forEach(d -> entries.add(new DetailEntry(ArmorTipGenerator.genAttrTip(d), () -> {
            openChild(new AttributeEditor(config, d));
        }, () -> {
            config.tips.remove(ArmorTipGenerator.genAttrTip(d));
            config.attributes.remove(d);
            refreshList();
        })));

        config.damageImmunities.forEach(d -> entries.add(new DetailEntry(ArmorTipGenerator.genImmTip(d), () -> {
            openChild(new ImmunityEditor(config, d));
        }, () -> {
            config.tips.remove(ArmorTipGenerator.genImmTip(d));
            config.damageImmunities.remove(d);
            refreshList();
        })));

        config.effectImmunities.forEach(d -> entries.add(new DetailEntry(ArmorTipGenerator.genEffImmTip(d), () -> {
            openChild(new PotionImmunityEditor(config, d));
        }, () -> {
            config.tips.remove(ArmorTipGenerator.genEffImmTip(d));
            config.effectImmunities.remove(d);
            refreshList();
        })));

        config.attackEffects.forEach(d -> entries.add(new DetailEntry(ArmorTipGenerator.genAtkTip(d), () -> {
            openChild(new AttackEffectEditor(config, d));
        }, () -> {
            config.tips.remove(ArmorTipGenerator.genAtkTip(d));
            config.attackEffects.remove(d);
            refreshList();
        })));

        config.damageConversions.forEach(d -> entries.add(new DetailEntry(ArmorTipGenerator.genConvTip(d), () -> {
            openChild(new DamageConversionEditor(config, d));
        }, () -> {
            config.tips.remove(ArmorTipGenerator.genConvTip(d));
            config.damageConversions.remove(d);
            refreshList();
        })));

        config.attackDamageMultipliers.forEach(d -> entries.add(new DetailEntry(ArmorTipGenerator.genAtkDmgTip(d), () -> {
            openChild(new AttackDamageEditor(config, d));
        }, () -> {
            config.tips.remove(ArmorTipGenerator.genAtkDmgTip(d));
            config.attackDamageMultipliers.remove(d);
            refreshList();
        })));

        if (config.allowFlight) {
            entries.add(new DetailEntry(ArmorTipGenerator.genFlightTip(config.flightConditions, config.flightConditionMatchMode, config.flightConditionMinCount), () -> {
                IConditionOwner flightOwner = new IConditionOwner() {
                    @Override public List<ConditionData> getConditions() { return config.flightConditions; }
                    @Override public String getMatchMode() { return config.flightConditionMatchMode == null ? "ANY" : config.flightConditionMatchMode; }
                    @Override public void setMatchMode(String mode) { config.flightConditionMatchMode = mode; }
                    @Override public int getMinCount() { return Math.max(config.flightConditionMinCount, 1); }
                    @Override public void setMinCount(int count) { config.flightConditionMinCount = count; }
                };
                openChild(new ConditionListPage(flightOwner));
            }, () -> {
                config.tips.remove(ArmorTipGenerator.genFlightTip(config.flightConditions, config.flightConditionMatchMode, config.flightConditionMinCount));
                config.allowFlight = false;
                if (config.flightConditions != null) config.flightConditions.clear();
                refreshList();
            }));
        }
        listWidget.setItems(entries);
    }

    @Override
    protected void renderBackground(KineticGraphics g, int mx, int my, float pt) {
        int cx = width() / 2;
        int padding = 15;
        int panelWidth = width() - padding * 2;
        int panelHeight = height() - padding * 2;

        KineticTheme.panel(g, cx - panelWidth / 2, padding, panelWidth, panelHeight);
        g.centeredText(title(), cx, padding + 10, 0xFFFFFF, true);

    }

    /** 一行效果：文字可含 [item:..] 图标，右侧内嵌删除按钮 / One effect row with inline icons and a delete button. */
    record DetailEntry(String text, Runnable onEdit, Runnable onDelete) {
    }

    class DetailListWidget extends KineticRowList<DetailEntry> {
        private static final int DELETE_BUTTON_W = 44;
        private static final int DELETE_BUTTON_H = 18;

        DetailListWidget(int x, int y, int width, int height) {
            super(x, y, width, height, 22);
        }

        private int deleteX(int left, int w) {
            return left + w - 16 - DELETE_BUTTON_W;
        }

        @Override
        protected void renderRowBackground(KineticGraphics g, int index, int x, int y, int width, int height,
                                           boolean hovered, boolean selected) {
            KineticTheme.stateSurface(g, x, y, width, 20, KineticTheme.Surface.PANEL_ALT, false, hovered, false);
        }

        @Override
        protected void renderRow(KineticGraphics g, DetailEntry entry, int index, int l, int t, int w, int h,
                                 boolean hv, boolean selected) {
            int deleteX = deleteX(l, w);
            renderTextWithIcons(g, entry.text(), l + 5, t + 6, deleteX - l - 10);
            boolean delHovered = mouseX() >= deleteX && mouseX() < deleteX + DELETE_BUTTON_W
                    && mouseY() >= t + 1 && mouseY() < t + 1 + DELETE_BUTTON_H;
            KineticTheme.button(g, deleteX, t + 1, DELETE_BUTTON_W, DELETE_BUTTON_H,
                    KineticI18n.translatable("gui.kineticarmory.armorsets.delete"), delHovered, true, false);
        }

        @Override
        protected boolean onRowClick(DetailEntry entry, int index, MouseInput input) {
            int t = rowTop(index);
            if (input.y() < t || input.y() >= t + 20) return false;
            if (input.isLeft() && input.inside(deleteX(controlX(), rowsWidth()), t + 1, DELETE_BUTTON_W, DELETE_BUTTON_H)) {
                entry.onDelete().run();
                return true;
            }
            if (input.isLeft()) {
                entry.onEdit().run();
                return true;
            }
            return false;
        }

        private void renderTextWithIcons(KineticGraphics g, String text, int x, int y, int maxWidth) {
            text = text.replaceAll("\\n\\s*(§[0-9a-fk-or])?", "");

            Pattern pattern = Pattern.compile("\\[(item|effect):([^]]+)]");
            Matcher measure = pattern.matcher(text);
            int contentWidth = 0;
            int lastEnd = 0;
            while (measure.find()) {
                contentWidth += KineticText.width(text.substring(lastEnd, measure.start()));
                contentWidth += 12;
                lastEnd = measure.end();
            }
            contentWidth += KineticText.width(text.substring(lastEnd));

            int offset = KineticText.scrollOffset(contentWidth, maxWidth);
            g.scissor(x, y - 2, x + maxWidth, y + KineticText.lineHeight() + 3);
            try {
                Matcher matcher = pattern.matcher(text);
                int currentX = x - offset;
                lastEnd = 0;
                while (matcher.find()) {
                    String plain = text.substring(lastEnd, matcher.start());
                    g.text(plain, currentX, y, 0xFFFFFF, true);
                    currentX += KineticText.width(plain);

                    String type = matcher.group(1);
                    String id = matcher.group(2);
                    if (type.equals("item")) {
                        net.minecraft.resources.ResourceLocation rl = KineticResourceIds.tryParse(id);
                        if (rl != null) {
                            net.minecraft.world.item.Item item = KineticRegistries.items().get(rl);
                            if (item != null && item != net.minecraft.world.item.Items.AIR) {
                                g.push();
                                g.translate(currentX, y - 2);
                                g.scale(0.7f, 0.7f);
                                g.item(new net.minecraft.world.item.ItemStack(item), 0, 0);
                                g.pop();
                            }
                        }
                    }

                    lastEnd = matcher.end();
                    currentX += 12;
                }
                g.text(text.substring(lastEnd), currentX, y, 0xFFFFFF, true);
            } finally {
                g.endScissor();
            }
        }
    }
}
