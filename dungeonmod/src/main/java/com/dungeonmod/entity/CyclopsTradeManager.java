package com.dungeonmod.entity;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.ActionResult;

import java.util.List;

public class CyclopsTradeManager {
    private final OgreEntity ogre;

    public CyclopsTradeManager(OgreEntity ogre) {
        this.ogre = ogre;
    }

    public void startDialogue(PlayerEntity player) {
        // Phase DEAD = le boss est mort (peu importe le deathStage interne).
        // On accepte le dialogue dès la bascule en DEAD, pas besoin d'attendre
        // la fin de la cinétique.
        if (ogre.getPhase() != 4) return;
        if (ogre.dialogueTicks > 0) return;
        if (player instanceof ServerPlayerEntity sp) {
            com.dungeonmod.DungeonMod.npcShopCache.put(sp.getUuid(), ogre.getUuid());
        }

        if (ogre.usedTradeIndices.size() >= 4 || ogre.clothsGiven >= 4) {
            sendSubtitles(player, com.dungeonmod.client.dialogue.CyclopsDialogue.ALL_GIVEN);
            return;
        }

        if (!ogre.hasTalked) {
            ogre.hasTalked = true;
            var lines = new java.util.ArrayList<String>();
            lines.addAll(com.dungeonmod.client.dialogue.CyclopsDialogue.FIRST_MEETING);
            lines.addAll(com.dungeonmod.client.dialogue.CyclopsDialogue.STANDARD_PROMPT);
            sendSubtitles(player, lines);
            return;
        }

        sendSubtitles(player, com.dungeonmod.client.dialogue.CyclopsDialogue.STANDARD_PROMPT);
    }

    public ActionResult openTradeShop(PlayerEntity player) {
        if (!(player instanceof ServerPlayerEntity sp)) return ActionResult.PASS;
        if (ogre.usedTradeIndices.size() >= 4 || ogre.clothsGiven >= 4) {
            sendSubtitles(player, com.dungeonmod.client.dialogue.CyclopsDialogue.ALL_GIVEN);
            return ActionResult.SUCCESS;
        }
        net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking.send(sp, new com.dungeonmod.network.SubtitlePayload("", java.util.List.of(), false));

        var cloth = new ItemStack(com.dungeonmod.DungeonMod.BOUT_TISSU);
        cloth.set(net.minecraft.component.DataComponentTypes.CUSTOM_NAME, net.minecraft.text.Text.literal("§7Bout de tissu"));

        var allTrades = new java.util.ArrayList<com.dungeonmod.network.TradeData>();
        String[] beerIds = {"biere_brune", "biere_viking", "biere_brune", "biere_viking"};
        for (int idx = 0; idx < 4; idx++) {
            if (!ogre.usedTradeIndices.contains(idx)) {
                allTrades.add(new com.dungeonmod.network.TradeData(makeBeer(beerIds[idx]), cloth.copy(), idx));
            }
        }
        var trades = java.util.List.copyOf(allTrades);

        var syncId = sp.openHandledScreen(new net.minecraft.screen.SimpleNamedScreenHandlerFactory(
            (id, inv, p) -> new com.dungeonmod.screen.ShopScreenHandler(id, inv),
            net.minecraft.text.Text.literal("§6Cyclope")
        ));
        syncId.ifPresent(id ->
            net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking.send(sp, new com.dungeonmod.network.TradesPayload(id, "cyclope", "Cyclope", true, false, trades))
        );
        return ActionResult.SUCCESS;
    }

    private ItemStack makeBeer(String id) {
        var custom = com.dungeonmod.ModItems.get(id);
        return custom != null ? custom.createStack() : ItemStack.EMPTY;
    }

    public void processBuyRequest(ServerPlayerEntity player, int originalIndex) {
        if (originalIndex < 0 || originalIndex >= 4) return;
        if (ogre.usedTradeIndices.contains(originalIndex)) return;
        if (ogre.clothsGiven >= 4 || ogre.usedTradeIndices.size() >= 4) return;

        String[] beerIds = {"biere_brune", "biere_viking", "biere_brune", "biere_viking"};
        String[] expectedNames = {"§9Bière périmée", "§9Bière de Viking", "§9Bière périmée", "§9Bière de Viking"};
        String beerId = beerIds[originalIndex];
        String expectedName = expectedNames[originalIndex];
        var beer = makeBeer(beerId);
        if (beer.isEmpty()) return;
        PlayerInventory inv = player.getInventory();
        int slot = -1;
        for (int i = 0; i < inv.size(); i++) {
            var s = inv.getStack(i);
            if (!s.isEmpty()) {
                var cn = s.get(net.minecraft.component.DataComponentTypes.CUSTOM_NAME);
                if (cn != null && cn.getString().equals(expectedName)) {
                    slot = i;
                    break;
                }
            }
        }
        if (slot < 0) return;

        var inStack = inv.getStack(slot);
        inStack.decrement(1);
        if (inStack.isEmpty()) inv.setStack(slot, ItemStack.EMPTY);

        var out = new ItemStack(com.dungeonmod.DungeonMod.BOUT_TISSU);
        out.set(net.minecraft.component.DataComponentTypes.CUSTOM_NAME, net.minecraft.text.Text.literal("§7Bout de tissu"));
        if (!inv.insertStack(out)) player.dropItem(out, false);
        ogre.usedTradeIndices.add(originalIndex);
        ogre.clothsGiven++;
    }

    public void sendSubtitles(PlayerEntity player, List<String> lines) {
        if (player instanceof ServerPlayerEntity sp) {
            // Cooldown anti-spam côté serveur (cf. BaseNpcEntity.damage + dialogueCooldown).
            // 60 ticks = 3 sec, aligné sur Mira/Gaspard. Tant que ce compteur est > 0,
            // OgreEntity.onPostMortemHit() ignore le clic → le même dialogue ne peut
            // pas être re-déclenché plein de fois par un spam de clic gauche.
            ogre.dialogueTicks = 60;
            net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking.send(sp, new com.dungeonmod.network.SubtitlePayload("Cyclope", lines, true));
        }
    }
}
