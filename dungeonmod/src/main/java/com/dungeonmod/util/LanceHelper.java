package com.dungeonmod.util;

import net.minecraft.component.DataComponentTypes;
import net.minecraft.item.Items;
import net.minecraft.item.ItemStack;

public class LanceHelper {

    public static boolean isLance(ItemStack stack) {
        if (stack.isEmpty() || !stack.isOf(Items.TRIDENT)) return false;
        if (!stack.contains(DataComponentTypes.CUSTOM_NAME)) return false;
        return stack.get(DataComponentTypes.CUSTOM_NAME).getString().contains("Lance");
    }
}
