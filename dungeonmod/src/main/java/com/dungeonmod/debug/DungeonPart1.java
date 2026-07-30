package com.dungeonmod.debug;

import com.dungeonmod.debug.DungeonAlgo.Point;
import com.dungeonmod.debug.DungeonAlgo.RoomIds;
import com.dungeonmod.debug.DungeonAlgo.RoomPools;
import com.dungeonmod.debug.DungeonAlgo.Shape;
import com.dungeonmod.debug.DungeonAlgo.Theme;
import com.dungeonmod.debug.DungeonAlgo.TavernResult;
import com.dungeonmod.debug.DungeonAlgo.TreeResult;

import java.util.*;

/**
 * Génération/analyse de la première partie du donjon (grotte de départ),
 * chemin de taverne et M5 avant porte1.
 *
 * Extraction mécanique depuis DungeonAlgo : l'objectif est de déplacer le code
 * sans modifier volontairement le comportement.
 */
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

    static Map<Point, String> analyzePart1(Point startPoint, Map<Point, Set<Point>> adj, Random rng) {
        DungeonLabelState labelState = new DungeonLabelState();
        labelState.setTheme(adj.keySet(), Theme.P12);
        // Les labels finaux ne sont pas encore construits ici : on ne pose que les specials.
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

        // Porte1 : leaf avec parent couloir DROIT et profondeur ≥3 (M5 + 1–2 gap)
        // Parcours : … → M5 → [1–2 C/I2] → porte → chemin taverne
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
            // Fallback : leaf la plus profonde
            for (Point leaf : others) {
                int depth = depthFromStart.getOrDefault(leaf, 0);
                if (depth > bestPorteScore) { bestPorteScore = depth; porte = leaf; }
            }
        }
        if (porte == null) porte = others.get(0);
        others.remove(porte);
        // others[0] était la porte : on l'a retirée, les indices loot/M1 suivent
        labelState.putSpecial(startPoint, RoomIds.START);
        labelState.putSpecial(porte, RoomIds.DOOR_1);
        labelState.putSpecial(prison, RoomIds.PRISON);
        Point prisonParent = adj.get(prison).iterator().next();
        labelState.putSpecial(prisonParent, RoomIds.MONSTER_2);
        // others a déjà la porte retirée → indice 0 = loot, le reste = feuilles candidates
        labelState.putSpecial(others.get(0), RoomIds.LOOT_1);

        // ESPACEMENT MONSTRES (règle dure, conversation 3) : chaque salle monstre (M1-M5)
        // doit être à distance >= DungeonAlgo.MONSTER_MIN_DIST des autres (au moins 2 salles neutres
        // entre elles). Infaisable => rejet (return null => retry amont). M2 (parent de la
        // prison) est structurelle et sert de point de référence.
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

        // 2e salle monstre de P1 : M4 (couloir) ou M3 (feuille) selon le tirage, avec repli
        // sur l'autre variante si l'espacement est impossible. Échec des deux => rejet (null).
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

        // Les feuilles non utilisées restent génériques : elles deviendront "cul"
        // lors de la construction finale des labels depuis adj + theme.

        for (Point n : cNodes) {
            if (!labelState.hasSpecial(n)) { labelState.putSpecial(n, RoomIds.WELL); break; }
        }

        // Les labels génériques (C/I/cul) sont construits à la fin depuis l'adj réelle.
        // Construction finale P1 : adj = géométrie, theme = P12, specials = salles imposées.
        // L'ordre reproduit l'ancien flux autant que possible pour les variantes C1/C2/C3.
        List<Point> genericOrder = new ArrayList<>();
        genericOrder.addAll(freeLeaves);
        genericOrder.addAll(cNodes);
        genericOrder.addAll(i2Nodes);
        genericOrder.addAll(i3Nodes);
        genericOrder.addAll(i4Nodes);
        Map<Point, String> finalLabels = labelState.buildLabels(adj, rng, genericOrder);

        if (!validatePart1SpecialShapes(finalLabels, adj)) return null;

        // Règle cul-de-sac (conversation 3) : jamais au bout d'une ligne droite
        // (virage/intersection requis) — sinon rejet et retry amont.
        if (!DungeonConstraints.enforceDeadEndAfterTurn(finalLabels, adj, rng)) return null;
        if (!validatePart1SpecialShapes(finalLabels, adj)) return null;
        return finalLabels;
    }

    /** Vérifie que les specials P1 sont restés posés sur une forme compatible. */
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
        // Direction sortante porte → chemin (alignée sur l'entrée intérieure)
        int dx = porte.x() - parent.x(), dy = porte.y() - parent.y();

        int maxLen = 2 + rng.nextInt(4);
        int cx = porte.x(), cy = porte.y();
        boolean lastStraight = false;
        List<Point> pathCells = new ArrayList<>();
        // Snapshot adj pour tester colinearRunAfterEdge avant d'ajouter (chemins temporaires)
        Map<Point, Set<Point>> tmpAdj = DungeonTreeBuilder.copyAdj(adj);
        Point curTmp = porte;

        for (int i = 0; i < maxLen; i++) {
            // Ne PAS forcer le droit : l'approche intérieure (M5→gap→porte) a déjà
            // consommé du budget colinéaire. On tourne si la droite dépasserait MAX.
            Point straight = new Point(cx + dx, cy + dy);
            boolean straightOk = !straight.isOutOfBounds() && !tmpAdj.containsKey(straight)
                    && DungeonConstraints.colinearRunAfterEdge(curTmp, straight, tmpAdj) <= DungeonAlgo.MAX_COLINEAR_RUN;
            // La première cellule après la porte doit continuer dans l'axe : la structure
            // "porte" est un couloir droit, pas un virage. Si l'axe dépasse la limite
            // colinéaire, on rejette ce layout et on laisse le retry amont choisir mieux.
            if (i == 0 && !straightOk) return null;
            boolean goStraight = (i == 0) || (straightOk && (lastStraight ? rng.nextBoolean() : rng.nextFloat() < 0.35f));
            // Si droit impossible ou non choisi → virage
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
                    // Dernier recours : droit si encore possible
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
            // Enregistre dans tmpAdj pour les tests suivants
            tmpAdj.putIfAbsent(curTmp, new HashSet<>());
            tmpAdj.putIfAbsent(next, new HashSet<>());
            tmpAdj.get(curTmp).add(next);
            tmpAdj.get(next).add(curTmp);
            pathCells.add(next);
            curTmp = next;
            cx = next.x(); cy = next.y();
            lastStraight = goStraight;
        }
        // Besoin d'au moins 2 cellules de chemin vers la taverne
        if (pathCells.size() < 2) return null;

        Point t1 = new Point(cx + dx, cy + dy);
        // t1 ne doit pas prolonger une droite adj > MAX (même axe que le chemin)
        Point lastPath = pathCells.get(pathCells.size() - 1);
        if (t1.isOutOfBounds() || adj.containsKey(t1)
                || DungeonConstraints.colinearRunAfterEdge(lastPath, t1, tmpAdj) > DungeonAlgo.MAX_COLINEAR_RUN) {
            // Forcer un virage d'approche de la taverne
            int[][] perp = {{dy, -dx}, {-dy, dx}};
            boolean okT = false;
            for (int[] turn : perp) {
                Point cand = new Point(cx + turn[0], cy + turn[1]);
                if (cand.isOutOfBounds() || adj.containsKey(cand) || tmpAdj.containsKey(cand)) continue;
                if (DungeonConstraints.colinearRunAfterEdge(lastPath, cand, tmpAdj) > DungeonAlgo.MAX_COLINEAR_RUN) continue;
                t1 = cand; dx = turn[0]; dy = turn[1]; okT = true; break;
            }
            if (!okT) return null;
        }
        DungeonCompositeRooms.Placement tavern = DungeonCompositeRooms.plan(adj, t1, dx, dy, DungeonCompositeRooms.TAVERN);
        if (tavern == null) {
            // Essayer les 3 autres orientations (90, 180, 270 degres)
            int[] rdx = {-dy, -dx, dy};
            int[] rdy = {dx, -dy, -dx};
            for (int rot = 0; rot < 3 && tavern == null; rot++) {
                Point rt = new Point(cx + rdx[rot], cy + rdy[rot]);
                if (rt.isOutOfBounds() || adj.containsKey(rt) || tmpAdj.containsKey(rt)) continue;
                if (DungeonConstraints.colinearRunAfterEdge(lastPath, rt, tmpAdj) > DungeonAlgo.MAX_COLINEAR_RUN) continue;
                tavern = DungeonCompositeRooms.plan(adj, rt, rdx[rot], rdy[rot], DungeonCompositeRooms.TAVERN);
            }
        }
        if (tavern == null) return null;

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
            // Déjà un M5 correctement espacé pour cette porte ?
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

        // Chaîne porte ← p1 ← p2 ← p3 (vers le départ)
        Point p1 = parent.get(door);       // distance 1 — trop proche, gap interdit
        Point p2 = p1 != null ? parent.get(p1) : null; // distance 2 — gap = 1 cellule
        Point p3 = p2 != null ? parent.get(p2) : null; // distance 3 — gap = 2 cellules

        for (Point cand : new Point[]{p2, p3}) {
            if (cand == null || cand.equals(startPoint)) continue;
            if (!isReplaceableStraightCorridor(cand, labels, adj)) continue;
            // ESPACEMENT MONSTRES (règle dure, conversation 3) : la M5 posée ici tardivement
            // doit aussi être à distance >= DungeonAlgo.MONSTER_MIN_DIST de toute salle monstre existante.
            if (!DungeonConstraints.isFarFromAll(adj, cand, DungeonConstraints.monsterPoints(labels), DungeonAlgo.MONSTER_MIN_DIST)) continue;
            // Les nœuds entre cand et door doivent rester des couloirs structurels (gap)
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
            // STRICT géométrie : deg 2 seulement → C ou I2, jamais intersection
            if (nb.size() != 2) return false;
            Shape sh = DungeonLabels.shapeOf(nb);
            if (sh != Shape.STRAIGHT && sh != Shape.TURN) return false;

            String lbl = labels.get(cur);
            if (lbl != null) {
                boolean okGap = RoomPools.CORRIDORS_P1_P2.contains(lbl)
                        || lbl.equals(RoomIds.CORRIDOR_TURN)
                        || lbl.equals(RoomIds.WELL)
                        || lbl.equals("I2");
                // Refuse I3/I4/M*/spéciales dans le gap
                if (!okGap) return false;
            }
            cur = parent.get(cur);
        }
        return cur != null && cur.equals(from);
    }
}
