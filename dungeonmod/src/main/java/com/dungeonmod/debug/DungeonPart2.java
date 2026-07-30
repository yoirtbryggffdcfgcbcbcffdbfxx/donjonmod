package com.dungeonmod.debug;

import com.dungeonmod.debug.DungeonAlgo.CampResult;
import com.dungeonmod.debug.DungeonAlgo.Point;
import com.dungeonmod.debug.DungeonAlgo.RoomIds;
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
                                                    Map<Point, String> existingLabels, Set<Point> pathSet, Random rng) {
        DungeonLabelState labelState = new DungeonLabelState();
        labelState.putSpecials(existingLabels); // labels déjà posés : P1 + taverne + approche porte2
        labelState.setTheme(exitPoint, Theme.P12);
        // Sortie de taverne : on conserve le timing historique de son choix de variante.
        labelState.putSpecial(exitPoint, DungeonLabels.labelForNeighbors(adj.get(exitPoint), Theme.P12, rng));
        Map<Point, String> specials = labelState.specials();

        Set<Point> allLabeled = new HashSet<>(specials.keySet());
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

        labelState.setTheme(p2Nodes, Theme.P12);

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
        Point existingPorte2 = findPointByValue(specials, RoomIds.DOOR_2);
        if (existingPorte2 != null) leaves.remove(existingPorte2);

        // Besoin : ogre + fontaine + m3 + m1 + loot ≥ 5 feuilles de branches.
        if (leaves.size() < 5) return null;
        // Et 3 couloirs droits LIBRES pour M2 + M4 + M5 (hors chemin taverne).
        long availCorr = cList.stream().filter(n -> !pathSet.contains(n) && !labelState.hasSpecial(n)).count();
        if (availCorr < 3) return null;

        // ESPACEMENT MONSTRES + ISOLEMENT OGRE (règles dures, conversation 3) : toute salle
        // monstre est à distance >= DungeonAlgo.MONSTER_MIN_DIST des autres ET à distance
        // >= DungeonAlgo.OGRE_MIN_MONSTER_DIST de l'Ogre. L'Ogre reste posé sur la feuille la plus
        // éloignée possible de la sortie de taverne. Infaisable => rejet (null => retry).
        Set<Point> monsters = DungeonConstraints.monsterPoints(specials); // M1-M4 hérités de P1
        Set<Point> monsterSet = new HashSet<>();

        List<Point> ogreCands = new ArrayList<>(leaves);
        // Tri par distance décroissante à la sortie : l'Ogre reste la feuille la plus éloignée.
        ogreCands.sort(Comparator.comparingInt((Point n) -> -dist.getOrDefault(n, 0)));
        Point ogreLeaf = null;
        for (Point n : ogreCands) {
            if (DungeonConstraints.isFarFromAll(adj, n, monsters, DungeonAlgo.OGRE_MIN_MONSTER_DIST)) { ogreLeaf = n; break; }
        }
        if (ogreLeaf == null) return null;
        labelState.putSpecial(ogreLeaf, RoomIds.OGRE); monsterSet.add(ogreLeaf);
        Map<Point, Integer> ogreDist = DungeonConstraints.bfsDistances(adj, ogreLeaf);
        List<Point> remain = new ArrayList<>(leaves);
        remain.remove(ogreLeaf);
        Collections.shuffle(remain, rng);

        if (remain.isEmpty()) return null;
        labelState.putSpecial(remain.get(0), RoomIds.FOUNTAIN);
        // porte2 déjà posée structurellement — ne pas la re-choisir ici

        Point m3Leaf = null;
        for (Point n : remain) {
            if (labelState.hasSpecial(n)) continue;
            if (ogreDist.getOrDefault(n, Integer.MAX_VALUE) < DungeonAlgo.OGRE_MIN_MONSTER_DIST) continue;
            if (!DungeonConstraints.isFarFromAll(adj, n, monsters, DungeonAlgo.MONSTER_MIN_DIST)) continue;
            m3Leaf = n; break;
        }
        if (m3Leaf == null) return null;
        labelState.putSpecial(m3Leaf, RoomIds.MONSTER_3); monsterSet.add(m3Leaf); monsters.add(m3Leaf);

        // M1 (feuille) : OBLIGATOIRE en P2, espacement respecté, sinon rejet (null => retry).
        Point m1Leaf = null;
        for (Point n : remain) {
            if (labelState.hasSpecial(n)) continue;
            if (ogreDist.getOrDefault(n, Integer.MAX_VALUE) < DungeonAlgo.OGRE_MIN_MONSTER_DIST) continue;
            if (!DungeonConstraints.isFarFromAll(adj, n, monsters, DungeonAlgo.MONSTER_MIN_DIST)) continue;
            m1Leaf = n; break;
        }
        if (m1Leaf == null) return null;
        labelState.putSpecial(m1Leaf, RoomIds.MONSTER_1); monsterSet.add(m1Leaf); monsters.add(m1Leaf);

        Point lootLeaf = null;
        for (Point n : remain) {
            if (labelState.hasSpecial(n)) continue;
            boolean hasAdj = false; for (Point nb : adj.get(n)) if (monsterSet.contains(nb)) { hasAdj = true; break; }
            if (!hasAdj) { lootLeaf = n; break; }
        }
        if (lootLeaf == null) { for (Point n : remain) { if (!labelState.hasSpecial(n)) { lootLeaf = n; break; } } }
        labelState.putSpecial(lootLeaf, RoomIds.LOOT_1);

        // Les feuilles restantes restent génériques : elles deviendront "cul" à la fin.

        // M4 (couloir droit) : OBLIGATOIRE en P2, espacement respecté, sinon rejet (null).
        Point m4Corr = null;
        for (Point n : cList) {
            if (pathSet.contains(n) || labelState.hasSpecial(n)) continue;
            if (ogreDist.getOrDefault(n, Integer.MAX_VALUE) < DungeonAlgo.OGRE_MIN_MONSTER_DIST) continue;
            if (!DungeonConstraints.isFarFromAll(adj, n, monsters, DungeonAlgo.MONSTER_MIN_DIST)) continue;
            m4Corr = n; break;
        }
        if (m4Corr == null) return null;
        labelState.putSpecial(m4Corr, RoomIds.MONSTER_4); monsterSet.add(m4Corr); monsters.add(m4Corr);

        // M2 (couloir droit) : OBLIGATOIRE en P2, espacement respecté, sinon rejet (null).
        Point m2Corr = null;
        for (Point n : cList) {
            if (pathSet.contains(n) || labelState.hasSpecial(n)) continue;
            if (ogreDist.getOrDefault(n, Integer.MAX_VALUE) < DungeonAlgo.OGRE_MIN_MONSTER_DIST) continue;
            if (!DungeonConstraints.isFarFromAll(adj, n, monsters, DungeonAlgo.MONSTER_MIN_DIST)) continue;
            m2Corr = n; break;
        }
        if (m2Corr == null) return null;
        labelState.putSpecial(m2Corr, RoomIds.MONSTER_2); monsterSet.add(m2Corr); monsters.add(m2Corr);

        // M5 (couloir droit) : n'est PLUS avant porte2 — elle est OBLIGATOIRE n'importe où
        // sur une ligne droite de la zone P2 (hors chemin taverne), espacement respecté,
        // sinon rejet (null => retry amont).
        Point m5Corr = null;
        for (Point n : cList) {
            if (pathSet.contains(n) || labelState.hasSpecial(n)) continue;
            if (ogreDist.getOrDefault(n, Integer.MAX_VALUE) < DungeonAlgo.OGRE_MIN_MONSTER_DIST) continue;
            if (!DungeonConstraints.isFarFromAll(adj, n, monsters, DungeonAlgo.MONSTER_MIN_DIST)) continue;
            m5Corr = n; break;
        }
        if (m5Corr == null) return null;
        labelState.putSpecial(m5Corr, RoomIds.MONSTER_5); monsterSet.add(m5Corr); monsters.add(m5Corr);

        List<Point> remC = new ArrayList<>();
        for (Point n : cList) { if (!labelState.hasSpecial(n)) remC.add(n); }
        if (!remC.isEmpty()) {
            Point pn = null;
            for (Point n : remC) {
                if (!pathSet.contains(n)) {
                    boolean adjSpecial = false;
                    for (Point nb : adj.get(n)) {
                        String lbl = specials.get(nb);
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
            labelState.putSpecial(pn, RoomIds.WELL);
            remC.remove(pn);
        }

        // Les labels génériques P2 (C/I/cul) sont construits à la fin depuis l'adj réelle.
        List<Point> genericOrder = new ArrayList<>();
        genericOrder.addAll(leaves);
        genericOrder.addAll(remC);
        genericOrder.addAll(i2List);
        genericOrder.addAll(i3List);
        genericOrder.addAll(i4List);
        Map<Point, String> finalLabels = labelState.buildLabels(adj, rng, genericOrder);


        // Règle cul-de-sac (conversation 3) : jamais au bout d'une ligne droite
        // (virage/intersection requis) — sinon rejet et retry amont.
        if (!validatePart2SpecialShapes(finalLabels, adj)) return null;
        if (!DungeonConstraints.enforceDeadEndAfterTurn(finalLabels, adj, rng)) return null;
        if (!validatePart2SpecialShapes(finalLabels, adj)) return null;
        return finalLabels;
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
            // La première cellule après porte2 doit continuer dans l'axe : la structure
            // "porte2" réutilise une porte/couloir droit, pas un virage. Si impossible,
            // on rejette ce layout et le retry amont régénère une approche compatible.
            if (i == 0 && !straightOk) return null;
            boolean goStraight = (i == 0) || (straightOk && (lastStraight ? rng.nextBoolean() : rng.nextFloat() < 0.35f));
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
        // Essayer les 4 orientations de la structure 2x2.
        DungeonCompositeRooms.Placement camp = null;
        int[] crdx = {dx, -dy, -dx, dy};
        int[] crdy = {dy, dx, -dy, -dx};
        for (int rot = 0; rot < 4 && camp == null; rot++) {
            int rdx = crdx[rot], rdy = crdy[rot];
            Point ct = new Point(cx + rdx, cy + rdy);
            if (ct.isOutOfBounds() || adj.containsKey(ct) || tmpAdj.containsKey(ct)) continue;
            camp = DungeonCompositeRooms.plan(adj, ct, rdx, rdy, DungeonCompositeRooms.CAMP);
        }
        if (camp == null) return null;

        Set<Point> campPathSet = new HashSet<>();
        Point cur = porte2;
        for (Point cell : pathCells) {
            campPathSet.add(cell);
            adj.put(cell, new HashSet<>()); adj.get(cell).add(cur); adj.get(cur).add(cell); cur = cell;
        }
        DungeonCompositeRooms.place(adj, cur, camp);

        CampResult cr = new CampResult();
        cr.campExit = camp.exitPoint(); cr.campPathSet = campPathSet;
        cr.campNodes = camp.labelPoints();
        return cr;
    }


    /** Vérifie que les specials posés par P2 sont compatibles avec leur forme géométrique. */
    private static boolean validatePart2SpecialShapes(Map<Point, String> labels, Map<Point, Set<Point>> adj) {
        for (var e : labels.entrySet()) {
            String label = e.getValue();
            Shape shape = DungeonLabels.shapeOf(adj.getOrDefault(e.getKey(), Set.of()));
            if (RoomIds.OGRE.equals(label) || RoomIds.FOUNTAIN.equals(label)
                    || RoomIds.MONSTER_1.equals(label) || RoomIds.MONSTER_3.equals(label)
                    || RoomIds.LOOT_1.equals(label)) {
                if (shape != Shape.DEAD_END) return false;
            } else if (RoomIds.MONSTER_2.equals(label) || RoomIds.MONSTER_4.equals(label)
                    || RoomIds.MONSTER_5.equals(label) || RoomIds.WELL.equals(label)) {
                if (shape != Shape.STRAIGHT) return false;
            }
        }
        return true;
    }

    private static Point findPointByValue(Map<Point, String> map, String value) {
        if (map == null || value == null) return null;
        for (var e : map.entrySet()) if (value.equals(e.getValue())) return e.getKey();
        return null;
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
                // Vérifier géométrie : chaque cellule de la chaîne doit être deg 2
                // (approche = couloir droit ou virage, JAMAIS intersection)
                for (Point gp : chain) {
                    if (adj.getOrDefault(gp, Set.of()).size() != 2) return null;
                    Shape sh = DungeonLabels.shapeOf(adj.get(gp));
                    if (sh != Shape.STRAIGHT && sh != Shape.TURN) return null;
                }
                // Même modèle que P1/P2 : les cellules d'approche sont génériques P12,
                // porte2 est un special, puis on construit les labels finaux du segment.
                DungeonLabelState labelState = new DungeonLabelState();
                labelState.setTheme(chain, Theme.P12);
                labelState.putSpecial(next, RoomIds.DOOR_2);
                labels.putAll(labelState.buildLabels(adj, rng, chain));
                return next;
            }
        }
        return null;
    }
}
