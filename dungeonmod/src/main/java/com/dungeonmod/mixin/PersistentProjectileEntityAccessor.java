package com.dungeonmod.mixin;

import net.minecraft.entity.projectile.PersistentProjectileEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(PersistentProjectileEntity.class)
public interface PersistentProjectileEntityAccessor {

    @Invoker("setPierceLevel")
    void invokeSetPierceLevel(byte level);

    @Accessor("pickupType")
    void setPickupType(PersistentProjectileEntity.PickupPermission type);
}
