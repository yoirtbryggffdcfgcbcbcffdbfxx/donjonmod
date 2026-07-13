package com.dungeonmod.mixin;

import com.dungeonmod.util.BaguetteData;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import java.util.AbstractMap;
import java.util.UUID;

@Mixin(PlayerEntity.class)
public class DarkBuffMixin {

    @Inject(method = "attack", at = @At("TAIL"))
    private void onAttackTail(Entity target, CallbackInfo ci) {
        PlayerEntity player = (PlayerEntity)(Object)this;
        if (player.getWorld().isClient()) return;
        if (!(target instanceof LivingEntity living) || !living.isAlive()) return;
        AbstractMap.SimpleEntry<UUID, Long> link = BaguetteData.darkLink.get(player.getUuid());
        if (link == null || System.currentTimeMillis() > link.getValue()) return;
        // Vérifier que c'est le même ennemi
        if (!link.getKey().equals(target.getUuid())) return;
        // Soin : 0.5 cœur par dégât infligé
        player.heal(1.0f);
    }
}
