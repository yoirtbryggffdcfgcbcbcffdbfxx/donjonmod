package com.dungeonmod.debug;

import com.dungeonmod.debug.DungeonAlgo.CampResult;
import com.dungeonmod.debug.DungeonAlgo.Point;
import com.dungeonmod.debug.DungeonAlgo.RoomIds;
import com.dungeonmod.debug.DungeonAlgo.RoomPools;
import com.dungeonmod.debug.DungeonAlgo.Shape;
import com.dungeonmod.debug.DungeonAlgo.Theme;
import com.dungeonmod.debug.DungeonAlgo.TreeResult;

import java.util.*;

/**
 * Génération/analyse de la deuxième partie du donjon : zone P2, porte2
 * et chemin vers le campement.
 *
 * Extraction mécanique depuis DungeonAlgo : déplacer le code sans modifier
 * volontairement le comportement.
 */
final class DungeonPart2 {
    private DungeonPart2() {}

    static Map<Point, String> analyzePart2(Map<Point, Set<Point>> adj, Point exitPoint,
                                                    Map<Point, String> labels, Set<Point> pathSet, Random rng) {
        // Sortie de taverne : label structurel déduit de l'adjacence réelle (forme P1/P2).
        labels.put(exitPoint, DungeonLabels.labelForNeighbors(adj.get(exitPoint), Theme.P12, rng));

        Set<Point> allLabeled = new HashSet<>(labels.keySet());
        List<Point> p2Nodes = new ArrayList<>();
        Map<Point, Integer> dist = new HashMap<>();
        Set<Point> seen = new HashSet<>(); Queue<Point> q = new LinkedList<>();
        q.add(exitPoint); seen.add(exitPoint); dist.put(exitPoint, 0);
        while (!q.isEmpty()) {
            Point n = q.poll();
            if (!allLabeled.contains(n)) p2Nodes.add(n);
            for (Point nb : adj.get(n)) {
                if (!seen.contains(nb)) {
                    seen.add(nb); q.add(nb); dist.put(nb, dist.get(n) + 1);
                }
            }
        }

        List<Point> leaves = new ArrayList<>(), internals = new ArrayList<>();
        for (Point n : p2Nodes) { if (adj.get(n).size() == 1) leaves.add(n); else internals.add(n); }

        List<Point> cList = new ArrayList<>(), i2List = new ArrayList<>(), i3List = new ArrayList<>(), i4List = new ArrayList<>();
        for (Point node : internals) {
            int deg = adj.get(node).size();
            if (deg == 2) {
                List<Point> nb = new ArrayList<>(adj.get(node));
                if (isStraight(nb.get(0), nb.get(1))) cList.add(node); else i2List.add(node);
            } else if (deg == 3) i3List.add(node);
            else if (deg == 4) i4List.add(node);
        }
        Collections.shuffle(cList, rng);

        // porte2 déjà placée en fin de tronc (appendP2ExitSequence) — exclure des leaves
        Point existingPorte2 = findPointByValue(labels, RoomIds.DOOR_2);
        if (existingPorte2 != null) leaves.remove(existingPorte2);

        // Besoin : ogre + fontaine + m3 + m1 + loot ≥ 5 feuilles de branches.
        if (leaves.size() < 5) return null;
        // Et 3 couloirs droits LIBRES pour M2 + M4 + M5 (hors chemin taverne).
        long availCorr = cList.stream().filter(n -> !pathSet.contains(n) && labels.get(n) == null).count();
        if (availCorr < 3) return null;

        // ESPACEMENT MONSTRES + ISOLEMENT OGRE (règles dures, conversation 3) : toute salle
        // monstre est à distance >= DungeonAlgo.MONSTER_MIN_DIST des autres ET à distance
        // >= DungeonAlgo.OGRE_MIN_MONSTER_DIST de l'Ogre. L'Ogre reste posé sur la feuille la plus
        // éloignée possible de la sortie de taverne. Infaisable => rejet (null => retry).
        Set<Point> monsters = DungeonConstraints.monsterPoints(labels); // M1-M4 hérités de P1
        Set<Point> monsterSet = new HashSet<>();

        List<Point> ogreCands = new ArrayList<>(leaves);
        // Tri par distance décroissante à la sortie : l'Ogre reste la feuille la plus éloignée.
        ogreCands.sort(Comparator.comparingInt((Point n) -> -dist.getOrDefault(n, 0)));
        Point ogreLeaf = null;
        for (Point n : ogreCands) {
            if (DungeonConstraints.isFarFromAll(adj, n, monsters, DungeonAlgo.OGRE_MIN_MONSTER_DIST)) { ogreLeaf = n; break; }
        }
        if (ogreLeaf == null) return null;
        labels.put(ogreLeaf, RoomIds.OGRE); monsterSet.add(ogreLeaf);
        Map<Point, Integer> ogreDist = DungeonConstraints.bfsDistances(adj, ogreLeaf);
        List<Point> remain = new ArrayList<>(leaves);
        remain.remove(ogreLeaf);
        Collections.shuffle(remain, rng);

        if (remain.isEmpty()) return null;
        labels.put(remain.get(0), RoomIds.FOUNTAIN);
        // porte2 déjà posée structurellement — ne pas la re-choisir ici

        Point m3Leaf = null;
        for (Point n : remain) {
            if (labels.containsKey(n)) continue;
            if (ogreDist.getOrDefault(n, Integer.MAX_VALUE) < DungeonAlgo.OGRE_MIN_MONSTER_DIST) continue;
            if (!DungeonConstraints.isFarFromAll(adj, n, monsters, DungeonAlgo.MONSTER_MIN_DIST)) continue;
            m3Leaf = n; break;
        }
        if (m3Leaf == null) return null;
        labels.put(m3Leaf, RoomIds.MONSTER_3); monsterSet.add(m3Leaf); monsters.add(m3Leaf);

        // M1 (feuille) : OBLIGATOIRE en P2, espacement respecté, sinon rejet (null => retry).
        Point m1Leaf = null;
        for (Point n : remain) {
            if (labels.containsKey(n)) continue;
            if (ogreDist.getOrDefault(n, Integer.MAX_VALUE) < DungeonAlgo.OGRE_MIN_MONSTER_DIST) continue;
            if (!DungeonConstraints.isFarFromAll(adj, n, monsters, DungeonAlgo.MONSTER_MIN_DIST)) continue;
            m1Leaf = n; break;
        }
        if (m1Leaf == null) return null;
        labels.put(m1Leaf, RoomIds.MONSTER_1); monsterSet.add(m1Leaf); monsters.add(m1Leaf);

        Point lootLeaf = null;
        for (Point n : remain) {
            if (labels.containsKey(n)) continue;
            boolean hasAdj = false; for (Point nb : adj.get(n)) if (monsterSet.contains(nb)) { hasAdj = true; break; }
            if (!hasAdj) { lootLeaf = n; break; }
        }
        if (lootLeaf == null) { for (Point n : remain) { if (!labels.containsKey(n)) { lootLeaf = n; break; } } }
        labels.put(lootLeaf, RoomIds.LOOT_1);

        for (Point n : remain) { if (!labels.containsKey(n)) labels.put(n, RoomIds.DEAD_END); }

        // M4 (couloir droit) : OBLIGATOIRE en P2, espacement respecté, sinon rejet (null).
        Point m4Corr = null;
        for (Point n : cList) {
            if (pathSet.contains(n) || labels.containsKey(n)) continue;
            if (ogreDist.getOrDefault(n, Integer.MAX_VALUE) < DungeonAlgo.OGRE_MIN_MONSTER_DIST) continue;
            if (!DungeonConstraints.isFarFromAll(adj, n, monsters, DungeonAlgo.MONSTER_MIN_DIST)) continue;
            m4Corr = n; break;
        }
        if (m4Corr == null) return null;
        labels.put(m4Corr, RoomIds.MONSTER_4); monsterSet.add(m4Corr); monsters.add(m4Corr);

        // M2 (couloir droit) : OBLIGATOIRE en P2, espacement respecté, sinon rejet (null).
        Point m2Corr = null;
        for (Point n : cList) {
            if (pathSet.contains(n) || labels.containsKey(n)) continue;
            if (ogreDist.getOrDefault(n, Integer.MAX_VALUE) < DungeonAlgo.OGRE_MIN_MONSTER_DIST) continue;
            if (!DungeonConstraints.isFarFromAll(adj, n, monsters, DungeonAlgo.MONSTER_MIN_DIST)) continue;
            m2Corr = n; break;
        }
        if (m2Corr == null) return null;
        labels.put(m2Corr, RoomIds.MONSTER_2); monsterSet.add(m2Corr); monsters.add(m2Corr);

        // M5 (couloir droit) : n'est PLUS avant porte2 — elle est OBLIGATOIRE n'importe où
        // sur une ligne droite de la zone P2 (hors chemin taverne), espacement respecté,
        // sinon rejet (null => retry amont).
        Point m5Corr = null;
        for (Point n : cList) {
            if (pathSet.contains(n) || labels.containsKey(n)) continue;
            if (ogreDist.getOrDefault(n, Integer.MAX_VALUE) < DungeonAlgo.OGRE_MIN_MONSTER_DIST) continue;
            if (!DungeonConstraints.isFarFromAll(adj, n, monsters, DungeonAlgo.MONSTER_MIN_DIST)) continue;
            m5Corr = n; break;
        }
        if (m5Corr == null) return null;
        labels.put(m5Corr, RoomIds.MONSTER_5); monsterSet.add(m5Corr); monsters.add(m5Corr);

        List<Point> remC = new ArrayList<>();
        for (Point n : cList) { if (!labels.containsKey(n)) remC.add(n); }
        if (!remC.isEmpty()) {
            Point pn = null;
            for (Point n : remC) {
                if (!pathSet.contains(n)) {
                    boolean adjSpecial = false;
                    for (Point nb : adj.get(n)) {
                        String lbl = labels.get(nb);
                        if (lbl != null && (lbl.equals(RoomIds.OGRE) || lbl.equals(RoomIds.FOUNTAIN) || lbl.equals(RoomIds.LOOT_1)
                            || lbl.equals(RoomIds.MONSTER_1) || lbl.equals(RoomIds.MONSTER_2) || lbl.equals(RoomIds.MONSTER_3) || lbl.equals(RoomIds.MONSTER_4)
                            || lbl.equals(RoomIds.MONSTER_5))) {
                            adjSpecial = true; break;
                        }
                    }
                    if (!adjSpecial) { pn = n; break; }
                }
            }
            if (pn == null) { for (Point n : remC) { if (!pathSet.contains(n)) { pn = n; break; } } }
            if (pn == null) pn = remC.get(0);
            labels.put(pn, RoomIds.WELL);
            remC.remove(pn);
        }

        Set<Point> toI2 = new HashSet<>();
        for (Point n : remC) {
            if (labels.containsKey(n)) continue;
            for (Point nb : adj.get(n)) {
                String lbl = labels.get(nb);
                if (lbl != null && (lbl.equals(RoomIds.MONSTER_2) || lbl.equals(RoomIds.MONSTER_4) || lbl.equals(RoomIds.MONSTER_5) || lbl.equals(RoomIds.WELL))) {
                    toI2.add(n); break;
                }
            }
        }

        Map<Point, String> remCDirs = new HashMap<>();
        for (Point n : remC) {
            if (toI2.contains(n)) continue;
            List<Point> nb = new ArrayList<>(adj.get(n));
            Point p0 = nb.get(0);
            Point p1 = nb.get(1);
            int dx = p1.x() - p0.x(), dz = p1.y() - p0.y();
            if (dx < 0 || (dx == 0 && dz < 0)) { dx = -dx; dz = -dz; }
            remCDirs.put(n, dx + "," + dz);
        }
        Set<Point> cSkip = new HashSet<>();
        for (Point n : remC) {
            if (cSkip.contains(n) || labels.containsKey(n) || toI2.contains(n)) continue;
            String dir = remCDirs.get(n);
            for (Point nb : adj.get(n)) {
                if (!remC.contains(nb) || cSkip.contains(nb) || labels.containsKey(nb) || toI2.contains(nb)) continue;
                if (dir.equals(remCDirs.get(nb))) { cSkip.add(nb); break; }
            }
        }
        for (Point n : remC) {
            if (toI2.contains(n)) {
                List<Point> nbs = new ArrayList<>(adj.get(n));
                if (nbs.size() == 2) {
                    if (!isStraight(nbs.get(0), nbs.get(1))) labels.put(n, RoomIds.CORRIDOR_TURN);
                    else labels.put(n, pickC(rng));
                }
            } else if (labels.containsKey(n)) continue;
            else labels.put(n, pickC(rng));
        }
        for (Point n : i2List) labels.put(n, RoomIds.CORRIDOR_TURN);
        for (Point n : i3List) labels.put(n, RoomIds.INTERSECTION_3);
        for (Point n : i4List) labels.put(n, RoomIds.INTERSECTION_4);

        for (Point ip : i4List) {
            for (Point nb : adj.get(ip)) {
                String lbl = labels.get(nb);
                if (lbl == null || !(lbl.equals("C1") || lbl.equals("C2") || lbl.equals("C3"))) continue;
                List<Point> nAdj = new ArrayList<>(adj.get(nb));
                if (nAdj.size() != 2) continue;
                Point a0 = nAdj.get(0);
                Point a1 = nAdj.get(1);
                int adx = a1.x() - a0.x(), adz = a1.y() - a0.y();
                if (adx * (nb.x() - ip.x()) + adz * (nb.y() - ip.y()) == 0) {
                    if (adx != 0 && adz != 0) labels.put(nb, RoomIds.CORRIDOR_TURN);
                }
            }
        }
        // Règle cul-de-sac (conversation 3) : jamais au bout d'une ligne droite
        // (virage/intersection requis) — sinon rejet et retry amont.
        if (!DungeonConstraints.enforceDeadEndAfterTurn(labels, adj, rng)) return null;
        return labels;
    }

    static CampResult placeCampAndPath(Map<Point, Set<Point>> adj, Point porte2, Random rng) {
        Point parent = adj.get(porte2).iterator().next();
        int dx = porte2.x() - parent.x(), dy = porte2.y() - parent.y();

        int maxLen = 2 + rng.nextInt(4);
        int cx = porte2.x(), cy = porte2.y();
        boolean lastStraight = false;
        List<Point> pathCells = new ArrayList<>();
        Map<Point, Set<Point>> tmpAdj = DungeonTreeBuilder.copyAdj(adj);
        Point curTmp = porte2;

        for (int i = 0; i < maxLen; i++) {
            // Respecte DungeonAlgo.MAX_COLINEAR_RUN sur l'adj globale (tronc + M5 + gap + porte déjà posés)
            Point straight = new Point(cx + dx, cy + dy);
            boolean straightOk = !straight.isOutOfBounds() && !tmpAdj.containsKey(straight)
                    && DungeonConstraints.colinearRunAfterEdge(curTmp, straight, tmpAdj) <= DungeonAlgo.MAX_COLINEAR_RUN;
            boolean goStraight = straightOk && (lastStraight ? rng.nextBoolean() : rng.nextFloat() < 0.35f);
            int ndx = dx, ndy = dy;
            if (!goStraight) {
                int[][] perp = {{dy, -dx}, {-dy, dx}};
                boolean turned = false;
                int startP = rng.nextInt(2);
                for (int k = 0; k < 2; k++) {
                    int[] turn = perp[(startP + k) % 2];
                    Point t = new Point(cx + turn[0], cy + turn[1]);
                    if (t.isOutOfBounds() || tmpAdj.containsKey(t)) continue;
                    if (DungeonConstraints.colinearRunAfterEdge(curTmp, t, tmpAdj) > DungeonAlgo.MAX_COLINEAR_RUN) continue;
                    ndx = turn[0]; ndy = turn[1];
                    dx = ndx; dy = ndy;
                    turned = true;
                    break;
                }
                if (!turned) {
                    if (!straightOk) break;
                    ndx = dx; ndy = dy;
                    goStraight = true;
                }
            } else {
                ndx = dx; ndy = dy;
            }
            Point next = new Point(cx + ndx, cy + ndy);
            if (tmpAdj.containsKey(next) || next.isOutOfBounds()) break;
            if (DungeonConstraints.colinearRunAfterEdge(curTmp, next, tmpAdj) > DungeonAlgo.MAX_COLINEAR_RUN) break;
            tmpAdj.putIfAbsent(curTmp, new HashSet<>());
            tmpAdj.putIfAbsent(next, new HashSet<>());
            tmpAdj.get(curTmp).add(next);
            tmpAdj.get(next).add(curTmp);
            pathCells.add(next);
            curTmp = next;
            cx = next.x(); cy = next.y();
            lastStraight = goStraight;
        }
        // Besoin d'au moins 2 cellules de chemin vers le campement
        if (pathCells.size() < 2) return null;

        Point c1 = new Point(cx + dx, cy + dy);
        Point lastPath = pathCells.get(pathCells.size() - 1);
        if (c1.isOutOfBounds() || adj.containsKey(c1)
                || DungeonConstraints.colinearRunAfterEdge(lastPath, c1, tmpAdj) > DungeonAlgo.MAX_COLINEAR_RUN) {
            int[][] perp = {{dy, -dx}, {-dy, dx}};
            boolean okC = false;
            for (int[] turn : perp) {
                Point cand = new Point(cx + turn[0], cy + turn[1]);
                if (cand.isOutOfBounds() || adj.containsKey(cand) || tmpAdj.containsKey(cand)) continue;
                if (DungeonConstraints.colinearRunAfterEdge(lastPath, cand, tmpAdj) > DungeonAlgo.MAX_COLINEAR_RUN) continue;
                c1 = cand; dx = turn[0]; dy = turn[1]; okC = true; break;
            }
            if (!okC) return null;
        }
        Point c2 = c1.move(dx, dy);
        int pex = dy, pey = -dx;
        Point c3 = c2.move(pex, pey);
        Point c4 = c3.move(-dx, -dy);
        Point cex = c3.move(dx, dy);

        for (Point k : Arrays.asList(c1, c2, c3, c4, cex)) {
            if (adj.containsKey(k) || k.isOutOfBounds()) return null;
        }

        Set<Point> campPathSet = new HashSet<>();
        Point cur = porte2;
        for (Point cell : pathCells) {
            campPathSet.add(cell);
            adj.put(cell, new HashSet<>()); adj.get(cell).add(cur); adj.get(cur).add(cell); cur = cell;
        }
        adj.put(c1, new HashSet<>()); adj.get(cur).add(c1); adj.get(c1).add(cur);
        adj.put(c2, new HashSet<>()); adj.get(c1).add(c2); adj.get(c2).add(c1);
        adj.put(c3, new HashSet<>()); adj.get(c2).add(c3); adj.get(c3).add(c2);
        adj.put(c4, new HashSet<>()); adj.get(c3).add(c4); adj.get(c4).add(c3);
        adj.put(cex, new HashSet<>()); adj.get(c3).add(cex); adj.get(cex).add(c3);

        CampResult cr = new CampResult();
        cr.campExit = cex; cr.campPathSet = campPathSet;
        cr.campNodes = Map.of(RoomIds.CAMP_1, c1, RoomIds.CAMP_2, c2, RoomIds.CAMP_3, c3, RoomIds.CAMP_4, c4);
        return cr;
    }


    private static Point findPointByValue(Map<Point, String> map, String value) {
        if (map == null || value == null) return null;
        for (var e : map.entrySet()) if (value.equals(e.getValue())) return e.getKey();
        return null;
    }

    private static String pickC(Random rng) {
        return RoomPools.CORRIDORS_P1_P2.get(rng.nextInt(3));
    }

    private static boolean isStraight(Point p1, Point p2) {
        return p1.x() == p2.x() || p1.y() == p2.y();
    }


    static TreeResult generatePart2Tree(Point startPoint, Set<Point> blocked, Random rng) {
        return generatePart2TrunkTree(startPoint, blocked, rng);
    }

    static TreeResult generatePart2TrunkTree(Point startPt, Set<Point> blocked, Random rng) {
        Map<Point, Set<Point>> adj = new HashMap<>();
        Set<Point> occupied = new HashSet<>(blocked != null ? blocked : Set.of());

        Point start = startPt != null ? startPt : new Point(DungeonAlgo.GRID_SIZE / 2, DungeonAlgo.GRID_SIZE / 2);
        adj.put(start, new HashSet<>());
        occupied.add(start);

        List<int[]> dirs = new ArrayList<>(Arrays.asList(DungeonAlgo.DIR_OFFSET));
        Collections.shuffle(dirs, rng);
        int dir = -1;
        for (int[] d : dirs) {
            Point target = start.move(d);
            if (!target.isOutOfBounds() && !occupied.contains(target)) {
                for (int i = 0; i < 4; i++) {
                    if (DungeonAlgo.DIR_OFFSET[i][0] == d[0] && DungeonAlgo.DIR_OFFSET[i][1] == d[1]) { dir = i; break; }
                }
                break;
            }
        }
        if (dir < 0) {
            TreeResult tr = new TreeResult();
            tr.startPoint = start; tr.startKey = start.key();
            tr.startX = start.x(); tr.startY = start.y(); tr.adj = adj;
            return tr;
        }

        Point c = start.move(DungeonAlgo.DIR_OFFSET[dir]);
        DungeonTreeBuilder.addEdge(adj, occupied, start, c);
        List<Point> trunkCells = new ArrayList<>();
        trunkCells.add(c);

        int trunkTarget = DungeonAlgo.PART2_TRUNK_MIN + rng.nextInt(DungeonAlgo.PART2_TRUNK_MAX - DungeonAlgo.PART2_TRUNK_MIN + 1);

        for (int t = 1; t < trunkTarget; t++) {
            // Run colinéaire dans l'ADJ si on continue dans `dir` (traverse I3/I4)
            int runIfStraight = DungeonConstraints.colinearRunAfterEdge(c, c.move(DungeonAlgo.DIR_OFFSET[dir]), adj);
            boolean forceTurn = runIfStraight > DungeonAlgo.PART2_MAX_COLINEAR_RUN;
            boolean maybeTurn = runIfStraight >= 2 && rng.nextFloat() < 0.40f;
            if (forceTurn || maybeTurn) {
                dir = (dir + (rng.nextBoolean() ? 1 : 3)) % 4;
            }

            Point n = c.move(DungeonAlgo.DIR_OFFSET[dir]);
            if (n.isOutOfBounds() || occupied.contains(n)
                    || DungeonConstraints.colinearRunAfterEdge(c, n, adj) > DungeonAlgo.PART2_MAX_COLINEAR_RUN) {
                int od = dir;
                boolean found = false;
                for (int a = 0; a < 4; a++) {
                    int td = (od + a) % 4;
                    Point cand = c.move(DungeonAlgo.DIR_OFFSET[td]);
                    if (cand.isOutOfBounds() || occupied.contains(cand)) continue;
                    if (DungeonConstraints.colinearRunAfterEdge(c, cand, adj) > DungeonAlgo.PART2_MAX_COLINEAR_RUN) continue;
                    dir = td; n = cand; found = true; break;
                }
                if (!found) break;
            }

            DungeonTreeBuilder.addEdge(adj, occupied, c, n);
            trunkCells.add(n);
            c = n;

            // Branches latérales (pas sur la toute fin : réservée pour l'approche de porte2)
            // growMiniTree peut allonger une droite via une I3 : on borne après coup
            if (t < trunkTarget - 3 && rng.nextFloat() < 0.55f) {
                int pDir = (dir + (rng.nextBoolean() ? 1 : 3)) % 4;
                growMiniTreeBounded(n, pDir, adj, occupied, rng);
            }
        }

        Point trunkEnd = trunkCells.isEmpty() ? start : trunkCells.get(trunkCells.size() - 1);
        if (!trunkCells.isEmpty()) {
            int side = (dir + (rng.nextBoolean() ? 1 : 3)) % 4;
            growMiniTreeBounded(trunkEnd, side, adj, occupied, rng);
        }

        // Remplissage : refuse toute arête qui créerait > MAX segments colinéaires
        // (compte dans l'adj réelle, y compris à travers les intersections)
        int targetSize = DungeonAlgo.PART2_TARGET_MIN + rng.nextInt(DungeonAlgo.PART2_TARGET_MAX - DungeonAlgo.PART2_TARGET_MIN + 1);
        int ci3 = 0, ci4 = 0;
        for (Set<Point> nb : adj.values()) {
            int d = nb.size();
            if (d == 3) ci3++; else if (d == 4) ci4++;
        }
        while (adj.size() < targetSize) {
            List<Point[]> candidates = new ArrayList<>();
            for (Point node : adj.keySet()) {
                if (node.equals(start) || node.equals(trunkEnd)) continue;
                int deg = adj.get(node).size();
                if (deg >= 4) continue;
                if (deg == 2 && ci3 >= DungeonAlgo.PART2_MAX_I3) continue;
                if (deg == 3 && ci4 >= 1) continue;
                for (int[] d : DungeonAlgo.DIR_OFFSET) {
                    Point next = node.move(d);
                    if (next.isOutOfBounds() || occupied.contains(next)) continue;
                    if (DungeonConstraints.colinearRunAfterEdge(node, next, adj) > DungeonAlgo.PART2_MAX_COLINEAR_RUN) continue;
                    int w = trunkCells.contains(node) ? 2 : 1;
                    for (int wi = 0; wi < w; wi++) candidates.add(new Point[]{node, next});
                }
            }
            if (candidates.isEmpty()) break;
            Point[] choice = candidates.get(rng.nextInt(candidates.size()));
            DungeonTreeBuilder.addEdge(adj, occupied, choice[0], choice[1]);
            int nd = adj.get(choice[0]).size();
            if (nd == 3) ci3++; else if (nd == 4) ci4++;
        }

        TreeResult tr = new TreeResult();
        tr.startPoint = start; tr.startKey = start.key();
        tr.startX = start.x(); tr.startY = start.y(); tr.adj = adj;
        tr.trunkEnd = trunkEnd;
        tr.trunkEndDir = dir;
        return tr;
    }

    static void growMiniTreeBounded(Point root, int pDir, Map<Point, Set<Point>> adj,
                                            Set<Point> occupied, Random rng) {
        Point n = root.move(DungeonAlgo.DIR_OFFSET[pDir]);
        if (n.isOutOfBounds() || occupied.contains(n)) return;
        if (DungeonConstraints.colinearRunAfterEdge(root, n, adj) > DungeonAlgo.PART2_MAX_COLINEAR_RUN) return;

        DungeonTreeBuilder.addEdge(adj, occupied, root, n);
        int cDir = pDir; Point c = n;
        int branchLen = 1 + rng.nextInt(3);

        for (int i = 0; i < branchLen; i++) {
            // Virage si la poursuite droite dépasserait la limite géométrique
            boolean mustTurn = DungeonConstraints.colinearRunAfterEdge(c, c.move(DungeonAlgo.DIR_OFFSET[cDir]), adj) > DungeonAlgo.PART2_MAX_COLINEAR_RUN;
            if (mustTurn || rng.nextFloat() < 0.4f) {
                cDir = (cDir + (rng.nextBoolean() ? 1 : 3)) % 4;
            }
            Point nn = c.move(DungeonAlgo.DIR_OFFSET[cDir]);
            if (nn.isOutOfBounds() || occupied.contains(nn)) break;
            if (DungeonConstraints.colinearRunAfterEdge(c, nn, adj) > DungeonAlgo.PART2_MAX_COLINEAR_RUN) {
                // tenter un virage
                boolean ok = false;
                for (int side : new int[]{1, 3}) {
                    int td = (cDir + side) % 4;
                    Point cand = c.move(DungeonAlgo.DIR_OFFSET[td]);
                    if (cand.isOutOfBounds() || occupied.contains(cand)) continue;
                    if (DungeonConstraints.colinearRunAfterEdge(c, cand, adj) > DungeonAlgo.PART2_MAX_COLINEAR_RUN) continue;
                    cDir = td; nn = cand; ok = true; break;
                }
                if (!ok) break;
            }
            DungeonTreeBuilder.addEdge(adj, occupied, c, nn);
            c = nn;
        }
    }

    static Point appendP2ExitSequence(Map<Point, Set<Point>> adj, Map<Point, String> labels,
                                              Point trunkEnd, int trunkEndDir, Set<Point> blocked,
                                              Random rng) {
        if (trunkEnd == null || trunkEndDir < 0) return null;
        Set<Point> occupied = new HashSet<>(adj.keySet());
        if (blocked != null) occupied.addAll(blocked);

        int dir = trunkEndDir;
        Point cursor = trunkEnd;
        List<Point> chain = new ArrayList<>(); // cellules intermédiaires (sans la porte)
        int gap = 1 + rng.nextInt(2); // 1 ou 2 cellules d'approche avant la porte
        int need = gap; // uniquement le gap, plus de cellule spéciale

        // Pose la chaîne d'approche + porte EN LIGNE (ou avec virages deg2 uniquement).
        // Chaque nœud de la chaîne (sauf la porte leaf) doit finir en deg 2 → C ou I2.
        for (int i = 0; i < need + 1; i++) {
            Point next = null;
            int chosenDir = -1;
            // Pour l'approche, on autorise droit ou virage, mais PAS de 3ᵉ voisin plus tard.
            // On pose d'abord tout droit pour garantir deg2, virage optionnel entre cellules.
            int[] tryOrder;
            if (i == 0) {
                // 1re cellule : continuer dans l'axe du tronc en priorité
                tryOrder = new int[]{dir, (dir + 1) % 4, (dir + 3) % 4, (dir + 2) % 4};
            } else {
                // approche / porte : droit ou virage (I2 ok), jamais forcer un I3
                tryOrder = new int[]{dir, (dir + 1) % 4, (dir + 3) % 4};
            }
            for (int td : tryOrder) {
                Point cand = cursor.move(DungeonAlgo.DIR_OFFSET[td]);
                if (cand.isOutOfBounds() || occupied.contains(cand) || adj.containsKey(cand)) continue;
                // Limite géométrique adj : pas plus de DungeonAlgo.MAX_COLINEAR_RUN segments alignés
                // (I3/I4 sur l'axe comptés dans la droite visuelle)
                if (DungeonConstraints.colinearRunAfterEdge(cursor, cand, adj) > DungeonAlgo.MAX_COLINEAR_RUN) continue;
                next = cand; chosenDir = td; break;
            }
            if (next == null) return null;

            adj.putIfAbsent(cursor, new HashSet<>());
            adj.putIfAbsent(next, new HashSet<>());
            adj.get(cursor).add(next);
            adj.get(next).add(cursor);
            occupied.add(next);
            dir = chosenDir;
            cursor = next;

            if (i < need) {
                chain.add(next);
            } else {
                // Porte2 = leaf (deg 1)
                labels.put(next, RoomIds.DOOR_2);
                // Vérifier géométrie : chaque cellule de la chaîne doit être deg 2
                // (approche = couloir droit ou virage, JAMAIS intersection)
                for (Point gp : chain) {
                    if (adj.getOrDefault(gp, Set.of()).size() != 2) return null;
                }
                // Labels : couloirs génériques C/I2 selon adjacence (plus de M5 ici —
                // la M5 de P2 se pose ailleurs sur une ligne droite, dans analyzePart2).
                for (Point gp : chain) {
                    Shape sh = DungeonLabels.shapeOf(adj.get(gp));
                    if (sh != Shape.STRAIGHT && sh != Shape.TURN) return null;
                    labels.put(gp, DungeonLabels.shapeLabel(sh, Theme.P12, rng));
                }
                return next;
            }
        }
        return null;
    }
}
