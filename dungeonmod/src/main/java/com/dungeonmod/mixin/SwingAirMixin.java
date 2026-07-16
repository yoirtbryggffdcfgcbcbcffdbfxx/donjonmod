package com.dungeonmod.mixin;

import com.dungeonmod.util.AncreGrappling;
import com.dungeonmod.util.FauxDeFerHelper;
import net.minecraft.network.packet.c2s.play.HandSwingC2SPacket;
import net.minecraft.server.network.ServerPlayNetworkHandler;
import net.minecraft.server.network.ServerPlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerPlayNetworkHandler.class)
public class SwingAirMixin {

    @Shadow
    public ServerPlayerEntity player;

    @Inject(method = "onHandSwing", at = @At("HEAD"))
    private void onHandSwing(HandSwingC2SPacket packet, CallbackInfo ci) {
        if (AncreGrappling.isAncreGrappling(player)) {
            AncreGrappling.onSwing(player);
            return;
        }

        var stack = player.getMainHandStack();
        if (!FauxDeFerHelper.isFauxDeFer(stack)) return;

        FauxDeFerHelper.doFauxAreaDamage(player);
        player.resetLastAttackedTicks();
    }
}
