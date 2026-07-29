package com.dungeonmod.debug;

import com.dungeonmod.debug.DungeonAlgo.Point;
import com.dungeonmod.debug.DungeonAlgo.RoomIds;
import com.dungeonmod.debug.DungeonAlgo.RoomPools;
import com.dungeonmod.debug.DungeonAlgo.Shape;

import java.util.*;

/**
 * Contraintes et validations géométriques/gameplay de l'algo de donjon.
 *
 * Cette classe regroupe les helpers qui disent si une position est valide :
 * distances entre monstres, isolement de l'Ogre, culs-de-sac après virage,
 * limites de lignes colinéaires, etc.
 *
 * DungeonAlgo conserve des wrappers pour ne pas casser l'API publique et pour
 * permettre une extraction progressive fichier par fichier.
 */
final class DungeonConstraints {
    private DungeonConstraints() {}

    /** Vrai si le label est une salle monstre (M1-M5 des grottes, MJ1-MJ5 du donjon). */
    static boolean isMonsterLabel(String v) {
        if (v == null) return false;
        return v.equals(RoomIds.MONSTER_1) || v.equals(RoomIds.MONSTER_2)
            || v.equals(RoomIds.MONSTER_3) || v.equals(RoomIds.MONSTER_4)
            || v.equals(RoomIds.MONSTER_5)
            || RoomPools.LEAF_MONSTERS_P3_P4.contains(v) || RoomPools.CORRIDOR_MONSTERS_P3_P4.contains(v);
    }

    /** Vrai pour les culs-de-sac GÉNÉRIQUES (jamais les salles spéciales en cul-de-sac). */
    static boolean isGenericDeadEnd(String v) {
        return RoomIds.DEAD_END.equals(v) || RoomIds.DEAD_END_DJ.equals(v) || RoomIds.GOBLIN_DEAD_END.equals(v);
    }

    /** Tous les points actuellement étiquetés comme salle monstre. */
    static Set<Point> monsterPoints(Map<Point, String> labels) {
        Set<Point> out = new HashSet<>();
        for (var e : labels.entrySet()) if (isMonsterLabel(e.getValue())) out.add(e.getKey());
        return out;
    }

    /** Distances BFS depuis src (en nombre de salles traversées). */
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

    /** Vrai si cand est à distance >= minDist de CHAQUE cible (BFS borné à minDist - 1). */
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
            if (d >= minDist) return true; // rien découvert ensuite ne peut plus être trop proche
            for (Point nb : adj.getOrDefault(p, Set.of())) {
                if (dist.containsKey(nb)) continue;
                dist.put(nb, d + 1);
                if (remaining.remove(nb) && d + 1 < minDist) return false; // cible trop proche
                q.add(nb);
            }
        }
        return true;
    }

    /** Premier candidat (liste pré-mélangée => aléatoire) à distance >= minDist des cibles. */
    static Point firstFar(Map<Point, Set<Point>> adj, List<Point> candidates, Collection<Point> targets, int minDist) {
        for (Point c : candidates) if (isFarFromAll(adj, c, targets, minDist)) return c;
        return null;
    }

    /** Cellule in-bounds et inoccupée par le graphe. */
    static boolean isFreeCell(Map<Point, Set<Point>> adj, Point p) {
        return !p.isOutOfBounds() && !adj.containsKey(p);
    }

    /** Déplace une feuille cul-de-sac : retire leaf et raccroche target à newParent. */
    static void moveDeadEnd(Map<Point, String> labels, Map<Point, Set<Point>> adj,
                            Point leaf, Point oldParent, Point newParent, Point target) {
        adj.get(oldParent).remove(leaf);
        adj.remove(leaf);
        Set<Point> t = new HashSet<>();
        t.add(newParent);
        adj.put(target, t);
        adj.get(newParent).add(target);
        String lbl = labels.remove(leaf);
        labels.put(target, lbl);
    }

    /**
     * PASSE CUL-DE-SAC : un cul-de-sac générique ne doit jamais terminer une ligne droite.
     */
    static boolean enforceDeadEndAfterTurn(Map<Point, String> labels, Map<Point, Set<Point>> adj, Random rng) {
        boolean fixed;
        do {
            fixed = false;
            List<Point> sorted = new ArrayList<>(labels.keySet());
            sorted.sort(Comparator.comparingInt(Point::x).thenComparingInt(Point::y));
            for (Point leaf : sorted) {
                String lbl = labels.get(leaf);
                if (!isGenericDeadEnd(lbl)) continue;
                Set<Point> nb = adj.get(leaf);
                if (nb == null || nb.size() != 1) continue;
                Point parent = nb.iterator().next();
                Set<Point> pAdj = adj.get(parent);
                if (pAdj == null || pAdj.size() != 2) continue; // intersection (ou cas exotique) : déjà valide
                if (DungeonLabels.shapeOf(pAdj) != Shape.STRAIGHT) continue;  // virage : déjà valide
                String parentLbl = labels.get(parent);
                if (parentLbl == null) continue;                 // cellule structurelle non étiquetée : on laisse
                Point gp = null;
                for (Point q : pAdj) if (!q.equals(leaf)) { gp = q; break; }
                if (gp == null) continue;

                boolean moved = false;
                // 1) Rebascule latérale du cul : le parent (générique) devient un virage.
                if (DungeonLabels.genericThemeOf(parentLbl) != null) {
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
                // 2) Rattachement au grand-parent : il devient intersection ; l'ancien parent
                //    générique, désormais feuille, devient un cul-de-sac valide à son tour.
                if (!moved && DungeonLabels.genericThemeOf(parentLbl) != null && DungeonLabels.genericThemeOf(labels.get(gp)) != null
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
                if (!moved) return false; // parent spécial droit ou aucune cellule libre : retry
                fixed = true;             // carte mutée : on recommence un scan complet
                break;
            }
        } while (fixed);
        DungeonLabels.reclassifyGeneric(labels, adj, rng);
        return true;
    }

    /**
     * Longueur max d'une run colinéaire dans l'adj (segments = arêtes alignées).
     */
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

    /** True si aucune droite géométrique de l'adj ne dépasse maxRun. */
    static boolean respectsColinearLimit(Map<Point, Set<Point>> adj, int maxRun) {
        return maxColinearRunInGraph(adj) <= maxRun;
    }

    /**
     * Longueur d'une run COLINÉAIRE dans l'adjacence RÉELLE le long de l'axe (dx,dy)
     * passant par origin (nombre de SEGMENTS = arêtes alignées).
     */
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

    /**
     * Run colinéaire qui résulterait de l'ajout de l'arête from→to.
     */
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
