package dev.xyat.kineticarmory;

import com.mojang.logging.LogUtils;
import dev.xyat.kineticarmory.armorsets.Network.ArmorNetwork;
import dev.xyat.kineticarmory.armorsets.Network.ArmorNetworkClient;
import dev.xyat.kineticarmory.armorsets.ArmorCommand;
import dev.xyat.kineticarmory.armorsets.command.ArmorCommandExtension;
import dev.xyat.kineticarmory.armorsets.config.ArmorConfig;
import dev.xyat.kineticarmory.armorsets.config.ArmorConfigGui;
import dev.xyat.kineticarmory.armorsets.config.ArmorClientConfig;
import dev.xyat.kineticarmory.armorsets.json.ArmorLoader;
import dev.xyat.kineticcore.api.config.server.KTServerConfigApi;
import dev.xyat.kineticcore.api.config.server.KTServerConfigSpec;
import net.minecraftforge.fml.common.Mod;
import dev.xyat.kineticcore.api.runtime.KineticPlatform;
import org.slf4j.Logger;

@Mod(KineticArmory.MODID)
public final class KineticArmory {
    public static final String MODID = "kineticarmory";
    public static final Logger LOGGER = LogUtils.getLogger();

    public KineticArmory() {
        ArmorNetwork.register();
        ArmorConfig.load();
        KTServerConfigApi.register(KTServerConfigSpec.builder("kineticarmory:armorsets")
                .booleanValue("enable_sets", () -> ArmorConfig.enableSets, value -> ArmorConfig.enableSets = value)
                .doubleValue(
                        "potion_refresh_interval",
                        () -> ArmorConfig.potionRefreshInterval / 20.0D,
                        value -> ArmorConfig.potionRefreshInterval = (int) Math.round(value * 20.0D),
                        1.0D / 20.0D,
                        10.0D
                )
                .booleanValue("sync_on_reload", () -> ArmorConfig.syncOnReload, value -> ArmorConfig.syncOnReload = value)
                .onSave(ArmorConfig::save)
                .afterSave(server -> ArmorCommand.executeReload(server.createCommandSourceStack()))
                .build());
        KTServerConfigApi.registerActionPage("kineticarmory:editor");
        // A dedicated server has no client config spec to migrate into. Remove the old
        // mixed-side key there, while clients still copy its value before removal below.
        if (KineticPlatform.isDedicatedServer()) {
            ArmorConfig.removeLegacyClientSetting();
        }
        ArmorLoader.load();
        ArmorCommandExtension.install();

        KineticPlatform.runOnClient(() -> () -> {
            ArmorClientConfig.register(ArmorConfig.defaultTipKey);
            ArmorConfig.removeLegacyClientSetting();
            ArmorConfigGui.load();
            ArmorNetworkClient.registerClient();
        });
    }
}
