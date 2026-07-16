package com.dungeonmod.mixin;

import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.item.consume.UseAction;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Item.class)
public class SyrinxMixin {

    private static boolean isSyrinx(ItemStack stack) {
        return !stack.isEmpty() && stack.isOf(Items.STICK)
            && stack.contains(net.minecraft.component.DataComponentTypes.CUSTOM_NAME)
            && stack.get(net.minecraft.component.DataComponentTypes.CUSTOM_NAME).getString().contains("Syrinx oublié");
    }

    @Inject(method = "getMaxUseTime", at = @At("HEAD"), cancellable = true)
    private void getMaxUseTime(ItemStack stack, LivingEntity entity, CallbackInfoReturnable<Integer> cir) {
        if (isSyrinx(stack)) {
            cir.setReturnValue(72000);
        }
    }

    @Inject(method = "getUseAction", at = @At("RETURN"), cancellable = true)
    private void getUseAction(ItemStack stack, CallbackInfoReturnable<UseAction> cir) {
        if (isSyrinx(stack)) {
            cir.setReturnValue(UseAction.BOW);
        }
    }

    @Inject(method = "use", at = @At("HEAD"), cancellable = true)
    private void onUse(World world, PlayerEntity user, Hand hand, CallbackInfoReturnable<ActionResult> cir) {
        ItemStack stack = user.getStackInHand(hand);
        if (isSyrinx(stack)) {
            user.setCurrentHand(hand);
            cir.setReturnValue(ActionResult.SUCCESS);
        }
    }
}
