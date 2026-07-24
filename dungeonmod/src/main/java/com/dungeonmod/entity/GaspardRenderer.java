package com.dungeonmod.entity;

import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.render.entity.ZombieEntityRenderer;
import net.minecraft.client.render.entity.state.ZombieEntityRenderState;
import net.minecraft.util.Identifier;

public class GaspardRenderer extends ZombieEntityRenderer {

    private static final Identifier TEXTURE = Identifier.of("dungeonmod", "textures/entity/menteur.png");

    public GaspardRenderer(EntityRendererFactory.Context context) {
        super(context);
    }

    @Override
    public Identifier getTexture(ZombieEntityRenderState state) {
        return TEXTURE;
    }
}
