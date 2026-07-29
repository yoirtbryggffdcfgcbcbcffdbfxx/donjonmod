package com.dungeonmod.debug;

import com.dungeonmod.debug.DungeonAlgo.Point;

import java.util.*;

/**
 * Outils génériques pour placer des structures composées multi-cellules.
 *
 * Une structure composée est décrite en coordonnées locales :
 * - axe forward : direction d'entrée/sortie principale
 * - axe side    : perpendiculaire gauche (-dy, dx)
 *
 * Exemple avec forward=(1,0), side=(0,1) :
 * local(0,0), local(1,0), local(1,1), local(0,1) = carré 2x2.
 *
 * Cette classe ne choisit pas encore où placer la structure : elle fournit le
 * mécanisme de projection, validation d'emprise et pose des arêtes/labels.
 */
final class DungeonCompositeRooms {
    private DungeonCompositeRooms() {}

    static final Spec TAVERN = Spec.builder()
            .entry(0, 0)
            .exit(0, 2)
            .label(0, 0, DungeonAlgo.RoomIds.TAVERN_1)
            .label(1, 0, DungeonAlgo.RoomIds.TAVERN_2)
            .label(1, 1, DungeonAlgo.RoomIds.TAVERN_3)
            .label(0, 1, DungeonAlgo.RoomIds.TAVERN_4)
            .node(0, 2) // sortie générique vers P2
            .edge(0, 0, 1, 0)
            .edge(1, 0, 1, 1)
            .edge(1, 1, 0, 1)
            .edge(0, 1, 0, 2)
            .build();

    record LocalPoint(int forward, int side) {}

    record LocalEdge(LocalPoint a, LocalPoint b) {}

    static final class Spec {
        private final LocalPoint entry;
        private final LocalPoint exit;
        private final Map<LocalPoint, String> labels;
        private final Set<LocalPoint> nodes;
        private final List<LocalEdge> edges;

        private Spec(LocalPoint entry, LocalPoint exit, Map<LocalPoint, String> labels,
                     Set<LocalPoint> nodes, List<LocalEdge> edges) {
            this.entry = entry;
            this.exit = exit;
            this.labels = Map.copyOf(labels);
            this.nodes = Set.copyOf(nodes);
            this.edges = List.copyOf(edges);
        }

        static Builder builder() {
            return new Builder();
        }
    }

    static final class Builder {
        private LocalPoint entry;
        private LocalPoint exit;
        private final Map<LocalPoint, String> labels = new LinkedHashMap<>();
        private final Set<LocalPoint> nodes = new LinkedHashSet<>();
        private final List<LocalEdge> edges = new ArrayList<>();

        Builder entry(int forward, int side) {
            this.entry = point(forward, side);
            node(forward, side);
            return this;
        }

        Builder exit(int forward, int side) {
            this.exit = point(forward, side);
            node(forward, side);
            return this;
        }

        Builder node(int forward, int side) {
            nodes.add(point(forward, side));
            return this;
        }

        Builder label(int forward, int side, String label) {
            LocalPoint p = point(forward, side);
            nodes.add(p);
            labels.put(p, label);
            return this;
        }

        Builder edge(int aForward, int aSide, int bForward, int bSide) {
            LocalPoint a = point(aForward, aSide);
            LocalPoint b = point(bForward, bSide);
            nodes.add(a);
            nodes.add(b);
            edges.add(new LocalEdge(a, b));
            return this;
        }

        Spec build() {
            if (entry == null) throw new IllegalStateException("CompositeRoom sans entry");
            if (exit == null) throw new IllegalStateException("CompositeRoom sans exit");
            return new Spec(entry, exit, labels, nodes, edges);
        }

        private static LocalPoint point(int forward, int side) {
            return new LocalPoint(forward, side);
        }
    }

    static final class Placement {
        private final Spec spec;
        private final Point anchor;
        private final int dx;
        private final int dy;
        private final Map<LocalPoint, Point> worldCells;

        private Placement(Spec spec, Point anchor, int dx, int dy, Map<LocalPoint, Point> worldCells) {
            this.spec = spec;
            this.anchor = anchor;
            this.dx = dx;
            this.dy = dy;
            this.worldCells = Map.copyOf(worldCells);
        }

        Point exitPoint() {
            return worldCells.get(spec.exit);
        }

        Map<String, Point> labelPoints() {
            Map<String, Point> out = new LinkedHashMap<>();
            for (var e : spec.labels.entrySet()) {
                out.put(e.getValue(), worldCells.get(e.getKey()));
            }
            return out;
        }

        Set<Point> occupiedCells() {
            return new HashSet<>(worldCells.values());
        }
    }

    /**
     * Prépare une pose sans modifier l'adj. Retourne null si l'emprise n'est pas libre.
     */
    static Placement plan(Map<Point, Set<Point>> adj, Point anchor, int dx, int dy, Spec spec) {
        Map<LocalPoint, Point> world = new LinkedHashMap<>();
        Set<Point> unique = new HashSet<>();
        for (LocalPoint local : spec.nodes) {
            Point p = toWorld(anchor, dx, dy, local);
            if (p.isOutOfBounds() || adj.containsKey(p) || !unique.add(p)) return null;
            world.put(local, p);
        }
        return new Placement(spec, anchor, dx, dy, world);
    }

    /**
     * Pose une structure déjà planifiée et la connecte au point d'entrée externe.
     */
    static void place(Map<Point, Set<Point>> adj, Point externalEntry, Placement placement) {
        for (Point p : placement.occupiedCells()) {
            adj.put(p, new HashSet<>());
        }
        Point entryPoint = placement.worldCells.get(placement.spec.entry);
        connect(adj, externalEntry, entryPoint);
        for (LocalEdge edge : placement.spec.edges) {
            connect(adj, placement.worldCells.get(edge.a()), placement.worldCells.get(edge.b()));
        }
    }

    private static void connect(Map<Point, Set<Point>> adj, Point a, Point b) {
        adj.putIfAbsent(a, new HashSet<>());
        adj.putIfAbsent(b, new HashSet<>());
        adj.get(a).add(b);
        adj.get(b).add(a);
    }

    private static Point toWorld(Point anchor, int dx, int dy, LocalPoint local) {
        int sx = -dy;
        int sy = dx;
        return new Point(
                anchor.x() + local.forward() * dx + local.side() * sx,
                anchor.y() + local.forward() * dy + local.side() * sy
        );
    }
}
