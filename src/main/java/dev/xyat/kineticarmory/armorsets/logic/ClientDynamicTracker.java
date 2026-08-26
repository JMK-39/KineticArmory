package dev.xyat.kineticarmory.armorsets.logic;

import dev.xyat.kineticarmory.armorsets.Network.ArmorNetwork;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraftforge.event.TickEvent;

public class ClientDynamicTracker {

    private static int leftMouseTicks = 0;
    private static int rightMouseTicks = 0;
    private static boolean leftMouseClick = false;
    private static boolean rightMouseClick = false;

    private static int lastSentLeftTicks = -1;
    private static int lastSentRightTicks = -1;
    private static boolean lastSentLeftClick = false;
    private static boolean lastSentRightClick = false;
    private static int syncCooldown = 0;


    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        if (player == null || mc.level == null) return;

        boolean leftDown = mc.options.keyAttack.isDown();
        boolean rightDown = mc.options.keyUse.isDown();
        leftMouseClick = false;
        rightMouseClick = false;

        if (leftDown) {
            leftMouseTicks++;
        } else {
            if (leftMouseTicks > 0 && leftMouseTicks <= 4) leftMouseClick = true;
            leftMouseTicks = 0;
        }

        if (rightDown) {
            rightMouseTicks++;
        } else {
            if (rightMouseTicks > 0 && rightMouseTicks <= 4) rightMouseClick = true;
            rightMouseTicks = 0;
        }

        syncToServer();
    }

    private static void syncToServer() {
        if (ArmorNetwork.CHANNEL == null) return;
        if (syncCooldown > 0) syncCooldown--;

        boolean changed = leftMouseTicks != lastSentLeftTicks
                || rightMouseTicks != lastSentRightTicks
                || leftMouseClick != lastSentLeftClick
                || rightMouseClick != lastSentRightClick;

        boolean holding = leftMouseTicks > 0 || rightMouseTicks > 0;
        if (!changed && (!holding || syncCooldown > 0)) return;

        if (holding && syncCooldown > 0 && !leftMouseClick && !rightMouseClick) return;
        syncCooldown = holding ? 4 : 0;

        lastSentLeftTicks = leftMouseTicks;
        lastSentRightTicks = rightMouseTicks;
        lastSentLeftClick = leftMouseClick;
        lastSentRightClick = rightMouseClick;

        ArmorNetwork.CHANNEL.sendToServer(new ArmorNetwork.ClientInputStatePacket(leftMouseTicks, rightMouseTicks, leftMouseClick, rightMouseClick));
    }
}
