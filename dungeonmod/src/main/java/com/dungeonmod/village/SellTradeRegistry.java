package com.dungeonmod.village;

import net.minecraft.item.ItemStack;

import java.util.*;

public class SellTradeRegistry {

    public record SellOffer(List<ItemStack> requiredItems, int requiredCount, ItemStack rewardItem, int rewardCount) {
        public boolean matches(ItemStack stack) {
            for (ItemStack req : requiredItems) {
                if (stack.isOf(req.getItem()) && stack.getName().getString().equals(req.getName().getString())) return true;
            }
            return false;
        }
    }

    private static final Map<String, List<SellOffer>> NPC_SELL_OFFERS = new HashMap<>();

    public static void register(String npcId, SellOffer offer) {
        NPC_SELL_OFFERS.computeIfAbsent(npcId, k -> new ArrayList<>()).add(offer);
    }

    public static List<SellOffer> getOffers(String npcId) {
        return NPC_SELL_OFFERS.getOrDefault(npcId, List.of());
    }

    public static void init() {
        var conseil = com.dungeonmod.ModItems.get("conseil");
        var biereB = com.dungeonmod.ModItems.get("biere_brune");
        var biereV = com.dungeonmod.ModItems.get("biere_viking");
        if (conseil != null && biereB != null && biereV != null) {
            register("gaspard", new SellOffer(List.of(biereB.createStack(), biereV.createStack()), 1, conseil.createStack(), 1));
        }
    }
}
