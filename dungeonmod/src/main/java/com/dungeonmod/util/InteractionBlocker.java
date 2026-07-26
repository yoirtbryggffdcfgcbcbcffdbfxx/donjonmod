package com.dungeonmod.util;

import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import java.util.HashSet;
import java.util.Set;

public class InteractionBlocker {
    private static final Set<Block> BLOCKED = new HashSet<>();

    static {
        block(
            Blocks.BREWING_STAND,
            Blocks.CRAFTING_TABLE,
            Blocks.FURNACE,
            Blocks.ANVIL,
            Blocks.CHIPPED_ANVIL,
            Blocks.DAMAGED_ANVIL,
            Blocks.STONECUTTER,
            Blocks.CARTOGRAPHY_TABLE,
            Blocks.FLETCHING_TABLE,
            Blocks.SMITHING_TABLE,
            Blocks.GRINDSTONE,
            Blocks.LOOM,
            Blocks.BLAST_FURNACE,
            Blocks.SMOKER,
            Blocks.DROPPER,
            Blocks.DISPENSER,
            Blocks.CRAFTER,
            Blocks.HOPPER
        );
    }

    public static void block(Block... blocks) {
        for (Block b : blocks) BLOCKED.add(b);
    }

    public static boolean isBlocked(Block block) {
        return BLOCKED.contains(block);
    }
}
