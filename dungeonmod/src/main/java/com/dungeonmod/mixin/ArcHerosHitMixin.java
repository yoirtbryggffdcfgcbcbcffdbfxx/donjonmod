package com.dungeonmod.mixin;

import com.dungeonmod.util.FlecheComboData;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.projectile.ArrowEntity;
import net.minecraft.entity.projectile.PersistentProjectileEntity;
import net.minecraft.util.hit.EntityHitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PersistentProjectileEntity.class)
public class ArcHerosHitMixin {

    @Inject(method = "onEntityHit", at = @At("HEAD"))
    private void onEntityHit(EntityHitResult hitResult, CallbackInfo ci) {
        if (!(((Object) this) instanceof ArrowEntity arrow)) return;
        if (!FlecheComboData.SANG2_ARROWS.remove(arrow.getUuid())) return;
        if (!(hitResult.getEntity() instanceof LivingEntity living)) return;
        if (!living.hasStatusEffect(StatusEffects.WITHER)) {
            living.addStatusEffect(new StatusEffectInstance(StatusEffects.WITHER, 60, 1, false, false, true));
        }
    }
}
