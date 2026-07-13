package com.dungeonmod.mixin;

import com.dungeonmod.util.GlaiveComboData;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.Vec3d;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PlayerEntity.class)
public class GlaiveAttackMixin {

    @Inject(method = "attack", at = @At("HEAD"), cancellable = true)
    private void onAttack(Entity target, CallbackInfo ci) {
        PlayerEntity player = (PlayerEntity)(Object)this;
        if (player.getWorld().isClient()) return;
        var stack = player.getMainHandStack();
        if (!GlaiveComboData.isGlaive(stack)) return;

        // On remplace l'attaque vanilla par notre attaque perforante
        ci.cancel();
        player.swingHand(net.minecraft.util.Hand.MAIN_HAND);

        if (GlaiveComboData.isOnCooldown(player)) return;

        int stage = GlaiveComboData.getComboStage(player);
        float dmg = (float) player.getAttributeValue(net.minecraft.entity.attribute.EntityAttributes.ATTACK_DAMAGE);
        float kb = 0.05f + stage * 0.033f;
        float pitch = 0.8f + stage * 0.2f;

        Vec3d eye = player.getEyePos();
        Vec3d look = player.getRotationVec(1.0f);
        double range = 6.0;
        boolean hitAny = false;

        for (Entity e : player.getWorld().getOtherEntities(player, player.getBoundingBox().expand(range))) {
            if (!(e instanceof LivingEntity living) || !living.isAlive()) continue;
            if (e instanceof PlayerEntity) continue;

            Vec3d toEntity = e.getPos().add(0, e.getHeight() * 0.5, 0).subtract(eye);
            double dist = toEntity.length();
            if (dist > range) continue;

            // Vérifier si l'entité est assez proche de la ligne de visée (perçage)
            Vec3d projection = look.multiply(toEntity.dotProduct(look));
            Vec3d perpendicular = toEntity.subtract(projection);
            if (perpendicular.length() > 0.8) continue;

            if (player.getWorld() instanceof ServerWorld sw) {
                living.damage(sw, player.getDamageSources().playerAttack(player), dmg);
                living.takeKnockback(kb, player.getX() - living.getX(), player.getZ() - living.getZ());
                sw.playSound(null, living.getX(), living.getY(), living.getZ(),
                    SoundEvents.ENTITY_PLAYER_ATTACK_SWEEP,
                    SoundCategory.PLAYERS, 1.0f, pitch);
            }
            hitAny = true;
        }

        if (hitAny) {
            GlaiveComboData.applyNextStage(player);
        }
    }
}
