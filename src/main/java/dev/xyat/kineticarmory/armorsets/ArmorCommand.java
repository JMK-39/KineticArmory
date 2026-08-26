package dev.xyat.kineticarmory.armorsets;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import dev.xyat.kineticarmory.armorsets.Network.ArmorNetwork;
import dev.xyat.kineticarmory.armorsets.config.ArmorConfig;
import dev.xyat.kineticarmory.armorsets.event.ArmorManager;
import dev.xyat.kineticarmory.armorsets.json.ArmorLoader;
import net.minecraft.commands.CommandSourceStack;
import net.minecraftforge.network.PacketDistributor;

public class ArmorCommand {

    /**
     * Retained as a source-compatible no-op for integrations compiled against
     * the former command entry. The visual editor now lives in KT's module menu.
     */
    @Deprecated(forRemoval = false)
    public static void register(LiteralArgumentBuilder<CommandSourceStack> root) {
    }

    public static void executeReload(CommandSourceStack source) {
        ArmorConfig.rebuildEntityRuleCache();
        ArmorLoader.load();
        if (ArmorConfig.syncOnReload) {
            ArmorNetwork.CHANNEL.send(PacketDistributor.ALL.noArg(),
                    new ArmorNetwork.SyncEntityFilterPacket(ArmorConfig.entityFilterMode, ArmorConfig.allowedEntities, false));
            ArmorNetwork.CHANNEL.send(PacketDistributor.ALL.noArg(),
                    new ArmorNetwork.SyncArmorConfigsPacket(ArmorLoader.LOADED_SETS, false));
        }
        ArmorManager.forceRecalculateAll(source.getServer());
    }
}
