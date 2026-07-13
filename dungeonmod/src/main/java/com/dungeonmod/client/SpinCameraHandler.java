package com.dungeonmod.client;

import com.dungeonmod.util.GlaiveComboData;
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
import net.minecraft.util.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SpinCameraHandler {
    private static final Logger LOGGER = LoggerFactory.getLogger("dungeonmod");
    private static ModifierLayer<IAnimation> spinLayer;
    private static boolean registered = false;
    private static boolean animationStarted = false;
    private static boolean debugLogged = false;

    public static void init() {
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            ClientPlayerEntity player = client.player;
            if (player == null) return;

            if (!registered) {
                LOGGER.info("[SpinCamera] Registering spin layer at priority 2000");
                var animStack = PlayerAnimationAccess.getPlayerAnimLayer(player);
                spinLayer = new ModifierLayer<>();
                animStack.addAnimLayer(2000, spinLayer);
                registered = true;
            }

            var stack = player.getMainHandStack();
            boolean hasGlaive = GlaiveComboData.isGlaive(stack);
            boolean rightClickHeld = client.options.useKey.isPressed();

            if (!debugLogged) {
                LOGGER.info("[SpinCamera] hasGlaive={}, rightClickHeld={}, spinLayer={}",
                    hasGlaive, rightClickHeld, spinLayer);
                debugLogged = true;
            }

            if (hasGlaive && rightClickHeld) {
                float speed = 18.0f;
                float newYaw = player.getYaw() + speed;
                player.prevYaw = player.getYaw();
                player.setYaw(newYaw);
                player.setBodyYaw(newYaw);
                player.prevBodyYaw = newYaw;
                player.setHeadYaw(newYaw);
                player.prevHeadYaw = newYaw;

                if (spinLayer != null && !animationStarted) {
                    var anim = PlayerAnimationRegistry.getAnimation(Identifier.of("dungeonmod", "animation.glaive.spin"));
                    LOGGER.info("[SpinCamera] Starting animation: anim={}", anim != null ? "FOUND" : "NULL");
                    if (anim != null) {
                        IAnimation playable = anim.playAnimation()
                            .setFirstPersonMode(FirstPersonMode.THIRD_PERSON_MODEL)
                            .setFirstPersonConfiguration(new FirstPersonConfiguration()
                                .setShowRightItem(true)
                                .setShowRightArm(true)
                                .setShowLeftItem(true)
                                .setShowLeftArm(true));
                        LOGGER.info("[SpinCamera] Animation playable={}", playable);
                        spinLayer.replaceAnimationWithFade(
                            AbstractFadeModifier.standardFadeIn(2, Ease.LINEAR),
                            playable
                        );
                        animationStarted = true;
                        LOGGER.info("[SpinCamera] Animation set! isActive={}", spinLayer.isActive());
                    }
                }
            } else {
                if (spinLayer != null && animationStarted) {
                    LOGGER.info("[SpinCamera] Stopping animation");
                    spinLayer.setAnimation(null);
                    animationStarted = false;
                }
            }
        });
    }
}
