package com.dungeonmod.mixin;

import com.dungeonmod.util.FlecheComboData;
import net.minecraft.entity.ItemEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ItemEntity.class)
public class ItemEntityMixin {

    @Inject(method = "<init>(Lnet/minecraft/world/World;DDDLnet/minecraft/item/ItemStack;)V", at = @At("TAIL"))
    private void onInit(World world, double x, double y, double z, ItemStack stack, CallbackInfo ci) {
        if (FlecheComboData.isFleche(stack) && FlecheComboData.getSangLevel(stack) > 0) {
            FlecheComboData.revertArrow(stack);
        }
    }
}
