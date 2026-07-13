package com.dungeonmod.mixin;

import com.dungeonmod.util.BaguetteData;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Items;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.Vec3d;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(MinecraftClient.class)
public class ClientAttackCooldownMixin {

    @Inject(method = "doAttack", at = @At("HEAD"), cancellable = true)
    private void onDoAttack(CallbackInfoReturnable<Boolean> cir) {
        PlayerEntity player = MinecraftClient.getInstance().player;
        if (player == null) return;
        float progress = player.getAttackCooldownProgress(0.5f);
        if (progress < 1.0f) {
            cir.setReturnValue(false);
            return;
        }
        // Sabre: air swing → charge combo
        var stack = player.getMainHandStack();
        if (!stack.isEmpty() && stack.isOf(Items.IRON_SWORD) 
            && stack.contains(net.minecraft.component.DataComponentTypes.CUSTOM_NAME)
            && stack.get(net.minecraft.component.DataComponentTypes.CUSTOM_NAME).getString().contains("Sabre")) {
            MinecraftClient client = MinecraftClient.getInstance();
            if (client.crosshairTarget == null || client.crosshairTarget.getType() != HitResult.Type.ENTITY) {
                com.dungeonmod.util.SabreComboData.addCombo(player);
            }
        }
        // Baguette: tir dans le vide/clique gauche
        if (BaguetteData.isBaguette(stack)) {
            if (BaguetteData.isOnCooldown(player)) {
                cir.setReturnValue(false);
                return;
            }
            com.dungeonmod.util.BaguetteData.tryWandAttackClient(player);
        }
        MinecraftClient client = MinecraftClient.getInstance();
        // Fouet: grappling hook (si pas sur un mob) — nécessite le plastron du chasseur
        if (!stack.isEmpty() && stack.isOf(Items.STICK) 
            && stack.contains(net.minecraft.component.DataComponentTypes.CUSTOM_NAME)
            && stack.get(net.minecraft.component.DataComponentTypes.CUSTOM_NAME).getString().contains("Fouet")) {
            var chest = player.getInventory().getArmorStack(2);
            if (chest.isEmpty() || !chest.isOf(Items.CHAINMAIL_CHESTPLATE)) return;
            if (!chest.contains(net.minecraft.component.DataComponentTypes.CUSTOM_NAME)) return;
            if (!chest.get(net.minecraft.component.DataComponentTypes.CUSTOM_NAME).getString().contains("Plastron du chasseur")) return;
            if (client.crosshairTarget == null || client.crosshairTarget.getType() != HitResult.Type.ENTITY) {
                Vec3d targetPos;
                if (client.crosshairTarget != null && client.crosshairTarget.getType() == HitResult.Type.BLOCK) {
                    targetPos = Vec3d.ofCenter(((BlockHitResult) client.crosshairTarget).getBlockPos());
                } else {
                    targetPos = player.getPos().add(player.getRotationVec(1.0f).multiply(10.0));
                }
                double dist = targetPos.distanceTo(player.getPos());
                if (dist > 10.0) return;
                Vec3d dir = targetPos.subtract(player.getPos()).normalize();
                double speed = Math.min(dist * 0.3, 2.5);
                player.setVelocity(dir.x * speed, dir.y * speed, dir.z * speed);
                player.velocityModified = true;
                player.resetLastAttackedTicks();
            }
        }
        if (client.crosshairTarget != null && client.crosshairTarget.getType() == HitResult.Type.BLOCK) {
            player.resetLastAttackedTicks();
        }
    }
}
