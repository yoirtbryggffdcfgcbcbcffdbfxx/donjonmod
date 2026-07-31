package com.dungeonmod.entity;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.SpawnGroup;
import net.minecraft.entity.ai.goal.ActiveTargetGoal;
import net.minecraft.entity.ai.goal.Goal;
import net.minecraft.entity.ai.goal.LookAtEntityGoal;
import net.minecraft.entity.ai.goal.LookAroundGoal;
import net.minecraft.entity.ai.goal.WanderAroundGoal;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.boss.BossBar;
import net.minecraft.entity.boss.ServerBossBar;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.data.TrackedData;
import net.minecraft.entity.data.TrackedDataHandlerRegistry;
import net.minecraft.entity.mob.PathAwareEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
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
    private static final double BOSS_BAR_SHOW_DIST = 8.0;   // bossbar visible si ≤ 8 blocs (64.0²)
    private static final double BOSS_BAR_HIDE_DIST = 10.0;  // bossbar cachée si ≥ 10 blocs (100.0²)
    private static final double IDLE_FOLLOW_RANGE = 5.0;    // détection joueur en idle (comme gobelins)
    private static final double CHASE_FOLLOW_RANGE = 15.0;  // détection joueur en chasse (comme gobelins)

    private static final TrackedData<Integer> ATTACK_TYPE =
        DataTracker.registerData(GoblinMinibossEntity.class, TrackedDataHandlerRegistry.INTEGER);

    public static final EntityType<GoblinMinibossEntity> TYPE = Registry.register(
        Registries.ENTITY_TYPE, Identifier.of("dungeonmod", "miniboss_goblin"),
        EntityType.Builder.<GoblinMinibossEntity>create(GoblinMinibossEntity::new, SpawnGroup.MONSTER)
            .dimensions(0.9f, 2.8f).maxTrackingRange(10).trackingTickInterval(1)
            .build(RegistryKey.of(Registries.ENTITY_TYPE.getKey(), Identifier.of("dungeonmod", "miniboss_goblin")))
    );

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
    private ServerBossBar bossBar;
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
    public void onStartedTrackingBy(ServerPlayerEntity player) {
        super.onStartedTrackingBy(player);
        if (bossBar == null) {
            bossBar = new ServerBossBar(Text.literal("§cMiniboss Gobelin"), BossBar.Color.RED, BossBar.Style.PROGRESS);
            bossBar.setDarkenSky(false);
            bossBar.setThickenFog(false);
            bossBar.setVisible(false);
        }
        bossBar.addPlayer(player);
    }

    @Override
    public void onStoppedTrackingBy(ServerPlayerEntity player) {
        super.onStoppedTrackingBy(player);
        if (bossBar != null) bossBar.removePlayer(player);
    }

    @Override
    public void remove(Entity.RemovalReason reason) {
        if (bossBar != null) bossBar.clearPlayers();
        super.remove(reason);
    }

    private boolean bossBarTriggered = false;
    private boolean bossBarVisible = false;

    private void updateBossBar() {
        if (bossBar == null) return;
        bossBar.setPercent(getMaxHealth() > 0 ? getHealth() / getMaxHealth() : 0f);
        if (roomAnchor != null) {
            double ax = roomAnchor.getX() + 0.5;
            double az = roomAnchor.getZ() + 0.5;
            for (var p : getWorld().getPlayers()) {
                if (p instanceof ServerPlayerEntity sp
                        && Math.abs(sp.getX() - ax) <= 5.0 && Math.abs(sp.getZ() - az) <= 5.0) {
                    bossBarTriggered = true;
                    break;
                }
            }
        }
        if (bossBarTriggered) {
            double closest = Double.MAX_VALUE;
            for (var p : getWorld().getPlayers()) {
                if (p instanceof ServerPlayerEntity sp) {
                    closest = Math.min(closest, sp.squaredDistanceTo(this));
                }
            }
            if (closest <= BOSS_BAR_SHOW_DIST * BOSS_BAR_SHOW_DIST) {
                bossBarVisible = true;
            } else if (closest >= BOSS_BAR_HIDE_DIST * BOSS_BAR_HIDE_DIST) {
                bossBarVisible = false;
            }
        }
        bossBar.setVisible(bossBarVisible);
    }

    @Override
    protected void initDataTracker(DataTracker.Builder builder) {
        super.initDataTracker(builder);
        builder.add(ATTACK_TYPE, 0);
    }

    @Override
    protected void initGoals() {
        goalSelector.add(1, new MinibossAttackGoal());
        goalSelector.add(2, new WanderAroundGoal(this, 0.6));
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
            if (animMoving) return PlayState.CONTINUE;
            return PlayState.STOP;
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
            updateBossBar();
            double anchorX = roomAnchor.getX() + 0.5;
            double anchorY = roomAnchor.getY() + 1.0;
            double anchorZ = roomAnchor.getZ() + 0.5;
            LivingEntity target = getTarget();
            if (target != null
                    && target.squaredDistanceTo(anchorX, anchorY, anchorZ) > CHASE_MAX_FROM_ROOM * CHASE_MAX_FROM_ROOM) {
                setTarget(null);
            }
            var followAttr = getAttributeInstance(EntityAttributes.FOLLOW_RANGE);
            if (getTarget() != null) {
                if (followAttr != null) followAttr.setBaseValue(CHASE_FOLLOW_RANGE);
            } else {
                if (followAttr != null) followAttr.setBaseValue(IDLE_FOLLOW_RANGE);
                if (squaredDistanceTo(anchorX, anchorY, anchorZ) > ROOM_RETURN_DIST * ROOM_RETURN_DIST) {
                    getNavigation().startMovingTo(anchorX, anchorY, anchorZ, 1.0);
                }
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
                if (squaredDistanceTo(target) > 2.25) {
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
