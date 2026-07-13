package com.dungeonmod.mixin;

import com.dungeonmod.util.EpeeBlockData;
import com.dungeonmod.util.EpeeHelper;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.Vec3d;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LivingEntity.class)
public class EpeeDamageMixin {

    @ModifyVariable(method = "damage", at = @At("HEAD"), argsOnly = true)
    private float reduceBlockingDamage(float amount, ServerWorld world, DamageSource source, float originalAmount) {
        if (!(((Object) this) instanceof PlayerEntity player)) return amount;
        if (!player.isUsingItem()) return amount;
        var stack = player.getActiveItem();
        if (!EpeeHelper.isEpee(stack)) return amount;

        EpeeBlockData data = EpeeBlockData.get(player);

        Entity attacker = source.getAttacker();
        if (attacker == null) {
            player.getWorld().playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.ITEM_SHIELD_BLOCK, SoundCategory.PLAYERS, 1.0f, 1.0f);
            return 0.0f;
        }

        Vec3d toAttacker = attacker.getPos().subtract(player.getPos()).normalize();

        boolean guardBreak = data.checkAndIncrement(player, toAttacker);

        if (guardBreak) {
            // Break guard
            player.stopUsingItem();
            data.startCooldown();

            // Larger knockback away from attacker
            Vec3d push = player.getPos().subtract(attacker.getPos()).normalize();
            player.addVelocity(push.x * 1.2, 0.4, push.z * 1.2);
            player.velocityModified = true;

            // Louder, higher-pitched sound
            player.getWorld().playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.ITEM_SHIELD_BREAK, SoundCategory.PLAYERS, 1.5f, 1.5f);

            // Still block the damage
            return 0.0f;
        }

        // Knockback 1 block on attacker
        if (attacker instanceof LivingEntity livingAttacker) {
            Vec3d kb = new Vec3d(livingAttacker.getX() - player.getX(), 0.0, livingAttacker.getZ() - player.getZ()).normalize();
            livingAttacker.addVelocity(kb.x * 0.5, 0.25, kb.z * 0.5);
            livingAttacker.velocityModified = true;
        }

        // Shield block sound
        player.getWorld().playSound(null, player.getX(), player.getY(), player.getZ(),
            SoundEvents.ITEM_SHIELD_BLOCK, SoundCategory.PLAYERS, 1.0f, 1.0f);

        return 0.0f;
    }

    @Inject(method = "takeKnockback", at = @At("HEAD"), cancellable = true)
    private void cancelKnockback(double strength, double x, double z, CallbackInfo ci) {
        if (!(((Object) this) instanceof PlayerEntity player)) return;
        if (!player.isUsingItem()) return;
        var stack = player.getActiveItem();
        if (EpeeHelper.isEpee(stack)) {
            ci.cancel();
        }
    }
}
