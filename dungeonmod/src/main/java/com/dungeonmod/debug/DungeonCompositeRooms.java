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
            .name("TAVERN")
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

    static final Spec CAMP = Spec.builder()
            .name("CAMP")
            .entry(0, 0)
            .exit(2, -1)
            .label(0, 0, DungeonAlgo.RoomIds.CAMP_1)
            .label(1, 0, DungeonAlgo.RoomIds.CAMP_2)
            .label(1, -1, DungeonAlgo.RoomIds.CAMP_3)
            .label(0, -1, DungeonAlgo.RoomIds.CAMP_4)
            .node(2, -1) // sortie générique vers P3
            .edge(0, 0, 1, 0)
            .edge(1, 0, 1, -1)
            .edge(1, -1, 0, -1)
            .edge(1, -1, 2, -1)
            .build();

    static final Spec PRISON_CENTRAL = Spec.builder()
            .name("PRISON_CENTRAL")
            .entry(0, 0)
            .exit(0, 1)
            .label(0, 0, DungeonAlgo.RoomIds.PRISON_C1)
            .label(1, 0, DungeonAlgo.RoomIds.PRISON_C2)
            .label(1, 1, DungeonAlgo.RoomIds.PRISON_C3)
            .label(0, 1, DungeonAlgo.RoomIds.PRISON_C4)
            .edge(0, 0, 1, 0)
            .edge(1, 0, 1, 1)
            .edge(1, 1, 0, 1)
            .build();

    record LocalPoint(int forward, int side) {}

    record LocalEdge(LocalPoint a, LocalPoint b) {}

    static final class Spec {
        private final String name;
        private final LocalPoint entry;
        private final LocalPoint exit;
        private final Map<LocalPoint, String> labels;
        private final Set<LocalPoint> nodes;
        private final List<LocalEdge> edges;

        private Spec(String name, LocalPoint entry, LocalPoint exit, Map<LocalPoint, String> labels,
                     Set<LocalPoint> nodes, List<LocalEdge> edges) {
            this.name = name;
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
        private String name = "ANONYMOUS_COMPOSITE";
        private LocalPoint entry;
        private LocalPoint exit;
        private final Map<LocalPoint, String> labels = new LinkedHashMap<>();
        private final Set<LocalPoint> nodes = new LinkedHashSet<>();
        private final List<LocalEdge> edges = new ArrayList<>();

        Builder name(String name) {
            if (name != null && !name.isBlank()) this.name = name;
            return this;
        }

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
            return new Spec(name, entry, exit, labels, nodes, edges);
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

        String name() {
            return spec.name;
        }

        Point exitPoint() {
            return worldCells.get(spec.exit);
        }

        Point point(int forward, int side) {
            return worldCells.get(new LocalPoint(forward, side));
        }

        Point point(LocalPoint localPoint) {
            return worldCells.get(localPoint);
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
        return plan(adj, anchor, dx, dy, spec, Set.of());
    }

    static Placement plan(Map<Point, Set<Point>> adj, Point anchor, int dx, int dy, Spec spec,
                          Set<Point> allowedExisting) {
        Map<LocalPoint, Point> world = new LinkedHashMap<>();
        Set<Point> unique = new HashSet<>();
        Set<Point> allowed = allowedExisting == null ? Set.of() : allowedExisting;
        for (LocalPoint local : spec.nodes) {
            Point p = toWorld(anchor, dx, dy, local);
            if (p.isOutOfBounds()) {
                DungeonFailureLog.compositeReject(spec.name, "OUT_OF_BOUNDS", anchor,
                        "local=" + local + " world=" + p.key());
                return null;
            }
            if (!allowed.contains(p) && adj.containsKey(p)) {
                DungeonFailureLog.compositeReject(spec.name, "ADJ_OCCUPIED", anchor,
                        "local=" + local + " world=" + p.key());
                return null;
            }
            if (!unique.add(p)) {
                DungeonFailureLog.compositeReject(spec.name, "DUPLICATE_CELL", anchor,
                        "local=" + local + " world=" + p.key());
                return null;
            }
            world.put(local, p);
        }
        return new Placement(spec, anchor, dx, dy, world);
    }

    /**
     * Pose une structure déjà planifiée et la connecte au point d'entrée externe.
     */
    static void place(Map<Point, Set<Point>> adj, Point externalEntry, Placement placement) {
        for (Point p : placement.occupiedCells()) {
            adj.putIfAbsent(p, new HashSet<>());
        }
        Point entryPoint = placement.worldCells.get(placement.spec.entry);
        if (externalEntry != null) connect(adj, externalEntry, entryPoint);
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
