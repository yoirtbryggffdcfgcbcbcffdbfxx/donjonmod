package com.dungeonmod.entity;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.SpawnGroup;
import net.minecraft.entity.projectile.thrown.SnowballEntity;
import net.minecraft.item.Item;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.world.World;

public class StoneEntity extends SnowballEntity {

    public static final EntityType<StoneEntity> STONE_TYPE = Registry.register(
        Registries.ENTITY_TYPE,
        Identifier.of("dungeonmod", "stone"),
        EntityType.Builder.<StoneEntity>create(StoneEntity::new, SpawnGroup.MISC)
            .dimensions(0.25f, 0.25f)
            .maxTrackingRange(64)
            .trackingTickInterval(1)
            .build(RegistryKey.of(Registries.ENTITY_TYPE.getKey(), Identifier.of("dungeonmod", "stone")))
    );

    public static final EntityType<StoneEntity> CYCLOPS_STONE_TYPE = Registry.register(
        Registries.ENTITY_TYPE,
        Identifier.of("dungeonmod", "cyclops_stone"),
        EntityType.Builder.<StoneEntity>create(CyclopsStoneEntity::new, SpawnGroup.MISC)
            .dimensions(0.25f, 0.25f)
            .maxTrackingRange(64)
            .trackingTickInterval(1)
            .build(RegistryKey.of(Registries.ENTITY_TYPE.getKey(), Identifier.of("dungeonmod", "cyclops_stone")))
    );

    public static final RegistryKey<Item> STONE_ITEM_KEY = RegistryKey.of(
        Registries.ITEM.getKey(), Identifier.of("dungeonmod", "stone_projectile"));
    public static final Item STONE_ITEM = Registry.register(
        Registries.ITEM, STONE_ITEM_KEY,
        new Item(new Item.Settings().registryKey(STONE_ITEM_KEY))
    );

    public static final RegistryKey<Item> CYCLOPS_STONE_ITEM_KEY = RegistryKey.of(
        Registries.ITEM.getKey(), Identifier.of("dungeonmod", "cyclops_stone"));
    public static final Item CYCLOPS_STONE_ITEM = Registry.register(
        Registries.ITEM, CYCLOPS_STONE_ITEM_KEY,
        new Item(new Item.Settings().registryKey(CYCLOPS_STONE_ITEM_KEY))
    );

    private LivingEntity cachedOwner;

    public StoneEntity(EntityType<? extends StoneEntity> entityType, World world) {
        super(entityType, world);
    }

    @Override
    protected Item getDefaultItem() {
        return STONE_ITEM;
    }

    public void setOwner(LivingEntity owner) {
        this.cachedOwner = owner;
    }

    @Override
    public Entity getOwner() {
        return cachedOwner != null ? cachedOwner : super.getOwner();
    }

    public static class CyclopsStoneEntity extends StoneEntity {
        public CyclopsStoneEntity(EntityType<? extends StoneEntity> entityType, World world) {
            super(entityType, world);
        }
        @Override
        protected Item getDefaultItem() {
            return CYCLOPS_STONE_ITEM;
        }
    }

    @Override
    protected void onCollision(HitResult hitResult) {
        super.onCollision(hitResult);
        if (!this.getWorld().isClient() && this.getWorld() instanceof ServerWorld sw) {
            // Sound on any impact
            sw.playSound(null, this.getX(), this.getY(), this.getZ(), net.minecraft.sound.SoundEvents.BLOCK_STONE_BREAK, net.minecraft.sound.SoundCategory.HOSTILE, 1.0f, 1.2f);
            
            if (hitResult.getType() == HitResult.Type.ENTITY && hitResult instanceof EntityHitResult entityHit) {
                if (entityHit.getEntity() instanceof LivingEntity target && this.getOwner() instanceof LivingEntity owner) {
                    if (target != owner) {
                        target.damage(sw, this.getDamageSources().mobProjectile(this, owner), 4.0f);
                        // Particles on hit
                        sw.spawnParticles(net.minecraft.particle.ParticleTypes.CRIT, target.getX(), target.getY() + 1.0, target.getZ(), 15, 0.5, 0.5, 0.5, 0.1);
                    }
                }
            }
            this.discard();
        }
    }
}
