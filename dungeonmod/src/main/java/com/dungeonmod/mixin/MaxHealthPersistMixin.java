package com.dungeonmod.mixin;

import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.network.ServerPlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerPlayerEntity.class)
public class MaxHealthPersistMixin {

    @Inject(method = "writeCustomDataToNbt", at = @At("TAIL"))
    private void onWriteNbt(NbtCompound nbt, CallbackInfo ci) {
        ServerPlayerEntity player = (ServerPlayerEntity)(Object)this;
        var attr = player.getAttributeInstance(EntityAttributes.MAX_HEALTH);
        if (attr != null) {
            nbt.putDouble("dungeonmod_max_health", attr.getBaseValue());
        }
        nbt.putBoolean("dungeonmod_in_dungeon", com.dungeonmod.DungeonMod.getHealthSet().contains(player.getUuid()));
    }

    @Inject(method = "readCustomDataFromNbt", at = @At("TAIL"))
    private void onReadNbt(NbtCompound nbt, CallbackInfo ci) {
        if (!nbt.contains("dungeonmod_max_health")) return;
        ServerPlayerEntity player = (ServerPlayerEntity)(Object)this;
        var attr = player.getAttributeInstance(EntityAttributes.MAX_HEALTH);
        if (attr != null) {
            attr.setBaseValue(nbt.getDouble("dungeonmod_max_health"));
        }
        // Restaurer l'état dans le donjon pour éviter le reset à la reconnexion
        if (nbt.getBoolean("dungeonmod_in_dungeon")) {
            com.dungeonmod.DungeonMod.getHealthSet().add(player.getUuid());
        }
    }
}
