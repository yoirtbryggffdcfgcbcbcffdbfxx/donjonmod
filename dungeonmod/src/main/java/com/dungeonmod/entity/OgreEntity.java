package com.dungeonmod.entity;

import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.goal.Goal;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.boss.BossBar;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.mob.PathAwareEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ActionResult;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Hand;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import com.dungeonmod.entity.boss.BossAnimation;
import com.dungeonmod.entity.boss.BossEntity;
import com.dungeonmod.entity.boss.BossPhase;
import com.dungeonmod.entity.boss.capability.BossBecomesNpc;
import com.dungeonmod.entity.boss.capability.BossHasCombos;
import com.dungeonmod.entity.boss.capability.BossHasDeathSequence;
import com.dungeonmod.entity.boss.capability.BossHasWeakPoint;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.PlayState;
import software.bernie.geckolib.animation.RawAnimation;

public class OgreEntity extends BossEntity
    implements NpcShopProvider, GeoEntity,
               BossHasWeakPoint, BossHasCombos<CyclopsAttack>,
               BossHasDeathSequence, BossBecomesNpc {

    public static final EntityType<OgreEntity> TYPE = Registry.register(
        Registries.ENTITY_TYPE, Identifier.of("dungeonmod", "ogre"),
        EntityType.Builder.<OgreEntity>create(OgreEntity::new, net.minecraft.entity.SpawnGroup.MONSTER)
            .dimensions(2.4f, 5.0f).maxTrackingRange(10).trackingTickInterval(1)
            .build(RegistryKey.of(Registries.ENTITY_TYPE.getKey(), Identifier.of("dungeonmod", "ogre")))
    );

    // ---------- Combos (legacy int codes conservés pour la persistance NBT) ----------
    private static final int[] COMBO_TC = {1, 5, 2};   // throw → charge → clap
    private static final int[] COMBO_TH = {1, 5, 3};   // throw → charge → headbutt
    private static final int[] COMBO_CH = {5, 3};      // charge → headbutt

    // ---------- État ----------
    private long lastHiddenEyeTime = 0;
    private static final long COOLDOWN_HIDDEN_EYE = 200;
    public boolean throwTestMode = false;
    // Champs room conservés publics pour compat (TestGenerator, etc.)
    public int roomMinX, roomMaxX, roomMinZ, roomMaxZ;
    public float roomFacing = 0f;
    public int deathStage = 0;
    public int clothsGiven = 0;
    public boolean hasTalked = false;
    public int dialogueTicks = 0;

    private boolean stoneThrown = false;
    private StoneEntity groundStone = null;
    // Combo state
    private int[] currentCombo = null;
    private int comboStep = 0;
    private int comboTimer = 0;
    // Charge state
    private float chargeYaw = 0f;
    private boolean chargeHitApplied = false;
    private LivingEntity attackTarget = null;
    private boolean eyeHitBoosted = false;
    private final java.util.Set<String> givenItems = new java.util.HashSet<>();
    public final java.util.Set<Integer> usedTradeIndices = new java.util.HashSet<>();
    private final java.util.Set<java.util.UUID> welcomedPlayers = new java.util.HashSet<>();
    public final CyclopsTradeManager tradeManager = new CyclopsTradeManager(this);

    public OgreEntity(EntityType<? extends PathAwareEntity> type, World world) {
        super(type, world);
    }

    // ---------- Hooks BossEntity ----------

    @Override protected Text getBossBarName()         { return Text.literal("§eCyclope"); }
    @Override protected SoundEvent getWelcomeSound()   { return SoundEvents.ENTITY_WITHER_SPAWN; }
    @Override protected Text getWelcomeMessage()       { return Text.literal("§6⚔ Salle du Cyclope ⚔"); }
    @Override protected BossAnimation getIdleAnimation() { return CyclopsAttack.IDLE; }
    @Override protected BossAnimation getWalkAnimation() { return CyclopsAttack.WALK; }
    @Override protected int welcomeDurationTicks()       { return 40; }
    @Override protected int postAttackCooldownTicks()    { return 20 + random.nextInt(41); }

    @Override
    protected RawAnimation phaseAnimation(int phase) {
        return switch (phase) {
            case BossPhase.WELCOME -> CyclopsAttack.WELCOME.toRaw();
            case BossPhase.DEAD    -> CyclopsAttack.DEAD.toRaw();
            default -> null;
        };
    }

    @Override
    protected RawAnimation attackAnimation(int attackState) {
        CyclopsAttack atk = CyclopsAttack.fromCode(attackState);
        return atk == null ? null : atk.toRaw();
    }

    // ---------- Bossbar custom : style notched pendant l'œil caché ----------

    @Override
    protected void updateBossBar() {
        if (bossBar == null) return;
        bossBar.setPercent(getHealth() / getMaxHealth());
        boolean playerVisible = false;

        for (var p : getWorld().getPlayers()) {
            if (p instanceof ServerPlayerEntity sp) {
                double dist = distanceTo(sp);
                boolean inRoom = room.isDefined() && room.contains(sp.getBlockX(), sp.getBlockZ());

                if (inRoom && !deadPermanent && getPhase() < BossPhase.DEAD) {
                    if (getPhase() == BossPhase.IDLE) {
                        setPhase(BossPhase.WELCOME);
                        animTimer = 1;
                        setInvulnerable(true);
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
        boolean invuln = getPhase() == BossPhase.WELCOME || getPhase() == BossPhase.DEAD || deadPermanent;
        bossBar.setColor(invuln ? BossBar.Color.WHITE : BossBar.Color.YELLOW);
        bossBar.setStyle(getAttackState() == 4 ? BossBar.Style.NOTCHED_6 : BossBar.Style.PROGRESS);
        bossBar.setVisible(playerVisible);
    }

    // ---------- Mort scénarisée (BossHasDeathSequence) ----------

    @Override
    public void tickDeathSequence() {
        int cx = room.isDefined() ? room.centerX() : 0;
        int cz = room.isDefined() ? room.centerZ() : 0;
        animTimer++;
        if (deathStage == 0) {
            double dx = cx - getX(), dz = cz - getZ();
            float targetYaw = (float)(Math.atan2(dz, dx) * 180.0 / Math.PI) - 90.0f;
            setBodyYaw(targetYaw); setHeadYaw(targetYaw); setYaw(targetYaw);
            getNavigation().startMovingTo(cx, getY(), cz, 0.8);
            deathStage = 1;
        }
        if (deathStage == 1) {
            // 20 ticks (1s) de marche au centre, c'est suffisant pour voir
            // la cinématique sans faire trop attendre le joueur.
            if (animTimer >= 20) {
                getNavigation().stop(); setVelocity(0, getVelocity().y, 0);
                setBodyYaw(roomFacing); setHeadYaw(roomFacing); setYaw(roomFacing);
                prevBodyYaw = roomFacing; prevHeadYaw = roomFacing; prevYaw = roomFacing;
                deathStage = 2; animTimer = 0;
            }
        }
        if (deathStage == 2) {
            getNavigation().stop(); setVelocity(0, getVelocity().y, 0);
            // 30 ticks (1.5s) de gel avant mort permanente.
            if (animTimer >= 30) {
                deathStage = 3; deadPermanent = true;
            }
        }
        if (deathStage == 3) {
            getNavigation().stop(); setVelocity(0, getVelocity().y, 0);
            setInvulnerable(true); setHealth(0.01f);
        }
    }

    @Override
    public boolean isDeathSequenceDone() { return deathStage == 3 && deadPermanent; }

    @Override
    protected void tickDeath() {
        // Décrément du cooldown de dialogue (était un bug : dialogueTicks n'était
        // jamais décrémenté, d'où l'impression de "cooldown infini" sur le PNJ).
        if (dialogueTicks > 0) dialogueTicks--;

        if (deathStage < 3) {
            tickDeathSequence();
        } else {
            getNavigation().stop();
            setVelocity(0, getVelocity().y, 0);
            setInvulnerable(true); setHealth(0.01f);
        }
    }

    // ---------- Combat (la grosse partie) ----------

    @Override
    protected void tickCombat() {
        // Combo timer between steps
        if (animTimer == 0 && comboTimer > 0) {
            comboTimer--;
            if (comboTimer == 0) { startComboStep(); }
        }

        // Test mode
        if (throwTestMode) {
            getNavigation().stop();
            if (getAttackState() == 0) {
                setAttackState(1); animTimer = 1;
                attackTarget = getWorld().getClosestPlayer(this, 20.0);
            }
        }

        if (animTimer > 0) {
            animTimer++;
            getNavigation().stop();
            int as = getAttackState();
            if (attackTarget != null) {
                double dx = attackTarget.getX() - getX(), dz = attackTarget.getZ() - getZ();
                float yaw = (float)(Math.atan2(dz, dx) * 180.0 / Math.PI) - 90.0f;
                if (as != 5) { // charge keeps its own yaw
                    setBodyYaw(yaw); setHeadYaw(yaw); setYaw(yaw);
                    prevBodyYaw = yaw; prevHeadYaw = yaw; prevYaw = yaw;
                }
            }
            setVelocity(0, getVelocity().y, 0);

            if (as == 1) { // Lancer
                if (animTimer == 12) setStackInHand(Hand.MAIN_HAND, new ItemStack(StoneEntity.STONE_ITEM));
                else if (animTimer == 22) {
                    setStackInHand(Hand.MAIN_HAND, ItemStack.EMPTY);
                    if (attackTarget != null && attackTarget.isAlive() && !stoneThrown) {
                        stoneThrown = true;
                        var look = getRotationVec(1.0f);
                        var spawnPos = getPos().add(look.x * 2.0, 1.2, look.z * 2.0);
                        var s = new StoneEntity.CyclopsStoneEntity(StoneEntity.CYCLOPS_STONE_TYPE, getWorld());
                        s.setPosition(spawnPos.x, spawnPos.y, spawnPos.z);
                        Vec3d targetPos = attackTarget.getPos().add(0, attackTarget.getHeight() * 0.3, 0);
                        double dx = targetPos.x - spawnPos.x, dy = targetPos.y - spawnPos.y, dz = targetPos.z - spawnPos.z;
                        double horizDist = Math.sqrt(dx * dx + dz * dz);
                        if (horizDist > 0.01) {
                            double speed = Math.min(3.0, 0.8 + horizDist * 0.15);
                            double time = horizDist / speed;
                            double vy = dy / time + 0.5 * 0.03 * time;
                            s.setVelocity(dx / time, vy, dz / time);
                        }
                        s.setOwner(this); getWorld().spawnEntity(s);
                    }
                } else if (animTimer >= 30) endAttack(1);
            } else if (as == 2 && animTimer >= 35 && animTimer <= 40) {
                if (animTimer == 35) {
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
            } else if (as == 2 && animTimer >= 75) endAttack(2);
            else if (as == 3 && animTimer == 15) applyHeadbuttEffects();
            else if (as == 3 && animTimer >= 60) endAttack(3);
            else if (as == 4 && animTimer >= 50) endAttack(4);
            else if (as == 5) { // Charge
                if (animTimer == 10 && attackTarget != null) {
                    double dx = attackTarget.getX() - getX();
                    double dz = attackTarget.getZ() - getZ();
                    chargeYaw = (float)(Math.atan2(dz, dx) * 180.0 / Math.PI) - 90.0f;
                    chargeHitApplied = false;
                }
                if (animTimer >= 10 && animTimer <= 35) {
                    setBodyYaw(chargeYaw); setHeadYaw(chargeYaw); setYaw(chargeYaw);
                    prevBodyYaw = chargeYaw; prevHeadYaw = chargeYaw; prevYaw = chargeYaw;
                    float rad = chargeYaw * (float)Math.PI / 180.0f;
                    double speed = 4.0 / 25.0;
                    setVelocity(-Math.sin(rad) * speed, getVelocity().y, Math.cos(rad) * speed);
                    if (!chargeHitApplied && getWorld() instanceof ServerWorld sw) {
                        var box = getBoundingBox().expand(1.5);
                        for (var e : getWorld().getOtherEntities(this, box)) {
                            if (e instanceof PlayerEntity p && !p.isDead() && squaredDistanceTo(p) <= 16.0) {
                                chargeHitApplied = true;
                                double kx = p.getX() - getX(), kz = p.getZ() - getZ();
                                if (kx*kx + kz*kz > 0.01) { double len = Math.sqrt(kx*kx + kz*kz); p.setVelocity(p.getVelocity().add(kx/len*4.0, 0.4, kz/len*4.0)); p.velocityModified = true; }
                                p.damage(sw, getDamageSources().mobAttack(this), 5.0f);
                                // (Slowness removed — charge reste un burst dégât + knockback)
                            }
                        }
                    }
                } else {
                    setVelocity(0, getVelocity().y, 0);
                }
                if (animTimer >= 40) endCharge();
            }
        }
    }

    private void endCharge() {
        setAttackState(0); animTimer = 0; stoneThrown = false;
        chargeHitApplied = false;
        proceedCombo();
    }

    private void proceedCombo() {
        comboStep++;
        if (currentCombo != null && comboStep < currentCombo.length) {
            comboTimer = 10;
        } else {
            currentCombo = null;
            comboStep = 0;
            comboTimer = 0;
            nextAttackTime = age + postAttackCooldownTicks();
            if (groundStone != null) { groundStone.discard(); groundStone = null; }
            if (!throwTestMode) attackTarget = null;
        }
    }

    private void endAttack(int as) {
        setAttackState(0); animTimer = 0; stoneThrown = false;
        if (currentCombo != null) {
            proceedCombo();
        } else {
            nextAttackTime = age + postAttackCooldownTicks();
            if (groundStone != null) { groundStone.discard(); groundStone = null; }
            if (!throwTestMode) attackTarget = null;
        }
    }

    private void startCombo(int[] combo) {
        currentCombo = combo;
        comboStep = 0;
        comboTimer = 0;
        startComboStep();
    }

    private void startComboStep() {
        if (currentCombo == null || comboStep >= currentCombo.length) return;
        setAttackState(currentCombo[comboStep]);
        animTimer = 1;
        getNavigation().stop();
    }

    // ---------- BossHasCombos ----------

    @Override
    public CyclopsAttack[] pickCombo(LivingEntity target, double distSq) {
        int[] codes;
        if (distSq > 25.0 && distSq <= 144.0) {
            codes = random.nextBoolean() ? COMBO_TC : COMBO_TH;
        } else if (distSq > 9.0 && distSq <= 25.0) {
            codes = COMBO_CH;
        } else {
            codes = random.nextBoolean() ? COMBO_TC : COMBO_TH;
        }
        CyclopsAttack[] out = new CyclopsAttack[codes.length];
        for (int i = 0; i < codes.length; i++) {
            out[i] = CyclopsAttack.fromCode(codes[i]);
        }
        return out;
    }

    /** Implémentation non utilisée : la combo est gérée dans tickCombat via comboTimer. */
    @Override
    public void tickCombos() { /* géré par tickCombat */ }

    /** Helper interne pour démarrer un combo à partir des codes legacy. */
    private void startComboFromCodes(int[] combo) { startCombo(combo); }

    // ---------- BossHasWeakPoint (œil) ----------

    public void onEyeHit(boolean boosted) {
        if (getWorld().isClient()) return;
        long now = age;
        if (now - lastHiddenEyeTime < COOLDOWN_HIDDEN_EYE) return;
        if (boosted) eyeHitBoosted = true;
        int cs = getAttackState();
        if (cs == 2 && animTimer >= 17) return;
        if (cs == 3 && animTimer >= 15) return;
        if (cs == 1 && animTimer >= 22) return;
        if (cs == 5 && animTimer >= 10) return;
        if (cs != 0) { setAttackState(0); animTimer = 0; }
        lastHiddenEyeTime = now;
        setAttackState(4); animTimer = 1; getNavigation().stop();
    }

    @Override
    public boolean onWeakPointHit(PlayerEntity attacker) {
        onEyeHit(true);
        return getAttackState() == 4;
    }

    // ---------- BossBecomesNpc (post-mortem dialogue/shop) ----------

    @Override
    public void startDialogue(PlayerEntity player) {
        tradeManager.startDialogue(player);
    }

    @Override
    public ActionResult openTradeShop(PlayerEntity player) {
        return tradeManager.openTradeShop(player);
    }

    // ---------- Hooks de dégâts ----------

    @Override
    protected float damageMultiplier(DamageSource source, float amount) {
        if (eyeHitBoosted) {
            eyeHitBoosted = false;
            return 4.0f;
        }
        return 1.0f;
    }

    @Override
    protected float damageReduction(DamageSource source, float amount) {
        if (getAttackState() == 4) return 0.5f;
        return 1.0f;
    }

    @Override
    protected void onFatalHit(DamageSource source) {
        // Démarre la séquence de mort scénarisée.
        this.deathStage = 0;
        this.animTimer = 0;
        // Vider les mains (le bloc de pierre du lancer ne doit pas rester
        // affiché pendant l'anim de mort).
        setStackInHand(Hand.MAIN_HAND, ItemStack.EMPTY);
        setStackInHand(Hand.OFF_HAND, ItemStack.EMPTY);
    }

    /**
     * Clic gauche sur le boss mort = dialogue (comme BaseNpcEntity).
     * Appelé par BossEntity.damage() (override direct, pas via mixin)
     * dès que la phase est DEAD — donc même pendant la cinématique de mort.
     * La condition "phase DEAD" est vérifiée dans BossEntity.damage(),
     * pas besoin de re-tester deathStage == 3 ici.
     */
    @Override
    public void onPostMortemHit(PlayerEntity attacker) {
        // Anti-spam : tant qu'un dialogue est "frais" (cooldown 3s), on
        // ignore le clic — sinon le user peut spammer et relancer le même
        // dialogue plein de fois (cf. BaseNpcEntity.damage + dialogueCooldown).
        if (dialogueTicks > 0) return;
        startDialogue(attacker);
    }

    // ---------- Effets de combat ----------

    private void applyHeadbuttEffects() {
        if (!(getWorld() instanceof ServerWorld sw)) return;
        var box = getBoundingBox().expand(5.0);
        for (var e : getWorld().getOtherEntities(this, box)) {
            if (e instanceof PlayerEntity p && !p.isDead() && squaredDistanceTo(p) <= 25.0) {
                p.damage(sw, getDamageSources().mobAttack(this), 4.0f);
                double kx = p.getX() - getX(), kz = p.getZ() - getZ();
                if (kx*kx + kz*kz > 0.01) { double len = Math.sqrt(kx*kx + kz*kz); p.setVelocity(p.getVelocity().add(kx/len*2.0, 0.5, kz/len*2.0)); p.velocityModified = true; }
                p.addStatusEffect(new StatusEffectInstance(StatusEffects.NAUSEA, 30, 0, false, true, true));
                p.addStatusEffect(new StatusEffectInstance(StatusEffects.BLINDNESS, 10, 0, false, true, true));
            }
        }
        sw.playSound(null, getX(), getY(), getZ(), SoundEvents.ENTITY_GENERIC_EXPLODE, SoundCategory.HOSTILE, 2.0f, 0.5f);
        sw.spawnParticles(ParticleTypes.EXPLOSION_EMITTER, getX(), getY()+1.5, getZ(), 1, 0.5, 0.5, 0.5, 0);
        sw.spawnParticles(ParticleTypes.SNOWFLAKE, getX(), getY()+1.5, getZ(), 80, 3.0, 2.0, 3.0, 0.3);
    }

    // ---------- AI Goals ----------

    @Override
    protected void initGoals() {
        goalSelector.add(0, new AttackGoal());
        goalSelector.add(1, new net.minecraft.entity.ai.goal.WanderAroundGoal(this, 0.6, 10) {
            @Override public boolean canStart() { return getPhase() == BossPhase.COMBAT && super.canStart(); }
        });
        goalSelector.add(2, new net.minecraft.entity.ai.goal.LookAroundGoal(this) {
            @Override public boolean canStart() { return getPhase() == BossPhase.COMBAT && super.canStart(); }
        });
        goalSelector.add(3, new net.minecraft.entity.ai.goal.LookAtEntityGoal(this, PlayerEntity.class, 8.0f) {
            @Override public boolean canStart() { return getPhase() == BossPhase.COMBAT && super.canStart(); }
        });
    }

    @Override
    public ActionResult interactMob(PlayerEntity player, Hand hand) {
        // Clic droit : on ne fait rien de spécial. Le dialogue passe par damage() (clic gauche).
        return ActionResult.PASS;
    }

    // ---------- API publique (compat BoutTissuItem, TradeManager, etc.) ----------

    public void processBuyRequest(ServerPlayerEntity player, int originalIndex) {
        tradeManager.processBuyRequest(player, originalIndex);
    }
    public void sendSubtitles(PlayerEntity player, java.util.List<String> lines) {
        tradeManager.sendSubtitles(player, lines);
    }
    public void openShop(ServerPlayerEntity player) { openTradeShop(player); }
    public void processBuy(ServerPlayerEntity player, int tradeIndex, int quantity) {
        for (int i = 0; i < quantity; i++) processBuyRequest(player, tradeIndex);
    }

    boolean hasItemGiven(String id) { return givenItems.contains(id); }
    void markItemGiven(String id) { givenItems.add(id); }

    String getRandomUngivenReward() {
        String[] allRewards = {"os", "pomme_rouge", "bottes_sept_lieues", "ongle_cyclope"};
        java.util.List<String> available = new java.util.ArrayList<>();
        for (String r : allRewards) { if (!givenItems.contains(r)) available.add(r); }
        if (available.isEmpty()) return null;
        return available.get(random.nextInt(available.size()));
    }

    ItemStack createReward(String id) {
        var custom = com.dungeonmod.ModItems.get(id);
        if (custom != null) return custom.createStack();
        var key = Identifier.of("dungeonmod", id);
        var item = Registries.ITEM.get(key);
        if (item == net.minecraft.item.Items.AIR) return ItemStack.EMPTY;
        return new ItemStack(item);
    }

    // ---------- GeckoLib controller (override : walk plus stable) ----------

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar registrar) {
        final int[] prevAttack = {0};
        final int[] prevPhase = {-1};
        final boolean[] wasMoving = {false};
        registrar.add(new AnimationController<>(this, "main", 2, state -> {
            int phase = getPhase();
            int attack = getAttackState();

            if (phase == BossPhase.DEAD && deathStage == 3) {
                prevPhase[0] = 4;
                return state.setAndContinue(CyclopsAttack.IDLE.toRaw());
            }
            if (phase == BossPhase.IDLE && prevPhase[0] != BossPhase.IDLE) {
                prevPhase[0] = BossPhase.IDLE; prevAttack[0] = 0;
                return state.setAndContinue(CyclopsAttack.IDLE.toRaw());
            }
            if (phase == BossPhase.IDLE) return PlayState.CONTINUE;
            if (phase == BossPhase.WELCOME && prevPhase[0] != BossPhase.WELCOME) {
                prevPhase[0] = BossPhase.WELCOME; prevAttack[0] = 0;
                return state.setAndContinue(CyclopsAttack.WELCOME.toRaw());
            }
            if (phase == BossPhase.WELCOME) return PlayState.CONTINUE;
            if (phase == BossPhase.DEAD && prevPhase[0] != BossPhase.DEAD) {
                prevPhase[0] = BossPhase.DEAD; prevAttack[0] = 0;
                return state.setAndContinue(CyclopsAttack.DEAD.toRaw());
            }
            if (phase == BossPhase.DEAD) return PlayState.CONTINUE;

            // COMBAT
            if (attack != 0 && attack != prevAttack[0]) {
                prevAttack[0] = attack; prevPhase[0] = BossPhase.COMBAT;
                CyclopsAttack atk = CyclopsAttack.fromCode(attack);
                return state.setAndContinue(atk == null ? CyclopsAttack.WALK.toRaw() : atk.toRaw());
            }
            if (attack == 0) {
                prevAttack[0] = 0; prevPhase[0] = BossPhase.COMBAT;
                boolean moving = this.getVelocity().horizontalLengthSquared() > 0.0001 || state.isMoving();
                if (moving && !wasMoving[0]) {
                    wasMoving[0] = true;
                    return state.setAndContinue(CyclopsAttack.WALK.toRaw());
                }
                if (!moving && wasMoving[0]) {
                    wasMoving[0] = false;
                    return PlayState.STOP;
                }
                if (moving) return PlayState.CONTINUE;
                return PlayState.STOP;
            }
            return PlayState.CONTINUE;
        }).setAnimationSpeed(1.0f));
    }

    // ---------- NBT : étend la version de la base ----------

    @Override
    public void writeCustomDataToNbt(net.minecraft.nbt.NbtCompound nbt) {
        super.writeCustomDataToNbt(nbt);
        nbt.putBoolean("throwTestMode", throwTestMode);
        nbt.putInt("deathStage", deathStage);
        nbt.putInt("clothsGiven", clothsGiven);
        nbt.putBoolean("hasTalked", hasTalked);
        var givenList = new net.minecraft.nbt.NbtList();
        for (String id : givenItems) { givenList.add(net.minecraft.nbt.NbtString.of(id)); }
        nbt.put("givenItems", givenList);
        var usedList = new net.minecraft.nbt.NbtList();
        for (int idx : usedTradeIndices) { usedList.add(net.minecraft.nbt.NbtInt.of(idx)); }
        nbt.put("usedTradeIndices", usedList);
    }

    @Override
    public void readCustomDataFromNbt(net.minecraft.nbt.NbtCompound nbt) {
        super.readCustomDataFromNbt(nbt);
        if (nbt.contains("throwTestMode")) throwTestMode = nbt.getBoolean("throwTestMode");
        if (nbt.contains("deathStage")) deathStage = nbt.getInt("deathStage");
        else if (nbt.contains("returnStage")) deathStage = nbt.getInt("returnStage"); // compatibilité
        if (nbt.contains("clothsGiven")) clothsGiven = nbt.getInt("clothsGiven");
        if (nbt.contains("hasTalked")) hasTalked = nbt.getBoolean("hasTalked");
        if (nbt.contains("givenItems")) {
            givenItems.clear();
            for (var t : nbt.getList("givenItems", net.minecraft.nbt.NbtElement.STRING_TYPE)) givenItems.add(t.asString());
        }
        if (nbt.contains("usedTradeIndices")) {
            usedTradeIndices.clear();
            for (var t : nbt.getList("usedTradeIndices", net.minecraft.nbt.NbtElement.INT_TYPE)) usedTradeIndices.add(((net.minecraft.nbt.NbtInt)t).intValue());
        }
    }

    // ---------- Synchro room (public pour TestGenerator / spawn helpers) ----------

    public void setRoom(int minX, int maxX, int minZ, int maxZ, float facing) {
        this.roomMinX = minX; this.roomMaxX = maxX;
        this.roomMinZ = minZ; this.roomMaxZ = maxZ;
        this.roomFacing = facing;
        this.room = new com.dungeonmod.entity.boss.BossRoom(minX, maxX, minZ, maxZ, facing);
    }

    // ---------- AttackGoal : utilise pickCombo de BossHasCombos ----------

    class AttackGoal extends Goal {
        @Override
        public boolean canStart() {
            if (getPhase() != BossPhase.COMBAT) return false;
            if (getAttackState() != 0) return false;
            if (comboTimer > 0) return false;
            if (currentCombo != null) return false;
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
            LivingEntity t = getTarget();
            if (t == null) return;
            attackTarget = t;
            double dist = squaredDistanceTo(t);
            int[] combo;
            if (dist > 25.0 && dist <= 144.0) {
                combo = random.nextBoolean() ? COMBO_TC : COMBO_TH;
            } else if (dist > 9.0 && dist <= 25.0) {
                combo = COMBO_CH;
            } else {
                combo = random.nextBoolean() ? COMBO_TC : COMBO_TH;
            }
            startComboFromCodes(combo);
        }

        @Override public boolean shouldContinue() { return false; }
    }

    public static void registerAttributes() {
        net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry.register(TYPE,
            PathAwareEntity.createMobAttributes()
                .add(EntityAttributes.MAX_HEALTH, 400.0)  // 200 coeurs
                .add(EntityAttributes.ATTACK_DAMAGE, 4.0)
                .add(EntityAttributes.FOLLOW_RANGE, 16.0)
                .add(EntityAttributes.MOVEMENT_SPEED, 0.2));
    }
}
