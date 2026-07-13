package com.dungeonmod.mixin;

import net.minecraft.client.gui.hud.InGameHud;
import net.minecraft.client.network.ClientPlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(InGameHud.class)
public class ArmorHudMixin {

    @Redirect(method = "renderStatusBars", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/network/ClientPlayerEntity;getArmor()I"))
    private int hideArmorBar(ClientPlayerEntity player) {
        return 0;
    }
}
