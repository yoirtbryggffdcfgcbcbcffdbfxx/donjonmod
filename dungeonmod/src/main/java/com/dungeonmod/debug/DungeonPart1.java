package com.dungeonmod.debug;

import com.dungeonmod.debug.DungeonAlgo.Point;
import com.dungeonmod.debug.DungeonAlgo.RoomIds;
import com.dungeonmod.debug.DungeonAlgo.RoomPools;
import com.dungeonmod.debug.DungeonAlgo.Shape;
import com.dungeonmod.debug.DungeonAlgo.Theme;
import com.dungeonmod.debug.DungeonAlgo.TavernResult;
import com.dungeonmod.debug.DungeonAlgo.TreeResult;

import java.util.*;

final class DungeonPart1 {
    private DungeonPart1() {}

    private static final int TAVERN_PATH_ATTEMPTS = 30;

    static boolean hasPrisonCandidate(Map<Point, Set<Point>> adj, Point startPoint) {
        for (var e : adj.entrySet()) {
            if (e.getValue().size() != 1 || e.getKey().equals(startPoint)) continue;
            Point leaf = e.getKey();
            Point parent = adj.get(leaf).iterator().next();
            if (adj.get(parent).size() == 2) {
                Set<Point> pn = new HashSet<>(adj.get(parent)); pn.remove(leaf);
                Point gp = pn.iterator().next();

                int pdx = parent.x() - gp.x();
                int pdy = parent.y() - gp.y();
                int cdx = leaf.x() - parent.x();
                int cdy = leaf.y() - parent.y();

                if (pdx == cdx && pdy == cdy) return true;
            }
        }
        return false;
    }

    static TreeResult generatePart1Tree(Random rng) {
        Point start = new Point(DungeonAlgo.GRID_SIZE / 2, DungeonAlgo.GRID_SIZE / 2);
        Map<Point, Set<Point>> adj = new HashMap<>();
        Set<Point> occupied = new HashSet<>();
        adj.put(start, new HashSet<>());
        occupied.add(start);

        List<int[]> dirs = new ArrayList<>(Arrays.asList(DungeonAlgo.DIR_OFFSET));
        Collections.shuffle(dirs, rng);
        int dir = -1;
        for (int[] d : dirs) {
            Point target = start.move(d);
            if (!target.isOutOfBounds() && !occupied.contains(target)) {
                for (int i = 0; i < 4; i++)
                    if (DungeonAlgo.DIR_OFFSET[i][0] == d[0] && DungeonAlgo.DIR_OFFSET[i][1] == d[1]) { dir = i; break; }
                break;
            }
        }
        if (dir < 0) { TreeResult tr = new TreeResult(); tr.startPoint = start; tr.startKey = start.key(); tr.startX = start.x(); tr.startY = start.y(); tr.adj = adj; return tr; }

        Point c = start.move(DungeonAlgo.DIR_OFFSET[dir]);
        DungeonTreeBuilder.addEdge(adj, occupied, start, c);
        List<Point> trunkCells = new ArrayList<>();
        trunkCells.add(c);

        int trunkTarget = DungeonAlgo.PART2_TRUNK_MIN + rng.nextInt(DungeonAlgo.PART2_TRUNK_MAX - DungeonAlgo.PART2_TRUNK_MIN + 1);

        for (int t = 1; t < trunkTarget; t++) {
            int runIfStraight = DungeonConstraints.colinearRunAfterEdge(c, c.move(DungeonAlgo.DIR_OFFSET[dir]), adj);
            boolean forceTurn = runIfStraight > DungeonAlgo.MAX_COLINEAR_RUN;
            boolean maybeTurn = runIfStraight >= 2 && rng.nextFloat() < 0.40f;
            if (forceTurn || maybeTurn) dir = (dir + (rng.nextBoolean() ? 1 : 3)) % 4;

            Point n = c.move(DungeonAlgo.DIR_OFFSET[dir]);
            if (n.isOutOfBounds() || occupied.contains(n)
                    || DungeonConstraints.colinearRunAfterEdge(c, n, adj) > DungeonAlgo.MAX_COLINEAR_RUN) {
                int od = dir; boolean found = false;
                for (int a = 0; a < 4; a++) {
                    int td = (od + a) % 4;
                    Point cand = c.move(DungeonAlgo.DIR_OFFSET[td]);
                    if (cand.isOutOfBounds() || occupied.contains(cand)) continue;
                    if (DungeonConstraints.colinearRunAfterEdge(c, cand, adj) > DungeonAlgo.MAX_COLINEAR_RUN) continue;
                    dir = td; n = cand; found = true; break;
                }
                if (!found) break;
            }

            DungeonTreeBuilder.addEdge(adj, occupied, c, n);
            trunkCells.add(n);
            c = n;

            if (t < trunkTarget - 3 && rng.nextFloat() < 0.55f) {
                int pDir = (dir + (rng.nextBoolean() ? 1 : 3)) % 4;
                DungeonPart2.growMiniTreeBounded(n, pDir, adj, occupied, rng);
            }
        }

        Point trunkEnd = trunkCells.isEmpty() ? start : trunkCells.get(trunkCells.size() - 1);
        if (!trunkCells.isEmpty()) {
            int side = (dir + (rng.nextBoolean() ? 1 : 3)) % 4;
            DungeonPart2.growMiniTreeBounded(trunkEnd, side, adj, occupied, rng);
        }

        // Reserve l'espace devant le tronc pour la sortie + chemin + taverne
        int[] td = DungeonAlgo.DIR_OFFSET[dir];
        for (int ri = 1; ri <= 10; ri++) {
            for (int sj = -2; sj <= 2; sj++) {
                Point rp = new Point(trunkEnd.x() + td[0] * ri - td[1] * sj,
                                     trunkEnd.y() + td[1] * ri + td[0] * sj);
                if (!rp.isOutOfBounds()) occupied.add(rp);
            }
        }

        int targetSize = DungeonAlgo.PART1_TARGET_MIN + rng.nextInt(DungeonAlgo.PART1_TARGET_MAX - DungeonAlgo.PART1_TARGET_MIN + 1);
        int ci3 = 0, ci4 = 0;
        for (Set<Point> nb : adj.values()) { int d = nb.size(); if (d == 3) ci3++; else if (d == 4) ci4++; }
        while (adj.size() < targetSize) {
            List<Point[]> candidates = new ArrayList<>();
            for (Point node : adj.keySet()) {
                if (node.equals(start) || node.equals(trunkEnd)) continue;
                int deg = adj.get(node).size();
                if (deg >= 4) continue;
                if (deg == 2 && ci3 >= DungeonAlgo.PART1_MAX_I3) continue;
                if (deg == 3 && ci4 >= 1) continue;
                for (int[] d : DungeonAlgo.DIR_OFFSET) {
                    Point next = node.move(d);
                    if (next.isOutOfBounds() || occupied.contains(next)) continue;
                    if (DungeonConstraints.colinearRunAfterEdge(node, next, adj) > DungeonAlgo.MAX_COLINEAR_RUN) continue;
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

    static Point appendP1ExitSequenceCells(Map<Point, Set<Point>> adj, Point trunkEnd, int trunkEndDir, Random rng) {
        if (trunkEnd == null || trunkEndDir < 0) return null;
        Set<Point> occupied = new HashSet<>(adj.keySet());
        int dir = trunkEndDir;
        Point cursor = trunkEnd;
        List<Point> gapCells = new ArrayList<>();
        int need = 0;
        for (int i = 0; i < need + 1; i++) {
            Point next = null; int chosenDir = -1;
            int[] tryOrder = (i == 0)
                ? new int[]{dir, (dir + 1) % 4, (dir + 3) % 4, (dir + 2) % 4}
                : new int[]{dir, (dir + 1) % 4, (dir + 3) % 4};
            for (int td : tryOrder) {
                Point cand = cursor.move(DungeonAlgo.DIR_OFFSET[td]);
                if (cand.isOutOfBounds() || occupied.contains(cand) || adj.containsKey(cand)) continue;
                if (DungeonConstraints.colinearRunAfterEdge(cursor, cand, adj) > DungeonAlgo.MAX_COLINEAR_RUN) continue;
                next = cand; chosenDir = td; break;
            }
            if (next == null) return null;
            adj.putIfAbsent(cursor, new HashSet<>()); adj.putIfAbsent(next, new HashSet<>());
            adj.get(cursor).add(next); adj.get(next).add(cursor);
            occupied.add(next); dir = chosenDir; cursor = next;
            if (i < need) gapCells.add(next);
            else {
                for (Point gp : gapCells) if (adj.getOrDefault(gp, Set.of()).size() != 2) return null;
                return next;
            }
        }
        return null;
    }

    static Map<Point, String> analyzePart1(Point startPoint, Map<Point, Set<Point>> adj, Point trunkEnd, int trunkEndDir, Random rng) {
        Point porte = appendP1ExitSequenceCells(adj, trunkEnd, trunkEndDir, rng);
        if (porte == null) return null;

        DungeonLabelState labelState = new DungeonLabelState();
        labelState.setTheme(adj.keySet(), Theme.P12);

        List<Point> leaves = new ArrayList<>();
        for (var e : adj.entrySet()) if (e.getValue().size() == 1 && !e.getKey().equals(startPoint) && !e.getKey().equals(porte)) leaves.add(e.getKey());
        if (leaves.size() < 5) return null;
        Collections.shuffle(leaves, rng);

        Point prison = null;
        for (Point leaf : leaves) {
            Point parent = adj.get(leaf).iterator().next();
            if (adj.get(parent).size() == 2) {
                Set<Point> pn = new HashSet<>(adj.get(parent)); pn.remove(leaf);
                Point gp = pn.iterator().next();
                if ((parent.x() - gp.x() == leaf.x() - parent.x()) && (parent.y() - gp.y() == leaf.y() - parent.y())) {
                    prison = leaf; break;
                }
            }
        }
        if (prison == null) return null;

        List<Point> others = new ArrayList<>();
        for (Point l : leaves) if (!l.equals(prison)) others.add(l);

        labelState.putSpecial(startPoint, RoomIds.START);
        labelState.putSpecial(porte, RoomIds.DOOR_1);
        labelState.putSpecial(prison, RoomIds.PRISON);
        Point prisonParent = adj.get(prison).iterator().next();
        labelState.putSpecial(prisonParent, RoomIds.MONSTER_2);
        labelState.putSpecial(others.get(0), RoomIds.LOOT_1);

        Set<Point> monsters = new HashSet<>();
        monsters.add(prisonParent);
        List<Point> freeLeaves = new ArrayList<>();
        for (int i = 1; i < others.size(); i++) freeLeaves.add(others.get(i));
        Collections.shuffle(freeLeaves, rng);

        Point m1 = DungeonConstraints.firstFar(adj, freeLeaves, monsters, DungeonAlgo.MONSTER_MIN_DIST);
        if (m1 == null) return null;
        labelState.putSpecial(m1, RoomIds.MONSTER_1);
        monsters.add(m1);
        freeLeaves.remove(m1);
        boolean isM4 = rng.nextBoolean();
        List<Point> cNodes = new ArrayList<>(), i2Nodes = new ArrayList<>(), i3Nodes = new ArrayList<>(), i4Nodes = new ArrayList<>();
        for (var e : adj.entrySet()) {
            if (labelState.hasSpecial(e.getKey())) continue;
            int deg = e.getValue().size();
            if (deg == 2) {
                List<Point> nb = new ArrayList<>(e.getValue());
                if (isStraight(nb.get(0), nb.get(1))) cNodes.add(e.getKey());
                else i2Nodes.add(e.getKey());
            } else if (deg == 3) i3Nodes.add(e.getKey());
            else if (deg == 4) i4Nodes.add(e.getKey());
        }

        String secondMonster = null;
        for (int variant = 0; variant < 2 && secondMonster == null; variant++) {
            boolean tryCorr = (variant == 0) == isM4;
            if (tryCorr) {
                List<Point> validC = new ArrayList<>();
                for (Point n : cNodes) {
                    if (!labelState.hasSpecial(n) && DungeonConstraints.isFarFromAll(adj, n, monsters, DungeonAlgo.MONSTER_MIN_DIST)) validC.add(n);
                }
                if (!validC.isEmpty()) {
                    Point m4Point = validC.get(rng.nextInt(validC.size()));
                    labelState.putSpecial(m4Point, RoomIds.MONSTER_4);
                    monsters.add(m4Point);
                    secondMonster = RoomIds.MONSTER_4;
                }
            } else {
                Point m3 = DungeonConstraints.firstFar(adj, freeLeaves, monsters, DungeonAlgo.MONSTER_MIN_DIST);
                if (m3 != null) {
                    labelState.putSpecial(m3, RoomIds.MONSTER_3);
                    monsters.add(m3);
                    freeLeaves.remove(m3);
                    secondMonster = RoomIds.MONSTER_3;
                }
            }
        }
        if (secondMonster == null) return null;

        for (Point n : cNodes) {
            if (!labelState.hasSpecial(n)) { labelState.putSpecial(n, RoomIds.WELL); break; }
        }

        List<Point> genericOrder = new ArrayList<>();
        genericOrder.addAll(freeLeaves);
        genericOrder.addAll(cNodes);
        genericOrder.addAll(i2Nodes);
        genericOrder.addAll(i3Nodes);
        genericOrder.addAll(i4Nodes);
        Map<Point, String> finalLabels = labelState.buildLabels(adj, rng, genericOrder);

        if (!validatePart1SpecialShapes(finalLabels, adj)) return null;
        if (!DungeonConstraints.enforceDeadEndAfterTurn(finalLabels, adj, rng)) return null;
        if (!validatePart1SpecialShapes(finalLabels, adj)) return null;
        return finalLabels;
    }

    private static boolean validatePart1SpecialShapes(Map<Point, String> labels, Map<Point, Set<Point>> adj) {
        for (var e : labels.entrySet()) {
            String label = e.getValue();
            Shape shape = DungeonLabels.shapeOf(adj.getOrDefault(e.getKey(), Set.of()));
            if (RoomIds.START.equals(label) || RoomIds.DOOR_1.equals(label)
                    || RoomIds.PRISON.equals(label) || RoomIds.LOOT_1.equals(label)
                    || RoomIds.MONSTER_1.equals(label) || RoomIds.MONSTER_3.equals(label)) {
                if (shape != Shape.DEAD_END) return false;
            } else if (RoomIds.MONSTER_2.equals(label) || RoomIds.MONSTER_4.equals(label)
                    || RoomIds.WELL.equals(label)) {
                if (shape != Shape.STRAIGHT) return false;
            }
        }
        return true;
    }

    static TavernResult placeTavernAndPath(Map<Point, Set<Point>> adj, Point porte, Random rng) {
        Point parent = adj.get(porte).iterator().next();
        int baseDx = porte.x() - parent.x(), baseDy = porte.y() - parent.y();

        for (int attempt = 1; attempt <= TAVERN_PATH_ATTEMPTS; attempt++) {
            int dx = baseDx, dy = baseDy;
            int maxLen = 2 + rng.nextInt(4);
            int cx = porte.x(), cy = porte.y();
            List<Point> pathCells = new ArrayList<>();
            Map<Point, Set<Point>> tmpAdj = DungeonTreeBuilder.copyAdj(adj);
            Point curTmp = porte;

            boolean failed = false;
            for (int i = 0; i < maxLen; i++) {
                int ndx, ndy;
                if (i == 0) {
                    ndx = dx; ndy = dy;
                    Point s = new Point(cx + dx, cy + dy);
                    if (s.isOutOfBounds() || tmpAdj.containsKey(s)) { failed = true; break; }
                } else {
                    Point s = new Point(cx + dx, cy + dy);
                    if (!s.isOutOfBounds() && !tmpAdj.containsKey(s) && rng.nextFloat() < 0.35f) {
                        ndx = dx; ndy = dy;
                    } else {
                        int[][] perp = {{dy, -dx}, {-dy, dx}};
                        boolean turned = false;
                        ndx = dx; ndy = dy;
                        int startP = rng.nextInt(2);
                        for (int k = 0; k < 2; k++) {
                            int[] turn = perp[(startP + k) % 2];
                            Point t = new Point(cx + turn[0], cy + turn[1]);
                            if (t.isOutOfBounds() || tmpAdj.containsKey(t)) continue;
                            ndx = turn[0]; ndy = turn[1];
                            dx = ndx; dy = ndy;
                            turned = true; break;
                        }
                        if (!turned) {
                            s = new Point(cx + dx, cy + dy);
                            if (!s.isOutOfBounds() && !tmpAdj.containsKey(s)) { ndx = dx; ndy = dy; }
                            else { failed = true; break; }
                        }
                    }
                }
                Point next = new Point(cx + ndx, cy + ndy);
                if (tmpAdj.containsKey(next) || next.isOutOfBounds()) { failed = true; break; }
                tmpAdj.putIfAbsent(curTmp, new HashSet<>());
                tmpAdj.putIfAbsent(next, new HashSet<>());
                tmpAdj.get(curTmp).add(next);
                tmpAdj.get(next).add(curTmp);
                pathCells.add(next);
                curTmp = next;
                cx = next.x(); cy = next.y();
            }
            if (failed || pathCells.size() < 2) continue;

            DungeonCompositeRooms.Placement tavern = null;
            int[] dxs = {dx, -dy, -dx, dy};
            int[] dys = {dy, dx, -dy, -dx};
            for (int rot = 0; rot < 4 && tavern == null; rot++) {
                int rdx = dxs[rot], rdy = dys[rot];
                Point t1 = new Point(cx + rdx, cy + rdy);
                if (t1.isOutOfBounds() || adj.containsKey(t1) || tmpAdj.containsKey(t1)) continue;
                tavern = DungeonCompositeRooms.plan(adj, t1, rdx, rdy, DungeonCompositeRooms.TAVERN);
            }
            if (tavern == null) continue;

            Set<Point> pathSet = new HashSet<>();
            Point cur = porte;
            for (Point cell : pathCells) {
                pathSet.add(cell); adj.put(cell, new HashSet<>()); adj.get(cell).add(cur); adj.get(cur).add(cell); cur = cell;
            }
            DungeonCompositeRooms.place(adj, cur, tavern);

            TavernResult tr = new TavernResult();
            tr.tavern = tavern.labelPoints();
            tr.exitPoint = tavern.exitPoint(); tr.pathSet = pathSet;
            return tr;
        }

        DungeonFailureLog.compositeReject("TAVERN", "NO_PATH_VARIANT", porte,
                "attempts=" + TAVERN_PATH_ATTEMPTS);
        return null;
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


    static int placeMonster5OnDoorPaths(Map<Point, String> labels, Map<Point, Set<Point>> adj,
                                                 Point startPoint) {
        int placed = 0;
        for (String doorId : List.of(RoomIds.DOOR_1)) {
            Point door = findPointByValue(labels, doorId);
            if (door == null) continue;
            if (hasM5WithGapBeforeDoor(door, labels, adj, startPoint)) {
                placed++;
                continue;
            }
            Point candidate = findM5WithGapBeforeDoor(door, labels, adj, startPoint);
            if (candidate == null) continue;
            labels.put(candidate, RoomIds.MONSTER_5);
            placed++;
        }
        return placed;
    }

    private static boolean hasM5WithGapBeforeDoor(Point door, Map<Point, String> labels,
                                                   Map<Point, Set<Point>> adj, Point startPoint) {
        Map<Point, Point> parent = bfsParents(startPoint, adj);
        Point p1 = parent.get(door);
        Point p2 = p1 != null ? parent.get(p1) : null;
        Point p3 = p2 != null ? parent.get(p2) : null;
        for (Point cand : new Point[]{p2, p3}) {
            if (cand != null && RoomIds.MONSTER_5.equals(labels.get(cand))) return true;
        }
        return false;
    }

    private static Map<Point, Point> bfsParents(Point start, Map<Point, Set<Point>> adj) {
        Map<Point, Point> parent = new HashMap<>();
        if (start == null) return parent;
        Deque<Point> q = new ArrayDeque<>();
        Set<Point> seen = new HashSet<>();
        q.add(start); seen.add(start);
        while (!q.isEmpty()) {
            Point cur = q.poll();
            for (Point nb : adj.getOrDefault(cur, Set.of())) {
                if (seen.add(nb)) { parent.put(nb, cur); q.add(nb); }
            }
        }
        return parent;
    }

    private static Point findM5WithGapBeforeDoor(Point door, Map<Point, String> labels,
                                                  Map<Point, Set<Point>> adj, Point startPoint) {
        if (startPoint == null || door == null) return null;

        Map<Point, Point> parent = bfsParents(startPoint, adj);

        Point p1 = parent.get(door);
        Point p2 = p1 != null ? parent.get(p1) : null;
        Point p3 = p2 != null ? parent.get(p2) : null;

        for (Point cand : new Point[]{p2, p3}) {
            if (cand == null || cand.equals(startPoint)) continue;
            if (!isReplaceableStraightCorridor(cand, labels, adj)) continue;
            if (!DungeonConstraints.isFarFromAll(adj, cand, DungeonConstraints.monsterPoints(labels), DungeonAlgo.MONSTER_MIN_DIST)) continue;
            if (!gapCellsAreCorridors(cand, door, parent, labels, adj)) continue;
            return cand;
        }
        return null;
    }

    private static boolean isReplaceableStraightCorridor(Point cand, Map<Point, String> labels,
                                                          Map<Point, Set<Point>> adj) {
        Set<Point> nb = adj.getOrDefault(cand, Set.of());
        if (nb.size() != 2) return false;
        List<Point> nbs = new ArrayList<>(nb);
        if (!isStraight(nbs.get(0), nbs.get(1))) return false;

        String lbl = labels.get(cand);
        if (lbl != null && (lbl.equals(RoomIds.MONSTER_2) || lbl.equals(RoomIds.PRISON)
                || lbl.equals(RoomIds.START) || lbl.equals(RoomIds.LOOT_1)
                || lbl.equals(RoomIds.MONSTER_1) || lbl.equals(RoomIds.MONSTER_3)
                || lbl.equals(RoomIds.FOUNTAIN) || lbl.equals(RoomIds.OGRE)
                || lbl.equals(RoomIds.DOOR_1) || lbl.equals(RoomIds.DOOR_2)
                || lbl.startsWith("T") || lbl.startsWith("Ca"))) {
            return false;
        }
        return lbl == null
                || RoomPools.CORRIDORS_P1_P2.contains(lbl)
                || lbl.equals(RoomIds.WELL)
                || lbl.equals(RoomIds.MONSTER_4)
                || lbl.equals(RoomIds.MONSTER_5)
                || lbl.equals(RoomIds.CORRIDOR_TURN);
    }

    private static boolean gapCellsAreCorridors(Point from, Point door, Map<Point, Point> parent,
                                                 Map<Point, String> labels, Map<Point, Set<Point>> adj) {
        Point cur = parent.get(door);
        int guard = 0;
        while (cur != null && !cur.equals(from) && guard++ < 8) {
            Set<Point> nb = adj.getOrDefault(cur, Set.of());
            if (nb.size() != 2) return false;
            Shape sh = DungeonLabels.shapeOf(nb);
            if (sh != Shape.STRAIGHT && sh != Shape.TURN) return false;

            String lbl = labels.get(cur);
            if (lbl != null) {
                boolean okGap = RoomPools.CORRIDORS_P1_P2.contains(lbl)
                        || lbl.equals(RoomIds.CORRIDOR_TURN)
                        || lbl.equals(RoomIds.WELL)
                        || lbl.equals("I2");
                if (!okGap) return false;
            }
            cur = parent.get(cur);
        }
        return cur != null && cur.equals(from);
    }
}
