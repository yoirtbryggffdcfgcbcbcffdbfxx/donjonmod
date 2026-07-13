package com.dungeonmod.mixin;

import com.dungeonmod.util.EpeeHelper;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Item.class)
public class EpeeMixin {

    @Inject(method = "getMaxUseTime", at = @At("HEAD"), cancellable = true)
    private void getMaxUseTime(ItemStack stack, LivingEntity entity, CallbackInfoReturnable<Integer> cir) {
        if (EpeeHelper.isEpee(stack)) {
            cir.setReturnValue(72000);
        }
    }

    @Inject(method = "use", at = @At("HEAD"), cancellable = true)
    private void onUse(World world, PlayerEntity user, Hand hand, CallbackInfoReturnable<ActionResult> cir) {
        ItemStack stack = user.getStackInHand(hand);
        if (EpeeHelper.isEpee(stack)) {
            if (com.dungeonmod.util.EpeeBlockData.get(user).isOnCooldown()) {
                cir.setReturnValue(ActionResult.FAIL);
                return;
            }
            user.setCurrentHand(hand);
            cir.setReturnValue(ActionResult.SUCCESS);
        }
    }
}
