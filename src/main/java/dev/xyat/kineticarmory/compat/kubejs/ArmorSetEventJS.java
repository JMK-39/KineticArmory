package dev.xyat.kineticarmory.compat.kubejs;

import dev.latvian.mods.kubejs.entity.EntityEventJS;
import dev.xyat.kineticarmory.armorsets.event.ArmorEvents;
import net.minecraft.world.entity.Entity;

public class ArmorSetEventJS extends EntityEventJS {
    private final ArmorEvents.StatusChange event;

    public ArmorSetEventJS(ArmorEvents.StatusChange event) {
        this.event = event;
    }

    @Override
    public Entity getEntity() {
        return event.getEntity();
    }

    public String getSetId() {
        return event.getSetId();
    }

    public boolean isActivated() {
        return event.isActivated();
    }

    public double getX() { return event.getEntity().getX(); }
    public double getY() { return event.getEntity().getY(); }
    public double getZ() { return event.getEntity().getZ(); }

    public String getDimension() {
        return event.getEntity().level().dimension().location().toString();
    }
}