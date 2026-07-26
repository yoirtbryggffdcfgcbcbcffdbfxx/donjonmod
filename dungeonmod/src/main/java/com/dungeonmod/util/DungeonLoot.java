package com.dungeonmod.util;

import net.minecraft.block.entity.BarrelBlockEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;

import java.util.*;

public class DungeonLoot {

    public record QuantityDist(int amount, int weight) {}

    public record LootEntry(ItemStack item, int spawnChance, List<QuantityDist> quantities) {
        public int getRandomQuantity(Random rand) {
            int total = quantities.stream().mapToInt(QuantityDist::weight).sum();
            if (total <= 0) return 1;
            int roll = rand.nextInt(total);
            int cumul = 0;
            for (var q : quantities) {
                cumul += q.weight;
                if (roll < cumul) return q.amount;
            }
            return 1;
        }
    }

    private static final List<LootEntry> BARREL_LOOT = new ArrayList<>();

    static {
        var biere = com.dungeonmod.ModItems.get("biere_brune");
        if (biere != null) {
            BARREL_LOOT.add(new LootEntry(biere.createStack(), 40,
                List.of(new QuantityDist(1, 70), new QuantityDist(2, 20), new QuantityDist(3, 10))));
        }
        var baton = com.dungeonmod.ModItems.get("baton");
        if (baton != null) {
            BARREL_LOOT.add(new LootEntry(baton.createStack(), 50,
                List.of(new QuantityDist(1, 70), new QuantityDist(2, 20), new QuantityDist(3, 10))));
        }
        var patate = com.dungeonmod.ModItems.get("patate_douce");
        if (patate != null) {
            BARREL_LOOT.add(new LootEntry(patate.createStack(), 10,
                List.of(new QuantityDist(1, 70), new QuantityDist(2, 20), new QuantityDist(3, 10))));
        }
        var caillou = com.dungeonmod.ModItems.get("caillou");
        if (caillou != null) { // Corrigé
            BARREL_LOOT.add(new LootEntry(caillou.createStack(), 60,
                List.of(new QuantityDist(1, 40), new QuantityDist(2, 40), new QuantityDist(3, 20))));
        }
        var toile_araigne = com.dungeonmod.ModItems.get("web");
        if (toile_araigne != null) { // Corrigé
            BARREL_LOOT.add(new LootEntry(toile_araigne.createStack(), 100, // 100% de chance
                List.of(new QuantityDist(1, 5), new QuantityDist(2, 10), new QuantityDist(3, 40), new QuantityDist(4, 20), new QuantityDist(5, 25))));
        }
        var cuir = com.dungeonmod.ModItems.get("leather");
        if (cuir != null) { // Corrigé
            BARREL_LOOT.add(new LootEntry(cuir.createStack(), 30,
                List.of(new QuantityDist(1, 70), new QuantityDist(2, 20), new QuantityDist(3, 10))));
        }
    }

    public static void addEntry(LootEntry entry) {
        BARREL_LOOT.add(entry);
    }

    public static void fillBarrel(ServerWorld world, BlockPos pos, Random rand) {
        var be = world.getBlockEntity(pos);
        if (!(be instanceof BarrelBlockEntity barrel)) return;
        if (BARREL_LOOT.isEmpty()) return;

        var inv = new ArrayList<ItemStack>();

        // PASSAGE SUR LA LISTE : Test de pourcentage réel sur 100
        for (var entry : BARREL_LOOT) {
            // rand.nextInt(100) donne un nombre entre 0 et 99.
            // Si spawnChance = 100, la condition est TOUJOURS vraie !
            if (rand.nextInt(100) < entry.spawnChance()) {
                int qty = entry.getRandomQuantity(rand);
                for (int q = 0; q < qty; q++) {
                    var stack = entry.item().copy();
                    stack.setCount(1);
                    inv.add(stack);
                }
            }
        }

        if (inv.isEmpty()) return;

        barrel.clear();
        var slots = new ArrayList<Integer>();
        for (int i = 0; i < 27; i++) slots.add(i);
        Collections.shuffle(slots, rand);

        for (int i = 0; i < Math.min(inv.size(), 27); i++) {
            barrel.setStack(slots.get(i), inv.get(i));
        }

        barrel.markDirty();
    }
}