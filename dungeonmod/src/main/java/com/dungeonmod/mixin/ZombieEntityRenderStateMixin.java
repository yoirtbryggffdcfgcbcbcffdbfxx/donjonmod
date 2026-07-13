package com.dungeonmod.mixin;

import com.dungeonmod.accessor.CustomSkinAccessor;
import net.minecraft.client.render.entity.state.ZombieEntityRenderState;
import net.minecraft.util.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

@Mixin(ZombieEntityRenderState.class)
public class ZombieEntityRenderStateMixin implements CustomSkinAccessor {
    @Unique
    private boolean dungeonmod$customSkin;
    @Unique
    private Identifier dungeonmod$customTexture;

    @Override
    public boolean dungeonmod$hasCustomSkin() {
        return dungeonmod$customSkin;
    }

    @Override
    public void dungeonmod$setCustomSkin(boolean value) {
        this.dungeonmod$customSkin = value;
    }

    @Override
    public Identifier dungeonmod$getCustomTexture() {
        return dungeonmod$customTexture;
    }

    @Override
    public void dungeonmod$setCustomTexture(Identifier texture) {
        this.dungeonmod$customTexture = texture;
    }
}
