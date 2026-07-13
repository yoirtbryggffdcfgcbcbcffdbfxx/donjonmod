package com.dungeonmod.mixin;

import com.dungeonmod.DungeonMod;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityPose;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Items;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Mixin(Entity.class)
public class LivingEntityPoseMixin {

    @Unique
    private static final Map<UUID, Boolean> wasProne = new ConcurrentHashMap<>();

    @Inject(method = "getPose", at = @At("RETURN"), cancellable = true)
    private void forcePronePose(CallbackInfoReturnable<EntityPose> cir) {
        Entity self = (Entity) (Object) this;
        if (!(self instanceof PlayerEntity player)) return;

        var legs = player.getInventory().getArmorStack(1);
        if (legs.isEmpty() || !legs.isOf(Items.CHAINMAIL_LEGGINGS)) return;
        if (!legs.contains(net.minecraft.component.DataComponentTypes.CUSTOM_NAME)) return;
        if (!legs.get(net.minecraft.component.DataComponentTypes.CUSTOM_NAME).getString().contains("Jambière du chasseur")) return;

        UUID uuid = player.getUuid();

        if (!player.isSneaking()) {
            // Set cooldown ONLY if was actually prone (was camouflaged)
            if (Boolean.TRUE.equals(wasProne.remove(uuid))) {
                DungeonMod.hunterCooldown.put(uuid, System.currentTimeMillis());
            }
            return;
        }

        // Check cooldown
        Long start = DungeonMod.hunterCooldown.get(uuid);
        if (start != null && System.currentTimeMillis() - start < 2000) {
            wasProne.remove(uuid);
            return;
        }

        wasProne.put(uuid, true);
        cir.setReturnValue(EntityPose.SWIMMING);
    }
}
