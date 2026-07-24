package com.dungeonmod.entity;

import com.dungeonmod.DungeonMod;
import com.dungeonmod.client.dialogue.BarmanDialogue;
import com.dungeonmod.network.CyclopsTradesPayload;
import com.dungeonmod.network.SubtitlePayload;
import com.dungeonmod.network.TradeData;
import java.util.List;
import com.dungeonmod.screen.CyclopsTradeScreenHandler;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.SpawnGroup;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.screen.SimpleNamedScreenHandlerFactory;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.world.World;

import java.util.List;

public class BarmanEntity extends BaseNpcEntity implements NpcShopProvider {

    private static final Identifier ID = Identifier.of(DungeonMod.MOD_ID, "barman");
    public static final EntityType<BarmanEntity> TYPE = Registry.register(
        Registries.ENTITY_TYPE, ID,
        EntityType.Builder.<BarmanEntity>create(BarmanEntity::new, SpawnGroup.MISC)
            .dimensions(0.6f, 1.95f).maxTrackingRange(64)
            .build(RegistryKey.of(Registries.ENTITY_TYPE.getKey(), ID))
    );

    public BarmanEntity(EntityType<? extends net.minecraft.entity.mob.ZombieEntity> entityType, World world) {
        super(entityType, world);
    }

    @Override public String getNpcName() { return "§6Mira"; }
    @Override public List<String> getFirstMeetingLines() { return BarmanDialogue.FIRST_MEETING; }
    @Override public List<String> getStandardPromptLines() { return BarmanDialogue.STANDARD_PROMPT; }

    @Override
    protected void startDialogue(PlayerEntity player) {
        if (!(player instanceof ServerPlayerEntity sp)) return;
        dialogueCooldown = 60;
        DungeonMod.npcShopCache.put(sp.getUuid(), this.getUuid());

        if (!hasTalked) {
            hasTalked = true;
            setCustomName(net.minecraft.text.Text.literal(getNpcName()));
            checkReturn(player); // initialise lastTalkPos sans incrémenter
            var lines = new java.util.ArrayList<String>();
            lines.addAll(getFirstMeetingLines());
            lines.addAll(getStandardPromptLines());
            sendSubtitles(sp, lines);
            return;
        }

        boolean returned = checkReturn(player);
        if (returned) returnCount++;

        if (returnCount == 1 && returned || returnCount == 2 && returned) {

            var lines = new java.util.ArrayList<String>();
            lines.addAll(returnCount == 1 ? BarmanDialogue.RETURN_1 : BarmanDialogue.RETURN_2);
            if (hasEnoughDeniers(sp)) {
                lines.addAll(getStandardPromptLines());
                ServerPlayNetworking.send(sp, new SubtitlePayload("Mira", lines, true));
            } else {
                lines.addAll(BarmanDialogue.NOT_ENOUGH_DENIERS);
                ServerPlayNetworking.send(sp, new SubtitlePayload("Mira", lines, false));
            }
            return;
        }

        if (!hasEnoughDeniers(sp)) {
            ServerPlayNetworking.send(sp, new SubtitlePayload("Mira", BarmanDialogue.NOT_ENOUGH_DENIERS, false));
            return;
        }

        sendSubtitles(sp, getStandardPromptLines());
    }

    private boolean hasEnoughDeniers(ServerPlayerEntity player) {
        var denierRef = com.dungeonmod.ModItems.get("denier").createStack();
        int count = 0;
        for (int i = 0; i < player.getInventory().size(); i++) {
            var s = player.getInventory().getStack(i);
            if (!s.isEmpty() && net.minecraft.item.ItemStack.areItemsAndComponentsEqual(s, denierRef)) {
                count += s.getCount();
                if (count >= 2) return true;
            }
        }
        return false;
    }

    @Override
    public void openShop(ServerPlayerEntity player) { openMiraShop(player); }

    @Override
    public void processBuy(ServerPlayerEntity player, int tradeIndex, int quantity) {
        int successCount = 0;
        for (int i = 0; i < quantity; i++) {
            int before = player.getInventory().count(com.dungeonmod.ModItems.get("biere_viking").createStack().getItem());
            processBuyRequest(player, tradeIndex);
            int after = player.getInventory().count(com.dungeonmod.ModItems.get("biere_viking").createStack().getItem());
            if (after > before) successCount++;
            else break;
        }
        if (successCount > 0) {
            dialogueCooldown = 200;
            DungeonMod.npcShopCache.remove(player.getUuid());
            List<String> lines = successCount == 1 ? BarmanDialogue.PURCHASE_SINGLE : BarmanDialogue.PURCHASE_MULTIPLE;

            ServerPlayNetworking.send(player, new SubtitlePayload("Mira", lines, false));
        }
    }

    public void openMiraShop(ServerPlayerEntity player) {
        ServerPlayNetworking.send(player, new SubtitlePayload("", List.of(), false));

        var denierStack = com.dungeonmod.ModItems.get("denier").createStack();
        denierStack.setCount(2);

        var beer = com.dungeonmod.ModItems.get("biere_viking").createStack();

        var trades = List.of(new TradeData(denierStack.copy(), beer, 0));

        var syncId = player.openHandledScreen(new SimpleNamedScreenHandlerFactory(
            (id, inv, p) -> new CyclopsTradeScreenHandler(id, inv),
            Text.literal("§8Taverne")
        ));
        syncId.ifPresent(id ->
            ServerPlayNetworking.send(player, new CyclopsTradesPayload(id, "barman", "Mira", true, false, trades))
        );
    }

    public void processBuyRequest(ServerPlayerEntity player, int tradeIndex) {
        if (tradeIndex != 0) return;

        PlayerInventory inv = player.getInventory();
        int denierCount = 0;
        for (int i = 0; i < inv.size(); i++) {
            ItemStack s = inv.getStack(i);
            if (isDenier(s)) denierCount += s.getCount();
        }
        if (denierCount < 2) {

            return;
        }

        int remaining = 2;
        for (int i = 0; i < inv.size() && remaining > 0; i++) {
            ItemStack s = inv.getStack(i);
            if (isDenier(s)) {
                int take = Math.min(remaining, s.getCount());
                s.decrement(take);
                remaining -= take;
                if (s.isEmpty()) inv.setStack(i, ItemStack.EMPTY);
            }
        }

        var beer = com.dungeonmod.ModItems.get("biere_viking").createStack();
        if (!inv.insertStack(beer)) player.dropItem(beer, false);

    }

    private boolean isDenier(ItemStack s) {
        if (s.isEmpty() || s.getItem() != Items.GOLD_NUGGET) return false;
        var cn = s.get(DataComponentTypes.CUSTOM_NAME);
        return cn != null && cn.getString().equals("§7Denier");
    }
}
