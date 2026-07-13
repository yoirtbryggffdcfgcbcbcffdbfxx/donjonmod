package com.dungeonmod.mixin;

import net.minecraft.component.DataComponentTypes;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Items;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Mixin(LivingEntity.class)
public class ComboMixin {

    @Unique
    private static final Map<UUID, UUID> currentTargets = new HashMap<>();
    @Unique
    private static final Map<UUID, Integer> combos = new HashMap<>();
    @Unique
    private static boolean comboProcessing = false;

    @Inject(method = "damage", at = @At("HEAD"), cancellable = true)
    private void onDamage(ServerWorld world, DamageSource source, float amount, CallbackInfoReturnable<Boolean> cir) {
        if (comboProcessing) return;
        if (!(source.getAttacker() instanceof PlayerEntity player)) return;
        if (!hasHache(player)) return;
        Entity target = (Entity) (Object) this;
        if (!(target instanceof LivingEntity living)) return;

        comboProcessing = true;
        UUID puid = player.getUuid();
        UUID tuid = target.getUuid();
        UUID currentTarget = currentTargets.get(puid);
        int combo = combos.getOrDefault(puid, 0);
        if (currentTarget == null || !currentTarget.equals(tuid)) {
            combo = 0;
        }
        float dmg = getComboDamage(puid, tuid);
        boolean result = living.damage(world, source, dmg);
        comboProcessing = false;

        float[] kbValues = {0.1f, 0.2f, 0.2f, 0.5f};
        float kb = kbValues[Math.min(combo, 3)];
        double dx = player.getX() - living.getX();
        double dz = player.getZ() - living.getZ();
        living.takeKnockback(kb, dx, dz);

        cir.setReturnValue(result);

        // Son d'attaque plus grave selon le combo
        float pitch = 1.0f - combo * 0.1f;
        world.playSound(null, living.getX(), living.getY(), living.getZ(),
            SoundEvents.BLOCK_ANVIL_LAND,
            SoundCategory.PLAYERS, 1.0f, pitch);
    }

    @Unique
    private static float getComboDamage(UUID puid, UUID tuid) {
        UUID currentTarget = currentTargets.get(puid);
        int combo = combos.getOrDefault(puid, 0);

        if (currentTarget == null || !currentTarget.equals(tuid)) {
            combo = 0;
        }

        float[] damages = {2.0f, 3.0f, 5.0f, 11.0f};
        float dmg = damages[Math.min(combo, 3)];

        currentTargets.put(puid, tuid);
        combos.put(puid, Math.min(combo + 1, 3));
        return dmg;
    }

    @Unique
    private static boolean hasHache(PlayerEntity player) {
        var stack = player.getMainHandStack();
        if (stack.isEmpty() || !stack.isOf(Items.IRON_AXE)) return false;
        if (!stack.contains(DataComponentTypes.CUSTOM_NAME)) return false;
        return stack.get(DataComponentTypes.CUSTOM_NAME).getString().contains("Hache en fer");
    }
}
