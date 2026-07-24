package com.dungeonmod.mixin;

import com.dungeonmod.client.SubtitleOverlay;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.hud.InGameHud;
import net.minecraft.client.render.RenderTickCounter;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(InGameHud.class)
public class DialogueHudMixin {

    @Inject(method = "renderHotbar", at = @At("HEAD"), cancellable = true)
    private void hideHotbar(DrawContext context, RenderTickCounter tickCounter, CallbackInfo ci) {
        if (SubtitleOverlay.isActive()) ci.cancel();
    }

    @Inject(method = "renderStatusBars", at = @At("HEAD"), cancellable = true)
    private void hideStatusBars(DrawContext context, CallbackInfo ci) {
        if (SubtitleOverlay.isActive()) ci.cancel();
    }

    @Inject(method = "renderCrosshair", at = @At("HEAD"), cancellable = true)
    private void hideCrosshair(DrawContext context, RenderTickCounter tickCounter, CallbackInfo ci) {
        if (SubtitleOverlay.isActive()) ci.cancel();
    }

    @Inject(method = "renderExperienceBar", at = @At("HEAD"), cancellable = true)
    private void hideExperienceBar(DrawContext context, int x, CallbackInfo ci) {
        if (SubtitleOverlay.isActive()) ci.cancel();
    }
}
