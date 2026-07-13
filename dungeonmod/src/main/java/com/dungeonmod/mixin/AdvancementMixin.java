package com.dungeonmod.mixin;

import net.minecraft.advancement.AdvancementDisplay;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(AdvancementDisplay.class)
public class AdvancementMixin {

    @Inject(method = "shouldAnnounceToChat", at = @At("HEAD"), cancellable = true)
    private void onShouldAnnounceToChat(CallbackInfoReturnable<Boolean> cir) {
        cir.setReturnValue(false);
    }
}
