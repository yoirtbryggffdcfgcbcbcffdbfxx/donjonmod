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
 *
 * Note: le clic-sinistre (clic gauche sur boss mort → dialogue) est
 * désormais géré directement dans BossEntity.damage() (cf. BaseNpcEntity).
 * Ce mixin ne sert plus que de filet de sécurité : si un dégât mortel
 * arrive sans passer par notre override (ex. dégât environnemental,
 * suffocation, etc.), il force quand même la transition vers DEAD.
 */
@Mixin(LivingEntity.class)
public class BossEntityDeathInterceptorMixin {

    @Inject(method = "damage", at = @At("TAIL"))
    private void onDamageTail(ServerWorld world, DamageSource source, float amount, CallbackInfoReturnable<Boolean> cir) {
        LivingEntity self = (LivingEntity) (Object) this;
        if (!(self instanceof BossEntity boss)) return;

        if (!world.isClient()) {
            System.out.println("[Cyclops-mixin] damage TAIL phase=" + boss.getPhase()
                + " deadPerm=" + boss.getDeadPermanent()
                + " hp=" + boss.getHealth() + " amount=" + amount
                + " attacker=" + source.getAttacker());
        }

        // Filet de sécurité : si HP <= 0.01f et qu'on n'est pas encore
        // en phase DEAD, forcer la transition. C'est un cas dégénéré
        // (devrait déjà être attrapé par l'override BossEntity.damage()).
        if (boss.getHealth() <= 0.01f && boss.getPhase() != BossPhase.DEAD) {
            boss.triggerDeathSequence();
        }
    }
}
