package com.dungeonmod.mixin;

import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.mob.ZombieEntity;
import net.minecraft.sound.SoundEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ZombieEntity.class)
public class ZombieEntityMixin {

    @Inject(method = "getAmbientSound", at = @At("HEAD"), cancellable = true)
    private void cancelAmbientSound(CallbackInfoReturnable<SoundEvent> ci) {
        ci.setReturnValue(null);
    }

    @Inject(method = "getHurtSound", at = @At("HEAD"), cancellable = true)
    private void cancelHurtSound(DamageSource source, CallbackInfoReturnable<SoundEvent> ci) {
        ci.setReturnValue(null);
    }

    @Inject(method = "getDeathSound", at = @At("HEAD"), cancellable = true)
    private void cancelDeathSound(CallbackInfoReturnable<SoundEvent> ci) {
        ci.setReturnValue(null);
    }

    @Inject(method = "playStepSound", at = @At("HEAD"), cancellable = true)
    private void cancelStepSound(CallbackInfo ci) {
        ci.cancel();
    }
}
