package com.dungeonmod.entity;

import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.SpawnGroup;
import net.minecraft.entity.ai.goal.ActiveTargetGoal;
import net.minecraft.entity.ai.goal.Goal;
import net.minecraft.entity.ai.goal.LookAtEntityGoal;
import net.minecraft.entity.ai.goal.LookAroundGoal;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.data.TrackedData;
import net.minecraft.entity.data.TrackedDataHandlerRegistry;
import net.minecraft.entity.mob.PathAwareEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Hand;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.PlayState;
import software.bernie.geckolib.animation.RawAnimation;
import software.bernie.geckolib.util.GeckoLibUtil;

public class GoblinMinibossEntity extends PathAwareEntity implements GeoEntity {

    private static final int ATTACK_MAIN = 1;
    private static final int ATTACK_PIED = 2;
    private static final int ATTACK_MAIN_DAMAGE_TICK = 30;  // 1.5s → impact
    private static final int ATTACK_PIED_DAMAGE_TICK = 20;  // 1.0s → impact
    private static final int ATTACK_MAIN_ANIM_TICKS = 50;   // animation 2.5s
    private static final int ATTACK_PIED_ANIM_TICKS = 30;   // animation 1.5s
    private static final double CHASE_MAX_FROM_ROOM = 15.0; // suit le joueur jusqu'à 15 blocs de la salle
    private static final double ROOM_RETURN_DIST = 4.0;     // rayon de retour dans la salle

    private static final TrackedData<Integer> ATTACK_TYPE =
        DataTracker.registerData(GoblinMinibossEntity.class, TrackedDataHandlerRegistry.INTEGER);

    public static final EntityType<GoblinMinibossEntity> TYPE = Registry.register(
        Registries.ENTITY_TYPE, Identifier.of("dungeonmod", "miniboss_goblin"),
        EntityType.Builder.<GoblinMinibossEntity>create(GoblinMinibossEntity::new, SpawnGroup.MONSTER)
            .dimensions(0.9f, 2.8f).maxTrackingRange(10).trackingTickInterval(1)
            .build(RegistryKey.of(Registries.ENTITY_TYPE.getKey(), Identifier.of("dungeonmod", "miniboss_goblin")))
    );

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
    private int attackCooldown = 0;
    private int attackStartAge = -1;
    private boolean animMoving = false;
    private double lastX = 0;
    private double lastZ = 0;
    private BlockPos roomAnchor = null;

    public GoblinMinibossEntity(EntityType<? extends PathAwareEntity> type, World world) {
        super(type, world);
    }

    public void setRoomAnchor(double x, double y, double z) {
        roomAnchor = new BlockPos((int)x, (int)y, (int)z);
    }

    @Override
    protected void initDataTracker(DataTracker.Builder builder) {
        super.initDataTracker(builder);
        builder.add(ATTACK_TYPE, 0);
    }

    @Override
    protected void initGoals() {
        goalSelector.add(1, new MinibossAttackGoal());
        goalSelector.add(2, new RoomWanderGoal());
        goalSelector.add(3, new LookAtEntityGoal(this, PlayerEntity.class, 8.0f));
        goalSelector.add(4, new LookAroundGoal(this));
        targetSelector.add(1, new ActiveTargetGoal<>(this, PlayerEntity.class, true));
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar registrar) {
        final boolean[] wasMoving = {false};
        registrar.add(new AnimationController<>(this, "main", 5, state -> {
            int atk = dataTracker.get(ATTACK_TYPE);
            if (atk != 0) {
                String anim = atk == ATTACK_MAIN ? "Attaque_main_1" : "attaque_pied";
                return state.setAndContinue(RawAnimation.begin().thenPlay(anim));
            }
            if (animMoving && !wasMoving[0]) {
                wasMoving[0] = true;
                return state.setAndContinue(RawAnimation.begin().thenLoop("Marche"));
            }
            if (!animMoving && wasMoving[0]) {
                wasMoving[0] = false;
                return PlayState.STOP;
            }
            return PlayState.CONTINUE;
        }));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() { return cache; }

    @Override
    public void tick() {
        double dx = getX() - lastX;
        double dz = getZ() - lastZ;
        animMoving = dx * dx + dz * dz > 1.0E-6;
        lastX = getX();
        lastZ = getZ();

        if (!getWorld().isClient() && roomAnchor != null) {
            double anchorX = roomAnchor.getX() + 0.5;
            double anchorY = roomAnchor.getY();
            double anchorZ = roomAnchor.getZ() + 0.5;
            LivingEntity target = getTarget();
            if (target != null
                    && target.squaredDistanceTo(anchorX, anchorY, anchorZ) > CHASE_MAX_FROM_ROOM * CHASE_MAX_FROM_ROOM) {
                setTarget(null);
            }
            if (getTarget() == null
                    && squaredDistanceTo(anchorX, anchorY, anchorZ) > ROOM_RETURN_DIST * ROOM_RETURN_DIST) {
                getNavigation().startMovingTo(anchorX, anchorY, anchorZ, 1.0);
            }
        }

        super.tick();
        if (attackCooldown > 0) attackCooldown--;
        int atk = dataTracker.get(ATTACK_TYPE);
        if (atk != 0 && attackStartAge >= 0) {
            int animTicks = atk == ATTACK_MAIN ? ATTACK_MAIN_ANIM_TICKS : ATTACK_PIED_ANIM_TICKS;
            if (age - attackStartAge >= animTicks) {
                dataTracker.set(ATTACK_TYPE, 0);
                attackStartAge = -1;
            }
        }
    }

    @Override
    public void writeCustomDataToNbt(NbtCompound nbt) {
        super.writeCustomDataToNbt(nbt);
        if (roomAnchor != null) {
            nbt.putInt("RoomAnchorX", roomAnchor.getX());
            nbt.putInt("RoomAnchorY", roomAnchor.getY());
            nbt.putInt("RoomAnchorZ", roomAnchor.getZ());
        }
    }

    @Override
    public void readCustomDataFromNbt(NbtCompound nbt) {
        super.readCustomDataFromNbt(nbt);
        if (nbt.contains("RoomAnchorX")) {
            roomAnchor = new BlockPos(nbt.getInt("RoomAnchorX"), nbt.getInt("RoomAnchorY"), nbt.getInt("RoomAnchorZ"));
        }
    }

    private class RoomWanderGoal extends Goal {
        private int cooldown = 0;

        @Override
        public boolean canStart() {
            if (getTarget() != null) return false;
            if (roomAnchor == null) return false;
            if (cooldown-- > 0) return false;
            return true;
        }

        @Override
        public void start() {
            if (roomAnchor == null) return;
            double ax = roomAnchor.getX() + 0.5;
            double ay = roomAnchor.getY();
            double az = roomAnchor.getZ() + 0.5;
            double angle = random.nextDouble() * Math.PI * 2;
            double radius = random.nextDouble() * (ROOM_RETURN_DIST - 1.0);
            getNavigation().startMovingTo(ax + Math.cos(angle) * radius, ay, az + Math.sin(angle) * radius, 0.6);
            cooldown = 40 + random.nextInt(60);
        }

        @Override
        public boolean shouldContinue() {
            return getTarget() == null && !getNavigation().isIdle();
        }

        @Override
        public void stop() {
            getNavigation().stop();
        }
    }

    private class MinibossAttackGoal extends Goal {
        private int attackType = 0;
        private int startAge = 0;
        private boolean damageApplied = false;

        @Override
        public boolean canStart() {
            LivingEntity target = getTarget();
            return target != null && target.isAlive();
        }

        @Override
        public void start() {
            attackType = 0;
            damageApplied = false;
        }

        @Override
        public void tick() {
            LivingEntity target = getTarget();
            if (target == null || !target.isAlive()) { damageApplied = true; return; }
            if (attackType == 0) {
                if (squaredDistanceTo(target) > 9.0) {
                    getNavigation().startMovingTo(target, 1.0);
                } else if (attackCooldown > 0) {
                    getNavigation().stop();
                } else {
                    attackType = random.nextBoolean() ? ATTACK_PIED : ATTACK_MAIN;
                    dataTracker.set(ATTACK_TYPE, attackType);
                    attackStartAge = age;
                    swingHand(Hand.MAIN_HAND);
                    startAge = age;
                    getNavigation().stop();
                }
                return;
            }
            if (damageApplied) return;
            int delay = attackType == ATTACK_PIED ? ATTACK_PIED_DAMAGE_TICK : ATTACK_MAIN_DAMAGE_TICK;
            if (age - startAge >= delay) {
                float dmg = attackType == ATTACK_PIED ? 4.0f : 3.0f;
                target.damage((ServerWorld)getWorld(), getDamageSources().mobAttack(GoblinMinibossEntity.this), dmg);
                damageApplied = true;
                attackCooldown = attackType == ATTACK_PIED ? 20 : 30;
            }
        }

        @Override
        public boolean shouldContinue() {
            if (damageApplied) return false;
            LivingEntity target = getTarget();
            return target != null && target.isAlive();
        }
    }

    public static void registerAttributes() {
        net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry.register(TYPE,
            PathAwareEntity.createMobAttributes()
                .add(EntityAttributes.MAX_HEALTH, 200.0)
                .add(EntityAttributes.ATTACK_DAMAGE, 3.0)
                .add(EntityAttributes.FOLLOW_RANGE, 16.0)
                .add(EntityAttributes.MOVEMENT_SPEED, 0.25));
    }
}
