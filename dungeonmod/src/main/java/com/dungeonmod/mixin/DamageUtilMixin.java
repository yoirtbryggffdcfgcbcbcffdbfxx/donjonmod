package com.dungeonmod.mixin;

import net.minecraft.entity.DamageUtil;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;

@Mixin(DamageUtil.class)
public class DamageUtilMixin {

    @Overwrite
    public static float getDamageLeft(LivingEntity entity, float amount, DamageSource source, float armor, float toughness) {
        if (armor < 0.0f) armor = 0.0f;
        if (armor > 100.0f) armor = 100.0f;
        return Math.max(0.0f, amount * (1.0f - armor / 100.0f));
    }
}
