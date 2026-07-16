package com.dungeonmod.screen;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

public class SacScreen extends HandledScreen<SacScreenHandler> {

    private static final Identifier SLOT_TEXTURE = Identifier.of("container/slot");

    public SacScreen(SacScreenHandler handler, PlayerInventory inventory, Text title) {
        super(handler, inventory, title);
        this.backgroundWidth = 176;
        this.backgroundHeight = 133;
        this.playerInventoryTitleY = 40;
    }

    @Override
    protected void init() {
        super.init();
        this.titleX = this.backgroundWidth - this.textRenderer.getWidth(this.title) / 2 - 160;
        this.titleY = 6;
    }

    @Override
    protected void drawBackground(DrawContext context, float delta, int mouseX, int mouseY) {
        int x = (this.width - this.backgroundWidth) / 2;
        int y = (this.height - this.backgroundHeight) / 2;

        context.fill(x, y, x + this.backgroundWidth, y + this.backgroundHeight, 0xFFC6C6C6);

        for (int c = 0; c < 4; c++) {
            context.drawGuiTexture(RenderLayer::getGuiTextured, SLOT_TEXTURE, x + 48 + c * 18, y + 18, 18, 18);
        }

        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                context.drawGuiTexture(RenderLayer::getGuiTextured, SLOT_TEXTURE, x + 7 + col * 18, y + 52 + row * 18, 18, 18);
            }
        }

        for (int col = 0; col < 9; col++) {
            context.drawGuiTexture(RenderLayer::getGuiTextured, SLOT_TEXTURE, x + 7 + col * 18, y + 110, 18, 18);
        }
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        this.renderBackground(context, mouseX, mouseY, delta);
        super.render(context, mouseX, mouseY, delta);
        this.drawMouseoverTooltip(context, mouseX, mouseY);
    }
}

