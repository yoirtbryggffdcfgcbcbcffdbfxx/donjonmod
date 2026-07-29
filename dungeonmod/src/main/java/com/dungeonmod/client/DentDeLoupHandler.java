package com.dungeonmod.client;

import com.dungeonmod.DungeonMod;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.Box;

public class DentDeLoupHandler {

    public static void init() {
        // HUD : protection + force
        HudRenderCallback.EVENT.register((drawContext, tickDelta) -> {
            MinecraftClient client = MinecraftClient.getInstance();
            PlayerEntity player = client.player;
            if (player == null) return;

            boolean hasDent = DungeonMod.isDentDeLoup(player.getMainHandStack())
                || DungeonMod.isDentDeLoup(player.getOffHandStack());
            if (!hasDent) return;

            // Protection réelle = attribut ARMOR (1 pt = 1 %), cap vanilla levé à 100
            // via ArmorAttributeCapMixin. getArmor() tronque en int — on lit le double.
            double armorPct = player.getAttributeValue(net.minecraft.entity.attribute.EntityAttributes.ARMOR);

            double strengthPercent = 0;
            var strength = player.getStatusEffect(StatusEffects.STRENGTH);
            if (strength != null) {
                strengthPercent = (strength.getAmplifier() + 1) * 50.0;
            }
            // Bonus des bières + armures (additif, via BeerStrengthData)
            float beerMult = com.dungeonmod.util.BeerStrengthData.getMultiplier(player);
            double beerPercent = (beerMult - 1.0f) * 100.0;
            // Attaque naturelle : 100 % (main nue) + force potion + bonus armure/bière
            double totalPercent = 100.0 + strengthPercent + beerPercent;

            int windowHeight = client.getWindow().getScaledHeight();
            int x = 4;
            int y = windowHeight - 30;
            int color = 0xFFFFFF;

            drawContext.getMatrices().scale(0.8f, 0.8f, 0.8f);
            drawContext.drawText(client.textRenderer,
                "§7Protection: §a" + String.format("%.0f", armorPct) + "%",
                (int)(x / 0.8f), (int)(y / 0.8f), color, true);
            drawContext.drawText(client.textRenderer,
                "§7Force: §e" + String.format("%.0f", totalPercent) + "%",
                (int)(x / 0.8f), (int)((y + 10) / 0.8f), color, true);
            drawContext.getMatrices().scale(1.25f, 1.25f, 1.25f);
        });

        // Le contour des ennemis est géré côté serveur dans DungeonMod.ServerTickEvents
    }
}
