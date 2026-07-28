package com.dungeonmod.entity;

import com.dungeonmod.DungeonMod;
import com.dungeonmod.client.dialogue.EliasDialogue;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.SpawnGroup;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;
import net.minecraft.world.World;

import java.util.List;

public class EliasEntity extends BaseNpcEntity {

    private static final Identifier ID = Identifier.of(DungeonMod.MOD_ID, "elias");
    public static final EntityType<EliasEntity> TYPE = EntityType.Builder.<EliasEntity>create(EliasEntity::new, SpawnGroup.MISC)
        .dimensions(0.6f, 1.95f).maxTrackingRange(64)
        .build(RegistryKey.of(Registries.ENTITY_TYPE.getKey(), ID));

    public static void register() {
        Registry.register(Registries.ENTITY_TYPE, ID, TYPE);
    }

    public EliasEntity(EntityType<? extends net.minecraft.entity.mob.ZombieEntity> entityType, World world) {
        super(entityType, world);
    }

    @Override public String getNpcName() { return "§6Elias"; }
    @Override public List<String> getFirstMeetingLines() { return EliasDialogue.FIRST_MEETING; }
    @Override public List<String> getStandardPromptLines() { return EliasDialogue.STANDARD_PROMPT; }

    @Override
    protected void startDialogue(PlayerEntity player) {
        if (!(player instanceof ServerPlayerEntity sp)) return;
        dialogueCooldown = 60;

        if (!hasTalked) {
            hasTalked = true;
            setCustomName(net.minecraft.text.Text.literal(getNpcName()));
            var lines = new java.util.ArrayList<String>();
            lines.addAll(getFirstMeetingLines());
            lines.addAll(getStandardPromptLines());
            sendSubtitles(sp, lines);
            // Give a fiole on first meeting
            var fiole = com.dungeonmod.ModItems.get("fiole");
            if (fiole != null) {
                ItemStack stack = fiole.createStack();
                if (!sp.getInventory().insertStack(stack)) {
                    sp.dropItem(stack, false);
                }
            }
            return;
        }

        sendSubtitles(sp, getStandardPromptLines());
    }
}
