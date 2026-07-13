package com.dungeonmod.mixin;

import com.dungeonmod.util.SabreComboData;
import dev.kosmx.playerAnim.api.firstPerson.FirstPersonConfiguration;
import dev.kosmx.playerAnim.api.firstPerson.FirstPersonMode;
import dev.kosmx.playerAnim.api.layered.IAnimation;
import dev.kosmx.playerAnim.api.layered.ModifierLayer;
import dev.kosmx.playerAnim.api.layered.modifier.AbstractFadeModifier;
import dev.kosmx.playerAnim.core.util.Ease;
import dev.kosmx.playerAnim.minecraftApi.PlayerAnimationAccess;
import dev.kosmx.playerAnim.minecraftApi.PlayerAnimationRegistry;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.network.packet.s2c.play.EntityAnimationS2CPacket;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Item.class)
public class SabreMixin {

    @Inject(method = "useOnEntity", at = @At("HEAD"), cancellable = true)
    private void onUseOnEntity(ItemStack stack, PlayerEntity user, LivingEntity entity, Hand hand, CallbackInfoReturnable<ActionResult> cir) {
        doSabreAction(user, cir);
    }

    @Inject(method = "use", at = @At("HEAD"), cancellable = true)
    private void onUse(World world, PlayerEntity user, Hand hand, CallbackInfoReturnable<ActionResult> cir) {
        if (world.isClient()) return;
        doSabreAction(user, cir);
    }

    @Unique
    private void doSabreAction(PlayerEntity user, CallbackInfoReturnable<ActionResult> cir) {
        ItemStack stack = user.getStackInHand(Hand.MAIN_HAND);
        if (stack.isEmpty() || !stack.isOf(Items.IRON_SWORD)) return;
        if (!stack.contains(DataComponentTypes.CUSTOM_NAME)) return;
        if (!stack.get(DataComponentTypes.CUSTOM_NAME).getString().contains("Sabre")) return;

        if (user instanceof ServerPlayerEntity sp) {
            int combo = SabreComboData.getCombo(sp);
            boolean hit = SabreComboData.unleashCombo(sp);
            if (combo == 0 && !hit) {
                sp.getWorld().playSound(null, sp.getX(), sp.getY(), sp.getZ(),
                    net.minecraft.sound.SoundEvents.ITEM_SHIELD_BREAK, net.minecraft.sound.SoundCategory.PLAYERS, 1.0f, 1.0f);
            } else if (combo >= 1 && !hit) {
                sp.getWorld().playSound(null, sp.getX(), sp.getY(), sp.getZ(),
                    net.minecraft.sound.SoundEvents.ENTITY_PLAYER_ATTACK_SWEEP, net.minecraft.sound.SoundCategory.PLAYERS, 1.0f, 1.0f);
            }
            playSabreSlash(sp);
            if (sp.getWorld() instanceof ServerWorld sw) {
                Vec3d look = sp.getRotationVec(1.0f);
                Vec3d base = sp.getPos().add(look.x * 2.0, sp.getStandingEyeHeight() * 0.3, look.z * 2.0);
                sw.spawnParticles(ParticleTypes.SWEEP_ATTACK, base.x, base.y, base.z, 1, 0, 0, 0, 0);
            }
        }
        cir.setReturnValue(ActionResult.SUCCESS);
    }

    private static void playSabreSlash(ServerPlayerEntity player) {
        ModifierLayer<IAnimation> layer = SabreComboData.animLayers.get(player.getUuid());
        if (layer == null) return;
        var playable = PlayerAnimationRegistry.getAnimation(Identifier.of("dungeonmod", "animation.sabre.slash"));
        if (playable == null) return;
        layer.replaceAnimationWithFade(
            AbstractFadeModifier.standardFadeIn(2, Ease.LINEAR),
            playable.playAnimation()
                .setFirstPersonMode(FirstPersonMode.THIRD_PERSON_MODEL)
                .setFirstPersonConfiguration(new FirstPersonConfiguration()
                    .setShowRightItem(true).setShowRightArm(true))
        );
    }
}
