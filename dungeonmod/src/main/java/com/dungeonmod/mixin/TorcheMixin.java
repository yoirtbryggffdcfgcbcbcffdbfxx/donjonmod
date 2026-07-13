package com.dungeonmod.mixin;

import com.dungeonmod.util.BeerStrengthData;
import com.dungeonmod.util.TorcheHelper;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PlayerEntity.class)
public class TorcheMixin {

    @Inject(method = "attack", at = @At("HEAD"), cancellable = true)
    private void onAttack(Entity target, CallbackInfo ci) {
        PlayerEntity player = (PlayerEntity) (Object) this;
        if (player.getWorld().isClient()) return;
        var stack = player.getMainHandStack();
        if (!TorcheHelper.isTorche(stack)) return;
        if (!(target instanceof LivingEntity living)) return;

        if (player.getAttackCooldownProgress(0.5f) < 1.0f) {
            ci.cancel();
            return;
        }

        if (player.squaredDistanceTo(living) > 9.0) return;

        float mult = BeerStrengthData.getMultiplier(player);
        int fireTime = Math.max(1, Math.round(3.0f * mult));
        living.setOnFireFor(fireTime);
        double dx = player.getX() - living.getX();
        double dz = player.getZ() - living.getZ();
        living.takeKnockback(0.2, dx, dz);

        player.swingHand(player.getActiveHand());
        player.resetLastAttackedTicks();
        ci.cancel();
    }
}
