package com.dungeonmod.mixin;

import com.dungeonmod.DungeonMod;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.s2c.play.GameStateChangeS2CPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerCommonNetworkHandler;
import net.minecraft.server.network.ServerPlayNetworkHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerCommonNetworkHandler.class)
public class NoRespawnBlockMessageMixin {

    @Shadow
    private MinecraftServer server;

    @Inject(method = "sendPacket", at = @At("HEAD"), cancellable = true)
    private void onSendPacket(Packet<?> packet, CallbackInfo ci) {
        if (!(packet instanceof GameStateChangeS2CPacket gameState)) return;
        if (gameState.getReason() != GameStateChangeS2CPacket.NO_RESPAWN_BLOCK) return;
        if (server == null) return;
        for (var p : server.getPlayerManager().getPlayerList()) {
            if (p.networkHandler == (Object)this && DungeonMod.lastAnchorSpawn.containsKey(p.getUuid())) {
                ci.cancel();
                return;
            }
        }
    }
}
