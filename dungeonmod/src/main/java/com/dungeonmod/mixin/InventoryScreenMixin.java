package com.dungeonmod.mixin;

import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.InventoryScreen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(InventoryScreen.class)
public class InventoryScreenMixin {

    @Inject(method = "init", at = @At("TAIL"))
    private void hideRecipeBookButton(CallbackInfo ci) {
        InventoryScreen screen = (InventoryScreen)(Object)this;
        for (var child : screen.children()) {
            if (child instanceof ButtonWidget btn) {
                btn.visible = false;
                btn.active = false;
            }
        }
    }

    @ModifyArg(method = "drawForeground", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/DrawContext;drawText(Lnet/minecraft/client/font/TextRenderer;Lnet/minecraft/text/Text;IIIZ)I", ordinal = 0), index = 1)
    private Text emptyCraftingTitle(Text text) {
        return Text.empty();
    }
}
