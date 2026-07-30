package com.dungeonmod.entity.boss.capability;

/**
 * Le boss peut s'enrager sous certaines conditions (typiquement : sous 50 % PV).
 */
public interface BossEnrages extends BossCapability {

    /** true si les conditions d'enragement sont réunies. */
    boolean shouldEnrage();

    /** Appelé une seule fois, au moment où le boss s'enrage. */
    void onEnrage();
}
