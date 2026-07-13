package com.dungeonmod.mixin;

import com.dungeonmod.util.FouetHelper;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.consume.UseAction;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Item.class)
public class FouetChargeMixin {

    @Inject(method = "getMaxUseTime", at = @At("HEAD"), cancellable = true)
    private void getMaxUseTime(ItemStack stack, LivingEntity entity, CallbackInfoReturnable<Integer> cir) {
        if (FouetHelper.isFouet(stack)) {
            cir.setReturnValue(72000);
        }
    }

    @Inject(method = "use", at = @At("HEAD"), cancellable = true)
    private void onUse(World world, PlayerEntity user, Hand hand, CallbackInfoReturnable<ActionResult> cir) {
        ItemStack stack = user.getStackInHand(hand);
        if (FouetHelper.isFouet(stack)) {
            user.setCurrentHand(hand);
            cir.setReturnValue(ActionResult.SUCCESS);
        }
    }

    @Inject(method = "getUseAction", at = @At("RETURN"), cancellable = true)
    private void getUseAction(ItemStack stack, CallbackInfoReturnable<UseAction> cir) {
        if (FouetHelper.isFouet(stack)) {
            cir.setReturnValue(UseAction.BOW);
        }
    }

    @Inject(method = "onStoppedUsing", at = @At("HEAD"), cancellable = true)
    private void onStoppedUsing(ItemStack stack, World world, LivingEntity user, int remainingUseTicks, CallbackInfoReturnable<Boolean> cir) {
        if (!FouetHelper.isFouet(stack)) return;
        if (!(user instanceof PlayerEntity player)) return;
        if (world.isClient()) return;

        int usedTicks = 72000 - remainingUseTicks;
        if (usedTicks < 2) {
            cir.setReturnValue(false);
            return;
        }

        FouetHelper.doWhipAttack(player, Math.min(usedTicks, 30));
        cir.setReturnValue(true);
    }

    @Inject(method = "usageTick", at = @At("HEAD"))
    private void onUsageTick(World world, LivingEntity user, ItemStack stack, int useTime, CallbackInfo ci) {
        if (!FouetHelper.isFouet(stack)) return;
        if (!(user instanceof PlayerEntity player)) return;
        if (world.isClient()) return;

        if (world instanceof ServerWorld sw) {
            Vec3d pos = player.getPos().add(0, player.getStandingEyeHeight(), 0)
                .add(player.getRotationVec(1.0f).multiply(0.5));
            double heightOffset = Math.min(useTime / 30.0, 1.0) * 1.5;
            sw.spawnParticles(ParticleTypes.SMOKE,
                pos.x, pos.y + heightOffset, pos.z,
                1, 0.05, 0.05, 0.05, 0.02);
        }
    }
}
