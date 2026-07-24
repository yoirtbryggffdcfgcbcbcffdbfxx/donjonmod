package com.dungeonmod.screen;

import com.dungeonmod.network.CyclopsBuyPayload;
import com.dungeonmod.network.CyclopsSellPayload;
import com.dungeonmod.network.TradeData;
import com.dungeonmod.village.SellTradeRegistry;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.screen.slot.Slot;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

import java.util.List;

public class CyclopsTradeScreen extends HandledScreen<CyclopsTradeScreenHandler> {

    private static final Identifier BG = Identifier.ofVanilla("textures/gui/container/villager.png");
    private static final Identifier SLOT = Identifier.of("container/slot");
    private static final Identifier TRADE_ROW_BG = Identifier.of("dungeonmod", "textures/gui/trade_row_bg.png");
    private static final Identifier TRADE_ROW_BG_DISABLED = Identifier.of("dungeonmod", "textures/gui/trade_row_bg_disabled.png");
    private static final Identifier TRADE_ROW_BG_SELECTED = Identifier.of("dungeonmod", "textures/gui/trade_row_bg_selected.png");
    private static final Identifier BTN_BUY = Identifier.of("dungeonmod", "textures/gui/bouton_buy.png");
    private static final Identifier BTN_BUY_GRAY = Identifier.of("dungeonmod", "textures/gui/bouton_buy_grise.png");
    private static final Identifier MODE_BUY = Identifier.of("dungeonmod", "textures/gui/btn_mode_buy.png");
    private static final Identifier MODE_SELL = Identifier.of("dungeonmod", "textures/gui/btn_mode_sell.png");
    private static final Identifier MODE_GRAY = Identifier.of("dungeonmod", "textures/gui/btn_mode_gray.png");
    private static final Identifier MODE_SELECT = Identifier.of("dungeonmod", "textures/gui/btn_mode_select.png");
    private static final Identifier BTN_RECOMMANDER = Identifier.of("dungeonmod", "textures/gui/recommander.png");
    private static final Identifier BTN_SELL = Identifier.of("dungeonmod", "textures/gui/bouton_sell.png");
    private static final Identifier BTN_SELL_GRAY = Identifier.of("dungeonmod", "textures/gui/bouton_sell_grise.png");
    private static final Identifier BTN_QTY_MINUS = Identifier.of("dungeonmod", "textures/gui/btn_qty_minus.png");
    private static final Identifier BTN_QTY_PLUS = Identifier.of("dungeonmod", "textures/gui/btn_qty_plus.png");
    private static final Identifier ICON_COUT = Identifier.of("dungeonmod", "textures/gui/villager_cout.png");
    private static final Identifier OVERLAY_BG = Identifier.of("dungeonmod", "textures/gui/overlay_shop_sell.png");
    private static final Identifier OVERLAY_ROW = Identifier.of("dungeonmod", "textures/gui/ligne_item_overlay.png");
    private static final Identifier SELL_ENTRE_2 = Identifier.of("dungeonmod", "textures/gui/sell_entre_2.png");
    private static final Identifier TRADE_SON_OFFRE = Identifier.of("dungeonmod", "textures/gui/trade_son_offre.png");
    private static final Identifier CROIX = Identifier.of("dungeonmod", "textures/gui/bouton_croix_sell.png");
    private static final int BG_W = 276;
    private static final int BG_H = 168;
    private static final int SLOT_LARGE = 54;

    private List<TradeData> trades;
    private int selectedTrade = -1;
    private int clickAnimTimer = 0;
    private int rowClickTimer = 0;
    private int buyQuantity = 1;
    private boolean hasQuantitySelector = false;
    private int tradeMode = 0; // 0=BUY, 1=SELL
    private boolean hasBuyMode = true;
    private boolean hasSellMode = false;
    private SellTradeRegistry.SellOffer selectedSellReward = null;
    private boolean showingRewards = false;
    private int rewardScrollOffset = 0;
    private static final Identifier ITEM_CHOICE_OVERLAY = Identifier.of("dungeonmod", "textures/gui/2_item_overlay.png");
    private List<SellTradeRegistry.SellOffer> premadeOffers = List.of();
    private List<SellTradeRegistry.SellOffer> customOffers = new java.util.ArrayList<>();
    private int selectedOfferRow = -1;
    private int customOfferScroll = 0;
    private boolean showingItemChoice = false;
    private java.util.Map<Integer, ItemStack> savedDeposits = new java.util.HashMap<>();
    private boolean firstTick = true;
    private int animTick = 0;
    private int shrinkingIndex = -1;
    private int shrinkTimer = 0;
    private static final int SHRINK_DURATION = 8;

    public CyclopsTradeScreen(CyclopsTradeScreenHandler handler, PlayerInventory inventory, Text title) {
        super(handler, inventory, title);
        this.backgroundWidth = BG_W;
        this.backgroundHeight = BG_H;
        this.playerInventoryTitleY = -1000;
        this.titleX = 8;
    }

    public void setTrades(List<TradeData> trades) {
        this.trades = trades;
    }

    public void setHasQuantitySelector(boolean v) {
        hasQuantitySelector = v;
        if (!v) buyQuantity = 1;
    }
    public void setHasBuyMode(boolean v) { hasBuyMode = v; }
    public void setHasSellMode(boolean v, String npcId) { hasSellMode = v; }

    @Override
    protected void init() {
        super.init();
        this.titleX = (BG_W - this.textRenderer.getWidth(this.title)) / 2 + 50;
        if (handler.selectedTrade >= 0 && trades != null && handler.selectedTrade < trades.size()) {
            selectedTrade = handler.selectedTrade;
        }
        if (this.title != null && !this.title.getString().contains("Cyclope")) {
            setHasQuantitySelector(true);
        }
        if (this.title != null && this.title.getString().contains("Gaspard")) {
            setHasBuyMode(false);
            setHasSellMode(true, "gaspard");
            premadeOffers = SellTradeRegistry.getOffers("gaspard");
            tradeMode = 1;
        }
    }

    @Override
    protected void handledScreenTick() {
        super.handledScreenTick();
        if (clickAnimTimer > 0) clickAnimTimer--;
        if (rowClickTimer > 0) rowClickTimer--;
        animTick++;
        // First tick: ensure premade offer is selected and slot filled
        if (firstTick) {
            firstTick = false;
            if (tradeMode == 1 && hasSellMode && !premadeOffers.isEmpty()) {
                selectedOfferRow = -1;
                switchToOffer(0);
            }
        }
        // Auto-update custom offer when deposit/reward changes
        if (tradeMode == 1 && hasSellMode && selectedOfferRow >= premadeOffers.size()) {
            int ci = selectedOfferRow - premadeOffers.size();
            if (ci >= 0 && ci < customOffers.size()) {
                ItemStack dep = this.handler.getSlot(0).getStack();
                var offer = customOffers.get(ci);
                if (!dep.isEmpty()) {
                    if (offer.requiredItems().isEmpty() || !ItemStack.areItemsAndComponentsEqual(dep, offer.requiredItems().get(0))) {
                        customOffers.set(ci, new SellTradeRegistry.SellOffer(java.util.List.of(dep.copy()), 1, selectedSellReward != null ? selectedSellReward.rewardItem().copy() : ItemStack.EMPTY, 1));
                    }
                } else if (!offer.requiredItems().isEmpty()) {
                    customOffers.set(ci, new SellTradeRegistry.SellOffer(java.util.List.of(), 1, selectedSellReward != null ? selectedSellReward.rewardItem().copy() : ItemStack.EMPTY, 1));
                }
            }
        }
        if (shrinkingIndex >= 0) {
            shrinkTimer--;
            if (shrinkTimer <= 0) {
                trades.remove(shrinkingIndex);
                shrinkingIndex = -1;
                if (!trades.isEmpty()) {
                    int newSel = (int)(Math.random() * trades.size());
                    selectedTrade = newSel;
                } else {
                    selectedTrade = -1;
                }
                if (trades.isEmpty()) this.client.setScreen(null);
            }
        }
    }

    private boolean canAffordTrade(int index) {
        if (trades == null || index < 0 || index >= trades.size()) return false;
        TradeData td = trades.get(index);
        if (this.client.player == null) return false;
        int needed = td.input().getCount() * buyQuantity;
        int have = 0;
        for (int i = 0; i < this.client.player.getInventory().size(); i++) {
            ItemStack s = this.client.player.getInventory().getStack(i);
            if (!s.isEmpty() && ItemStack.areItemsAndComponentsEqual(s, td.input())) {
                have += s.getCount();
                if (have >= needed) return true;
            }
        }
        return false;
    }

    private boolean hasItemInInventory(ItemStack target) {
        if (this.client.player == null) return false;
        for (int i = 0; i < this.client.player.getInventory().size(); i++) {
            ItemStack s = this.client.player.getInventory().getStack(i);
            if (!s.isEmpty() && s.isOf(target.getItem()) && s.getName().getString().equals(target.getName().getString())) return true;
        }
        return false;
    }

    @Override
    protected void drawBackground(DrawContext context, float delta, int mouseX, int mouseY) {
        int x = (this.width - BG_W) / 2;
        int y = (this.height - BG_H) / 2;

        context.drawTexture(RenderLayer::getGuiTextured, BG, x, y, 0, 0, BG_W, BG_H, 512, 256);

        // Mode buttons (Achat / Vente)
        int mx = x + 4, my = y + 3;
        drawModeButton(context, mx, my, "§7Achat", 0);
        drawModeButton(context, mx + 45, my, "§7Vente", 1);

        // Mode SELL
        if (tradeMode == 1 && hasSellMode && !showingRewards) {
            int rowH2 = 22;
            int rowY = y + 20;

            // First separator with "offre de Gaspard"
            context.drawTexture(RenderLayer::getGuiTextured, SELL_ENTRE_2, x + 5, rowY, 0f, 0f, 88, 10, 88, 10);
            context.getMatrices().push();
            context.getMatrices().scale(0.75f, 0.75f, 1.0f);
            context.drawText(this.textRenderer, "offre de Gaspard", (int)((x + 12) / 0.75f), (int)((rowY + 3) / 0.75f), 0x000000, false);
            context.getMatrices().pop();
            rowY += 12;

            // Premade offers
            for (int i = 0; i < premadeOffers.size(); i++) {
                var offer = premadeOffers.get(i);
                ItemStack in = offer.requiredItems().get((animTick / 20) % offer.requiredItems().size());
                ItemStack out = offer.rewardItem();
                context.drawTexture(RenderLayer::getGuiTextured, TRADE_ROW_BG, x + 5, rowY, 0f, 0f, 87, 22, 87, 22);
                if (i == selectedOfferRow) {
                    context.drawTexture(RenderLayer::getGuiTextured, TRADE_ROW_BG_SELECTED, x + 5, rowY, 0f, 0f, 87, 22, 87, 22);
                }
                context.drawItem(in, x + 12, rowY + 3);
                context.drawStackOverlay(this.textRenderer, in, x + 12, rowY + 3);
                context.drawText(this.textRenderer, "\u2192", x + 35, rowY + 6, 0x3C3C3C, false);
                context.drawItem(out, x + 55, rowY + 3);
                context.drawStackOverlay(this.textRenderer, out, x + 55, rowY + 3);
                rowY += rowH2;
            }

            // Second separator with "offre personnalisable"
            context.drawTexture(RenderLayer::getGuiTextured, SELL_ENTRE_2, x + 5, rowY +2, 0f, 0f, 88, 10, 88, 10);
            context.getMatrices().push();
            context.getMatrices().scale(0.75f, 0.75f, 1.0f);
            context.drawText(this.textRenderer, "offres supplémentaires", (int)((x -31) / 0.50f), (int)((rowY +5) / 0.75f), 0x000000, false);
            context.getMatrices().pop();
            rowY += 14;

            // Custom offers (scrollable)
            int maxCustomVis = Math.max(1, 4 - premadeOffers.size());
            for (int i = 0; i < customOffers.size(); i++) {
                int idx = i - customOfferScroll;
                if (idx < 0) continue;
                if (idx >= maxCustomVis) break;
                var offer = customOffers.get(i);
                ItemStack in = offer.requiredItems().isEmpty() ? ItemStack.EMPTY : offer.requiredItems().get(0);
                ItemStack out = offer.rewardItem();
                int absIdx = premadeOffers.size() + i;
                context.drawTexture(RenderLayer::getGuiTextured, TRADE_ROW_BG, x + 5, rowY, 0f, 0f, 87, 22, 87, 22);
                if (absIdx == selectedOfferRow) {
                    context.drawTexture(RenderLayer::getGuiTextured, TRADE_ROW_BG_SELECTED, x + 5, rowY, 0f, 0f, 87, 22, 87, 22);
                }
                context.drawItem(in, x + 12, rowY + 3);
                context.drawStackOverlay(this.textRenderer, in, x + 12, rowY + 3);
                context.drawText(this.textRenderer, "\u2192", x + 35, rowY + 6, 0x3C3C3C, false);
                context.drawItem(out, x + 55, rowY + 3);
                context.drawStackOverlay(this.textRenderer, out, x + 55, rowY + 3);
                // Cross button to delete custom trade
                context.drawTexture(RenderLayer::getGuiTextured, CROIX, x + 71, rowY + 1, 0f, 0f, 20, 20, 20, 20);
                rowY += rowH2;
            }

            // Scrollbar for custom offers (Minecraft scroller)
            if (customOffers.size() > maxCustomVis) {
                int sbX = x + 94;
                int sbY = rowY - maxCustomVis * rowH2;
                int sbH = maxCustomVis * rowH2;
                int thumbH = Math.max(15, sbH * maxCustomVis / customOffers.size());
                int thumbY = sbY + (sbH - thumbH) * customOfferScroll / (customOffers.size() - maxCustomVis);
                context.drawGuiTexture(RenderLayer::getGuiTextured, Identifier.ofVanilla("container/villager/scroller"), sbX, sbY, 6, sbH);
                context.drawGuiTexture(RenderLayer::getGuiTextured, Identifier.ofVanilla("container/villager/scroller"), sbX, thumbY, 6, thumbH);
            }

            // trade_son_offre button
            int btnOffreY = rowY + 4;
            context.drawTexture(RenderLayer::getGuiTextured, TRADE_SON_OFFRE, x + 5, btnOffreY, 0f, 0f, 87, 22, 87, 22);
            context.getMatrices().push();
            context.getMatrices().scale(0.75f, 0.75f, 1.0f);
            context.drawText(this.textRenderer, "créer une offre +", (int)((x + 15) / 0.75f), (int)((btnOffreY + 8) / 0.75f), 0x000000, false);
            context.getMatrices().pop();

            // Custom section (right side)
            int slotSize = 27;
            int sx = x + 125, sy = y + 41;
            int rx = sx + slotSize + 30;

            context.drawText(this.textRenderer, "§7mon offre", sx + (slotSize - this.textRenderer.getWidth("mon offre")) / 2, sy - 10, 0x7C7C7C, false);
            context.drawText(this.textRenderer, "§7\u2192", sx + slotSize + 10, sy + 6, 0x3C3C3C, false);
            context.drawText(this.textRenderer, "§7son offre", rx + (slotSize - this.textRenderer.getWidth("son offre")) / 2, sy - 10, 0x7C7C7C, false);

            context.getMatrices().push();
            context.getMatrices().translate(rx, sy, 0);
            context.getMatrices().scale(1.5f, 1.5f, 1.0f);
            context.drawGuiTexture(RenderLayer::getGuiTextured, SLOT, 0, 0, 18, 18);
            context.getMatrices().pop();
            if (selectedSellReward != null) {
                context.getMatrices().push();
                context.getMatrices().translate(rx + slotSize / 2, sy + slotSize / 2, 0);
                context.getMatrices().scale(1.5f, 1.5f, 1.0f);
                context.drawItemWithoutEntity(selectedSellReward.rewardItem(), -8, -8);
                context.getMatrices().pop();
            }

            ItemStack depositCheck = this.handler.getSlot(0).getStack();
            boolean itemOk = !depositCheck.isEmpty() && premadeOffers.stream().anyMatch(o -> o.matches(depositCheck));
            boolean canSell = itemOk && selectedSellReward != null;
            int by2 = sy + (slotSize - 11) / 2;
            int bx = rx + slotSize + 10;
            int pressOff = clickAnimTimer > 0 ? 1 : 0;
            Identifier btnTex = canSell ? BTN_SELL : BTN_SELL_GRAY;
            context.getMatrices().push();
            context.getMatrices().translate(bx + pressOff, by2 + pressOff, 0);
            context.getMatrices().scale(1.5f, 1.5f, 1.0f);
            context.drawTexture(RenderLayer::getGuiTextured, btnTex, 0, 0, 0f, 0f, 20, 11, 20, 11);
            context.getMatrices().pop();

            // Item choice overlay for premade offers with multiple inputs
            if (showingItemChoice && selectedOfferRow >= 0 && selectedOfferRow < premadeOffers.size()) {
                var offer = premadeOffers.get(selectedOfferRow);
                if (offer.requiredItems().size() > 1) {
                    int ox = sx - (52 - slotSize) / 2;
                    int oy = sy - 36;
                    context.drawTexture(RenderLayer::getGuiTextured, ITEM_CHOICE_OVERLAY, ox, oy, 0, 0, 52, 34, 52, 34);
                    int slotW = 26;
                    for (int ci = 0; ci < offer.requiredItems().size() && ci < 2; ci++) {
                        ItemStack ciStack = offer.requiredItems().get(ci);
                        context.drawGuiTexture(RenderLayer::getGuiTextured, SLOT, ox + 4 + ci * slotW, oy +3, 18, 18);
                        if (this.client.player != null && hasItemInInventory(ciStack)) {
                            context.drawItem(ciStack, ox + 4 + ci * slotW, oy + 3);
                            context.drawStackOverlay(this.textRenderer, ciStack, ox + 4 + ci * slotW, oy +3);
                        }
                    }
                }
            }

            return;
        }

        if (trades != null) {
        for (int i = 0; i < trades.size(); i++) {
            boolean isShrinking = (i == shrinkingIndex);
            float rowScale = isShrinking ? (float)shrinkTimer / SHRINK_DURATION : 1.0f;
            if (isShrinking && rowScale <= 0.01f) continue;

            int tyBase = y + 22 + i * 22;
            int rh = (int)(22 * rowScale);
            int ty = tyBase + (22 - rh);
            if (rh <= 0) continue;

            TradeData td = trades.get(i);
            boolean canAfford = canAffordTrade(i);
            Identifier bgTex = canAfford ? TRADE_ROW_BG : TRADE_ROW_BG_DISABLED;

            if (isShrinking) {
                context.getMatrices().push();
                context.getMatrices().translate(0, 0, 0);
            }

            context.drawTexture(RenderLayer::getGuiTextured, bgTex, x + 5, ty, 0f, 0f, 87, rh, 87, 22);

            if (i == selectedTrade) {
                context.drawTexture(RenderLayer::getGuiTextured, TRADE_ROW_BG_SELECTED, x + 5, ty, 0f, 0f, 87, rh, 87, 22);
            }
            if (rowClickTimer > 0 && i == selectedTrade) {
                context.fill(x + 5, ty, x + 92, ty + rh, 0x88FFFFFF);
            }

            context.drawItem(td.input(), x + 12, ty + 3);
            context.drawStackOverlay(this.textRenderer, td.input(), x + 12, ty + 3);
            context.drawText(this.textRenderer, "\u2192", x + 35, ty + 6, 0x3C3C3C, false);
            context.drawItem(td.output(), x + 55, ty + 3);
            context.drawStackOverlay(this.textRenderer, td.output(), x + 55, ty + 3);

            if (isShrinking) {
                context.getMatrices().pop();
            }
        }

        if (selectedTrade >= 0 && selectedTrade < trades.size()) {
            int sx = x + 155;
            int sy = y + 12;
            boolean canAfford = canAffordTrade(selectedTrade);

            context.getMatrices().push();
            context.getMatrices().translate(sx, sy, 0);
            context.getMatrices().scale(3.0f, 3.0f, 1.0f);
            context.drawGuiTexture(RenderLayer::getGuiTextured, SLOT, 0, 0, 18, 18);
            context.getMatrices().pop();

            ItemStack output = trades.get(selectedTrade).output();
            context.getMatrices().push();
            context.getMatrices().translate(sx + SLOT_LARGE / 2, sy + SLOT_LARGE / 2, 0);
            context.getMatrices().scale(3.0f, 3.0f, 1.0f);
            context.drawItemWithoutEntity(output, -8, -8);
            context.getMatrices().pop();

            int bx = sx + SLOT_LARGE + 15;
            int by = sy + (SLOT_LARGE - 5) / 2;
            int pressOff = (clickAnimTimer > 0 && canAfford) ? 1 : 0;
            Identifier btnTex = canAfford ? BTN_BUY : BTN_BUY_GRAY;
            context.getMatrices().push();
            context.getMatrices().translate(bx + pressOff, by + pressOff, 0);
            context.getMatrices().scale(1.5f, 1.5f, 1.0f);
            context.drawTexture(RenderLayer::getGuiTextured, btnTex, 0, 0, 0f, 0f, 20, 11, 20, 11);
            context.getMatrices().pop();

            // Boutons quantité [-] [coût] [+] en dessous du slot
            if (hasQuantitySelector) {
                int qy = sy + SLOT_LARGE + 2;
                context.drawTexture(RenderLayer::getGuiTextured, BTN_QTY_MINUS, sx + 2, qy, 0f, 0f, 13, 9, 13, 9);
                context.drawTexture(RenderLayer::getGuiTextured, ICON_COUT, sx + 16, qy, 0f, 0f, 20, 9, 20, 9);
                context.drawText(this.textRenderer, "§0" + buyQuantity, sx + 23, qy + 1, 0x000000, false);
                context.drawTexture(RenderLayer::getGuiTextured, BTN_QTY_PLUS, sx + 37, qy, 0f, 0f, 13, 9, 13, 9);
            }
        }
        } // end if (trades != null)
    }

    private void returnDepositToPlayer() {
        ItemStack stack = this.handler.getSlot(0).getStack();
        if (!stack.isEmpty() && this.client.player != null) {
            if (!this.client.player.getInventory().insertStack(stack)) {
                this.client.player.dropItem(stack, false);
            }
            this.handler.getSlot(0).setStack(ItemStack.EMPTY);
        }
    }

    private void switchToOffer(int newIdx) {
        if (newIdx == selectedOfferRow) return;
        int oldIdx = selectedOfferRow;
        ItemStack currentDep = this.handler.getSlot(0).getStack();

        if (oldIdx >= 0 && oldIdx < premadeOffers.size() && !currentDep.isEmpty()) {
            // Premade → quelque chose : sauvegarde SANS rendre au joueur
            savedDeposits.put(oldIdx, currentDep.copy());
            this.handler.getSlot(0).setStack(ItemStack.EMPTY);
            currentDep = ItemStack.EMPTY;
        }
        // Pour les offres custom : rendre au joueur
        if (!currentDep.isEmpty()) {
            returnDepositToPlayer();
        }

        selectedOfferRow = newIdx;
        selectedSellReward = null;

        if (newIdx >= 0 && newIdx < premadeOffers.size()) {
            // Restore saved deposit for this premade
            if (savedDeposits.containsKey(newIdx)) {
                this.handler.getSlot(0).setStack(savedDeposits.remove(newIdx));
            }
            var offer = premadeOffers.get(newIdx);
            selectedSellReward = offer;
            if (this.handler.getSlot(0).getStack().isEmpty() && this.client.player != null) {
                var inv = this.client.player.getInventory();
                for (int si = 0; si < inv.size(); si++) {
                    ItemStack s = inv.getStack(si);
                    if (!s.isEmpty() && offer.matches(s)) {
                        this.handler.getSlot(0).setStack(s.split(1));
                        if (s.isEmpty()) inv.setStack(si, ItemStack.EMPTY);
                        break;
                    }
                }
            }
        }
    }

    private void drawModeButton(DrawContext context, int mx, int my, String label, int mode) {
        boolean selected = (tradeMode == mode);
        boolean available = (mode == 0) ? hasBuyMode : hasSellMode;
        Identifier tex = available ? (mode == 0 ? MODE_BUY : MODE_SELL) : MODE_GRAY;
        context.drawTexture(RenderLayer::getGuiTextured, tex, mx, my, 0f, 0f, 45, 14, 45, 14);
        if (selected) {
            context.drawTexture(RenderLayer::getGuiTextured, MODE_SELECT, mx, my, 0f, 0f, 45, 14, 45, 14);
        }
        context.drawText(this.textRenderer, label, mx + 10, my + 3, selected ? 0xFFFFFF : 0x7C7C7C, false);
    }

    @Override
    protected void drawSlot(DrawContext context, Slot slot) {
        if (showingRewards) return;
        if (slot.id == 0) return;
        super.drawSlot(context, slot);
    }

    @Override
    public void close() {
        // Return saved deposits to player
        for (ItemStack stack : savedDeposits.values()) {
            if (!stack.isEmpty() && this.client.player != null) {
                if (!this.client.player.getInventory().insertStack(stack)) {
                    this.client.player.dropItem(stack, false);
                }
            }
        }
        savedDeposits.clear();
        super.close();
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        this.renderBackground(context, mouseX, mouseY, delta);
        super.render(context, mouseX, mouseY, delta);
        // Dessine le slot dépôt 27x27 PAR-DESSUS le handler slot 18x18
        if (tradeMode == 1 && hasSellMode && !showingRewards) {
            int x = (this.width - BG_W) / 2;
            int y = (this.height - BG_H) / 2;
            int slotSize = 27;
            int sx = x + 125, sy = y + 41;
            context.getMatrices().push();
            context.getMatrices().translate(sx, sy, 0);
            context.getMatrices().scale(1.5f, 1.5f, 1.0f);
            context.drawGuiTexture(RenderLayer::getGuiTextured, SLOT, 0, 0, 18, 18);
            context.getMatrices().pop();
            ItemStack depositStack = this.handler.getSlot(0).getStack();
            if (!depositStack.isEmpty()) {
                context.getMatrices().push();
                context.getMatrices().translate(sx + slotSize / 2, sy + slotSize / 2, 0);
                context.getMatrices().scale(1.5f, 1.5f, 1.0f);
                context.drawItemWithoutEntity(depositStack, -8, -8);
                context.getMatrices().pop();
            }
        }

        // Reward selection overlay
        if (showingRewards && tradeMode == 1 && hasSellMode) {
            int ox = (this.width - 176) / 2;
            int oy = (this.height - 176) / 2;
            context.fill(0, 0, this.width, this.height, 0xAA000000);
            context.drawTexture(RenderLayer::getGuiTextured, OVERLAY_BG, ox, oy, 0, 0, 176, 176, 176, 176);
            String npcName = "Gaspard";
            context.drawText(this.textRenderer, "§6Offre de " + npcName, ox + 8, oy + 5, 0x3C3C3C, false);
            var offers = com.dungeonmod.village.SellTradeRegistry.getOffers("gaspard");
            int rowW = 156, rowH = 21;
            int maxVisRows = 7;
            int visibleRows = Math.min(offers.size(), maxVisRows);
            for (int i = 0; i < visibleRows; i++) {
                int idx = i + rewardScrollOffset;
                if (idx >= offers.size()) break;
                var offer = offers.get(idx);
                int oy2 = oy + 20 + i * rowH;
                context.drawTexture(RenderLayer::getGuiTextured, OVERLAY_ROW, ox + 7, oy2, 0, 0, rowW, rowH, rowW, rowH);
                boolean hovered = mouseX >= ox + 5 && mouseX < ox + 5 + rowW && mouseY >= oy2 && mouseY < oy2 + rowH;
                if (hovered) context.fill(ox + 5, oy2, ox + 5 + rowW, oy2 + rowH, 0x44FFFFFF);
                context.drawItem(offer.rewardItem(), ox + 10, oy2 + 2);
                context.drawStackOverlay(this.textRenderer, offer.rewardItem(), ox + 10, oy2 + 2);
                String label = offer.rewardCount() + "x " + offer.rewardItem().getName().getString();
                context.drawText(this.textRenderer, label, ox + 32, oy2 + 4, 0x3C3C3C, false);
            }
            // Scrollbar
            int totalRows = offers.size();
            if (totalRows > maxVisRows) {
                int sbX = ox + 168;
                int sbY = oy + 18;
                int sbH = maxVisRows * rowH;
                int thumbH = Math.max(12, sbH * maxVisRows / totalRows);
                int thumbY = sbY + (sbH - thumbH) * rewardScrollOffset / (totalRows - maxVisRows);
                context.fill(sbX, sbY, sbX + 4, sbY + sbH, 0xFF444444);
                context.fill(sbX, thumbY, sbX + 4, thumbY + thumbH, 0xFFAAAAAA);
            }
        }

        this.drawMouseoverTooltip(context, mouseX, mouseY);
        renderCustomTooltips(context, mouseX, mouseY);
    }

    private void renderCustomTooltips(DrawContext context, int mouseX, int mouseY) {
        int x = (this.width - BG_W) / 2;
        int y = (this.height - BG_H) / 2;

        // Tooltip for disabled mode buttons
        if (this.title != null) {
            String name = this.title.getString().replaceAll("§.", "").trim();
            int mx2 = x + 4, my2 = y + 5;
            // Buy button tooltip
            if (!hasBuyMode && mouseX >= mx2 && mouseX < mx2 + 45 && mouseY >= my2 && mouseY < my2 + 14) {
                context.drawTooltip(this.textRenderer, Text.literal(name + " ne permet pas l'achat"), mouseX, mouseY);
                return;
            }
            // Sell button tooltip
            if (!hasSellMode && mouseX >= mx2 + 45 && mouseX < mx2 + 90 && mouseY >= my2 && mouseY < my2 + 14) {
                context.drawTooltip(this.textRenderer, Text.literal(name + " ne permet pas la vente"), mouseX, mouseY);
                return;
            }
        }

        // Sell mode tooltips (avant buy pour que ça marche même quand trades est null)
        if (tradeMode == 1 && hasSellMode) {
            int rowY2 = y + 32;
            for (int i = 0; i < premadeOffers.size(); i++) {
                boolean overIn = mouseX >= x + 10 && mouseX < x + 28 && mouseY >= rowY2 + 2 && mouseY < rowY2 + 18;
                boolean overOut = mouseX >= x + 53 && mouseX < x + 71 && mouseY >= rowY2 + 2 && mouseY < rowY2 + 18;
                if (overIn || overOut) {
                    var offer = premadeOffers.get(i);
                    ItemStack stack = overIn ? offer.requiredItems().get((animTick / 20) % offer.requiredItems().size()) : offer.rewardItem();
                    context.drawItemTooltip(this.textRenderer, stack, mouseX, mouseY);
                    return;
                }
                rowY2 += 22;
            }
            rowY2 += 14;
            int maxCustomVis3 = Math.max(1, 4 - premadeOffers.size());
            for (int i = 0; i < customOffers.size(); i++) {
                int idx2 = i - customOfferScroll;
                if (idx2 < 0 || idx2 >= maxCustomVis3) continue;
                boolean overIn = mouseX >= x + 10 && mouseX < x + 28 && mouseY >= rowY2 + 2 && mouseY < rowY2 + 18;
                boolean overOut = mouseX >= x + 53 && mouseX < x + 71 && mouseY >= rowY2 + 2 && mouseY < rowY2 + 18;
                if (overIn || overOut) {
                    ItemStack stack = overIn ? (!customOffers.get(i).requiredItems().isEmpty() ? customOffers.get(i).requiredItems().get(0) : ItemStack.EMPTY) : customOffers.get(i).rewardItem();
                    if (!stack.isEmpty()) context.drawItemTooltip(this.textRenderer, stack, mouseX, mouseY);
                    return;
                }
                rowY2 += 22;
            }
            // Tooltip for item choice overlay
            if (showingItemChoice && selectedOfferRow >= 0 && selectedOfferRow < premadeOffers.size()) {
                var offer = premadeOffers.get(selectedOfferRow);
                if (offer.requiredItems().size() > 1) {
                    int ox4 = x + 125 - (52 - 27) / 2;
                    int oy4 = y + 41 - 35;
                    for (int ci = 0; ci < offer.requiredItems().size() && ci < 2; ci++) {
                        ItemStack ciStack = offer.requiredItems().get(ci);
                        if (mouseX >= ox4 + 1 + ci * 26 && mouseX < ox4 + 1 + ci * 26 + 18 && mouseY >= oy4 + 7 && mouseY < oy4 + 7 + 18) {
                            if (!ciStack.isEmpty()) context.drawItemTooltip(this.textRenderer, ciStack, mouseX, mouseY);
                            return;
                        }
                    }
                }
            }
            // Tooltip for reward slot (slot 2 on right)
            int sx3 = x + 125 + 27 + 30;
            int sy3 = y + 41;
            if (selectedSellReward != null && mouseX >= sx3 && mouseX < sx3 + 27 && mouseY >= sy3 && mouseY < sy3 + 27) {
                context.drawItemTooltip(this.textRenderer, selectedSellReward.rewardItem(), mouseX, mouseY);
                return;
            }
            // Tooltip for sell button
            int btnX = x + 209, btnY = y + 44;
            if (mouseX >= btnX && mouseX < btnX + 30 && mouseY >= btnY && mouseY < btnY + 16) {
                ItemStack depTip = this.handler.getSlot(0).getStack();
                boolean itemOkTip = !depTip.isEmpty() && premadeOffers.stream().anyMatch(o -> o.matches(depTip));
                if (itemOkTip && selectedSellReward != null) {
                    context.drawTooltip(this.textRenderer, Text.literal("§aTroc possible"), mouseX, mouseY);
                } else if (!itemOkTip && !depTip.isEmpty()) {
                    context.drawTooltip(this.textRenderer, Text.literal("§cItem non accepté par Gaspard"), mouseX, mouseY);
                } else if (depTip.isEmpty()) {
                    context.drawTooltip(this.textRenderer, Text.literal("§cDéposez un item accepté"), mouseX, mouseY);
                } else {
                    context.drawTooltip(this.textRenderer, Text.literal("§cSélectionnez une récompense"), mouseX, mouseY);
                }
                return;
            }
            return;
        }

        if (trades == null) return;

        for (int i = 0; i < trades.size(); i++) {
            if (i == shrinkingIndex) continue;
            int ty = y + 22 + i * 20;
            TradeData td = trades.get(i);
            if (mouseX >= x + 10 && mouseX < x + 28 && mouseY >= ty + 2 && mouseY < ty + 18) {
                context.drawItemTooltip(this.textRenderer, td.input(), mouseX, mouseY);
                return;
            }
            if (mouseX >= x + 53 && mouseX < x + 71 && mouseY >= ty + 2 && mouseY < ty + 18) {
                context.drawItemTooltip(this.textRenderer, td.output(), mouseX, mouseY);
                return;
            }
        }

        if (selectedTrade >= 0 && selectedTrade < trades.size()) {
            int sx = x + 155;
            int sy = y + 12;
            if (mouseX >= sx && mouseX < sx + SLOT_LARGE && mouseY >= sy && mouseY < sy + SLOT_LARGE) {
                context.drawItemTooltip(this.textRenderer, trades.get(selectedTrade).output(), mouseX, mouseY);
                return;
            }
            // Tooltip du bouton Buy (au curseur)
            int bx = sx + SLOT_LARGE + 15;
            int by = sy + (SLOT_LARGE - 5) / 2;
            if (mouseX >= bx && mouseX < bx + 30 && mouseY >= by && mouseY < by + 16) {
                if (canAffordTrade(selectedTrade)) {
                    context.drawTooltip(this.textRenderer, Text.literal("§aAcheter"), mouseX, mouseY);
                } else {
                    String needed = trades.get(selectedTrade).input().getName().getString();
                    int total = trades.get(selectedTrade).input().getCount() * buyQuantity;
                    String plural = total >= 2 ? "s" : "";
                    context.drawTooltip(this.textRenderer, Text.literal("§cIl vous faut : " + total + " " + needed + plural), mouseX, mouseY);
                }
            }
        }
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        if (showingRewards) {
            var offers = com.dungeonmod.village.SellTradeRegistry.getOffers("gaspard");
            int maxOffset = Math.max(0, offers.size() - 7);
            if (verticalAmount > 0) rewardScrollOffset = Math.max(0, rewardScrollOffset - 1);
            else if (verticalAmount < 0) rewardScrollOffset = Math.min(maxOffset, rewardScrollOffset + 1);
            return true;
        }
        if (tradeMode == 1 && hasSellMode) {
            int maxCustomVis = Math.max(1, 4 - premadeOffers.size());
            int maxOff = Math.max(0, customOffers.size() - maxCustomVis);
            if (verticalAmount > 0) customOfferScroll = Math.max(0, customOfferScroll - 1);
            else if (verticalAmount < 0) customOfferScroll = Math.min(maxOff, customOfferScroll + 1);
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        // Reward selection overlay
        if (showingRewards) {
            int ox = (this.width - 176) / 2;
            int oy = (this.height - 176) / 2;
            var offers = com.dungeonmod.village.SellTradeRegistry.getOffers("gaspard");
            int rowW = 156, rowH = 21;
            int maxVisRows = 7;
            int visibleRows = Math.min(offers.size(), maxVisRows);
            for (int i = 0; i < visibleRows; i++) {
                int idx = i + rewardScrollOffset;
                if (idx >= offers.size()) break;
                int oy2 = oy + 20 + i * rowH;
                if (mouseX >= ox + 7 && mouseX < ox + 7 + rowW && mouseY >= oy2 && mouseY < oy2 + rowH) {
                    selectedSellReward = offers.get(idx);
                    showingRewards = false;
                    return true;
                }
            }
            showingRewards = false;
            return true;
        }

        if (button == 0 && trades != null) {
            int x = (this.width - BG_W) / 2;
            int y = (this.height - BG_H) / 2;

            int mx = x + 4, my = y + 5;
            // Clic sur Achat
            if (mouseX >= mx && mouseX < mx + 45 && mouseY >= my && mouseY < my + 14) {
                if (hasBuyMode) { tradeMode = 0; }
                return true;
            }
            // Clic sur Vente
            if (mouseX >= mx + 45 && mouseX < mx + 90 && mouseY >= my && mouseY < my + 14) {
                if (hasSellMode) { tradeMode = 1; }
                return true;
            }
            // Mode SELL
            if (tradeMode == 1 && hasSellMode) {
                int rowH2 = 22;
                int rowY2 = y + 32;

                // Premade offers click
                for (int i = 0; i < premadeOffers.size(); i++) {
                    if (mouseX >= x + 5 && mouseX < x + 92 && mouseY >= rowY2 && mouseY < rowY2 + rowH2) {
                        switchToOffer(i);
                        return true;
                    }
                    rowY2 += rowH2;
                }

                // Skip separator
                rowY2 += 14;

                // Custom offers click
                int maxCustomVis2 = Math.max(1, 4 - premadeOffers.size());
                for (int i = 0; i < customOffers.size(); i++) {
                    int idx = i - customOfferScroll;
                    if (idx < 0 || idx >= maxCustomVis2) continue;
                    int absIdx = premadeOffers.size() + i;
                    // Cross button click (delete)
                    if (mouseX >= x + 76 && mouseX < x + 96 && mouseY >= rowY2 + 1 && mouseY < rowY2 + 21) {
                        customOffers.remove(i);
                        if (selectedOfferRow == absIdx) {
                            switchToOffer(-1);
                        }
                        return true;
                    }
                    if (mouseX >= x + 5 && mouseX < x + 92 && mouseY >= rowY2 && mouseY < rowY2 + rowH2) {
                        switchToOffer(absIdx);
                        return true;
                    }
                    rowY2 += rowH2;
                }

                // Scrollbar click
                int maxCustomVis4 = Math.max(1, 4 - premadeOffers.size());
                if (customOffers.size() > maxCustomVis4) {
                    int sbX = x + 94;
                    int sbY2 = y + 32 + premadeOffers.size() * 22 + 14 + (premadeOffers.size() > 0 ? 0 : 0);
                    int sbH2 = maxCustomVis4 * 22;
                    if (mouseX >= sbX && mouseX < sbX + 6 && mouseY >= sbY2 && mouseY < sbY2 + sbH2) {
                        int relY = (int)(mouseY - sbY2);
                        int maxOff2 = Math.max(0, customOffers.size() - maxCustomVis4);
                        customOfferScroll = relY * maxOff2 / sbH2;
                        return true;
                    }
                }

                // trade_son_offre button click
                int btnOffreY2 = rowY2 + 4;
                if (mouseX >= x + 5 && mouseX < x + 92 && mouseY >= btnOffreY2 && mouseY < btnOffreY2 + 22) {
                    switchToOffer(-1);
                    customOffers.add(new SellTradeRegistry.SellOffer(java.util.List.of(), 1, ItemStack.EMPTY, 1));
                    return true;
                }

                // Item choice overlay click
                if (showingItemChoice && selectedOfferRow >= 0 && selectedOfferRow < premadeOffers.size()) {
                    var offer = premadeOffers.get(selectedOfferRow);
                    if (offer.requiredItems().size() > 1) {
                        int slotSize4 = 27;
                        int sx4 = x + 125;
                        int sy4 = y + 36;
                        int ox3 = sx4 - (52 - slotSize4) / 2;
                        int oy3 = sy4 - 35;
                        int slotW3 = 26;
                        for (int ci = 0; ci < offer.requiredItems().size() && ci < 2; ci++) {
                            ItemStack ciStack = offer.requiredItems().get(ci);
                            if (mouseX >= ox3 + 1 + ci * slotW3 && mouseX < ox3 + 1 + ci * slotW3 + 18 && mouseY >= oy3 + 7 && mouseY < oy3 + 7 + 18) {
                                if (this.client.player != null && hasItemInInventory(ciStack)) {
                                    ItemStack cur = this.handler.getSlot(0).getStack();
                                    if (!cur.isEmpty()) this.client.player.getInventory().offerOrDrop(cur);
                                    var inv = this.client.player.getInventory();
                                    for (int si = 0; si < inv.size(); si++) {
                                        ItemStack s = inv.getStack(si);
                                        if (!s.isEmpty() && s.isOf(ciStack.getItem()) && s.getName().getString().equals(ciStack.getName().getString())) {
                                            this.handler.getSlot(0).setStack(s.split(1));
                                            if (s.isEmpty()) inv.setStack(si, ItemStack.EMPTY);
                                            break;
                                        }
                                    }
                                    savedDeposits.put(selectedOfferRow, this.handler.getSlot(0).getStack().copy());
                                }
                                showingItemChoice = false;
                                return true;
                            }
                        }
                    }
                    showingItemChoice = false;
                    return true;
                }

                // Click on deposit slot for premade → show choice overlay
                if (selectedOfferRow >= 0 && selectedOfferRow < premadeOffers.size()) {
                    int dvx = x + 125, dvy = y + 36;
                    if (mouseX >= dvx && mouseX < dvx + 27 && mouseY >= dvy && mouseY < dvy + 27) {
                        var offer = premadeOffers.get(selectedOfferRow);
                        if (offer.requiredItems().size() > 1) {
                            showingItemChoice = !showingItemChoice;
                            return true;
                        }
                        return true; // block manual removal
                    }
                }

                int slotSize = 27;
                int sx = x + 125, sy2 = y + 36;

                int rx = sx + slotSize + 20;
                if (mouseX >= rx && mouseX < rx + slotSize && mouseY >= sy2 && mouseY < sy2 + slotSize) {
                    showingRewards = true;
                    return true;
                }
                ItemStack depCheck = this.handler.getSlot(0).getStack();
                boolean itemOk2 = !depCheck.isEmpty() && premadeOffers.stream().anyMatch(o -> o.matches(depCheck));
                boolean canSell2 = itemOk2 && selectedSellReward != null;
                int bx = rx + slotSize + 10;
                int by2 = sy2 + (slotSize - 11) / 2;
                if (mouseX >= bx && mouseX < bx + 30 && mouseY >= by2 && mouseY < by2 + 16 && canSell2) {
                    clickAnimTimer = 3;
                    ClientPlayNetworking.send(new CyclopsSellPayload(this.handler.syncId, selectedOfferRow));
                    this.handler.getSlot(0).setStack(ItemStack.EMPTY);
                    if (selectedOfferRow >= 0 && selectedOfferRow < premadeOffers.size()) {
                        savedDeposits.remove(selectedOfferRow);
                        selectedOfferRow = -1;
                    }
                    selectedSellReward = null;
                    return true;
                }
                return super.mouseClicked(mouseX, mouseY, button);
            }

            for (int i = 0; i < trades.size(); i++) {
                int ty = y + 22 + i * 20;
                if (mouseX >= x + 5 && mouseX < x + 92 && mouseY >= ty && mouseY < ty + 22) {
                    selectedTrade = i;
                    handler.selectedTrade = i;
                    rowClickTimer = 3;
                    return true;
                }
            }

            if (selectedTrade >= 0) {
                if (mouseX >= x + 140 && mouseX < x + 260 && mouseY >= y + 5 && mouseY < y + 90) {
                    int sx = x + 155;
                    int sy = y + 12;
                    if (hasQuantitySelector) {
                        int qy = sy + SLOT_LARGE + 2;
                        if (mouseX >= sx + 2 && mouseX < sx + 15 && mouseY >= qy && mouseY < qy + 9) {
                            if (buyQuantity > 1) buyQuantity--; return true;
                        }
                        if (mouseX >= sx + 37 && mouseX < sx + 50 && mouseY >= qy && mouseY < qy + 9) {
                            if (buyQuantity < 8) buyQuantity++; return true;
                        }
                    }
                    clickAnimTimer = 3;
                    if (canAffordTrade(selectedTrade)) {
                        int origIdx = trades.get(selectedTrade).originalIndex();
                        ClientPlayNetworking.send(new CyclopsBuyPayload(this.handler.syncId, origIdx, buyQuantity));
                        shrinkingIndex = selectedTrade;
                        shrinkTimer = SHRINK_DURATION;
                    }
                    return true;
                }
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }
}
