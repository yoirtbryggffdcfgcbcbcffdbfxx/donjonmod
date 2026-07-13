package com.dungeonmod.mixin;

import net.minecraft.component.DataComponentTypes;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Items;
import net.minecraft.server.world.ServerWorld;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(LivingEntity.class)
public class RingOfBloodMixin {

    private static boolean hasRing(LivingEntity entity) {
        if (!(entity instanceof PlayerEntity)) return false;
        var main = ((PlayerEntity)entity).getMainHandStack();
        var off = ((PlayerEntity)entity).getOffHandStack();
        return isRing(main) || isRing(off);
    }

    private static boolean isRing(net.minecraft.item.ItemStack stack) {
        if (stack.isEmpty() || !stack.isOf(Items.STICK)) return false;
        if (!stack.contains(DataComponentTypes.CUSTOM_NAME)) return false;
        return stack.get(DataComponentTypes.CUSTOM_NAME).getString().contains("Anneau de sang");
    }

    @ModifyVariable(method = "damage", at = @At("HEAD"), argsOnly = true)
    private float modifyDamage(float amount, ServerWorld world, DamageSource source, float originalAmount) {
        LivingEntity self = (LivingEntity)(Object)this;
        // Dégâts entrants ×2 si le porteur a l'anneau
        if (hasRing(self)) {
            return amount * 2.0f;
        }
        // Dégâts sortants ×2.5 si l'attaquant a l'anneau
        if (source.getAttacker() instanceof LivingEntity attacker && hasRing(attacker)) {
            return amount * 2.5f;
        }
        return amount;
    }
}
