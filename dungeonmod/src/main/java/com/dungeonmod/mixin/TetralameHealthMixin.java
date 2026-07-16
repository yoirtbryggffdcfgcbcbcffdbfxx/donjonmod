package com.dungeonmod.mixin;

import com.dungeonmod.DungeonMod;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.NbtCompound;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PlayerEntity.class)
public class TetralameHealthMixin {

    @Inject(method = "writeCustomDataToNbt", at = @At("TAIL"))
    private void onWriteNbt(NbtCompound nbt, CallbackInfo ci) {
        PlayerEntity player = (PlayerEntity)(Object)this;
        Double saved = DungeonMod.tetralameSavedHealth.get(player.getUuid());
        if (saved != null) {
            nbt.putDouble("dungeonmod_tetralame_health", saved);
        }
    }

    @Inject(method = "readCustomDataFromNbt", at = @At("TAIL"))
    private void onReadNbt(NbtCompound nbt, CallbackInfo ci) {
        if (nbt.contains("dungeonmod_tetralame_health")) {
            PlayerEntity player = (PlayerEntity)(Object)this;
            double saved = nbt.getDouble("dungeonmod_tetralame_health");
            DungeonMod.tetralameSavedHealth.put(player.getUuid(), saved);
        }
    }
}
