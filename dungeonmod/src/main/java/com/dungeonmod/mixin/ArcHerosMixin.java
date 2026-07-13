package com.dungeonmod.mixin;

import com.dungeonmod.util.FlecheComboData;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.ArrowEntity;
import net.minecraft.entity.projectile.PersistentProjectileEntity;
import net.minecraft.item.BowItem;
import net.minecraft.item.ItemStack;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(BowItem.class)
public class ArcHerosMixin {

    @Inject(method = "onStoppedUsing", at = @At("HEAD"), cancellable = true)
    private void onStoppedUsing(ItemStack stack, World world, LivingEntity user, int remainingUseTicks, CallbackInfoReturnable<Boolean> cir) {
        if (world.isClient()) { cir.setReturnValue(false); return; }
        if (!(user instanceof PlayerEntity player)) return;
        if (!isArcHeros(stack)) return;

        int chargeTime = stack.getMaxUseTime(player) - remainingUseTicks;
        float charge = BowItem.getPullProgress(chargeTime);
        if (charge < 0.1) {
            cir.setReturnValue(false);
            return;
        }

        int[] sangLevel = {0};
        ItemStack arrowStack = FlecheComboData.findArrowForBow(player, sangLevel);
        if (arrowStack.isEmpty()) {
            cir.setReturnValue(false);
            return;
        }

        float damage = switch (sangLevel[0]) {
            case 2 -> 14.0f;
            case 1 -> 10.0f;
            default -> 6.0f;
        };

        FlecheComboData.revertArrow(arrowStack);

        ArrowEntity arrow = new ArrowEntity(world, player, arrowStack, stack);
        arrow.setVelocity(player, player.getPitch(), player.getYaw(), 0.0f, 1.0f, 1.0f);
        arrow.setDamage(damage);
        ((PersistentProjectileEntityAccessor) arrow).setPickupType(net.minecraft.entity.projectile.PersistentProjectileEntity.PickupPermission.ALLOWED);
        if (sangLevel[0] >= 1) {
            ((PersistentProjectileEntityAccessor) arrow).invokeSetPierceLevel(Byte.MAX_VALUE);
        }

        if (sangLevel[0] == 2) {
            com.dungeonmod.util.FlecheComboData.SANG2_ARROWS.add(arrow.getUuid());
        }

        com.dungeonmod.DungeonMod.LOGGER.info("ArcHeros: Damage=" + damage + " Sang=" + sangLevel[0] + " Crit=" + (charge >= 1.0));

        world.spawnEntity(arrow);

        if (!player.isCreative()) {
            arrowStack.decrement(1);
        }

        world.playSound(null, player.getX(), player.getY(), player.getZ(),
            SoundEvents.ENTITY_ARROW_SHOOT, SoundCategory.PLAYERS,
            1.0f, 1.0f / (world.random.nextFloat() * 0.4f + 1.2f) + charge * 0.5f);

        cir.setReturnValue(true);
    }

    private static boolean isArcHeros(ItemStack stack) {
        if (stack.isEmpty() || !stack.isOf(net.minecraft.item.Items.BOW)) return false;
        if (!stack.contains(net.minecraft.component.DataComponentTypes.CUSTOM_NAME)) return false;
        return stack.get(net.minecraft.component.DataComponentTypes.CUSTOM_NAME).getString().contains("Arc du héros");
    }
}
