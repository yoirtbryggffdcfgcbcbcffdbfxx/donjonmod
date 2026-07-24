package com.dungeonmod.village;

import net.minecraft.component.DataComponentTypes;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.LoreComponent;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.text.Text;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.predicate.ComponentPredicate;
import net.minecraft.registry.Registries;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.screen.MerchantScreenHandler;
import net.minecraft.screen.SimpleNamedScreenHandlerFactory;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.village.SimpleMerchant;
import net.minecraft.village.TradeOffer;
import net.minecraft.village.TradeOfferList;
import net.minecraft.village.TradedItem;

import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

public class NpcMerchant extends SimpleMerchant {

    private Consumer<ServerPlayerEntity> onTradeCallback;
    private Runnable onCloseCallback;
    private boolean tradeCompleted = false;
    private int syncId = -1;
    private int maxTrades = 0;

    public NpcMerchant(PlayerEntity player, List<TradeOffer> offers, Consumer<ServerPlayerEntity> onTrade, Runnable onClose) {
        super(player);
        TradeOfferList list = new TradeOfferList();
        list.addAll(offers);
        setOffersFromServer(list);
        this.onTradeCallback = onTrade;
        this.onCloseCallback = onClose;
    }

    public void setSyncId(int id) { this.syncId = id; }
    public void setMaxTrades(int m) { this.maxTrades = m; }

    @Override
    public void setCustomer(PlayerEntity player) {
        super.setCustomer(player);
        if (player == null && tradeCompleted && onCloseCallback != null) {
            onCloseCallback.run();
        }
    }

    @Override
    public void trade(TradeOffer offer) {
        super.trade(offer);
        tradeCompleted = true;
        if (getCustomer() instanceof ServerPlayerEntity sp) {
            sp.getWorld().playSound(null, sp.getX(), sp.getY(), sp.getZ(),
                SoundEvents.BLOCK_AMETHYST_CLUSTER_BREAK, SoundCategory.PLAYERS, 1.0f, 1.5f);
            ((net.minecraft.server.world.ServerWorld)sp.getWorld()).spawnParticles(
                ParticleTypes.WAX_OFF, sp.getX(), sp.getY() + 1.0, sp.getZ(), 20, 0.5, 0.5, 0.5, 0.1);
            int totalUses = getOffers().stream().mapToInt(TradeOffer::getUses).sum();
            if (maxTrades > 0 && totalUses >= maxTrades) {
                for (TradeOffer o : getOffers()) o.disable();
            }
            sp.getServer().execute(() -> {
                if (onTradeCallback != null) onTradeCallback.accept(sp);
                if (syncId >= 0) {
                    sp.sendTradeOffers(syncId, getOffers(), 0, 0, false, false);
                }
            });
        }
    }

    public static void open(ServerPlayerEntity player, String title, List<TradeOffer> offers,
                            Consumer<ServerPlayerEntity> onTrade, Runnable onClose, int maxTrades) {
        var merchant = new NpcMerchant(player, offers, onTrade, onClose);
        merchant.setMaxTrades(maxTrades);
        var syncIdOpt = player.openHandledScreen(new SimpleNamedScreenHandlerFactory(
            (syncId, inv, p) -> {
                var handler = new MerchantScreenHandler(syncId, inv, merchant);
                merchant.setSyncId(syncId);
                return handler;
            },
            Text.literal(title)
        ));
        syncIdOpt.ifPresent(syncId -> {
            merchant.setSyncId(syncId);
            player.sendTradeOffers(syncId, merchant.getOffers(), 0, 0, false, false);
        });
    }

    public static TradeOffer beerTrade(String beerId, ItemStack output) {
        var custom = com.dungeonmod.ModItems.get(beerId);
        ItemStack template = custom != null ? custom.createStack() : new ItemStack(Items.POTION);
        var entry = Registries.ITEM.getEntry(template.getItem());
        var predicate = ComponentPredicate.of(template.getComponents());
        var tradedInput = new TradedItem(entry, 1, predicate, template);
        return new TradeOffer(tradedInput, output, 1, 0, 0.0f);
    }

    public static TradeOffer offerFromItems(ItemStack input, ItemStack output) {
        var ti = new TradedItem(input.getItem(), input.getCount());
        return new TradeOffer(ti, output, 1, 0, 0.0f);
    }

    public static TradeOffer offer(ItemStack input, ItemStack secondary, ItemStack output) {
        var ti = new TradedItem(input.getItem(), input.getCount());
        var si = Optional.of(new TradedItem(secondary.getItem(), secondary.getCount()));
        return new TradeOffer(ti, si, output, 1, 0, 0.0f);
    }

    public static ItemStack questionMark() {
        ItemStack stack = new ItemStack(com.dungeonmod.DungeonMod.BOUT_TISSU);
        stack.set(DataComponentTypes.CUSTOM_NAME, Text.literal("§7Bout de tissu"));
        stack.set(DataComponentTypes.LORE, new LoreComponent(java.util.List.of(
            Text.literal("§7Un tissu mystérieux..."),
            Text.literal("§7À ouvrir près du cyclope.")
        )));
        return stack;
    }
}
