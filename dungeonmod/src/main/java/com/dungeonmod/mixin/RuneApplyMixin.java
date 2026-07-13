package com.dungeonmod.mixin;

import com.dungeonmod.util.BaguetteData;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.slot.Slot;
import net.minecraft.screen.slot.SlotActionType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ScreenHandler.class)
public class RuneApplyMixin {

    @Inject(method = "onSlotClick", at = @At("HEAD"), cancellable = true)
    private void onSlotClick(int slotIndex, int button, SlotActionType actionType, PlayerEntity player, CallbackInfo ci) {
        if (player.getWorld().isClient()) return;
        if (slotIndex < 0 || slotIndex >= player.currentScreenHandler.slots.size()) return;
        Slot slot = player.currentScreenHandler.getSlot(slotIndex);
        ItemStack slotStack = slot.getStack();
        ItemStack cursorStack = player.currentScreenHandler.getCursorStack();

        if (cursorStack.isEmpty() || slotStack.isEmpty()) return;

        // Rune dans le curseur → clic sur baguette dans l'inventaire
        String runeType = BaguetteData.getRuneType(cursorStack);
        if (runeType != null && BaguetteData.isBaguette(slotStack)) {
            BaguetteData.appliquerRune(slotStack, runeType, player);
            if (!player.isCreative()) cursorStack.decrement(1);
            player.currentScreenHandler.setCursorStack(cursorStack.isEmpty() ? ItemStack.EMPTY : cursorStack);
            ci.cancel();
            return;
        }

        // Rune dans l'inventaire → clic sur baguette dans le curseur
        runeType = BaguetteData.getRuneType(slotStack);
        if (runeType != null && BaguetteData.isBaguette(cursorStack)) {
            BaguetteData.appliquerRune(cursorStack, runeType, player);
            if (!player.isCreative()) slotStack.decrement(1);
            slot.setStack(slotStack.isEmpty() ? ItemStack.EMPTY : slotStack);
            ci.cancel();
            return;
        }
    }
}
