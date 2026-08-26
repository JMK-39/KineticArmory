package dev.xyat.kineticarmory.armorsets.client;

import dev.xyat.kineticarmory.armorsets.data.ArmorDataConfig;
import dev.xyat.kineticarmory.armorsets.json.ArmorLoader;
import net.minecraft.world.item.Item;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@OnlyIn(Dist.CLIENT)
public final class ArmorClientSnapshot {
    private static final Map<String, ArmorDataConfig> CONFIGS = new HashMap<>();
    private static final Map<Item, Set<ArmorDataConfig>> ITEM_CACHE = new HashMap<>();
    private static final Set<ArmorDataConfig> ALWAYS_CHECK_SETS = new LinkedHashSet<>();
    private static String entityFilterMode = "BLACKLIST";
    private static List<String> entityFilterRules = List.of();

    private ArmorClientSnapshot() {
    }

    public static void replaceConfigs(Map<String, ArmorDataConfig> configs) {
        CONFIGS.clear();
        if (configs != null) CONFIGS.putAll(configs);
        rebuildItemCache();
    }

    public static List<ArmorDataConfig> configs() {
        return new ArrayList<>(CONFIGS.values());
    }

    public static void put(ArmorDataConfig config) {
        if (config == null || config.id == null) return;
        CONFIGS.put(config.id, config);
        rebuildItemCache();
    }

    public static void remove(String id) {
        if (id == null) return;
        CONFIGS.remove(id);
        rebuildItemCache();
    }

    public static Set<ArmorDataConfig> configsForItem(Item item) {
        Set<ArmorDataConfig> configs = ITEM_CACHE.get(item);
        return configs == null ? Set.of() : Set.copyOf(configs);
    }

    public static Set<ArmorDataConfig> alwaysCheckSets() {
        return Set.copyOf(ALWAYS_CHECK_SETS);
    }

    public static void replaceEntityFilter(String mode, List<String> rules) {
        entityFilterMode = "BLACKLIST".equalsIgnoreCase(mode) ? "BLACKLIST" : "WHITELIST";
        if (rules == null || rules.isEmpty()) {
            entityFilterRules = List.of();
            return;
        }
        LinkedHashSet<String> sanitized = new LinkedHashSet<>();
        for (String rule : rules) {
            if (rule == null) continue;
            String trimmed = rule.trim();
            if (!trimmed.isEmpty()) sanitized.add(trimmed);
        }
        entityFilterRules = List.copyOf(sanitized);
    }

    public static String entityFilterMode() {
        return entityFilterMode;
    }

    public static List<String> entityFilterRules() {
        return entityFilterRules;
    }

    public static void clear() {
        CONFIGS.clear();
        ITEM_CACHE.clear();
        ALWAYS_CHECK_SETS.clear();
        entityFilterMode = "BLACKLIST";
        entityFilterRules = List.of();
        ArmorCache.clear();
    }

    private static void rebuildItemCache() {
        ArmorLoader.rebuildItemCache(CONFIGS, ITEM_CACHE, ALWAYS_CHECK_SETS);
    }
}
