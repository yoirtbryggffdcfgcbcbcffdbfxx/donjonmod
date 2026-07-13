package com.dungeonmod.mixin;

import com.dungeonmod.util.GlaiveComboData;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.*;

@Mixin(Item.class)
public class GlaiveMixin {

    private static final Map<UUID, Set<Integer>> spinHitEntities = new HashMap<>();

    @Inject(method = "getMaxUseTime", at = @At("HEAD"), cancellable = true)
    private void onGetMaxUseTime(ItemStack stack, LivingEntity entity, CallbackInfoReturnable<Integer> cir) {
        if (GlaiveComboData.isGlaive(stack)) cir.setReturnValue(72000);
    }

    @Inject(method = "use", at = @At("HEAD"), cancellable = true)
    private void onUse(World world, PlayerEntity user, Hand hand, CallbackInfoReturnable<ActionResult> cir) {
        ItemStack stack = user.getStackInHand(hand);
        if (!GlaiveComboData.isGlaive(stack)) return;
        if (world.isClient()) return;
        user.setCurrentHand(hand);
        spinHitEntities.put(user.getUuid(), new HashSet<>());
        cir.setReturnValue(ActionResult.SUCCESS);
    }

    @Inject(method = "usageTick", at = @At("HEAD"))
    private void onUsageTick(World world, LivingEntity user, ItemStack stack, int remainingUseTicks, CallbackInfo ci) {
        if (!(user instanceof ServerPlayerEntity player)) return;
        if (!GlaiveComboData.isGlaive(stack)) return;
        // Attaque tournoyante : dégâts dans un cône directionnel (comme le curseur)
        var hitIds = spinHitEntities.get(player.getUuid());
        if (hitIds == null) return;
        Vec3d eye = player.getEyePos();
        Vec3d look = player.getRotationVec(1.0f);
        for (Entity e : player.getWorld().getOtherEntities(player, player.getBoundingBox().expand(3))) {
            if (!(e instanceof LivingEntity living) || !living.isAlive()) continue;
            if (hitIds.contains(e.getId())) continue;
            if (e.distanceTo(player) > 3.0) continue;
            if (e instanceof net.minecraft.entity.player.PlayerEntity) continue;
            // Vérifier si l'entité est dans un cône de 30° devant le joueur
            Vec3d toEntity = e.getPos().add(0, e.getHeight() * 0.5, 0).subtract(eye).normalize();
            if (toEntity.dotProduct(look) < 0.866) continue; // cos(30°) = 0.866
            if (player.getWorld() instanceof ServerWorld sw) {
                living.damage(sw, player.getDamageSources().playerAttack(player), 1.0f);
                living.takeKnockback(0.1, player.getX() - living.getX(), player.getZ() - living.getZ());
                sw.playSound(null, living.getX(), living.getY(), living.getZ(),
                    net.minecraft.sound.SoundEvents.ENTITY_PLAYER_ATTACK_SWEEP,
                    net.minecraft.sound.SoundCategory.PLAYERS, 1.0f, 1.0f);
            }
            hitIds.add(e.getId());
        }
        // Particules autour du joueur (fixes par rapport à son orientation)
        if (player.getWorld() instanceof ServerWorld sw) {
            int useTime = player.getItemUseTime();
            Vec3d right = look.rotateY((float) Math.PI / 2);
            double radius = 1.8;
            // Positions relatives : devant, derrière, gauche, droite
            Vec3d[] offsets = {
                look.multiply(radius),
                look.multiply(-radius),
                right.multiply(radius),
                right.multiply(-radius)
            };
            // Alterne aléatoirement entre 2 positions par tick
            for (int i = 0; i < 2; i++) {
                int idx = player.getRandom().nextInt(offsets.length);
                Vec3d pos = offsets[idx];
                double px = player.getX() + pos.x;
                double pz = player.getZ() + pos.z;
                sw.spawnParticles(ParticleTypes.SWEEP_ATTACK,
                    px, player.getY() + 0.5 + player.getRandom().nextDouble() * 1.0, pz,
                    1, 0, 0, 0, 0);
            }

            // Son de vent aléatoire tous les 3 ticks
            if (useTime % 3 == 0) {
                float pitch = 0.3f + player.getRandom().nextFloat() * 0.6f;
                sw.playSound(null, player.getX(), player.getY(), player.getZ(),
                    SoundEvents.ENTITY_PLAYER_ATTACK_SWEEP,
                    SoundCategory.PLAYERS, 0.4f, pitch);
            }
        }
        // Reset tout les 20 ticks (1 tour)
        if (player.getItemUseTime() % 20 == 0) hitIds.clear();
    }

    @Inject(method = "onStoppedUsing", at = @At("HEAD"))
    private void onStoppedUsing(ItemStack stack, World world, LivingEntity user, int remainingUseTicks, CallbackInfoReturnable<Boolean> cir) {
        if (!(user instanceof ServerPlayerEntity player)) return;
        spinHitEntities.remove(player.getUuid());
    }
}
