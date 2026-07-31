package com.dungeonmod.entity.boss.capability;

/**
 * Le boss peut invoquer des sbires (Mangemorts : squelettes, …).
 * <p>Appelé une fois par cycle. L'implémentation décide de la fréquence
 * et du type de sbire à spawn.
 */
public interface BossSummonsAdds extends BossCapability {

    /** Cooldown entre deux invocations en ticks. */
    int summonCooldownTicks();

    /** Avance d'un tick. */
    void tickSummon();
}
