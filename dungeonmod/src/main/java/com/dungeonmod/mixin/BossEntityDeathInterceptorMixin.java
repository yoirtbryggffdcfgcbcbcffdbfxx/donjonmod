package com.dungeonmod.mixin;

import com.dungeonmod.entity.boss.BossEntity;
import net.minecraft.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Filet de sécurité : à la fin de chaque tick de toute entité qui est
 * un BossEntity, si sa santé est tombée à 0 (ou en dessous) et qu'on
 * n'est pas déjà en phase DEAD, on déclenche la séquence de mort
 * scénarisée.
 * <p>Pourquoi : d'autres mixins (ComboMixin, BackstabMixin, …) s'injectent
 * sur LivingEntity.damage() et peuvent modifier le flux avant que
 * l'override BossEntity.damage() ne s'exécute. Ce mixin tick-based est
 * exécuté après damage() et lit l'état final, donc il attrape tous les
 * cas où la santé a atteint 0 sans que la bascule en phase DEAD n'ait
 * été faite.
 */
@Mixin(LivingEntity.class)
public class BossEntityDeathInterceptorMixin {

    @Inject(method = "tick", at = @At("TAIL"))
    private void onTick(CallbackInfo ci) {
        LivingEntity self = (LivingEntity) (Object) this;
        if (!(self instanceof BossEntity boss)) return;

        if (boss.getHealth() <= 0.01f) {
            boss.triggerDeathSequence();
        }
    }
}
