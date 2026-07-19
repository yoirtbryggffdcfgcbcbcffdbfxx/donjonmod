package com.dungeonmod.entity;

import net.minecraft.util.Identifier;
import software.bernie.geckolib.model.GeoModel;
import software.bernie.geckolib.renderer.GeoRenderer;

public class OgreModel extends GeoModel<OgreEntity> {
    @Override
    public Identifier getModelResource(OgreEntity object, GeoRenderer<OgreEntity> renderer) {
        return Identifier.of("dungeonmod", "geo/cyclops.geo.json");
    }

    @Override
    public Identifier getTextureResource(OgreEntity object, GeoRenderer<OgreEntity> renderer) {
        return Identifier.of("dungeonmod", "textures/entity/texture_cyclops.png");
    }

    @Override
    public Identifier getAnimationResource(OgreEntity object) {
        return Identifier.of("dungeonmod", "animations/cyclops.animation.json");
    }
}
