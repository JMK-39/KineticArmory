package dev.xyat.kineticarmory.armorsets.Network;

import dev.xyat.kineticarmory.armorsets.client.ArmorCache;
import dev.xyat.kineticarmory.armorsets.client.ArmorClientSnapshot;
import dev.xyat.kineticarmory.armorsets.client.ArmorTooltip;
import dev.xyat.kineticarmory.armorsets.client.gui.ArmorEditPage;
import dev.xyat.kineticarmory.armorsets.client.gui.ArmorEntityFilterPage;
import dev.xyat.kineticarmory.armorsets.client.gui.ArmorListPage;
import dev.xyat.kineticarmory.armorsets.config.ArmorConfigGui;
import dev.xyat.kineticarmory.armorsets.logic.ClientDynamicTracker;
import dev.xyat.kineticcore.api.client.event.KineticClientEvents;
import dev.xyat.kineticcore.api.client.gui.KineticGui;
import dev.xyat.kineticcore.api.client.gui.overlay.KineticOverlays;
import dev.xyat.kineticcore.api.text.KineticI18n;
import dev.xyat.kineticcore.api.client.tooltip.KineticItemTooltips;
import dev.xyat.kineticcore.api.config.client.KTConfigApi;
import dev.xyat.kineticcore.api.runtime.KineticClientRuntime;
import net.minecraft.network.chat.Component;

public final class ArmorNetworkClient {
    private ArmorNetworkClient() {
    }

    public static void registerClient() {
        ArmorTooltip.registerTooltipComponents();
        KineticItemTooltips.onBuild(ArmorTooltip::onTooltip);
        KineticItemTooltips.onGather(ArmorTooltip::onGatherTooltipComponents);
        KineticClientEvents.onTick(KineticClientEvents.TickPhase.END, ClientDynamicTracker::onClientTick);
        KineticClientEvents.onLogin(ArmorNetworkClient::onClientLogin);
        KineticClientEvents.onLogout(ArmorNetworkClient::onClientLogout);
    }

    public static void handleSyncActiveSets(ArmorNetwork.SyncActiveSetsPacket packet) {
        ArmorCache.update(packet.activeSets(), packet.pieceCounts());
    }

    public static void handleSyncConfigs(ArmorNetwork.SyncArmorConfigsPacket packet) {
        ArmorClientSnapshot.replaceConfigs(packet.configs());
        ArmorListPage listPage = KineticGui.currentPage(ArmorListPage.class);
        if (listPage != null) {
            listPage.refreshFromSnapshot();
        }
        if (packet.openGui()) {
            KineticGui.openChild(new ArmorListPage());
        }
    }

    public static void handleEditorSaveResult(boolean success) {
        ArmorEditPage editPage = KineticGui.currentPage(ArmorEditPage.class);
        if (editPage != null) {
            editPage.handleSaveResult(success);
        }
        if (success) {
            KTConfigApi.notifySaved(ArmorConfigGui.EDITOR_PAGE_ID);
        } else {
            KineticOverlays.toast(KineticI18n.translatable("gui.kineticcore.config.save_failed"));
        }
    }

    public static void handleSyncEntityFilter(ArmorNetwork.SyncEntityFilterPacket packet) {
        ArmorClientSnapshot.replaceEntityFilter(packet.mode(), packet.rules());
        if (!packet.openGui()) return;
        if (KineticGui.currentPage() instanceof ArmorListPage) {
            KineticGui.openChild(new ArmorEntityFilterPage());
        }
    }

    public static void requestOpenEditor() {
        if (!KineticClientRuntime.connected()) return;
        ArmorNetwork.requestOpenEditor();
    }

    private static void onClientLogin() {
        ArmorClientSnapshot.clear();
    }

    private static void onClientLogout() {
        ArmorClientSnapshot.clear();
    }
}
