package com.dungeonmod.mixin;

import com.dungeonmod.util.BeerStrengthData;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.world.ServerWorld;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(LivingEntity.class)
public class BeerStrengthMixin {

    @ModifyVariable(method = "damage", at = @At("HEAD"), argsOnly = true)
    private float multiplyBeerDamage(float amount, ServerWorld world, DamageSource source, float originalAmount) {
        if (!(source.getAttacker() instanceof PlayerEntity player)) return amount;
        float mult = BeerStrengthData.getMultiplier(player);
        return mult != 1.0f ? amount * mult : amount;
    }
}
