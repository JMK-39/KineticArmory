package dev.xyat.kineticarmory.armorsets.compat;

import dev.xyat.kineticarmory.armorsets.event.ArmorManager;
import dev.xyat.kineticcore.api.event.KineticExternalEvents;
import top.theillusivec4.curios.api.event.CurioChangeEvent;

/**
 * Curios 饰品变化 → 重新计算套装。KineticCore 不再内置 Curios 联动，本模组已依赖 Curios，直接订阅其事件。
 * Curios slot changes → recompute armor sets. KineticCore no longer ships a Curios bridge; this mod already depends
 * on Curios, so it subscribes to Curios' own event.
 */
public final class ArmorCuriosEvents {
    private static boolean registered;

    private ArmorCuriosEvents() {
    }

    public static synchronized void register() {
        if (registered) return;
        registered = true;
        // Curios 15 reports a different item and a changed item state as separate events; set matching depends on both.
        //? if >=26.1 {
        /*KineticExternalEvents.subscribe(CurioChangeEvent.Item.class, ArmorCuriosEvents::onCurioChange);
        KineticExternalEvents.subscribe(CurioChangeEvent.State.class, ArmorCuriosEvents::onCurioChange);
        *///?} else {
        KineticExternalEvents.subscribe(CurioChangeEvent.class, ArmorCuriosEvents::onCurioChange);
        //?}
    }

    private static void onCurioChange(CurioChangeEvent event) {
        ArmorManager.onCurioChange(event.getEntity());
    }
}
