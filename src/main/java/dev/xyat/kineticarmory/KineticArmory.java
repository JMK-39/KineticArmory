package dev.xyat.kineticarmory;

import com.mojang.logging.LogUtils;
import dev.xyat.kineticarmory.armorsets.Network.ArmorNetwork;
import dev.xyat.kineticarmory.armorsets.ArmorCommand;
import dev.xyat.kineticarmory.armorsets.command.ArmorCommandExtension;
import dev.xyat.kineticarmory.armorsets.config.ArmorConfig;
import dev.xyat.kineticarmory.armorsets.config.ArmorConfigGui;
import dev.xyat.kineticarmory.armorsets.config.ArmorClientConfig;
import dev.xyat.kineticarmory.armorsets.json.ArmorLoader;
import dev.xyat.kineticcore.config.server.KTServerConfigApi;
import dev.xyat.kineticcore.config.server.KTServerConfigSpec;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.loading.FMLEnvironment;
import org.slf4j.Logger;

@Mod(KineticArmory.MODID)
public final class KineticArmory {
    public static final String MODID = "kineticarmory";
    public static final Logger LOGGER = LogUtils.getLogger();

    public KineticArmory(FMLJavaModLoadingContext context) {
        IEventBus modEventBus = context.getModEventBus();

        ArmorNetwork.register(modEventBus);
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
        if (FMLEnvironment.dist == Dist.DEDICATED_SERVER) {
            ArmorConfig.removeLegacyClientSetting();
        }
        ArmorLoader.load();
        ArmorCommandExtension.install();

        DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> {
            ArmorClientConfig.register(context, ArmorConfig.defaultTipKey);
            ArmorConfig.removeLegacyClientSetting();
            ArmorConfigGui.load();
        });
    }
}
