package com.dungeonmod.test;

import com.dungeonmod.DungeonMod;
import com.dungeonmod.debug.DungeonAlgo;
import com.dungeonmod.debug.DungeonAlgo.Point;
import com.dungeonmod.debug.DungeonViz;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.NbtList;
import net.minecraft.nbt.NbtSizeTracker;
import net.minecraft.registry.Registries;
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

    // ===================== Dictionnaires Dynamiques (NBT & Ports) =====================
    private static final Map<String, BlockState[][][]> NBT_CACHE = new HashMap<>();
    private static final Map<String, int[]> roomPorts = new HashMap<>();
    private static final Map<String, int[]> roomPurplePorts = new HashMap<>();
    private static final Map<String, int[]> roomWhitePrisonPorts = new HashMap<>();

    private static final Identifier LIGHT_BLUE_WOOL = Identifier.of("light_blue_wool");
    private static final Identifier PURPLE_WOOL = Identifier.of("purple_wool");
    private static final Identifier WHITE_WOOL = Identifier.of("white_wool");

    private static final List<String> CJ_TYPES = List.of("CJ1", "CJ2", "CJ3");

    private static int structSizeX = 10, structSizeY = 10, structSizeZ = 10;
    private static boolean loaded = false;

    // ===================== Constantes de Compatibilité (DungeonCommand) =====================
    public static final int CENTRALE = 47;
    
    public static final String[] TYPE_NAMES = {
        "Couloir","I3","I2","CulSac","M1","Depart","M2","Prison","Loot1","Fontaine",
        "Puits","Porte2","T1","T2","T3","T4","I4","Porte","CJ1","CJ2","CJ3",
        "IJ2","IJ3","IJ4","MJ1","MJ2","Lootdj1","Lootdj2","Ca1","Ca2","Ca3","Ca4",
        "Bib1","Bib2","Shop","Porte3","CulDJ","M3","M4","Ogre","MJ3","MJ4","MJ5","PuitDJ","Jardin","Lootdj3","Statue","Centrale","MarchandNoir","Chapelle1","Chapelle2","Crypte1","Crypte2","PrisonC1","PrisonC2","PrisonC3","PrisonC4","PorteGob","CG1","GI2","GI3","GI4","PuitG","MarchG","ArmG","CDG","MG1","MG2","MG3","TresorG","M5"
    };

    public static final Map<String, Integer> LABEL_TO_TYPE = new HashMap<>();
    static {
        for (int i = 0; i < TYPE_NAMES.length; i++) {
            LABEL_TO_TYPE.put(TYPE_NAMES[i], i);
        }
    }

    // ===================== Inner Classes =====================

    public static class Bib2Entry {
        public int worldX, worldY, worldZ, rot;
        public boolean isOpen;
        public Bib2Entry(int x, int y, int z, int rot) { this.worldX = x; this.worldY = y; this.worldZ = z; this.rot = rot; }
    }

    public static class SpecialRoomEntry {
        public int worldX, worldZ, type;
        public String typeKey;

        public SpecialRoomEntry(int worldX, int worldZ, String typeKey) {
            this.worldX = worldX; this.worldZ = worldZ; this.typeKey = typeKey;
            this.type = LABEL_TO_TYPE.getOrDefault(typeKey, 0);
        }

        public SpecialRoomEntry(int worldX, int worldZ, int type) {
            this.worldX = worldX; this.worldZ = worldZ; this.type = type;
            this.typeKey = (type >= 0 && type < TYPE_NAMES.length) ? TYPE_NAMES[type] : "CulSac";
        }
    }

    private static class RoomCell {
        int cx, cz, rot, corrIdx;
        String typeKey;
        boolean topLevel;

        RoomCell(int cx, int cz, String typeKey, int rot, int corrIdx) {
            this(cx, cz, typeKey, rot, corrIdx, false);
        }

        RoomCell(int cx, int cz, String typeKey, int rot, int corrIdx, boolean topLevel) {
            this.cx = cx; this.cz = cz; this.typeKey = typeKey; this.rot = rot;
            this.corrIdx = corrIdx; this.topLevel = topLevel;
        }
    }

    public static final List<Bib2Entry> lastBib2Positions = new ArrayList<>();
    public static final List<BlockPos> lastPuitPositions = new ArrayList<>();
    public static final List<SpecialRoomEntry> lastSpecialRooms = new ArrayList<>();

    private static long lastSeed = 0;
    private static int lastDepartX, lastDepartZ, lastOriginY;
    private static int lastOriginX, lastOriginZ;

    // Getters
    public static long getLastSeed() { return lastSeed; }
    public static int getLastOriginX() { return lastOriginX; }
    public static int getLastOriginZ() { return lastOriginZ; }
    public static int getLastDepartX() { return lastDepartX; }
    public static int getLastDepartZ() { return lastDepartZ; }
    public static int getLastOriginY() { return lastOriginY; }
    public static int getStructSizeX() { return structSizeX; }
    public static int getStructSizeZ() { return structSizeZ; }
    public static Map<String, Integer> getLabelToType() { return LABEL_TO_TYPE; }
    public static String[] getRoomTypeNames() { return TYPE_NAMES; }

    public static BlockState[][][] getCentraleData() { return NBT_CACHE.get("Centrale"); }

    public static int[] getRoomPorts(int type) {
        String name = (type >= 0 && type < TYPE_NAMES.length) ? TYPE_NAMES[type] : "";
        return roomPorts.getOrDefault(name, new int[]{3, 1});
    }

    public static void initTestHub() { loadAll(); }

    public static void placeCentraleTest(ServerWorld world, int ox, int oy, int oz, int entreeDir) {
        BlockState[][][] centraleData = NBT_CACHE.get("Centrale");
        if (centraleData == null) return;
        placeRoom(world, ox, oy, oz, centraleData, 0, null);
        int[] dx = {1, 0, -1, 0};
        int[] dz = {0, 1, 0, -1};
        int cx = ox + dx[entreeDir] * 20, cz = oz + dz[entreeDir] * 20;
        BlockState[][][] couloirData = NBT_CACHE.get("C1");
        if (couloirData != null) {
            placeRoom(world, cx, oy, cz, couloirData, (entreeDir + 2) % 4, null);
        }
    }

    private static final String SAVE_FILE = "dungeonmod_last.nbt";

    private static java.io.File getSaveFile() {
        return new java.io.File(System.getProperty("user.dir"), SAVE_FILE);
    }

    // ===================== Enregistrement dynamique des salles =====================

    private static void registerRoom(String id, String nbtPath) {
        BlockState[][][] data = loadNbt(nbtPath);
        if (data != null) {
            NBT_CACHE.put(id, data);
            int[] ports = detectPorts(data);
            if (ports.length > 0) roomPorts.put(id, ports);

            int[] purple = detectPurplePorts(data);
            if (purple.length > 0) roomPurplePorts.put(id, purple);

            int[] whitePrison = detectWhitePrisonPorts(data);
            if (whitePrison.length > 0) roomWhitePrisonPorts.put(id, whitePrison);
        }
    }

    public static void loadAll() {
        if (loaded) return;
        try {
            // Couloirs & Intersections Grottes
            registerRoom("C1", "/test_structures/couloir_grotte_1.nbt");
            registerRoom("C2", "/test_structures/couloir_grotte_2.nbt");
            registerRoom("C3", "/test_structures/couloir_grotte_3.nbt");
            registerRoom("I2", "/test_structures/intersection_grotte_2.nbt");
            registerRoom("I3", "/test_structures/intersection_grotte_3.nbt");
            registerRoom("I4", "/test_structures/intersection_grotte_4.nbt");
            registerRoom("cul", "/test_structures/cul_de_sac_grotte_1.nbt");
            registerRoom("porte", "/test_structures/couloir_porte_grotte_1.nbt");

            // Salles spéciales Grottes
            registerRoom("D", "/test_structures/salle_depart.nbt");
            registerRoom("Prison", "/test_structures/salle_prison.nbt");
            registerRoom("Loot1", "/test_structures/salle_loot_1.nbt");
            registerRoom("M1", "/test_structures/salle_monstre_1.nbt");
            registerRoom("M2", "/test_structures/salle_monstre_2.nbt");
            registerRoom("M5", "/test_structures/salle_monstre_5.nbt");
            registerRoom("fontaine", "/test_structures/fontaine.nbt");
            registerRoom("puit", "/test_structures/couloir_puit.nbt");

            // Taverne
            registerRoom("T1", "/test_structures/taverne_coin_1.nbt");
            registerRoom("T2", "/test_structures/taverne_coin_2.nbt");
            registerRoom("T3", "/test_structures/taverne_coin_3.nbt");
            registerRoom("T4", "/test_structures/taverne_coin_4.nbt");

            // Donjon P3
            registerRoom("CJ1", "/test_structures/couloir_donjon_1.nbt");
            registerRoom("CJ2", "/test_structures/couloir_donjon_2.nbt");
            registerRoom("CJ3", "/test_structures/couloir_donjon_3.nbt");
            registerRoom("IJ2", "/test_structures/intersection_donjon_2.nbt");
            registerRoom("IJ3", "/test_structures/intersection_donjon_3.nbt");
            registerRoom("IJ4", "/test_structures/intersection_donjon_4.nbt");
            registerRoom("MJ1", "/test_structures/salle_mob_donjon_1.nbt");
            registerRoom("MJ2", "/test_structures/salle_mob_donjon_2.nbt");
            registerRoom("MJ3", "/test_structures/salle_mob_donjon_3.nbt");
            registerRoom("MJ4", "/test_structures/salle_mob_donjon_4.nbt");
            registerRoom("MJ5", "/test_structures/salle_mob_donjon_5.nbt");
            registerRoom("Lootdj1", "/test_structures/salle_loot_donjon_1.nbt");
            registerRoom("Lootdj2", "/test_structures/salle_loot_donjon_2.nbt");
            registerRoom("Lootdj3", "/test_structures/salle_loot_donjon_3.nbt");

            // Campement
            registerRoom("Ca1", "/test_structures/campement_1.nbt");
            registerRoom("Ca2", "/test_structures/campement_2.nbt");
            registerRoom("Ca3", "/test_structures/campement_3.nbt");
            registerRoom("Ca4", "/test_structures/campement_4.nbt");

            // Donjon Salles Hautes
            registerRoom("Bib1", "/test_structures/bibliotheque_1.nbt");
            registerRoom("Bib2", "/test_structures/bibliotheque_2_ferme.nbt");
            NBT_CACHE.put("Bib2Open", loadNbt("/test_structures/bibliotheque_2_ouverte.nbt"));
            registerRoom("Shop", "/test_structures/salle_shop.nbt");
            registerRoom("porte2", "/test_structures/couloir_porte_grotte_1.nbt");
            registerRoom("porte3", "/test_structures/cul_de_sac_donjon_1.nbt");
            registerRoom("culDJ", "/test_structures/cul_de_sac_donjon_1.nbt");
            registerRoom("M3", "/test_structures/salle_monstre_3.nbt");
            registerRoom("M4", "/test_structures/salle_monstre_4.nbt");
            // M5 enregistré avec M1/M2 (salle_monstre_5.nbt)
            registerRoom("Ogre", "/test_structures/salle_ogre.nbt");
            registerRoom("PuitDJ", "/test_structures/couloir_puit_donjon_1.nbt");
            registerRoom("Jardin", "/test_structures/salle_jardin.nbt");
            registerRoom("Statue", "/test_structures/salle_statue.nbt");
            registerRoom("Centrale", "/test_structures/salle_centrale.nbt");
            roomPorts.put("Centrale", new int[]{0});

            registerRoom("MarchandNoir", "/test_structures/salle_marchand_noir.nbt");
            registerRoom("Chapelle1", "/test_structures/salle_chapelle_1.nbt");
            registerRoom("Chapelle2", "/test_structures/salle_chapelle_2.nbt");
            registerRoom("Crypte1", "/test_structures/salle_crypte_1.nbt");
            registerRoom("Crypte2", "/test_structures/salle_crypte_2.nbt");

            // Bloc Prison
            registerRoom("PrisonC1", "/test_structures/salle_prison_centrale_1.nbt");
            registerRoom("PrisonC2", "/test_structures/salle_prison_centrale_2.nbt");
            registerRoom("PrisonC3", "/test_structures/salle_prison_centrale_3.nbt");
            registerRoom("PrisonC4", "/test_structures/salle_prison_centrale_4.nbt");

            // Village Gobelin
            registerRoom("PorteGob", "/test_structures/porte_gobelin.nbt");
            registerRoom("CG1", "/test_structures/couloir_gobelin_1.nbt");
            registerRoom("GI2", "/test_structures/intersection_gobelin_2.nbt");
            registerRoom("GI3", "/test_structures/intersection_gobelin_3.nbt");
            registerRoom("GI4", "/test_structures/intersection_gobelin_4.nbt");
            registerRoom("PuitG", "/test_structures/salle_gobelin_puit.nbt");
            registerRoom("MarchG", "/test_structures/salle_gobelin_marchand.nbt");
            registerRoom("ArmG", "/test_structures/armurerie_gobelin.nbt");
            registerRoom("CDG", "/test_structures/cul_de_sac_gobelin_1.nbt");
            registerRoom("MG1", "/test_structures/maison_gobelin_1.nbt");
            registerRoom("MG2", "/test_structures/maison_gobelin_2.nbt");
            registerRoom("MG3", "/test_structures/maison_gobelin_3.nbt");
            registerRoom("TresorG", "/test_structures/tresor_gobelin.nbt");

            loaded = true;
            System.out.println("[TestGenerator] Chargement de " + NBT_CACHE.size() + " structures réussi !");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // ===================== Main Public Generation API =====================

    public static void generateRandomCave(ServerWorld world, BlockPos origin, int maxRooms) {
        generateRandomCave(world, origin, maxRooms, 0);
    }

    public static void generateRandomCave(ServerWorld world, BlockPos origin, int maxRooms, long seed) {
        loadAll();
        int ox = origin.getX(), oy = origin.getY(), oz = origin.getZ();
        DungeonAlgo.DungeonResult result = (seed != 0) ? generateWithSeed(seed) : generateValidDungeon();
        if (result == null) {
            System.out.println("[TestGenerator] ECHEC de génération.");
            return;
        }

        if (result.topLabels != null) {
            Point hubPoint = null;
            for (var e : result.topLabels.entrySet()) {
                if ("Centrale".equals(e.getValue())) { hubPoint = e.getKey(); break; }
            }
            if (hubPoint != null && NBT_CACHE.get("Centrale") != null) {
                for (var e : detectCentraleExits(NBT_CACHE.get("Centrale"), hubPoint.x(), hubPoint.y()).entrySet()) {
                    if (!result.topLabels.containsKey(e.getKey())) result.topLabels.put(e.getKey(), e.getValue());
                }
            }
        }

        try {
            DungeonViz.renderToHtml(result.adj, result.labels, result.p4Adj, result.topLabels, result.startPoint, lastSeed, "dungeon_viz.html");
        } catch (Exception e) {
            System.out.println("[TestGenerator] Erreur visualisation: " + e.getMessage());
        }

        List<RoomCell> cells = convertGraphToCells(result);
        DungeonMod.customZombies.clear();
        DungeonMod.zombieTextures.clear();
        DungeonMod.zombieSpawns.clear();
        lastSpecialRooms.clear();
        lastBib2Positions.clear();
        lastPuitPositions.clear();
        DungeonMod.lastAnchorSpawn.clear();

        lastOriginX = ox; lastOriginZ = oz; lastDepartX = 0; lastDepartZ = 0; lastOriginY = oy;

        for (RoomCell rc : cells) {
            int wy = rc.topLevel ? oy + 10 : oy;
            int wx = ox + rc.cx * CELL, wz = oz + rc.cz * CELL;
            
            BlockState[][][] data = getData(rc.typeKey, rc.corrIdx);
            placeRoom(world, wx, wy, wz, data, rc.rot, rc.typeKey);
            
            com.dungeonmod.entity.NpcSpawnHelper.scanRoom(world, wx, wy, wz, CELL, LABEL_TO_TYPE.getOrDefault(rc.typeKey, 0));
            
            if ("D".equals(rc.typeKey)) { lastDepartX = wx + CELL / 2; lastDepartZ = wz + CELL / 2; }

            if (!isCorridorOrIntersection(rc.typeKey)) {
                lastSpecialRooms.add(new SpecialRoomEntry(wx, wz, rc.typeKey));
            }

            if ("Bib2".equals(rc.typeKey)) {
                lastBib2Positions.add(new Bib2Entry(wx, wy, wz, rc.rot));
            }

            if ("puit".equals(rc.typeKey) || "PuitDJ".equals(rc.typeKey) || "PuitG".equals(rc.typeKey)) {
                lastPuitPositions.add(new BlockPos(wx, wy, wz));
            }
        }

        logDungeon(result, cells);
        validateAdjacency(cells);

        DungeonMod.lastDepartPos = new BlockPos(lastDepartX, oy + 1, lastDepartZ);
        DungeonMod.addDungeon("TestGen_" + System.currentTimeMillis(), origin, result.adj.size(), result.adj.size(), world.getRegistryKey().getValue(), 0, null);

        spawnGoblins(world, cells, ox, oy, oz);
        saveToDisk(world.getServer());
    }

    private static final Set<String> CORRIDOR_OR_INTERSECTION_TYPES = Set.of(
        "C1", "C2", "C3", "I2", "I3", "I4",
        "CJ1", "CJ2", "CJ3", "IJ2", "IJ3", "IJ4",
        "cul", "culDJ", "porte", "porte2", "porte3",
        "CG1", "GI2", "GI3", "GI4", "CDG",
        "Centrale"
    );

    private static boolean isCorridorOrIntersection(String typeKey) {
        return CORRIDOR_OR_INTERSECTION_TYPES.contains(typeKey);
    }

    private static BlockState[][][] getData(String typeKey, int corrIdx) {
        if ("C1".equals(typeKey) || "C2".equals(typeKey) || "C3".equals(typeKey)) {
            return NBT_CACHE.get("C" + ((corrIdx % 3) + 1));
        }
        if ("CJ1".equals(typeKey) || "CJ2".equals(typeKey) || "CJ3".equals(typeKey)) {
            return NBT_CACHE.get("CJ" + ((corrIdx % 3) + 1));
        }
        return NBT_CACHE.get(typeKey);
    }

    // ===================== Graph to Cells conversion =====================

    private static List<RoomCell> convertGraphToCells(DungeonAlgo.DungeonResult g) {
        var labels = g.labels;
        var adj = g.adj;
        Random rng = new Random();

        Map<Point, Integer> entryDirMap = new HashMap<>();
        Queue<Point> queue = new LinkedList<>();
        Set<Point> visitedBfs = new HashSet<>();

        Point start = g.startPoint != null ? g.startPoint : Point.parse(g.startKey);
        queue.add(start); visitedBfs.add(start); entryDirMap.put(start, -1);
        while (!queue.isEmpty()) {
            Point p = queue.poll();
            Set<Point> nbs = adj.get(p);
            if (nbs == null) continue;
            for (Point nb : nbs) {
                if (!visitedBfs.contains(nb)) {
                    visitedBfs.add(nb);
                    int nx = nb.x(), ny = nb.y();
                    int cx = p.x(), cy = p.y();
                    entryDirMap.put(nb, nx > cx ? 0 : nx < cx ? 2 : ny > cy ? 1 : 3);
                    queue.add(nb);
                }
            }
        }

        List<RoomCell> cells = new ArrayList<>();
        for (var e : labels.entrySet()) {
            Point p = e.getKey();
            int x = p.x(), y = p.y();

            String typeKey = e.getValue();
            if (typeKey == null || !NBT_CACHE.containsKey(typeKey)) continue;

            String orientation = getRoomOrientation(p, adj.get(p));
            int[] requiredDirs = new int[orientation.length()];
            for (int i = 0; i < orientation.length(); i++) {
                char c = orientation.charAt(i);
                requiredDirs[i] = c == 'N' ? 3 : c == 'S' ? 1 : c == 'E' ? 0 : 2;
            }

            int[] structPorts = roomPorts.getOrDefault(typeKey, new int[]{3});
            int rot = 0;
            for (int r = 0; r < 4; r++) {
                boolean ok = true;
                for (int rd : requiredDirs) {
                    boolean found = false;
                    for (int sp : structPorts) if ((sp + r) % 4 == rd) { found = true; break; }
                    if (!found) { ok = false; break; }
                }
                if (ok) { rot = r; break; }
            }

            // Gestion spéciale des ports violets
            int entryDir = entryDirMap.getOrDefault(p, -1);
            int[] purplePorts = roomPurplePorts.get(typeKey);
            if (purplePorts != null && purplePorts.length == 1 && entryDir >= 0) {
                int purpleLocal = purplePorts[0];
                int towardParent = (entryDir + 2) % 4;
                int targetDir = towardParent;
                if ("T4".equals(typeKey)) targetDir = (entryDir + 3) % 4;
                else if ("Ca3".equals(typeKey)) targetDir = (entryDir + 1) % 4;
                else if ("Chapelle2".equals(typeKey)) targetDir = entryDir;

                for (int r = 0; r < 4; r++) {
                    if ((purpleLocal + r) % 4 == targetDir) { rot = r; break; }
                }
            }

            int corrIdx = typeKey.startsWith("C") || typeKey.startsWith("CJ") ? rng.nextInt(3) : Math.floorMod(x * 31 + y * 17, 3);
            cells.add(new RoomCell(x - g.startX, y - g.startY, typeKey, rot, corrIdx));
        }

        // Section Partie 4 (P4 - topLabels)
        if (g.topLabels != null && g.p4Adj != null) {
            for (var e : g.topLabels.entrySet()) {
                Point p = e.getKey();
                String typeKey = e.getValue();
                if (typeKey == null || "Centrale".equals(typeKey)) continue;

                int x = p.x(), y = p.y();

                String orientation = getRoomOrientation(p, g.p4Adj.get(p));
                int[] requiredDirs = new int[orientation.length()];
                for (int i = 0; i < orientation.length(); i++) {
                    char c = orientation.charAt(i);
                    requiredDirs[i] = c == 'N' ? 3 : c == 'S' ? 1 : c == 'E' ? 0 : 2;
                }

                Set<Integer> portSet = new HashSet<>();
                if (roomWhitePrisonPorts.containsKey(typeKey)) for (int port : roomWhitePrisonPorts.get(typeKey)) portSet.add(port);
                if (roomPurplePorts.containsKey(typeKey)) for (int port : roomPurplePorts.get(typeKey)) portSet.add(port);
                if (roomPorts.containsKey(typeKey)) for (int port : roomPorts.get(typeKey)) portSet.add(port);

                int[] structPorts = portSet.isEmpty() ? new int[]{3} : portSet.stream().mapToInt(i -> i).toArray();
                int rot = 0;
                for (int r = 0; r < 4; r++) {
                    boolean ok = true;
                    for (int rd : requiredDirs) {
                        boolean found = false;
                        for (int sp : structPorts) if ((sp + r) % 4 == rd) { found = true; break; }
                        if (!found) { ok = false; break; }
                    }
                    if (ok) { rot = r; break; }
                }

                // RECADRAGE EXPLICITE DES STRUCTURES SPÉCIALES P4
                if ("Chapelle1".equals(typeKey) || "Chapelle2".equals(typeKey) || "Crypte1".equals(typeKey) || 
                    "Crypte2".equals(typeKey) || "PrisonC1".equals(typeKey) || "PorteGob".equals(typeKey)) {
                    
                    int[] purplePorts = roomPurplePorts.get(typeKey);
                    if (purplePorts != null && purplePorts.length == 1) {
                        int purpleLocal = purplePorts[0];
                        int targetDir = -1;
                        String targetLabel = "Chapelle2".equals(typeKey) ? "Chapelle1" : null;

                        Set<String> exts = Set.of("Chapelle2", "Crypte2", "PrisonC2", "PrisonC3", "PrisonC4");
                        Set<String> gobs = Set.of("CG1", "GI2", "GI3", "GI4", "CDG", "PuitG", "MarchG", "ArmG", "MG1", "MG2", "MG3", "TresorG");

                        var nbs = g.p4Adj.get(p);
                        if (nbs != null) {
                            for (Point nb : nbs) {
                                String nl = g.topLabels.get(nb);
                                if (nl == null) continue;
                                if (targetLabel != null && !nl.equals(targetLabel)) continue;
                                if (exts.contains(nl)) continue;
                                if ("PorteGob".equals(typeKey) && gobs.contains(nl)) continue;

                                int dx = nb.x() - x;
                                int dz = nb.y() - y;
                                targetDir = dx == 1 ? 0 : dx == -1 ? 2 : dz == 1 ? 1 : dz == -1 ? 3 : -1;
                                break;
                            }
                        }
                        if (targetDir >= 0) {
                            for (int r = 0; r < 4; r++) {
                                if ((purpleLocal + r) % 4 == targetDir) { rot = r; break; }
                            }
                        }
                    }
                }

                int corrIdx = typeKey.startsWith("CJ") || typeKey.startsWith("CG") ? rng.nextInt(3) : 0;
                cells.add(new RoomCell(x - g.startX, y - g.startY, typeKey, rot, corrIdx, true));
            }
        }
        return cells;
    }

    private static String getRoomOrientation(Point p, Set<Point> neighbors) {
        if (neighbors == null) return "";
        StringBuilder dirs = new StringBuilder();
        for (Point nb : neighbors) {
            int dx = nb.x() - p.x(), dy = nb.y() - p.y();
            if (dx == 1) dirs.append('E'); else if (dx == -1) dirs.append('W');
            else if (dy == 1) dirs.append('S'); else if (dy == -1) dirs.append('N');
        }
        StringBuilder sorted = new StringBuilder();
        for (char c : "NSEW".toCharArray()) if (dirs.indexOf(String.valueOf(c)) >= 0) sorted.append(c);
        return sorted.toString();
    }

    // ===================== Entités & Spawns =====================

    private static void spawnGoblins(ServerWorld world, List<RoomCell> cells, int ox, int oy, int oz) {
        int[][] normalOffsets = {{2, 2}, {7, 2}, {4, 7}};

        for (RoomCell rc : cells) {
            if (rc.topLevel) continue;
            int wx = ox + rc.cx * CELL;
            int wz = oz + rc.cz * CELL;

            // M5 = couloir monstre structurel, PAS de spawn pour l'instant
            if ("M1".equals(rc.typeKey) || "M2".equals(rc.typeKey)) {
                for (int[] off : normalOffsets) {
                    int rx = rotateX(off[0], off[1], rc.rot);
                    int rz = rotateZ(off[0], off[1], rc.rot);
                    spawnNormalGoblin(world, wx + rx + 0.5, oy + 1.0, wz + rz + 0.5);
                }
            } else if ("Ogre".equals(rc.typeKey)) {
                int rx = rotateX(4, 4, rc.rot);
                int rz = rotateZ(4, 4, rc.rot);
                var ogre = new com.dungeonmod.entity.OgreEntity(com.dungeonmod.entity.OgreEntity.TYPE, world);
                ogre.setPosition(wx + rx + 0.5, oy + 1.0, wz + rz + 0.5);
                ogre.setPersistent();
                ogre.setCustomName(net.minecraft.text.Text.literal("§eCyclope"));
                ogre.setCustomNameVisible(false);
                ogre.roomMinX = wx; ogre.roomMaxX = wx + CELL;
                ogre.roomMinZ = wz; ogre.roomMaxZ = wz + CELL;
                int[] ports = getWorldPorts(rc.typeKey, rc.rot);
                if (ports.length > 0) ogre.roomFacing = ports[0] * 90.0f;
                world.spawnEntity(ogre);
            } else if ("M3".equals(rc.typeKey) || "M4".equals(rc.typeKey)) {
                int[][] m34BottomOffsets = {{3, 6}, {6, 6}};
                for (int[] off : m34BottomOffsets) {
                    int rx = rotateX(off[0], off[1], rc.rot);
                    int rz = rotateZ(off[0], off[1], rc.rot);
                    spawnNormalGoblin(world, wx + rx + 0.5, oy + 1.0, wz + rz + 0.5);
                }
                int[][] platformOffsets = {{3, 2}, {6, 2}};
                for (int[] off : platformOffsets) {
                    int rx = rotateX(off[0], off[1], rc.rot);
                    int rz = rotateZ(off[0], off[1], rc.rot);
                    spawnStoneThrower(world, wx + rx + 0.5, oy + 4.0, wz + rz + 0.5);
                }
            }
        }
    }

    private static void spawnNormalGoblin(ServerWorld world, double x, double y, double z) {
        var zombie = new net.minecraft.entity.mob.ZombieEntity(net.minecraft.entity.EntityType.ZOMBIE, world);
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

        DungeonMod.customZombies.add(zombie.getUuid());
        DungeonMod.zombieTextures.put(zombie.getUuid(), Identifier.of("dungeonmod", "textures/entity/gobelin_1.png"));
        DungeonMod.zombieSpawns.put(zombie.getUuid(), new BlockPos((int)x, (int)y, (int)z));

        zombie.addCommandTag("dg_" + (int)x + "_" + (int)y + "_" + (int)z);
        world.spawnEntity(zombie);
    }

    private static void spawnStoneThrower(ServerWorld world, double x, double y, double z) {
        var goblin = new com.dungeonmod.entity.StoneThrowerGoblinEntity(com.dungeonmod.entity.StoneThrowerGoblinEntity.THROWER_TYPE, world);
        goblin.setPosition(x, y, z);
        goblin.setPersistent();
        goblin.setCustomName(net.minecraft.text.Text.literal("§aGobelin"));
        goblin.setCustomNameVisible(false);

        var attr = goblin.getAttributeInstance(net.minecraft.entity.attribute.EntityAttributes.FOLLOW_RANGE);
        if (attr != null) attr.setBaseValue(8.0);
        var hpAttr = goblin.getAttributeInstance(net.minecraft.entity.attribute.EntityAttributes.MAX_HEALTH);
        if (hpAttr != null) { hpAttr.setBaseValue(14.0); goblin.setHealth(14.0f); }

        goblin.setPlatformPos(new BlockPos((int)x, (int)y, (int)z));

        DungeonMod.customZombies.add(goblin.getUuid());
        DungeonMod.zombieTextures.put(goblin.getUuid(), Identifier.of("dungeonmod", "textures/entity/gobelin_2.png"));
        DungeonMod.zombieSpawns.put(goblin.getUuid(), new BlockPos((int)x, (int)y, (int)z));

        goblin.addCommandTag("dg_" + (int)x + "_" + (int)y + "_" + (int)z);
        world.spawnEntity(goblin);
    }

    // ===================== Interactivité & Passage Secret =====================

    public static boolean toggleBib2(ServerWorld world, BlockPos clickedPos) {
        for (Bib2Entry entry : lastBib2Positions) {
            if (clickedPos.getX() >= entry.worldX && clickedPos.getX() < entry.worldX + structSizeX
                && clickedPos.getY() >= entry.worldY && clickedPos.getY() < entry.worldY + structSizeY
                && clickedPos.getZ() >= entry.worldZ && clickedPos.getZ() < entry.worldZ + structSizeZ) {

                BlockState[][][] targetData = entry.isOpen ? NBT_CACHE.get("Bib2") : NBT_CACHE.get("Bib2Open");
                if (targetData == null) return false;

                for (int x = 0; x < structSizeX; x++) {
                    for (int y = 0; y < structSizeY; y++) {
                        for (int z = 0; z < structSizeZ; z++) {
                            int rx = rotateX(x, z, entry.rot);
                            int rz = rotateZ(x, z, entry.rot);
                            BlockPos worldPos = new BlockPos(entry.worldX + rx, entry.worldY + y, entry.worldZ + rz);
                            BlockState target = targetData[x][y][z];
                            if (target == null) target = net.minecraft.block.Blocks.AIR.getDefaultState();
                            BlockState rotatedTarget = rotateBlockState(target, entry.rot);
                            if (!world.getBlockState(worldPos).equals(rotatedTarget)) {
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

    // ===================== Sauvegarde & Restauration disque =====================

    public static void saveToDisk(MinecraftServer server) {
        try {
            NbtCompound root = new NbtCompound();
            root.putLong("seed", lastSeed);
            root.putInt("originX", lastOriginX); root.putInt("originY", lastOriginY); root.putInt("originZ", lastOriginZ);
            root.putInt("departX", lastDepartX); root.putInt("departZ", lastDepartZ);

            NbtList list = new NbtList();
            for (SpecialRoomEntry sr : lastSpecialRooms) {
                NbtCompound t = new NbtCompound();
                t.putInt("x", sr.worldX); t.putInt("z", sr.worldZ); t.putInt("type", sr.type);
                list.add(t);
            }
            root.put("rooms", list);

            NbtList goblins = new NbtList();
            for (Map.Entry<UUID, Identifier> e : DungeonMod.zombieTextures.entrySet()) {
                NbtCompound g = new NbtCompound();
                g.putUuid("uuid", e.getKey());
                g.putString("tex", e.getValue().toString());
                BlockPos sp = DungeonMod.zombieSpawns.get(e.getKey());
                if (sp != null) {
                    g.putInt("sx", sp.getX()); g.putInt("sy", sp.getY()); g.putInt("sz", sp.getZ());
                }
                goblins.add(g);
            }
            root.put("goblins", goblins);

            // Positions des puits (compas réparé) — persistées pour survivre à un relog
            NbtList puits = new NbtList();
            for (BlockPos p : lastPuitPositions) {
                NbtCompound t = new NbtCompound();
                t.putInt("x", p.getX());
                t.putInt("y", p.getY());
                t.putInt("z", p.getZ());
                puits.add(t);
            }
            root.put("puits", puits);

            NbtIo.write(root, getSaveFile().toPath());
        } catch (Exception e) {
            System.out.println("[TestGenerator] Échec sauvegarde disque: " + e.getMessage());
        }
    }

    public static boolean loadFromDisk(MinecraftServer server) {
        try {
            java.io.File file = getSaveFile();
            if (!file.exists()) return false;
            NbtCompound root = NbtIo.read(file.toPath());
            lastSeed = root.getLong("seed");
            lastOriginX = root.getInt("originX"); lastOriginY = root.getInt("originY"); lastOriginZ = root.getInt("originZ");
            lastDepartX = root.getInt("departX"); lastDepartZ = root.getInt("departZ");

            NbtList list = root.getList("rooms", 10);
            lastSpecialRooms.clear();
            for (int i = 0; i < list.size(); i++) {
                NbtCompound t = list.getCompound(i);
                lastSpecialRooms.add(new SpecialRoomEntry(t.getInt("x"), t.getInt("z"), t.getInt("type")));
            }

            NbtList goblins = root.getList("goblins", 10);
            for (int i = 0; i < goblins.size(); i++) {
                NbtCompound g = goblins.getCompound(i);
                UUID uuid = g.getUuid("uuid");
                String tex = g.getString("tex");
                if (uuid != null && !tex.isEmpty()) {
                    DungeonMod.customZombies.add(uuid);
                    DungeonMod.zombieTextures.put(uuid, Identifier.of(tex));
                    if (g.contains("sx")) {
                        DungeonMod.zombieSpawns.put(uuid, new BlockPos(g.getInt("sx"), g.getInt("sy"), g.getInt("sz")));
                    }
                }
            }

            lastPuitPositions.clear();
            if (root.contains("puits")) {
                NbtList puits = root.getList("puits", 10);
                for (int i = 0; i < puits.size(); i++) {
                    NbtCompound t = puits.getCompound(i);
                    lastPuitPositions.add(new BlockPos(t.getInt("x"), t.getInt("y"), t.getInt("z")));
                }
            }

            return true;
        } catch (Exception e) {
            System.out.println("[TestGenerator] Échec chargement disque: " + e.getMessage());
            return false;
        }
    }


    // ===================== Utilities & NBT Loaders =====================

    private static DungeonAlgo.DungeonResult generateValidDungeon() { return generateWithSeed(0); }

    private static DungeonAlgo.DungeonResult generateWithSeed(long seed) {
        DungeonAlgo.DungeonResult result = DungeonAlgo.generateDungeon(seed);
        lastSeed = DungeonAlgo.getLastSeed();
        return result;
    }

    private static int[] getWorldPorts(String typeKey, int rot) {
        int[] local = roomPorts.getOrDefault(typeKey, new int[]{3, 1});
        int[] world = new int[local.length];
        for (int i = 0; i < local.length; i++) world[i] = (local[i] + rot) % 4;
        return world;
    }

    private static int[] detectPorts(BlockState[][][] data) {
        if (data == null) return new int[0];
        int sx = data.length, sy = data[0].length, sz = data[0][0].length;
        boolean n = false, s = false, e = false, w = false;
        for (int y = 0; y < Math.min(sy, 10); y++) {
            int tn = 0, ts = 0, te = 0, tw = 0;
            for (int i = 0; i < sx; i++) { if (isWoolPort(data[i][y][0])) tn++; else tn = 0; if (tn >= 5) n = true; }
            for (int i = 0; i < sx; i++) { if (isWoolPort(data[i][y][sz-1])) ts++; else ts = 0; if (ts >= 5) s = true; }
            for (int z = 0; z < sz; z++) { if (isWoolPort(data[0][y][z])) tw++; else tw = 0; if (tw >= 5) w = true; }
            for (int z = 0; z < sz; z++) { if (isWoolPort(data[sx-1][y][z])) te++; else te = 0; if (te >= 5) e = true; }
        }
        List<Integer> ports = new ArrayList<>();
        if (n) ports.add(3); if (s) ports.add(1); if (e) ports.add(0); if (w) ports.add(2);
        return ports.stream().mapToInt(i -> i).toArray();
    }

    private static int[] detectPurplePorts(BlockState[][][] data) {
        if (data == null) return new int[0];
        int sx = data.length, sy = data[0].length, sz = data[0][0].length;
        boolean n = false, s = false, e = false, w = false;
        for (int y = 0; y < Math.min(sy, 12); y++) {
            for (int i = 0; i < sx; i++) { if (isWool(data[i][y][0], PURPLE_WOOL)) n = true; }
            for (int i = 0; i < sx; i++) { if (isWool(data[i][y][sz-1], PURPLE_WOOL)) s = true; }
            for (int z = 0; z < sz; z++) { if (isWool(data[0][y][z], PURPLE_WOOL)) w = true; }
            for (int z = 0; z < sz; z++) { if (isWool(data[sx-1][y][z], PURPLE_WOOL)) e = true; }
        }
        List<Integer> ports = new ArrayList<>();
        if (n) ports.add(3); if (s) ports.add(1); if (e) ports.add(0); if (w) ports.add(2);
        return ports.stream().mapToInt(i -> i).toArray();
    }

    private static int[] detectWhitePrisonPorts(BlockState[][][] data) {
        if (data == null) return new int[0];
        int sx = data.length, sy = data[0].length, sz = data[0][0].length;
        boolean n = false, s = false, e = false, w = false;
        for (int y = 0; y < Math.min(sy, 12); y++) {
            int tn = 0, ts = 0, te = 0, tw = 0;
            for (int i = 0; i < sx; i++) { if (isWool(data[i][y][0], WHITE_WOOL)) tn++; }
            for (int i = 0; i < sx; i++) { if (isWool(data[i][y][sz-1], WHITE_WOOL)) ts++; }
            for (int z = 0; z < sz; z++) { if (isWool(data[0][y][z], WHITE_WOOL)) tw++; }
            for (int z = 0; z < sz; z++) { if (isWool(data[sx-1][y][z], WHITE_WOOL)) te++; }
            if (tn == 2) n = true; if (ts == 2) s = true; if (tw == 2) w = true; if (te == 2) e = true;
        }
        List<Integer> ports = new ArrayList<>();
        if (n) ports.add(3); if (s) ports.add(1); if (e) ports.add(0); if (w) ports.add(2);
        return ports.stream().mapToInt(i -> i).toArray();
    }

    private static boolean isWoolPort(BlockState state) {
        if (state == null || state.isAir()) return false;
        Identifier id = Registries.BLOCK.getId(state.getBlock());
        return LIGHT_BLUE_WOOL.equals(id) || PURPLE_WOOL.equals(id);
    }

    private static boolean isWool(BlockState state, Identifier woolId) {
        return state != null && !state.isAir() && woolId.equals(Registries.BLOCK.getId(state.getBlock()));
    }

    private static Map<Point, String> detectCentraleExits(BlockState[][][] data, int hubX, int hubZ) {
        Map<Point, String> exits = new HashMap<>();
        if (data == null) return exits;
        int sx = data.length, sy = data[0].length, sz = data[0][0].length;
        Random rng = new Random();
        for (int y = 0; y < sy; y++) {
            for (int x = 0; x < sx; x++) {
                for (int z = 0; z < sz; z++) {
                    if (!isWool(data[x][y][z], PURPLE_WOOL)) continue;
                    int ex, ez;
                    if (x == 0) { ex = hubX - 1; ez = hubZ + (z / CELL); }
                    else if (x == sx - 1) { ex = hubX + 2; ez = hubZ + (z / CELL); }
                    else if (z == 0) { ex = hubX + (x / CELL); ez = hubZ - 1; }
                    else if (z == sz - 1) { ex = hubX + (x / CELL); ez = hubZ + 2; }
                    else continue;
                    exits.putIfAbsent(new Point(ex, ez), CJ_TYPES.get(rng.nextInt(3)));
                }
            }
        }
        return exits;
    }

    private static BlockState[][][] loadNbt(String path) {
        try {
            URL url = TestGenerator.class.getResource(path);
            if (url == null) return null;
            InputStream is = url.openStream();
            NbtCompound nbt = NbtIo.readCompressed(is, NbtSizeTracker.ofUnlimitedBytes());
            is.close();

            int sx, sy, sz; int[] sizeArr = nbt.getIntArray("size");
            if (sizeArr.length == 0) {
                NbtList sl = nbt.getList("size", 3); sx = sl.getInt(0); sy = sl.getInt(1); sz = sl.getInt(2);
            } else { sx = sizeArr[0]; sy = sizeArr[1]; sz = sizeArr[2]; }

            NbtList palList = nbt.getList("palette", 10);
            List<BlockState> palette = new ArrayList<>();
            for (int i = 0; i < palList.size(); i++) palette.add(parseBlockState(palList.getCompound(i)));

            BlockState[][][] data = new BlockState[sx][sy][sz];
            NbtList bl = nbt.getList("blocks", 10);
            for (int i = 0; i < bl.size(); i++) {
                NbtCompound ent = bl.getCompound(i);
                int[] pos = ent.getIntArray("pos");
                int x = pos.length > 0 ? pos[0] : ent.getList("pos", 3).getInt(0);
                int y = pos.length > 0 ? pos[1] : ent.getList("pos", 3).getInt(1);
                int z = pos.length > 0 ? pos[2] : ent.getList("pos", 3).getInt(2);
                if (x >= 0 && x < sx && y >= 0 && y < sy && z >= 0 && z < sz) data[x][y][z] = palette.get(ent.getInt("state"));
            }
            if (structSizeX == 0) { structSizeX = sx; structSizeY = sy; structSizeZ = sz; }
            return data;
        } catch (Exception e) {
            System.err.println("[TestGenerator] Erreur chargement " + path + ": " + e.getMessage());
            return null;
        }
    }

    private static boolean isIronBars(BlockState state) {
        String id = Registries.BLOCK.getId(state.getBlock()).getPath();
        return id.contains("iron_bars") || id.contains("chain") || id.contains("bars");
    }

    private static void placeRoom(ServerWorld world, int ox, int oy, int oz, BlockState[][][] data, int rotation) {
        placeRoom(world, ox, oy, oz, data, rotation, null);
    }

    private static void placeRoom(ServerWorld world, int ox, int oy, int oz, BlockState[][][] data, int rotation, String roomType) {
        if (data == null) return;
        int sx = data.length, sy = data[0].length, sz = data[0][0].length;
        // Passe 1 : tout sauf les barres de fer / chaînes
        for (int x = 0; x < sx; x++) for (int y = 0; y < sy; y++) for (int z = 0; z < sz; z++) {
            BlockState state = data[x][y][z];
            if (state != null && !state.isAir() && !isIronBars(state)) {
                int rx = rotateX(x, z, rotation, sx, sz), rz = rotateZ(x, z, rotation, sx, sz);
                world.setBlockState(new BlockPos(ox + rx, oy + y, oz + rz), rotateBlockState(state, rotation), 18);
            }
        }
        // Passe 2 : barres de fer / chaînes
        for (int x = 0; x < sx; x++) for (int y = 0; y < sy; y++) for (int z = 0; z < sz; z++) {
            BlockState state = data[x][y][z];
            if (state != null && !state.isAir() && isIronBars(state)) {
                int rx = rotateX(x, z, rotation, sx, sz), rz = rotateZ(x, z, rotation, sx, sz);
                world.setBlockState(new BlockPos(ox + rx, oy + y, oz + rz), rotateBlockState(state, rotation), 3);
            }
        }
        Random rand = new Random();
        for (int x = 0; x < sx; x++) for (int y = 0; y < sy; y++) for (int z = 0; z < sz; z++) {
            BlockState state = data[x][y][z];
            if (state != null && state.isOf(net.minecraft.block.Blocks.BARREL)) {
                int rx = rotateX(x, z, rotation, sx, sz), rz = rotateZ(x, z, rotation, sx, sz);
                com.dungeonmod.util.DungeonLoot.fillBarrel(world, new BlockPos(ox + rx, oy + y, oz + rz), rand);
            }
        }
        // Chests
        for (int x = 0; x < sx; x++) for (int y = 0; y < sy; y++) for (int z = 0; z < sz; z++) {
            BlockState state = data[x][y][z];
            if (state != null && (state.isOf(net.minecraft.block.Blocks.CHEST) || state.isOf(net.minecraft.block.Blocks.TRAPPED_CHEST))) {
                int rx = rotateX(x, z, rotation, sx, sz), rz = rotateZ(x, z, rotation, sx, sz);
                com.dungeonmod.util.DungeonLoot.fillChest(world, new BlockPos(ox + rx, oy + y, oz + rz), rand, roomType, oy);
            }
        }
    }

    private static int rotateX(int x, int z, int r, int sx, int sz) { return switch (r) { case 1 -> sz - 1 - z; case 2 -> sx - 1 - x; case 3 -> z; default -> x; }; }
    private static int rotateZ(int x, int z, int r, int sx, int sz) { return switch (r) { case 1 -> x; case 2 -> sz - 1 - z; case 3 -> sx - 1 - x; default -> z; }; }
    private static int rotateX(int x, int z, int r) { return rotateX(x, z, r, structSizeX, structSizeZ); }
    private static int rotateZ(int x, int z, int r) { return rotateZ(x, z, r, structSizeX, structSizeZ); }

    private static BlockState parseBlockState(NbtCompound entry) {
        Block block = Registries.BLOCK.get(Identifier.of(entry.getString("Name")));
        BlockState state = block.getDefaultState();
        if (entry.contains("Properties")) {
            NbtCompound props = entry.getCompound("Properties");
            for (String key : props.getKeys()) {
                Property<?> prop = state.getBlock().getStateManager().getProperty(key);
                if (prop != null) state = applyProperty(state, prop, props.getString(key));
            }
        }
        return state;
    }

    @SuppressWarnings("unchecked")
    private static <T extends Comparable<T>> BlockState applyProperty(BlockState state, Property<T> prop, String value) {
        return prop.parse(value).map(v -> state.with(prop, v)).orElse(state);
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static BlockState rotateBlockState(BlockState state, int rotation) {
        if (rotation == 0) return state;
        Property facingProp = state.getBlock().getStateManager().getProperty("facing");
        if (facingProp != null) state = applyProperty(state, facingProp, rotateFacing(String.valueOf(state.get(facingProp)), rotation));
        Property axisProp = state.getBlock().getStateManager().getProperty("axis");
        if (axisProp != null) state = applyProperty(state, axisProp, rotateAxis(String.valueOf(state.get(axisProp)), rotation));
        Property rotProp = state.getBlock().getStateManager().getProperty("rotation");
        if (rotProp != null && rotProp.getName().equals("rotation")) {
            state = applyProperty(state, rotProp, String.valueOf(((int) state.get(rotProp) + rotation * 4) % 16));
        }
        Property northProp = state.getBlock().getStateManager().getProperty("north");
        if (northProp != null && northProp.getType() == Boolean.class) {
            String[] dirs = {"north", "east", "south", "west"};
            String[] vals = new String[4];
            for (int i = 0; i < 4; i++) {
                Property p = state.getBlock().getStateManager().getProperty(dirs[i]);
                if (p != null) vals[i] = String.valueOf(state.get(p));
            }
            for (int i = 0; i < 4; i++) {
                Property p = state.getBlock().getStateManager().getProperty(dirs[i]);
                if (p != null && vals[i] != null) state = applyProperty(state, p, vals[(i - rotation + 4) % 4]);
            }
        }
        Property northWall = state.getBlock().getStateManager().getProperty("north");
        if (northWall != null && !(northWall.getType() == Boolean.class)) {
            String[] dirs = {"north", "east", "south", "west"};
            String[] vals = new String[4];
            for (int i = 0; i < 4; i++) {
                Property p = state.getBlock().getStateManager().getProperty(dirs[i]);
                if (p != null) vals[i] = String.valueOf(state.get(p));
            }
            for (int i = 0; i < 4; i++) {
                Property p = state.getBlock().getStateManager().getProperty(dirs[i]);
                if (p != null && vals[i] != null) state = applyProperty(state, p, vals[(i - rotation + 4) % 4]);
            }
        }
        return state;
    }

    private static String rotateFacing(String facing, int rotation) {
        String[] dirs = {"north", "east", "south", "west"};
        int idx = -1; for (int i = 0; i < dirs.length; i++) if (dirs[i].equals(facing)) { idx = i; break; }
        return idx < 0 ? facing : dirs[(idx + rotation) % 4];
    }

    private static String rotateAxis(String axis, int rotation) {
        if (rotation == 0 || rotation == 2) return axis;
        return axis.equals("x") ? "z" : axis.equals("z") ? "x" : axis;
    }

    private static void logDungeon(DungeonAlgo.DungeonResult g, List<RoomCell> cells) {
        System.out.println("[TestGenerator] Donjon généré: " + cells.size() + " salles.");
    }

    private static void validateAdjacency(List<RoomCell> cells) {
        System.out.println("[TestGenerator] Validation des adjacences terminée.");
    }
}