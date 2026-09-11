package dev.xyat.kineticarmory.armorsets.Network;

import dev.xyat.kineticarmory.armorsets.client.ArmorCache;
import dev.xyat.kineticarmory.armorsets.client.ArmorClientSnapshot;
import dev.xyat.kineticarmory.armorsets.client.ArmorTooltip;
import dev.xyat.kineticcore.api.client.overlay.GuiOverlay;
import dev.xyat.kineticcore.config.client.KTConfigApi;
import dev.xyat.kineticarmory.armorsets.config.ArmorConfigGui;
import dev.xyat.kineticarmory.armorsets.logic.ClientDynamicTracker;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.IEventBus;

import dev.xyat.kineticarmory.armorsets.client.gui.ArmorEntityFilterScreen;
import dev.xyat.kineticarmory.armorsets.client.gui.ArmorListScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;

@OnlyIn(Dist.CLIENT)
public class ArmorNetworkClient {
    private static Screen pendingEditorParent;

    public static void registerClient(IEventBus modEventBus) {
        MinecraftForge.EVENT_BUS.addListener(ArmorTooltip::onTooltip);
        MinecraftForge.EVENT_BUS.addListener(ArmorTooltip::onGatherTooltipComponents);
        MinecraftForge.EVENT_BUS.addListener(ClientDynamicTracker::onClientTick);
        MinecraftForge.EVENT_BUS.addListener(ArmorNetworkClient::onClientLogin);
        MinecraftForge.EVENT_BUS.addListener(ArmorNetworkClient::onClientLogout);
        modEventBus.addListener(ArmorTooltip::registerTooltipComponents);
    }

    public static void handleSyncActiveSets(ArmorNetwork.SyncActiveSetsPacket packet) {
        ArmorCache.update(packet.activeSets(), packet.pieceCounts());
    }

    public static void handleSyncConfigs(ArmorNetwork.SyncArmorConfigsPacket packet) {
        ArmorClientSnapshot.replaceConfigs(packet.configs());
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.screen instanceof ArmorListScreen listScreen) {
            listScreen.refreshFromSnapshot();
        }
        if (packet.openGui()) {
            Screen parent = pendingEditorParent != null ? pendingEditorParent : minecraft.screen;
            pendingEditorParent = null;
            minecraft.setScreen(new ArmorListScreen(parent));
        }
    }

    public static void handleEditorSaveResult(boolean success) {
        if (success) {
            KTConfigApi.notifySaved(ArmorConfigGui.EDITOR_PAGE_ID);
        } else {
            GuiOverlay.toast(Component.translatable("gui.kineticcore.config.save_failed"));
        }
    }

    public static void handleSyncEntityFilter(ArmorNetwork.SyncEntityFilterPacket packet) {
        ArmorClientSnapshot.replaceEntityFilter(packet.mode(), packet.rules());
        if (!packet.openGui()) return;
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.screen instanceof ArmorListScreen parent) {
            minecraft.setScreen(new ArmorEntityFilterScreen(parent));
        }
    }

    public static void requestOpenEditor() {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.getConnection() == null) return;
        pendingEditorParent = minecraft.screen;
        ArmorNetwork.CHANNEL.sendToServer(new ArmorNetwork.RequestOpenEditorPacket());
    }

    private static void onClientLogin(ClientPlayerNetworkEvent.LoggingIn event) {
        pendingEditorParent = null;
        ArmorClientSnapshot.clear();
    }

    private static void onClientLogout(ClientPlayerNetworkEvent.LoggingOut event) {
        pendingEditorParent = null;
        ArmorClientSnapshot.clear();
    }
}
