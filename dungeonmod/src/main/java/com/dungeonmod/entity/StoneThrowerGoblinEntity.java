package com.dungeonmod.entity;

import com.dungeonmod.DungeonMod;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.SpawnGroup;
import net.minecraft.entity.ai.goal.ActiveTargetGoal;
import net.minecraft.entity.ai.goal.Goal;
import net.minecraft.entity.ai.goal.LookAroundGoal;
import net.minecraft.entity.ai.goal.LookAtEntityGoal;
import net.minecraft.entity.ai.goal.RevengeGoal;
import net.minecraft.entity.mob.ZombieEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.state.property.Properties;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import net.minecraft.nbt.NbtCompound;
import java.util.EnumSet;

public class StoneThrowerGoblinEntity extends ZombieEntity {

    public static final EntityType<StoneThrowerGoblinEntity> THROWER_TYPE = Registry.register(
        Registries.ENTITY_TYPE,
        Identifier.of("dungeonmod", "stone_thrower_goblin"),
        EntityType.Builder.<StoneThrowerGoblinEntity>create(StoneThrowerGoblinEntity::new, SpawnGroup.MONSTER)
            .dimensions(0.6f, 1.95f)
            .maxTrackingRange(32)
            .trackingTickInterval(1)
            .build(RegistryKey.of(Registries.ENTITY_TYPE.getKey(), Identifier.of("dungeonmod", "stone_thrower_goblin")))
    );

    private BlockPos platformPos;
    private int throwCooldown = 0;
    private int ladderScanCooldown = 0;
    private static final int PLATFORM_RADIUS = 4;
    private static final int LADDER_SEARCH_RADIUS = 10;
    boolean climbingLadder = false;
    BlockPos ladderTarget;
    private BlockPos ladderBlockPos;

    public StoneThrowerGoblinEntity(EntityType<? extends StoneThrowerGoblinEntity> entityType, World world) {
        super(entityType, world);
    }

    @Override
    protected void initGoals() {
        this.goalSelector.add(1, new ReturnToPlatformGoal(this)); // Priorité max : retour plateforme
        this.goalSelector.add(2, new ThrowStoneGoal(this));
        this.goalSelector.add(3, new PlatformWanderGoal(this, () -> this.platformPos, 0.6, PLATFORM_RADIUS));
        this.goalSelector.add(6, new LookAtEntityGoal(this, PlayerEntity.class, 8.0f));
        this.goalSelector.add(7, new LookAroundGoal(this));
        this.targetSelector.add(1, new ActiveTargetGoal<>(this, PlayerEntity.class, true));
        this.targetSelector.add(2, new RevengeGoal(this));
    }

    public void setPlatformPos(BlockPos pos) {
        this.platformPos = pos;
    }

    @Override
    public void writeCustomDataToNbt(NbtCompound nbt) {
        super.writeCustomDataToNbt(nbt);
        if (platformPos != null) {
            nbt.putInt("platformX", platformPos.getX());
            nbt.putInt("platformY", platformPos.getY());
            nbt.putInt("platformZ", platformPos.getZ());
        }
    }

    @Override
    public void readCustomDataFromNbt(NbtCompound nbt) {
        super.readCustomDataFromNbt(nbt);
        if (nbt.contains("platformX")) {
            platformPos = new BlockPos(nbt.getInt("platformX"), nbt.getInt("platformY"), nbt.getInt("platformZ"));
        }
    }

    @Override
    public void tickMovement() {
        super.tickMovement();
        if (throwCooldown > 0) throwCooldown--;

        if (this.getTarget() != null && this.getTarget().isAlive()) {
            this.getLookControl().lookAt(this.getTarget(), 30.0f, 30.0f);
        }

        if (platformPos == null) {
            return;
        }

        double hDistSq = Math.pow(this.getX() - platformPos.getX(), 2) + Math.pow(this.getZ() - platformPos.getZ(), 2);
        boolean onPlatform = hDistSq <= 9.0 && this.getY() >= platformPos.getY() - 1.0;

        // Détection de bord de plateforme : demi-tour (cooldown 20 ticks)
        if (onPlatform && this.age % 20 == 0 && this.getNavigation().isFollowingPath() && PlatformWanderGoal.isNearEdge(this)) {
            Vec3d back = new Vec3d(platformPos.getX() - this.getX(), 0, platformPos.getZ() - this.getZ()).normalize();
            this.setVelocity(this.getVelocity().add(back.x * 0.3, 0.1, back.z * 0.3));
            this.velocityModified = true;
        }
    }

    void findAndGoToLadder() {
        if (ladderScanCooldown > 0 && climbingLadder && ladderTarget != null) {
            ladderScanCooldown--;
            if (!this.getNavigation().isFollowingPath()) {
                this.getNavigation().startMovingTo(ladderTarget.getX(), ladderTarget.getY(), ladderTarget.getZ(), 1.0);
            }
            return;
        }
        ladderScanCooldown = 20;

        BlockPos bestLadder = null;
        double bestDist = Double.MAX_VALUE;
        BlockPos bestTop = null;

        for (int dx = -LADDER_SEARCH_RADIUS; dx <= LADDER_SEARCH_RADIUS; dx++) {
            for (int dy = -5; dy <= 5; dy++) {
                for (int dz = -LADDER_SEARCH_RADIUS; dz <= LADDER_SEARCH_RADIUS; dz++) {
                    BlockPos pos = new BlockPos((int)this.getX() + dx, (int)this.getY() + dy, (int)this.getZ() + dz);
                    BlockState state = this.getWorld().getBlockState(pos);
                    if (state.isIn(BlockTags.CLIMBABLE)) {
                        BlockPos top = pos;
                        while (this.getWorld().getBlockState(top.up()).isIn(BlockTags.CLIMBABLE)) {
                            top = top.up();
                        }
                        if (top.getY() >= platformPos.getY() - 2) {
                            double dist = this.squaredDistanceTo(Vec3d.ofCenter(pos));
                            if (dist < bestDist) {
                                bestDist = dist;
                                bestLadder = pos;
                                bestTop = top;
                            }
                        }
                    }
                }
            }
        }

        if (bestLadder != null) {
            climbingLadder = true;
            ladderBlockPos = bestLadder;
            // Calculer la position pile devant la face grimpable
            BlockState ladderState = this.getWorld().getBlockState(bestLadder);
            Direction facing = ladderState.contains(Properties.HORIZONTAL_FACING)
                ? ladderState.get(Properties.HORIZONTAL_FACING) : Direction.NORTH;
            Direction standDir = facing.getOpposite();
            BlockPos standPos = new BlockPos(
                bestLadder.getX() + standDir.getOffsetX(),
                bestLadder.getY(),
                bestLadder.getZ() + standDir.getOffsetZ()
            );
            // Si le standPos est occupé, essayer l'autre côté
            if (!this.getWorld().getBlockState(standPos).isAir() || !this.getWorld().getBlockState(standPos.up()).isAir()) {
                BlockPos otherSide = new BlockPos(
                    bestLadder.getX() + facing.getOffsetX(),
                    bestLadder.getY(),
                    bestLadder.getZ() + facing.getOffsetZ()
                );
                if (this.getWorld().getBlockState(otherSide).isAir()) {
                    standPos = otherSide;
                }
            }
            ladderTarget = standPos;
            // Naviguer vers la position devant l'échelle
            this.getNavigation().startMovingTo(standPos.getX(), standPos.getY(), standPos.getZ(), 1.0);
        } else {
            // Pas d'échelle → retour à pied vers la plateforme (à Y du gobelin)
            this.getNavigation().startMovingTo(platformPos.getX(), this.getY(), platformPos.getZ(), 1.0);
        }
    }

    public boolean canThrow() {
        LivingEntity target = this.getTarget();
        return throwCooldown <= 0 && target != null && target.isAlive()
            && this.squaredDistanceTo(target) < 400.0
            && this.canSee(target);
    }

    public void resetThrowCooldown() {
        this.throwCooldown = 40;
    }

    // Goal prioritaire : retour à la plateforme par l'échelle
    static class ReturnToPlatformGoal extends Goal {
        private final StoneThrowerGoblinEntity goblin;

        ReturnToPlatformGoal(StoneThrowerGoblinEntity goblin) {
            this.goblin = goblin;
            this.setControls(EnumSet.of(Control.MOVE, Control.LOOK));
        }

        @Override
        public boolean canStart() {
            if (goblin.platformPos == null) {
                return false;
            }
            double hDistSq = Math.pow(goblin.getX() - goblin.platformPos.getX(), 2) + Math.pow(goblin.getZ() - goblin.platformPos.getZ(), 2);
            boolean onPlatform = hDistSq <= 9.0 && goblin.getY() >= goblin.platformPos.getY() - 1.0;
            boolean result = !onPlatform && goblin.isOnGround();
            return result;
        }

        @Override
        public void start() {
            goblin.setTarget(null);
            if (!goblin.climbingLadder) {
                goblin.findAndGoToLadder();
            }
        }

        @Override
        public void tick() {
            if (goblin.climbingLadder && goblin.ladderTarget != null) {
                // Regarder vers le haut de l'échelle (le bloc ladder, pas le standPos)
                if (goblin.ladderBlockPos != null) {
                    goblin.getLookControl().lookAt(
                        goblin.ladderBlockPos.getX() + 0.5,
                        goblin.ladderBlockPos.getY() + 2,
                        goblin.ladderBlockPos.getZ() + 0.5, 30, 30);
                }

                double hDistSq = Math.pow(goblin.getX() - goblin.platformPos.getX(), 2) + Math.pow(goblin.getZ() - goblin.platformPos.getZ(), 2);
                boolean onPlat = hDistSq <= 9.0 && goblin.getY() >= goblin.platformPos.getY() - 0.5 && goblin.isOnGround();
                if (onPlat) {
                    goblin.climbingLadder = false;
                    goblin.ladderTarget = null;
                    goblin.ladderBlockPos = null;
                    return;
                }

                // Vérifier si le gobelin est DANS un bloc grimpable (échelle)
                boolean touchingLadder = goblin.getWorld().getBlockState(goblin.getBlockPos()).isIn(BlockTags.CLIMBABLE);
                double hdSq = Math.pow(goblin.getX() - goblin.ladderTarget.getX() - 0.5, 2)
                    + Math.pow(goblin.getZ() - goblin.ladderTarget.getZ() - 0.5, 2);
                boolean nearLadder = touchingLadder || hdSq < 4.0;

                // Pousser vers le haut si près de l'échelle (continue jusqu'en haut)
                if (nearLadder && goblin.getY() < goblin.platformPos.getY() - 0.5) {
                    goblin.setVelocity(goblin.getVelocity().x, 0.2, goblin.getVelocity().z);
                    goblin.velocityModified = true;
                }

                // Pousser horizontalement vers la dalle solide la plus proche (pas les trappes)
                if (nearLadder && goblin.getY() >= goblin.platformPos.getY() - 2.0 && goblin.getY() < goblin.platformPos.getY() + 0.5) {
                    BlockPos bp = goblin.getBlockPos();
                    Vec3d bestDir = null;
                    double bestDist = Double.MAX_VALUE;
                    for (int dx = -1; dx <= 1; dx++) {
                        for (int dz = -1; dz <= 1; dz++) {
                            if (dx == 0 && dz == 0) continue;
                            BlockPos pos = new BlockPos(bp.getX() + dx, goblin.platformPos.getY() - 1, bp.getZ() + dz);
                            BlockState st = goblin.getWorld().getBlockState(pos);
                            if (!st.isAir() && !(st.getBlock() instanceof net.minecraft.block.TrapdoorBlock)) {
                                double d = goblin.squaredDistanceTo(Vec3d.ofCenter(pos));
                                if (d < bestDist) {
                                    bestDist = d;
                                    bestDir = new Vec3d(pos.getX() + 0.5 - goblin.getX(), 0, pos.getZ() + 0.5 - goblin.getZ()).normalize();
                                }
                            }
                        }
                    }
                    if (bestDir != null) {
                        goblin.addVelocity(bestDir.x * 0.15, 0, bestDir.z * 0.15);
                    } else {
                        // Fallback vers le centre de la plateforme
                        Vec3d toward = new Vec3d(
                            goblin.platformPos.getX() - goblin.getX(), 0,
                            goblin.platformPos.getZ() - goblin.getZ()).normalize();
                        goblin.addVelocity(toward.x * 0.1, 0, toward.z * 0.1);
                    }
                }

                // Re-lancer la navigation vers le standPos si perdue
                if (!goblin.getNavigation().isFollowingPath()) {
                    int targetY = touchingLadder ? goblin.platformPos.getY()
                        : Math.max(goblin.ladderTarget.getY(), goblin.getBlockY());
                    goblin.getNavigation().startMovingTo(
                        goblin.ladderTarget.getX(), targetY, goblin.ladderTarget.getZ(), 1.0);
                }
                // Une fois arrivé au standPos et à l'arrêt → pas vers l'échelle
                if (!touchingLadder && goblin.ladderBlockPos != null
                    && !goblin.getNavigation().isFollowingPath()
                    && goblin.squaredDistanceTo(Vec3d.ofCenter(goblin.ladderTarget)) < 1.5
                    && goblin.getY() < goblin.platformPos.getY() - 2.0) {
                    Vec3d towardLadder = new Vec3d(
                        goblin.ladderBlockPos.getX() + 0.5 - goblin.getX(), 0,
                        goblin.ladderBlockPos.getZ() + 0.5 - goblin.getZ()).normalize();
                    goblin.setVelocity(towardLadder.x * 0.15, 0, towardLadder.z * 0.15);
                    goblin.velocityModified = true;
                }
                // Micro-poussée vers le standPos (pour arriver exactement au centre)
                if (!touchingLadder && goblin.getY() < goblin.platformPos.getY() - 2.0) {
                    double d = goblin.squaredDistanceTo(Vec3d.ofCenter(
                        new BlockPos(goblin.ladderTarget.getX(), goblin.getBlockY(), goblin.ladderTarget.getZ())));
                    if (d > 0.5 && d < 4.0) {
                        goblin.addVelocity(
                            (goblin.ladderTarget.getX() + 0.5 - goblin.getX()) * 0.05,
                            0,
                            (goblin.ladderTarget.getZ() + 0.5 - goblin.getZ()) * 0.05);
                    }
                }
            } else if (!goblin.getNavigation().isFollowingPath() && goblin.platformPos != null) {
                goblin.findAndGoToLadder();
            }
        }

        @Override
        public boolean shouldContinue() {
            if (goblin.platformPos == null) return false;
            double hDistSq = Math.pow(goblin.getX() - goblin.platformPos.getX(), 2) + Math.pow(goblin.getZ() - goblin.platformPos.getZ(), 2);
            boolean onPlatform = hDistSq <= 9.0 && goblin.getY() >= goblin.platformPos.getY() - 1.0 && goblin.isOnGround();
            return !onPlatform;
        }

        @Override
        public void stop() {
            goblin.climbingLadder = false;
            goblin.ladderTarget = null;
        }
    }

    static class ThrowStoneGoal extends Goal {
        private final StoneThrowerGoblinEntity goblin;
        ThrowStoneGoal(StoneThrowerGoblinEntity goblin) {
            this.goblin = goblin;
        }

        @Override
        public boolean canStart() {
            return goblin.canThrow();
        }

        @Override
        public void tick() {
            LivingEntity target = goblin.getTarget();
            if (target == null) return;

            Vec3d dir = target.getPos().add(0, target.getStandingEyeHeight() * 0.5, 0)
                .subtract(goblin.getPos().add(0, goblin.getStandingEyeHeight(), 0)).normalize();

            StoneEntity stone = new StoneEntity(StoneEntity.STONE_TYPE, goblin.getWorld());
            stone.setOwner(goblin);
            Vec3d look = goblin.getRotationVec(1.0f);
            Vec3d armPos = goblin.getPos().add(look.x * 0.4, goblin.getStandingEyeHeight() * 0.6, look.z * 0.4);
            stone.setPosition(armPos.x, armPos.y, armPos.z);
            stone.setVelocity(dir.x, dir.y + 0.1, dir.z);
            goblin.getWorld().spawnEntity(stone);

            goblin.resetThrowCooldown();
            goblin.swingHand(goblin.getActiveHand());
        }
    }
}
