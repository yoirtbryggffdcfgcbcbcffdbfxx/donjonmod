package com.dungeonmod.mixin;

import net.minecraft.client.render.entity.model.AbstractZombieModel;
import net.minecraft.client.render.entity.state.ZombieEntityRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(AbstractZombieModel.class)
public class AbstractZombieModelMixin {

    @Inject(method = "setAngles(Lnet/minecraft/client/render/entity/state/ZombieEntityRenderState;)V", at = @At("TAIL"))
    private void onSetAngles(ZombieEntityRenderState state, CallbackInfo ci) {
        var self = (AbstractZombieModel)(Object)this;
        // Reset arm pitch to neutral (not forward)
        self.rightArm.pitch = 0.0f;
        self.leftArm.pitch = 0.0f;
        // Reset arm roll to neutral
        self.rightArm.roll = 0.0f;
        self.leftArm.roll = 0.0f;
    }
}
