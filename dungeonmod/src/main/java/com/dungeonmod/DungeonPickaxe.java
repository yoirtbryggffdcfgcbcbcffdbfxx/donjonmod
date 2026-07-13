package com.dungeonmod;

import net.minecraft.component.DataComponentTypes;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.PickaxeItem;
import net.minecraft.item.ToolMaterial;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Rarity;
import net.minecraft.util.math.BlockPos;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class DungeonPickaxe extends PickaxeItem {

    private static final Set<BlockPos> breakablePositions = new HashSet<>();
    private static final String BROKEN_NAME = "§8Pioche du Donjon §c§l[brisée]";

    public static void setBreakablePositions(Set<BlockPos> positions) {
        breakablePositions.clear();
        breakablePositions.addAll(positions);
    }

    public static void clearBreakablePositions() {
        breakablePositions.clear();
    }

    public static boolean isBlockBreakable(BlockPos pos) {
        return breakablePositions.contains(pos);
    }

    public static boolean hasBreakableBlocks() {
        return !breakablePositions.isEmpty();
    }

    public static Set<BlockPos> getBreakablePositions() {
        return new HashSet<>(breakablePositions);
    }

    public static int breakableCount() {
        return breakablePositions.size();
    }

    public DungeonPickaxe(Item.Settings settings) {
        super(ToolMaterial.DIAMOND, 0, -2.8f, settings
            .rarity(Rarity.EPIC)
            .component(DataComponentTypes.ENCHANTMENT_GLINT_OVERRIDE, true)
        );
    }

    @Override
    public float getMiningSpeed(ItemStack stack, net.minecraft.block.BlockState state) {
        if (isBrokenDungeonPickaxe(stack)) return 0.0f;
        return 128.0f;
    }

    @Override
    public boolean canMine(net.minecraft.block.BlockState state, net.minecraft.world.World world, BlockPos pos, net.minecraft.entity.player.PlayerEntity player) {
        if (isBrokenDungeonPickaxe(player.getMainHandStack())) return false;
        return true;
    }

    @Override
    public void appendTooltip(ItemStack stack, TooltipContext context, List<Text> tooltip, TooltipType type) {
        if (isBrokenDungeonPickaxe(stack)) {
            tooltip.add(Text.literal("§8Pioche du Donjon §c§l[brisée]"));
            tooltip.add(Text.literal(""));
            tooltip.add(Text.literal("§7La pioche s'est brisée en"));
            tooltip.add(Text.literal("§7traversant le premier mur."));
            tooltip.add(Text.literal(""));
            tooltip.add(Text.literal("§8§oElle ne servira plus à rien..."));
            tooltip.add(Text.literal(""));
            tooltip.add(Text.literal("§4Durabilité: §c0/§459"));
        } else {
            tooltip.add(Text.literal("§c§l⚡ Pioche du Donjon ⚡"));
            tooltip.add(Text.literal(""));
            tooltip.add(Text.literal("§7Une pioche forgée dans les"));
            tooltip.add(Text.literal("§7profondeurs du donjon."));
            tooltip.add(Text.literal(""));
            tooltip.add(Text.literal("§8§oSeuls les murs scellés du"));
            tooltip.add(Text.literal("§8§odonjon peuvent être brisés..."));
            tooltip.add(Text.literal(""));
            tooltip.add(Text.literal("§4§lmythique"));
        }
    }

    public static boolean isDungeonPickaxe(ItemStack stack) {
        return false; // Pickaxe removed
    }

    public static boolean isBrokenDungeonPickaxe(ItemStack stack) {
        if (stack.isEmpty()) return false;
        if (stack.getItem() != net.minecraft.item.Items.WOODEN_PICKAXE) return false;
        if (!stack.contains(DataComponentTypes.CUSTOM_NAME)) return false;
        String name = stack.get(DataComponentTypes.CUSTOM_NAME).getString();
        return name.contains("Pioche du Donjon");
    }

    public static ItemStack createBrokenPickaxe() {
        ItemStack broken = new ItemStack(net.minecraft.item.Items.WOODEN_PICKAXE);
        broken.set(DataComponentTypes.CUSTOM_NAME, Text.literal(BROKEN_NAME));
        return broken;
    }

    public static void onBlockBreak(ServerWorld world, ServerPlayerEntity player, BlockPos pos) {
        breakablePositions.remove(pos);
        world.playSound(null, pos, SoundEvents.BLOCK_ANVIL_LAND, SoundCategory.BLOCKS, 0.8f, 1.5f);
        world.spawnParticles(ParticleTypes.CRIT,
            pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5,
            15, 0.5, 0.5, 0.5, 0.1);

        if (breakablePositions.isEmpty()) {
            player.sendMessage(Text.literal("§6§l✦ §e§lLe donjon s'ouvre devant vous... §6§l✦"), false);
            world.playSound(null, pos, SoundEvents.ENTITY_ENDER_DRAGON_GROWL, SoundCategory.AMBIENT, 0.3f, 1.2f);
            world.spawnParticles(ParticleTypes.REVERSE_PORTAL,
                pos.getX() + 0.5, pos.getY() + 1.5, pos.getZ() + 0.5,
                30, 1.5, 1.0, 1.5, 0.05);
        }
    }
}
