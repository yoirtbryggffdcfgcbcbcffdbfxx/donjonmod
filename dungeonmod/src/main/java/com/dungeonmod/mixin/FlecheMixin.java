package com.dungeonmod.mixin;

import com.dungeonmod.util.FlecheComboData;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.ArrowEntity;
import net.minecraft.entity.projectile.PersistentProjectileEntity;
import net.minecraft.util.hit.EntityHitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PersistentProjectileEntity.class)
public class FlecheMixin {

    @Inject(method = "onEntityHit", at = @At("HEAD"))
    private void onEntityHit(EntityHitResult hitResult, CallbackInfo ci) {
        if (!(((Object) this) instanceof ArrowEntity arrow)) return;

        var arrowStack = arrow.getItemStack();
        if (!FlecheComboData.isFleche(arrowStack)) return;

        if (arrow.getDamage() > 1.5) return;

        arrow.setDamage(1.0);
        int level = FlecheComboData.getSangLevel(arrowStack);
        if (level == 2 && hitResult.getEntity() instanceof LivingEntity living && !living.hasStatusEffect(StatusEffects.WITHER)) {
            living.addStatusEffect(new StatusEffectInstance(StatusEffects.WITHER, 60, 1, false, false, true));
        }
    }
}
