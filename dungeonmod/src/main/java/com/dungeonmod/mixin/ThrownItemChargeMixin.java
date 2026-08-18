package com.dungeonmod.mixin;

import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.consume.UseAction;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Charge au clic droit (comme la lance) pour les items lançables :
 * bâton, caillou, os, torche. Maintien 1 seconde (20 ticks), relâcher pour
 * lancer. Relâcher avant la fin de la charge = annulé.
 */
@Mixin(Item.class)
public class ThrownItemChargeMixin {

    private static final int CHARGE_TICKS = 20;
    /** Durée quasi infinie (comme arc/trident) pour que l'usage ne se termine
     * jamais automatiquement — le lancer se fait au relâcher uniquement. */
    private static final int MAX_USE_TICKS = 72000;

    private static boolean isThrowable(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return false;
        return com.dungeonmod.DungeonMod.isBaton(stack)
            || com.dungeonmod.DungeonMod.isCaillou(stack)
            || com.dungeonmod.DungeonMod.isOs(stack)
            || com.dungeonmod.util.TorcheHelper.isTorche(stack);
    }

    @Inject(method = "getMaxUseTime", at = @At("HEAD"), cancellable = true)
    private void maxUseTime(ItemStack stack, net.minecraft.entity.LivingEntity user,
                            CallbackInfoReturnable<Integer> cir) {
        if (isThrowable(stack)) cir.setReturnValue(MAX_USE_TICKS);
    }

    @Inject(method = "getUseAction", at = @At("HEAD"), cancellable = true)
    private void useAction(ItemStack stack, CallbackInfoReturnable<UseAction> cir) {
        if (isThrowable(stack)) cir.setReturnValue(UseAction.SPEAR);
    }

    @Inject(method = "use", at = @At("HEAD"), cancellable = true)
    private void startCharging(World world, PlayerEntity user, Hand hand,
                               CallbackInfoReturnable<ActionResult> cir) {
        ItemStack stack = user.getStackInHand(hand);
        if (isThrowable(stack)) {
            user.setCurrentHand(hand);
            cir.setReturnValue(ActionResult.SUCCESS);
        }
    }

    @Inject(method = "onStoppedUsing(Lnet/minecraft/item/ItemStack;Lnet/minecraft/world/World;Lnet/minecraft/entity/LivingEntity;I)Z", at = @At("HEAD"), cancellable = true)
    private void onStoppedUsing(ItemStack stack, World world, LivingEntity user, int remainingUseTicks,
                                CallbackInfoReturnable<Boolean> cir) {
        System.out.println("[ThrownItem] onStoppedUsing: remaining=" + remainingUseTicks
            + " maxUse=" + MAX_USE_TICKS + " item=" + stack.getName().getString());
        if (!isThrowable(stack)) return;
        // Charge incomplète (relâché avant 20 ticks) → annulé
        int usedTicks = MAX_USE_TICKS - remainingUseTicks;
        System.out.println("[ThrownItem] usedTicks=" + usedTicks);
        if (usedTicks < CHARGE_TICKS) { cir.setReturnValue(false); return; }
        // Charge complète → lancer
        if (!world.isClient() && user instanceof ServerPlayerEntity sp) {
            System.out.println("[ThrownItem] canThrow=" + com.dungeonmod.DungeonMod.canThrow(sp)
                + " wasJustCrafted=" + com.dungeonmod.util.CraftingHelper.wasJustCrafted(sp));
            if (com.dungeonmod.DungeonMod.canThrow(sp)
                    && !com.dungeonmod.util.CraftingHelper.wasJustCrafted(sp)) {
                var snowball = new net.minecraft.entity.projectile.thrown.SnowballEntity(world, sp, stack);
                snowball.setVelocity(sp, sp.getPitch(), sp.getYaw(), 0.0f, 1.5f, 0.0f);
                world.spawnEntity(snowball);
                if (!sp.isCreative()) stack.decrement(1);
                System.out.println("[ThrownItem] Lance!");
            }
        }
        cir.setReturnValue(false);
    }
}
