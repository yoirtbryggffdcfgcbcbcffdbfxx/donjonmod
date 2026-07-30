package com.dungeonmod.entity.boss;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.boss.BossBar;
import net.minecraft.entity.boss.ServerBossBar;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.data.TrackedData;
import net.minecraft.entity.data.TrackedDataHandlerRegistry;
import net.minecraft.entity.mob.PathAwareEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvent;
import net.minecraft.text.Text;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.PlayState;
import software.bernie.geckolib.animation.RawAnimation;
import software.bernie.geckolib.util.GeckoLibUtil;

/**
 * Base abstraite pour tous les boss du mod.
 *
 * <h2>Responsabilités centralisées</h2>
 * <ul>
 *   <li>Machine à états de phase (IDLE / WELCOME / COMBAT / DEAD)</li>
 *   <li>Ancrage à une salle (AABB 2D + facingYaw)</li>
 *   <li>Bossbar dynamique (couleur selon vulnérabilité, visibilité selon room)</li>
 *   <li>Persistance NBT des champs communs</li>
 *   <li>Squelette d'animation GeckoLib (dispatch phase + attack → RawAnimation)</li>
 * </ul>
 *
 * <h2>Extension par capabilities</h2>
 * Les comportements <i>optionnels</i> (combos, points faibles, mort scénarisée,
 * invocations, post-mortem PNJ) sont branchés par interfaces dans
 * {@code boss.capability} : chaque entité concrète implémente celles qu'elle
 * souhaite. La base ne force rien.
 *
 * <h2>Cycle de vie d'un tick</h2>
 * <pre>
 *   super.tick()
 *   updateBossBar()           ← déclenche WELCOME si un joueur entre
 *   switch (phase) {
 *     IDLE    → tickIdle()    (overridable)
 *     WELCOME → tickWelcome() (overridable)
 *     COMBAT  → tickCombat()  (overridable, ex. dispatch vers capabilities)
 *     DEAD    → tickDeath()   (overridable, ex. BossHasDeathSequence)
 *   }
 * </pre>
 */
public abstract class BossEntity extends PathAwareEntity implements GeoEntity {

    /** Phase courante, synchronisée client/serveur. Voir {@link BossPhase}. */
    protected static final TrackedData<Integer> PHASE =
        DataTracker.registerData(BossEntity.class, TrackedDataHandlerRegistry.INTEGER);

    /** Attaque ou posture en cours (0 = aucune). Sémantique propre à chaque boss. */
    protected static final TrackedData<Integer> ATTACK_STATE =
        DataTracker.registerData(BossEntity.class, TrackedDataHandlerRegistry.INTEGER);

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
    protected ServerBossBar bossBar;
    protected BossRoom room = new BossRoom(0, 0, 0, 0, 0f);
    protected long nextAttackTime = 100;
    protected boolean deadPermanent = false;
    protected boolean hasPlayedWelcome = false;
    protected int animTimer = 0;

    protected BossEntity(EntityType<? extends PathAwareEntity> type, World world) {
        super(type, world);
    }

    // ---------- Hooks à implémenter par les sous-classes ----------

    /** Nom affiché dans la bossbar (ex. "§eCyclope"). */
    protected abstract Text getBossBarName();

    /** Son joué quand un joueur entre dans la salle. */
    protected abstract SoundEvent getWelcomeSound();

    /** Message broadcast au joueur qui entre (vide ou null = rien). */
    protected abstract Text getWelcomeMessage();

    /** Animation d'idle (par défaut : boucle "idle"). */
    protected abstract BossAnimation getIdleAnimation();

    /** Animation de marche (par défaut : boucle "walk"). */
    protected abstract BossAnimation getWalkAnimation();

    /** Durée de la phase WELCOME en ticks, avant bascule en COMBAT. */
    protected int welcomeDurationTicks() { return 40; }

    /** Cooldown minimum après un combo/attaque, en ticks. */
    protected int postAttackCooldownTicks() { return 20 + random.nextInt(41); }

    // ---------- TrackedData / phases ----------

    @Override
    protected void initDataTracker(DataTracker.Builder builder) {
        super.initDataTracker(builder);
        builder.add(PHASE, BossPhase.IDLE);
        builder.add(ATTACK_STATE, 0);
    }

    public int getPhase() { return dataTracker.get(PHASE); }
    public void setPhase(int v) { dataTracker.set(PHASE, v); }
    public int getAttackState() { return dataTracker.get(ATTACK_STATE); }
    public void setAttackState(int v) {
        dataTracker.set(ATTACK_STATE, v);
        animTimer = (v == 0) ? 0 : 1;
    }
    public boolean getDeadPermanent() { return deadPermanent; }
    public void setDeadPermanent(boolean v) { this.deadPermanent = v; }
    public boolean isAnimating() {
        return getAttackState() != 0
            || getPhase() == BossPhase.WELCOME
            || getPhase() == BossPhase.DEAD;
    }

    // ---------- Bossbar ----------

    @Override
    public void onStartedTrackingBy(ServerPlayerEntity player) {
        super.onStartedTrackingBy(player);
        if (bossBar == null) {
            bossBar = new ServerBossBar(getBossBarName(), BossBar.Color.YELLOW, BossBar.Style.PROGRESS);
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

    // ---------- Interception des dégâts (coup fatal → phase DEAD au lieu de remove) ----------

    /**
     * Hook de multiplicateur de dégâts : appliqué au montant AVANT la réduction.
     * Ex. le Cyclope l'override pour faire ×4 quand l'œil vient d'être frappé.
     */
    protected float damageMultiplier(DamageSource source, float amount) { return 1.0f; }

    /** Hook de réduction (0.5 = -50 %). Ex. le Cyclope pendant l'anim œil caché. */
    protected float damageReduction(DamageSource source, float amount) { return 1.0f; }

    /**
     * Override du damage vanilla (3 args en 1.21.4 : ServerWorld, DamageSource, float).
     * <p>Comportement :
     * <ul>
     *   <li>Si le boss est en phase DEAD (ou déjà mort permanent) : pas de dégât,
     *       mais clic gauche d'un joueur (auto-hit) déclenche {@code onPostMortemHit}
     *       pour ouvrir le dialogue (cf. {@code BaseNpcEntity.damage}).</li>
     *   <li>Si le coup est fatal : on ne meurt pas, on bascule en phase DEAD
     *       (via {@code triggerDeathSequence()}, mort scénarisée via
     *       {@code tickDeath()}) au lieu de laisser Minecraft retirer l'entité
     *       immédiatement.</li>
     *   <li>Sinon : on délègue à {@code super.damage()} après application des
     *       modificateurs custom {@code damageMultiplier} / {@code damageReduction}.</li>
     * </ul>
     */
    @Override
    public boolean damage(net.minecraft.server.world.ServerWorld world, DamageSource source, float amount) {
        // Phase DEAD : pas de dégât, mais clic gauche du joueur → dialogue (comme BaseNpcEntity).
        if (deadPermanent || getPhase() == BossPhase.DEAD) {
            // Clic gauche d'un joueur (auto-hit) sur le boss mort = dialogue.
            // On l'appelle inconditionnellement (même avant la fin de la cinématique
            // de mort) pour que ça marche dès que la phase passe en DEAD.
            if (source.getAttacker() instanceof PlayerEntity attacker
                && source.getAttacker() == source.getSource()) {
                onPostMortemHit(attacker);
            }
            return false;
        }

        // Application des modificateurs custom du boss.
        amount *= damageMultiplier(source, amount);
        amount *= damageReduction(source, amount);

        // Si le coup est fatal : on ne meurt pas, on bascule en phase DEAD.
        // On délègue à triggerDeathSequence() pour garder une seule source de vérité.
        if (this.getHealth() - amount <= 0.01f) {
            triggerDeathSequence();
            onFatalHit(source);
            return false;
        }
        return super.damage(world, source, amount);
    }

    /** Appelé quand le boss vient d'être tué (avant la phase DEAD). */
    protected void onFatalHit(DamageSource source) { }

    /** Appelé quand un joueur frappe un boss déjà mort (post-mortem). */
    public void onPostMortemHit(PlayerEntity attacker) { }

    /**
     * Bascule le boss en phase DEAD (mort scénarisée via {@code tickDeath()}) au lieu
     * de laisser Minecraft le retirer. Idempotent : safe à appeler plusieurs fois.
     * <p>Appelé depuis {@code damage()} sur coup fatal, ou depuis un filet de sécurité
     * externe (ex. dégât environnemental qui by-pass notre override).
     */
    public void triggerDeathSequence() {
        if (deadPermanent || getPhase() == BossPhase.DEAD) return;
        this.setHealth(0.1f);
        this.setPhase(BossPhase.DEAD);
        this.animTimer = 0;
        this.setInvulnerable(true);
        this.getNavigation().stop();
        if (bossBar != null) bossBar.clearPlayers();
    }

    // ---------- Tick : dispatch par phase ----------

    @Override
    public void tick() {
        super.tick();
        updateBossBar();
        int phase = getPhase();
        if      (phase == BossPhase.IDLE)    tickIdle();
        else if (phase == BossPhase.WELCOME) tickWelcome();
        else if (phase == BossPhase.COMBAT)  tickCombat();
        else if (phase == BossPhase.DEAD)    tickDeath();
    }

    protected void updateBossBar() {
        if (bossBar == null) return;
        bossBar.setPercent(getMaxHealth() > 0 ? getHealth() / getMaxHealth() : 0f);

        boolean playerVisible = false;
        for (var p : getWorld().getPlayers()) {
            if (p instanceof ServerPlayerEntity sp) {
                if (room.contains(sp.getBlockX(), sp.getBlockZ())
                    && !deadPermanent
                    && getPhase() < BossPhase.DEAD) {
                    if (getPhase() == BossPhase.IDLE) {
                        setPhase(BossPhase.WELCOME);
                        animTimer = 1;
                        setInvulnerable(true);
                        if (!hasPlayedWelcome) {
                            hasPlayedWelcome = true;
                            Text msg = getWelcomeMessage();
                            if (msg != null && !msg.getString().isEmpty()) {
                                sp.sendMessage(msg, false);
                            }
                            sp.playSoundToPlayer(getWelcomeSound(), SoundCategory.HOSTILE, 1.0f, 1.0f);
                        }
                    }
                    if (sp.squaredDistanceTo(this) <= 100.0) playerVisible = true;
                }
            }
        }
        boolean invuln = getPhase() == BossPhase.WELCOME
                      || getPhase() == BossPhase.DEAD
                      || deadPermanent;
        bossBar.setColor(invuln ? BossBar.Color.WHITE : BossBar.Color.YELLOW);
        bossBar.setVisible(playerVisible);
    }

    /** IDLE : figé face au facingYaw. */
    protected void tickIdle() {
        getNavigation().stop();
        setVelocity(0, getVelocity().y, 0);
        float f = room.facingYaw();
        setBodyYaw(f); setHeadYaw(f); setYaw(f);
        prevBodyYaw = f; prevHeadYaw = f; prevYaw = f;
    }

    /** WELCOME : invulnérable pendant welcomeDurationTicks() puis bascule en COMBAT. */
    protected void tickWelcome() {
        animTimer++;
        getNavigation().stop();
        setVelocity(0, getVelocity().y, 0);
        if (animTimer >= welcomeDurationTicks()) {
            setPhase(BossPhase.COMBAT);
            animTimer = 0;
            setInvulnerable(false);
            nextAttackTime = age + 20;
        }
    }

    /**
     * COMBAT : no-op par défaut. Les sous-classes (ou leurs capabilities)
     * override pour brancher leurs mécaniques d'attaque.
     * <p>L'AI goal reste le canal privilégié pour démarrer les combos.
     */
    protected void tickCombat() { }

    /**
     * DEAD : mort scénarisée. Par défaut, gel simple après 60 ticks.
     * Les boss avec une vraie séquence implémentent {@code BossHasDeathSequence}.
     */
    protected void tickDeath() {
        getNavigation().stop();
        setVelocity(0, getVelocity().y, 0);
        if (!deadPermanent) {
            animTimer++;
            if (animTimer >= 60) {
                deadPermanent = true;
                setInvulnerable(true);
                setHealth(0.01f);
            }
        }
    }

    /** Helper : centre de la salle en Vec3d. */
    protected Vec3d roomCenter() {
        return new Vec3d(room.centerX() + 0.5, getY(), room.centerZ() + 0.5);
    }

    // ---------- NBT (champs communs) ----------

    @Override
    public void writeCustomDataToNbt(net.minecraft.nbt.NbtCompound nbt) {
        super.writeCustomDataToNbt(nbt);
        nbt.putLong("nextAttackTime", nextAttackTime);
        nbt.putInt("roomMinX", room.minX()); nbt.putInt("roomMaxX", room.maxX());
        nbt.putInt("roomMinZ", room.minZ()); nbt.putInt("roomMaxZ", room.maxZ());
        nbt.putFloat("roomFacing", room.facingYaw());
        nbt.putInt("phase", getPhase());
        nbt.putBoolean("dead", deadPermanent);
    }

    @Override
    public void readCustomDataFromNbt(net.minecraft.nbt.NbtCompound nbt) {
        super.readCustomDataFromNbt(nbt);
        if (nbt.contains("nextAttackTime")) {
            nextAttackTime = nbt.getLong("nextAttackTime");
            if (this.age - nextAttackTime < 0) nextAttackTime = 0;
        }
        if (nbt.contains("roomMinX")) {
            room = new BossRoom(
                nbt.getInt("roomMinX"), nbt.getInt("roomMaxX"),
                nbt.getInt("roomMinZ"), nbt.getInt("roomMaxZ"),
                nbt.getFloat("roomFacing")
            );
        }
        if (nbt.contains("dead")) {
            deadPermanent = nbt.getBoolean("dead");
            setPhase(deadPermanent ? BossPhase.DEAD : BossPhase.IDLE);
        }
        if (nbt.contains("phase")) setPhase(nbt.getInt("phase"));
    }

    // ---------- GeckoLib ----------

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() { return cache; }
    @Override
    public double getTick(Object object) { return this.age; }

    /**
     * Controller par défaut : dispatch phase + attack → animation.
     * <p>Les sous-classes peuvent ajouter des controllers secondaires
     * (ex. bras animés indépendamment du corps) en overriddant cette méthode
     * et en appelant super.registerControllers(registrar) en plus.
     */
    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar registrar) {
        final int[] prevAttack = {0};
        final int[] prevPhase = {-1};
        registrar.add(new AnimationController<>(this, "main", 2, state -> {
            int phase = getPhase();
            int attack = getAttackState();

            if (phase == BossPhase.DEAD && deadPermanent) {
                prevPhase[0] = phase;
                return state.setAndContinue(getIdleAnimation().toRaw());
            }
            if (phase != prevPhase[0]) {
                prevPhase[0] = phase;
                prevAttack[0] = 0;
                RawAnimation phaseAnim = phaseAnimation(phase);
                if (phaseAnim != null) return state.setAndContinue(phaseAnim);
            }
            if (attack != 0 && attack != prevAttack[0]) {
                prevAttack[0] = attack;
                RawAnimation atkAnim = attackAnimation(attack);
                if (atkAnim != null) return state.setAndContinue(atkAnim);
            }
            if (attack == 0) {
                prevAttack[0] = 0;
                boolean moving = this.getVelocity().horizontalLengthSquared() > 0.0001 || state.isMoving();
                if (moving) return state.setAndContinue(getWalkAnimation().toRaw());
                return PlayState.STOP;
            }
            return PlayState.CONTINUE;
        }).setAnimationSpeed(1.0f));
    }

    // Hooks d'animation : à override par chaque boss
    protected RawAnimation phaseAnimation(int phase) {
        if (phase == BossPhase.WELCOME) {
            // Par défaut on utilise l'idle pendant le welcome ; les boss peuvent override
            return getIdleAnimation().toRaw();
        }
        if (phase == BossPhase.DEAD) {
            // Override par BossHasDeathSequence si elle veut un cinematic
            return getIdleAnimation().toRaw();
        }
        return null;
    }

    /**
     * @param attackState code d'attaque (sémantique propre à chaque boss)
     * @return l'animation à jouer, ou null pour laisser GeckoLib continuer
     */
    protected RawAnimation attackAnimation(int attackState) {
        return null;
    }
}
