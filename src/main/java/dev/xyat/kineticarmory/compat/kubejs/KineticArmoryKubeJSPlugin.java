package dev.xyat.kineticarmory.compat.kubejs;

import dev.latvian.mods.kubejs.event.EventGroup;
import dev.latvian.mods.kubejs.event.EventHandler;
import dev.xyat.kineticarmory.armorsets.event.ArmorEvents;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.SubscribeEvent;

public final class KineticArmoryKubeJSPlugin extends dev.latvian.mods.kubejs.KubeJSPlugin {
    public static final EventGroup GROUP = EventGroup.of("kineticarmoryEvents");
    private static EventHandler armorSetChange;

    @Override
    public void init() {
        MinecraftForge.EVENT_BUS.register(this);
    }

    @Override
    public void registerEvents() {
        armorSetChange = GROUP.server("armorSetChange", () -> ArmorSetEventJS.class);
        GROUP.register();
    }

    @SubscribeEvent
    public void onArmorSetStatusChange(ArmorEvents.StatusChange event) {
        if (armorSetChange != null && armorSetChange.hasListeners()) {
            armorSetChange.post(new ArmorSetEventJS(event));
        }
    }
}
