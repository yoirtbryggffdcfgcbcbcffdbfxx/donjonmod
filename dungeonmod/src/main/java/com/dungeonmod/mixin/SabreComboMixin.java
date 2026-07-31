package com.dungeonmod.mixin;

import net.minecraft.component.DataComponentTypes;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Items;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * L'ancien hook applyComboOnHit etait un no-op (combo consomme uniquement au
 * clic droit). Ce mixin n'a plus de raison d'etre : la methode vided a ete
 * supprimee. Le mixin est garde vide pour eviter de casser la refmap, mais
 * l'injection est retiree.
 */
@Mixin(PlayerEntity.class)
public class SabreComboMixin {
}
