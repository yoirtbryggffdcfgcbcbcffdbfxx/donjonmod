package com.dungeonmod.debug;

import com.dungeonmod.debug.DungeonAlgo.Point;
import com.dungeonmod.debug.DungeonAlgo.Shape;
import com.dungeonmod.debug.DungeonAlgo.Theme;
import com.dungeonmod.debug.DungeonAlgo.TreeResult;

import java.util.*;

final class DungeonPart3 {
    private DungeonPart3() {}

    private static void removeNodesClean(Map<Point, Set<Point>> adj, Collection<Point> nodes) {
        for (Point n : nodes) {
            Set<Point> ns = adj.remove(n);
            if (ns != null) for (Point nb : ns) { Set<Point> back = adj.get(nb); if (back != null) back.remove(n); }
        }
    }

    private static void growMiniTree(Point root, int pDir, Map<Point, Set<Point>> adj, Set<Point> occupied, Random rng) {
        DungeonPart2.growMiniTreeBounded(root, pDir, adj, occupied, rng);
    }

    private static void growSplitBranches(Point e, Map<Point, Set<Point>> adj, Set<Point> occupied, Random rng) {
        List<Integer> ad = new ArrayList<>();
        for (int d = 0; d < 4; d++) {
            Point n = e.move(DungeonAlgo.DIR_OFFSET[d]);
            if (!n.isOutOfBounds() && !occupied.contains(n)) {
                boolean isBack = adj.get(e).stream().anyMatch(nb -> nb.equals(n));
                if (!isBack) ad.add(d);
            }
        }
        Collections.shuffle(ad, rng);
        for (int b = 0; b < Math.min(2, ad.size()); b++) {
            int bDir = ad.get(b); Point bk = e.move(DungeonAlgo.DIR_OFFSET[bDir]);
            if (DungeonConstraints.colinearRunAfterEdge(e, bk, adj) > DungeonAlgo.MAX_COLINEAR_RUN) continue;
            DungeonTreeBuilder.addEdge(adj, occupied, e, bk);
            int bl = 3 + rng.nextInt(4); int cDir = bDir; Point c = bk;
            for (int s = 0; s < bl; s++) {
                boolean mustTurn = DungeonConstraints.colinearRunAfterEdge(c, c.move(DungeonAlgo.DIR_OFFSET[cDir]), adj) > DungeonAlgo.MAX_COLINEAR_RUN;
                if (mustTurn || rng.nextFloat() < 0.3f) cDir = (cDir + (rng.nextBoolean() ? 1 : 3)) % 4;
                Point nn = c.move(DungeonAlgo.DIR_OFFSET[cDir]);
                if (nn.isOutOfBounds() || occupied.contains(nn)) break;
                if (DungeonConstraints.colinearRunAfterEdge(c, nn, adj) > DungeonAlgo.MAX_COLINEAR_RUN) {
                    boolean ok = false;
                    for (int side : new int[]{1, 3}) {
                        int td = (cDir + side) % 4; Point cand = c.move(DungeonAlgo.DIR_OFFSET[td]);
                        if (cand.isOutOfBounds() || occupied.contains(cand)) continue;
                        if (DungeonConstraints.colinearRunAfterEdge(c, cand, adj) > DungeonAlgo.MAX_COLINEAR_RUN) continue;
                        cDir = td; nn = cand; ok = true; break;
                    }
                    if (!ok) break;
                }
                DungeonTreeBuilder.addEdge(adj, occupied, c, nn); c = nn;
            }
        }
    }

    private static TreeResult generateTrunkTree(int targetMin, int targetMax, Point startPt, Set<Point> blocked, int maxI3, int maxI4, Random rng) {
        Map<Point, Set<Point>> adj = new HashMap<>();
        Set<Point> occupied = new HashSet<>(blocked != null ? blocked : new HashSet<>());
        Point start = startPt != null ? startPt : new Point(DungeonAlgo.GRID_SIZE / 2, DungeonAlgo.GRID_SIZE / 2);
        adj.put(start, new HashSet<>()); occupied.add(start);
        List<int[]> dirs = new ArrayList<>(Arrays.asList(DungeonAlgo.DIR_OFFSET));
        Collections.shuffle(dirs, rng);
        int dir = -1;
        for (int[] d : dirs) {
            Point target = start.move(d);
            if (!target.isOutOfBounds() && !occupied.contains(target)) {
                for (int i = 0; i < 4; i++) if (DungeonAlgo.DIR_OFFSET[i][0] == d[0] && DungeonAlgo.DIR_OFFSET[i][1] == d[1]) { dir = i; break; }
                break;
            }
        }
        if (dir < 0) { TreeResult tr = new TreeResult(); tr.startPoint = start; tr.startKey = start.key(); tr.startX = start.x(); tr.startY = start.y(); tr.adj = adj; return tr; }
        Point c = start.move(DungeonAlgo.DIR_OFFSET[dir]); DungeonTreeBuilder.addEdge(adj, occupied, start, c);
        List<Point> trunkCells = new ArrayList<>(); trunkCells.add(c);
        int trunkTarget = 8 + rng.nextInt(5);
        for (int t = 1; t < trunkTarget; t++) {
            int runIfStraight = DungeonConstraints.colinearRunAfterEdge(c, c.move(DungeonAlgo.DIR_OFFSET[dir]), adj);
            boolean forceTurn = runIfStraight > DungeonAlgo.MAX_COLINEAR_RUN;
            boolean maybeTurn = runIfStraight >= 2 && rng.nextFloat() < 0.35f;
            if (forceTurn || maybeTurn) dir = (dir + (rng.nextBoolean() ? 1 : 3)) % 4;
            Point n = c.move(DungeonAlgo.DIR_OFFSET[dir]);
            if (n.isOutOfBounds() || occupied.contains(n) || DungeonConstraints.colinearRunAfterEdge(c, n, adj) > DungeonAlgo.MAX_COLINEAR_RUN) {
                int od = dir; boolean found = false;
                for (int a = 0; a < 4; a++) {
                    int td = (od + a) % 4; Point cand = c.move(DungeonAlgo.DIR_OFFSET[td]);
                    if (cand.isOutOfBounds() || occupied.contains(cand)) continue;
                    if (DungeonConstraints.colinearRunAfterEdge(c, cand, adj) > DungeonAlgo.MAX_COLINEAR_RUN) continue;
                    dir = td; n = cand; found = true; break;
                }
                if (!found) break;
            }
            DungeonTreeBuilder.addEdge(adj, occupied, c, n); trunkCells.add(n); c = n;
            if (t < trunkTarget - 1 && rng.nextFloat() < 0.5) { int pDir = (dir + (rng.nextBoolean() ? 1 : 3)) % 4; growMiniTree(n, pDir, adj, occupied, rng); }
        }
        Point endPoint = trunkCells.get(trunkCells.size() - 1); growSplitBranches(endPoint, adj, occupied, rng);
        int targetSize = targetMin + rng.nextInt(targetMax - targetMin + 1);
        int ci3 = 0, ci4 = 0;
        for (Set<Point> nb : adj.values()) { int d = nb.size(); if (d == 3) ci3++; else if (d == 4) ci4++; }
        while (adj.size() < targetSize) {
            List<Point[]> candidates = new ArrayList<>();
            for (Point node : adj.keySet()) {
                if (node.equals(start)) continue;
                int deg = adj.get(node).size(); if (deg >= 4) continue;
                if (deg == 2 && ci3 >= maxI3) continue;
                if (deg == 3 && ci4 >= maxI4) continue;
                for (int[] d : DungeonAlgo.DIR_OFFSET) {
                    Point next = node.move(d);
                    if (next.isOutOfBounds() || occupied.contains(next)) continue;
                    if (DungeonConstraints.colinearRunAfterEdge(node, next, adj) > DungeonAlgo.MAX_COLINEAR_RUN) continue;
                    if (deg >= 2) {
                        boolean skip = false;
                        for (Point n1 : adj.get(node)) for (Point n2 : adj.get(node)) {
                            if (n1.equals(n2)) continue;
                            if (node.x() - n1.x() == n2.x() - node.x() && node.y() - n1.y() == n2.y() - node.y()) {
                                Set<Point> a1 = adj.get(n1), a2 = adj.get(n2);
                                if ((a1 != null && a1.size() >= 3) || (a2 != null && a2.size() >= 3)) { skip = true; break; }
                            }
                        }
                        if (skip) continue;
                    }
                    int w = trunkCells.contains(node) ? 3 : 1;
                    for (int wi = 0; wi < w; wi++) candidates.add(new Point[]{node, next});
                }
            }
            if (candidates.isEmpty()) break;
            Point[] choice = candidates.get(rng.nextInt(candidates.size()));
            DungeonTreeBuilder.addEdge(adj, occupied, choice[0], choice[1]);
            int nd = adj.get(choice[0]).size(); if (nd == 3) ci3++; else if (nd == 4) ci4++;
        }
        TreeResult tr = new TreeResult(); tr.startPoint = start; tr.startKey = start.key(); tr.startX = start.x(); tr.startY = start.y(); tr.adj = adj; return tr;
    }

    private static boolean isStraight(Point p1, Point p2) { return p1.x() == p2.x() || p1.y() == p2.y(); }

    private static boolean validatePart3SpecialShapes(Map<Point, RoomType> labels, Map<Point, Set<Point>> adj) {
        for (var e : labels.entrySet()) {
            RoomType label = e.getValue();
            if (label == null) continue;
            Shape shape = DungeonLabels.shapeOf(adj.getOrDefault(e.getKey(), Set.of()));
            if (label == RoomType.SHOP || label == RoomType.GARDEN || label == RoomType.STATUE
                    || label == RoomType.LOOT_DJ_1 || label == RoomType.LOOT_DJ_3
                    || label == RoomType.MONSTER_DJ_1 || label == RoomType.MONSTER_DJ_3 || label == RoomType.MONSTER_DJ_5) {
                if (shape != Shape.DEAD_END) return false;
            } else if (label == RoomType.LOOT_DJ_2 || label == RoomType.MONSTER_DJ_2 || label == RoomType.MONSTER_DJ_4
                    || label == RoomType.WELL_DJ) {
                if (shape != Shape.STRAIGHT) return false;
            }
        }
        return true;
    }

    static TreeResult generatePart3Tree(Point startPoint, Set<Point> blocked, Random rng) {
        return generateTrunkTree(35, 45, startPoint, blocked, DungeonAlgo.PART3_MAX_IJ3, DungeonAlgo.PART3_MAX_IJ4, rng);
    }

    static Map<Point, RoomType> analyzePart3(Map<Point, Set<Point>> adj, Point campExit,
                                                     Map<Point, RoomType> existingLabels, Random rng) {
        DungeonLabelState labelState = new DungeonLabelState();
        labelState.putSpecials(existingLabels);
        labelState.setTheme(campExit, Theme.DJ);
        Map<Point, RoomType> specials = labelState.specials();

        Set<Point> allLabeled = new HashSet<>(specials.keySet());
        allLabeled.add(campExit);
        List<Point> bfsP3 = new ArrayList<>();
        Set<Point> seen = new HashSet<>(); Queue<Point> q = new LinkedList<>();
        q.add(campExit); seen.add(campExit);
        while (!q.isEmpty()) {
            Point n = q.poll();
            if (!allLabeled.contains(n)) bfsP3.add(n);
            for (Point nb : adj.get(n)) { if (!seen.contains(nb)) { seen.add(nb); q.add(nb); } }
        }
        labelState.setTheme(bfsP3, Theme.DJ);

        Point[] bibNodes = null;
        List<Point> shuffled = new ArrayList<>(bfsP3); Collections.shuffle(shuffled, rng);
        for (Point src : shuffled) {
            for (int[] d : DungeonAlgo.DIR_OFFSET) {
                Point[] chainKeys = new Point[2]; boolean chainOk = true; Point curr = src;
                for (int i = 0; i < 2; i++) {
                    Point next = curr.move(d);
                    if (next.isOutOfBounds() || adj.containsKey(next)) { chainOk = false; break; }
                    chainKeys[i] = next; curr = next;
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

        Point shopNode = null;
        shuffled = new ArrayList<>(bfsP3); Collections.shuffle(shuffled, rng);
        for (Point p : shuffled) {
            for (int[] d : DungeonAlgo.DIR_OFFSET) {
                Point next = p.move(d);
                if (!next.isOutOfBounds() && !adj.containsKey(next)) {
                    adj.put(next, new HashSet<>()); adj.get(p).add(next); adj.get(next).add(p);
                    shopNode = next; break;
                }
            }
            if (shopNode != null) break;
        }
        if (shopNode == null) return null;

        Set<Point> allNodes = new HashSet<>(adj.keySet());
        List<Point> leaves = new ArrayList<>(), internals = new ArrayList<>();
        for (Point n : allNodes) {
            if (n.equals(campExit)) continue;
            if (adj.get(n).size() == 1 && !labelState.hasSpecial(n)) leaves.add(n);
            else if (adj.get(n).size() >= 2 && !labelState.hasSpecial(n)) internals.add(n);
        }

        List<Point> cjList = new ArrayList<>(), ij2List = new ArrayList<>(), ij3List = new ArrayList<>(), ij4List = new ArrayList<>();
        for (Point node : internals) {
            int deg = adj.get(node).size();
            if (deg == 2) { List<Point> nb = new ArrayList<>(adj.get(node)); if (isStraight(nb.get(0), nb.get(1))) cjList.add(node); else ij2List.add(node); }
            else if (deg == 3) ij3List.add(node); else if (deg == 4) ij4List.add(node);
        }

        labelState.putSpecial(bibNodes[0], RoomType.BIB_1); labelState.putSpecial(bibNodes[1], RoomType.BIB_2);
        Point b2 = bibNodes[1]; Point b1 = bibNodes[0];
        int ddx = b2.x() - b1.x(), ddz = b2.y() - b1.y();
        int cdx, cdz;
        if (ddx == 1 && ddz == 0) { cdx = 0; cdz = 1; }
        else if (ddx == -1 && ddz == 0) { cdx = 0; cdz = -1; }
        else if (ddx == 0 && ddz == 1) { cdx = -1; cdz = 0; }
        else if (ddx == 0 && ddz == -1) { cdx = 1; cdz = 0; }
        else { cdx = 0; cdz = 1; }

        boolean hubOk = false;
        for (int corridorLen : new int[]{5, 6, 7}) {
            if (hubOk) break;
            int cx = b2.x(), cz = b2.y(); Point prev = bibNodes[1]; List<Point> corrNodes = new ArrayList<>();
            boolean ok = true; int mid = corridorLen / 2;
            for (int i = 0; i < corridorLen && ok; i++) {
                if (i == mid) {
                    int[][] perp = {{ddx, ddz}, {-ddx, -ddz}}; int ti = rng.nextInt(2);
                    Point t = new Point(cx + perp[ti][0], cz + perp[ti][1]);
                    if (adj.containsKey(t) || t.isOutOfBounds()) { ti = ti == 0 ? 1 : 0; t = new Point(cx + perp[ti][0], cz + perp[ti][1]); }
                    if (!adj.containsKey(t) && !t.isOutOfBounds()) { corrNodes.add(t); adj.put(t, new HashSet<>()); adj.get(t).add(prev); adj.get(prev).add(t); cx = t.x(); cz = t.y(); prev = t; }
                    else { ok = false; break; }
                }
                Point next = new Point(cx + cdx, cz + cdz);
                if (adj.containsKey(next) || next.isOutOfBounds()) { ok = false; break; }
                corrNodes.add(next); adj.put(next, new HashSet<>()); adj.get(next).add(prev); adj.get(prev).add(next); cx = next.x(); cz = next.y(); prev = next;
            }
            if (!ok || corrNodes.size() < 3) { removeNodesClean(adj, corrNodes); continue; }
            Point wKey = new Point(cx + 1, cz);
            if (adj.containsKey(wKey) || wKey.isOutOfBounds()) { removeNodesClean(adj, corrNodes); continue; }
            adj.put(wKey, new HashSet<>()); adj.get(wKey).add(prev); adj.get(prev).add(wKey); corrNodes.add(wKey);
            int hx2 = cx, hz2 = cz + 1;
            if (hz2 + 1 >= DungeonAlgo.GRID_SIZE) { removeNodesClean(adj, corrNodes); continue; }
            Point[] hubCells = {new Point(hx2, hz2), new Point(hx2+1, hz2), new Point(hx2, hz2+1), new Point(hx2+1, hz2+1)};
            boolean hubFree = true; for (Point hc : hubCells) { if (adj.containsKey(hc) || hc.isOutOfBounds()) { hubFree = false; break; } }
            if (!hubFree) { removeNodesClean(adj, corrNodes); continue; }
            Point hubKey = new Point(hx2, hz2); adj.put(hubKey, new HashSet<>()); adj.get(hubKey).add(wKey); adj.get(wKey).add(hubKey);
            labelState.setTheme(corrNodes, Theme.P12); labelState.putSpecial(hubKey, RoomType.CENTRALE);
            hubOk = true;
        }
        if (!hubOk) { bibNodes = null; return null; }
        cjList.remove(bibNodes[0]);
        if (shopNode != null) { labelState.putSpecial(shopNode, RoomType.SHOP); leaves.remove(shopNode); }
        leaves.remove(bibNodes[1]); leaves.remove(bibNodes[0]);

        List<RoomType> p3LootList = new ArrayList<>(RoomType.LOOT_P3_P4);
        Collections.shuffle(p3LootList, rng);
        RoomType p3LootA = p3LootList.get(0), p3LootB = p3LootList.get(1);
        int leafLoot, corrLoot;
        if (p3LootA == RoomType.LOOT_DJ_2 || p3LootB == RoomType.LOOT_DJ_2) { leafLoot = 1; corrLoot = 1; }
        else { leafLoot = 2; corrLoot = 0; }

        int targetM3 = 4 + rng.nextInt(3);
        int availLeafM = Math.max(0, leaves.size() - leafLoot - 2);
        if (availLeafM + cjList.size() < targetM3 || leaves.size() < leafLoot + 2) return null;
        int leafM3 = Math.min(targetM3, availLeafM);
        int corrM3 = targetM3 - leafM3;
        if (corrM3 > cjList.size()) { corrM3 = cjList.size(); leafM3 = targetM3 - corrM3; }

        Collections.shuffle(leaves, rng); int li = leafLoot;
        Map<Point, RoomType> p3LootAssign = new HashMap<>();
        if (leafLoot == 2) { p3LootAssign.put(leaves.get(0), p3LootA); p3LootAssign.put(leaves.get(1), p3LootB); }
        else { RoomType leafType = p3LootA == RoomType.LOOT_DJ_2 ? p3LootB : p3LootA; p3LootAssign.put(leaves.get(0), leafType); }
        for (var e : p3LootAssign.entrySet()) labelState.putSpecial(e.getKey(), e.getValue());

        Set<Point> mjSet = DungeonConstraints.monsterPoints(specials);
        int placedLeafM3 = 0;
        while (placedLeafM3 < leafM3 && li < leaves.size()) {
            Point cand = leaves.get(li++);
            if (!DungeonConstraints.isFarFromAll(adj, cand, mjSet, DungeonAlgo.MONSTER_MIN_DIST)) continue;
            labelState.putSpecial(cand, RoomType.LEAF_MONSTERS_P3_P4.get(rng.nextInt(3))); mjSet.add(cand); placedLeafM3++;
        }
        if (placedLeafM3 < leafM3) return null;

        List<Point> restLeaves = new ArrayList<>();
        for (int i = leafLoot; i < leaves.size(); i++) if (!labelState.hasSpecial(leaves.get(i))) restLeaves.add(leaves.get(i));
        restLeaves.sort(Comparator.comparingInt(lp -> -Math.abs(lp.x() - campExit.x()) - Math.abs(lp.y() - campExit.y())));
        if (restLeaves.size() < 2) return null;
        labelState.putSpecial(restLeaves.get(0), RoomType.GARDEN);
        labelState.putSpecial(restLeaves.get(1), RoomType.STATUE);

        if (cjList.size() < corrM3 + corrLoot + 1) return null;
        Collections.shuffle(cjList, rng);
        int mjPlaced = 0; List<Point> remCJ = new ArrayList<>();
        for (Point n : cjList) {
            if (mjPlaced < corrM3) {
                if (DungeonConstraints.isFarFromAll(adj, n, mjSet, DungeonAlgo.MONSTER_MIN_DIST)) { labelState.putSpecial(n, RoomType.CORRIDOR_MONSTERS_P3_P4.get(rng.nextInt(2))); mjSet.add(n); mjPlaced++; }
                else remCJ.add(n);
            } else remCJ.add(n);
        }
        int lc = 0;
        if (corrLoot == 1 && lc < remCJ.size()) { RoomType corrType = p3LootA == RoomType.LOOT_DJ_2 ? p3LootA : p3LootB; labelState.putSpecial(remCJ.get(lc), corrType); lc++; }
        if (remCJ.size() > lc) labelState.putSpecial(remCJ.get(lc), RoomType.WELL_DJ); else lc--;

        List<Point> genericOrder = new ArrayList<>();
        genericOrder.add(campExit); genericOrder.addAll(restLeaves); genericOrder.addAll(remCJ);
        genericOrder.addAll(ij2List); genericOrder.addAll(ij3List); genericOrder.addAll(ij4List);
        Map<Point, RoomType> finalLabels = labelState.buildLabels(adj, rng, genericOrder);

        if (!validatePart3SpecialShapes(finalLabels, adj)) return null;
        if (!DungeonConstraints.enforceDeadEndAfterTurn(finalLabels, adj, rng)) return null;
        if (!validatePart3SpecialShapes(finalLabels, adj)) return null;
        return finalLabels;
    }
}
