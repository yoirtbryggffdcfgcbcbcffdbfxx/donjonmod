package com.dungeonmod.entity.boss.capability;

import net.minecraft.entity.LivingEntity;

/**
 * Le boss enchaîne des attaques en combo, choisi en fonction de la distance
 * à la cible. Implémenté par le Cyclope (3 combos : TC / TH / CH).
 * <p>Un boss qui n'implémente pas cette capability n'a pas de système
 * de combo : son AI goal déclenche des attaques uniques directement.
 *
 * @param <A> le type enum des attaques du boss (ex. {@code CyclopsAttack})
 */
public interface BossHasCombos<A extends Enum<A>> extends BossCapability {

    /**
     * Choisit un combo en fonction de la distance² à la cible.
     * @param target la cible (vivante)
     * @param distSq distance² à la cible
     * @return un tableau d'attaques ordonnées, ou null pour "pas d'attaque"
     */
    A[] pickCombo(LivingEntity target, double distSq);

    /** Appelé à chaque tick de combat pour faire avancer l'anim du combo. */
    void tickCombos();
}
