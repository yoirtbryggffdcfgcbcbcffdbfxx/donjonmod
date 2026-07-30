package com.dungeonmod.entity.boss.capability;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.ActionResult;

/**
 * Le boss, une fois vaincu, devient un PNJ avec lequel on peut interagir
 * (dialogue, troc, …). Implémenté par le Cyclope (PNJ marchand de bières).
 * <p>La base n'invoque jamais ces méthodes directement : c'est l'item
 * d'interaction ou le clic qui dispatche.
 */
public interface BossBecomesNpc extends BossCapability {

    void startDialogue(PlayerEntity player);
    ActionResult openTradeShop(PlayerEntity player);
}
