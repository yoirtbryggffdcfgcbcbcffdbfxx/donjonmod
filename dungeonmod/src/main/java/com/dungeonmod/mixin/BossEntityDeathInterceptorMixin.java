package com.dungeonmod.mixin;

import com.dungeonmod.entity.boss.BossEntity;
import com.dungeonmod.entity.boss.BossPhase;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.server.world.ServerWorld;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Mixin sur LivingEntity.applyDamage (protected, signature ServerWorld/DamageSource/float).
 * <p>C'est la méthode que LivingEntity.damage() appelle en interne pour appliquer
 * le dégât. Notre BossEntity.applyDamage fait la même chose mais intercepte
 * le coup fatal pour déclencher la phase DEAD.
 * <p>Ce mixin est un filet de sécurité :
 * <ul>
 *   <li>Si la santé tombe à 0 sans qu'on soit en phase DEAD → bascule DEAD.</li>
 *   <li>Si on est en phase DEAD et qu'un coup tente de nous tuer → on force
 *       setHealth(0.1f) pour rester en vie pendant la séquence.</li>
 *   <li>Si le joueur tape (clic gauche) sur le boss mort → dialogue (post-mortem).</li>
 * </ul>
 */
@Mixin(LivingEntity.class)
public class BossEntityDeathInterceptorMixin {

    @Inject(method = "applyDamage", at = @At("TAIL"))
    private void onApplyDamageTail(ServerWorld world, DamageSource source, float amount, CallbackInfo ci) {
        LivingEntity self = (LivingEntity) (Object) this;
        if (!(self instanceof BossEntity boss)) return;

        if (boss.getPhase() == BossPhase.DEAD) {
            if (boss.getHealth() <= 0.01f) {
                boss.setHealth(0.1f);
            }
            // Post-mortem : clic gauche du joueur (self-hit) → dialogue
            if (source.getAttacker() instanceof net.minecraft.entity.player.PlayerEntity attacker
                && source.getAttacker() == source.getSource()) {
                boss.onPostMortemHit(attacker);
            }
            return;
        }

        if (boss.getHealth() <= 0.01f) {
            boss.triggerDeathSequence();
        }
    }
}
