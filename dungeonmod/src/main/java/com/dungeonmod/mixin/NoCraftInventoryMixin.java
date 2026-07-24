package com.dungeonmod.mixin;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.InventoryScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(InventoryScreen.class)
public class NoCraftInventoryMixin {

    @Inject(method = "init", at = @At("TAIL"))
    private void hideRecipeBookAndCraft(CallbackInfo ci) {
        InventoryScreen screen = (InventoryScreen)(Object)this;
        // Cacher tous les éléments liés au livre de recettes
        screen.children().removeIf(c -> {
            String name = c.getClass().getName();
            return name.contains("RecipeBook") || name.contains("recipe") || name.contains("RecipeButton");
        });
    }

    @Inject(method = "drawBackground", at = @At("TAIL"))
    private void coverCraftAndRecipeArea(DrawContext context, float delta, int mouseX, int mouseY, CallbackInfo ci) {
        InventoryScreen screen = (InventoryScreen)(Object)this;
        int x = (screen.width - 176) / 2;
        int y = (screen.height - 166) / 2;
        // Couvrir toute la zone de droite (craft + livre de recettes)
        context.fill(x + 78, y + 4, screen.width, y + 85, 0xFFC6C6C6);
    }
}
