package com.dungeonmod.mixin;

import com.dungeonmod.DungeonMod;
import net.minecraft.entity.player.PlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PlayerEntity.class)
public class PlayerAttackMixin {

    @Inject(method = "attack", at = @At("HEAD"), cancellable = true)
    private void cancelAttackIfProne(net.minecraft.entity.Entity target, CallbackInfo ci) {
        PlayerEntity player = (PlayerEntity) (Object) this;
        if (DungeonMod.isHunterProne(player)) {
            ci.cancel();
        }
    }
}
