package com.dungeonmod.entity;

import com.dungeonmod.DungeonMod;
import com.dungeonmod.client.dialogue.GaspardDialogue;
import com.dungeonmod.network.CyclopsTradesPayload;
import com.dungeonmod.network.SubtitlePayload;
import com.dungeonmod.screen.CyclopsTradeScreenHandler;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.SpawnGroup;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.screen.SimpleNamedScreenHandlerFactory;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.world.World;

import java.util.List;

public class GaspardEntity extends BaseNpcEntity implements NpcShopProvider {

    private static final Identifier ID = Identifier.of(DungeonMod.MOD_ID, "gaspard");
    public static final EntityType<GaspardEntity> TYPE = Registry.register(
        Registries.ENTITY_TYPE, ID,
        EntityType.Builder.<GaspardEntity>create(GaspardEntity::new, SpawnGroup.MISC)
            .dimensions(0.6f, 1.95f).maxTrackingRange(64)
            .build(RegistryKey.of(Registries.ENTITY_TYPE.getKey(), ID))
    );

    public GaspardEntity(EntityType<? extends net.minecraft.entity.mob.ZombieEntity> entityType, World world) {
        super(entityType, world);
    }

    @Override public String getNpcName() { return "§6Gaspard"; }
    @Override public List<String> getFirstMeetingLines() { return GaspardDialogue.FIRST_MEETING; }
    @Override public List<String> getStandardPromptLines() { return GaspardDialogue.STANDARD_PROMPT; }

    @Override
    public void openShop(ServerPlayerEntity player) {
        ServerPlayNetworking.send(player, new SubtitlePayload("", List.of(), false));
        var syncId = player.openHandledScreen(new SimpleNamedScreenHandlerFactory(
            (id, inv, p) -> {
                var h = new CyclopsTradeScreenHandler(id, inv, true, "gaspard", "Gaspard");
                h.hasBuyMode = false;
                return h;
            },
            Text.literal("§8Gaspard")
        ));
        syncId.ifPresent(id ->
            ServerPlayNetworking.send(player, new CyclopsTradesPayload(id, "gaspard", "Gaspard", false, true, List.of()))
        );
    }

    @Override
    public void processBuy(ServerPlayerEntity player, int tradeIndex, int quantity) {}

    @Override
    public void processSell(ServerPlayerEntity player, int tradeIndex) {
        var offers = com.dungeonmod.village.SellTradeRegistry.getOffers("gaspard");
        if (tradeIndex < 0 || tradeIndex >= offers.size()) return;
        var offer = offers.get(tradeIndex);
        var inv = player.getInventory();
        for (int i = 0; i < inv.size(); i++) {
            ItemStack s = inv.getStack(i);
            if (!s.isEmpty() && offer.matches(s)) {
                handleConseilTrade(player, s, i);
                break;
            }
        }
    }

    public static void handleConseilTrade(ServerPlayerEntity player, ItemStack soldStack, int slotIndex) {
        var biereV = com.dungeonmod.ModItems.get("biere_viking");
        var biereB = com.dungeonmod.ModItems.get("biere_brune");
        var inv = player.getInventory();
        if (biereV != null && soldStack.isOf(biereV.createStack().getItem()) && soldStack.getName().getString().equals(biereV.createStack().getName().getString())) {
            // Bière viking → consommer, dialogue conseil aléatoire, fermer shop
            if (slotIndex >= 0) {
                soldStack.decrement(1);
                if (soldStack.isEmpty()) inv.setStack(slotIndex, ItemStack.EMPTY);
            }
            java.util.List<String> conseil = com.dungeonmod.client.dialogue.GaspardDialogue.CONSEILS.get(new java.util.Random().nextInt(com.dungeonmod.client.dialogue.GaspardDialogue.CONSEILS.size()));
            net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking.send(player, new com.dungeonmod.network.SubtitlePayload("§6Gaspard", conseil, false));
            player.closeHandledScreen();
        } else if (biereB != null && soldStack.isOf(biereB.createStack().getItem()) && soldStack.getName().getString().equals(biereB.createStack().getName().getString())) {
            // Bière brune → refus, dialogue, rendre bière (si slotIndex < 0: insérer dans l'inventaire), fermer shop
            if (slotIndex < 0) {
                if (!inv.insertStack(soldStack)) {
                    player.dropItem(soldStack, false);
                }
            }
            net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking.send(player, new com.dungeonmod.network.SubtitlePayload("§6Gaspard", com.dungeonmod.client.dialogue.GaspardDialogue.REFUSAL_BIERE_PERIMEE, false));
            player.closeHandledScreen();
        }
    }

}
