package com.dungeonmod.entity;

import com.dungeonmod.entity.boss.BossAnimation;

/**
 * Attaques (et postures) du Cyclope. Les codes d'attaque utilisés par
 * {@code OgreEntity.ATTACK_STATE} restent identiques à l'implémentation
 * historique (1=LANCER, 2=CLAP, 3=HEADBUTT, 4=EYE_HIDDEN, 5=CHARGE)
 * pour ne pas casser la persistance NBT déjà sauvegardée.
 */
public enum CyclopsAttack implements BossAnimation {
    IDLE       ("pose_sans_joueur",             AnimType.LOOP),
    WALK       ("walk",                         AnimType.LOOP),
    WELCOME    ("animation_when_player_going",  AnimType.PLAY),
    DEAD       ("dead",                         AnimType.PLAY),
    LANCER     ("lancer_de_pierre",             AnimType.PLAY),
    CLAP       ("attack",                       AnimType.PLAY),
    HEADBUTT   ("attack_coup_de_tete",          AnimType.PLAY),
    EYE_HIDDEN ("oeil_cache",                   AnimType.PLAY),
    CHARGE     ("charge",                       AnimType.PLAY);

    /** Code stocké dans {@code ATTACK_STATE} (0 = aucune). 0 n'est pas une attaque. */
    public static final int NONE = 0;

    private final String name;
    private final AnimType type;
    CyclopsAttack(String n, AnimType t) { this.name = n; this.type = t; }

    @Override public String getName() { return name; }
    @Override public AnimType getType() { return type; }

    /** Convertit un code entier (legacy) en enum, ou null si 0. */
    public static CyclopsAttack fromCode(int code) {
        if (code <= 0) return null;
        int i = code - 1;
        if (i < 0 || i >= values().length) return null;
        // Le mapping historique : 1=LANCER, 2=CLAP, 3=HEADBUTT, 4=EYE_HIDDEN, 5=CHARGE
        // L'enum est déclaré dans le même ordre, donc on accède par ordinal.
        // Les 5 premières valeurs (IDLE, WALK, WELCOME, DEAD) ne sont pas des attaques
        // → on les ignore ici.
        // Au-dessus de CHARGE, on retourne null (valeur inattendue).
        return switch (code) {
            case 1 -> LANCER;
            case 2 -> CLAP;
            case 3 -> HEADBUTT;
            case 4 -> EYE_HIDDEN;
            case 5 -> CHARGE;
            default -> null;
        };
    }

    /** Code entier (1-based) utilisé par {@code ATTACK_STATE}. */
    public int code() {
        return switch (this) {
            case LANCER     -> 1;
            case CLAP       -> 2;
            case HEADBUTT   -> 3;
            case EYE_HIDDEN -> 4;
            case CHARGE     -> 5;
            default -> 0; // IDLE, WALK, WELCOME, DEAD ne sont pas des "attaques"
        };
    }
}
