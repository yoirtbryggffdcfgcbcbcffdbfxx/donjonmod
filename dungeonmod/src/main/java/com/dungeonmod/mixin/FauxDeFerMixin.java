package com.dungeonmod.mixin;

import com.dungeonmod.util.FauxDeFerHelper;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PlayerEntity.class)
public class FauxDeFerMixin {

    @Inject(method = "attack", at = @At("HEAD"), cancellable = true)
    private void onAttack(Entity target, CallbackInfo ci) {
        PlayerEntity player = (PlayerEntity) (Object) this;
        if (player.getWorld().isClient()) return;

        var stack = player.getMainHandStack();
        if (!FauxDeFerHelper.isFauxDeFer(stack)) return;

        if (player.getAttackCooldownProgress(0.5f) < 1.0f) {
            ci.cancel();
            return;
        }

        FauxDeFerHelper.doFauxAreaDamage(player);
        player.swingHand(player.getActiveHand());
        player.resetLastAttackedTicks();
        ci.cancel();
    }
}
