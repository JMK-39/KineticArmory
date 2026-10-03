package dev.xyat.kineticarmory.armorsets.data;

import net.minecraft.world.item.ItemStack;

/** Uses the item data syntax native to each Minecraft version. */
public final class ArmorItemData {
    private ArmorItemData() {}

    public static String emptyData() {
        //? if >=1.21 {
        /*return "[]";
        *///?} else {
        return "{}";
        //?}
    }

    public static String format(ItemStack stack) {
        //? if >=1.21 {
        /*if (stack.isEmpty()) return emptyData();
        String text = dev.xyat.kineticcore.api.inventory.KineticItemText.format(stack);
        int start = text.indexOf('[');
        return start < 0 ? emptyData() : text.substring(start);
        *///?} else {
        return stack.hasTag() && stack.getTag() != null ? stack.getTag().toString() : emptyData();
        //?}
    }

    //? if >=1.21 {
    /*public record Rule(net.minecraft.core.component.DataComponentPatch patch, ItemStack display) {
        public boolean matches(ItemStack actual, boolean strong) {
            if (strong) return display.getComponentsPatch().equals(actual.getComponentsPatch());
            for (var entry : patch.entrySet()) {
                Object value = actual.get(entry.getKey());
                if (entry.getValue().isEmpty()) {
                    if (value != null) return false;
                } else if (entry.getValue().get() instanceof net.minecraft.world.item.component.CustomData expected) {
                    if (!(value instanceof net.minecraft.world.item.component.CustomData found)
                            || !net.minecraft.nbt.NbtUtils.compareNbt(expected.copyTag(), found.copyTag(), true)) return false;
                } else if (!entry.getValue().get().equals(value)) return false;
            }
            return true;
        }
    }

    public static Rule compile(String id, String data) {
        String components = data == null || data.isBlank() ? "[]" : data.trim();
        if (!components.startsWith("[")) throw new IllegalArgumentException("Expected [components]");
        var reader = new com.mojang.brigadier.StringReader(id + components);
        try {
            var parsed = new net.minecraft.commands.arguments.item.ItemParser(registries()).parse(reader);
            if (reader.canRead()) throw new IllegalArgumentException("Trailing item constraint text");
            return new Rule(parsed.components(), new ItemStack(parsed.item(), 1, parsed.components()));
        } catch (com.mojang.brigadier.exceptions.CommandSyntaxException invalid) {
            throw new IllegalArgumentException(invalid.getMessage(), invalid);
        }
    }

    public static Object registryContext() {
        var server = dev.xyat.kineticcore.api.runtime.KineticServerRuntime.currentServer();
        if (server != null) return server.registryAccess();
        var lookup = dev.xyat.kineticcore.api.runtime.KineticPlatform.callOnClient(() -> () -> {
            var level = dev.xyat.kineticcore.api.runtime.KineticClientRuntime.currentLevel();
            return level == null ? null : level.registryAccess();
        }, null);
        return lookup != null ? lookup : net.minecraft.core.registries.BuiltInRegistries.REGISTRY;
    }

    private static net.minecraft.core.HolderLookup.Provider registries() {
        Object context = registryContext();
        return context instanceof net.minecraft.core.HolderLookup.Provider lookup ? lookup
                : net.minecraft.core.RegistryAccess.fromRegistryOfRegistries(net.minecraft.core.registries.BuiltInRegistries.REGISTRY);
    }
    *///?}
}