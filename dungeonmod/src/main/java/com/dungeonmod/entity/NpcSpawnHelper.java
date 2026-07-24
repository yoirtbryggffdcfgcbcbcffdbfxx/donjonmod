package com.dungeonmod.entity;

import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;

import java.util.List;

public class NpcSpawnHelper {

    public static void scanRoom(ServerWorld world, int wx, int wy, int wz, int size, int roomType) {
        List<NpcRegistry.NpcEntry> entries = NpcRegistry.getAll();
        if (entries.isEmpty()) return;

        for (int x = wx; x < wx + size; x++) {
            for (int z = wz; z < wz + size; z++) {
                for (int y = wy; y < wy + size; y++) {
                    BlockPos pos = new BlockPos(x, y, z);
                    var state = world.getBlockState(pos);
                    for (var entry : entries) {
                        if (entry.roomType() != roomType || !state.isOf(entry.block())) continue;
                        BlockPos spawnPos = pos.up();
                        if (!world.getBlockState(spawnPos).isAir()) break;
                        if (!world.getEntitiesByClass(
                            BaseNpcEntity.class, new Box(spawnPos).expand(1),
                            e -> e.getType() == entry.type()
                        ).isEmpty()) break;
                        var npc = entry.type().create(world, net.minecraft.entity.SpawnReason.SPAWNER);
                        if (npc != null) {
                            npc.setPos(spawnPos.getX() + 0.5, spawnPos.getY(), spawnPos.getZ() + 0.5);
                            world.spawnEntity(npc);
                        }
                        break;
                    }
                }
            }
        }
    }
}
