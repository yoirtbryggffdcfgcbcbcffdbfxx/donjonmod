package com.dungeonmod.entity;

import net.minecraft.util.Identifier;
import software.bernie.geckolib.model.GeoModel;
import software.bernie.geckolib.renderer.GeoRenderer;

public class GoblinMinibossModel extends GeoModel<GoblinMinibossEntity> {
    @Override
    public Identifier getModelResource(GoblinMinibossEntity object, GeoRenderer<GoblinMinibossEntity> renderer) {
        return Identifier.of("dungeonmod", "geo/miniboss_goblin.geo.json");
    }

    @Override
    public Identifier getTextureResource(GoblinMinibossEntity object, GeoRenderer<GoblinMinibossEntity> renderer) {
        return Identifier.of("dungeonmod", "textures/entity/miniboss_goblin.png");
    }

    @Override
    public Identifier getAnimationResource(GoblinMinibossEntity object) {
        return Identifier.of("dungeonmod", "animations/miniboss_goblin.animation.json");
    }
}
