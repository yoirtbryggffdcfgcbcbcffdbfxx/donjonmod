package com.dungeonmod.mixin;

import com.dungeonmod.util.FlecheComboData;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Item.class)
public class FlecheUsingMixin {

    @Inject(method = "getMaxUseTime", at = @At("HEAD"), cancellable = true)
    private void onGetMaxUseTime(ItemStack stack, LivingEntity entity, CallbackInfoReturnable<Integer> cir) {
        if (FlecheComboData.isFleche(stack)) cir.setReturnValue(72000);
    }

    @Inject(method = "usageTick", at = @At("HEAD"))
    private void onUsageTick(World world, LivingEntity user, ItemStack stack, int remainingUseTicks, CallbackInfo ci) {
        if (!(user instanceof ServerPlayerEntity player)) return;
        if (!FlecheComboData.isFleche(stack)) return;
        int level = FlecheComboData.getSangLevel(stack);
        Identifier model;
        if (level >= 2) model = Identifier.of("dungeonmod", "arrow_sang_2_using");
        else if (level == 1) model = Identifier.of("dungeonmod", "arrow_sang_1_using");
        else model = Identifier.of("dungeonmod", "fleche_using");
        stack.set(DataComponentTypes.ITEM_MODEL, model);
    }

    @Inject(method = "onStoppedUsing", at = @At("HEAD"))
    private void onStoppedUsing(ItemStack stack, World world, LivingEntity user, int remainingUseTicks, CallbackInfoReturnable<Boolean> cir) {
        if (!(user instanceof ServerPlayerEntity player)) return;
        if (!FlecheComboData.isFleche(stack)) return;
        int level = FlecheComboData.getSangLevel(stack);
        Identifier model;
        if (level >= 2) model = Identifier.of("dungeonmod", "arrow_sang_2");
        else if (level == 1) model = Identifier.of("dungeonmod", "arrow_sang_1");
        else model = Identifier.ofVanilla("arrow");
        stack.set(DataComponentTypes.ITEM_MODEL, model);
    }
}
