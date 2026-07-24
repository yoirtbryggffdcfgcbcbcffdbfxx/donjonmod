package com.dungeonmod.util;

import net.minecraft.component.ComponentType;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.AttributeModifiersComponent;
import net.minecraft.entity.attribute.EntityAttributeModifier;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.util.Identifier;
import org.spongepowered.asm.mixin.Unique;

import java.util.*;
import java.util.concurrent.*;
import dev.kosmx.playerAnim.api.layered.IAnimation;
import dev.kosmx.playerAnim.api.layered.ModifierLayer;

public class GlaiveComboData {
    public static final Map<UUID, ModifierLayer<IAnimation>> spinLayers = new HashMap<>();
    private static final Map<UUID, Integer> comboStage = new ConcurrentHashMap<>(); // stage(0-3)
    private static final Map<UUID, Long> lastHitTime = new HashMap<>();
    private static final long RESET_TIME = 3000;

    public static boolean isGlaive(ItemStack stack) {
        if (stack.isEmpty() || !stack.isOf(Items.STICK)) return false;
        if (!stack.contains(DataComponentTypes.CUSTOM_NAME)) return false;
        return stack.get(DataComponentTypes.CUSTOM_NAME).getString().contains("Glaive");
    }

    public static boolean isOnCooldown(PlayerEntity player) {
        Integer stage = comboStage.get(player.getUuid());
        if (stage == null) return false;
        long lastHit = lastHitTime.getOrDefault(player.getUuid(), 0L);
        long elapsed = System.currentTimeMillis() - lastHit;
        if (elapsed > RESET_TIME) {
            comboStage.remove(player.getUuid());
            lastHitTime.remove(player.getUuid());
            return false;
        }
        int cd = getCooldownForStage(stage);
        return elapsed < cd;
    }

    public static void applyNextStage(PlayerEntity player) {
        Integer stage = comboStage.get(player.getUuid());
        long now = System.currentTimeMillis();
        Long lastHit = lastHitTime.get(player.getUuid());

        int newStage;
        if (stage == null || lastHit == null || now - lastHit > RESET_TIME) {
            newStage = 0;
        } else {
            newStage = Math.min(stage + 1, 3);
        }

        comboStage.put(player.getUuid(), newStage);
        lastHitTime.put(player.getUuid(), now);

        // Mettre à jour l'ATTACK_SPEED sur l'item en main
        var stack = player.getMainHandStack();
        if (stack.isEmpty() || !isGlaive(stack)) return;

        float speedMod = getModifierForStage(newStage);
        var builder = AttributeModifiersComponent.builder();
        builder.add(EntityAttributes.ATTACK_DAMAGE,
            new EntityAttributeModifier(Identifier.of("dungeonmod", "glaive_attack"), 5.0, EntityAttributeModifier.Operation.ADD_VALUE),
            net.minecraft.component.type.AttributeModifierSlot.MAINHAND);
        builder.add(EntityAttributes.ATTACK_SPEED,
            new EntityAttributeModifier(Identifier.of("dungeonmod", "glaive_speed"), speedMod, EntityAttributeModifier.Operation.ADD_VALUE),
            net.minecraft.component.type.AttributeModifierSlot.MAINHAND);
        stack.set(DataComponentTypes.ATTRIBUTE_MODIFIERS, builder.build().withShowInTooltip(false));

        // Sync inventaire au client (slot barre = 31 + selectedSlot)
        if (player instanceof net.minecraft.server.network.ServerPlayerEntity sp) {
            sp.networkHandler.sendPacket(new net.minecraft.network.packet.s2c.play.ScreenHandlerSlotUpdateS2CPacket(
                0, 0, 31 + player.getInventory().selectedSlot, stack.copy()));
        }

    }

    public static int getComboStage(PlayerEntity player) {
        return comboStage.getOrDefault(player.getUuid(), 0);
    }

    private static int getCooldownForStage(int stage) {
        switch (stage) {
            case 0: return 1000;
            case 1: return 800;
            case 2: return 600;
            case 3: return 500;
            default: return 500;
        }
    }

    private static float getModifierForStage(int stage) {
        switch (stage) {
            case 0: return -3.0f;   // 1.0 attaque/s = 1s
            case 1: return -2.75f;  // 1.25 attaque/s = 0.8s
            case 2: return -2.33f;  // 1.67 attaque/s = 0.6s
            case 3: return -2.0f;   // 2.0 attaque/s = 0.5s
            default: return -2.0f;
        }
    }
}
