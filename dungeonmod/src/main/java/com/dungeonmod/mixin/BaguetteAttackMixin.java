package com.dungeonmod.mixin;

import com.dungeonmod.util.BaguetteData;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PlayerEntity.class)
public class BaguetteAttackMixin {

    @Inject(method = "attack", at = @At("HEAD"), cancellable = true)
    private void onAttack(Entity target, CallbackInfo ci) {
        PlayerEntity player = (PlayerEntity) (Object) this;
        if (player.getWorld().isClient()) return;
        if (!BaguetteData.isBaguette(player.getMainHandStack())) return;
        ci.cancel();
        // Vérifier le cooldown — ne pas spawn si en cooldown
        if (BaguetteData.isOnCooldown(player)) return;
        // En multi, le serveur doit spawn le projectile (le client ne peut pas)
        var server = player.getWorld().getServer();
        if (server != null && !server.isSingleplayer()) {
            BaguetteData.tryWandAttackServer(player);
        }
    }
}