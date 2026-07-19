package com.dungeonmod.entity;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.goal.Goal;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.boss.BossBar;
import net.minecraft.entity.boss.ServerBossBar;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.data.TrackedData;
import net.minecraft.entity.data.TrackedDataHandlerRegistry;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.mob.PathAwareEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Hand;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import com.dungeonmod.DungeonMod;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.PlayState;
import software.bernie.geckolib.animation.RawAnimation;
import software.bernie.geckolib.util.GeckoLibUtil;

public class OgreEntity extends PathAwareEntity implements GeoEntity {
    public static final EntityType<OgreEntity> TYPE = Registry.register(
        Registries.ENTITY_TYPE, Identifier.of("dungeonmod", "ogre"),
        EntityType.Builder.<OgreEntity>create(OgreEntity::new, net.minecraft.entity.SpawnGroup.MONSTER)
            .dimensions(2.4f, 5.0f).maxTrackingRange(10).trackingTickInterval(1)
            .build(RegistryKey.of(Registries.ENTITY_TYPE.getKey(), Identifier.of("dungeonmod", "ogre")))
    );

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
    private static final RawAnimation IDLE_ANIM = RawAnimation.begin().thenLoop("pose_sans_joueur");
    private static final RawAnimation WELCOME_ANIM = RawAnimation.begin().thenPlay("animation_when_player_going");
    private static final RawAnimation DEAD_ANIM = RawAnimation.begin().thenPlay("dead");
    private static final RawAnimation WALK_ANIM = RawAnimation.begin().thenLoop("walk");
    private static final RawAnimation THROW_ANIM = RawAnimation.begin().thenPlay("lancer_de_pierre");
    private static final RawAnimation ATTACK_ANIM = RawAnimation.begin().thenPlay("attack");
    private static final RawAnimation HEADBUTT_ANIM = RawAnimation.begin().thenPlay("attack_coup_de_tete");
    private static final RawAnimation EYE_ANIM = RawAnimation.begin().thenPlay("oeil_cache");

    private static final TrackedData<Integer> PHASE = DataTracker.registerData(OgreEntity.class, TrackedDataHandlerRegistry.INTEGER);
    // PHASE: 0=IDLE, 1=WELCOME, 2=COMBAT, 3=RETURN, 4=DEAD
    private static final TrackedData<Integer> ATTACK_STATE = DataTracker.registerData(OgreEntity.class, TrackedDataHandlerRegistry.INTEGER);
    // ATTACK_STATE: 0=none, 1=throw, 2=clap, 3=headbutt, 4=eye

    private int animTimer = 0;
    private long nextAttackTime = 100, lastHiddenEyeTime = 0;
    private static final long COOLDOWN_HIDDEN_EYE = 200;
    public boolean throwTestMode = false;
    public int roomMinX, roomMaxX, roomMinZ, roomMaxZ;
    private boolean stoneThrown = false;
    private StoneEntity groundStone = null;
    private LivingEntity attackTarget = null;
    private ServerBossBar bossBar;
    private boolean isDeadPermanent = false;
    private boolean hasPlayedWelcome = false;
    public float roomFacing = 0f;
    private int deathStage = 0;
    private boolean eyeHitBoosted = false;
    private final java.util.Set<java.util.UUID> welcomedPlayers = new java.util.HashSet<>();

    public OgreEntity(EntityType<? extends PathAwareEntity> type, World world) {
        super(type, world);
    }

    @Override protected void initDataTracker(DataTracker.Builder builder) {
        super.initDataTracker(builder);
        builder.add(PHASE, 0);
        builder.add(ATTACK_STATE, 0);
    }

    public int getPhase() { return dataTracker.get(PHASE); }
    public void setPhase(int v) { dataTracker.set(PHASE, v); }
    public int getAttackState() { return dataTracker.get(ATTACK_STATE); }
    public void setAttackState(int v) { dataTracker.set(ATTACK_STATE, v); }
    public boolean isAnimating() { return getAttackState() != 0 || getPhase() == 1 || getPhase() == 3 || getPhase() == 4; }

    @Override public void writeCustomDataToNbt(net.minecraft.nbt.NbtCompound nbt) {
        super.writeCustomDataToNbt(nbt);
        nbt.putLong("nextAttackTime", nextAttackTime);
        nbt.putBoolean("throwTestMode", throwTestMode);
        nbt.putInt("roomMinX", roomMinX); nbt.putInt("roomMaxX", roomMaxX);
        nbt.putInt("roomMinZ", roomMinZ); nbt.putInt("roomMaxZ", roomMaxZ);
        nbt.putFloat("roomFacing", roomFacing);
        nbt.putInt("phase", getPhase());
        nbt.putBoolean("dead", isDeadPermanent);
        nbt.putInt("deathStage", deathStage);
    }

    @Override public void readCustomDataFromNbt(net.minecraft.nbt.NbtCompound nbt) {
        super.readCustomDataFromNbt(nbt);
        if (nbt.contains("nextAttackTime")) {
            nextAttackTime = nbt.getLong("nextAttackTime");
            if (this.age - nextAttackTime < 0) nextAttackTime = 0;
        }
        if (nbt.contains("throwTestMode")) throwTestMode = nbt.getBoolean("throwTestMode");
        if (nbt.contains("roomMinX")) {
            roomMinX = nbt.getInt("roomMinX"); roomMaxX = nbt.getInt("roomMaxX");
            roomMinZ = nbt.getInt("roomMinZ"); roomMaxZ = nbt.getInt("roomMaxZ");
        }
        if (nbt.contains("roomFacing")) roomFacing = nbt.getFloat("roomFacing");
        if (nbt.contains("dead")) { isDeadPermanent = nbt.getBoolean("dead"); setPhase(isDeadPermanent ? 4 : 0); }
        if (nbt.contains("phase")) setPhase(nbt.getInt("phase"));
        if (nbt.contains("deathStage")) deathStage = nbt.getInt("deathStage");
        else if (nbt.contains("returnStage")) deathStage = nbt.getInt("returnStage"); // compatibilité
    }

    @Override public void remove(Entity.RemovalReason reason) {
        if (bossBar != null) bossBar.clearPlayers();
        super.remove(reason);
    }

    @Override public AnimatableInstanceCache getAnimatableInstanceCache() { return cache; }
    @Override public double getTick(Object object) { return this.age; }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar registrar) {
        final int[] prevAttack = {0};
        final int[] prevPhase = {-1};
        final boolean[] wasMoving = {false};
        registrar.add(new AnimationController<>(this, "main", 2, state -> {
            int phase = getPhase();
            int attack = getAttackState();
            
            // Si mort définitive et qu'il a fini de s'allonger -> "pose_sans_joueur" permanente
            if (phase == 4 && deathStage == 3) { prevPhase[0] = 4; return state.setAndContinue(IDLE_ANIM); }

            if (phase == 0 && prevPhase[0] != 0) { prevPhase[0] = 0; prevAttack[0] = 0; return state.setAndContinue(IDLE_ANIM); }
            if (phase == 0) return PlayState.CONTINUE;
            if (phase == 1 && prevPhase[0] != 1) { prevPhase[0] = 1; prevAttack[0] = 0; return state.setAndContinue(WELCOME_ANIM); }
            if (phase == 1) return PlayState.CONTINUE;
            if (phase == 4 && prevPhase[0] != 4) { prevPhase[0] = 4; prevAttack[0] = 0; return state.setAndContinue(DEAD_ANIM); }
            if (phase == 4) return PlayState.CONTINUE;
            
            // COMBAT
            if (attack != 0 && attack != prevAttack[0]) {
                prevAttack[0] = attack; prevPhase[0] = 2;
                RawAnimation a;
                switch (attack) {
                    case 1: a = THROW_ANIM; break; case 2: a = ATTACK_ANIM; break;
                    case 3: a = HEADBUTT_ANIM; break; case 4: a = EYE_ANIM; break;
                    default: a = WALK_ANIM;
                }
                return state.setAndContinue(a);
            }
            if (attack == 0) {
                prevAttack[0] = 0; prevPhase[0] = 2;
                boolean moving = this.getVelocity().horizontalLengthSquared() > 0.0001 || state.isMoving();
                if (moving && !wasMoving[0]) {
                    wasMoving[0] = true;
                    return state.setAndContinue(WALK_ANIM);
                }
                if (!moving && wasMoving[0]) {
                    wasMoving[0] = false;
                    return PlayState.STOP;
                }
                if (moving) return PlayState.CONTINUE;
                return PlayState.STOP;
            }
            return PlayState.CONTINUE;
        }).setAnimationSpeed(0.5f)); // Vitesse de 0.5f conservée pour respecter vos animations et timers
    }

    // Interception des dégâts pour déclencher la mort scénarisée au centre de la salle
    public boolean damage(net.minecraft.server.world.ServerWorld world, net.minecraft.entity.damage.DamageSource source, float amount) {
        if (isDeadPermanent || getPhase() == 4) return false;

        // 4x dégâts si ce coup a déclenché l'œil
        if (eyeHitBoosted) { amount *= 4.0f; eyeHitBoosted = false; }

        // 50% de réduction pendant l'anim œil
        if (getAttackState() == 4) amount *= 0.5f;

        if (this.getHealth() - amount <= 0.01f) {
            this.setHealth(0.1f);
            this.setPhase(4);
            this.deathStage = 0;
            this.animTimer = 0;
            this.setInvulnerable(true);
            this.getNavigation().stop();
            if (bossBar != null) bossBar.clearPlayers();
            return false;
        }
        return super.damage(world, source, amount);
    }

    @Override
    public void tick() {
        super.tick();

        if (age % 40 == 0) DungeonMod.LOGGER.info("[Cyclope] age={} phase={} attack={} room=({},{})-({},{}) dead={}",
            age, getPhase(), getAttackState(), roomMinX, roomMinZ, roomMaxX, roomMaxZ, isDeadPermanent);

        // Bossbar + welcome simple (une seule fois, pas de RETURN)
        if (bossBar != null) {
            bossBar.setPercent(getHealth() / getMaxHealth());
            boolean playerVisible = false;
            
            for (var p : getWorld().getPlayers()) {
                if (p instanceof ServerPlayerEntity sp) {
                    double dist = distanceTo(sp);
                    boolean inRoom = roomMaxX > 0 && sp.getBlockX() >= roomMinX - 2 && sp.getBlockX() <= roomMaxX + 2
                        && sp.getBlockZ() >= roomMinZ - 2 && sp.getBlockZ() <= roomMaxZ + 2;
                    
                    if (inRoom && !isDeadPermanent && getPhase() < 4) {
                        if (getPhase() == 0) {
                            setPhase(1); animTimer = 1; setInvulnerable(true);
                            if (!hasPlayedWelcome) {
                                hasPlayedWelcome = true;
                                sp.sendMessage(Text.literal(""), true);
                                sp.sendMessage(Text.literal("§6⚔ Salle du Cyclope ⚔"), false);
                                sp.playSoundToPlayer(SoundEvents.ENTITY_WITHER_SPAWN, SoundCategory.HOSTILE, 1.0f, 1.0f);
                            }
                        }
                        if (dist <= 10.0) playerVisible = true;
                    }
                }
            }
            
            boolean invuln = getPhase() == 1 || getPhase() == 4 || isDeadPermanent;
            bossBar.setColor(invuln ? BossBar.Color.WHITE : BossBar.Color.YELLOW);
            // Style différent pendant l'œil caché (dégâts réduits)
            bossBar.setStyle(getAttackState() == 4 ? BossBar.Style.NOTCHED_6 : BossBar.Style.PROGRESS);
            bossBar.setVisible(playerVisible);
        }

        int phase = getPhase();

        if (phase == 0) { // IDLE
            getNavigation().stop(); setVelocity(0, getVelocity().y, 0);
            setBodyYaw(roomFacing); setHeadYaw(roomFacing); setYaw(roomFacing);
            prevBodyYaw = roomFacing; prevHeadYaw = roomFacing; prevYaw = roomFacing;
        } else if (phase == 1) { // WELCOME
            animTimer++;
            getNavigation().stop(); setVelocity(0, getVelocity().y, 0);
            if (animTimer >= 80) { setPhase(2); animTimer = 0; setInvulnerable(false); nextAttackTime = age + 20; }
        } else if (phase == 4) { // DEATH: walk to center → dead anim → permanent IDLE
            int cx = (roomMinX + roomMaxX) / 2, cz = (roomMinZ + roomMaxZ) / 2;
            animTimer++;
            
            if (deathStage == 0) {
                double dx = cx - getX(), dz = cz - getZ();
                float targetYaw = (float)(Math.atan2(dz, dx) * 180.0 / Math.PI) - 90.0f;
                setBodyYaw(targetYaw); setHeadYaw(targetYaw); setYaw(targetYaw);
                getNavigation().startMovingTo(cx, getY(), cz, 0.8);
                deathStage = 1;
            }
            if (deathStage == 1) {
                if (squaredDistanceTo(new Vec3d(cx + 0.5, getY(), cz + 0.5)) <= 4.0 || animTimer > 200) {
                    getNavigation().stop(); setVelocity(0, getVelocity().y, 0);
                    setBodyYaw(roomFacing); setHeadYaw(roomFacing); setYaw(roomFacing);
                    prevBodyYaw = roomFacing; prevHeadYaw = roomFacing; prevYaw = roomFacing;
                    deathStage = 2; animTimer = 0;
                }
            }
            if (deathStage == 2) {
                getNavigation().stop(); setVelocity(0, getVelocity().y, 0);
                if (animTimer >= 120) {
                    deathStage = 3; isDeadPermanent = true;
                }
            }
            if (deathStage == 3) {
                getNavigation().stop(); setVelocity(0, getVelocity().y, 0);
                setInvulnerable(true); setHealth(0.01f);
            }
        } else if (phase == 2) { // COMBAT (Tous vos timers originaux sont restaurés)
            // Test mode
            if (throwTestMode) {
                getNavigation().stop();
                if (getAttackState() == 0) { setAttackState(1); animTimer = 1; attackTarget = getWorld().getClosestPlayer(this, 20.0); }
            }

            if (animTimer > 0) {
                animTimer++;
                getNavigation().stop();
                int as = getAttackState();
                if (attackTarget != null) {
                    double dx = attackTarget.getX() - getX(), dz = attackTarget.getZ() - getZ();
                    float yaw = (float)(Math.atan2(dz, dx) * 180.0 / Math.PI) - 90.0f;
                    setBodyYaw(yaw); setHeadYaw(yaw); setYaw(yaw);
                    prevBodyYaw = yaw; prevHeadYaw = yaw; prevYaw = yaw;
                }
                setVelocity(0, getVelocity().y, 0);

                if (as == 1) { // Lancer d'origine (vitesse 0.5f)
                    if (animTimer == 24) setStackInHand(Hand.MAIN_HAND, new net.minecraft.item.ItemStack(StoneEntity.STONE_ITEM));
                    else if (animTimer == 45) {
                        setStackInHand(Hand.MAIN_HAND, net.minecraft.item.ItemStack.EMPTY);
                        if (attackTarget != null && attackTarget.isAlive() && !stoneThrown) {
                            stoneThrown = true;
                            var look = getRotationVec(1.0f);
                            var spawnPos = getPos().add(look.x * 2.0, 1.2, look.z * 2.0);
                            var s = new StoneEntity(StoneEntity.STONE_TYPE, getWorld());
                            s.setPosition(spawnPos.x, spawnPos.y, spawnPos.z);
                            var dir = attackTarget.getPos().add(0, attackTarget.getHeight() * 0.3, 0).subtract(spawnPos);
                            double dist = dir.length(); var horiz = new Vec3d(dir.x, 0, dir.z).normalize();
                            double speed = Math.min(3.0, dist * 0.18 + 0.8); double vert = 0.25 + dist * 0.04;
                            s.setVelocity(horiz.x * speed, vert, horiz.z * speed);
                            s.setOwner(this); getWorld().spawnEntity(s);
                        }
                    } else if (animTimer >= 60) endAttack(1);
                } else if (as == 2 && animTimer >= 70 && animTimer <= 75) {
                    if (animTimer == 70) {
                        if (getWorld() instanceof ServerWorld sw) {
                            var box = getBoundingBox().expand(3.0);
                            for (var e : getWorld().getOtherEntities(this, box)) {
                                if (e instanceof PlayerEntity p && !p.isDead()) {
                                    double kx = p.getX() - getX(), kz = p.getZ() - getZ();
                                    if (kx*kx + kz*kz > 0.01) { double len = Math.sqrt(kx*kx + kz*kz); p.setVelocity(p.getVelocity().add(kx/len*3.0, 0.3, kz/len*3.0)); p.velocityModified = true; }
                                    p.damage(sw, getDamageSources().mobAttack(this), 3.0f);
                                }
                            }
                            sw.playSound(null, getX(), getY(), getZ(), SoundEvents.ENTITY_PLAYER_ATTACK_STRONG, SoundCategory.HOSTILE, 1.5f, 0.8f);
                            sw.spawnParticles(ParticleTypes.CRIT, getX(), getY()+1.5, getZ(), 60, 2.5, 1.5, 2.5, 0.2);
                        }
                    }
                    DungeonMod.LOGGER.info("[Cyclope] clap tick={} as={}", animTimer, as);
                } else if (as == 2 && animTimer >= 150) endAttack(2);
                else if (as == 3 && animTimer == 30) applyHeadbuttEffects();
                else if (as == 3 && animTimer >= 120) endAttack(3);
                else if (as == 4 && animTimer >= 100) endAttack(4); // Camouflage œil d'origine
            }
        }
    }

    private void endAttack(int as) {
        setAttackState(0); animTimer = 0; stoneThrown = false;
        nextAttackTime = age + 20 + random.nextInt(41);
        if (groundStone != null) { groundStone.discard(); groundStone = null; }
        if (!throwTestMode) attackTarget = null;
    }

    public void onEyeHit(boolean boosted) {
        if (getWorld().isClient()) return;
        long now = age;
        if (now - lastHiddenEyeTime < COOLDOWN_HIDDEN_EYE) return;
        if (boosted) eyeHitBoosted = true;
        int cs = getAttackState();
        if (cs == 2 && animTimer >= 35) return; if (cs == 3 && animTimer >= 30) return; if (cs == 1 && animTimer >= 45) return;
        if (cs != 0) { setAttackState(0); animTimer = 0; }
        lastHiddenEyeTime = now; setAttackState(4); animTimer = 1; getNavigation().stop();
    }

    private void applyHeadbuttEffects() {
        if (!(getWorld() instanceof ServerWorld sw)) return;
        var box = getBoundingBox().expand(5.0);
        for (var e : getWorld().getOtherEntities(this, box)) {
            if (e instanceof PlayerEntity p && !p.isDead() && squaredDistanceTo(p) <= 25.0) {
                p.damage(sw, getDamageSources().mobAttack(this), 4.0f);
                double kx = p.getX() - getX(), kz = p.getZ() - getZ();
                if (kx*kx + kz*kz > 0.01) { double len = Math.sqrt(kx*kx + kz*kz); p.setVelocity(p.getVelocity().add(kx/len*2.0, 0.5, kz/len*2.0)); p.velocityModified = true; }
                p.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS, 20, 10, false, true, true));
                p.addStatusEffect(new StatusEffectInstance(StatusEffects.NAUSEA, 30, 0, false, true, true));
                p.addStatusEffect(new StatusEffectInstance(StatusEffects.BLINDNESS, 10, 0, false, true, true));
            }
        }
        sw.playSound(null, getX(), getY(), getZ(), SoundEvents.ENTITY_GENERIC_EXPLODE, SoundCategory.HOSTILE, 2.0f, 0.5f);
        sw.spawnParticles(ParticleTypes.EXPLOSION_EMITTER, getX(), getY()+1.5, getZ(), 1, 0.5, 0.5, 0.5, 0);
        sw.spawnParticles(ParticleTypes.SNOWFLAKE, getX(), getY()+1.5, getZ(), 80, 3.0, 2.0, 3.0, 0.3);
    }

    @Override
    protected void initGoals() {
        goalSelector.add(0, new AttackGoal());
        goalSelector.add(1, new net.minecraft.entity.ai.goal.WanderAroundGoal(this, 0.6, 10) {
            @Override public boolean canStart() { return getPhase() == 2 && super.canStart(); }
        });
        goalSelector.add(2, new net.minecraft.entity.ai.goal.LookAroundGoal(this) {
            @Override public boolean canStart() { return getPhase() == 2 && super.canStart(); }
        });
        goalSelector.add(3, new net.minecraft.entity.ai.goal.LookAtEntityGoal(this, PlayerEntity.class, 8.0f) {
            @Override public boolean canStart() { return getPhase() == 2 && super.canStart(); }
        });
    }

    class AttackGoal extends Goal {
        @Override
        public boolean canStart() {
            if (getPhase() != 2) return false;
            if (getAttackState() != 0) return false;
            if (throwTestMode) return false;
            if (age < nextAttackTime) return false;
            LivingEntity t = OgreEntity.this.getTarget();
            if (t == null || !t.isAlive()) {
                t = getWorld().getClosestPlayer(OgreEntity.this, 12.0);
                if (t != null) setTarget(t);
            }
            return t != null && t.isAlive() && squaredDistanceTo(t) <= 144.0 && canSee(t);
        }

        @Override
        public void start() {
            LivingEntity t = getTarget(); if (t == null) return;
            attackTarget = t;
            double dist = squaredDistanceTo(t);
            if (dist > 25.0 && dist <= 144.0) setAttackState(1);
            else if (dist > 9.0 && dist <= 25.0) setAttackState(3);
            else if (dist > 0.0 && dist <= 9.0) setAttackState(2);
            else return;
            nextAttackTime = age + 20 + random.nextInt(41);
            animTimer = 1; getNavigation().stop();
        }

        @Override public boolean shouldContinue() { return false; }
    }

    @Override
    public void onStartedTrackingBy(ServerPlayerEntity player) {
        super.onStartedTrackingBy(player);
        if (bossBar == null) {
            bossBar = new ServerBossBar(Text.literal("§eCyclope"), BossBar.Color.YELLOW, BossBar.Style.PROGRESS);
            bossBar.setDarkenSky(false); bossBar.setThickenFog(false); bossBar.setVisible(false);
        }
        bossBar.addPlayer(player);
    }

    @Override
    public void onStoppedTrackingBy(ServerPlayerEntity player) {
        super.onStoppedTrackingBy(player);
        if (bossBar != null) bossBar.removePlayer(player);
    }

    public static void registerAttributes() {
        net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry.register(TYPE,
            PathAwareEntity.createMobAttributes()
                .add(EntityAttributes.MAX_HEALTH, 100.0)
                .add(EntityAttributes.ATTACK_DAMAGE, 4.0)
                .add(EntityAttributes.FOLLOW_RANGE, 16.0)
                .add(EntityAttributes.MOVEMENT_SPEED, 0.2));
    }
}