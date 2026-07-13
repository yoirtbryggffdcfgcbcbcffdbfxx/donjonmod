package com.dungeonmod.mixin;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.item.Items;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.Vec3d;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientPlayerEntity.class)
public class DoubleJumpMixin {

    @Unique
    private boolean canDoubleJump = false;
    @Unique
    private boolean lastJumpPressed = false;

    @Inject(method = "tickMovement", at = @At("HEAD"))
    private void onTickMovement(CallbackInfo ci) {
        ClientPlayerEntity player = (ClientPlayerEntity) (Object) this;

        boolean jumpPressed = MinecraftClient.getInstance().options.jumpKey.isPressed();
        boolean onGround = player.isOnGround() || player.isClimbing();

        if (onGround) {
            canDoubleJump = true;
        } else if (jumpPressed && !lastJumpPressed && canDoubleJump && hasApollonBoots(player) && !player.getAbilities().flying) {
            canDoubleJump = false;

            Vec3d vel = player.getVelocity();
            player.setVelocity(vel.x, 0.55, vel.z);

            player.getWorld().playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.ENTITY_WIND_CHARGE_WIND_BURST,
                SoundCategory.PLAYERS, 1.0f, 1.4f);

            for (int i = 0; i < 8; i++) {
                player.getWorld().addParticle(
                    ParticleTypes.CLOUD,
                    player.getX(), player.getY() - 0.2, player.getZ(),
                    (Math.random() - 0.5) * 0.3, -0.1, (Math.random() - 0.5) * 0.3
                );
            }
        }

        lastJumpPressed = jumpPressed;
    }

    @Unique
    private boolean hasApollonBoots(ClientPlayerEntity player) {
        var boots = player.getInventory().getArmorStack(0);
        if (boots.isEmpty() || !boots.isOf(Items.GOLDEN_BOOTS)) return false;
        if (!boots.contains(DataComponentTypes.CUSTOM_NAME)) return false;
        return boots.get(DataComponentTypes.CUSTOM_NAME).getString().contains("Bottes d'Apollon");
    }
}
