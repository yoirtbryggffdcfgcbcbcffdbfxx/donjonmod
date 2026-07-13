package com.dungeonmod.mixin;

import net.minecraft.component.DataComponentTypes;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Items;
import net.minecraft.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PlayerEntity.class)
public class AttackCooldownMixin {

    @Inject(method = "attack", at = @At("HEAD"), cancellable = true)
    private void onAttack(Entity target, CallbackInfo ci) {
        PlayerEntity player = (PlayerEntity) (Object) this;
        float minProgress = isDualDague(player) ? 0.68f : 1.0f;
        if (player.getAttackCooldownProgress(0.5f) < minProgress) {
            ci.cancel();
        }
    }

    private static boolean isDualDague(PlayerEntity player) {
        ItemStack main = player.getMainHandStack();
        ItemStack off = player.getOffHandStack();
        if (main.isEmpty() || off.isEmpty() || !main.isOf(Items.FLINT) || !off.isOf(Items.FLINT)) return false;
        if (!main.contains(DataComponentTypes.CUSTOM_NAME) || !off.contains(DataComponentTypes.CUSTOM_NAME)) return false;
        return main.get(DataComponentTypes.CUSTOM_NAME).getString().contains("Dague")
            && off.get(DataComponentTypes.CUSTOM_NAME).getString().contains("Dague");
    }
}
