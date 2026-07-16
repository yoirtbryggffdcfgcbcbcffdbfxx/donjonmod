package com.dungeonmod.mixin;

import com.dungeonmod.DungeonMod;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.util.math.BlockPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PlayerEntity.class)
public class AnchorSpawnMixin {

    @Inject(method = "writeCustomDataToNbt", at = @At("TAIL"))
    private void onWriteNbt(NbtCompound nbt, CallbackInfo ci) {
        PlayerEntity player = (PlayerEntity)(Object)this;
        BlockPos anchorPos = DungeonMod.lastAnchorSpawn.get(player.getUuid());
        if (anchorPos != null) {
            NbtCompound anchor = new NbtCompound();
            anchor.putInt("x", anchorPos.getX());
            anchor.putInt("y", anchorPos.getY());
            anchor.putInt("z", anchorPos.getZ());
            nbt.put("dungeonmod_anchor_spawn", anchor);
        }
    }

    @Inject(method = "readCustomDataFromNbt", at = @At("TAIL"))
    private void onReadNbt(NbtCompound nbt, CallbackInfo ci) {
        if (nbt.contains("dungeonmod_anchor_spawn")) {
            NbtCompound anchor = nbt.getCompound("dungeonmod_anchor_spawn");
            BlockPos pos = new BlockPos(anchor.getInt("x"), anchor.getInt("y"), anchor.getInt("z"));
            PlayerEntity player = (PlayerEntity)(Object)this;
            DungeonMod.lastAnchorSpawn.put(player.getUuid(), pos);
        }
    }
}
