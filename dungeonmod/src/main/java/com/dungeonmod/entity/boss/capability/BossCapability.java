package com.dungeonmod.entity.boss.capability;

/**
 * Marqueur pour toutes les capabilities de boss.
 * <p>Une capability est un comportement <i>optionnel</i> qu'un boss peut
 * implémenter (ou non). La base {@link com.dungeonmod.entity.boss.BossEntity}
 * dispatche vers les capabilities via {@code instanceof} dans ses hooks
 * (par exemple {@code tickCombat()} appelle {@code tickWeakPoint()} si
 * le boss implémente {@link BossHasWeakPoint}).
 *
 * <h2>Pourquoi des interfaces et pas de l'héritage multiple ?</h2>
 * <ul>
 *   <li>Chaque boss reste libre de composer ses mécaniques</li>
 *   <li>Ajouter une nouvelle capability n'oblige pas à toucher aux boss existants</li>
 *   <li>Pas d'explosion combinatoire</li>
 * </ul>
 */
public interface BossCapability { }
