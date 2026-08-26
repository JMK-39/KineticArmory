package dev.xyat.kineticarmory.util;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

import java.util.HashMap;
import java.util.Map;

public final class ColorText {
    private static final Map<String, ChatFormatting[][]> ARG_STYLES = new HashMap<>();

    static {
        ARG_STYLES.put("gui.kineticarmory.armorsets.btn_piece_bonuses", new ChatFormatting[][]{new ChatFormatting[]{ChatFormatting.GREEN}});
        ARG_STYLES.put("gui.kineticarmory.armorsets.editor.conditions", new ChatFormatting[][]{new ChatFormatting[]{ChatFormatting.GOLD}});
        ARG_STYLES.put("gui.kineticarmory.armorsets.effect.damage_multiplier", new ChatFormatting[][]{new ChatFormatting[]{ChatFormatting.GOLD}});
        ARG_STYLES.put("gui.kineticarmory.armorsets.entity_filter.entity.tooltip.id", new ChatFormatting[][]{new ChatFormatting[]{ChatFormatting.AQUA}});
        ARG_STYLES.put("gui.kineticarmory.armorsets.entity_filter.entity.tooltip.name", new ChatFormatting[][]{new ChatFormatting[]{ChatFormatting.GOLD, ChatFormatting.BOLD}});
        ARG_STYLES.put("gui.kineticarmory.armorsets.entity_filter.entity.tooltip.zoom", new ChatFormatting[][]{new ChatFormatting[]{ChatFormatting.GREEN, ChatFormatting.BOLD}});
        ARG_STYLES.put("gui.kineticarmory.armorsets.entity_filter.global_status", new ChatFormatting[][]{new ChatFormatting[]{ChatFormatting.AQUA, ChatFormatting.BOLD}});
        ARG_STYLES.put("gui.kineticarmory.armorsets.entity_filter.left_blacklist", new ChatFormatting[][]{new ChatFormatting[]{ChatFormatting.RED, ChatFormatting.BOLD}});
        ARG_STYLES.put("gui.kineticarmory.armorsets.entity_filter.left_set", new ChatFormatting[][]{new ChatFormatting[]{ChatFormatting.GREEN, ChatFormatting.BOLD}});
        ARG_STYLES.put("gui.kineticarmory.armorsets.entity_filter.left_whitelist", new ChatFormatting[][]{new ChatFormatting[]{ChatFormatting.GREEN, ChatFormatting.BOLD}});
        ARG_STYLES.put("gui.kineticarmory.armorsets.entity_filter.main_button.tooltip", new ChatFormatting[][]{new ChatFormatting[]{ChatFormatting.GOLD, ChatFormatting.BOLD}});
        ARG_STYLES.put("gui.kineticarmory.armorsets.entity_filter.right_available", new ChatFormatting[][]{new ChatFormatting[]{ChatFormatting.YELLOW, ChatFormatting.BOLD}});
        ARG_STYLES.put("gui.kineticarmory.armorsets.entity_filter.set_status", new ChatFormatting[][]{new ChatFormatting[]{ChatFormatting.AQUA, ChatFormatting.BOLD}});
        ARG_STYLES.put("gui.kineticarmory.armorsets.info.summary", new ChatFormatting[][]{new ChatFormatting[]{ChatFormatting.GOLD}, new ChatFormatting[]{ChatFormatting.GOLD}, new ChatFormatting[]{ChatFormatting.GOLD}, new ChatFormatting[]{ChatFormatting.GOLD}, new ChatFormatting[]{ChatFormatting.GOLD}, new ChatFormatting[]{ChatFormatting.GOLD}, new ChatFormatting[]{ChatFormatting.GOLD}});
        ARG_STYLES.put("gui.kineticarmory.armorsets.info.summary_detailed", new ChatFormatting[][]{new ChatFormatting[]{ChatFormatting.GOLD}, new ChatFormatting[]{ChatFormatting.GOLD}, new ChatFormatting[]{ChatFormatting.GOLD}, new ChatFormatting[]{ChatFormatting.GOLD}, new ChatFormatting[]{ChatFormatting.GOLD}, new ChatFormatting[]{ChatFormatting.GOLD}, new ChatFormatting[]{ChatFormatting.GOLD}, new ChatFormatting[]{ChatFormatting.GOLD}, new ChatFormatting[]{ChatFormatting.GOLD}});
        ARG_STYLES.put("gui.kineticarmory.armorsets.mode.min", new ChatFormatting[][]{new ChatFormatting[]{ChatFormatting.GOLD}});
        ARG_STYLES.put("gui.kineticarmory.armorsets.nbt_prefix", new ChatFormatting[][]{new ChatFormatting[]{ChatFormatting.GOLD}});
        ARG_STYLES.put("gui.kineticarmory.armorsets.piece.bonus.full_note", new ChatFormatting[][]{new ChatFormatting[]{ChatFormatting.GREEN}});
        ARG_STYLES.put("gui.kineticarmory.armorsets.piece.bonus.group_row", new ChatFormatting[][]{new ChatFormatting[]{ChatFormatting.GREEN}, new ChatFormatting[]{ChatFormatting.GREEN}});
        ARG_STYLES.put("gui.kineticarmory.armorsets.piece.bonus.tier_row", new ChatFormatting[][]{new ChatFormatting[]{ChatFormatting.GREEN}});
        ARG_STYLES.put("gui.kineticarmory.armorsets.piece.bonus.tier_value", new ChatFormatting[][]{new ChatFormatting[]{ChatFormatting.GREEN}});
        ARG_STYLES.put("gui.kineticarmory.armorsets.piece.bonus.value_base", new ChatFormatting[][]{new ChatFormatting[]{ChatFormatting.GREEN}});
        ARG_STYLES.put("gui.kineticarmory.armorsets.piece.bonus.value_custom", new ChatFormatting[][]{new ChatFormatting[]{ChatFormatting.YELLOW}});
        ARG_STYLES.put("gui.kineticarmory.armorsets.piece.bonus.warn_duplicate", new ChatFormatting[][]{new ChatFormatting[]{ChatFormatting.GREEN}});
        ARG_STYLES.put("gui.kineticarmory.armorsets.piece.bonus.warn_full", new ChatFormatting[][]{new ChatFormatting[]{ChatFormatting.GREEN}});
        ARG_STYLES.put("gui.kineticarmory.armorsets.tipkey", new ChatFormatting[][]{new ChatFormatting[]{ChatFormatting.GOLD}});
        ARG_STYLES.put("gui.kineticarmory.armorsets.tooltip.conflict_more", new ChatFormatting[][]{new ChatFormatting[]{ChatFormatting.YELLOW, ChatFormatting.BOLD}});
        ARG_STYLES.put("gui.kineticarmory.armorsets.tooltip.conflict_row", new ChatFormatting[][]{new ChatFormatting[]{ChatFormatting.RED}});
        ARG_STYLES.put("gui.kineticarmory.armorsets.tooltip.current_pieces", new ChatFormatting[][]{new ChatFormatting[]{ChatFormatting.YELLOW}, new ChatFormatting[]{ChatFormatting.YELLOW}});
        ARG_STYLES.put("gui.kineticarmory.armorsets.tooltip.empty_slot", new ChatFormatting[][]{new ChatFormatting[]{ChatFormatting.GOLD}});
        ARG_STYLES.put("gui.kineticarmory.armorsets.tooltip.hold_key_details", new ChatFormatting[][]{new ChatFormatting[]{ChatFormatting.GOLD, ChatFormatting.BOLD}});
        ARG_STYLES.put("gui.kineticarmory.armorsets.tooltip.name_with_pieces", new ChatFormatting[][]{new ChatFormatting[]{ChatFormatting.AQUA, ChatFormatting.BOLD}, new ChatFormatting[]{ChatFormatting.YELLOW, ChatFormatting.BOLD}, new ChatFormatting[]{ChatFormatting.GREEN, ChatFormatting.BOLD}});
        ARG_STYLES.put("gui.kineticarmory.armorsets.tooltip.piece_tier", new ChatFormatting[][]{new ChatFormatting[]{ChatFormatting.GOLD}});
        ARG_STYLES.put("gui.kineticarmory.armorsets.tooltip.sets_more", new ChatFormatting[][]{new ChatFormatting[]{ChatFormatting.GOLD}});
        ARG_STYLES.put("gui.kineticarmory.armorsets.variant.row_sub", new ChatFormatting[][]{new ChatFormatting[]{ChatFormatting.AQUA}, new ChatFormatting[]{ChatFormatting.YELLOW}});
        ARG_STYLES.put("gui.kineticarmory.armorsets.variant.title", new ChatFormatting[][]{new ChatFormatting[]{ChatFormatting.GOLD, ChatFormatting.BOLD}});
        ARG_STYLES.put("gui.kineticarmory.armorsets.variant.tooltip.open_list", new ChatFormatting[][]{new ChatFormatting[]{ChatFormatting.GREEN, ChatFormatting.BOLD}});
        ARG_STYLES.put("gui.kineticarmory.predicate.list.type", new ChatFormatting[][]{new ChatFormatting[]{ChatFormatting.YELLOW}});
        ARG_STYLES.put("gui.kineticarmory.predicate.list.type.inverted", new ChatFormatting[][]{new ChatFormatting[]{ChatFormatting.RED}, new ChatFormatting[]{ChatFormatting.YELLOW}});
        ARG_STYLES.put("gui.kineticarmory.predicate.param.hint", new ChatFormatting[][]{new ChatFormatting[]{ChatFormatting.GOLD}});
        ARG_STYLES.put("gui.kineticarmory.predicate.param.label", new ChatFormatting[][]{new ChatFormatting[]{ChatFormatting.YELLOW}, new ChatFormatting[]{ChatFormatting.GOLD}});
        ARG_STYLES.put("msg.kineticarmory.armorsets.load_error", new ChatFormatting[][]{new ChatFormatting[]{ChatFormatting.YELLOW}});
        ARG_STYLES.put("msg.kineticarmory.armorsets.rejected", new ChatFormatting[][]{new ChatFormatting[]{ChatFormatting.GOLD, ChatFormatting.BOLD}});
        ARG_STYLES.put("tip.kineticarmory.armorsets.attack", new ChatFormatting[][]{new ChatFormatting[]{ChatFormatting.GOLD}, new ChatFormatting[]{ChatFormatting.GOLD}, new ChatFormatting[]{ChatFormatting.GOLD}});
        ARG_STYLES.put("tip.kineticarmory.armorsets.attack_damage", new ChatFormatting[][]{new ChatFormatting[]{ChatFormatting.GREEN}});
        ARG_STYLES.put("tip.kineticarmory.armorsets.attr", new ChatFormatting[][]{new ChatFormatting[]{ChatFormatting.GOLD}, new ChatFormatting[]{ChatFormatting.GOLD}, new ChatFormatting[]{ChatFormatting.GOLD}});
        ARG_STYLES.put("tip.kineticarmory.armorsets.cond.attr", new ChatFormatting[][]{new ChatFormatting[]{ChatFormatting.DARK_AQUA}, new ChatFormatting[]{ChatFormatting.AQUA}, new ChatFormatting[]{ChatFormatting.YELLOW}, new ChatFormatting[]{ChatFormatting.YELLOW}});
        ARG_STYLES.put("tip.kineticarmory.armorsets.cond.block", new ChatFormatting[][]{new ChatFormatting[]{ChatFormatting.DARK_AQUA}, new ChatFormatting[]{ChatFormatting.DARK_AQUA}});
        ARG_STYLES.put("tip.kineticarmory.armorsets.cond.potion", new ChatFormatting[][]{new ChatFormatting[]{ChatFormatting.DARK_AQUA}, new ChatFormatting[]{ChatFormatting.LIGHT_PURPLE}, new ChatFormatting[]{ChatFormatting.YELLOW}, new ChatFormatting[]{ChatFormatting.YELLOW}});
        ARG_STYLES.put("tip.kineticarmory.armorsets.cond.range", new ChatFormatting[][]{new ChatFormatting[]{ChatFormatting.DARK_AQUA}, new ChatFormatting[]{ChatFormatting.YELLOW}, new ChatFormatting[]{ChatFormatting.YELLOW}});
        ARG_STYLES.put("tip.kineticarmory.armorsets.cond.range.seconds", new ChatFormatting[][]{new ChatFormatting[]{ChatFormatting.DARK_AQUA}, new ChatFormatting[]{ChatFormatting.AQUA}, new ChatFormatting[]{ChatFormatting.AQUA}});
        ARG_STYLES.put("tip.kineticarmory.armorsets.cond.ticks", new ChatFormatting[][]{new ChatFormatting[]{ChatFormatting.DARK_AQUA}, new ChatFormatting[]{ChatFormatting.AQUA}});
        ARG_STYLES.put("tip.kineticarmory.armorsets.cond.value", new ChatFormatting[][]{new ChatFormatting[]{ChatFormatting.DARK_AQUA}, new ChatFormatting[]{ChatFormatting.AQUA}});
        ARG_STYLES.put("tip.kineticarmory.armorsets.convert", new ChatFormatting[][]{new ChatFormatting[]{ChatFormatting.GOLD}, new ChatFormatting[]{ChatFormatting.GOLD}, new ChatFormatting[]{ChatFormatting.GOLD}, new ChatFormatting[]{ChatFormatting.GOLD}});
        ARG_STYLES.put("tip.kineticarmory.armorsets.effect_immunity", new ChatFormatting[][]{new ChatFormatting[]{ChatFormatting.GOLD}});
        ARG_STYLES.put("tip.kineticarmory.armorsets.hold_key", new ChatFormatting[][]{new ChatFormatting[]{ChatFormatting.AQUA}});
        ARG_STYLES.put("tip.kineticarmory.armorsets.immunity", new ChatFormatting[][]{new ChatFormatting[]{ChatFormatting.GOLD}, new ChatFormatting[]{ChatFormatting.GOLD}});
        ARG_STYLES.put("tip.kineticarmory.armorsets.immunity_simple", new ChatFormatting[][]{new ChatFormatting[]{ChatFormatting.GOLD}});
        ARG_STYLES.put("tip.kineticarmory.armorsets.potion", new ChatFormatting[][]{new ChatFormatting[]{ChatFormatting.GOLD}, new ChatFormatting[]{ChatFormatting.GOLD}, new ChatFormatting[]{ChatFormatting.GOLD}});
        ARG_STYLES.put("tip.kineticarmory.armorsets.prefix.attack", new ChatFormatting[][]{new ChatFormatting[]{ChatFormatting.RED}, new ChatFormatting[]{ChatFormatting.RED}, new ChatFormatting[]{ChatFormatting.RED}});
        ARG_STYLES.put("tip.kineticarmory.armorsets.prefix.attack_damage_buff", new ChatFormatting[][]{new ChatFormatting[]{ChatFormatting.GOLD}});
        ARG_STYLES.put("tip.kineticarmory.armorsets.prefix.attack_damage_debuff", new ChatFormatting[][]{new ChatFormatting[]{ChatFormatting.RED}});
        ARG_STYLES.put("tip.kineticarmory.armorsets.prefix.attr_buff", new ChatFormatting[][]{new ChatFormatting[]{ChatFormatting.GOLD}, new ChatFormatting[]{ChatFormatting.GOLD}});
        ARG_STYLES.put("tip.kineticarmory.armorsets.prefix.attr_debuff", new ChatFormatting[][]{new ChatFormatting[]{ChatFormatting.DARK_RED}, new ChatFormatting[]{ChatFormatting.DARK_RED}});
        ARG_STYLES.put("tip.kineticarmory.armorsets.prefix.attr_set", new ChatFormatting[][]{new ChatFormatting[]{ChatFormatting.GOLD}, new ChatFormatting[]{ChatFormatting.GOLD}});
        ARG_STYLES.put("tip.kineticarmory.armorsets.prefix.convert", new ChatFormatting[][]{new ChatFormatting[]{ChatFormatting.DARK_PURPLE}, new ChatFormatting[]{ChatFormatting.DARK_PURPLE}, new ChatFormatting[]{ChatFormatting.DARK_PURPLE}, new ChatFormatting[]{ChatFormatting.DARK_PURPLE}});
        ARG_STYLES.put("tip.kineticarmory.armorsets.prefix.effect_immunity", new ChatFormatting[][]{new ChatFormatting[]{ChatFormatting.AQUA}});
        ARG_STYLES.put("tip.kineticarmory.armorsets.prefix.immunity", new ChatFormatting[][]{new ChatFormatting[]{ChatFormatting.AQUA}});
        ARG_STYLES.put("tip.kineticarmory.armorsets.prefix.potion", new ChatFormatting[][]{new ChatFormatting[]{ChatFormatting.LIGHT_PURPLE}, new ChatFormatting[]{ChatFormatting.LIGHT_PURPLE}, new ChatFormatting[]{ChatFormatting.LIGHT_PURPLE}});
        ARG_STYLES.put("tip.kineticarmory.armorsets.prefix.reduction_buff", new ChatFormatting[][]{new ChatFormatting[]{ChatFormatting.AQUA}});
        ARG_STYLES.put("tip.kineticarmory.armorsets.prefix.typed_damage_buff", new ChatFormatting[][]{new ChatFormatting[]{ChatFormatting.BLUE}, new ChatFormatting[]{ChatFormatting.BLUE}});
        ARG_STYLES.put("tip.kineticarmory.armorsets.prefix.typed_damage_debuff", new ChatFormatting[][]{new ChatFormatting[]{ChatFormatting.DARK_RED}, new ChatFormatting[]{ChatFormatting.DARK_RED}});
        ARG_STYLES.put("tip.kineticarmory.armorsets.reduction", new ChatFormatting[][]{new ChatFormatting[]{ChatFormatting.GOLD}});
        ARG_STYLES.put("tip.kineticarmory.armorsets.remove_blocking_slots", new ChatFormatting[][]{new ChatFormatting[]{ChatFormatting.RED}});
        ARG_STYLES.put("tip.kineticarmory.armorsets.require_slot_any", new ChatFormatting[][]{new ChatFormatting[]{ChatFormatting.GREEN}});
        ARG_STYLES.put("tip.kineticarmory.armorsets.require_slot_empty", new ChatFormatting[][]{new ChatFormatting[]{ChatFormatting.RED}});
        ARG_STYLES.put("tip.kineticarmory.armorsets.require_slots_any", new ChatFormatting[][]{new ChatFormatting[]{ChatFormatting.GREEN}});
        ARG_STYLES.put("tip.kineticarmory.armorsets.require_slots_empty", new ChatFormatting[][]{new ChatFormatting[]{ChatFormatting.RED}});
        ARG_STYLES.put("tip.kineticarmory.armorsets.requires_min", new ChatFormatting[][]{new ChatFormatting[]{ChatFormatting.GOLD}});
    }

    private ColorText() {
    }

    public static MutableComponent translatable(String key, Object... args) {
        ChatFormatting[][] styles = ARG_STYLES.get(key);
        if (styles == null || args.length == 0) {
            return Component.translatable(key, args);
        }
        Object[] styledArgs = args.clone();
        int count = Math.min(styles.length, styledArgs.length);
        for (int i = 0; i < count; i++) {
            ChatFormatting[] formats = styles[i];
            if (formats == null || formats.length == 0) continue;
            Object value = styledArgs[i];
            boolean preserveColor = value instanceof Component existing && existing.getStyle().getColor() != null;
            MutableComponent component = value instanceof Component existing
                    ? existing.copy()
                    : Component.literal(String.valueOf(value));
            if (preserveColor) {
                for (int j = 1; j < formats.length; j++) {
                    component.withStyle(formats[j]);
                }
            } else {
                component.withStyle(formats);
            }
            styledArgs[i] = component;
        }
        return Component.translatable(key, styledArgs);
    }
}
