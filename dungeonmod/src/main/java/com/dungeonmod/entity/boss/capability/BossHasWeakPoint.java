package com.dungeonmod.entity.boss.capability;

import net.minecraft.entity.player.PlayerEntity;

/**
 * Le boss possède un point faible (œil du Cyclope, crâne du Cerbère).
 * <p>Déclenché quand le joueur frappe la tête du boss en mêlée ou avec
 * un projectile. L'implémentation décide si oui ou non elle interrompt
 * l'attaque en cours et applique un boost de dégâts.
 *
 * <h2>Exemple d'implémentation (Cyclope)</h2>
 * <pre>{@code
 * public boolean onWeakPointHit(PlayerEntity attacker) {
 *     if (getWorld().isClient()) return false;
 *     long now = age;
 *     if (now - lastWeakPointTime < weakPointCooldownTicks()) return false;
 *     if (!isInInterruptibleWindow()) return false;
 *     if (getAttackState() != 0) { setAttackState(0); animTimer = 0; }
 *     lastWeakPointTime = now;
 *     pendingDamageBoost = 4.0f;     // ×4 sur le prochain coup
 *     pendingDamageReduction = 0.5f;  // -50% pendant l'anim
 *     setAttackState(CyclopsAttack.EYE_HIDDEN.ordinal() + 1);
 *     return true;
 * }
 * }</pre>
 */
public interface BossHasWeakPoint extends BossCapability {

    /**
     * Appelé quand un joueur a frappé le point faible.
     * @param attacker le joueur qui a frappé
     * @return true si le coup a été pris en compte (posture déclenchée)
     */
    boolean onWeakPointHit(PlayerEntity attacker);

    /** Cooldown du point faible en ticks (par défaut 200 = 10 s). */
    default long weakPointCooldownTicks() { return 200; }
}
