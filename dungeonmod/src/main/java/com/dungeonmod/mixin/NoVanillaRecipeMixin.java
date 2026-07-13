package com.dungeonmod.mixin;

import net.minecraft.component.DataComponentTypes;
import net.minecraft.item.ItemStack;
import net.minecraft.recipe.Ingredient;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Ingredient.class)
public class NoVanillaRecipeMixin {

    @Inject(method = "test(Lnet/minecraft/item/ItemStack;)Z", at = @At("HEAD"), cancellable = true)
    private void test(ItemStack stack, CallbackInfoReturnable<Boolean> cir) {
        if (stack.contains(DataComponentTypes.CUSTOM_NAME)) {
            cir.setReturnValue(false);
        }
    }
}
