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
    private static final int PLATFORM_RADIUS = 4;

    /** Detection unifiee : le gobelin est-il sur sa plateforme ? */
    public boolean isOnPlatform() {
        if (platformPos == null) return false;
        double hDistSq = Math.pow(this.getX() - platformPos.getX(), 2) + Math.pow(this.getZ() - platformPos.getZ(), 2);
        return hDistSq <= 9.0 && this.getY() >= platformPos.getY() - 1.0 && this.isOnGround();
    }

    public StoneThrowerGoblinEntity(EntityType<? extends StoneThrowerGoblinEntity> entityType, World world) {
        super(entityType, world);
    }

    @Override
    protected void initGoals() {
        // Lancer de pierre : actif partout (sol comme plateforme), priorite max.
        // Jamais de melee : le lanceur n'attaque qu'a distance.
        this.goalSelector.add(1, new ThrowStoneGoal(this));
        // Comportement de deplacement "gobelin normal" au sol : errer,
        // uniquement quand le gobelin n'est pas sur sa plateforme.
        this.goalSelector.add(4, new GroundOnlyGoal(new net.minecraft.entity.ai.goal.WanderAroundGoal(this, 0.8)));
        this.goalSelector.add(3, new PlatformWanderGoal(this, () -> this.platformPos, 0.6, PLATFORM_RADIUS));
        this.goalSelector.add(6, new LookAtEntityGoal(this, PlayerEntity.class, 8.0f));
        this.goalSelector.add(7, new LookAroundGoal(this));
        this.targetSelector.add(1, new ActiveTargetGoal<>(this, PlayerEntity.class, true));
        this.targetSelector.add(2, new RevengeGoal(this));
    }

    /** Active le goal interne uniquement quand le gobelin est au sol (hors plateforme). */
    private class GroundOnlyGoal extends Goal {
        private final Goal delegate;

        GroundOnlyGoal(Goal delegate) {
            this.delegate = delegate;
            this.setControls(delegate.getControls());
        }

        @Override
        public boolean canStart() {
            return !StoneThrowerGoblinEntity.this.isOnPlatform() && delegate.canStart();
        }

        @Override
        public boolean shouldContinue() {
            return !StoneThrowerGoblinEntity.this.isOnPlatform() && delegate.shouldContinue();
        }

        @Override
        public void start() { delegate.start(); }

        @Override
        public void stop() { delegate.stop(); }

        @Override
        public void tick() { delegate.tick(); }
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
        boolean onPlatform = this.isOnPlatform();

        // Effet de lourdeur : tant que le gobelin est sur sa plateforme, il est
        // ralenti (difficile a pousser). S'il est au sol, il bouge normalement.
        if (onPlatform) {
            if (!this.hasStatusEffect(net.minecraft.entity.effect.StatusEffects.SLOWNESS)) {
                this.addStatusEffect(new net.minecraft.entity.effect.StatusEffectInstance(
                    net.minecraft.entity.effect.StatusEffects.SLOWNESS, 60, 2, false, false));
            }
        } else if (this.hasStatusEffect(net.minecraft.entity.effect.StatusEffects.SLOWNESS)) {
            this.removeStatusEffect(net.minecraft.entity.effect.StatusEffects.SLOWNESS);
        }

        // Détection de bord de plateforme : demi-tour (cooldown 20 ticks)
        if (onPlatform && this.age % 20 == 0 && this.getNavigation().isFollowingPath() && PlatformWanderGoal.isNearEdge(this)) {
            Vec3d back = new Vec3d(platformPos.getX() - this.getX(), 0, platformPos.getZ() - this.getZ()).normalize();
            this.setVelocity(this.getVelocity().add(back.x * 0.3, 0.1, back.z * 0.3));
            this.velocityModified = true;
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
