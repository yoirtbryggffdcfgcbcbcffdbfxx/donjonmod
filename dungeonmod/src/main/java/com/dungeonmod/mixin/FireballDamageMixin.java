package com.dungeonmod.mixin;

import com.dungeonmod.util.BaguetteData;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.projectile.SmallFireballEntity;
import net.minecraft.util.hit.EntityHitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(SmallFireballEntity.class)
public class FireballDamageMixin {

    @Inject(method = "onEntityHit", at = @At("HEAD"), cancellable = true)
    private void onFireballHit(EntityHitResult result, CallbackInfo ci) {
        if (!(result.getEntity() instanceof LivingEntity target)) return;
        var fireball = (SmallFireballEntity) (Object) this;
        if (target.isOnFire()) return;
        if (fireball.getWorld() instanceof net.minecraft.server.world.ServerWorld sw) {
            target.damage(sw, fireball.getDamageSources().fireball(fireball, fireball.getOwner()), 1.0f);
            target.setOnFireFor(2); // 2.5 secondes de feu ≈ 2.5 cœurs au total
            BaguetteData.fireTagged.add(target.getUuid());
        }
        ci.cancel();
    }
}
