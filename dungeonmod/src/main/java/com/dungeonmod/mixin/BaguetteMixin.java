package com.dungeonmod.mixin;

import com.dungeonmod.util.BaguetteData;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Item.class)
public class BaguetteMixin {

    @Inject(method = "use", at = @At("HEAD"), cancellable = true)
    private void onUse(World world, PlayerEntity user, Hand hand, CallbackInfoReturnable<ActionResult> cir) {
        ItemStack stack = user.getStackInHand(hand);
        if (world.isClient()) return;
        if (!BaguetteData.isBaguette(stack)) return;

        // Transformation cyclique
        String type = BaguetteData.getWandType(stack);
        String next = type;
        if (type.equals("feu") && BaguetteData.hasRune(stack, "dark")) next = "sombre";
        else if (type.equals("sombre") && BaguetteData.hasRune(stack, "ice")) next = "glace";
        else if (type.equals("glace")) next = "feu";
        else if (type.equals("feu") && BaguetteData.hasRune(stack, "ice")) next = "glace";
        else if (type.equals("sombre")) next = "feu";

        if (!next.equals(type)) {
            BaguetteData.setWandType(stack, next);
            BaguetteData.setWandModel(stack, next);
            // Cooldown sur la nouvelle baguette pour éviter le glitch entre les formes
            BaguetteData.resetCooldown(user);
            cir.setReturnValue(ActionResult.SUCCESS);
            return;
        }

        // Rien à appliquer/transformer → le clic droit ne fait rien
        cir.setReturnValue(ActionResult.SUCCESS);
    }
}
