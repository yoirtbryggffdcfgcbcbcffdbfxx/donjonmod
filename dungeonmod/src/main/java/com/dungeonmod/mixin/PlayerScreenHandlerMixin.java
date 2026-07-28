package com.dungeonmod.mixin;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.PlayerScreenHandler;
import net.minecraft.screen.slot.Slot;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(PlayerScreenHandler.class)
public class PlayerScreenHandlerMixin {

    @Shadow @Final private PlayerEntity owner;

    // Positions d'origine Vanilla des 5 slots de craft (0 = résultat, 1..4 = grille 2x2)
    private static final int[] ORIGINAL_X = {154, 98, 116, 98, 116};
    private static final int[] ORIGINAL_Y = {28, 18, 18, 36, 36};

    private static boolean isCreativeSafe(PlayerEntity player) {
        if (player == null) return false;
        if (player.getAbilities() != null && player.getAbilities().creativeMode) {
            return true;
        }
        try {
            return player.isCreative();
        } catch (Exception e) {
            return false;
        }
    }

    // Mise à jour dynamique de la position des slots selon le gamemode
    private void updateSlotsPosition(PlayerScreenHandler handler, PlayerEntity player) {
        if (player == null) return;
        boolean creative = isCreativeSafe(player);
        for (int i = 0; i < 5 && i < handler.slots.size(); i++) {
            Slot slot = handler.slots.get(i);
            SlotAccessor accessor = (SlotAccessor) slot;
            if (creative) {
                // En Créatif : position normale Vanilla
                accessor.setX(ORIGINAL_X[i]);
                accessor.setY(ORIGINAL_Y[i]);
            } else {
                // En Survie / Aventure : masque les slots hors de l'écran
                accessor.setX(-9999);
                accessor.setY(-9999);
            }
        }
    }

    @Inject(method = "<init>", at = @At("RETURN"))
    private void onInit(PlayerInventory inventory, boolean onServer, PlayerEntity owner, CallbackInfo ci) {
        PlayerScreenHandler self = (PlayerScreenHandler)(Object)this;
        updateSlotsPosition(self, owner);
    }

    // Déclenché DÈS L'OUVERTURE DE L'INVENTAIRE (Frame 1), sans attendre d'interaction !
    @Inject(method = "canUse", at = @At("HEAD"))
    private void onCanUse(PlayerEntity player, CallbackInfoReturnable<Boolean> cir) {
        PlayerScreenHandler self = (PlayerScreenHandler)(Object)this;
        updateSlotsPosition(self, player);
    }

    @Inject(method = "onContentChanged", at = @At("HEAD"))
    private void onContentChanged(CallbackInfo ci) {
        PlayerScreenHandler self = (PlayerScreenHandler)(Object)this;
        updateSlotsPosition(self, this.owner);
    }

    @Inject(method = "quickMove", at = @At("HEAD"), cancellable = true)
    private void fixQuickMove(PlayerEntity player, int index, CallbackInfoReturnable<ItemStack> cir) {
        if (isCreativeSafe(player)) {
            return; // En créatif, comportement Vanilla 100% normal
        }

        PlayerScreenHandler self = (PlayerScreenHandler)(Object)this;
        if (index < 0 || index >= self.slots.size()) {
            cir.setReturnValue(ItemStack.EMPTY);
            return;
        }

        // En Survie, ignorer les interactions avec les slots 0..4 masqués
        if (index < 5) {
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

        if (index >= 5 && index <= 8) {
            if (!invoker.invokeInsertItem(stack, 9, 45, false)) {
                cir.setReturnValue(ItemStack.EMPTY);
                return;
            }
        } else if (index >= 9 && index <= 35) {
            if (!invoker.invokeInsertItem(stack, 5, 9, false) && !invoker.invokeInsertItem(stack, 36, 45, false)) {
                cir.setReturnValue(ItemStack.EMPTY);
                return;
            }
        } else if (index >= 36 && index <= 44) {
            if (!invoker.invokeInsertItem(stack, 5, 9, false) && !invoker.invokeInsertItem(stack, 9, 36, false)) {
                cir.setReturnValue(ItemStack.EMPTY);
                return;
            }
        } else if (index == 45) {
            if (!invoker.invokeInsertItem(stack, 9, 45, false)) {
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