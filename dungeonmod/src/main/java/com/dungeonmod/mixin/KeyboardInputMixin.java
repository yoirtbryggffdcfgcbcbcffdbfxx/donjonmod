package com.dungeonmod.mixin;

import com.dungeonmod.client.SubtitleOverlay;
import net.minecraft.client.input.KeyboardInput;
import net.minecraft.util.PlayerInput;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(KeyboardInput.class)
public class KeyboardInputMixin {

    @Inject(method = "tick", at = @At("TAIL"))
    private void disableMovementDuringDialogue(CallbackInfo ci) {
        if (SubtitleOverlay.isActive()) {
            KeyboardInput self = (KeyboardInput)(Object)this;
            self.movementForward = 0.0f;
            self.movementSideways = 0.0f;
            self.playerInput = new PlayerInput(
                false, false, false, false,
                false,
                self.playerInput.sneak(),
                false
            );
        }
    }
}
