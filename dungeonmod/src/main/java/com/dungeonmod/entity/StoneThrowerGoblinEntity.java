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
    private BlockPos ladderTopPos;
    /** Phase de retour : 0=sol, 1=montee echelle, 2=sortie (impulsion), 3=atterrissage */
    private int climbPhase = 0;

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
        boolean onPlatform = this.isOnPlatform();

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
                        }                    }
                }
            }
        }

        if (bestLadder != null) {
            climbingLadder = true;
            ladderBlockPos = bestLadder;
            ladderTopPos = bestTop;
            // Descendre a la BASE de l'echelle (le standPos doit etre au sol)
            BlockPos base = bestLadder;
            while (this.getWorld().getBlockState(base.down()).isIn(BlockTags.CLIMBABLE)) {
                base = base.down();
            }
            System.out.println("[Lanceur] echelle trouvee a " + bestLadder + " (base=" + base
                + " haut=" + bestTop + ")");
            // Calculer la position pile devant la face grimpable (a la base, au sol)
            BlockState ladderState = this.getWorld().getBlockState(base);
            Direction facing = ladderState.contains(Properties.HORIZONTAL_FACING)
                ? ladderState.get(Properties.HORIZONTAL_FACING) : Direction.NORTH;
            Direction standDir = facing.getOpposite();
            BlockPos standPos = new BlockPos(
                base.getX() + standDir.getOffsetX(),
                base.getY(),
                base.getZ() + standDir.getOffsetZ()
            );
            // Si le standPos est occupé, essayer l'autre côté
            if (!this.getWorld().getBlockState(standPos).isAir() || !this.getWorld().getBlockState(standPos.up()).isAir()) {
                BlockPos otherSide = new BlockPos(
                    base.getX() + facing.getOffsetX(),
                    base.getY(),
                    base.getZ() + facing.getOffsetZ()
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
            System.out.println("[Lanceur] AUCUNE echelle trouvee (pos=" + this.getBlockPos()
                + " platformY=" + platformPos.getY() + ")");
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

    // Goal prioritaire : retour à la plateforme par l'échelle (3 phases déterministes)
    static class ReturnToPlatformGoal extends Goal {
        private final StoneThrowerGoblinEntity goblin;

        ReturnToPlatformGoal(StoneThrowerGoblinEntity goblin) {
            this.goblin = goblin;
            this.setControls(EnumSet.of(Control.MOVE, Control.LOOK));
        }

        @Override
        public boolean canStart() {
            boolean start = goblin.platformPos != null && !goblin.isOnPlatform() && goblin.isOnGround();
            if (start && goblin.age % 40 == 0) {
                System.out.println("[Lanceur] ReturnToPlatform canStart: pos=" + goblin.getBlockPos()
                    + " platform=" + goblin.platformPos + " onPlatform=" + goblin.isOnPlatform());
            }
            return start;
        }

        @Override
        public void start() {
            goblin.setTarget(null);
            goblin.climbPhase = 0;
            goblin.findAndGoToLadder();
        }

        @Override
        public void tick() {
            if (goblin.platformPos == null) return;
            if (goblin.isOnPlatform()) { finish(); return; }

            boolean touchingLadder = goblin.getWorld().getBlockState(goblin.getBlockPos()).isIn(BlockTags.CLIMBABLE);

            switch (goblin.climbPhase) {
                case 0: // Phase sol : marcher jusqu'à la base de l'échelle
                    if (goblin.ladderTarget != null) {
                        if (!goblin.getNavigation().isFollowingPath()) {
                            goblin.getNavigation().startMovingTo(
                                goblin.ladderTarget.getX(), goblin.ladderTarget.getY(), goblin.ladderTarget.getZ(), 1.0);
                        }
                        double d = goblin.squaredDistanceTo(Vec3d.ofCenter(goblin.ladderTarget));
                        if (d < 1.5 || touchingLadder) {
                            goblin.climbPhase = 1;
                            System.out.println("[Lanceur] Phase 1 (montee): pres de l'echelle d=" + d);
                        }
                    } else if (goblin.age % 40 == 0) {
                        System.out.println("[Lanceur] Phase 0 mais ladderTarget null, re-scan...");
                        goblin.findAndGoToLadder();
                    }
                    break;

                case 1: // Phase montée : viser au-dessus du haut de l'échelle, le pathfinding grimpe
                    if (goblin.ladderTopPos != null) {
                        goblin.getNavigation().startMovingTo(
                            goblin.ladderTopPos.getX(), goblin.ladderTopPos.getY() + 1, goblin.ladderTopPos.getZ(), 1.0);
                        goblin.getLookControl().lookAt(
                            goblin.ladderTopPos.getX() + 0.5, goblin.ladderTopPos.getY() + 2,
                            goblin.ladderTopPos.getZ() + 0.5, 30, 30);
                    }
                    if (touchingLadder && goblin.getY() < goblin.platformPos.getY() - 0.5) {
                        goblin.setVelocity(goblin.getVelocity().x, 0.2, goblin.getVelocity().z);
                        goblin.velocityModified = true;
                    }
                    // Sortie : a la bonne hauteur (dans l'echelle OU juste au-dessus apres etre sorti par le haut)
                    if (goblin.getY() >= goblin.platformPos.getY() - 0.5) {
                        goblin.climbPhase = 2;
                        System.out.println("[Lanceur] Phase 2 (sortie): y=" + goblin.getY()
                            + " platformY=" + goblin.platformPos.getY());
                    }
                    break;

                case 2: // Phase sortie : UNE impulsion vers la dalle solide la plus proche
                    Vec3d bestDir = null;
                    double bestDist = Double.MAX_VALUE;
                    BlockPos bp = goblin.getBlockPos();
                    for (int dx = -2; dx <= 2; dx++) {
                        for (int dz = -2; dz <= 2; dz++) {
                            if (dx == 0 && dz == 0) continue;
                            BlockPos pos = new BlockPos(bp.getX() + dx, goblin.platformPos.getY() - 1, bp.getZ() + dz);
                            BlockState st = goblin.getWorld().getBlockState(pos);
                            if (st.isAir()) continue;
                            if (st.getBlock() instanceof net.minecraft.block.TrapdoorBlock) continue;
                            if (!goblin.getWorld().getBlockState(pos.up()).isAir()) continue;
                            double d = goblin.squaredDistanceTo(Vec3d.ofCenter(pos));
                            if (d < bestDist) {
                                bestDist = d;
                                bestDir = new Vec3d(pos.getX() + 0.5 - goblin.getX(), 0, pos.getZ() + 0.5 - goblin.getZ()).normalize();
                            }
                        }
                    }
                    if (bestDir != null) {
                        goblin.setVelocity(bestDir.x * 0.35, 0.12, bestDir.z * 0.35);
                        goblin.velocityModified = true;
                    }
                    goblin.climbPhase = 3;
                    break;

                case 3: // Phase atterrissage : navigation douce vers le centre
                    goblin.getNavigation().startMovingTo(
                        goblin.platformPos.getX() + 0.5, goblin.platformPos.getY(),
                        goblin.platformPos.getZ() + 0.5, 0.6);
                    break;
            }
        }

        private void finish() {
            goblin.climbingLadder = false;
            goblin.ladderTarget = null;
            goblin.ladderBlockPos = null;
            goblin.ladderTopPos = null;
            goblin.climbPhase = 0;
        }

        @Override
        public boolean shouldContinue() {
            return goblin.platformPos != null && !goblin.isOnPlatform();
        }

        @Override
        public void stop() {
            finish();
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
