package com.dungeonmod.debug;

import com.dungeonmod.debug.DungeonAlgo.Point;
import com.dungeonmod.debug.DungeonAlgo.RoomIds;
import com.dungeonmod.debug.DungeonAlgo.RoomPools;
import com.dungeonmod.debug.DungeonAlgo.Shape;
import com.dungeonmod.debug.DungeonAlgo.Theme;

import java.util.*;

/**
 * Helpers de classification des salles.
 *
 * Règle importante : les labels structurels génériques (C/I/cul, CJ/IJ/culDJ,
 * CG/GI/CDG) doivent être dérivés de l'adjacence réelle du graphe, pas d'une
 * décision locale prise pendant la génération.
 *
 * Cette classe extrait la logique de labels hors de DungeonAlgo sans changer
 * l'API publique : DungeonAlgo garde ses wrappers pour compatibilité.
 */
final class DungeonLabels {
    private DungeonLabels() {}

    /** Déduit la forme d'un nœud depuis son ensemble de voisins. */
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

    /** Mapping UNIQUE forme + thème -> identifiant de salle. */
    static String shapeLabel(Shape shape, Theme theme, Random rng) {
        return switch (shape) {
            case DEAD_END -> switch (theme) {
                case P12 -> RoomIds.DEAD_END; case DJ -> RoomIds.DEAD_END_DJ; case GOBLIN -> RoomIds.GOBLIN_DEAD_END;
            };
            case STRAIGHT -> switch (theme) {
                case P12 -> pickC(rng); case DJ -> pickCJ(rng); case GOBLIN -> RoomIds.GOBLIN_CORRIDOR;
            };
            case TURN -> switch (theme) {
                case P12 -> RoomIds.CORRIDOR_TURN; case DJ -> RoomIds.CORRIDOR_TURN_J; case GOBLIN -> RoomIds.GOBLIN_TURN;
            };
            case CROSS_3 -> switch (theme) {
                case P12 -> RoomIds.INTERSECTION_3; case DJ -> RoomIds.INTERSECTION_3_J; case GOBLIN -> RoomIds.GOBLIN_I3;
            };
            case CROSS_4 -> switch (theme) {
                case P12 -> RoomIds.INTERSECTION_4; case DJ -> RoomIds.INTERSECTION_4_J; case GOBLIN -> RoomIds.GOBLIN_I4;
            };
        };
    }

    /** Raccourci : label structurel déduit directement de l'adjacence du nœud. */
    static String labelForNeighbors(Set<Point> neighbors, Theme theme, Random rng) {
        return shapeLabel(shapeOf(neighbors), theme, rng);
    }

    /** Vrai si le label structurel correspond exactement à la forme géométrique. */
    static boolean shapeMatchesLabel(Shape shape, String label) {
        return switch (shape) {
            case DEAD_END -> label.equals(RoomIds.DEAD_END) || label.equals(RoomIds.DEAD_END_DJ) || label.equals(RoomIds.GOBLIN_DEAD_END);
            case STRAIGHT -> RoomPools.CORRIDORS_P1_P2.contains(label) || RoomPools.CORRIDORS_P3_P4.contains(label) || label.equals(RoomIds.GOBLIN_CORRIDOR);
            case TURN -> label.equals(RoomIds.CORRIDOR_TURN) || label.equals(RoomIds.CORRIDOR_TURN_J) || label.equals(RoomIds.GOBLIN_TURN);
            case CROSS_3 -> label.equals(RoomIds.INTERSECTION_3) || label.equals(RoomIds.INTERSECTION_3_J) || label.equals(RoomIds.GOBLIN_I3);
            case CROSS_4 -> label.equals(RoomIds.INTERSECTION_4) || label.equals(RoomIds.INTERSECTION_4_J) || label.equals(RoomIds.GOBLIN_I4);
        };
    }

    /** Thème d'un label structurel générique, ou null si c'est une salle spéciale. */
    static Theme genericThemeOf(String label) {
        if (label == null) return null;
        switch (label) {
            case "C1", "C2", "C3", "I2", "I3", "I4", "cul": return Theme.P12;
            case "CJ1", "CJ2", "CJ3", "IJ2", "IJ3", "IJ4", "culDJ": return Theme.DJ;
            case "CG1", "GI2", "GI3", "GI4", "CDG": return Theme.GOBLIN;
            default: return null;
        }
    }

    /**
     * PASSE DE COHÉRENCE FINALE : re-dérive chaque label structurel générique depuis
     * l'adjacence FINALE du graphe. Ne touche jamais aux salles spéciales.
     *
     * @return le nombre de labels corrigés.
     */
    static int reclassifyGeneric(Map<Point, String> labels, Map<Point, Set<Point>> adj, Random rng) {
        List<Point> sorted = new ArrayList<>(labels.keySet());
        sorted.sort(Comparator.comparingInt(Point::x).thenComparingInt(Point::y));
        int fixes = 0;
        for (Point p : sorted) {
            String current = labels.get(p);
            Theme theme = genericThemeOf(current);
            if (theme == null) continue;
            Set<Point> nb = adj.get(p);
            Shape actual = shapeOf(nb);
            if (shapeMatchesLabel(actual, current)) continue;
            labels.put(p, shapeLabel(actual, theme, rng));
            fixes++;
        }
        return fixes;
    }

    /**
     * VALIDATEUR : retourne les incohérences entre labels structurels et adjacence réelle.
     */
    static List<String> validateStructure(Map<Point, String> labels, Map<Point, Set<Point>> adj, String scope) {
        List<String> problems = new ArrayList<>();
        List<Point> sorted = new ArrayList<>(labels.keySet());
        sorted.sort(Comparator.comparingInt(Point::x).thenComparingInt(Point::y));
        for (Point p : sorted) {
            String label = labels.get(p);
            if (genericThemeOf(label) == null) continue;
            Set<Point> nb = adj.getOrDefault(p, Set.of());
            Shape actual = shapeOf(nb);
            if (!shapeMatchesLabel(actual, label)) {
                problems.add(scope + " @ (" + p.key() + ") : label '" + label + "' mais adjacence=" + actual
                        + " (" + nb.size() + " voisins : " + nb.stream().map(Point::key).sorted().toList() + ")");
            }
        }
        return problems;
    }

    private static boolean isStraight(Point p1, Point p2) {
        return p1.x() == p2.x() || p1.y() == p2.y();
    }

    private static String pickC(Random rng) {
        return RoomPools.CORRIDORS_P1_P2.get(rng.nextInt(3));
    }

    private static String pickCJ(Random rng) {
        return RoomPools.CORRIDORS_P3_P4.get(rng.nextInt(3));
    }
}
