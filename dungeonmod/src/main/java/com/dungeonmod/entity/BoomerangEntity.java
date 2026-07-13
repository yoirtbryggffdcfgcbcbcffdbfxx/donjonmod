package com.dungeonmod.entity;

import com.dungeonmod.util.BoomerangHelper;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.FlyingItemEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.MovementType;
import net.minecraft.entity.SpawnGroup;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.data.TrackedData;
import net.minecraft.entity.data.TrackedDataHandlerRegistry;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

import java.util.ArrayList;
import java.util.List;

public class BoomerangEntity extends Entity implements FlyingItemEntity {

public static final EntityType<BoomerangEntity> BOOMERANG_TYPE = Registry.register(
    Registries.ENTITY_TYPE,
    Identifier.of("dungeonmod", "boomerang"),
    EntityType.Builder.<BoomerangEntity>create(BoomerangEntity::new, SpawnGroup.MISC)
        .dimensions(0.6f, 0.6f)
        .maxTrackingRange(64)
        .trackingTickInterval(1)
        .build(RegistryKey.of(Registries.ENTITY_TYPE.getKey(), Identifier.of("dungeonmod", "boomerang")))
);

    private static final int OUT_TICKS = 15;
    private static final double SPEED = 0.67;
    private static final double MAX_RANGE = 10.0;

    private int age;
    public int getAge() { return age; }
    private Vec3d direction;
    private Vec3d startPos;
    private boolean returning;
    private boolean done;
    private static final TrackedData<ItemStack> ITEM = DataTracker.registerData(BoomerangEntity.class, TrackedDataHandlerRegistry.ITEM_STACK);
    private LivingEntity cachedOwner;

    // --- Deferred processing (runs at END_SERVER_TICK to avoid entity index corruption) ---
    private record DeferredReturn(ServerWorld world, Vec3d pos, ItemStack stack) {}
    private static final List<DeferredReturn> PENDING_RETURN = new ArrayList<>();
    private static final List<BoomerangEntity> PENDING_DISCARD = new ArrayList<>();

    public static void processPending() {
        for (var e : PENDING_DISCARD) e.discard();
        PENDING_DISCARD.clear();
        for (var r : PENDING_RETURN) {
            r.world.spawnEntity(new net.minecraft.entity.ItemEntity(r.world, r.pos.x, r.pos.y, r.pos.z, r.stack));
        }
        PENDING_RETURN.clear();
    }

    // --- Constructors ---
    public BoomerangEntity(EntityType<? extends BoomerangEntity> type, World world) {
        super(type, world);
    }

    public BoomerangEntity(World world, LivingEntity owner, ItemStack stack) {
        this(BOOMERANG_TYPE, world);
        this.cachedOwner = owner;
        this.getDataTracker().set(ITEM, stack.copyWithCount(1));
        this.setPosition(owner.getX(), owner.getEyeY() - 0.1, owner.getZ());
        this.setNoGravity(true);
    }

    // --- Movement ---
    @Override
    public void tick() {
        if (done) return;

        if (age == 0) {
            direction = getVelocity().normalize();
            startPos = getPos();
        }

        if ((horizontalCollision || verticalCollision) && !returning) returning = true;
        if (!returning && startPos.subtract(getPos()).horizontalLength() >= MAX_RANGE) returning = true;

        age++;
        if (age > OUT_TICKS && !returning) returning = true;

        if (age > OUT_TICKS * 2 || (returning && cachedOwner != null && distanceTo(cachedOwner) < 1.5)) {
            finish();
            return;
        }

        Vec3d dir = returning ? direction.multiply(-1) : direction;
        setVelocity(dir.multiply(SPEED));
        move(MovementType.SELF, getVelocity());

        // Hit enemies sur les deux trajets (aller et retour)
        if (!getWorld().isClient()) {
            for (var e : getWorld().getOtherEntities(this, getBoundingBox().expand(0.3))) {
                if (e instanceof LivingEntity living && living != cachedOwner) {
                    living.damage((ServerWorld) getWorld(), getDamageSources().thrown(this, cachedOwner), 2.0f);
                    double dx = getX() - living.getX();
                    double dz = getZ() - living.getZ();
                    living.takeKnockback(0.1, dx, dz);
                    break;
                }
            }
        }
    }

    private void finish() {
        if (done) return;
        done = true;
        setVelocity(Vec3d.ZERO);

        if (!getWorld().isClient() && cachedOwner != null && cachedOwner instanceof net.minecraft.entity.player.PlayerEntity player && !player.isCreative()) {
            // Queue return to inventory
            var s = getStack();
            PENDING_RETURN.add(new DeferredReturn(
                (ServerWorld) getWorld(),
                cachedOwner.getPos().add(0, 0.5, 0),
                s.isEmpty() ? ItemStack.EMPTY : s.copyWithCount(1)
            ));
        }
        PENDING_DISCARD.add(this);
    }

    // --- FlyingItemEntity ---
    @Override
    public ItemStack getStack() { return getDataTracker().get(ITEM); }

    // --- Required Entity overrides ---
    @Override protected void initDataTracker(DataTracker.Builder builder) {
        builder.add(ITEM, ItemStack.EMPTY);
    }
    @Override protected void readCustomDataFromNbt(NbtCompound n) {}
    @Override protected void writeCustomDataToNbt(NbtCompound n) {}

    @Override
    public boolean damage(ServerWorld world, DamageSource source, float amount) {
        return false;
    }
}
