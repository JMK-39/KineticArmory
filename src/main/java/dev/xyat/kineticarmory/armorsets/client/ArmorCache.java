package dev.xyat.kineticarmory.armorsets.client;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@OnlyIn(Dist.CLIENT)
public class ArmorCache {
    private static final Set<String> ACTIVE_SET_IDS = new HashSet<>();
    private static final Map<String, Integer> ACTIVE_SET_PIECES = new HashMap<>();

    public static void update(List<String> newActiveSets, Map<String, Integer> newPieceCounts) {
        ACTIVE_SET_IDS.clear();
        ACTIVE_SET_PIECES.clear();
        ACTIVE_SET_IDS.addAll(newActiveSets);
        if (newPieceCounts != null) ACTIVE_SET_PIECES.putAll(newPieceCounts);
    }

    public static boolean isSetActive(String setId) {
        return ACTIVE_SET_IDS.contains(setId);
    }

    public static int getSetPieceCount(String setId) {
        return ACTIVE_SET_PIECES.getOrDefault(setId, 0);
    }

    public static void clear() {
        ACTIVE_SET_IDS.clear();
        ACTIVE_SET_PIECES.clear();
    }
}
