package com.dungeonmod.mixin;

import com.dungeonmod.DungeonMod;
import com.dungeonmod.util.BeerStrengthData;
import com.dungeonmod.util.FlecheComboData;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.thrown.SnowballEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.hit.EntityHitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(SnowballEntity.class)
public class SnowballDamageMixin {

    @Inject(method = "onEntityHit", at = @At("HEAD"))
    private void onHit(EntityHitResult entityHitResult, CallbackInfo ci) {
        SnowballEntity snowball = (SnowballEntity) (Object) this;
        var stack = snowball.getStack();
        if (!stack.isOf(Items.STICK) && !stack.isOf(Items.BONE) && !stack.isOf(Items.ARROW) && !stack.isOf(Items.FLINT) && !stack.isOf(Items.FIRE_CHARGE) && !stack.isOf(Items.SNOWBALL) && !stack.isOf(Items.ENDER_PEARL)) return;
        if (!stack.contains(net.minecraft.component.DataComponentTypes.CUSTOM_NAME)) return;
        String name = stack.get(net.minecraft.component.DataComponentTypes.CUSTOM_NAME).getString();

        if (!(snowball.getWorld() instanceof ServerWorld sw)) return;
        Entity owner = snowball.getOwner();
        if (owner == null) return;
        if (!(entityHitResult.getEntity() instanceof LivingEntity target)) return;

        if (name.contains("Bâton")) {
            target.damage(sw, owner.getDamageSources().thrown(snowball, owner), 2.0f);
        } else if (name.contains("Os")) {
            target.damage(sw, owner.getDamageSources().thrown(snowball, owner), 5.0f);
            target.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS, 40, 255, false, true, true));
            DungeonMod.stunnedEntities.add(target.getUuid());
        } else if (name.contains("Torche")) {
            float mult = 1.0f;
            if (owner instanceof PlayerEntity p) mult = BeerStrengthData.getMultiplier(p);
            int fireTime = Math.max(1, Math.round(3.0f * mult));
            target.setOnFireFor(fireTime);
        } else if (name.contains("Dague")) {
            float dmg = snowball.hasNoGravity() ? 10.0f : 5.0f;
            target.damage(sw, owner.getDamageSources().thrown(snowball, owner), dmg);
        } else if (name.contains("Flèche")) {
            int level = FlecheComboData.getSangLevel(stack);
            float dmg = switch (level) {
                case 2 -> 1.0f;
                case 1 -> 1.0f;
                default -> 1.0f;
            };
            target.damage(sw, owner.getDamageSources().thrown(snowball, owner), dmg);
            if (level == 2 && !target.hasStatusEffect(StatusEffects.WITHER)) {
                target.addStatusEffect(new StatusEffectInstance(StatusEffects.WITHER, 60, 1, false, false, true));
            }
            var vel = snowball.getVelocity();
            target.takeKnockback(0.1, -vel.x, -vel.z);
        } else if (name.contains("Boule de feu")) {
            if (owner instanceof PlayerEntity p) {
                if (!target.isOnFire()) {
                    target.damage(sw, owner.getDamageSources().playerAttack(p), 1.0f);
                    target.setOnFireFor(2.5f);
                }
            }
        } else if (name.contains("Boule de glace")) {
            if (owner instanceof PlayerEntity p) {
                if (!target.hasStatusEffect(StatusEffects.SLOWNESS)) {
                    target.damage(sw, owner.getDamageSources().playerAttack(p), 2.0f);
                    target.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS, 100, 0, false, false, false));
                    // DOT personnalisé (sans particules Wither)
                    com.dungeonmod.util.BaguetteData.iceDot.put(target.getUuid(), System.currentTimeMillis() + 5000);
                }
            }
        } else if (name.contains("Boule sombre")) {
            if (owner instanceof PlayerEntity p) {
                target.damage(sw, owner.getDamageSources().playerAttack(p), 4.0f);
                // DOT 1 cœur/s pendant 5s (non cumulable)
                if (!com.dungeonmod.util.BaguetteData.darkDot.containsKey(target.getUuid())) {
                    com.dungeonmod.util.BaguetteData.darkDot.put(target.getUuid(), System.currentTimeMillis() + 5000);
                }
                // Lien sombre : 5s, les dégâts sur CET ennemi soignent
                com.dungeonmod.util.BaguetteData.darkLink.put(p.getUuid(),
                    new java.util.AbstractMap.SimpleEntry<>(target.getUuid(), System.currentTimeMillis() + 5000));
            }
        }
    }
}
