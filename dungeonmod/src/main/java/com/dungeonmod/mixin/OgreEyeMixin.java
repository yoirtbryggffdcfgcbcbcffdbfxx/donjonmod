package com.dungeonmod.mixin;

import com.dungeonmod.entity.OgreEntity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.player.PlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LivingEntity.class)
public class OgreEyeMixin {

    @Inject(method = "damage", at = @At("HEAD"))
    private void onDamage(net.minecraft.server.world.ServerWorld world, DamageSource source, float amount, CallbackInfoReturnable<Boolean> cir) {
        LivingEntity self = (LivingEntity)(Object)this;
        if (!(self instanceof OgreEntity ogre)) return;
        if (self.getWorld().isClient()) return;

        Entity attacker = source.getAttacker();
        if (!(attacker instanceof PlayerEntity)) return;

        double headY = ogre.getY() + 3.5;

        // Melee : joueur à hauteur de tête et à moins de 3 blocs
        PlayerEntity player = (PlayerEntity) attacker;
        double dist = ogre.distanceTo(player);
        double playerY = player.getY() + 1.0;
        if (Math.abs(playerY - headY) < 1.5 && dist < 3.0) {
            ogre.onEyeHit(true);
            return;
        }

        // Projectile : l'entité source (flèche, trident, bâton, etc.) est à hauteur de tête
        Entity sourceEntity = source.getSource();
        if (sourceEntity != null && Math.abs(sourceEntity.getY() - headY) < 1.5) {
            ogre.onEyeHit(true);
        }
    }
}
