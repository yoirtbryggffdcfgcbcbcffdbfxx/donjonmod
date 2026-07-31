package com.dungeonmod.debug;

import com.dungeonmod.debug.DungeonAlgo.Point;
import com.dungeonmod.debug.DungeonAlgo.Shape;
import com.dungeonmod.debug.DungeonAlgo.Theme;
import com.dungeonmod.debug.DungeonAlgo.TavernResult;
import com.dungeonmod.debug.DungeonAlgo.TreeResult;

import java.util.*;

final class DungeonPart1 {
    private DungeonPart1() {}

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
        return DungeonTreeBuilder.generateRawTree(DungeonAlgo.PART1_TARGET_MIN, DungeonAlgo.PART1_TARGET_MAX, DungeonAlgo.PART1_MAX_I3, DungeonAlgo.PART1_MAX_I4, DungeonAlgo.PART1_STRAIGHT_WEIGHT, null, null, DungeonAlgo.MAX_COLINEAR_RUN, rng);
    }

    static Map<Point, RoomType> analyzePart1(Point startPoint, Map<Point, Set<Point>> adj, Random rng) {
        DungeonLabelState labelState = new DungeonLabelState();
        labelState.setTheme(adj.keySet(), Theme.P12);
        List<Point> leaves = new ArrayList<>();
        for (var e : adj.entrySet()) if (e.getValue().size() == 1 && !e.getKey().equals(startPoint)) leaves.add(e.getKey());
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

        Map<Point, Integer> depthFromStart = new HashMap<>();
        {
            Deque<Point> dq = new ArrayDeque<>();
            dq.add(startPoint);
            depthFromStart.put(startPoint, 0);
            while (!dq.isEmpty()) {
                Point cur = dq.poll();
                int d0 = depthFromStart.get(cur);
                for (Point nb : adj.getOrDefault(cur, Set.of())) {
                    if (!depthFromStart.containsKey(nb)) {
                        depthFromStart.put(nb, d0 + 1);
                        dq.add(nb);
                    }
                }
            }
        }
        Point porte = null;
        int bestPorteScore = -1;
        for (Point leaf : others) {
            int depth = depthFromStart.getOrDefault(leaf, 0);
            if (depth < 3) continue;
            Point par = adj.get(leaf).iterator().next();
            if (par.equals(startPoint)) continue;
            Set<Point> pnb = adj.get(par);
            if (pnb == null || pnb.size() != 2) continue;
            List<Point> pnbs = new ArrayList<>(pnb);
            if (!isStraight(pnbs.get(0), pnbs.get(1))) continue;
            int score = depth + 10;
            if (score > bestPorteScore) { bestPorteScore = score; porte = leaf; }
        }
        if (porte == null) {
            for (Point leaf : others) {
                int depth = depthFromStart.getOrDefault(leaf, 0);
                if (depth > bestPorteScore) { bestPorteScore = depth; porte = leaf; }
            }
        }
        if (porte == null) porte = others.get(0);
        others.remove(porte);
        labelState.putSpecial(startPoint, RoomType.START);
        labelState.putSpecial(porte, RoomType.DOOR_1);
        labelState.putSpecial(prison, RoomType.PRISON);
        Point prisonParent = adj.get(prison).iterator().next();
        labelState.putSpecial(prisonParent, RoomType.MONSTER_2);
        labelState.putSpecial(others.get(0), RoomType.LOOT_1);

        Set<Point> monsters = new HashSet<>();
        monsters.add(prisonParent);
        List<Point> freeLeaves = new ArrayList<>();
        for (int i = 1; i < others.size(); i++) freeLeaves.add(others.get(i));
        Collections.shuffle(freeLeaves, rng);

        Point m1 = DungeonConstraints.firstFar(adj, freeLeaves, monsters, DungeonAlgo.MONSTER_MIN_DIST);
        if (m1 == null) return null;
        labelState.putSpecial(m1, RoomType.MONSTER_1);
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

        String secondMonster = null; // legacy string for internal flow control
        for (int variant = 0; variant < 2 && secondMonster == null; variant++) {
            boolean tryCorr = (variant == 0) == isM4;
            if (tryCorr) {
                List<Point> validC = new ArrayList<>();
                for (Point n : cNodes) {
                    if (!labelState.hasSpecial(n) && DungeonConstraints.isFarFromAll(adj, n, monsters, DungeonAlgo.MONSTER_MIN_DIST)) validC.add(n);
                }
                if (!validC.isEmpty()) {
                    Point m4Point = validC.get(rng.nextInt(validC.size()));
                    if (rng.nextBoolean()) {
                        labelState.putSpecial(prisonParent, RoomType.MONSTER_4);
                        labelState.putSpecial(m4Point, RoomType.MONSTER_2);
                        monsters.add(m4Point);
                        secondMonster = "M2";
                    } else {
                        labelState.putSpecial(m4Point, RoomType.MONSTER_4);
                        monsters.add(m4Point);
                        secondMonster = "M4";
                    }
                }
            } else {
                Point m3 = DungeonConstraints.firstFar(adj, freeLeaves, monsters, DungeonAlgo.MONSTER_MIN_DIST);
                if (m3 != null) {
                    labelState.putSpecial(m3, RoomType.MONSTER_3);
                    monsters.add(m3);
                    freeLeaves.remove(m3);
                    secondMonster = "M3";
                }
            }
        }
        if (secondMonster == null) return null;

        boolean wellPlaced = false;
        for (Point n : cNodes) {
            if (!labelState.hasSpecial(n)) { labelState.putSpecial(n, RoomType.WELL); wellPlaced = true; break; }
        }
        if (!wellPlaced) return null;

        List<Point> genericOrder = new ArrayList<>();
        genericOrder.addAll(freeLeaves);
        genericOrder.addAll(cNodes);
        genericOrder.addAll(i2Nodes);
        genericOrder.addAll(i3Nodes);
        genericOrder.addAll(i4Nodes);
        Map<Point, RoomType> finalLabels = labelState.buildLabels(adj, rng, genericOrder);

        if (!validatePart1SpecialShapes(finalLabels, adj)) return null;
        if (!DungeonConstraints.enforceDeadEndAfterTurn(finalLabels, adj, rng)) return null;
        if (!validatePart1SpecialShapes(finalLabels, adj)) return null;
        return finalLabels;
    }

    private static boolean validatePart1SpecialShapes(Map<Point, RoomType> labels, Map<Point, Set<Point>> adj) {
        for (var e : labels.entrySet()) {
            RoomType label = e.getValue();
            if (label == null) continue;
            Shape shape = DungeonLabels.shapeOf(adj.getOrDefault(e.getKey(), Set.of()));
            if (label == RoomType.START || label == RoomType.DOOR_1
                    || label == RoomType.PRISON || label == RoomType.LOOT_1
                    || label == RoomType.MONSTER_1 || label == RoomType.MONSTER_3) {
                if (shape != Shape.DEAD_END) return false;
            } else if (label == RoomType.MONSTER_2 || label == RoomType.MONSTER_4
                    || label == RoomType.WELL) {
                if (shape != Shape.STRAIGHT) return false;
            }
        }
        return true;
    }

    static TavernResult placeTavernAndPath(Map<Point, Set<Point>> adj, Point porte, Random rng) {
        Point parent = adj.get(porte).iterator().next();
        int baseDx = porte.x() - parent.x(), baseDy = porte.y() - parent.y();

        // Solution "Maline" : Décalages à essayer
        int[][] offsets = {{0,0}, {1,0}, {-1,0}, {0,1}, {0,-1}, {1,1}, {-1,-1}, {1,-1}, {-1,1}};

        for (int maxLen = 2; maxLen <= 7; maxLen++) {
            int dx = baseDx, dy = baseDy;
            int cx = porte.x(), cy = porte.y();
            List<Point> pathCells = new ArrayList<>();
            Map<Point, Set<Point>> tmpAdj = DungeonTreeBuilder.copyAdj(adj);
            Point curTmp = porte;

            boolean failed = false;
            for (int i = 0; i < maxLen; i++) {
                Point straight = new Point(cx + dx, cy + dy);
                boolean straightOk = !straight.isOutOfBounds() && !tmpAdj.containsKey(straight)
                        && DungeonConstraints.colinearRunAfterEdge(curTmp, straight, tmpAdj) <= DungeonAlgo.MAX_COLINEAR_RUN;
                if (i == 0 && !straightOk) { failed = true; break; }
                boolean goStraight = (i == 0) || (straightOk && rng.nextFloat() < 0.35f);
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
                        turned = true; break;
                    }
                    if (!turned) {
                        if (!straightOk) { failed = true; break; }
                        ndx = dx; ndy = dy;
                        goStraight = true;
                    }
                } else {
                    ndx = dx; ndy = dy;
                }
                Point next = new Point(cx + ndx, cy + ndy);
                if (tmpAdj.containsKey(next) || next.isOutOfBounds()) { failed = true; break; }
                if (DungeonConstraints.colinearRunAfterEdge(curTmp, next, tmpAdj) > DungeonAlgo.MAX_COLINEAR_RUN) { failed = true; break; }
                tmpAdj.putIfAbsent(curTmp, new HashSet<>());
                tmpAdj.putIfAbsent(next, new HashSet<>());
                tmpAdj.get(curTmp).add(next);
                tmpAdj.get(next).add(curTmp);
                pathCells.add(next);
                curTmp = next;
                cx = next.x(); cy = next.y();
            }
            if (failed || pathCells.size() < 2) continue;

            Point t1 = new Point(cx + dx, cy + dy);
            Point lastPath = pathCells.get(pathCells.size() - 1);
            if (t1.isOutOfBounds() || adj.containsKey(t1)
                    || DungeonConstraints.colinearRunAfterEdge(lastPath, t1, tmpAdj) > DungeonAlgo.MAX_COLINEAR_RUN) {
                int[][] perp = {{dy, -dx}, {-dy, dx}};
                boolean okT = false;
                for (int[] turn : perp) {
                    Point cand = new Point(cx + turn[0], cy + turn[1]);
                    if (cand.isOutOfBounds() || adj.containsKey(cand) || tmpAdj.containsKey(cand)) continue;
                    if (DungeonConstraints.colinearRunAfterEdge(lastPath, cand, tmpAdj) > DungeonAlgo.MAX_COLINEAR_RUN) continue;
                    t1 = cand; dx = turn[0]; dy = turn[1]; okT = true; break;
                }
                if (!okT) continue;
            }

            // Solution "Chirurgicale" : Pré-vérification de l'espace avant plan()
            // Solution "Maline" : Essayer plusieurs décalages et rotations
            DungeonCompositeRooms.Placement tavern = null;
            int[] rdx = {-dy, -dx, dy};
            int[] rdy = {dx, -dy, -dx};
            
            for (int[] offset : offsets) {
                for (int rot = 0; rot < 3 && tavern == null; rot++) {
                    Point rt = new Point(cx + rdx[rot] + offset[0], cy + rdy[rot] + offset[1]);
                    if (rt.isOutOfBounds() || adj.containsKey(rt) || tmpAdj.containsKey(rt)) continue;
                    if (DungeonConstraints.colinearRunAfterEdge(lastPath, rt, tmpAdj) > DungeonAlgo.MAX_COLINEAR_RUN) continue;
                    
                    // Pré-vérification de l'espace (Solution Chirurgicale)
                    if (!DungeonCompositeRooms.isSpaceFree(adj, rt, rdx[rot], rdy[rot], DungeonCompositeRooms.TAVERN)) {
                        continue;
                    }
                    
                    tavern = DungeonCompositeRooms.plan(adj, rt, rdx[rot], rdy[rot], DungeonCompositeRooms.TAVERN);
                }
                if (tavern != null) break;
            }
            
            // Si pas trouvé avec décalages, essayer sans décalage (ancienne méthode)
            if (tavern == null) {
                if (DungeonCompositeRooms.isSpaceFree(adj, t1, dx, dy, DungeonCompositeRooms.TAVERN)) {
                    tavern = DungeonCompositeRooms.plan(adj, t1, dx, dy, DungeonCompositeRooms.TAVERN);
                }
                if (tavern == null) {
                    for (int rot = 0; rot < 3 && tavern == null; rot++) {
                        Point rt = new Point(cx + rdx[rot], cy + rdy[rot]);
                        if (rt.isOutOfBounds() || adj.containsKey(rt) || tmpAdj.containsKey(rt)) continue;
                        if (DungeonConstraints.colinearRunAfterEdge(lastPath, rt, tmpAdj) > DungeonAlgo.MAX_COLINEAR_RUN) continue;
                        if (!DungeonCompositeRooms.isSpaceFree(adj, rt, rdx[rot], rdy[rot], DungeonCompositeRooms.TAVERN)) continue;
                        tavern = DungeonCompositeRooms.plan(adj, rt, rdx[rot], rdy[rot], DungeonCompositeRooms.TAVERN);
                    }
                }
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
        return null;
    }

    private static Point findPointByValue(Map<Point, RoomType> map, RoomType value) {
        if (map == null || value == null) return null;
        for (var e : map.entrySet()) if (value == e.getValue()) return e.getKey();
        return null;
    }

    private static boolean isStraight(Point p1, Point p2) {
        return p1.x() == p2.x() || p1.y() == p2.y();
    }

    static int placeMonster5OnDoorPaths(Map<Point, RoomType> labels, Map<Point, Set<Point>> adj,
                                                 Point startPoint) {
        int placed = 0;
        Point door = findPointByValue(labels, RoomType.DOOR_1);
        if (door == null) return 0;
        if (hasM5WithGapBeforeDoor(door, labels, adj, startPoint)) placed++;
        else {
            Point candidate = findM5WithGapBeforeDoor(door, labels, adj, startPoint);
            if (candidate != null) { labels.put(candidate, RoomType.MONSTER_5); placed++; }
        }
        return placed;
    }

    private static boolean hasM5WithGapBeforeDoor(Point door, Map<Point, RoomType> labels,
                                                   Map<Point, Set<Point>> adj, Point startPoint) {
        Map<Point, Point> parent = bfsParents(startPoint, adj);
        Point p1 = parent.get(door);
        Point p2 = p1 != null ? parent.get(p1) : null;
        Point p3 = p2 != null ? parent.get(p2) : null;
        for (Point cand : new Point[]{p2, p3}) {
            if (cand != null && labels.get(cand) == RoomType.MONSTER_5) return true;
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

    private static Point findM5WithGapBeforeDoor(Point door, Map<Point, RoomType> labels,
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

    private static boolean isReplaceableStraightCorridor(Point cand, Map<Point, RoomType> labels,
                                                          Map<Point, Set<Point>> adj) {
        Set<Point> nb = adj.getOrDefault(cand, Set.of());
        if (nb.size() != 2) return false;
        List<Point> nbs = new ArrayList<>(nb);
        if (!isStraight(nbs.get(0), nbs.get(1))) return false;
        RoomType lbl = labels.get(cand);
        if (lbl != null && (lbl == RoomType.MONSTER_2 || lbl == RoomType.PRISON
                || lbl == RoomType.START || lbl == RoomType.LOOT_1
                || lbl == RoomType.MONSTER_1 || lbl == RoomType.MONSTER_3
                || lbl == RoomType.FOUNTAIN || lbl == RoomType.OGRE
                || lbl == RoomType.DOOR_1 || lbl == RoomType.DOOR_2
                || lbl.isTavern() || lbl.isCamp())) {
            return false;
        }
        return lbl == null
                || RoomType.CORRIDORS_P1_P2.contains(lbl)
                || lbl == RoomType.WELL
                || lbl == RoomType.MONSTER_4
                || lbl == RoomType.MONSTER_5
                || lbl == RoomType.I2;
    }

    private static boolean gapCellsAreCorridors(Point from, Point door, Map<Point, Point> parent,
                                                 Map<Point, RoomType> labels, Map<Point, Set<Point>> adj) {
        Point cur = parent.get(door);
        int guard = 0;
        while (cur != null && !cur.equals(from) && guard++ < 8) {
            Set<Point> nb = adj.getOrDefault(cur, Set.of());
            if (nb.size() != 2) return false;
            Shape sh = DungeonLabels.shapeOf(nb);
            if (sh != Shape.STRAIGHT && sh != Shape.TURN) return false;
            RoomType lbl = labels.get(cur);
            if (lbl != null) {
                boolean okGap = RoomType.CORRIDORS_P1_P2.contains(lbl)
                        || lbl == RoomType.I2
                        || lbl == RoomType.WELL;
                if (!okGap) return false;
            }
            cur = parent.get(cur);
        }
        return cur != null && cur.equals(from);
    }
}
