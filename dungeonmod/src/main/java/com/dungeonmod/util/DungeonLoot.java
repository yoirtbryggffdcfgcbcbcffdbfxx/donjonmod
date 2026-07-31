package com.dungeonmod.util;

import net.minecraft.block.ChestBlock;
import net.minecraft.block.entity.BarrelBlockEntity;
import net.minecraft.block.entity.ChestBlockEntity;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;

import java.util.*;

public class DungeonLoot {

    private static final Set<String> ONE_TIME_PLACED = new HashSet<>();

    public record QuantityDist(int amount, int weight) {}

    public record LootEntry(ItemStack item, int spawnChance, List<QuantityDist> quantities, boolean oneTime) {
        public LootEntry(ItemStack item, int spawnChance, List<QuantityDist> quantities) { 
            this(item, spawnChance, quantities, false); 
        }

        public int getRandomQuantity(Random rand) {
            if (oneTime) return 1; // Si l'objet est unique, la quantité est toujours de 1
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
    private static final List<LootEntry> CHEST_LOOT = new ArrayList<>();
    private static final List<LootEntry> MOB_LOOT = new ArrayList<>();
    private static final Map<String, List<LootEntry>> ROOM_CHEST_LOOT = new HashMap<>();

    public static void registerRoomChest(String roomType, LootEntry entry) {
        ROOM_CHEST_LOOT.computeIfAbsent(roomType, k -> new ArrayList<>()).add(entry);
    }

    /** Loot des gobelins (normaux + lanceurs) quand ils meurent. */
    public static List<ItemStack> rollMobLoot(Random rand) {
        return rollLoot(MOB_LOOT, rand);
    }

    static {
        // ==========================================
        // BARREL LOOT (Vos valeurs d'origine intactes)
        // ==========================================
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
        if (caillou != null) {
            BARREL_LOOT.add(new LootEntry(caillou.createStack(), 60,
                List.of(new QuantityDist(1, 40), new QuantityDist(2, 40), new QuantityDist(3, 20))));
        }
        var toile_araigne = com.dungeonmod.ModItems.get("web");
        if (toile_araigne != null) {
            BARREL_LOOT.add(new LootEntry(toile_araigne.createStack(), 100,
                List.of(new QuantityDist(1, 5), new QuantityDist(2, 10), new QuantityDist(3, 40), new QuantityDist(4, 20), new QuantityDist(5, 25))));
        }
        var cuir = com.dungeonmod.ModItems.get("leather");
        if (cuir != null) {
            BARREL_LOOT.add(new LootEntry(cuir.createStack(), 30,
                List.of(new QuantityDist(1, 70), new QuantityDist(2, 20), new QuantityDist(3, 10))));
        }
        var chope_biere = com.dungeonmod.ModItems.get("chope_biere");
        if (chope_biere != null) {
            BARREL_LOOT.add(new LootEntry(chope_biere.createStack(), 50,
                List.of(new QuantityDist(1, 50), new QuantityDist(2, 40), new QuantityDist(3, 10))));
        }
        var poussiere_de_pierre = com.dungeonmod.ModItems.get("poussiere_de_pierre");
        if (poussiere_de_pierre != null) {
            BARREL_LOOT.add(new LootEntry(poussiere_de_pierre.createStack(), 100,
                List.of(new QuantityDist(1, 5), new QuantityDist(2, 10), new QuantityDist(3, 40), new QuantityDist(4, 20), new QuantityDist(5, 25))));
        }

        // ==========================================
        // CHEST LOOT (Vos valeurs d'origine intactes)
        // ==========================================
        var montre = com.dungeonmod.ModItems.get("montre");
        if (montre != null) {
            CHEST_LOOT.add(new LootEntry(montre.createStack(), 20,
                List.of(new QuantityDist(1, 100))));
        }
        var sablier = com.dungeonmod.ModItems.get("sablier");
        if (sablier != null) {
            CHEST_LOOT.add(new LootEntry(sablier.createStack(), 20,
                List.of(new QuantityDist(1, 100))));
        }
        var compas = com.dungeonmod.ModItems.get("compas_casse");
        if (compas != null) {
            CHEST_LOOT.add(new LootEntry(compas.createStack(), 20,
                List.of(new QuantityDist(1, 100))));
        }
        var tablette = com.dungeonmod.ModItems.get("tablette_de_pierre");
        if (tablette != null) {
            CHEST_LOOT.add(new LootEntry(tablette.createStack(), 20,
                List.of(new QuantityDist(1, 100))));
        }
        var anneau = com.dungeonmod.ModItems.get("anneau_basique");
        if (anneau != null) {
            CHEST_LOOT.add(new LootEntry(anneau.createStack(), 20,
                List.of(new QuantityDist(1, 100))));
        }
        var croix = com.dungeonmod.ModItems.get("croix");
        if (croix != null) {
            CHEST_LOOT.add(new LootEntry(croix.createStack(), 20,
                List.of(new QuantityDist(1, 100))));
        }
        var fragment = com.dungeonmod.ModItems.get("fragment_de_fer");
        if (fragment != null) {
            CHEST_LOOT.add(new LootEntry(fragment.createStack(), 10,
                List.of(new QuantityDist(1, 100))));
        }
        var os = com.dungeonmod.ModItems.get("os");
        if (os != null) {
            CHEST_LOOT.add(new LootEntry(os.createStack(), 25,
                List.of(new QuantityDist(1, 100))));
        }
        var pomme = com.dungeonmod.ModItems.get("pomme_rouge");
        if (pomme != null) {
            CHEST_LOOT.add(new LootEntry(pomme.createStack(), 50,
                List.of(new QuantityDist(1, 75), new QuantityDist(2, 25))));
        }
        var fleche = com.dungeonmod.ModItems.get("fleche");
        if (fleche != null) {
            CHEST_LOOT.add(new LootEntry(fleche.createStack(), 30,
                List.of(new QuantityDist(1, 75), new QuantityDist(2, 25))));
        }
        var dague = com.dungeonmod.ModItems.get("dague");
        if (dague != null) {
            CHEST_LOOT.add(new LootEntry(dague.createStack(), 10, 
                List.of(new QuantityDist(1, 100))));
        }
        var torche = com.dungeonmod.ModItems.get("torche");
        if (torche != null) {
            CHEST_LOOT.add(new LootEntry(torche.createStack(), 30, 
                List.of(new QuantityDist(1, 75), new QuantityDist(2, 25))));
        }
        var denier = com.dungeonmod.ModItems.get("denier");
        if (denier != null) {
            CHEST_LOOT.add(new LootEntry(denier.createStack(), 20, 
                List.of(new QuantityDist(1, 75), new QuantityDist(2, 20), new QuantityDist(3, 5))));
        }
        var coeur = com.dungeonmod.ModItems.get("coeur");
        if (coeur != null) {
            CHEST_LOOT.add(new LootEntry(coeur.createStack(), 10, 
                List.of(new QuantityDist(1, 100))));
        }
        var chairGobelinCrue = com.dungeonmod.ModItems.get("chair_gobelin_crue");
        if (chairGobelinCrue != null) {
            CHEST_LOOT.add(new LootEntry(chairGobelinCrue.createStack(), 30, 
                List.of(new QuantityDist(1, 100))));
        }
        var chairGobelinCuite = com.dungeonmod.ModItems.get("chair_gobelin_cuite");
        if (chairGobelinCuite != null) {
            CHEST_LOOT.add(new LootEntry(chairGobelinCuite.createStack(), 15, 
                List.of(new QuantityDist(1, 100))));
        }
        var jambiereChasseur = com.dungeonmod.ModItems.get("jambiere_chasseur");
        if (jambiereChasseur != null) {
            CHEST_LOOT.add(new LootEntry(jambiereChasseur.createStack(), 5,
                List.of(new QuantityDist(1, 100)), true));
        }
        var casqueLourd = com.dungeonmod.ModItems.get("casque_lourd");
        if (casqueLourd != null) {
            CHEST_LOOT.add(new LootEntry(casqueLourd.createStack(), 5,
                List.of(new QuantityDist(1, 100))));
        }
        var jambiereVoyageur = com.dungeonmod.ModItems.get("jambiere_voyageur");
        if (jambiereVoyageur != null) {
            CHEST_LOOT.add(new LootEntry(jambiereVoyageur.createStack(), 5,
                List.of(new QuantityDist(1, 100)), true));
        }
        var plastronVoyageur = com.dungeonmod.ModItems.get("plastron_voyageur");
        if (plastronVoyageur != null) {
            CHEST_LOOT.add(new LootEntry(plastronVoyageur.createStack(), 5,
                List.of(new QuantityDist(1, 100)), true));
        }

        // Salle loot 1 : coffre du bas → casque mineur, coffre du haut → clé
        var casqueMineur = com.dungeonmod.ModItems.get("casque_mineur");
        if (casqueMineur != null) {
            registerRoomChest("Loot1_bottom", new LootEntry(casqueMineur.createStack(), 100,
                List.of(new QuantityDist(1, 100))));
        }
        var cle = com.dungeonmod.ModItems.get("cle");
        if (cle != null) {
            registerRoomChest("Loot1_top", new LootEntry(cle.createStack(), 100,
                List.of(new QuantityDist(1, 100))));
        }

        // Clés de salle monstre (coffre de récompense RoomRewardManager)
        if (cle != null) {
            // P1 : salle adjacente à la prison (M2 ou M4) — clé obligatoire
            registerRoomChest("M2_PRISON", new LootEntry(cle.createStack(), 100,
                List.of(new QuantityDist(1, 100))));
            // P1 : salle M5 — clé obligatoire
            registerRoomChest("M5_P1", new LootEntry(cle.createStack(), 100,
                List.of(new QuantityDist(1, 100))));
            // P2 : 2 salles tirées au sort, clé garantie chacune
            registerRoomChest("M_P2_Key1", new LootEntry(cle.createStack(), 100,
                List.of(new QuantityDist(1, 100))));
            registerRoomChest("M_P2_Key2", new LootEntry(cle.createStack(), 100,
                List.of(new QuantityDist(1, 100))));
            // P3 : tables prêtes (les mobs DJ n'existent pas encore)
            registerRoomChest("M_P3_Key1", new LootEntry(cle.createStack(), 100,
                List.of(new QuantityDist(1, 100))));
            registerRoomChest("M_P3_Key2", new LootEntry(cle.createStack(), 100,
                List.of(new QuantityDist(1, 100))));
            // P4 : tables prêtes
            registerRoomChest("M_P4_Key1", new LootEntry(cle.createStack(), 100,
                List.of(new QuantityDist(1, 100))));
            registerRoomChest("M_P4_Key2", new LootEntry(cle.createStack(), 100,
                List.of(new QuantityDist(1, 100))));
        }
        var fiole = com.dungeonmod.ModItems.get("fiole");
        if (fiole != null) {
            registerRoomChest("fontaine", new LootEntry(fiole.createStack(), 100,
                List.of(new QuantityDist(1, 80), new QuantityDist(2, 20))));
        }

        // ==========================================
        // MOB LOOT : gobelins (normaux + lanceurs)
        // ==========================================
        var osGob = com.dungeonmod.ModItems.get("os");
        if (osGob != null) {
            MOB_LOOT.add(new LootEntry(osGob.createStack(), 20,
                List.of(new QuantityDist(1, 70), new QuantityDist(2, 30))));
        }
        var chairCrue = com.dungeonmod.ModItems.get("chair_gobelin_crue");
        if (chairCrue != null) {
            MOB_LOOT.add(new LootEntry(chairCrue.createStack(), 20,
                List.of(new QuantityDist(1, 50), new QuantityDist(2, 30), new QuantityDist(3, 20))));
        }
        var cuirG = com.dungeonmod.ModItems.get("leather");
        if (cuirG != null) {
            MOB_LOOT.add(new LootEntry(cuirG.createStack(), 55,
                List.of(new QuantityDist(1, 60), new QuantityDist(2, 25), new QuantityDist(3, 15))));
        }
        var coeurGob = com.dungeonmod.ModItems.get("coeur");
        if (coeurGob != null) {
            MOB_LOOT.add(new LootEntry(coeurGob.createStack(), 20,
                List.of(new QuantityDist(1, 100))));
        }
    }

    public static void addEntry(LootEntry entry) {
        BARREL_LOOT.add(entry);
    }

    public static void addChestEntry(LootEntry entry) {
        CHEST_LOOT.add(entry);
    }

    public static void resetOneTimeItems() {
        ONE_TIME_PLACED.clear();
    }

    private static List<ItemStack> rollLoot(List<LootEntry> pool, Random rand) {
        var inv = new ArrayList<ItemStack>();
        for (var entry : pool) {
            // Clé unique basée sur l'identifiant d'enregistrement de l'item (ex: dungeonmod:montre)
            String itemKey = Registries.ITEM.getId(entry.item().getItem()).toString();

            if (entry.oneTime() && ONE_TIME_PLACED.contains(itemKey)) {
                continue; // L'objet a déjà été placé dans ce donjon, on le passe
            }

            if (rand.nextInt(100) < entry.spawnChance()) {
                int qty = entry.getRandomQuantity(rand);
                for (int q = 0; q < qty; q++) {
                    var stack = entry.item().copy();
                    stack.setCount(1);
                    inv.add(stack);

                    if (entry.oneTime()) {
                        ONE_TIME_PLACED.add(itemKey);
                        break; // Empêche de générer plusieurs fois le même objet unique d'un coup
                    }
                }
            }
        }
        return inv;
    }

    public static void fillBarrel(ServerWorld world, BlockPos pos, Random rand) {
        var be = world.getBlockEntity(pos);
        if (!(be instanceof BarrelBlockEntity barrel)) return;

        var items = rollLoot(BARREL_LOOT, rand);
        if (items.isEmpty()) return;

        barrel.clear();
        var slots = new ArrayList<Integer>();
        for (int i = 0; i < 27; i++) slots.add(i);
        Collections.shuffle(slots, rand);

        for (int i = 0; i < Math.min(items.size(), 27); i++) {
            barrel.setStack(slots.get(i), items.get(i));
        }
        barrel.markDirty();
    }

    public static void fillChest(ServerWorld world, BlockPos pos, Random rand) {
        fillChest(world, pos, rand, null);
    }

    public static void fillChest(ServerWorld world, BlockPos pos, Random rand, String roomType) {
        fillChest(world, pos, rand, roomType, -1);
    }

    public static void fillChest(ServerWorld world, BlockPos pos, Random rand, String roomType, int roomBaseY) {
        if (roomType != null && roomBaseY >= 0) {
            String suffix = pos.getY() - roomBaseY >= 4 ? "_top" : "_bottom";
            String typedKey = roomType + suffix;
            if (ROOM_CHEST_LOOT.containsKey(typedKey)) {
                roomType = typedKey;
            }
        }
        fillChestInternal(world, pos, rand, roomType);
    }

    private static void fillChestInternal(ServerWorld world, BlockPos pos, Random rand, String roomType) {
        var be = world.getBlockEntity(pos);
        if (!(be instanceof ChestBlockEntity chest)) return;

        // 1. Loot spécifique à la pièce en PRIORITAIRE
        var items = new ArrayList<ItemStack>();
        if (roomType != null && ROOM_CHEST_LOOT.containsKey(roomType)) {
            items.addAll(rollLoot(ROOM_CHEST_LOOT.get(roomType), rand));
        }
        // 2. Loot générique en complément
        items.addAll(rollLoot(CHEST_LOOT, rand));
        if (items.isEmpty()) return;

        // Récupère l'inventaire (gère les coffres simples de 27 slots ainsi que les coffres doubles de 54 slots)
        var state = world.getBlockState(pos);
        Inventory inv = (state.getBlock() instanceof ChestBlock chestBlock) 
            ? ChestBlock.getInventory(chestBlock, state, world, pos, true) 
            : chest;

        if (inv == null) inv = chest;

        inv.clear();
        int size = inv.size();
        var slots = new ArrayList<Integer>();
        for (int i = 0; i < size; i++) slots.add(i);
        Collections.shuffle(slots, rand);

        for (int i = 0; i < Math.min(items.size(), size); i++) {
            inv.setStack(slots.get(i), items.get(i));
        }
        chest.markDirty();
    }
}