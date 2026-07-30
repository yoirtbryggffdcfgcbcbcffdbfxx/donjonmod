package com.dungeonmod.mixin;

import com.dungeonmod.entity.boss.BossEntity;
import com.dungeonmod.entity.boss.BossPhase;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.server.world.ServerWorld;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Filet de sécurité pour la mort des boss. S'injecte à TAIL de
 * damage(ServerWorld, DamageSource, float) — la seule méthode
 * damage() dans LivingEntity en 1.21.4 yarn.
 */
@Mixin(LivingEntity.class)
public class BossEntityDeathInterceptorMixin {

    @Inject(method = "damage", at = @At("TAIL"))
    private void onDamageTail(ServerWorld world, DamageSource source, float amount, CallbackInfoReturnable<Boolean> cir) {
        LivingEntity self = (LivingEntity) (Object) this;
        if (!(self instanceof BossEntity boss)) return;

        if (!world.isClient()) {
            System.out.println("[Cyclops-mixin] damage TAIL phase=" + boss.getPhase() + " hp=" + boss.getHealth() + " amount=" + amount + " attacker=" + source.getAttacker() + " source=" + source.getSource());
        }

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
