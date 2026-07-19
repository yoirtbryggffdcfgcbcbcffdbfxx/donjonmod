package com.dungeonmod.mixin;

import com.dungeonmod.entity.OgreEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.Vec3d;
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
        if (!(source.getAttacker() instanceof PlayerEntity player)) return;
        if (self.getWorld().isClient()) return;

        // Check if player is roughly at the cyclops's head height and within melee range (3 blocks)
        double headY = ogre.getY() + 3.5;
        double playerY = player.getY() + 1.0; // player eye height
        double dist = ogre.distanceTo(player);

        // Simple check: player at head height (±1 block) and close enough
        if (Math.abs(playerY - headY) < 1.5 && dist < 3.0) {
            ogre.onEyeHit(true);
        }
    }
}
