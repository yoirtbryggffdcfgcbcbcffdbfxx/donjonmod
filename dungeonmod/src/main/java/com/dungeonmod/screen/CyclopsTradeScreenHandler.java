package com.dungeonmod.screen;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.slot.Slot;

public class CyclopsTradeScreenHandler extends ScreenHandler {

    public int selectedTrade = -1;
    private final Inventory depositInventory;

    public CyclopsTradeScreenHandler(int syncId, PlayerInventory playerInventory) {
        super(ModScreenHandlers.CYCLOPS_TRADE_SCREEN_HANDLER, syncId);

        depositInventory = new SingleSlotInventory();

        this.addSlot(new Slot(depositInventory, 0, 125, 41) {
            @Override public boolean canInsert(ItemStack stack) { return true; }
            @Override public int getMaxItemCount() { return 1; }
        });

        for (int row = 0; row < 3; ++row) {
            for (int col = 0; col < 9; ++col) {
                this.addSlot(new Slot(playerInventory, col + row * 9 + 9, 108 + col * 18, 84 + row * 18));
            }
        }
        for (int col = 0; col < 9; ++col) {
            this.addSlot(new Slot(playerInventory, col, 108 + col * 18, 142));
        }
    }

    public CyclopsTradeScreenHandler(int syncId, PlayerInventory playerInventory, int selectedTrade) {
        this(syncId, playerInventory);
        this.selectedTrade = selectedTrade;
    }

    @Override
    public ItemStack quickMove(PlayerEntity player, int slotIndex) {
        return ItemStack.EMPTY;
    }

    @Override
    public boolean canUse(PlayerEntity player) {
        return true;
    }

    @Override
    public void onClosed(PlayerEntity player) {
        super.onClosed(player);
        if (!depositInventory.isEmpty()) {
            ItemStack stack = depositInventory.removeStack(0);
            if (!player.getInventory().insertStack(stack)) {
                player.dropItem(stack, false);
            }
        }
    }

    private static class SingleSlotInventory implements Inventory {
        private ItemStack stack = ItemStack.EMPTY;

        @Override public int size() { return 1; }
        @Override public boolean isEmpty() { return stack.isEmpty(); }
        @Override public ItemStack getStack(int slot) { return slot == 0 ? stack : ItemStack.EMPTY; }
        @Override public ItemStack removeStack(int slot, int amount) {
            if (slot != 0 || stack.isEmpty()) return ItemStack.EMPTY;
            ItemStack result = stack.split(amount);
            if (stack.isEmpty()) stack = ItemStack.EMPTY;
            return result;
        }
        @Override public ItemStack removeStack(int slot) {
            if (slot != 0) return ItemStack.EMPTY;
            ItemStack result = stack;
            stack = ItemStack.EMPTY;
            return result;
        }
        @Override public void setStack(int slot, ItemStack s) { if (slot == 0) stack = s; }
        @Override public void markDirty() {}
        @Override public boolean canPlayerUse(PlayerEntity player) { return true; }
        @Override public void clear() { stack = ItemStack.EMPTY; }
    }
}
