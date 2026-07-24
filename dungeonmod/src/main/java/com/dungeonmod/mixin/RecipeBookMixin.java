package com.dungeonmod.mixin;

import net.minecraft.client.gui.screen.recipebook.RecipeBookWidget;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(RecipeBookWidget.class)
public class RecipeBookMixin {

    // Forcer isOpen() à renvoyer false désactive proprement l'interface
    // et évite que l'inventaire ne se décale vers la droite.
    @Inject(method = "isOpen", at = @At("HEAD"), cancellable = true)
    private void alwaysClosed(CallbackInfoReturnable<Boolean> cir) {
        cir.setReturnValue(false);
    }
}