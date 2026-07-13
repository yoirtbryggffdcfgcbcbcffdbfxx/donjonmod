package com.dungeonmod.util;

import net.minecraft.entity.player.PlayerEntity;
import java.util.*;

public class BeerStrengthData {

    private static final Map<UUID, List<BeerBoost>> DATA = new HashMap<>();

    public static void applyBoost(PlayerEntity player, String typeId, float multiplier, int durationTicks) {
        List<BeerBoost> boosts = DATA.computeIfAbsent(player.getUuid(), k -> new ArrayList<>());
        long now = System.currentTimeMillis();
        boosts.removeIf(b -> now >= b.endTime);
        for (BeerBoost b : boosts) {
            if (b.typeId.equals(typeId)) {
                // Same type: extend time, keep best multiplier
                return;
            }
        }
        boosts.add(new BeerBoost(typeId, multiplier, now + durationTicks * 50L));
    }

    public static float getMultiplier(PlayerEntity player) {
        List<BeerBoost> boosts = DATA.get(player.getUuid());
        if (boosts == null || boosts.isEmpty()) return 1.0f;
        long now = System.currentTimeMillis();
        boosts.removeIf(b -> now >= b.endTime);
        if (boosts.isEmpty()) {
            DATA.remove(player.getUuid());
            return 1.0f;
        }
        float total = 1.0f;
        for (BeerBoost b : boosts) {
            total += (b.multiplier - 1.0f);
        }
        return total;
    }

    private record BeerBoost(String typeId, float multiplier, long endTime) {}
}
