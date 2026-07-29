package com.dungeonmod.debug;

import com.dungeonmod.debug.DungeonAlgo.Point;
import com.dungeonmod.debug.DungeonAlgo.TreeResult;

import java.util.*;

/**
 * Helpers de construction de graphes/arborescences pour le donjon.
 *
 * Cette extraction reste volontairement mécanique : les algos appelants gardent
 * les mêmes règles, mais la manipulation bas niveau des arbres commence à sortir
 * de DungeonAlgo.
 */
final class DungeonTreeBuilder {
    private DungeonTreeBuilder() {}

    static TreeResult generateRawTree(int targetMin, int targetMax, int maxI3, int maxI4,
                                      int straightWeight, Point startPt, Set<Point> blocked,
                                      int maxColinearRun, Random rng) {
        if (blocked == null) blocked = new HashSet<>();
        Point start = startPt != null ? startPt : new Point(DungeonAlgo.GRID_SIZE / 2, DungeonAlgo.GRID_SIZE / 2);

        Set<Point> occupied = new HashSet<>();
        occupied.add(start);
        Map<Point, Set<Point>> adj = new HashMap<>();
        adj.put(start, new HashSet<>());
        List<Point> allNodes = new ArrayList<>();
        allNodes.add(start);
        Map<Point, int[]> entryDir = new HashMap<>();

        List<int[]> dirs = new ArrayList<>(Arrays.asList(DungeonAlgo.DIR_OFFSET));
        Collections.shuffle(dirs, rng);
        boolean found = false;
        int fdx = 0, fdy = 0;
        for (int[] d : dirs) {
            Point target = start.move(d);
            if (!target.isOutOfBounds() && !blocked.contains(target)) {
                fdx = d[0]; fdy = d[1]; found = true; break;
            }
        }
        if (!found) {
            TreeResult tr = new TreeResult();
            tr.startPoint = start; tr.startKey = start.key(); tr.startX = start.x(); tr.startY = start.y(); tr.adj = adj;
            return tr;
        }

        Point firstPoint = start.move(fdx, fdy);
        occupied.add(firstPoint); allNodes.add(firstPoint);
        adj.put(firstPoint, new HashSet<>());
        adj.get(start).add(firstPoint); adj.get(firstPoint).add(start);
        entryDir.put(firstPoint, new int[]{fdx, fdy});

        int targetSize = targetMin + rng.nextInt(targetMax - targetMin + 1);
        while (allNodes.size() < targetSize) {
            List<Point[]> candidates = new ArrayList<>();
            int cI3 = 0, cI4 = 0;
            for (Set<Point> nb : adj.values()) { int d = nb.size(); if (d == 3) cI3++; else if (d == 4) cI4++; }
            for (Point p : allNodes) {
                if (p.equals(start)) continue;
                int deg = adj.get(p).size();
                if (deg == 2 && cI3 >= maxI3) continue;
                if (deg == 3 && cI4 >= maxI4) continue;
                if (deg >= 4) continue;

                for (int[] d : DungeonAlgo.DIR_OFFSET) {
                    Point next = p.move(d);
                    if (!next.isOutOfBounds()) {
                        if (!occupied.contains(next) && !blocked.contains(next)) {
                            // Limite géométrique : max maxColinearRun segments alignés
                            // dans l'adj réelle (I3/I4 sur l'axe comptés comme la droite)
                            if (DungeonConstraints.colinearRunAfterEdge(p, next, adj) > maxColinearRun) continue;
                            if (deg >= 2) {
                                boolean skip = false;
                                for (Point n1 : adj.get(p)) {
                                    for (Point n2 : adj.get(p)) {
                                        if (n1.equals(n2)) continue;
                                        if (p.x() - n1.x() == n2.x() - p.x() && p.y() - n1.y() == n2.y() - p.y()) {
                                            Set<Point> a1 = adj.get(n1), a2 = adj.get(n2);
                                            if ((a1 != null && a1.size() >= 3) || (a2 != null && a2.size() >= 3)) {
                                                skip = true; break;
                                            }
                                        }
                                    }
                                    if (skip) break;
                                }
                                if (skip) continue;
                            }
                            candidates.add(new Point[]{p, next});
                        }
                    }
                }
            }
            List<Point[]> weighted = new ArrayList<>();
            for (Point[] cand : candidates) {
                int w = 1;
                Point p = cand[0];
                int deg = adj.get(p).size();
                int[] ed = entryDir.get(p);
                if (deg == 1 && ed != null) {
                    Point c = cand[1];
                    if ((c.x() - p.x()) == ed[0] && (c.y() - p.y()) == ed[1]) w = straightWeight;
                }
                for (int i = 0; i < w; i++) weighted.add(cand);
            }
            if (weighted.isEmpty()) break;
            Point[] choice = weighted.get(rng.nextInt(weighted.size()));
            Point parent = choice[0], child = choice[1];
            occupied.add(child); allNodes.add(child);
            adj.put(child, new HashSet<>());
            adj.get(parent).add(child); adj.get(child).add(parent);

            entryDir.put(child, new int[]{child.x() - parent.x(), child.y() - parent.y()});
        }
        TreeResult tr = new TreeResult();
        tr.startPoint = start; tr.startKey = start.key(); tr.startX = start.x(); tr.startY = start.y(); tr.adj = adj;
        return tr;
    }

    static void addEdge(Map<Point, Set<Point>> adj, Set<Point> occupied, Point a, Point b) {
        adj.putIfAbsent(a, new HashSet<>());
        adj.putIfAbsent(b, new HashSet<>());
        adj.get(a).add(b);
        adj.get(b).add(a);
        occupied.add(a);
        occupied.add(b);
    }

    /** Copie superficielle de l'adjacence (sets clonés) pour tests de chemins. */
    static Map<Point, Set<Point>> copyAdj(Map<Point, Set<Point>> adj) {
        Map<Point, Set<Point>> out = new HashMap<>();
        for (var e : adj.entrySet()) out.put(e.getKey(), new HashSet<>(e.getValue()));
        return out;
    }
}
