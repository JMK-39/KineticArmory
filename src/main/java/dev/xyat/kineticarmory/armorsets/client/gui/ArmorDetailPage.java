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
import dev.xyat.kineticcore.api.registry.KineticRegistries;
import dev.xyat.kineticcore.api.resource.KineticResourceIds;
import net.minecraft.world.effect.MobEffect;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ArmorDetailPage extends KineticPage {
    // Keep self-drawn text clear of panel edges and the row's delete button.
    private static final int TEXT_GAP = 4;
    
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

        ui().button(padding + 4, padding + 3, 100).text(KineticI18n.translatable("gui.kineticarmory.armorsets.back")).onClick(b -> {
            navigateBack();
        }).build();

        refreshList();
        listWidget.setScrollOffset(savedScrollOffset);
    }

    private void refreshList() {
        List<DetailEntry> entries = new java.util.ArrayList<>();

        config.potionEffects.forEach(d -> entries.add(DetailEntry.potion(ArmorTipGenerator.singleLine(() -> ArmorTipGenerator.genPotTip(d)), d.effectId, () -> {
            openChild(new PotionEditor(config, d));
        }, () -> {
            config.tips.remove(ArmorTipGenerator.genPotTip(d));
            config.potionEffects.remove(d);
            refreshList();
        })));

        config.attributes.forEach(d -> entries.add(new DetailEntry(ArmorTipGenerator.singleLine(() -> ArmorTipGenerator.genAttrTip(d)), () -> {
            openChild(new AttributeEditor(config, d));
        }, () -> {
            config.tips.remove(ArmorTipGenerator.genAttrTip(d));
            config.attributes.remove(d);
            refreshList();
        })));

        config.damageImmunities.forEach(d -> entries.add(new DetailEntry(ArmorTipGenerator.singleLine(() -> ArmorTipGenerator.genImmTip(d)), () -> {
            openChild(new ImmunityEditor(config, d));
        }, () -> {
            config.tips.remove(ArmorTipGenerator.genImmTip(d));
            config.damageImmunities.remove(d);
            refreshList();
        })));

        config.effectImmunities.forEach(d -> entries.add(DetailEntry.potion(ArmorTipGenerator.singleLine(() -> ArmorTipGenerator.genEffImmTip(d)), d.effectId, () -> {
            openChild(new PotionImmunityEditor(config, d));
        }, () -> {
            config.tips.remove(ArmorTipGenerator.genEffImmTip(d));
            config.effectImmunities.remove(d);
            refreshList();
        })));

        config.attackEffects.forEach(d -> entries.add(DetailEntry.potion(ArmorTipGenerator.singleLine(() -> ArmorTipGenerator.genAtkTip(d)), d.effectId, () -> {
            openChild(new AttackEffectEditor(config, d));
        }, () -> {
            config.tips.remove(ArmorTipGenerator.genAtkTip(d));
            config.attackEffects.remove(d);
            refreshList();
        })));

        config.damageConversions.forEach(d -> entries.add(new DetailEntry(ArmorTipGenerator.singleLine(() -> ArmorTipGenerator.genConvTip(d)), () -> {
            openChild(new DamageConversionEditor(config, d));
        }, () -> {
            config.tips.remove(ArmorTipGenerator.genConvTip(d));
            config.damageConversions.remove(d);
            refreshList();
        })));

        config.attackDamageMultipliers.forEach(d -> entries.add(new DetailEntry(ArmorTipGenerator.singleLine(() -> ArmorTipGenerator.genAtkDmgTip(d)), () -> {
            openChild(new AttackDamageEditor(config, d));
        }, () -> {
            config.tips.remove(ArmorTipGenerator.genAtkDmgTip(d));
            config.attackDamageMultipliers.remove(d);
            refreshList();
        })));

        if (config.allowFlight) {
            entries.add(new DetailEntry(ArmorTipGenerator.singleLine(() -> ArmorTipGenerator.genFlightTip(config.flightConditions, config.flightConditionMatchMode, config.flightConditionMinCount)), () -> {
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
        int titleLeft = padding + 4 + 100 + 2;
        int titleRight = width() - padding - TEXT_GAP;
        g.scrollingTextCentered(title(), (titleLeft + titleRight) / 2, padding + 10,
                Math.max(0, titleRight - titleLeft), 0xFFFFFF, true);

    }

    /** 一行效果：文字可含 [item:..] 图标，右侧内嵌删除按钮 / One effect row with inline icons and a delete button. */
    record DetailEntry(String text, Runnable onEdit, Runnable onDelete, MobEffect effect) {
        DetailEntry(String text, Runnable onEdit, Runnable onDelete) {
            this(text, onEdit, onDelete, null);
        }

        static DetailEntry potion(String text, String effectId, Runnable onEdit, Runnable onDelete) {
            var id = effectId == null ? null : KineticResourceIds.tryParse(effectId);
            var effect = id == null ? null : KineticRegistries.mobEffects().get(id);
            return new DetailEntry(effect == null ? text : text + " §7(" + id + ")", onEdit, onDelete, effect);
        }
    }

    class DetailListWidget extends KineticRowList<DetailEntry> {
        private static final int DELETE_BUTTON_W = 44;
        private static final int DELETE_BUTTON_H = 16;
        private static final int ROW_TEXT_INSET = 5;
        // Row frames are ROW_FRAME_H tall; icon, text and Delete button are centred, 3 px clear of the frame lines.
        private static final int ROW_FRAME_H = 22;
        private static final int BUTTON_Y = (ROW_FRAME_H - DELETE_BUTTON_H) / 2;

        DetailListWidget(int x, int y, int width, int height) {
            super(x, y, width, height, ROW_FRAME_H + 2);
        }

        private int deleteX(int left, int w) {
            return left + w - 16 - DELETE_BUTTON_W;
        }

        @Override
        protected void renderRowBackground(KineticGraphics g, int index, int x, int y, int width, int height,
                                           boolean hovered, boolean selected) {
            KineticTheme.stateSurface(g, x, y, width, ROW_FRAME_H, KineticTheme.Surface.PANEL_ALT, false, hovered, false);
        }

        @Override
        protected void renderRow(KineticGraphics g, DetailEntry entry, int index, int l, int t, int w, int h,
                                 boolean hv, boolean selected) {
            int deleteX = deleteX(l, w);
            int textX = l + ROW_TEXT_INSET;
            if (entry.effect() != null) {
                g.effectIcon(entry.effect(), textX, t + (ROW_FRAME_H - 16) / 2, 16);
                textX += 20;
            }
            renderTextWithIcons(g, entry.text(), textX, t + (ROW_FRAME_H - 8) / 2,
                    Math.max(0, deleteX - textX - TEXT_GAP));
            boolean delHovered = mouseX() >= deleteX && mouseX() < deleteX + DELETE_BUTTON_W
                    && mouseY() >= t + BUTTON_Y && mouseY() < t + BUTTON_Y + DELETE_BUTTON_H;
            KineticTheme.button(g, deleteX, t + BUTTON_Y, DELETE_BUTTON_W, DELETE_BUTTON_H,
                    KineticI18n.translatable("gui.kineticarmory.armorsets.delete"), delHovered, true, false);
        }

        @Override
        protected boolean onRowClick(DetailEntry entry, int index, MouseInput input) {
            int t = rowTop(index);
            if (input.y() < t || input.y() >= t + ROW_FRAME_H) return false;
            if (input.isLeft() && input.inside(deleteX(controlX(), rowsWidth()), t + BUTTON_Y, DELETE_BUTTON_W, DELETE_BUTTON_H)) {
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
                contentWidth += KineticText.width(ArmorTipEditorPage.formattingBefore(text, lastEnd) + text.substring(lastEnd, measure.start()));
                contentWidth += measure.group(1).equals("item") ? 20 : 12;
                lastEnd = measure.end();
            }
            contentWidth += KineticText.width(ArmorTipEditorPage.formattingBefore(text, lastEnd) + text.substring(lastEnd));

            int offset = KineticText.scrollOffset(contentWidth, maxWidth);
            g.scissor(x, y - 4, x + maxWidth, y + 12);
            try {
                Matcher matcher = pattern.matcher(text);
                int currentX = x - offset;
                lastEnd = 0;
                while (matcher.find()) {
                    String plain = ArmorTipEditorPage.formattingBefore(text, lastEnd) + text.substring(lastEnd, matcher.start());
                    g.text(plain, currentX, y, 0xFFFFFF, true);
                    currentX += KineticText.width(plain);

                    String type = matcher.group(1);
                    String id = matcher.group(2);
                    if (type.equals("item")) {
                        net.minecraft.resources.ResourceLocation rl = KineticResourceIds.tryParse(id);
                        if (rl != null) {
                            net.minecraft.world.item.Item item = KineticRegistries.items().get(rl);
                            if (item != null && item != net.minecraft.world.item.Items.AIR) {
                                KineticTheme.itemSlot(g, currentX + 2, y - 4, 16, false);
                                KineticTheme.item(g, new net.minecraft.world.item.ItemStack(item), currentX + 2, y - 4, 16, 0.625F, false);
                            }
                        }
                    }

                    lastEnd = matcher.end();
                    currentX += type.equals("item") ? 20 : 12;
                }
                g.text(ArmorTipEditorPage.formattingBefore(text, lastEnd) + text.substring(lastEnd), currentX, y, 0xFFFFFF, true);
            } finally {
                g.endScissor();
            }
        }
    }
}
