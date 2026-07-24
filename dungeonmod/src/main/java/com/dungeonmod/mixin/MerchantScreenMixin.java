package com.dungeonmod.mixin;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.MerchantScreen;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.util.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MerchantScreen.class)
public class MerchantScreenMixin {

    private static final Identifier SLOT = Identifier.of("container/slot");
    private static final int BG_W = 276;
    private static final int BG_H = 168;

    @Inject(method = "drawBackground", at = @At("TAIL"))
    private void drawTradeSlots(DrawContext context, float delta, int mouseX, int mouseY, CallbackInfo ci) {
        MerchantScreen self = (MerchantScreen)(Object)this;
        if (!self.getTitle().getString().contains("Cyclope")) return;
        int x = (self.width - BG_W) / 2;
        int y = (self.height - BG_H) / 2;
        context.drawTexture(RenderLayer::getGuiTextured, SLOT, x + 136, y + 47, 0, 0, 18, 18, 18, 18);
        context.drawTexture(RenderLayer::getGuiTextured, SLOT, x + 116, y + 47, 0, 0, 18, 18, 18, 18);
        context.drawTexture(RenderLayer::getGuiTextured, SLOT, x + 174, y + 47, 0, 0, 18, 18, 18, 18);
    }
}
