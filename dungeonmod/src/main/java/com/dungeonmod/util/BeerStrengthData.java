package com.dungeonmod.util;

import net.minecraft.component.DataComponentTypes;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import java.util.*;

public class BeerStrengthData {

    private static final Map<UUID, List<BeerBoost>> DATA = new HashMap<>();

    /**
     * Bonus d'attaque des armures custom, indexés par (item vanilla + fragment de nom).
     * On ne peut PAS se baser uniquement sur l'Item vanilla : plusieurs pièces custom
     * partagent le même item de base (ex. LEATHER_BOOTS). Le nom custom départage.
     */
    private static final List<ArmorAttackBonus> ARMOR_ATTACK_BONUSES = new ArrayList<>();

    public static void registerArmorAttackBonus(Item item, float bonus) {
        // Compat ancienne API (casques) : match sur l'item seul (nameHint vide).
        // Remplace un éventuel enregistrement précédent pour le même item sans hint.
        ARMOR_ATTACK_BONUSES.removeIf(b -> b.item == item && b.nameHint.isEmpty());
        ARMOR_ATTACK_BONUSES.add(new ArmorAttackBonus(item, "", bonus));
    }

    /** Enregistre un bonus pour une pièce custom précise (item + sous-chaîne du nom). */
    public static void registerArmorAttackBonus(Item item, String nameHint, float bonus) {
        String hint = nameHint == null ? "" : nameHint;
        ARMOR_ATTACK_BONUSES.removeIf(b -> b.item == item && b.nameHint.equals(hint));
        ARMOR_ATTACK_BONUSES.add(new ArmorAttackBonus(item, hint, bonus));
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
        // Bonus d'attaque des armures custom équipées
        for (var slot : net.minecraft.entity.EquipmentSlot.values()) {
            var stack = player.getEquippedStack(slot);
            if (stack.isEmpty()) continue;
            Float bonus = matchBonus(stack);
            if (bonus != null) total += bonus;
        }
        return total;
    }

    private static Float matchBonus(ItemStack stack) {
        Item item = stack.getItem();
        String name = "";
        if (stack.contains(DataComponentTypes.CUSTOM_NAME)) {
            var cn = stack.get(DataComponentTypes.CUSTOM_NAME);
            if (cn != null) name = cn.getString();
        }
        // Préférer le match le plus spécifique (avec nameHint non vide)
        Float generic = null;
        for (ArmorAttackBonus b : ARMOR_ATTACK_BONUSES) {
            if (b.item != item) continue;
            if (!b.nameHint.isEmpty()) {
                if (name.contains(b.nameHint)) return b.bonus;
            } else {
                generic = b.bonus;
            }
        }
        return generic;
    }

    private record BeerBoost(String typeId, float multiplier, long endTime) {}
    private record ArmorAttackBonus(Item item, String nameHint, float bonus) {}
}
