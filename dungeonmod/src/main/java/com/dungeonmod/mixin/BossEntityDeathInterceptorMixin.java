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
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Filet de sécurité pour la mort des boss. Deux points d'injection :
 *
 * <h3>1. {@code damage} à TAIL</h3>
 * Après application du dégât (peu importe par quel mixin), si la santé
 * de l'entité est tombée à 0 et qu'on n'est pas en phase DEAD, on
 * bascule en phase DEAD (déclenche la séquence scénarisée). Si on est
 * déjà en phase DEAD, on force {@code setHealth(0.1f)} pour éviter
 * que Minecraft ne voie 0 et ne retire l'entité avant que la séquence
 * n'ait le temps de jouer.
 *
 * <h3>2. {@code tick} à TAIL</h3>
 * Dernier filet : si l'entité est encore à 0 en fin de tick (parce que
 * le code vanilla a quand même atteint le check de mort), on retente
 * la bascule en phase DEAD.
 */
@Mixin(LivingEntity.class)
public class BossEntityDeathInterceptorMixin {

    @Inject(method = "damage", at = @At("TAIL"))
    private void onDamageTail(ServerWorld world, DamageSource source, float amount, CallbackInfoReturnable<Boolean> cir) {
        LivingEntity self = (LivingEntity) (Object) this;
        if (!(self instanceof BossEntity boss)) return;

        if (boss.getPhase() == BossPhase.DEAD) {
            if (boss.getHealth() <= 0.01f) {
                boss.setHealth(0.1f);
            }
            // Post-mortem : clic gauche du joueur (self-hit) → dialogue
            if (source.getAttacker() instanceof net.minecraft.entity.player.PlayerEntity attacker
                && source.getAttacker() == source.getSource()) {
                System.out.println("[Cyclops-debug] onPostMortemHit called, attacker=" + attacker.getName().getString() + " deathStage=" + ((com.dungeonmod.entity.OgreEntity) boss).deathStage);
                boss.onPostMortemHit(attacker);
            } else {
                System.out.println("[Cyclops-debug] phase DEAD but condition failed: attacker=" + source.getAttacker() + " source=" + source.getSource());
            }
            return;
        }

        if (boss.getHealth() <= 0.01f) {
            boss.triggerDeathSequence();
        }
    }

    @Inject(method = "tick", at = @At("TAIL"))
    private void onTick(CallbackInfo ci) {
        LivingEntity self = (LivingEntity) (Object) this;
        if (!(self instanceof BossEntity boss)) return;

        // Phase DEAD : empêcher l'entité de mourir pendant la séquence.
        if (boss.getPhase() == BossPhase.DEAD) {
            if (boss.getHealth() <= 0.01f) {
                boss.setHealth(0.1f);
            }
            return;
        }

        // Pas en phase DEAD mais santé très basse : on bascule.
        if (boss.getHealth() <= 0.01f) {
            boss.triggerDeathSequence();
        }
    }
}
