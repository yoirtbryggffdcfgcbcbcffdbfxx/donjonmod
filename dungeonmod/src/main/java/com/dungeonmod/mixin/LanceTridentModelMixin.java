package com.dungeonmod.mixin;

import net.minecraft.client.model.*;
import net.minecraft.client.render.entity.model.TridentEntityModel;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;

@Mixin(TridentEntityModel.class)
public class LanceTridentModelMixin {

    @Overwrite
    public static TexturedModelData getTexturedModelData() {
        ModelData data = new ModelData();
        ModelPartData root = data.getRoot();
        root.addChild("pole", ModelPartBuilder.create().uv(0, 6).cuboid(-0.5F, 2.0F, -0.5F, 1.0F, 25.0F, 1.0F), ModelTransform.NONE);
        root.addChild("base", ModelPartBuilder.create().uv(4, 0).cuboid(-1.5F, 0.0F, -0.5F, 3.0F, 2.0F, 1.0F), ModelTransform.NONE);
        return TexturedModelData.of(data, 32, 32);
    }
}
