package com.dungeonmod.mixin;

import com.dungeonmod.util.FouetHelper;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.Vec3d;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PlayerEntity.class)
public class FouetMixin {

    @Inject(method = "attack", at = @At("HEAD"), cancellable = true)
    private void onAttack(Entity target, CallbackInfo ci) {
        PlayerEntity player = (PlayerEntity) (Object) this;
        if (player.getWorld().isClient()) return;
        if (!FouetHelper.isFouet(player.getMainHandStack())) return;
        if (!(target instanceof LivingEntity living)) return;

        if (player.getAttackCooldownProgress(0.5f) < 1.0f) {
            ci.cancel();
            return;
        }

        if (player.squaredDistanceTo(living) > 49.0) return;

        living.damage((ServerWorld) player.getWorld(), player.getDamageSources().playerAttack(player), 1.0f);

        Vec3d pull = player.getPos().subtract(living.getPos()).normalize();
        living.addVelocity(pull.x * 1.5, 0, pull.z * 1.5);
        living.velocityModified = true;

        if (!player.getWorld().isClient() && player.getWorld() instanceof ServerWorld sw) {
            sw.spawnParticles(ParticleTypes.GUST,
                living.getX(), living.getY() + living.getHeight() * 0.5, living.getZ(),
                3, 0.2, 0.2, 0.2, 0.02);
        }

        player.swingHand(player.getActiveHand());
        player.resetLastAttackedTicks();
        ci.cancel();
    }
}
