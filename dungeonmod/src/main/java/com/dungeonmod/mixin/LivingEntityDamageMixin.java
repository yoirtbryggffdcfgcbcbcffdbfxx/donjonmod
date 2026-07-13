package com.dungeonmod.mixin;

import com.dungeonmod.DungeonMod;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.Vec3d;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ServerPlayerEntity.class)
public class LivingEntityDamageMixin {

    @Unique
    private float dungeonmod$healthBefore;

    @Inject(method = "damage", at = @At("HEAD"), cancellable = true)
    private void onDamage(ServerWorld world, DamageSource source, float amount, CallbackInfoReturnable<Boolean> cir) {
        ServerPlayerEntity player = (ServerPlayerEntity) (Object) this;

        if (DungeonMod.isHunterProne(player)) {
            cir.setReturnValue(false);
            return;
        }

        if (hasHeroChestplate(player)) {
            dungeonmod$healthBefore = player.getHealth();
        }
    }

    @Inject(method = "damage", at = @At("TAIL"))
    private void onDamageTail(ServerWorld world, DamageSource source, float amount, CallbackInfoReturnable<Boolean> cir) {
        if (!cir.getReturnValue()) return;
        ServerPlayerEntity player = (ServerPlayerEntity) (Object) this;
        if (!hasHeroChestplate(player)) return;

        LivingEntity attacker = source.getAttacker() instanceof LivingEntity living ? living : null;
        if (attacker == null || !attacker.isAlive()) return;

        // Only reflect if attacker is in front of the player
        Vec3d toAttacker = attacker.getPos().subtract(player.getPos()).normalize();
        Vec3d lookDir = player.getRotationVec(1.0f).normalize();
        if (toAttacker.dotProduct(lookDir) < 0) return;

        float actualDamage = dungeonmod$healthBefore - player.getHealth();
        if (actualDamage <= 0) return;

        attacker.damage(world, player.getDamageSources().thorns(player), actualDamage);
    }

    private static boolean hasHeroChestplate(PlayerEntity player) {
        var chest = player.getInventory().getArmorStack(2);
        if (chest.isEmpty() || !chest.isOf(net.minecraft.item.Items.GOLDEN_CHESTPLATE)) return false;
        if (!chest.contains(net.minecraft.component.DataComponentTypes.CUSTOM_NAME)) return false;
        return chest.get(net.minecraft.component.DataComponentTypes.CUSTOM_NAME).getString().contains("Plastron du héros");
    }
}
