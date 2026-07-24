package com.dungeonmod.test;

import com.dungeonmod.DungeonMod;
import com.dungeonmod.debug.DungeonAlgo;
import com.dungeonmod.debug.DungeonViz;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.NbtList;
import net.minecraft.nbt.NbtSizeTracker;
import net.minecraft.registry.Registries;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.state.property.Property;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;

import java.io.InputStream;
import java.net.URL;
import java.util.*;

public class TestGenerator {

    private static final int CELL = 10;
    private static final int[][] DIR_OFFSET = {{1, 0}, {0, 1}, {-1, 0}, {0, -1}};
    private static final int GRID_SIZE = 64;

    private static final int PART1_TARGET_MIN = 21;
    private static final int PART1_TARGET_MAX = 25;
    private static final int PART1_MAX_I3 = 3;
    private static final int PART1_MAX_I4 = 1;
    private static final int PART1_STRAIGHT_WEIGHT = 7;

    private static final int PART2_TARGET_MIN = 18;
    private static final int PART2_TARGET_MAX = 30;
    private static final int PART2_MAX_I3 = 4;
    private static final int PART2_STRAIGHT_WEIGHT = 1;

    private static final int PART3_TARGET = 45;
    private static final int PART3_MAX_IJ3 = 3;
    private static final int PART3_MAX_IJ4 = 1;
    private static final int PART3_STRAIGHT_WEIGHT = 1;

    private static final String[] COULOIR_PATHS = {
        "/test_structures/couloir_grotte_1.nbt", "/test_structures/couloir_grotte_2.nbt", "/test_structures/couloir_grotte_3.nbt"
    };
    private static final String I3_PATH = "/test_structures/intersection_grotte_3.nbt";
    private static final String I2_PATH = "/test_structures/intersection_grotte_2.nbt";
    private static final String I4_PATH = "/test_structures/intersection_grotte_4.nbt";
    private static final String CULDESAC_PATH = "/test_structures/cul_de_sac_grotte_1.nbt";
    private static final String DOOR_CORRIDOR_PATH = "/test_structures/couloir_porte_grotte_1.nbt";
    private static final String M1_PATH = "/test_structures/salle_monstre_1.nbt";
    private static final String DEPART_PATH = "/test_structures/salle_depart.nbt";
    private static final String M2_PATH = "/test_structures/salle_monstre_2.nbt";
    private static final String PRISON_PATH = "/test_structures/salle_prison.nbt";
    private static final String LOOT1_PATH = "/test_structures/salle_loot_1.nbt";
    private static final String FONTAINE_PATH = "/test_structures/fontaine.nbt";
    private static final String PUITS_PATH = "/test_structures/couloir_puit.nbt";
    private static final String[] TAVERN_PATHS = {
        "/test_structures/taverne_coin_1.nbt", "/test_structures/taverne_coin_2.nbt",
        "/test_structures/taverne_coin_3.nbt", "/test_structures/taverne_coin_4.nbt"
    };
    private static final String[] CJ_PATHS = {
        "/test_structures/couloir_donjon_1.nbt", "/test_structures/couloir_donjon_2.nbt", "/test_structures/couloir_donjon_3.nbt"
    };
    private static final String IJ2_PATH = "/test_structures/intersection_donjon_2.nbt";
    private static final String IJ3_PATH = "/test_structures/intersection_donjon_3.nbt";
    private static final String IJ4_PATH = "/test_structures/intersection_donjon_4.nbt";
    private static final String MJ1_PATH = "/test_structures/salle_mob_donjon_1.nbt";
    private static final String MJ2_PATH = "/test_structures/salle_mob_donjon_2.nbt";
    private static final String MJ3_PATH = "/test_structures/salle_mob_donjon_3.nbt";
    private static final String MJ4_PATH = "/test_structures/salle_mob_donjon_4.nbt";
    private static final String MJ5_PATH = "/test_structures/salle_mob_donjon_5.nbt";
    private static final String LOOTDJ1_PATH = "/test_structures/salle_loot_donjon_1.nbt";
    private static final String LOOTDJ2_PATH = "/test_structures/salle_loot_donjon_2.nbt";
    private static final String LOOTDJ3_PATH = "/test_structures/salle_loot_donjon_3.nbt";
    private static final String[] CAMP_PATHS = {
        "/test_structures/campement_1.nbt", "/test_structures/campement_2.nbt",
        "/test_structures/campement_3.nbt", "/test_structures/campement_4.nbt"
    };
    private static final String BIB1_PATH = "/test_structures/bibliotheque_1.nbt";
    private static final String BIB2_PATH = "/test_structures/bibliotheque_2_ferme.nbt";
    private static final String BIB2_OPEN_PATH = "/test_structures/bibliotheque_2_ouverte.nbt";
    private static final String SHOP_PATH = "/test_structures/salle_shop.nbt";
    private static final String CULDESACDONJON_PATH = "/test_structures/cul_de_sac_donjon_1.nbt";
    private static final String PUITDJ_PATH = "/test_structures/couloir_puit_donjon_1.nbt";
    private static final String JARDIN_PATH = "/test_structures/salle_jardin.nbt";
    private static final String STATUE_PATH = "/test_structures/salle_statue.nbt";
    private static final String CENTRALE_PATH = "/test_structures/salle_centrale.nbt";
    private static final String M3_PATH = "/test_structures/salle_monstre_3.nbt";
    private static final String M4_PATH = "/test_structures/salle_monstre_4.nbt";
    private static final String OGRE_PATH = "/test_structures/salle_ogre.nbt";
    private static final String MARCHAND_NOIR_PATH = "/test_structures/salle_marchand_noir.nbt";
    private static final String CHAPELLE1_PATH = "/test_structures/salle_chapelle_1.nbt";
    private static final String CHAPELLE2_PATH = "/test_structures/salle_chapelle_2.nbt";
    private static final String CRYPTE1_PATH = "/test_structures/salle_crypte_1.nbt";
    private static final String CRYPTE2_PATH = "/test_structures/salle_crypte_2.nbt";
    private static final String PRISON_CENTRALE_1_PATH = "/test_structures/salle_prison_centrale_1.nbt";
    private static final String PRISON_CENTRALE_2_PATH = "/test_structures/salle_prison_centrale_2.nbt";
    private static final String PRISON_CENTRALE_3_PATH = "/test_structures/salle_prison_centrale_3.nbt";
    private static final String PRISON_CENTRALE_4_PATH = "/test_structures/salle_prison_centrale_4.nbt";
    private static final String PORTE_GOBELIN_PATH = "/test_structures/porte_gobelin.nbt";
    private static final String COULOIR_GOBELIN_1_PATH = "/test_structures/couloir_gobelin_1.nbt";
    private static final String INTERSECTION_GOBELIN_2_PATH = "/test_structures/intersection_gobelin_2.nbt";
    private static final String INTERSECTION_GOBELIN_3_PATH = "/test_structures/intersection_gobelin_3.nbt";
    private static final String INTERSECTION_GOBELIN_4_PATH = "/test_structures/intersection_gobelin_4.nbt";
    private static final String PUIT_GOBELIN_PATH = "/test_structures/salle_gobelin_puit.nbt";
    private static final String MARCHAND_GOBELIN_PATH = "/test_structures/salle_gobelin_marchand.nbt";
    private static final String ARMURERIE_GOBELIN_PATH = "/test_structures/armurerie_gobelin.nbt";
    private static final String CUL_DE_SAC_GOBELIN_PATH = "/test_structures/cul_de_sac_gobelin_1.nbt";
    private static final String MAISON_GOBELIN_1_PATH = "/test_structures/maison_gobelin_1.nbt";
    private static final String MAISON_GOBELIN_2_PATH = "/test_structures/maison_gobelin_2.nbt";
    private static final String MAISON_GOBELIN_3_PATH = "/test_structures/maison_gobelin_3.nbt";
    private static final String TRESORERIE_GOBELIN_PATH = "/test_structures/tresor_gobelin.nbt";

    private static BlockState[][][][] couloirDataList = new BlockState[3][][][];
    private static BlockState[][][] i3Data, i2Data, i4Data, culDeSacData, doorCorridorData;
    private static BlockState[][][] m1Data, departData, m2Data, prisonData, loot1Data;
    private static BlockState[][][] fontaineData, puitData;
    private static BlockState[][][][] tavernData = new BlockState[4][][][];
    private static BlockState[][][][] cjDataList = new BlockState[3][][][];
    private static BlockState[][][] ij2Data, ij3Data, ij4Data;
    private static BlockState[][][] mj1Data, mj2Data, mj3Data, mj4Data, mj5Data, lootdj1Data, lootdj2Data, lootdj3Data;
    private static BlockState[][][][] campDataList = new BlockState[4][][][];
    private static BlockState[][][] bib1Data, bib2Data, bib2OpenData, shopData, culDeSacDonjonData;
    private static BlockState[][][] m3Data, m4Data, ogreData, puitDjData, jardinData, statueData, centraleData, marchandNoirData, chapelle1Data, chapelle2Data, crypte1Data, crypte2Data, prisonCentrale1Data, prisonCentrale2Data, prisonCentrale3Data, prisonCentrale4Data;
    private static BlockState[][][] porteGobelinData, couloirGobelin1Data, intersectionGobelin2Data, intersectionGobelin3Data, intersectionGobelin4Data;
    private static BlockState[][][] puitGobelinData, marchandGobelinData, armurerieGobelinData, culDeSacGobelinData;
    private static BlockState[][][] maisonGobelin1Data, maisonGobelin2Data, maisonGobelin3Data;
    private static BlockState[][][] tresorerieGobelinData;

    public static class Bib2Entry {
        public int worldX, worldY, worldZ, rot;
        public boolean isOpen;
        public Bib2Entry(int x, int y, int z, int rot) { this.worldX = x; this.worldY = y; this.worldZ = z; this.rot = rot; }
    }
    public static final List<Bib2Entry> lastBib2Positions = new ArrayList<>();
    public static final List<BlockPos> lastPuitPositions = new ArrayList<>();
    private static int structSizeX, structSizeY, structSizeZ;
    private static boolean loaded = false;

    private static final int CORRIDOR = 0, I3 = 1, I2 = 2, CULDESAC = 3, M1 = 4, DEPART = 5;
    private static final int M2 = 6, PRISON = 7, LOOT1 = 8, FONTAINE = 9, PUITS = 10;
    private static final int PORTE2 = 11, T1 = 12, T2 = 13, T3 = 14, T4 = 15, I4 = 16, PORTE = 17;
    private static final int CJ1 = 18, CJ2 = 19, CJ3 = 20;
    private static final int IJ2 = 21, IJ3 = 22, IJ4 = 23;
    private static final int MJ1 = 24, MJ2 = 25, LOOTDJ1 = 26, LOOTDJ2 = 27;
    private static final int CA1 = 28, CA2 = 29, CA3 = 30, CA4 = 31;
    private static final int BIB1 = 32, BIB2 = 33, SHOP = 34;
    private static final int PORTE3 = 35, CULDJ = 36;
    private static final int M3 = 37, M4 = 38, OGRE = 39;
    private static final int MJ3 = 40, MJ4 = 41, MJ5 = 42, PUITDJ = 43, JARDIN = 44, LOOTDJ3 = 45, STATUE = 46;
    public static final int CENTRALE = 47;
    public static final int MARCHAND_NOIR = 48;
    public static final int CHAPELLE1 = 49;
    public static final int CHAPELLE2 = 50;
    public static final int CRYPTE1 = 51;
    public static final int CRYPTE2 = 52;
    public static final int PRISON_CENTRALE_1 = 53;
    public static final int PRISON_CENTRALE_2 = 54;
    public static final int PRISON_CENTRALE_3 = 55;
    public static final int PRISON_CENTRALE_4 = 56;
    public static final int PORTE_GOBELIN = 57;
    public static final int COULOIR_GOBELIN_1 = 58;
    public static final int INTERSECTION_GOBELIN_2 = 59;
    public static final int INTERSECTION_GOBELIN_3 = 60;
    public static final int INTERSECTION_GOBELIN_4 = 61;
    public static final int PUIT_GOBELIN = 62;
    public static final int MARCHAND_GOBELIN = 63;
    public static final int ARMURERIE_GOBELIN = 64;
    public static final int CUL_DE_SAC_GOBELIN = 65;
    public static final int MAISON_GOBELIN_1 = 66;
    public static final int MAISON_GOBELIN_2 = 67;
    public static final int MAISON_GOBELIN_3 = 68;
    public static final int TRESORERIE_GOBELIN = 69;

    private static int[][] roomPorts = new int[70][];
    private static int[][] roomPurplePorts = new int[69][];
    private static int[][] roomWhitePrisonPorts = new int[69][];
    private static boolean portsDetected = false;

    private static final Identifier LIGHT_BLUE_WOOL = Identifier.of("light_blue_wool");
    private static final Identifier PURPLE_WOOL = Identifier.of("purple_wool");
    private static final Identifier WHITE_WOOL = Identifier.of("white_wool");

    private static final String[] TYPE_NAMES = {
        "Couloir","I3","I2","CulSac","M1","Depart","M2","Prison","Loot1","Fontaine",
        "Puits","Porte2","T1","T2","T3","T4","I4","Porte","CJ1","CJ2","CJ3",
        "IJ2","IJ3","IJ4","MJ1","MJ2","Lootdj1","Lootdj2","Ca1","Ca2","Ca3","Ca4",
        "Bib1","Bib2","Shop","Porte3","CulDJ","M3","M4","Ogre","MJ3","MJ4","MJ5","PuitDJ","Jardin","Lootdj3","Statue","Centrale","MarchandNoir","Chapelle1","Chapelle2","Crypte1","Crypte2","PrisonC1","PrisonC2","PrisonC3","PrisonC4","PorteGob","CG1","GI2","GI3","GI4","PuitG","MarchG","ArmG","CDG","MG1","MG2","MG3","TresorG"
    };

    private static class RoomCell {
        int cx, cz, type, rot, corrIdx;
        boolean topLevel;
        RoomCell(int cx, int cz, int type, int rot, int corrIdx) {
            this.cx = cx; this.cz = cz; this.type = type; this.rot = rot; this.corrIdx = corrIdx; this.topLevel = false;
        }
        RoomCell(int cx, int cz, int type, int rot, int corrIdx, boolean topLevel) {
            this.cx = cx; this.cz = cz; this.type = type; this.rot = rot; this.corrIdx = corrIdx; this.topLevel = topLevel;
        }
    }

    public static class SpecialRoomEntry {
        public int worldX, worldZ, type;
        public SpecialRoomEntry(int worldX, int worldZ, int type) {
            this.worldX = worldX; this.worldZ = worldZ; this.type = type;
        }
    }
    public static final List<SpecialRoomEntry> lastSpecialRooms = new ArrayList<>();
    private static long lastSeed = 0;

    public static long getLastSeed() { return lastSeed; }

    private static final String SAVE_FILE = "dungeonmod_last.nbt";

    private static java.io.File getSaveFile() {
        return new java.io.File(System.getProperty("user.dir"), SAVE_FILE);
    }

    public static void saveToDisk(MinecraftServer server) {
        try {
            NbtCompound root = new NbtCompound();
            root.putLong("seed", lastSeed);
            root.putInt("originX", lastOriginX);
            root.putInt("originY", lastOriginY);
            root.putInt("originZ", lastOriginZ);
            root.putInt("departX", lastDepartX);
            root.putInt("departZ", lastDepartZ);
            NbtList list = new NbtList();
            for (SpecialRoomEntry sr : lastSpecialRooms) {
                NbtCompound t = new NbtCompound();
                t.putInt("x", sr.worldX); t.putInt("z", sr.worldZ); t.putInt("type", sr.type);
                list.add(t);
            }
            root.put("rooms", list);

            // Save goblin UUID -> texture mapping
            NbtList goblins = new NbtList();
            for (java.util.Map.Entry<java.util.UUID, net.minecraft.util.Identifier> e : com.dungeonmod.DungeonMod.zombieTextures.entrySet()) {
                NbtCompound g = new NbtCompound();
                g.putUuid("uuid", e.getKey());
                g.putString("tex", e.getValue().toString());
                BlockPos sp = com.dungeonmod.DungeonMod.zombieSpawns.get(e.getKey());
                if (sp != null) {
                    g.putInt("sx", sp.getX()); g.putInt("sy", sp.getY()); g.putInt("sz", sp.getZ());
                }
                goblins.add(g);
            }
            root.put("goblins", goblins);

            NbtIo.write(root, getSaveFile().toPath());
        } catch (Exception e) {
            System.out.println("[TestGenerator] Failed to save dungeon data: " + e.getMessage());
        }
    }

    public static boolean loadFromDisk(MinecraftServer server) {
        try {
            java.io.File file = getSaveFile();
            if (!file.exists()) return false;
            NbtCompound root = NbtIo.read(file.toPath());
            long seed = root.getLong("seed");
            int ox = root.getInt("originX");
            int oy = root.getInt("originY");
            int oz = root.getInt("originZ");
            int dx = root.getInt("departX");
            int dz = root.getInt("departZ");
            NbtList list = root.getList("rooms", 10);
            List<int[]> rooms = new ArrayList<>();
            for (int i = 0; i < list.size(); i++) {
                NbtCompound t = list.getCompound(i);
                rooms.add(new int[]{t.getInt("x"), t.getInt("z"), t.getInt("type")});
            }
            lastOriginX = ox;
            lastOriginZ = oz;
            restoreFromSave(seed, oy, dx, dz, rooms);

            // Restore goblin textures
            NbtList goblins = root.getList("goblins", 10);
            for (int i = 0; i < goblins.size(); i++) {
                NbtCompound g = goblins.getCompound(i);
                java.util.UUID uuid = g.getUuid("uuid");
                String tex = g.getString("tex");
                if (uuid != null && !tex.isEmpty()) {
                    com.dungeonmod.DungeonMod.customZombies.add(uuid);
                    com.dungeonmod.DungeonMod.zombieTextures.put(uuid,
                        net.minecraft.util.Identifier.of(tex));
                    if (g.contains("sx")) {
                        com.dungeonmod.DungeonMod.zombieSpawns.put(uuid,
                            new net.minecraft.util.math.BlockPos(g.getInt("sx"), g.getInt("sy"), g.getInt("sz")));
                    }
                }
            }

            return true;
        } catch (Exception e) {
            System.out.println("[TestGenerator] Failed to load dungeon data: " + e.getMessage());
            return false;
        }
    }

    public static void restoreFromSave(long seed, int originY, int departX, int departZ,
                                        List<int[]> specialRooms) {
        lastSeed = seed;
        lastOriginY = originY;
        lastDepartX = departX;
        lastDepartZ = departZ;
        lastSpecialRooms.clear();
        lastPuitPositions.clear();
        for (int[] sr : specialRooms) {
            lastSpecialRooms.add(new SpecialRoomEntry(sr[0], sr[1], sr[2]));
            if (sr[2] == 10 || sr[2] == 43 || sr[2] == 62) {
                lastPuitPositions.add(new BlockPos(sr[0], 0, sr[1]));
            }
        }
    }

    public static Map<String, Integer> getLabelToType() { return LABEL_TO_TYPE; }

    public static String[] getRoomTypeNames() {
        return new String[]{
            "Depart", "Prison", "Loot1", "M1", "M2", "Shop",
            "T1", "T2", "T3", "T4", "Ca1", "Ca2", "Ca3", "Ca4",
            "Bib1", "Bib2", "Fontaine", "Puits", "Porte",
            "Porte2", "Porte3", "MJ1", "MJ2", "Lootdj1", "Lootdj2",
            "CulDJ", "CulSac", "MarchandNoir", "Chapelle1", "Chapelle2", "Crypte1", "Crypte2",
            "PrisonC1", "PrisonC2", "PrisonC3", "PrisonC4",
            "PorteGob","CG1","GI2","GI3","GI4","PuitG","MarchG","ArmG","CDG","MG1","MG2","MG3"
        };
    }

    private static int lastDepartX, lastDepartZ, lastOriginY;
    private static int lastOriginX, lastOriginZ;

    public static int getLastOriginX() { return lastOriginX; }
    public static int getLastOriginZ() { return lastOriginZ; }

    public static void generateRandomCave(ServerWorld world, BlockPos origin, int maxRooms) {
        generateRandomCave(world, origin, maxRooms, 0);
    }

    public static void generateRandomCave(ServerWorld world, BlockPos origin, int maxRooms, long seed) {
        loadAll();
        int ox = origin.getX(), oy = origin.getY(), oz = origin.getZ();
        DungeonAlgo.DungeonResult result = (seed != 0) ? generateWithSeed(seed) : generateValidDungeon();
        if (result == null) {
            System.out.println("[TestGenerator] ECHEC");
            return;
        }
        // Ajouter les sorties P4 depuis les ports violets du hub NBT (avant le rendu HTML)
        if (result.topLabels != null) {
            String hubKey = null;
            for (var e : result.topLabels.entrySet()) {
                if (e.getValue().equals("Centrale")) { hubKey = e.getKey(); break; }
            }
            if (hubKey != null && centraleData != null) {
                String[] hp = hubKey.split(",");
                int hubX = Integer.parseInt(hp[0]), hubZ = Integer.parseInt(hp[1]);
                for (var e : detectCentraleExits(centraleData, hubX, hubZ).entrySet()) {
                    if (!result.topLabels.containsKey(e.getKey())) result.topLabels.put(e.getKey(), e.getValue());
                }
            }
        }
        try {
            DungeonViz.renderToHtml(result.adj, result.labels, result.p4Adj, result.topLabels, result.startKey, lastSeed, "dungeon_viz.html");
        } catch (Exception e) {
            System.out.println("[TestGenerator] Erreur sauvegarde visualisation: " + e.getMessage());
        }

        List<RoomCell> cells = convertGraphToCells(result);
        com.dungeonmod.DungeonMod.customZombies.clear();
        com.dungeonmod.DungeonMod.zombieTextures.clear();
        com.dungeonmod.DungeonMod.zombieSpawns.clear();
        lastSpecialRooms.clear();
        lastBib2Positions.clear();
        lastPuitPositions.clear();
        com.dungeonmod.DungeonMod.lastAnchorSpawn.clear();
        lastOriginX = ox; lastOriginZ = oz; lastDepartX = 0; lastDepartZ = 0; lastOriginY = oy;
        for (RoomCell rc : cells) {
            int wy = rc.topLevel ? oy + 10 : oy;
            int wx = ox + rc.cx * CELL, wz = oz + rc.cz * CELL;
            placeRoom(world, wx, wy, wz, getData(rc.type, rc.corrIdx), rc.rot);
            com.dungeonmod.entity.NpcSpawnHelper.scanRoom(world, wx, wy, wz, CELL, rc.type);
            if (rc.type == DEPART) { lastDepartX = wx + CELL / 2; lastDepartZ = wz + CELL / 2; }
            if (rc.type != CORRIDOR && rc.type != I2 && rc.type != I3 && rc.type != I4
                && rc.type != CJ1 && rc.type != CJ2 && rc.type != CJ3
                && rc.type != IJ2 && rc.type != IJ3 && rc.type != IJ4) {
                lastSpecialRooms.add(new SpecialRoomEntry(wx, wz, rc.type));
            }
            if (rc.type == BIB2) {
                lastBib2Positions.add(new Bib2Entry(wx, wy, wz, rc.rot));
            }
            if (rc.type == PUITS || rc.type == PUITDJ || rc.type == PUIT_GOBELIN) {
                lastPuitPositions.add(new BlockPos(wx, wy, wz));
            }
        }
        logDungeon(result, cells);
        validateAdjacency(cells);

        // Register dungeon for handleDungeonFood
        com.dungeonmod.DungeonMod.lastDepartPos = new net.minecraft.util.math.BlockPos(lastDepartX, oy + 1, lastDepartZ);
        com.dungeonmod.DungeonMod.addDungeon(
            "TestGen_" + System.currentTimeMillis(),
            origin,
            result.adj.size(), result.adj.size(),
            world.getRegistryKey().getValue(),
            0, null
        );

        // Spawn gobelins in monster rooms
        spawnGoblins(world, cells, ox, oy, oz);

        // Save to disk
        saveToDisk(world.getServer());
    }

    private static void spawnGoblins(ServerWorld world, List<RoomCell> cells, int ox, int oy, int oz) {
        int MOB1 = 4, MOB2 = 6, M3 = 37, M4 = 38;
        java.util.Random rnd = new java.util.Random();
        // M1/M2 room offsets
        int[][] normalOffsets = {{2, 2}, {7, 2}, {4, 7}};
        // M3/M4 room offsets: 2 normaux en bas, 3 lanceurs sur plateforme
        int[][] bottomOffsets = {{2, 2}, {7, 2}};
        int[][] topOffsets = {{2, 5}, {7, 5}, {4, 8}};

        for (RoomCell rc : cells) {
            if (rc.topLevel) continue;
            int wx = ox + rc.cx * CELL;
            int wz = oz + rc.cz * CELL;

            if (rc.type == MOB1 || rc.type == MOB2) {
                for (int[] off : normalOffsets) {
                    int rx = rotateX(off[0], off[1], rc.rot);
                    int rz = rotateZ(off[0], off[1], rc.rot);
                    double sx = wx + rx + 0.5;
                    double sz = wz + rz + 0.5;
                    spawnNormalGoblin(world, sx, oy + 1.0, sz, oy);
                }
            } else if (rc.type == OGRE) {
                int rx = rotateX(4, 4, rc.rot);
                int rz = rotateZ(4, 4, rc.rot);
                double sx = wx + rx + 0.5;
                double sz = wz + rz + 0.5;
                var ogre = new com.dungeonmod.entity.OgreEntity(
                    com.dungeonmod.entity.OgreEntity.TYPE, world);
                ogre.setPosition(sx, oy + 1.0, sz);
                ogre.setPersistent();
                ogre.setCustomName(net.minecraft.text.Text.literal("§eCyclope"));
                ogre.setCustomNameVisible(false);
                ogre.roomMinX = wx;
                ogre.roomMaxX = wx + CELL;
                ogre.roomMinZ = wz;
                ogre.roomMaxZ = wz + CELL;
                // Set facing toward entrance (based on room rotation)
                int[] ports = getWorldPorts(OGRE, rc.rot);
                if (ports.length > 0) ogre.roomFacing = ports[0] * 90.0f;
                world.spawnEntity(ogre);
            } else if (rc.type == M3 || rc.type == M4) {
                // Gobelins normaux en bas (milieu de salle, hors plateforme)
                int[][] m34BottomOffsets = {{3, 6}, {6, 6}};
                for (int[] off : m34BottomOffsets) {
                    int rx = rotateX(off[0], off[1], rc.rot);
                    int rz = rotateZ(off[0], off[1], rc.rot);
                    double sx = wx + rx + 0.5;
                    double sz = wz + rz + 0.5;
                    spawnNormalGoblin(world, sx, oy + 1.0, sz, oy);
                }
                // Lanceurs de pierre sur la plateforme (Y=3 dans la structure)
                int[][] platformOffsets = {{3, 2}, {6, 2}};
                for (int[] off : platformOffsets) {
                    int rx = rotateX(off[0], off[1], rc.rot);
                    int rz = rotateZ(off[0], off[1], rc.rot);
                    double sx = wx + rx + 0.5;
                    double sz = wz + rz + 0.5;
                    spawnStoneThrower(world, sx, oy + 4.0, sz, oy);
                }
            }
        }
    }

    private static void spawnNormalGoblin(ServerWorld world, double x, double y, double z, int oy) {
        var zombie = new net.minecraft.entity.mob.ZombieEntity(
            net.minecraft.entity.EntityType.ZOMBIE, world);
        zombie.setPosition(x, y, z);
        zombie.setPersistent();
        zombie.setCustomName(net.minecraft.text.Text.literal("§aGobelin"));
        zombie.setCustomNameVisible(false);

        var attr = zombie.getAttributeInstance(net.minecraft.entity.attribute.EntityAttributes.FOLLOW_RANGE);
        if (attr != null) attr.setBaseValue(5.0);
        var hpAttr = zombie.getAttributeInstance(net.minecraft.entity.attribute.EntityAttributes.MAX_HEALTH);
        if (hpAttr != null) { hpAttr.setBaseValue(10.0); zombie.setHealth(10.0f); }
        var dmgAttr = zombie.getAttributeInstance(net.minecraft.entity.attribute.EntityAttributes.ATTACK_DAMAGE);
        if (dmgAttr != null) dmgAttr.setBaseValue(2.0);
        var speedAttr = zombie.getAttributeInstance(net.minecraft.entity.attribute.EntityAttributes.MOVEMENT_SPEED);
        if (speedAttr != null) speedAttr.setBaseValue(speedAttr.getBaseValue() * 1.4);

        com.dungeonmod.DungeonMod.customZombies.add(zombie.getUuid());
        com.dungeonmod.DungeonMod.zombieTextures.put(zombie.getUuid(),
            net.minecraft.util.Identifier.of("dungeonmod", "textures/entity/gobelin_1.png"));
        com.dungeonmod.DungeonMod.zombieSpawns.put(zombie.getUuid(),
            new net.minecraft.util.math.BlockPos((int)x, (int)y, (int)z));

        zombie.addCommandTag("dg_" + (int)x + "_" + (int)y + "_" + (int)z);
        world.spawnEntity(zombie);
    }

    private static void spawnStoneThrower(ServerWorld world, double x, double y, double z, int oy) {
        DungeonMod.LOGGER.info("[spawnStoneThrower] Spawning at {},{}", x, z);
        var goblin = new com.dungeonmod.entity.StoneThrowerGoblinEntity(
            com.dungeonmod.entity.StoneThrowerGoblinEntity.THROWER_TYPE, world);
        goblin.setPosition(x, y, z);
        goblin.setPersistent();
        goblin.setCustomName(net.minecraft.text.Text.literal("§aGobelin"));
        goblin.setCustomNameVisible(false);

        var attr = goblin.getAttributeInstance(net.minecraft.entity.attribute.EntityAttributes.FOLLOW_RANGE);
        if (attr != null) attr.setBaseValue(8.0);
        var hpAttr = goblin.getAttributeInstance(net.minecraft.entity.attribute.EntityAttributes.MAX_HEALTH);
        if (hpAttr != null) { hpAttr.setBaseValue(14.0); goblin.setHealth(14.0f); }

        // Set platform position for AI
        goblin.setPlatformPos(new net.minecraft.util.math.BlockPos((int)x, (int)y, (int)z));
        DungeonMod.LOGGER.info("[spawnStoneThrower] Done UUID={}", goblin.getUuid());

        com.dungeonmod.DungeonMod.customZombies.add(goblin.getUuid());
        com.dungeonmod.DungeonMod.zombieTextures.put(goblin.getUuid(),
            net.minecraft.util.Identifier.of("dungeonmod", "textures/entity/gobelin_2.png"));
        com.dungeonmod.DungeonMod.zombieSpawns.put(goblin.getUuid(),
            new net.minecraft.util.math.BlockPos((int)x, (int)y, (int)z));

        goblin.addCommandTag("dg_" + (int)x + "_" + (int)y + "_" + (int)z);
        world.spawnEntity(goblin);
    }

    public static int getLastDepartX() { return lastDepartX; }
    public static int getLastDepartZ() { return lastDepartZ; }
    public static int getLastOriginY() { return lastOriginY; }

    public static boolean toggleBib2(ServerWorld world, BlockPos clickedPos) {
        for (Bib2Entry entry : lastBib2Positions) {
            if (clickedPos.getX() >= entry.worldX && clickedPos.getX() < entry.worldX + structSizeX
                && clickedPos.getY() >= entry.worldY && clickedPos.getY() < entry.worldY + structSizeY
                && clickedPos.getZ() >= entry.worldZ && clickedPos.getZ() < entry.worldZ + structSizeZ) {
                BlockState[][][] targetData = entry.isOpen ? bib2Data : bib2OpenData;
                if (targetData == null) return false;
                // Only replace blocks that actually differ (lanterns stay untouched)
                for (int x = 0; x < structSizeX; x++) {
                    for (int y = 0; y < structSizeY; y++) {
                        for (int z = 0; z < structSizeZ; z++) {
                            int rx = rotateX(x, z, entry.rot);
                            int rz = rotateZ(x, z, entry.rot);
                            BlockPos worldPos = new BlockPos(entry.worldX + rx, entry.worldY + y, entry.worldZ + rz);
                            BlockState current = world.getBlockState(worldPos);
                            BlockState target = targetData[x][y][z];
                            if (target == null) target = net.minecraft.block.Blocks.AIR.getDefaultState();
                            BlockState rotatedTarget = rotateBlockState(target, entry.rot);
                            if (!current.equals(rotatedTarget)) {
                                world.setBlockState(worldPos, rotatedTarget, 2);
                            }
                        }
                    }
                }
                entry.isOpen = !entry.isOpen;
                return true;
            }
        }
        return false;
    }

    private static class DungeonResult extends DungeonAlgo.DungeonResult {}

    private static class TreeResult {
        String startKey;
        int startX, startY;
        Map<String, Set<String>> adj;
    }

    private static class TavernResult {
        Map<String, String> tavern;
        String exitKey;
        Set<String> pathSet;
    }

    private static class CampResult {
        String campExit;
        Set<String> campPathSet;
        Map<String, String> campNodes;
    }

    private static class RoomConfig {
        String type;
        List<String> doors;
        boolean turnAfter2;
        RoomConfig(String type, List<String> doors, boolean turnAfter2) {
            this.type = type; this.doors = doors; this.turnAfter2 = turnAfter2;
        }
    }

    private static final Map<String, RoomConfig> ROOM_CONFIGS = new LinkedHashMap<>();
    static {
        ROOM_CONFIGS.put("D",        new RoomConfig("Cul de sac",    List.of("S"), false));
        ROOM_CONFIGS.put("Prison",   new RoomConfig("Cul de sac",    List.of("N"), false));
        ROOM_CONFIGS.put("porte",    new RoomConfig("Couloir droit", List.of(), false));
        ROOM_CONFIGS.put("T1",       new RoomConfig("Couloir droit", List.of("N","S"), false));
        ROOM_CONFIGS.put("T2",       new RoomConfig("Virage",        List.of("N","E"), false));
        ROOM_CONFIGS.put("T3",       new RoomConfig("Virage",        List.of("E","S"), false));
        ROOM_CONFIGS.put("T4",       new RoomConfig("Virage",        List.of("S","E"), false));
        ROOM_CONFIGS.put("Loot1",    new RoomConfig("Cul de sac",    List.of("N"), false));
        ROOM_CONFIGS.put("cul",      new RoomConfig("Cul de sac",    List.of("N"), false));
        ROOM_CONFIGS.put("M1",       new RoomConfig("Cul de sac",    List.of("N"), false));
        ROOM_CONFIGS.put("M2",       new RoomConfig("Couloir droit", List.of("N","S"), true));
        ROOM_CONFIGS.put("C1",       new RoomConfig("Couloir droit", List.of("N","S"), true));
        ROOM_CONFIGS.put("C2",       new RoomConfig("Couloir droit", List.of("N","S"), true));
        ROOM_CONFIGS.put("C3",       new RoomConfig("Couloir droit", List.of("N","S"), true));
        ROOM_CONFIGS.put("puit",     new RoomConfig("Couloir droit", List.of("N","S"), true));
        ROOM_CONFIGS.put("fontaine", new RoomConfig("Cul de sac",    List.of("N"), false));
        ROOM_CONFIGS.put("porte2",   new RoomConfig("Couloir droit", List.of("N","S"), false));
        ROOM_CONFIGS.put("CJ1",      new RoomConfig("Couloir droit", List.of("N","S"), true));
        ROOM_CONFIGS.put("CJ2",      new RoomConfig("Couloir droit", List.of("N","S"), true));
        ROOM_CONFIGS.put("CJ3",      new RoomConfig("Couloir droit", List.of("N","S"), true));
        ROOM_CONFIGS.put("IJ2",      new RoomConfig("Virage",        List.of("N","E"), false));
        ROOM_CONFIGS.put("IJ3",      new RoomConfig("Intersection",  List.of("E","N","S"), false));
        ROOM_CONFIGS.put("IJ4",      new RoomConfig("Intersection",  List.of("N","E","S","W"), false));
        ROOM_CONFIGS.put("MJ1",      new RoomConfig("Cul de sac",    List.of("N"), false));
        ROOM_CONFIGS.put("MJ2",      new RoomConfig("Couloir droit", List.of("N","S"), true));
        ROOM_CONFIGS.put("Lootdj1",  new RoomConfig("Cul de sac",    List.of("N"), false));
        ROOM_CONFIGS.put("Lootdj2",  new RoomConfig("Couloir droit", List.of("N","S"), true));
        ROOM_CONFIGS.put("porte3",   new RoomConfig("Cul de sac",    List.of("N"), false));
        ROOM_CONFIGS.put("Ca1",      new RoomConfig("Couloir droit", List.of("S","N"), false));
        ROOM_CONFIGS.put("Ca2",      new RoomConfig("Virage",        List.of("S","W"), false));
        ROOM_CONFIGS.put("Ca3",      new RoomConfig("Intersection",  List.of("E","N","S"), false));
        ROOM_CONFIGS.put("Ca4",      new RoomConfig("Cul de sac",    List.of("N"), false));
        ROOM_CONFIGS.put("Bib1",     new RoomConfig("Couloir droit", List.of("N","S"), false));
        ROOM_CONFIGS.put("Bib2",     new RoomConfig("Cul de sac",    List.of("S"), false));
        ROOM_CONFIGS.put("Shop",     new RoomConfig("Cul de sac",    List.of("N"), false));
        ROOM_CONFIGS.put("I2",       new RoomConfig("Virage",        List.of("N","E"), false));
        ROOM_CONFIGS.put("I3",       new RoomConfig("Intersection",  List.of("N","S","E"), false));
        ROOM_CONFIGS.put("I4",       new RoomConfig("Intersection",  List.of("N","S","E","W"), false));
        ROOM_CONFIGS.put("culDJ",    new RoomConfig("Cul de sac",    List.of("N"), false));
        ROOM_CONFIGS.put("MarchandNoir", new RoomConfig("Cul de sac", List.of("N"), false));
        ROOM_CONFIGS.put("Chapelle1", new RoomConfig("Couloir droit", List.of("N","S"), false));
        ROOM_CONFIGS.put("Chapelle2", new RoomConfig("Couloir droit", List.of("N","S"), true));
        ROOM_CONFIGS.put("Crypte1", new RoomConfig("Couloir droit", List.of("N","S"), false));
        ROOM_CONFIGS.put("Crypte2", new RoomConfig("Cul de sac", List.of("N"), false));
        ROOM_CONFIGS.put("PrisonC1", new RoomConfig("Couloir droit", List.of("N","S"), false));
        ROOM_CONFIGS.put("PrisonC2", new RoomConfig("Virage", List.of("N","E"), false));
        ROOM_CONFIGS.put("PrisonC3", new RoomConfig("Virage", List.of("N","E"), false));
        ROOM_CONFIGS.put("PrisonC4", new RoomConfig("Cul de sac", List.of("N"), false));
        ROOM_CONFIGS.put("PorteGob", new RoomConfig("Couloir droit", List.of("N","S"), false));
        ROOM_CONFIGS.put("CG1", new RoomConfig("Couloir droit", List.of("N","S"), true));
        ROOM_CONFIGS.put("GI2", new RoomConfig("Virage", List.of("N","E"), false));
        ROOM_CONFIGS.put("GI3", new RoomConfig("Intersection", List.of("E","N","S"), false));
        ROOM_CONFIGS.put("GI4", new RoomConfig("Intersection", List.of("N","E","S","W"), false));
        ROOM_CONFIGS.put("PuitG", new RoomConfig("Couloir droit", List.of("N","S"), true));
        ROOM_CONFIGS.put("MarchG", new RoomConfig("Cul de sac", List.of("N"), false));
        ROOM_CONFIGS.put("TresorG", new RoomConfig("Cul de sac", List.of("N"), false));
        ROOM_CONFIGS.put("ArmG", new RoomConfig("Cul de sac", List.of("N"), false));
        ROOM_CONFIGS.put("CDG", new RoomConfig("Cul de sac", List.of("N"), false));
        ROOM_CONFIGS.put("MG1", new RoomConfig("Cul de sac", List.of("N"), false));
        ROOM_CONFIGS.put("MG2", new RoomConfig("Virage", List.of("N","E"), false));
        ROOM_CONFIGS.put("MG3", new RoomConfig("Intersection", List.of("E","N","S"), false));
    }

    private static final List<String> CORRIDOR_TYPES = List.of("C1", "C2", "C3");
    private static final List<String> CJ_TYPES = List.of("CJ1", "CJ2", "CJ3");

    private static String pickC(Random rng) { return CORRIDOR_TYPES.get(rng.nextInt(3)); }
    private static String pickCJ(Random rng) { return CJ_TYPES.get(rng.nextInt(3)); }

    private static boolean hasPrisonCandidate(Map<String, Set<String>> adj, String startKey) {
        for (var e : adj.entrySet()) {
            if (e.getValue().size() != 1 || e.getKey().equals(startKey)) continue;
            String leaf = e.getKey();
            String parent = adj.get(leaf).iterator().next();
            if (adj.get(parent).size() == 2) {
                Set<String> pn = new HashSet<>(adj.get(parent)); pn.remove(leaf);
                String gp = pn.iterator().next();
                int pdx = Integer.parseInt(parent.split(",")[0]) - Integer.parseInt(gp.split(",")[0]);
                int pdy = Integer.parseInt(parent.split(",")[1]) - Integer.parseInt(gp.split(",")[1]);
                int cdx = Integer.parseInt(leaf.split(",")[0]) - Integer.parseInt(parent.split(",")[0]);
                int cdy = Integer.parseInt(leaf.split(",")[1]) - Integer.parseInt(parent.split(",")[1]);
                if (pdx == cdx && pdy == cdy) return true;
            }
        }
        return false;
    }

    // ===================== Tree generation =====================

    private static TreeResult generateRawTree(int targetMin, int targetMax, int maxI3, int maxI4,
                                               int straightWeight, Integer[] start, Set<String> blocked) {
        if (blocked == null) blocked = new HashSet<>();
        Random rng = new Random();
        int sx = start != null ? start[0] : GRID_SIZE / 2;
        int sy = start != null ? start[1] : GRID_SIZE / 2;
        String startKey = sx + "," + sy;
        Set<String> occupied = new HashSet<>();
        occupied.add(startKey);
        Map<String, Set<String>> adj = new HashMap<>();
        adj.put(startKey, new HashSet<>());
        List<String> allNodes = new ArrayList<>();
        allNodes.add(startKey);
        Map<String, int[]> entryDir = new HashMap<>();

        List<int[]> dirs = new ArrayList<>(Arrays.asList(DIR_OFFSET));
        Collections.shuffle(dirs, rng);
        boolean found = false;
        int fdx = 0, fdy = 0;
        for (int[] d : dirs) {
            int tx = sx + d[0], ty = sy + d[1];
            if (tx >= 0 && tx < GRID_SIZE && ty >= 0 && ty < GRID_SIZE && !blocked.contains(tx + "," + ty)) {
                fdx = d[0]; fdy = d[1]; found = true; break;
            }
        }
        if (!found) { TreeResult tr = new TreeResult(); tr.startKey = startKey; tr.startX = sx; tr.startY = sy; tr.adj = adj; return tr; }

        String firstKey = (sx + fdx) + "," + (sy + fdy);
        occupied.add(firstKey); allNodes.add(firstKey);
        adj.put(firstKey, new HashSet<>());
        adj.get(startKey).add(firstKey); adj.get(firstKey).add(startKey);
        entryDir.put(firstKey, new int[]{fdx, fdy});

        int targetSize = targetMin + rng.nextInt(targetMax - targetMin + 1);
        while (allNodes.size() < targetSize) {
            List<String[]> candidates = new ArrayList<>();
            int cI3 = 0, cI4 = 0;
            for (Set<String> nb : adj.values()) { int d = nb.size(); if (d == 3) cI3++; else if (d == 4) cI4++; }
            for (String node : allNodes) {
                if (node.equals(startKey)) continue;
                int deg = adj.get(node).size();
                if (deg == 2 && cI3 >= maxI3) continue;
                if (deg == 3 && cI4 >= maxI4) continue;
                if (deg >= 4) continue;
                String[] pp = node.split(",");
                int px = Integer.parseInt(pp[0]), py = Integer.parseInt(pp[1]);
                for (int[] d : DIR_OFFSET) {
                    int nx = px + d[0], ny = py + d[1];
                    if (nx >= 0 && nx < GRID_SIZE && ny >= 0 && ny < GRID_SIZE) {
                        String nk = nx + "," + ny;
                        if (!occupied.contains(nk) && !blocked.contains(nk)) candidates.add(new String[]{node, nk});
                    }
                }
            }
            List<String[]> weighted = new ArrayList<>();
            for (String[] cand : candidates) {
                int w = 1;
                String[] pp = cand[0].split(",");
                int px = Integer.parseInt(pp[0]), py = Integer.parseInt(pp[1]);
                int deg = adj.get(cand[0]).size();
                int[] ed = entryDir.get(cand[0]);
                if (deg == 1 && ed != null) {
                    String[] cp = cand[1].split(",");
                    int cx = Integer.parseInt(cp[0]), cy = Integer.parseInt(cp[1]);
                    if ((cx - px) == ed[0] && (cy - py) == ed[1]) w = straightWeight;
                }
                for (int i = 0; i < w; i++) weighted.add(cand);
            }
            if (weighted.isEmpty()) break;
            String[] choice = weighted.get(rng.nextInt(weighted.size()));
            String parent = choice[0], child = choice[1];
            String[] pp = parent.split(","), cp = child.split(",");
            occupied.add(child); allNodes.add(child);
            adj.put(child, new HashSet<>());
            adj.get(parent).add(child); adj.get(child).add(parent);
            entryDir.put(child, new int[]{Integer.parseInt(cp[0]) - Integer.parseInt(pp[0]), Integer.parseInt(cp[1]) - Integer.parseInt(pp[1])});
        }
        TreeResult tr = new TreeResult(); tr.startKey = startKey; tr.startX = sx; tr.startY = sy; tr.adj = adj; return tr;
    }

    private static TreeResult generatePart1Tree() { return generateRawTree(PART1_TARGET_MIN, PART1_TARGET_MAX, PART1_MAX_I3, PART1_MAX_I4, PART1_STRAIGHT_WEIGHT, null, null); }
    private static TreeResult generatePart2Tree(String startKey, Set<String> blocked) { String[] s = startKey.split(","); return generateRawTree(PART2_TARGET_MIN, PART2_TARGET_MAX, PART2_MAX_I3, 1, PART2_STRAIGHT_WEIGHT, new Integer[]{Integer.parseInt(s[0]), Integer.parseInt(s[1])}, blocked); }
    private static TreeResult generatePart3Tree(String startKey, Set<String> blocked) { String[] s = startKey.split(","); return generateRawTree(PART3_TARGET, PART3_TARGET, PART3_MAX_IJ3, PART3_MAX_IJ4, PART3_STRAIGHT_WEIGHT, new Integer[]{Integer.parseInt(s[0]), Integer.parseInt(s[1])}, blocked); }

    // ===================== Algo: analyzePart1 =====================

    private static Map<String, String> analyzePart1(String startKey, Map<String, Set<String>> adj, Random rng) {
        Map<String, String> labels = new HashMap<>();
        List<String> leaves = new ArrayList<>();
        for (var e : adj.entrySet()) if (e.getValue().size() == 1 && !e.getKey().equals(startKey)) leaves.add(e.getKey());
        if (leaves.size() < 4) return null;
        Collections.shuffle(leaves, rng);

        String prison = null, prisonParent = null;
        for (String leaf : leaves) {
            String parent = adj.get(leaf).iterator().next();
            if (adj.get(parent).size() == 2) {
                Set<String> pn = new HashSet<>(adj.get(parent)); pn.remove(leaf);
                String gp = pn.iterator().next();
                int pdx = Integer.parseInt(parent.split(",")[0]) - Integer.parseInt(gp.split(",")[0]);
                int pdy = Integer.parseInt(parent.split(",")[1]) - Integer.parseInt(gp.split(",")[1]);
                int cdx = Integer.parseInt(leaf.split(",")[0]) - Integer.parseInt(parent.split(",")[0]);
                int cdy = Integer.parseInt(leaf.split(",")[1]) - Integer.parseInt(parent.split(",")[1]);
                if (pdx == cdx && pdy == cdy) { prison = leaf; prisonParent = parent; break; }
            }
        }
        if (prison == null) return null;

        List<String> others = new ArrayList<>();
        for (String l : leaves) if (!l.equals(prison)) others.add(l);

        String porte = others.get(0);
        labels.put(startKey, "D");
        labels.put(porte, "porte");
        labels.put(prison, "Prison");
        labels.put(prisonParent, "M2");
        labels.put(others.get(1), "Loot1");

        for (int i = 2; i < others.size(); i++) labels.put(others.get(i), "cul");

        List<String> cNodes = new ArrayList<>(), i2Nodes = new ArrayList<>(), i3Nodes = new ArrayList<>(), i4Nodes = new ArrayList<>();
        for (var e : adj.entrySet()) {
            if (labels.containsKey(e.getKey())) continue;
            int deg = e.getValue().size();
            if (deg == 2) {
                List<String> nb = new ArrayList<>(e.getValue());
                String[] p1 = nb.get(0).split(","), p2 = nb.get(1).split(",");
                if (p1[0].equals(p2[0]) || p1[1].equals(p2[1])) cNodes.add(e.getKey());
                else i2Nodes.add(e.getKey());
            } else if (deg == 3) i3Nodes.add(e.getKey());
            else if (deg == 4) i4Nodes.add(e.getKey());
        }
        for (String n : cNodes) if (!labels.containsKey(n)) labels.put(n, pickC(rng));
        for (String n : i2Nodes) labels.put(n, "I2");
        for (String n : i3Nodes) labels.put(n, "I3");
        for (String n : i4Nodes) labels.put(n, "I4");
        return labels;
    }

    // ===================== Algo: placeTavernAndPath (v2 forced I2 turn) =====================

    private static TavernResult placeTavernAndPath(Map<String, Set<String>> adj, String porteKey, Random rng) {
        String[] pp = porteKey.split(",");
        int px = Integer.parseInt(pp[0]), py = Integer.parseInt(pp[1]);
        String parent = adj.get(porteKey).iterator().next();
        String[] pap = parent.split(",");
        int[] continueDir = {px - Integer.parseInt(pap[0]), py - Integer.parseInt(pap[1])};

        int dx = continueDir[0], dy = continueDir[1];
        int preTurn = 2, postTurn = 2 + rng.nextInt(2);
        List<int[]> pathCells = new ArrayList<>();
        int cx = px, cy = py;
        boolean ok = true;

        for (int i = 0; i < preTurn; i++) {
            int nx = cx + dx, ny = cy + dy;
            if (adj.containsKey(nx + "," + ny) || nx < 0 || nx >= GRID_SIZE || ny < 0 || ny >= GRID_SIZE) { ok = false; break; }
            pathCells.add(new int[]{nx, ny}); cx = nx; cy = ny;
        }
        if (!ok) return null;

        int[][] perpDirs = {{dy, -dx}, {-dy, dx}};
        List<int[]> perpList = new ArrayList<>(Arrays.asList(perpDirs));
        Collections.shuffle(perpList, rng);
        boolean turned = false;
        for (int[] pd : perpList) {
            int nx = cx + pd[0], ny = cy + pd[1];
            if (!adj.containsKey(nx + "," + ny) && nx >= 0 && nx < GRID_SIZE && ny >= 0 && ny < GRID_SIZE) {
                pathCells.add(new int[]{nx, ny}); cx = nx; cy = ny; turned = true; break;
            }
        }
        if (!turned) return null;

        for (int i = 0; i < postTurn; i++) {
            int nx = cx + dx, ny = cy + dy;
            if (adj.containsKey(nx + "," + ny) || nx < 0 || nx >= GRID_SIZE || ny < 0 || ny >= GRID_SIZE) { ok = false; break; }
            pathCells.add(new int[]{nx, ny}); cx = nx; cy = ny;
        }
        if (!ok) return null;

        int t1x = cx + dx, t1y = cy + dy, t2x = t1x + dx, t2y = t1y + dy;
        int pex = -dy, pey = dx;
        int t3x = t2x + pex, t3y = t2y + pey, t4x = t1x + pex, t4y = t1y + pey;
        int extx = t4x + pex, exty = t4y + pey;

        Set<String> existing = adj.keySet();
        for (String k : Arrays.asList(t1x+","+t1y, t2x+","+t2y, t3x+","+t3y, t4x+","+t4y, extx+","+exty)) {
            if (existing.contains(k)) return null;
            String[] kp = k.split(","); int kx = Integer.parseInt(kp[0]), ky = Integer.parseInt(kp[1]);
            if (kx < 0 || kx >= GRID_SIZE || ky < 0 || ky >= GRID_SIZE) return null;
        }

        Set<String> pathSet = new HashSet<>();
        String cur = porteKey;
        for (int[] cell : pathCells) {
            String ck = cell[0] + "," + cell[1];
            pathSet.add(ck); adj.put(ck, new HashSet<>()); adj.get(ck).add(cur); adj.get(cur).add(ck); cur = ck;
        }
        String t1k = t1x+","+t1y, t2k = t2x+","+t2y, t3k = t3x+","+t3y, t4k = t4x+","+t4y, ek = extx+","+exty;
        adj.put(t1k, new HashSet<>()); adj.get(cur).add(t1k); adj.get(t1k).add(cur);
        adj.put(t2k, new HashSet<>()); adj.get(t1k).add(t2k); adj.get(t2k).add(t1k);
        adj.put(t3k, new HashSet<>()); adj.get(t2k).add(t3k); adj.get(t3k).add(t2k);
        adj.put(t4k, new HashSet<>()); adj.get(t3k).add(t4k); adj.get(t4k).add(t3k);
        adj.put(ek, new HashSet<>()); adj.get(t4k).add(ek); adj.get(ek).add(t4k);

        TavernResult tr = new TavernResult();
        tr.tavern = Map.of("T1", t1k, "T2", t2k, "T3", t3k, "T4", t4k);
        tr.exitKey = ek; tr.pathSet = pathSet;
        return tr;
    }

    // ===================== Algo: analyzePart2 =====================

    private static Map<String, String> analyzePart2(Map<String, Set<String>> adj, String exitKey,
                                                      Map<String, String> labels, Set<String> pathSet, Random rng) {
        // Label exit based on actual adjacency, not forced corridor
        List<String> exitNb = new ArrayList<>(adj.get(exitKey));
        if (exitNb.size() == 2) {
            String[] p1 = exitNb.get(0).split(","), p2 = exitNb.get(1).split(",");
            if (p1[0].equals(p2[0]) || p1[1].equals(p2[1])) labels.put(exitKey, pickC(rng));
            else labels.put(exitKey, "I2");
        } else if (exitNb.size() == 1) labels.put(exitKey, "cul");
        else labels.put(exitKey, pickC(rng));
        Set<String> allLabeled = new HashSet<>(labels.keySet());
        List<String> p2Nodes = new ArrayList<>();
        Set<String> seen = new HashSet<>(); Queue<String> q = new LinkedList<>();
        q.add(exitKey); seen.add(exitKey);
        while (!q.isEmpty()) { String n = q.poll(); if (!allLabeled.contains(n)) p2Nodes.add(n); for (String nb : adj.get(n)) { if (!seen.contains(nb)) { seen.add(nb); q.add(nb); } } }

        List<String> leaves = new ArrayList<>(), internals = new ArrayList<>();
        for (String n : p2Nodes) { if (adj.get(n).size() == 1) leaves.add(n); else internals.add(n); }

        int p1M1 = 0, p1M2 = 0;
        for (String v : labels.values()) { if (v.equals("M1")) p1M1++; else if (v.equals("M2")) p1M2++; }
        int m1Budget = Math.max(0, 2 - p1M1), m2Budget = Math.max(0, 3 - p1M2);
        if (leaves.size() < 2 + m1Budget) return null;
        Collections.shuffle(leaves, rng);
        labels.put(leaves.get(0), "fontaine");
        labels.put(leaves.get(1), "porte2");
        int idx = 2;
        for (int i = 0; i < m1Budget && idx < leaves.size(); i++) { labels.put(leaves.get(idx), "M1"); idx++; }
        for (int i = idx; i < leaves.size(); i++) labels.put(leaves.get(i), "cul");

        List<String> cList = new ArrayList<>(), i2List = new ArrayList<>(), i3List = new ArrayList<>(), i4List = new ArrayList<>();
        for (String node : internals) {
            int deg = adj.get(node).size();
            if (deg == 2) { List<String> nb = new ArrayList<>(adj.get(node)); String[] p1 = nb.get(0).split(","), p2 = nb.get(1).split(","); if (p1[0].equals(p2[0]) || p1[1].equals(p2[1])) cList.add(node); else i2List.add(node); }
            else if (deg == 3) i3List.add(node);
            else if (deg == 4) i4List.add(node);
        }
        Collections.shuffle(cList, rng);
        if (cList.size() < m2Budget) return null;

        int m2A = 0; Set<String> m2Set = new HashSet<>(); Set<String> m1Set = new HashSet<>();
        for (String v : labels.keySet()) { if (labels.get(v).equals("M1")) m1Set.add(v); }
        List<String> remC = new ArrayList<>();
        for (String n : cList) {
            if (m2A < m2Budget) {
                boolean adjMonster = false;
                for (String nb : adj.get(n)) { if (m2Set.contains(nb) || m1Set.contains(nb)) { adjMonster = true; break; } }
                if (!adjMonster) { labels.put(n, "M2"); m2Set.add(n); m2A++; } else remC.add(n);
            } else remC.add(n);
        }
        if (m2A < m2Budget) return null;

        if (!remC.isEmpty()) {
            List<String> puitPool = new ArrayList<>();
            for (String n : remC) if (!pathSet.contains(n) && !labels.getOrDefault(n, "").equals("fontaine") && adj.get(n).stream().noneMatch(nb -> "fontaine".equals(labels.get(nb)))) puitPool.add(n);
            if (!puitPool.isEmpty()) { String ch = puitPool.get(rng.nextInt(puitPool.size())); labels.put(ch, "puit"); remC.remove(ch); }
        }
        for (String n : remC) labels.put(n, pickC(rng));
        for (String n : i2List) labels.put(n, "I2");
        for (String n : i3List) labels.put(n, "I3");
        for (String n : i4List) labels.put(n, "I4");
        return labels;
    }

    // ===================== Algo: placeCampAndPath =====================

    private static CampResult placeCampAndPath(Map<String, Set<String>> adj, String porte2Key, Random rng) {
        String[] pp = porte2Key.split(",");
        int px = Integer.parseInt(pp[0]), py = Integer.parseInt(pp[1]);
        String parent = adj.get(porte2Key).iterator().next();
        String[] pap = parent.split(",");
        int dx = px - Integer.parseInt(pap[0]), dy = py - Integer.parseInt(pap[1]);
        int preTurn = 2, postTurn = 2 + rng.nextInt(2);
        List<int[]> pathCells = new ArrayList<>();
        int cx = px, cy = py; boolean ok = true;

        for (int i = 0; i < preTurn; i++) {
            int nx = cx + dx, ny = cy + dy;
            if (adj.containsKey(nx+","+ny) || nx < 0 || nx >= GRID_SIZE || ny < 0 || ny >= GRID_SIZE) { ok = false; break; }
            pathCells.add(new int[]{nx, ny}); cx = nx; cy = ny;
        }
        if (!ok) return null;

        List<int[]> perpList = new ArrayList<>(Arrays.asList(new int[]{dy, -dx}, new int[]{-dy, dx}));
        Collections.shuffle(perpList, rng);
        boolean turned = false;
        for (int[] pd : perpList) {
            int nx = cx + pd[0], ny = cy + pd[1];
            if (!adj.containsKey(nx+","+ny) && nx >= 0 && nx < GRID_SIZE && ny >= 0 && ny < GRID_SIZE) {
                pathCells.add(new int[]{nx, ny}); cx = nx; cy = ny; turned = true; break;
            }
        }
        if (!turned) return null;

        for (int i = 0; i < postTurn; i++) {
            int nx = cx + dx, ny = cy + dy;
            if (adj.containsKey(nx+","+ny) || nx < 0 || nx >= GRID_SIZE || ny < 0 || ny >= GRID_SIZE) { ok = false; break; }
            pathCells.add(new int[]{nx, ny}); cx = nx; cy = ny;
        }
        if (!ok) return null;

        int c1x = cx + dx, c1y = cy + dy, c2x = c1x + dx, c2y = c1y + dy;
        int pex = dy, pey = -dx;
        int c3x = c2x + pex, c3y = c2y + pey, c4x = c3x - dx, c4y = c3y - dy;
        int cex = c3x + dx, cey = c3y + dy;

        Set<String> existing = adj.keySet();
        for (String k : Arrays.asList(c1x+","+c1y, c2x+","+c2y, c3x+","+c3y, c4x+","+c4y, cex+","+cey)) {
            if (existing.contains(k)) return null;
            String[] kp = k.split(","); int kx = Integer.parseInt(kp[0]), ky = Integer.parseInt(kp[1]);
            if (kx < 0 || kx >= GRID_SIZE || ky < 0 || ky >= GRID_SIZE) return null;
        }

        Set<String> campPathSet = new HashSet<>();
        String cur = porte2Key;
        for (int[] cell : pathCells) {
            String ck = cell[0]+","+cell[1]; campPathSet.add(ck); adj.put(ck, new HashSet<>()); adj.get(ck).add(cur); adj.get(cur).add(ck); cur = ck;
        }
        String c1k = c1x+","+c1y, c2k = c2x+","+c2y, c3k = c3x+","+c3y, c4k = c4x+","+c4y, cek = cex+","+cey;
        adj.put(c1k, new HashSet<>()); adj.get(cur).add(c1k); adj.get(c1k).add(cur);
        adj.put(c2k, new HashSet<>()); adj.get(c1k).add(c2k); adj.get(c2k).add(c1k);
        adj.put(c3k, new HashSet<>()); adj.get(c2k).add(c3k); adj.get(c3k).add(c2k);
        adj.put(c4k, new HashSet<>()); adj.get(c3k).add(c4k); adj.get(c4k).add(c3k);
        adj.put(cek, new HashSet<>()); adj.get(c3k).add(cek); adj.get(cek).add(c3k);

        CampResult cr = new CampResult();
        cr.campExit = cek; cr.campPathSet = campPathSet;
        cr.campNodes = Map.of("Ca1", c1k, "Ca2", c2k, "Ca3", c3k, "Ca4", c4k);
        return cr;
    }

    // ===================== Algo: analyzePart3 =====================

    private static Map<String, String> analyzePart3(Map<String, Set<String>> adj, String campExit, Map<String, String> labels, Random rng) {
        // Label campExit based on actual adjacency
        List<String> campExitNb = new ArrayList<>(adj.get(campExit));
        if (campExitNb.size() == 2) {
            String[] p1 = campExitNb.get(0).split(","), p2 = campExitNb.get(1).split(",");
            if (p1[0].equals(p2[0]) || p1[1].equals(p2[1])) labels.put(campExit, pickCJ(rng));
            else labels.put(campExit, "IJ2");
        } else if (campExitNb.size() == 1) labels.put(campExit, "culDJ");
        else labels.put(campExit, pickCJ(rng));

        Set<String> allLabeled = new HashSet<>(labels.keySet());
        List<String> bfsP3 = new ArrayList<>();
        Set<String> seen = new HashSet<>(); Queue<String> q = new LinkedList<>();
        q.add(campExit); seen.add(campExit);
        while (!q.isEmpty()) { String n = q.poll(); if (!allLabeled.contains(n)) bfsP3.add(n); for (String nb : adj.get(n)) { if (!seen.contains(nb)) { seen.add(nb); q.add(nb); } } }

        // Create Bib chain (Bib1 + Bib2) by extending - validate first, then add
        String[] bibNodes = null;
        List<String> shuffled = new ArrayList<>(bfsP3); Collections.shuffle(shuffled, rng);
        for (String src : shuffled) {
            for (int[] d : DIR_OFFSET) {
                String[] pp = src.split(","); int sx = Integer.parseInt(pp[0]), sy = Integer.parseInt(pp[1]);
                String[] chainKeys = new String[2];
                boolean chainOk = true;
                int cx = sx, cy = sy;
                for (int i = 0; i < 2; i++) {
                    int nx = cx + d[0], ny = cy + d[1];
                    String nk = nx+","+ny;
                    if (nx < 0 || nx >= GRID_SIZE || ny < 0 || ny >= GRID_SIZE || adj.containsKey(nk)) { chainOk = false; break; }
                    chainKeys[i] = nk; cx = nx; cy = ny;
                }
                if (chainOk) {
                    adj.put(chainKeys[0], new HashSet<>()); adj.get(src).add(chainKeys[0]); adj.get(chainKeys[0]).add(src);
                    adj.put(chainKeys[1], new HashSet<>()); adj.get(chainKeys[0]).add(chainKeys[1]); adj.get(chainKeys[1]).add(chainKeys[0]);
                    bibNodes = chainKeys; break;
                }
            }
            if (bibNodes != null) break;
        }
        if (bibNodes == null) return null;

        // Create Shop leaf
        String shopNode = null;
        shuffled = new ArrayList<>(bfsP3); Collections.shuffle(shuffled, rng);
        for (String src : shuffled) {
            String[] pp = src.split(",");
            for (int[] d : DIR_OFFSET) {
                int nx = Integer.parseInt(pp[0]) + d[0], ny = Integer.parseInt(pp[1]) + d[1];
                String nk = nx+","+ny;
                if (nx >= 0 && nx < GRID_SIZE && ny >= 0 && ny < GRID_SIZE && !adj.containsKey(nk)) {
                    adj.put(nk, new HashSet<>()); adj.get(src).add(nk); adj.get(nk).add(src);
                    shopNode = nk; break;
                }
            }
            if (shopNode != null) break;
        }

        // Recompute leaves and internals
        Set<String> allNodes = new HashSet<>(adj.keySet());
        List<String> leaves = new ArrayList<>(), internals = new ArrayList<>();
        for (String n : allNodes) { if (adj.get(n).size() == 1 && !labels.containsKey(n)) leaves.add(n); else if (adj.get(n).size() >= 2 && !labels.containsKey(n)) internals.add(n); }

        List<String> cjList = new ArrayList<>(), ij2List = new ArrayList<>(), ij3List = new ArrayList<>(), ij4List = new ArrayList<>();
        for (String node : internals) {
            int deg = adj.get(node).size();
            if (deg == 2) { List<String> nb = new ArrayList<>(adj.get(node)); String[] p1 = nb.get(0).split(","), p2 = nb.get(1).split(","); if (p1[0].equals(p2[0]) || p1[1].equals(p2[1])) cjList.add(node); else ij2List.add(node); }
            else if (deg == 3) ij3List.add(node);
            else if (deg == 4) ij4List.add(node);
        }

        labels.put(bibNodes[0], "Bib1"); labels.put(bibNodes[1], "Bib2");
        cjList.remove(bibNodes[0]);
        if (shopNode != null) { labels.put(shopNode, "Shop"); leaves.remove(shopNode); }
        // Remove Bib2 from leaves to prevent relabeling
        leaves.remove(bibNodes[1]);
        leaves.remove(bibNodes[0]);

        if (leaves.size() < 4) return null;
        Collections.shuffle(leaves, rng);
        labels.put(leaves.get(0), "Lootdj1");
        labels.put(leaves.get(1), "Lootdj1");
        labels.put(leaves.get(2), "MJ1");
        labels.put(leaves.get(3), "MJ1");
        for (int i = 4; i < leaves.size(); i++) labels.put(leaves.get(i), "culDJ");

        if (cjList.size() < 3) return null;
        Collections.shuffle(cjList, rng);
        Set<String> monsterSet = new HashSet<>();
        for (var e : labels.entrySet()) if (e.getValue().equals("M1") || e.getValue().equals("M2")) monsterSet.add(e.getKey());
        int mj2A = 0; List<String> remCJ = new ArrayList<>();
        for (String n : cjList) {
            if (mj2A < 2) {
                boolean adjM = adj.get(n).stream().anyMatch(nb -> monsterSet.contains(nb) || labels.getOrDefault(nb,"").startsWith("MJ"));
                if (!adjM) { labels.put(n, "MJ2"); monsterSet.add(n); mj2A++; } else remCJ.add(n);
            } else remCJ.add(n);
        }
        if (mj2A < 2 || remCJ.size() < 1) return null;
        labels.put(remCJ.get(0), "Lootdj2");
        for (int i = 1; i < remCJ.size(); i++) labels.put(remCJ.get(i), pickCJ(rng));
        for (String n : ij2List) labels.put(n, "IJ2");
        for (String n : ij3List) labels.put(n, "IJ3");
        for (String n : ij4List) labels.put(n, "IJ4");
        return labels;
    }

    // ===================== Algo: generateValidDungeon (P1+P2+P3) =====================

    private static DungeonAlgo.DungeonResult generateValidDungeon() {
        return generateWithSeed(0);
    }

    private static DungeonAlgo.DungeonResult generateWithSeed(long seed) {
        DungeonAlgo.DungeonResult result = DungeonAlgo.generateDungeon(seed);
        lastSeed = DungeonAlgo.getLastSeed();
        return result;
    }

    private static String findKeyByValue(Map<String, String> map, String value) { for (var e : map.entrySet()) if (e.getValue().equals(value)) return e.getKey(); return null; }

    // ===================== Validation =====================

    private static boolean checkRoomRules(String startKey, Map<String, Set<String>> adj, Map<String, String> labels, int relaxLevel) {
        for (var e : labels.entrySet()) {
            String name = e.getValue();
            RoomConfig cfg = ROOM_CONFIGS.get(name);
            if (cfg == null) continue;
            Set<String> neighbors = adj.get(e.getKey());
            List<String> roomDirs = new ArrayList<>();
            for (String nb : neighbors) { String[] np = nb.split(","), np2 = e.getKey().split(","); int dx = Integer.parseInt(np[0]) - Integer.parseInt(np2[0]), dy = Integer.parseInt(np[1]) - Integer.parseInt(np2[1]); if (dx == 1) roomDirs.add("E"); else if (dx == -1) roomDirs.add("W"); else if (dy == 1) roomDirs.add("S"); else if (dy == -1) roomDirs.add("N"); }
            if (relaxLevel < 4) {
                if ("Cul de sac".equals(cfg.type) && neighbors.size() != 1) return false;
                if ("Couloir droit".equals(cfg.type) && (neighbors.size() != 2 || new ArrayList<>(neighbors).get(0).split(",")[0].equals(new ArrayList<>(neighbors).get(1).split(",")[0]) == false && new ArrayList<>(neighbors).get(0).split(",")[1].equals(new ArrayList<>(neighbors).get(1).split(",")[1]) == false)) return false;
                if ("Virage".equals(cfg.type) && neighbors.size() != 2) return false;
                if ("Intersection".equals(cfg.type) && neighbors.size() != 3 && neighbors.size() != 4) return false;
            }
            if (relaxLevel < 3) { for (String door : cfg.doors) if (!roomDirs.contains(door)) return false; }
        }
        // M2 no adjacent
        if (relaxLevel < 4) { for (var e : labels.entrySet()) { if (!e.getValue().equals("M2")) continue; for (String nb : adj.get(e.getKey())) if ("M2".equals(labels.get(nb))) return false; } }
        // No 3 consecutive collinear corridors (any corridor type, not just same type)
        if (relaxLevel < 3) {
            for (var e : labels.entrySet()) {
                RoomConfig cfg = ROOM_CONFIGS.get(e.getValue());
                if (cfg == null || !"Couloir droit".equals(cfg.type)) continue;
                List<String> neighs = new ArrayList<>(adj.get(e.getKey()));
                if (neighs.size() == 2) {
                    String v1 = neighs.get(0), v2 = neighs.get(1);
                    RoomConfig cfg1 = ROOM_CONFIGS.get(labels.get(v1));
                    if (cfg1 != null && "Couloir droit".equals(cfg1.type)) {
                        String[] v1p = v1.split(","), np = e.getKey().split(","), v2p = v2.split(",");
                        int dx = Integer.parseInt(np[0]) - Integer.parseInt(v1p[0]);
                        int dy = Integer.parseInt(np[1]) - Integer.parseInt(v1p[1]);
                        int nxtX = Integer.parseInt(v2p[0]) + dx, nxtY = Integer.parseInt(v2p[1]) + dy;
                        if (adj.containsKey(nxtX+","+nxtY) && adj.get(v2).contains(nxtX+","+nxtY)) return false;
                    }
                }
            }
        }
        return true;
    }

    // ===================== Graph to RoomCells =====================

    private static final Map<String, Integer> LABEL_TO_TYPE = new HashMap<>();
    static {
        LABEL_TO_TYPE.put("C1", CORRIDOR); LABEL_TO_TYPE.put("C2", CORRIDOR); LABEL_TO_TYPE.put("C3", CORRIDOR);
        LABEL_TO_TYPE.put("I3", I3); LABEL_TO_TYPE.put("I2", I2); LABEL_TO_TYPE.put("I4", I4);
        LABEL_TO_TYPE.put("cul", CULDESAC); LABEL_TO_TYPE.put("porte", PORTE);
        LABEL_TO_TYPE.put("M1", M1); LABEL_TO_TYPE.put("M2", M2); LABEL_TO_TYPE.put("D", DEPART);
        LABEL_TO_TYPE.put("Prison", PRISON); LABEL_TO_TYPE.put("Loot1", LOOT1);
        LABEL_TO_TYPE.put("fontaine", FONTAINE); LABEL_TO_TYPE.put("puit", PUITS); LABEL_TO_TYPE.put("porte2", PORTE2);
        LABEL_TO_TYPE.put("T1", T1); LABEL_TO_TYPE.put("T2", T2); LABEL_TO_TYPE.put("T3", T3); LABEL_TO_TYPE.put("T4", T4);
        LABEL_TO_TYPE.put("CJ1", CJ1); LABEL_TO_TYPE.put("CJ2", CJ2); LABEL_TO_TYPE.put("CJ3", CJ3);
        LABEL_TO_TYPE.put("IJ2", IJ2); LABEL_TO_TYPE.put("IJ3", IJ3); LABEL_TO_TYPE.put("IJ4", IJ4);
        LABEL_TO_TYPE.put("MJ1", MJ1); LABEL_TO_TYPE.put("MJ2", MJ2);         LABEL_TO_TYPE.put("MJ3", MJ3); LABEL_TO_TYPE.put("MJ4", MJ4); LABEL_TO_TYPE.put("MJ5", MJ5); LABEL_TO_TYPE.put("PuitDJ", PUITDJ); LABEL_TO_TYPE.put("Jardin", JARDIN); LABEL_TO_TYPE.put("Lootdj3", LOOTDJ3); LABEL_TO_TYPE.put("Statue", STATUE); LABEL_TO_TYPE.put("Centrale", CENTRALE); LABEL_TO_TYPE.put("MarchandNoir", MARCHAND_NOIR); LABEL_TO_TYPE.put("Chapelle1", CHAPELLE1); LABEL_TO_TYPE.put("Chapelle2", CHAPELLE2);
        LABEL_TO_TYPE.put("Lootdj1", LOOTDJ1); LABEL_TO_TYPE.put("Lootdj2", LOOTDJ2);
        LABEL_TO_TYPE.put("Ca1", CA1); LABEL_TO_TYPE.put("Ca2", CA2); LABEL_TO_TYPE.put("Ca3", CA3); LABEL_TO_TYPE.put("Ca4", CA4);
        LABEL_TO_TYPE.put("Bib1", BIB1); LABEL_TO_TYPE.put("Bib2", BIB2); LABEL_TO_TYPE.put("Shop", SHOP);
        LABEL_TO_TYPE.put("porte3", PORTE3); LABEL_TO_TYPE.put("culDJ", CULDJ);
        LABEL_TO_TYPE.put("M3", M3); LABEL_TO_TYPE.put("M4", M4); LABEL_TO_TYPE.put("Ogre", OGRE);
        LABEL_TO_TYPE.put("Crypte1", CRYPTE1); LABEL_TO_TYPE.put("Crypte2", CRYPTE2);
        LABEL_TO_TYPE.put("PrisonC1", PRISON_CENTRALE_1); LABEL_TO_TYPE.put("PrisonC2", PRISON_CENTRALE_2); LABEL_TO_TYPE.put("PrisonC3", PRISON_CENTRALE_3); LABEL_TO_TYPE.put("PrisonC4", PRISON_CENTRALE_4);
        LABEL_TO_TYPE.put("PorteGob", PORTE_GOBELIN); LABEL_TO_TYPE.put("CG1", COULOIR_GOBELIN_1); LABEL_TO_TYPE.put("GI2", INTERSECTION_GOBELIN_2); LABEL_TO_TYPE.put("GI3", INTERSECTION_GOBELIN_3); LABEL_TO_TYPE.put("GI4", INTERSECTION_GOBELIN_4); LABEL_TO_TYPE.put("PuitG", PUIT_GOBELIN); LABEL_TO_TYPE.put("MarchG", MARCHAND_GOBELIN); LABEL_TO_TYPE.put("ArmG", ARMURERIE_GOBELIN); LABEL_TO_TYPE.put("CDG", CUL_DE_SAC_GOBELIN); LABEL_TO_TYPE.put("MG1", MAISON_GOBELIN_1); LABEL_TO_TYPE.put("MG2", MAISON_GOBELIN_2); LABEL_TO_TYPE.put("MG3", MAISON_GOBELIN_3); LABEL_TO_TYPE.put("TresorG", TRESORERIE_GOBELIN);
    }

    private static String getRoomOrientation(int x, int y, Set<String> neighbors) {
        StringBuilder dirs = new StringBuilder();
        for (String nb : neighbors) { String[] np = nb.split(","); int dx = Integer.parseInt(np[0]) - x, dy = Integer.parseInt(np[1]) - y; if (dx == 1) dirs.append('E'); else if (dx == -1) dirs.append('W'); else if (dy == 1) dirs.append('S'); else if (dy == -1) dirs.append('N'); }
        StringBuilder sorted = new StringBuilder();
        for (char c : "NSEW".toCharArray()) if (dirs.indexOf(String.valueOf(c)) >= 0) sorted.append(c);
        return sorted.toString();
    }

    private static List<RoomCell> convertGraphToCells(DungeonAlgo.DungeonResult g) {
        Map<String, String> labels = g.labels; Map<String, Set<String>> adj = g.adj; Random rng = new Random();
        Map<String, Integer> entryDirMap = new HashMap<>();
        Queue<String> queue = new LinkedList<>(); Set<String> visitedBfs = new HashSet<>();
        queue.add(g.startKey); visitedBfs.add(g.startKey); entryDirMap.put(g.startKey, -1);
        while (!queue.isEmpty()) { String key = queue.poll(); String[] pp = key.split(","); int cx = Integer.parseInt(pp[0]), cy = Integer.parseInt(pp[1]); for (String nb : adj.get(key)) { if (!visitedBfs.contains(nb)) { visitedBfs.add(nb); String[] np = nb.split(","); int nx = Integer.parseInt(np[0]), ny = Integer.parseInt(np[1]); entryDirMap.put(nb, nx > cx ? 0 : nx < cx ? 2 : ny > cy ? 1 : 3); queue.add(nb); } } }

        // BFS pour la P4 (via p4Adj, depuis les cellules du hub)
        Map<String, Integer> p4EntryDir = new HashMap<>();
        if (g.p4Adj != null && g.topLabels != null) {
            Queue<String> q4 = new LinkedList<>(); Set<String> v4 = new HashSet<>();
            for (var e : g.topLabels.entrySet()) {
                if (e.getValue() != null && e.getValue().equals("Centrale")) {
                    q4.add(e.getKey()); v4.add(e.getKey()); p4EntryDir.put(e.getKey(), -1);
                }
            }
            while (!q4.isEmpty()) {
                String key = q4.poll(); if (!g.p4Adj.containsKey(key)) continue;
                String[] pp = key.split(","); int cx4 = Integer.parseInt(pp[0]), cy4 = Integer.parseInt(pp[1]);
                for (String nb : g.p4Adj.get(key)) {
                    if (!v4.contains(nb)) { v4.add(nb); String[] np = nb.split(","); int nx = Integer.parseInt(np[0]), ny = Integer.parseInt(np[1]); p4EntryDir.put(nb, nx > cx4 ? 0 : nx < cx4 ? 2 : ny > cy4 ? 1 : 3); q4.add(nb); }
                }
            }
        }

        List<RoomCell> cells = new ArrayList<>();
        for (var e : labels.entrySet()) {
            String key = e.getKey(); String[] pp = key.split(","); int x = Integer.parseInt(pp[0]), y = Integer.parseInt(pp[1]);
            Integer typeObj = LABEL_TO_TYPE.get(e.getValue());
            if (typeObj == null) continue;
            int type = typeObj;
            String orientation = getRoomOrientation(x, y, adj.get(key));
            int[] requiredDirs = new int[orientation.length()];
            for (int i = 0; i < orientation.length(); i++) { char c = orientation.charAt(i); requiredDirs[i] = c == 'N' ? 3 : c == 'S' ? 1 : c == 'E' ? 0 : 2; }
            int[] structPorts = (type < roomPorts.length && roomPorts[type] != null && roomPorts[type].length > 0) ? roomPorts[type] : new int[]{3};
            int rot = 0;
            for (int r = 0; r < 4; r++) { boolean ok = true; for (int rd : requiredDirs) { boolean found = false; for (int sp : structPorts) if ((sp + r) % 4 == rd) { found = true; break; } if (!found) { ok = false; break; } } if (ok) { rot = r; break; } }

            // Purple port constraint for T1, T4, Ca1, Ca3, Bib1, Bib2, Chapelle1, Chapelle2
            int entryDir = entryDirMap.getOrDefault(key, -1);
            if ((type == T1 || type == T4 || type == CA1 || type == CA3 || type == BIB1 || type == BIB2 || type == CHAPELLE1 || type == CHAPELLE2 || type == CRYPTE1 || type == CRYPTE2 || type == PRISON_CENTRALE_1 || type == PRISON_CENTRALE_2 || type == PRISON_CENTRALE_3 || type == PRISON_CENTRALE_4 || type == PORTE_GOBELIN) && entryDir >= 0) {
                int[] purplePorts = (type < roomPurplePorts.length) ? roomPurplePorts[type] : null;
                if (purplePorts != null && purplePorts.length == 1) {
                    int purpleLocal = purplePorts[0];
                    int towardParent = (entryDir + 2) % 4;
                    int exitDir = (entryDir + 3) % 4;
                    int targetDir;
                    if (type == T1 || type == CA1 || type == BIB1) targetDir = towardParent;
                    else if (type == T4) targetDir = exitDir;
                    else if (type == CA3) targetDir = (entryDir + 1) % 4;
                    else if (type == CHAPELLE2) targetDir = entryDir; // Chapelle2: oppose a Chapelle1 (loin du parent)
                    else if (type == PORTE_GOBELIN) targetDir = towardParent;
                    else if (type == PRISON_CENTRALE_1 || type == PRISON_CENTRALE_2 || type == PRISON_CENTRALE_3 || type == PRISON_CENTRALE_4) targetDir = towardParent;
                    else targetDir = towardParent;
                    for (int r = 0; r < 4; r++) {
                        if ((purpleLocal + r) % 4 != targetDir) continue;
                        if (type == CHAPELLE2) { rot = r; break; }
                        boolean ok = true;
                        for (int rd : requiredDirs) { boolean found = false; for (int sp : structPorts) if ((sp + r) % 4 == rd) { found = true; break; } if (!found) { ok = false; break; } }
                        if (ok) { rot = r; break; }
                    }
                    if (type == CA1 || type == CA3) System.out.println("[ROT] " + e.getValue() + " at (" + x + "," + y + ") entryDir=" + entryDir + " towardParent=" + towardParent + " purpleLocal=" + purpleLocal + " targetDir=" + targetDir + " rot=" + rot + " nbs=" + adj.get(key));
                }
            }

            int corrIdx = e.getValue().startsWith("C") || e.getValue().startsWith("CJ") ? rng.nextInt(3) : Math.floorMod(x * 31 + y * 17, 3);
            cells.add(new RoomCell(x - g.startX, y - g.startY, type, rot, corrIdx));
        }
        // Traiter la P4 (topLabels) avec p4Adj pour l'orientation
        System.out.println("[ROT] P4 section: topLabels=" + (g.topLabels == null ? "null" : g.topLabels.size()) + " p4Adj=" + (g.p4Adj == null ? "null" : g.p4Adj.size()));
        if (g.topLabels != null && g.p4Adj != null) {
            for (var e : g.topLabels.entrySet()) {
                String key = e.getKey(); String val = e.getValue();
                if (val == null || val.equals("Centrale")) continue;
                String[] pp = key.split(","); int x = Integer.parseInt(pp[0]), y = Integer.parseInt(pp[1]);
                Integer typeObj = LABEL_TO_TYPE.get(val);
                if (typeObj == null) continue;
                int type = typeObj;
                String orientation = getRoomOrientation(x, y, g.p4Adj.get(key));
                int[] requiredDirs = new int[orientation.length()];
                for (int i = 0; i < orientation.length(); i++) { char c = orientation.charAt(i); requiredDirs[i] = c == 'N' ? 3 : c == 'S' ? 1 : c == 'E' ? 0 : 2; }
                Set<Integer> portSet = new HashSet<>();
                if (type < roomWhitePrisonPorts.length && roomWhitePrisonPorts[type] != null) for (int p : roomWhitePrisonPorts[type]) portSet.add(p);
                if (type < roomPurplePorts.length && roomPurplePorts[type] != null) for (int p : roomPurplePorts[type]) portSet.add(p);
                if (type < roomPorts.length && roomPorts[type] != null) for (int p : roomPorts[type]) portSet.add(p);
                int[] structPorts = portSet.isEmpty() ? new int[]{3} : portSet.stream().mapToInt(i->i).toArray();
                int rot = 0;
                for (int r = 0; r < 4; r++) { boolean ok = true; for (int rd : requiredDirs) { boolean found = false; for (int sp : structPorts) if ((sp + r) % 4 == rd) { found = true; break; } if (!found) { ok = false; break; } } if (ok) { rot = r; break; } }
                if (type == CHAPELLE1 || type == CHAPELLE2 || type == CRYPTE1 || type == CRYPTE2 || type == PRISON_CENTRALE_1 || type == PORTE_GOBELIN) {
                    int[] purplePorts = (type < roomPurplePorts.length) ? roomPurplePorts[type] : null;
                    if (purplePorts != null && purplePorts.length == 1) {
                        int purpleLocal = purplePorts[0];
                        int targetDir = -1;
                        String targetLabel = type == CHAPELLE2 ? "Chapelle1" : null;
                        Set<String> exts = new HashSet<>(Arrays.asList("Chapelle2","Crypte2","PrisonC2","PrisonC3","PrisonC4"));
                        Set<String> gobs = new HashSet<>(Arrays.asList("CG1","GI2","GI3","GI4","CDG","PuitG","MarchG","ArmG","MG1","MG2","MG3","TresorG"));
                        Set<String> nbs = g.p4Adj.get(key);
                        String foundLabel = "?";
                        if (nbs != null) {
                            for (String nb : nbs) {
                                String nl = g.topLabels.get(nb); if (nl == null) continue;
                                if (targetLabel != null && !nl.equals(targetLabel)) continue;
                                if (exts.contains(nl)) continue;
                                if (type == PORTE_GOBELIN && gobs.contains(nl)) continue;
                                String[] np = nb.split(",");
                                int dx = Integer.parseInt(np[0]) - x;
                                int dz = Integer.parseInt(np[1]) - y;
                                targetDir = dx == 1 ? 0 : dx == -1 ? 2 : dz == 1 ? 1 : dz == -1 ? 3 : -1;
                                foundLabel = nl;
                                System.out.println("[ROT] " + val + " at (" + x + "," + y + ") picks neighbor " + nl + " at (" + np[0] + "," + np[1] + ") targetLabel=" + (targetLabel != null ? targetLabel : "any"));
                                break;
                            }
                        }
                        if (targetDir >= 0) {
                            for (int r = 0; r < 4; r++) {
                                if ((purpleLocal + r) % 4 == targetDir) { rot = r; break; }
                            }
                            System.out.println("[ROT] " + val + " at (" + x + "," + y + ") found=" + foundLabel + " targetDir=" + targetDir + " purpleLocal=" + purpleLocal + " rot=" + rot);
                        } else {
                            System.out.println("[ROT] " + val + " at (" + x + "," + y + ") NO PARENT targetDir=" + targetDir + " nbs=" + nbs);
                        }
                    }
                }
                int corrIdx = val.startsWith("CJ") || val.startsWith("CG") || val.equals("C1") || val.equals("C2") || val.equals("C3") ? rng.nextInt(3) : 0;
                cells.add(new RoomCell(x - g.startX, y - g.startY, type, rot, corrIdx, true));
            }
        }
        return cells;
    }

    // ===================== Logging =====================

    private static void logDungeon(DungeonAlgo.DungeonResult g, List<RoomCell> cells) {
        System.out.println("[TestGenerator] ========== DONJON V2 ==========");
        System.out.println("[TestGenerator] " + cells.size() + " salles, " + g.adj.size() + " noeuds");
        Map<Integer, Integer> counts = new HashMap<>(); for (RoomCell rc : cells) counts.merge(rc.type, 1, Integer::sum);
        System.out.println("[TestGenerator] Types:");
        for (var e : counts.entrySet()) System.out.println("  " + TYPE_NAMES[e.getKey()] + ": " + e.getValue());
        int minX = 0, maxX = 0, minZ = 0, maxZ = 0;
        for (RoomCell rc : cells) { if (rc.cx < minX) minX = rc.cx; if (rc.cx > maxX) maxX = rc.cx; if (rc.cz < minZ) minZ = rc.cz; if (rc.cz > maxZ) maxZ = rc.cz; }
        int w = maxX - minX + 1, h = maxZ - minZ + 1;
        String[][] grid = new String[w][h]; for (int x = 0; x < w; x++) for (int z = 0; z < h; z++) grid[x][z] = " . ";
        for (RoomCell rc : cells) { String name = TYPE_NAMES[rc.type]; grid[rc.cx - minX][rc.cz - minZ] = String.format("%2s ", name.substring(0, Math.min(2, name.length()))); }
        System.out.println("[TestGenerator] Carte:"); for (int z = 0; z < h; z++) { StringBuilder sb = new StringBuilder("  "); for (int x = 0; x < w; x++) sb.append(grid[x][z]); System.out.println("[TestGenerator] " + sb); }
    }

    private static void validateAdjacency(List<RoomCell> cells) {
        Set<String> posSet = new HashSet<>(); for (RoomCell rc : cells) posSet.add(rc.cx + "," + rc.cz);
        int errors = 0;
        for (RoomCell rc : cells) { int[] ports = getWorldPorts(rc.type, rc.rot); for (int wp : ports) { int nx = rc.cx + DIR_OFFSET[wp][0], nz = rc.cz + DIR_OFFSET[wp][1]; if (!posSet.contains(nx + "," + nz)) continue; int oppDir = (wp + 2) % 4; boolean otherHasPort = false; for (RoomCell other : cells) { if (other.cx != nx || other.cz != nz) continue; for (int p : getWorldPorts(other.type, other.rot)) if (p == oppDir) { otherHasPort = true; break; } if (otherHasPort) break; } if (!otherHasPort) { int ntype = getRoomType(nx, nz, cells); int nrot = getRoomRot(nx, nz, cells); String nname = ntype >= 0 && ntype < TYPE_NAMES.length ? TYPE_NAMES[ntype] : "?"; System.out.println("[TestGenerator]   ERR: " + TYPE_NAMES[rc.type] + " (" + rc.cx + "," + rc.cz + ") face " + wp + " -> (" + nx + "," + nz + ") " + nname + " ports=" + Arrays.toString(getWorldPorts(ntype, nrot))); errors++; } } }
        if (errors == 0) System.out.println("[TestGenerator] Adjacence: OK");
        else System.out.println("[TestGenerator] Adjacence: " + errors + " erreur(s)");
    }

    private static int getRoomType(int x, int z, List<RoomCell> cells) { for (RoomCell rc : cells) if (rc.cx == x && rc.cz == z) return rc.type; return -1; }
    private static int getRoomRot(int x, int z, List<RoomCell> cells) { for (RoomCell rc : cells) if (rc.cx == x && rc.cz == z) return rc.rot; return 0; }

    // ===================== Port detection =====================

    private static int[] detectPorts(BlockState[][][] data) {
        if (data == null) return new int[0];
        int sx = data.length, sy = data[0].length, sz = data[0][0].length;
        int scanLimit = Math.min(sy, 10);
        int runNeeded = 5;
        boolean n = false, s = false, e = false, w = false;
        for (int y = 0; y < scanLimit && !(n && s && e && w); y++) {
            int tn = 0, ts = 0, te = 0, tw = 0;
            for (int i = 0; i < sx; i++) { BlockState ns = data[i][y][0]; if (ns != null && !ns.isAir() && isWoolPort(ns)) tn++; else tn = 0; if (tn >= runNeeded) n = true; }
            for (int i = 0; i < sx; i++) { BlockState ss = data[i][y][sz-1]; if (ss != null && !ss.isAir() && isWoolPort(ss)) ts++; else ts = 0; if (ts >= runNeeded) s = true; }
            for (int z = 0; z < sz; z++) { BlockState ws = data[0][y][z]; if (ws != null && !ws.isAir() && isWoolPort(ws)) tw++; else tw = 0; if (tw >= runNeeded) w = true; }
            for (int z = 0; z < sz; z++) { BlockState es = data[sx-1][y][z]; if (es != null && !es.isAir() && isWoolPort(es)) te++; else te = 0; if (te >= runNeeded) e = true; }
        }
        List<Integer> ports = new ArrayList<>(); if (n) ports.add(3); if (s) ports.add(1); if (e) ports.add(0); if (w) ports.add(2);
        int[] r = new int[ports.size()]; for (int i = 0; i < ports.size(); i++) r[i] = ports.get(i); return r;
    }

    private static int[] detectPurplePorts(BlockState[][][] data) {
        if (data == null) return new int[0];
        int sx = data.length, sy = data[0].length, sz = data[0][0].length;
        int scanLimit = Math.min(sy, 12);
        boolean n = false, s = false, e = false, w = false;
        for (int y = 0; y < scanLimit && !(n && s && e && w); y++) {
            for (int i = 0; i < sx; i++) { BlockState ns = data[i][y][0]; if (ns != null && !ns.isAir() && PURPLE_WOOL.equals(Registries.BLOCK.getId(ns.getBlock()))) n = true; }
            for (int i = 0; i < sx; i++) { BlockState ss = data[i][y][sz-1]; if (ss != null && !ss.isAir() && PURPLE_WOOL.equals(Registries.BLOCK.getId(ss.getBlock()))) s = true; }
            for (int z = 0; z < sz; z++) { BlockState ws = data[0][y][z]; if (ws != null && !ws.isAir() && PURPLE_WOOL.equals(Registries.BLOCK.getId(ws.getBlock()))) w = true; }
            for (int z = 0; z < sz; z++) { BlockState es = data[sx-1][y][z]; if (es != null && !es.isAir() && PURPLE_WOOL.equals(Registries.BLOCK.getId(es.getBlock()))) e = true; }
        }
        List<Integer> ports = new ArrayList<>(); if (n) ports.add(3); if (s) ports.add(1); if (e) ports.add(0); if (w) ports.add(2);
        int[] r = new int[ports.size()]; for (int i = 0; i < ports.size(); i++) r[i] = ports.get(i); return r;
    }

    private static int[] detectWhitePrisonPorts(BlockState[][][] data) {
        // Detecte 2 laine blanche sur une face (pour les salles prison)
        if (data == null) return new int[0];
        int sx = data.length, sy = data[0].length, sz = data[0][0].length;
        int scanLimit = Math.min(sy, 12);
        boolean n = false, s = false, e = false, w = false;
        for (int y = 0; y < scanLimit && !(n && s && e && w); y++) {
            int tn = 0, ts = 0, te = 0, tw = 0;
            for (int i = 0; i < sx; i++) { BlockState bs = data[i][y][0]; if (bs != null && !bs.isAir() && WHITE_WOOL.equals(Registries.BLOCK.getId(bs.getBlock()))) tn++; }
            for (int i = 0; i < sx; i++) { BlockState bs = data[i][y][sz-1]; if (bs != null && !bs.isAir() && WHITE_WOOL.equals(Registries.BLOCK.getId(bs.getBlock()))) ts++; }
            for (int z = 0; z < sz; z++) { BlockState bs = data[0][y][z]; if (bs != null && !bs.isAir() && WHITE_WOOL.equals(Registries.BLOCK.getId(bs.getBlock()))) tw++; }
            for (int z = 0; z < sz; z++) { BlockState bs = data[sx-1][y][z]; if (bs != null && !bs.isAir() && WHITE_WOOL.equals(Registries.BLOCK.getId(bs.getBlock()))) te++; }
            if (tn == 2) n = true; if (ts == 2) s = true; if (tw == 2) w = true; if (te == 2) e = true;
        }
        List<Integer> ports = new ArrayList<>(); if (n) ports.add(3); if (s) ports.add(1); if (e) ports.add(0); if (w) ports.add(2);
        int[] r = new int[ports.size()]; for (int i = 0; i < ports.size(); i++) r[i] = ports.get(i); return r;
    }

    private static int[] detectWhitePorts(BlockState[][][] data) {
        if (data == null) return new int[0];
        int sx = data.length, sy = data[0].length, sz = data[0][0].length;
        int scanLimit = Math.min(sy, 12);
        int runNeeded = Math.min(sx, 5);
        boolean n = false, s = false, e = false, w = false;
        for (int y = 0; y < scanLimit && !(n && s && e && w); y++) {
            int tn = 0, ts = 0, te = 0, tw = 0;
            for (int i = 0; i < sx; i++) { BlockState ns = data[i][y][0]; if (ns != null && !ns.isAir() && WHITE_WOOL.equals(Registries.BLOCK.getId(ns.getBlock()))) tn++; else tn = 0; if (tn >= runNeeded) n = true; }
            for (int i = 0; i < sx; i++) { BlockState ss = data[i][y][sz-1]; if (ss != null && !ss.isAir() && WHITE_WOOL.equals(Registries.BLOCK.getId(ss.getBlock()))) ts++; else ts = 0; if (ts >= runNeeded) s = true; }
            for (int z = 0; z < sz; z++) { BlockState ws = data[0][y][z]; if (ws != null && !ws.isAir() && WHITE_WOOL.equals(Registries.BLOCK.getId(ws.getBlock()))) tw++; else tw = 0; if (tw >= runNeeded) w = true; }
            for (int z = 0; z < sz; z++) { BlockState es = data[sx-1][y][z]; if (es != null && !es.isAir() && WHITE_WOOL.equals(Registries.BLOCK.getId(es.getBlock()))) te++; else te = 0; if (te >= runNeeded) e = true; }
        }
        List<Integer> ports = new ArrayList<>(); if (n) ports.add(3); if (s) ports.add(1); if (e) ports.add(0); if (w) ports.add(2);
        int[] r = new int[ports.size()]; for (int i = 0; i < ports.size(); i++) r[i] = ports.get(i); return r;
    }

    private static boolean isWoolPort(BlockState state) { Identifier id = Registries.BLOCK.getId(state.getBlock()); return LIGHT_BLUE_WOOL.equals(id) || PURPLE_WOOL.equals(id); }

    private static Map<String, String> detectCentraleExits(BlockState[][][] data, int hubX, int hubZ) {
        Map<String, String> exits = new HashMap<>();
        if (data == null) return exits;
        int sx = data.length, sy = data[0].length, sz = data[0][0].length;
        int cellSize = 10; // taille d'une cellule en blocs
        Random rng = new Random();
        for (int y = 0; y < sy; y++) {
            for (int x = 0; x < sx; x++) {
                for (int z = 0; z < sz; z++) {
                    BlockState bs = data[x][y][z];
                    if (bs == null || bs.isAir() || !PURPLE_WOOL.equals(Registries.BLOCK.getId(bs.getBlock()))) continue;
                    int ex, ez;
                    if (x == 0) { // OUEST
                        ex = hubX - 1; ez = hubZ + (z / cellSize);
                    } else if (x == sx - 1) { // EST
                        ex = hubX + 2; ez = hubZ + (z / cellSize);
                    } else if (z == 0) { // NORD
                        ex = hubX + (x / cellSize); ez = hubZ - 1;
                    } else if (z == sz - 1) { // SUD
                        ex = hubX + (x / cellSize); ez = hubZ + 2;
                    } else continue;
                    exits.putIfAbsent(ex + "," + ez, CJ_TYPES.get(rng.nextInt(3)));
                }
            }
        }
        return exits;
    }

    private static int[] getWorldPorts(int roomType, int rot) {
        if (roomType < 0 || roomType >= roomPorts.length || roomPorts[roomType] == null) return new int[]{3, 1};
        int[] local = roomPorts[roomType]; int[] world = new int[local.length]; for (int i = 0; i < local.length; i++) world[i] = (local[i] + rot) % 4; return world;
    }

    // ===================== Data loading =====================

    private static BlockState[][][] getData(int roomType, int corrIdx) {
        switch (roomType) {
            case CORRIDOR: return couloirDataList[corrIdx % 3]; case I3: return i3Data; case I2: return i2Data; case I4: return i4Data;
            case CULDESAC: return culDeSacData; case M1: return m1Data; case DEPART: return departData; case M2: return m2Data;
            case PRISON: return prisonData; case LOOT1: return loot1Data; case FONTAINE: return fontaineData; case PUITS: return puitData;
            case PORTE2: case PORTE: return doorCorridorData;
            case T1: return tavernData[0]; case T2: return tavernData[1]; case T3: return tavernData[2]; case T4: return tavernData[3];
            case CJ1: return cjDataList[0]; case CJ2: return cjDataList[1]; case CJ3: return cjDataList[2];
            case IJ2: return ij2Data; case IJ3: return ij3Data; case IJ4: return ij4Data;
            case MJ1: return mj1Data; case MJ2: return mj2Data; case MJ3: return mj3Data; case MJ4: return mj4Data; case MJ5: return mj5Data;
            case LOOTDJ1: return lootdj1Data; case LOOTDJ2: return lootdj2Data; case LOOTDJ3: return lootdj3Data;
            case CA1: return campDataList[0]; case CA2: return campDataList[1]; case CA3: return campDataList[2]; case CA4: return campDataList[3];
            case BIB1: return bib1Data; case BIB2: return bib2Data; case SHOP: return shopData;
            case PORTE3: case CULDJ: return culDeSacDonjonData;
            case M3: return m3Data; case M4: return m4Data; case OGRE: return ogreData; case PUITDJ: return puitDjData; case JARDIN: return jardinData; case STATUE: return statueData; case CENTRALE: return centraleData; case MARCHAND_NOIR: return marchandNoirData; case CHAPELLE1: return chapelle1Data; case CHAPELLE2: return chapelle2Data; case CRYPTE1: return crypte1Data; case CRYPTE2: return crypte2Data;
            case PRISON_CENTRALE_1: return prisonCentrale1Data; case PRISON_CENTRALE_2: return prisonCentrale2Data; case PRISON_CENTRALE_3: return prisonCentrale3Data; case PRISON_CENTRALE_4: return prisonCentrale4Data;
            case PORTE_GOBELIN: return porteGobelinData; case COULOIR_GOBELIN_1: return couloirGobelin1Data; case INTERSECTION_GOBELIN_2: return intersectionGobelin2Data; case INTERSECTION_GOBELIN_3: return intersectionGobelin3Data; case INTERSECTION_GOBELIN_4: return intersectionGobelin4Data; case PUIT_GOBELIN: return puitGobelinData; case MARCHAND_GOBELIN: return marchandGobelinData; case ARMURERIE_GOBELIN: return armurerieGobelinData; case CUL_DE_SAC_GOBELIN: return culDeSacGobelinData; case MAISON_GOBELIN_1: return maisonGobelin1Data; case MAISON_GOBELIN_2: return maisonGobelin2Data; case MAISON_GOBELIN_3: return maisonGobelin3Data; case TRESORERIE_GOBELIN: return tresorerieGobelinData;
            default: return null;
        }
    }

    private static void loadAll() {
        if (loaded) return;
        try {
            for (int i = 0; i < 3; i++) couloirDataList[i] = loadNbt(COULOIR_PATHS[i]);
            i3Data = loadNbt(I3_PATH); i2Data = loadNbt(I2_PATH); i4Data = loadNbt(I4_PATH);
            culDeSacData = loadNbt(CULDESAC_PATH); doorCorridorData = loadNbt(DOOR_CORRIDOR_PATH);
            m1Data = loadNbt(M1_PATH); departData = loadNbt(DEPART_PATH); m2Data = loadNbt(M2_PATH);
            prisonData = loadNbt(PRISON_PATH); loot1Data = loadNbt(LOOT1_PATH);
            fontaineData = loadNbt(FONTAINE_PATH); puitData = loadNbt(PUITS_PATH);
            for (int i = 0; i < 4; i++) tavernData[i] = loadNbt(TAVERN_PATHS[i]);
            for (int i = 0; i < 3; i++) cjDataList[i] = loadNbt(CJ_PATHS[i]);
            ij2Data = loadNbt(IJ2_PATH); ij3Data = loadNbt(IJ3_PATH); ij4Data = loadNbt(IJ4_PATH);
            mj1Data = loadNbt(MJ1_PATH); mj2Data = loadNbt(MJ2_PATH); mj3Data = loadNbt(MJ3_PATH); mj4Data = loadNbt(MJ4_PATH); mj5Data = loadNbt(MJ5_PATH);
            lootdj1Data = loadNbt(LOOTDJ1_PATH); lootdj2Data = loadNbt(LOOTDJ2_PATH); lootdj3Data = loadNbt(LOOTDJ3_PATH);
            for (int i = 0; i < 4; i++) campDataList[i] = loadNbt(CAMP_PATHS[i]);
            bib1Data = loadNbt(BIB1_PATH); bib2Data = loadNbt(BIB2_PATH); bib2OpenData = loadNbt(BIB2_OPEN_PATH); shopData = loadNbt(SHOP_PATH);
            culDeSacDonjonData = loadNbt(CULDESACDONJON_PATH);
            m3Data = loadNbt(M3_PATH); m4Data = loadNbt(M4_PATH); ogreData = loadNbt(OGRE_PATH); puitDjData = loadNbt(PUITDJ_PATH); jardinData = loadNbt(JARDIN_PATH); statueData = loadNbt(STATUE_PATH); centraleData = loadNbt(CENTRALE_PATH); marchandNoirData = loadNbt(MARCHAND_NOIR_PATH); chapelle1Data = loadNbt(CHAPELLE1_PATH); chapelle2Data = loadNbt(CHAPELLE2_PATH);             crypte1Data = loadNbt(CRYPTE1_PATH); crypte2Data = loadNbt(CRYPTE2_PATH);
            prisonCentrale1Data = loadNbt(PRISON_CENTRALE_1_PATH); prisonCentrale2Data = loadNbt(PRISON_CENTRALE_2_PATH); prisonCentrale3Data = loadNbt(PRISON_CENTRALE_3_PATH); prisonCentrale4Data = loadNbt(PRISON_CENTRALE_4_PATH);
            porteGobelinData = loadNbt(PORTE_GOBELIN_PATH); couloirGobelin1Data = loadNbt(COULOIR_GOBELIN_1_PATH); intersectionGobelin2Data = loadNbt(INTERSECTION_GOBELIN_2_PATH); intersectionGobelin3Data = loadNbt(INTERSECTION_GOBELIN_3_PATH); intersectionGobelin4Data = loadNbt(INTERSECTION_GOBELIN_4_PATH); puitGobelinData = loadNbt(PUIT_GOBELIN_PATH); marchandGobelinData = loadNbt(MARCHAND_GOBELIN_PATH); armurerieGobelinData = loadNbt(ARMURERIE_GOBELIN_PATH); culDeSacGobelinData = loadNbt(CUL_DE_SAC_GOBELIN_PATH);             maisonGobelin1Data = loadNbt(MAISON_GOBELIN_1_PATH); maisonGobelin2Data = loadNbt(MAISON_GOBELIN_2_PATH); maisonGobelin3Data = loadNbt(MAISON_GOBELIN_3_PATH); tresorerieGobelinData = loadNbt(TRESORERIE_GOBELIN_PATH);
            loaded = true;

            if (!portsDetected) {
                roomPorts[CORRIDOR] = detectPorts(couloirDataList[0]); roomPorts[I3] = detectPorts(i3Data);
                roomPorts[I2] = detectPorts(i2Data); roomPorts[I4] = detectPorts(i4Data);
                roomPorts[CULDESAC] = detectPorts(culDeSacData); roomPorts[DEPART] = detectPorts(departData);
                roomPorts[M1] = detectPorts(m1Data); roomPorts[M2] = detectPorts(m2Data);
                roomPorts[PRISON] = detectPorts(prisonData); roomPorts[LOOT1] = detectPorts(loot1Data);
                roomPorts[FONTAINE] = detectPorts(fontaineData); roomPorts[PUITS] = detectPorts(puitData);
                roomPorts[PORTE2] = detectPorts(doorCorridorData); roomPorts[PORTE] = detectPorts(doorCorridorData);
                for (int i = 0; i < 4; i++) { roomPorts[T1 + i] = detectPorts(tavernData[i]); roomPurplePorts[T1 + i] = detectPurplePorts(tavernData[i]); }
                for (int i = 0; i < 3; i++) roomPorts[CJ1 + i] = detectPorts(cjDataList[i]);
                roomPorts[IJ2] = detectPorts(ij2Data); roomPorts[IJ3] = detectPorts(ij3Data); roomPorts[IJ4] = detectPorts(ij4Data);
                roomPorts[MJ1] = detectPorts(mj1Data); roomPorts[MJ2] = detectPorts(mj2Data);
                roomPorts[MJ3] = detectPorts(mj3Data); roomPorts[MJ4] = detectPorts(mj4Data); roomPorts[MJ5] = detectPorts(mj5Data);
                roomPorts[LOOTDJ1] = detectPorts(lootdj1Data); roomPorts[LOOTDJ2] = detectPorts(lootdj2Data); roomPorts[LOOTDJ3] = detectPorts(lootdj3Data);
                for (int i = 0; i < 4; i++) { roomPorts[CA1 + i] = detectPorts(campDataList[i]); roomPurplePorts[CA1 + i] = detectPurplePorts(campDataList[i]); }
                roomPorts[BIB1] = detectPorts(bib1Data); roomPurplePorts[BIB1] = detectPurplePorts(bib1Data);
                roomPorts[BIB2] = detectPorts(bib2Data); roomPurplePorts[BIB2] = detectPurplePorts(bib2Data); roomPorts[SHOP] = detectPorts(shopData);
                roomPorts[PORTE3] = detectPorts(culDeSacDonjonData); roomPorts[CULDJ] = detectPorts(culDeSacDonjonData);
                roomPorts[M3] = detectPorts(m3Data); roomPorts[M4] = detectPorts(m4Data);
                roomPorts[OGRE] = detectPorts(ogreData); roomPorts[PUITDJ] = detectPorts(puitDjData); roomPorts[JARDIN] = detectPorts(jardinData); roomPorts[STATUE] = detectPorts(statueData);
                roomPorts[CENTRALE] = new int[]{0}; roomPorts[MARCHAND_NOIR] = detectPorts(marchandNoirData); roomPorts[CHAPELLE1] = detectPorts(chapelle1Data); roomPorts[CHAPELLE2] = detectPorts(chapelle2Data); roomPorts[CRYPTE1] = detectPorts(crypte1Data); roomPorts[CRYPTE2] = detectPorts(crypte2Data); roomPurplePorts[CHAPELLE1] = detectPurplePorts(chapelle1Data); roomPurplePorts[CHAPELLE2] = detectPurplePorts(chapelle2Data); roomPurplePorts[CRYPTE1] = detectPurplePorts(crypte1Data); roomPurplePorts[CRYPTE2] = detectPurplePorts(crypte2Data);
                roomPorts[PRISON_CENTRALE_1] = detectPorts(prisonCentrale1Data); roomPorts[PRISON_CENTRALE_2] = detectPorts(prisonCentrale2Data); roomPorts[PRISON_CENTRALE_3] = detectPorts(prisonCentrale3Data); roomPorts[PRISON_CENTRALE_4] = detectPorts(prisonCentrale4Data);
                roomPurplePorts[PRISON_CENTRALE_1] = detectPurplePorts(prisonCentrale1Data); roomPurplePorts[PRISON_CENTRALE_2] = detectPurplePorts(prisonCentrale2Data); roomPurplePorts[PRISON_CENTRALE_3] = detectPurplePorts(prisonCentrale3Data); roomPurplePorts[PRISON_CENTRALE_4] = detectPurplePorts(prisonCentrale4Data);
                roomWhitePrisonPorts[PRISON_CENTRALE_1] = detectWhitePrisonPorts(prisonCentrale1Data); roomWhitePrisonPorts[PRISON_CENTRALE_2] = detectWhitePrisonPorts(prisonCentrale2Data); roomWhitePrisonPorts[PRISON_CENTRALE_3] = detectWhitePrisonPorts(prisonCentrale3Data); roomWhitePrisonPorts[PRISON_CENTRALE_4] = detectWhitePrisonPorts(prisonCentrale4Data);
                roomPorts[PORTE_GOBELIN] = detectPorts(porteGobelinData); roomPorts[COULOIR_GOBELIN_1] = detectPorts(couloirGobelin1Data); roomPorts[INTERSECTION_GOBELIN_2] = detectPorts(intersectionGobelin2Data); roomPorts[INTERSECTION_GOBELIN_3] = detectPorts(intersectionGobelin3Data); roomPorts[INTERSECTION_GOBELIN_4] = detectPorts(intersectionGobelin4Data); roomPorts[PUIT_GOBELIN] = detectPorts(puitGobelinData); roomPorts[MARCHAND_GOBELIN] = detectPorts(marchandGobelinData); roomPorts[ARMURERIE_GOBELIN] = detectPorts(armurerieGobelinData); roomPorts[CUL_DE_SAC_GOBELIN] = detectPorts(culDeSacGobelinData); roomPorts[MAISON_GOBELIN_1] = detectPorts(maisonGobelin1Data); roomPorts[MAISON_GOBELIN_2] = detectPorts(maisonGobelin2Data); roomPorts[MAISON_GOBELIN_3] = detectPorts(maisonGobelin3Data); roomPorts[TRESORERIE_GOBELIN] = detectPorts(tresorerieGobelinData);
                roomPurplePorts[PORTE_GOBELIN] = detectPurplePorts(porteGobelinData);
                System.out.println("[TestGenerator] Centrale ports: " + java.util.Arrays.toString(roomPorts[CENTRALE]));
                portsDetected = true;
                System.out.println("[TestGenerator] Ports detectes (toutes les salles):");
                for (int i = 0; i < roomPorts.length; i++) System.out.println("  " + (i < TYPE_NAMES.length ? TYPE_NAMES[i] : "?"+i) + ": " + Arrays.toString(roomPorts[i]));
            }
        } catch (Exception e) { e.printStackTrace(); }
    }

    // ===================== NBT loading & placement =====================

    private static BlockState[][][] loadNbt(String path) {
        try {
            URL url = TestGenerator.class.getResource(path); if (url == null) return null;
            InputStream is = url.openStream(); NbtCompound nbt = NbtIo.readCompressed(is, NbtSizeTracker.ofUnlimitedBytes()); is.close();
            int sx, sy, sz; int[] sizeArr = nbt.getIntArray("size");
            if (sizeArr.length == 0) { NbtList sl = nbt.getList("size", 3); sx = sl.getInt(0); sy = sl.getInt(1); sz = sl.getInt(2); } else { sx = sizeArr[0]; sy = sizeArr[1]; sz = sizeArr[2]; }
            NbtList palList = nbt.getList("palette", 10); List<BlockState> palette = new ArrayList<>();
            for (int i = 0; i < palList.size(); i++) palette.add(parseBlockState(palList.getCompound(i)));
            BlockState[][][] data = new BlockState[sx][sy][sz]; NbtList bl = nbt.getList("blocks", 10);
            for (int i = 0; i < bl.size(); i++) { NbtCompound ent = bl.getCompound(i); int[] pos = ent.getIntArray("pos"); int x, y, z;
                if (pos.length == 0) { NbtList pl = ent.getList("pos", 3); x = pl.getInt(0); y = pl.getInt(1); z = pl.getInt(2); } else { x = pos[0]; y = pos[1]; z = pos[2]; }
                if (x >= 0 && x < sx && y >= 0 && y < sy && z >= 0 && z < sz) data[x][y][z] = palette.get(ent.getInt("state")); }
            if (structSizeX == 0) { structSizeX = sx; structSizeY = sy; structSizeZ = sz; }
            return data;
        } catch (Exception e) { throw new RuntimeException("Erreur chargement " + path, e); }
    }

    public static int getStructSizeX() { return structSizeX; }
    public static int getStructSizeZ() { return structSizeZ; }
    public static BlockState[][][] getCentraleData() { return centraleData; }
    public static int[] getPurplePorts(int type) { return type >= 0 && type < roomPurplePorts.length ? roomPurplePorts[type] : null; }
    public static int[] getRoomPorts(int type) { return type >= 0 && type < roomPorts.length ? roomPorts[type] : null; }
    public static void initTestHub() { loadAll(); }

    public static void placeCentraleTest(ServerWorld world, int ox, int oy, int oz, int entreeDir) {
        if (centraleData == null) return;
        // Placer la Centrale
        placeRoom(world, ox, oy, oz, centraleData, 0);
        int[] dx = {1, 0, -1, 0};
        int[] dz = {0, 1, 0, -1};
        int cx = ox + dx[entreeDir] * 20, cz = oz + dz[entreeDir] * 20;
        // Placer un couloir dans la direction de l'entree
        if (couloirDataList[0] != null) {
            placeRoom(world, cx, oy, cz, couloirDataList[0], (entreeDir + 2) % 4);
        }
    }

    private static boolean isIronBars(BlockState state) {
        String id = Registries.BLOCK.getId(state.getBlock()).getPath();
        return id.contains("iron_bars") || id.contains("chain") || id.contains("bars");
    }

    private static void placeRoom(ServerWorld world, int ox, int oy, int oz, BlockState[][][] data, int rotation) {
        if (data == null) return;
        int sx = data.length, sy = data[0].length, sz = data[0][0].length;
        for (int x = 0; x < sx; x++) for (int y = 0; y < sy; y++) for (int z = 0; z < sz; z++) {
            BlockState state = data[x][y][z]; if (state != null && !state.isAir() && !isIronBars(state)) {
                int rx = rotateX(x, z, rotation, sx, sz), rz = rotateZ(x, z, rotation, sx, sz);
                world.setBlockState(new BlockPos(ox + rx, oy + y, oz + rz), rotateBlockState(state, rotation), 18);
            }
        }
        for (int x = 0; x < sx; x++) for (int y = 0; y < sy; y++) for (int z = 0; z < sz; z++) {
            BlockState state = data[x][y][z]; if (state != null && !state.isAir() && isIronBars(state)) {
                int rx = rotateX(x, z, rotation, sx, sz), rz = rotateZ(x, z, rotation, sx, sz);
                world.setBlockState(new BlockPos(ox + rx, oy + y, oz + rz), rotateBlockState(state, rotation), 3);
            }
        }
    }

    private static int rotateX(int x, int z, int r, int sx, int sz) { return switch (r) { case 1 -> sz - 1 - z; case 2 -> sx - 1 - x; case 3 -> z; default -> x; }; }
    private static int rotateZ(int x, int z, int r, int sx, int sz) { return switch (r) { case 1 -> x; case 2 -> sz - 1 - z; case 3 -> sx - 1 - x; default -> z; }; }
    private static int rotateX(int x, int z, int r) { return rotateX(x, z, r, structSizeX, structSizeZ); }
    private static int rotateZ(int x, int z, int r) { return rotateZ(x, z, r, structSizeX, structSizeZ); }

    private static BlockState parseBlockState(NbtCompound entry) {
        Block block = Registries.BLOCK.get(Identifier.of(entry.getString("Name"))); BlockState state = block.getDefaultState();
        if (entry.contains("Properties")) { NbtCompound props = entry.getCompound("Properties"); for (String key : props.getKeys()) { Property<?> prop = state.getBlock().getStateManager().getProperty(key); if (prop != null) state = applyProperty(state, prop, props.getString(key)); } }
        return state;
    }

    @SuppressWarnings("unchecked")
    private static <T extends Comparable<T>> BlockState applyProperty(BlockState state, Property<T> prop, String value) { return prop.parse(value).map(v -> state.with(prop, v)).orElse(state); }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static BlockState rotateBlockState(BlockState state, int rotation) {
        if (rotation == 0) return state;
        Property facingProp = state.getBlock().getStateManager().getProperty("facing");
        if (facingProp != null) state = applyProperty(state, facingProp, rotateFacing(String.valueOf(state.get(facingProp)), rotation));
        Property axisProp = state.getBlock().getStateManager().getProperty("axis");
        if (axisProp != null) state = applyProperty(state, axisProp, rotateAxis(String.valueOf(state.get(axisProp)), rotation));
        Property rotProp = state.getBlock().getStateManager().getProperty("rotation");
        if (rotProp != null && rotProp.getName().equals("rotation")) state = applyProperty(state, rotProp, String.valueOf(((int) state.get(rotProp) + rotation * 4) % 16));
        // Rotate directional connections (iron bars, fences, etc.)
        Property northProp = state.getBlock().getStateManager().getProperty("north");
        if (northProp != null && northProp.getType() == Boolean.class) {
            String[] dirs = {"north", "east", "south", "west"}; String[] vals = new String[4];
            for (int i = 0; i < 4; i++) { Property p = state.getBlock().getStateManager().getProperty(dirs[i]); if (p != null) vals[i] = String.valueOf(state.get(p)); }
            for (int i = 0; i < 4; i++) { Property p = state.getBlock().getStateManager().getProperty(dirs[i]); if (p != null && vals[i] != null) state = applyProperty(state, p, vals[(i - rotation + 4) % 4]); }
        }
        // Rotate WallSide properties (cobblestone walls, stone brick walls, etc.)
        Property northWall = state.getBlock().getStateManager().getProperty("north");
        if (northWall != null && !(northWall.getType() == Boolean.class)) {
            String[] dirs = {"north", "east", "south", "west"}; String[] vals = new String[4];
            for (int i = 0; i < 4; i++) { Property p = state.getBlock().getStateManager().getProperty(dirs[i]); if (p != null) vals[i] = String.valueOf(state.get(p)); }
            for (int i = 0; i < 4; i++) { Property p = state.getBlock().getStateManager().getProperty(dirs[i]); if (p != null && vals[i] != null) state = applyProperty(state, p, vals[(i - rotation + 4) % 4]); }
        }
        return state;
    }

    private static String rotateFacing(String facing, int rotation) { String[] dirs = {"north", "east", "south", "west"}; int idx = -1; for (int i = 0; i < dirs.length; i++) if (dirs[i].equals(facing)) { idx = i; break; } return idx < 0 ? facing : dirs[(idx + rotation) % 4]; }
    private static String rotateAxis(String axis, int rotation) { if (rotation == 0 || rotation == 2) return axis; return axis.equals("x") ? "z" : axis.equals("z") ? "x" : axis; }
}


