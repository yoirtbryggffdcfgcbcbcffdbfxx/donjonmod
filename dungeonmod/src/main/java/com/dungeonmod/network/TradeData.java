package com.dungeonmod.network;

import net.minecraft.item.ItemStack;

public record TradeData(ItemStack input, ItemStack output, int originalIndex) {}
