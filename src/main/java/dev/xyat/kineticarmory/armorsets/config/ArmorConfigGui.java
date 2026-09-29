package dev.xyat.kineticarmory.armorsets.config;

import dev.xyat.kineticcore.api.text.KineticI18n;
import dev.xyat.kineticarmory.armorsets.Network.ArmorNetworkClient;
import dev.xyat.kineticcore.api.config.client.KTConfigApi;
import dev.xyat.kineticcore.api.config.client.KTClientConfigAdapter;
import dev.xyat.kineticcore.api.config.client.KTConfigPage;
import dev.xyat.kineticcore.api.config.client.KTConfigScope;

public final class ArmorConfigGui {
    public static final String PAGE_ID = "kineticarmory:armorsets";
    public static final String CLIENT_PAGE_ID = "kineticarmory:client";
    public static final String EDITOR_PAGE_ID = "kineticarmory:editor";

    private ArmorConfigGui() {
    }

    public static void load() {
        KTConfigApi.register(KTConfigPage.builder(
                        PAGE_ID,
                        KineticI18n.translatable("cfg.kineticarmory.armorsets.title")
                )
                .pageDescription(KineticI18n.translatable("cfg.kineticarmory.armorsets.description"))
                .scope(KTConfigScope.SERVER_AUTHORITATIVE)
                .serverManaged()
                .applyTiming(KTConfigPage.ApplyTiming.MIXED)
                .applyNotice(KineticI18n.translatable("cfg.kineticarmory.armorsets.apply_notice"))
                .divider()
                .description(KineticI18n.translatable("cfg.kineticarmory.armorsets.general.description"))
                .booleanValue(
                        "enable_sets",
                        KineticI18n.translatable("cfg.kineticarmory.armorsets.enable"),
                        () -> ArmorConfig.enableSets,
                        value -> ArmorConfig.enableSets = value,
                        true,
                        KineticI18n.translatable("cfg.kineticarmory.armorsets.enable.tooltip")
                )
                .tickSecondsValue(
                        "potion_refresh_interval",
                        KineticI18n.translatable("cfg.kineticarmory.armorsets.interval"),
                        () -> ArmorConfig.potionRefreshInterval,
                        value -> ArmorConfig.potionRefreshInterval = value,
                        20,
                        1,
                        200,
                        KineticI18n.translatable("cfg.kineticarmory.armorsets.interval.tooltip")
                )
                .booleanValue(
                        "sync_on_reload",
                        KineticI18n.translatable("cfg.kineticarmory.armorsets.sync"),
                        () -> ArmorConfig.syncOnReload,
                        value -> ArmorConfig.syncOnReload = value,
                        true,
                        KineticI18n.translatable("cfg.kineticarmory.armorsets.sync.tooltip")
                )
                .build());

        KTConfigApi.register(KTClientConfigAdapter.pageBuilder(
                        CLIENT_PAGE_ID,
                        KineticI18n.translatable("cfg.kineticarmory.armorsets.client.title"),
                        ArmorClientConfig.spec()
                )
                .pageDescription(KineticI18n.translatable("cfg.kineticarmory.armorsets.client.description"))
                .build());

        KTConfigApi.register(KTConfigPage.builder(
                        EDITOR_PAGE_ID,
                        KineticI18n.translatable("cfg.kineticarmory.armorsets.editor.title")
                )
                .pageDescription(KineticI18n.translatable("cfg.kineticarmory.armorsets.editor.description"))
                .scope(KTConfigScope.SERVER_AUTHORITATIVE)
                .serverManaged()
                .applyTiming(KTConfigPage.ApplyTiming.IMMEDIATE)
                .action(
                        "open_editor",
                        KineticI18n.translatable("cfg.kineticarmory.armorsets.editor.action"),
                        ArmorNetworkClient::requestOpenEditor,
                        KineticI18n.translatable("cfg.kineticarmory.armorsets.editor.action.tooltip")
                )
                .build());
    }

}
