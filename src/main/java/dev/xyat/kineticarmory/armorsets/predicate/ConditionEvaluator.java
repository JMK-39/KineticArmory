package dev.xyat.kineticarmory.armorsets.predicate;

import dev.xyat.kineticarmory.KineticArmory;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.registries.ForgeRegistries;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Locale;

public final class ConditionEvaluator {
    private static Method gameStageMethod;
    private static boolean gameStageMethodSearched;

    private ConditionEvaluator() {
    }

    private static boolean checkGameStage(Player player, String stage) {
        if (!gameStageMethodSearched) {
            gameStageMethodSearched = true;
            try {
                Class<?> helper = Class.forName("net.darkhax.gamestages.GameStageHelper");
                gameStageMethod = helper.getMethod("hasStage", Player.class, String.class);
            } catch (ReflectiveOperationException ignored) {
                gameStageMethod = null;
            }
        }

        if (gameStageMethod != null) {
            try {
                return (boolean) gameStageMethod.invoke(null, player, stage);
            } catch (ReflectiveOperationException ignored) {
                gameStageMethod = null;
            }
        }

        return player.getTags().contains(stage);
    }

    public static boolean checkConditions(LivingEntity entity, List<ConditionData> conditions) {
        if (conditions == null || conditions.isEmpty()) return true;

        for (ConditionData condition : conditions) {
            if (evaluateSingle(entity, condition)) return true;
        }

        return false;
    }

    private static boolean evaluateSingle(LivingEntity entity, ConditionData condition) {
        if (condition == null || condition.type == null) return false;

        String type = condition.type.toUpperCase(Locale.ROOT);
        double min = parseDouble(condition.params.get("min"), 0.0);
        double max = parseDouble(condition.params.get("max"), Double.MAX_VALUE);
        String id = condition.params.getOrDefault("id", "minecraft:air");

        boolean result = false;
        Level level = entity.level();
        long time = level.getDayTime() % 24000L;

        try {
            switch (type) {
                case "IN_AIR" -> result = !entity.onGround();
                case "SNEAKING" -> result = entity.isCrouching();
                case "IN_WATER" -> result = entity.isInWater();
                case "ON_FIRE" -> result = entity.isOnFire();
                case "CLIMBING" -> result = entity.onClimbable();
                case "FALLING" -> result = entity.getDeltaMovement().y < -0.01;
                case "RISING" -> result = entity.getDeltaMovement().y > 0.01;
                case "SPEED_RANGE" -> {
                    double speed = entity.getDeltaMovement().horizontalDistance();
                    result = speed >= min && speed <= max;
                }
                case "ON_BLOCK" -> {
                    BlockPos pos = entity.blockPosition().below();
                    BlockState state = level.getBlockState(pos);
                    ResourceLocation blockId = ForgeRegistries.BLOCKS.getKey(state.getBlock());
                    result = blockId != null && blockId.toString().equals(id);
                }
                case "HEALTH_RANGE" -> {
                    float health = entity.getHealth();
                    result = health >= min && health <= max;
                }
                case "POTION_RANGE" -> {
                    ResourceLocation effectId = ResourceLocation.tryParse(id);
                    MobEffect effect = effectId == null ? null : ForgeRegistries.MOB_EFFECTS.getValue(effectId);
                    if (effect != null && entity.hasEffect(effect)) {
                        MobEffectInstance instance = entity.getEffect(effect);
                        int levelValue = instance == null ? 0 : instance.getAmplifier();
                        int minLevel = (int) parseDouble(condition.params.get("min_lvl"), 0);
                        int maxLevel = (int) parseDouble(condition.params.get("max_lvl"), 255);
                        result = levelValue >= minLevel && levelValue <= maxLevel;
                    }
                }
                case "ATTR_RANGE" -> {
                    ResourceLocation attributeId = ResourceLocation.tryParse(id);
                    Attribute attribute = attributeId == null ? null : ForgeRegistries.ATTRIBUTES.getValue(attributeId);
                    if (attribute != null && entity.getAttributes().hasAttribute(attribute)) {
                        double value = entity.getAttributeValue(attribute);
                        result = value >= min && value <= max;
                    }
                }
                case "RAINING" -> result = level.isRaining();
                case "THUNDERING" -> result = level.isThundering();
                case "CLEAR_WEATHER" -> result = !level.isRaining() && !level.isThundering();
                case "DAYTIME" -> result = time < 13000L;
                case "NIGHTTIME" -> result = time >= 13000L;
                case "DIMENSION" -> result = level.dimension().location().toString().equals(id);
                case "STAGE" -> {
                    if (entity instanceof Player player) {
                        String stagesText = condition.params.getOrDefault("stage", "");
                        if (!stagesText.isEmpty()) {
                            boolean hasAll = true;
                            for (String rawStage : stagesText.split(",")) {
                                String stage = rawStage.trim();
                                if (stage.isEmpty()) continue;
                                if (!checkGameStage(player, stage)) {
                                    hasAll = false;
                                    break;
                                }
                            }
                            result = hasAll;
                        }
                    }
                }
                case "MOON_PHASE" -> {
                    int targetPhase = (int) parseDouble(condition.params.get("phase"), 0);
                    result = level.getMoonPhase() == targetPhase;
                }
                case "TIME_RANGE" -> result = time >= min && time <= max;
                case "FOOD_RANGE" -> {
                    if (entity instanceof Player player) {
                        int food = player.getFoodData().getFoodLevel();
                        result = food >= min && food <= max;
                    }
                }
                case "EXP_RANGE" -> {
                    if (entity instanceof Player player) {
                        int experienceLevel = player.experienceLevel;
                        result = experienceLevel >= min && experienceLevel <= max;
                    }
                }
            }
        } catch (RuntimeException exception) {
            KineticArmory.LOGGER.error("谓词测试: 异常 类型={} 参数={}", type, condition.params, exception);
        }

        return condition.invert != result;
    }

    private static double parseDouble(String value, double defaultValue) {
        if (value == null || value.isEmpty()) return defaultValue;

        try {
            return Double.parseDouble(value);
        } catch (NumberFormatException ignored) {
            return defaultValue;
        }
    }
}