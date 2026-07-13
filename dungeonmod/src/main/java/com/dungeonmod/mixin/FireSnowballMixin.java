package com.dungeonmod.mixin;

import net.minecraft.entity.projectile.thrown.SnowballEntity;
import net.minecraft.item.Items;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(SnowballEntity.class)
public class FireSnowballMixin {

    @Inject(method = "onCollision", at = @At("HEAD"))
    private void onCollision(net.minecraft.util.hit.HitResult hitResult, CallbackInfo ci) {
        SnowballEntity snowball = (SnowballEntity)(Object)this;
        var stack = snowball.getStack();
        if (stack.isEmpty()) return;
        if (!stack.contains(net.minecraft.component.DataComponentTypes.CUSTOM_NAME)) return;
        String name = stack.get(net.minecraft.component.DataComponentTypes.CUSTOM_NAME).getString();
        if (!(snowball.getWorld() instanceof ServerWorld sw)) return;

        if (name.contains("Boule de feu")) {
            sw.spawnParticles(ParticleTypes.FLAME, snowball.getX(), snowball.getY(), snowball.getZ(), 10, 0.5, 0.5, 0.5, 0.1);
            sw.spawnParticles(ParticleTypes.LAVA, snowball.getX(), snowball.getY(), snowball.getZ(), 5, 0.3, 0.3, 0.3, 0.1);
        } else if (name.contains("Boule de glace")) {
            sw.spawnParticles(ParticleTypes.SNOWFLAKE, snowball.getX(), snowball.getY(), snowball.getZ(), 10, 0.5, 0.5, 0.5, 0.05);
            sw.spawnParticles(ParticleTypes.GUST, snowball.getX(), snowball.getY(), snowball.getZ(), 5, 0.3, 0.3, 0.3, 0.02);
        } else if (name.contains("Boule sombre")) {
            sw.spawnParticles(ParticleTypes.SMOKE, snowball.getX(), snowball.getY(), snowball.getZ(), 8, 0.5, 0.5, 0.5, 0.02);
        }
    }
}
