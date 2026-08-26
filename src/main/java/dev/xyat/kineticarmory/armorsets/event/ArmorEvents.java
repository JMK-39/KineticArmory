package dev.xyat.kineticarmory.armorsets.event;

import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.eventbus.api.Event;

public class ArmorEvents {

    public static class StatusChange extends Event {
        private final LivingEntity entity;
        private final String setId;
        private final boolean activated; // true=激活, false=失效

        public StatusChange(LivingEntity entity, String setId, boolean activated) {
            this.entity = entity;
            this.setId = setId;
            this.activated = activated;
        }

        public LivingEntity getEntity() { return entity; }
        public String getSetId() { return setId; }
        public boolean isActivated() { return activated; }
    }
}