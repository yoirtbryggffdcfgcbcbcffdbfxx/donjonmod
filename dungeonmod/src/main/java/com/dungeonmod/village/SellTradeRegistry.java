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
    private static final Map<String, List<ItemStack>> NPC_ACCEPTED_INPUTS = new HashMap<>();

    public static void register(String npcId, SellOffer offer) {
        NPC_SELL_OFFERS.computeIfAbsent(npcId, k -> new ArrayList<>()).add(offer);
    }

    public static List<SellOffer> getOffers(String npcId) {
        return NPC_SELL_OFFERS.getOrDefault(npcId, List.of());
    }

    public static void registerAcceptedInput(String npcId, ItemStack stack) {
        NPC_ACCEPTED_INPUTS.computeIfAbsent(npcId, k -> new ArrayList<>()).add(stack);
    }

    public static List<ItemStack> getAcceptedInputs(String npcId) {
        return NPC_ACCEPTED_INPUTS.getOrDefault(npcId, List.of());
    }

    public static boolean isItemAccepted(String npcId, ItemStack stack) {
        for (SellOffer offer : NPC_SELL_OFFERS.getOrDefault(npcId, List.of())) {
            if (offer.matches(stack)) return true;
        }
        for (ItemStack accepted : NPC_ACCEPTED_INPUTS.getOrDefault(npcId, List.of())) {
            if (stack.isOf(accepted.getItem()) && stack.getName().getString().equals(accepted.getName().getString())) return true;
        }
        return false;
    }

    public static void init() {
        var conseil = com.dungeonmod.ModItems.get("conseil");
        var biereV = com.dungeonmod.ModItems.get("biere_viking");
        var biereB = com.dungeonmod.ModItems.get("biere_brune");
        if (conseil != null && biereV != null) {
            register("gaspard", new SellOffer(List.of(biereV.createStack()), 1, conseil.createStack(), 1));
        }
        if (biereB != null) {
            registerAcceptedInput("gaspard", biereB.createStack());
        }
    }
}
