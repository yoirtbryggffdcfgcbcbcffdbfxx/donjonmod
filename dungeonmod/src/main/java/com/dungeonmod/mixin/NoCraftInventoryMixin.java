package com.dungeonmod.mixin;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.InventoryScreen;
import net.minecraft.world.GameMode;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(InventoryScreen.class)
public class NoCraftInventoryMixin {

    private static boolean isNotCreative() {
        return MinecraftClient.getInstance().player != null
            && MinecraftClient.getInstance().interactionManager != null
            && MinecraftClient.getInstance().interactionManager.getCurrentGameMode() != GameMode.CREATIVE;
    }

    @Inject(method = "init", at = @At("TAIL"))
    private void hideRecipeBookAndCraft(CallbackInfo ci) {
        if (!isNotCreative()) return;
        InventoryScreen screen = (InventoryScreen)(Object)this;
        screen.children().removeIf(c -> {
            String name = c.getClass().getName();
            return name.contains("RecipeBook") || name.contains("recipe") || name.contains("RecipeButton");
        });
    }

    @Inject(method = "drawBackground", at = @At("TAIL"))
    private void coverCraftAndRecipeArea(DrawContext context, float delta, int mouseX, int mouseY, CallbackInfo ci) {
        if (!isNotCreative()) return;
        InventoryScreen screen = (InventoryScreen)(Object)this;
        int x = (screen.width - 176) / 2;
        int y = (screen.height - 166) / 2;
        context.fill(x + 78, y + 4, screen.width, y + 85, 0xFFC6C6C6);
    }
}
