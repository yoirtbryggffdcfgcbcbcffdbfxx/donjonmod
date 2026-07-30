package com.dungeonmod.debug;

import com.dungeonmod.debug.DungeonAlgo.Point;
import com.dungeonmod.debug.DungeonAlgo.Theme;

import java.util.*;

/**
 * Etat intermediaire de labeling :
 *
 * - themes  : dit a quel style appartient une cellule generique (P12, DJ, GOBLIN)
 * - specials: dit quelle salle speciale/contenu gameplay doit remplacer le generique
 * - labels  : ne sont construits qu'a la fin via buildLabels(...)
 */
final class DungeonLabelState {
    private final Map<Point, Theme> themes = new HashMap<>();
    private final Map<Point, RoomType> specials = new HashMap<>();

    void setTheme(Point point, Theme theme) {
        if (point != null && theme != null) themes.put(point, theme);
    }

    void setTheme(Collection<Point> points, Theme theme) {
        if (points == null) return;
        for (Point point : points) setTheme(point, theme);
    }

    void putSpecial(Point point, RoomType label) {
        if (point != null && label != null) specials.put(point, label);
    }

    void putSpecials(Map<Point, RoomType> labels) {
        if (labels == null) return;
        for (var e : labels.entrySet()) putSpecial(e.getKey(), e.getValue());
    }

    /**
     * Importe une map de labels deja construite : les labels generiques deviennent
     * des themes, les autres labels deviennent des specials.
     */
    void absorbLabels(Map<Point, RoomType> labels) {
        if (labels == null) return;
        for (var e : labels.entrySet()) {
            RoomType rt = e.getValue();
            if (rt != null && rt.isGeneric()) setTheme(e.getKey(), rt.theme);
            else putSpecial(e.getKey(), rt);
        }
    }

    boolean hasSpecial(Point point) {
        return specials.containsKey(point);
    }

    RoomType special(Point point) {
        return specials.get(point);
    }

    /** Vue mutable des specials. */
    Map<Point, RoomType> specials() {
        return specials;
    }

    Map<Point, Theme> themes() {
        return themes;
    }

    Map<Point, RoomType> buildLabels(Map<Point, Set<Point>> adj, Random rng) {
        return buildLabels(adj, rng, List.of());
    }

    /**
     * Construit les labels finaux.
     */
    Map<Point, RoomType> buildLabels(Map<Point, Set<Point>> adj, Random rng, Collection<Point> orderedGenericPoints) {
        Map<Point, RoomType> out = new HashMap<>();

        // Specials first (imposed, not derived from adj)
        for (var e : specials.entrySet()) {
            out.put(e.getKey(), e.getValue());
        }

        Set<Point> all = new HashSet<>();
        if (adj != null) all.addAll(adj.keySet());
        all.addAll(themes.keySet());
        all.addAll(specials.keySet());

        if (orderedGenericPoints != null) {
            for (Point point : orderedGenericPoints) {
                putGenericIfNeeded(out, adj, rng, point);
            }
        }

        List<Point> rest = new ArrayList<>(all);
        rest.sort(Comparator.comparingInt(Point::x).thenComparingInt(Point::y));
        for (Point point : rest) {
            putGenericIfNeeded(out, adj, rng, point);
        }
        return out;
    }

    private void putGenericIfNeeded(Map<Point, RoomType> out, Map<Point, Set<Point>> adj, Random rng, Point point) {
        if (point == null || out.containsKey(point)) return;
        Theme theme = themes.get(point);
        if (theme == null) return;
        Set<Point> neighbors = adj == null ? Set.of() : adj.getOrDefault(point, Set.of());
        out.put(point, DungeonLabels.shapeLabel(DungeonLabels.shapeOf(neighbors), theme, rng));
    }
}
