package com.dungeonmod.client;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.item.Items;

public class DoubleJumpHandler {

    private static boolean doubleJumpUsed = false;
    private static boolean lastJumpDown = false;

    public static void init() {
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            ClientPlayerEntity player = client.player;
            if (player == null) return;

            boolean hasBoots = hasApollonBoots(player);

            // Au sol : réinitialiser le double saut
            if (player.isOnGround()) {
                doubleJumpUsed = false;
            }

            // En l'air : détecter l'appui sur saut (front montant)
            boolean jumpDown = client.options.jumpKey.isPressed();
            if (hasBoots && !player.isOnGround() && !doubleJumpUsed && jumpDown && !lastJumpDown) {
                player.setVelocity(player.getVelocity().x, 0.42, player.getVelocity().z);
                doubleJumpUsed = true;
            }
            lastJumpDown = jumpDown;
        });
    }

    private static boolean hasApollonBoots(ClientPlayerEntity player) {
        var boots = player.getInventory().getArmorStack(0);
        if (boots.isEmpty() || !boots.isOf(Items.GOLDEN_BOOTS)) return false;
        if (!boots.contains(DataComponentTypes.CUSTOM_NAME)) return false;
        return boots.get(DataComponentTypes.CUSTOM_NAME).getString().contains("Bottes d'Apollon");
    }
}
