package com.dungeonmod.debug;

import com.dungeonmod.debug.DungeonAlgo.Point;
import com.dungeonmod.debug.DungeonAlgo.Shape;

import java.util.*;

/**
 * Contraintes et validations geometriques/gameplay de l'algo de donjon.
 */
final class DungeonConstraints {
    private DungeonConstraints() {}

    /** Vrai si le label est une salle monstre. */
    static boolean isMonsterLabel(RoomType v) {
        return v != null && v.isMonster();
    }

    /** Vrai pour les culs-de-sac GENERIQUES (jamais les salles speciales en cul-de-sac). */
    static boolean isGenericDeadEnd(RoomType v) {
        return v != null && v.isGenericDeadEnd();
    }

    /** Tous les points actuellement etiquetes comme salle monstre. */
    static Set<Point> monsterPoints(Map<Point, RoomType> labels) {
        Set<Point> out = new HashSet<>();
        for (var e : labels.entrySet()) if (e.getValue() != null && e.getValue().isMonster()) out.add(e.getKey());
        return out;
    }

    /** Distances BFS depuis src (en nombre de salles traversees). */
    static Map<Point, Integer> bfsDistances(Map<Point, Set<Point>> adj, Point src) {
        Map<Point, Integer> dist = new HashMap<>();
        if (src == null) return dist;
        Queue<Point> q = new ArrayDeque<>();
        dist.put(src, 0); q.add(src);
        while (!q.isEmpty()) {
            Point p = q.poll();
            int d = dist.get(p);
            for (Point nb : adj.getOrDefault(p, Set.of())) {
                if (!dist.containsKey(nb)) { dist.put(nb, d + 1); q.add(nb); }
            }
        }
        return dist;
    }

    /** Vrai si cand est a distance >= minDist de CHAQUE cible (BFS borne a minDist - 1). */
    static boolean isFarFromAll(Map<Point, Set<Point>> adj, Point cand, Collection<Point> targets, int minDist) {
        if (targets == null || targets.isEmpty()) return true;
        Set<Point> remaining = new HashSet<>(targets);
        remaining.remove(cand);
        if (remaining.isEmpty()) return true;
        Map<Point, Integer> dist = new HashMap<>();
        Queue<Point> q = new ArrayDeque<>();
        dist.put(cand, 0); q.add(cand);
        while (!q.isEmpty()) {
            Point p = q.poll();
            int d = dist.get(p);
            if (d >= minDist) return true;
            for (Point nb : adj.getOrDefault(p, Set.of())) {
                if (dist.containsKey(nb)) continue;
                dist.put(nb, d + 1);
                if (remaining.remove(nb) && d + 1 < minDist) return false;
                q.add(nb);
            }
        }
        return true;
    }

    /** Premier candidat (liste pre-melangee => aleatoire) a distance >= minDist des cibles. */
    static Point firstFar(Map<Point, Set<Point>> adj, List<Point> candidates, Collection<Point> targets, int minDist) {
        for (Point c : candidates) if (isFarFromAll(adj, c, targets, minDist)) return c;
        return null;
    }

    /** Cellule in-bounds et inoccupee par le graphe. */
    static boolean isFreeCell(Map<Point, Set<Point>> adj, Point p) {
        return !p.isOutOfBounds() && !adj.containsKey(p);
    }

    /** Deplace une feuille cul-de-sac : retire leaf et raccroche target a newParent. */
    static void moveDeadEnd(Map<Point, RoomType> labels, Map<Point, Set<Point>> adj,
                            Point leaf, Point oldParent, Point newParent, Point target) {
        adj.get(oldParent).remove(leaf);
        adj.remove(leaf);
        Set<Point> t = new HashSet<>();
        t.add(newParent);
        adj.put(target, t);
        adj.get(newParent).add(target);
        RoomType lbl = labels.remove(leaf);
        labels.put(target, lbl);
    }

    /**
     * PASSE CUL-DE-SAC : un cul-de-sac generique ne doit jamais terminer une ligne droite.
     */
    static boolean enforceDeadEndAfterTurn(Map<Point, RoomType> labels, Map<Point, Set<Point>> adj, Random rng) {
        boolean fixed;
        do {
            fixed = false;
            List<Point> sorted = new ArrayList<>(labels.keySet());
            sorted.sort(Comparator.comparingInt(Point::x).thenComparingInt(Point::y));
            for (Point leaf : sorted) {
                RoomType lbl = labels.get(leaf);
                if (lbl == null || !lbl.isGenericDeadEnd()) continue;
                Set<Point> nb = adj.get(leaf);
                if (nb == null || nb.size() != 1) continue;
                Point parent = nb.iterator().next();
                Set<Point> pAdj = adj.get(parent);
                if (pAdj == null || pAdj.size() != 2) continue;
                if (DungeonLabels.shapeOf(pAdj) != Shape.STRAIGHT) continue;
                RoomType parentLbl = labels.get(parent);
                if (parentLbl == null) continue;
                Point gp = null;
                for (Point q : pAdj) if (!q.equals(leaf)) { gp = q; break; }
                if (gp == null) continue;

                boolean moved = false;
                if (parentLbl.isGeneric()) {
                    int dx = parent.x() - gp.x(), dy = parent.y() - gp.y();
                    List<Point> laterals = new ArrayList<>();
                    Point l1 = new Point(parent.x() - dy, parent.y() + dx);
                    Point l2 = new Point(parent.x() + dy, parent.y() - dx);
                    if (isFreeCell(adj, l1)) laterals.add(l1);
                    if (isFreeCell(adj, l2)) laterals.add(l2);
                    if (!laterals.isEmpty()) {
                        Point target = laterals.get(rng.nextInt(laterals.size()));
                        moveDeadEnd(labels, adj, leaf, parent, parent, target);
                        moved = true;
                    }
                }
                if (!moved && parentLbl.isGeneric() && labels.get(gp) != null && labels.get(gp).isGeneric()
                        && adj.get(gp).size() <= 3) {
                    List<Point> opts = new ArrayList<>();
                    for (int[] d : DungeonAlgo.DIR_OFFSET) {
                        Point q = gp.move(d);
                        if (!q.equals(parent) && isFreeCell(adj, q)) opts.add(q);
                    }
                    if (!opts.isEmpty()) {
                        Point target = opts.get(rng.nextInt(opts.size()));
                        moveDeadEnd(labels, adj, leaf, parent, gp, target);
                        moved = true;
                    }
                }
                if (!moved) return false;
                fixed = true;
                break;
            }
        } while (fixed);
        DungeonLabels.reclassifyGeneric(labels, adj, rng);
        return true;
    }

    /** Longueur max d'une run colineaire dans l'adj. */
    static int maxColinearRunInGraph(Map<Point, Set<Point>> adj) {
        int max = 0;
        Set<String> seenAxes = new HashSet<>();
        for (Point p : adj.keySet()) {
            for (Point nb : adj.getOrDefault(p, Set.of())) {
                int dx = Integer.signum(nb.x() - p.x());
                int dy = Integer.signum(nb.y() - p.y());
                if (Math.abs(dx) + Math.abs(dy) != 1) continue;
                int ndx = dx, ndy = dy;
                if (ndx < 0 || (ndx == 0 && ndy < 0)) { ndx = -ndx; ndy = -ndy; }
                Point cur = p;
                while (adj.getOrDefault(cur, Set.of()).contains(cur.move(-ndx, -ndy))) {
                    cur = cur.move(-ndx, -ndy);
                }
                String key = cur.key() + "|" + ndx + "," + ndy;
                if (!seenAxes.add(key)) continue;
                int run = colinearRunInAdj(cur, ndx, ndy, adj);
                if (run > max) max = run;
            }
        }
        return max;
    }

    /** True si aucune droite geometrique de l'adj ne depasse maxRun. */
    static boolean respectsColinearLimit(Map<Point, Set<Point>> adj, int maxRun) {
        return maxColinearRunInGraph(adj) <= maxRun;
    }

    /** Longueur d'une run COLINAIRE dans l'adjacence REELLE le long de l'axe (dx,dy). */
    static int colinearRunInAdj(Point origin, int dx, int dy, Map<Point, Set<Point>> adj) {
        if (dx == 0 && dy == 0) return 0;
        int back = 0;
        Point cur = origin;
        while (back < 16) {
            Point prev = cur.move(-dx, -dy);
            if (!adj.getOrDefault(cur, Set.of()).contains(prev)) break;
            back++;
            cur = prev;
        }
        int forward = 0;
        cur = origin;
        while (forward < 16) {
            Point next = cur.move(dx, dy);
            if (!adj.getOrDefault(cur, Set.of()).contains(next)) break;
            forward++;
            cur = next;
        }
        return back + forward;
    }

    /** Run colineaire qui resulterait de l'ajout de l'arete from->to. */
    static int colinearRunAfterEdge(Point from, Point to, Map<Point, Set<Point>> adj) {
        int dx = to.x() - from.x();
        int dy = to.y() - from.y();
        if (Math.abs(dx) + Math.abs(dy) != 1) return Integer.MAX_VALUE;
        int back = 0;
        Point cur = from;
        while (back < 16) {
            Point prev = cur.move(-dx, -dy);
            if (!adj.getOrDefault(cur, Set.of()).contains(prev)) break;
            back++;
            cur = prev;
        }
        int forward = 0;
        cur = to;
        while (forward < 16) {
            Point next = cur.move(dx, dy);
            if (!adj.getOrDefault(cur, Set.of()).contains(next)) break;
            forward++;
            cur = next;
        }
        return back + 1 + forward;
    }
}
