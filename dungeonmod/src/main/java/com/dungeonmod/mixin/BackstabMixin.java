package com.dungeonmod.mixin;

import net.minecraft.component.DataComponentTypes;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Items;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.Vec3d;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LivingEntity.class)
public class BackstabMixin {

    @ModifyVariable(method = "damage", at = @At("HEAD"), argsOnly = true)
    private float backstabDamage(float amount, ServerWorld world, DamageSource source, float originalAmount) {
        if (!(source.getAttacker() instanceof PlayerEntity player)) return amount;
        var stack = player.getMainHandStack();
        if (!stack.isOf(Items.FLINT)) return amount;
        if (!stack.contains(DataComponentTypes.CUSTOM_NAME)) return amount;
        if (!stack.get(DataComponentTypes.CUSTOM_NAME).getString().contains("Dague")) return amount;

        Entity target = (Entity) (Object) this;
        Vec3d lookDir = target.getRotationVec(1.0f).normalize();
        Vec3d toAttacker = player.getPos().subtract(target.getPos()).normalize();
        double dot = lookDir.dotProduct(toAttacker);
        if (dot < -0.3) {
            return amount * 2.0f;
        }
        return amount;
    }

    @Inject(method = "damage", at = @At("TAIL"))
    private void applyDagueKnockback(ServerWorld world, DamageSource source, float amount, CallbackInfoReturnable<Boolean> cir) {
        if (!(source.getAttacker() instanceof PlayerEntity player)) return;
        var stack = player.getMainHandStack();
        if (!stack.isOf(Items.FLINT)) return;
        if (!stack.contains(DataComponentTypes.CUSTOM_NAME)) return;
        if (!stack.get(DataComponentTypes.CUSTOM_NAME).getString().contains("Dague")) return;

        Entity target = (Entity) (Object) this;
        if (!(target instanceof LivingEntity living)) return;
        double dx = player.getX() - living.getX();
        double dz = player.getZ() - living.getZ();
        living.takeKnockback(0.1, dx, dz);
    }
}
