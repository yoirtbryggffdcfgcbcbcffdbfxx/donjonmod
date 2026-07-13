package com.dungeonmod.util;

import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.Vec3d;
import java.util.*;

public class SabreComboData {
    private static final Map<UUID, Integer> comboMap = new HashMap<>();
    public static final Map<UUID, dev.kosmx.playerAnim.api.layered.ModifierLayer<dev.kosmx.playerAnim.api.layered.IAnimation>> animLayers = new HashMap<>();
    public static boolean animRegistered = false;

    public static int getCombo(PlayerEntity player) {
        return comboMap.getOrDefault(player.getUuid(), 0);
    }

    public static void addCombo(PlayerEntity player) {
        comboMap.put(player.getUuid(), Math.min(getCombo(player) + 1, 3));
    }

    public static void resetCombo(PlayerEntity player) {
        comboMap.put(player.getUuid(), 0);
    }

    /** @return true si le combo a touché un ennemi */
    public static boolean unleashCombo(PlayerEntity player) {
        int combo = getCombo(player);
        if (combo <= 0) return false;
        if (!(player.getWorld() instanceof ServerWorld sw)) return false;
        Entity target = findTarget(player);
        if (!(target instanceof LivingEntity living) || !living.isAlive()) return false;
        resetCombo(player);
        float dmg = combo * 4.0f;
        living.damage(sw, player.getDamageSources().playerAttack(player), dmg);
        double dx = player.getX() - living.getX();
        double dz = player.getZ() - living.getZ();
        living.takeKnockback(combo == 1 ? 0.1 : combo == 2 ? 0.2 : 0.5, dx, dz);
        return true;
    }

    public static void applyComboOnHit(PlayerEntity player, Entity target) {
        // Combo is only consumed on right-click unleash, not on hit
        // This allows players to build combo via air swings while still hitting enemies
    }

    private static Entity findTarget(PlayerEntity player) {
        double range = 4.0;
        var eye = player.getEyePos();
        var look = player.getRotationVec(1.0f).normalize();
        Entity closest = null;
        double closestDist = range + 1;
        for (Entity e : player.getWorld().getOtherEntities(player,
                player.getBoundingBox().expand(range))) {
            if (!(e instanceof LivingEntity)) continue;
            var toEntity = e.getPos().add(0, e.getHeight() * 0.5, 0).subtract(eye);
            double dist = toEntity.length();
            if (dist > range || dist > closestDist) continue;
            if (toEntity.normalize().dotProduct(look) < 0.9) continue;
            closest = e;
            closestDist = dist;
        }
        return closest;
    }
}
