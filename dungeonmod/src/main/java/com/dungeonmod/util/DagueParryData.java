package com.dungeonmod.util;

import net.minecraft.entity.player.PlayerEntity;
import java.util.*;

public class DagueParryData {
    private static final Set<UUID> activeParry = new HashSet<>();

    public static boolean isActive(PlayerEntity player) {
        return activeParry.contains(player.getUuid());
    }

    public static void setActive(PlayerEntity player, boolean active) {
        if (active) activeParry.add(player.getUuid());
        else activeParry.remove(player.getUuid());
    }

    public static void clearAll(PlayerEntity player) {
        activeParry.remove(player.getUuid());
    }
}
