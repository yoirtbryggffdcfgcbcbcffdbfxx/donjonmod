package com.dungeonmod.debug;

import java.util.*;

public class DungeonAlgo {

    // ===================== Constants =====================

    public static final int[][] DIR_OFFSET = {{1, 0}, {0, 1}, {-1, 0}, {0, -1}};
    public static final int GRID_SIZE = 400;
    /** Hauteur monde (en blocs) entre deux couches logiques (level). */
    public static final int LEVEL_HEIGHT = 10;

    static final int PART1_TARGET_MIN = 24;
    static final int PART1_TARGET_MAX = 30;
    static final int PART1_MAX_I3 = 3;
    static final int PART1_MAX_I4 = 1;
    static final int PART1_STRAIGHT_WEIGHT = 1;

    static final int PART2_TARGET_MIN = 24;
    static final int PART2_TARGET_MAX = 30;
    static final int PART2_MAX_I3 = 4;
    static final int PART2_TRUNK_MIN = 8;
    static final int PART2_TRUNK_MAX = 12;
    static final int MAX_COLINEAR_RUN = 3;
    @Deprecated
    static final int PART2_MAX_COLINEAR_RUN = MAX_COLINEAR_RUN;

    static final int PART3_TARGET = 45;
    static final int PART3_MAX_IJ3 = 3;
    static final int PART3_MAX_IJ4 = 1;

    // ===================== Point Record & Helpers =====================

    /**
     * Nœud 3D natif : (x, y) dans le plan, {@code level} = couche logique (sans maximum).
     * Le plan reste borné par {@link #GRID_SIZE} ; la couche est native et sans limite.
     */
    public record Point(int x, int y, int level) {
        /** Point du plan sur la couche 0 (compat historique 2D). */
        public Point(int x, int y) { this(x, y, 0); }

        /** Parse "x,y" (couche 0) ou "x,y,level". */
        public static Point parse(String key) {
            int i1 = key.indexOf(',');
            int i2 = key.indexOf(',', i1 + 1);
            if (i2 < 0) {
                return new Point(
                    Integer.parseInt(key.substring(0, i1).trim()),
                    Integer.parseInt(key.substring(i1 + 1).trim()),
                    0
                );
            }
            return new Point(
                Integer.parseInt(key.substring(0, i1).trim()),
                Integer.parseInt(key.substring(i1 + 1, i2).trim()),
                Integer.parseInt(key.substring(i2 + 1).trim())
            );
        }

        /** Clé stable : "x,y" sur la couche 0 (compat), "x,y,level" sinon. */
        public String key() { return level == 0 ? x + "," + y : x + "," + y + "," + level; }

        /** Déplacement planaire ; la couche est conservée. */
        public Point move(int[] dir) { return new Point(x + dir[0], y + dir[1], level); }

        /** Déplacement planaire ; la couche est conservée. */
        public Point move(int dx, int dy) { return new Point(x + dx, y + dy, level); }

        /** Déplacement générique, vertical inclus (dl = ±1). */
        public Point move(int dx, int dy, int dl) { return new Point(x + dx, y + dy, level + dl); }

        /** Même cellule planaire, autre couche. */
        public Point atLevel(int l) { return new Point(x, y, l); }

        public Point above() { return new Point(x, y, level + 1); }

        public Point below() { return new Point(x, y, level - 1); }

        public boolean isOutOfBounds() {
            return x < 0 || x >= GRID_SIZE || y < 0 || y >= GRID_SIZE;
        }
    }

    // ===================== Classification geometrique =====================

    public enum Shape { DEAD_END, STRAIGHT, TURN, CROSS_3, CROSS_4 }

    public static Shape shapeOf(Set<Point> neighbors) {
        return DungeonLabels.shapeOf(neighbors);
    }

    public enum Theme { P12, DJ, GOBLIN }

    // ===================== Inner classes =====================

    public static class DungeonResult {
        public Map<Point, Set<Point>> adj;
        public Map<Point, RoomType> labels;
        public Map<Point, RoomType> topLabels;
        public Map<Point, Set<Point>> p4Adj;
        public Point startPoint;
        public String startKey;
        public int startX, startY;
        public String missingLootType;
        public long seed;
        public Set<Point> p2Monster5Cells = new HashSet<>();
        public Set<Point> p1MonsterCells = new HashSet<>();
        public Set<Point> p2MonsterCells = new HashSet<>();
        public Point p1PrisonAdjacent = null;

        /**
         * GRAPHE 3D UNIFIE : contient TOUTES les couches (sol + étage) ET les arêtes
         * verticales (escalier du hub 2x2x2). Source de vérité pour la connectivité
         * globale ; {@link #adj}/{@link #p4Adj} restent les vues par couche.
         */
        public Map<Point, Set<Point>> unifiedAdj;
        public Map<Point, RoomType> unifiedLabels;
        /** Cellules reservees (registre d'occupation 3D) : salles multi-couches protegees. */
        public Set<Point> reservedCells = new HashSet<>();

        /** Adjacence d'une seule couche : arêtes dont les DEUX extrémités sont sur la couche. */
        public Map<Point, Set<Point>> adjAt(int level) {
            Map<Point, Set<Point>> out = new HashMap<>();
            if (unifiedAdj == null) return out;
            for (var e : unifiedAdj.entrySet()) {
                if (e.getKey().level() != level) continue;
                Set<Point> nb = new HashSet<>();
                for (Point n : e.getValue()) if (n.level() == level) nb.add(n);
                out.put(e.getKey(), nb);
            }
            return out;
        }

        /** Labels d'une seule couche. */
        public Map<Point, RoomType> labelsAt(int level) {
            Map<Point, RoomType> out = new HashMap<>();
            if (unifiedLabels == null) return out;
            for (var e : unifiedLabels.entrySet()) if (e.getKey().level() == level) out.put(e.getKey(), e.getValue());
            return out;
        }
    }

    static class TreeResult {
        Point startPoint;
        String startKey;
        int startX, startY;
        Map<Point, Set<Point>> adj;
        Point trunkEnd;
        int trunkEndDir = -1;
    }

    static class TavernResult {
        Map<RoomType, Point> tavern;
        Point exitPoint;
        Set<Point> pathSet;
    }

    static class CampResult {
        Point campExit;
        Set<Point> campPathSet;
        Map<RoomType, Point> campNodes;
    }

    // ===================== Rules =====================

    public static final int MONSTER_MIN_DIST = 3;
    public static final int OGRE_MIN_MONSTER_DIST = 4;

    public static boolean isMonsterLabel(RoomType v) {
        return DungeonConstraints.isMonsterLabel(v);
    }

    public static boolean isGenericDeadEnd(RoomType v) {
        return DungeonConstraints.isGenericDeadEnd(v);
    }

    // ===================== Facades =====================

    private static String shapeLabel(Shape shape, Theme theme, Random rng) {
        return DungeonLabels.shapeLabel(shape, theme, rng).id;
    }

    private static RoomType labelForNeighbors(Set<Point> neighbors, Theme theme, Random rng) {
        return DungeonLabels.labelForNeighbors(neighbors, theme, rng);
    }

    public static boolean shapeMatchesLabel(Shape shape, RoomType label) {
        return DungeonLabels.shapeMatchesLabel(shape, label);
    }

    private static Theme genericThemeOf(RoomType label) {
        return DungeonLabels.genericThemeOf(label);
    }

    private static int reclassifyGeneric(Map<Point, RoomType> labels, Map<Point, Set<Point>> adj, Random rng) {
        return DungeonLabels.reclassifyGeneric(labels, adj, rng);
    }

    public static List<String> validateStructure(Map<Point, RoomType> labels, Map<Point, Set<Point>> adj, String scope) {
        return DungeonLabels.validateStructure(labels, adj, scope);
    }

    private static Set<Point> monsterPoints(Map<Point, RoomType> labels) {
        return DungeonConstraints.monsterPoints(labels);
    }

    public static Map<Point, Integer> bfsDistances(Map<Point, Set<Point>> adj, Point src) {
        return DungeonConstraints.bfsDistances(adj, src);
    }

    private static boolean isFarFromAll(Map<Point, Set<Point>> adj, Point cand, Collection<Point> targets, int minDist) {
        return DungeonConstraints.isFarFromAll(adj, cand, targets, minDist);
    }

    private static Point firstFar(Map<Point, Set<Point>> adj, List<Point> candidates, Collection<Point> targets, int minDist) {
        return DungeonConstraints.firstFar(adj, candidates, targets, minDist);
    }

    private static boolean isFreeCell(Map<Point, Set<Point>> adj, Point p) {
        return DungeonConstraints.isFreeCell(adj, p);
    }

    private static void moveDeadEnd(Map<Point, RoomType> labels, Map<Point, Set<Point>> adj,
                                    Point leaf, Point oldParent, Point newParent, Point target) {
        DungeonConstraints.moveDeadEnd(labels, adj, leaf, oldParent, newParent, target);
    }

    private static boolean enforceDeadEndAfterTurn(Map<Point, RoomType> labels, Map<Point, Set<Point>> adj, Random rng) {
        return DungeonConstraints.enforceDeadEndAfterTurn(labels, adj, rng);
    }

    // ===================== Algorithm facades =====================

    private static boolean hasPrisonCandidate(Map<Point, Set<Point>> adj, Point startPoint) {
        return DungeonPart1.hasPrisonCandidate(adj, startPoint);
    }

    private static TreeResult generateRawTree(int targetMin, int targetMax, int maxI3, int maxI4,
                                               int straightWeight, Point startPt, Set<Point> blocked,
                                               Random rng) {
        return DungeonTreeBuilder.generateRawTree(targetMin, targetMax, maxI3, maxI4, straightWeight,
                startPt, blocked, MAX_COLINEAR_RUN, rng);
    }

    private static TreeResult generatePart1Tree(Random rng) {
        return DungeonPart1.generatePart1Tree(rng);
    }

    private static Map<Point, RoomType> analyzePart1(Point startPoint, Map<Point, Set<Point>> adj, Random rng) {
        return DungeonPart1.analyzePart1(startPoint, adj, rng);
    }

    private static TavernResult placeTavernAndPath(Map<Point, Set<Point>> adj, Point porte, Random rng) {
        return DungeonPart1.placeTavernAndPath(adj, porte, rng);
    }

    private static Map<Point, RoomType> analyzePart2(Map<Point, Set<Point>> adj, Point exitPoint,
                                                    Map<Point, RoomType> labels, Set<Point> pathSet, Random rng) {
        return DungeonPart2.analyzePart2(adj, exitPoint, labels, pathSet, rng);
    }

    private static CampResult placeCampAndPath(Map<Point, Set<Point>> adj, Point porte2, Random rng) {
        return DungeonPart2.placeCampAndPath(adj, porte2, rng);
    }

    private static int maxColinearRunInGraph(Map<Point, Set<Point>> adj) {
        return DungeonConstraints.maxColinearRunInGraph(adj);
    }

    private static boolean respectsColinearLimit(Map<Point, Set<Point>> adj) {
        return DungeonConstraints.respectsColinearLimit(adj, MAX_COLINEAR_RUN);
    }

    private static TreeResult generatePart3Tree(Point startPoint, Set<Point> blocked, Random rng) {
        return DungeonPart3.generatePart3Tree(startPoint, blocked, rng);
    }

    private static Map<Point, RoomType> analyzePart3(Map<Point, Set<Point>> adj, Point campExit,
                                                     Map<Point, RoomType> labels, Random rng) {
        return DungeonPart3.analyzePart3(adj, campExit, labels, rng);
    }

    private static boolean generatePart4Tree(Map<Point, Set<Point>> adj,
                                              Map<Point, RoomType> topLabels,
                                              int hx, int hz, int level, Set<Point> externalBlocked,
                                              String missingLootType, Random rng) {
        return DungeonPart4.generatePart4Tree(adj, topLabels, hx, hz, level, externalBlocked, missingLootType, rng);
    }

    private static TreeResult generatePart2Tree(Point startPoint, Set<Point> blocked, Random rng) {
        return DungeonPart2.generatePart2Tree(startPoint, blocked, rng);
    }

    private static int colinearRunInAdj(Point origin, int dx, int dy, Map<Point, Set<Point>> adj) {
        return DungeonConstraints.colinearRunInAdj(origin, dx, dy, adj);
    }

    private static int colinearRunAfterEdge(Point from, Point to, Map<Point, Set<Point>> adj) {
        return DungeonConstraints.colinearRunAfterEdge(from, to, adj);
    }

    private static TreeResult generatePart2TrunkTree(Point startPt, Set<Point> blocked, Random rng) {
        return DungeonPart2.generatePart2TrunkTree(startPt, blocked, rng);
    }

    private static void growMiniTreeBounded(Point root, int pDir, Map<Point, Set<Point>> adj,
                                            Set<Point> occupied, Random rng) {
        DungeonPart2.growMiniTreeBounded(root, pDir, adj, occupied, rng);
    }

    private static Point appendP2ExitSequence(Map<Point, Set<Point>> adj, Map<Point, RoomType> labels,
                                              Point trunkEnd, int trunkEndDir, Set<Point> blocked,
                                              Random rng) {
        return DungeonPart2.appendP2ExitSequence(adj, labels, trunkEnd, trunkEndDir, blocked, rng);
    }

    private static int placeMonster5OnDoorPaths(Map<Point, RoomType> labels, Map<Point, Set<Point>> adj,
                                                 Point startPoint) {
        return DungeonPart1.placeMonster5OnDoorPaths(labels, adj, startPoint);
    }

    private static Point findPointByValue(Map<Point, RoomType> map, RoomType value) {
        if (map == null || value == null) return null;
        for (var e : map.entrySet()) if (value == e.getValue()) return e.getKey();
        return null;
    }

    // ===================== Public Main API =====================

    private static long lastSeed = 0;
    private static Map<Point, RoomType> lastTopLabels = null;

    public static long getLastSeed() { return lastSeed; }

    public static Map<Point, RoomType> getLastTopLabels() { return lastTopLabels; }

    public static DungeonResult generateDungeon(long seed) {
        int maxAttempts = seed != 0 ? 20 : 100;
        lastTopLabels = null;

        for (int outer = 0; outer < maxAttempts; outer++) {
            long actualSeed = seed != 0 ? seed + outer : System.nanoTime() + outer;
            Random rng = new Random(actualSeed);

            TreeResult sp1 = null;
            Map<Point, RoomType> labels = null;
            for (int inner = 0; inner < 50; inner++) {
                TreeResult try1 = generatePart1Tree(rng);
                if (try1.adj.size() < 10) continue;
                List<Point> leaves = new ArrayList<>();
                for (var e : try1.adj.entrySet()) if (e.getValue().size() == 1 && !e.getKey().equals(try1.startPoint)) leaves.add(e.getKey());
                if (leaves.size() < 4) continue;
                if (!hasPrisonCandidate(try1.adj, try1.startPoint)) continue;
                Map<Point, RoomType> tryLabels = analyzePart1(try1.startPoint, try1.adj, rng);
                if (tryLabels == null) continue;
                sp1 = try1; labels = tryLabels; break;
            }
            if (sp1 == null) continue;

            Set<Point> p1MonsterCells = new HashSet<>();
            Point p1PrisonAdjacent = null;
            for (var e : labels.entrySet()) {
                RoomType v = e.getValue();
                if (v == RoomType.MONSTER_1 || v == RoomType.MONSTER_2
                        || v == RoomType.MONSTER_3 || v == RoomType.MONSTER_4
                        || v == RoomType.MONSTER_5) {
                    p1MonsterCells.add(e.getKey());
                }
                if (v == RoomType.PRISON) {
                    for (Point nb : sp1.adj.getOrDefault(e.getKey(), Set.of())) {
                        RoomType nl = labels.get(nb);
                        if (nl == RoomType.MONSTER_2 || nl == RoomType.MONSTER_4) {
                            p1PrisonAdjacent = nb;
                        }
                    }
                }
            }

            Point porteKey = findPointByValue(labels, RoomType.DOOR_1);
            if (porteKey == null) continue;

            TavernResult tavern = placeTavernAndPath(sp1.adj, porteKey, rng);
            if (tavern == null) continue;
            for (var e : tavern.tavern.entrySet()) labels.put(e.getValue(), e.getKey());

            TreeResult sp2 = generatePart2Tree(tavern.exitPoint, new HashSet<>(sp1.adj.keySet()), rng);
            if (sp2.adj.size() < 5) continue;
            for (var e : sp2.adj.entrySet()) { if (sp1.adj.containsKey(e.getKey())) sp1.adj.get(e.getKey()).addAll(e.getValue()); else sp1.adj.put(e.getKey(), e.getValue()); }

            Point porte2Key = appendP2ExitSequence(sp1.adj, labels, sp2.trunkEnd, sp2.trunkEndDir,
                    null, rng);
            if (porte2Key == null) continue;

            int p1Loot = 0; for (RoomType v : labels.values()) if (v == RoomType.LOOT_1) p1Loot++;
            int totalTarget = 1 + rng.nextInt(2);
            if (p1Loot > totalTarget) {
                List<Point> lootNodes = new ArrayList<>();
                for (var e : labels.entrySet()) if (e.getValue() == RoomType.LOOT_1 && !e.getKey().equals(sp1.startPoint)) lootNodes.add(e.getKey());
                Collections.shuffle(lootNodes, rng);
                for (int i = 0; i < p1Loot - totalTarget && i < lootNodes.size(); i++) labels.put(lootNodes.get(i), RoomType.CUL);
            }

            labels = analyzePart2(sp1.adj, tavern.exitPoint, labels, tavern.pathSet, rng);
            if (labels == null) continue;

            Set<Point> p2M5Cells = new HashSet<>();
            Set<Point> p2MonsterCells = new HashSet<>();
            for (var e : labels.entrySet()) {
                RoomType v = e.getValue();
                if (v == null) continue;
                if (!p1MonsterCells.contains(e.getKey())
                        && (v == RoomType.MONSTER_1 || v == RoomType.MONSTER_2
                        || v == RoomType.MONSTER_3 || v == RoomType.MONSTER_4
                        || v == RoomType.MONSTER_5)) {
                    p2MonsterCells.add(e.getKey());
                }
                if (v == RoomType.MONSTER_5) p2M5Cells.add(e.getKey());
            }

            if (findPointByValue(labels, RoomType.DOOR_2) == null) continue;

            CampResult camp = placeCampAndPath(sp1.adj, porte2Key, rng);
            if (camp == null) continue;
            for (Point e : camp.campPathSet) {
                if (labels.containsKey(e)) continue;
                Set<Point> nb = sp1.adj.get(e);
                if (nb != null && nb.size() == 2) labels.put(e, labelForNeighbors(nb, Theme.P12, rng));
            }
            for (var e : camp.campNodes.entrySet()) labels.put(e.getValue(), e.getKey());

            TreeResult sp3 = generatePart3Tree(camp.campExit, new HashSet<>(sp1.adj.keySet()), rng);
            if (sp3.adj.size() < 10) continue;
            if (sp3.adj.get(sp3.startPoint).isEmpty()) continue;
            for (var e : sp3.adj.entrySet()) { if (sp1.adj.containsKey(e.getKey())) sp1.adj.get(e.getKey()).addAll(e.getValue()); else sp1.adj.put(e.getKey(), e.getValue()); }

            labels = analyzePart3(sp1.adj, camp.campExit, labels, rng);
            if (labels != null) {
                String missingLoot = null;
                Set<RoomType> p3LootTypes = new HashSet<>();
                for (RoomType v : labels.values()) if (v != null && v.isDjLoot()) p3LootTypes.add(v);
                for (RoomType lt : RoomType.LOOT_P3_P4) { if (!p3LootTypes.contains(lt)) { missingLoot = lt.id; break; } }

                boolean p4Ok = false;
                Map<Point, RoomType> currentTopLabels = null;
                Map<Point, Set<Point>> p4Adj = null;
                DungeonOccupancy occupancy = new DungeonOccupancy();
                Point hubPoint = findPointByValue(labels, RoomType.CENTRALE);
                if (hubPoint != null) {
                    occupancy.reserve(hubPoint, RoomType.CENTRALE.size);
                    Point topHub = hubPoint.atLevel(1);
                    for (int p4Retry = 0; p4Retry < 15; p4Retry++) {
                        currentTopLabels = new HashMap<>();
                        currentTopLabels.put(topHub, RoomType.CENTRALE);
                        p4Adj = new HashMap<>();
                        if (generatePart4Tree(p4Adj, currentTopLabels, topHub.x(), topHub.y(), topHub.level(), occupancy.cells(), missingLoot, rng)) {
                            p4Ok = true;
                            break;
                        }
                    }
                }
                if (!p4Ok) continue;

                int m5 = placeMonster5OnDoorPaths(labels, sp1.adj, sp1.startPoint);
                if (m5 < 1) continue;

                if (!respectsColinearLimit(sp1.adj)) continue;

                reclassifyGeneric(labels, sp1.adj, rng);

                DungeonResult dr = new DungeonResult();
                dr.adj = sp1.adj; dr.labels = labels;
                dr.startPoint = sp1.startPoint;
                dr.startKey = sp1.startKey;
                dr.startX = sp1.startX; dr.startY = sp1.startY;
                dr.topLabels = currentTopLabels;
                dr.p4Adj = p4Adj.isEmpty() ? null : p4Adj;
                dr.missingLootType = missingLoot;
                dr.seed = actualSeed;
                dr.p2Monster5Cells = p2M5Cells;
                dr.p1MonsterCells = p1MonsterCells;
                dr.p2MonsterCells = p2MonsterCells;
                dr.p1PrisonAdjacent = p1PrisonAdjacent;
                dr.reservedCells = new HashSet<>(occupancy.cells());

                // --- Graphe 3D unifie : toutes les couches + aretes verticales du hub ---
                dr.unifiedAdj = new HashMap<>();
                for (var e : dr.adj.entrySet()) dr.unifiedAdj.computeIfAbsent(e.getKey(), k -> new HashSet<>()).addAll(e.getValue());
                if (dr.p4Adj != null) for (var e : dr.p4Adj.entrySet()) dr.unifiedAdj.computeIfAbsent(e.getKey(), k -> new HashSet<>()).addAll(e.getValue());
                dr.unifiedLabels = new HashMap<>(dr.labels);
                if (dr.topLabels != null) dr.unifiedLabels.putAll(dr.topLabels);
                // Salle multi-cellules generique : maillage du volume + aretes verticales
                // (une salle de size.y() > 1 est reliee nativement entre ses couches).
                DungeonRoomPlacement.meshFootprint(dr.unifiedAdj, hubPoint, RoomType.CENTRALE.size);
                DungeonRoomPlacement.linkVertical(dr.unifiedAdj, hubPoint, RoomType.CENTRALE.size);

                lastSeed = actualSeed;
                DungeonAlgo.lastTopLabels = currentTopLabels;

                return dr;
            }
        }
        return null;
    }
}
