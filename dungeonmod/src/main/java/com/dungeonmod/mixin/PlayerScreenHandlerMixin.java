package com.dungeonmod.mixin;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.PlayerScreenHandler;
import net.minecraft.screen.slot.Slot;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(PlayerScreenHandler.class)
public class PlayerScreenHandlerMixin {

    @Inject(method = "<init>*", at = @At("RETURN"))
    private void removeCraftSlots(CallbackInfo ci) {
        PlayerScreenHandler self = (PlayerScreenHandler)(Object)this;
        for (int i = 4; i >= 0; i--) {
            if (i < self.slots.size()) self.slots.remove(i);
        }
        // Réassigner les IDs des slots après la suppression
        for (int i = 0; i < self.slots.size(); i++) {
            self.slots.get(i).id = i;
        }
    }

    @Inject(method = "quickMove", at = @At("HEAD"), cancellable = true)
    private void fixQuickMove(PlayerEntity player, int index, CallbackInfoReturnable<ItemStack> cir) {
        PlayerScreenHandler self = (PlayerScreenHandler)(Object)this;
        if (index < 0 || index >= self.slots.size()) {
            cir.setReturnValue(ItemStack.EMPTY);
            return;
        }
        Slot slot = self.slots.get(index);
        if (slot == null || !slot.hasStack()) {
            cir.setReturnValue(ItemStack.EMPTY);
            return;
        }
        ItemStack stack = slot.getStack();
        ItemStack original = stack.copy();
        var invoker = (ScreenHandlerInvoker)self;

        if (index < 4) {
            // Armor → main inventory + hotbar + offhand
            if (!invoker.invokeInsertItem(stack, 4, 40, false)) {
                cir.setReturnValue(ItemStack.EMPTY);
                return;
            }
        } else if (index < 31) {
            // Main inventory → armor first, then hotbar + offhand
            if (!invoker.invokeInsertItem(stack, 0, 4, false)
                && !invoker.invokeInsertItem(stack, 31, 40, false)) {
                cir.setReturnValue(ItemStack.EMPTY);
                return;
            }
        } else if (index < 40) {
            // Hotbar → armor first, then main inventory
            if (!invoker.invokeInsertItem(stack, 0, 4, false)
                && !invoker.invokeInsertItem(stack, 4, 31, false)) {
                cir.setReturnValue(ItemStack.EMPTY);
                return;
            }
        } else {
            // Offhand → main inventory + hotbar
            if (!invoker.invokeInsertItem(stack, 4, 40, false)) {
                cir.setReturnValue(ItemStack.EMPTY);
                return;
            }
        }

        if (stack.isEmpty()) slot.setStack(ItemStack.EMPTY);
        slot.markDirty();
        if (stack.getCount() == original.getCount()) {
            cir.setReturnValue(ItemStack.EMPTY);
            return;
        }
        slot.onTakeItem(player, stack);
        cir.setReturnValue(original);
    }
}
