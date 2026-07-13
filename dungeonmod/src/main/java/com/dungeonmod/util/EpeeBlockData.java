package com.dungeonmod.util;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.Vec3d;
import java.util.*;

public class EpeeBlockData {

    private static final Map<UUID, EpeeBlockData> DATA = new HashMap<>();

    public int blockedHits;
    public long lastBlockTime;
    public long cooldownEnd;

    public static EpeeBlockData get(PlayerEntity player) {
        return DATA.computeIfAbsent(player.getUuid(), k -> new EpeeBlockData());
    }

    public boolean isOnCooldown() {
        return System.currentTimeMillis() < cooldownEnd;
    }

    public void startCooldown() {
        cooldownEnd = System.currentTimeMillis() + 1500;
        blockedHits = 0;
    }

    public boolean checkAndIncrement(PlayerEntity player, Vec3d toAttacker) {
        long now = System.currentTimeMillis();

        // Reset if more than 2s since last block
        if (now - lastBlockTime > 2000) {
            blockedHits = 0;
        }

        lastBlockTime = now;
        blockedHits++;

        // Check if attacker is behind
        Vec3d lookDir = player.getRotationVec(1.0f).normalize();
        if (toAttacker.dotProduct(lookDir) < 0) {
            return true; // break guard
        }

        // Check if 4 hits blocked
        return blockedHits >= 4;
    }
}
