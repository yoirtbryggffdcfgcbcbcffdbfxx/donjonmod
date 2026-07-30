package com.dungeonmod.mixin;

import com.dungeonmod.entity.boss.BossEntity;
import com.dungeonmod.entity.boss.BossPhase;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Filet de sécurité pour la mort des boss. S'injecte à TAIL de
 * damage(DamageSource, float) — c'est la méthode héritée de Entity
 * (la 2-args) que Minecraft appelle via PlayerEntity.attack.
 * <p>Cas couverts :
 * <ul>
 *   <li>Si on est en phase DEAD et que la santé tombe à 0 → forcer setHealth(0.1f)
 *       pour rester en vie pendant la séquence.</li>
 *   <li>Si on n'est pas en phase DEAD mais que la santé est descendue à 0
 *       sans que damage() n'ait fait la transition → déclencher la séquence.</li>
 *   <li>Si le joueur clique (clic gauche, self-hit) sur un boss mort
 *       → déclencher le dialogue post-mortem.</li>
 * </ul>
 * <p>Note : on injecte sur la 2-args (héritée de Entity) parce que c'est
 * ce que Minecraft appelle. La 3-args (ServerWorld, DamageSource, float)
 * existe aussi dans LivingEntity mais n'est pas appelée directement.
 */
@Mixin(LivingEntity.class)
public class BossEntityDeathInterceptorMixin {

    @Inject(method = "damage(Lnet/minecraft/entity/damage/DamageSource;F)Z", at = @At("TAIL"))
    private void onDamageTail(DamageSource source, float amount, CallbackInfoReturnable<Boolean> cir) {
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
