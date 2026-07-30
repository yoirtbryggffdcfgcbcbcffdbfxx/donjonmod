package com.dungeonmod.debug;

import com.dungeonmod.debug.DungeonAlgo.Shape;
import com.dungeonmod.debug.DungeonAlgo.Theme;
import java.util.*;

/**
 * Enum type-safe remplacant les constantes String de RoomIds.
 *
 * Chaque type de salle a un id (utilise pour les fichiers NBT / TestGenerator),
 * un Shape attendu (pour les salles generiques derivees de l'adjacence),
 * et un Theme (null pour les salles speciales).
 *
 * Les methodes helper (isMonster, isCorridor, etc.) remplacent les
 * comparaisons String fragiles du code precedent.
 */
public enum RoomType {

    // ===================== P12 Generics =====================
    C1("C1", Shape.STRAIGHT, Theme.P12),
    C2("C2", Shape.STRAIGHT, Theme.P12),
    C3("C3", Shape.STRAIGHT, Theme.P12),
    I2("I2", Shape.TURN, Theme.P12),
    I3("I3", Shape.CROSS_3, Theme.P12),
    I4("I4", Shape.CROSS_4, Theme.P12),
    CUL("cul", Shape.DEAD_END, Theme.P12),

    // ===================== DJ Generics =====================
    CJ1("CJ1", Shape.STRAIGHT, Theme.DJ),
    CJ2("CJ2", Shape.STRAIGHT, Theme.DJ),
    CJ3("CJ3", Shape.STRAIGHT, Theme.DJ),
    IJ2("IJ2", Shape.TURN, Theme.DJ),
    IJ3("IJ3", Shape.CROSS_3, Theme.DJ),
    IJ4("IJ4", Shape.CROSS_4, Theme.DJ),
    CUL_DJ("culDJ", Shape.DEAD_END, Theme.DJ),

    // ===================== Goblin Generics =====================
    CG1("CG1", Shape.STRAIGHT, Theme.GOBLIN),
    GI2("GI2", Shape.TURN, Theme.GOBLIN),
    GI3("GI3", Shape.CROSS_3, Theme.GOBLIN),
    GI4("GI4", Shape.CROSS_4, Theme.GOBLIN),
    CDG("CDG", Shape.DEAD_END, Theme.GOBLIN),

    // ===================== P12 Specials =====================
    START("D"),
    PRISON("Prison"),
    DOOR_1("porte"),
    DOOR_2("porte2"),
    TAVERN_1("T1"),
    TAVERN_2("T2"),
    TAVERN_3("T3"),
    TAVERN_4("T4"),
    LOOT_1("Loot1"),
    MONSTER_1("M1"),
    MONSTER_2("M2"),
    MONSTER_3("M3"),
    MONSTER_4("M4"),
    MONSTER_5("M5"),
    WELL("puit"),
    FOUNTAIN("fontaine"),
    OGRE("Ogre"),
    CAMP_1("Ca1"),
    CAMP_2("Ca2"),
    CAMP_3("Ca3"),
    CAMP_4("Ca4"),
    BIB_1("Bib1"),
    BIB_2("Bib2"),
    SHOP("Shop"),

    // ===================== DJ Specials =====================
    DOOR_3("porte3"),
    MONSTER_DJ_1("MJ1"),
    MONSTER_DJ_2("MJ2"),
    MONSTER_DJ_3("MJ3"),
    MONSTER_DJ_4("MJ4"),
    MONSTER_DJ_5("MJ5"),
    LOOT_DJ_1("Lootdj1"),
    LOOT_DJ_2("Lootdj2"),
    LOOT_DJ_3("Lootdj3"),
    WELL_DJ("PuitDJ"),
    GARDEN("Jardin"),
    STATUE("Statue"),
    CENTRALE("Centrale"),
    BLACK_MARKET("MarchandNoir"),
    CHAPEL_1("Chapelle1"),
    CHAPEL_2("Chapelle2"),
    CRYPT_1("Crypte1"),
    CRYPT_2("Crypte2"),
    PRISON_C1("PrisonC1"),
    PRISON_C2("PrisonC2"),
    PRISON_C3("PrisonC3"),
    PRISON_C4("PrisonC4"),

    // ===================== Goblin Specials =====================
    GOBLIN_DOOR("PorteGob"),
    GOBLIN_WELL("PuitG"),
    GOBLIN_MARCH("MarchG"),
    GOBLIN_TREASURE("TresorG"),
    GOBLIN_ARMORY("ArmG"),
    GOBLIN_HOUSE_1("MG1"),
    GOBLIN_HOUSE_2("MG2"),
    GOBLIN_HOUSE_3("MG3"),
    ;

    // ===================== Fields =====================
    public final String id;
    public final Shape shape;
    public final Theme theme; // null = salle speciale

    RoomType(String id) { this(id, null, null); }
    RoomType(String id, Shape shape, Theme theme) {
        this.id = id;
        this.shape = shape;
        this.theme = theme;
    }

    // ===================== Helpers =====================

    /** Vrai si c'est une salle structurelle generique (derivee de l'adjacence). */
    public boolean isGeneric() { return theme != null; }

    /** Vrai si c'est une salle speciale (imposee, pas derivee de l'adjacence). */
    public boolean isSpecial() { return theme == null; }

    /** Vrai si c'est une salle monstre. */
    public boolean isMonster() {
        return this == MONSTER_1 || this == MONSTER_2 || this == MONSTER_3
            || this == MONSTER_4 || this == MONSTER_5
            || this == MONSTER_DJ_1 || this == MONSTER_DJ_2 || this == MONSTER_DJ_3
            || this == MONSTER_DJ_4 || this == MONSTER_DJ_5;
    }

    /** Vrai si c'est un cul-de-sac generique. */
    public boolean isGenericDeadEnd() {
        return this == CUL || this == CUL_DJ || this == CDG;
    }

    /** Vrai si c'est un couloir droit generique (P12, DJ ou Goblin). */
    public boolean isGenericStraight() {
        return this == C1 || this == C2 || this == C3
            || this == CJ1 || this == CJ2 || this == CJ3
            || this == CG1;
    }

    /** Vrai si c'est un couloir DJ (CJ1/CJ2/CJ3). */
    public boolean isDjCorridor() {
        return this == CJ1 || this == CJ2 || this == CJ3;
    }

    /** Vrai si c'est un couloir Goblin (CG1). */
    public boolean isGoblinCorridor() {
        return this == CG1;
    }

    /** Vrai pour les loots DJ (Lootdj1/2/3). */
    public boolean isDjLoot() {
        return this == LOOT_DJ_1 || this == LOOT_DJ_2 || this == LOOT_DJ_3;
    }

    /** Vrai pour les maisons gobelin. */
    public boolean isGoblinHouse() {
        return this == GOBLIN_HOUSE_1 || this == GOBLIN_HOUSE_2 || this == GOBLIN_HOUSE_3;
    }

    /** Vrai pour les salles de taverne. */
    public boolean isTavern() {
        return this == TAVERN_1 || this == TAVERN_2 || this == TAVERN_3 || this == TAVERN_4;
    }

    /** Vrai pour les salles de camp. */
    public boolean isCamp() {
        return this == CAMP_1 || this == CAMP_2 || this == CAMP_3 || this == CAMP_4;
    }

    /** Vrai pour les salles de prison centrale. */
    public boolean isPrisonCentral() {
        return this == PRISON_C1 || this == PRISON_C2 || this == PRISON_C3 || this == PRISON_C4;
    }

    /** Vrai si le label est une porte. */
    public boolean isDoor() {
        return this == DOOR_1 || this == DOOR_2 || this == DOOR_3 || this == GOBLIN_DOOR;
    }

    // ===================== Pools (ex-RoomPools) =====================

    public static final List<RoomType> CORRIDORS_P1_P2 = List.of(C1, C2, C3);
    public static final List<RoomType> CORRIDORS_P3_P4 = List.of(CJ1, CJ2, CJ3);
    public static final List<RoomType> LEAF_MONSTERS_P3_P4 = List.of(MONSTER_DJ_1, MONSTER_DJ_3, MONSTER_DJ_5);
    public static final List<RoomType> CORRIDOR_MONSTERS_P3_P4 = List.of(MONSTER_DJ_2, MONSTER_DJ_4);
    public static final List<RoomType> LOOT_P3_P4 = List.of(LOOT_DJ_1, LOOT_DJ_2, LOOT_DJ_3);

    /** Tirage aleatoire de C1/C2/C3. */
    public static RoomType pickC(Random rng) { return CORRIDORS_P1_P2.get(rng.nextInt(3)); }
    /** Tirage aleatoire de CJ1/CJ2/CJ3. */
    public static RoomType pickCJ(Random rng) { return CORRIDORS_P3_P4.get(rng.nextInt(3)); }

    // ===================== Shape/Label mapping =====================

    /**
     * Mapping forme + theme -> label generique.
     * @return le RoomType correspondant (jamais null pour les combinaisons valides).
     */
    public static RoomType fromShape(Shape shape, Theme theme, Random rng) {
        return switch (shape) {
            case DEAD_END -> switch (theme) {
                case P12 -> CUL; case DJ -> CUL_DJ; case GOBLIN -> CDG;
            };
            case STRAIGHT -> switch (theme) {
                case P12 -> pickC(rng); case DJ -> pickCJ(rng); case GOBLIN -> CG1;
            };
            case TURN -> switch (theme) {
                case P12 -> I2; case DJ -> IJ2; case GOBLIN -> GI2;
            };
            case CROSS_3 -> switch (theme) {
                case P12 -> I3; case DJ -> IJ3; case GOBLIN -> GI3;
            };
            case CROSS_4 -> switch (theme) {
                case P12 -> I4; case DJ -> IJ4; case GOBLIN -> GI4;
            };
        };
    }

    /**
     * Theme d'un label generique, ou null si c'est une salle speciale.
     */
    public static Theme themeOf(RoomType type) {
        return type != null ? type.theme : null;
    }

    /**
     * Cherche le RoomType par son id (pour compatibilite avec TestGenerator).
     */
    public static RoomType byId(String id) {
        for (RoomType rt : values()) if (rt.id.equals(id)) return rt;
        return null;
    }
}
