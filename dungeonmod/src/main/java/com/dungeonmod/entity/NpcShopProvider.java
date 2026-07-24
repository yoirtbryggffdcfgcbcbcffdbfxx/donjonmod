package com.dungeonmod.entity;

import net.minecraft.server.network.ServerPlayerEntity;

public interface NpcShopProvider {
    void openShop(ServerPlayerEntity player);
    void processBuy(ServerPlayerEntity player, int tradeIndex, int quantity);
    default void processSell(ServerPlayerEntity player, int tradeIndex) {}
}
