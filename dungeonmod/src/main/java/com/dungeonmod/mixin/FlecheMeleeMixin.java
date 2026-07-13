package com.dungeonmod.mixin;

import com.dungeonmod.util.FlecheComboData;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.world.ServerWorld;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PlayerEntity.class)
public class FlecheMeleeMixin {

    @Inject(method = "attack", at = @At("HEAD"), cancellable = true)
    private void onAttack(Entity target, CallbackInfo ci) {
        PlayerEntity player = (PlayerEntity) (Object) this;
        if (player.getWorld().isClient()) return;
        var stack = player.getMainHandStack();
        if (!FlecheComboData.isFleche(stack)) return;
        if (!(target instanceof LivingEntity living)) return;

        if (player.getAttackCooldownProgress(0.5f) < 1.0f) {
            ci.cancel();
            return;
        }

        if (player.squaredDistanceTo(living) > 7.84) return;

        var world = player.getWorld();

        FlecheComboData.checkExpired(stack);

        if (FlecheComboData.isSangActive(stack)) {
            int level = FlecheComboData.getSangLevel(stack);
            com.dungeonmod.DungeonMod.LOGGER.info("Fleche: Sang active, level=" + level + ", alive=" + living.isAlive());
            living.damage((ServerWorld) world, player.getDamageSources().playerAttack(player), 2.0f);

            if (level == 2 && living.isAlive() && !living.hasStatusEffect(StatusEffects.WITHER)) {
                living.addStatusEffect(new StatusEffectInstance(StatusEffects.WITHER, 60, 1, false, false, true));
            }

            if (level == 1 && !living.isAlive()) {
                FlecheComboData.enterSang2(stack);
            }
        } else {
            FlecheComboData data = FlecheComboData.getCombo(player);
            long now = System.currentTimeMillis();

            if (now - data.lastHitTime > 2000) {
                data.hits = 0;
            }
            data.hits++;
            data.lastHitTime = now;

            com.dungeonmod.DungeonMod.LOGGER.info("Fleche: Combo hits=" + data.hits);

            living.damage((ServerWorld) world, player.getDamageSources().playerAttack(player), 1.0f);

            if (data.hits >= 3) {
                FlecheComboData.enterSang1(stack);
                com.dungeonmod.DungeonMod.LOGGER.info("Fleche: enterSang1 called");
                data.hits = 0;
            }
        }

        double dx = player.getX() - living.getX();
        double dz = player.getZ() - living.getZ();
        living.takeKnockback(0.1, dx, dz);

        player.swingHand(player.getActiveHand());
        player.resetLastAttackedTicks();
        ci.cancel();
    }
}
