package com.dungeonmod.util;

import com.dungeonmod.DungeonMod;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class AncreGrappling {

    private static final Map<UUID, Vec3d> ANCRE_HOOKS = new ConcurrentHashMap<>();
    private static final Map<UUID, Boolean> ANCRE_FIRST_PULL = new ConcurrentHashMap<>();
    /** Cooldown d'attaque du joueur en grappin (anti-spam, 0.5s = 10 ticks). */
    private static final Map<UUID, Long> ANCRE_ATTACK_COOLDOWN = new ConcurrentHashMap<>();
    /** Entites traversees : UUID → expiration (worldTime + 10). Le joueur est
     *  immunise contre leurs attaques pendant 0.5 seconde. */
    private static final Map<UUID, Long> ANCRE_IMMUNIZED_ENTITIES = new ConcurrentHashMap<>();

    /** Le joueur est-il immunise contre les degats de cette entite (grappin) ? */
    public static boolean isImmuneTo(Entity attacker) {
        if (attacker == null) return false;
        Long exp = ANCRE_IMMUNIZED_ENTITIES.get(attacker.getUuid());
        if (exp == null) return false;
        if (attacker.getWorld().getTime() >= exp) {
            ANCRE_IMMUNIZED_ENTITIES.remove(attacker.getUuid());
            return false;
        }
        return true;
    }

    public static boolean isAncre(ItemStack stack) {
        return !stack.isEmpty() && stack.isOf(Items.CONDUIT)
            && stack.contains(DataComponentTypes.CUSTOM_NAME)
            && stack.get(DataComponentTypes.CUSTOM_NAME).getString().contains("Ancre");
    }

    public static boolean isAncreGrappling(PlayerEntity player) {
        return isAncre(player.getMainHandStack()) || isAncre(player.getOffHandStack());
    }

    public static boolean onAttackBlock(PlayerEntity player, World world, Hand hand, BlockPos pos) {
        if (world.isClient()) return false;
        ItemStack held = hand == Hand.MAIN_HAND ? player.getMainHandStack() : player.getOffHandStack();
        if (!isAncre(held)) return false;

        double maxDistance = 17.0;
        HitResult hitResult = player.raycast(maxDistance, 0.0f, false);
        if (hitResult.getType() == HitResult.Type.BLOCK) {
            Vec3d hookPos = ((BlockHitResult) hitResult).getPos();
            ANCRE_HOOKS.put(player.getUuid(), hookPos);
            ANCRE_FIRST_PULL.put(player.getUuid(), true);
            world.playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.ENTITY_WIND_CHARGE_THROW, SoundCategory.PLAYERS, 1.0f, 1.4f);
            return true;
        }
        return false;
    }

    public static void tickHooks(ServerWorld world) {
        for (var it = ANCRE_HOOKS.entrySet().iterator(); it.hasNext();) {
            var entry = it.next();
            UUID uuid = entry.getKey();
            Vec3d target = entry.getValue();

            PlayerEntity player = world.getPlayerByUuid(uuid);
            if (player == null || !player.isAlive()) {
                DungeonMod.LOGGER.info("[AncreGrappling] tickHooks: player dead/null, removing");
                it.remove();
                ANCRE_FIRST_PULL.remove(uuid);
                continue;
            }
            if (!isAncreGrappling(player)) {
                DungeonMod.LOGGER.info("[AncreGrappling] tickHooks: player lost ancre, removing hook");
                it.remove();
                ANCRE_FIRST_PULL.remove(uuid);
                continue;
            }

            Vec3d playerPos = player.getEyePos();
            Vec3d dir = target.subtract(playerPos);
            double distance = dir.length();

            if (distance < 1.5) {
                DungeonMod.LOGGER.info("[AncreGrappling] tickHooks: arrived at target, removing");
                it.remove();
                ANCRE_FIRST_PULL.remove(uuid);
                continue;
            }

            // Direct velocity toward target, speed based on distance
            double speed = Math.min(distance * 0.6, 3.0);
            if (ANCRE_FIRST_PULL.remove(uuid) != null) speed = Math.max(speed, 2.0);
            Vec3d newVel = dir.normalize().multiply(speed);
            player.setVelocity(newVel);
            player.velocityModified = true;
            player.fallDistance = 0;

            // Traversee d'entite : 0.5 coeur de degats, le joueur devient immunise
            // contre cette entite pendant 0.5s (10 ticks).
            long worldTime = world.getTime();
            Long cd = ANCRE_ATTACK_COOLDOWN.get(uuid);
            if (cd == null || worldTime >= cd) {
                boolean hit = false;
                for (Entity e : world.getOtherEntities(player, player.getBoundingBox().expand(0.8),
                        ent -> ent instanceof LivingEntity living && living.isAlive())) {
                    e.damage(world, player.getDamageSources().playerAttack(player), 1.0f);
                    ANCRE_IMMUNIZED_ENTITIES.put(e.getUuid(), worldTime + 10);
                    hit = true;
                }
                if (hit) {
                    ANCRE_ATTACK_COOLDOWN.put(uuid, worldTime + 10);
                }
            }

            Vec3d start = player.getPos().add(0, 1.0, 0);
            double dist = start.distanceTo(target);
            int count = Math.min((int)(dist * 0.625), 10);
            for (int i = 0; i <= count; i++) {
                Vec3d point = start.lerp(target, (double)i / count);
                world.spawnParticles(ParticleTypes.SMALL_GUST, point.x, point.y, point.z, 1, 0, 0, 0, 0);
            }
        }
    }

    public static void onAttackEntity(PlayerEntity player, Entity target) {
        if (player.getWorld().isClient()) return;
        if (!isAncreGrappling(player)) return;
        if (!(target instanceof LivingEntity)) return;

        Vec3d hitPos = target.getPos().add(0, target.getHeight() * 0.5, 0);
        if (player.squaredDistanceTo(hitPos) > 289.0) return;

        ANCRE_HOOKS.put(player.getUuid(), hitPos);
        player.getWorld().playSound(null, player.getX(), player.getY(), player.getZ(),
            SoundEvents.ENTITY_WIND_CHARGE_THROW, SoundCategory.PLAYERS, 1.0f, 1.4f);
    }

    public static boolean onSwing(ServerPlayerEntity player) {
        if (player.getWorld().isClient()) return false;
        if (!isAncreGrappling(player)) return false;

        double maxDistance = 17.0;
        HitResult hitResult = player.raycast(maxDistance, 0.0f, false);

        if (hitResult.getType() == HitResult.Type.BLOCK) {
            Vec3d hookPos = ((BlockHitResult) hitResult).getPos();
            ANCRE_HOOKS.put(player.getUuid(), hookPos);
            ANCRE_FIRST_PULL.put(player.getUuid(), true);
            player.getWorld().playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.ENTITY_WIND_CHARGE_THROW, SoundCategory.PLAYERS, 1.0f, 1.4f);
            return true;
        }

        if (hitResult.getType() == HitResult.Type.ENTITY) {
            Entity target = ((net.minecraft.util.hit.EntityHitResult) hitResult).getEntity();
            if (target instanceof LivingEntity) {
                Vec3d hitPos = target.getPos().add(0, target.getHeight() * 0.5, 0);
                if (player.squaredDistanceTo(hitPos) <= 289.0) {
                    ANCRE_HOOKS.put(player.getUuid(), hitPos);
                    ANCRE_FIRST_PULL.put(player.getUuid(), true);
                    player.getWorld().playSound(null, player.getX(), player.getY(), player.getZ(),
                        SoundEvents.ENTITY_WIND_CHARGE_THROW, SoundCategory.PLAYERS, 1.0f, 1.4f);
                    return true;
                }
            }
        }
        return false;
    }
}
