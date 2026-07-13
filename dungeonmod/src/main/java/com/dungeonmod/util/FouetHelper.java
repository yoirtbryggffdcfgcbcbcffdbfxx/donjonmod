package com.dungeonmod.util;

import net.minecraft.component.DataComponentTypes;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;

import java.util.*;

public class FouetHelper {

    public static boolean isFouet(ItemStack stack) {
        if (stack.isEmpty() || !stack.isOf(Items.STICK)) return false;
        if (!stack.contains(DataComponentTypes.CUSTOM_NAME)) return false;
        return stack.get(DataComponentTypes.CUSTOM_NAME).getString().contains("Fouet");
    }

    public static void doWhipAttack(PlayerEntity player, int chargeTicks) {
        var world = player.getWorld();
        if (world.isClient()) return;

        float progress = Math.min(chargeTicks / 40.0f, 1.0f);
        double range = 3.0 + progress * 4.0;

        Vec3d playerPos = player.getPos();
        Vec3d lookDir = player.getRotationVec(1.0f).normalize();
        Vec3d startPos = playerPos.add(0, player.getStandingEyeHeight() + 0.5, 0);
        Vec3d right = lookDir.crossProduct(new Vec3d(0, 1, 0)).normalize();
        Vec3d whipOrigin = startPos.add(right.multiply(0.4));

        Vec3d endPos = playerPos.add(lookDir.multiply(range));

        Box box = new Box(
            playerPos.x - range, playerPos.y - range, playerPos.z - range,
            playerPos.x + range, playerPos.y + range, playerPos.z + range
        );

        List<Entity> entities = world.getOtherEntities(player, box, entity -> {
            if (!(entity instanceof LivingEntity living) || !living.isAlive()) return false;
            Vec3d toEntity = entity.getPos().subtract(playerPos);
            double dist = toEntity.length();
            if (dist > range || dist < 0.1) return false;
            return toEntity.normalize().dotProduct(lookDir) > 0.707;
        });

        if (world instanceof ServerWorld sw) {
            List<LivingEntity> hitEntities = new ArrayList<>();
            for (Entity entity : entities) {
                if (entity instanceof LivingEntity living) {
                float dmg = progress * 1.0f;
                if (dmg > 0) {
                    living.damage(sw, player.getDamageSources().playerAttack(player), dmg);
                }
                int slownessTicks = (int)(progress * 40);
living.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS, slownessTicks, 254, false, false, true));
                hitEntities.add(living);
                if (progress > 0.95f) {
                    com.dungeonmod.DungeonMod.stunnedEntities.add(living.getUuid());
                }
                }
            }

            if (hitEntities.isEmpty()) {
                // nothing
            } else {
                sw.playSound(null, player.getX(), player.getY(), player.getZ(),
                    SoundEvents.ENTITY_GENERIC_BURN, SoundCategory.PLAYERS, 0.5f, 1.5f);
            }

            // Always spawn wind trail toward aim direction
            {
                Vec3d toTarget = endPos.subtract(whipOrigin);
                double dist = toTarget.length();
                if (dist > 0.1) {
                    double density = (0.05 + progress * 0.9) * 0.8;
                    int steps = Math.max(0, (int)(dist * density));
                    for (int i = 0; i <= steps; i++) {
                        float t = steps == 0 ? 0 : i / (float)steps;
                        Vec3d p = whipOrigin.add(toTarget.multiply(t));
                        sw.spawnParticles(ParticleTypes.GUST,
                            p.x, p.y + 0.2, p.z,
                            1, 0.1, 0.1, 0.1, 0.01);
                    }
                }
            }

            // Sweep attack crescent at the center of effect
            if (progress > 0.3f) {
                float crescentScale = -2.0f - progress * 2.0f;
                sw.spawnParticles(ParticleTypes.SWEEP_ATTACK,
                    endPos.x, endPos.y + 0.3, endPos.z,
                    0, crescentScale, 0, 0, 1);
            }
        }
    }
}
