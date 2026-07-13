package com.dungeonmod.mixin;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.hud.InGameHud;
import net.minecraft.entity.player.PlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(InGameHud.class)
public class InGameHudMixin {

    @ModifyVariable(method = "renderHealthBar", at = @At("HEAD"), index = 3)
    private int centerHealthBarX(int x) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null) return x;
        float maxHealth = client.player.getMaxHealth();
        int halfHearts = Math.min((int)(maxHealth / 2.0f), 10);
        int shift = 91 - halfHearts * 4;
        return x + shift;
    }

    @Inject(method = "renderArmor", at = @At("HEAD"), cancellable = true)
    private static void cancelArmor(DrawContext context, PlayerEntity player, int i, int j, int k, int m, CallbackInfo ci) {
        ci.cancel();
    }

    @Inject(method = "render", at = @At("TAIL"))
    private void onRender(DrawContext context, net.minecraft.client.render.RenderTickCounter tickCounter, CallbackInfo ci) {
        // Protection affichée uniquement via la Dent de loup (DentDeLoupHandler)
    }
}
