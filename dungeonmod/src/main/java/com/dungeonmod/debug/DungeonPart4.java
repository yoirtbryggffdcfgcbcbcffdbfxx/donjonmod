package com.dungeonmod.debug;

import com.dungeonmod.debug.DungeonAlgo.Point;
import com.dungeonmod.debug.DungeonAlgo.Theme;
import com.dungeonmod.debug.DungeonAlgo.TreeResult;

import java.util.*;

final class DungeonPart4 {
    private DungeonPart4() {}

    private static final int P4_MAX_IJ3 = 2;
    private static final int P4_MAX_IJ4 = 1;

    private static void putSpecial(DungeonLabelState labelState, Map<Point, RoomType> workingLabels,
                                   Point point, RoomType label) {
        labelState.putSpecial(point, label); workingLabels.put(point, label);
    }

    private static void putGeneric(DungeonLabelState labelState, Map<Point, RoomType> workingLabels,
                                   Point point, Theme theme, Set<Point> neighbors, Random rng) {
        labelState.setTheme(point, theme);
        workingLabels.put(point, DungeonLabels.labelForNeighbors(neighbors, theme, rng));
    }

    private static void putGenericWorking(DungeonLabelState labelState, Map<Point, RoomType> workingLabels,
                                          Point point, Theme theme, RoomType workingLabel) {
        labelState.setTheme(point, theme); workingLabels.put(point, workingLabel);
    }

    private static void labelTreeNodes(DungeonLabelState labelState, Map<Point, Set<Point>> tr,
                                       Map<Point, RoomType> topLabels, Point startPoint, Random rng) {
        List<Point> lf = new ArrayList<>(), co = new ArrayList<>(), i3 = new ArrayList<>(), i4 = new ArrayList<>();
        for (Point k : tr.keySet()) { int d = tr.get(k).size(); if (d == 1) lf.add(k); else if (d == 2) co.add(k); else if (d == 3) i3.add(k); else if (d == 4) i4.add(k); }
        if (tr.get(startPoint).size() == 1) putGenericWorking(labelState, topLabels, startPoint, Theme.DJ, RoomType.pickCJ(rng));
        else putGeneric(labelState, topLabels, startPoint, Theme.DJ, tr.get(startPoint), rng);
        for (Point k : i3) if (!topLabels.containsKey(k)) putGeneric(labelState, topLabels, k, Theme.DJ, tr.get(k), rng);
        for (Point k : i4) if (!topLabels.containsKey(k)) putGeneric(labelState, topLabels, k, Theme.DJ, tr.get(k), rng);
        for (Point k : co) if (!topLabels.containsKey(k)) putGeneric(labelState, topLabels, k, Theme.DJ, tr.get(k), rng);
        for (Point k : lf) if (!topLabels.containsKey(k)) putGeneric(labelState, topLabels, k, Theme.DJ, tr.get(k), rng);
        for (Point ip : i4) {
            for (Point nb : tr.get(ip)) {
                RoomType lbl = topLabels.get(nb);
                if (lbl == null || !lbl.isDjCorridor()) continue;
                List<Point> nAd = new ArrayList<>(tr.get(nb));
                if (nAd.size() != 2) continue;
                Point a0 = nAd.get(0), a1 = nAd.get(1);
                if ((a1.x() - a0.x()) * (nb.x() - ip.x()) + (a1.y() - a0.y()) * (nb.y() - ip.y()) == 0)
                    putGenericWorking(labelState, topLabels, nb, Theme.DJ, RoomType.IJ2);
            }
        }
    }

    private static boolean placeGoblinVillage(DungeonLabelState labelState, Map<Point, Set<Point>> adj,
                                               Map<Point, RoomType> topLabels, Map<Point, Set<Point>> gt,
                                               Set<Point> globalOccupied, Set<Point> goblinCells, Random rng) {
        Point gLeaf = null, firstGob = null, parent = null;
        for (Point testLeaf : new ArrayList<>(gt.keySet())) {
            RoomType v = topLabels.get(testLeaf);
            if (v == null || v != RoomType.CUL_DJ) continue;
            parent = gt.get(testLeaf).iterator().next();
            int gdx = testLeaf.x() - parent.x(), gdy = testLeaf.y() - parent.y();
            Point target = testLeaf.move(gdx, gdy);
            if (!target.isOutOfBounds() && !globalOccupied.contains(target)) { firstGob = target; gLeaf = testLeaf; break; }
        }
        if (gLeaf == null || firstGob == null) return false;
        putSpecial(labelState, topLabels, gLeaf, RoomType.GOBLIN_DOOR);
        Map<Point, Set<Point>> ga = new HashMap<>();
        ga.put(gLeaf, new HashSet<>()); globalOccupied.add(firstGob); ga.put(firstGob, new HashSet<>());
        ga.get(gLeaf).add(firstGob); ga.get(firstGob).add(gLeaf);
        TreeResult gobRaw = DungeonTreeBuilder.generateRawTree(20, 25, 3, 1, 2, firstGob, new HashSet<>(globalOccupied), DungeonAlgo.MAX_COLINEAR_RUN, rng);
        if (gobRaw.adj.size() < 6) return false;
        for (var e : gobRaw.adj.entrySet()) { ga.putIfAbsent(e.getKey(), new HashSet<>()); ga.get(e.getKey()).addAll(e.getValue()); }
        if (ga.get(gLeaf).size() > 2) {
            Point keepNb = null; for (Point nb : ga.get(gLeaf)) { if (!nb.equals(parent)) { keepNb = nb; break; } }
            for (Point nb : new ArrayList<>(ga.get(gLeaf))) { if (!nb.equals(parent) && !nb.equals(keepNb)) { ga.get(nb).remove(gLeaf); ga.get(gLeaf).remove(nb); } }
        }
        globalOccupied.addAll(ga.keySet()); labelState.setTheme(ga.keySet(), Theme.GOBLIN);
        List<Point> gl = new ArrayList<>(), gco = new ArrayList<>(), gi3 = new ArrayList<>(), gi4 = new ArrayList<>();
        for (Point k : ga.keySet()) { if (k.equals(gLeaf)) continue; int d = ga.get(k).size();
            if (d == 1) gl.add(k); else if (d == 2) gco.add(k); else if (d == 3) gi3.add(k); else if (d == 4) gi4.add(k); }
        for (Point k : gi3) if (!topLabels.containsKey(k)) putGeneric(labelState, topLabels, k, Theme.GOBLIN, ga.get(k), rng);
        for (Point k : gi4) if (!topLabels.containsKey(k)) putGeneric(labelState, topLabels, k, Theme.GOBLIN, ga.get(k), rng);
        for (Point k : gco) { List<Point> nb2 = new ArrayList<>(ga.get(k)); if (isStraight(nb2.get(0), nb2.get(1))) { putSpecial(labelState, topLabels, k, RoomType.GOBLIN_WELL); break; } }
        Collections.shuffle(gl, rng); int gli = 0;
        if (gli < gl.size()) { putSpecial(labelState, topLabels, gl.get(gli), RoomType.GOBLIN_MARCH); gli++; }
        if (gli < gl.size()) { putSpecial(labelState, topLabels, gl.get(gli), RoomType.GOBLIN_ARMORY); gli++; }
        if (gli < gl.size()) { putSpecial(labelState, topLabels, gl.get(gli), RoomType.GOBLIN_TREASURE); gli++; }
        for (Point k : gco) if (!topLabels.containsKey(k)) putGeneric(labelState, topLabels, k, Theme.GOBLIN, ga.get(k), rng);
        for (Point k : gl) if (!topLabels.containsKey(k)) putGeneric(labelState, topLabels, k, Theme.GOBLIN, ga.get(k), rng);
        List<Point> hl = new ArrayList<>();
        for (Point k : ga.keySet()) { if (k.equals(gLeaf)) continue;
            RoomType v = topLabels.get(k);
            if (v != null && (v == RoomType.CG1 || v == RoomType.GI2 || v == RoomType.CDG || v == RoomType.GI3 || v == RoomType.GI4)) hl.add(k); }
        Collections.shuffle(hl, rng); Set<Point> hs = new HashSet<>(); int hMax = 3 + rng.nextInt(3);
        for (Point k : hl) { if (hs.size() >= hMax) break;
            int dk = ga.get(k).size(); if (dk == 2) { List<Point> nb2 = new ArrayList<>(ga.get(k)); if (isStraight(nb2.get(0), nb2.get(1))) continue; }
            boolean treeAdj = ga.getOrDefault(k, Set.of()).stream().anyMatch(hs::contains); if (treeAdj) continue;
            boolean adjSpecial = ga.getOrDefault(k, Set.of()).stream().anyMatch(nb -> {
                RoomType vl = topLabels.get(nb);
                return vl != null && (vl == RoomType.GOBLIN_WELL || vl == RoomType.GOBLIN_MARCH || vl == RoomType.GOBLIN_ARMORY || vl == RoomType.GOBLIN_TREASURE);
            }); if (adjSpecial) continue; hs.add(k); }
        for (Point k : hs) { int d = ga.get(k).size();
            if (d == 3) putSpecial(labelState, topLabels, k, RoomType.GOBLIN_HOUSE_3);
            else if (d == 2) { List<Point> nb2 = new ArrayList<>(ga.get(k)); if (!isStraight(nb2.get(0), nb2.get(1))) putSpecial(labelState, topLabels, k, RoomType.GOBLIN_HOUSE_2); }
            else if (d == 1) putSpecial(labelState, topLabels, k, RoomType.GOBLIN_HOUSE_1); }
        for (var e : ga.entrySet()) { adj.putIfAbsent(e.getKey(), new HashSet<>()); adj.get(e.getKey()).addAll(e.getValue()); goblinCells.add(e.getKey()); }
        goblinCells.add(gLeaf); return true;
    }

    private static void placeChapelAndCrypt(DungeonLabelState labelState, Map<Point, Set<Point>> adj,
                                             Map<Point, RoomType> topLabels, Map<Point, Set<Point>> ct,
                                             Point cs, Set<Point> globalOccupied, Random rng) {
        for (Point k : ct.keySet()) {
            RoomType vl = topLabels.get(k);
            if (ct.get(k).size() != 1 || k.equals(cs) || vl == null || vl != RoomType.CUL_DJ) continue;
            Point mb = ct.get(k).iterator().next(); int dx = k.x() - mb.x(), dy = k.y() - mb.y();
            if (k.move(dx, dy).isOutOfBounds() || globalOccupied.contains(k.move(dx, dy))) continue;
            boolean placed = false;
            boolean logged = false;
            for (int pathLen = 3; pathLen <= 5 && !placed; pathLen++) {
                for (int turnAt = 1; turnAt < pathLen && !placed; turnAt++) {
                    for (int turnDir : new int[]{1, -1}) {
                        DungeonCompositeRooms.Builder builder = DungeonCompositeRooms.Spec.builder().name("CHAPEL_CRYPT")
                                .entry(0, 0).label(0, 0, RoomType.CHAPEL_1).label(1, 0, RoomType.CHAPEL_2).edge(0, 0, 1, 0);
                        List<DungeonCompositeRooms.LocalPoint> pathLocals = new ArrayList<>();
                        DungeonCompositeRooms.LocalPoint prev = new DungeonCompositeRooms.LocalPoint(1, 0);
                        int forward = 1, side = 0, dForward = 1, dSide = 0;
                        for (int step = 0; step < pathLen; step++) {
                            if (step == turnAt) { dForward = 0; dSide = turnDir; }
                            forward += dForward; side += dSide;
                            DungeonCompositeRooms.LocalPoint next = new DungeonCompositeRooms.LocalPoint(forward, side);
                            builder.node(forward, side).edge(prev.forward(), prev.side(), forward, side); pathLocals.add(next); prev = next;
                        }
                        for (int step = 0; step < 2; step++) {
                            forward += dForward; side += dSide;
                            RoomType label = step == 0 ? RoomType.CRYPT_1 : RoomType.CRYPT_2;
                            builder.label(forward, side, label).edge(prev.forward(), prev.side(), forward, side);
                            prev = new DungeonCompositeRooms.LocalPoint(forward, side);
                        }
                        DungeonCompositeRooms.Spec chapelCrypt = builder.exit(forward, side).build();
                        DungeonCompositeRooms.Placement placement = DungeonCompositeRooms.plan(adj, k, dx, dy, chapelCrypt, Set.of(k));
                        if (placement == null) continue;
                        boolean occupied = false; Point firstBlocked = null;
                        for (Point cell : placement.occupiedCells()) { if (!cell.equals(k) && globalOccupied.contains(cell)) { occupied = true; firstBlocked = cell; break; } }
                        if (occupied) { if (!logged) { DungeonFailureLog.compositeReject(placement.name(), "GLOBAL_OCCUPIED", k, "cellule=" + firstBlocked.key() + " (turnDir=" + turnDir + " pathLen=" + pathLen + " turnAt=" + turnAt + ")"); logged = true; } continue; }
                        DungeonCompositeRooms.place(adj, null, placement); globalOccupied.addAll(placement.occupiedCells());
                        for (var e : placement.labelPoints().entrySet()) putSpecial(labelState, topLabels, e.getValue(), e.getKey());
                        for (int step = 0; step < pathLocals.size(); step++) {
                            Point pathPoint = placement.point(pathLocals.get(step));
                            putGenericWorking(labelState, topLabels, pathPoint, Theme.P12, step == turnAt - 1 ? RoomType.I2 : RoomType.pickC(rng));
                        }
                        placed = true;
                        return;
                    }
                }
            }
            if (!placed) DungeonFailureLog.compositeReject("CHAPEL_CRYPT", "NO_TURN_VARIANT", k, "all pathLen/turnAt variants");
        }
    }

    private static void placePrisonBlock(DungeonLabelState labelState, Map<Point, Set<Point>> adj,
                                          Map<Point, RoomType> topLabels, Map<Point, Set<Point>> pt,
                                          Point ps, Set<Point> globalOccupied) {
        for (Point pk : pt.keySet()) {
            RoomType vp = topLabels.get(pk);
            if (pt.get(pk).size() != 1 || pk.equals(ps) || vp == null || vp != RoomType.CUL_DJ) continue;
            Point np = pt.get(pk).iterator().next(); int dx = pk.x() - np.x(), dy = pk.y() - np.y();
            DungeonCompositeRooms.Placement prison = DungeonCompositeRooms.plan(adj, pk, dx, dy, DungeonCompositeRooms.PRISON_CENTRAL, Set.of(pk));
            if (prison == null) continue;
            boolean occupied = false; for (Point cell : prison.occupiedCells()) { if (!cell.equals(pk) && globalOccupied.contains(cell)) { occupied = true; break; } }
            if (occupied) continue;
            DungeonCompositeRooms.place(adj, null, prison); globalOccupied.addAll(prison.occupiedCells());
            for (var e : prison.labelPoints().entrySet()) putSpecial(labelState, topLabels, e.getValue(), e.getKey());
            return;
        }
    }

    private static void rebuildFinalLabels(Map<Point, RoomType> topLabels, Map<Point, Set<Point>> adj, Random rng) {
        DungeonLabelState labelState = new DungeonLabelState(); labelState.absorbLabels(topLabels);
        topLabels.clear(); topLabels.putAll(labelState.buildLabels(adj, rng));
    }

    static boolean generatePart4Tree(Map<Point, Set<Point>> adj, Map<Point, RoomType> topLabels,
                                              int hx, int hz, String missingLootType, Random rng) {
        DungeonLabelState labelState = new DungeonLabelState(); labelState.absorbLabels(topLabels);
        Point[] p4Exits = { new Point(hx, hz + 2), new Point(hx + 1, hz + 2), new Point(hx - 1, hz), new Point(hx + 2, hz), new Point(hx + 1, hz - 1) };
        List<Point> p4List = new ArrayList<>(Arrays.asList(p4Exits)); Collections.shuffle(p4List, rng);
        Set<Point> globalOccupied = new HashSet<>();
        for (int x = hx; x <= hx + 1; x++) for (int z = hz; z <= hz + 1; z++) { Point hp = new Point(x, z); globalOccupied.add(hp); adj.putIfAbsent(hp, new HashSet<>()); }
        List<Point> cjKeys = new ArrayList<>(), exitKeys = new ArrayList<>(); List<int[]> cjDirs = new ArrayList<>();
        for (Point pp : p4List) {
            int adx = 0, ady = 0; Point hk = null;
            if (pp.x() == hx && pp.y() == hz+2) { adx = 0; ady = 1; hk = new Point(hx, hz+1); }
            else if (pp.x() == hx+1 && pp.y() == hz+2) { adx = 0; ady = 1; hk = new Point(hx+1, hz+1); }
            else if (pp.x() == hx-1 && pp.y() == hz) { adx = -1; ady = 0; hk = new Point(hx, hz); }
            else if (pp.x() == hx+2 && pp.y() == hz) { adx = 1; ady = 0; hk = new Point(hx+1, hz); }
            else if (pp.x() == hx+1 && pp.y() == hz-1) { adx = 0; ady = -1; hk = new Point(hx+1, hz); }
            if (hk == null) continue;
            Point cjPt = pp.move(adx, ady);
            if (!cjPt.isOutOfBounds() && !globalOccupied.contains(cjPt)) {
                adj.putIfAbsent(pp, new HashSet<>()); adj.putIfAbsent(cjPt, new HashSet<>()); adj.get(pp).add(hk); adj.get(hk).add(pp);
                adj.get(pp).add(cjPt); adj.get(cjPt).add(pp); globalOccupied.add(pp); globalOccupied.add(cjPt);
                labelState.setTheme(pp, Theme.DJ); labelState.setTheme(cjPt, Theme.DJ); cjKeys.add(cjPt); cjDirs.add(new int[]{adx, ady}); exitKeys.add(pp);
            }
        }
        Map<Point, List<Point>> reservedChains = new HashMap<>();
        for (int ti = 0; ti < cjKeys.size(); ti++) {
            Point cp = cjKeys.get(ti); int rdx = cjDirs.get(ti)[0], rdy = cjDirs.get(ti)[1]; List<Point> chain = new ArrayList<>();
            Point f1 = cp.move(rdx, rdy);
            if (!f1.isOutOfBounds() && !globalOccupied.contains(f1)) { chain.add(f1); globalOccupied.add(f1);
                Point f2 = f1.move(rdx, rdy); if (!f2.isOutOfBounds() && !globalOccupied.contains(f2)) { chain.add(f2); globalOccupied.add(f2); } }
            reservedChains.put(cp, chain);
        }
        List<Map<Point, Set<Point>>> allTrees = new ArrayList<>(); List<Point> allStarts = new ArrayList<>();
        for (int ti = 0; ti < 5 && ti < cjKeys.size(); ti++) {
            Point startPoint = cjKeys.get(ti); int adx = cjDirs.get(ti)[0], ady = cjDirs.get(ti)[1];
            Map<Point, Set<Point>> tr = new HashMap<>(); tr.put(startPoint, new HashSet<>()); globalOccupied.add(startPoint);
            int ci3 = 0, ci4 = 0, target = 11 + rng.nextInt(5);
            List<Point> chain = reservedChains.getOrDefault(startPoint, List.of()); Point prev = startPoint;
            for (Point f : chain) { tr.put(f, new HashSet<>()); tr.get(prev).add(f); tr.get(f).add(prev); prev = f; }
            while (tr.size() < target) {
                List<Object[]> cands = new ArrayList<>();
                for (Point p : tr.keySet()) { int d = tr.get(p).size(); if (d >= 4) continue; if (d == 2 && ci3 >= P4_MAX_IJ3) continue; if (d == 3 && ci4 >= P4_MAX_IJ4) continue;
                    for (int[] dir : DungeonAlgo.DIR_OFFSET) { Point next = p.move(dir); if (next.isOutOfBounds()) continue; if (!globalOccupied.contains(next)) {
                        if (DungeonConstraints.colinearRunAfterEdge(p, next, tr) > DungeonAlgo.MAX_COLINEAR_RUN) continue;
                        if (d >= 2) { boolean sk = false; for (Point m1 : tr.get(p)) for (Point m2 : tr.get(p)) { if (m1.equals(m2)) continue;
                            if (p.x() - m1.x() == m2.x() - p.x() && p.y() - m1.y() == m2.y() - p.y()) { Set<Point> a1 = tr.get(m1), a2 = tr.get(m2); if ((a1 != null && a1.size() >= 3) || (a2 != null && a2.size() >= 3)) { sk = true; break; } } } if (sk) continue; }
                        if (d == 1) { Point op = tr.get(p).iterator().next(); if (p.x() - op.x() == next.x() - p.x() && p.y() - op.y() == next.y() - p.y()) { Point bk = op.move(-(p.x() - op.x()), -(p.y() - op.y())); if (tr.containsKey(bk) && tr.get(bk).contains(op)) continue; } }
                        cands.add(new Object[]{p, next, dir}); } }
                }
                if (cands.isEmpty()) break; List<Object[]> w = new ArrayList<>(); for (Object[] c : cands) { int[] cd = (int[]) c[2]; int wt = (cd[0] == adx && cd[1] == ady) ? 20 : 1; for (int i = 0; i < wt; i++) w.add(c); }
                if (w.isEmpty()) break; Object[] ch = w.get(rng.nextInt(w.size())); Point src = (Point) ch[0], dst = (Point) ch[1];
                globalOccupied.add(dst); tr.put(dst, new HashSet<>()); tr.get(src).add(dst); tr.get(dst).add(src); int nd = tr.get(src).size(); if (nd == 3) ci3++; else if (nd == 4) ci4++;
            }
            if (tr.size() < 6) continue; allTrees.add(tr); allStarts.add(startPoint); labelTreeNodes(labelState, tr, topLabels, startPoint, rng);
            for (var e : tr.entrySet()) { adj.putIfAbsent(e.getKey(), new HashSet<>()); adj.get(e.getKey()).addAll(e.getValue()); }
        }
        Set<Point> seenTreeCells = new HashSet<>(); for (Map<Point, Set<Point>> t : allTrees) for (Point k : t.keySet()) if (!seenTreeCells.add(k)) return false;
        for (Point ek : exitKeys) { if (topLabels.containsKey(ek)) continue; Set<Point> skn = adj.get(ek); if (skn == null) continue; putGeneric(labelState, topLabels, ek, Theme.DJ, skn, rng); }
        for (Point cjk : cjKeys) { Set<Point> cn = adj.get(cjk); if (cn == null) continue; putGeneric(labelState, topLabels, cjk, Theme.DJ, cn, rng); }
        if (allTrees.size() < 5) return false;
        List<Integer> idxs = new ArrayList<>(Arrays.asList(0, 1, 2, 3, 4)); Collections.shuffle(idxs, rng);
        int gIdx = idxs.get(0), cIdx = idxs.get(1), pIdx = idxs.get(2); Set<Point> goblinCells = new HashSet<>();
        placePrisonBlock(labelState, adj, topLabels, allTrees.get(pIdx), allStarts.get(pIdx), globalOccupied);
        placeChapelAndCrypt(labelState, adj, topLabels, allTrees.get(cIdx), allStarts.get(cIdx), globalOccupied, rng);
        if (!placeGoblinVillage(labelState, adj, topLabels, allTrees.get(gIdx), globalOccupied, goblinCells, rng)) return false;
        DungeonLabels.reclassifyGeneric(topLabels, adj, rng);
        java.util.function.Predicate<Point> isHubExit = kp -> (kp.x() == hx-1 && kp.y() == hz) || (kp.x() == hx+2 && kp.y() == hz) || (kp.x() == hx+1 && kp.y() == hz-1) || (kp.x() == hx && kp.y() == hz+2) || (kp.x() == hx+1 && kp.y() == hz+2);
        int mjT = 5 + rng.nextInt(6); Map<Point, Integer> treeForNode = new HashMap<>(); for (int ti = 0; ti < allTrees.size(); ti++) for (Point k : allTrees.get(ti).keySet()) treeForNode.put(k, ti);
        Set<Point> mjPlacedKeys = new HashSet<>(); Map<Integer, Set<RoomType>> mjTypesOnTree = new HashMap<>(); for (int ti = 0; ti < allTrees.size(); ti++) mjTypesOnTree.put(ti, new HashSet<>());
        for (int attempt = 0; attempt < 50 && mjPlacedKeys.size() < mjT; attempt++) {
            List<Object[]> cands = new ArrayList<>();
            for (Point k : topLabels.keySet()) { if (mjPlacedKeys.contains(k) || goblinCells.contains(k) || isHubExit.test(k)) continue;
                RoomType v = topLabels.get(k); if (v == null) continue;
                boolean isLeaf = v == RoomType.CUL_DJ; boolean isCorr = v.isDjCorridor() || v.isGoblinCorridor();
                if (!isLeaf && !isCorr) continue;
                if (!DungeonConstraints.isFarFromAll(adj, k, mjPlacedKeys, DungeonAlgo.MONSTER_MIN_DIST)) continue;
                int ti = treeForNode.getOrDefault(k, -1); Set<RoomType> usedOnTree = mjTypesOnTree.getOrDefault(ti, new HashSet<>());
                List<RoomType> pool = isLeaf ? RoomType.LEAF_MONSTERS_P3_P4 : RoomType.CORRIDOR_MONSTERS_P3_P4;
                for (RoomType t : pool) if (!usedOnTree.contains(t)) cands.add(new Object[]{k, t, ti});
            }
            if (cands.isEmpty()) break;
            Object[] ch = cands.get(rng.nextInt(cands.size()));
            Point best = (Point) ch[0]; RoomType bestType = (RoomType) ch[1]; int ti = (int) ch[2];
            putSpecial(labelState, topLabels, best, bestType); mjPlacedKeys.add(best);
            if (ti >= 0) mjTypesOnTree.get(ti).add(bestType);
        }
        if (!(topLabels.containsValue(RoomType.MONSTER_DJ_1) && topLabels.containsValue(RoomType.MONSTER_DJ_2)
            && topLabels.containsValue(RoomType.MONSTER_DJ_3) && topLabels.containsValue(RoomType.MONSTER_DJ_4)
            && topLabels.containsValue(RoomType.MONSTER_DJ_5))) return false;
        for (var e : new ArrayList<>(topLabels.entrySet())) { if (e.getValue() == RoomType.CUL_DJ && !isHubExit.test(e.getKey())) { putSpecial(labelState, topLabels, e.getKey(), RoomType.BLACK_MARKET); break; } }
        if (missingLootType != null) {
            RoomType missingLT = RoomType.byId(missingLootType);
            boolean lootCorr = missingLT == RoomType.LOOT_DJ_2; Point lk = null;
            for (var e : topLabels.entrySet()) { RoomType v = e.getValue(); Point k = e.getKey(); if (v == null || goblinCells.contains(k) || isHubExit.test(k)) continue;
                if (lootCorr && (v.isDjCorridor() || v.isGoblinCorridor())) { lk = k; break; }
                if (!lootCorr && v == RoomType.CUL_DJ) { lk = k; break; } }
            if (lk != null && missingLT != null) putSpecial(labelState, topLabels, lk, missingLT);
        }
        Point puitDJKey = null; for (var e : topLabels.entrySet()) { RoomType v = e.getValue(); Point k = e.getKey(); if (v == null || goblinCells.contains(k) || isHubExit.test(k)) continue;
            if (v.isDjCorridor() || v.isGoblinCorridor()) { puitDJKey = k; break; } }
        if (puitDJKey != null) putSpecial(labelState, topLabels, puitDJKey, RoomType.WELL_DJ);
        if (!DungeonConstraints.enforceDeadEndAfterTurn(topLabels, adj, rng)) return false;
        rebuildFinalLabels(topLabels, adj, rng);
        boolean hc1 = topLabels.containsValue(RoomType.CHAPEL_1) && topLabels.containsValue(RoomType.CRYPT_1);
        boolean hpr = false; for (RoomType v : topLabels.values()) if (v != null && v.isPrisonCentral()) { hpr = true; break; }
        boolean hpg = topLabels.containsValue(RoomType.GOBLIN_DOOR);
        boolean hmn = topLabels.containsValue(RoomType.BLACK_MARKET);
        boolean hlt = topLabels.containsValue(RoomType.LOOT_DJ_1) || topLabels.containsValue(RoomType.LOOT_DJ_2) || topLabels.containsValue(RoomType.LOOT_DJ_3);
        boolean hPuitDJ = topLabels.containsValue(RoomType.WELL_DJ);
        int gbc = 0; for (RoomType v : topLabels.values()) if (v == RoomType.GOBLIN_WELL || v == RoomType.GOBLIN_MARCH || v == RoomType.GOBLIN_ARMORY || v == RoomType.GOBLIN_TREASURE) gbc++;
        boolean hmg = false; int hmgCount = 0; for (RoomType v : topLabels.values()) if (v != null && v.isGoblinHouse()) { hmgCount++; }
        hmg = hmgCount >= 3;
        return hc1 && hpr && hpg && hmn && hlt && hPuitDJ && hmg && gbc == 4 && mjPlacedKeys.size() >= 5;
    }

    private static boolean isStraight(Point p1, Point p2) { return p1.x() == p2.x() || p1.y() == p2.y(); }
}
