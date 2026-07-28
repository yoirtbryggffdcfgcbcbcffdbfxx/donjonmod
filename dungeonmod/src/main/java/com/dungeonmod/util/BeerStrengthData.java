package com.dungeonmod.util;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import java.util.*;

public class BeerStrengthData {

    private static final Map<UUID, List<BeerBoost>> DATA = new HashMap<>();
    private static final Map<net.minecraft.item.Item, Float> ARMOR_ATTACK_BONUS = new HashMap<>();

    public static void registerArmorAttackBonus(net.minecraft.item.Item item, float bonus) {
        ARMOR_ATTACK_BONUS.put(item, bonus);
    }

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
        long now = System.currentTimeMillis();
        float total = 1.0f;
        if (boosts != null) {
            boosts.removeIf(b -> now >= b.endTime);
            if (boosts.isEmpty()) DATA.remove(player.getUuid());
            else for (BeerBoost b : boosts) total += (b.multiplier - 1.0f);
        }
        // Bonus d'attaque des armures (via Map, pas via les attributs vanilla)
        for (var slot : net.minecraft.entity.EquipmentSlot.values()) {
            var stack = player.getEquippedStack(slot);
            if (stack.isEmpty()) continue;
            Float bonus = ARMOR_ATTACK_BONUS.get(stack.getItem());
            if (bonus != null) total += bonus;
        }
        return total;
    }

    private record BeerBoost(String typeId, float multiplier, long endTime) {}
}
