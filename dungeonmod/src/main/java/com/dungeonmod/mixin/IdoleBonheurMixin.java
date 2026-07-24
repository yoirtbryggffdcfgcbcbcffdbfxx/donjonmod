package com.dungeonmod.mixin;

import net.minecraft.component.DataComponentTypes;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.Vec3d;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ServerPlayerEntity.class)
public class IdoleBonheurMixin {

    @Inject(method = "damage", at = @At("HEAD"), cancellable = true)
    private void onDamage(ServerWorld world, DamageSource source, float amount, CallbackInfoReturnable<Boolean> cir) {
        ServerPlayerEntity player = (ServerPlayerEntity)(Object)this;
        if (!hasIdole(player)) return;

        if (source.getAttacker() instanceof LivingEntity attacker) {
            if (world.getRandom().nextFloat() < 0.25f) {
                // Son
                world.playSound(null, player.getX(), player.getY(), player.getZ(),
                    SoundEvents.BLOCK_RESPAWN_ANCHOR_CHARGE, SoundCategory.PLAYERS, 0.6f, 1.2f);

                // Particules bleues autour du joueur
                world.spawnParticles(ParticleTypes.SCULK_SOUL,
                    player.getX(), player.getY() + 0.5, player.getZ(),
                    8, 1.0, 1.0, 1.0, 0.3);

                // Knockback joueur (recule)
                Vec3d awayFromAttacker = player.getPos().subtract(attacker.getPos()).normalize().multiply(0.15);
                player.addVelocity(awayFromAttacker.x, 0.15, awayFromAttacker.z);
                player.velocityModified = true;

                // Knockback tous les ennemis dans un rayon de 3 blocs
                for (var entity : world.getOtherEntities(player, player.getBoundingBox().expand(3.0))) {
                    if (entity instanceof LivingEntity living && living.isAlive()) {
                        Vec3d away = living.getPos().subtract(player.getPos()).normalize().multiply(0.15);
                        living.addVelocity(away.x, 0.15, away.z);
                        living.velocityModified = true;
                    }
                }

                cir.setReturnValue(false);
            }
        }
    }

    private static boolean hasIdole(PlayerEntity player) {
        return isIdole(player.getMainHandStack()) || isIdole(player.getOffHandStack());
    }

    private static boolean isIdole(ItemStack stack) {
        return !stack.isEmpty() && stack.isOf(Items.ECHO_SHARD)
            && stack.contains(DataComponentTypes.CUSTOM_NAME)
            && stack.get(DataComponentTypes.CUSTOM_NAME).getString().contains("Idole du bonheur");
    }
}
