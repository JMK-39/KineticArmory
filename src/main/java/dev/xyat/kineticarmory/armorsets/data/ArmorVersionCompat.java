package dev.xyat.kineticarmory.armorsets.data;

import dev.xyat.kineticcore.api.registry.KineticRegistries;
import dev.xyat.kineticcore.api.resource.KineticResourceIds;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.item.ItemStack;
import java.util.UUID;

/** Game API differences; configuration UUIDs retain their original meaning. */
public final class ArmorVersionCompat {
    private ArmorVersionCompat() {}

    /** The attribute a config names, or null. */
    public static Attribute attribute(net.minecraft.resources.ResourceLocation id) {
        if (id == null) return null;
        Attribute attribute = KineticRegistries.attributes().get(id);
        // 26.1 dropped the "generic." style prefixes from attribute ids; configs written for older versions keep matching.
        int prefix = id.getPath().indexOf('.');
        if (attribute == null && prefix > 0) {
            attribute = KineticRegistries.attributes().get(KineticResourceIds.of(id.getNamespace(), id.getPath().substring(prefix + 1)));
        }
        return attribute;
    }

    //? if >=1.21 {
    /*public static net.minecraft.core.Holder<MobEffect> effect(MobEffect value) {
        return KineticRegistries.mobEffects().holder(value);
    }
    public static net.minecraft.core.Holder<Attribute> attribute(Attribute value) {
        return KineticRegistries.attributes().holder(value);
    }
    public static net.minecraft.resources.ResourceLocation modifierId(UUID uuid) {
        return KineticResourceIds.of("kineticarmory", uuid.toString());
    }
    public static AttributeModifier modifier(UUID uuid, String name, double amount, AttributeModifier.Operation operation) {
        return new AttributeModifier(modifierId(uuid), amount, operation);
    }
    public static double amount(AttributeModifier modifier) { return modifier.amount(); }
    public static AttributeModifier.Operation operation(AttributeModifier modifier) { return modifier.operation(); }
    public static int dataHash(ItemStack stack) { return stack.getComponentsPatch().hashCode(); }
    *///?} else {
    public static MobEffect effect(MobEffect value) { return value; }
    public static Attribute attribute(Attribute value) { return value; }
    public static UUID modifierId(UUID uuid) { return uuid; }
    public static AttributeModifier modifier(UUID uuid, String name, double amount, AttributeModifier.Operation operation) {
        return new AttributeModifier(uuid, name, amount, operation);
    }
    public static double amount(AttributeModifier modifier) { return modifier.getAmount(); }
    public static AttributeModifier.Operation operation(AttributeModifier modifier) { return modifier.getOperation(); }
    public static int dataHash(ItemStack stack) { return stack.hasTag() && stack.getTag() != null ? stack.getTag().hashCode() : 0; }
    //?}
}
