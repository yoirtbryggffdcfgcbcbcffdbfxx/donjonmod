package com.dungeonmod.entity;

import net.minecraft.client.render.entity.EntityRendererFactory;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

public class GoblinMinibossRenderer extends GeoEntityRenderer<GoblinMinibossEntity> {
    public GoblinMinibossRenderer(EntityRendererFactory.Context ctx) {
        super(ctx, new GoblinMinibossModel());
        this.shadowRadius = 0.6f;
        withScale(1.5f);
    }
}
