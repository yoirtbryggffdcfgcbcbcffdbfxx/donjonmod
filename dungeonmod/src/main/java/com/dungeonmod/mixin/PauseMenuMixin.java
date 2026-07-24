package com.dungeonmod.mixin;

import com.dungeonmod.client.SubtitleOverlay;
import net.minecraft.client.MinecraftClient;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MinecraftClient.class)
public class PauseMenuMixin {

    @Inject(method = "openGameMenu", at = @At("HEAD"), cancellable = true)
    private void cancelPauseDuringDialogue(boolean pause, CallbackInfo ci) {
        if (SubtitleOverlay.isActive()) ci.cancel();
    }
}
