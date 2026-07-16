package com.dungeonmod.client;

import dev.kosmx.playerAnim.api.firstPerson.FirstPersonConfiguration;
import dev.kosmx.playerAnim.api.firstPerson.FirstPersonMode;
import dev.kosmx.playerAnim.api.layered.IAnimation;
import dev.kosmx.playerAnim.api.layered.ModifierLayer;
import dev.kosmx.playerAnim.api.layered.modifier.AbstractFadeModifier;
import dev.kosmx.playerAnim.core.util.Ease;
import dev.kosmx.playerAnim.minecraftApi.PlayerAnimationAccess;
import dev.kosmx.playerAnim.minecraftApi.PlayerAnimationRegistry;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.random.Random;

public class SyrinxHandler {

    private static ModifierLayer<IAnimation> syrinxLayer;
    private static boolean registered = false;
    private static boolean animationStarted = false;
    private static int tickCounter = 0;

    public static void init() {
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            ClientPlayerEntity player = client.player;
            if (player == null) return;

            if (!registered) {
                var animStack = PlayerAnimationAccess.getPlayerAnimLayer(player);
                syrinxLayer = new ModifierLayer<>();
                animStack.addAnimLayer(2100, syrinxLayer);
                registered = true;
            }

            boolean hasSyrinx = isSyrinx(player.getMainHandStack()) || isSyrinx(player.getOffHandStack());
            boolean rightClickHeld = client.options.useKey.isPressed();

            if (hasSyrinx && rightClickHeld) {
                if (syrinxLayer != null && !animationStarted) {
                    var anim = PlayerAnimationRegistry.getAnimation(Identifier.of("dungeonmod", "animation.syrinx.play"));
                    if (anim != null) {
                        IAnimation playable = anim.playAnimation()
                            .setFirstPersonMode(FirstPersonMode.THIRD_PERSON_MODEL)
                            .setFirstPersonConfiguration(new FirstPersonConfiguration()
                                .setShowRightItem(true)
                                .setShowRightArm(true)
                                .setShowLeftItem(true)
                                .setShowLeftArm(true));
                        syrinxLayer.replaceAnimationWithFade(
                            AbstractFadeModifier.standardFadeIn(2, Ease.LINEAR),
                            playable
                        );
                        animationStarted = true;
                    }
                }

                tickCounter++;
                if (tickCounter % 10 == 0) {
                    Random r = player.getRandom();
                    float pitch = 0.5f + r.nextFloat() * 2.0f;
                    player.getWorld().playSound(player, player.getX(), player.getY(), player.getZ(), SoundEvents.BLOCK_NOTE_BLOCK_FLUTE.value(), SoundCategory.RECORDS, 0.6f, pitch);

                    for (int i = 0; i < 3; i++) {
                        double rx = (r.nextDouble() - 0.5) * 2.0;
                        double ry = r.nextDouble() * 1.5;
                        double rz = (r.nextDouble() - 0.5) * 2.0;
                        player.getWorld().addParticle(ParticleTypes.NOTE,
                            player.getX() + rx, player.getY() + 1.0 + ry, player.getZ() + rz,
                            0, 0, 0);
                    }
                }
            } else {
                if (syrinxLayer != null && animationStarted) {
                    syrinxLayer.setAnimation(null);
                    animationStarted = false;
                }
            }
        });
    }

    private static boolean isSyrinx(ItemStack stack) {
        return !stack.isEmpty() && stack.isOf(Items.STICK)
            && stack.contains(DataComponentTypes.CUSTOM_NAME)
            && stack.get(DataComponentTypes.CUSTOM_NAME).getString().contains("Syrinx oublié");
    }
}
