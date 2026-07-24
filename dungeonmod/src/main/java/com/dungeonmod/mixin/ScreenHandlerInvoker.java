package com.dungeonmod.mixin;

import net.minecraft.item.ItemStack;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.slot.Slot;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(ScreenHandler.class)
public interface ScreenHandlerInvoker {
    @Invoker("insertItem")
    boolean invokeInsertItem(ItemStack stack, int start, int end, boolean reverse);

    @Invoker("addSlot")
    Slot invokeAddSlot(Slot slot);
}
