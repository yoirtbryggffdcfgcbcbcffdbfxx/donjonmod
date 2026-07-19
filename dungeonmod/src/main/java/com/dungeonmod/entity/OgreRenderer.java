package com.dungeonmod.entity;

import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.ItemStack;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.renderer.GeoEntityRenderer;
import software.bernie.geckolib.renderer.layer.BlockAndItemGeoLayer;

public class OgreRenderer extends GeoEntityRenderer<OgreEntity> {
    public OgreRenderer(EntityRendererFactory.Context ctx) {
        super(ctx, new OgreModel());
        this.shadowRadius = 1.2f;
        withScale(2.0f);
        addRenderLayer(new BlockAndItemGeoLayer<>(this, (bone, animatable) -> {
            if ("left_hand".equals(bone.getName())) {
                return animatable.getMainHandStack();
            }
            return ItemStack.EMPTY;
        }, (bone, animatable) -> null) {
            @Override
            protected void renderStackForBone(MatrixStack poseStack, GeoBone bone, ItemStack stack, OgreEntity animatable, net.minecraft.client.render.VertexConsumerProvider bufferSource, float partialTick, int packedLight, int packedOverlay) {
                if ("left_hand".equals(bone.getName()) && !stack.isEmpty()) {
                    poseStack.push();
                    poseStack.translate(0.30, 0, 0);
                    poseStack.scale(0.6f, 0.6f, 0.6f);
                    super.renderStackForBone(poseStack, bone, stack, animatable, bufferSource, partialTick, packedLight, packedOverlay);
                    poseStack.pop();
                } else {
                    super.renderStackForBone(poseStack, bone, stack, animatable, bufferSource, partialTick, packedLight, packedOverlay);
                }
            }
        });
    }
}
