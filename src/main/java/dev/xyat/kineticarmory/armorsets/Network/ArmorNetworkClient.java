package dev.xyat.kineticarmory.armorsets.Network;

import dev.xyat.kineticarmory.armorsets.client.ArmorCache;
import dev.xyat.kineticarmory.armorsets.client.ArmorClientSnapshot;
import dev.xyat.kineticarmory.armorsets.client.ArmorTooltip;
import dev.xyat.kineticarmory.armorsets.client.gui.ArmorEntityFilterScreen;
import dev.xyat.kineticarmory.armorsets.client.gui.ArmorListScreen;
import dev.xyat.kineticarmory.armorsets.config.ArmorConfigGui;
import dev.xyat.kineticarmory.armorsets.logic.ClientDynamicTracker;
import dev.xyat.kineticcore.api.client.event.KineticClientEvents;
import dev.xyat.kineticcore.api.client.overlay.KineticOverlays;
import dev.xyat.kineticcore.api.client.tooltip.KineticItemTooltips;
import dev.xyat.kineticcore.api.config.client.KTConfigApi;
import dev.xyat.kineticcore.api.runtime.KineticClientRuntime;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public final class ArmorNetworkClient {
    private static Screen pendingEditorParent;

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
        Screen current = KineticClientRuntime.currentScreen();
        if (current instanceof ArmorListScreen listScreen) {
            listScreen.refreshFromSnapshot();
        }
        if (packet.openGui()) {
            Screen parent = pendingEditorParent != null ? pendingEditorParent : current;
            pendingEditorParent = null;
            KineticClientRuntime.openScreen(new ArmorListScreen(parent));
        }
    }

    public static void handleEditorSaveResult(boolean success) {
        if (success) {
            KTConfigApi.notifySaved(ArmorConfigGui.EDITOR_PAGE_ID);
        } else {
            KineticOverlays.toast(Component.translatable("gui.kineticcore.config.save_failed"));
        }
    }

    public static void handleSyncEntityFilter(ArmorNetwork.SyncEntityFilterPacket packet) {
        ArmorClientSnapshot.replaceEntityFilter(packet.mode(), packet.rules());
        if (!packet.openGui()) return;
        Screen current = KineticClientRuntime.currentScreen();
        if (current instanceof ArmorListScreen parent) {
            KineticClientRuntime.openScreen(new ArmorEntityFilterScreen(parent));
        }
    }

    public static void requestOpenEditor() {
        if (!KineticClientRuntime.connected()) return;
        pendingEditorParent = KineticClientRuntime.currentScreen();
        ArmorNetwork.requestOpenEditor();
    }

    private static void onClientLogin() {
        pendingEditorParent = null;
        ArmorClientSnapshot.clear();
    }

    private static void onClientLogout() {
        pendingEditorParent = null;
        ArmorClientSnapshot.clear();
    }
}
