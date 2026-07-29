package com.dungeonmod.debug;

import java.util.*;

public class DungeonAlgo {

    // ===================== Constants =====================

    public static final int[][] DIR_OFFSET = {{1, 0}, {0, 1}, {-1, 0}, {0, -1}};
    public static final int GRID_SIZE = 400;

    // Arbres légèrement agrandis (règles de l'espacement monstres, conversation 3) :
    // plus de place pour respecter >= 2 salles neutres entre salles monstre et
    // l'isolement de l'Ogre sans rejet massif.
    private static final int PART1_TARGET_MIN = 24;
    private static final int PART1_TARGET_MAX = 30;
    private static final int PART1_MAX_I3 = 3;
    private static final int PART1_MAX_I4 = 1;
    private static final int PART1_STRAIGHT_WEIGHT = 1;

    // Arbre P2 nettement agrandi (correctif "GENERATION NULLE" /teste, conversation 4) :
    // il faut caser 5 feuilles (ogre/fontaine/m3/m1/loot) + 3 couloirs monstre (M2/M4/M5)
    // avec espacement >= 3 entre tous les monstres ET >= 4 entre l'Ogre et chaque monstre
    // (monstres hérités de P1 inclus). Avec l'ancienne taille 24-30 (tronc 8-12), le diamètre
    // de l'arbre était trop faible : feuilles/couloirs trop proches => analyzePart2 retournait
    // null presque à chaque tentative (echec massif -> GENERATION NULLE cote joueur).
    //
    // Un tronc plus long donne PLUS de points de branche (donc plus de feuilles, une branche
    // laterale etant tiree a proba 0.55 le long du tronc) et un diametre plus grand : l'Ogre,
    // pose sur la feuille la plus eloignee de la sortie de taverne, se retrouve naturellement
    // a >= 4 des autres monstres. Un target plus grand fait que la passe de remplissage
    // ALLONGE les branches (couloirs plus longs) => plus de distance entre salles => les
    // contraintes d'espacement deviennent faciles a satisfaire, donc beaucoup moins de rejets.
    private static final int PART2_TARGET_MIN = 38;
    private static final int PART2_TARGET_MAX = 50;
    private static final int PART2_MAX_I3 = 4;
    private static final int PART2_TRUNK_MIN = 12;
    private static final int PART2_TRUNK_MAX = 18;
    /**
     * Max de segments COLINÉAIRES d'affilée dans l'ADJACENCE (géométrie pure),
     * pas "nombre de labels C". Compte aussi le passage tout droit à travers
     * une I3/I4. Au-delà → virage forcé.
     * Ex. I3—C—C—I2 alignés = 3 segments → ok ; un 4ᵉ segment droit interdit.
     */
    private static final int MAX_COLINEAR_RUN = 3;
    /** @deprecated alias — utiliser {@link #MAX_COLINEAR_RUN} */
    @Deprecated
    private static final int PART2_MAX_COLINEAR_RUN = MAX_COLINEAR_RUN;

    private static final int PART3_TARGET = 45;
    private static final int PART3_MAX_IJ3 = 3;
    private static final int PART3_MAX_IJ4 = 1;

    // ===================== Room Identifiers (Anti-Typo Constants) =====================

    public static class RoomIds {
        public static final String START = "D";
        public static final String PRISON = "Prison";
        public static final String DOOR_1 = "porte";
        public static final String DOOR_2 = "porte2";
        public static final String DOOR_3 = "porte3";

        public static final String TAVERN_1 = "T1";
        public static final String TAVERN_2 = "T2";
        public static final String TAVERN_3 = "T3";
        public static final String TAVERN_4 = "T4";

        public static final String LOOT_1 = "Loot1";
        public static final String DEAD_END = "cul";
        public static final String MONSTER_1 = "M1";
        public static final String MONSTER_2 = "M2";
        public static final String MONSTER_3 = "M3";
        public static final String MONSTER_4 = "M4";
        /** Couloir monstre placé juste avant porte1 / porte2. */
        public static final String MONSTER_5 = "M5";
        public static final String WELL = "puit";
        public static final String FOUNTAIN = "fontaine";
        public static final String OGRE = "Ogre";

        public static final String CAMP_1 = "Ca1";
        public static final String CAMP_2 = "Ca2";
        public static final String CAMP_3 = "Ca3";
        public static final String CAMP_4 = "Ca4";

        public static final String BIB_1 = "Bib1";
        public static final String BIB_2 = "Bib2";
        public static final String SHOP = "Shop";

        public static final String CORRIDOR_TURN = "I2";
        public static final String INTERSECTION_3 = "I3";
        public static final String INTERSECTION_4 = "I4";

        public static final String CORRIDOR_TURN_J = "IJ2";
        public static final String INTERSECTION_3_J = "IJ3";
        public static final String INTERSECTION_4_J = "IJ4";

        public static final String DEAD_END_DJ = "culDJ";
        public static final String WELL_DJ = "PuitDJ";
        public static final String GARDEN = "Jardin";
        public static final String STATUE = "Statue";
        public static final String CENTRALE = "Centrale";
        public static final String BLACK_MARKET = "MarchandNoir";

        public static final String CHAPEL_1 = "Chapelle1";
        public static final String CHAPEL_2 = "Chapelle2";
        public static final String CRYPT_1 = "Crypte1";
        public static final String CRYPT_2 = "Crypte2";

        public static final String PRISON_C1 = "PrisonC1";
        public static final String PRISON_C2 = "PrisonC2";
        public static final String PRISON_C3 = "PrisonC3";
        public static final String PRISON_C4 = "PrisonC4";

        public static final String GOBLIN_DOOR = "PorteGob";
        public static final String GOBLIN_CORRIDOR = "CG1";
        public static final String GOBLIN_TURN = "GI2";
        public static final String GOBLIN_I3 = "GI3";
        public static final String GOBLIN_I4 = "GI4";
        public static final String GOBLIN_WELL = "PuitG";
        public static final String GOBLIN_MARCH = "MarchG";
        public static final String GOBLIN_TREASURE = "TresorG";
        public static final String GOBLIN_ARMORY = "ArmG";
        public static final String GOBLIN_DEAD_END = "CDG";
        public static final String GOBLIN_HOUSE_1 = "MG1";
        public static final String GOBLIN_HOUSE_2 = "MG2";
        public static final String GOBLIN_HOUSE_3 = "MG3";
    }

    // ===================== Point Record & Helpers =====================

    public record Point(int x, int y) {
        public static Point parse(String key) {
            int idx = key.indexOf(',');
            return new Point(
                Integer.parseInt(key.substring(0, idx)),
                Integer.parseInt(key.substring(idx + 1))
            );
        }

        public String key() {
            return x + "," + y;
        }

        public Point move(int[] dir) {
            return new Point(x + dir[0], y + dir[1]);
        }

        public Point move(int dx, int dy) {
            return new Point(x + dx, y + dy);
        }

        public boolean isOutOfBounds() {
            return x < 0 || x >= GRID_SIZE || y < 0 || y >= GRID_SIZE;
        }
    }

    private static boolean isStraight(Point p1, Point p2) {
        return p1.x() == p2.x() || p1.y() == p2.y();
    }

    // ===================== Classification géométrique UNIQUE =====================
    // RÈGLE D'OR : un label structurel (couloir / virage / intersection / cul-de-sac)
    // se déduit TOUJOURS de l'adjacence FINALE du nœud — jamais d'une décision locale
    // prise pendant la construction. Toute mutation post-classification doit être
    // suivie d'un reclassifyGeneric() sur la map concernée.

    /** Forme géométrique d'un nœud, déduite UNIQUEMENT de son adjacence. */
    public enum Shape { DEAD_END, STRAIGHT, TURN, CROSS_3, CROSS_4 }

    /** Déduit la forme d'un nœud depuis son ensemble de voisins. */
    public static Shape shapeOf(Set<Point> neighbors) {
        int deg = neighbors == null ? 0 : neighbors.size();
        if (deg <= 1) return Shape.DEAD_END;
        if (deg == 2) {
            Iterator<Point> it = neighbors.iterator();
            return isStraight(it.next(), it.next()) ? Shape.STRAIGHT : Shape.TURN;
        }
        if (deg == 3) return Shape.CROSS_3;
        return Shape.CROSS_4;
    }

    /** Thème visuel d'un étage : P1/P2, donjon (P3/P4) ou village gobelin. */
    public enum Theme { P12, DJ, GOBLIN }

    /** Mapping UNIQUE forme + thème -> identifiant de salle. */
    private static String shapeLabel(Shape shape, Theme theme, Random rng) {
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
    private static String labelForNeighbors(Set<Point> neighbors, Theme theme, Random rng) {
        return shapeLabel(shapeOf(neighbors), theme, rng);
    }

    /** Vrai si le label structurel correspond exactement à la forme géométrique. */
    public static boolean shapeMatchesLabel(Shape shape, String label) {
        return switch (shape) {
            case DEAD_END -> label.equals(RoomIds.DEAD_END) || label.equals(RoomIds.DEAD_END_DJ) || label.equals(RoomIds.GOBLIN_DEAD_END);
            case STRAIGHT -> RoomPools.CORRIDORS_P1_P2.contains(label) || RoomPools.CORRIDORS_P3_P4.contains(label) || label.equals(RoomIds.GOBLIN_CORRIDOR);
            case TURN -> label.equals(RoomIds.CORRIDOR_TURN) || label.equals(RoomIds.CORRIDOR_TURN_J) || label.equals(RoomIds.GOBLIN_TURN);
            case CROSS_3 -> label.equals(RoomIds.INTERSECTION_3) || label.equals(RoomIds.INTERSECTION_3_J) || label.equals(RoomIds.GOBLIN_I3);
            case CROSS_4 -> label.equals(RoomIds.INTERSECTION_4) || label.equals(RoomIds.INTERSECTION_4_J) || label.equals(RoomIds.GOBLIN_I4);
        };
    }

    /** Thème d'un label structurel générique, ou null si c'est une salle spéciale. */
    private static Theme genericThemeOf(String label) {
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
     * l'adjacence FINALE du graphe. À appeler après toute modification de la topologie
     * ayant suivi une classification. Ne touche JAMAIS aux salles spéciales.
     * Ne consomme le RNG que pour les nœuds réellement corrigés.
     * @return le nombre de labels corrigés.
     */
    private static int reclassifyGeneric(Map<Point, String> labels, Map<Point, Set<Point>> adj, Random rng) {
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
     * VALIDATEUR : retourne la liste des incohérences entre labels structurels et
     * adjacence réelle (chaîne vide si tout est cohérent). Outil de debug, sans effet
     * de bord — DungeonViz l'appelle après chaque génération.
     */
    public static List<String> validateStructure(Map<Point, String> labels, Map<Point, Set<Point>> adj, String scope) {
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

    // ===================== Inner classes =====================

    public static class RoomConfig {
        public String type;
        public List<String> doors;
        public boolean turnAfter2;
        public RoomConfig(String type, List<String> doors, boolean turnAfter2) {
            this.type = type; this.doors = doors; this.turnAfter2 = turnAfter2;
        }
    }

    public static class DungeonResult {
        public Map<Point, Set<Point>> adj;
        public Map<Point, String> labels;
        public Map<Point, String> topLabels;
        public Map<Point, Set<Point>> p4Adj;
        public Point startPoint;
        public String startKey;
        public int startX, startY;
        public String missingLootType;
        public long seed;
    }

    private static class TreeResult {
        Point startPoint;
        String startKey;
        int startX, startY;
        Map<Point, Set<Point>> adj;
        /** Fin du tronc P2 (pour enchaîner M5 → gap → porte2). */
        Point trunkEnd;
        /** Direction du dernier pas du tronc (index dans DIR_OFFSET), ou -1. */
        int trunkEndDir = -1;
    }

    private static class TavernResult {
        Map<String, Point> tavern;
        Point exitPoint;
        Set<Point> pathSet;
    }

    private static class CampResult {
        Point campExit;
        Set<Point> campPathSet;
        Map<String, Point> campNodes;
    }

    // ===================== Règles d'espacement (monstres / Ogre / culs-de-sac) =====================
    // (Reportées de la conversation 3 sur la base "vraie version" : fusion 52b2450)

    /** Distance min entre DEUX salles monstre : 3 => au moins 2 salles neutres entre elles. */
    public static final int MONSTER_MIN_DIST = 3;
    /** Distance min entre l'OGRE et toute salle monstre : 4 => au moins 3 salles neutres. */
    public static final int OGRE_MIN_MONSTER_DIST = 4;

    /** Vrai si le label est une salle monstre (M1-M5 des grottes, MJ1-MJ5 du donjon). */
    public static boolean isMonsterLabel(String v) {
        if (v == null) return false;
        return v.equals(RoomIds.MONSTER_1) || v.equals(RoomIds.MONSTER_2)
            || v.equals(RoomIds.MONSTER_3) || v.equals(RoomIds.MONSTER_4)
            || v.equals(RoomIds.MONSTER_5)
            || RoomPools.LEAF_MONSTERS_P3_P4.contains(v) || RoomPools.CORRIDOR_MONSTERS_P3_P4.contains(v);
    }

    /** Vrai pour les culs-de-sac GÉNÉRIQUES (jamais les salles spéciales en cul-de-sac). */
    public static boolean isGenericDeadEnd(String v) {
        return RoomIds.DEAD_END.equals(v) || RoomIds.DEAD_END_DJ.equals(v) || RoomIds.GOBLIN_DEAD_END.equals(v);
    }

    /** Tous les points actuellement étiquetés comme salle monstre. */
    private static Set<Point> monsterPoints(Map<Point, String> labels) {
        Set<Point> out = new HashSet<>();
        for (var e : labels.entrySet()) if (isMonsterLabel(e.getValue())) out.add(e.getKey());
        return out;
    }

    /** Distances BFS depuis src (en nombre de salles traversées). */
    public static Map<Point, Integer> bfsDistances(Map<Point, Set<Point>> adj, Point src) {
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
    private static boolean isFarFromAll(Map<Point, Set<Point>> adj, Point cand, Collection<Point> targets, int minDist) {
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
    private static Point firstFar(Map<Point, Set<Point>> adj, List<Point> candidates, Collection<Point> targets, int minDist) {
        for (Point c : candidates) if (isFarFromAll(adj, c, targets, minDist)) return c;
        return null;
    }

    /** Cellule in-bounds et inoccupée par le graphe. */
    private static boolean isFreeCell(Map<Point, Set<Point>> adj, Point p) {
        return !p.isOutOfBounds() && !adj.containsKey(p);
    }

    /** Déplace une feuille cul-de-sac : retire leaf et raccroche target à newParent. */
    private static void moveDeadEnd(Map<Point, String> labels, Map<Point, Set<Point>> adj,
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
     * PASSE CUL-DE-SAC (règle gameplay) : un cul-de-sac générique (cul / culDJ / CDG) ne doit
     * JAMAIS terminer une ligne droite — il ne peut suivre qu'un virage ou une intersection.
     * Pour chaque cul fautif (parent droit, 2 voisins), on tente :
     *   1) de déplacer sa cellule sur un voisin LATÉRAL libre du parent (le parent devient virage) ;
     *   2) sinon de le rattacher au GRAND-PARENT générique (qui devient intersection — l'ancien
     *      parent, privé de sa feuille, devient lui-même un cul valide après cette intersection).
     * Un parent SPÉCIAL droit (puit, M2/M4, MJ2/4...) ne peut pas être courbé : la passe échoue
     * alors (false) et le layout est rejeté par l'appelant (retry en amont).
     * Invariant : seules des feuilles bougent — AUCUNE distance entre les autres salles ne change.
     */
    private static boolean enforceDeadEndAfterTurn(Map<Point, String> labels, Map<Point, Set<Point>> adj, Random rng) {
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
                if (shapeOf(pAdj) != Shape.STRAIGHT) continue;  // virage : déjà valide
                String parentLbl = labels.get(parent);
                if (parentLbl == null) continue;                 // cellule structurelle non étiquetée : on laisse
                Point gp = null;
                for (Point q : pAdj) if (!q.equals(leaf)) { gp = q; break; }
                if (gp == null) continue;

                boolean moved = false;
                // 1) Rebascule latérale du cul : le parent (générique) devient un virage.
                if (genericThemeOf(parentLbl) != null) {
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
                if (!moved && genericThemeOf(parentLbl) != null && genericThemeOf(labels.get(gp)) != null
                        && adj.get(gp).size() <= 3) {
                    List<Point> opts = new ArrayList<>();
                    for (int[] d : DIR_OFFSET) {
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
        reclassifyGeneric(labels, adj, rng);
        return true;
    }

    // ===================== Config maps & Pools =====================

    private static final Map<String, RoomConfig> ROOM_CONFIGS = new LinkedHashMap<>();
    private static void reg(String id, String type, String doors, boolean turnAfter2) {
        List<String> doorList = new ArrayList<>();
        for (char c : doors.toCharArray()) doorList.add(String.valueOf(c));
        ROOM_CONFIGS.put(id, new RoomConfig(type, doorList, turnAfter2));
    }

    static {
        reg(RoomIds.START,          "Cul de sac",    "S",    false);
        reg(RoomIds.PRISON,         "Cul de sac",    "N",    false);
        reg(RoomIds.DOOR_1,         "Couloir droit", "",     false);
        reg(RoomIds.TAVERN_1,       "Couloir droit", "NS",   false);
        reg(RoomIds.TAVERN_2,       "Virage",        "NE",   false);
        reg(RoomIds.TAVERN_3,       "Virage",        "ES",   false);
        reg(RoomIds.TAVERN_4,       "Virage",        "SE",   false);
        reg(RoomIds.LOOT_1,         "Cul de sac",    "N",    false);
        reg(RoomIds.DEAD_END,       "Cul de sac",    "N",    false);
        reg(RoomIds.MONSTER_1,      "Cul de sac",    "N",    false);
        reg(RoomIds.MONSTER_2,      "Couloir droit", "NS",   true);
        reg(RoomIds.MONSTER_5,      "Couloir droit", "NS",   true);
        reg("C1",                   "Couloir droit", "NS",   true);
        reg("C2",                   "Couloir droit", "NS",   true);
        reg("C3",                   "Couloir droit", "NS",   true);
        reg(RoomIds.WELL,           "Couloir droit", "NS",   true);
        reg(RoomIds.FOUNTAIN,       "Cul de sac",    "N",    false);
        reg(RoomIds.DOOR_2,         "Couloir droit", "NS",   false);
        reg("CJ1",                  "Couloir droit", "NS",   true);
        reg("CJ2",                  "Couloir droit", "NS",   true);
        reg("CJ3",                  "Couloir droit", "NS",   true);
        reg(RoomIds.CORRIDOR_TURN_J,"Virage",        "NE",   false);
        reg(RoomIds.INTERSECTION_3_J,"Intersection", "ENS",  false);
        reg(RoomIds.INTERSECTION_4_J,"Intersection", "NESW", false);
        reg("MJ1",                  "Cul de sac",    "N",    false);
        reg("MJ2",                  "Couloir droit", "NS",   true);
        reg("Lootdj1",              "Cul de sac",    "N",    false);
        reg("Lootdj2",              "Couloir droit", "NS",   true);
        reg(RoomIds.DOOR_3,         "Cul de sac",    "N",    false);
        reg(RoomIds.CAMP_1,         "Couloir droit", "SN",   false);
        reg(RoomIds.CAMP_2,         "Virage",        "SW",   false);
        reg(RoomIds.CAMP_3,         "Intersection",  "ENS",  false);
        reg(RoomIds.CAMP_4,         "Cul de sac",    "N",    false);
        reg(RoomIds.BIB_1,          "Couloir droit", "NS",   false);
        reg(RoomIds.BIB_2,          "Cul de sac",    "S",    false);
        reg(RoomIds.SHOP,           "Cul de sac",    "N",    false);
        reg(RoomIds.CORRIDOR_TURN,  "Virage",        "NE",   false);
        reg(RoomIds.INTERSECTION_3, "Intersection",  "NSE",  false);
        reg(RoomIds.INTERSECTION_4, "Intersection",  "NSEW", false);
        reg(RoomIds.DEAD_END_DJ,    "Cul de sac",    "N",    false);
        reg(RoomIds.MONSTER_3,      "Cul de sac",    "N",    false);
        reg(RoomIds.MONSTER_4,      "Couloir droit", "NS",   true);
        // M5 déjà enregistré plus haut (couloir avant portes)
        reg(RoomIds.OGRE,           "Cul de sac",    "N",    false);
        reg("MJ3",                  "Cul de sac",    "N",    false);
        reg("MJ4",                  "Couloir droit", "NS",   true);
        reg("MJ5",                  "Cul de sac",    "N",    false);
        reg(RoomIds.WELL_DJ,        "Couloir droit", "NS",   true);
        reg(RoomIds.GARDEN,         "Cul de sac",    "N",    false);
        reg("Lootdj3",              "Cul de sac",    "N",    false);
        reg(RoomIds.STATUE,         "Cul de sac",    "N",    false);
        reg(RoomIds.CENTRALE,       "Cul de sac",    "S",    false);
        reg(RoomIds.BLACK_MARKET,   "Cul de sac",    "N",    false);
        reg(RoomIds.CHAPEL_1,       "Couloir droit", "NS",   false);
        reg(RoomIds.CHAPEL_2,       "Couloir droit", "NS",   true);
        reg(RoomIds.CRYPT_1,        "Couloir droit", "NS",   false);
        reg(RoomIds.CRYPT_2,        "Cul de sac",    "N",    false);
        reg(RoomIds.PRISON_C1,      "Couloir droit", "NS",   false);
        reg(RoomIds.PRISON_C2,      "Virage",        "NE",   false);
        reg(RoomIds.PRISON_C3,      "Virage",        "NE",   false);
        reg(RoomIds.PRISON_C4,      "Cul de sac",    "N",    false);
        reg(RoomIds.GOBLIN_DOOR,    "Couloir droit", "NS",   false);
        reg(RoomIds.GOBLIN_CORRIDOR,"Couloir droit", "NS",   true);
        reg(RoomIds.GOBLIN_TURN,    "Virage",        "NE",   false);
        reg(RoomIds.GOBLIN_I3,      "Intersection",  "ENS",  false);
        reg(RoomIds.GOBLIN_I4,      "Intersection",  "NESW", false);
        reg(RoomIds.GOBLIN_WELL,    "Couloir droit", "NS",   true);
        reg(RoomIds.GOBLIN_MARCH,   "Cul de sac",    "N",    false);
        reg(RoomIds.GOBLIN_TREASURE,"Cul de sac",    "N",    false);
        reg(RoomIds.GOBLIN_ARMORY,  "Cul de sac",    "N",    false);
        reg(RoomIds.GOBLIN_DEAD_END,"Cul de sac",    "N",    false);
        reg(RoomIds.GOBLIN_HOUSE_1, "Cul de sac",    "N",    false);
        reg(RoomIds.GOBLIN_HOUSE_2, "Virage",        "NE",   false);
        reg(RoomIds.GOBLIN_HOUSE_3, "Intersection",  "ENS",  false);
    }

    public static class RoomPools {
        public static final List<String> CORRIDORS_P1_P2 = List.of("C1", "C2", "C3");
        public static final List<String> CORRIDORS_P3_P4 = List.of("CJ1", "CJ2", "CJ3");
        public static final List<String> LEAF_MONSTERS_P3_P4 = List.of("MJ1", "MJ3", "MJ5");
        public static final List<String> CORRIDOR_MONSTERS_P3_P4 = List.of("MJ2", "MJ4");
        public static final List<String> LOOT_P3_P4 = List.of("Lootdj1", "Lootdj2", "Lootdj3");
    }

    private static String pickC(Random rng) { return RoomPools.CORRIDORS_P1_P2.get(rng.nextInt(3)); }
    private static String pickCJ(Random rng) { return RoomPools.CORRIDORS_P3_P4.get(rng.nextInt(3)); }

    private static Point findPointByValue(Map<Point, String> map, String value) {
        if (map == null || value == null) return null;
        for (var e : map.entrySet()) if (value.equals(e.getValue())) return e.getKey();
        return null;
    }

    // ===================== Algorithm: Part 1 tree =====================

    private static boolean hasPrisonCandidate(Map<Point, Set<Point>> adj, Point startPoint) {
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

    private static TreeResult generateRawTree(int targetMin, int targetMax, int maxI3, int maxI4,
                                               int straightWeight, Point startPt, Set<Point> blocked,
                                               Random rng) {
        if (blocked == null) blocked = new HashSet<>();
        Point start = startPt != null ? startPt : new Point(GRID_SIZE / 2, GRID_SIZE / 2);

        Set<Point> occupied = new HashSet<>();
        occupied.add(start);
        Map<Point, Set<Point>> adj = new HashMap<>();
        adj.put(start, new HashSet<>());
        List<Point> allNodes = new ArrayList<>();
        allNodes.add(start);
        Map<Point, int[]> entryDir = new HashMap<>();

        List<int[]> dirs = new ArrayList<>(Arrays.asList(DIR_OFFSET));
        Collections.shuffle(dirs, rng);
        boolean found = false;
        int fdx = 0, fdy = 0;
        for (int[] d : dirs) {
            Point target = start.move(d);
            if (!target.isOutOfBounds() && !blocked.contains(target)) {
                fdx = d[0]; fdy = d[1]; found = true; break;
            }
        }
        if (!found) { 
            TreeResult tr = new TreeResult(); 
            tr.startPoint = start; tr.startKey = start.key(); tr.startX = start.x(); tr.startY = start.y(); tr.adj = adj; 
            return tr; 
        }

        Point firstPoint = start.move(fdx, fdy);
        occupied.add(firstPoint); allNodes.add(firstPoint);
        adj.put(firstPoint, new HashSet<>());
        adj.get(start).add(firstPoint); adj.get(firstPoint).add(start);
        entryDir.put(firstPoint, new int[]{fdx, fdy});

        int targetSize = targetMin + rng.nextInt(targetMax - targetMin + 1);
        while (allNodes.size() < targetSize) {
            List<Point[]> candidates = new ArrayList<>();
            int cI3 = 0, cI4 = 0;
            for (Set<Point> nb : adj.values()) { int d = nb.size(); if (d == 3) cI3++; else if (d == 4) cI4++; }
            for (Point p : allNodes) {
                if (p.equals(start)) continue;
                int deg = adj.get(p).size();
                if (deg == 2 && cI3 >= maxI3) continue;
                if (deg == 3 && cI4 >= maxI4) continue;
                if (deg >= 4) continue;
                
                for (int[] d : DIR_OFFSET) {
                    Point next = p.move(d);
                    if (!next.isOutOfBounds()) {
                        if (!occupied.contains(next) && !blocked.contains(next)) {
                            // Limite géométrique : max MAX_COLINEAR_RUN segments alignés
                            // dans l'adj réelle (I3/I4 sur l'axe comptés comme la droite)
                            if (colinearRunAfterEdge(p, next, adj) > MAX_COLINEAR_RUN) continue;
                            if (deg >= 2) {
                                boolean skip = false;
                                for (Point n1 : adj.get(p)) {
                                    for (Point n2 : adj.get(p)) {
                                        if (n1.equals(n2)) continue;
                                        if (p.x() - n1.x() == n2.x() - p.x() && p.y() - n1.y() == n2.y() - p.y()) {
                                            Set<Point> a1 = adj.get(n1), a2 = adj.get(n2);
                                            if ((a1 != null && a1.size() >= 3) || (a2 != null && a2.size() >= 3)) {
                                                skip = true; break;
                                            }
                                        }
                                    }
                                    if (skip) break;
                                }
                                if (skip) continue;
                            }
                            candidates.add(new Point[]{p, next});
                        }
                    }
                }
            }
            List<Point[]> weighted = new ArrayList<>();
            for (Point[] cand : candidates) {
                int w = 1;
                Point p = cand[0];
                int deg = adj.get(p).size();
                int[] ed = entryDir.get(p);
                if (deg == 1 && ed != null) {
                    Point c = cand[1];
                    if ((c.x() - p.x()) == ed[0] && (c.y() - p.y()) == ed[1]) w = straightWeight;
                }
                for (int i = 0; i < w; i++) weighted.add(cand);
            }
            if (weighted.isEmpty()) break;
            Point[] choice = weighted.get(rng.nextInt(weighted.size()));
            Point parent = choice[0], child = choice[1];
            occupied.add(child); allNodes.add(child);
            adj.put(child, new HashSet<>());
            adj.get(parent).add(child); adj.get(child).add(parent);
            
            entryDir.put(child, new int[]{child.x() - parent.x(), child.y() - parent.y()});
        }
        TreeResult tr = new TreeResult(); 
        tr.startPoint = start; tr.startKey = start.key(); tr.startX = start.x(); tr.startY = start.y(); tr.adj = adj; 
        return tr;
    }

    private static TreeResult generatePart1Tree(Random rng) {
        return generateRawTree(PART1_TARGET_MIN, PART1_TARGET_MAX, PART1_MAX_I3, PART1_MAX_I4, PART1_STRAIGHT_WEIGHT, null, null, rng);
    }

    // ===================== Algorithm: analyzePart1 =====================

    private static Map<Point, String> analyzePart1(Point startPoint, Map<Point, Set<Point>> adj, Random rng) {
        Map<Point, String> labels = new HashMap<>();
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
        labels.put(startPoint, RoomIds.START);
        labels.put(porte, RoomIds.DOOR_1);
        labels.put(prison, RoomIds.PRISON);
        Point prisonParent = adj.get(prison).iterator().next();
        labels.put(prisonParent, RoomIds.MONSTER_2);
        // others a déjà la porte retirée → indice 0 = loot, le reste = feuilles candidates
        labels.put(others.get(0), RoomIds.LOOT_1);

        // ESPACEMENT MONSTRES (règle dure, conversation 3) : chaque salle monstre (M1-M5)
        // doit être à distance >= MONSTER_MIN_DIST des autres (au moins 2 salles neutres
        // entre elles). Infaisable => rejet (return null => retry amont). M2 (parent de la
        // prison) est structurelle et sert de point de référence.
        Set<Point> monsters = new HashSet<>();
        monsters.add(prisonParent);
        List<Point> freeLeaves = new ArrayList<>();
        for (int i = 1; i < others.size(); i++) freeLeaves.add(others.get(i));
        Collections.shuffle(freeLeaves, rng);

        Point m1 = firstFar(adj, freeLeaves, monsters, MONSTER_MIN_DIST);
        if (m1 == null) return null;
        labels.put(m1, RoomIds.MONSTER_1);
        monsters.add(m1);
        freeLeaves.remove(m1);
        boolean isM4 = rng.nextBoolean();

        List<Point> cNodes = new ArrayList<>(), i2Nodes = new ArrayList<>(), i3Nodes = new ArrayList<>(), i4Nodes = new ArrayList<>();
        for (var e : adj.entrySet()) {
            if (labels.containsKey(e.getKey())) continue;
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
                    if (!labels.containsKey(n) && isFarFromAll(adj, n, monsters, MONSTER_MIN_DIST)) validC.add(n);
                }
                if (!validC.isEmpty()) {
                    Point m4Point = validC.get(rng.nextInt(validC.size()));
                    labels.put(m4Point, RoomIds.MONSTER_4);
                    monsters.add(m4Point);
                    secondMonster = RoomIds.MONSTER_4;
                }
            } else {
                Point m3 = firstFar(adj, freeLeaves, monsters, MONSTER_MIN_DIST);
                if (m3 != null) {
                    labels.put(m3, RoomIds.MONSTER_3);
                    monsters.add(m3);
                    freeLeaves.remove(m3);
                    secondMonster = RoomIds.MONSTER_3;
                }
            }
        }
        if (secondMonster == null) return null;

        // Les feuilles non utilisées deviennent des culs-de-sac génériques.
        for (Point n : freeLeaves) labels.put(n, RoomIds.DEAD_END);

        for (Point n : cNodes) {
            if (!labels.containsKey(n)) { labels.put(n, RoomIds.WELL); break; }
        }

        for (Point n : cNodes) {
            if (labels.containsKey(n)) continue;
            for (Point nb : adj.get(n)) {
                String lbl = labels.get(nb);
                if (lbl != null && (lbl.equals(RoomIds.MONSTER_2) || lbl.equals(RoomIds.MONSTER_4) || lbl.equals(RoomIds.WELL))) {
                    List<Point> nbs = new ArrayList<>(adj.get(n));
                    if (nbs.size() == 2 && !isStraight(nbs.get(0), nbs.get(1))) {
                        labels.put(n, RoomIds.CORRIDOR_TURN); break;
                    }
                }
            }
        }
        for (Point n : cNodes) if (!labels.containsKey(n)) labels.put(n, pickC(rng));
        for (Point n : i2Nodes) if (!labels.containsKey(n)) labels.put(n, RoomIds.CORRIDOR_TURN);
        for (Point n : i3Nodes) labels.put(n, RoomIds.INTERSECTION_3);
        for (Point n : i4Nodes) labels.put(n, RoomIds.INTERSECTION_4);

        for (Point ip : i4Nodes) {
            for (Point nb : adj.get(ip)) {
                String lbl = labels.get(nb);
                if (lbl == null || !(lbl.equals("C1") || lbl.equals("C2") || lbl.equals("C3"))) continue;
                List<Point> nAdj = new ArrayList<>(adj.get(nb));
                if (nAdj.size() != 2) continue;
                
                Point a0 = nAdj.get(0);
                Point a1 = nAdj.get(1);
                int adx = a1.x() - a0.x();
                int adz = a1.y() - a0.y();
                if (adx * (nb.x() - ip.x()) + adz * (nb.y() - ip.y()) == 0) {
                    if (adx != 0 && adz != 0) labels.put(nb, RoomIds.CORRIDOR_TURN);
                }
            }
        }
        // Règle cul-de-sac (conversation 3) : jamais au bout d'une ligne droite
        // (virage/intersection requis) — sinon rejet et retry amont.
        if (!enforceDeadEndAfterTurn(labels, adj, rng)) return null;
        return labels;
    }

    // ===================== Algorithm: Tavern =====================

    private static TavernResult placeTavernAndPath(Map<Point, Set<Point>> adj, Point porte, Random rng) {
        Point parent = adj.get(porte).iterator().next();
        // Direction sortante porte → chemin (alignée sur l'entrée intérieure)
        int dx = porte.x() - parent.x(), dy = porte.y() - parent.y();

        int maxLen = 2 + rng.nextInt(4);
        int cx = porte.x(), cy = porte.y();
        boolean lastStraight = false;
        List<Point> pathCells = new ArrayList<>();
        // Snapshot adj pour tester colinearRunAfterEdge avant d'ajouter (chemins temporaires)
        Map<Point, Set<Point>> tmpAdj = copyAdj(adj);
        Point curTmp = porte;

        for (int i = 0; i < maxLen; i++) {
            // Ne PAS forcer le droit : l'approche intérieure (M5→gap→porte) a déjà
            // consommé du budget colinéaire. On tourne si la droite dépasserait MAX.
            Point straight = new Point(cx + dx, cy + dy);
            boolean straightOk = !straight.isOutOfBounds() && !tmpAdj.containsKey(straight)
                    && colinearRunAfterEdge(curTmp, straight, tmpAdj) <= MAX_COLINEAR_RUN;
            boolean goStraight = straightOk && (lastStraight ? rng.nextBoolean() : rng.nextFloat() < 0.35f);
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
                    if (colinearRunAfterEdge(curTmp, t, tmpAdj) > MAX_COLINEAR_RUN) continue;
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
            if (colinearRunAfterEdge(curTmp, next, tmpAdj) > MAX_COLINEAR_RUN) break;
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
                || colinearRunAfterEdge(lastPath, t1, tmpAdj) > MAX_COLINEAR_RUN) {
            // Forcer un virage d'approche de la taverne
            int[][] perp = {{dy, -dx}, {-dy, dx}};
            boolean okT = false;
            for (int[] turn : perp) {
                Point cand = new Point(cx + turn[0], cy + turn[1]);
                if (cand.isOutOfBounds() || adj.containsKey(cand) || tmpAdj.containsKey(cand)) continue;
                if (colinearRunAfterEdge(lastPath, cand, tmpAdj) > MAX_COLINEAR_RUN) continue;
                t1 = cand; dx = turn[0]; dy = turn[1]; okT = true; break;
            }
            if (!okT) return null;
        }
        Point t2 = t1.move(dx, dy);
        int pex = -dy, pey = dx;
        Point t3 = t2.move(pex, pey);
        Point t4 = t1.move(pex, pey);
        Point ext = t4.move(pex, pey);

        for (Point p : Arrays.asList(t1, t2, t3, t4, ext)) {
            if (adj.containsKey(p) || p.isOutOfBounds()) return null;
        }

        Set<Point> pathSet = new HashSet<>();
        Point cur = porte;
        for (Point cell : pathCells) {
            pathSet.add(cell); adj.put(cell, new HashSet<>()); adj.get(cell).add(cur); adj.get(cur).add(cell); cur = cell;
        }
        adj.put(t1, new HashSet<>()); adj.get(cur).add(t1); adj.get(t1).add(cur);
        adj.put(t2, new HashSet<>()); adj.get(t1).add(t2); adj.get(t2).add(t1);
        adj.put(t3, new HashSet<>()); adj.get(t2).add(t3); adj.get(t3).add(t2);
        adj.put(t4, new HashSet<>()); adj.get(t3).add(t4); adj.get(t4).add(t3);
        adj.put(ext, new HashSet<>()); adj.get(t4).add(ext); adj.get(ext).add(t4);

        TavernResult tr = new TavernResult();
        tr.tavern = Map.of(RoomIds.TAVERN_1, t1, RoomIds.TAVERN_2, t2, RoomIds.TAVERN_3, t3, RoomIds.TAVERN_4, t4);
        tr.exitPoint = ext; tr.pathSet = pathSet;
        return tr;
    }

    // ===================== Algorithm: analyzePart2 =====================

    private static Map<Point, String> analyzePart2(Map<Point, Set<Point>> adj, Point exitPoint,
                                                    Map<Point, String> labels, Set<Point> pathSet, Random rng) {
        // Sortie de taverne : label structurel déduit de l'adjacence réelle (forme P1/P2).
        labels.put(exitPoint, labelForNeighbors(adj.get(exitPoint), Theme.P12, rng));

        Set<Point> allLabeled = new HashSet<>(labels.keySet());
        List<Point> p2Nodes = new ArrayList<>();
        Map<Point, Integer> dist = new HashMap<>();
        Set<Point> seen = new HashSet<>(); Queue<Point> q = new LinkedList<>();
        q.add(exitPoint); seen.add(exitPoint); dist.put(exitPoint, 0);
        while (!q.isEmpty()) { 
            Point n = q.poll(); 
            if (!allLabeled.contains(n)) p2Nodes.add(n); 
            for (Point nb : adj.get(n)) { 
                if (!seen.contains(nb)) { 
                    seen.add(nb); q.add(nb); dist.put(nb, dist.get(n) + 1); 
                } 
            } 
        }

        List<Point> leaves = new ArrayList<>(), internals = new ArrayList<>();
        for (Point n : p2Nodes) { if (adj.get(n).size() == 1) leaves.add(n); else internals.add(n); }

        List<Point> cList = new ArrayList<>(), i2List = new ArrayList<>(), i3List = new ArrayList<>(), i4List = new ArrayList<>();
        for (Point node : internals) {
            int deg = adj.get(node).size();
            if (deg == 2) {
                List<Point> nb = new ArrayList<>(adj.get(node));
                if (isStraight(nb.get(0), nb.get(1))) cList.add(node); else i2List.add(node);
            } else if (deg == 3) i3List.add(node);
            else if (deg == 4) i4List.add(node);
        }
        Collections.shuffle(cList, rng);

        // porte2 déjà placée en fin de tronc (appendP2ExitSequence) — exclure des leaves
        Point existingPorte2 = findPointByValue(labels, RoomIds.DOOR_2);
        if (existingPorte2 != null) leaves.remove(existingPorte2);

        // Besoin : ogre + fontaine + m3 + m1 + loot ≥ 5 feuilles de branches.
        if (leaves.size() < 5) return null;
        // Et 3 couloirs droits LIBRES pour M2 + M4 + M5 (hors chemin taverne).
        long availCorr = cList.stream().filter(n -> !pathSet.contains(n) && labels.get(n) == null).count();
        if (availCorr < 3) return null;

        // ESPACEMENT MONSTRES + ISOLEMENT OGRE (règles dures, conversation 3) : toute salle
        // monstre est à distance >= MONSTER_MIN_DIST des autres ET à distance
        // >= OGRE_MIN_MONSTER_DIST de l'Ogre. L'Ogre reste posé sur la feuille la plus
        // éloignée possible de la sortie de taverne. Infaisable => rejet (null => retry).
        Set<Point> monsters = monsterPoints(labels); // M1-M4 hérités de P1
        Set<Point> monsterSet = new HashSet<>();

        List<Point> ogreCands = new ArrayList<>(leaves);
        // Tri par distance décroissante à la sortie : l'Ogre reste la feuille la plus éloignée.
        ogreCands.sort(Comparator.comparingInt((Point n) -> -dist.getOrDefault(n, 0)));
        Point ogreLeaf = null;
        for (Point n : ogreCands) {
            if (isFarFromAll(adj, n, monsters, OGRE_MIN_MONSTER_DIST)) { ogreLeaf = n; break; }
        }
        if (ogreLeaf == null) return null;
        labels.put(ogreLeaf, RoomIds.OGRE); monsterSet.add(ogreLeaf);
        Map<Point, Integer> ogreDist = bfsDistances(adj, ogreLeaf);
        List<Point> remain = new ArrayList<>(leaves);
        remain.remove(ogreLeaf);
        Collections.shuffle(remain, rng);

        if (remain.isEmpty()) return null;
        labels.put(remain.get(0), RoomIds.FOUNTAIN);
        // porte2 déjà posée structurellement — ne pas la re-choisir ici

        Point m3Leaf = null;
        for (Point n : remain) {
            if (labels.containsKey(n)) continue;
            if (ogreDist.getOrDefault(n, Integer.MAX_VALUE) < OGRE_MIN_MONSTER_DIST) continue;
            if (!isFarFromAll(adj, n, monsters, MONSTER_MIN_DIST)) continue;
            m3Leaf = n; break;
        }
        if (m3Leaf == null) return null;
        labels.put(m3Leaf, RoomIds.MONSTER_3); monsterSet.add(m3Leaf); monsters.add(m3Leaf);

        // M1 (feuille) : OBLIGATOIRE en P2, espacement respecté, sinon rejet (null => retry).
        Point m1Leaf = null;
        for (Point n : remain) {
            if (labels.containsKey(n)) continue;
            if (ogreDist.getOrDefault(n, Integer.MAX_VALUE) < OGRE_MIN_MONSTER_DIST) continue;
            if (!isFarFromAll(adj, n, monsters, MONSTER_MIN_DIST)) continue;
            m1Leaf = n; break;
        }
        if (m1Leaf == null) return null;
        labels.put(m1Leaf, RoomIds.MONSTER_1); monsterSet.add(m1Leaf); monsters.add(m1Leaf);

        Point lootLeaf = null;
        for (Point n : remain) {
            if (labels.containsKey(n)) continue;
            boolean hasAdj = false; for (Point nb : adj.get(n)) if (monsterSet.contains(nb)) { hasAdj = true; break; }
            if (!hasAdj) { lootLeaf = n; break; }
        }
        if (lootLeaf == null) { for (Point n : remain) { if (!labels.containsKey(n)) { lootLeaf = n; break; } } }
        labels.put(lootLeaf, RoomIds.LOOT_1);

        for (Point n : remain) { if (!labels.containsKey(n)) labels.put(n, RoomIds.DEAD_END); }

        // M4 (couloir droit) : OBLIGATOIRE en P2, espacement respecté, sinon rejet (null).
        Point m4Corr = null;
        for (Point n : cList) {
            if (pathSet.contains(n) || labels.containsKey(n)) continue;
            if (ogreDist.getOrDefault(n, Integer.MAX_VALUE) < OGRE_MIN_MONSTER_DIST) continue;
            if (!isFarFromAll(adj, n, monsters, MONSTER_MIN_DIST)) continue;
            m4Corr = n; break;
        }
        if (m4Corr == null) return null;
        labels.put(m4Corr, RoomIds.MONSTER_4); monsterSet.add(m4Corr); monsters.add(m4Corr);

        // M2 (couloir droit) : OBLIGATOIRE en P2, espacement respecté, sinon rejet (null).
        Point m2Corr = null;
        for (Point n : cList) {
            if (pathSet.contains(n) || labels.containsKey(n)) continue;
            if (ogreDist.getOrDefault(n, Integer.MAX_VALUE) < OGRE_MIN_MONSTER_DIST) continue;
            if (!isFarFromAll(adj, n, monsters, MONSTER_MIN_DIST)) continue;
            m2Corr = n; break;
        }
        if (m2Corr == null) return null;
        labels.put(m2Corr, RoomIds.MONSTER_2); monsterSet.add(m2Corr); monsters.add(m2Corr);

        // M5 (couloir droit) : n'est PLUS avant porte2 — elle est OBLIGATOIRE n'importe où
        // sur une ligne droite de la zone P2 (hors chemin taverne), espacement respecté,
        // sinon rejet (null => retry amont).
        Point m5Corr = null;
        for (Point n : cList) {
            if (pathSet.contains(n) || labels.containsKey(n)) continue;
            if (ogreDist.getOrDefault(n, Integer.MAX_VALUE) < OGRE_MIN_MONSTER_DIST) continue;
            if (!isFarFromAll(adj, n, monsters, MONSTER_MIN_DIST)) continue;
            m5Corr = n; break;
        }
        if (m5Corr == null) return null;
        labels.put(m5Corr, RoomIds.MONSTER_5); monsterSet.add(m5Corr); monsters.add(m5Corr);

        List<Point> remC = new ArrayList<>();
        for (Point n : cList) { if (!labels.containsKey(n)) remC.add(n); }
        if (!remC.isEmpty()) {
            Point pn = null;
            for (Point n : remC) {
                if (!pathSet.contains(n)) {
                    boolean adjSpecial = false;
                    for (Point nb : adj.get(n)) {
                        String lbl = labels.get(nb);
                        if (lbl != null && (lbl.equals(RoomIds.OGRE) || lbl.equals(RoomIds.FOUNTAIN) || lbl.equals(RoomIds.LOOT_1)
                            || lbl.equals(RoomIds.MONSTER_1) || lbl.equals(RoomIds.MONSTER_2) || lbl.equals(RoomIds.MONSTER_3) || lbl.equals(RoomIds.MONSTER_4)
                            || lbl.equals(RoomIds.MONSTER_5))) {
                            adjSpecial = true; break;
                        }
                    }
                    if (!adjSpecial) { pn = n; break; }
                }
            }
            if (pn == null) { for (Point n : remC) { if (!pathSet.contains(n)) { pn = n; break; } } }
            if (pn == null) pn = remC.get(0);
            labels.put(pn, RoomIds.WELL);
            remC.remove(pn);
        }

        Set<Point> toI2 = new HashSet<>();
        for (Point n : remC) {
            if (labels.containsKey(n)) continue;
            for (Point nb : adj.get(n)) {
                String lbl = labels.get(nb);
                if (lbl != null && (lbl.equals(RoomIds.MONSTER_2) || lbl.equals(RoomIds.MONSTER_4) || lbl.equals(RoomIds.MONSTER_5) || lbl.equals(RoomIds.WELL))) {
                    toI2.add(n); break;
                }
            }
        }

        Map<Point, String> remCDirs = new HashMap<>();
        for (Point n : remC) {
            if (toI2.contains(n)) continue;
            List<Point> nb = new ArrayList<>(adj.get(n));
            Point p0 = nb.get(0);
            Point p1 = nb.get(1);
            int dx = p1.x() - p0.x(), dz = p1.y() - p0.y();
            if (dx < 0 || (dx == 0 && dz < 0)) { dx = -dx; dz = -dz; }
            remCDirs.put(n, dx + "," + dz);
        }
        Set<Point> cSkip = new HashSet<>();
        for (Point n : remC) {
            if (cSkip.contains(n) || labels.containsKey(n) || toI2.contains(n)) continue;
            String dir = remCDirs.get(n);
            for (Point nb : adj.get(n)) {
                if (!remC.contains(nb) || cSkip.contains(nb) || labels.containsKey(nb) || toI2.contains(nb)) continue;
                if (dir.equals(remCDirs.get(nb))) { cSkip.add(nb); break; }
            }
        }
        for (Point n : remC) {
            if (toI2.contains(n)) {
                List<Point> nbs = new ArrayList<>(adj.get(n));
                if (nbs.size() == 2) {
                    if (!isStraight(nbs.get(0), nbs.get(1))) labels.put(n, RoomIds.CORRIDOR_TURN);
                    else labels.put(n, pickC(rng));
                }
            } else if (labels.containsKey(n)) continue;
            else labels.put(n, pickC(rng));
        }
        for (Point n : i2List) labels.put(n, RoomIds.CORRIDOR_TURN);
        for (Point n : i3List) labels.put(n, RoomIds.INTERSECTION_3);
        for (Point n : i4List) labels.put(n, RoomIds.INTERSECTION_4);

        for (Point ip : i4List) {
            for (Point nb : adj.get(ip)) {
                String lbl = labels.get(nb);
                if (lbl == null || !(lbl.equals("C1") || lbl.equals("C2") || lbl.equals("C3"))) continue;
                List<Point> nAdj = new ArrayList<>(adj.get(nb));
                if (nAdj.size() != 2) continue;
                Point a0 = nAdj.get(0);
                Point a1 = nAdj.get(1);
                int adx = a1.x() - a0.x(), adz = a1.y() - a0.y();
                if (adx * (nb.x() - ip.x()) + adz * (nb.y() - ip.y()) == 0) {
                    if (adx != 0 && adz != 0) labels.put(nb, RoomIds.CORRIDOR_TURN);
                }
            }
        }
        // Règle cul-de-sac (conversation 3) : jamais au bout d'une ligne droite
        // (virage/intersection requis) — sinon rejet et retry amont.
        if (!enforceDeadEndAfterTurn(labels, adj, rng)) return null;
        return labels;
    }

    // ===================== Algorithm: Camp =====================

    private static CampResult placeCampAndPath(Map<Point, Set<Point>> adj, Point porte2, Random rng) {
        Point parent = adj.get(porte2).iterator().next();
        int dx = porte2.x() - parent.x(), dy = porte2.y() - parent.y();

        int maxLen = 2 + rng.nextInt(4);
        int cx = porte2.x(), cy = porte2.y();
        boolean lastStraight = false;
        List<Point> pathCells = new ArrayList<>();
        Map<Point, Set<Point>> tmpAdj = copyAdj(adj);
        Point curTmp = porte2;

        for (int i = 0; i < maxLen; i++) {
            // Respecte MAX_COLINEAR_RUN sur l'adj globale (tronc + M5 + gap + porte déjà posés)
            Point straight = new Point(cx + dx, cy + dy);
            boolean straightOk = !straight.isOutOfBounds() && !tmpAdj.containsKey(straight)
                    && colinearRunAfterEdge(curTmp, straight, tmpAdj) <= MAX_COLINEAR_RUN;
            boolean goStraight = straightOk && (lastStraight ? rng.nextBoolean() : rng.nextFloat() < 0.35f);
            int ndx = dx, ndy = dy;
            if (!goStraight) {
                int[][] perp = {{dy, -dx}, {-dy, dx}};
                boolean turned = false;
                int startP = rng.nextInt(2);
                for (int k = 0; k < 2; k++) {
                    int[] turn = perp[(startP + k) % 2];
                    Point t = new Point(cx + turn[0], cy + turn[1]);
                    if (t.isOutOfBounds() || tmpAdj.containsKey(t)) continue;
                    if (colinearRunAfterEdge(curTmp, t, tmpAdj) > MAX_COLINEAR_RUN) continue;
                    ndx = turn[0]; ndy = turn[1];
                    dx = ndx; dy = ndy;
                    turned = true;
                    break;
                }
                if (!turned) {
                    if (!straightOk) break;
                    ndx = dx; ndy = dy;
                    goStraight = true;
                }
            } else {
                ndx = dx; ndy = dy;
            }
            Point next = new Point(cx + ndx, cy + ndy);
            if (tmpAdj.containsKey(next) || next.isOutOfBounds()) break;
            if (colinearRunAfterEdge(curTmp, next, tmpAdj) > MAX_COLINEAR_RUN) break;
            tmpAdj.putIfAbsent(curTmp, new HashSet<>());
            tmpAdj.putIfAbsent(next, new HashSet<>());
            tmpAdj.get(curTmp).add(next);
            tmpAdj.get(next).add(curTmp);
            pathCells.add(next);
            curTmp = next;
            cx = next.x(); cy = next.y();
            lastStraight = goStraight;
        }
        // Besoin d'au moins 2 cellules de chemin vers le campement
        if (pathCells.size() < 2) return null;

        Point c1 = new Point(cx + dx, cy + dy);
        Point lastPath = pathCells.get(pathCells.size() - 1);
        if (c1.isOutOfBounds() || adj.containsKey(c1)
                || colinearRunAfterEdge(lastPath, c1, tmpAdj) > MAX_COLINEAR_RUN) {
            int[][] perp = {{dy, -dx}, {-dy, dx}};
            boolean okC = false;
            for (int[] turn : perp) {
                Point cand = new Point(cx + turn[0], cy + turn[1]);
                if (cand.isOutOfBounds() || adj.containsKey(cand) || tmpAdj.containsKey(cand)) continue;
                if (colinearRunAfterEdge(lastPath, cand, tmpAdj) > MAX_COLINEAR_RUN) continue;
                c1 = cand; dx = turn[0]; dy = turn[1]; okC = true; break;
            }
            if (!okC) return null;
        }
        Point c2 = c1.move(dx, dy);
        int pex = dy, pey = -dx;
        Point c3 = c2.move(pex, pey);
        Point c4 = c3.move(-dx, -dy);
        Point cex = c3.move(dx, dy);

        for (Point k : Arrays.asList(c1, c2, c3, c4, cex)) {
            if (adj.containsKey(k) || k.isOutOfBounds()) return null;
        }

        Set<Point> campPathSet = new HashSet<>();
        Point cur = porte2;
        for (Point cell : pathCells) {
            campPathSet.add(cell);
            adj.put(cell, new HashSet<>()); adj.get(cell).add(cur); adj.get(cur).add(cell); cur = cell;
        }
        adj.put(c1, new HashSet<>()); adj.get(cur).add(c1); adj.get(c1).add(cur);
        adj.put(c2, new HashSet<>()); adj.get(c1).add(c2); adj.get(c2).add(c1);
        adj.put(c3, new HashSet<>()); adj.get(c2).add(c3); adj.get(c3).add(c2);
        adj.put(c4, new HashSet<>()); adj.get(c3).add(c4); adj.get(c4).add(c3);
        adj.put(cex, new HashSet<>()); adj.get(c3).add(cex); adj.get(cex).add(c3);

        CampResult cr = new CampResult();
        cr.campExit = cex; cr.campPathSet = campPathSet;
        cr.campNodes = Map.of(RoomIds.CAMP_1, c1, RoomIds.CAMP_2, c2, RoomIds.CAMP_3, c3, RoomIds.CAMP_4, c4);
        return cr;
    }

    // ===================== Part 3 Trunk Tree & Mini-Trees =====================

    /**
     * Retire proprement des nœuds du graphe, y compris les arêtes RÉCIPROQUES chez les
     * voisins qui ne font PAS partie de l'ensemble retiré (ex: Bib2 au départ du couloir
     * du hub). Sans ça, un voisin gardait un lien fantôme vers une cellule supprimée.
     */
    private static void removeNodesClean(Map<Point, Set<Point>> adj, Collection<Point> nodes) {
        for (Point n : nodes) {
            Set<Point> ns = adj.remove(n);
            if (ns != null) {
                for (Point nb : ns) {
                    Set<Point> back = adj.get(nb);
                    if (back != null) back.remove(n);
                }
            }
        }
    }

    private static void addEdge(Map<Point, Set<Point>> adj, Set<Point> occupied, Point a, Point b) {
        adj.putIfAbsent(a, new HashSet<>());
        adj.putIfAbsent(b, new HashSet<>());
        adj.get(a).add(b);
        adj.get(b).add(a);
        occupied.add(a);
        occupied.add(b);
    }

    /** Copie superficielle de l'adjacence (sets clonés) pour tests de chemins. */
    private static Map<Point, Set<Point>> copyAdj(Map<Point, Set<Point>> adj) {
        Map<Point, Set<Point>> out = new HashMap<>();
        for (var e : adj.entrySet()) out.put(e.getKey(), new HashSet<>(e.getValue()));
        return out;
    }

    private static void growMiniTree(Point root, int pDir, Map<Point, Set<Point>> adj,
                                     Set<Point> occupied, Random rng) {
        // Délègue à la version bornée (limite colinéaire sur l'adj réelle)
        growMiniTreeBounded(root, pDir, adj, occupied, rng);
    }

    private static void growSplitBranches(Point e, Map<Point, Set<Point>> adj,
                                          Set<Point> occupied, Random rng) {
        List<Integer> ad = new ArrayList<>();
        for (int d = 0; d < 4; d++) {
            Point n = e.move(DIR_OFFSET[d]);
            if (!n.isOutOfBounds() && !occupied.contains(n)) {
                boolean isBack = adj.get(e).stream().anyMatch(nb -> nb.equals(n));
                if (!isBack) ad.add(d);
            }
        }
        Collections.shuffle(ad, rng);

        for (int b = 0; b < Math.min(2, ad.size()); b++) {
            int bDir = ad.get(b);
            Point bk = e.move(DIR_OFFSET[bDir]);
            if (colinearRunAfterEdge(e, bk, adj) > MAX_COLINEAR_RUN) continue;
            addEdge(adj, occupied, e, bk);

            int bl = 3 + rng.nextInt(4);
            int cDir = bDir; Point c = bk;
            for (int s = 0; s < bl; s++) {
                boolean mustTurn = colinearRunAfterEdge(c, c.move(DIR_OFFSET[cDir]), adj) > MAX_COLINEAR_RUN;
                if (mustTurn || rng.nextFloat() < 0.3f) {
                    cDir = (cDir + (rng.nextBoolean() ? 1 : 3)) % 4;
                }
                Point nn = c.move(DIR_OFFSET[cDir]);
                if (nn.isOutOfBounds() || occupied.contains(nn)) break;
                if (colinearRunAfterEdge(c, nn, adj) > MAX_COLINEAR_RUN) {
                    boolean ok = false;
                    for (int side : new int[]{1, 3}) {
                        int td = (cDir + side) % 4;
                        Point cand = c.move(DIR_OFFSET[td]);
                        if (cand.isOutOfBounds() || occupied.contains(cand)) continue;
                        if (colinearRunAfterEdge(c, cand, adj) > MAX_COLINEAR_RUN) continue;
                        cDir = td; nn = cand; ok = true; break;
                    }
                    if (!ok) break;
                }
                addEdge(adj, occupied, c, nn);
                c = nn;
            }
        }
    }

    /**
     * Longueur max d'une run colinéaire dans l'adj (segments = arêtes alignées).
     * Parcourt tous les axes cardinaux depuis chaque nœud.
     * Une droite I3—C—C—I2 = 3 segments ; au-delà de {@link #MAX_COLINEAR_RUN} → invalide.
     */
    private static int maxColinearRunInGraph(Map<Point, Set<Point>> adj) {
        int max = 0;
        // Pour chaque arête, mesurer la run complète sur son axe (évite double-comptage partiel)
        Set<String> seenAxes = new HashSet<>();
        for (Point p : adj.keySet()) {
            for (Point nb : adj.getOrDefault(p, Set.of())) {
                int dx = Integer.signum(nb.x() - p.x());
                int dy = Integer.signum(nb.y() - p.y());
                if (Math.abs(dx) + Math.abs(dy) != 1) continue;
                // Normaliser l'axe pour ne le traiter qu'une fois (direction + point d'ancrage min)
                int ndx = dx, ndy = dy;
                if (ndx < 0 || (ndx == 0 && ndy < 0)) { ndx = -ndx; ndy = -ndy; }
                // Ancre = extrémité "minimale" de la run
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

    /** True si aucune droite géométrique de l'adj ne dépasse MAX_COLINEAR_RUN. */
    private static boolean respectsColinearLimit(Map<Point, Set<Point>> adj) {
        return maxColinearRunInGraph(adj) <= MAX_COLINEAR_RUN;
    }

    private static TreeResult generateTrunkTree(int targetMin, int targetMax,
                                                 Point startPt, Set<Point> blocked, int maxI3, int maxI4, Random rng) {
        Map<Point, Set<Point>> adj = new HashMap<>();
        Set<Point> occupied = new HashSet<>(blocked != null ? blocked : new HashSet<>());

        Point start = startPt != null ? startPt : new Point(GRID_SIZE / 2, GRID_SIZE / 2);
        adj.put(start, new HashSet<>());
        occupied.add(start);

        List<int[]> dirs = new ArrayList<>(Arrays.asList(DIR_OFFSET));
        Collections.shuffle(dirs, rng);
        int dir = -1;
        for (int[] d : dirs) {
            Point target = start.move(d);
            if (!target.isOutOfBounds() && !occupied.contains(target)) {
                for (int i = 0; i < 4; i++) {
                    if (DIR_OFFSET[i][0] == d[0] && DIR_OFFSET[i][1] == d[1]) { dir = i; break; }
                }
                break;
            }
        }
        if (dir < 0) { 
            TreeResult tr = new TreeResult(); 
            tr.startPoint = start; tr.startKey = start.key(); tr.startX = start.x(); tr.startY = start.y(); tr.adj = adj; 
            return tr; 
        }

        Point c = start.move(DIR_OFFSET[dir]);
        addEdge(adj, occupied, start, c);

        List<Point> trunkCells = new ArrayList<>();
        trunkCells.add(c);

        int trunkTarget = 8 + rng.nextInt(5);

        for (int t = 1; t < trunkTarget; t++) {
            // Limite géométrique : max MAX_COLINEAR_RUN segments alignés dans l'adj
            // (compte aussi le passage tout droit à travers une I3)
            int runIfStraight = colinearRunAfterEdge(c, c.move(DIR_OFFSET[dir]), adj);
            boolean forceTurn = runIfStraight > MAX_COLINEAR_RUN;
            boolean maybeTurn = runIfStraight >= 2 && rng.nextFloat() < 0.35f;
            if (forceTurn || maybeTurn) {
                dir = (dir + (rng.nextBoolean() ? 1 : 3)) % 4;
            }

            Point n = c.move(DIR_OFFSET[dir]);
            if (n.isOutOfBounds() || occupied.contains(n)
                    || colinearRunAfterEdge(c, n, adj) > MAX_COLINEAR_RUN) {
                int od = dir;
                boolean found = false;
                for (int a = 0; a < 4; a++) {
                    int td = (od + a) % 4;
                    Point cand = c.move(DIR_OFFSET[td]);
                    if (cand.isOutOfBounds() || occupied.contains(cand)) continue;
                    if (colinearRunAfterEdge(c, cand, adj) > MAX_COLINEAR_RUN) continue;
                    dir = td; n = cand; found = true; break;
                }
                if (!found) break;
            }

            addEdge(adj, occupied, c, n);
            trunkCells.add(n);
            c = n;

            if (t < trunkTarget - 1 && rng.nextFloat() < 0.5) {
                int pDir = (dir + (rng.nextBoolean() ? 1 : 3)) % 4;
                growMiniTree(n, pDir, adj, occupied, rng);
            }
        }

        Point endPoint = trunkCells.get(trunkCells.size() - 1);
        growSplitBranches(endPoint, adj, occupied, rng);

        int targetSize = targetMin + rng.nextInt(targetMax - targetMin + 1);
        int ci3 = 0, ci4 = 0;
        for (Set<Point> nb : adj.values()) { int d = nb.size(); if (d == 3) ci3++; else if (d == 4) ci4++; }

        while (adj.size() < targetSize) {
            List<Point[]> candidates = new ArrayList<>();
            for (Point node : adj.keySet()) {
                if (node.equals(start)) continue;
                int deg = adj.get(node).size();
                if (deg >= 4) continue;
                if (deg == 2 && ci3 >= maxI3) continue;
                if (deg == 3 && ci4 >= maxI4) continue;

                for (int[] d : DIR_OFFSET) {
                    Point next = node.move(d);
                    if (next.isOutOfBounds() || occupied.contains(next)) continue;
                    // Limite géométrique globale : pas plus de MAX_COLINEAR_RUN
                    // segments alignés dans l'adj (I3 comptés dans l'axe)
                    if (colinearRunAfterEdge(node, next, adj) > MAX_COLINEAR_RUN) continue;
                    if (deg >= 2) {
                        boolean skip = false;
                        for (Point n1 : adj.get(node)) {
                            for (Point n2 : adj.get(node)) {
                                if (n1.equals(n2)) continue;
                                if (node.x() - n1.x() == n2.x() - node.x() && node.y() - n1.y() == n2.y() - node.y()) {
                                    Set<Point> a1 = adj.get(n1), a2 = adj.get(n2);
                                    if ((a1 != null && a1.size() >= 3) || (a2 != null && a2.size() >= 3)) {
                                        skip = true; break;
                                    }
                                }
                            }
                            if (skip) break;
                        }
                        if (skip) continue;
                    }
                    int w = trunkCells.contains(node) ? 3 : 1;
                    for (int wi = 0; wi < w; wi++) candidates.add(new Point[]{node, next});
                }
            }
            if (candidates.isEmpty()) break;
            Point[] choice = candidates.get(rng.nextInt(candidates.size()));
            addEdge(adj, occupied, choice[0], choice[1]);
            int nd = adj.get(choice[0]).size();
            if (nd == 3) ci3++; else if (nd == 4) ci4++;
        }

        TreeResult tr = new TreeResult();
        tr.startPoint = start; tr.startKey = start.key(); tr.startX = start.x(); tr.startY = start.y(); tr.adj = adj;
        return tr;
    }

    private static TreeResult generatePart3Tree(Point startPoint, Set<Point> blocked, Random rng) {
        return generateTrunkTree(35, 45, startPoint, blocked, PART3_MAX_IJ3, PART3_MAX_IJ4, rng);
    }

    // ===================== Algorithm: analyzePart3 =====================

    private static Map<Point, String> analyzePart3(Map<Point, Set<Point>> adj, Point campExit,
                                                     Map<Point, String> labels, Random rng) {
        // Sortie de camp : label structurel déduit de l'adjacence réelle (thème donjon).
        labels.put(campExit, labelForNeighbors(adj.get(campExit), Theme.DJ, rng));

        Set<Point> allLabeled = new HashSet<>(labels.keySet());
        List<Point> bfsP3 = new ArrayList<>();
        Set<Point> seen = new HashSet<>(); Queue<Point> q = new LinkedList<>();
        q.add(campExit); seen.add(campExit);
        while (!q.isEmpty()) { 
            Point n = q.poll(); 
            if (!allLabeled.contains(n)) bfsP3.add(n); 
            for (Point nb : adj.get(n)) { 
                if (!seen.contains(nb)) { seen.add(nb); q.add(nb); } 
            } 
        }

        Point[] bibNodes = null;
        List<Point> shuffled = new ArrayList<>(bfsP3); Collections.shuffle(shuffled, rng);
        for (Point src : shuffled) {
            for (int[] d : DIR_OFFSET) {
                Point[] chainKeys = new Point[2];
                boolean chainOk = true;
                Point curr = src;
                for (int i = 0; i < 2; i++) {
                    Point next = curr.move(d);
                    if (next.isOutOfBounds() || adj.containsKey(next)) { chainOk = false; break; }
                    chainKeys[i] = next; curr = next;
                }
                if (chainOk) {
                    adj.put(chainKeys[0], new HashSet<>()); adj.get(src).add(chainKeys[0]); adj.get(chainKeys[0]).add(src);
                    adj.put(chainKeys[1], new HashSet<>()); adj.get(chainKeys[0]).add(chainKeys[1]); adj.get(chainKeys[1]).add(chainKeys[0]);
                    bibNodes = chainKeys; break;
                }
            }
            if (bibNodes != null) break;
        }
        if (bibNodes == null) return null;

        Point shopNode = null;
        shuffled = new ArrayList<>(bfsP3); Collections.shuffle(shuffled, rng);
        for (Point p : shuffled) {
            for (int[] d : DIR_OFFSET) {
                Point next = p.move(d);
                if (!next.isOutOfBounds() && !adj.containsKey(next)) {
                    adj.put(next, new HashSet<>()); adj.get(p).add(next); adj.get(next).add(p);
                    shopNode = next; break;
                }
            }
            if (shopNode != null) break;
        }

        Set<Point> allNodes = new HashSet<>(adj.keySet());
        List<Point> leaves = new ArrayList<>(), internals = new ArrayList<>();
        for (Point n : allNodes) { 
            if (adj.get(n).size() == 1 && !labels.containsKey(n)) leaves.add(n); 
            else if (adj.get(n).size() >= 2 && !labels.containsKey(n)) internals.add(n); 
        }

        List<Point> cjList = new ArrayList<>(), ij2List = new ArrayList<>(), ij3List = new ArrayList<>(), ij4List = new ArrayList<>();
        for (Point node : internals) {
            int deg = adj.get(node).size();
            if (deg == 2) {
                List<Point> nb = new ArrayList<>(adj.get(node));
                if (isStraight(nb.get(0), nb.get(1))) cjList.add(node); else ij2List.add(node);
            } else if (deg == 3) ij3List.add(node);
            else if (deg == 4) ij4List.add(node);
        }

        labels.put(bibNodes[0], RoomIds.BIB_1); labels.put(bibNodes[1], RoomIds.BIB_2);
        
        Point b2 = bibNodes[1];
        Point b1 = bibNodes[0];
        int ddx = b2.x() - b1.x(), ddz = b2.y() - b1.y();
        int cdx, cdz;
        if (ddx == 1 && ddz == 0) { cdx = 0; cdz = 1; }
        else if (ddx == -1 && ddz == 0) { cdx = 0; cdz = -1; }
        else if (ddx == 0 && ddz == 1) { cdx = -1; cdz = 0; }
        else if (ddx == 0 && ddz == -1) { cdx = 1; cdz = 0; }
        else { cdx = 0; cdz = 1; }

        boolean hubOk = false;
        Map<Point, String> topLbls = new HashMap<>();

        for (int corridorLen : new int[]{5, 6, 7}) {
            if (hubOk) break;
            int cx = b2.x(), cz = b2.y();
            Point prev = bibNodes[1];
            List<Point> corrNodes = new ArrayList<>();
            boolean ok = true;
            int mid = corridorLen / 2;
            for (int i = 0; i < corridorLen && ok; i++) {
                if (i == mid) {
                    int[][] perp = {{ddx, ddz}, {-ddx, -ddz}};
                    int ti = rng.nextInt(2);
                    Point t = new Point(cx + perp[ti][0], cz + perp[ti][1]);
                    if (adj.containsKey(t) || t.isOutOfBounds()) { 
                        ti = ti == 0 ? 1 : 0; 
                        t = new Point(cx + perp[ti][0], cz + perp[ti][1]); 
                    }
                    if (!adj.containsKey(t) && !t.isOutOfBounds()) {
                        corrNodes.add(t); adj.put(t, new HashSet<>());
                        adj.get(t).add(prev); adj.get(prev).add(t); cx = t.x(); cz = t.y(); prev = t;
                    } else { ok = false; break; }
                }
                Point next = new Point(cx + cdx, cz + cdz);
                if (adj.containsKey(next) || next.isOutOfBounds()) { ok = false; break; }
                corrNodes.add(next); adj.put(next, new HashSet<>());
                adj.get(next).add(prev); adj.get(prev).add(next); cx = next.x(); cz = next.y(); prev = next;
            }
            if (!ok || corrNodes.size() < 3) { removeNodesClean(adj, corrNodes); continue; }

            Point wKey = new Point(cx + 1, cz);
            if (adj.containsKey(wKey) || wKey.isOutOfBounds()) { removeNodesClean(adj, corrNodes); continue; }
            adj.put(wKey, new HashSet<>()); adj.get(wKey).add(prev); adj.get(prev).add(wKey);
            corrNodes.add(wKey);

            int hx2 = cx, hz2 = cz + 1;
            if (hz2 + 1 >= GRID_SIZE) { removeNodesClean(adj, corrNodes); continue; }
            Point[] hubCells = {new Point(hx2, hz2), new Point(hx2+1, hz2), new Point(hx2, hz2+1), new Point(hx2+1, hz2+1)};
            boolean hubFree = true;
            for (Point c : hubCells) { if (adj.containsKey(c) || c.isOutOfBounds()) { hubFree = false; break; } }
            if (!hubFree) { removeNodesClean(adj, corrNodes); continue; }

            Point hubKey = new Point(hx2, hz2);
            adj.put(hubKey, new HashSet<>()); adj.get(hubKey).add(wKey); adj.get(wKey).add(hubKey);

            for (Point n : corrNodes) {
                List<Point> nb = new ArrayList<>(adj.get(n));
                if (nb.size() == 2) {
                    Point na = nb.get(0);
                    Point nb2 = nb.get(1);
                    boolean coll = (n.x() - na.x()) == (nb2.x() - n.x()) && (n.y() - na.y()) == (nb2.y() - n.y());
                    labels.put(n, coll ? pickC(rng) : RoomIds.CORRIDOR_TURN);
                }
            }
            labels.put(hubKey, RoomIds.CENTRALE);

            topLbls.put(hubKey, RoomIds.CENTRALE);
            int[] exOff = {-1, 2, 1, 1, 0};
            int[] ezOff = {0, 0, -1, 2, 2};
            for (int ei = 0; ei < 5; ei++) {
                Point ePt = new Point(hx2 + exOff[ei], hz2 + ezOff[ei]);
                if (!ePt.isOutOfBounds()) {
                    topLbls.put(ePt, pickCJ(rng));
                }
            }
            hubOk = true;
        }
        if (!hubOk) { bibNodes = null; return null; }
        cjList.remove(bibNodes[0]);
        if (shopNode != null) { labels.put(shopNode, RoomIds.SHOP); leaves.remove(shopNode); }
        leaves.remove(bibNodes[1]); leaves.remove(bibNodes[0]);

        List<String> p3LootList = new ArrayList<>(RoomPools.LOOT_P3_P4);
        Collections.shuffle(p3LootList, rng);
        String p3LootA = p3LootList.get(0), p3LootB = p3LootList.get(1);
        int leafLoot, corrLoot;
        if (p3LootA.equals("Lootdj2") || p3LootB.equals("Lootdj2")) {
            leafLoot = 1; corrLoot = 1;
        } else {
            leafLoot = 2; corrLoot = 0;
        }

        int targetM3 = 4 + rng.nextInt(3);
        int availLeafM = Math.max(0, leaves.size() - leafLoot - 2);
        if (availLeafM + cjList.size() < targetM3 || leaves.size() < leafLoot + 2) return null;
        int leafM3 = Math.min(targetM3, availLeafM);
        int corrM3 = targetM3 - leafM3;
        if (corrM3 > cjList.size()) { corrM3 = cjList.size(); leafM3 = targetM3 - corrM3; }

        Collections.shuffle(leaves, rng);
        int li = leafLoot;
        Map<Point, String> p3LootAssign = new HashMap<>();
        if (leafLoot == 2) {
            p3LootAssign.put(leaves.get(0), p3LootA);
            p3LootAssign.put(leaves.get(1), p3LootB);
        } else {
            String leafType = p3LootA.equals("Lootdj2") ? p3LootB : p3LootA;
            p3LootAssign.put(leaves.get(0), leafType);
        }
        for (var e : p3LootAssign.entrySet()) labels.put(e.getKey(), e.getValue());

        // ESPACEMENT MONSTRES (règle dure, conversation 3) : chaque MJ est à distance
        // >= MONSTER_MIN_DIST des monstres déjà posés (M1-M5 de P1/P2 inclus).
        // Infaisable => rejet (return null => retry amont).
        Set<Point> mjSet = monsterPoints(labels);
        int placedLeafM3 = 0;
        while (placedLeafM3 < leafM3 && li < leaves.size()) {
            Point cand = leaves.get(li++);
            if (!isFarFromAll(adj, cand, mjSet, MONSTER_MIN_DIST)) continue;
            labels.put(cand, RoomPools.LEAF_MONSTERS_P3_P4.get(rng.nextInt(3)));
            mjSet.add(cand); placedLeafM3++;
        }
        if (placedLeafM3 < leafM3) return null;

        List<Point> restLeaves = new ArrayList<>();
        for (int i = leafLoot; i < leaves.size(); i++) if (!labels.containsKey(leaves.get(i))) restLeaves.add(leaves.get(i));
        restLeaves.sort(Comparator.comparingInt(lp -> -Math.abs(lp.x() - campExit.x()) - Math.abs(lp.y() - campExit.y())));
        for (Point n : restLeaves) {
            labels.put(n, n.equals(restLeaves.get(0)) ? RoomIds.GARDEN : (restLeaves.size() > 1 && n.equals(restLeaves.get(1)) ? RoomIds.STATUE : RoomIds.DEAD_END_DJ));
        }

        if (cjList.size() < corrM3 + corrLoot + 1) return null;
        Collections.shuffle(cjList, rng);
        // Couloirs MJ : même règle d'espacement (distance >= MONSTER_MIN_DIST), sinon la
        // cellule retourne au pool de couloirs génériques (comportement d'origine conservé).
        int mjPlaced = 0; List<Point> remCJ = new ArrayList<>();
        for (Point n : cjList) {
            if (mjPlaced < corrM3) {
                if (isFarFromAll(adj, n, mjSet, MONSTER_MIN_DIST)) { labels.put(n, RoomPools.CORRIDOR_MONSTERS_P3_P4.get(rng.nextInt(2))); mjSet.add(n); mjPlaced++; } else remCJ.add(n);
            } else remCJ.add(n);
        }
        int lc = 0;
        if (corrLoot == 1 && lc < remCJ.size()) {
            String corrType = p3LootA.equals("Lootdj2") ? p3LootA : p3LootB;
            labels.put(remCJ.get(lc), corrType); lc++;
        }
        if (remCJ.size() > lc) labels.put(remCJ.get(lc), RoomIds.WELL_DJ); else lc--;
        for (int i = lc + 1; i < remCJ.size(); i++) labels.put(remCJ.get(i), pickCJ(rng));
        for (Point n : ij2List) labels.put(n, RoomIds.CORRIDOR_TURN_J);
        for (Point n : ij3List) labels.put(n, RoomIds.INTERSECTION_3_J);
        for (Point n : ij4List) labels.put(n, RoomIds.INTERSECTION_4_J);
        for (Point ip : ij4List) {
            for (Point nb : adj.get(ip)) {
                String lbl = labels.get(nb);
                if (lbl == null || !(lbl.equals("CJ1") || lbl.equals("CJ2") || lbl.equals("CJ3"))) continue;
                List<Point> nAdj = new ArrayList<>(adj.get(nb));
                if (nAdj.size() != 2) continue;
                Point a0 = nAdj.get(0);
                Point a1 = nAdj.get(1);
                int adx = a1.x() - a0.x(), adz = a1.y() - a0.y();
                if (adx * (nb.x() - ip.x()) + adz * (nb.y() - ip.y()) == 0) {
                    if (adx != 0 && adz != 0) labels.put(nb, RoomIds.CORRIDOR_TURN_J);
                }
            }
        }

        // Règle cul-de-sac (conversation 3) : jamais au bout d'une ligne droite
        // (virage/intersection requis) — sinon rejet et retry amont.
        if (!enforceDeadEndAfterTurn(labels, adj, rng)) return null;
        return labels;
    }

    // ===================== Part 4 Sub-methods (Modular Refactoring) =====================

    private static final int P4_MAX_IJ3 = 2;
    private static final int P4_MAX_IJ4 = 1;

    private static void labelTreeNodes(Map<Point, Set<Point>> tr, Map<Point, String> topLabels, Point startPoint, Random rng) {
        List<Point> lf = new ArrayList<>(), co = new ArrayList<>(), i3 = new ArrayList<>(), i4 = new ArrayList<>();
        for (Point k : tr.keySet()) {
            int d = tr.get(k).size();
            if (d == 1) lf.add(k); else if (d == 2) co.add(k); else if (d == 3) i3.add(k); else if (d == 4) i4.add(k);
        }
        // Racine : si elle n'a qu'un voisin dans 'tr', l'arête vers l'exit du hub (absente de 'tr')
        // la rendra droite dans 'adj' — pickCJ direct est donc correct et évite un re-pick futur.
        if (tr.get(startPoint).size() == 1) topLabels.put(startPoint, pickCJ(rng));
        else topLabels.put(startPoint, labelForNeighbors(tr.get(startPoint), Theme.DJ, rng));
        for (Point k : i3) if (!topLabels.containsKey(k)) topLabels.put(k, labelForNeighbors(tr.get(k), Theme.DJ, rng));
        for (Point k : i4) if (!topLabels.containsKey(k)) topLabels.put(k, labelForNeighbors(tr.get(k), Theme.DJ, rng));
        for (Point k : co) if (!topLabels.containsKey(k)) topLabels.put(k, labelForNeighbors(tr.get(k), Theme.DJ, rng));
        for (Point k : lf) if (!topLabels.containsKey(k)) topLabels.put(k, labelForNeighbors(tr.get(k), Theme.DJ, rng));
        // Règle spéciale : un couloir droit rattaché perpendiculairement à une intersection 4
        // devient un virage (visuellement, on tourne en entrant/sortant de l'intersection).
        for (Point ip : i4) {
            for (Point nb : tr.get(ip)) {
                String lbl = topLabels.get(nb);
                if (lbl == null || !(lbl.equals("CJ1") || lbl.equals("CJ2") || lbl.equals("CJ3"))) continue;
                List<Point> nAd = new ArrayList<>(tr.get(nb));
                if (nAd.size() != 2) continue;
                Point a0 = nAd.get(0);
                Point a1 = nAd.get(1);
                if ((a1.x() - a0.x()) * (nb.x() - ip.x()) + (a1.y() - a0.y()) * (nb.y() - ip.y()) == 0) topLabels.put(nb, RoomIds.CORRIDOR_TURN_J);
            }
        }
    }

    private static boolean placeGoblinVillage(Map<Point, Set<Point>> adj, Map<Point, String> topLabels, 
                                               Map<Point, Set<Point>> gt, Set<Point> globalOccupied, 
                                               Set<Point> goblinCells, Random rng) {
        Point gLeaf = null; Point firstGob = null; Point parent = null;
        for (Point testLeaf : new ArrayList<>(gt.keySet())) {
            if (topLabels.get(testLeaf) == null || !topLabels.get(testLeaf).equals(RoomIds.DEAD_END_DJ)) continue;
            parent = gt.get(testLeaf).iterator().next();
            int gdx = testLeaf.x() - parent.x(), gdy = testLeaf.y() - parent.y();
            Point target = testLeaf.move(gdx, gdy);
            if (!target.isOutOfBounds() && !globalOccupied.contains(target)) {
                firstGob = target; gLeaf = testLeaf; break;
            }
        }
        if (gLeaf == null || firstGob == null) return false;

        topLabels.put(gLeaf, RoomIds.GOBLIN_DOOR);
        Map<Point, Set<Point>> ga = new HashMap<>();
        ga.put(gLeaf, new HashSet<>());
        globalOccupied.add(firstGob); ga.put(firstGob, new HashSet<>());
        ga.get(gLeaf).add(firstGob); ga.get(firstGob).add(gLeaf);

        TreeResult gobRaw = generateRawTree(20, 25, 3, 1, 2, firstGob, new HashSet<>(globalOccupied), rng);
        if (gobRaw.adj.size() < 6) return false;

        for (var e : gobRaw.adj.entrySet()) {
            ga.putIfAbsent(e.getKey(), new HashSet<>());
            ga.get(e.getKey()).addAll(e.getValue());
        }

        if (ga.get(gLeaf).size() > 2) {
            Point keepNb = null;
            for (Point nb : ga.get(gLeaf)) { if (!nb.equals(parent)) { keepNb = nb; break; } }
            for (Point nb : new ArrayList<>(ga.get(gLeaf))) {
                if (!nb.equals(parent) && !nb.equals(keepNb)) { ga.get(nb).remove(gLeaf); ga.get(gLeaf).remove(nb); }
            }
        }
        globalOccupied.addAll(ga.keySet());

        List<Point> gl = new ArrayList<>(), gco = new ArrayList<>(), gi3 = new ArrayList<>(), gi4 = new ArrayList<>();
        for (Point k : ga.keySet()) {
            if (k.equals(gLeaf)) continue;
            int d = ga.get(k).size();
            if (d == 1) gl.add(k); else if (d == 2) gco.add(k); else if (d == 3) gi3.add(k); else if (d == 4) gi4.add(k);
        }
        for (Point k : gi3) if (!topLabels.containsKey(k)) topLabels.put(k, labelForNeighbors(ga.get(k), Theme.GOBLIN, rng));
        for (Point k : gi4) if (!topLabels.containsKey(k)) topLabels.put(k, labelForNeighbors(ga.get(k), Theme.GOBLIN, rng));

        for (Point k : gco) {
            List<Point> nb2 = new ArrayList<>(ga.get(k));
            if (isStraight(nb2.get(0), nb2.get(1))) { topLabels.put(k, RoomIds.GOBLIN_WELL); break; }
        }

        Collections.shuffle(gl, rng);
        int gli = 0;
        if (gli < gl.size()) { topLabels.put(gl.get(gli), RoomIds.GOBLIN_MARCH); gli++; }
        if (gli < gl.size()) { topLabels.put(gl.get(gli), RoomIds.GOBLIN_ARMORY); gli++; }
        if (gli < gl.size()) { topLabels.put(gl.get(gli), RoomIds.GOBLIN_TREASURE); gli++; }

        for (Point k : gco) if (!topLabels.containsKey(k)) topLabels.put(k, labelForNeighbors(ga.get(k), Theme.GOBLIN, rng));
        for (Point k : gl) if (!topLabels.containsKey(k)) topLabels.put(k, labelForNeighbors(ga.get(k), Theme.GOBLIN, rng));

        List<Point> hl = new ArrayList<>();
        for (Point k : ga.keySet()) {
            if (k.equals(gLeaf)) continue;
            String v = topLabels.get(k);
            if (v != null && (v.equals(RoomIds.GOBLIN_CORRIDOR) || v.equals(RoomIds.GOBLIN_TURN) || v.equals(RoomIds.GOBLIN_DEAD_END) || v.equals(RoomIds.GOBLIN_I3) || v.equals(RoomIds.GOBLIN_I4))) hl.add(k);
        }
        Collections.shuffle(hl, rng);
        Set<Point> hs = new HashSet<>();
        int hMax = 3 + rng.nextInt(3);
        for (Point k : hl) {
            if (hs.size() >= hMax) break;
            int dk = ga.get(k).size();
            if (dk == 2) {
                List<Point> nb2 = new ArrayList<>(ga.get(k));
                if (isStraight(nb2.get(0), nb2.get(1))) continue;
            }
            boolean treeAdj = ga.getOrDefault(k, Set.of()).stream().anyMatch(hs::contains);
            if (treeAdj) continue;
            boolean adjSpecial = ga.getOrDefault(k, Set.of()).stream().anyMatch(nb -> {
                String vl = topLabels.get(nb);
                return vl != null && (vl.equals(RoomIds.GOBLIN_WELL) || vl.equals(RoomIds.GOBLIN_MARCH) || vl.equals(RoomIds.GOBLIN_ARMORY) || vl.equals(RoomIds.GOBLIN_TREASURE));
            });
            if (adjSpecial) continue;
            hs.add(k);
        }
        for (Point k : hs) {
            int d = ga.get(k).size();
            if (d == 3) topLabels.put(k, RoomIds.GOBLIN_HOUSE_3);
            else if (d == 2) {
                List<Point> nb2 = new ArrayList<>(ga.get(k));
                if (!isStraight(nb2.get(0), nb2.get(1))) topLabels.put(k, RoomIds.GOBLIN_HOUSE_2);
            } else if (d == 1) topLabels.put(k, RoomIds.GOBLIN_HOUSE_1);
        }
        for (var e : ga.entrySet()) { adj.putIfAbsent(e.getKey(), new HashSet<>()); adj.get(e.getKey()).addAll(e.getValue()); goblinCells.add(e.getKey()); }
        goblinCells.add(gLeaf);
        return true;
    }

    private static void placeChapelAndCrypt(Map<Point, Set<Point>> adj, Map<Point, String> topLabels, 
                                             Map<Point, Set<Point>> ct, Point cs, Set<Point> globalOccupied, Random rng) {
        for (Point k : ct.keySet()) {
            String vl = topLabels.get(k);
            if (ct.get(k).size() != 1 || k.equals(cs) || vl == null || !vl.equals(RoomIds.DEAD_END_DJ)) continue;
            Point mb = ct.get(k).iterator().next();
            Point e = k.move(k.x() - mb.x(), k.y() - mb.y());
            if (e.isOutOfBounds() || globalOccupied.contains(e)) continue;

            globalOccupied.add(e);
            adj.put(e, new HashSet<>());
            adj.get(k).add(e); adj.get(e).add(k);
            topLabels.put(k, RoomIds.CHAPEL_1); topLabels.put(e, RoomIds.CHAPEL_2);

            int dir = (k.x() - mb.x() == 1) ? 0 : (k.x() - mb.x() == -1) ? 2 : (k.y() - mb.y() == 1) ? 1 : 3;
            int pathLen = 3 + rng.nextInt(3);
            int turnAt = 1 + rng.nextInt(pathLen - 1);
            int turnDir = rng.nextBoolean() ? 1 : -1;
            Point pv = e; Point curr = e;

            for (int s = 0; s < pathLen; s++) {
                if (s == turnAt) dir = (dir + turnDir + 4) % 4;
                Point ncx = curr.move(DIR_OFFSET[dir]);
                if (ncx.isOutOfBounds() || globalOccupied.contains(ncx)) break;
                globalOccupied.add(ncx); adj.put(ncx, new HashSet<>());
                adj.get(pv).add(ncx); adj.get(ncx).add(pv);
                // Chemin chapelle -> crypte : couloirs thème P1/P2 (C/I2), demande du dev.
                topLabels.put(ncx, s == turnAt - 1 ? RoomIds.CORRIDOR_TURN : pickC(rng));
                pv = ncx; curr = ncx;
            }
            for (int s = 0; s < 2; s++) {
                Point ncx = curr.move(DIR_OFFSET[dir]);
                if (ncx.isOutOfBounds() || globalOccupied.contains(ncx)) break;
                globalOccupied.add(ncx); adj.put(ncx, new HashSet<>());
                adj.get(pv).add(ncx); adj.get(ncx).add(pv);
                topLabels.put(ncx, s == 0 ? RoomIds.CRYPT_1 : RoomIds.CRYPT_2); 
                pv = ncx; curr = ncx;
            }
            break;
        }
    }

    private static void placePrisonBlock(Map<Point, Set<Point>> adj, Map<Point, String> topLabels, 
                                          Map<Point, Set<Point>> pt, Point ps, Set<Point> globalOccupied) {
        for (Point pk : pt.keySet()) {
            String vp = topLabels.get(pk);
            if (pt.get(pk).size() != 1 || pk.equals(ps) || vp == null || !vp.equals(RoomIds.DEAD_END_DJ)) continue;
            Point np = pt.get(pk).iterator().next();
            int dx = pk.x() - np.x(), dy = pk.y() - np.y();
            int d = dx == 1 ? 0 : dx == -1 ? 2 : dy == 1 ? 1 : 3;

            Point p2 = pk.move(dx, dy);
            int r1 = (d + 1) % 4; Point p3 = p2.move(DIR_OFFSET[r1]);
            int r2 = (r1 + 1) % 4; Point p4 = p3.move(DIR_OFFSET[r2]);

            if (p2.isOutOfBounds() || p3.isOutOfBounds() || p4.isOutOfBounds()) continue;
            if (globalOccupied.contains(p2) || globalOccupied.contains(p3) || globalOccupied.contains(p4)) continue;

            topLabels.put(pk, RoomIds.PRISON_C1);
            Point[] prisonCells = {p2, p3, p4};
            String[] prisonLabels = {RoomIds.PRISON_C2, RoomIds.PRISON_C3, RoomIds.PRISON_C4};
            Point pv2 = pk;
            for (int pi = 0; pi < prisonCells.length; pi++) {
                globalOccupied.add(prisonCells[pi]);
                adj.put(prisonCells[pi], new HashSet<>());
                adj.get(pv2).add(prisonCells[pi]); adj.get(prisonCells[pi]).add(pv2);
                topLabels.put(prisonCells[pi], prisonLabels[pi]); pv2 = prisonCells[pi];
            }
            break;
        }
    }

    // ===================== Algorithm: generatePart4Tree =====================

    private static boolean generatePart4Tree(Map<Point, Set<Point>> adj,
                                              Map<Point, String> topLabels,
                                              int hx, int hz, String missingLootType, Random rng) {
        Point[] p4Exits = {
            new Point(hx, hz + 2),
            new Point(hx + 1, hz + 2),
            new Point(hx - 1, hz),
            new Point(hx + 2, hz),
            new Point(hx + 1, hz - 1)
        };
        List<Point> p4List = new ArrayList<>(Arrays.asList(p4Exits));
        Collections.shuffle(p4List, rng);

        Set<Point> globalOccupied = new HashSet<>();
        for (int x = hx; x <= hx + 1; x++) {
            for (int z = hz; z <= hz + 1; z++) {
                Point hp = new Point(x, z);
                globalOccupied.add(hp);
                adj.putIfAbsent(hp, new HashSet<>());
            }
        }

        List<Point> cjKeys = new ArrayList<>(), exitKeys = new ArrayList<>();
        List<int[]> cjDirs = new ArrayList<>();
        for (Point pp : p4List) {
            int adx = 0, ady = 0; Point hk = null;
            if (pp.x() == hx && pp.y() == hz+2) { adx = 0; ady = 1; hk = new Point(hx, hz+1); }
            else if (pp.x() == hx+1 && pp.y() == hz+2) { adx = 0; ady = 1; hk = new Point(hx+1, hz+1); }
            else if (pp.x() == hx-1 && pp.y() == hz) { adx = -1; ady = 0; hk = new Point(hx, hz); }
            else if (pp.x() == hx+2 && pp.y() == hz) { adx = 1; ady = 0; hk = new Point(hx+1, hz); }
            else if (pp.x() == hx+1 && pp.y() == hz-1) { adx = 0; ady = -1; hk = new Point(hx+1, hz); }
            if (hk == null) continue;

            Point cjPt = pp.move(adx, ady);
            if (!cjPt.isOutOfBounds() && !globalOccupied.contains(cjPt)) {
                adj.putIfAbsent(pp, new HashSet<>()); adj.putIfAbsent(cjPt, new HashSet<>());
                adj.get(pp).add(hk); adj.get(hk).add(pp);
                adj.get(pp).add(cjPt); adj.get(cjPt).add(pp);
                globalOccupied.add(pp); globalOccupied.add(cjPt);
                cjKeys.add(cjPt); cjDirs.add(new int[]{adx, ady}); exitKeys.add(pp);
            }
        }

        // RÉSERVATION DES CHAÎNES INITIALES : les 2 premiers segments (f1, f2) de chaque
        // arbre sont posés par la croissance SANS vérifier globalOccupied — on les réserve
        // donc AVANT qu'aucun arbre ne pousse. Sinon, un arbre construit tôt (ex. sortie
        // sud-ouest) pouvait coloniser les cellules f1/f2 de l'arbre adjacent (sortie
        // sud-est) pas encore bâti → cellules partagées par deux arbres, voisinages
        // fusionnés au merge addAll, et labels calculés sur la vue PARTIELLE d'un seul
        // arbre (les fameux faux virages à 3-4 connexions, toujours au sud).
        Map<Point, List<Point>> reservedChains = new HashMap<>();
        for (int ti = 0; ti < cjKeys.size(); ti++) {
            Point cp = cjKeys.get(ti);
            int rdx = cjDirs.get(ti)[0], rdy = cjDirs.get(ti)[1];
            List<Point> chain = new ArrayList<>();
            Point f1 = cp.move(rdx, rdy);
            if (!f1.isOutOfBounds() && !globalOccupied.contains(f1)) {
                chain.add(f1);
                globalOccupied.add(f1);
                Point f2 = f1.move(rdx, rdy);
                if (!f2.isOutOfBounds() && !globalOccupied.contains(f2)) {
                    chain.add(f2);
                    globalOccupied.add(f2);
                }
            }
            reservedChains.put(cp, chain);
        }

        List<Map<Point, Set<Point>>> allTrees = new ArrayList<>();
        List<Point> allStarts = new ArrayList<>();
        for (int ti = 0; ti < 5 && ti < cjKeys.size(); ti++) {
            Point startPoint = cjKeys.get(ti);
            int adx = cjDirs.get(ti)[0], ady = cjDirs.get(ti)[1];
            Map<Point, Set<Point>> tr = new HashMap<>();
            tr.put(startPoint, new HashSet<>());
            globalOccupied.add(startPoint);
            int ci3 = 0, ci4 = 0, target = 13 + rng.nextInt(5);

            // Chaîne initiale f1/f2 : déjà réservée (voir plus haut) → garantie libre,
            // sans vérification d'occupation ici (c'est tout l'intérêt de la réservation).
            List<Point> chain = reservedChains.getOrDefault(startPoint, List.of());
            Point prev = startPoint;
            for (Point f : chain) {
                tr.put(f, new HashSet<>());
                tr.get(prev).add(f); tr.get(f).add(prev);
                prev = f;
            }

            while (tr.size() < target) {
                List<Object[]> cands = new ArrayList<>();
                for (Point p : tr.keySet()) {
                    int d = tr.get(p).size(); if (d >= 4) continue;
                    if (d == 2 && ci3 >= P4_MAX_IJ3) continue;
                    if (d == 3 && ci4 >= P4_MAX_IJ4) continue;

                    for (int[] dir : DIR_OFFSET) {
                        Point next = p.move(dir);
                        if (next.isOutOfBounds()) continue;
                        if (!globalOccupied.contains(next)) {
                            // Limite géométrique : max MAX_COLINEAR_RUN segments alignés
                            // dans l'adj de l'arbre (I3 comptés dans l'axe)
                            if (colinearRunAfterEdge(p, next, tr) > MAX_COLINEAR_RUN) continue;
                            if (d >= 2) {
                                boolean sk = false;
                                for (Point m1 : tr.get(p)) {
                                    for (Point m2 : tr.get(p)) {
                                        if (m1.equals(m2)) continue;
                                        if (p.x() - m1.x() == m2.x() - p.x() && p.y() - m1.y() == m2.y() - p.y()) {
                                            Set<Point> a1 = tr.get(m1), a2 = tr.get(m2);
                                            if ((a1 != null && a1.size() >= 3) || (a2 != null && a2.size() >= 3)) { sk = true; break; }
                                        }
                                    }
                                    if (sk) break;
                                }
                                if (sk) continue;
                            }
                            if (d == 1) {
                                Point op = tr.get(p).iterator().next();
                                if (p.x() - op.x() == next.x() - p.x() && p.y() - op.y() == next.y() - p.y()) {
                                    Point bk = op.move(-(p.x() - op.x()), -(p.y() - op.y()));
                                    if (tr.containsKey(bk) && tr.get(bk).contains(op)) continue;
                                }
                            }
                            cands.add(new Object[]{p, next, dir});
                        }
                    }
                }
                if (cands.isEmpty()) break;
                List<Object[]> w = new ArrayList<>();
                for (Object[] c : cands) {
                    int[] cd = (int[]) c[2];
                    int wt = (cd[0] == adx && cd[1] == ady) ? 20 : 1;
                    for (int i = 0; i < wt; i++) w.add(c);
                }
                if (w.isEmpty()) break;
                Object[] ch = w.get(rng.nextInt(w.size()));
                Point src = (Point) ch[0], dst = (Point) ch[1];
                globalOccupied.add(dst); tr.put(dst, new HashSet<>());
                tr.get(src).add(dst); tr.get(dst).add(src);
                int nd = tr.get(src).size(); if (nd == 3) ci3++; else if (nd == 4) ci4++;
            }
            if (tr.size() < 6) continue;
            allTrees.add(tr); allStarts.add(startPoint);

            labelTreeNodes(tr, topLabels, startPoint, rng);

            for (var e : tr.entrySet()) {
                adj.putIfAbsent(e.getKey(), new HashSet<>());
                adj.get(e.getKey()).addAll(e.getValue());
            }
        }

        // Garde anti-fusion : aucune cellule ne doit appartenir à deux arbres distincts
        // (impossible depuis la réservation des chaînes ; si un futur changement la
        // réintroduisait, on préfère un retry propre à un graphe fusionné silencieux).
        Set<Point> seenTreeCells = new HashSet<>();
        for (Map<Point, Set<Point>> t : allTrees) {
            for (Point k : t.keySet()) {
                if (!seenTreeCells.add(k)) return false;
            }
        }

        // Rattrapage : les exits et racines d'arbres voient leur degré réel dans 'adj'
        // (le hub et l'arête racine ne sont pas dans les maps d'arbres locales).
        for (Point ek : exitKeys) {
            if (topLabels.containsKey(ek)) continue;
            Set<Point> skn = adj.get(ek); if (skn == null) continue;
            topLabels.put(ek, labelForNeighbors(skn, Theme.DJ, rng));
        }

        for (Point cjk : cjKeys) {
            Set<Point> cn = adj.get(cjk); if (cn == null) continue;
            topLabels.put(cjk, labelForNeighbors(cn, Theme.DJ, rng));
        }

        if (allTrees.size() < 5) return false;

        List<Integer> idxs = new ArrayList<>(Arrays.asList(0, 1, 2, 3, 4));
        Collections.shuffle(idxs, rng);
        int gIdx = idxs.get(0), cIdx = idxs.get(1), pIdx = idxs.get(2);
        Set<Point> goblinCells = new HashSet<>();

        if (!placeGoblinVillage(adj, topLabels, allTrees.get(gIdx), globalOccupied, goblinCells, rng)) return false;
        placeChapelAndCrypt(adj, topLabels, allTrees.get(cIdx), allStarts.get(cIdx), globalOccupied, rng);
        placePrisonBlock(adj, topLabels, allTrees.get(pIdx), allStarts.get(pIdx), globalOccupied);

        // PASSE DE COHÉRENCE FINALE : la topologie P4 est désormais figée (arbres, exits,
        // village gobelin, chapelle, prison). On re-déduit chaque label structurel générique
        // (CJ/IJ/culDJ + CG/GI/CDG) de l'adjacence RÉELLE. Corrige notamment les nœuds classés
        // "virage" qui ont gagné un 3e voisin via une greffe post-classification.
        reclassifyGeneric(topLabels, adj, rng);

        java.util.function.Predicate<Point> isHubExit = kp -> 
            (kp.x() == hx-1 && kp.y() == hz) || (kp.x() == hx+2 && kp.y() == hz) || 
            (kp.x() == hx+1 && kp.y() == hz-1) || (kp.x() == hx && kp.y() == hz+2) || 
            (kp.x() == hx+1 && kp.y() == hz+2);

        int mjT = 5 + rng.nextInt(6);
        Map<Point, Integer> treeForNode = new HashMap<>();
        for (int ti = 0; ti < allTrees.size(); ti++) for (Point k : allTrees.get(ti).keySet()) treeForNode.put(k, ti);
        Set<Point> mjPlacedKeys = new HashSet<>();
        Map<Integer, Set<String>> mjTypesOnTree = new HashMap<>();
        for (int ti = 0; ti < allTrees.size(); ti++) mjTypesOnTree.put(ti, new HashSet<>());

        for (int attempt = 0; attempt < 50 && mjPlacedKeys.size() < mjT; attempt++) {
            Point best = null; String bestType = null;
            for (Point k : topLabels.keySet()) {
                if (mjPlacedKeys.contains(k) || goblinCells.contains(k) || isHubExit.test(k)) continue;
                if (mjPlacedKeys.size() >= mjT) break;
                String v = topLabels.get(k); if (v == null) continue;

                boolean isLeaf = RoomIds.DEAD_END_DJ.equals(v);
                boolean isCorr = v.startsWith("CJ") || v.startsWith("CG");
                if (!isLeaf && !isCorr) continue;

                // ESPACEMENT MONSTRES (règle dure, conversation 3) : distance
                // >= MONSTER_MIN_DIST entre salles MJ de l'étage 1.
                if (!isFarFromAll(adj, k, mjPlacedKeys, MONSTER_MIN_DIST)) continue;

                int ti = treeForNode.getOrDefault(k, -1);
                Set<String> usedOnTree = mjTypesOnTree.getOrDefault(ti, new HashSet<>());
                List<String> pool = isLeaf ? RoomPools.LEAF_MONSTERS_P3_P4 : RoomPools.CORRIDOR_MONSTERS_P3_P4;
                String availType = null;
                for (String t : pool) if (!usedOnTree.contains(t)) { availType = t; break; }
                if (availType == null) continue;
                best = k; bestType = availType; break;
            }
            if (best != null) {
                topLabels.put(best, bestType);
                mjPlacedKeys.add(best);
                int ti = treeForNode.getOrDefault(best, -1);
                if (ti >= 0) mjTypesOnTree.get(ti).add(bestType);
            }
        }

        for (var e : new ArrayList<>(topLabels.entrySet())) {
            if (e.getValue() != null && e.getValue().equals(RoomIds.DEAD_END_DJ) && !isHubExit.test(e.getKey())) {
                topLabels.put(e.getKey(), RoomIds.BLACK_MARKET); break;
            }
        }

        if (missingLootType != null) {
            boolean lootCorr = missingLootType.equals("Lootdj2");
            Point lk = null;
            for (var e : topLabels.entrySet()) {
                String v = e.getValue(); Point k = e.getKey(); 
                if (v == null || goblinCells.contains(k) || isHubExit.test(k)) continue;
                if (lootCorr && (v.startsWith("CJ") || v.startsWith("CG"))) { lk = k; break; }
                if (!lootCorr && v.equals(RoomIds.DEAD_END_DJ)) { lk = k; break; }
            }
            if (lk != null) topLabels.put(lk, missingLootType);
        }

        Point puitDJKey = null;
        for (var e : topLabels.entrySet()) {
            String v = e.getValue(); Point k = e.getKey(); 
            if (v == null || goblinCells.contains(k) || isHubExit.test(k)) continue;
            if (v.startsWith("CJ") || v.startsWith("CG")) { puitDJKey = k; break; }
        }
        if (puitDJKey != null) topLabels.put(puitDJKey, RoomIds.WELL_DJ);

        // Règle cul-de-sac étage 1 (conversation 3) : pas de culDJ/CDG au bout d'une
        // ligne droite. Échec => rejet (false => retry amont, 15 tentatives).
        if (!enforceDeadEndAfterTurn(topLabels, adj, rng)) return false;

        // Validation finale
        boolean hc1 = topLabels.containsValue(RoomIds.CHAPEL_1) && topLabels.containsValue(RoomIds.CRYPT_1);
        boolean hpr = topLabels.values().stream().anyMatch(v -> v != null && v.startsWith("PrisonC"));
        boolean hpg = topLabels.containsValue(RoomIds.GOBLIN_DOOR);
        boolean hmn = topLabels.containsValue(RoomIds.BLACK_MARKET);
        boolean hlt = topLabels.containsValue("Lootdj1") || topLabels.containsValue("Lootdj2") || topLabels.containsValue("Lootdj3");
        boolean hPuitDJ = topLabels.containsValue(RoomIds.WELL_DJ);
        int gbc = (int) topLabels.values().stream().filter(v -> v != null && (v.equals(RoomIds.GOBLIN_WELL) || v.equals(RoomIds.GOBLIN_MARCH) || v.equals(RoomIds.GOBLIN_ARMORY) || v.equals(RoomIds.GOBLIN_TREASURE))).count();
        boolean hmg = topLabels.values().stream().anyMatch(v -> v != null && v.startsWith("MG"));

        return hc1 && hpr && hpg && hmn && hlt && hPuitDJ && hmg && gbc >= 2 && mjPlacedKeys.size() >= 5;
    }

    /**
     * P2 = arbre à tronc + branches (comme P3).
     * La limite de "droite" porte sur l'ADJACENCE colinéaire
     * ({@link #PART2_MAX_COLINEAR_RUN} segments alignés max), pas sur les labels.
     * Fin de tronc réservée : 1-2 C/I2 (deg2 only) → porte2 → camp (plus de M5 ici).
     */
    private static TreeResult generatePart2Tree(Point startPoint, Set<Point> blocked, Random rng) {
        return generatePart2TrunkTree(startPoint, blocked, rng);
    }

    /**
     * Longueur d'une run COLINÉAIRE dans l'adjacence RÉELLE le long de l'axe (dx,dy)
     * passant par {@code origin} (nombre de SEGMENTS = arêtes alignées).
     * <p>
     * Traverse aussi les intersections I3/I4 tant qu'elles ont un voisin dans l'axe
     * (passage "tout droit" géométrique). Les labels sont ignorés : seule l'adj compte.
     * Ex. I3—C—C—C—I2 alignés = 4 segments → refusé si max=3.
     */
    private static int colinearRunInAdj(Point origin, int dx, int dy, Map<Point, Set<Point>> adj) {
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
     * Run colinéaire qui résulterait de l'ajout de l'arête {@code from}→{@code to}
     * (to pas encore dans adj, ou déjà lié). = segments déjà présents en arrière
     * depuis from dans l'axe + 1 (nouvelle arête) + éventuelle continuation depuis to.
     */
    private static int colinearRunAfterEdge(Point from, Point to, Map<Point, Set<Point>> adj) {
        int dx = to.x() - from.x();
        int dy = to.y() - from.y();
        if (Math.abs(dx) + Math.abs(dy) != 1) return Integer.MAX_VALUE;
        // Segments déjà derrière `from` dans la direction opposée
        int back = 0;
        Point cur = from;
        while (back < 16) {
            Point prev = cur.move(-dx, -dy);
            if (!adj.getOrDefault(cur, Set.of()).contains(prev)) break;
            back++;
            cur = prev;
        }
        // Segments déjà devant `to` dans la même direction (si to déjà connecté)
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

    private static TreeResult generatePart2TrunkTree(Point startPt, Set<Point> blocked, Random rng) {
        Map<Point, Set<Point>> adj = new HashMap<>();
        Set<Point> occupied = new HashSet<>(blocked != null ? blocked : Set.of());

        Point start = startPt != null ? startPt : new Point(GRID_SIZE / 2, GRID_SIZE / 2);
        adj.put(start, new HashSet<>());
        occupied.add(start);

        List<int[]> dirs = new ArrayList<>(Arrays.asList(DIR_OFFSET));
        Collections.shuffle(dirs, rng);
        int dir = -1;
        for (int[] d : dirs) {
            Point target = start.move(d);
            if (!target.isOutOfBounds() && !occupied.contains(target)) {
                for (int i = 0; i < 4; i++) {
                    if (DIR_OFFSET[i][0] == d[0] && DIR_OFFSET[i][1] == d[1]) { dir = i; break; }
                }
                break;
            }
        }
        if (dir < 0) {
            TreeResult tr = new TreeResult();
            tr.startPoint = start; tr.startKey = start.key();
            tr.startX = start.x(); tr.startY = start.y(); tr.adj = adj;
            return tr;
        }

        Point c = start.move(DIR_OFFSET[dir]);
        addEdge(adj, occupied, start, c);
        List<Point> trunkCells = new ArrayList<>();
        trunkCells.add(c);

        int trunkTarget = PART2_TRUNK_MIN + rng.nextInt(PART2_TRUNK_MAX - PART2_TRUNK_MIN + 1);

        for (int t = 1; t < trunkTarget; t++) {
            // Run colinéaire dans l'ADJ si on continue dans `dir` (traverse I3/I4)
            int runIfStraight = colinearRunAfterEdge(c, c.move(DIR_OFFSET[dir]), adj);
            boolean forceTurn = runIfStraight > PART2_MAX_COLINEAR_RUN;
            boolean maybeTurn = runIfStraight >= 2 && rng.nextFloat() < 0.40f;
            if (forceTurn || maybeTurn) {
                dir = (dir + (rng.nextBoolean() ? 1 : 3)) % 4;
            }

            Point n = c.move(DIR_OFFSET[dir]);
            if (n.isOutOfBounds() || occupied.contains(n)
                    || colinearRunAfterEdge(c, n, adj) > PART2_MAX_COLINEAR_RUN) {
                int od = dir;
                boolean found = false;
                for (int a = 0; a < 4; a++) {
                    int td = (od + a) % 4;
                    Point cand = c.move(DIR_OFFSET[td]);
                    if (cand.isOutOfBounds() || occupied.contains(cand)) continue;
                    if (colinearRunAfterEdge(c, cand, adj) > PART2_MAX_COLINEAR_RUN) continue;
                    dir = td; n = cand; found = true; break;
                }
                if (!found) break;
            }

            addEdge(adj, occupied, c, n);
            trunkCells.add(n);
            c = n;

            // Branches latérales (pas sur la toute fin : réservée pour l'approche de porte2)
            // growMiniTree peut allonger une droite via une I3 : on borne après coup
            if (t < trunkTarget - 3 && rng.nextFloat() < 0.55f) {
                int pDir = (dir + (rng.nextBoolean() ? 1 : 3)) % 4;
                growMiniTreeBounded(n, pDir, adj, occupied, rng);
            }
        }

        Point trunkEnd = trunkCells.isEmpty() ? start : trunkCells.get(trunkCells.size() - 1);
        if (!trunkCells.isEmpty()) {
            int side = (dir + (rng.nextBoolean() ? 1 : 3)) % 4;
            growMiniTreeBounded(trunkEnd, side, adj, occupied, rng);
        }

        // Remplissage : refuse toute arête qui créerait > MAX segments colinéaires
        // (compte dans l'adj réelle, y compris à travers les intersections)
        int targetSize = PART2_TARGET_MIN + rng.nextInt(PART2_TARGET_MAX - PART2_TARGET_MIN + 1);
        int ci3 = 0, ci4 = 0;
        for (Set<Point> nb : adj.values()) {
            int d = nb.size();
            if (d == 3) ci3++; else if (d == 4) ci4++;
        }
        while (adj.size() < targetSize) {
            List<Point[]> candidates = new ArrayList<>();
            for (Point node : adj.keySet()) {
                if (node.equals(start) || node.equals(trunkEnd)) continue;
                int deg = adj.get(node).size();
                if (deg >= 4) continue;
                if (deg == 2 && ci3 >= PART2_MAX_I3) continue;
                if (deg == 3 && ci4 >= 1) continue;
                for (int[] d : DIR_OFFSET) {
                    Point next = node.move(d);
                    if (next.isOutOfBounds() || occupied.contains(next)) continue;
                    if (colinearRunAfterEdge(node, next, adj) > PART2_MAX_COLINEAR_RUN) continue;
                    int w = trunkCells.contains(node) ? 2 : 1;
                    for (int wi = 0; wi < w; wi++) candidates.add(new Point[]{node, next});
                }
            }
            if (candidates.isEmpty()) break;
            Point[] choice = candidates.get(rng.nextInt(candidates.size()));
            addEdge(adj, occupied, choice[0], choice[1]);
            int nd = adj.get(choice[0]).size();
            if (nd == 3) ci3++; else if (nd == 4) ci4++;
        }

        TreeResult tr = new TreeResult();
        tr.startPoint = start; tr.startKey = start.key();
        tr.startX = start.x(); tr.startY = start.y(); tr.adj = adj;
        tr.trunkEnd = trunkEnd;
        tr.trunkEndDir = dir;
        return tr;
    }

    /**
     * Mini-branche latérale qui respecte {@link #PART2_MAX_COLINEAR_RUN}
     * dans l'adjacence (ne prolonge pas une droite au-delà de la limite,
     * même à travers une intersection).
     */
    private static void growMiniTreeBounded(Point root, int pDir, Map<Point, Set<Point>> adj,
                                            Set<Point> occupied, Random rng) {
        Point n = root.move(DIR_OFFSET[pDir]);
        if (n.isOutOfBounds() || occupied.contains(n)) return;
        if (colinearRunAfterEdge(root, n, adj) > PART2_MAX_COLINEAR_RUN) return;

        addEdge(adj, occupied, root, n);
        int cDir = pDir; Point c = n;
        int branchLen = 1 + rng.nextInt(3);

        for (int i = 0; i < branchLen; i++) {
            // Virage si la poursuite droite dépasserait la limite géométrique
            boolean mustTurn = colinearRunAfterEdge(c, c.move(DIR_OFFSET[cDir]), adj) > PART2_MAX_COLINEAR_RUN;
            if (mustTurn || rng.nextFloat() < 0.4f) {
                cDir = (cDir + (rng.nextBoolean() ? 1 : 3)) % 4;
            }
            Point nn = c.move(DIR_OFFSET[cDir]);
            if (nn.isOutOfBounds() || occupied.contains(nn)) break;
            if (colinearRunAfterEdge(c, nn, adj) > PART2_MAX_COLINEAR_RUN) {
                // tenter un virage
                boolean ok = false;
                for (int side : new int[]{1, 3}) {
                    int td = (cDir + side) % 4;
                    Point cand = c.move(DIR_OFFSET[td]);
                    if (cand.isOutOfBounds() || occupied.contains(cand)) continue;
                    if (colinearRunAfterEdge(c, cand, adj) > PART2_MAX_COLINEAR_RUN) continue;
                    cDir = td; nn = cand; ok = true; break;
                }
                if (!ok) break;
            }
            addEdge(adj, occupied, c, nn);
            c = nn;
        }
    }

    /**
     * Appende en FIN de tronc P2 la séquence forcée :
     *   trunkEnd → [1–2 cellules deg2 : C ou I2 uniquement] → porte2
     * Jamais d'intersection (I3/I4).
     * (Historiquement la 1re cellule était une M5 ; la M5 de P2 se pose désormais
     *  n'importe où sur un couloir droit dans analyzePart2, et l'approche de porte2
     *  redevient de simples couloirs.)
     * Retourne la position de porte2, ou null si impossible.
     */
    private static Point appendP2ExitSequence(Map<Point, Set<Point>> adj, Map<Point, String> labels,
                                              Point trunkEnd, int trunkEndDir, Set<Point> blocked,
                                              Random rng) {
        if (trunkEnd == null || trunkEndDir < 0) return null;
        Set<Point> occupied = new HashSet<>(adj.keySet());
        if (blocked != null) occupied.addAll(blocked);

        int dir = trunkEndDir;
        Point cursor = trunkEnd;
        List<Point> chain = new ArrayList<>(); // cellules intermédiaires (sans la porte)
        int gap = 1 + rng.nextInt(2); // 1 ou 2 cellules d'approche avant la porte
        int need = gap; // uniquement le gap, plus de cellule spéciale

        // Pose la chaîne d'approche + porte EN LIGNE (ou avec virages deg2 uniquement).
        // Chaque nœud de la chaîne (sauf la porte leaf) doit finir en deg 2 → C ou I2.
        for (int i = 0; i < need + 1; i++) {
            Point next = null;
            int chosenDir = -1;
            // Pour l'approche, on autorise droit ou virage, mais PAS de 3ᵉ voisin plus tard.
            // On pose d'abord tout droit pour garantir deg2, virage optionnel entre cellules.
            int[] tryOrder;
            if (i == 0) {
                // 1re cellule : continuer dans l'axe du tronc en priorité
                tryOrder = new int[]{dir, (dir + 1) % 4, (dir + 3) % 4, (dir + 2) % 4};
            } else {
                // approche / porte : droit ou virage (I2 ok), jamais forcer un I3
                tryOrder = new int[]{dir, (dir + 1) % 4, (dir + 3) % 4};
            }
            for (int td : tryOrder) {
                Point cand = cursor.move(DIR_OFFSET[td]);
                if (cand.isOutOfBounds() || occupied.contains(cand) || adj.containsKey(cand)) continue;
                // Limite géométrique adj : pas plus de MAX_COLINEAR_RUN segments alignés
                // (I3/I4 sur l'axe comptés dans la droite visuelle)
                if (colinearRunAfterEdge(cursor, cand, adj) > MAX_COLINEAR_RUN) continue;
                next = cand; chosenDir = td; break;
            }
            if (next == null) return null;

            adj.putIfAbsent(cursor, new HashSet<>());
            adj.putIfAbsent(next, new HashSet<>());
            adj.get(cursor).add(next);
            adj.get(next).add(cursor);
            occupied.add(next);
            dir = chosenDir;
            cursor = next;

            if (i < need) {
                chain.add(next);
            } else {
                // Porte2 = leaf (deg 1)
                labels.put(next, RoomIds.DOOR_2);
                // Vérifier géométrie : chaque cellule de la chaîne doit être deg 2
                // (approche = couloir droit ou virage, JAMAIS intersection)
                for (Point gp : chain) {
                    if (adj.getOrDefault(gp, Set.of()).size() != 2) return null;
                }
                // Labels : couloirs génériques C/I2 selon adjacence (plus de M5 ici —
                // la M5 de P2 se pose ailleurs sur une ligne droite, dans analyzePart2).
                for (Point gp : chain) {
                    Shape sh = shapeOf(adj.get(gp));
                    if (sh != Shape.STRAIGHT && sh != Shape.TURN) return null;
                    labels.put(gp, shapeLabel(sh, Theme.P12, rng));
                }
                return next;
            }
        }
        return null;
    }

    /**
     * Place M5 (couloir monstre DROIT) sur le chemin intérieur vers porte1 UNIQUEMENT,
     * avec 1–2 cellules (C ou I2) entre M5 et la porte — jamais côte à côte.
     * Parcours : … → M5 → [1–2 C/I2] → porte → chemin → taverne.
     * (Plus de M5 avant porte2 : le campement garde un chemin basique, et la M5 de P2
     *  est posée sur un couloir droit quelconque de la zone dans analyzePart2.)
     * @return nombre de M5 placés (idéal 1)
     */
    private static int placeMonster5OnDoorPaths(Map<Point, String> labels, Map<Point, Set<Point>> adj,
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

    /** True si un M5 existe déjà à distance 2 ou 3 de la porte (gap 1–2). */
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

    /**
     * Remonte depuis la porte vers le départ : cherche un couloir DROIT
     * à distance 2 ou 3 (donc 1–2 nœuds entre M5 et la porte).
     * Préfère distance 2, sinon 3.
     */
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
            // doit aussi être à distance >= MONSTER_MIN_DIST de toute salle monstre existante.
            if (!isFarFromAll(adj, cand, monsterPoints(labels), MONSTER_MIN_DIST)) continue;
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

    /**
     * Gap M5↔porte : UNIQUEMENT deg 2 (couloir droit C ou virage I2).
     * Jamais d'intersection I3/I4, jamais de salle spéciale.
     */
    private static boolean gapCellsAreCorridors(Point from, Point door, Map<Point, Point> parent,
                                                 Map<Point, String> labels, Map<Point, Set<Point>> adj) {
        Point cur = parent.get(door);
        int guard = 0;
        while (cur != null && !cur.equals(from) && guard++ < 8) {
            Set<Point> nb = adj.getOrDefault(cur, Set.of());
            // STRICT géométrie : deg 2 seulement → C ou I2, jamais intersection
            if (nb.size() != 2) return false;
            Shape sh = shapeOf(nb);
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

    // ===================== Public Main API =====================

    private static long lastSeed = 0;
    private static Map<Point, String> lastTopLabels = null;

    public static long getLastSeed() {
        return lastSeed;
    }

    public static Map<Point, String> getLastTopLabels() {
        return lastTopLabels;
    }

    public static DungeonResult generateDungeon(long seed) {
        int maxAttempts = seed != 0 ? 20 : 100;
        lastTopLabels = null;

        for (int outer = 0; outer < maxAttempts; outer++) {
            // Seed déterministe : chaque tentative (y compris les retries) découle de la seed demandée
            long actualSeed = seed != 0 ? seed + outer : System.nanoTime() + outer;
            Random rng = new Random(actualSeed);

            TreeResult sp1 = null;
            Map<Point, String> labels = null;
            for (int inner = 0; inner < 50; inner++) {
                TreeResult try1 = generatePart1Tree(rng);
                if (try1.adj.size() < 10) continue;
                List<Point> leaves = new ArrayList<>();
                for (var e : try1.adj.entrySet()) if (e.getValue().size() == 1 && !e.getKey().equals(try1.startPoint)) leaves.add(e.getKey());
                if (leaves.size() < 4) continue;
                if (!hasPrisonCandidate(try1.adj, try1.startPoint)) continue;
                Map<Point, String> tryLabels = analyzePart1(try1.startPoint, try1.adj, rng);
                if (tryLabels == null) continue;
                sp1 = try1; labels = tryLabels; break;
            }
            if (sp1 == null) continue;

            Point porteKey = findPointByValue(labels, RoomIds.DOOR_1);
            if (porteKey == null) continue;

            TavernResult tavern = placeTavernAndPath(sp1.adj, porteKey, rng);
            if (tavern == null) continue;
            for (var e : tavern.tavern.entrySet()) labels.put(e.getValue(), e.getKey());

            TreeResult sp2 = generatePart2Tree(tavern.exitPoint, new HashSet<>(sp1.adj.keySet()), rng);
            if (sp2.adj.size() < 5) continue;
            for (var e : sp2.adj.entrySet()) { if (sp1.adj.containsKey(e.getKey())) sp1.adj.get(e.getKey()).addAll(e.getValue()); else sp1.adj.put(e.getKey(), e.getValue()); }

            // Fin de tronc P2 forcée : trunkEnd → [1–2 C/I2] → porte2 (approche simple)
            Point porte2Key = appendP2ExitSequence(sp1.adj, labels, sp2.trunkEnd, sp2.trunkEndDir,
                    null, rng);
            if (porte2Key == null) continue;

            int p1Loot = 0; for (String v : labels.values()) if (v.equals(RoomIds.LOOT_1)) p1Loot++;
            int totalTarget = 1 + rng.nextInt(2);
            if (p1Loot > totalTarget) {
                List<Point> lootNodes = new ArrayList<>();
                for (var e : labels.entrySet()) if (e.getValue().equals(RoomIds.LOOT_1) && !e.getKey().equals(sp1.startPoint)) lootNodes.add(e.getKey());
                Collections.shuffle(lootNodes, rng);
                for (int i = 0; i < p1Loot - totalTarget && i < lootNodes.size(); i++) labels.put(lootNodes.get(i), RoomIds.DEAD_END);
            }

            labels = analyzePart2(sp1.adj, tavern.exitPoint, labels, tavern.pathSet, rng);
            if (labels == null) continue;

            // porte2 déjà posée par appendP2ExitSequence
            if (findPointByValue(labels, RoomIds.DOOR_2) == null) continue;

            CampResult camp = placeCampAndPath(sp1.adj, porte2Key, rng);
            if (camp == null) continue;
            for (Point e : camp.campPathSet) {
                if (labels.containsKey(e)) continue; // ne pas écraser M5 / spéciaux
                Set<Point> nb = sp1.adj.get(e);
                if (nb != null && nb.size() == 2) labels.put(e, labelForNeighbors(nb, Theme.P12, rng));
            }
            for (var e : camp.campNodes.entrySet()) labels.put(e.getValue(), e.getKey());

            TreeResult sp3 = generatePart3Tree(camp.campExit, new HashSet<>(sp1.adj.keySet()), rng);
            if (sp3.adj.size() < 10) continue;
            if (sp3.adj.get(sp3.startPoint).isEmpty()) continue;
            for (var e : sp3.adj.entrySet()) { if (sp1.adj.containsKey(e.getKey())) sp1.adj.get(e.getKey()).addAll(e.getValue()); else sp1.adj.put(e.getKey(), e.getValue()); }

            labels = analyzePart3(sp1.adj, camp.campExit, labels, rng);
            if (labels != null) {
                String missingLoot = null;
                Set<String> p3LootTypes = new HashSet<>();
                for (String v : labels.values()) if (v != null && v.startsWith("Lootdj")) p3LootTypes.add(v);
                for (String lt : RoomPools.LOOT_P3_P4) { if (!p3LootTypes.contains(lt)) { missingLoot = lt; break; } }

                boolean p4Ok = false;
                Map<Point, String> currentTopLabels = null;
                Map<Point, Set<Point>> p4Adj = null;
                Point hubPoint = findPointByValue(labels, RoomIds.CENTRALE);
                if (hubPoint != null) {
                    for (int p4Retry = 0; p4Retry < 15; p4Retry++) {
                        currentTopLabels = new HashMap<>();
                        currentTopLabels.put(hubPoint, RoomIds.CENTRALE); // FIX #1: Inscription explicite de Centrale dans topLabels
                        p4Adj = new HashMap<>();
                        if (generatePart4Tree(p4Adj, currentTopLabels, hubPoint.x(), hubPoint.y(), missingLoot, rng)) {
                            p4Ok = true;
                            break;
                        }
                    }
                }
                if (!p4Ok) continue;

                // M5 "chemin P1" : couloir droit AVANT porte1 avec 1–2 C/I2 d'écart (sans mobs)
                // Parcours : … → M5 → [1–2 C/I2] → porte → chemin taverne
                // (l'autre M5, en P2 sur un couloir droit, est posée dans analyzePart2)
                int m5 = placeMonster5OnDoorPaths(labels, sp1.adj, sp1.startPoint);
                if (m5 < 1) continue; // M5 obligatoire avant porte1 (gap inclus)

                // Garde-fou FINAL : après M5 + chemins taverne/camp + P3, aucune droite
                // géométrique de l'adj (I3/M5/porte/couloirs comptés) ne doit dépasser
                // MAX_COLINEAR_RUN segments. Couvre le cas :
                //   I3—C—I3—M5—C—porte—C—C—virage  → run trop longue → retry
                if (!respectsColinearLimit(sp1.adj)) continue;

                // PASSE DE COHÉRENCE finale pour l'étage 0 : après toutes les mutations P1-P3
                // (chemins taverne/camp, greffes bibliothèque/shop/hub...), tout label structurel
                // générique est re-déduit de l'adjacence réelle. Salles spéciales jamais touchées.
                reclassifyGeneric(labels, sp1.adj, rng);

                DungeonResult dr = new DungeonResult();
                dr.adj = sp1.adj; dr.labels = labels;
                dr.startPoint = sp1.startPoint;
                dr.startKey = sp1.startKey;
                dr.startX = sp1.startX; dr.startY = sp1.startY;
                dr.topLabels = currentTopLabels;
                dr.p4Adj = p4Adj.isEmpty() ? null : p4Adj;
                dr.missingLootType = missingLoot;
                dr.seed = actualSeed;

                lastSeed = actualSeed;
                DungeonAlgo.lastTopLabels = currentTopLabels; // FIX #2: Restauration du getter static getLastTopLabels()

                return dr;
            }
        }
        return null;
    }
}