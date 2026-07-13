package com.dungeonmod.mixin;

import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.server.world.ServerWorld;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LivingEntity.class)
public class NoXpMixin {

    @Inject(method = "getExperienceToDrop", at = @At("HEAD"), cancellable = true)
    private void getExperienceToDrop(ServerWorld world, Entity killer, CallbackInfoReturnable<Integer> cir) {
        cir.setReturnValue(0);
    }
}
