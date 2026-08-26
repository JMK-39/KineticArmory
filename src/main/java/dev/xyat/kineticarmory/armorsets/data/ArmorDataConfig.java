package dev.xyat.kineticarmory.armorsets.data;

import dev.xyat.kineticarmory.armorsets.predicate.ConditionData;
import dev.xyat.kineticarmory.armorsets.predicate.IConditionOwner;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.TagParser;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class ArmorDataConfig {
    public transient String id;
    public String displayName = "Unnamed Set";
    public Map<String, ItemReq> equipment = new HashMap<>();
    public Map<String, List<ItemReq>> equipmentVariants = new HashMap<>();
    public List<ItemReq> curios = new ArrayList<>();
    public List<ItemReq> rejectedCurios = new ArrayList<>();

    public List<AttributeModifierData> attributes = new ArrayList<>();
    public List<PotionEffectData> potionEffects = new ArrayList<>();
    public List<AttackEffectData> attackEffects = new ArrayList<>();
    public List<CommandData> activationCommands = new ArrayList<>();
    public List<CommandData> deactivationCommands = new ArrayList<>();

    public List<DamageImmunityData> damageImmunities = new ArrayList<>();
    public List<EffectImmunityData> effectImmunities = new ArrayList<>();
    public List<DamageConversionData> damageConversions = new ArrayList<>();
    public List<DamageMultiplierData> damageMultipliers = new ArrayList<>();
    public List<AttackDamageMultiplierData> attackDamageMultipliers = new ArrayList<>();

    public boolean allowFlight = false;
    public boolean playerOnly = true;
    public boolean entityWhitelistEnabled = false;
    public List<String> allowedEntityTypes = new ArrayList<>();
    public transient ArmorEntityRule cachedEntityRule = ArmorEntityRule.empty();
    public transient int runtimeTotalPieceCount = -1;
    public transient int runtimeMinimumPieceCount = -1;
    public transient boolean runtimePieceGroupsPresent = false;
    public transient Map<String, PieceEffectRuntimeCache> runtimePieceEffectCache = Map.of();
    public transient List<RuntimeEquipmentSlot> runtimeEquipmentSlots = List.of();
    public transient List<ItemReq> runtimeCurioRequirements = List.of();
    public transient List<ItemReq> runtimeRejectedCurios = List.of();
    public List<String> tips = new ArrayList<>();
    public boolean manualTips = false;
    public List<TipLineData> tipLayout = new ArrayList<>();
    public List<String> hiddenTipKeys = new ArrayList<>();
    public Map<String, String> tipOverrides = new HashMap<>();
    public String tipKey = "shift";
    public boolean flexiblePieces = false;
    public int minimumPieces = 2;
    public List<PieceBonusGroup> pieceBonusGroups = new ArrayList<>();
    public int flightRequiredPieces = 0;
    public String flightPieceKey = "flight";
    public List<ConditionData> flightConditions = new ArrayList<>();
    public String flightConditionMatchMode = "ANY";
    public int flightConditionMinCount = 1;

    public static class AttributeModifierData implements IConditionOwner {
        public String attribute; public String uuid; public double amount; public String operation; public int requiredPieces = 0; public String pieceKey = "";
        public List<ConditionData> conditions = new ArrayList<>(); public String conditionMatchMode = "ANY"; public int conditionMinCount = 1;

        // 运行时缓存：避免激活/失效属性时反复解析 ResourceLocation、UUID、Operation
        public transient Attribute cachedAttribute;
        public transient UUID cachedUuid;
        public transient AttributeModifier.Operation cachedOperation;
        public transient boolean runtimeCacheReady = false;

        public AttributeModifierData() {}

        public void prepareCache() {
            if (runtimeCacheReady) return;
            runtimeCacheReady = true;
            ResourceLocation rl = attribute == null ? null : ResourceLocation.tryParse(attribute);
            cachedAttribute = rl == null ? null : ForgeRegistries.ATTRIBUTES.getValue(rl);
            try { cachedUuid = uuid == null ? null : UUID.fromString(uuid); } catch (Exception ignored) { cachedUuid = null; }
            cachedOperation = parseAttributeOperation(operation);
        }

        private static AttributeModifier.Operation parseAttributeOperation(String op) {
            if (op == null) return AttributeModifier.Operation.ADDITION;
            return switch (op.toUpperCase()) {
                case "MULTIPLY", "MULTIPLY_BASE" -> AttributeModifier.Operation.MULTIPLY_BASE;
                case "FINAL_MULTIPLY", "MULTIPLY_TOTAL" -> AttributeModifier.Operation.MULTIPLY_TOTAL;
                default -> AttributeModifier.Operation.ADDITION;
            };
        }

        @Override public List<ConditionData> getConditions() { return conditions; }
        @Override public String getMatchMode() { return conditionMatchMode == null ? "ANY" : conditionMatchMode; }
        @Override public void setMatchMode(String mode) { conditionMatchMode = mode; }
        @Override public int getMinCount() { return Math.max(conditionMinCount, 1); }
        @Override public void setMinCount(int count) { conditionMinCount = count; }
    }

    public static class PotionEffectData implements IConditionOwner {
        public String effectId; public int amplifier; public int duration = 15; public boolean showParticles = false; public int requiredPieces = 0; public String pieceKey = "";
        public List<ConditionData> conditions = new ArrayList<>(); public String conditionMatchMode = "ANY"; public int conditionMinCount = 1;

        public transient MobEffect cachedEffect;
        public transient int cachedDurationTicks = 1;
        public transient boolean runtimeCacheReady = false;

        public PotionEffectData() {}

        public void prepareCache() {
            if (runtimeCacheReady) return;
            runtimeCacheReady = true;
            ResourceLocation rl = effectId == null ? null : ResourceLocation.tryParse(effectId);
            cachedEffect = rl == null ? null : ForgeRegistries.MOB_EFFECTS.getValue(rl);
            cachedDurationTicks = Math.max(1, duration * 20);
        }

        @Override public List<ConditionData> getConditions() { return conditions; }
        @Override public String getMatchMode() { return conditionMatchMode == null ? "ANY" : conditionMatchMode; }
        @Override public void setMatchMode(String mode) { conditionMatchMode = mode; }
        @Override public int getMinCount() { return Math.max(conditionMinCount, 1); }
        @Override public void setMinCount(int count) { conditionMinCount = count; }
    }

    public static class AttackEffectData implements IConditionOwner {
        public String effectId; public double duration = 0.05; public int amplifier; public double chance = 1.0; public int requiredPieces = 0; public String pieceKey = "";
        public List<ConditionData> conditions = new ArrayList<>(); public String conditionMatchMode = "ANY"; public int conditionMinCount = 1;

        public transient MobEffect cachedEffect;
        public transient int cachedDurationTicks = 1;
        public transient boolean runtimeCacheReady = false;

        public AttackEffectData() {}

        public void prepareCache() {
            if (runtimeCacheReady) return;
            runtimeCacheReady = true;
            ResourceLocation rl = effectId == null ? null : ResourceLocation.tryParse(effectId);
            cachedEffect = rl == null ? null : ForgeRegistries.MOB_EFFECTS.getValue(rl);
            cachedDurationTicks = Math.max(1, (int)(duration * 20));
        }

        @Override public List<ConditionData> getConditions() { return conditions; }
        @Override public String getMatchMode() { return conditionMatchMode == null ? "ANY" : conditionMatchMode; }
        @Override public void setMatchMode(String mode) { conditionMatchMode = mode; }
        @Override public int getMinCount() { return Math.max(conditionMinCount, 1); }
        @Override public void setMinCount(int count) { conditionMinCount = count; }
    }

    public static class CommandData implements IConditionOwner {
        public String command; public int requiredPieces = 0; public String pieceKey = "";
        public List<ConditionData> conditions = new ArrayList<>(); public String conditionMatchMode = "ANY"; public int conditionMinCount = 1;
        public CommandData() {}
        @Override public List<ConditionData> getConditions() { return conditions; }
        @Override public String getMatchMode() { return conditionMatchMode == null ? "ANY" : conditionMatchMode; }
        @Override public void setMatchMode(String mode) { conditionMatchMode = mode; }
        @Override public int getMinCount() { return Math.max(conditionMinCount, 1); }
        @Override public void setMinCount(int count) { conditionMinCount = count; }
    }

    public static class PieceBonusGroup {
        public int pieces = 2;
        public List<String> effectKeys = new ArrayList<>();
        public Map<String, Double> effectValues = new HashMap<>();

        public PieceBonusGroup() {}

        public static PieceBonusGroup create(int pieces) {
            PieceBonusGroup group = new PieceBonusGroup();
            group.pieces = pieces;
            return group;
        }
    }


    public record RuntimeEquipmentSlot(String slot, List<ItemReq> variants, boolean emptyOnly) {}

    public static final class PieceEffectRuntimeCache {
        public final int[] tiers;
        public final double[] values;
        public final boolean[] hasValues;

        PieceEffectRuntimeCache(int[] tiers, double[] values, boolean[] hasValues) {
            this.tiers = tiers;
            this.values = values;
            this.hasValues = hasValues;
        }

        public int minimumTier() {
            return tiers.length == 0 ? Integer.MAX_VALUE : tiers[0];
        }

        public double resolveCumulativeValue(int pieceCount, double fallback) {
            boolean matched = false;
            double result = 0.0;
            for (int i = 0; i < tiers.length; i++) {
                if (pieceCount < tiers[i]) break;
                result += tierValue(i, fallback);
                matched = true;
            }
            return matched ? result : fallback;
        }

        public double resolveMaximumValue(int pieceCount, double fallback) {
            boolean matched = false;
            double result = Double.NEGATIVE_INFINITY;
            for (int i = 0; i < tiers.length; i++) {
                if (pieceCount < tiers[i]) break;
                result = Math.max(result, tierValue(i, fallback));
                matched = true;
            }
            return matched ? result : fallback;
        }

        public double resolveCumulativeOffsetValue(int pieceCount, double fallback, double neutralValue) {
            boolean matched = false;
            double result = neutralValue;
            for (int i = 0; i < tiers.length; i++) {
                if (pieceCount < tiers[i]) break;
                result += tierValue(i, fallback) - neutralValue;
                matched = true;
            }
            return matched ? result : fallback;
        }

        public double resolveTierValue(int tier, double fallback) {
            for (int i = 0; i < tiers.length; i++) {
                if (tiers[i] == tier) return tierValue(i, fallback);
                if (tiers[i] > tier) break;
            }
            return fallback;
        }

        private double tierValue(int index, double fallback) {
            return hasValues[index] ? values[index] : fallback;
        }
    }

    public static class TipLineData {
        public String type = "text";
        public String key = "";
        public String text = "";
        public String role = "effect";
        public List<Integer> activePieces = new ArrayList<>();
        public boolean fullOnly = false;
        public boolean iconLine = true;

        public TipLineData() {}

        public static TipLineData text(String text) {
            TipLineData data = new TipLineData();
            data.type = "text";
            data.text = text == null ? "" : text;
            data.role = "effect";
            data.iconLine = true;
            return data;
        }

        public static TipLineData generated(String key) {
            TipLineData data = new TipLineData();
            data.type = "generated";
            data.key = key == null ? "" : key;
            data.text = "";
            data.role = "effect";
            data.iconLine = true;
            return data;
        }

        public boolean isGenerated() {
            return "generated".equalsIgnoreCase(type) && key != null && !key.isBlank();
        }

        public void setActivePieces(List<Integer> pieces) {
            activePieces = new ArrayList<>();
            if (pieces == null) return;
            for (Integer piece : pieces) {
                if (piece != null && piece > 0 && !activePieces.contains(piece)) activePieces.add(piece);
            }
        }
    }

    public static class ItemReq {
        public String id;
        public String nbtMode = "NONE";
        public String nbtTag = "{}";

        // 运行时缓存：不会写入 JSON，只用于性能优化
        private transient boolean cacheReady = false;
        private transient String cachedId;
        private transient String cachedNbtMode;
        private transient String cachedNbtTag;
        private transient Item cachedItem;
        private transient CompoundTag cachedReqTag;
        private transient ItemStack cachedDisplayStack = ItemStack.EMPTY;
        private transient boolean invalidNbt = false;

        public ItemReq() {}

        public static ItemReq create(String id) {
            ItemReq req = new ItemReq();
            req.id = id;
            return req;
        }

        private void prepareCache() {
            String currentId = (id == null || id.isBlank()) ? "minecraft:air" : id;
            String currentMode = (nbtMode == null || nbtMode.isBlank()) ? "NONE" : nbtMode.toUpperCase();
            String currentTag = (nbtTag == null || nbtTag.isBlank()) ? "{}" : nbtTag;
            boolean needsItemLookup = !currentId.equalsIgnoreCase("EMPTY") && !currentId.equalsIgnoreCase("ANY") && !currentId.equals("minecraft:air");

            if (cacheReady && currentId.equals(cachedId) && currentMode.equals(cachedNbtMode) && currentTag.equals(cachedNbtTag) && (!needsItemLookup || cachedItem != null)) {
                return;
            }

            cacheReady = true;
            cachedId = currentId;
            cachedNbtMode = currentMode;
            cachedNbtTag = currentTag;
            cachedItem = null;
            cachedReqTag = null;
            cachedDisplayStack = ItemStack.EMPTY;
            invalidNbt = false;

            if (needsItemLookup) {
                ResourceLocation rl = ResourceLocation.tryParse(cachedId);
                cachedItem = rl == null ? null : ForgeRegistries.ITEMS.getValue(rl);
            }

            CompoundTag parsedTag = null;
            try {
                if (!currentTag.trim().isEmpty() && !currentTag.equals("{}")) {
                    parsedTag = TagParser.parseTag(currentTag);
                }
            } catch (Exception e) {
                if (!"NONE".equals(cachedNbtMode)) {
                    invalidNbt = true;
                }
            }

            if (!"NONE".equals(cachedNbtMode)) {
                cachedReqTag = parsedTag;
            }

            if (cachedItem != null) {
                cachedDisplayStack = new ItemStack(cachedItem);
                if (parsedTag != null) {
                    cachedDisplayStack.setTag(parsedTag.copy());
                }
            }
        }

        public ItemStack createDisplayStack() {
            prepareCache();
            return cachedDisplayStack == null ? ItemStack.EMPTY : cachedDisplayStack.copy();
        }
    }


    public static class DamageMatchCache {
        public transient String cachedKey;
        public transient boolean matchAll;
        public transient String directMsgId;
        public transient TagKey<DamageType> tagKey;

        public void prepare(String key) {
            String k = (key == null || key.isBlank()) ? "all" : key;
            if (k.equals(cachedKey)) return;
            cachedKey = k;
            matchAll = k.equalsIgnoreCase("all");
            directMsgId = null;
            tagKey = null;
            if (matchAll) return;
            if (k.startsWith("#")) {
                ResourceLocation loc = ResourceLocation.tryParse(k.substring(1));
                if (loc != null) tagKey = TagKey.create(Registries.DAMAGE_TYPE, loc);
            } else {
                directMsgId = k;
            }
        }
    }

    public static class DamageConversionData implements IConditionOwner {
        public String sourceType = "all"; public String targetType = ""; public double ratio = 0.5; public double chance = 1.0; public int requiredPieces = 0; public String pieceKey = "";
        public List<ConditionData> conditions = new ArrayList<>(); public String conditionMatchMode = "ANY"; public int conditionMinCount = 1;

        public transient DamageMatchCache sourceCache = new DamageMatchCache();

        public DamageConversionData() {}

        public void prepareCache() {
            if (sourceCache == null) sourceCache = new DamageMatchCache();
            sourceCache.prepare(sourceType);
        }
        @Override public List<ConditionData> getConditions() { return conditions; }
        @Override public String getMatchMode() { return conditionMatchMode == null ? "ANY" : conditionMatchMode; }
        @Override public void setMatchMode(String mode) { conditionMatchMode = mode; }
        @Override public int getMinCount() { return Math.max(conditionMinCount, 1); }
        @Override public void setMinCount(int count) { conditionMinCount = count; }
    }

    public static class DamageImmunityData implements IConditionOwner {
        public String damageType = ""; public double multiplier = 0.0; public int requiredPieces = 0; public String pieceKey = "";
        public List<ConditionData> conditions = new ArrayList<>(); public String conditionMatchMode = "ANY"; public int conditionMinCount = 1;

        public transient DamageMatchCache damageCache = new DamageMatchCache();

        public DamageImmunityData() {}

        public void prepareCache() {
            if (damageCache == null) damageCache = new DamageMatchCache();
            damageCache.prepare(damageType);
        }
        @Override public List<ConditionData> getConditions() { return conditions; }
        @Override public String getMatchMode() { return conditionMatchMode == null ? "ANY" : conditionMatchMode; }
        @Override public void setMatchMode(String mode) { conditionMatchMode = mode; }
        @Override public int getMinCount() { return Math.max(conditionMinCount, 1); }
        @Override public void setMinCount(int count) { conditionMinCount = count; }
    }

    public static class EffectImmunityData implements IConditionOwner {
        public String effectId = ""; public int requiredPieces = 0; public String pieceKey = "";
        public List<ConditionData> conditions = new ArrayList<>(); public String conditionMatchMode = "ANY"; public int conditionMinCount = 1;

        public transient MobEffect cachedEffect;
        public transient boolean runtimeCacheReady = false;

        public EffectImmunityData() {}

        public void prepareCache() {
            if (runtimeCacheReady) return;
            runtimeCacheReady = true;
            ResourceLocation rl = effectId == null ? null : ResourceLocation.tryParse(effectId);
            cachedEffect = rl == null ? null : ForgeRegistries.MOB_EFFECTS.getValue(rl);
        }
        @Override public List<ConditionData> getConditions() { return conditions; }
        @Override public String getMatchMode() { return conditionMatchMode == null ? "ANY" : conditionMatchMode; }
        @Override public void setMatchMode(String mode) { conditionMatchMode = mode; }
        @Override public int getMinCount() { return Math.max(conditionMinCount, 1); }
        @Override public void setMinCount(int count) { conditionMinCount = count; }
    }

    public static class DamageMultiplierData implements IConditionOwner {
        public double multiplier = 1.0; public int requiredPieces = 0; public String pieceKey = "";
        public List<ConditionData> conditions = new ArrayList<>(); public String conditionMatchMode = "ANY"; public int conditionMinCount = 1;
        public DamageMultiplierData() {}
        @Override public List<ConditionData> getConditions() { return conditions; }
        @Override public String getMatchMode() { return conditionMatchMode == null ? "ANY" : conditionMatchMode; }
        @Override public void setMatchMode(String mode) { conditionMatchMode = mode; }
        @Override public int getMinCount() { return Math.max(conditionMinCount, 1); }
        @Override public void setMinCount(int count) { conditionMinCount = count; }
    }

    public static class AttackDamageMultiplierData implements IConditionOwner {
        public double multiplier = 1.0; public int requiredPieces = 0; public String pieceKey = "";
        public List<ConditionData> conditions = new ArrayList<>(); public String conditionMatchMode = "ANY"; public int conditionMinCount = 1;
        public AttackDamageMultiplierData() {}
        @Override public List<ConditionData> getConditions() { return conditions; }
        @Override public String getMatchMode() { return conditionMatchMode == null ? "ANY" : conditionMatchMode; }
        @Override public void setMatchMode(String mode) { conditionMatchMode = mode; }
        @Override public int getMinCount() { return Math.max(conditionMinCount, 1); }
        @Override public void setMinCount(int count) { conditionMinCount = count; }
    }

    public void initNullFields() {
        if (equipment == null) equipment = new HashMap<>();
        if (equipmentVariants == null) equipmentVariants = new HashMap<>();
        normalizeEquipmentVariants();
        if (curios == null) curios = new ArrayList<>();
        if (rejectedCurios == null) rejectedCurios = new ArrayList<>();
        if (attributes == null) attributes = new ArrayList<>();
        if (potionEffects == null) potionEffects = new ArrayList<>();
        if (attackEffects == null) attackEffects = new ArrayList<>();
        if (damageImmunities == null) damageImmunities = new ArrayList<>();
        if (effectImmunities == null) effectImmunities = new ArrayList<>();
        if (damageConversions == null) damageConversions = new ArrayList<>();
        if (damageMultipliers == null) damageMultipliers = new ArrayList<>();
        if (attackDamageMultipliers == null) attackDamageMultipliers = new ArrayList<>();
        if (allowedEntityTypes == null) allowedEntityTypes = new ArrayList<>();
        if (cachedEntityRule == null) cachedEntityRule = ArmorEntityRule.empty();
        if (runtimePieceEffectCache == null) runtimePieceEffectCache = Map.of();
        if (runtimeEquipmentSlots == null) runtimeEquipmentSlots = List.of();
        if (runtimeCurioRequirements == null) runtimeCurioRequirements = List.of();
        if (runtimeRejectedCurios == null) runtimeRejectedCurios = List.of();
        if (tips == null) tips = new ArrayList<>();
        if (tipLayout == null) tipLayout = new ArrayList<>();
        if (hiddenTipKeys == null) hiddenTipKeys = new ArrayList<>();
        if (tipOverrides == null) tipOverrides = new HashMap<>();
        if (tipKey == null) tipKey = "shift";
        if (pieceBonusGroups == null) pieceBonusGroups = new ArrayList<>();
        if (flightPieceKey == null || flightPieceKey.isBlank()) flightPieceKey = "flight";
        if (activationCommands == null) activationCommands = new ArrayList<>();
        if (deactivationCommands == null) deactivationCommands = new ArrayList<>();
        if (flightConditions == null) flightConditions = new ArrayList<>();
        if (flightConditionMatchMode == null) flightConditionMatchMode = "ANY";
        if (minimumPieces < 1) minimumPieces = 2;
        if (flightRequiredPieces < 0) flightRequiredPieces = 0;
        preparePieceBonusData();
    }

    public void clearEmptyFields() {
        normalizeEquipmentVariants();
        if (equipmentVariants != null && equipmentVariants.isEmpty()) equipmentVariants = null;
        if (equipment != null && equipment.isEmpty()) equipment = null;
        if (curios != null && curios.isEmpty()) curios = null;
        if (rejectedCurios != null && rejectedCurios.isEmpty()) rejectedCurios = null;
        if (attributes != null && attributes.isEmpty()) attributes = null;
        if (potionEffects != null && potionEffects.isEmpty()) potionEffects = null;
        if (attackEffects != null && attackEffects.isEmpty()) attackEffects = null;
        if (damageImmunities != null && damageImmunities.isEmpty()) damageImmunities = null;
        if (effectImmunities != null && effectImmunities.isEmpty()) effectImmunities = null;
        if (damageConversions != null && damageConversions.isEmpty()) damageConversions = null;
        if (damageMultipliers != null && damageMultipliers.isEmpty()) damageMultipliers = null;
        if (attackDamageMultipliers != null && attackDamageMultipliers.isEmpty()) attackDamageMultipliers = null;
        if (allowedEntityTypes != null && allowedEntityTypes.isEmpty()) allowedEntityTypes = null;
        if (tips != null && tips.isEmpty()) tips = null;
        if (tipLayout != null && tipLayout.isEmpty()) tipLayout = null;
        if (hiddenTipKeys != null && hiddenTipKeys.isEmpty()) hiddenTipKeys = null;
        if (tipOverrides != null && tipOverrides.isEmpty()) tipOverrides = null;
        if (pieceBonusGroups != null && pieceBonusGroups.isEmpty()) pieceBonusGroups = null;
        if (activationCommands != null && activationCommands.isEmpty()) activationCommands = null;
        if (deactivationCommands != null && deactivationCommands.isEmpty()) deactivationCommands = null;
        if (flightConditions != null && flightConditions.isEmpty()) flightConditions = null;
    }


    /**
     * 预热运行时缓存：在配置加载/同步完成后调用一次。
     * 这样 NBT 字符串和物品 ID 会提前解析，避免第一次战斗或第一次装备检测时卡顿。
     */
    public void prepareRuntimeCache() {
        normalizeEquipmentVariants();
        cachedEntityRule = ArmorEntityRule.compile(allowedEntityTypes);
        prepareEquipmentRuntimeCache();
        preparePieceBonusData();
        preparePieceRuntimeCache();
        if (equipment != null) {
            for (ItemReq req : equipment.values()) {
                if (req != null) req.prepareCache();
            }
        }
        if (equipmentVariants != null) {
            for (List<ItemReq> variants : equipmentVariants.values()) {
                if (variants == null) continue;
                for (ItemReq req : variants) {
                    if (req != null) req.prepareCache();
                }
            }
        }
        if (curios != null) {
            for (ItemReq req : curios) {
                if (req != null) req.prepareCache();
            }
        }
        if (rejectedCurios != null) {
            for (ItemReq req : rejectedCurios) {
                if (req != null) req.prepareCache();
            }
        }
        if (attributes != null) {
            for (AttributeModifierData data : attributes) if (data != null) data.prepareCache();
        }
        if (potionEffects != null) {
            for (PotionEffectData data : potionEffects) if (data != null) data.prepareCache();
        }
        if (attackEffects != null) {
            for (AttackEffectData data : attackEffects) if (data != null) data.prepareCache();
        }
        if (damageImmunities != null) {
            for (DamageImmunityData data : damageImmunities) if (data != null) data.prepareCache();
        }
        if (effectImmunities != null) {
            for (EffectImmunityData data : effectImmunities) if (data != null) data.prepareCache();
        }
        if (damageConversions != null) {
            for (DamageConversionData data : damageConversions) if (data != null) data.prepareCache();
        }
    }


    private void prepareEquipmentRuntimeCache() {
        List<RuntimeEquipmentSlot> slots = new ArrayList<>();
        if (equipmentVariants != null) {
            for (Map.Entry<String, List<ItemReq>> entry : equipmentVariants.entrySet()) {
                List<ItemReq> variants = entry.getValue();
                if (variants == null || variants.isEmpty()) continue;
                boolean emptyOnly = isExclusiveSlotState(variants, "EMPTY");
                if (emptyOnly || countPieceRequirements(variants) > 0) {
                    slots.add(new RuntimeEquipmentSlot(entry.getKey(), List.copyOf(variants), emptyOnly));
                }
            }
        }
        runtimeEquipmentSlots = List.copyOf(slots);

        if (curios == null || curios.isEmpty()) {
            runtimeCurioRequirements = List.of();
        } else {
            List<ItemReq> counted = new ArrayList<>();
            for (ItemReq req : curios) if (isCountedPieceRequirement(req)) counted.add(req);
            runtimeCurioRequirements = List.copyOf(counted);
        }

        if (rejectedCurios == null || rejectedCurios.isEmpty()) {
            runtimeRejectedCurios = List.of();
        } else {
            List<ItemReq> rejected = new ArrayList<>();
            for (ItemReq req : rejectedCurios) if (isPieceRequirement(req)) rejected.add(req);
            runtimeRejectedCurios = List.copyOf(rejected);
        }
    }

    public List<RuntimeEquipmentSlot> getRuntimeEquipmentSlots() {
        if (runtimeEquipmentSlots == null) prepareEquipmentRuntimeCache();
        return runtimeEquipmentSlots;
    }

    public List<ItemReq> getRuntimeCurioRequirements() {
        if (runtimeCurioRequirements == null) prepareEquipmentRuntimeCache();
        return runtimeCurioRequirements;
    }

    public List<ItemReq> getRuntimeRejectedCurios() {
        if (runtimeRejectedCurios == null) prepareEquipmentRuntimeCache();
        return runtimeRejectedCurios;
    }

    public boolean isEntityAllowed(EntityType<?> entityType) {
        if (!entityWhitelistEnabled) return true;
        if (allowedEntityTypes == null || allowedEntityTypes.isEmpty()) return false;
        if (cachedEntityRule == null) cachedEntityRule = ArmorEntityRule.compile(allowedEntityTypes);
        return cachedEntityRule.matches(entityType);
    }

    public void normalizeEquipmentVariants() {
        if (equipment == null) equipment = new HashMap<>();
        if (equipmentVariants == null) equipmentVariants = new HashMap<>();

        for (Map.Entry<String, ItemReq> entry : new ArrayList<>(equipment.entrySet())) {
            String slot = entry.getKey();
            ItemReq req = entry.getValue();
            if (slot == null || slot.isBlank() || !isPieceRequirement(req)) continue;
            List<ItemReq> variants = equipmentVariants.computeIfAbsent(slot, k -> new ArrayList<>());
            if (variants.isEmpty()) {
                variants.add(req);
            }
        }

        List<String> emptySlots = new ArrayList<>();
        for (Map.Entry<String, List<ItemReq>> entry : equipmentVariants.entrySet()) {
            List<ItemReq> variants = entry.getValue();
            if (variants == null) {
                emptySlots.add(entry.getKey());
                continue;
            }
            variants.removeIf(req -> req == null || req.id == null || req.id.isBlank() || req.id.equals("minecraft:air"));
            removeDuplicateItemReqs(variants);
            if (variants.isEmpty()) {
                emptySlots.add(entry.getKey());
            } else {
                equipment.put(entry.getKey(), variants.get(0));
            }
        }
        for (String slot : emptySlots) {
            equipmentVariants.remove(slot);
            equipment.remove(slot);
        }
    }

    private static void removeDuplicateItemReqs(List<ItemReq> variants) {
        java.util.HashSet<String> seen = new java.util.HashSet<>();
        for (int i = variants.size() - 1; i >= 0; i--) {
            ItemReq req = variants.get(i);
            String key = itemReqKey(req);
            if (seen.contains(key)) variants.remove(i);
            else seen.add(key);
        }
    }

    private static String itemReqKey(ItemReq req) {
        if (req == null) return "";
        return (req.id == null ? "" : req.id) + "|" + (req.nbtMode == null ? "NONE" : req.nbtMode) + "|" + (req.nbtTag == null ? "{}" : req.nbtTag);
    }

    public List<ItemReq> getEquipmentVariants(String slot) {
        if (equipment == null) equipment = new HashMap<>();
        if (equipmentVariants == null) equipmentVariants = new HashMap<>();
        if (slot == null || slot.isBlank()) return new ArrayList<>();
        List<ItemReq> variants = equipmentVariants.computeIfAbsent(slot, k -> new ArrayList<>());
        ItemReq legacy = equipment.get(slot);
        if (variants.isEmpty() && isPieceRequirement(legacy)) variants.add(legacy);
        variants.removeIf(req -> req == null || req.id == null || req.id.isBlank() || req.id.equals("minecraft:air"));
        if (!variants.isEmpty()) equipment.put(slot, variants.get(0));
        return variants;
    }

    public void setSingleEquipmentVariant(String slot, ItemReq req) {
        if (equipment == null) equipment = new HashMap<>();
        if (equipmentVariants == null) equipmentVariants = new HashMap<>();
        List<ItemReq> variants = equipmentVariants.computeIfAbsent(slot, k -> new ArrayList<>());
        variants.clear();
        if (isPieceRequirement(req)) {
            variants.add(req);
            equipment.put(slot, req);
        } else {
            equipment.remove(slot);
            equipmentVariants.remove(slot);
        }
    }

    public ItemReq getDisplayedEquipmentReq(String slot, long timeMillis) {
        List<ItemReq> variants = getEquipmentVariants(slot);
        if (!variants.isEmpty()) {
            int index = (int) ((Math.max(0L, timeMillis) / 500L) % variants.size());
            return variants.get(index);
        }
        ItemReq req = equipment == null ? null : equipment.get(slot);
        return req == null ? ItemReq.create("minecraft:air") : req;
    }

    public boolean hasMultipleEquipmentVariants(String slot) {
        return countPieceRequirements(getEquipmentVariants(slot)) > 1;
    }

    public int countEquipmentRequirements() {
        normalizeEquipmentVariants();
        int count = 0;
        if (equipmentVariants != null) {
            for (List<ItemReq> variants : equipmentVariants.values()) {
                if (countPieceRequirements(variants) > 0) count++;
            }
        }
        return count;
    }

    public static int countPieceRequirements(List<ItemReq> reqs) {
        if (reqs == null) return 0;
        if (isExclusiveSlotState(reqs, "EMPTY")) return 0;
        int count = 0;
        for (ItemReq req : reqs) {
            if (isCountedPieceRequirement(req)) count++;
        }
        return count;
    }

    public static boolean isExclusiveSlotState(List<ItemReq> reqs, String stateId) {
        if (reqs == null || stateId == null || stateId.isBlank()) return false;
        int validCount = 0;
        boolean stateMatched = false;
        for (ItemReq req : reqs) {
            if (!isPieceRequirement(req)) continue;
            validCount++;
            if (req.id != null && req.id.equalsIgnoreCase(stateId)) stateMatched = true;
        }
        return validCount == 1 && stateMatched;
    }

    public static boolean isItemMatchingAny(ItemStack stack, List<ItemReq> reqs) {
        if (reqs == null || reqs.isEmpty()) return stack == null || stack.isEmpty();
        for (ItemReq req : reqs) {
            if (isItemMatching(stack, req)) return true;
        }
        return false;
    }

    public int getRuntimeTotalPieceCount() {
        if (runtimeTotalPieceCount < 0) preparePieceRuntimeCache();
        return runtimeTotalPieceCount;
    }

    public int getRuntimeMinimumPieces() {
        if (runtimeMinimumPieceCount < 0) preparePieceRuntimeCache();
        return runtimeMinimumPieceCount;
    }

    public int getTotalPieceCount() {
        int count = countEquipmentRequirements();
        if (curios != null) {
            for (ItemReq req : curios) {
                if (isCountedPieceRequirement(req)) count++;
            }
        }
        return count;
    }

    public int getPieceBonusGroupCount() {
        preparePieceBonusData();
        return pieceBonusGroups == null ? 0 : pieceBonusGroups.size();
    }

    public boolean isEffectAllowedByPieces(String effectKey, int pieceCount, int legacyRequiredPieces) {
        if (runtimeTotalPieceCount < 0) preparePieceRuntimeCache();
        int total = Math.max(1, runtimeTotalPieceCount);
        if (!flexiblePieces) return pieceCount >= total;
        if (pieceCount <= 0) return false;
        if (runtimePieceGroupsPresent) {
            if (effectKey == null || effectKey.isBlank()) return pieceCount >= total;
            PieceEffectRuntimeCache cache = runtimePieceEffectCache.get(effectKey);
            return cache == null ? pieceCount >= total : pieceCount >= cache.minimumTier();
        }
        int required = legacyRequiredPieces <= 0 || legacyRequiredPieces >= total ? total : Math.max(2, legacyRequiredPieces);
        return pieceCount >= required;
    }

    public double resolveCumulativePieceValue(String effectKey, int pieceCount, double fallback) {
        PieceEffectRuntimeCache cache = getPieceEffectRuntimeCache(effectKey, pieceCount);
        return cache == null ? fallback : cache.resolveCumulativeValue(pieceCount, fallback);
    }

    public double resolveHighestPieceValue(String effectKey, int pieceCount, double fallback) {
        PieceEffectRuntimeCache cache = getPieceEffectRuntimeCache(effectKey, pieceCount);
        return cache == null ? fallback : cache.resolveMaximumValue(pieceCount, fallback);
    }

    public double resolveCumulativePieceMultiplier(String effectKey, int pieceCount, double fallback) {
        PieceEffectRuntimeCache cache = getPieceEffectRuntimeCache(effectKey, pieceCount);
        return cache == null ? fallback : cache.resolveCumulativeOffsetValue(pieceCount, fallback, 1.0);
    }

    public double resolvePieceTierValue(String effectKey, int tier, double fallback) {
        if (!flexiblePieces || tier <= 0 || effectKey == null || effectKey.isBlank()) return fallback;
        if (runtimeTotalPieceCount < 0) preparePieceRuntimeCache();
        PieceEffectRuntimeCache cache = runtimePieceEffectCache.get(effectKey);
        return cache == null ? fallback : cache.resolveTierValue(tier, fallback);
    }

    private PieceEffectRuntimeCache getPieceEffectRuntimeCache(String effectKey, int pieceCount) {
        if (!flexiblePieces || pieceCount <= 0 || effectKey == null || effectKey.isBlank()) return null;
        if (runtimeTotalPieceCount < 0) preparePieceRuntimeCache();
        return runtimePieceEffectCache.get(effectKey);
    }


    public void preparePieceBonusData() {
        runtimeTotalPieceCount = -1;
        runtimeMinimumPieceCount = -1;
        runtimePieceGroupsPresent = false;
        runtimePieceEffectCache = Map.of();
        ensureAllPieceKeys();
        if (pieceBonusGroups == null) pieceBonusGroups = new ArrayList<>();
        migrateLegacyRequiredPieces();
        normalizePieceBonusGroups();
    }

    private void preparePieceRuntimeCache() {
        int total = Math.max(1, getTotalPieceCount());
        runtimeTotalPieceCount = total;

        int minimum = total;
        if (flexiblePieces && pieceBonusGroups != null && !pieceBonusGroups.isEmpty()) {
            for (PieceBonusGroup group : pieceBonusGroups) {
                if (group != null && group.pieces >= 2 && group.pieces < minimum) minimum = group.pieces;
            }
        } else if (flexiblePieces) {
            int legacy = getMinimumLegacyRequiredPieces(total);
            if (legacy > 0) minimum = legacy;
        }
        runtimeMinimumPieceCount = minimum;

        if (!flexiblePieces || pieceBonusGroups == null || pieceBonusGroups.isEmpty()) {
            runtimePieceGroupsPresent = false;
            runtimePieceEffectCache = Map.of();
            return;
        }

        runtimePieceGroupsPresent = true;
        Map<String, List<Integer>> tiersByKey = new HashMap<>();
        Map<String, List<Double>> valuesByKey = new HashMap<>();
        Map<String, List<Boolean>> hasValuesByKey = new HashMap<>();

        for (PieceBonusGroup group : pieceBonusGroups) {
            if (group == null || group.pieces < 2) continue;
            java.util.LinkedHashSet<String> keys = new java.util.LinkedHashSet<>();
            if (group.effectKeys != null) keys.addAll(group.effectKeys);
            if (group.effectValues != null) keys.addAll(group.effectValues.keySet());
            for (String key : keys) {
                if (key == null || key.isBlank()) continue;
                tiersByKey.computeIfAbsent(key, ignored -> new ArrayList<>()).add(group.pieces);
                Double value = group.effectValues == null ? null : group.effectValues.get(key);
                valuesByKey.computeIfAbsent(key, ignored -> new ArrayList<>()).add(value == null ? 0.0 : value);
                hasValuesByKey.computeIfAbsent(key, ignored -> new ArrayList<>()).add(value != null && Double.isFinite(value));
            }
        }

        Map<String, PieceEffectRuntimeCache> compiled = new HashMap<>();
        for (Map.Entry<String, List<Integer>> entry : tiersByKey.entrySet()) {
            String key = entry.getKey();
            List<Integer> tierList = entry.getValue();
            List<Double> valueList = valuesByKey.get(key);
            List<Boolean> hasValueList = hasValuesByKey.get(key);
            int size = tierList.size();
            int[] tiers = new int[size];
            double[] values = new double[size];
            boolean[] hasValues = new boolean[size];
            for (int i = 0; i < size; i++) {
                tiers[i] = tierList.get(i);
                values[i] = valueList.get(i);
                hasValues[i] = hasValueList.get(i);
            }
            compiled.put(key, new PieceEffectRuntimeCache(tiers, values, hasValues));
        }
        runtimePieceEffectCache = Map.copyOf(compiled);
    }

    public String keyOf(AttributeModifierData data) { if (data == null) return ""; data.pieceKey = ensureKey(data.pieceKey, "attr"); return data.pieceKey; }
    public String keyOf(PotionEffectData data) { if (data == null) return ""; data.pieceKey = ensureKey(data.pieceKey, "potion"); return data.pieceKey; }
    public String keyOf(AttackEffectData data) { if (data == null) return ""; data.pieceKey = ensureKey(data.pieceKey, "attack"); return data.pieceKey; }
    public String keyOf(DamageImmunityData data) { if (data == null) return ""; data.pieceKey = ensureKey(data.pieceKey, "dmg_imm"); return data.pieceKey; }
    public String keyOf(EffectImmunityData data) { if (data == null) return ""; data.pieceKey = ensureKey(data.pieceKey, "eff_imm"); return data.pieceKey; }
    public String keyOf(DamageConversionData data) { if (data == null) return ""; data.pieceKey = ensureKey(data.pieceKey, "dmg_conv"); return data.pieceKey; }
    public String keyOf(DamageMultiplierData data) { if (data == null) return ""; data.pieceKey = ensureKey(data.pieceKey, "dmg_mul"); return data.pieceKey; }
    public String keyOf(AttackDamageMultiplierData data) { if (data == null) return ""; data.pieceKey = ensureKey(data.pieceKey, "atk_dmg"); return data.pieceKey; }
    public String keyOfFlight() { flightPieceKey = ensureKey(flightPieceKey, "flight"); return flightPieceKey; }

    private void ensureAllPieceKeys() {
        if (attributes != null) for (AttributeModifierData data : attributes) keyOf(data);
        if (potionEffects != null) for (PotionEffectData data : potionEffects) keyOf(data);
        if (attackEffects != null) for (AttackEffectData data : attackEffects) keyOf(data);
        if (damageImmunities != null) for (DamageImmunityData data : damageImmunities) keyOf(data);
        if (effectImmunities != null) for (EffectImmunityData data : effectImmunities) keyOf(data);
        if (damageConversions != null) for (DamageConversionData data : damageConversions) keyOf(data);
        if (damageMultipliers != null) for (DamageMultiplierData data : damageMultipliers) keyOf(data);
        if (attackDamageMultipliers != null) for (AttackDamageMultiplierData data : attackDamageMultipliers) keyOf(data);
        if (allowFlight) keyOfFlight();
    }

    private String ensureKey(String value, String prefix) {
        if (value != null && !value.isBlank()) return value;
        return prefix + "_" + UUID.randomUUID().toString().replace("-", "");
    }

    private void migrateLegacyRequiredPieces() {
        if (pieceBonusGroups != null && !pieceBonusGroups.isEmpty()) return;
        int total = Math.max(1, getTotalPieceCount());
        Map<Integer, PieceBonusGroup> groups = new HashMap<>();
        addLegacyGroup(groups, total, attributes);
        addLegacyGroup(groups, total, potionEffects);
        addLegacyGroup(groups, total, attackEffects);
        addLegacyGroup(groups, total, damageImmunities);
        addLegacyGroup(groups, total, effectImmunities);
        addLegacyGroup(groups, total, damageConversions);
        addLegacyGroup(groups, total, damageMultipliers);
        addLegacyGroup(groups, total, attackDamageMultipliers);
        if (allowFlight && flightRequiredPieces > 0 && flightRequiredPieces < total) {
            groups.computeIfAbsent(flightRequiredPieces, PieceBonusGroup::create).effectKeys.add(keyOfFlight());
        }
        if (!groups.isEmpty()) {
            pieceBonusGroups.addAll(groups.values());
            flexiblePieces = true;
        }
    }

    private int getMinimumLegacyRequiredPieces(int total) {
        int min = total;
        min = Math.min(min, minLegacyRequired(total, attributes));
        min = Math.min(min, minLegacyRequired(total, potionEffects));
        min = Math.min(min, minLegacyRequired(total, attackEffects));
        min = Math.min(min, minLegacyRequired(total, damageImmunities));
        min = Math.min(min, minLegacyRequired(total, effectImmunities));
        min = Math.min(min, minLegacyRequired(total, damageConversions));
        min = Math.min(min, minLegacyRequired(total, damageMultipliers));
        min = Math.min(min, minLegacyRequired(total, attackDamageMultipliers));
        if (allowFlight && flightRequiredPieces > 0 && flightRequiredPieces < total) min = Math.min(min, flightRequiredPieces);
        return min < total ? min : -1;
    }

    private <T> int minLegacyRequired(int total, List<T> list) {
        if (list == null) return total;
        int min = total;
        for (T item : list) {
            int required = getLegacyRequired(item);
            if (required > 0 && required < total) min = Math.min(min, required);
        }
        return min;
    }

    private <T> void addLegacyGroup(Map<Integer, PieceBonusGroup> groups, int total, List<T> list) {
        if (list == null) return;
        for (T item : list) {
            int required = getLegacyRequired(item);
            if (required <= 0 || required >= total) continue;
            String key = getPieceKey(item);
            if (!key.isBlank()) groups.computeIfAbsent(required, PieceBonusGroup::create).effectKeys.add(key);
        }
    }

    private int getLegacyRequired(Object item) {
        if (item instanceof AttributeModifierData data) return data.requiredPieces;
        if (item instanceof PotionEffectData data) return data.requiredPieces;
        if (item instanceof AttackEffectData data) return data.requiredPieces;
        if (item instanceof DamageImmunityData data) return data.requiredPieces;
        if (item instanceof EffectImmunityData data) return data.requiredPieces;
        if (item instanceof DamageConversionData data) return data.requiredPieces;
        if (item instanceof DamageMultiplierData data) return data.requiredPieces;
        if (item instanceof AttackDamageMultiplierData data) return data.requiredPieces;
        return 0;
    }

    private String getPieceKey(Object item) {
        if (item instanceof AttributeModifierData data) return keyOf(data);
        if (item instanceof PotionEffectData data) return keyOf(data);
        if (item instanceof AttackEffectData data) return keyOf(data);
        if (item instanceof DamageImmunityData data) return keyOf(data);
        if (item instanceof EffectImmunityData data) return keyOf(data);
        if (item instanceof DamageConversionData data) return keyOf(data);
        if (item instanceof DamageMultiplierData data) return keyOf(data);
        if (item instanceof AttackDamageMultiplierData data) return keyOf(data);
        return "";
    }

    private void normalizePieceBonusGroups() {
        int total = Math.max(1, getTotalPieceCount());
        Map<Integer, PieceBonusGroup> unique = new java.util.TreeMap<>();
        if (pieceBonusGroups != null) {
            for (PieceBonusGroup group : pieceBonusGroups) {
                if (group == null || group.pieces < 2 || group.pieces > total) continue;
                PieceBonusGroup target = unique.computeIfAbsent(group.pieces, PieceBonusGroup::create);
                if (target.effectKeys == null) target.effectKeys = new ArrayList<>();
                if (target.effectValues == null) target.effectValues = new HashMap<>();
                if (group.effectKeys != null) {
                    for (String key : group.effectKeys) {
                        if (key != null && !key.isBlank() && !target.effectKeys.contains(key)) target.effectKeys.add(key);
                    }
                }
                if (group.effectValues != null) {
                    for (Map.Entry<String, Double> entry : group.effectValues.entrySet()) {
                        String key = entry.getKey();
                        Double value = entry.getValue();
                        if (key == null || key.isBlank() || value == null || !Double.isFinite(value)) continue;
                        target.effectValues.put(key, value);
                        if (!target.effectKeys.contains(key)) target.effectKeys.add(key);
                    }
                }
            }
        }
        pieceBonusGroups = new ArrayList<>(unique.values());
        minimumPieces = pieceBonusGroups.isEmpty() ? 2 : pieceBonusGroups.get(0).pieces;
        if (pieceBonusGroups.isEmpty()) flexiblePieces = false;
    }

    public static boolean isPieceRequirement(ItemReq req) {
        if (req == null || req.id == null) return false;
        String id = req.id.trim();
        return !id.isEmpty() && !id.equals("minecraft:air");
    }

    public static boolean isCountedPieceRequirement(ItemReq req) {
        if (!isPieceRequirement(req)) return false;
        return !"EMPTY".equalsIgnoreCase(req.id.trim());
    }

    public static boolean isItemMatching(ItemStack stack, ItemReq req) {
        if (req == null) return stack.isEmpty();

        req.prepareCache();

        String reqId = req.cachedId;
        if (reqId.equalsIgnoreCase("EMPTY") || reqId.equals("minecraft:air")) return stack.isEmpty();
        if (reqId.equalsIgnoreCase("ANY")) return !stack.isEmpty();
        if (stack.isEmpty()) return false;

        // 性能优化：配置加载后第一次匹配会缓存 Item，之后不再反复查注册表/拼字符串
        if (req.cachedItem == null || stack.getItem() != req.cachedItem) return false;

        String mode = req.cachedNbtMode;
        if (mode == null || mode.equals("NONE")) return true;
        if (req.invalidNbt) return false;

        CompoundTag stackTag = stack.getTag();
        CompoundTag reqTag = req.cachedReqTag;

        if (mode.equals("WEAK")) {
            if (reqTag == null || reqTag.isEmpty()) return true;
            if (stackTag == null) return false;
            return NbtUtils.compareNbt(reqTag, stackTag, true);
        } else if (mode.equals("STRONG")) {
            CompoundTag effectiveStackTag = (stackTag == null) ? new CompoundTag() : stackTag;
            CompoundTag effectiveReqTag = (reqTag == null) ? new CompoundTag() : reqTag;
            return effectiveReqTag.equals(effectiveStackTag);
        }
        return true;
    }
}
