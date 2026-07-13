package com.dungeonmod.client;

import com.dungeonmod.entity.BoomerangEntity;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.render.entity.FlyingItemEntityRenderer;
import net.minecraft.client.render.entity.state.FlyingItemEntityRenderState;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.RotationAxis;

public class BoomerangEntityRenderer extends FlyingItemEntityRenderer<BoomerangEntity> {

    private int entityAge;

    public BoomerangEntityRenderer(EntityRendererFactory.Context ctx) {
        super(ctx, 4.0F, false);
    }

    @Override
    public void updateRenderState(BoomerangEntity entity, FlyingItemEntityRenderState state, float tickDelta) {
        super.updateRenderState(entity, state, tickDelta);
        this.entityAge = entity.getAge();
    }

    @Override
    public void render(FlyingItemEntityRenderState state, MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light) {
        matrices.push();
        matrices.scale(4.0F, 4.0F, 4.0F);
        matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(-60));
        matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(entityAge * 20));
        state.itemRenderState.render(matrices, vertexConsumers, light, OverlayTexture.DEFAULT_UV);
        matrices.pop();
    }
}
