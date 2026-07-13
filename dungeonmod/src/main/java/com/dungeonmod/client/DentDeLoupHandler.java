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

            int armor = player.getArmor();
            float armorToughness = (float) player.getAttributeValue(net.minecraft.entity.attribute.EntityAttributes.ARMOR_TOUGHNESS);

            double strengthPercent = 0;
            var strength = player.getStatusEffect(StatusEffects.STRENGTH);
            if (strength != null) {
                strengthPercent = (strength.getAmplifier() + 1) * 50.0;
            }
            // Bonus des bières (additif)
            float beerMult = com.dungeonmod.util.BeerStrengthData.getMultiplier(player);
            double beerPercent = (beerMult - 1.0f) * 100.0;
            // Attaque naturelle : 1.0 (main nue), chaque amplification de force = +50%
            double totalPercent = 100.0 + strengthPercent + beerPercent;

            int windowHeight = client.getWindow().getScaledHeight();
            int x = 4;
            int y = windowHeight - 30;
            int color = 0xFFFFFF;

            drawContext.getMatrices().scale(0.8f, 0.8f, 0.8f);
            drawContext.drawText(client.textRenderer,
                "§7Protection: §a" + armor + " §7(§8" + String.format("%.0f", armorToughness) + "§7)",
                (int)(x / 0.8f), (int)(y / 0.8f), color, true);
            drawContext.drawText(client.textRenderer,
                "§7Force: §e" + String.format("%.0f", totalPercent) + "%",
                (int)(x / 0.8f), (int)((y + 10) / 0.8f), color, true);
            drawContext.getMatrices().scale(1.25f, 1.25f, 1.25f);
        });

        // Le contour des ennemis est géré côté serveur dans DungeonMod.ServerTickEvents
    }
}
