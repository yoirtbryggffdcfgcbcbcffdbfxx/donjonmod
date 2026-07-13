package com.dungeonmod.accessor;

import net.minecraft.util.Identifier;

public interface CustomSkinAccessor {
    boolean dungeonmod$hasCustomSkin();
    void dungeonmod$setCustomSkin(boolean value);
    Identifier dungeonmod$getCustomTexture();
    void dungeonmod$setCustomTexture(Identifier texture);
}
