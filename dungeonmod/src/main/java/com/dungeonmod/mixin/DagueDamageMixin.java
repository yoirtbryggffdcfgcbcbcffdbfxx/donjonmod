package com.dungeonmod.mixin;

import net.minecraft.entity.player.PlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(net.minecraft.entity.LivingEntity.class)
public class DagueDamageMixin {

    @ModifyVariable(method = "damage", at = @At("HEAD"), argsOnly = true)
    private float reduceDagueParryDamage(float amount, net.minecraft.server.world.ServerWorld world,
                                          net.minecraft.entity.damage.DamageSource source, float originalAmount) {
        if (!(((Object) this) instanceof PlayerEntity player)) return amount;
        if (!com.dungeonmod.util.DagueParryData.isActive(player)) return amount;
        return amount * 0.5f;
    }
}
