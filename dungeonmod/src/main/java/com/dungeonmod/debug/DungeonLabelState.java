package com.dungeonmod.debug;

import com.dungeonmod.debug.DungeonAlgo.Point;
import com.dungeonmod.debug.DungeonAlgo.Theme;

import java.util.*;

/**
 * État intermédiaire de labeling :
 *
 * - themes  : dit à quel style appartient une cellule générique (P12, DJ, GOBLIN)
 * - specials: dit quelle salle spéciale/contenu gameplay doit remplacer le générique
 * - labels  : ne sont construits qu'à la fin via buildLabels(...)
 *
 * Objectif long terme : ne plus poser les labels génériques pendant les mutations
 * d'adjacence. L'adj reste la vérité géométrique, les labels deviennent la sortie finale.
 */
final class DungeonLabelState {
    private final Map<Point, Theme> themes = new HashMap<>();
    private final Map<Point, String> specials = new HashMap<>();

    void setTheme(Point point, Theme theme) {
        if (point != null && theme != null) themes.put(point, theme);
    }

    void setTheme(Collection<Point> points, Theme theme) {
        if (points == null) return;
        for (Point point : points) setTheme(point, theme);
    }

    void putSpecial(Point point, String label) {
        if (point != null && label != null) specials.put(point, label);
    }

    void putSpecials(Map<Point, String> labels) {
        if (labels == null) return;
        for (var e : labels.entrySet()) putSpecial(e.getKey(), e.getValue());
    }

    /**
     * Importe une map de labels déjà construite : les labels génériques deviennent
     * des thèmes, les autres labels deviennent des specials.
     */
    void absorbLabels(Map<Point, String> labels) {
        if (labels == null) return;
        for (var e : labels.entrySet()) {
            Theme theme = DungeonLabels.genericThemeOf(e.getValue());
            if (theme != null) setTheme(e.getKey(), theme);
            else putSpecial(e.getKey(), e.getValue());
        }
    }

    boolean hasSpecial(Point point) {
        return specials.containsKey(point);
    }

    String special(Point point) {
        return specials.get(point);
    }

    /**
     * Vue mutable des specials, utile pendant la migration progressive de l'ancien code.
     * À terme, les callers devraient passer par putSpecial/hasSpecial.
     */
    Map<Point, String> specials() {
        return specials;
    }

    Map<Point, Theme> themes() {
        return themes;
    }

    Map<Point, String> buildLabels(Map<Point, Set<Point>> adj, Random rng) {
        return buildLabels(adj, rng, List.of());
    }

    /**
     * Construit les labels finaux.
     *
     * orderedGenericPoints permet de préserver temporairement l'ordre historique de
     * consommation du RNG pour les variantes C1/C2/C3 pendant la migration.
     */
    Map<Point, String> buildLabels(Map<Point, Set<Point>> adj, Random rng, Collection<Point> orderedGenericPoints) {
        Map<Point, String> out = new HashMap<>();
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

    private void putGenericIfNeeded(Map<Point, String> out, Map<Point, Set<Point>> adj, Random rng, Point point) {
        if (point == null || out.containsKey(point)) return;
        Theme theme = themes.get(point);
        if (theme == null) return;
        Set<Point> neighbors = adj == null ? Set.of() : adj.getOrDefault(point, Set.of());
        out.put(point, DungeonLabels.shapeLabel(DungeonLabels.shapeOf(neighbors), theme, rng));
    }
}
