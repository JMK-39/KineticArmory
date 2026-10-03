package dev.xyat.kineticarmory.compat.kubejs;

import dev.latvian.mods.kubejs.event.EventGroup;
import dev.latvian.mods.kubejs.event.EventHandler;
import dev.xyat.kineticarmory.armorsets.event.ArmorEvents;
import dev.xyat.kineticcore.api.event.KineticExternalEvents;

public final class KineticArmoryKubeJSPlugin
//? if >=1.21 {
/*implements dev.latvian.mods.kubejs.plugin.KubeJSPlugin
*///?} else {
extends dev.latvian.mods.kubejs.KubeJSPlugin
//?}
{
    public static final EventGroup GROUP = EventGroup.of("kineticarmoryEvents");
    private static EventHandler armorSetChange;

    @Override
    public void init() {
        KineticExternalEvents.subscribe(ArmorEvents.StatusChange.class, this::onArmorSetStatusChange);
    }

    @Override
    //? if >=1.21 {
    /*public void registerEvents(dev.latvian.mods.kubejs.event.EventGroupRegistry registry) {
    *///?} else {
    public void registerEvents() {
    //?}
        armorSetChange = GROUP.server("armorSetChange", () -> ArmorSetEventJS.class);
        //? if >=1.21 {
        /*registry.register(GROUP);
        *///?} else {
        GROUP.register();
        //?}
    }

    public void onArmorSetStatusChange(ArmorEvents.StatusChange event) {
        if (armorSetChange != null && armorSetChange.hasListeners()) {
            armorSetChange.post(new ArmorSetEventJS(event));
        }
    }
}
