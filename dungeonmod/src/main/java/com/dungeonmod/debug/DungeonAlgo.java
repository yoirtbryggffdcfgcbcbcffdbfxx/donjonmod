package com.dungeonmod.debug;

import java.util.*;

public class DungeonAlgo {

    // ===================== Constants =====================

    public static final int[][] DIR_OFFSET = {{1, 0}, {0, 1}, {-1, 0}, {0, -1}};
    public static final int GRID_SIZE = 400;

    // Arbres légèrement agrandis (règles de l'espacement monstres, conversation 3) :
    // plus de place pour respecter >= 2 salles neutres entre salles monstre et
    // l'isolement de l'Ogre sans rejet massif.
    static final int PART1_TARGET_MIN = 24;
    static final int PART1_TARGET_MAX = 30;
    static final int PART1_MAX_I3 = 3;
    static final int PART1_MAX_I4 = 1;
    static final int PART1_STRAIGHT_WEIGHT = 1;

    // 24-30 : 5 feuilles (ogre/fontaine/m3/m1/loot) + 3 couloirs monstre (M2/M4/M5) à caser,
    // avec espacement >= 3 entre tous les monstres et l'Ogre.
    static final int PART2_TARGET_MIN = 24;
    static final int PART2_TARGET_MAX = 30;
    static final int PART2_MAX_I3 = 4;
    static final int PART2_TRUNK_MIN = 8;
    static final int PART2_TRUNK_MAX = 12;
    /**
     * Max de segments COLINÉAIRES d'affilée dans l'ADJACENCE (géométrie pure),
     * pas "nombre de labels C". Compte aussi le passage tout droit à travers
     * une I3/I4. Au-delà → virage forcé.
     * Ex. I3—C—C—I2 alignés = 3 segments → ok ; un 4ᵉ segment droit interdit.
     */
    static final int MAX_COLINEAR_RUN = 3;
    /** @deprecated alias — utiliser {@link #MAX_COLINEAR_RUN} */
    @Deprecated
    static final int PART2_MAX_COLINEAR_RUN = MAX_COLINEAR_RUN;

    static final int PART3_TARGET = 45;
    static final int PART3_MAX_IJ3 = 3;
    static final int PART3_MAX_IJ4 = 1;

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
        return DungeonLabels.shapeOf(neighbors);
    }

    /** Thème visuel d'un étage : P1/P2, donjon (P3/P4) ou village gobelin. */
    public enum Theme { P12, DJ, GOBLIN }

    /** Mapping UNIQUE forme + thème -> identifiant de salle. */
    private static String shapeLabel(Shape shape, Theme theme, Random rng) {
        return DungeonLabels.shapeLabel(shape, theme, rng);
    }

    /** Raccourci : label structurel déduit directement de l'adjacence du nœud. */
    private static String labelForNeighbors(Set<Point> neighbors, Theme theme, Random rng) {
        return DungeonLabels.labelForNeighbors(neighbors, theme, rng);
    }

    /** Vrai si le label structurel correspond exactement à la forme géométrique. */
    public static boolean shapeMatchesLabel(Shape shape, String label) {
        return DungeonLabels.shapeMatchesLabel(shape, label);
    }

    /** Thème d'un label structurel générique, ou null si c'est une salle spéciale. */
    private static Theme genericThemeOf(String label) {
        return DungeonLabels.genericThemeOf(label);
    }

    /**
     * PASSE DE COHÉRENCE FINALE : re-dérive chaque label structurel générique depuis
     * l'adjacence FINALE du graphe. À appeler après toute modification de la topologie
     * ayant suivi une classification. Ne touche JAMAIS aux salles spéciales.
     * Ne consomme le RNG que pour les nœuds réellement corrigés.
     * @return le nombre de labels corrigés.
     */
    private static int reclassifyGeneric(Map<Point, String> labels, Map<Point, Set<Point>> adj, Random rng) {
        return DungeonLabels.reclassifyGeneric(labels, adj, rng);
    }

    /**
     * VALIDATEUR : retourne la liste des incohérences entre labels structurels et
     * adjacence réelle (chaîne vide si tout est cohérent). Outil de debug, sans effet
     * de bord — DungeonViz l'appelle après chaque génération.
     */
    public static List<String> validateStructure(Map<Point, String> labels, Map<Point, Set<Point>> adj, String scope) {
        return DungeonLabels.validateStructure(labels, adj, scope);
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

    static class TreeResult {
        Point startPoint;
        String startKey;
        int startX, startY;
        Map<Point, Set<Point>> adj;
        /** Fin du tronc P2 (pour enchaîner M5 → gap → porte2). */
        Point trunkEnd;
        /** Direction du dernier pas du tronc (index dans DIR_OFFSET), ou -1. */
        int trunkEndDir = -1;
    }

    static class TavernResult {
        Map<String, Point> tavern;
        Point exitPoint;
        Set<Point> pathSet;
    }

    static class CampResult {
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
        return DungeonConstraints.isMonsterLabel(v);
    }

    /** Vrai pour les culs-de-sac GÉNÉRIQUES (jamais les salles spéciales en cul-de-sac). */
    public static boolean isGenericDeadEnd(String v) {
        return DungeonConstraints.isGenericDeadEnd(v);
    }

    /** Tous les points actuellement étiquetés comme salle monstre. */
    private static Set<Point> monsterPoints(Map<Point, String> labels) {
        return DungeonConstraints.monsterPoints(labels);
    }

    /** Distances BFS depuis src (en nombre de salles traversées). */
    public static Map<Point, Integer> bfsDistances(Map<Point, Set<Point>> adj, Point src) {
        return DungeonConstraints.bfsDistances(adj, src);
    }

    /** Vrai si cand est à distance >= minDist de CHAQUE cible (BFS borné à minDist - 1). */
    private static boolean isFarFromAll(Map<Point, Set<Point>> adj, Point cand, Collection<Point> targets, int minDist) {
        return DungeonConstraints.isFarFromAll(adj, cand, targets, minDist);
    }

    /** Premier candidat (liste pré-mélangée => aléatoire) à distance >= minDist des cibles. */
    private static Point firstFar(Map<Point, Set<Point>> adj, List<Point> candidates, Collection<Point> targets, int minDist) {
        return DungeonConstraints.firstFar(adj, candidates, targets, minDist);
    }

    /** Cellule in-bounds et inoccupée par le graphe. */
    private static boolean isFreeCell(Map<Point, Set<Point>> adj, Point p) {
        return DungeonConstraints.isFreeCell(adj, p);
    }

    /** Déplace une feuille cul-de-sac : retire leaf et raccroche target à newParent. */
    private static void moveDeadEnd(Map<Point, String> labels, Map<Point, Set<Point>> adj,
                                    Point leaf, Point oldParent, Point newParent, Point target) {
        DungeonConstraints.moveDeadEnd(labels, adj, leaf, oldParent, newParent, target);
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
        return DungeonConstraints.enforceDeadEndAfterTurn(labels, adj, rng);
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
        return DungeonPart1.hasPrisonCandidate(adj, startPoint);
    }

    private static TreeResult generateRawTree(int targetMin, int targetMax, int maxI3, int maxI4,
                                               int straightWeight, Point startPt, Set<Point> blocked,
                                               Random rng) {
        return DungeonTreeBuilder.generateRawTree(targetMin, targetMax, maxI3, maxI4, straightWeight,
                startPt, blocked, MAX_COLINEAR_RUN, rng);
    }

    private static TreeResult generatePart1Tree(Random rng) {
        return DungeonPart1.generatePart1Tree(rng);
    }

    // ===================== Algorithm: analyzePart1 =====================

    private static Map<Point, String> analyzePart1(Point startPoint, Map<Point, Set<Point>> adj, Random rng) {
        return DungeonPart1.analyzePart1(startPoint, adj, rng);
    }

    // ===================== Algorithm: Tavern =====================

    private static TavernResult placeTavernAndPath(Map<Point, Set<Point>> adj, Point porte, Random rng) {
        return DungeonPart1.placeTavernAndPath(adj, porte, rng);
    }

    // ===================== Algorithm: analyzePart2 =====================

    private static Map<Point, String> analyzePart2(Map<Point, Set<Point>> adj, Point exitPoint,
                                                    Map<Point, String> labels, Set<Point> pathSet, Random rng) {
        return DungeonPart2.analyzePart2(adj, exitPoint, labels, pathSet, rng);
    }

    // ===================== Algorithm: Camp =====================

    private static CampResult placeCampAndPath(Map<Point, Set<Point>> adj, Point porte2, Random rng) {
        return DungeonPart2.placeCampAndPath(adj, porte2, rng);
    }

    // ===================== Part 3 facade =====================

    /**
     * Longueur max d'une run colinéaire dans l'adj (segments = arêtes alignées).
     * Parcourt tous les axes cardinaux depuis chaque nœud.
     * Une droite I3—C—C—I2 = 3 segments ; au-delà de {@link #MAX_COLINEAR_RUN} → invalide.
     */
    private static int maxColinearRunInGraph(Map<Point, Set<Point>> adj) {
        return DungeonConstraints.maxColinearRunInGraph(adj);
    }

    /** True si aucune droite géométrique de l'adj ne dépasse MAX_COLINEAR_RUN. */
    private static boolean respectsColinearLimit(Map<Point, Set<Point>> adj) {
        return DungeonConstraints.respectsColinearLimit(adj, MAX_COLINEAR_RUN);
    }


    private static TreeResult generatePart3Tree(Point startPoint, Set<Point> blocked, Random rng) {
        return DungeonPart3.generatePart3Tree(startPoint, blocked, rng);
    }

    // ===================== Algorithm: analyzePart3 =====================

    private static Map<Point, String> analyzePart3(Map<Point, Set<Point>> adj, Point campExit,
                                                     Map<Point, String> labels, Random rng) {
        return DungeonPart3.analyzePart3(adj, campExit, labels, rng);
    }

    // ===================== Algorithm: generatePart4Tree =====================

    private static boolean generatePart4Tree(Map<Point, Set<Point>> adj,
                                              Map<Point, String> topLabels,
                                              int hx, int hz, String missingLootType, Random rng) {
        return DungeonPart4.generatePart4Tree(adj, topLabels, hx, hz, missingLootType, rng);
    }

    /**
     * P2 = arbre à tronc + branches (comme P3).
     * La limite de "droite" porte sur l'ADJACENCE colinéaire
     * ({@link #PART2_MAX_COLINEAR_RUN} segments alignés max), pas sur les labels.
     * Fin de tronc réservée : 1-2 C/I2 (deg2 only) → porte2 → camp (plus de M5 ici).
     */
    private static TreeResult generatePart2Tree(Point startPoint, Set<Point> blocked, Random rng) {
        return DungeonPart2.generatePart2Tree(startPoint, blocked, rng);
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
        return DungeonConstraints.colinearRunInAdj(origin, dx, dy, adj);
    }

    /**
     * Run colinéaire qui résulterait de l'ajout de l'arête {@code from}→{@code to}
     * (to pas encore dans adj, ou déjà lié). = segments déjà présents en arrière
     * depuis from dans l'axe + 1 (nouvelle arête) + éventuelle continuation depuis to.
     */
    private static int colinearRunAfterEdge(Point from, Point to, Map<Point, Set<Point>> adj) {
        return DungeonConstraints.colinearRunAfterEdge(from, to, adj);
    }

    private static TreeResult generatePart2TrunkTree(Point startPt, Set<Point> blocked, Random rng) {
        return DungeonPart2.generatePart2TrunkTree(startPt, blocked, rng);
    }

    /**
     * Mini-branche latérale qui respecte {@link #PART2_MAX_COLINEAR_RUN}
     * dans l'adjacence (ne prolonge pas une droite au-delà de la limite,
     * même à travers une intersection).
     */
    private static void growMiniTreeBounded(Point root, int pDir, Map<Point, Set<Point>> adj,
                                            Set<Point> occupied, Random rng) {
        DungeonPart2.growMiniTreeBounded(root, pDir, adj, occupied, rng);
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
        return DungeonPart2.appendP2ExitSequence(adj, labels, trunkEnd, trunkEndDir, blocked, rng);
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
        return DungeonPart1.placeMonster5OnDoorPaths(labels, adj, startPoint);
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
