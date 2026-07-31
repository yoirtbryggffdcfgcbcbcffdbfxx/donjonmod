package com.dungeonmod.mixin;

import com.dungeonmod.util.AncreGrappling;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.world.ServerWorld;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Immunite grappin : apres avoir traverse une entite avec l'ancre, le joueur
 * ignore les degats de cette entite pendant 0.5 seconde.
 */
@Mixin(PlayerEntity.class)
public class AncreImmunityMixin {

    @Inject(method = "damage(Lnet/minecraft/server/world/ServerWorld;Lnet/minecraft/entity/damage/DamageSource;F)Z",
            at = @At("HEAD"), cancellable = true)
    private void onDamageImmunity(ServerWorld world, DamageSource source, float amount,
                                  CallbackInfoReturnable<Boolean> cir) {
        if (AncreGrappling.isImmuneTo(source.getAttacker())) {
            cir.setReturnValue(false);
        }
    }
}
