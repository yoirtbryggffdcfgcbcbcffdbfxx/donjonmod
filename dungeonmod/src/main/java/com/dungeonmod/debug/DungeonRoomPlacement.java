package com.dungeonmod.debug;

import com.dungeonmod.debug.DungeonAlgo.Point;
import com.dungeonmod.debug.RoomType.Size3D;

import java.util.*;

/**
 * Reservation/placement GENERIQUE des salles multi-cellules.
 *
 * Une salle est decrite par sa {@link Size3D} (defaut 1x1x1). Son empreinte XZ
 * occupe size.x() x size.z() cellules, repetee sur size.y() couches a partir de
 * la couche de l'ancre. Cela permet des salles du type 2x12x4 sans logique
 * specifique par salle : on reserve le volume, on maille, et si size.y() > 1 on
 * relie nativement les couches par des aretes verticales.
 *
 * Les cellules reservees sont ajoutees a l'adjacence comme noeuds vides (sans
 * label) : la generation ne les traverse pas, et TestGenerator ne les pose pas
 * (il n'itere que les labels) -> un seul NBT est pose a l'ancre.
 */
final class DungeonRoomPlacement {
    private DungeonRoomPlacement() {}

    /** Empreinte XZ sur la couche de l'ancre (size.x() x size.z() cellules). */
    static List<Point> footprint(Point anchor, Size3D size) {
        List<Point> out = new ArrayList<>();
        for (int dx = 0; dx < size.x(); dx++)
            for (int dz = 0; dz < size.z(); dz++)
                out.add(new Point(anchor.x() + dx, anchor.y() + dz, anchor.level()));
        return out;
    }

    /** Volume complet : empreinte XZ repetee sur size.y() couches. */
    static List<Point> volume(Point anchor, Size3D size) {
        List<Point> out = new ArrayList<>();
        for (int dy = 0; dy < size.y(); dy++)
            for (Point p : footprint(anchor, size))
                out.add(p.atLevel(anchor.level() + dy));
        return out;
    }

    /** Vrai si toute l'empreinte est libre (in-bounds, absente de adj hors cellules autorisees). */
    static boolean isFootprintFree(Map<Point, Set<Point>> adj, Point anchor, Size3D size, Set<Point> allowed) {
        Set<Point> allow = allowed == null ? Set.of() : allowed;
        for (Point p : footprint(anchor, size)) {
            if (p.isOutOfBounds()) return false;
            if (!allow.contains(p) && adj.containsKey(p)) return false;
        }
        return true;
    }

    /** Cree les noeuds vides de l'empreinte (reservation sur la couche de l'ancre). */
    static void reserveFootprint(Map<Point, Set<Point>> adj, Point anchor, Size3D size) {
        for (Point p : footprint(anchor, size)) adj.computeIfAbsent(p, k -> new HashSet<>());
    }

    /** Maille l'empreinte (voisins orthogonaux) sur chaque couche : volume connexe. */
    static void meshFootprint(Map<Point, Set<Point>> adj, Point anchor, Size3D size) {
        for (int dy = 0; dy < size.y(); dy++)
            for (int dx = 0; dx < size.x(); dx++)
                for (int dz = 0; dz < size.z(); dz++) {
                    Point a = new Point(anchor.x() + dx, anchor.y() + dz, anchor.level() + dy);
                    adj.computeIfAbsent(a, k -> new HashSet<>());
                    if (dx > 0) connect(adj, a, new Point(anchor.x() + dx - 1, anchor.y() + dz, anchor.level() + dy));
                    if (dz > 0) connect(adj, a, new Point(anchor.x() + dx, anchor.y() + dz - 1, anchor.level() + dy));
                }
    }

    /**
     * Relie verticalement chaque colonne de l'empreinte sur size.y() couches.
     * C'est le vertical "opt-in" : une salle de size.y() > 1 obtient ses aretes
     * verticales nativement, sans toucher aux regles de forme planaires.
     */
    static void linkVertical(Map<Point, Set<Point>> adj, Point anchor, Size3D size) {
        for (int dx = 0; dx < size.x(); dx++)
            for (int dz = 0; dz < size.z(); dz++)
                for (int dy = 0; dy + 1 < size.y(); dy++) {
                    Point a = new Point(anchor.x() + dx, anchor.y() + dz, anchor.level() + dy);
                    Point b = new Point(anchor.x() + dx, anchor.y() + dz, anchor.level() + dy + 1);
                    connect(adj, a, b);
                }
    }

    private static void connect(Map<Point, Set<Point>> adj, Point a, Point b) {
        adj.computeIfAbsent(a, k -> new HashSet<>());
        adj.computeIfAbsent(b, k -> new HashSet<>());
        adj.get(a).add(b);
        adj.get(b).add(a);
    }
}
