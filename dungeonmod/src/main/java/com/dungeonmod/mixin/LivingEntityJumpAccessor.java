package com.dungeonmod.mixin;

import net.minecraft.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/**
 * Accesseur pour lire l'état de la touche SAUT (espace) côté serveur.
 * Le champ LivingEntity#jumping est synchronisé par le paquet d'input du client.
 */
@Mixin(LivingEntity.class)
public interface LivingEntityJumpAccessor {

    @Accessor("jumping")
    boolean dungeonmod$isJumping();
}
