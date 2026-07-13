package com.dungeonmod.mixin;

import com.dungeonmod.util.LanceHelper;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.projectile.ProjectileEntity;
import net.minecraft.entity.projectile.thrown.SnowballEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.Direction;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ProjectileEntity.class)
public class LanceBlockHitMixin {

    @Inject(method = "onBlockHit", at = @At("HEAD"))
    private void onBlockHit(BlockHitResult blockHitResult, CallbackInfo ci) {
        if (!(((Object) this) instanceof SnowballEntity snowball)) return;
        var stack = snowball.getStack();
        if (!LanceHelper.isLance(stack)) return;
        if (!(snowball.getWorld() instanceof ServerWorld sw)) return;

        Direction side = blockHitResult.getSide();
        double x = blockHitResult.getPos().x + side.getOffsetX() * 0.5;
        double y = blockHitResult.getPos().y + side.getOffsetY() * 0.5;
        double z = blockHitResult.getPos().z + side.getOffsetZ() * 0.5;

        ItemStack drop = stack.copy();
        drop.setCount(1);
        ItemEntity item = new ItemEntity(sw, x, y, z, drop);
        sw.spawnEntity(item);
    }
}
