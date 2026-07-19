package com.dungeonmod;

import com.dungeonmod.client.BoomerangEntityRenderer;
import com.dungeonmod.client.DentDeLoupHandler;
import com.dungeonmod.client.SpinCameraHandler;
import com.dungeonmod.entity.BoomerangEntity;
import com.dungeonmod.util.SabreComboData;
import dev.kosmx.playerAnim.api.layered.IAnimation;
import dev.kosmx.playerAnim.api.layered.ModifierLayer;
import dev.kosmx.playerAnim.minecraftApi.PlayerAnimationAccess;
import com.dungeonmod.client.SyrinxHandler;
import com.dungeonmod.entity.StoneEntity;
import com.dungeonmod.entity.StoneThrowerGoblinEntity;
import com.dungeonmod.screen.ModScreenHandlers;
import com.dungeonmod.screen.SacScreen;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.minecraft.client.gui.screen.ingame.HandledScreens;

public class DungeonModClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        HandledScreens.register(ModScreenHandlers.SAC_SCREEN_HANDLER, SacScreen::new);
        EntityRendererRegistry.register(BoomerangEntity.BOOMERANG_TYPE, BoomerangEntityRenderer::new);
        EntityRendererRegistry.register(StoneEntity.STONE_TYPE, ctx -> new net.minecraft.client.render.entity.FlyingItemEntityRenderer<>(ctx, 6.0f, false));
        EntityRendererRegistry.register(StoneThrowerGoblinEntity.THROWER_TYPE, net.minecraft.client.render.entity.ZombieEntityRenderer::new);
        EntityRendererRegistry.register(com.dungeonmod.entity.OgreEntity.TYPE, com.dungeonmod.entity.OgreRenderer::new);



        SpinCameraHandler.init();
        DentDeLoupHandler.init();
        SyrinxHandler.init();

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (client.world == null || client.player == null) return;
            if (SabreComboData.animRegistered) return;
            SabreComboData.animRegistered = true;
            var animStack = PlayerAnimationAccess.getPlayerAnimLayer(client.player);
            ModifierLayer<IAnimation> layer = new ModifierLayer<>();
            animStack.addAnimLayer(42, layer);
            SabreComboData.animLayers.put(client.player.getUuid(), layer);
        });
    }
}
