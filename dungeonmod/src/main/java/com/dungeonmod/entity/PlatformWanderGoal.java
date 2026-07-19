package com.dungeonmod.entity;

import com.dungeonmod.DungeonMod;
import net.minecraft.entity.ai.goal.Goal;
import net.minecraft.entity.mob.PathAwareEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;

import java.util.EnumSet;
import java.util.function.Supplier;

public class PlatformWanderGoal extends Goal {

    private final PathAwareEntity mob;
    private final Supplier<BlockPos> platformSupplier;
    private final double speed;
    private final int radius;
    private BlockPos targetPos;
    private int logCounter = 0;
    private int stuckTicks = 0;
    private BlockPos lastTickPos;

    public PlatformWanderGoal(PathAwareEntity mob, Supplier<BlockPos> platformSupplier, double speed, int radius) {
        this.mob = mob;
        this.platformSupplier = platformSupplier;
        this.speed = speed;
        this.radius = radius;
        this.setControls(EnumSet.of(Control.MOVE));
    }

    @Override
    public boolean canStart() {
        BlockPos center = platformSupplier.get();
        if (center == null) {
            DungeonMod.LOGGER.info("[WanderGoal] {} canStart: center=null", mob.getUuid());
            return false;
        }

        // Ne pas démarrer si le gobelin n'est pas sur la plateforme (tombé)
        double hDist = Math.sqrt(Math.pow(mob.getX() - center.getX(), 2)
            + Math.pow(mob.getZ() - center.getZ(), 2));
        if (hDist > radius + 2 || mob.getY() < center.getY() - 1.5) {
            if (++logCounter % 20 == 0) DungeonMod.LOGGER.info("[WanderGoal] {} canStart: skip (hDist={} y={} platY={})", mob.getUuid(), String.format("%.1f", hDist), String.format("%.1f", mob.getY()), center.getY());
            return false;
        }

        this.targetPos = findValidTarget(center);
        DungeonMod.LOGGER.info("[WanderGoal] {} canStart: hDist={} target={} success={}", mob.getUuid(), String.format("%.2f", hDist), this.targetPos, this.targetPos != null);
        return this.targetPos != null;
    }

    @Override
    public void tick() {
        super.tick();
        if (this.targetPos == null) return;

        if (mob.squaredDistanceTo(Vec3d.ofCenter(this.targetPos)) < 2.0) {
            this.targetPos = null;
            this.stuckTicks = 0;
            return;
        }

        BlockPos currentBP = mob.getBlockPos();
        if (currentBP.equals(lastTickPos)) {
            stuckTicks++;
            if (stuckTicks > 60) {
                this.targetPos = null;
                this.stuckTicks = 0;
                lastTickPos = null;
                return;
            }
        } else {
            stuckTicks = 0;
            lastTickPos = currentBP;
        }

        if (!mob.getNavigation().isFollowingPath()) {
            this.mob.getNavigation().startMovingTo(this.targetPos.getX(), this.targetPos.getY(), this.targetPos.getZ(), this.speed);
        }
    }

    @Override
    public boolean shouldContinue() {
        BlockPos center = platformSupplier.get();
        if (center == null) return false;
        double hDist = Math.sqrt(Math.pow(mob.getX() - center.getX(), 2)
            + Math.pow(mob.getZ() - center.getZ(), 2));
        return this.targetPos != null
            && mob.squaredDistanceTo(Vec3d.ofCenter(this.targetPos)) > 1.0
            && hDist <= radius + 2
            && mob.getY() >= center.getY() - 1.5;
    }

    @Override
    public void start() {
        if (this.targetPos != null) {
            this.mob.getNavigation().startMovingTo(this.targetPos.getX(), this.targetPos.getY(), this.targetPos.getZ(), this.speed);
        }
    }

    @Override
    public void stop() {
        this.targetPos = null;
    }

    private BlockPos findValidTarget(BlockPos center) {
        for (int i = 0; i < 60; i++) {
            int dx = mob.getRandom().nextInt(radius * 2 + 1) - radius;
            int dz = mob.getRandom().nextInt(radius * 2 + 1) - radius;
            BlockPos pos = new BlockPos(
                center.getX() + dx,
                center.getY(),
                center.getZ() + dz
            );

            BlockPos groundPos = pos.down();
            var groundState = mob.getWorld().getBlockState(groundPos);
            if (!groundState.isAir()
                && mob.getWorld().getBlockState(pos).isAir()
                && mob.getWorld().getBlockState(pos.up()).isAir()
                && hasWalkableNeighbor(pos)) {
                return pos;
            }
        }
        return null;
    }

    private boolean hasWalkableNeighbor(BlockPos pos) {
        return !mob.getWorld().getBlockState(pos.north().down()).isAir()
            || !mob.getWorld().getBlockState(pos.south().down()).isAir()
            || !mob.getWorld().getBlockState(pos.east().down()).isAir()
            || !mob.getWorld().getBlockState(pos.west().down()).isAir();
    }

    public static boolean isNearEdge(PathAwareEntity mob) {
        Vec3d vel = mob.getVelocity();
        double hSpeed = Math.sqrt(vel.x * vel.x + vel.z * vel.z);
        if (hSpeed < 0.01) return false;
        Vec3d dir = new Vec3d(vel.x, 0, vel.z).normalize();
        BlockPos front = BlockPos.ofFloored(mob.getX() + dir.x * 2.5, mob.getY() - 0.5, mob.getZ() + dir.z * 2.5);
        return mob.getWorld().getBlockState(front).isAir();
    }
}
