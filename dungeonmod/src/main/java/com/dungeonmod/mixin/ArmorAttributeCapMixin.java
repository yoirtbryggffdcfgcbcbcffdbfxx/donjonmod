package com.dungeonmod.mixin;

import net.minecraft.entity.attribute.ClampedEntityAttribute;
import net.minecraft.entity.attribute.EntityAttribute;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.util.math.MathHelper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Vanilla clamp l'attribut {@code generic.armor} à 30 (plafond historique des
 * icônes d'armure). Notre formule donjon est {@code dégâts × (1 - armure/100)}
 * avec un cap à 100 % ({@link DamageUtilMixin}) — sans lever ce clamp vanilla,
 * un set complet (ex. 17+23+23+14 = 77) reste bloqué à 30 % à l'affichage
 * (Dent de loup) ET en réduction réelle.
 */
@Mixin(ClampedEntityAttribute.class)
public class ArmorAttributeCapMixin {

    @Inject(method = "clamp", at = @At("HEAD"), cancellable = true)
    private void dungeonmod$armorCap100(double value, CallbackInfoReturnable<Double> cir) {
        EntityAttribute self = (EntityAttribute) (Object) this;
        if (!isArmorAttribute(self)) return;
        cir.setReturnValue(MathHelper.clamp(value, 0.0, 100.0));
    }

    private static boolean isArmorAttribute(EntityAttribute self) {
        // Identité registry (1.21 : EntityAttributes.ARMOR est un RegistryEntry)
        try {
            if (self == EntityAttributes.ARMOR.value()) return true;
        } catch (Throwable ignored) {
            // fallback ci-dessous
        }
        // Fallback translation key (évite les soucis de mappings)
        String key = self.getTranslationKey();
        return key != null
                && key.contains("armor")
                && !key.contains("toughness");
    }
}
