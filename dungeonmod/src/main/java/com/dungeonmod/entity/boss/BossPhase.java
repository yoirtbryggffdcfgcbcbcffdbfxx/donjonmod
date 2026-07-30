package com.dungeonmod.entity.boss;

/**
 * Phases partagées par tous les boss.
 * <p>Codes conservés identiques à l'implémentation historique d'OgreEntity
 * (0, 1, 2, 4) pour ne pas casser la persistance NBT déjà sauvegardée.
 */
public final class BossPhase {
    public static final int IDLE     = 0; // statue figée face à facingYaw
    public static final int WELCOME  = 1; // intro scénarisée (invulnérable)
    public static final int COMBAT   = 2; // vulnérable, IA active
    public static final int DEAD     = 4; // mort scénarisée → devient inerte
    private BossPhase() {}
}
