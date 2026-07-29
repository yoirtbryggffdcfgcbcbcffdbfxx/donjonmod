package com.dungeonmod.mixin;

import com.dungeonmod.DungeonMod;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.item.consume.UseAction;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.Identifier;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Animations vanilla de mange/boire pour les consommables du mod.
 * Sans plastron du glouton : durée vanilla (32 ticks ≈ 1,6 s).
 * Avec plastron du glouton : instantané (1 tick) + soins ×2 / durées potions-bières ×2.
 */
@Mixin(Item.class)
public class DungeonConsumableMixin {

    private static final int VANILLA_CONSUME_TICKS = 32;

    private static boolean isModFood(ItemStack stack) {
        return DungeonMod.isApple(stack)
            || DungeonMod.isPotato(stack)
            || DungeonMod.isSteak(stack)
            || DungeonMod.isGoblinMeat(stack);
    }

    private static boolean isModDrink(ItemStack stack) {
        return DungeonMod.isBiere(stack)
            || (DungeonMod.isFlask(stack) && DungeonMod.isBlessedFlask(stack));
    }

    private static boolean isModConsumable(ItemStack stack) {
        return isModFood(stack) || isModDrink(stack);
    }

    @Inject(method = "getMaxUseTime", at = @At("HEAD"), cancellable = true)
    private void dungeonmod$consumeTime(ItemStack stack, LivingEntity entity, CallbackInfoReturnable<Integer> cir) {
        if (!isModConsumable(stack)) return;
        if (entity instanceof PlayerEntity player && DungeonMod.hasGloutonChestplate(player)) {
            cir.setReturnValue(1); // instantané
        } else {
            cir.setReturnValue(VANILLA_CONSUME_TICKS);
        }
    }

    @Inject(method = "getUseAction", at = @At("HEAD"), cancellable = true)
    private void dungeonmod$consumeAction(ItemStack stack, CallbackInfoReturnable<UseAction> cir) {
        if (isModFood(stack)) {
            cir.setReturnValue(UseAction.EAT);
        } else if (isModDrink(stack)) {
            cir.setReturnValue(UseAction.DRINK);
        }
    }

    @Inject(method = "use", at = @At("HEAD"), cancellable = true)
    private void dungeonmod$startConsume(World world, PlayerEntity user, Hand hand, CallbackInfoReturnable<ActionResult> cir) {
        ItemStack stack = user.getStackInHand(hand);
        if (!isModConsumable(stack)) return;
        // Ne pas démarrer l'anim si on remplit une fiole vide à la fontaine
        // (géré ailleurs) — ici on ne traite que les consommables prêts.
        user.setCurrentHand(hand);
        // SUCCESS + setCurrentHand = démarre l'anim (comme SyrinxMixin)
        cir.setReturnValue(ActionResult.SUCCESS);
    }

    @Inject(method = "finishUsing", at = @At("HEAD"), cancellable = true)
    private void dungeonmod$finishConsume(ItemStack stack, World world, LivingEntity user, CallbackInfoReturnable<ItemStack> cir) {
        if (!isModConsumable(stack)) return;
        if (world.isClient() || !(user instanceof ServerPlayerEntity player)) {
            cir.setReturnValue(stack);
            return;
        }

        boolean glouton = DungeonMod.hasGloutonChestplate(player);
        float healMult = glouton ? 2.0f : 1.0f;
        int durationMult = glouton ? 2 : 1;

        if (DungeonMod.isApple(stack)) {
            player.heal(1.0f * healMult);
            consumeOne(player, stack, cir, null);
            return;
        }
        if (DungeonMod.isPotato(stack)) {
            player.heal(2.0f * healMult);
            player.addStatusEffect(new StatusEffectInstance(StatusEffects.NAUSEA, 200, 0, false, false));
            consumeOne(player, stack, cir, null);
            return;
        }
        if (DungeonMod.isSteak(stack)) {
            player.heal(6.0f * healMult);
            consumeOne(player, stack, cir, null);
            return;
        }
        if (DungeonMod.isGoblinMeat(stack)) {
            // crue = 2 cœurs (4 HP), cuite = 5 cœurs (10 HP)
            boolean cooked = stack.isOf(Items.COOKED_BEEF)
                || (stack.contains(DataComponentTypes.CUSTOM_NAME)
                    && stack.get(DataComponentTypes.CUSTOM_NAME).getString().contains("cuite"));
            player.heal((cooked ? 10.0f : 4.0f) * healMult);
            consumeOne(player, stack, cir, null);
            return;
        }
        if (DungeonMod.isBiere(stack)) {
            if (stack.isOf(Items.POTION)) {
                // Bière périmée : nausée inchangée, boost force × durée si glouton
                player.addStatusEffect(new StatusEffectInstance(StatusEffects.NAUSEA, 400, 0, false, false));
                com.dungeonmod.util.BeerStrengthData.applyBoost(player, "brune", 1.5f, 400 * durationMult);
            } else {
                com.dungeonmod.util.BeerStrengthData.applyBoost(player, "viking", 2.5f, 600 * durationMult);
            }
            ItemStack chope = makeChope();
            consumeOne(player, stack, cir, chope);
            return;
        }
        if (DungeonMod.isFlask(stack) && DungeonMod.isBlessedFlask(stack)) {
            // 8 s → 16 s avec glouton
            long durationMs = 8000L * durationMult;
            DungeonMod.holyWaterTimers.put(player.getUuid(), System.currentTimeMillis() + durationMs);
            // Remplace par fiole vide
            ItemStack empty = DungeonMod.createFlask();
            if (!player.isCreative()) {
                cir.setReturnValue(empty);
            } else {
                cir.setReturnValue(stack);
            }
            return;
        }

        cir.setReturnValue(stack);
    }

    private static ItemStack makeChope() {
        ItemStack chope = new ItemStack(Items.GLASS_BOTTLE);
        chope.set(DataComponentTypes.CUSTOM_NAME, net.minecraft.text.Text.literal("§7Chope de bière"));
        chope.set(DataComponentTypes.ITEM_MODEL, Identifier.of("dungeonmod", "chope_biere"));
        return chope;
    }

    private static void consumeOne(ServerPlayerEntity player, ItemStack stack,
                                   CallbackInfoReturnable<ItemStack> cir, ItemStack remainder) {
        if (player.isCreative()) {
            cir.setReturnValue(stack);
            return;
        }
        stack.decrement(1);
        if (remainder != null) {
            if (stack.isEmpty()) {
                cir.setReturnValue(remainder);
            } else {
                if (!player.getInventory().insertStack(remainder)) {
                    player.dropItem(remainder, false);
                }
                cir.setReturnValue(stack);
            }
        } else {
            cir.setReturnValue(stack);
        }
    }
}
