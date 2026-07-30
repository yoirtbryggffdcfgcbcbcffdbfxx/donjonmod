package com.dungeonmod.debug;

import com.dungeonmod.debug.DungeonAlgo.Point;
import com.dungeonmod.debug.DungeonAlgo.Shape;
import com.dungeonmod.debug.DungeonAlgo.Theme;

import java.util.*;

/**
 * Helpers de classification des salles.
 *
 * Regle importante : les labels structurels generiques (C/I/cul, CJ/IJ/culDJ,
 * CG/GI/CDG) doivent etre derives de l'adjacence reelle du graphe, pas d'une
 * decision locale prise pendant la generation.
 */
final class DungeonLabels {
    private DungeonLabels() {}

    /** Deduit la forme d'un noeud depuis son ensemble de voisins. */
    static Shape shapeOf(Set<Point> neighbors) {
        int deg = neighbors == null ? 0 : neighbors.size();
        if (deg <= 1) return Shape.DEAD_END;
        if (deg == 2) {
            Iterator<Point> it = neighbors.iterator();
            return isStraight(it.next(), it.next()) ? Shape.STRAIGHT : Shape.TURN;
        }
        if (deg == 3) return Shape.CROSS_3;
        return Shape.CROSS_4;
    }

    /** Mapping UNIQUE forme + theme -> RoomType. */
    static RoomType shapeLabel(Shape shape, Theme theme, Random rng) {
        return RoomType.fromShape(shape, theme, rng);
    }

    /** Raccourci : label structurel deduit directement de l'adjacence du noeud. */
    static RoomType labelForNeighbors(Set<Point> neighbors, Theme theme, Random rng) {
        return shapeLabel(shapeOf(neighbors), theme, rng);
    }

    /** Vrai si le label structurel correspond exactement a la forme geometrique. */
    static boolean shapeMatchesLabel(Shape shape, RoomType label) {
        if (label == null || !label.isGeneric()) return true; // specials: always ok
        return label.shape == shape;
    }

    /** Theme d'un label structurel generique, ou null si c'est une salle speciale. */
    static Theme genericThemeOf(RoomType label) {
        return RoomType.themeOf(label);
    }

    /**
     * PASSE DE COHERENCE FINALE : re-derive chaque label structurel generique depuis
     * l'adjacence FINALE du graphe. Ne touche jamais aux salles speciales.
     *
     * @return le nombre de labels corriges.
     */
    static int reclassifyGeneric(Map<Point, RoomType> labels, Map<Point, Set<Point>> adj, Random rng) {
        List<Point> sorted = new ArrayList<>(labels.keySet());
        sorted.sort(Comparator.comparingInt(Point::x).thenComparingInt(Point::y));
        int fixes = 0;
        for (Point p : sorted) {
            RoomType current = labels.get(p);
            if (current == null || !current.isGeneric()) continue;
            Set<Point> nb = adj.get(p);
            Shape actual = shapeOf(nb);
            if (current.shape == actual) continue;
            labels.put(p, RoomType.fromShape(actual, current.theme, rng));
            fixes++;
        }
        return fixes;
    }

    /**
     * VALIDATEUR : retourne les incoherences entre labels structurels et adjacence reelle.
     */
    static List<String> validateStructure(Map<Point, RoomType> labels, Map<Point, Set<Point>> adj, String scope) {
        List<String> problems = new ArrayList<>();
        List<Point> sorted = new ArrayList<>(labels.keySet());
        sorted.sort(Comparator.comparingInt(Point::x).thenComparingInt(Point::y));
        for (Point p : sorted) {
            RoomType label = labels.get(p);
            if (label == null || !label.isGeneric()) continue;
            Set<Point> nb = adj.getOrDefault(p, Set.of());
            Shape actual = shapeOf(nb);
            if (label.shape != actual) {
                problems.add(scope + " @ (" + p.key() + ") : label '" + label.id + "' mais adjacence=" + actual
                        + " (" + nb.size() + " voisins : " + nb.stream().map(Point::key).sorted().toList() + ")");
            }
        }
        return problems;
    }

    private static boolean isStraight(Point p1, Point p2) {
        return p1.x() == p2.x() || p1.y() == p2.y();
    }
}
