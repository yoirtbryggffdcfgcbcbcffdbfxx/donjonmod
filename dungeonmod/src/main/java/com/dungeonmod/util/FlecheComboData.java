package com.dungeonmod.util;

import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.LoreComponent;
import net.minecraft.component.type.NbtComponent;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import java.util.*;

public class FlecheComboData {

    public static final Set<UUID> SANG2_ARROWS = new HashSet<>();

    private static final Map<UUID, FlecheComboData> COMBO_DATA = new HashMap<>();
    private static final String SANG_END_KEY = "dungeonmod:sangEnd";
    private static final String SANG_LEVEL_KEY = "dungeonmod:sangLevel";

    public int hits;
    public long lastHitTime;

    public static FlecheComboData getCombo(PlayerEntity player) {
        return COMBO_DATA.computeIfAbsent(player.getUuid(), k -> new FlecheComboData());
    }

    public static long getSangEnd(ItemStack stack) {
        NbtComponent comp = stack.get(DataComponentTypes.CUSTOM_DATA);
        if (comp == null) return 0;
        return comp.getNbt().getLong(SANG_END_KEY);
    }

    public static int getSangLevel(ItemStack stack) {
        NbtComponent comp = stack.get(DataComponentTypes.CUSTOM_DATA);
        if (comp == null) return 0;
        return comp.getNbt().getInt(SANG_LEVEL_KEY);
    }

    public static boolean isSangActive(ItemStack stack) {
        long end = getSangEnd(stack);
        return end > 0 && System.currentTimeMillis() < end;
    }

    public static void setSangData(ItemStack stack, int level, long durationMs) {
        NbtCompound nbt = new NbtCompound();
        nbt.putLong(SANG_END_KEY, System.currentTimeMillis() + durationMs);
        nbt.putInt(SANG_LEVEL_KEY, level);
        stack.set(DataComponentTypes.CUSTOM_DATA, NbtComponent.of(nbt));
    }

    public static void clearSangData(ItemStack stack) {
        stack.remove(DataComponentTypes.CUSTOM_DATA);
    }

    public static void checkExpired(ItemStack stack) {
        long end = getSangEnd(stack);
        if (end > 0 && System.currentTimeMillis() >= end) {
            revertArrow(stack);
        }
    }

    public static void enterSang1(ItemStack stack) {
        setSangData(stack, 1, 10000);
        stack.set(DataComponentTypes.ITEM_MODEL, Identifier.of("dungeonmod", "arrow_sang_1"));
        stack.set(DataComponentTypes.CUSTOM_NAME, Text.literal("§cFlèche §7Sang I"));
        stack.set(DataComponentTypes.LORE, new LoreComponent(List.of(
            Text.literal("§7Une flèche qui a reçu"),
            Text.literal("§7trop de sang.")
        )));
    }

    public static void enterSang2(ItemStack stack) {
        setSangData(stack, 2, 5000);
        stack.set(DataComponentTypes.ITEM_MODEL, Identifier.of("dungeonmod", "arrow_sang_2"));
        stack.set(DataComponentTypes.CUSTOM_NAME, Text.literal("§4Flèche §7Sang II"));
        stack.set(DataComponentTypes.LORE, new LoreComponent(List.of(
            Text.literal("§7Une flèche simple"),
            Text.literal("§7devenue mortelle.")
        )));
    }

    public static void extendEndTime(ItemStack stack, long extraMs) {
        long end = getSangEnd(stack);
        if (end <= 0) return;
        NbtComponent comp = stack.get(DataComponentTypes.CUSTOM_DATA);
        if (comp == null) return;
        NbtCompound nbt = comp.getNbt().copy();
        nbt.putLong(SANG_END_KEY, end + extraMs);
        stack.set(DataComponentTypes.CUSTOM_DATA, NbtComponent.of(nbt));
    }

    public static void revertArrow(ItemStack stack) {
        clearSangData(stack);
        stack.set(DataComponentTypes.ITEM_MODEL, Identifier.ofVanilla("arrow"));
        stack.set(DataComponentTypes.CUSTOM_NAME, Text.literal("§9Flèche"));
        stack.set(DataComponentTypes.LORE, new LoreComponent(List.of(
            Text.literal("§7Une flèche simple."),
            Text.literal("§7Utilisable comme poignard ou projectile.")
        )));
    }

    public static boolean isFleche(ItemStack stack) {
        if (stack.isEmpty() || !stack.isOf(Items.ARROW)) return false;
        if (!stack.contains(DataComponentTypes.CUSTOM_NAME)) return false;
        return stack.get(DataComponentTypes.CUSTOM_NAME).getString().contains("Flèche");
    }

    public static ItemStack findArrowForBow(PlayerEntity player, int[] outLevel) {
        int[][] ranges = {{9, 36}, {0, 9}, {40, 41}};
        for (int targetLevel = 2; targetLevel >= 0; targetLevel--) {
            for (int[] r : ranges) {
                for (int i = r[0]; i < r[1]; i++) {
                    ItemStack s = player.getInventory().getStack(i);
                    if (isFleche(s) && getSangLevel(s) == targetLevel) {
                        outLevel[0] = targetLevel;
                        return s;
                    }
                }
            }
        }
        return ItemStack.EMPTY;
    }
}
