package dev.xyat.kineticarmory.armorsets.data;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EntityType;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public final class ArmorEntityRule {
    private static final ArmorEntityRule EMPTY = new ArmorEntityRule(false, Set.of(), Set.of(), List.of());

    private final boolean matchAll;
    private final Set<ResourceLocation> exactIds;
    private final Set<String> namespaces;
    private final List<TagKey<EntityType<?>>> tags;
    private final ConcurrentHashMap<EntityType<?>, Boolean> resultCache = new ConcurrentHashMap<>();

    private ArmorEntityRule(boolean matchAll, Set<ResourceLocation> exactIds, Set<String> namespaces, List<TagKey<EntityType<?>>> tags) {
        this.matchAll = matchAll;
        this.exactIds = exactIds;
        this.namespaces = namespaces;
        this.tags = tags;
    }

    public static ArmorEntityRule empty() {
        return EMPTY;
    }

    public static ArmorEntityRule compile(List<String> rules) {
        if (rules == null || rules.isEmpty()) return EMPTY;

        boolean all = false;
        Set<ResourceLocation> exact = new HashSet<>();
        Set<String> mods = new HashSet<>();
        List<TagKey<EntityType<?>>> tagRules = new ArrayList<>();

        for (String raw : rules) {
            if (raw == null) continue;
            String rule = raw.trim();
            if (rule.isEmpty()) continue;

            if ("ALL".equalsIgnoreCase(rule)) {
                all = true;
                continue;
            }

            if (rule.charAt(0) == '@') {
                String namespace = rule.substring(1).trim();
                if (!namespace.isEmpty()) mods.add(namespace);
                continue;
            }

            if (rule.charAt(0) == '#') {
                ResourceLocation id = ResourceLocation.tryParse(rule.substring(1).trim());
                if (id != null) tagRules.add(TagKey.create(Registries.ENTITY_TYPE, id));
                continue;
            }

            ResourceLocation id = ResourceLocation.tryParse(rule);
            if (id != null) exact.add(id);
        }

        return new ArmorEntityRule(all, Set.copyOf(exact), Set.copyOf(mods), List.copyOf(tagRules));
    }

    public boolean matches(EntityType<?> type) {
        if (type == null) return false;
        if (matchAll) return true;
        return resultCache.computeIfAbsent(type, this::computeMatch);
    }

    private boolean computeMatch(EntityType<?> type) {
        ResourceLocation id = ForgeRegistries.ENTITY_TYPES.getKey(type);
        if (id == null) return false;
        if (exactIds.contains(id) || namespaces.contains(id.getNamespace())) return true;

        if (!tags.isEmpty()) {
            var manager = ForgeRegistries.ENTITY_TYPES.tags();
            if (manager != null) {
                for (TagKey<EntityType<?>> tag : tags) {
                    if (manager.getTag(tag).contains(type)) return true;
                }
            }
        }

        return false;
    }
}
