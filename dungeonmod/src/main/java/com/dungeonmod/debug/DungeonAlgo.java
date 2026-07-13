package com.dungeonmod.debug;

import java.util.*;

public class DungeonAlgo {

    // ===================== Constants =====================

    private static final int[][] DIR_OFFSET = {{1, 0}, {0, 1}, {-1, 0}, {0, -1}};
    private static final int GRID_SIZE = 400;

    private static final int PART1_TARGET_MIN = 20;
    private static final int PART1_TARGET_MAX = 26;
    private static final int PART1_MAX_I3 = 3;
    private static final int PART1_MAX_I4 = 1;
    private static final int PART1_STRAIGHT_WEIGHT = 1;

    private static final int PART2_TARGET_MIN = 18;
    private static final int PART2_TARGET_MAX = 25;
    private static final int PART2_MAX_I3 = 4;
    private static final int PART2_STRAIGHT_WEIGHT = 1;

    private static final int PART3_TARGET = 45;
    private static final int PART3_MAX_IJ3 = 3;
    private static final int PART3_MAX_IJ4 = 1;
    private static final int PART3_STRAIGHT_WEIGHT = 2;

    // ===================== Inner classes =====================

    public static class RoomConfig {
        public String type;
        public List<String> doors;
        public boolean turnAfter2;
        public RoomConfig(String type, List<String> doors, boolean turnAfter2) {
            this.type = type; this.doors = doors; this.turnAfter2 = turnAfter2;
        }
    }

    public static class DungeonResult {
        public Map<String, Set<String>> adj;
        public Map<String, String> labels;
        public Map<String, String> topLabels;
        public Map<String, Set<String>> p4Adj;
        public String startKey;
        public int startX, startY;
        public String missingLootType;
    }

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

    // ===================== Config maps =====================

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
        ROOM_CONFIGS.put("M3",       new RoomConfig("Cul de sac",    List.of("N"), false));
        ROOM_CONFIGS.put("M4",       new RoomConfig("Couloir droit", List.of("N","S"), true));
        ROOM_CONFIGS.put("Ogre",     new RoomConfig("Cul de sac",    List.of("N"), false));
        ROOM_CONFIGS.put("MJ3",      new RoomConfig("Cul de sac",    List.of("N"), false));
        ROOM_CONFIGS.put("MJ4",      new RoomConfig("Couloir droit", List.of("N","S"), true));
        ROOM_CONFIGS.put("MJ5",      new RoomConfig("Cul de sac",    List.of("N"), false));
        ROOM_CONFIGS.put("PuitDJ",   new RoomConfig("Couloir droit", List.of("N","S"), true));
        ROOM_CONFIGS.put("Jardin",   new RoomConfig("Cul de sac",    List.of("N"), false));
        ROOM_CONFIGS.put("Lootdj3",  new RoomConfig("Cul de sac",    List.of("N"), false));
        ROOM_CONFIGS.put("Statue",  new RoomConfig("Cul de sac",    List.of("N"), false));
        ROOM_CONFIGS.put("Centrale", new RoomConfig("Cul de sac",   List.of("S"), false));
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

    // ===================== Algorithm: Part 1 tree =====================

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
                        if (!occupied.contains(nk) && !blocked.contains(nk)) {
                            // Eviter I3/I4 consecutifs en ligne droite
                            if (deg >= 2) {
                                boolean skip = false;
                                for (String nb1 : adj.get(node)) {
                                    String[] n1p = nb1.split(",");
                                    int n1x = Integer.parseInt(n1p[0]), n1z = Integer.parseInt(n1p[1]);
                                    for (String nb2 : adj.get(node)) {
                                        if (nb1.equals(nb2)) continue;
                                        String[] n2p = nb2.split(",");
                                        int n2x = Integer.parseInt(n2p[0]), n2z = Integer.parseInt(n2p[1]);
                                        if (px - n1x == n2x - px && py - n1z == n2z - py) {
                                            Set<String> a1 = adj.get(nb1), a2 = adj.get(nb2);
                                            if ((a1 != null && a1.size() >= 3) || (a2 != null && a2.size() >= 3)) {
                                                skip = true; break;
                                            }
                                        }
                                    }
                                    if (skip) break;
                                }
                                if (skip) continue;
                            }
                            candidates.add(new String[]{node, nk});
                        }
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
            occupied.add(child); allNodes.add(child);
            adj.put(child, new HashSet<>());
            adj.get(parent).add(child); adj.get(child).add(parent);
            entryDir.put(child, new int[]{Integer.parseInt(child.split(",")[0]) - Integer.parseInt(parent.split(",")[0]), Integer.parseInt(child.split(",")[1]) - Integer.parseInt(parent.split(",")[1])});
        }
        TreeResult tr = new TreeResult(); tr.startKey = startKey; tr.startX = sx; tr.startY = sy; tr.adj = adj; return tr;
    }

    private static TreeResult generatePart1Tree() {
        return generateRawTree(PART1_TARGET_MIN, PART1_TARGET_MAX, PART1_MAX_I3, PART1_MAX_I4, PART1_STRAIGHT_WEIGHT, null, null);
    }

    // ===================== Algorithm: analyzePart1 =====================

    private static Map<String, String> analyzePart1(String startKey, Map<String, Set<String>> adj, Random rng) {
        Map<String, String> labels = new HashMap<>();
        List<String> leaves = new ArrayList<>();
        for (var e : adj.entrySet()) if (e.getValue().size() == 1 && !e.getKey().equals(startKey)) leaves.add(e.getKey());
        if (leaves.size() < 5) return null;
        Collections.shuffle(leaves, rng);

        String prison = null;
        for (String leaf : leaves) {
            String parent = adj.get(leaf).iterator().next();
            if (adj.get(parent).size() == 2) {
                Set<String> pn = new HashSet<>(adj.get(parent)); pn.remove(leaf);
                String gp = pn.iterator().next();
                int pdx = Integer.parseInt(parent.split(",")[0]) - Integer.parseInt(gp.split(",")[0]);
                int pdy = Integer.parseInt(parent.split(",")[1]) - Integer.parseInt(gp.split(",")[1]);
                int cdx = Integer.parseInt(leaf.split(",")[0]) - Integer.parseInt(parent.split(",")[0]);
                int cdy = Integer.parseInt(leaf.split(",")[1]) - Integer.parseInt(parent.split(",")[1]);
                if (pdx == cdx && pdy == cdy) { prison = leaf; break; }
            }
        }
        if (prison == null) return null;

        List<String> others = new ArrayList<>();
        for (String l : leaves) if (!l.equals(prison)) others.add(l);

        String porte = others.get(0);
        labels.put(startKey, "D");
        labels.put(porte, "porte");
        labels.put(prison, "Prison");
        String prisonParent = adj.get(prison).iterator().next();
        labels.put(prisonParent, "M2");
        labels.put(others.get(1), "Loot1");
        labels.put(others.get(2), "M1");
        boolean isM4 = rng.nextBoolean();
        // Marquer les feuilles restantes comme cul pour l'instant
        if (others.size() > 3) {
            for (int i = 3; i < others.size(); i++) labels.put(others.get(i), "cul");
        }

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

        // M4 sur couloir droit si possible, sinon M3 sur feuille
        String m4Key = null;
        if (isM4) {
            for (String n : cNodes) { if (!labels.containsKey(n)) { m4Key = n; break; } }
        }
        if (m4Key != null) {
            labels.put(m4Key, "M4");
        } else if (others.size() > 3) {
            // Fallback M3 sur la feuille others.get(3)
            labels.put(others.get(3), "M3");
        }

        // Ajouter 1 puit garanti (toujours sur couloir droit)
        boolean puitPlaced = false;
        for (String n : cNodes) {
            if (!labels.containsKey(n)) { labels.put(n, "puit"); puitPlaced = true; break; }
        }
        // P1: puit/M2/M4 ne peuvent pas avoir de couloir a cote
        for (String n : cNodes) {
            if (labels.containsKey(n)) continue;
            for (String nb : adj.get(n)) {
                String lbl = labels.get(nb);
                if (lbl != null && (lbl.equals("M2") || lbl.equals("M4") || lbl.equals("puit"))) {
                    List<String> nbs = new ArrayList<>(adj.get(n));
                    if (nbs.size() == 2) {
                        String[] p1 = nbs.get(0).split(","), p2 = nbs.get(1).split(",");
                        boolean droit = Integer.parseInt(p1[0]) - Integer.parseInt(p2[0]) == 0
                            || Integer.parseInt(p1[1]) - Integer.parseInt(p2[1]) == 0;
                        if (!droit) { labels.put(n, "I2"); break; }
                    }
                }
            }
        }
        for (String n : cNodes) if (!labels.containsKey(n)) labels.put(n, pickC(rng));
        for (String n : i2Nodes) if (!labels.containsKey(n)) labels.put(n, "I2");
        for (String n : i3Nodes) labels.put(n, "I3");
        for (String n : i4Nodes) labels.put(n, "I4");
        // Apres une I4, les voisins couloirs perpendiculaires deviennent I2
        for (String n : i4Nodes) {
            String[] ip = n.split(",");
            int ix = Integer.parseInt(ip[0]), iz = Integer.parseInt(ip[1]);
            for (String nb : adj.get(n)) {
                String lbl = labels.get(nb);
                if (lbl == null || !(lbl.equals("C1") || lbl.equals("C2") || lbl.equals("C3"))) continue;
                String[] np = nb.split(",");
                int nx = Integer.parseInt(np[0]), nz = Integer.parseInt(np[1]);
                List<String> nAdj = new ArrayList<>(adj.get(nb));
                if (nAdj.size() != 2) continue;
                String[] a0 = nAdj.get(0).split(","), a1 = nAdj.get(1).split(",");
                int adx = Integer.parseInt(a1[0]) - Integer.parseInt(a0[0]);
                int adz = Integer.parseInt(a1[1]) - Integer.parseInt(a0[1]);
                if (adx * (nx - ix) + adz * (nz - iz) == 0) {
                    boolean droit = adx == 0 || adz == 0;
                    if (!droit) labels.put(nb, "I2");
                }
            }
        }
        return labels;
    }

    // ===================== Algorithm: Tavern =====================

    private static TavernResult placeTavernAndPath(Map<String, Set<String>> adj, String porteKey, Random rng) {
        String[] pp = porteKey.split(",");
        int px = Integer.parseInt(pp[0]), py = Integer.parseInt(pp[1]);
        String parent = adj.get(porteKey).iterator().next();
        String[] pap = parent.split(",");
        int dx = px - Integer.parseInt(pap[0]), dy = py - Integer.parseInt(pap[1]);

        int maxLen = 2 + rng.nextInt(4); // 2 a 5
        int cx = px, cy = py;
        boolean lastStraight = false;
        List<int[]> pathCells = new ArrayList<>();

        for (int i = 0; i < maxLen; i++) {
            boolean goStraight = (i == 0) || (!lastStraight && rng.nextBoolean());
            int ndx = dx, ndy = dy;
            if (!goStraight) {
                int[][] perp = {{dy, -dx}, {-dy, dx}};
                int[] turn = perp[rng.nextInt(2)];
                ndx = turn[0]; ndy = turn[1];
                int tx = cx + ndx, ty = cy + ndy;
                if (adj.containsKey(tx + "," + ty) || tx < 0 || tx >= GRID_SIZE || ty < 0 || ty >= GRID_SIZE) {
                    ndx = perp[0][0] == ndx && perp[0][1] == ndy ? perp[1][0] : perp[0][0];
                    ndy = perp[0][0] == ndx && perp[0][1] == ndy ? perp[1][1] : perp[0][1];
                }
                dx = ndx; dy = ndy;
            }
            int nx = cx + ndx, ny = cy + ndy;
            if (adj.containsKey(nx + "," + ny) || nx < 0 || nx >= GRID_SIZE || ny < 0 || ny >= GRID_SIZE) break;
            pathCells.add(new int[]{nx, ny}); cx = nx; cy = ny;
            lastStraight = goStraight;
        }
        if (pathCells.size() < 2) return null;

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

    // ===================== Algorithm: analyzePart2 =====================

    private static Map<String, String> analyzePart2(Map<String, Set<String>> adj, String exitKey,
                                                      Map<String, String> labels, Set<String> pathSet, Random rng) {
        List<String> exitNb = new ArrayList<>(adj.get(exitKey));
        if (exitNb.size() == 2) {
            String[] p1 = exitNb.get(0).split(","), p2 = exitNb.get(1).split(",");
            if (p1[0].equals(p2[0]) || p1[1].equals(p2[1])) labels.put(exitKey, pickC(rng));
            else {
                List<String> nbs = new ArrayList<>(adj.get(exitKey));
                if (nbs.size() == 2) {
                    String[] pp1 = nbs.get(0).split(","), pp2 = nbs.get(1).split(",");
                    boolean droit = Integer.parseInt(pp1[0]) - Integer.parseInt(pp2[0]) == 0
                        || Integer.parseInt(pp1[1]) - Integer.parseInt(pp2[1]) == 0;
                    if (!droit) labels.put(exitKey, "I2");
                }
            }
        } else if (exitNb.size() == 1) labels.put(exitKey, "cul");
        else labels.put(exitKey, pickC(rng));

        Set<String> allLabeled = new HashSet<>(labels.keySet());
        List<String> p2Nodes = new ArrayList<>();
        Map<String, Integer> dist = new HashMap<>();
        Set<String> seen = new HashSet<>(); Queue<String> q = new LinkedList<>();
        q.add(exitKey); seen.add(exitKey); dist.put(exitKey, 0);
        while (!q.isEmpty()) { String n = q.poll(); if (!allLabeled.contains(n)) p2Nodes.add(n); for (String nb : adj.get(n)) { if (!seen.contains(nb)) { seen.add(nb); q.add(nb); dist.put(nb, dist.get(n) + 1); } } }

        List<String> leaves = new ArrayList<>(), internals = new ArrayList<>();
        for (String n : p2Nodes) { if (adj.get(n).size() == 1) leaves.add(n); else internals.add(n); }

        List<String> cList = new ArrayList<>(), i2List = new ArrayList<>(), i3List = new ArrayList<>(), i4List = new ArrayList<>();
        for (String node : internals) {
            int deg = adj.get(node).size();
            if (deg == 2) {
                List<String> nb = new ArrayList<>(adj.get(node));
                String[] p1 = nb.get(0).split(","), p2 = nb.get(1).split(",");
                if (p1[0].equals(p2[0]) || p1[1].equals(p2[1])) cList.add(node); else i2List.add(node);
            } else if (deg == 3) i3List.add(node);
            else if (deg == 4) i4List.add(node);
        }
        Collections.shuffle(cList, rng);

        // P2: 3 salles monstre (M3 obligatoire + M4 obligatoire + M1 ou M2 aleatoire)
        if (leaves.size() < 6) return null;
        long availCorr = cList.stream().filter(n -> !pathSet.contains(n)).count();
        if (availCorr < 2) return null;

        Set<String> monsterSet = new HashSet<>();

        // Ogre: feuille la plus eloignee de la taverne
        String ogreLeaf = null; int maxDist = -1;
        for (String n : leaves) { int d = dist.getOrDefault(n, 0); if (d > maxDist) { maxDist = d; ogreLeaf = n; } }
        labels.put(ogreLeaf, "Ogre"); monsterSet.add(ogreLeaf);
        List<String> remain = new ArrayList<>(leaves);
        remain.remove(ogreLeaf);
        Collections.shuffle(remain, rng);

        labels.put(remain.get(0), "fontaine");
        labels.put(remain.get(1), "porte2");

        // M3 sur une feuille, pas adjacente a Ogre
        String m3Leaf = null;
        for (String n : remain) {
            if (labels.containsKey(n)) continue;
            boolean hasAdj = false; for (String nb : adj.get(n)) if (monsterSet.contains(nb)) { hasAdj = true; break; }
            if (!hasAdj) { m3Leaf = n; break; }
        }
        if (m3Leaf == null) { for (String n : remain) { if (!labels.containsKey(n)) { m3Leaf = n; break; } } }
        labels.put(m3Leaf, "M3"); monsterSet.add(m3Leaf);

        // 1 seul Loot sur une feuille
        String lootLeaf = null;
        for (String n : remain) {
            if (labels.containsKey(n)) continue;
            boolean hasAdj = false; for (String nb : adj.get(n)) if (monsterSet.contains(nb)) { hasAdj = true; break; }
            if (!hasAdj) { lootLeaf = n; break; }
        }
        if (lootLeaf == null) { for (String n : remain) { if (!labels.containsKey(n)) { lootLeaf = n; break; } } }
        labels.put(lootLeaf, "Loot1");

        // Reste des feuilles en cul
        for (String n : remain) { if (!labels.containsKey(n)) labels.put(n, "cul"); }

        // M4 sur un couloir droit, pas adjacent a un monstre
        String m4Corr = null;
        for (String n : cList) {
            if (!pathSet.contains(n) && !labels.containsKey(n)) {
                boolean hasAdj = false; for (String nb : adj.get(n)) if (monsterSet.contains(nb)) { hasAdj = true; break; }
                if (!hasAdj) { m4Corr = n; break; }
            }
        }
        if (m4Corr == null) { for (String n : cList) { if (!pathSet.contains(n) && !labels.containsKey(n)) { m4Corr = n; break; } } }
        if (m4Corr != null) { labels.put(m4Corr, "M4"); monsterSet.add(m4Corr); }

        // M1 ou M2 aleatoire
        boolean useM1 = rng.nextBoolean();
        String extraMonster = null;
        if (useM1) {
            for (String n : remain) {
                if (!labels.containsKey(n)) {
                    boolean hasAdj = false; for (String nb : adj.get(n)) if (monsterSet.contains(nb)) { hasAdj = true; break; }
                    if (!hasAdj) { extraMonster = n; break; }
                }
            }
            if (extraMonster == null) { for (String n : remain) { if (!labels.containsKey(n)) { extraMonster = n; break; } } }
            if (extraMonster != null) labels.put(extraMonster, "M1");
        } else {
            for (String n : cList) {
                if (n.equals(m4Corr) || pathSet.contains(n) || labels.containsKey(n)) continue;
                boolean hasAdj = false; for (String nb : adj.get(n)) if (monsterSet.contains(nb)) { hasAdj = true; break; }
                if (!hasAdj) { extraMonster = n; break; }
            }
            if (extraMonster == null) { for (String n : cList) { if (!pathSet.contains(n) && !labels.containsKey(n)) { extraMonster = n; break; } } }
            if (extraMonster != null) labels.put(extraMonster, "M2");
        }

        // Puit sur un couloir droit restant, pas adjacent a une salle speciale
        List<String> remC = new ArrayList<>();
        for (String n : cList) { if (!labels.containsKey(n)) remC.add(n); }
        if (!remC.isEmpty()) {
            String pn = null;
            for (String n : remC) {
                if (!pathSet.contains(n)) {
                    boolean adjSpecial = false;
                    for (String nb : adj.get(n)) {
                        String lbl = labels.get(nb);
                        if (lbl != null && (lbl.equals("Ogre") || lbl.equals("fontaine") || lbl.equals("Loot1")
                            || lbl.equals("M1") || lbl.equals("M2") || lbl.equals("M3") || lbl.equals("M4"))) {
                            adjSpecial = true; break;
                        }
                    }
                    if (!adjSpecial) { pn = n; break; }
                }
            }
            if (pn == null) { for (String n : remC) { if (!pathSet.contains(n)) { pn = n; break; } } }
            if (pn == null) pn = remC.get(0);
            labels.put(pn, "puit");
            remC.remove(pn);
        }
        // P2: puit/M2/M4 ne peuvent pas avoir de couloir a cote
        Set<String> toI2 = new HashSet<>();
        for (String n : remC) {
            if (labels.containsKey(n)) continue;
            for (String nb : adj.get(n)) {
                String lbl = labels.get(nb);
                if (lbl != null && (lbl.equals("M2") || lbl.equals("M4") || lbl.equals("puit"))) {
                    toI2.add(n); break;
                }
            }
        }
        // P2: max 1 couloir consecutif (comme P1)
        Map<String, String> remCDirs = new HashMap<>();
        for (String n : remC) {
            if (toI2.contains(n)) continue;
            List<String> nb = new ArrayList<>(adj.get(n));
            int dx = Integer.parseInt(nb.get(1).split(",")[0]) - Integer.parseInt(nb.get(0).split(",")[0]);
            int dz = Integer.parseInt(nb.get(1).split(",")[1]) - Integer.parseInt(nb.get(0).split(",")[1]);
            if (dx < 0 || (dx == 0 && dz < 0)) { dx = -dx; dz = -dz; }
            remCDirs.put(n, dx + "," + dz);
        }
        Set<String> cSkip = new HashSet<>();
        for (String n : remC) {
            if (cSkip.contains(n) || labels.containsKey(n) || toI2.contains(n)) continue;
            String dir = remCDirs.get(n);
            for (String nb : adj.get(n)) {
                if (!remC.contains(nb) || cSkip.contains(nb) || labels.containsKey(nb) || toI2.contains(nb)) continue;
                if (dir.equals(remCDirs.get(nb))) { cSkip.add(nb); break; }
            }
        }
        for (String n : remC) {
            if (toI2.contains(n)) {
                List<String> nbs = new ArrayList<>(adj.get(n));
                if (nbs.size() == 2) {
                    String[] p1 = nbs.get(0).split(","), p2 = nbs.get(1).split(",");
                    boolean droit = Integer.parseInt(p1[0]) - Integer.parseInt(p2[0]) == 0
                        || Integer.parseInt(p1[1]) - Integer.parseInt(p2[1]) == 0;
                    if (!droit) labels.put(n, "I2");
                }
            } else if (labels.containsKey(n)) continue;
            else labels.put(n, pickC(rng));
        }
        for (String n : i2List) labels.put(n, "I2");
        for (String n : i3List) labels.put(n, "I3");
        for (String n : i4List) labels.put(n, "I4");
        // Apres une I4, les voisins couloirs perpendiculaires deviennent I2
        for (String n : i4List) {
            String[] ip = n.split(",");
            int ix = Integer.parseInt(ip[0]), iz = Integer.parseInt(ip[1]);
            for (String nb : adj.get(n)) {
                String lbl = labels.get(nb);
                if (lbl == null || !(lbl.equals("C1") || lbl.equals("C2") || lbl.equals("C3"))) continue;
                String[] np = nb.split(",");
                int nx = Integer.parseInt(np[0]), nz = Integer.parseInt(np[1]);
                List<String> nAdj = new ArrayList<>(adj.get(nb));
                if (nAdj.size() != 2) continue;
                String[] a0 = nAdj.get(0).split(","), a1 = nAdj.get(1).split(",");
                int adx = Integer.parseInt(a1[0]) - Integer.parseInt(a0[0]);
                int adz = Integer.parseInt(a1[1]) - Integer.parseInt(a0[1]);
                if (adx * (nx - ix) + adz * (nz - iz) == 0) {
                    boolean droit = adx == 0 || adz == 0;
                    if (!droit) labels.put(nb, "I2");
                }
            }
        }
        return labels;
    }

    // ===================== Algorithm: Camp =====================

    private static CampResult placeCampAndPath(Map<String, Set<String>> adj, String porte2Key, Random rng) {
        String[] pp = porte2Key.split(",");
        int px = Integer.parseInt(pp[0]), py = Integer.parseInt(pp[1]);
        String parent = adj.get(porte2Key).iterator().next();
        String[] pap = parent.split(",");
        int dx = px - Integer.parseInt(pap[0]), dy = py - Integer.parseInt(pap[1]);
        int maxLen = 2 + rng.nextInt(4);
        int cx = px, cy = py;
        boolean lastStraight = false;
        List<int[]> pathCells = new ArrayList<>();

        for (int i = 0; i < maxLen; i++) {
            boolean goStraight = (i == 0) || (!lastStraight && rng.nextBoolean());
            int ndx = dx, ndy = dy;
            if (!goStraight) {
                int[][] perp = {{dy, -dx}, {-dy, dx}};
                int[] turn = perp[rng.nextInt(2)];
                ndx = turn[0]; ndy = turn[1];
                int tx = cx + ndx, ty = cy + ndy;
                if (adj.containsKey(tx+","+ty) || tx < 0 || tx >= GRID_SIZE || ty < 0 || ty >= GRID_SIZE) {
                    ndx = perp[0][0] == ndx && perp[0][1] == ndy ? perp[1][0] : perp[0][0];
                    ndy = perp[0][0] == ndx && perp[0][1] == ndy ? perp[1][1] : perp[0][1];
                }
                dx = ndx; dy = ndy;
            }
            int nx = cx + ndx, ny = cy + ndy;
            if (adj.containsKey(nx+","+ny) || nx < 0 || nx >= GRID_SIZE || ny < 0 || ny >= GRID_SIZE) break;
            pathCells.add(new int[]{nx, ny}); cx = nx; cy = ny;
            lastStraight = goStraight;
        }
        if (pathCells.size() < 2) return null;

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
            String ck = cell[0]+","+cell[1]; campPathSet.add(ck);
            adj.put(ck, new HashSet<>()); adj.get(ck).add(cur); adj.get(cur).add(ck); cur = ck;
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

    // ===================== Algorithm: analyzePart3 =====================

    private static Map<String, String> analyzePart3(Map<String, Set<String>> adj, String campExit,
                                                      Map<String, String> labels, Random rng) {
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

        Set<String> allNodes = new HashSet<>(adj.keySet());
        List<String> leaves = new ArrayList<>(), internals = new ArrayList<>();
        for (String n : allNodes) { if (adj.get(n).size() == 1 && !labels.containsKey(n)) leaves.add(n); else if (adj.get(n).size() >= 2 && !labels.containsKey(n)) internals.add(n); }

        List<String> cjList = new ArrayList<>(), ij2List = new ArrayList<>(), ij3List = new ArrayList<>(), ij4List = new ArrayList<>();
        for (String node : internals) {
            int deg = adj.get(node).size();
            if (deg == 2) {
                List<String> nb = new ArrayList<>(adj.get(node));
                String[] p1 = nb.get(0).split(","), p2 = nb.get(1).split(",");
                if (p1[0].equals(p2[0]) || p1[1].equals(p2[1])) cjList.add(node); else ij2List.add(node);
            } else if (deg == 3) ij3List.add(node);
            else if (deg == 4) ij4List.add(node);
        }

        labels.put(bibNodes[0], "Bib1"); labels.put(bibNodes[1], "Bib2");
        // Chemin Bib2 -> Centrale (type Python)
        String bib2k = bibNodes[1], bib1k = bibNodes[0];
        String[] bp = bib2k.split(",");
        int bx = Integer.parseInt(bp[0]), bz = Integer.parseInt(bp[1]);
        String[] b1p = bib1k.split(",");
        int ddx = bx - Integer.parseInt(b1p[0]), ddz = bz - Integer.parseInt(b1p[1]);
        int cdx, cdz;
        if (ddx == 1 && ddz == 0) { cdx = 0; cdz = 1; }      // Est → Sud
        else if (ddx == -1 && ddz == 0) { cdx = 0; cdz = -1; } // Ouest → Nord
        else if (ddx == 0 && ddz == 1) { cdx = -1; cdz = 0; }  // Sud → Ouest
        else if (ddx == 0 && ddz == -1) { cdx = 1; cdz = 0; }  // Nord → Est
        else { cdx = 0; cdz = 1; }
        // Chemin Bib2 -> Centrale (direct, sans hub)
        boolean hubOk = false;
        for (int corridorLen : new int[]{5, 6, 7}) {
            if (hubOk) break;
            int cx = bx, cz = bz;
            String prev = bib2k;
            List<String> corrNodes = new ArrayList<>();
            boolean ok = true;
            int mid = corridorLen / 2;
            for (int i = 0; i < corridorLen && ok; i++) {
                if (i == mid) {
                    int[][] perp = {{ddx, ddz}, {-ddx, -ddz}};
                    int ti = rng.nextInt(2);
                    int tx = cx + perp[ti][0], ty = cz + perp[ti][1];
                    if (adj.containsKey(tx+","+ty) || tx < 0 || tx >= GRID_SIZE || ty < 0 || ty >= GRID_SIZE) { ti = ti==0?1:0; tx = cx + perp[ti][0]; ty = cz + perp[ti][1]; }
                    if (!adj.containsKey(tx+","+ty) && tx >= 0 && tx < GRID_SIZE && ty >= 0 && ty < GRID_SIZE) {
                        corrNodes.add(tx+","+ty); adj.put(tx+","+ty, new HashSet<>());
                        adj.get(tx+","+ty).add(prev); adj.get(prev).add(tx+","+ty); cx = tx; cz = ty; prev = tx+","+ty;
                    } else { ok = false; break; }
                }
                int nx = cx + cdx, ny = cz + cdz;
                if (adj.containsKey(nx+","+ny) || nx < 0 || nx >= GRID_SIZE || ny < 0 || ny >= GRID_SIZE) { ok = false; break; }
                corrNodes.add(nx+","+ny); adj.put(nx+","+ny, new HashSet<>());
                adj.get(nx+","+ny).add(prev); adj.get(prev).add(nx+","+ny); cx = nx; cz = ny; prev = nx+","+ny;
            }
            if (!ok || corrNodes.size() < 3) { for (String n : corrNodes) { adj.get(n).clear(); adj.remove(n); } continue; }
            // Ajouter un virage vers l'est pour aligner avec l'entree (x=11..18)
            int wX = cx + 1, wZ = cz;
            String wKey = wX + "," + wZ;
            if (adj.containsKey(wKey) || wX >= GRID_SIZE) { for (String n : corrNodes) { adj.get(n).clear(); adj.remove(n); } continue; }
            adj.put(wKey, new HashSet<>()); adj.get(wKey).add(prev); adj.get(prev).add(wKey);
            corrNodes.add(wKey);
            // Hub a (hx2, hz2) avec connexion alignee sur l'entree
            int hx2 = cx, hz2 = cz + 1;
            if (hz2 + 1 >= GRID_SIZE) { for (String n : corrNodes) { adj.get(n).clear(); adj.remove(n); } continue; }
            String[] hubCells = {hx2+","+hz2, (hx2+1)+","+hz2, hx2+","+(hz2+1), (hx2+1)+","+(hz2+1)};
            boolean hubFree = true;
            for (String c : hubCells) { if (adj.containsKey(c) || hx2+1 >= GRID_SIZE) { hubFree = false; break; } }
            if (!hubFree) { for (String n : corrNodes) { adj.get(n).clear(); adj.remove(n); } continue; }
            // Connecter le hub
            String hubKey = hx2 + "," + hz2;
            adj.put(hubKey, new HashSet<>()); adj.get(hubKey).add(wKey); adj.get(wKey).add(hubKey);
            // Etiqueter le corridor (maintenant la derniere cellule wKey a 2 voisins)
            for (String n : corrNodes) {
                List<String> nb = new ArrayList<>(adj.get(n));
                if (nb.size() == 2) {
                    String[] na = nb.get(0).split(","), nb2 = nb.get(1).split(",");
                    String[] kp = n.split(",");
                    int kx = Integer.parseInt(kp[0]), kz = Integer.parseInt(kp[1]);
                    boolean coll = (kx - Integer.parseInt(na[0])) == (Integer.parseInt(nb2[0]) - kx) && (kz - Integer.parseInt(na[1])) == (Integer.parseInt(nb2[1]) - kz);
                    labels.put(n, coll ? pickC(rng) : "I2");
                }
            }
            labels.put(hubKey, "Centrale");
            // P4 + hub sur l'etage 1
            Map<String, String> topLbls = new HashMap<>();
            topLbls.put(hubKey, "Centrale");
            int[] exOff = {-1, 2, 1, 1, 0};
            int[] ezOff = {0, 0, -1, 2, 2};
            for (int ei = 0; ei < 5; ei++) {
                int ex = hx2 + exOff[ei], ez = hz2 + ezOff[ei];
                String ek = ex + "," + ez;
                if (ex >= 0 && ex < GRID_SIZE && ez >= 0 && ez < GRID_SIZE) {
                    topLbls.put(ek, pickCJ(rng));
                }
            }
            lastTopLabels = topLbls;
            hubOk = true;
        }
        if (!hubOk) { bibNodes = null; return null; }
        cjList.remove(bibNodes[0]);
        if (shopNode != null) { labels.put(shopNode, "Shop"); leaves.remove(shopNode); }
        leaves.remove(bibNodes[1]); leaves.remove(bibNodes[0]);

        // P3: 2 loot obligatoires (types aleatoires parmi les 3, toujours differents)
        String[] lootTypes = {"Lootdj1", "Lootdj2", "Lootdj3"};
        List<String> p3LootList = new ArrayList<>(Arrays.asList(lootTypes));
        Collections.shuffle(p3LootList, rng);
        String p3LootA = p3LootList.get(0), p3LootB = p3LootList.get(1);
        int leafLoot, corrLoot;
        if (p3LootA.equals("Lootdj2") || p3LootB.equals("Lootdj2")) {
            leafLoot = 1; corrLoot = 1; // Lootdj2 sur couloir, l'autre sur feuille
        } else {
            leafLoot = 2; corrLoot = 0; // Lootdj1+Lootdj3 sur feuilles
        }
        // P3: 4 a 6 salles monstre (MJ1 a MJ5 aleatoire)
        int targetM3 = 4 + rng.nextInt(3);
        int availLeafM = Math.max(0, leaves.size() - leafLoot - 2); // leafLoot + Jardin + Statue
        if (availLeafM + cjList.size() < targetM3 || leaves.size() < leafLoot + 2) return null;
        int leafM3 = Math.min(targetM3, availLeafM);
        int corrM3 = targetM3 - leafM3;
        if (corrM3 > cjList.size()) { corrM3 = cjList.size(); leafM3 = targetM3 - corrM3; }

        Collections.shuffle(leaves, rng);
        int li = leafLoot;
        // Placer les 2 loots P3 avec les types choisis aleatoirement
        java.util.Map<String, String> p3LootAssign = new HashMap<>();
        if (leafLoot == 2) {
            p3LootAssign.put(leaves.get(0), p3LootA);
            p3LootAssign.put(leaves.get(1), p3LootB);
        } else { // leafLoot == 1
            String leafType = p3LootA.equals("Lootdj2") ? p3LootB : p3LootA;
            p3LootAssign.put(leaves.get(0), leafType);
        }
        for (var e : p3LootAssign.entrySet()) labels.put(e.getKey(), e.getValue());
        String[] leafMTypes = {"MJ1", "MJ3", "MJ5"};
        for (int i = 0; i < leafM3 && li < leaves.size(); i++) { labels.put(leaves.get(li), leafMTypes[rng.nextInt(3)]); li++; }
        // Jardin + Statue: 2 feuilles les plus eloignees du campExit
        String[] cp = campExit.split(",");
        int cex = Integer.parseInt(cp[0]), cez = Integer.parseInt(cp[1]);
        List<String> sortedLeaves = new ArrayList<>();
        for (int i = li; i < leaves.size(); i++) sortedLeaves.add(leaves.get(i));
        sortedLeaves.sort(Comparator.comparingInt(n -> {
            String[] lp = n.split(",");
            return -Math.abs(Integer.parseInt(lp[0]) - cex) - Math.abs(Integer.parseInt(lp[1]) - cez);
        }));
        for (int i = li; i < leaves.size(); i++) {
            String n = leaves.get(i);
            labels.put(n, n.equals(sortedLeaves.get(0)) ? "Jardin" : (sortedLeaves.size() > 1 && n.equals(sortedLeaves.get(1)) ? "Statue" : "culDJ"));
        }

        if (cjList.size() < corrM3 + corrLoot + 1) return null;
        Collections.shuffle(cjList, rng);
        Set<String> monsterSet = new HashSet<>();
        String[] corrMTypes = {"MJ2", "MJ4"};
        int mjPlaced = 0; List<String> remCJ = new ArrayList<>();
        for (String n : cjList) {
            if (mjPlaced < corrM3) {
                boolean adjM = adj.get(n).stream().anyMatch(nb -> monsterSet.contains(nb) || labels.getOrDefault(nb, "").startsWith("MJ"));
                if (!adjM) { labels.put(n, corrMTypes[rng.nextInt(2)]); monsterSet.add(n); mjPlaced++; } else remCJ.add(n);
            } else remCJ.add(n);
        }
        int lc = 0;
        if (corrLoot == 1 && lc < remCJ.size()) {
            String corrType = p3LootA.equals("Lootdj2") ? p3LootA : p3LootB;
            labels.put(remCJ.get(lc), corrType); lc++;
        }
        if (remCJ.size() > lc) labels.put(remCJ.get(lc), "PuitDJ"); else lc--;
        for (int i = lc + 1; i < remCJ.size(); i++) labels.put(remCJ.get(i), pickCJ(rng));
        for (String n : ij2List) labels.put(n, "IJ2");
        for (String n : ij3List) labels.put(n, "IJ3");
        for (String n : ij4List) labels.put(n, "IJ4");
        for (String n : ij4List) {
            String[] ip = n.split(",");
            int ix = Integer.parseInt(ip[0]), iz = Integer.parseInt(ip[1]);
            for (String nb : adj.get(n)) {
                String lbl = labels.get(nb);
                if (lbl == null || !(lbl.equals("CJ1") || lbl.equals("CJ2") || lbl.equals("CJ3"))) continue;
                String[] np = nb.split(",");
                int nx = Integer.parseInt(np[0]), nz = Integer.parseInt(np[1]);
                List<String> nAdj = new ArrayList<>(adj.get(nb));
                if (nAdj.size() != 2) continue;
                String[] a0 = nAdj.get(0).split(","), a1 = nAdj.get(1).split(",");
                int adx = Integer.parseInt(a1[0]) - Integer.parseInt(a0[0]);
                int adz = Integer.parseInt(a1[1]) - Integer.parseInt(a0[1]);
                if (adx * (nx - ix) + adz * (nz - iz) == 0) {
                    boolean droit = adx == 0 || adz == 0;
                    if (!droit) labels.put(nb, "IJ2");
                }
            }
        }
        return labels;
    }

    // ===================== Part 4: 5 P4 trees (Etage 1, un par sortie du hub) =====================

    private static final int P4_TREE_TARGET = 15;
    private static final int P4_MAX_IJ3 = 2;
    private static final int P4_MAX_IJ4 = 1;

    private static boolean generatePart4Tree(Map<String, Set<String>> adj,
                                              Map<String, String> topLabels,
                                              int hx, int hz, String missingLootType, Random rng) {
        String[][] p4Info = {
            {hx + "," + (hz+2),       "0,1"},
            {(hx+1) + "," + (hz+2),   "0,1"},
            {(hx-1) + "," + hz,       "-1,0"},
            {(hx+2) + "," + hz,       "1,0"},
            {(hx+1) + "," + (hz-1),   "0,-1"}
        };
        List<String[]> p4List = new ArrayList<>(Arrays.asList(p4Info));
        Collections.shuffle(p4List, rng);
        p4Info = p4List.toArray(new String[0][]);
        Set<String> globalOccupied = new HashSet<>();
        for (int x = hx; x <= hx + 1; x++) for (int z = hz; z <= hz + 1; z++) globalOccupied.add(x + "," + z);
        // Ajouter les 4 cellules du hub dans adj
        for (int hx2 = hx; hx2 <= hx + 1; hx2++) for (int hz2 = hz; hz2 <= hz + 1; hz2++) adj.putIfAbsent(hx2 + "," + hz2, new HashSet<>());
        // 5 CJ fixes pour les 5 sorties, en determinant direction et hub par les coordonnees
        List<String> cjKeys = new ArrayList<>(), exitKeys = new ArrayList<>();
        List<int[]> cjDirs = new ArrayList<>();
        for (int ei = 0; ei < 5; ei++) {
            String ek = p4Info[ei][0];
            int[] pp = {Integer.parseInt(ek.split(",")[0]), Integer.parseInt(ek.split(",")[1])};
            // Determiner la direction d'eloignement et le hub selon la position
            int adx = 0, ady = 0; String hk = null;
            if (pp[0] == hx && pp[1] == hz+2) { adx = 0; ady = 1; hk = hx+","+(hz+1); }       // SUD-left
            else if (pp[0] == hx+1 && pp[1] == hz+2) { adx = 0; ady = 1; hk = (hx+1)+","+(hz+1); } // SUD-right
            else if (pp[0] == hx-1 && pp[1] == hz) { adx = -1; ady = 0; hk = hx+","+hz; }         // OUEST
            else if (pp[0] == hx+2 && pp[1] == hz) { adx = 1; ady = 0; hk = (hx+1)+","+hz; }      // EST
            else if (pp[0] == hx+1 && pp[1] == hz-1) { adx = 0; ady = -1; hk = (hx+1)+","+hz; }   // NORD
            if (hk == null) continue;
            int cjx = pp[0] + adx, cjz = pp[1] + ady;
            String cjk = cjx + "," + cjz;
            if (cjx >= 0 && cjx < GRID_SIZE && cjz >= 0 && cjz < GRID_SIZE && !globalOccupied.contains(cjk)) {
                adj.putIfAbsent(ek, new HashSet<>()); adj.putIfAbsent(cjk, new HashSet<>());
                adj.get(ek).add(hk); adj.get(hk).add(ek);
                adj.get(ek).add(cjk); adj.get(cjk).add(ek);
                globalOccupied.add(ek); globalOccupied.add(cjk);
                cjKeys.add(cjk); cjDirs.add(new int[]{adx, ady}); exitKeys.add(ek);
            }
        }
        // Faire partir les arbres des CJ
        List<Map<String, Set<String>>> allTrees = new ArrayList<>();
        List<String> allStarts = new ArrayList<>();
        for (int ti = 0; ti < 5 && ti < cjKeys.size(); ti++) {
            String startKey = cjKeys.get(ti);
            int adx = cjDirs.get(ti)[0], ady = cjDirs.get(ti)[1];
            Map<String, Set<String>> tr = new HashMap<>();
            tr.put(startKey, new HashSet<>());
            globalOccupied.add(startKey);
            int ci3 = 0, ci4 = 0, target = 13 + rng.nextInt(5);
            // Pas forces : 2 cellules dans la direction d'eloignement
            String[] skp = startKey.split(",");
            int skx = Integer.parseInt(skp[0]), sky = Integer.parseInt(skp[1]);
            int f1x = skx + adx, f1y = sky + ady;
            String f1k = f1x + "," + f1y;
            if (f1x >= 0 && f1x < GRID_SIZE && f1y >= 0 && f1y < GRID_SIZE) {
                if (!globalOccupied.contains(f1k)) globalOccupied.add(f1k);
                tr.put(f1k, new HashSet<>());
                tr.get(startKey).add(f1k); tr.get(f1k).add(startKey);
                int f2x = f1x + adx, f2y = f1y + ady;
                String f2k = f2x + "," + f2y;
                if (f2x >= 0 && f2x < GRID_SIZE && f2y >= 0 && f2y < GRID_SIZE) {
                    if (!globalOccupied.contains(f2k)) globalOccupied.add(f2k);
                    tr.put(f2k, new HashSet<>());
                    tr.get(f1k).add(f2k); tr.get(f2k).add(f1k);
                }
            }
            while (tr.size() < target) {
                List<String[]> cands = new ArrayList<>();
                for (String node : tr.keySet()) {
                    int d = tr.get(node).size(); if (d >= 4) continue;
                    if (d == 2 && ci3 >= P4_MAX_IJ3) continue;
                    if (d == 3 && ci4 >= P4_MAX_IJ4) continue;
                    String[] pp = node.split(",");
                    int px = Integer.parseInt(pp[0]), py = Integer.parseInt(pp[1]);
                    for (int[] dir : DIR_OFFSET) {
                        int nx = px + dir[0], ny = py + dir[1];
                        if (nx < 0 || nx >= GRID_SIZE || ny < 0 || ny >= GRID_SIZE) continue;
                        String nk = nx + "," + ny;
                        if (!globalOccupied.contains(nk)) {
                            if (d >= 2) {
                                boolean sk = false;
                                for (String n1 : tr.get(node)) {
                                    String[] m1 = n1.split(",");
                                    int mx = Integer.parseInt(m1[0]), mz = Integer.parseInt(m1[1]);
                                    for (String n2 : tr.get(node)) {
                                        if (n1.equals(n2)) continue;
                                        String[] m2 = n2.split(",");
                                        if (px - mx == Integer.parseInt(m2[0]) - px && py - mz == Integer.parseInt(m2[1]) - py) {
                                            Set<String> a1 = tr.get(n1), a2 = tr.get(n2);
                                            if ((a1 != null && a1.size() >= 3) || (a2 != null && a2.size() >= 3)) { sk = true; break; }
                                        }
                                    }
                                    if (sk) break;
                                }
                                if (sk) continue;
                            }
                            if (d == 1) {
                                String on = tr.get(node).iterator().next();
                                String[] op = on.split(",");
                                if (px - Integer.parseInt(op[0]) == Integer.parseInt(nk.split(",")[0]) - px
                                    && py - Integer.parseInt(op[1]) == Integer.parseInt(nk.split(",")[1]) - py) {
                                    int bx = Integer.parseInt(op[0]) - (px - Integer.parseInt(op[0]));
                                    String bk = bx + "," + (Integer.parseInt(op[1]) - (py - Integer.parseInt(op[1])));
                                    if (tr.containsKey(bk) && tr.get(bk).contains(on)) continue;
                                }
                            }
                            cands.add(new String[]{node, nk, dir[0] + "," + dir[1]});
                        }
                    }
                }
                if (cands.isEmpty()) break;
                List<String[]> w = new ArrayList<>();
                for (String[] c : cands) {
                    String[] cd = c[2].split(",");
                    int wt = (Integer.parseInt(cd[0]) == adx && Integer.parseInt(cd[1]) == ady) ? 20 : 1;
                    for (int i = 0; i < wt; i++) w.add(c);
                }
                if (w.isEmpty()) break;
                String[] ch = w.get(rng.nextInt(w.size()));
                globalOccupied.add(ch[1]); tr.put(ch[1], new HashSet<>());
                tr.get(ch[0]).add(ch[1]); tr.get(ch[1]).add(ch[0]);
                int nd = tr.get(ch[0]).size(); if (nd == 3) ci3++; else if (nd == 4) ci4++;
            }
            if (tr.size() < 6) continue;
            allTrees.add(tr); allStarts.add(startKey);
            // Labelisation
            List<String> lf = new ArrayList<>(), co = new ArrayList<>(), i3 = new ArrayList<>(), i4 = new ArrayList<>();
            for (String k : tr.keySet()) {
                int d = tr.get(k).size();
                if (d == 1) lf.add(k); else if (d == 2) co.add(k); else if (d == 3) i3.add(k); else if (d == 4) i4.add(k);
            }
            // Labeliser startKey selon son degre et alignement
            int skDeg = tr.get(startKey).size();
            if (skDeg == 1) topLabels.put(startKey, pickCJ(rng));
            else if (skDeg == 2) {
                List<String> nb = new ArrayList<>(tr.get(startKey));
                String[] p1 = nb.get(0).split(","), p2 = nb.get(1).split(",");
                topLabels.put(startKey, (Integer.parseInt(p1[0]) - Integer.parseInt(p2[0]) == 0 || Integer.parseInt(p1[1]) - Integer.parseInt(p2[1]) == 0) ? pickCJ(rng) : "IJ2");
            } else if (skDeg == 3) topLabels.put(startKey, "IJ3");
            else if (skDeg == 4) topLabels.put(startKey, "IJ4");
            for (String k : i3) if (!topLabels.containsKey(k)) topLabels.put(k, "IJ3");
            for (String k : i4) if (!topLabels.containsKey(k)) topLabels.put(k, "IJ4");
            for (String k : co) {
                if (topLabels.containsKey(k)) continue;
                List<String> nb = new ArrayList<>(tr.get(k));
                if (nb.size() == 2) {
                    String[] p1 = nb.get(0).split(","), p2 = nb.get(1).split(",");
                    topLabels.put(k, (Integer.parseInt(p1[0]) - Integer.parseInt(p2[0]) == 0 || Integer.parseInt(p1[1]) - Integer.parseInt(p2[1]) == 0) ? pickCJ(rng) : "IJ2");
                }
            }
            for (String k : lf) if (!topLabels.containsKey(k)) topLabels.put(k, "culDJ");
            for (String k : i4) {
                String[] ip = k.split(","); int ix = Integer.parseInt(ip[0]), iz = Integer.parseInt(ip[1]);
                for (String nb : tr.get(k)) {
                    String lbl = topLabels.get(nb);
                    if (lbl == null || !(lbl.equals("CJ1") || lbl.equals("CJ2") || lbl.equals("CJ3"))) continue;
                    String[] np = nb.split(","); int nx = Integer.parseInt(np[0]), nz = Integer.parseInt(np[1]);
                    List<String> nAd = new ArrayList<>(tr.get(nb));
                    if (nAd.size() != 2) continue;
                    String[] a0 = nAd.get(0).split(","), a1 = nAd.get(1).split(",");
                    if ((Integer.parseInt(a1[0]) - Integer.parseInt(a0[0])) * (nx - ix) + (Integer.parseInt(a1[1]) - Integer.parseInt(a0[1])) * (nz - iz) == 0) topLabels.put(nb, "IJ2");
                }
            }
            for (var e : tr.entrySet()) {
                adj.putIfAbsent(e.getKey(), new HashSet<>());
                adj.get(e.getKey()).addAll(e.getValue());
            }
        }
        // Labeliser les 5 sorties selon leur adjacence reelle dans adj
        for (String ek : exitKeys) {
            if (topLabels.containsKey(ek)) continue;
            Set<String> skn = adj.get(ek);
            if (skn == null) continue;
            int deg = skn.size();
            if (deg == 1) topLabels.put(ek, "culDJ");
            else if (deg == 2) {
                List<String> nb = new ArrayList<>(skn);
                String[] p1 = nb.get(0).split(","), p2 = nb.get(1).split(",");
                boolean straight = Integer.parseInt(p1[0]) - Integer.parseInt(p2[0]) == 0
                    || Integer.parseInt(p1[1]) - Integer.parseInt(p2[1]) == 0;
                topLabels.put(ek, straight ? pickCJ(rng) : "IJ2");
            } else if (deg == 3) topLabels.put(ek, "IJ3");
            else if (deg == 4) topLabels.put(ek, "IJ4");
        }
        // Labeliser les CJ keys (startKeys des arbres) selon leur adjacence dans adj (apres fusion hub+tree)
        for (String cjk : cjKeys) {
            Set<String> cn = adj.get(cjk);
            if (cn == null) continue;
            int deg = cn.size();
            if (deg == 1) topLabels.put(cjk, "culDJ");
            else if (deg == 2) {
                List<String> nb = new ArrayList<>(cn);
                String[] p1 = nb.get(0).split(","), p2 = nb.get(1).split(",");
                boolean straight = Integer.parseInt(p1[0]) - Integer.parseInt(p2[0]) == 0
                    || Integer.parseInt(p1[1]) - Integer.parseInt(p2[1]) == 0;
                topLabels.put(cjk, straight ? pickCJ(rng) : "IJ2");
            } else if (deg == 3) topLabels.put(cjk, "IJ3");
            else if (deg == 4) topLabels.put(cjk, "IJ4");
        }
        if (allTrees.size() < 5) return false;
        // Assigner roles: gobelin, chapelle, prison sur 3 arbres distincts
        List<Integer> idxs = new ArrayList<>(Arrays.asList(0,1,2,3,4));
        Collections.shuffle(idxs, rng);
        int gIdx = idxs.get(0), cIdx = idxs.get(1), pIdx = idxs.get(2);
        Set<String> goblinCells = new HashSet<>();
        // Gobelin sur une feuille
        Map<String, Set<String>> gt = allTrees.get(gIdx);
        String gLeaf = null; Map<String, Set<String>> ga = null;
        String firstGob = null; String parent = null;
        // Essayer chaque culDJ jusqu'a en trouver un qui permet une ligne droite
        for (String testLeaf : new ArrayList<>(gt.keySet())) {
            if (topLabels.get(testLeaf) == null || !topLabels.get(testLeaf).equals("culDJ")) continue;
            parent = gt.get(testLeaf).iterator().next();
            String[] tp = parent.split(",");
            String[] lp = testLeaf.split(",");
            int tlx = Integer.parseInt(lp[0]), tly = Integer.parseInt(lp[1]);
            int gdx = tlx - Integer.parseInt(tp[0]), gdy = tly - Integer.parseInt(tp[1]);
            int[][] tryDirs = {{gdx, gdy}};
            firstGob = null;
            for (int[] td : tryDirs) {
                int tx = tlx + td[0], ty = tly + td[1];
                if (tx >= 0 && tx < GRID_SIZE && ty >= 0 && ty < GRID_SIZE && !globalOccupied.contains(tx+","+ty)) {
                    firstGob = tx+","+ty; break;
                }
            }
            if (firstGob != null) { gLeaf = testLeaf; break; }
        }
        if (gLeaf != null && firstGob != null) {
            topLabels.put(gLeaf, "PorteGob");
            ga = new HashMap<>();
            ga.put(gLeaf, new HashSet<>());
            globalOccupied.add(firstGob); ga.put(firstGob, new HashSet<>());
            ga.get(gLeaf).add(firstGob); ga.get(firstGob).add(gLeaf);
            Integer[] gobStart = {Integer.parseInt(firstGob.split(",")[0]), Integer.parseInt(firstGob.split(",")[1])};
            TreeResult gobRaw = generateRawTree(20, 25, 3, 1, 2, gobStart, new HashSet<>(globalOccupied));
            if (gobRaw.adj.size() < 6) { gLeaf = null; firstGob = null; ga = null; }
            if (gLeaf != null) for (var e : gobRaw.adj.entrySet()) {
                ga.putIfAbsent(e.getKey(), new HashSet<>());
                ga.get(e.getKey()).addAll(e.getValue());
            }
                // Forcer PorteGob a max 2 voisins (parent + 1 tree)
                if (ga.get(gLeaf).size() > 2) {
                    String keepNb = null;
                    for (String nb : ga.get(gLeaf)) { if (!nb.equals(parent)) { keepNb = nb; break; } }
                    for (String nb : new ArrayList<>(ga.get(gLeaf))) {
                        if (!nb.equals(parent) && !nb.equals(keepNb)) { ga.get(nb).remove(gLeaf); ga.get(gLeaf).remove(nb); }
                    }
                }
                for (String nk : ga.keySet()) globalOccupied.add(nk);
                // Labeliser: deg 1 = CDG, deg 2 droit = CG1, deg 2 virage = GI2, deg 3 = GI3, deg 4 = GI4
                List<String> gl = new ArrayList<>(), gco = new ArrayList<>(), gi3 = new ArrayList<>(), gi4 = new ArrayList<>();
                for (String k : ga.keySet()) {
                    if (k.equals(gLeaf)) continue;
                    int d = ga.get(k).size();
                    if (d == 1) gl.add(k); else if (d == 2) gco.add(k); else if (d == 3) gi3.add(k); else if (d == 4) gi4.add(k);
                }
                for (String k : gi3) if (!topLabels.containsKey(k)) topLabels.put(k, "GI3");
                for (String k : gi4) if (!topLabels.containsKey(k)) topLabels.put(k, "GI4");
                // PuitG sur un couloir droit
                for (String k : gco) {
                    List<String> nb2 = new ArrayList<>(ga.get(k));
                    String[] p1 = nb2.get(0).split(","), p2 = nb2.get(1).split(",");
                    if (Integer.parseInt(p1[0]) - Integer.parseInt(p2[0]) == 0 || Integer.parseInt(p1[1]) - Integer.parseInt(p2[1]) == 0) { topLabels.put(k, "PuitG"); break; }
                }
                // MarchG, ArmG sur feuilles
                Collections.shuffle(gl, rng);
                int gli = 0;
                if (gli < gl.size()) { topLabels.put(gl.get(gli), "MarchG"); gli++; }
                if (gli < gl.size()) { topLabels.put(gl.get(gli), "ArmG"); gli++; }
                if (gli < gl.size()) { topLabels.put(gl.get(gli), "TresorG"); gli++; }
                // Corridors restants: CG1 (droit) ou GI2 (virage)
                for (String k : gco) if (!topLabels.containsKey(k)) {
                    List<String> nb2 = new ArrayList<>(ga.get(k));
                    String[] p1 = nb2.get(0).split(","), p2 = nb2.get(1).split(",");
                    boolean st = Integer.parseInt(p1[0])-Integer.parseInt(p2[0])==0 || Integer.parseInt(p1[1])-Integer.parseInt(p2[1])==0;
                    topLabels.put(k, st ? "CG1" : "GI2");
                }
                for (String k : gl) if (!topLabels.containsKey(k)) topLabels.put(k, "CDG");
                // Maisons: 3-5, remplacent des CG1/GI2/CDG, pas adjacentes entre elles
                List<String> hl = new ArrayList<>();
                for (String k : ga.keySet()) {
                    if (k.equals(gLeaf)) continue;
                    String v = topLabels.get(k);
                    if (v != null && (v.equals("CG1") || v.equals("GI2") || v.equals("CDG") || v.equals("GI3") || v.equals("GI4"))) hl.add(k);
                }
                Collections.shuffle(hl, rng);
                Set<String> hs = new HashSet<>();
                int hMax = 3 + rng.nextInt(3);
                for (String k : hl) {
                    if (hs.size() >= hMax) break;
                    int dk = ga.get(k).size();
                    // Seulement les cellules qui peuvent devenir une maison (deg1, deg3, ou deg2 virage)
                    if (dk == 2) {
                        List<String> nb2 = new ArrayList<>(ga.get(k));
                        String[] p1 = nb2.get(0).split(","), p2 = nb2.get(1).split(",");
                        boolean straight = Integer.parseInt(p1[0]) - Integer.parseInt(p2[0]) == 0
                            || Integer.parseInt(p1[1]) - Integer.parseInt(p2[1]) == 0;
                        if (straight) continue; // CG1, pas une maison
                    }
                    boolean treeAdj = false; for (String nb : ga.getOrDefault(k, Set.of())) if (hs.contains(nb)) { treeAdj = true; break; }
                    if (treeAdj) continue;
                    boolean adjSpecial = false; for (String nb : ga.getOrDefault(k, Set.of())) { String vl = topLabels.get(nb); if (vl != null && (vl.equals("PuitG") || vl.equals("MarchG") || vl.equals("ArmG") || vl.equals("TresorG"))) { adjSpecial = true; break; } }
                    if (adjSpecial) continue;
                    hs.add(k);
                }
                for (String k : hs) {
                    int d = ga.get(k).size();
                    if (d == 3) topLabels.put(k, "MG3");
                    else if (d == 2) {
                        List<String> nb2 = new ArrayList<>(ga.get(k));
                        String[] p1 = nb2.get(0).split(","), p2 = nb2.get(1).split(",");
                        boolean turn = Integer.parseInt(p1[0]) - Integer.parseInt(p2[0]) != 0
                            && Integer.parseInt(p1[1]) - Integer.parseInt(p2[1]) != 0;
                        if (turn) topLabels.put(k, "MG2");
                    } else if (d == 1) topLabels.put(k, "MG1");
                }
                for (var e : ga.entrySet()) { adj.putIfAbsent(e.getKey(), new HashSet<>()); adj.get(e.getKey()).addAll(e.getValue()); goblinCells.add(e.getKey()); }
                goblinCells.add(gLeaf);
            }
        // Chapelle + Crypte
        Map<String, Set<String>> ct = allTrees.get(cIdx);
        String cs = allStarts.get(cIdx);
        for (String k : ct.keySet()) {
            String vl = topLabels.get(k);
            if (ct.get(k).size() != 1 || k.equals(cs) || vl == null || !vl.equals("culDJ")) continue;
            String mb = ct.get(k).iterator().next();
            String[] kp = k.split(","), mp = mb.split(",");
            int ex = Integer.parseInt(kp[0]) + (Integer.parseInt(kp[0]) - Integer.parseInt(mp[0]));
            int ez = Integer.parseInt(kp[1]) + (Integer.parseInt(kp[1]) - Integer.parseInt(mp[1]));
            if (ex < 0 || ex >= GRID_SIZE || ez < 0 || ez >= GRID_SIZE || globalOccupied.contains(ex+","+ez)) continue;
            globalOccupied.add(ex+","+ez);
            adj.put(ex+","+ez, new HashSet<>());
            adj.get(k).add(ex+","+ez); adj.get(ex+","+ez).add(k);
            topLabels.put(k, "Chapelle1"); topLabels.put(ex+","+ez, "Chapelle2");
            int dir = (Integer.parseInt(kp[0])-Integer.parseInt(mp[0])==1)?0:(Integer.parseInt(kp[0])-Integer.parseInt(mp[0])==-1)?2:(Integer.parseInt(kp[1])-Integer.parseInt(mp[1])==1)?1:3;
            int pathLen = 3 + rng.nextInt(3);
            int turnAt = 1 + rng.nextInt(pathLen - 1);
            int turnDir = rng.nextBoolean() ? 1 : -1;
            String pv = ex+","+ez; int cx = ex, cz = ez;
            for (int s = 0; s < pathLen; s++) {
                if (s == turnAt) dir = (dir + turnDir + 4) % 4;
                int ncx = cx + DIR_OFFSET[dir][0], ncz = cz + DIR_OFFSET[dir][1];
                if (ncx < 0 || ncx >= GRID_SIZE || ncz < 0 || ncz >= GRID_SIZE || globalOccupied.contains(ncx+","+ncz)) break;
                globalOccupied.add(ncx+","+ncz); adj.put(ncx+","+ncz, new HashSet<>());
                adj.get(pv).add(ncx+","+ncz); adj.get(ncx+","+ncz).add(pv);
                topLabels.put(ncx+","+ncz, s == turnAt - 1 ? "I2" : pickC(rng)); pv = ncx+","+ncz; cx = ncx; cz = ncz;
            }
            for (int s = 0; s < 2; s++) {
                int ncx = cx + DIR_OFFSET[dir][0], ncz = cz + DIR_OFFSET[dir][1];
                if (ncx < 0 || ncx >= GRID_SIZE || ncz < 0 || ncz >= GRID_SIZE || globalOccupied.contains(ncx+","+ncz)) break;
                globalOccupied.add(ncx+","+ncz); adj.put(ncx+","+ncz, new HashSet<>());
                adj.get(pv).add(ncx+","+ncz); adj.get(ncx+","+ncz).add(pv);
                topLabels.put(ncx+","+ncz, s == 0 ? "Crypte1" : "Crypte2"); pv = ncx+","+ncz; cx = ncx; cz = ncz;
            }
            break;
        }
        // Prison
        Map<String, Set<String>> pt = allTrees.get(pIdx);
        String ps = allStarts.get(pIdx);
        for (String pk : pt.keySet()) {
            String vp = topLabels.get(pk);
            if (pt.get(pk).size() != 1 || pk.equals(ps) || vp == null || !vp.equals("culDJ")) continue;
            String nb = pt.get(pk).iterator().next();
            String[] kp = pk.split(","), np = nb.split(",");
            int dx = Integer.parseInt(kp[0]) - Integer.parseInt(np[0]), dy = Integer.parseInt(kp[1]) - Integer.parseInt(np[1]);
            int d = dx == 1 ? 0 : dx == -1 ? 2 : dy == 1 ? 1 : 3;
            int x2 = Integer.parseInt(kp[0]) + dx, z2 = Integer.parseInt(kp[1]) + dy;
            int r1 = (d+1)%4, x3 = x2 + DIR_OFFSET[r1][0], z3 = z2 + DIR_OFFSET[r1][1];
            int r2 = (r1+1)%4, x4 = x3 + DIR_OFFSET[r2][0], z4 = z3 + DIR_OFFSET[r2][1];
            if (x2 < 0 || x2 >= GRID_SIZE || z2 < 0 || z2 >= GRID_SIZE || x3 < 0 || x3 >= GRID_SIZE || z3 < 0 || z3 >= GRID_SIZE || x4 < 0 || x4 >= GRID_SIZE || z4 < 0 || z4 >= GRID_SIZE) continue;
            if (globalOccupied.contains(x2+","+z2) || globalOccupied.contains(x3+","+z3) || globalOccupied.contains(x4+","+z4)) continue;
            topLabels.put(pk, "PrisonC1");
            String[] prisonCells = {x2+","+z2, x3+","+z3, x4+","+z4};
            String[] prisonLabels = {"PrisonC2", "PrisonC3", "PrisonC4"};
            String pv2 = pk;
            for (int pi = 0; pi < prisonCells.length; pi++) {
                globalOccupied.add(prisonCells[pi]);
                adj.put(prisonCells[pi], new HashSet<>());
                adj.get(pv2).add(prisonCells[pi]); adj.get(prisonCells[pi]).add(pv2);
                topLabels.put(prisonCells[pi], prisonLabels[pi]); pv2 = prisonCells[pi];
            }
            break;
        }
        // Helper: verifier si une cellule est une sortie du hub
        java.util.function.Predicate<String> isHubExit = k -> {
            String[] kp = k.split(",");
            int kx = Integer.parseInt(kp[0]), kz = Integer.parseInt(kp[1]);
            return (kx == hx-1 && kz == hz) || (kx == hx+2 && kz == hz) || (kx == hx+1 && kz == hz-1) || (kx == hx && kz == hz+2) || (kx == hx+1 && kz == hz+2);
        };
        // MJ 5-10 : MJ1/3/5 sur culDJ, MJ2/4 sur CJ, jamais adjacents entre eux, jamais 2 memes types sur le meme arbre
        // Exclure les cellules gobelin du pool MJ
        int mjT = 5 + rng.nextInt(6);
        Map<String, Integer> treeForNode = new HashMap<>();
        for (int ti = 0; ti < allTrees.size(); ti++) for (String k : allTrees.get(ti).keySet()) treeForNode.put(k, ti);
        Set<String> mjPlacedKeys = new HashSet<>();
        Map<Integer, Set<String>> mjTypesOnTree = new HashMap<>();
        for (int ti = 0; ti < allTrees.size(); ti++) mjTypesOnTree.put(ti, new HashSet<>());
        String[] leafMj = {"MJ1", "MJ3", "MJ5"}, corrMj = {"MJ2", "MJ4"};
        for (int attempt = 0; attempt < 50 && mjPlacedKeys.size() < mjT; attempt++) {
            // Chercher une feuille culDJ ou un couloir CJ non adjacent a un MJ deja place
            String best = null; boolean bestIsLeaf = false; String bestType = null;
            for (String k : topLabels.keySet()) {
                if (mjPlacedKeys.contains(k)) continue;
                if (goblinCells.contains(k)) continue;
                if (isHubExit.test(k)) continue;
                if (mjPlacedKeys.size() >= mjT) break;
                String v = topLabels.get(k);
                if (v == null) continue;
                boolean isLeaf = v.equals("culDJ");
                boolean isCorr = v.startsWith("CJ") || v.startsWith("CG");
                if (!isLeaf && !isCorr) continue;
                // Verifier adjacence avec un MJ deja place
                boolean adjMj = false;
                for (String nb : adj.getOrDefault(k, Set.of())) if (mjPlacedKeys.contains(nb)) { adjMj = true; break; }
                if (adjMj) continue;
                // Verifier qu'il reste un type non utilise sur cet arbre
                int ti = treeForNode.getOrDefault(k, -1);
                Set<String> usedOnTree = mjTypesOnTree.getOrDefault(ti, new HashSet<>());
                String[] pool = isLeaf ? leafMj : corrMj;
                String availType = null;
                for (String t : pool) if (!usedOnTree.contains(t)) { availType = t; break; }
                if (availType == null) continue;
                best = k; bestIsLeaf = isLeaf; bestType = availType; break;
            }
            if (best != null) {
                topLabels.put(best, bestType);
                mjPlacedKeys.add(best);
                int ti = treeForNode.getOrDefault(best, -1);
                if (ti >= 0) mjTypesOnTree.get(ti).add(bestType);
            }
        }
        // MarchandNoir: uniquement sur culDJ, pas sur sortie hub
        for (var e : new ArrayList<>(topLabels.entrySet())) {
            if (e.getValue() != null && e.getValue().equals("culDJ") && !isHubExit.test(e.getKey())) {
                topLabels.put(e.getKey(), "MarchandNoir"); break;
            }
        }
        // Loot manquant: Lootdj2 sur CJ, Lootdj1/3 sur culDJ, pas dans le tree gobelin
        if (missingLootType != null) {
            boolean lootCorr = missingLootType.equals("Lootdj2");
            String lk = null;
            for (var e : topLabels.entrySet()) {
                String v = e.getValue(), k = e.getKey(); if (v == null || goblinCells.contains(k) || isHubExit.test(k)) continue;
                if (lootCorr && (v.startsWith("CJ") || v.startsWith("CG"))) { lk = k; break; }
                if (!lootCorr && v.equals("culDJ")) { lk = k; break; }
            }
            if (lk != null) topLabels.put(lk, missingLootType);
        }
        // PuitDJ: 1 sur couloir CJ, pas dans le tree gobelin, pas sur sortie hub
        String puitDJKey = null;
        for (var e : topLabels.entrySet()) {
            String v = e.getValue(), k = e.getKey(); if (v == null || goblinCells.contains(k) || isHubExit.test(k)) continue;
            if (v.startsWith("CJ") || v.startsWith("CG")) { puitDJKey = k; break; }
        }
        if (puitDJKey != null) topLabels.put(puitDJKey, "PuitDJ");
        // Validation
        boolean hc1 = topLabels.containsValue("Chapelle1") && topLabels.containsValue("Crypte1");
        boolean hpr = false; for (var e : topLabels.entrySet()) if (e.getValue() != null && e.getValue().startsWith("PrisonC")) hpr = true;
        boolean hpg = topLabels.containsValue("PorteGob");
        boolean hmn = topLabels.containsValue("MarchandNoir");
        boolean hlt = topLabels.containsValue("Lootdj1") || topLabels.containsValue("Lootdj2") || topLabels.containsValue("Lootdj3");
        boolean hPuitDJ = topLabels.containsValue("PuitDJ");
        int gbc = 0; for (var e : topLabels.entrySet()) { String v = e.getValue(); if (v != null && (v.equals("PuitG") || v.equals("MarchG") || v.equals("ArmG") || v.equals("TresorG"))) gbc++; }
        boolean hmg = false; for (var e : topLabels.entrySet()) { String v = e.getValue(); if (v != null && v.startsWith("MG")) { hmg = true; break; } }
        if (!hc1 || !hpr || !hpg || !hmn || !hlt || !hPuitDJ || !hmg || gbc < 2 || mjPlacedKeys.size() < 5) return false;
        return true;
    }

    private static long lastSeed = 0;
    private static Map<String, String> lastTopLabels = null;

    public static long getLastSeed() { return lastSeed; }
    public static Map<String, String> getLastTopLabels() { return lastTopLabels; }

    public static DungeonResult generateDungeon(long seed) {
        int maxAttempts = seed != 0 ? 20 : 100;
        lastTopLabels = null;
        for (int outer = 0; outer < maxAttempts; outer++) {
            long actualSeed = seed != 0 && outer < 1 ? seed : System.nanoTime() + outer;
            Random rng = new Random(actualSeed);
            lastSeed = actualSeed;

            TreeResult sp1 = null;
            Map<String, String> labels = null;
            for (int inner = 0; inner < 50; inner++) {
                TreeResult try1 = generatePart1Tree();
                if (try1.adj.size() < 10) continue;
                List<String> leaves = new ArrayList<>();
                for (var e : try1.adj.entrySet()) if (e.getValue().size() == 1 && !e.getKey().equals(try1.startKey)) leaves.add(e.getKey());
                if (leaves.size() < 4) continue;
                if (!hasPrisonCandidate(try1.adj, try1.startKey)) continue;
                Map<String, String> tryLabels = analyzePart1(try1.startKey, try1.adj, rng);
                if (tryLabels == null) continue;
                sp1 = try1; labels = tryLabels; break;
            }
            if (sp1 == null) continue;

            String porteKey = findKeyByValue(labels, "porte");
            if (porteKey == null) continue;

            TavernResult tavern = placeTavernAndPath(sp1.adj, porteKey, rng);
            if (tavern == null) continue;
            for (var e : tavern.tavern.entrySet()) labels.put(e.getValue(), e.getKey());

            TreeResult sp2 = generatePart2Tree(tavern.exitKey, new HashSet<>(sp1.adj.keySet()));
            if (sp2.adj.size() < 5) continue;
            for (var e : sp2.adj.entrySet()) { if (sp1.adj.containsKey(e.getKey())) sp1.adj.get(e.getKey()).addAll(e.getValue()); else sp1.adj.put(e.getKey(), e.getValue()); }

            int p1Loot = 0; for (String v : labels.values()) if (v.equals("Loot1")) p1Loot++;
            int totalTarget = 1 + rng.nextInt(2);
            if (p1Loot > totalTarget) {
                List<String> lootNodes = new ArrayList<>();
                for (var e : labels.entrySet()) if (e.getValue().equals("Loot1") && !e.getKey().equals(sp1.startKey)) lootNodes.add(e.getKey());
                Collections.shuffle(lootNodes, rng);
                for (int i = 0; i < p1Loot - totalTarget && i < lootNodes.size(); i++) labels.put(lootNodes.get(i), "cul");
            }

            labels = analyzePart2(sp1.adj, tavern.exitKey, labels, tavern.pathSet, rng);
            if (labels == null) continue;

            String porte2Key = findKeyByValue(labels, "porte2");
            if (porte2Key == null) continue;

            CampResult camp = placeCampAndPath(sp1.adj, porte2Key, rng);
            if (camp == null) continue;
            for (String e : camp.campPathSet) {
                int deg = sp1.adj.get(e).size();
                if (deg == 2) {
                    List<String> nb = new ArrayList<>(sp1.adj.get(e));
                    String[] p1nb = nb.get(0).split(","), p2nb = nb.get(1).split(",");
                    labels.put(e, (p1nb[0].equals(p2nb[0]) || p1nb[1].equals(p2nb[1])) ? pickC(rng) : "I2");
                }
            }
            for (var e : camp.campNodes.entrySet()) labels.put(e.getValue(), e.getKey());

            TreeResult sp3 = generatePart3Tree(camp.campExit, new HashSet<>(sp1.adj.keySet()), rng);
            if (sp3.adj.size() < 10) continue;
            if (sp3.adj.get(sp3.startKey).isEmpty()) continue;
            for (var e : sp3.adj.entrySet()) { if (sp1.adj.containsKey(e.getKey())) sp1.adj.get(e.getKey()).addAll(e.getValue()); else sp1.adj.put(e.getKey(), e.getValue()); }

            labels = analyzePart3(sp1.adj, camp.campExit, labels, rng);
            if (labels != null) {
                // Trouver le type Lootdj manquant en P3 pour le placer en P4
                String missingLoot = null;
                Set<String> p3LootTypes = new HashSet<>();
                for (String v : labels.values()) {
                    if (v.startsWith("Lootdj")) p3LootTypes.add(v);
                }
                String[] allLootTypes = {"Lootdj1", "Lootdj2", "Lootdj3"};
                for (String lt : allLootTypes) {
                    if (!p3LootTypes.contains(lt)) { missingLoot = lt; break; }
                }

                Map<String, Set<String>> p4Adj = new HashMap<>();
                if (lastTopLabels != null) {
                    String hubKey = null;
                    for (var e : lastTopLabels.entrySet()) {
                        if (e.getValue().equals("Centrale")) { hubKey = e.getKey(); break; }
                    }
                    if (hubKey != null) {
                        String[] hp = hubKey.split(",");
                        int hx = Integer.parseInt(hp[0]), hz = Integer.parseInt(hp[1]);
                        if (!generatePart4Tree(p4Adj, lastTopLabels, hx, hz, missingLoot, rng)) { continue; }
                    }
                }
                DungeonResult dr = new DungeonResult();
                dr.adj = sp1.adj; dr.labels = labels; dr.startKey = sp1.startKey;
                dr.startX = sp1.startX; dr.startY = sp1.startY;
                dr.topLabels = lastTopLabels;
                dr.p4Adj = p4Adj.isEmpty() ? null : p4Adj;
                dr.missingLootType = missingLoot;
                return dr;
            }
        }
        return null;
    }

    private static TreeResult generatePart2Tree(String startKey, Set<String> blocked) {
        String[] s = startKey.split(",");
        return generateRawTree(PART2_TARGET_MIN, PART2_TARGET_MAX, PART2_MAX_I3, 1, PART2_STRAIGHT_WEIGHT,
            new Integer[]{Integer.parseInt(s[0]), Integer.parseInt(s[1])}, blocked);
    }

    // ===================== Part 3: Trunk tree =====================

    private static void addEdge(Map<String, Set<String>> adj, Set<String> occupied, String a, String b) {
        adj.putIfAbsent(a, new HashSet<>());
        adj.putIfAbsent(b, new HashSet<>());
        adj.get(a).add(b);
        adj.get(b).add(a);
        occupied.add(a);
        occupied.add(b);
    }

    private static void growMiniTree(String rootKey, int pDir, Map<String, Set<String>> adj,
                                     Set<String> occupied, Random rng) {
        String[] rp = rootKey.split(",");
        int rx = Integer.parseInt(rp[0]), ry = Integer.parseInt(rp[1]);
        int nx = rx + DIR_OFFSET[pDir][0], ny = ry + DIR_OFFSET[pDir][1];
        if (nx < 0 || nx >= GRID_SIZE || ny < 0 || ny >= GRID_SIZE || occupied.contains(nx+","+ny)) return;

        addEdge(adj, occupied, rootKey, nx+","+ny);
        int cDir = pDir, cx = nx, cy = ny;
        int branchLen = 1 + rng.nextInt(3);
        int st = 0;

        for (int i = 0; i < branchLen; i++) {
            st++;
            if (st >= 2 && rng.nextFloat() < 0.4) {
                cDir = (cDir + (rng.nextBoolean() ? 1 : 3)) % 4;
                st = 0;
            }
            int nnx = cx + DIR_OFFSET[cDir][0], nny = cy + DIR_OFFSET[cDir][1];
            if (nnx < 0 || nnx >= GRID_SIZE || nny < 0 || nny >= GRID_SIZE || occupied.contains(nnx+","+nny)) break;
            addEdge(adj, occupied, cx+","+cy, nnx+","+nny);
            cx = nnx; cy = nny;

            if (rng.nextFloat() < 0.25) {
                int sDir = (cDir + (rng.nextBoolean() ? 1 : 3)) % 4;
                int snx = cx + DIR_OFFSET[sDir][0], sny = cy + DIR_OFFSET[sDir][1];
                if (snx >= 0 && snx < GRID_SIZE && sny >= 0 && sny < GRID_SIZE && !occupied.contains(snx+","+sny)) {
                    addEdge(adj, occupied, cx+","+cy, snx+","+sny);
                }
            }
        }
    }

    private static void growSplitBranches(String endKey, Map<String, Set<String>> adj,
                                          Set<String> occupied, Random rng) {
        String[] ep = endKey.split(",");
        int ex = Integer.parseInt(ep[0]), ey = Integer.parseInt(ep[1]);

        List<Integer> ad = new ArrayList<>();
        for (int d = 0; d < 4; d++) {
            int nx = ex + DIR_OFFSET[d][0], ny = ey + DIR_OFFSET[d][1];
            if (nx >= 0 && nx < GRID_SIZE && ny >= 0 && ny < GRID_SIZE && !occupied.contains(nx+","+ny)) {
                boolean isBack = adj.get(endKey).stream().anyMatch(nb -> {
                    String[] np = nb.split(",");
                    return Integer.parseInt(np[0]) == nx && Integer.parseInt(np[1]) == ny;
                });
                if (!isBack) ad.add(d);
            }
        }
        Collections.shuffle(ad, rng);

        for (int b = 0; b < Math.min(2, ad.size()); b++) {
            int bDir = ad.get(b);
            int bx = ex + DIR_OFFSET[bDir][0], by = ey + DIR_OFFSET[bDir][1];
            addEdge(adj, occupied, endKey, bx+","+by);

            int bl = 3 + rng.nextInt(4);
            int cDir = bDir, cx = bx, cy = by, st = 0;
            for (int s = 0; s < bl; s++) {
                st++;
                if (st >= 2 && rng.nextFloat() < 0.3) {
                    cDir = (cDir + (rng.nextBoolean() ? 1 : 3)) % 4;
                    st = 0;
                }
                int nnx = cx + DIR_OFFSET[cDir][0], nny = cy + DIR_OFFSET[cDir][1];
                if (nnx < 0 || nnx >= GRID_SIZE || nny < 0 || nny >= GRID_SIZE || occupied.contains(nnx+","+nny)) break;
                addEdge(adj, occupied, cx+","+cy, nnx+","+nny);
                cx = nnx; cy = nny;
            }
        }
    }

    private static TreeResult generateTrunkTree(int targetMin, int targetMax,
                                                 Integer[] start, Set<String> blocked, int maxI3, int maxI4, Random rng) {
        Map<String, Set<String>> adj = new HashMap<>();
        Set<String> occupied = new HashSet<>(blocked != null ? blocked : new HashSet<>());

        int sx = start != null ? start[0] : GRID_SIZE / 2;
        int sy = start != null ? start[1] : GRID_SIZE / 2;
        String startKey = sx + "," + sy;
        adj.put(startKey, new HashSet<>());
        occupied.add(startKey);

        // Phase 1: Find first direction and build trunk
        List<int[]> dirs = new ArrayList<>(Arrays.asList(DIR_OFFSET));
        Collections.shuffle(dirs, rng);
        int dir = -1;
        for (int[] d : dirs) {
            int tx = sx + d[0], ty = sy + d[1];
            if (tx >= 0 && tx < GRID_SIZE && ty >= 0 && ty < GRID_SIZE && !occupied.contains(tx + "," + ty)) {
                for (int i = 0; i < 4; i++) {
                    if (DIR_OFFSET[i][0] == d[0] && DIR_OFFSET[i][1] == d[1]) { dir = i; break; }
                }
                break;
            }
        }
        if (dir < 0) { TreeResult tr = new TreeResult(); tr.startKey = startKey; tr.startX = sx; tr.startY = sy; tr.adj = adj; return tr; }

        int cx = sx + DIR_OFFSET[dir][0], cy = sy + DIR_OFFSET[dir][1];
        addEdge(adj, occupied, startKey, cx+","+cy);

        List<String> trunkCells = new ArrayList<>();
        trunkCells.add(cx+","+cy);

        int trunkTarget = 8 + rng.nextInt(5);
        int stepsSinceTurn = 1;

        for (int t = 1; t < trunkTarget; t++) {
            stepsSinceTurn++;
            if (stepsSinceTurn >= 2 && rng.nextFloat() < 0.35) {
                dir = (dir + (rng.nextBoolean() ? 1 : 3)) % 4;
                stepsSinceTurn = 0;
            }

            int nx = cx + DIR_OFFSET[dir][0], ny = cy + DIR_OFFSET[dir][1];
            if (nx < 0 || nx >= GRID_SIZE || ny < 0 || ny >= GRID_SIZE || occupied.contains(nx+","+ny)) {
                int od = dir;
                for (int a = 0; a < 4; a++) {
                    dir = (od + a) % 4;
                    nx = cx + DIR_OFFSET[dir][0]; ny = cy + DIR_OFFSET[dir][1];
                    if (nx >= 0 && nx < GRID_SIZE && ny >= 0 && ny < GRID_SIZE && !occupied.contains(nx+","+ny)) break;
                }
                if (nx < 0 || nx >= GRID_SIZE || ny < 0 || ny >= GRID_SIZE || occupied.contains(nx+","+ny)) break;
                stepsSinceTurn = 0;
            }

            String nk = nx + "," + ny;
            addEdge(adj, occupied, cx+","+cy, nk);
            trunkCells.add(nk);
            cx = nx; cy = ny;

            // Side branch from this trunk cell
            if (t < trunkTarget - 1 && rng.nextFloat() < 0.5) {
                int pDir = (dir + (rng.nextBoolean() ? 1 : 3)) % 4;
                growMiniTree(nk, pDir, adj, occupied, rng);
            }
        }

        // Phase 2: Split at end
        String endKey = trunkCells.get(trunkCells.size() - 1);
        growSplitBranches(endKey, adj, occupied, rng);

        // Phase 3: Fill remaining cells with random growth
        int targetSize = targetMin + rng.nextInt(targetMax - targetMin + 1);
        int ci3 = 0, ci4 = 0;
        for (Set<String> nb : adj.values()) { int d = nb.size(); if (d == 3) ci3++; else if (d == 4) ci4++; }

        while (adj.size() < targetSize) {
            List<String[]> candidates = new ArrayList<>();
            for (String node : adj.keySet()) {
                if (node.equals(startKey)) continue;
                int deg = adj.get(node).size();
                if (deg >= 4) continue;
                if (deg == 2 && ci3 >= maxI3) continue;
                if (deg == 3 && ci4 >= maxI4) continue;

                String[] pp = node.split(",");
                int px = Integer.parseInt(pp[0]), py = Integer.parseInt(pp[1]);
                for (int[] d : DIR_OFFSET) {
                    int nx = px + d[0], ny = py + d[1];
                    if (nx < 0 || nx >= GRID_SIZE || ny < 0 || ny >= GRID_SIZE || occupied.contains(nx+","+ny)) continue;
                    if (deg >= 2) {
                        boolean skip = false;
                        for (String nb1 : adj.get(node)) {
                            String[] n1p = nb1.split(",");
                            int n1x = Integer.parseInt(n1p[0]), n1z = Integer.parseInt(n1p[1]);
                            for (String nb2 : adj.get(node)) {
                                if (nb1.equals(nb2)) continue;
                                String[] n2p = nb2.split(",");
                                int n2x = Integer.parseInt(n2p[0]), n2z = Integer.parseInt(n2p[1]);
                                if (px - n1x == n2x - px && py - n1z == n2z - py) {
                                    Set<String> a1 = adj.get(nb1), a2 = adj.get(nb2);
                                    if ((a1 != null && a1.size() >= 3) || (a2 != null && a2.size() >= 3)) {
                                        skip = true; break;
                                    }
                                }
                            }
                            if (skip) break;
                        }
                        if (skip) continue;
                    }
                    // Weight toward trunk cells for more side branches
                    int w = trunkCells.contains(node) ? 3 : 1;
                    for (int wi = 0; wi < w; wi++) candidates.add(new String[]{node, nx+","+ny});
                }
            }
            if (candidates.isEmpty()) break;
            String[] choice = candidates.get(rng.nextInt(candidates.size()));
            addEdge(adj, occupied, choice[0], choice[1]);
            int nd = adj.get(choice[0]).size();
            if (nd == 3) ci3++; else if (nd == 4) ci4++;
        }

        TreeResult tr = new TreeResult();
        tr.startKey = startKey;
        tr.startX = sx;
        tr.startY = sy;
        tr.adj = adj;
        return tr;
    }

    private static TreeResult generatePart3Tree(String startKey, Set<String> blocked, Random rng) {
        String[] s = startKey.split(",");
        return generateTrunkTree(35, 45,
            new Integer[]{Integer.parseInt(s[0]), Integer.parseInt(s[1])}, blocked,
            PART3_MAX_IJ3, PART3_MAX_IJ4, rng);
    }

    private static String findKeyByValue(Map<String, String> map, String value) {
        for (var e : map.entrySet()) if (e.getValue().equals(value)) return e.getKey();
        return null;
    }
}
