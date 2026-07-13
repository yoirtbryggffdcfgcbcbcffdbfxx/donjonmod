package com.dungeonmod.mixin;

import com.dungeonmod.util.DagueParryData;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.Identifier;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Item.class)
public class DagueDualMixin {

    private static final java.util.Map<java.util.UUID, Long> lastToggle = new java.util.HashMap<>();

    @Inject(method = "getMaxUseTime", at = @At("HEAD"), cancellable = true)
    private void onGetMaxUseTime(ItemStack stack, LivingEntity entity, CallbackInfoReturnable<Integer> cir) {
        if (isDague(stack)) cir.setReturnValue(72000);
    }

    @Inject(method = "inventoryTick", at = @At("HEAD"))
    private void onInventoryTick(ItemStack stack, World world, net.minecraft.entity.Entity entity, int slot, boolean selected, CallbackInfo ci) {
        if (!(entity instanceof ServerPlayerEntity player)) return;
        if (!DagueParryData.isActive(player)) return;
        if (!isDague(player.getMainHandStack()) || !isDague(player.getOffHandStack())) {
            toggleParry(player, false);
            return;
        }
        player.addStatusEffect(new net.minecraft.entity.effect.StatusEffectInstance(
            net.minecraft.entity.effect.StatusEffects.SLOWNESS, 10, 1, false, false, false));
    }

    @Inject(method = "use", at = @At("HEAD"), cancellable = true)
    private void onUse(World world, PlayerEntity user, Hand hand, CallbackInfoReturnable<ActionResult> cir) {
        ItemStack stack = user.getStackInHand(hand);
        if (world.isClient()) return;
        if (!isDague(stack)) return;
        if (stack.isEmpty()) return;

        if (!isDague(user.getOffHandStack())) {
            cir.setReturnValue(ActionResult.SUCCESS);
            return;
        }

        // Anti-toggle loop (pas de cooldown visuel)
        long now = System.currentTimeMillis();
        Long last = lastToggle.get(user.getUuid());
        if (last != null && now - last < 400) {
            cir.setReturnValue(ActionResult.FAIL);
            return;
        }
        lastToggle.put(user.getUuid(), now);
        // Toggle parry
        toggleParry((ServerPlayerEntity) user, !DagueParryData.isActive(user));
        cir.setReturnValue(ActionResult.SUCCESS);
    }

    @Unique
    private static void toggleParry(ServerPlayerEntity player, boolean active) {
        DagueParryData.setActive(player, active);
        Identifier model = active ? Identifier.of("dungeonmod", "dague_blocking") : Identifier.of("dungeonmod", "dague");
        player.getOffHandStack().set(DataComponentTypes.ITEM_MODEL, model);
    }

    @Unique
    private static boolean isDague(ItemStack stack) {
        if (stack.isEmpty() || !stack.isOf(Items.FLINT)) return false;
        if (!stack.contains(DataComponentTypes.CUSTOM_NAME)) return false;
        return stack.get(DataComponentTypes.CUSTOM_NAME).getString().contains("Dague");
    }
}
