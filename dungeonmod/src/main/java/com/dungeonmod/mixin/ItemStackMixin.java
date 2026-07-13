package com.dungeonmod.mixin;

import net.minecraft.component.DataComponentTypes;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ArmorItem;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.function.Consumer;

@Mixin(ItemStack.class)
public class ItemStackMixin {

    @Inject(method = "appendAttributeModifiersTooltip", at = @At("HEAD"), cancellable = true)
    private void hideArmorTooltip(Consumer<Text> textConsumer, PlayerEntity player, CallbackInfo ci) {
        ItemStack stack = (ItemStack) (Object) this;
        if (stack.getItem() instanceof ArmorItem || stack.getItem() == Items.SKELETON_SKULL) {
            ci.cancel();
        }
    }
}
