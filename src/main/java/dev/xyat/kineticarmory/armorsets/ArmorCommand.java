package dev.xyat.kineticarmory.armorsets;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import dev.xyat.kineticarmory.armorsets.Network.ArmorNetwork;
import dev.xyat.kineticarmory.armorsets.config.ArmorConfig;
import dev.xyat.kineticarmory.armorsets.event.ArmorManager;
import dev.xyat.kineticarmory.armorsets.json.ArmorLoader;
import net.minecraft.commands.CommandSourceStack;

public class ArmorCommand {

    /**
     * Retained as a source-compatible no-op for integrations compiled against
     * the former command entry. The visual editor now lives in KT's module menu.
     */
    @Deprecated(forRemoval = false)
    public static void register(LiteralArgumentBuilder<CommandSourceStack> root) {
    }

    /** The reload command: reloads the sets and, when "sync on reload" is on, refreshes every online player. */
    public static void executeReload(CommandSourceStack source) {
        reloadSets(source.getServer());
        if (ArmorConfig.syncOnReload) {
            ArmorNetwork.broadcastEntityFilter();
            ArmorNetwork.broadcastArmorConfigs();
        }
    }

    /** Reloads the sets after an editor save without sending anything; the caller answers the editing player. */
    public static void reloadSets(net.minecraft.server.MinecraftServer server) {
        ArmorConfig.rebuildEntityRuleCache();
        ArmorLoader.load();
        ArmorManager.forceRecalculateAll(server);
    }
}
