package com.dungeonmod.entity.boss.capability;

/**
 * Le boss possède une séquence de mort scénarisée en plusieurs étapes
 * (Cyclope : marche au centre → freeze → permanent IDLE).
 * <p>La base {@link com.dungeonmod.entity.boss.BossEntity#tickDeath()}
 * appelle {@link #tickDeathSequence()} tant que {@link #isDeathSequenceDone()}
 * vaut false. À la fin, le boss doit set {@code isDeadPermanent = true}.
 */
public interface BossHasDeathSequence extends BossCapability {

    /** Avance d'un tick dans la séquence. */
    void tickDeathSequence();

    /** true si la séquence est terminée. */
    boolean isDeathSequenceDone();
}
