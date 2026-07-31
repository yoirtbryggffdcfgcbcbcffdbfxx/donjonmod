package com.dungeonmod;

import com.dungeonmod.client.BoomerangEntityRenderer;
import com.dungeonmod.client.DentDeLoupHandler;
import com.dungeonmod.client.SpinCameraHandler;
import com.dungeonmod.client.SubtitleOverlay;
import com.dungeonmod.entity.BoomerangEntity;
import com.dungeonmod.util.SabreComboData;
import dev.kosmx.playerAnim.api.layered.IAnimation;
import dev.kosmx.playerAnim.api.layered.ModifierLayer;
import dev.kosmx.playerAnim.minecraftApi.PlayerAnimationAccess;
import com.dungeonmod.client.SyrinxHandler;
import com.dungeonmod.entity.StoneEntity;
import com.dungeonmod.entity.StoneThrowerGoblinEntity;
import com.dungeonmod.network.SubtitlePayload;
import com.dungeonmod.network.TradesPayload;
import com.dungeonmod.screen.ShopScreen;
import com.dungeonmod.screen.ModScreenHandlers;
import com.dungeonmod.screen.SacScreen;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.ingame.HandledScreens;

public class DungeonModClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        HandledScreens.register(ModScreenHandlers.SAC_SCREEN_HANDLER, SacScreen::new);
        HandledScreens.register(ModScreenHandlers.SHOP_SCREEN_HANDLER, ShopScreen::new);
        EntityRendererRegistry.register(BoomerangEntity.BOOMERANG_TYPE, BoomerangEntityRenderer::new);
        EntityRendererRegistry.register(StoneEntity.STONE_TYPE, ctx -> new net.minecraft.client.render.entity.FlyingItemEntityRenderer<>(ctx, 6.0f, false));
        EntityRendererRegistry.register(StoneEntity.CYCLOPS_STONE_TYPE, ctx -> new net.minecraft.client.render.entity.FlyingItemEntityRenderer<>(ctx, 6.0f, false));
        EntityRendererRegistry.register(StoneThrowerGoblinEntity.THROWER_TYPE, net.minecraft.client.render.entity.ZombieEntityRenderer::new);
        EntityRendererRegistry.register(com.dungeonmod.entity.OgreEntity.TYPE, com.dungeonmod.entity.OgreRenderer::new);
        EntityRendererRegistry.register(com.dungeonmod.entity.GoblinMinibossEntity.TYPE, com.dungeonmod.entity.GoblinMinibossRenderer::new);
        EntityRendererRegistry.register(com.dungeonmod.entity.BarmanEntity.TYPE, com.dungeonmod.entity.BarmanRenderer::new);
        EntityRendererRegistry.register(com.dungeonmod.entity.GaspardEntity.TYPE, com.dungeonmod.entity.GaspardRenderer::new);
        EntityRendererRegistry.register(com.dungeonmod.entity.EliasEntity.TYPE, com.dungeonmod.entity.EliasRenderer::new);



        SpinCameraHandler.init();
        DentDeLoupHandler.init();
        SyrinxHandler.init();
        SubtitleOverlay.init();

        ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> {
            ClientPlayNetworking.registerReceiver(TradesPayload.ID, (payload, context) -> {
                context.client().execute(() -> {
                    var screen = context.client().currentScreen;
                    if (screen instanceof ShopScreen cts && cts.getScreenHandler().syncId == payload.syncId()) {
                        cts.getScreenHandler().npcId = payload.npcId();
                        cts.getScreenHandler().npcName = payload.npcName();
                        cts.getScreenHandler().hasBuyMode = payload.hasBuyMode();
                        cts.getScreenHandler().hasSellMode = payload.hasSellMode();
                        cts.applyNpcConfig();
                        cts.setTrades(payload.trades());
                    }
                });
            });
        });
        ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> {
            ClientPlayNetworking.registerReceiver(SubtitlePayload.ID, (payload, context) -> {
                context.client().execute(() -> {
                    SubtitleOverlay.showSubtitles(payload.speakerName(), payload.lines(), payload.canOpenShop());
                });
            });
        });

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (client.world == null || client.player == null) return;
            if (SabreComboData.animRegistered) return;
            SabreComboData.animRegistered = true;
            var animStack = PlayerAnimationAccess.getPlayerAnimLayer(client.player);
            ModifierLayer<IAnimation> layer = new ModifierLayer<>();
            animStack.addAnimLayer(42, layer);
            SabreComboData.animLayers.put(client.player.getUuid(), layer);
        });

        // Signale au serveur quand la touche ESPACE est pressée/relâchée (cape du voyageur)
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (client.world == null || client.player == null) { capeJumpSent = false; return; }
            boolean pressed = client.options.jumpKey.isPressed();
            if (pressed != capeJumpSent) {
                capeJumpSent = pressed;
                ClientPlayNetworking.send(new com.dungeonmod.network.JumpStatePayload(pressed));
            }
        });
    }

    private static boolean capeJumpSent = false;
}
