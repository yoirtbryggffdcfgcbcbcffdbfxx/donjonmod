package com.dungeonmod.entity.boss;

import software.bernie.geckolib.animation.RawAnimation;

/**
 * Wrapper immutable (String name + RawAnimation) pour une animation GeckoLib
 * identifiée par un nom lisible. Utilisé à la place d'un String brut
 * pour éviter les fautes de frappe et permettre la vérification
 * statique par le compilateur.
 *
 * <h2>Usage</h2>
 * <pre>{@code
 * public enum CyclopsAnim implements BossAnimation {
 *     IDLE       ("pose_sans_joueur", AnimType.LOOP),
 *     WELCOME    ("animation_when_player_going", AnimType.PLAY),
 *     LANCER     ("lancer_de_pierre", AnimType.PLAY),
 *     CLAP       ("attack", AnimType.PLAY),
 *     HEADBUTT   ("attack_coup_de_tete", AnimType.PLAY),
 *     EYE_HIDDEN ("oeil_cache", AnimType.PLAY),
 *     CHARGE     ("charge", AnimType.PLAY),
 *     DEAD       ("dead", AnimType.PLAY);
 *
 *     private final String name;
 *     private final AnimType type;
 *     CyclopsAnim(String n, AnimType t) { this.name = n; this.type = t; }
 *
 *     public String getName() { return name; }
 *     public AnimType getType() { return type; }
 *
 *     public RawAnimation toRaw() {
 *         return switch (type) {
 *             case LOOP -> RawAnimation.begin().thenLoop(name);
 *             case PLAY -> RawAnimation.begin().thenPlay(name);
 *         };
 *     }
 * }
 * }</pre>
 */
public interface BossAnimation {
    enum AnimType { LOOP, PLAY }
    String getName();
    AnimType getType();

    default RawAnimation toRaw() {
        return switch (getType()) {
            case LOOP -> RawAnimation.begin().thenLoop(getName());
            case PLAY -> RawAnimation.begin().thenPlay(getName());
        };
    }
}
