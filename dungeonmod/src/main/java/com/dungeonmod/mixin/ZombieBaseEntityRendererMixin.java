package com.dungeonmod.mixin;

import com.dungeonmod.accessor.CustomSkinAccessor;
import com.dungeonmod.DungeonMod;
import net.minecraft.client.render.entity.ZombieBaseEntityRenderer;
import net.minecraft.client.render.entity.state.ZombieEntityRenderState;
import net.minecraft.entity.mob.ZombieEntity;
import net.minecraft.util.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ZombieBaseEntityRenderer.class)
public class ZombieBaseEntityRendererMixin {

    @Inject(method = "updateRenderState(Lnet/minecraft/entity/mob/ZombieEntity;Lnet/minecraft/client/render/entity/state/ZombieEntityRenderState;F)V", at = @At("TAIL"))
    private void onUpdateRenderState(ZombieEntity entity, ZombieEntityRenderState state, float tickDelta, CallbackInfo ci) {
        CustomSkinAccessor acc = (CustomSkinAccessor)(Object)state;
        if (DungeonMod.customZombies.contains(entity.getUuid())) {
            acc.dungeonmod$setCustomSkin(true);
            Identifier tex = DungeonMod.zombieTextures.get(entity.getUuid());
            if (tex == null) tex = Identifier.of("dungeonmod", "textures/entity/zombie_custom.png");
            acc.dungeonmod$setCustomTexture(tex);
        } else {
            acc.dungeonmod$setCustomSkin(false);
        }
    }

    @Inject(method = "getTexture(Lnet/minecraft/client/render/entity/state/ZombieEntityRenderState;)Lnet/minecraft/util/Identifier;", at = @At("HEAD"), cancellable = true)
    private void onGetTexture(ZombieEntityRenderState state, CallbackInfoReturnable<Identifier> ci) {
        CustomSkinAccessor acc = (CustomSkinAccessor)(Object)state;
        if (acc.dungeonmod$hasCustomSkin()) {
            ci.setReturnValue(acc.dungeonmod$getCustomTexture());
        }
    }
}
