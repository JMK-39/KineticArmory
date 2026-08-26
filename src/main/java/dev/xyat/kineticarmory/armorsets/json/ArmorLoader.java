package dev.xyat.kineticarmory.armorsets.json;

import com.google.gson.*;
import dev.xyat.kineticarmory.KineticArmory;
import dev.xyat.kineticarmory.armorsets.data.ArmorDataConfig;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.fml.loading.FMLPaths;
import net.minecraftforge.registries.ForgeRegistries;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import java.util.regex.Pattern;

public class ArmorLoader {
    public static final int MAX_SET_ID_LENGTH = 128;
    private static final Pattern SAFE_SET_ID = Pattern.compile(
            "[\\p{L}\\p{N}_-](?:[\\p{L}\\p{N}_.-]{0,126}[\\p{L}\\p{N}_-])?"
    );
    private static final Pattern WINDOWS_RESERVED_NAME = Pattern.compile(
            "(?i)(?:con|prn|aux|nul|com[1-9]|lpt[1-9])(?:\\..*)?"
    );
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();
    public static final Map<String, ArmorDataConfig> LOADED_SETS = new HashMap<>();
    public static final Map<Item, Set<ArmorDataConfig>> CACHE_ITEM_TO_SETS = new HashMap<>();
    /**
     * 需要兜底检查的套装：例如只要求 ANY/EMPTY，或者没有任何具体物品 ID 的套装。
     * 普通套装会走物品候选缓存，不再每次扫描全部套装。
     */
    public static final Set<ArmorDataConfig> CACHE_ALWAYS_CHECK_SETS = new LinkedHashSet<>();
    private static int cacheVersion = 0;
    private static volatile boolean loadedOnce = false;
    public static final List<String> FAILED_SETS = new ArrayList<>();
    public static final Path CONFIG_DIR = FMLPaths.CONFIGDIR.get().resolve("kineticcore").resolve("armorsets");

    public static void cleanUpConfig(ArmorDataConfig config) {
        config.normalizeEquipmentVariants();
        if (config.equipment != null) {
            config.equipment.entrySet().removeIf(e -> e.getValue() == null || e.getValue().id == null || e.getValue().id.equals("minecraft:air"));
            config.equipment.values().forEach(ArmorLoader::cleanItemReq);
        }
        if (config.equipmentVariants != null) {
            config.equipmentVariants.entrySet().removeIf(e -> e.getValue() == null || e.getValue().isEmpty());
            config.equipmentVariants.values().forEach(list -> {
                list.removeIf(req -> req == null || req.id == null || req.id.equals("minecraft:air"));
                list.forEach(ArmorLoader::cleanItemReq);
            });
        }
        if (config.curios != null) {
            config.curios.removeIf(req -> req == null || req.id == null || req.id.equals("minecraft:air"));
            config.curios.forEach(ArmorLoader::cleanItemReq);
        }
        if (config.rejectedCurios != null) {
            config.rejectedCurios.removeIf(req -> req == null || req.id == null || req.id.equals("minecraft:air"));
            config.rejectedCurios.forEach(ArmorLoader::cleanItemReq);
        }
        if (config.allowedEntityTypes != null) {
            LinkedHashSet<String> unique = new LinkedHashSet<>();
            for (String raw : config.allowedEntityTypes) {
                if (raw == null) continue;
                String rule = raw.trim();
                if (!rule.isEmpty()) unique.add(rule);
            }
            config.allowedEntityTypes = new ArrayList<>(unique);
        }
    }

    private static void cleanItemReq(ArmorDataConfig.ItemReq req) {
        if (req != null) {
            if ("NONE".equals(req.nbtMode)) req.nbtMode = null;
            if ("{}".equals(req.nbtTag) || "".equals(req.nbtTag)) req.nbtTag = null;
        }
    }

    public static int getCacheVersion() {
        return cacheVersion;
    }

    public static void ensureLoaded() {
        if (!loadedOnce) {
            load();
        }
    }

    public static void load() {
        LOADED_SETS.clear(); FAILED_SETS.clear();
        File folder = CONFIG_DIR.toFile();
        if (!folder.exists() && !folder.mkdirs()) {
            loadedOnce = true;
            rebuildItemCache();
            return;
        }
        File[] files = folder.listFiles((dir, name) -> name.endsWith(".json"));
        if (files == null) {
            loadedOnce = true;
            rebuildItemCache();
            return;
        }
        for (File file : files) {
            String setId = file.getName().substring(0, file.getName().length() - 5);
            if (!isSafeSetId(setId)) {
                FAILED_SETS.add(file.getName());
                KineticArmory.LOGGER.error("ArmorSet ID 不安全，已跳过: {}", file.getName());
                continue;
            }
            try (FileReader reader = new FileReader(file, StandardCharsets.UTF_8)) {
                ArmorDataConfig config = GSON.fromJson(JsonParser.parseReader(reader), ArmorDataConfig.class);
                if (config != null) {
                    config.id = setId;
                    config.initNullFields();
                    cleanUpConfig(config);
                    config.prepareRuntimeCache();
                    LOADED_SETS.put(config.id, config);
                }
            } catch (Exception e) {
                FAILED_SETS.add(file.getName());
                KineticArmory.LOGGER.error("ArmorSet 加载失败: {}", file.getName());
            }
        }
        loadedOnce = true;
        rebuildItemCache();
    }

    public static void save(String filename, ArmorDataConfig config) {
        saveChecked(filename, config);
    }

    /**
     * Persists an armor set after resolving it strictly inside the armor-set directory.
     * The boolean result lets authenticated network editors avoid publishing a failed save.
     */
    public static boolean saveChecked(String filename, ArmorDataConfig config) {
        loadedOnce = true;
        config.clearEmptyFields();
        try {
            Path root = CONFIG_DIR.toAbsolutePath().normalize();
            Files.createDirectories(root);
            Path file = resolveConfigPath(filename);
            try (Writer writer = Files.newBufferedWriter(file, StandardCharsets.UTF_8)) {
                GSON.toJson(config, writer);
            }
            return true;
        } catch (IOException | IllegalArgumentException e) {
            KineticArmory.LOGGER.error("ArmorSet 保存失败: {}", filename, e);
            return false;
        } finally {
            config.initNullFields();
        }
    }

    public static boolean isSafeSetId(String id) {
        if (id == null || id.isBlank() || id.length() > MAX_SET_ID_LENGTH) return false;
        return SAFE_SET_ID.matcher(id).matches()
                && !WINDOWS_RESERVED_NAME.matcher(id).matches();
    }

    public static Path resolveConfigPath(String id) {
        if (!isSafeSetId(id)) throw new IllegalArgumentException("Unsafe armor set id");
        Path root = CONFIG_DIR.toAbsolutePath().normalize();
        Path target = root.resolve(id + ".json").normalize();
        if (!target.startsWith(root) || !root.equals(target.getParent())) {
            throw new IllegalArgumentException("Armor set path escapes config directory");
        }
        return target;
    }

    public static void rebuildItemCache() {
        cacheVersion++;
        rebuildItemCache(LOADED_SETS, CACHE_ITEM_TO_SETS, CACHE_ALWAYS_CHECK_SETS);
    }

    public static void rebuildItemCache(
            Map<String, ArmorDataConfig> configs,
            Map<Item, Set<ArmorDataConfig>> itemCache,
            Set<ArmorDataConfig> alwaysCheckSets
    ) {
        itemCache.clear();
        alwaysCheckSets.clear();

        for (ArmorDataConfig config : configs.values()) {
            if (config == null) continue;
            config.initNullFields();
            config.prepareRuntimeCache();

            boolean hasConcreteReq = false;
            boolean hasSpecialReq = false;

            if (config.equipmentVariants != null) {
                for (List<ArmorDataConfig.ItemReq> variants : config.equipmentVariants.values()) {
                    if (variants == null) continue;
                    for (ArmorDataConfig.ItemReq req : variants) {
                        CacheResult result = addReqToCache(req, config, itemCache);
                        hasConcreteReq |= result.hasConcreteReq;
                        hasSpecialReq |= result.hasSpecialReq;
                    }
                }
            }
            if (config.curios != null) {
                for (ArmorDataConfig.ItemReq req : config.curios) {
                    CacheResult result = addReqToCache(req, config, itemCache);
                    hasConcreteReq |= result.hasConcreteReq;
                    hasSpecialReq |= result.hasSpecialReq;
                }
            }
            // 只有 ANY/EMPTY/无具体物品的套装，无法通过玩家身上的某个物品反查出来，需要兜底检查。
            if (!hasConcreteReq || hasSpecialReq) {
                alwaysCheckSets.add(config);
            }
        }
    }

    public static void collectCandidateSets(Iterable<ItemStack> stacks, Set<ArmorDataConfig> target) {
        target.clear();
        target.addAll(CACHE_ALWAYS_CHECK_SETS);
        if (stacks == null) return;
        for (ItemStack stack : stacks) {
            if (stack == null || stack.isEmpty()) continue;
            Set<ArmorDataConfig> sets = CACHE_ITEM_TO_SETS.get(stack.getItem());
            if (sets != null) target.addAll(sets);
        }
    }

    private record CacheResult(boolean hasConcreteReq, boolean hasSpecialReq) {}

    private static CacheResult addReqToCache(
            ArmorDataConfig.ItemReq req,
            ArmorDataConfig config,
            Map<Item, Set<ArmorDataConfig>> itemCache
    ) {
        if (req == null || req.id == null) return new CacheResult(false, false);
        String id = req.id.trim();
        if (id.isEmpty() || id.equals("minecraft:air") || id.equalsIgnoreCase("ANY") || id.equalsIgnoreCase("EMPTY")) {
            return new CacheResult(false, true);
        }
        ResourceLocation rl = ResourceLocation.tryParse(id);
        if (rl != null && ForgeRegistries.ITEMS.containsKey(rl)) {
            Item item = ForgeRegistries.ITEMS.getValue(rl);
            if (item != null) {
                Set<ArmorDataConfig> sets = itemCache.computeIfAbsent(item, k -> new LinkedHashSet<>());
                sets.add(config);
                return new CacheResult(true, false);
            }
        }
        return new CacheResult(false, true);
    }
}
