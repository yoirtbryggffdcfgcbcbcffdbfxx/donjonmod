package com.dungeonmod.util;

import net.minecraft.server.network.ServerPlayerEntity;
import java.util.*;

public class CraftingHelper {

    private static final Map<UUID, Long> JUST_CRAFTED = new HashMap<>();

    public static boolean wasJustCrafted(ServerPlayerEntity player) {
        Long t = JUST_CRAFTED.get(player.getUuid());
        if (t == null) return false;
        if (System.currentTimeMillis() - t > 1000) {
            JUST_CRAFTED.remove(player.getUuid());
            return false;
        }
        return true;
    }

    public static void markCrafted(ServerPlayerEntity player) {
        JUST_CRAFTED.put(player.getUuid(), System.currentTimeMillis());
    }
}
