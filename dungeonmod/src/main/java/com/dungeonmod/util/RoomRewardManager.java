package com.dungeonmod.util;

import net.minecraft.block.Blocks;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtList;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Coffre de recompense de salle : quand tous les monstres d'une salle M sont
 * tues, un coffre apparait sur un bloc d'air au-dessus du sol, rempli avec le
 * loot standard (DungeonLoot.fillChest).
 *
 * Une salle ne peut etre recompensee qu'une seule fois (persiste en NBT via
 * TestGenerator.saveToDisk / loadFromDisk).
 */
public final class RoomRewardManager {
    private RoomRewardManager() {}

    /** salle (coin bas) → UUID des mobs vivants */
    private static final Map<BlockPos, Set<UUID>> ROOM_MOBS = new HashMap<>();
    /** UUID mob → position (pour verifier le chargement du chunk) */
    private static final Map<UUID, BlockPos> MOB_POS = new HashMap<>();
    /** UUID mob → ticks consecutifs ou l'entite etait introuvable (anti faux positif chunk streaming) */
    private static final Map<UUID, Integer> MISSING_TICKS = new HashMap<>();
    /** salle → cle de loot (table de DungeonLoot utilisee pour le coffre) */
    private static final Map<BlockPos, String> ROOM_LOOT_KEYS = new HashMap<>();
    /** salles deja recompensees (persiste) */
    private static final Set<BlockPos> REWARDED_ROOMS = new HashSet<>();

    /** Largeur d'une salle en blocs (CELL dans TestGenerator). */
    private static final int ROOM_SIZE = 10;
    /** Ticks consecutifs sans trouver l'entite (et chunk charge) avant de la declarer morte. */
    private static final int MISSING_DEAD_TICKS = 30;

    public static void clear() {
        ROOM_MOBS.clear();
        MOB_POS.clear();
        MISSING_TICKS.clear();
        ROOM_LOOT_KEYS.clear();
        REWARDED_ROOMS.clear();
    }

    public static void registerRoom(BlockPos roomCorner, String lootKey) {
        ROOM_LOOT_KEYS.put(roomCorner, lootKey);
    }

    public static void registerMob(BlockPos roomCorner, UUID mobUuid, BlockPos mobPos) {
        ROOM_MOBS.computeIfAbsent(roomCorner, k -> new HashSet<>()).add(mobUuid);
        MOB_POS.put(mobUuid, mobPos);
    }

    /**
     * Appele a chaque tick serveur : retire les mobs morts (uniquement si leur
     * chunk est charge, sinon on ne peut pas conclure), et quand une salle
     * n'a plus aucun mob vivant, spawn le coffre de recompense.
     */
    public static void checkRooms(ServerWorld world) {
        if (ROOM_MOBS.isEmpty()) return;
        var it = ROOM_MOBS.entrySet().iterator();
        while (it.hasNext()) {
            var e = it.next();
            BlockPos room = e.getKey();
            if (REWARDED_ROOMS.contains(room)) { it.remove(); continue; }
            Set<UUID> mobs = e.getValue();
            mobs.removeIf(uuid -> isDefinitivelyDead(world, uuid));
            if (mobs.isEmpty()) {
                if (!world.isChunkLoaded(room.getX() >> 4, room.getZ() >> 4)) continue;
                it.remove();
                spawnRewardChest(world, room);
            }
        }
    }

    /**
     * Mort uniquement si le chunk du mob est charge et que l'entite y est
     * introuvable pendant plusieurs ticks consecutifs (evite les faux positifs
     * pendant le streaming des chunks).
     */
    private static boolean isDefinitivelyDead(ServerWorld world, UUID uuid) {
        var ent = world.getEntity(uuid);
        if (ent != null) {
            MISSING_TICKS.remove(uuid);
            return !ent.isAlive();
        }
        BlockPos pos = MOB_POS.get(uuid);
        if (pos == null) return true;
        if (!world.isChunkLoaded(pos.getX() >> 4, pos.getZ() >> 4)) {
            MISSING_TICKS.remove(uuid);
            return false;
        }
        int ticks = MISSING_TICKS.merge(uuid, 1, Integer::sum);
        return ticks >= MISSING_DEAD_TICKS;
    }

    private static void spawnRewardChest(ServerWorld world, BlockPos roomCorner) {
        BlockPos chestPos = findChestSpot(world, roomCorner);
        if (chestPos == null) {
            System.out.println("[RoomReward] ECHEC findChestSpot dans la salle " + roomCorner);
            return;
        }
        world.setBlockState(chestPos, Blocks.CHEST.getDefaultState(), 3);
        String lootKey = ROOM_LOOT_KEYS.getOrDefault(roomCorner, "M");
        DungeonLoot.fillChest(world, chestPos, new java.util.Random(), lootKey, roomCorner.getY());
        world.playSound(null, chestPos, net.minecraft.sound.SoundEvents.ENTITY_PLAYER_LEVELUP,
            net.minecraft.sound.SoundCategory.BLOCKS, 1.0f, 1.0f);
        REWARDED_ROOMS.add(roomCorner);
        System.out.println("[RoomReward] Coffre place a " + chestPos + " (salle " + roomCorner + ")");
    }

    /**
     * Cherche un bloc d'air juste au-dessus du sol de la salle (y+1, avec un
     * bloc solide en dessous a y). Le coffre est donc toujours pose sur le
     * plancher de la salle, jamais sur une structure (plateforme, table...).
     */
    private static BlockPos findChestSpot(ServerWorld world, BlockPos roomCorner) {
        int baseX = roomCorner.getX();
        int baseY = roomCorner.getY();
        int baseZ = roomCorner.getZ();
        for (int attempt = 0; attempt < 40; attempt++) {
            int rx = baseX + world.random.nextInt(ROOM_SIZE);
            int rz = baseZ + world.random.nextInt(ROOM_SIZE);
            BlockPos p = new BlockPos(rx, baseY + 1, rz);
            if (!world.getBlockState(p).isAir()) continue;
            if (world.getBlockState(p.down()).isAir()) continue;
            if (!world.getBlockState(p.up()).isAir()) continue;
            return p;
        }
        return null;
    }

    // ===================== Persistance (via TestGenerator) =====================

    public static NbtList writeToNbt() {
        NbtList list = new NbtList();
        for (BlockPos p : REWARDED_ROOMS) {
            NbtCompound t = new NbtCompound();
            t.putInt("x", p.getX());
            t.putInt("y", p.getY());
            t.putInt("z", p.getZ());
            list.add(t);
        }
        return list;
    }

    public static void readFromNbt(NbtList list) {
        REWARDED_ROOMS.clear();
        if (list == null) return;
        for (int i = 0; i < list.size(); i++) {
            NbtCompound t = list.getCompound(i);
            REWARDED_ROOMS.add(new BlockPos(t.getInt("x"), t.getInt("y"), t.getInt("z")));
        }
        ROOM_MOBS.clear();
    }
}
