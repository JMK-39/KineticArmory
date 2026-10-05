//? if >=1.21 {
/*package dev.xyat.kineticarmoryvalidation;

import dev.xyat.kineticarmory.armorsets.data.*;
import dev.xyat.kineticarmory.armorsets.event.ArmorManager;
import dev.xyat.kineticarmory.armorsets.predicate.*;
import dev.xyat.kineticarmory.armorsets.Network.ArmorNetwork;
import dev.xyat.kineticcore.api.inventory.KineticItemText;
import dev.xyat.kineticcore.api.network.*;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.effect.*;
import net.minecraft.world.item.*;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.server.ServerStartedEvent;
import java.util.*;

@Mod("kineticarmory_validation")
public final class RuntimeValidation {
    private final ArmorDataConfig.ItemReq preheated = rule("minecraft:diamond_sword", "WEAK", "[enchantments={levels:{\"minecraft:sharpness\":2}}]");
    public RuntimeValidation() {
        if (Boolean.getBoolean("kineticarmory.guiValidation")) {MinecraftForge.EVENT_BUS.addListener(this::validate);return;}
        // 26.1 binds item components after mods are constructed, so no stack can exist this early there.
*///?}
//? if >=1.21 <26.1
/*        preheated.createDisplayStack();*/
//? if >=1.21 {
/*        MinecraftForge.EVENT_BUS.addListener(this::validate);
    }

    private static void require(boolean value, String message) {
        if (!value) throw new AssertionError(message);
    }
    private static ArmorDataConfig.ItemReq rule(String id, String mode, String data) {
        var req = ArmorDataConfig.ItemReq.create(id);
        req.setDataMode(mode);
        req.setItemData(data);
        return req;
    }
    private static void matching(ItemStack stack, String mode, String data, boolean expected, String name) {
        require(ArmorDataConfig.isItemMatching(stack, rule("minecraft:diamond_sword", mode, data)) == expected, name);
    }
    private void validate(ServerStartedEvent event) {
        if (Boolean.getBoolean("kineticarmory.guiValidation")) {dev.xyat.kineticcore.api.runtime.KineticClientRuntime.execute(GuiLongTextValidation::install);return;}
        try {
            var gson = new com.google.gson.Gson();
            var json = gson.toJson(rule("minecraft:diamond_sword", "WEAK", "[damage=5]"));
            require(json.contains("\"components\"") && json.contains("\"componentMode\"")
                    && !json.contains("\"nbtTag\"") && !json.contains("\"nbtMode\""), "Neo config uses component fields only");
            require("[]".equals(ArmorDataConfig.ItemReq.create("minecraft:diamond_sword").getItemData()), "empty components default");
            var actual = KineticItemText.parse("minecraft:diamond_sword[damage=5,custom_data={sample:1,extra:2}]");
            matching(actual, "NONE", "invalid", true, "NONE ignores malformed constraint");
            matching(actual, "WEAK", "{Damage:5,sample:1}", false, "legacy item NBT rejected");
            matching(actual, "STRONG", "{Damage:5,sample:1,extra:2}", false, "strong rejects legacy item NBT");
            matching(actual, "WEAK", "[damage=5,custom_data={sample:1}]", true, "component subset");
            matching(actual, "WEAK", "[damage=0]", false, "explicit default remains constraint");
            matching(actual, "WEAK", "[!custom_data]", false, "explicit removal remains constraint");
            matching(actual, "STRONG", "[damage=5,custom_data={sample:1}]", false, "strong rejects additional data");
            matching(actual, "STRONG", ArmorItemData.format(actual), true, "editor import strong self-match");
            matching(actual, "WEAK", "[damage=invalid]", false, "malformed component rejected");
            matching(actual, "WEAK", "[damage=5]extra", false, "trailing data rejected");
            var named = KineticItemText.parse("minecraft:diamond_sword[custom_name='\"Example\"']");
            matching(named, "WEAK", "[custom_name='\"Example\"']", true, "component name");
            matching(named, "WEAK", "[custom_name='\"Different\"']", false, "different component name rejected");
            var pristine = new ItemStack(Items.DIAMOND_SWORD);
            matching(pristine, "WEAK", "[damage=0,!custom_data]", true, "default and removal on pristine");
            matching(pristine, "STRONG", "[]", true, "empty strong constraint");
            matching(actual, "STRONG", "[]", false, "empty strong rejects data");
            require(ArmorVersionCompat.dataHash(actual) != ArmorVersionCompat.dataHash(pristine), "component equipment cache invalidation");
            var changing = rule("minecraft:diamond_sword", "WEAK", "[damage=5]");
            require(ArmorDataConfig.isItemMatching(actual, changing), "cache initial");
            changing.setItemData("[damage=6]");
            require(!ArmorDataConfig.isItemMatching(actual, changing), "cache invalidates edited constraint");
            var enchanted = KineticItemText.parse("minecraft:diamond_sword[enchantments={levels:{\"minecraft:sharpness\":2,\"minecraft:unbreaking\":3}}]");
            matching(enchanted, "WEAK", "[enchantments={levels:{\"minecraft:sharpness\":2}}]", false, "enchantment component is matched as a whole");
            matching(enchanted, "STRONG", ArmorItemData.format(enchanted), true, "enchantment component strong self-match");
            require(ArmorDataConfig.isItemMatching(KineticItemText.parse("minecraft:diamond_sword" + preheated.getItemData()), preheated), "startup registry cache refresh");
            // Detached entity: never added to the user's world.
            Zombie entity = new Zombie(EntityType.ZOMBIE, event.getServer().overworld());
            var set = new ArmorDataConfig();
            set.id = "validation";
            var attr = new ArmorDataConfig.AttributeModifierData();
            attr.attribute = "minecraft:generic.attack_damage";
            attr.uuid = "11c892cc-bf62-4d93-9269-f88530860b30";
            attr.operation = "ADD";
            attr.amount = 2;
            var apply = ArmorManager.class.getDeclaredMethod("applyOrRefreshAttribute", LivingEntity.class,
                    ArmorDataConfig.class, ArmorDataConfig.AttributeModifierData.class, int.class, String.class);
            var remove = ArmorManager.class.getDeclaredMethod("removeSingleAttribute", LivingEntity.class, ArmorDataConfig.AttributeModifierData.class);
            apply.setAccessible(true); remove.setAccessible(true);
            var inst = entity.getAttribute(Attributes.ATTACK_DAMAGE);
            double base = inst.getValue();
            apply.invoke(null, entity, set, attr, 4, "test");
            require(inst.getValue() == base + 2, "attribute applied");
            apply.invoke(null, entity, set, attr, 4, "test");
            require(inst.getValue() == base + 2 && inst.getModifiers().size() == 1, "same UUID does not accumulate");
            attr.amount = 3;
            apply.invoke(null, entity, set, attr, 4, "test");
            require(inst.getValue() == base + 3 && inst.getModifiers().size() == 1, "attribute refreshed");
            remove.invoke(null, entity, attr);
            require(inst.getValue() == base && inst.getModifiers().isEmpty(), "same UUID removes modifier");
            attr.operation = "SET"; attr.amount = 10;
            apply.invoke(null, entity, set, attr, 4, "test");
            require(inst.getValue() == 10, "SET preserves base-relative semantics");
            remove.invoke(null, entity, attr);
            entity.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 200, 1));
            var potion = new ConditionData("POTION_RANGE");
            potion.params.put("id", "minecraft:speed"); potion.params.put("min_lvl", "1"); potion.params.put("max_lvl", "1");
            require(ConditionEvaluator.checkConditions(entity, List.of(potion)), "Holder potion predicate");
            var attribute = new ConditionData("ATTR_RANGE");
            attribute.params.put("id", attr.attribute); attribute.params.put("min", Double.toString(base)); attribute.params.put("max", Double.toString(base));
            require(ConditionEvaluator.checkConditions(entity, List.of(attribute)), "Holder attribute predicate");

            set.equipment.put("mainhand", changing);
            set.attributes.add(attr);
            var packet = new ArmorNetwork.SyncArmorConfigsPacket(Map.of(set.id, set), true);
            var encode = packet.getClass().getDeclaredMethod("encode", NetworkBuffer.class); encode.setAccessible(true);
            byte[] bytes = NetworkBuffers.encode(buffer -> {
                try { encode.invoke(packet, buffer); } catch (ReflectiveOperationException failure) { throw new IllegalStateException(failure); }
            });
            var decoded = NetworkBuffers.decode(bytes, ArmorNetwork.SyncArmorConfigsPacket::new);
            var restored = decoded.configs().get(set.id);
            require(decoded.openGui() && restored != null && restored.attributes.get(0).uuid.equals(attr.uuid)
                    && restored.equipment.get("mainhand").getItemData().equals(changing.getItemData()), "compressed network config round trip");
            org.slf4j.LoggerFactory.getLogger(RuntimeValidation.class).info("KINETICARMORY_RUNTIME_VALIDATION_PASS");
        } catch (Throwable failure) {
            org.slf4j.LoggerFactory.getLogger(RuntimeValidation.class).error("KINETICARMORY_RUNTIME_VALIDATION_FAIL", failure);
            // Keep the test client usable so it can save and exit normally.
        }
    }
}
*///?}
