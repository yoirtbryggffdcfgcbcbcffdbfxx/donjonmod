package com.dungeonmod.mixin;

import net.minecraft.item.ItemStack;
import net.minecraft.screen.ScreenHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import java.util.List;

@Mixin(ScreenHandler.class)
public class ScreenHandlerMixin {

    @Inject(method = "updateSlotStacks", at = @At("HEAD"))
    private void truncateSlotStacks(int syncId, List<ItemStack> stacks, ItemStack cursor, CallbackInfo ci) {
        ScreenHandler self = (ScreenHandler)(Object)this;
        if (stacks.size() > self.slots.size()) {
            while (stacks.size() > self.slots.size()) {
                stacks.remove(stacks.size() - 1);
            }
        }
    }
}
