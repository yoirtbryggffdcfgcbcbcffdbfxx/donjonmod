package com.dungeonmod.mixin;

import com.dungeonmod.util.CraftingHelper;
import com.dungeonmod.util.TorcheHelper;
import net.minecraft.block.Blocks;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.server.network.ServerPlayNetworkHandler;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerPlayNetworkHandler.class)
public class CraftingMixin {

    @Shadow public ServerPlayerEntity player;

    @Inject(method = "onPlayerInteractBlock", at = @At("HEAD"), cancellable = true)
    private void onBlock(net.minecraft.network.packet.c2s.play.PlayerInteractBlockC2SPacket packet, CallbackInfo ci) {
        if (packet.getHand() == net.minecraft.util.Hand.OFF_HAND) { ci.cancel(); return; }
        if (tryCraft(packet.getHand())) ci.cancel();
    }

    @Inject(method = "onPlayerInteractItem", at = @At("HEAD"), cancellable = true)
    private void onItem(net.minecraft.network.packet.c2s.play.PlayerInteractItemC2SPacket packet, CallbackInfo ci) {
        if (packet.getHand() == net.minecraft.util.Hand.OFF_HAND) { ci.cancel(); return; }
        if (tryCraft(packet.getHand())) ci.cancel();
    }

    private static boolean isNearWater(net.minecraft.world.World world, BlockPos pos) {
        for (int dx = -1; dx <= 1; dx++) {
            for (int dy = -1; dy <= 1; dy++) {
                for (int dz = -1; dz <= 1; dz++) {
                    if (world.getFluidState(pos.add(dx, dy, dz)).isOf(net.minecraft.fluid.Fluids.WATER) ||
                        world.getBlockState(pos.add(dx, dy, dz)).isOf(Blocks.WATER)) return true;
                }
            }
        }
        return false;
    }

    private boolean tryCraft(net.minecraft.util.Hand hand) {
        var world = player.getServerWorld();
        if (world == null || world.isClient()) return false;
        var stack = player.getStackInHand(hand);

        var hit = player.raycast(5.0, 1.0f, true);
        if (hit.getType() != HitResult.Type.BLOCK) return false;
        BlockPos pos = ((BlockHitResult) hit).getBlockPos();

        if (world.getBlockState(pos).isOf(Blocks.CAMPFIRE) && stack.isOf(Items.STICK) && stack.contains(DataComponentTypes.CUSTOM_NAME)) {
            String name = stack.get(DataComponentTypes.CUSTOM_NAME).getString();
            if (name.contains("Bâton")) {
                ItemStack torche = Items.STICK.getDefaultStack();
                torche.set(DataComponentTypes.CUSTOM_NAME, Text.literal("§9Torche"));
                torche.set(DataComponentTypes.ITEM_MODEL, Identifier.of("dungeonmod", "torche"));
                player.getInventory().setStack(hand == net.minecraft.util.Hand.MAIN_HAND ? player.getInventory().selectedSlot : 40, torche);
                CraftingHelper.markCrafted(player);
                return true;
            }
            if (name.contains("Torche")) return true;
        }

        if (TorcheHelper.isTorche(stack) && isNearWater(world, pos)) {
            ItemStack baton = Items.STICK.getDefaultStack();
            baton.set(DataComponentTypes.CUSTOM_NAME, Text.literal("§9Bâton"));
            player.getInventory().setStack(hand == net.minecraft.util.Hand.MAIN_HAND ? player.getInventory().selectedSlot : 40, baton);
            CraftingHelper.markCrafted(player);
            return true;
        }
        return false;
    }
}
