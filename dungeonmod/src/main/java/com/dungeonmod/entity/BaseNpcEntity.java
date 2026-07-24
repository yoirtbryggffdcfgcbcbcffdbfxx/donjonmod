package com.dungeonmod.entity;

import com.dungeonmod.DungeonMod;
import com.dungeonmod.network.SubtitlePayload;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.ai.goal.LookAtEntityGoal;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.mob.ZombieEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.world.World;

import java.util.List;

public abstract class BaseNpcEntity extends ZombieEntity {

    public boolean hasTalked = false;
    public int returnCount = 0;
    protected int dialogueCooldown = 0;
    public boolean wasFar = false;
    private int trackTick = 0;

    public abstract String getNpcName();
    public abstract List<String> getFirstMeetingLines();
    public abstract List<String> getStandardPromptLines();

    public BaseNpcEntity(EntityType<? extends ZombieEntity> entityType, World world) {
        super(entityType, world);
        setInvulnerable(true);
        setPersistent();
        setCustomName(Text.literal("???"));
        setCustomNameVisible(true);
    }

    @Override
    public void writeCustomDataToNbt(net.minecraft.nbt.NbtCompound nbt) {
        super.writeCustomDataToNbt(nbt);
        nbt.putBoolean("hasTalked", hasTalked);
        nbt.putInt("returnCount", returnCount);
        nbt.putBoolean("wasFar", wasFar);
    }

    @Override
    public void readCustomDataFromNbt(net.minecraft.nbt.NbtCompound nbt) {
        super.readCustomDataFromNbt(nbt);
        if (nbt.contains("hasTalked")) hasTalked = nbt.getBoolean("hasTalked");
        if (nbt.contains("returnCount")) returnCount = nbt.getInt("returnCount");
        if (nbt.contains("wasFar")) wasFar = nbt.getBoolean("wasFar");
    }

    @Override
    public void tick() {
        super.tick();
        if (dialogueCooldown > 0) dialogueCooldown--;
        trackTick++;
        if (trackTick >= 40) { // toutes les 2 secondes
            trackTick = 0;
            if (!getWorld().isClient() && hasTalked) {
                var nearest = getWorld().getClosestPlayer(this, 30.0);
                if (nearest == null) wasFar = true;
            }
        }
    }

    @Override
    protected void initGoals() {
        goalSelector.add(0, new LookAtEntityGoal(this, PlayerEntity.class, 8.0f, 1.0f));
    }

    @Override
    public boolean damage(ServerWorld world, DamageSource source, float amount) {
        if (source.getAttacker() instanceof PlayerEntity player && source.getAttacker() == source.getSource()) {
            if (dialogueCooldown > 0) return false;
            startDialogue(player);
        }
        return false;
    }

    protected void startDialogue(PlayerEntity player) {
        if (!(player instanceof ServerPlayerEntity sp)) return;
        dialogueCooldown = 60;
        if (this instanceof NpcShopProvider) {
            DungeonMod.npcShopCache.put(sp.getUuid(), this.getUuid());
        }

        if (!hasTalked) {
            hasTalked = true;
            setCustomName(Text.literal(getNpcName()));
            var lines = new java.util.ArrayList<String>();
            lines.addAll(getFirstMeetingLines());
            lines.addAll(getStandardPromptLines());
            sendSubtitles(sp, lines);
            return;
        }

        sendSubtitles(sp, getStandardPromptLines());
    }

    public boolean checkReturn(PlayerEntity player) {
        boolean far = wasFar;

        wasFar = false;
        return far;
    }

    protected void sendSubtitles(ServerPlayerEntity player, List<String> lines) {
        ServerPlayNetworking.send(player, new SubtitlePayload(getNpcName(), lines, true));
    }
}
