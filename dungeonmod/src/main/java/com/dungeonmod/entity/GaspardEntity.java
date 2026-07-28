package com.dungeonmod.entity;

import com.dungeonmod.DungeonMod;
import com.dungeonmod.client.dialogue.GaspardDialogue;
import com.dungeonmod.network.TradesPayload;
import com.dungeonmod.network.SubtitlePayload;
import com.dungeonmod.screen.ShopScreenHandler;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.entity.player.PlayerEntity;
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
import java.util.Set;

public class GaspardEntity extends BaseNpcEntity implements NpcShopProvider {
    private final Set<Integer> givenConseils = new java.util.HashSet<>();
    private static final java.util.Random RANDOM = new java.util.Random();

    private static final Identifier ID = Identifier.of(DungeonMod.MOD_ID, "gaspard");
    public static final EntityType<GaspardEntity> TYPE = EntityType.Builder.<GaspardEntity>create(GaspardEntity::new, SpawnGroup.MISC)
        .dimensions(0.6f, 1.95f).maxTrackingRange(64)
        .build(RegistryKey.of(Registries.ENTITY_TYPE.getKey(), ID));

    public static void register() {
        Registry.register(Registries.ENTITY_TYPE, ID, TYPE);
    }

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
                var h = new ShopScreenHandler(id, inv, true, "gaspard", "Gaspard");
                h.hasBuyMode = false;
                return h;
            },
            Text.literal("§8Gaspard")
        ));
        syncId.ifPresent(id ->
            ServerPlayNetworking.send(player, new TradesPayload(id, "gaspard", "Gaspard", false, true, List.of()))
        );
    }

    @Override
    protected void startDialogue(PlayerEntity player) {
        if (!(player instanceof ServerPlayerEntity sp)) return;
        // Check if all conseils have been given
        if (givenConseils.size() >= com.dungeonmod.client.dialogue.GaspardDialogue.CONSEILS.size()) {
            ServerPlayNetworking.send(sp, new com.dungeonmod.network.SubtitlePayload(getNpcName(), com.dungeonmod.client.dialogue.GaspardDialogue.NO_MORE_CONSEIL, false));
            return;
        }
        // First meeting always shown regardless of beer
        if (!hasTalked) {
            hasTalked = true;
            setCustomName(net.minecraft.text.Text.literal(getNpcName()));
            var lines = new java.util.ArrayList<String>();
            lines.addAll(getFirstMeetingLines());
            lines.addAll(getStandardPromptLines());
            sendSubtitles(sp, lines);
            return;
        }
        // Check if player has any beer (viking or brune)
        var biereV = com.dungeonmod.ModItems.get("biere_viking");
        var biereB = com.dungeonmod.ModItems.get("biere_brune");
        boolean hasBeer = false;
        var inv = player.getInventory();
        for (int i = 0; i < inv.size(); i++) {
            ItemStack s = inv.getStack(i);
            if (!s.isEmpty()) {
                if (biereV != null && s.isOf(biereV.createStack().getItem()) && s.getName().getString().equals(biereV.createStack().getName().getString())) { hasBeer = true; break; }
                if (biereB != null && s.isOf(biereB.createStack().getItem()) && s.getName().getString().equals(biereB.createStack().getName().getString())) { hasBeer = true; break; }
            }
        }
        if (!hasBeer) {
            ServerPlayNetworking.send(sp, new com.dungeonmod.network.SubtitlePayload(getNpcName(), com.dungeonmod.client.dialogue.GaspardDialogue.NO_BEER, false));
            return;
        }
        super.startDialogue(player);
    }

    @Override
    public void writeCustomDataToNbt(net.minecraft.nbt.NbtCompound nbt) {
        super.writeCustomDataToNbt(nbt);
        var list = new net.minecraft.nbt.NbtList();
        for (int idx : givenConseils) list.add(net.minecraft.nbt.NbtInt.of(idx));
        nbt.put("givenConseils", list);
    }

    @Override
    public void readCustomDataFromNbt(net.minecraft.nbt.NbtCompound nbt) {
        super.readCustomDataFromNbt(nbt);
        givenConseils.clear();
        if (nbt.contains("givenConseils")) {
            for (var t : nbt.getList("givenConseils", net.minecraft.nbt.NbtElement.INT_TYPE)) {
                givenConseils.add(((net.minecraft.nbt.NbtInt)t).intValue());
            }
        }
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
                this.handleConseilTrade(player, s, i);
                break;
            }
        }
    }

    public void handleConseilTrade(ServerPlayerEntity player, ItemStack soldStack, int slotIndex) {
        // Si tous les conseils sont donnés, retourner l'item sans rien consommer
        if (givenConseils.size() >= com.dungeonmod.client.dialogue.GaspardDialogue.CONSEILS.size()) {
            if (slotIndex < 0) {
                if (!player.getInventory().insertStack(soldStack)) {
                    player.dropItem(soldStack, false);
                }
            }
            net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking.send(player, new com.dungeonmod.network.SubtitlePayload("§6Gaspard", com.dungeonmod.client.dialogue.GaspardDialogue.NO_MORE_CONSEIL, false));
            player.closeHandledScreen();
            return;
        }
        var biereV = com.dungeonmod.ModItems.get("biere_viking");
        var biereB = com.dungeonmod.ModItems.get("biere_brune");
        var inv = player.getInventory();
        if (biereV != null && soldStack.isOf(biereV.createStack().getItem()) && soldStack.getName().getString().equals(biereV.createStack().getName().getString())) {
            // Bière viking → consommer, dialogue conseil aléatoire, fermer shop
            if (slotIndex >= 0) {
                soldStack.decrement(1);
                if (soldStack.isEmpty()) inv.setStack(slotIndex, ItemStack.EMPTY);
            }
            var conseils = com.dungeonmod.client.dialogue.GaspardDialogue.CONSEILS;
            var available = new java.util.ArrayList<Integer>();
            for (int ci = 0; ci < conseils.size(); ci++) {
                if (!givenConseils.contains(ci)) available.add(ci);
            }
            int chosen = available.get(RANDOM.nextInt(available.size()));
            givenConseils.add(chosen);
            net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking.send(player, new com.dungeonmod.network.SubtitlePayload("§6Gaspard", conseils.get(chosen), false));
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
