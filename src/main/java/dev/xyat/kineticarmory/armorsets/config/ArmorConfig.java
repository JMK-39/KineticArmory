package dev.xyat.kineticarmory.armorsets.config;

import com.electronwill.nightconfig.core.file.CommentedFileConfig;
import com.electronwill.nightconfig.core.io.WritingMode;
import dev.xyat.kineticarmory.KineticArmory;
import dev.xyat.kineticarmory.armorsets.data.ArmorEntityRule;
import dev.xyat.kineticarmory.armorsets.event.ArmorManager;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.fml.loading.FMLPaths;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class ArmorConfig {
    private static final Path CONFIG_PATH = FMLPaths.CONFIGDIR.get().resolve("kineticcore/armorsets.toml");
    private static CommentedFileConfig configData;
    private static volatile ArmorEntityRule entityRuleCache = ArmorEntityRule.empty();
    private static volatile boolean entityFilterAllowAll = true;
    private static volatile boolean entityFilterDenyAll = false;

    public static boolean enableSets = true;
    public static int potionRefreshInterval = 20;
    public static String defaultTipKey = "shift";
    public static boolean syncOnReload = true;
    public static String entityFilterMode = "BLACKLIST";
    public static List<String> allowedEntities = new ArrayList<>();

    public static void load() {
        ArmorManager.registerEvents();
        try {
            configData = CommentedFileConfig.builder(CONFIG_PATH)
                    .sync()
                    .preserveInsertionOrder()
                    .writingMode(WritingMode.REPLACE)
                    .build();
            configData.load();
            setupConfig();
            configData.save();
            readValues();
            rebuildEntityRuleCache();
        } catch (Exception e) {
            KineticArmory.LOGGER.error("ArmorConfig Load Failed", e);
        }
    }

    private static void setupConfig() {
        configData.setComment("armorsets", """
         套装系统全局设置。
         Armor Sets Global Settings.""");
        define("armorsets.enableSets", true, """
         是否启用自定义套装系统。
         Whether to enable the custom armor set system.""");
        define("armorsets.potionRefreshInterval", 20, """
         药水效果刷新间隔 (Tick)。
         Potion effect refresh interval (Ticks).
         默认 20 (每秒发放一次药水Buff)。
         Default 20 (Grants potion buffs once per second).""");
        define("armorsets.syncOnReload", true, """
         重载时是否同步。
         Whether to sync during reload.
         当管理员执行 /kt reload 时，是否强制同步套装配置给所有在线玩家。
         Whether to force synchronization of armor set configs to all online players when /kt reload is executed.""");
        define("armorsets.entityFilterMode", "BLACKLIST", """
         全局实体过滤模式：WHITELIST 或 BLACKLIST。玩家不受全局实体过滤限制。
         Global entity filter mode: WHITELIST or BLACKLIST. Players bypass the global entity filter.""");
        define("armorsets.allowedEntities", new ArrayList<>(), """
         全局实体过滤列表。玩家默认允许，无需添加。
         Global entity filter list. Players are allowed by default and do not need to be added.
         使用 ALL 表示所有实体，@ 表示指定模组，# 表示实体标签，单个 id 表示指定实体。
         Use ALL for all entities, @ for a mod namespace, # for an entity tag, or a single entity id.""");
    }

    private static void define(String path, Object def, String comment) {
        if (!configData.contains(path)) configData.set(path, def);
        configData.setComment(path, " " + comment.trim());
    }

    private static void readValues() {
        enableSets = configData.getOrElse("armorsets.enableSets", true);
        potionRefreshInterval = configData.getOrElse("armorsets.potionRefreshInterval", 20);
        defaultTipKey = configData.getOrElse("armorsets.defaultTipKey", "shift");
        syncOnReload = configData.getOrElse("armorsets.syncOnReload", true);
        entityFilterMode = normalizeMode(configData.getOrElse("armorsets.entityFilterMode", "BLACKLIST"));
        allowedEntities = new ArrayList<>(configData.getOrElse("armorsets.allowedEntities", new ArrayList<>()));
    }

    public static void reloadFromDisk() {
        if (configData == null) {
            load();
            return;
        }
        try {
            configData.load();
            setupConfig();
            configData.save();
            readValues();
            rebuildEntityRuleCache();
        } catch (Exception e) {
            KineticArmory.LOGGER.error("ArmorConfig Reload Failed", e);
        }
    }

    public static void save() {
        entityFilterMode = normalizeMode(entityFilterMode);
        allowedEntities = sanitizeRules(allowedEntities);
        if (configData != null) {
            configData.set("armorsets.enableSets", enableSets);
            configData.set("armorsets.potionRefreshInterval", potionRefreshInterval);
            configData.set("armorsets.syncOnReload", syncOnReload);
            configData.set("armorsets.entityFilterMode", entityFilterMode);
            configData.set("armorsets.allowedEntities", allowedEntities);
            configData.save();
        }
        rebuildEntityRuleCache();
    }

    /** Removes the client-only legacy key after its value seeds the CLIENT spec. */
    public static void removeLegacyClientSetting() {
        if (configData == null || !configData.contains("armorsets.defaultTipKey")) return;
        configData.remove("armorsets.defaultTipKey");
        configData.save();
    }

    public static void applySyncedEntityFilter(String mode, List<String> rules) {
        entityFilterMode = normalizeMode(mode);
        allowedEntities = sanitizeRules(rules);
        rebuildEntityRuleCache();
    }

    public static void rebuildEntityRuleCache() {
        entityFilterMode = normalizeMode(entityFilterMode);
        allowedEntities = sanitizeRules(allowedEntities);
        boolean containsAll = allowedEntities.stream().anyMatch("ALL"::equalsIgnoreCase);
        if ("BLACKLIST".equals(entityFilterMode)) {
            entityFilterAllowAll = allowedEntities.isEmpty();
            entityFilterDenyAll = containsAll;
        } else {
            entityFilterAllowAll = containsAll;
            entityFilterDenyAll = allowedEntities.isEmpty();
        }
        entityRuleCache = entityFilterAllowAll || entityFilterDenyAll ? ArmorEntityRule.empty() : ArmorEntityRule.compile(allowedEntities);
    }

    public static boolean isEntityAllowed(LivingEntity entity) {
        if (entity instanceof Player || entityFilterAllowAll) return true;
        if (entityFilterDenyAll) return false;
        boolean listed = entityRuleCache.matches(entity.getType());
        return "BLACKLIST".equals(entityFilterMode) != listed;
    }

    private static List<String> sanitizeRules(List<String> rules) {
        if (rules == null || rules.isEmpty()) return new ArrayList<>();
        java.util.LinkedHashSet<String> unique = new java.util.LinkedHashSet<>();
        for (String raw : rules) {
            if (raw == null) continue;
            String rule = raw.trim();
            if (!rule.isEmpty()) unique.add(rule);
        }
        return new ArrayList<>(unique);
    }

    private static String normalizeMode(String mode) {
        return "BLACKLIST".equalsIgnoreCase(mode) ? "BLACKLIST" : "WHITELIST";
    }
}
