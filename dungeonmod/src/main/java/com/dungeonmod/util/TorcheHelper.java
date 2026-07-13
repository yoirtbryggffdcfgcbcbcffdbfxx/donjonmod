package com.dungeonmod.util;

import net.minecraft.component.DataComponentTypes;
import net.minecraft.item.Items;
import net.minecraft.item.ItemStack;

public class TorcheHelper {

    public static boolean isTorche(ItemStack stack) {
        if (stack.isEmpty() || !stack.isOf(Items.STICK)) return false;
        if (!stack.contains(DataComponentTypes.CUSTOM_NAME)) return false;
        return stack.get(DataComponentTypes.CUSTOM_NAME).getString().contains("Torche");
    }
}
