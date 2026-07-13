package com.dungeonmod.mixin;

import net.minecraft.component.DataComponentTypes;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Items;
import net.minecraft.server.world.ServerWorld;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LivingEntity.class)
public class VoyageurMixin {

    @Inject(method = "damage", at = @At("TAIL"))
    private void onDamage(ServerWorld world, DamageSource source, float amount, CallbackInfoReturnable<Boolean> cir) {
        if (!cir.getReturnValue()) return;
        if (!(((Object) this) instanceof LivingEntity target)) return;
        if (target.isAlive()) return;
        if (!(source.getAttacker() instanceof PlayerEntity player)) return;

        var chest = player.getInventory().getArmorStack(2);
        if (chest.isEmpty() || !chest.isOf(Items.LEATHER_CHESTPLATE)) return;
        if (!chest.contains(DataComponentTypes.CUSTOM_NAME)) return;
        if (!chest.get(DataComponentTypes.CUSTOM_NAME).getString().contains("Plastron du voyageur")) return;

        player.heal(1.0f);
    }
}
