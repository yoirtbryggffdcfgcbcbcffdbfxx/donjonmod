package com.dungeonmod.mixin;

import com.dungeonmod.DungeonMod;
import com.dungeonmod.util.DungeonLoot;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.mob.ZombieEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.Vec3d;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LivingEntity.class)
public class ZombieGoblinLootMixin {

    static {
        System.out.println("[GoblinLoot] Mixin charge (ZombieGoblinLootMixin)");
    }

    @Inject(method = "onDeath(Lnet/minecraft/entity/damage/DamageSource;)V", at = @At("HEAD"))
    private void onDeathDropGoblinLoot(DamageSource source, CallbackInfo ci) {
        LivingEntity entity = (LivingEntity)(Object)this;
        if (!(entity instanceof ZombieEntity zombie)) return;
        boolean isCustom = DungeonMod.customZombies.contains(zombie.getUuid());
        System.out.println("[GoblinLoot] onDeath uuid=" + zombie.getUuid() + " isCustom=" + isCustom);
        if (!zombie.getWorld().isClient() && isCustom) {
            var items = DungeonLoot.rollMobLoot(new java.util.Random());
            System.out.println("[GoblinLoot] drops " + items.size() + " item(s)");
            if (items.isEmpty()) return;
            Vec3d pos = zombie.getPos();
            for (ItemStack stack : items) {
                ItemEntity item = new ItemEntity(zombie.getWorld(),
                    pos.x, pos.y + 0.5, pos.z, stack);
                item.setVelocity(
                    (zombie.getWorld().random.nextDouble() - 0.5) * 0.2,
                    0.2,
                    (zombie.getWorld().random.nextDouble() - 0.5) * 0.2);
                zombie.getWorld().spawnEntity(item);
            }
        }
    }
}
