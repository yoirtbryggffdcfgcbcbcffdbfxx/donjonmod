package com.dungeonmod.mixin;

import com.dungeonmod.util.SabreComboData;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Items;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PlayerEntity.class)
public class SabreComboMixin {

    @Inject(method = "attack", at = @At("TAIL"))
    private void onAttackTail(Entity target, CallbackInfo ci) {
        if (target == null) return;
        PlayerEntity player = (PlayerEntity)(Object)this;
        if (player.getWorld().isClient()) return;
        var stack = player.getMainHandStack();
        if (stack.isEmpty() || !stack.isOf(Items.IRON_SWORD)) return;
        if (!stack.contains(DataComponentTypes.CUSTOM_NAME)) return;
        if (!stack.get(DataComponentTypes.CUSTOM_NAME).getString().contains("Sabre")) return;
        SabreComboData.applyComboOnHit(player, target);
    }
}
