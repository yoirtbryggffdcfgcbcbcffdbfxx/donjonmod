package com.dungeonmod.entity;

import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.entity.EntityType;

import java.util.*;

public class NpcRegistry {

    private static final List<NpcEntry> ENTRIES = new ArrayList<>();

    public record NpcEntry(Block block, EntityType<? extends BaseNpcEntity> type, int roomType) {}

    public static void register(Block block, EntityType<? extends BaseNpcEntity> type, int roomType) {
        ENTRIES.add(new NpcEntry(block, type, roomType));
    }

    public static List<NpcEntry> getAll() { return ENTRIES; }

    public static void init() {
        int T1 = 12, T2 = 13, T3 = 14, T4 = 15;
        for (int t : new int[]{T1, T2, T3, T4}) {
            register(Blocks.WHITE_GLAZED_TERRACOTTA, BarmanEntity.TYPE, t);
            register(Blocks.LIGHT_GRAY_GLAZED_TERRACOTTA, GaspardEntity.TYPE, t);
        }
    }
}
