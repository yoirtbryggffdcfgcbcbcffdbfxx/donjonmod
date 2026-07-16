package com.dungeonmod.screen;

import com.dungeonmod.DungeonMod;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.resource.featuretoggle.FeatureSet;
import net.minecraft.screen.ScreenHandlerType;
import net.minecraft.util.Identifier;

public class ModScreenHandlers {

    public static final ScreenHandlerType<SacScreenHandler> SAC_SCREEN_HANDLER =
        Registry.register(
            Registries.SCREEN_HANDLER,
            Identifier.of(DungeonMod.MOD_ID, "sac"),
            new ScreenHandlerType<>(SacScreenHandler::new, FeatureSet.empty())
        );

    public static void register() {}
}
