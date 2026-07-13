package com.dungeonmod.mixin;

import net.minecraft.component.DataComponentTypes;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Items;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PlayerEntity.class)
public class WeaponKnockbackMixin {

    @Inject(method = "attack", at = @At("RETURN"))
    private void applyWeaponKnockback(Entity target, CallbackInfo ci) {
        if (target == null) return;
        if (!(target instanceof LivingEntity living)) return;
        PlayerEntity player = (PlayerEntity) (Object) this;
        if (player.getWorld().isClient()) return;
        var stack = player.getMainHandStack();
        if (stack.isEmpty()) return;
        if (!stack.contains(DataComponentTypes.CUSTOM_NAME)) return;
        String name = stack.get(DataComponentTypes.CUSTOM_NAME).getString();

        boolean isDague = stack.isOf(Items.FLINT) && name.contains("Dague");
        boolean isBaton = stack.isOf(Items.STICK) && name.contains("Bâton");
        boolean isOs = stack.isOf(Items.BONE) && name.contains("Os");
        boolean isEpee = stack.isOf(Items.STONE_SWORD) && name.contains("Epée");
        boolean isLance = stack.isOf(Items.TRIDENT) && name.contains("Lance");
        boolean isSabre = stack.isOf(Items.IRON_SWORD) && name.contains("Sabre");

        double kb = 0;
        if (isDague) kb = 0.1;
        else if (isBaton) kb = 0.2;
        else if (isOs) kb = 0.5;
        else if (isEpee) kb = 0.2;
        else if (isLance) kb = 0.2;
        else if (isSabre) kb = 0.1;

        if (kb > 0) {
            double dx = player.getX() - living.getX();
            double dz = player.getZ() - living.getZ();
            living.takeKnockback(kb, dx, dz);
        }
    }
}