package dev.xyat.kineticarmory.armorsets.event;

import dev.xyat.kineticarmory.util.ColorText;
import dev.xyat.kineticarmory.KineticArmory;
import dev.xyat.kineticarmory.armorsets.Network.ArmorNetwork;
import dev.xyat.kineticarmory.armorsets.config.ArmorConfig;
import dev.xyat.kineticarmory.armorsets.data.ArmorDataConfig;
import dev.xyat.kineticarmory.armorsets.json.ArmorLoader;
import dev.xyat.kineticarmory.armorsets.predicate.ConditionData;
import dev.xyat.kineticarmory.armorsets.predicate.ConditionEvaluator;
import dev.xyat.kineticcore.feature.flight.api.FlightAPI;
import top.theillusivec4.curios.api.event.CurioChangeEvent;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.event.entity.living.*;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.server.ServerStartedEvent;
import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.server.ServerLifecycleHooks;
import top.theillusivec4.curios.api.CuriosApi;
import top.theillusivec4.curios.api.type.inventory.IDynamicStackHandler;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class ArmorManager {

    private static boolean eventsRegistered;

    public static synchronized void registerEvents() {
        if (eventsRegistered) return;
        eventsRegistered = true;
        MinecraftForge.EVENT_BUS.addListener(ArmorManager::onLivingTick);
        MinecraftForge.EVENT_BUS.addListener(ArmorManager::onCurioChange);
        MinecraftForge.EVENT_BUS.addListener(ArmorManager::onEquipmentChange);
        MinecraftForge.EVENT_BUS.addListener(ArmorManager::onPlayerLogin);
        MinecraftForge.EVENT_BUS.addListener(ArmorManager::onPlayerLogout);
        MinecraftForge.EVENT_BUS.addListener(ArmorManager::onLivingDeath);
        MinecraftForge.EVENT_BUS.addListener(ArmorManager::onPlayerRespawn);
        MinecraftForge.EVENT_BUS.addListener(ArmorManager::onDimensionChange);
        MinecraftForge.EVENT_BUS.addListener(ArmorManager::onServerStarted);
        MinecraftForge.EVENT_BUS.addListener(ArmorManager::onServerTick);
        MinecraftForge.EVENT_BUS.addListener(ArmorManager::onEntityJoin);
        MinecraftForge.EVENT_BUS.addListener(ArmorManager::onLivingHurt);
        MinecraftForge.EVENT_BUS.addListener(ArmorManager::onLivingDamage);
        MinecraftForge.EVENT_BUS.addListener(ArmorManager::onLivingAttack);
        MinecraftForge.EVENT_BUS.addListener(ArmorManager::onPotionApplicable);
    }

    private static final String NBT_KEY_ACTIVE_SETS = "kt_active_armorsets";
    private static final String NBT_KEY_ACTIVE_SET_PIECES = "kt_active_armorsets_pieces";
    private static final ThreadLocal<Boolean> IS_CONVERTING_DAMAGE = ThreadLocal.withInitial(() -> false);
    private static final Map<UUID, Set<String>> DYNAMIC_ACTIVE_STATES = new ConcurrentHashMap<>();

    private static final Map<UUID, ActiveEntityState> ACTIVE_STATE_CACHE = new ConcurrentHashMap<>();

    // 伤害转换目标 msgId -> DamageType Key 缓存，避免每次伤害转换都遍历整个 DamageType 注册表
    private static final Map<String, Optional<ResourceKey<DamageType>>> DAMAGE_TARGET_KEY_CACHE = new ConcurrentHashMap<>();

    // 装备签名缓存：如果装备/饰品/套装配置版本都没变，就不重复计算候选套装和 NBT 匹配
    private static final Map<UUID, Long> EQUIPMENT_SIGNATURE_CACHE = new ConcurrentHashMap<>();
    private static final Map<UUID, CurrentSetResult> CALCULATED_SET_CACHE = new ConcurrentHashMap<>();
    private static final ThreadLocal<CalculationScratch> CALCULATION_SCRATCH = ThreadLocal.withInitial(CalculationScratch::new);
    private static final Map<UUID, ClientInputState> CLIENT_INPUT_STATES = new ConcurrentHashMap<>();
    private static final Map<UUID, Integer> PENDING_LOGIN_RECHECKS = new ConcurrentHashMap<>();

    private record ClientInputState(int leftTicks, int rightTicks, boolean leftClick, boolean rightClick) {}
    private record ActiveSetRuntime(ArmorDataConfig config, int pieceCount) {}
    private record ActiveEntityState(Set<String> ids, Map<String, Integer> pieceCounts, List<ActiveSetRuntime> runtimes) {}

    private static final class CalculationScratch {
        final List<ItemStack> curios = new ArrayList<>();
        final List<ItemStack> candidateStacks = new ArrayList<>();
        final LinkedHashSet<ArmorDataConfig> candidateSets = new LinkedHashSet<>();
        final List<ArmorDataConfig> satisfiedSets = new ArrayList<>();
        final Set<String> satisfiedIds = new HashSet<>();
        final Map<String, Integer> pieceCounts = new HashMap<>();
        final Set<String> toRemove = new HashSet<>();
        final Set<String> toAdd = new HashSet<>();
        final Set<String> toRefresh = new HashSet<>();
        final long[] hashHolder = new long[1];
        boolean[] usedCurios = new boolean[0];

        boolean[] prepareUsedCurios(int size) {
            if (usedCurios.length < size) usedCurios = new boolean[Math.max(size, usedCurios.length * 2 + 4)];
            Arrays.fill(usedCurios, 0, size, false);
            return usedCurios;
        }
    }

    public static void updateClientInputState(ServerPlayer player, int leftTicks, int rightTicks, boolean leftClick, boolean rightClick) {
        if (!ArmorConfig.enableSets) return;
        CLIENT_INPUT_STATES.put(player.getUUID(), new ClientInputState(Math.max(0, leftTicks), Math.max(0, rightTicks), leftClick, rightClick));
        refreshDynamicConditions(player);
    }

    public static void forceRecalculateAll(MinecraftServer server) {
        if (server == null) return;
        List<LivingEntity> loadedEntities = new ArrayList<>();
        for (ServerLevel level : server.getAllLevels()) {
            for (Entity entity : level.getAllEntities()) {
                if (entity instanceof LivingEntity living) loadedEntities.add(living);
            }
        }

        if (!ArmorConfig.enableSets) {
            for (LivingEntity entity : loadedEntities) deactivateEntity(entity);
            clearAllRuntimeCaches();
            return;
        }

        clearAllRuntimeCaches();
        for (LivingEntity entity : loadedEntities) updateEntitySets(entity, true);
    }

    private static void deactivateEntity(LivingEntity entity) {
        List<ActiveSetRuntime> active = getActiveRuntimeSets(entity);
        for (ActiveSetRuntime runtime : active) {
            removeAttributes(entity, runtime.config());
            executeCommands(entity, runtime.config(), runtime.config().deactivationCommands, runtime.pieceCount());
        }
        if (entity instanceof ServerPlayer player) FlightAPI.removeFlightSource(player, "armor_set");
        persistSets(entity, Set.of(), Map.of());
        DYNAMIC_ACTIVE_STATES.remove(entity.getUUID());
    }

    public static void clearAllRuntimeCaches() {
        DYNAMIC_ACTIVE_STATES.clear();
        ACTIVE_STATE_CACHE.clear();
        EQUIPMENT_SIGNATURE_CACHE.clear();
        CALCULATED_SET_CACHE.clear();
        CLIENT_INPUT_STATES.clear();
        PENDING_LOGIN_RECHECKS.clear();
    }

    public static boolean checkEffectConditions(LivingEntity entity, List<ConditionData> conds, String mode, int minCount) {
        if (conds == null || conds.isEmpty()) return true;

        String m = mode == null ? "ANY" : mode;

        if (!"ALL".equalsIgnoreCase(m) && !"MIN".equalsIgnoreCase(m) && !"MIN_COUNT".equalsIgnoreCase(m)) {
            for (ConditionData cond : conds) {
                if (checkSingleCondition(entity, cond)) return true;
            }
            return false;
        }

        if ("ALL".equalsIgnoreCase(m)) {
            for (ConditionData cond : conds) {
                if (!checkSingleCondition(entity, cond)) return false;
            }
            return true;
        }

        int need = Math.max(1, minCount);
        int met = 0;
        for (ConditionData cond : conds) {
            if (checkSingleCondition(entity, cond) && ++met >= need) return true;
        }
        return false;
    }

    private static boolean checkSingleCondition(LivingEntity entity, ConditionData cond) {
        if (cond == null || cond.type == null) return false;
        String type = cond.type.toUpperCase(Locale.ROOT);
        boolean result;

        if (type.equals("MOUSE_LEFT_HOLD") || type.equals("MOUSE_RIGHT_HOLD") || type.equals("MOUSE_LEFT_CLICK") || type.equals("MOUSE_RIGHT_CLICK")) {
            ClientInputState state = CLIENT_INPUT_STATES.get(entity.getUUID());
            if (state == null) state = new ClientInputState(0, 0, false, false);

            result = switch (type) {
                case "MOUSE_LEFT_HOLD" -> state.leftTicks() >= parseInt(cond.params == null ? null : cond.params.get("ticks"));
                case "MOUSE_RIGHT_HOLD" -> state.rightTicks() >= parseInt(cond.params == null ? null : cond.params.get("ticks"));
                case "MOUSE_LEFT_CLICK" -> state.leftClick();
                case "MOUSE_RIGHT_CLICK" -> state.rightClick();
                default -> false;
            };
            return cond.invert != result;
        }

        return ConditionEvaluator.checkConditions(entity, Collections.singletonList(cond));
    }

    private static int parseInt(String value) {
        try {
            return value == null ? 1 : Integer.parseInt(value);
        } catch (Exception ignored) {
            return 1;
        }
    }

    private static int getConfiguredPotionDurationTicks(ArmorDataConfig.PotionEffectData pot) {
        return pot == null ? 1 : Math.max(1, pot.cachedDurationTicks);
    }

    private static int getConfiguredPotionAmplifier(ArmorDataConfig config, ArmorDataConfig.PotionEffectData pot, int pieceCount) {
        if (config == null || pot == null) return 0;
        return Math.max(0, (int)Math.round(config.resolveHighestPieceValue(config.keyOf(pot), pieceCount, pot.amplifier)));
    }

    private static int getConfiguredAttackEffectAmplifier(ArmorDataConfig config, ArmorDataConfig.AttackEffectData effect, int pieceCount) {
        if (config == null || effect == null) return 0;
        return Math.max(0, (int)Math.round(config.resolveHighestPieceValue(config.keyOf(effect), pieceCount, effect.amplifier)));
    }

    private static double getConfiguredAttributeAmount(ArmorDataConfig config, ArmorDataConfig.AttributeModifierData data, int pieceCount) {
        if (config == null || data == null) return 0.0;
        return config.resolveCumulativePieceValue(config.keyOf(data), pieceCount, data.amount);
    }

    private static double getConfiguredDamageImmunityMultiplier(ArmorDataConfig config, ArmorDataConfig.DamageImmunityData data, int pieceCount) {
        if (config == null || data == null) return 0.0;
        return config.resolveCumulativePieceMultiplier(config.keyOf(data), pieceCount, data.multiplier);
    }

    private static double getConfiguredDamageMultiplier(ArmorDataConfig config, ArmorDataConfig.DamageMultiplierData data, int pieceCount) {
        if (config == null || data == null) return 1.0;
        return config.resolveCumulativePieceMultiplier(config.keyOf(data), pieceCount, data.multiplier);
    }

    private static double getConfiguredAttackDamageMultiplier(ArmorDataConfig config, ArmorDataConfig.AttackDamageMultiplierData data, int pieceCount) {
        if (config == null || data == null) return 1.0;
        return config.resolveCumulativePieceMultiplier(config.keyOf(data), pieceCount, data.multiplier);
    }

    private static double getConfiguredDamageConversionRatio(ArmorDataConfig config, ArmorDataConfig.DamageConversionData data, int pieceCount) {
        if (config == null || data == null) return 0.0;
        return config.resolveCumulativePieceValue(config.keyOf(data), pieceCount, data.ratio);
    }

    public static void handleDynamicEffectSync(LivingEntity entity, ArmorDataConfig config, String type, String effectId, boolean active, int pieceCount) {
        if (config == null) return;
        String stateKey = config.id + "_" + type + "_" + effectId;
        Set<String> states = DYNAMIC_ACTIVE_STATES.computeIfAbsent(entity.getUUID(), k -> ConcurrentHashMap.newKeySet());

        if (active) {
            states.add(stateKey);
            if (type.equals("ATTRIBUTE") && config.attributes != null) {
                for (ArmorDataConfig.AttributeModifierData attr : config.attributes) {
                    if (Objects.equals(attr.uuid, effectId)) {
                        applyOrRefreshAttribute(entity, config, attr, Math.max(0, pieceCount), "Dynamic Armor Bonus");
                    }
                }
            }
        } else {
            if (states.remove(stateKey)) {
                if (type.equals("ATTRIBUTE") && config.attributes != null) {
                    for (ArmorDataConfig.AttributeModifierData attr : config.attributes) {
                        if (Objects.equals(attr.uuid, effectId)) {
                            removeSingleAttribute(entity, attr);
                        }
                    }
                }
            }
        }
    }

    public static void onLivingTick(LivingEvent.LivingTickEvent event) {
        if (!ArmorConfig.enableSets) return;
        LivingEntity entity = event.getEntity();
        if (entity.level().isClientSide()) return;
        int interval = Math.max(1, ArmorConfig.potionRefreshInterval);
        if (entity.tickCount % interval != 0 || !isEntityAllowed(entity)) return;

        List<ActiveSetRuntime> activeSets = getActiveRuntimeSets(entity);
        if (activeSets.isEmpty()) return;

        for (ActiveSetRuntime runtime : activeSets) {
            ArmorDataConfig config = runtime.config();
            int pieceCount = runtime.pieceCount();

            if (config.attributes != null) {
                for (ArmorDataConfig.AttributeModifierData attr : config.attributes) {
                    handleDynamicEffectSync(entity, config, "ATTRIBUTE", attr.uuid, isEffectActive(entity, config, pieceCount, config.keyOf(attr), attr.requiredPieces, attr.conditions, attr.conditionMatchMode, attr.conditionMinCount), pieceCount);
                }
            }

            if (config.attackEffects != null) {
                for (ArmorDataConfig.AttackEffectData atk : config.attackEffects) {
                    handleDynamicEffectSync(entity, config, "ATTACK", atk.effectId, isEffectActive(entity, config, pieceCount, config.keyOf(atk), atk.requiredPieces, atk.conditions, atk.conditionMatchMode, atk.conditionMinCount), pieceCount);
                }
            }

            if (config.potionEffects != null) {
                for (ArmorDataConfig.PotionEffectData pot : config.potionEffects) {
                    boolean isMet = isEffectActive(entity, config, pieceCount, config.keyOf(pot), pot.requiredPieces, pot.conditions, pot.conditionMatchMode, pot.conditionMinCount);
                    handleDynamicEffectSync(entity, config, "POTION", pot.effectId, isMet, pieceCount);
                    if (isMet) {
                        MobEffect effect = pot.cachedEffect;
                        if (effect != null) entity.addEffect(new MobEffectInstance(effect, getConfiguredPotionDurationTicks(pot), getConfiguredPotionAmplifier(config, pot, pieceCount), false, pot.showParticles));
                    }
                }
            }
        }

        if (entity instanceof ServerPlayer player) syncFlight(player, activeSets);
    }

    private static boolean isEffectActive(LivingEntity entity, ArmorDataConfig config, int pieceCount, String effectKey, int legacyRequiredPieces, List<ConditionData> conditions, String mode, int minCount) {
        return hasRequiredPieces(config, pieceCount, effectKey, legacyRequiredPieces) && checkEffectConditions(entity, conditions, mode, minCount);
    }

    private static boolean hasRequiredPieces(ArmorDataConfig config, int pieceCount, String effectKey, int legacyRequiredPieces) {
        if (config == null) return false;
        return config.isEffectAllowedByPieces(effectKey, pieceCount, legacyRequiredPieces);
    }

    public static void onCurioChange(CurioChangeEvent event) { if (ArmorConfig.enableSets) updateEntitySets(event.getEntity()); }

    public static void onEquipmentChange(LivingEquipmentChangeEvent event) {
        if (!ArmorConfig.enableSets) return;
        updateEntitySets(event.getEntity());
    }

    public static void onPlayerLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) handlePlayerLogin(player);
    }

    public static void onPlayerLogout(PlayerEvent.PlayerLoggedOutEvent event) { clearRuntimeCache(event.getEntity()); }
    public static void onLivingDeath(LivingDeathEvent event) { clearRuntimeCache(event.getEntity()); }
    public static void onPlayerRespawn(PlayerEvent.PlayerRespawnEvent event) { if (ArmorConfig.enableSets) updateEntitySets(event.getEntity(), true); }
    public static void onDimensionChange(PlayerEvent.PlayerChangedDimensionEvent event) { if (ArmorConfig.enableSets) updateEntitySets(event.getEntity(), true); }

    private static void clearRuntimeCache(LivingEntity entity) {
        UUID uuid = entity.getUUID();
        DYNAMIC_ACTIVE_STATES.remove(uuid);
        ACTIVE_STATE_CACHE.remove(uuid);
        EQUIPMENT_SIGNATURE_CACHE.remove(uuid);
        CALCULATED_SET_CACHE.remove(uuid);
        CLIENT_INPUT_STATES.remove(uuid);
        PENDING_LOGIN_RECHECKS.remove(uuid);
    }

    public static void onServerStarted(ServerStartedEvent event) {
        ArmorLoader.load();
        forceRecalculateAll(event.getServer());
    }

    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (!ArmorConfig.enableSets) {
            PENDING_LOGIN_RECHECKS.clear();
            return;
        }
        if (event.phase != TickEvent.Phase.END || PENDING_LOGIN_RECHECKS.isEmpty()) return;
        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        if (server == null || server.getTickCount() % 20 != 0) return;

        Iterator<Map.Entry<UUID, Integer>> iterator = PENDING_LOGIN_RECHECKS.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<UUID, Integer> entry = iterator.next();
            ServerPlayer player = server.getPlayerList().getPlayer(entry.getKey());
            if (player == null) {
                iterator.remove();
                continue;
            }
            updateEntitySets(player, true);
            int remaining = entry.getValue() - 1;
            if (remaining <= 0) {
                iterator.remove();
            } else {
                entry.setValue(remaining);
            }
        }
    }

    public static void handlePlayerLogin(ServerPlayer player) {
        ArmorLoader.ensureLoaded();
        ArmorNetwork.CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), new ArmorNetwork.SyncEntityFilterPacket(ArmorConfig.entityFilterMode, ArmorConfig.allowedEntities, false));
        ArmorNetwork.CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), new ArmorNetwork.SyncArmorConfigsPacket(ArmorLoader.LOADED_SETS, false));
        if (ArmorConfig.enableSets) {
            updateEntitySets(player, true);
            PENDING_LOGIN_RECHECKS.put(player.getUUID(), 3);
        }
    }

    public static void onEntityJoin(EntityJoinLevelEvent event) {
        if (!ArmorConfig.enableSets) return;
        if (event.getEntity() instanceof LivingEntity entity && !(entity instanceof ServerPlayer) && !entity.level().isClientSide()) {
            updateEntitySets(entity, true);
        }
    }

    public static void updateEntitySets(LivingEntity entity) { updateEntitySets(entity, false); }

    public static void updateEntitySets(LivingEntity entity, boolean forceSync) {
        try {
            if (!ArmorConfig.enableSets || entity.level().isClientSide()) return;
            ArmorLoader.ensureLoaded();
            if (!isEntityAllowed(entity)) {
                if (!getPersistedSets(entity).isEmpty()) deactivateEntity(entity);
                return;
            }
            Set<String> previousSetIds = getPersistedSets(entity);
            Map<String, Integer> previousPieceCounts = getPersistedPieceCounts(entity);
            CurrentSetResult currentResult = getCurrentSetsWithCache(entity, forceSync);
            List<ArmorDataConfig> currentSets = currentResult.sets();
            Set<String> currentSetIds = currentResult.ids();
            Map<String, Integer> currentPieceCounts = currentResult.pieceCounts();

            boolean pieceChanged = !previousPieceCounts.equals(currentPieceCounts);
            if (!previousSetIds.equals(currentSetIds) || pieceChanged || forceSync) {
                CalculationScratch scratch = CALCULATION_SCRATCH.get();
                scratch.toRemove.clear();
                scratch.toRemove.addAll(previousSetIds);
                scratch.toRemove.removeAll(currentSetIds);
                scratch.toAdd.clear();
                scratch.toAdd.addAll(currentSetIds);
                scratch.toAdd.removeAll(previousSetIds);
                scratch.toRefresh.clear();
                scratch.toRefresh.addAll(currentSetIds);
                scratch.toRefresh.retainAll(previousSetIds);
                scratch.toRefresh.removeIf(id -> Objects.equals(previousPieceCounts.get(id), currentPieceCounts.get(id)) && !forceSync);

                for (String id : scratch.toRemove) {
                    ArmorDataConfig config = ArmorLoader.LOADED_SETS.get(id);
                    if (config != null) {
                        removeAttributes(entity, config);
                        executeCommands(entity, config, config.deactivationCommands, previousPieceCounts.getOrDefault(id, 0));
                    }
                    removeDynamicStatePrefix(entity.getUUID(), id + "_");
                    MinecraftForge.EVENT_BUS.post(new ArmorEvents.StatusChange(entity, id, false));
                }

                for (String id : scratch.toAdd) {
                    ArmorDataConfig config = ArmorLoader.LOADED_SETS.get(id);
                    int pieceCount = currentPieceCounts.getOrDefault(id, 0);
                    if (config != null) {
                        applyAttributes(entity, config);
                        executeCommands(entity, config, config.activationCommands, pieceCount);
                        evaluateImmediateConditions(entity, config, pieceCount);
                    }
                    MinecraftForge.EVENT_BUS.post(new ArmorEvents.StatusChange(entity, id, true));
                }

                for (String id : scratch.toRefresh) {
                    ArmorDataConfig config = ArmorLoader.LOADED_SETS.get(id);
                    if (config != null) {
                        int pieceCount = currentPieceCounts.getOrDefault(id, 0);
                        int oldPieceCount = previousPieceCounts.getOrDefault(id, 0);
                        if (oldPieceCount != pieceCount) {
                            removeAttributes(entity, config);
                            removeDynamicStatePrefix(entity.getUUID(), id + "_ATTRIBUTE_");
                        }
                        evaluateImmediateConditions(entity, config, pieceCount);
                    }
                }

                if (forceSync) {
                    for (String id : currentSetIds) {
                        ArmorDataConfig config = ArmorLoader.LOADED_SETS.get(id);
                        if (config != null) applyAttributes(entity, config);
                    }
                }

                if (entity instanceof ServerPlayer player) {
                    syncFlight(player, currentSets, currentPieceCounts);
                    ArmorNetwork.CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), new ArmorNetwork.SyncActiveSetsPacket(new ArrayList<>(currentSetIds), currentPieceCounts));
                }

                persistSets(entity, currentSetIds, currentPieceCounts);
                if (currentSetIds.isEmpty()) DYNAMIC_ACTIVE_STATES.remove(entity.getUUID());
            }
        } catch (Exception e) {
            KineticArmory.LOGGER.error("ArmorSets 状态检查异常", e);
        }
    }

    private static void removeDynamicStatePrefix(UUID uuid, String prefix) {
        Set<String> states = DYNAMIC_ACTIVE_STATES.get(uuid);
        if (states != null) states.removeIf(key -> key.startsWith(prefix));
    }

    private static void refreshDynamicConditions(LivingEntity entity) {
        if (!ArmorConfig.enableSets || entity == null || entity.level().isClientSide() || !isEntityAllowed(entity)) return;
        List<ActiveSetRuntime> activeSets = getActiveRuntimeSets(entity);
        if (activeSets.isEmpty()) return;
        for (ActiveSetRuntime runtime : activeSets) evaluateImmediateConditions(entity, runtime.config(), runtime.pieceCount());
        if (entity instanceof ServerPlayer player) syncFlight(player, activeSets);
    }

    private static void evaluateImmediateConditions(LivingEntity entity, ArmorDataConfig config, int pieceCount) {
        if (config.attributes != null) {
            for (ArmorDataConfig.AttributeModifierData attr : config.attributes) {
                handleDynamicEffectSync(entity, config, "ATTRIBUTE", attr.uuid, isEffectActive(entity, config, pieceCount, config.keyOf(attr), attr.requiredPieces, attr.conditions, attr.conditionMatchMode, attr.conditionMinCount), pieceCount);
            }
        }
        if (config.potionEffects != null) {
            for (ArmorDataConfig.PotionEffectData pot : config.potionEffects) {
                boolean met = isEffectActive(entity, config, pieceCount, config.keyOf(pot), pot.requiredPieces, pot.conditions, pot.conditionMatchMode, pot.conditionMinCount);
                handleDynamicEffectSync(entity, config, "POTION", pot.effectId, met, pieceCount);
                if (met) {
                    MobEffect effect = pot.cachedEffect;
                    if (effect != null) entity.addEffect(new MobEffectInstance(effect, pot.cachedDurationTicks, getConfiguredPotionAmplifier(config, pot, pieceCount), false, pot.showParticles));
                }
            }
        }
        if (config.attackEffects != null) {
            for (ArmorDataConfig.AttackEffectData atk : config.attackEffects) {
                handleDynamicEffectSync(entity, config, "ATTACK", atk.effectId, isEffectActive(entity, config, pieceCount, config.keyOf(atk), atk.requiredPieces, atk.conditions, atk.conditionMatchMode, atk.conditionMinCount), pieceCount);
            }
        }
    }

    private static void syncFlight(ServerPlayer player, List<ArmorDataConfig> current, Map<String, Integer> pieceCounts) {
        boolean now = false;
        for (ArmorDataConfig set : current) {
            if (set != null && set.allowFlight) {
                int pieceCount = pieceCounts.getOrDefault(set.id, 0);
                if (isEffectActive(player, set, pieceCount, set.keyOfFlight(), set.flightRequiredPieces, set.flightConditions, set.flightConditionMatchMode, set.flightConditionMinCount)) {
                    now = true;
                    break;
                }
            }
        }
        setFlightState(player, now);
    }

    private static void syncFlight(ServerPlayer player, List<ActiveSetRuntime> activeSets) {
        boolean now = false;
        for (ActiveSetRuntime runtime : activeSets) {
            ArmorDataConfig set = runtime.config();
            if (set.allowFlight && isEffectActive(player, set, runtime.pieceCount(), set.keyOfFlight(), set.flightRequiredPieces, set.flightConditions, set.flightConditionMatchMode, set.flightConditionMinCount)) {
                now = true;
                break;
            }
        }
        setFlightState(player, now);
    }

    private static void setFlightState(ServerPlayer player, boolean enabled) {
        if (enabled) FlightAPI.addFlightSource(player, "armor_set");
        else FlightAPI.removeFlightSource(player, "armor_set");
    }

    public static void onLivingHurt(LivingHurtEvent event) {
        if (!ArmorConfig.enableSets) return;
        LivingEntity victim = event.getEntity();
        if (isEntityAllowed(victim)) {
            float damageBonus = 0.0f;
            for (ActiveSetRuntime runtime : getActiveRuntimeSets(victim)) {
                ArmorDataConfig config = runtime.config();
                int pieceCount = runtime.pieceCount();
                if (config.damageImmunities != null) {
                    for (ArmorDataConfig.DamageImmunityData imm : config.damageImmunities) {
                        if (isEffectActive(victim, config, pieceCount, config.keyOf(imm), imm.requiredPieces, imm.conditions, imm.conditionMatchMode, imm.conditionMinCount) && isDamageMatch(event.getSource(), imm.damageCache)) {
                            damageBonus += ((float) getConfiguredDamageImmunityMultiplier(config, imm, pieceCount) - 1.0f);
                        }
                    }
                }
                if (config.damageMultipliers != null) {
                    for (ArmorDataConfig.DamageMultiplierData dm : config.damageMultipliers) {
                        if (isEffectActive(victim, config, pieceCount, config.keyOf(dm), dm.requiredPieces, dm.conditions, dm.conditionMatchMode, dm.conditionMinCount)) {
                            damageBonus += ((float) getConfiguredDamageMultiplier(config, dm, pieceCount) - 1.0f);
                        }
                    }
                }
            }
            float totalMultiplier = Math.max(0.0f, 1.0f + damageBonus);
            if (totalMultiplier != 1.0f) event.setAmount(event.getAmount() * totalMultiplier);
        }
        handleDamageConversion(event);
    }

    public static void onLivingDamage(LivingDamageEvent event) {
        if (!ArmorConfig.enableSets) return;
        if (event.getSource().getEntity() instanceof LivingEntity attacker && isEntityAllowed(attacker)) {
            float damageBonus = 0.0f;
            for (ActiveSetRuntime runtime : getActiveRuntimeSets(attacker)) {
                ArmorDataConfig config = runtime.config();
                if (config.attackDamageMultipliers == null) continue;
                int pieceCount = runtime.pieceCount();
                for (ArmorDataConfig.AttackDamageMultiplierData adm : config.attackDamageMultipliers) {
                    if (isEffectActive(attacker, config, pieceCount, config.keyOf(adm), adm.requiredPieces, adm.conditions, adm.conditionMatchMode, adm.conditionMinCount)) {
                        damageBonus += ((float) getConfiguredAttackDamageMultiplier(config, adm, pieceCount) - 1.0f);
                    }
                }
            }
            float totalMultiplier = Math.max(0.0f, 1.0f + damageBonus);
            if (totalMultiplier != 1.0f) event.setAmount(event.getAmount() * totalMultiplier);
        }
    }

    public static void onLivingAttack(LivingAttackEvent event) {
        if (!ArmorConfig.enableSets) return;
        LivingEntity victim = event.getEntity();
        if (isEntityAllowed(victim)) {
            for (ActiveSetRuntime runtime : getActiveRuntimeSets(victim)) {
                ArmorDataConfig config = runtime.config();
                if (config.damageImmunities == null) continue;
                int pieceCount = runtime.pieceCount();
                for (ArmorDataConfig.DamageImmunityData entry : config.damageImmunities) {
                    if (getConfiguredDamageImmunityMultiplier(config, entry, pieceCount) <= 0.0 && isEffectActive(victim, config, pieceCount, config.keyOf(entry), entry.requiredPieces, entry.conditions, entry.conditionMatchMode, entry.conditionMinCount) && isDamageMatch(event.getSource(), entry.damageCache)) {
                        event.setCanceled(true);
                        return;
                    }
                }
            }
        }
        if (event.getSource().getEntity() instanceof LivingEntity attacker && isEntityAllowed(attacker)) {
            Set<String> dynamicStates = DYNAMIC_ACTIVE_STATES.getOrDefault(attacker.getUUID(), Collections.emptySet());
            for (ActiveSetRuntime runtime : getActiveRuntimeSets(attacker)) {
                ArmorDataConfig config = runtime.config();
                if (config.attackEffects == null) continue;
                for (ArmorDataConfig.AttackEffectData ae : config.attackEffects) {
                    boolean isMet = dynamicStates.contains(config.id + "_ATTACK_" + ae.effectId);
                    if (isMet && attacker.getRandom().nextDouble() < ae.chance) {
                        MobEffect effect = ae.cachedEffect;
                        if (effect != null) victim.addEffect(new MobEffectInstance(effect, ae.cachedDurationTicks, getConfiguredAttackEffectAmplifier(config, ae, runtime.pieceCount())));
                    }
                }
            }
        }
    }

    public static void onPotionApplicable(MobEffectEvent.Applicable event) {
        if (!ArmorConfig.enableSets) return;
        LivingEntity entity = event.getEntity();
        if (!isEntityAllowed(entity)) return;
        MobEffect incomingEffect = event.getEffectInstance().getEffect();
        for (ActiveSetRuntime runtime : getActiveRuntimeSets(entity)) {
            ArmorDataConfig config = runtime.config();
            if (config.effectImmunities == null) continue;
            int pieceCount = runtime.pieceCount();
            for (ArmorDataConfig.EffectImmunityData eff : config.effectImmunities) {
                if (eff.cachedEffect == incomingEffect && isEffectActive(entity, config, pieceCount, config.keyOf(eff), eff.requiredPieces, eff.conditions, eff.conditionMatchMode, eff.conditionMinCount)) {
                    event.setResult(Event.Result.DENY);
                    return;
                }
            }
        }
    }

    private static void executeCommands(LivingEntity entity, ArmorDataConfig config, List<ArmorDataConfig.CommandData> commands, int pieceCount) {
        if (commands == null || commands.isEmpty() || entity.level().isClientSide()) return;
        MinecraftServer server = entity.getServer();
        if (server == null) return;
        CommandSourceStack source = server.createCommandSourceStack().withPermission(2).withSuppressedOutput();
        for (ArmorDataConfig.CommandData cd : commands) {
            if (cd != null && isEffectActive(entity, config, pieceCount, cd.pieceKey, cd.requiredPieces, cd.conditions, cd.conditionMatchMode, cd.conditionMinCount)) {
                server.getCommands().performPrefixedCommand(source, cd.command.replace("@p", entity.getScoreboardName()));
            }
        }
    }

    private static boolean isDamageMatch(DamageSource source, ArmorDataConfig.DamageMatchCache cache) {
        if (cache == null) return true;
        if (cache.matchAll) return true;
        if (cache.directMsgId != null) return cache.directMsgId.equals(source.getMsgId());
        return cache.tagKey != null && source.is(cache.tagKey);
    }

    private static ResourceKey<DamageType> getDamageTypeKeyByMsgId(LivingEntity entity, String msgId) {
        if (msgId == null || msgId.isEmpty()) return null;
        Optional<ResourceKey<DamageType>> cached = DAMAGE_TARGET_KEY_CACHE.get(msgId);
        if (cached != null) return cached.orElse(null);

        var registry = entity.level().registryAccess().registryOrThrow(Registries.DAMAGE_TYPE);
        for (var regEntry : registry.entrySet()) {
            if (regEntry.getValue().msgId().equals(msgId)) {
                ResourceKey<DamageType> key = ResourceKey.create(Registries.DAMAGE_TYPE, regEntry.getKey().location());
                DAMAGE_TARGET_KEY_CACHE.put(msgId, Optional.of(key));
                return key;
            }
        }
        DAMAGE_TARGET_KEY_CACHE.put(msgId, Optional.empty());
        return null;
    }

    private static void applyAttributes(LivingEntity entity, ArmorDataConfig set) {
        if (set.flexiblePieces || set.attributes == null) return;
        int pieceCount = set.getRuntimeTotalPieceCount();
        for (ArmorDataConfig.AttributeModifierData data : set.attributes) {
            if (data.conditions != null && !data.conditions.isEmpty()) continue;
            applyOrRefreshAttribute(entity, set, data, pieceCount, "ArmorSet Bonus");
        }
    }

    private static void applyOrRefreshAttribute(LivingEntity entity, ArmorDataConfig set, ArmorDataConfig.AttributeModifierData data, int pieceCount, String modifierName) {
        try {
            data.prepareCache();
            Attribute attr = data.cachedAttribute;
            UUID uuid = data.cachedUuid;
            if (attr == null || uuid == null) {
                KineticArmory.LOGGER.warn("套装 [{}] 填写的属性 [{}] 或 UUID [{}] 无效, 无法添加!", set.id, data.attribute, data.uuid);
                return;
            }
            AttributeInstance inst = entity.getAttribute(attr);
            if (inst == null) return;
            double amount = getConfiguredAttributeAmount(set, data, pieceCount);
            double value = "SET".equalsIgnoreCase(data.operation) ? amount - inst.getBaseValue() : amount;
            AttributeModifier.Operation operation = "SET".equalsIgnoreCase(data.operation) ? AttributeModifier.Operation.ADDITION : data.cachedOperation;
            AttributeModifier existing = inst.getModifier(uuid);
            if (existing != null && (Double.compare(existing.getAmount(), value) != 0 || existing.getOperation() != operation)) {
                inst.removeModifier(uuid);
                existing = null;
            }
            if (existing == null) {
                inst.addTransientModifier(new AttributeModifier(uuid, modifierName, value, operation));
            }
        } catch (Exception e) {
            KineticArmory.LOGGER.error("实体 [{}] 添加或刷新属性 [{}] 时出错!", entity.getName().getString(), data.attribute, e);
        }
    }

    private static void removeSingleAttribute(LivingEntity entity, ArmorDataConfig.AttributeModifierData data) {
        try {
            data.prepareCache();
            Attribute attr = data.cachedAttribute;
            UUID uuid = data.cachedUuid;
            if (attr == null || uuid == null) return;
            AttributeInstance inst = entity.getAttribute(attr);
            if (inst != null && inst.getModifier(uuid) != null) inst.removeModifier(uuid);
        } catch (Exception e) {
            KineticArmory.LOGGER.error("实体 [{}] 清除属性 [{}] 时发生了错误", entity.getName().getString(), data.attribute, e);
        }
    }

    private static void removeAttributes(LivingEntity entity, ArmorDataConfig set) {
        if (set.attributes == null) return;
        for (ArmorDataConfig.AttributeModifierData data : set.attributes) {
            removeSingleAttribute(entity, data);
        }
    }

    private static void handleDamageConversion(LivingHurtEvent event) {
        if (IS_CONVERTING_DAMAGE.get() || !(event.getSource().getEntity() instanceof LivingEntity attacker) || !isEntityAllowed(attacker)) return;
        List<ActiveSetRuntime> activeSets = getActiveRuntimeSets(attacker);
        if (activeSets.isEmpty()) return;

        float originalAmount = event.getAmount();
        float remainingAmount = originalAmount;
        Map<String, Float> newDamages = null;

        outer:
        for (ActiveSetRuntime runtime : activeSets) {
            ArmorDataConfig config = runtime.config();
            if (config.damageConversions == null) continue;
            for (ArmorDataConfig.DamageConversionData conv : config.damageConversions) {
                if (conv.targetType == null || conv.targetType.isEmpty()) continue;
                if (!isEffectActive(attacker, config, runtime.pieceCount(), config.keyOf(conv), conv.requiredPieces, conv.conditions, conv.conditionMatchMode, conv.conditionMinCount)) continue;
                if (remainingAmount <= 0) break outer;
                if (attacker.getRandom().nextDouble() > conv.chance || event.getSource().getMsgId().equals(conv.targetType)) continue;
                if (!isDamageMatch(event.getSource(), conv.sourceCache)) continue;

                float actualConvert = Math.min(remainingAmount, originalAmount * (float) getConfiguredDamageConversionRatio(config, conv, runtime.pieceCount()));
                if (actualConvert <= 0) continue;
                remainingAmount -= actualConvert;
                if (newDamages == null) newDamages = new HashMap<>();
                newDamages.merge(conv.targetType, actualConvert, Float::sum);
            }
        }

        if (remainingAmount >= originalAmount || newDamages == null || newDamages.isEmpty()) return;
        event.setAmount(Math.max(0, remainingAmount));
        if (event.getAmount() <= 0) event.setCanceled(true);
        IS_CONVERTING_DAMAGE.set(true);
        try {
            var registry = attacker.level().registryAccess().registryOrThrow(Registries.DAMAGE_TYPE);
            for (Map.Entry<String, Float> entry : newDamages.entrySet()) {
                if (entry.getValue() <= 0) continue;
                ResourceKey<DamageType> targetKey = getDamageTypeKeyByMsgId(attacker, entry.getKey());
                if (targetKey == null) continue;
                var targetHolder = registry.getHolder(targetKey).orElse(null);
                if (targetHolder != null) {
                    event.getEntity().invulnerableTime = 0;
                    event.getEntity().hurt(new DamageSource(targetHolder, attacker), entry.getValue());
                }
            }
        } finally {
            IS_CONVERTING_DAMAGE.set(false);
        }
    }

    private record CurrentSetResult(List<ArmorDataConfig> sets, Set<String> ids, Map<String, Integer> pieceCounts) {}

    private static CurrentSetResult getCurrentSetsWithCache(LivingEntity entity, boolean forceRecalculate) {
        UUID uuid = entity.getUUID();
        CalculationScratch scratch = CALCULATION_SCRATCH.get();
        long signature = buildEquipmentSnapshot(entity, scratch);
        Long cachedSignature = EQUIPMENT_SIGNATURE_CACHE.get(uuid);

        if (!forceRecalculate && cachedSignature != null && cachedSignature == signature) {
            CurrentSetResult cached = CALCULATED_SET_CACHE.get(uuid);
            if (cached != null) return cached;
        }

        CurrentSetResult calculated = calculateActiveSets(entity, scratch);
        EQUIPMENT_SIGNATURE_CACHE.put(uuid, signature);
        CALCULATED_SET_CACHE.put(uuid, calculated);
        return calculated;
    }

    private static long buildEquipmentSnapshot(LivingEntity entity, CalculationScratch scratch) {
        scratch.curios.clear();
        scratch.candidateStacks.clear();

        long hash = 0xcbf29ce484222325L;
        hash = mixHash(hash, ArmorLoader.getCacheVersion());
        ItemStack head = entity.getItemBySlot(EquipmentSlot.HEAD);
        ItemStack chest = entity.getItemBySlot(EquipmentSlot.CHEST);
        ItemStack legs = entity.getItemBySlot(EquipmentSlot.LEGS);
        ItemStack feet = entity.getItemBySlot(EquipmentSlot.FEET);
        ItemStack mainHand = entity.getMainHandItem();
        ItemStack offhand = entity.getOffhandItem();

        scratch.candidateStacks.add(head);
        scratch.candidateStacks.add(chest);
        scratch.candidateStacks.add(legs);
        scratch.candidateStacks.add(feet);
        scratch.candidateStacks.add(mainHand);
        scratch.candidateStacks.add(offhand);

        hash = mixStack(hash, head);
        hash = mixStack(hash, chest);
        hash = mixStack(hash, legs);
        hash = mixStack(hash, feet);
        hash = mixStack(hash, mainHand);
        hash = mixStack(hash, offhand);

        scratch.hashHolder[0] = hash;
        CuriosApi.getCuriosInventory(entity).ifPresent(handler -> handler.getCurios().forEach((slotId, stackHandler) -> {
            scratch.hashHolder[0] = mixHash(scratch.hashHolder[0], slotId.hashCode());
            IDynamicStackHandler stacks = stackHandler.getStacks();
            scratch.hashHolder[0] = mixHash(scratch.hashHolder[0], stacks.getSlots());
            for (int i = 0; i < stacks.getSlots(); i++) {
                ItemStack stack = stacks.getStackInSlot(i);
                scratch.hashHolder[0] = mixStack(scratch.hashHolder[0], stack);
                if (!stack.isEmpty()) {
                    scratch.curios.add(stack);
                    scratch.candidateStacks.add(stack);
                }
            }
        }));
        return scratch.hashHolder[0];
    }

    private static long mixStack(long hash, ItemStack stack) {
        if (stack == null || stack.isEmpty()) return mixHash(hash, 0);
        hash = mixHash(hash, System.identityHashCode(stack.getItem()));
        hash = mixHash(hash, stack.getCount());
        hash = mixHash(hash, stack.hasTag() && stack.getTag() != null ? stack.getTag().hashCode() : 0);
        return hash;
    }

    private static long mixHash(long hash, int value) {
        hash ^= value;
        hash *= 0x100000001b3L;
        return hash;
    }

    private static CurrentSetResult calculateActiveSets(LivingEntity entity, CalculationScratch scratch) {
        scratch.satisfiedSets.clear();
        scratch.satisfiedIds.clear();
        scratch.pieceCounts.clear();

        ArmorLoader.collectCandidateSets(scratch.candidateStacks, scratch.candidateSets);
        for (ArmorDataConfig set : scratch.candidateSets) {
            if (set.playerOnly && !(entity instanceof Player)) continue;
            if (!set.isEntityAllowed(entity.getType())) continue;
            int matchedPieces = getMatchedPieceCount(set, entity, scratch.curios, scratch);
            if (matchedPieces < 0) continue;

            ItemStack conflictCurio = getConflictingRejectedCurio(set, scratch.curios);
            if (conflictCurio == null) {
                scratch.satisfiedSets.add(set);
                scratch.satisfiedIds.add(set.id);
                scratch.pieceCounts.put(set.id, matchedPieces);
            } else if (entity instanceof ServerPlayer player) {
                player.displayClientMessage(ColorText.translatable("msg.kineticarmory.armorsets.rejected", ColorText.translatable(conflictCurio.getDescriptionId())), true);
            }
        }
        return new CurrentSetResult(List.copyOf(scratch.satisfiedSets), Set.copyOf(scratch.satisfiedIds), Map.copyOf(scratch.pieceCounts));
    }

    private static int getMatchedPieceCount(ArmorDataConfig set, LivingEntity entity, List<ItemStack> curios, CalculationScratch scratch) {
        int total = set.getRuntimeTotalPieceCount();
        if (total <= 0) return -1;

        int matched = 0;
        for (ArmorDataConfig.RuntimeEquipmentSlot slot : set.getRuntimeEquipmentSlots()) {
            ItemStack slotStack = getSlotStack(entity, slot.slot());
            if (slot.emptyOnly()) {
                if (!slotStack.isEmpty()) return -1;
                continue;
            }
            if (ArmorDataConfig.isItemMatchingAny(slotStack, slot.variants())) {
                matched++;
            } else if (!set.flexiblePieces) {
                return -1;
            }
        }

        List<ArmorDataConfig.ItemReq> curioRequirements = set.getRuntimeCurioRequirements();
        if (!curioRequirements.isEmpty()) {
            boolean[] used = scratch.prepareUsedCurios(curios.size());
            for (ArmorDataConfig.ItemReq req : curioRequirements) {
                int foundIndex = -1;
                for (int i = 0; i < curios.size(); i++) {
                    if (!used[i] && ArmorDataConfig.isItemMatching(curios.get(i), req)) {
                        foundIndex = i;
                        break;
                    }
                }
                if (foundIndex >= 0) {
                    used[foundIndex] = true;
                    matched++;
                } else if (!set.flexiblePieces) {
                    return -1;
                }
            }
        }

        int required = set.flexiblePieces ? set.getRuntimeMinimumPieces() : total;
        return matched >= required ? matched : -1;
    }

    private static ItemStack getSlotStack(LivingEntity entity, String slot) {
        if (slot == null) return ItemStack.EMPTY;
        return switch (slot) {
            case "head" -> entity.getItemBySlot(EquipmentSlot.HEAD);
            case "chest" -> entity.getItemBySlot(EquipmentSlot.CHEST);
            case "legs" -> entity.getItemBySlot(EquipmentSlot.LEGS);
            case "feet" -> entity.getItemBySlot(EquipmentSlot.FEET);
            case "mainhand" -> entity.getMainHandItem();
            case "offhand" -> entity.getOffhandItem();
            default -> ItemStack.EMPTY;
        };
    }

    private static ItemStack getConflictingRejectedCurio(ArmorDataConfig set, List<ItemStack> curios) {
        List<ArmorDataConfig.ItemReq> rejected = set.getRuntimeRejectedCurios();
        if (rejected.isEmpty()) return null;
        for (ArmorDataConfig.ItemReq req : rejected) {
            for (ItemStack curioStack : curios) {
                if (ArmorDataConfig.isItemMatching(curioStack, req)) return curioStack;
            }
        }
        return null;
    }

    private static ActiveEntityState getActiveState(LivingEntity entity) {
        UUID uuid = entity.getUUID();
        ActiveEntityState cached = ACTIVE_STATE_CACHE.get(uuid);
        if (cached != null) return cached;

        Set<String> sets = new HashSet<>();
        ListTag list = entity.getPersistentData().getList(NBT_KEY_ACTIVE_SETS, Tag.TAG_STRING);
        for (Tag tag : list) sets.add(tag.getAsString());

        Map<String, Integer> pieces = new HashMap<>();
        CompoundTag tag = entity.getPersistentData().getCompound(NBT_KEY_ACTIVE_SET_PIECES);
        for (String key : tag.getAllKeys()) pieces.put(key, Math.max(0, tag.getInt(key)));

        Set<String> readonlySets = Set.copyOf(sets);
        Map<String, Integer> readonlyPieces = Map.copyOf(pieces);
        ActiveEntityState state = new ActiveEntityState(readonlySets, readonlyPieces, buildActiveRuntimeSets(readonlySets, readonlyPieces));
        ACTIVE_STATE_CACHE.put(uuid, state);
        return state;
    }

    private static List<ActiveSetRuntime> getActiveRuntimeSets(LivingEntity entity) {
        return getActiveState(entity).runtimes();
    }

    private static List<ActiveSetRuntime> buildActiveRuntimeSets(Set<String> ids, Map<String, Integer> pieceCounts) {
        if (ids == null || ids.isEmpty()) return List.of();
        List<ActiveSetRuntime> runtimes = new ArrayList<>(ids.size());
        for (String id : ids) {
            ArmorDataConfig config = ArmorLoader.LOADED_SETS.get(id);
            if (config != null) runtimes.add(new ActiveSetRuntime(config, pieceCounts.getOrDefault(id, 0)));
        }
        return List.copyOf(runtimes);
    }

    private static Set<String> getPersistedSets(LivingEntity entity) {
        return getActiveState(entity).ids();
    }

    private static Map<String, Integer> getPersistedPieceCounts(LivingEntity entity) {
        return getActiveState(entity).pieceCounts();
    }

    private static void persistSets(LivingEntity entity, Set<String> sets, Map<String, Integer> pieceCounts) {
        ListTag list = new ListTag();
        for (String s : sets) list.add(StringTag.valueOf(s));
        entity.getPersistentData().put(NBT_KEY_ACTIVE_SETS, list);

        CompoundTag pieceTag = new CompoundTag();
        for (String s : sets) {
            pieceTag.putInt(s, Math.max(0, pieceCounts.getOrDefault(s, 0)));
        }
        entity.getPersistentData().put(NBT_KEY_ACTIVE_SET_PIECES, pieceTag);

        UUID uuid = entity.getUUID();
        Set<String> readonlySets = Set.copyOf(sets);
        Map<String, Integer> readonlyPieces = Map.copyOf(pieceCounts);
        ACTIVE_STATE_CACHE.put(uuid, new ActiveEntityState(readonlySets, readonlyPieces, buildActiveRuntimeSets(readonlySets, readonlyPieces)));
    }

    public static boolean isEntityAllowed(LivingEntity entity) {
        return ArmorConfig.isEntityAllowed(entity);
    }
}
