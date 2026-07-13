package com.dungeonmod.util;

import net.minecraft.component.DataComponentTypes;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;

import java.util.List;

public class FauxDeFerHelper {

    public static boolean isFauxDeFer(ItemStack stack) {
        if (stack.isEmpty() || !stack.isOf(Items.IRON_HOE)) return false;
        if (!stack.contains(DataComponentTypes.CUSTOM_NAME)) return false;
        return stack.get(DataComponentTypes.CUSTOM_NAME).getString().contains("Faux de fer");
    }

    public static void doFauxAreaDamage(PlayerEntity player) {
        var world = player.getWorld();
        if (world.isClient()) return;

        if (player.getAttackCooldownProgress(0.5f) < 1.0f) return;

        Vec3d playerPos = player.getPos();
        Vec3d lookDir = player.getRotationVec(1.0f).normalize();

        double range = 3.5;
        Box box = new Box(
            playerPos.x - range, playerPos.y - 2.0, playerPos.z - range,
            playerPos.x + range, playerPos.y + 2.0, playerPos.z + range
        );

        List<Entity> entities = world.getOtherEntities(player, box, entity -> {
            if (!(entity instanceof LivingEntity living) || !living.isAlive()) return false;
            Vec3d toEntity = entity.getPos().subtract(playerPos);
            double dist = toEntity.length();
            if (dist > range || dist < 0.1) return false;
            return toEntity.normalize().dotProduct(lookDir) > 0.707;
        });

        if (world instanceof ServerWorld sw) {
            if (!entities.isEmpty()) {
                Entity first = entities.get(0);
                sw.spawnParticles(ParticleTypes.SWEEP_ATTACK,
                    first.getX(), first.getY() + first.getHeight() * 0.5, first.getZ(),
                    0, -6.0, 0, 0, 1);
            } else {
                Vec3d centerPos = playerPos.add(lookDir.multiply(range + 1.0));
                sw.spawnParticles(ParticleTypes.SWEEP_ATTACK,
                    centerPos.x, centerPos.y + 0.5, centerPos.z,
                    0, -6.0, 0, 0, 1);
            }

            for (Entity entity : entities) {
                if (!(entity instanceof LivingEntity living)) continue;
                living.damage(sw, player.getDamageSources().playerAttack(player), 3.0f);
                double dx = player.getX() - living.getX();
                double dz = player.getZ() - living.getZ();
                living.takeKnockback(0.2, dx, dz);
            }
        }
    }
}
