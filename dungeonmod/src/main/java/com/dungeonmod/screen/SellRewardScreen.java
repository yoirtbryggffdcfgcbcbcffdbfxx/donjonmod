package com.dungeonmod.screen;

import com.dungeonmod.village.SellTradeRegistry;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

import java.util.List;
import java.util.function.Consumer;

public class SellRewardScreen extends Screen {

    private static final Identifier BG = Identifier.of("dungeonmod", "textures/gui/sac_interface.png");
    private static final int BG_W = 176;
    private static final int BG_H = 96;

    private final String npcId;
    private final List<SellTradeRegistry.SellOffer> offers;
    private final Consumer<SellTradeRegistry.SellOffer> onSelect;
    private SellTradeRegistry.SellOffer selectedOffer;
    private int quantity = 1;

    public SellRewardScreen(String npcName, String npcId, List<SellTradeRegistry.SellOffer> offers, Consumer<SellTradeRegistry.SellOffer> onSelect) {
        super(Text.literal("§6Offre de " + npcName));
        this.npcId = npcId;
        this.offers = offers;
        this.onSelect = onSelect;
    }

    @Override
    protected void init() {
        super.init();
    }

    @Override
    public void renderBackground(DrawContext context, int mouseX, int mouseY, float delta) {
        context.fill(0, 0, this.width, this.height, 0x88000000);
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        this.renderBackground(context, mouseX, mouseY, delta);
        int x = (this.width - BG_W) / 2;
        int y = (this.height - BG_H) / 2;
        context.drawTexture(RenderLayer::getGuiTextured, BG, x, y, 0, 0, BG_W, BG_H, 176, 176);

        context.drawText(this.textRenderer, this.title, x + 8, y + 6, 0x3C3C3C, false);

        for (int i = 0; i < offers.size(); i++) {
            int oy = y + 20 + i * 22;
            var offer = offers.get(i);
            boolean hovered = mouseX >= x + 10 && mouseX < x + 160 && mouseY >= oy && mouseY < oy + 20;
            if (hovered) context.fill(x + 10, oy, x + 160, oy + 20, 0x33FFFFFF);
            if (offer == selectedOffer) context.fill(x + 10, oy, x + 160, oy + 20, 0x55FFFFFF);

            context.drawItem(offer.rewardItem(), x + 14, oy + 2);
            context.drawStackOverlay(this.textRenderer, offer.rewardItem(), x + 14, oy + 2);
            String label = offer.rewardCount() + "x " + offer.rewardItem().getName().getString();
            context.drawText(this.textRenderer, label, x + 36, oy + 5, 0x3C3C3C, false);
        }
        super.render(context, mouseX, mouseY, delta);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        int x = (this.width - BG_W) / 2;
        int y = (this.height - BG_H) / 2;
        for (int i = 0; i < offers.size(); i++) {
            int oy = y + 20 + i * 22;
            if (mouseX >= x + 10 && mouseX < x + 160 && mouseY >= oy && mouseY < oy + 20) {
                selectedOffer = offers.get(i);
                onSelect.accept(selectedOffer);
                this.close();
                return true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }
}
