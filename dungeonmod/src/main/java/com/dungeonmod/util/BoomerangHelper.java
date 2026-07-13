package com.dungeonmod.util;

import com.dungeonmod.entity.BoomerangEntity;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.item.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.Vec3d;

public class BoomerangHelper {

    public static boolean isBoomerang(ItemStack stack) {
        if (stack.isEmpty() || !stack.isOf(Items.STICK)) return false;
        if (!stack.contains(DataComponentTypes.CUSTOM_NAME)) return false;
        return stack.get(DataComponentTypes.CUSTOM_NAME).getString().contains("Boomerang");
    }

    public static void throwBoomerang(ServerPlayerEntity player, ItemStack stack) {
        var world = (ServerWorld) player.getWorld();
        var boomerang = new BoomerangEntity(world, player, stack.copyWithCount(1));
        // Calculate velocity from player's look direction (no pitch component for horizontal flight)
        Vec3d look = player.getRotationVec(1.0f);
        Vec3d vel = look.multiply(0.5);
        boomerang.setVelocity(vel);
        world.spawnEntity(boomerang);

        if (!player.isCreative()) {
            stack.decrement(1);
        }
    }
}
