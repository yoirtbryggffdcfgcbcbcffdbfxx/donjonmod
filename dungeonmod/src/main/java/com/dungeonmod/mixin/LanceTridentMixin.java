package com.dungeonmod.mixin;

import com.dungeonmod.util.LanceHelper;
import net.minecraft.entity.projectile.TridentEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(TridentEntity.class)
public class LanceTridentMixin {

    @Inject(method = "onEntityHit", at = @At("HEAD"))
    private void onEntityHit(net.minecraft.util.hit.EntityHitResult hitResult, CallbackInfo ci) {
        TridentEntity trident = (TridentEntity) (Object) this;
        var stack = trident.getItemStack();
        if (!LanceHelper.isLance(stack)) return;
        trident.setDamage(6.0);
    }
}
