package com.dungeonmod.mixin;

import com.dungeonmod.DungeonMod;
import com.dungeonmod.item.SacItem;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.ArrayList;
import java.util.List;

@Mixin(ServerPlayerEntity.class)
public class SacKeepMixin {

    @Inject(method = "onDeath", at = @At("HEAD"))
    private void onDeathSaveSac(DamageSource source, CallbackInfo ci) {
        ServerPlayerEntity player = (ServerPlayerEntity)(Object)this;
        List<ItemStack> sacs = new ArrayList<>();
        var inv = player.getInventory();
        for (int i = 0; i < inv.main.size(); i++) {
            ItemStack stack = inv.main.get(i);
            if (stack.getItem() instanceof SacItem) {
                sacs.add(stack.copy());
                inv.main.set(i, ItemStack.EMPTY);
            }
        }
        if (!sacs.isEmpty()) {
            DungeonMod.LOGGER.info("[Sac] Saved and removed {} sac(s) before drop", sacs.size());
            DungeonMod.sacBackup.put(player.getUuid(), sacs);
        }
    }
}
