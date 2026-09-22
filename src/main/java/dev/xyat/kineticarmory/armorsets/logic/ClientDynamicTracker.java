package dev.xyat.kineticarmory.armorsets.logic;

import dev.xyat.kineticarmory.armorsets.Network.ArmorNetwork;
import dev.xyat.kineticcore.api.runtime.KineticClientRuntime;

public final class ClientDynamicTracker {

    private static int leftMouseTicks = 0;
    private static int rightMouseTicks = 0;
    private static boolean leftMouseClick = false;
    private static boolean rightMouseClick = false;

    private static int lastSentLeftTicks = -1;
    private static int lastSentRightTicks = -1;
    private static boolean lastSentLeftClick = false;
    private static boolean lastSentRightClick = false;
    private static int syncCooldown = 0;

    private ClientDynamicTracker() {
    }

    public static void onClientTick() {
        if (KineticClientRuntime.localPlayer() == null || KineticClientRuntime.currentLevel() == null) return;

        boolean leftDown = KineticClientRuntime.attackKeyDown();
        boolean rightDown = KineticClientRuntime.useKeyDown();
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
        if (!KineticClientRuntime.connected()) return;
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

        ArmorNetwork.sendClientInputState(leftMouseTicks, rightMouseTicks, leftMouseClick, rightMouseClick);
    }
}
