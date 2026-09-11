package dev.xyat.kineticarmory.armorsets.data;

import dev.xyat.kineticarmory.armorsets.predicate.ConditionData;
import dev.xyat.kineticarmory.armorsets.predicate.ConditionTypeUtil;
import dev.xyat.kineticcore.api.client.search.KineticSearch;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

public class ArmorTipGenerator {

    private static final Pattern COLOR_CODE_PATTERN = Pattern.compile("§[0-9a-fk-or]", Pattern.CASE_INSENSITIVE);
    private static final Pattern ICON_PATTERN = Pattern.compile("\\[(item|effect):([^]]+)]");
    private static final List<String> EQUIPMENT_SLOT_ORDER = List.of("head", "chest", "legs", "feet", "mainhand", "offhand");

    private static String fmt(double d) { return d == (long) d ? String.valueOf((long) d) : String.valueOf(d); }

    private static ResourceLocation safeResourceLocation(String id) {
        return id == null || id.isBlank() ? null : ResourceLocation.tryParse(id);
    }

    private static String getPotionName(String id) {
        ResourceLocation rl = safeResourceLocation(id);
        var effect = rl == null ? null : ForgeRegistries.MOB_EFFECTS.getValue(rl);
        if (effect != null) {
            String key = effect.getDescriptionId();
            String translated = Component.translatable(key).getString();
            return translated.equals(key) ? id : translated;
        }
        return id == null ? "" : id;
    }

    private static String getAttrName(String id) {
        ResourceLocation rl = safeResourceLocation(id);
        var attr = rl == null ? null : ForgeRegistries.ATTRIBUTES.getValue(rl);
        if (attr != null) {
            String key = attr.getDescriptionId();
            String translated = Component.translatable(key).getString();
            return translated.equals(key) ? id : translated;
        }
        return id == null ? "" : id;
    }

    private static String getItemName(String id) {
        ResourceLocation rl = safeResourceLocation(id);
        var item = rl == null ? null : ForgeRegistries.ITEMS.getValue(rl);
        if (item != null) {
            String key = item.getDescriptionId();
            String translated = Component.translatable(key).getString();
            return translated.equals(key) ? id : translated;
        }
        return id == null ? "" : id;
    }

    private static String getDimensionName(String id) {
        ResourceLocation rl = safeResourceLocation(id);
        if (rl != null) {
            String key = "dimension." + rl.getNamespace() + "." + rl.getPath();
            String translated = Component.translatable(key).getString();
            return translated.equals(key) ? id : translated;
        }
        return id == null ? "" : id;
    }

    private static String buildCondString(ConditionData cond) {
        if (cond == null || cond.type == null) return "";
        Map<String, String> params = cond.params == null ? Collections.emptyMap() : cond.params;
        String type = cond.type.toUpperCase(Locale.ROOT);
        String typeName = ConditionTypeUtil.getTranslatedName(type);
        String pId = params.getOrDefault("id", "minecraft:air");
        String pMin = params.getOrDefault("min", "0");
        String pMax = params.getOrDefault("max", "0");

        return switch (type) {
            case "ON_BLOCK" -> Component.translatable("tip.kineticarmory.armorsets.cond.block", "§3" + typeName, "§3[item:" + pId + "] " + getItemName(pId)).getString();
            case "POTION_RANGE" -> Component.translatable("tip.kineticarmory.armorsets.cond.potion", "§3" + typeName, "§d" + getPotionName(pId), "§e" + params.getOrDefault("min_lvl", "0"), "§e" + params.getOrDefault("max_lvl", "255")).getString();
            case "ATTR_RANGE" -> Component.translatable("tip.kineticarmory.armorsets.cond.attr", "§3" + typeName, "§b" + getAttrName(pId), "§e" + pMin, "§e" + pMax).getString();
            case "DIMENSION" -> Component.translatable("tip.kineticarmory.armorsets.cond.value", "§3" + typeName, "§b" + getDimensionName(pId)).getString();
            case "STAGE" -> Component.translatable("tip.kineticarmory.armorsets.cond.value", "§3" + typeName, "§b" + params.getOrDefault("stage", "unknown")).getString();
            case "MOON_PHASE" -> Component.translatable("tip.kineticarmory.armorsets.cond.value", "§3" + typeName, "§b[item:minecraft:clock] " + params.getOrDefault("phase", "0")).getString();
            case "MOUSE_LEFT_HOLD", "MOUSE_RIGHT_HOLD" -> Component.translatable(
                    "tip.kineticarmory.armorsets.cond.ticks",
                    "§3" + typeName,
                    "§b" + formatTickSeconds(params.getOrDefault("ticks", "1"))
            ).getString();
            case "HEALTH_RANGE" -> Component.translatable("tip.kineticarmory.armorsets.cond.range", "§3" + typeName, "§e[item:minecraft:golden_apple] " + pMin, "§e" + pMax).getString();
            case "FOOD_RANGE" -> Component.translatable("tip.kineticarmory.armorsets.cond.range", "§3" + typeName, "§e[item:minecraft:cooked_beef] " + pMin, "§e" + pMax).getString();
            case "EXP_RANGE" -> Component.translatable("tip.kineticarmory.armorsets.cond.range", "§3" + typeName, "§e[item:minecraft:experience_bottle] " + pMin, "§e" + pMax).getString();
            case "SPEED_RANGE" -> Component.translatable(
                    "tip.kineticarmory.armorsets.cond.range", "§3" + typeName, "§e" + pMin, "§e" + pMax).getString();
            case "TIME_RANGE" -> Component.translatable(
                    "tip.kineticarmory.armorsets.cond.range.seconds",
                    "§3" + typeName,
                    "§b" + formatOptionalTickSeconds(params.get("min"), "0"),
                    "§b" + formatOptionalTickSeconds(params.get("max"), "1199.95")
            ).getString();
            default -> typeName;
        };
    }

    private static String formatTickSeconds(String rawTicks) {
        try {
            return java.math.BigDecimal.valueOf(Double.parseDouble(rawTicks) / 20.0D)
                    .stripTrailingZeros()
                    .toPlainString();
        } catch (NumberFormatException ignored) {
            return "0.05";
        }
    }

    private static String formatOptionalTickSeconds(String rawTicks, String fallback) {
        if (rawTicks == null || rawTicks.isBlank()) return fallback;
        return formatTickSeconds(rawTicks);
    }

    private static String getCondTxt(List<ConditionData> conditions, String mode, int minCount) {
        if (conditions == null || conditions.isEmpty()) return "";
        StringBuilder sb = new StringBuilder(" §f(");

        String joinStr = Component.translatable("tip.kineticarmory.armorsets.or").getString();
        String prefix = Component.translatable("tip.kineticarmory.armorsets.requires_or").getString();

        if ("ALL".equalsIgnoreCase(mode)) {
            joinStr = Component.translatable("tip.kineticarmory.armorsets.and").getString();
            prefix = Component.translatable("tip.kineticarmory.armorsets.requires_all").getString();
        } else if ("MIN".equalsIgnoreCase(mode) || "MIN_COUNT".equalsIgnoreCase(mode)) {
            joinStr = Component.translatable("tip.kineticarmory.armorsets.or").getString();
            prefix = Component.translatable("tip.kineticarmory.armorsets.requires_min", "§e" + minCount).getString();
        }

        sb.append(prefix);
        for (int i = 0; i < conditions.size(); i++) {
            ConditionData cond = conditions.get(i);
            if (cond == null) continue;
            if (cond.invert) {
                sb.append("§c").append(Component.translatable("tip.kineticarmory.armorsets.invert.prefix").getString()).append(" ");
            }
            sb.append("§b");
            sb.append(buildCondString(cond));
            if (i < conditions.size() - 1) {
                sb.append(joinStr);
            }
        }
        sb.append("§f)");
        return sb.toString();
    }

    private static String applyLineWrap(String text) {
        StringBuilder result = new StringBuilder();
        int count = 0;
        int i = 0;
        String lastColor = "§f";

        while (i < text.length()) {
            if (text.charAt(i) == '§' && i + 1 < text.length()) {
                lastColor = "§" + text.charAt(i + 1);
                result.append(text.charAt(i)).append(text.charAt(i + 1));
                i += 2;
                continue;
            }

            if (text.startsWith("[item:", i) || text.startsWith("[effect:", i)) {
                int end = text.indexOf(']', i);
                if (end != -1) {
                    result.append(text, i, end + 1);
                    i = end + 1;
                    count += 2;
                    continue;
                }
            }

            if (count >= 32) {
                result.append("\n  ").append(lastColor);
                count = 0;
                if (text.charAt(i) == ' ') {
                    i++;
                    if (i >= text.length()) break;
                }
            }

            result.append(text.charAt(i));
            count++;
            i++;
        }
        return result.toString();
    }

    public static String genAttrTip(ArmorDataConfig.AttributeModifierData d) {
        String name = getAttrName(d.attribute);
        String operation = d.operation == null ? "ADDITION" : d.operation;
        boolean isPositive = d.amount > 0;
        String color = isPositive ? "§a" : "§c";
        String valStr;

        if (operation.equalsIgnoreCase("SET")) {
            valStr = "§e" + fmt(d.amount);
            String prefixKey = "tip.kineticarmory.armorsets.prefix.attr_set";
            return applyLineWrap(Component.translatable(prefixKey, "§b" + name, valStr).getString() + getCondTxt(d.conditions, d.conditionMatchMode, d.conditionMinCount));
        } else if (operation.toUpperCase(Locale.ROOT).contains("MULTIPLY")) {
            valStr = color + (isPositive ? "+" : "") + fmt(d.amount * 100) + "%";
        } else {
            valStr = color + (isPositive ? "+" : "") + fmt(d.amount);
        }

        String prefixKey = isPositive ? "tip.kineticarmory.armorsets.prefix.attr_buff" : "tip.kineticarmory.armorsets.prefix.attr_debuff";
        return applyLineWrap(Component.translatable(prefixKey, "§b" + name, valStr).getString() + getCondTxt(d.conditions, d.conditionMatchMode, d.conditionMinCount));
    }

    public static String genPotTip(ArmorDataConfig.PotionEffectData d) {
        String name = getPotionName(d.effectId);
        String lvlStr = "§e" + (d.amplifier + 1);
        String durStr = "§e" + d.duration;
        return applyLineWrap(Component.translatable("tip.kineticarmory.armorsets.prefix.potion", "§d" + name, lvlStr, durStr).getString() + getCondTxt(d.conditions, d.conditionMatchMode, d.conditionMinCount));
    }

    public static String genImmTip(ArmorDataConfig.DamageImmunityData d) {
        String name = KineticSearch.dictionaryName(d.damageType, KineticSearch.damageDictionary());
        if (d.multiplier == 0.0) {
            return applyLineWrap(Component.translatable("tip.kineticarmory.armorsets.prefix.immunity", "§a" + name).getString() + getCondTxt(d.conditions, d.conditionMatchMode, d.conditionMinCount));
        } else {
            boolean isBuff = d.multiplier < 1.0;
            String color = isBuff ? "§a" : "§c";
            String valStr = color + "x" + fmt(d.multiplier);
            String prefixKey = isBuff ? "tip.kineticarmory.armorsets.prefix.typed_damage_buff" : "tip.kineticarmory.armorsets.prefix.typed_damage_debuff";
            return applyLineWrap(Component.translatable(prefixKey, "§c" + name, valStr).getString() + getCondTxt(d.conditions, d.conditionMatchMode, d.conditionMinCount));
        }
    }

    public static String genEffImmTip(ArmorDataConfig.EffectImmunityData d) {
        String name = getPotionName(d.effectId);
        return applyLineWrap(Component.translatable("tip.kineticarmory.armorsets.prefix.effect_immunity", "§a" + name).getString() + getCondTxt(d.conditions, d.conditionMatchMode, d.conditionMinCount));
    }

    public static String genAtkTip(ArmorDataConfig.AttackEffectData d) {
        String name = getPotionName(d.effectId);
        String chanceStr = "§e" + (int)(d.chance * 100);
        String durStr = "§e" + fmt(d.duration);
        return applyLineWrap(Component.translatable("tip.kineticarmory.armorsets.prefix.attack", chanceStr, "§c" + name, durStr).getString() + getCondTxt(d.conditions, d.conditionMatchMode, d.conditionMinCount));
    }

    public static String genConvTip(ArmorDataConfig.DamageConversionData d) {
        String srcName = KineticSearch.dictionaryName(d.sourceType, KineticSearch.damageDictionary());
        String tgtName = KineticSearch.dictionaryName(d.targetType, KineticSearch.specificDamageDictionary());
        String chanceStr = "§e" + (int)(d.chance * 100);
        String ratioStr = "§e" + (int)(d.ratio * 100);
        return applyLineWrap(Component.translatable("tip.kineticarmory.armorsets.prefix.convert", chanceStr, "§c" + srcName, "§d" + tgtName, ratioStr).getString() + getCondTxt(d.conditions, d.conditionMatchMode, d.conditionMinCount));
    }

    public static String genDmgMulTip(ArmorDataConfig.DamageMultiplierData d) {
        boolean isBuff = d.multiplier < 1.0;
        String color = isBuff ? "§a" : "§c";
        String valStr = color + "x" + fmt(d.multiplier);
        return applyLineWrap(Component.translatable("gui.kineticarmory.armorsets.effect.damage_multiplier", valStr).getString() + getCondTxt(d.conditions, d.conditionMatchMode, d.conditionMinCount));
    }

    public static String genAtkDmgTip(ArmorDataConfig.AttackDamageMultiplierData d) {
        boolean isBuff = d.multiplier > 1.0;
        String color = isBuff ? "§a" : "§c";
        String valStr = color + "x" + fmt(d.multiplier);
        String prefixKey = isBuff ? "tip.kineticarmory.armorsets.prefix.attack_damage_buff" : "tip.kineticarmory.armorsets.prefix.attack_damage_debuff";
        return applyLineWrap(Component.translatable(prefixKey, valStr).getString() + getCondTxt(d.conditions, d.conditionMatchMode, d.conditionMinCount));
    }

    public static String genFlightTip(List<ConditionData> flightConditions, String mode, int minCount) {
        String base = "§b" + Component.translatable("gui.kineticarmory.armorsets.detail.flight_effect").getString();
        return applyLineWrap(base + getCondTxt(flightConditions, mode, minCount));
    }

    public static List<TooltipLine> buildTooltipLines(ArmorDataConfig config, int pieceCount, boolean includePieceCounter) {
        List<TooltipLine> lines = new ArrayList<>();
        if (config == null) return lines;
        config.preparePieceBonusData();

        int total = Math.max(1, config.getTotalPieceCount());
        boolean flexible = config.flexiblePieces && config.pieceBonusGroups != null && !config.pieceBonusGroups.isEmpty();
        if (config.manualTips) {
            addManualLayoutLines(lines, config, total, flexible, includePieceCounter, pieceCount);
            addEquipmentRequirementLines(lines, config);
            return lines;
        }

        List<GeneratedEffectTip> effectTips = buildGeneratedEffectTips(config, total);
        Set<String> generatedNormals = collectGeneratedNormals(effectTips);

        if (includePieceCounter && flexible) {
            String key = buildStaticTipOverrideKey("current_pieces", Collections.singletonList(total));
            String text = Component.translatable("gui.kineticarmory.armorsets.tooltip.current_pieces", "§e" + pieceCount, "§a" + total).getString();
            lines.add(TooltipLine.text(getTipOverride(config, key, text), key));
        }

        addEquipmentRequirementLines(lines, config);

        addCustomTipLines(lines, config, generatedNormals);

        if (flexible) {
            addFlexibleGeneratedLines(lines, config, effectTips, total);
        } else {
            addPlainGeneratedLines(lines, config, effectTips, total);
        }

        return lines;
    }


    public static List<GeneratedEffectTip> buildGeneratedEffectTips(ArmorDataConfig config, int pieceCount) {
        return buildGeneratedEffectTips(config, pieceCount, false);
    }

    private static List<GeneratedEffectTip> buildGeneratedEffectTips(ArmorDataConfig config, int pieceCount, boolean exactTier) {
        List<GeneratedEffectTip> result = new ArrayList<>();
        if (config == null) return result;
        if (config.potionEffects != null) for (ArmorDataConfig.PotionEffectData d : config.potionEffects) result.add(new GeneratedEffectTip(config.keyOf(d), genPotTip(config, d, pieceCount, exactTier)));
        if (config.attributes != null) for (ArmorDataConfig.AttributeModifierData d : config.attributes) result.add(new GeneratedEffectTip(config.keyOf(d), genAttrTip(config, d, pieceCount, exactTier)));
        if (config.damageImmunities != null) for (ArmorDataConfig.DamageImmunityData d : config.damageImmunities) result.add(new GeneratedEffectTip(config.keyOf(d), genImmTip(config, d, pieceCount, exactTier)));
        if (config.effectImmunities != null) for (ArmorDataConfig.EffectImmunityData d : config.effectImmunities) result.add(new GeneratedEffectTip(config.keyOf(d), genEffImmTip(d)));
        if (config.attackEffects != null) for (ArmorDataConfig.AttackEffectData d : config.attackEffects) result.add(new GeneratedEffectTip(config.keyOf(d), genAtkTip(config, d, pieceCount, exactTier)));
        if (config.damageConversions != null) for (ArmorDataConfig.DamageConversionData d : config.damageConversions) result.add(new GeneratedEffectTip(config.keyOf(d), genConvTip(config, d, pieceCount, exactTier)));
        if (config.damageMultipliers != null) for (ArmorDataConfig.DamageMultiplierData d : config.damageMultipliers) result.add(new GeneratedEffectTip(config.keyOf(d), genDmgMulTip(config, d, pieceCount, exactTier)));
        if (config.attackDamageMultipliers != null) for (ArmorDataConfig.AttackDamageMultiplierData d : config.attackDamageMultipliers) result.add(new GeneratedEffectTip(config.keyOf(d), genAtkDmgTip(config, d, pieceCount, exactTier)));
        if (config.allowFlight) result.add(new GeneratedEffectTip(config.keyOfFlight(), genFlightTip(config.flightConditions, config.flightConditionMatchMode, config.flightConditionMinCount)));
        return result;
    }

    private static String genAttrTip(ArmorDataConfig config, ArmorDataConfig.AttributeModifierData d, int pieceCount, boolean exactTier) {
        double old = d.amount;
        try {
            d.amount = exactTier
                    ? config.resolvePieceTierValue(config.keyOf(d), pieceCount, old)
                    : config.resolveCumulativePieceValue(config.keyOf(d), pieceCount, old);
            return genAttrTip(d);
        } finally {
            d.amount = old;
        }
    }

    private static String genPotTip(ArmorDataConfig config, ArmorDataConfig.PotionEffectData d, int pieceCount, boolean exactTier) {
        int old = d.amplifier;
        try {
            double value = exactTier
                    ? config.resolvePieceTierValue(config.keyOf(d), pieceCount, old)
                    : config.resolveHighestPieceValue(config.keyOf(d), pieceCount, old);
            d.amplifier = Math.max(0, (int)Math.round(value));
            return genPotTip(d);
        } finally {
            d.amplifier = old;
        }
    }

    private static String genAtkTip(ArmorDataConfig config, ArmorDataConfig.AttackEffectData d, int pieceCount, boolean exactTier) {
        int old = d.amplifier;
        try {
            double value = exactTier
                    ? config.resolvePieceTierValue(config.keyOf(d), pieceCount, old)
                    : config.resolveHighestPieceValue(config.keyOf(d), pieceCount, old);
            d.amplifier = Math.max(0, (int)Math.round(value));
            return genAtkTip(d);
        } finally {
            d.amplifier = old;
        }
    }

    private static String genImmTip(ArmorDataConfig config, ArmorDataConfig.DamageImmunityData d, int pieceCount, boolean exactTier) {
        double old = d.multiplier;
        try {
            d.multiplier = exactTier
                    ? config.resolvePieceTierValue(config.keyOf(d), pieceCount, old)
                    : config.resolveCumulativePieceMultiplier(config.keyOf(d), pieceCount, old);
            return genImmTip(d);
        } finally {
            d.multiplier = old;
        }
    }

    private static String genConvTip(ArmorDataConfig config, ArmorDataConfig.DamageConversionData d, int pieceCount, boolean exactTier) {
        double old = d.ratio;
        try {
            d.ratio = exactTier
                    ? config.resolvePieceTierValue(config.keyOf(d), pieceCount, old)
                    : config.resolveCumulativePieceValue(config.keyOf(d), pieceCount, old);
            return genConvTip(d);
        } finally {
            d.ratio = old;
        }
    }

    private static String genDmgMulTip(ArmorDataConfig config, ArmorDataConfig.DamageMultiplierData d, int pieceCount, boolean exactTier) {
        double old = d.multiplier;
        try {
            d.multiplier = exactTier
                    ? config.resolvePieceTierValue(config.keyOf(d), pieceCount, old)
                    : config.resolveCumulativePieceMultiplier(config.keyOf(d), pieceCount, old);
            return genDmgMulTip(d);
        } finally {
            d.multiplier = old;
        }
    }

    private static String genAtkDmgTip(ArmorDataConfig config, ArmorDataConfig.AttackDamageMultiplierData d, int pieceCount, boolean exactTier) {
        double old = d.multiplier;
        try {
            d.multiplier = exactTier
                    ? config.resolvePieceTierValue(config.keyOf(d), pieceCount, old)
                    : config.resolveCumulativePieceMultiplier(config.keyOf(d), pieceCount, old);
            return genAtkDmgTip(d);
        } finally {
            d.multiplier = old;
        }
    }

    private static Set<String> collectGeneratedNormals(List<GeneratedEffectTip> effectTips) {
        Set<String> normals = new HashSet<>();
        for (GeneratedEffectTip tip : effectTips) {
            for (String line : splitTipLines(tip.text())) {
                String normal = normalizeTipText(line);
                if (!normal.isBlank()) normals.add(normal);
            }
        }
        return normals;
    }

    private static void addManualLayoutLines(List<TooltipLine> lines, ArmorDataConfig config, int total, boolean flexible, boolean includePieceCounter, int pieceCount) {
        if (config.tipLayout == null || config.tipLayout.isEmpty()) {
            addManualTipLines(lines, config);
            return;
        }

        Map<String, TooltipLine> generatedByKey = new LinkedHashMap<>();
        for (TooltipLine line : buildGeneratedLayoutSource(config, total, flexible, includePieceCounter, pieceCount)) {
            if (line.overrideKey() != null && !line.overrideKey().isBlank()) {
                generatedByKey.putIfAbsent(line.overrideKey(), line);
            }
        }

        for (int i = 0; i < config.tipLayout.size(); i++) {
            ArmorDataConfig.TipLineData data = config.tipLayout.get(i);
            if (data == null) continue;
            if (data.isGenerated()) {
                addGeneratedLayoutLine(lines, data, generatedByKey.get(data.key), i);
            } else {
                addTextLayoutLine(lines, data, i);
            }
        }
    }

    private static void addGeneratedLayoutLine(List<TooltipLine> lines, ArmorDataConfig.TipLineData data, TooltipLine source, int customIndex) {
        String text = data.text == null || data.text.isBlank() ? (source == null ? "" : source.text()) : data.text;
        if (text == null || text.isBlank()) return;

        boolean iconLine = source == null ? data.iconLine : source.iconLine();
        boolean fullOnly = source == null ? data.fullOnly : source.fullOnly();
        List<Integer> activePieces = source == null ? safePieces(data.activePieces) : safePieces(source.activePieces());
        String key = data.key == null ? "" : data.key;
        String displayText = applyDefaultColor(text, iconLine);
        lines.add(new TooltipLine(displayText, iconLine, false, customIndex, activePieces, fullOnly, key));
    }

    private static void addTextLayoutLine(List<TooltipLine> lines, ArmorDataConfig.TipLineData data, int customIndex) {
        String text = data.text == null ? "" : data.text;
        if (text.isBlank()) return;
        boolean iconLine = data.iconLine;
        String displayText = applyDefaultColor(text, iconLine);
        if (iconLine) {
            lines.add(TooltipLine.custom(displayText, customIndex, safePieces(data.activePieces), data.fullOnly));
        } else {
            lines.add(new TooltipLine(displayText, false, true, customIndex, safePieces(data.activePieces), data.fullOnly, ""));
        }
    }

    private static List<Integer> safePieces(List<Integer> pieces) {
        if (pieces == null || pieces.isEmpty()) return Collections.emptyList();
        List<Integer> result = new ArrayList<>();
        for (Integer piece : pieces) {
            if (piece != null && piece > 0 && !result.contains(piece)) result.add(piece);
        }
        return result;
    }

    private static String applyDefaultColor(String text, boolean iconLine) {
        if (hasColorCode(text)) return text;
        return iconLine ? "§b" + text : "§6" + text;
    }

    private static boolean hasColorCode(String text) {
        return text != null && COLOR_CODE_PATTERN.matcher(text).find();
    }

    public static List<TooltipLine> buildGeneratedLayoutSource(ArmorDataConfig config, int total, boolean flexible, boolean includePieceCounter, int pieceCount) {
        List<TooltipLine> lines = new ArrayList<>();
        if (config == null) return lines;
        List<GeneratedEffectTip> effectTips = buildGeneratedEffectTips(config, total);

        if (includePieceCounter && flexible) {
            String key = buildStaticTipOverrideKey("current_pieces", Collections.singletonList(total));
            String text = Component.translatable("gui.kineticarmory.armorsets.tooltip.current_pieces", "§e" + pieceCount, "§a" + total).getString();
            lines.add(TooltipLine.text(getTipOverride(config, key, text), key));
        }

        addEquipmentRequirementLines(lines, config);

        if (flexible) {
            addFlexibleGeneratedLines(lines, config, effectTips, total);
        } else {
            addPlainGeneratedLines(lines, config, effectTips, total);
        }
        return lines;
    }

    private static void addEquipmentRequirementLines(List<TooltipLine> lines, ArmorDataConfig config) {
        if (config == null || config.equipmentVariants == null || config.equipmentVariants.isEmpty()) return;
        List<String> emptyNames = new ArrayList<>();
        List<String> anyNames = new ArrayList<>();
        for (String slot : EQUIPMENT_SLOT_ORDER) {
            List<ArmorDataConfig.ItemReq> variants = config.equipmentVariants.get(slot);
            if (ArmorDataConfig.isExclusiveSlotState(variants, "EMPTY")) {
                emptyNames.add(getSlotName(slot));
            } else if (ArmorDataConfig.isExclusiveSlotState(variants, "ANY")) {
                anyNames.add(getSlotName(slot));
            }
        }
        if (!emptyNames.isEmpty()) addCombinedRequirementLine(lines, config, "slots_empty", "tip.kineticarmory.armorsets.require_slots_empty", emptyNames);
        if (!anyNames.isEmpty()) addCombinedRequirementLine(lines, config, "slots_any", "tip.kineticarmory.armorsets.require_slots_any", anyNames);
    }

    private static void addCombinedRequirementLine(List<TooltipLine> lines, ArmorDataConfig config, String type, String translationKey, List<String> slotNames) {
        if (slotNames == null || slotNames.isEmpty()) return;
        String key = buildStaticTipOverrideKey(type, Collections.emptyList());
        for (TooltipLine line : lines) {
            if (line != null && key.equals(line.overrideKey())) return;
        }
        String text = Component.translatable(translationKey, ("tip.kineticarmory.armorsets.require_slots_empty".equals(translationKey) ? "§c" : "§a") + joinSlotNames(slotNames)).getString();
        lines.add(TooltipLine.text(getTipOverride(config, key, text), key));
    }

    private static String getSlotName(String slot) {
        return Component.translatable("gui.kineticarmory.armorsets.slot." + slot).getString();
    }

    private static String joinSlotNames(List<String> slotNames) {
        String separator = Component.translatable("tip.kineticarmory.armorsets.slot_separator").getString();
        StringBuilder builder = new StringBuilder();
        for (String name : slotNames) {
            if (name == null || name.isBlank()) continue;
            if (!builder.isEmpty()) builder.append(separator);
            builder.append(name);
        }
        return builder.toString();
    }

    private static void addManualTipLines(List<TooltipLine> lines, ArmorDataConfig config) {
        if (config.tips == null || config.tips.isEmpty()) return;
        for (int i = 0; i < config.tips.size(); i++) {
            String tip = config.tips.get(i);
            if (tip == null || tip.isBlank()) continue;
            for (String line : splitTipLines(tip)) {
                if (line == null || line.isBlank()) continue;
                lines.add(TooltipLine.custom(line, i));
            }
        }
    }

    private static void addCustomTipLines(List<TooltipLine> lines, ArmorDataConfig config, Set<String> generatedNormals) {
        if (config.tips == null || config.tips.isEmpty()) return;
        Set<String> added = new HashSet<>();
        for (int i = 0; i < config.tips.size(); i++) {
            String tip = config.tips.get(i);
            if (tip == null || tip.isBlank()) continue;
            for (String line : splitTipLines(tip)) {
                String normal = normalizeTipText(line);
                if (normal.isBlank() || generatedNormals.contains(normal) || !added.add(normal)) continue;
                lines.add(TooltipLine.custom(line, i));
            }
        }
    }

    private static void addPlainGeneratedLines(List<TooltipLine> lines, ArmorDataConfig config, List<GeneratedEffectTip> effectTips, int total) {
        Set<String> added = new HashSet<>();
        for (GeneratedEffectTip tip : effectTips) {
            addEffectTipLines(lines, config, tip, Collections.singletonList(total), false, added);
        }
    }

    private static void addFlexibleGeneratedLines(List<TooltipLine> lines, ArmorDataConfig config, List<GeneratedEffectTip> effectTips, int total) {
        List<ArmorDataConfig.PieceBonusGroup> groups = new ArrayList<>(config.pieceBonusGroups);
        groups.removeIf(group -> group == null || group.effectKeys == null || group.pieces < 2 || group.pieces > total);
        groups.sort(Comparator.comparingInt(group -> group.pieces));

        Set<String> configuredKeys = new HashSet<>();
        for (ArmorDataConfig.PieceBonusGroup group : groups) {
            if (group.effectKeys == null || group.effectKeys.isEmpty()) continue;
            Map<String, GeneratedEffectTip> tipsByKey = new LinkedHashMap<>();
            for (GeneratedEffectTip tip : buildGeneratedEffectTips(config, group.pieces, true)) {
                if (tip.key() != null && !tip.key().isBlank()) tipsByKey.putIfAbsent(tip.key(), tip);
            }

            List<Integer> groupPieces = Collections.singletonList(group.pieces);
            String groupTitleKey = buildStaticTipOverrideKey("piece_tier", groupPieces);
            lines.add(TooltipLine.effect(getTipOverride(config, groupTitleKey, pieceTierTitle(groupPieces)), groupPieces, false, groupTitleKey));
            Set<String> sectionAdded = new HashSet<>();
            for (String key : group.effectKeys) {
                if (key == null || key.isBlank()) continue;
                GeneratedEffectTip tip = tipsByKey.get(key);
                if (tip == null) continue;
                configuredKeys.add(key);
                addEffectTipLines(lines, config, tip, Collections.singletonList(group.pieces), false, sectionAdded);
            }
        }

        List<GeneratedEffectTip> fullOnly = new ArrayList<>();
        for (GeneratedEffectTip tip : effectTips) {
            if (!configuredKeys.contains(tip.key())) fullOnly.add(tip);
        }

        String fullTitleKey = buildStaticTipOverrideKey("full_tier", Collections.emptyList());
        lines.add(TooltipLine.effect(getTipOverride(config, fullTitleKey, Component.translatable("gui.kineticarmory.armorsets.tooltip.full_tier").getString()), Collections.emptyList(), true, fullTitleKey));
        if (fullOnly.isEmpty()) {
            String fullAutoKey = buildStaticTipOverrideKey("full_auto_all", Collections.emptyList());
            lines.add(TooltipLine.effect(getTipOverride(config, fullAutoKey, Component.translatable("gui.kineticarmory.armorsets.tooltip.full_auto_all").getString()), Collections.emptyList(), true, fullAutoKey));
            return;
        }

        Set<String> fullAdded = new HashSet<>();
        for (GeneratedEffectTip tip : fullOnly) {
            addEffectTipLines(lines, config, tip, Collections.emptyList(), true, fullAdded);
        }
    }

    private static void addEffectTipLines(List<TooltipLine> lines, ArmorDataConfig config, GeneratedEffectTip tip, List<Integer> activePieces, boolean fullOnly, Set<String> added) {
        List<String> splitLines = splitTipLines(tip == null ? null : tip.text());
        for (int i = 0; i < splitLines.size(); i++) {
            String line = splitLines.get(i);
            String normal = normalizeTipText(line);
            if (normal.isBlank() || !added.add(normal)) continue;
            String overrideKey = buildTipOverrideKey(tip == null ? null : tip.key(), activePieces, fullOnly, i, normal);
            String displayLine = getTipOverride(config, overrideKey, line);
            lines.add(TooltipLine.effect(displayLine, activePieces, fullOnly, overrideKey));
        }
    }

    private static String buildStaticTipOverrideKey(String type, List<Integer> activePieces) {
        String safeType = type == null || type.isBlank() ? "text" : type;
        String tier = activePieces == null || activePieces.isEmpty() ? "base" : piecesLabel(activePieces);
        return "text|" + safeType + "|" + tier;
    }

    private static String getTipOverride(ArmorDataConfig config, String key, String fallback) {
        if (config == null || config.tipOverrides == null || key == null || key.isBlank()) return fallback;
        String value = config.tipOverrides.get(key);
        return value == null || value.isBlank() ? fallback : value;
    }

    private static String buildTipOverrideKey(String effectKey, List<Integer> activePieces, boolean fullOnly, int lineIndex, String normal) {
        boolean hasEffectKey = effectKey != null && !effectKey.isBlank();
        String safeKey = hasEffectKey ? effectKey : "tip";
        String tier;
        if (fullOnly) {
            tier = "full";
        } else if (activePieces == null || activePieces.isEmpty()) {
            tier = "base";
        } else {
            tier = piecesLabel(activePieces);
        }
        if (hasEffectKey) return safeKey + "|" + tier + "|" + lineIndex;
        String suffix = normal == null || normal.isBlank() ? String.valueOf(lineIndex) : normal;
        return safeKey + "|" + tier + "|" + lineIndex + "|" + suffix;
    }

    private static List<String> splitTipLines(String text) {
        if (text == null || text.isBlank()) return Collections.emptyList();
        String[] parts = text.split("\\n");
        List<String> lines = new ArrayList<>();
        for (String part : parts) {
            if (part != null && !part.isBlank()) lines.add(part);
        }
        return lines;
    }

    private static String pieceTierTitle(List<Integer> pieces) {
        if (pieces == null || pieces.isEmpty()) {
            return Component.translatable("gui.kineticarmory.armorsets.tooltip.piece_tier", "§e0").getString();
        }
        String first = String.valueOf(pieces.get(0));
        String label = piecesLabel(pieces);
        String base = Component.translatable("gui.kineticarmory.armorsets.tooltip.piece_tier", "§e" + pieces.get(0)).getString();
        return label.equals(first) ? base : base.replaceFirst(Pattern.quote(first), label);
    }

    private static String piecesLabel(List<Integer> pieces) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < pieces.size(); i++) {
            if (i > 0) sb.append('/');
            sb.append(pieces.get(i));
        }
        return sb.toString();
    }

    private static String normalizeTipText(String text) {
        if (text == null) return "";
        return ICON_PATTERN.matcher(COLOR_CODE_PATTERN.matcher(text).replaceAll("")).replaceAll("").replaceAll("\\s+", "").trim();
    }

    public record GeneratedEffectTip(String key, String text) {}


    public record TooltipLine(String text, boolean iconLine, boolean customLine, int customIndex, List<Integer> activePieces, boolean fullOnly, String overrideKey) {
        public static TooltipLine text(String text) {
            return new TooltipLine(text, false, false, -1, Collections.emptyList(), false, "");
        }

        public static TooltipLine text(String text, String overrideKey) {
            return new TooltipLine(text, false, false, -1, Collections.emptyList(), false, overrideKey == null ? "" : overrideKey);
        }

        public static TooltipLine custom(String text, int customIndex) {
            return custom(text, customIndex, Collections.emptyList(), false);
        }

        public static TooltipLine custom(String text, int customIndex, List<Integer> activePieces, boolean fullOnly) {
            return new TooltipLine(text, true, true, customIndex, activePieces == null ? Collections.emptyList() : new ArrayList<>(activePieces), fullOnly, "");
        }

        public static TooltipLine effect(String text, List<Integer> activePieces, boolean fullOnly, String overrideKey) {
            return new TooltipLine(text, true, false, -1, activePieces == null ? Collections.emptyList() : new ArrayList<>(activePieces), fullOnly, overrideKey == null ? "" : overrideKey);
        }

        public boolean isActive(int pieceCount, int total, boolean setActive) {
            if (!iconLine) return true;
            if (pieceCount <= 0 || !setActive) return false;
            if (fullOnly) return pieceCount >= total;
            if (activePieces != null && !activePieces.isEmpty()) {
                for (Integer activePiece : activePieces) {
                    if (activePiece != null && pieceCount >= activePiece) return true;
                }
                return false;
            }
            if (customLine) return true;
            return pieceCount >= total;
        }

        public boolean hasAnyActive(int pieceCount, boolean anyActive) {
            if (activePieces != null && !activePieces.isEmpty()) return pieceCount > 0;
            if (fullOnly) return pieceCount > 0;
            return customLine ? anyActive : pieceCount > 0;
        }
    }
}
