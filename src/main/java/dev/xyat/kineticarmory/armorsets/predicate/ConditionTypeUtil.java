package dev.xyat.kineticarmory.armorsets.predicate;

import dev.xyat.kineticcore.api.client.search.KineticSearch;
import dev.xyat.kineticcore.api.client.widget.input.KineticAutoComplete.Suggestion;
import dev.xyat.kineticcore.api.registry.KineticRegistries;
import dev.xyat.kineticcore.api.runtime.KineticClientRuntime;
import dev.xyat.kineticcore.api.runtime.KineticPlatform;
import net.minecraft.network.chat.Component;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class ConditionTypeUtil {

    public static final List<String> ALL_TYPES = Arrays.asList(
            "SNEAKING", "IN_WATER", "RAINING", "THUNDERING", "CLEAR_WEATHER", "DAYTIME", "NIGHTTIME", "MOON_PHASE", "TIME_RANGE",
            "IN_AIR", "DIMENSION", "HEALTH_RANGE", "FOOD_RANGE", "EXP_RANGE",
            "MOUSE_LEFT_HOLD", "MOUSE_RIGHT_HOLD", "MOUSE_LEFT_CLICK", "MOUSE_RIGHT_CLICK",
            "CLIMBING", "FALLING", "RISING", "ON_BLOCK", "SPEED_RANGE",
            "ON_FIRE", "POTION_RANGE", "ATTR_RANGE", "STAGE"
    );

    public static final List<String> NO_PARAM_TYPES = Arrays.asList(
            "SNEAKING", "IN_WATER", "RAINING", "THUNDERING", "CLEAR_WEATHER", "DAYTIME", "NIGHTTIME",
            "IN_AIR", "MOUSE_LEFT_CLICK", "MOUSE_RIGHT_CLICK", "CLIMBING", "FALLING", "RISING", "ON_FIRE"
    );

    public enum ParamDataType { NUMBER, STRING, DIMENSION, ATTRIBUTE, POTION, BLOCK, ITEM }

    public record ParamDef(String key, ParamDataType type, String defaultVal) {}

    public static List<ParamDef> getParamSchema(String rawType) {
        if (rawType == null || NO_PARAM_TYPES.contains(rawType.toUpperCase())) return Collections.emptyList();
        return switch (rawType.toUpperCase()) {
            case "ON_BLOCK" -> List.of(new ParamDef("id", ParamDataType.ITEM, ""));
            case "POTION_RANGE" -> List.of(new ParamDef("id", ParamDataType.POTION, ""), new ParamDef("min_lvl", ParamDataType.NUMBER, ""), new ParamDef("max_lvl", ParamDataType.NUMBER, ""));
            case "ATTR_RANGE" -> List.of(new ParamDef("id", ParamDataType.ATTRIBUTE, ""), new ParamDef("min", ParamDataType.NUMBER, ""), new ParamDef("max", ParamDataType.NUMBER, ""));
            case "DIMENSION" -> List.of(new ParamDef("id", ParamDataType.DIMENSION, ""));
            case "STAGE" -> List.of(new ParamDef("stage", ParamDataType.STRING, ""));
            case "HEALTH_RANGE", "FOOD_RANGE", "EXP_RANGE", "SPEED_RANGE", "TIME_RANGE" -> List.of(new ParamDef("min", ParamDataType.NUMBER, ""), new ParamDef("max", ParamDataType.NUMBER, ""));
            case "MOON_PHASE" -> List.of(new ParamDef("phase", ParamDataType.NUMBER, ""));
            case "MOUSE_LEFT_HOLD", "MOUSE_RIGHT_HOLD" -> List.of(new ParamDef("ticks", ParamDataType.NUMBER, "1"));
            default -> Collections.emptyList();
        };
    }

    public static List<Suggestion> getSuggestionsFor(ParamDataType type) {
        if (KineticPlatform.isDedicatedServer()) return Collections.emptyList();

        return switch (type) {
            case BLOCK -> KineticRegistries.blocks().ids().stream()
                    .map(id -> new Suggestion(id.toString(), Component.empty()))
                    .toList();
            case DIMENSION -> {
                var levels = KineticClientRuntime.knownLevels();
                if (!levels.isEmpty()) {
                    yield levels.stream()
                            .map(key -> new Suggestion(key.location().toString(), Component.empty()))
                            .toList();
                }
                yield List.of(
                        new Suggestion("minecraft:overworld", Component.empty()),
                        new Suggestion("minecraft:the_nether", Component.empty()),
                        new Suggestion("minecraft:the_end", Component.empty())
                );
            }
            case ATTRIBUTE -> KineticSearch.attributeDictionary();
            case POTION -> KineticSearch.potionDictionary();
            default -> Collections.emptyList();
        };
    }

    public static List<Suggestion> getSuggestions() {
        return ALL_TYPES.stream().map(type -> {
            String translated = Component.translatable(
                    "gui.kineticarmory.predicate.type." + type.toLowerCase()
            ).getString();
            Component translation = translated.equalsIgnoreCase(type) || translated.startsWith("gui.")
                    ? Component.empty()
                    : Component.literal(translated);
            return new Suggestion(type, translation);
        }).toList();
    }

    public static String getRawType(String input) {
        if (input == null) return "";
        String val = input.trim();
        if (val.contains(" - ")) val = val.substring(0, val.indexOf(" - ")).trim();
        return val.toUpperCase();
    }

    public static String extractValue(String input) {
        if (input == null) return "";
        String val = input.trim();
        if (val.contains(" - ")) val = val.substring(0, val.indexOf(" - ")).trim();
        return val;
    }

    public static String getTranslatedName(String rawType) {
        return Component.translatable("gui.kineticarmory.predicate.type." + rawType.toLowerCase()).getString();
    }

    public static String getTranslatedParamName(String paramKey) {
        return Component.translatable("gui.kineticarmory.predicate.param.name." + paramKey.toLowerCase()).getString();
    }

    public static String getTranslatedParamHint(String paramKey) {
        return Component.translatable("gui.kineticarmory.predicate.param.desc." + paramKey.toLowerCase()).getString();
    }
}
