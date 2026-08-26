package dev.xyat.kineticarmory.armorsets.config;

import dev.xyat.kineticarmory.armorsets.Network.ArmorNetworkClient;
import dev.xyat.kineticcore.config.client.KTConfigApi;
import dev.xyat.kineticcore.config.client.KTClientConfigAdapter;
import dev.xyat.kineticcore.config.client.KTConfigPage;
import dev.xyat.kineticcore.config.client.KTConfigScope;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public final class ArmorConfigGui {
    public static final String PAGE_ID = "kineticarmory:armorsets";
    public static final String CLIENT_PAGE_ID = "kineticarmory:client";
    public static final String EDITOR_PAGE_ID = "kineticarmory:editor";

    private ArmorConfigGui() {
    }

    public static void load() {
        KTConfigApi.register(KTConfigPage.builder(
                        PAGE_ID,
                        Component.translatable("cfg.kineticarmory.armorsets.title")
                )
                .pageDescription(Component.translatable("cfg.kineticarmory.armorsets.description"))
                .scope(KTConfigScope.SERVER_AUTHORITATIVE)
                .serverManaged()
                .applyTiming(KTConfigPage.ApplyTiming.MIXED)
                .applyNotice(Component.translatable("cfg.kineticarmory.armorsets.apply_notice"))
                .section(Component.translatable("cfg.kineticarmory.armorsets.general.section"))
                .description(Component.translatable("cfg.kineticarmory.armorsets.general.description"))
                .booleanValue(
                        "enable_sets",
                        Component.translatable("cfg.kineticarmory.armorsets.enable"),
                        () -> ArmorConfig.enableSets,
                        value -> ArmorConfig.enableSets = value,
                        true,
                        Component.translatable("cfg.kineticarmory.armorsets.enable.tooltip")
                )
                .tickSecondsValue(
                        "potion_refresh_interval",
                        Component.translatable("cfg.kineticarmory.armorsets.interval"),
                        () -> ArmorConfig.potionRefreshInterval,
                        value -> ArmorConfig.potionRefreshInterval = value,
                        20,
                        1,
                        200,
                        Component.translatable("cfg.kineticarmory.armorsets.interval.tooltip")
                )
                .booleanValue(
                        "sync_on_reload",
                        Component.translatable("cfg.kineticarmory.armorsets.sync"),
                        () -> ArmorConfig.syncOnReload,
                        value -> ArmorConfig.syncOnReload = value,
                        true,
                        Component.translatable("cfg.kineticarmory.armorsets.sync.tooltip")
                )
                .build());

        KTConfigApi.register(KTClientConfigAdapter.pageBuilder(
                        CLIENT_PAGE_ID,
                        Component.translatable("cfg.kineticarmory.armorsets.client.title"),
                        ArmorClientConfig.spec()
                )
                .pageDescription(Component.translatable("cfg.kineticarmory.armorsets.client.description"))
                .build());

        KTConfigApi.register(KTConfigPage.builder(
                        EDITOR_PAGE_ID,
                        Component.translatable("cfg.kineticarmory.armorsets.editor.title")
                )
                .pageDescription(Component.translatable("cfg.kineticarmory.armorsets.editor.description"))
                .scope(KTConfigScope.SERVER_AUTHORITATIVE)
                .serverManaged()
                .applyTiming(KTConfigPage.ApplyTiming.IMMEDIATE)
                .action(
                        "open_editor",
                        Component.translatable("cfg.kineticarmory.armorsets.editor.action"),
                        ArmorNetworkClient::requestOpenEditor,
                        Component.translatable("cfg.kineticarmory.armorsets.editor.action.tooltip")
                )
                .build());
    }


    public static Screen create(Screen parent) {
        return KTConfigApi.createScreen(parent, PAGE_ID);
    }
}
