package com.dungeonmod.mixin;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.entity.LivingEntityRenderer;
import net.minecraft.client.render.entity.state.LivingEntityRenderState;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.mob.Monster;
import net.minecraft.item.Items;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LivingEntityRenderer.class)
public class LivingEntityRendererMixin {

    @Inject(method = "updateRenderState(Lnet/minecraft/entity/LivingEntity;Lnet/minecraft/client/render/entity/state/LivingEntityRenderState;F)V", at = @At("TAIL"))
    private void onUpdateRenderState(LivingEntity entity, LivingEntityRenderState state, float tickDelta, CallbackInfo ci) {
        if (!(entity instanceof Monster)) return;

        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null) return;

        // Always strip old health/damage suffix to get base name
        String displayName = entity.getDisplayName().getString();
        int idx = displayName.indexOf(" \u00a7c");
        if (idx >= 0) displayName = displayName.substring(0, idx);

        var helmet = client.player.getInventory().getArmorStack(3);
        boolean canSee = !helmet.isEmpty() && helmet.isOf(Items.CHAINMAIL_HELMET)
            && helmet.contains(net.minecraft.component.DataComponentTypes.CUSTOM_NAME)
            && helmet.get(net.minecraft.component.DataComponentTypes.CUSTOM_NAME).getString().contains("Casque du chasseur")
            && client.player.squaredDistanceTo(entity) <= 25.0;

        if (canSee) {
            float hp = entity.getHealth();
            float dmg = 1;
            if (entity.hasCustomName() && entity.getCustomName().getString().contains("Gobelin")) {
                dmg = 2;
            } else {
                var dmgAttr = entity.getAttributeInstance(net.minecraft.entity.attribute.EntityAttributes.ATTACK_DAMAGE);
                if (dmgAttr != null) dmg = (float)dmgAttr.getValue();
            }
            entity.setCustomName(Text.literal(displayName + " \u00a7c" + String.format("%.0f", hp / 2.0f) + "\u2764 \u00a7e" + String.format("%.1f", dmg / 2.0f) + "\u2764"));
            entity.setCustomNameVisible(true);
        } else if (client.player.squaredDistanceTo(entity) <= 25.0) {
            entity.setCustomName(Text.literal(displayName));
            entity.setCustomNameVisible(true);
        } else {
            if (entity.hasCustomName()) {
                entity.setCustomName(Text.literal(displayName));
            }
            entity.setCustomNameVisible(false);
        }
    }
}
