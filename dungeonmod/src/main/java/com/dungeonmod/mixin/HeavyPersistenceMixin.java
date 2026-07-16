package com.dungeonmod.mixin;

import com.dungeonmod.DungeonMod;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.NbtCompound;
import java.util.UUID;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PlayerEntity.class)
public class HeavyPersistenceMixin {

    @Inject(method = "writeCustomDataToNbt", at = @At("HEAD"))
    private void onWrite(NbtCompound nbt, CallbackInfo ci) {
        PlayerEntity self = (PlayerEntity)(Object)this;
        UUID uuid = self.getUuid();
        var pieces = DungeonMod.getHeavyClaimed(uuid);
        if (pieces != null && !pieces.isEmpty()) {
            NbtCompound heavy = new NbtCompound();
            for (String p : pieces) {
                heavy.putBoolean(p, true);
            }
            nbt.put("dungeonmod_heavy", heavy);
            nbt.putFloat("dungeonmod_heavy_absorption", self.getAbsorptionAmount());
            // Sauvegarder les stored values
            NbtCompound storedNbt = new NbtCompound();
            var stored = DungeonMod.getHeavyStored(uuid);
            if (stored != null) {
                for (var e : stored.entrySet()) {
                    storedNbt.putFloat(e.getKey(), e.getValue());
                }
            }
            nbt.put("dungeonmod_heavy_stored", storedNbt);
        }
    }

    @Inject(method = "readCustomDataFromNbt", at = @At("TAIL"))
    private void onRead(NbtCompound nbt, CallbackInfo ci) {
        PlayerEntity self = (PlayerEntity)(Object)this;
        UUID uuid = self.getUuid();
        if (nbt.contains("dungeonmod_heavy")) {
            NbtCompound heavy = nbt.getCompound("dungeonmod_heavy");
            var pieces = DungeonMod.getOrCreateHeavyClaimed(uuid);
            for (String key : heavy.getKeys()) {
                if (heavy.getBoolean(key)) {
                    pieces.add(key);
                }
            }
            // Restaurer les stored values
            if (nbt.contains("dungeonmod_heavy_stored")) {
                NbtCompound storedNbt = nbt.getCompound("dungeonmod_heavy_stored");
                var stored = DungeonMod.getOrCreateHeavyStored(uuid);
                for (String key : storedNbt.getKeys()) {
                    stored.put(key, storedNbt.getFloat(key));
                }
            }
            if (nbt.contains("dungeonmod_heavy_absorption")) {
                self.setAbsorptionAmount(nbt.getFloat("dungeonmod_heavy_absorption"));
            }
        }
    }
}
