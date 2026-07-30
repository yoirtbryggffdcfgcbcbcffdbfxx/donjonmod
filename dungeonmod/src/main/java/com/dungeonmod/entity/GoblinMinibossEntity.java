package com.dungeonmod.entity;

import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.SpawnGroup;
import net.minecraft.entity.ai.goal.ActiveTargetGoal;
import net.minecraft.entity.ai.goal.Goal;
import net.minecraft.entity.ai.goal.LookAtEntityGoal;
import net.minecraft.entity.ai.goal.LookAroundGoal;
import net.minecraft.entity.ai.goal.WanderAroundGoal;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.data.TrackedData;
import net.minecraft.entity.data.TrackedDataHandlerRegistry;
import net.minecraft.entity.mob.PathAwareEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.util.Hand;
import net.minecraft.util.Identifier;
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

    public GoblinMinibossEntity(EntityType<? extends PathAwareEntity> type, World world) {
        super(type, world);
    }

    @Override
    protected void initDataTracker(DataTracker.Builder builder) {
        super.initDataTracker(builder);
        builder.add(ATTACK_TYPE, 0);
    }

    @Override
    protected void initGoals() {
        goalSelector.add(1, new MinibossAttackGoal());
        goalSelector.add(2, new WanderAroundGoal(this, 0.8));
        goalSelector.add(3, new LookAtEntityGoal(this, PlayerEntity.class, 8.0f));
        goalSelector.add(4, new LookAroundGoal(this));
        targetSelector.add(1, new ActiveTargetGoal<>(this, PlayerEntity.class, true));
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar registrar) {
        registrar.add(new AnimationController<>(this, "main", 5, state -> {
            int atk = dataTracker.get(ATTACK_TYPE);
            if (atk != 0) {
                String anim = atk == ATTACK_MAIN ? "Attaque_main_1" : "attaque_pied";
                return state.setAndContinue(RawAnimation.begin().thenPlay(anim));
            }
            if (getVelocity().horizontalLengthSquared() > 0.0001) {
                return state.setAndContinue(RawAnimation.begin().thenLoop("Marche"));
            }
            return PlayState.CONTINUE;
        }));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() { return cache; }

    @Override
    public void tick() {
        super.tick();
        if (attackCooldown > 0) attackCooldown--;
        int atk = dataTracker.get(ATTACK_TYPE);
        if (atk != 0 && handSwingTicks == 0) {
            dataTracker.set(ATTACK_TYPE, 0);
        }
    }

    private class MinibossAttackGoal extends Goal {
        @Override
        public boolean canStart() {
            if (attackCooldown > 0) return false;
            LivingEntity target = getTarget();
            return target != null && target.isAlive() && squaredDistanceTo(target) <= 9.0;
        }

        @Override
        public void start() {
            LivingEntity target = getTarget();
            if (target == null) return;
            int atk = random.nextBoolean() ? ATTACK_PIED : ATTACK_MAIN;
            dataTracker.set(ATTACK_TYPE, atk);
            swingHand(Hand.MAIN_HAND);
            float dmg = atk == ATTACK_PIED ? 4.0f : 3.0f;
            target.damage((net.minecraft.server.world.ServerWorld)getWorld(), getDamageSources().mobAttack(GoblinMinibossEntity.this), dmg);
            attackCooldown = atk == ATTACK_PIED ? 20 : 30;
            getNavigation().stop();
        }

        @Override
        public boolean shouldContinue() { return false; }
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
