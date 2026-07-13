package com.dungeonmod;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.NbtList;
import net.minecraft.nbt.NbtSizeTracker;
import net.minecraft.registry.Registries;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.state.property.Property;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.GameRules;

import java.io.InputStream;
import java.net.URL;
import java.util.*;

public class DungeonGenerator {

    private static final int ROOM_SIZE = 10;
    private static final int PASSAGE_MIN = 3;
    private static final int PASSAGE_MAX = 6;

    private static BlockState[][][] entryData;
    private static BlockState[][][] roomData;
    private static BlockState[][][] passageSecretData;
    private static BlockState[][][] escalierAvecLootData;
    private static BlockState[][][] escalierSansLootData;
    private static BlockState[][][] finescalierAvecLootData;
    private static BlockState[][][] finescalierSansLootData;
    private static BlockState[][][][] sortieDifficultData;
    private static int sizeX, sizeY, sizeZ;

    enum RoomType { ENTRY, NORMAL, ESCALIER, FINESCALIER, PASSAGE_SECRET, EXIT, EMPTY }

    private static List<Room> dungeonRooms;
    private static List<int[]> pileLocalPositions = new ArrayList<>();

    public static void generate(ServerWorld world, BlockPos playerPos, int numRooms, int difficulty, ServerPlayerEntity player, String dungeonName) {
        if (entryData == null) {
            loadStructures();
        }

        DungeonPickaxe.clearBreakablePositions();

        int gridTarget = Math.max(4, numRooms);
        int gridW = (int) Math.ceil(Math.sqrt(gridTarget));
        int gridH = (int) Math.ceil((double) gridTarget / gridW);

        BlockPos origin = playerPos.add(
            -gridW * ROOM_SIZE / 2,
            0,
            -gridH * ROOM_SIZE / 2
        );

        Random rng = new Random();
        int[] dx = {1, 0, -1, 0};
        int[] dz = {0, 1, 0, -1};

        boolean[][] hWalls = new boolean[gridW][gridH - 1];
        boolean[][] vWalls = new boolean[gridW - 1][gridH];
        for (int x = 0; x < gridW; x++) Arrays.fill(hWalls[x], true);
        for (int x = 0; x < gridW - 1; x++) Arrays.fill(vWalls[x], true);

        Set<Integer> reservedCells = new HashSet<>();
        reservedCells.add(0);

        Map<Integer, Integer> passageToEscalier = new HashMap<>();
        Map<Integer, Integer> passagePreopenedWalls = new HashMap<>();
        Map<Integer, Boolean> stairLootMap = new HashMap<>();
        Map<Integer, Boolean> finLootMap = new HashMap<>();
        List<BlockPos> extraPositions = new ArrayList<>();

        List<int[]> candidates = new ArrayList<>();
        for (int i = 1; i < gridW * gridH; i++) {
            if (reservedCells.contains(i)) continue;
            int gx = i % gridW;
            int gz = i / gridW;
            for (int d = 0; d < 4; d++) {
                int nx = gx + dx[d];
                int nz = gz + dz[d];
                if (nx >= 0 && nx < gridW && nz >= 0 && nz < gridH) {
                    int neighborIdx = nz * gridW + nx;
                    if (!reservedCells.contains(neighborIdx)) {
                        candidates.add(new int[]{i, neighborIdx, d});
                    }
                }
            }
        }

        Collections.shuffle(candidates, rng);
        int maxStaircases = Math.max(1, numRooms / 8);

        Set<Integer> passageIndices = new HashSet<>();
        Set<Integer> escalierIndices = new HashSet<>();

        for (int[] candidate : candidates) {
            if (passageIndices.size() >= maxStaircases) break;

            int passageIdx = candidate[0];
            int escIdx = candidate[1];
            int direction = candidate[2];

            if (reservedCells.contains(passageIdx) || reservedCells.contains(escIdx)) continue;

            reservedCells.add(passageIdx);
            reservedCells.add(escIdx);
            passageIndices.add(passageIdx);
            escalierIndices.add(escIdx);
            passageToEscalier.put(passageIdx, escIdx);

            boolean hasLoot = rng.nextBoolean();
            stairLootMap.put(escIdx, hasLoot);

            int escGx = escIdx % gridW;
            int escGz = escIdx / gridW;
            BlockPos finPos = new BlockPos(
                origin.getX() + escGx * ROOM_SIZE,
                origin.getY() + 5,
                origin.getZ() + escGz * ROOM_SIZE
            );
            boolean finHasLoot = rng.nextBoolean();
            finLootMap.put(escIdx, finHasLoot);

            if (!finHasLoot) {
                BlockPos upperPassagePos = new BlockPos(
                    finPos.getX() + dx[direction] * ROOM_SIZE,
                    finPos.getY(),
                    finPos.getZ() + dz[direction] * ROOM_SIZE
                );
                extraPositions.add(upperPassagePos);
                extraPositions.add(finPos);
            } else {
                extraPositions.add(finPos);
            }
        }

        for (int pIdx : passageIndices) {
            reservedCells.remove(pIdx);
        }

        for (int passageIdx : passageIndices) {
            int pgx = passageIdx % gridW;
            int pgz = passageIdx / gridW;
            int escIdx = passageToEscalier.get(passageIdx);
            int egx = escIdx % gridW;
            int egz = escIdx / gridW;
            int passageWall = -1;
            if (egx > pgx) passageWall = 0;
            else if (egz > pgz) passageWall = 1;
            else if (egx < pgx) passageWall = 2;
            else if (egz < pgz) passageWall = 3;

            List<Integer> availableWalls = new ArrayList<>();
            for (int d = 0; d < 4; d++) {
                if (d == passageWall) continue;
                int nx = pgx + dx[d];
                int nz = pgz + dz[d];
                if (nx >= 0 && nx < gridW && nz >= 0 && nz < gridH) {
                    int nIdx = nz * gridW + nx;
                    if (!reservedCells.contains(nIdx)) {
                        availableWalls.add(d);
                    }
                }
            }

            Collections.shuffle(availableWalls, rng);
            int wallsToOpen = Math.min(2, availableWalls.size());
            int wallMask = 0;
            for (int w = 0; w < wallsToOpen; w++) {
                wallMask |= (1 << availableWalls.get(w));
            }
            passagePreopenedWalls.put(passageIdx, wallMask);

            for (int w = 0; w < wallsToOpen; w++) {
                int d = availableWalls.get(w);
                int nx = pgx + dx[d];
                int nz = pgz + dz[d];
                if (d == 0 && pgx < gridW - 1) vWalls[pgx][pgz] = false;
                else if (d == 1 && pgz < gridH - 1) hWalls[pgx][pgz] = false;
                else if (d == 2 && pgx > 0) vWalls[nx][pgz] = false;
                else if (d == 3 && pgz > 0) hWalls[pgx][nz] = false;
            }
        }

        boolean[][] visited = new boolean[gridW][gridH];
        for (int idx : reservedCells) {
            visited[idx % gridW][idx / gridW] = true;
        }

        generateMaze(visited, hWalls, vWalls, -1, -1, gridW, gridH, rng);

        for (int z = 0; z < gridH; z++) {
            for (int x = 0; x < gridW; x++) {
                if (!visited[x][z]) {
                    boolean hasUnvisitedNeighbor = false;
                    for (int d = 0; d < 4; d++) {
                        int nx2 = x + dx[d];
                        int nz2 = z + dz[d];
                        if (nx2 >= 0 && nx2 < gridW && nz2 >= 0 && nz2 < gridH && !visited[nx2][nz2]) {
                            hasUnvisitedNeighbor = true;
                            break;
                        }
                    }
                    if (!hasUnvisitedNeighbor) {
                        for (int d = 0; d < 4; d++) {
                            int nx2 = x + dx[d];
                            int nz2 = z + dz[d];
                            if (nx2 >= 0 && nx2 < gridW && nz2 >= 0 && nz2 < gridH && visited[nx2][nz2]) {
                                if (dx[d] == 1 && x < gridW - 1) vWalls[x][z] = false;
                                else if (dx[d] == -1 && nx2 < gridW - 1) vWalls[nx2][z] = false;
                                else if (dz[d] == 1 && z < gridH - 1) hWalls[x][z] = false;
                                else if (dz[d] == -1 && nz2 < gridH - 1) hWalls[x][nz2] = false;
                                visited[x][z] = true;
                                break;
                            }
                        }
                    }
                }
            }
        }



        int entryNeighborDir = -1;
        for (int d = 0; d < 4; d++) {
            int nx = dx[d];
            int nz = dz[d];
            if (nx >= 0 && nx < gridW && nz >= 0 && nz < gridH) {
                int nIdx = nz * gridW + nx;
                if (!reservedCells.contains(nIdx) && visited[nx][nz]) {
                    entryNeighborDir = d;
                    break;
                }
            }
        }
        if (entryNeighborDir < 0) {
            for (int d = 0; d < 4; d++) {
                int nx = dx[d];
                int nz = dz[d];
                if (nx >= 0 && nx < gridW && nz >= 0 && nz < gridH) {
                    int nIdx = nz * gridW + nx;
                    if (!reservedCells.contains(nIdx)) {
                        entryNeighborDir = d;
                        break;
                    }
                }
            }
        }
        int entryRotation = entryNeighborDir >= 0 ? (entryNeighborDir + 2) % 4 : 2;
        if (entryNeighborDir == 0 && 0 < gridW - 1) vWalls[0][0] = false;
        else if (entryNeighborDir == 1 && 0 < gridH - 1) hWalls[0][0] = false;
        else if (entryNeighborDir == 2 && 0 > 0) vWalls[-1][0] = false;
        else if (entryNeighborDir == 3 && 0 > 0) hWalls[0][-1] = false;

        int room2Idx = -1;
        if (entryNeighborDir >= 0) {
            int enx = dx[entryNeighborDir];
            int enz = dz[entryNeighborDir];
            room2Idx = enz * gridW + enx;
        }

        if (room2Idx >= 0 && !escalierIndices.contains(room2Idx) && !passageIndices.contains(room2Idx)) {
            if (rng.nextFloat() < 0.3f) {
                escalierIndices.add(room2Idx);
                stairLootMap.put(room2Idx, rng.nextBoolean());
                finLootMap.put(room2Idx, rng.nextBoolean());
            }
        }

        Set<Integer> upperActiveCells = new HashSet<>();
        for (int idx = 1; idx < gridW * gridH; idx++) {
            if (escalierIndices.contains(idx)) upperActiveCells.add(idx);
            else if (room2Idx >= 0 && idx == room2Idx) upperActiveCells.add(idx);
            else if (rng.nextFloat() < 0.5f) upperActiveCells.add(idx);
        }

        boolean[][] hWallsUpper = new boolean[gridW][gridH - 1];
        boolean[][] vWallsUpper = new boolean[gridW - 1][gridH];
        for (int x = 0; x < gridW; x++) Arrays.fill(hWallsUpper[x], true);
        for (int x = 0; x < gridW - 1; x++) Arrays.fill(vWallsUpper[x], true);
        boolean[][] visitedUpper = new boolean[gridW][gridH];
        for (int idx : reservedCells) visitedUpper[idx % gridW][idx / gridW] = true;
        for (int idx = 1; idx < gridW * gridH; idx++) {
            if (!upperActiveCells.contains(idx)) visitedUpper[idx % gridW][idx / gridW] = true;
        }
        generateMaze(visitedUpper, hWallsUpper, vWallsUpper, -1, -1, gridW, gridH, new Random(rng.nextLong()));

        List<Room> rooms = new ArrayList<>();
        for (int z = 0; z < gridH; z++) {
            for (int x = 0; x < gridW; x++) {
                BlockPos pos = new BlockPos(
                    origin.getX() + x * ROOM_SIZE,
                    origin.getY(),
                    origin.getZ() + z * ROOM_SIZE
                );
                rooms.add(new Room(x, z, pos, rng.nextInt(4)));
            }
        }

        rooms.set(0, new Room(0, 0, rooms.get(0).pos, entryRotation, RoomType.ENTRY));

        if (room2Idx > 0 && room2Idx < rooms.size()) {
            Room r2 = rooms.get(room2Idx);
            if (escalierIndices.contains(room2Idx)) {
                rooms.set(room2Idx, new Room(r2.gx, r2.gz, r2.pos, entryNeighborDir, RoomType.ESCALIER));
            } else {
                int[] dirs = {1, 0, -1, 0};
                int[] dzz = {0, 1, 0, -1};
                int towardEntry = (entryNeighborDir + 2) % 4;
                List<Integer> valid = new ArrayList<>();
                for (int d = 0; d < 4; d++) {
                    if (d == towardEntry) continue;
                    int nx = r2.gx + dirs[d];
                    int nz = r2.gz + dzz[d];
                    if (nx >= 0 && nx < gridW && nz >= 0 && nz < gridH) valid.add(d);
                }
                int room2Dir = valid.isEmpty() ? entryNeighborDir : valid.get(rng.nextInt(valid.size()));
                rooms.set(room2Idx, new Room(r2.gx, r2.gz, r2.pos, room2Dir, RoomType.PASSAGE_SECRET));
            }
        }

        int[][] bfsDirs = {{1, 0, 0}, {0, 1, 1}, {-1, 0, 2}, {0, -1, 3}};
        int totalCells = gridW * gridH;
        int[] bfsDist = new int[totalCells * 2];
        java.util.Arrays.fill(bfsDist, -1);
        java.util.Queue<Integer> bfsQueue = new java.util.LinkedList<>();
        bfsQueue.add(0);
        bfsDist[0] = 0;

        java.util.Map<Integer, Integer> verticalUp = new java.util.HashMap<>();
        if (room2Idx >= 0) {
            verticalUp.put(room2Idx, totalCells + room2Idx);
            verticalUp.put(totalCells + room2Idx, room2Idx);
        }
        for (int escIdx : escalierIndices) {
            verticalUp.put(escIdx, totalCells + escIdx);
            verticalUp.put(totalCells + escIdx, escIdx);
        }

        int farthestIdx = 0;
        int maxDist = 0;
        while (!bfsQueue.isEmpty()) {
            int cur = bfsQueue.poll();
            int floor = cur < totalCells ? 0 : 1;
            int cellIdx = cur % totalCells;
            int cx = cellIdx % gridW;
            int cz = cellIdx / gridW;
            int dist = bfsDist[cur];
            for (int[] bd : bfsDirs) {
                int nx = cx + bd[0];
                int nz = cz + bd[1];
                if (nx < 0 || nx >= gridW || nz < 0 || nz >= gridH) continue;
                int nIdx = floor == 0 ? nz * gridW + nx : totalCells + nz * gridW + nx;
                if (bfsDist[nIdx] >= 0) continue;
                boolean wallOpen = false;
                if (floor == 0) {
                    if (bd[2] == 0 && cx < gridW - 1) wallOpen = !vWalls[cx][cz];
                    else if (bd[2] == 1 && cz < gridH - 1) wallOpen = !hWalls[cx][cz];
                    else if (bd[2] == 2 && cx > 0) wallOpen = !vWalls[nx][cz];
                    else if (bd[2] == 3 && cz > 0) wallOpen = !hWalls[cx][nz];
                } else {
                    if (bd[2] == 0 && cx < gridW - 1) wallOpen = !vWallsUpper[cx][cz];
                    else if (bd[2] == 1 && cz < gridH - 1) wallOpen = !hWallsUpper[cx][cz];
                    else if (bd[2] == 2 && cx > 0) wallOpen = !vWallsUpper[nx][cz];
                    else if (bd[2] == 3 && cz > 0) wallOpen = !hWallsUpper[cx][nz];
                }
                if (wallOpen) {
                    bfsDist[nIdx] = dist + 1;
                    bfsQueue.add(nIdx);
                    if (dist + 1 > maxDist) {
                        maxDist = dist + 1;
                        farthestIdx = nIdx;
                    }
                }
            }
            if (verticalUp.containsKey(cur)) {
                int up = verticalUp.get(cur);
                if (bfsDist[up] < 0) {
                    bfsDist[up] = dist + 1;
                    bfsQueue.add(up);
                    if (dist + 1 > maxDist) {
                        maxDist = dist + 1;
                        farthestIdx = up;
                    }
                }
            }
        }

        boolean exitOnUpper = farthestIdx >= totalCells;
        int exitIdx = farthestIdx % totalCells;
        if (exitOnUpper) {
            if (exitIdx == 0 || escalierIndices.contains(exitIdx) || (room2Idx >= 0 && exitIdx == room2Idx)) {
                int bestDist = -1;
                int bestManhattan = 0;
                int bestCell = -1;
                for (int i = 1; i < totalCells; i++) {
                    if (escalierIndices.contains(i) || (room2Idx >= 0 && i == room2Idx)) continue;
                    int d = bfsDist[totalCells + i];
                    if (d > bestDist) {
                        bestDist = d;
                        bestManhattan = (i % gridW) + (i / gridW);
                        bestCell = i;
                    } else if (d == bestDist) {
                        int manhattan = (i % gridW) + (i / gridW);
                        if (manhattan > bestManhattan) { bestManhattan = manhattan; bestCell = i; }
                    }
                }
                if (bestCell >= 0) exitIdx = bestCell;
            }
        } else {
            if (passageIndices.contains(exitIdx) || escalierIndices.contains(exitIdx) || exitIdx == 0) {
                int bestDist = -1;
                int bestManhattan = 0;
                int bestCell = -1;
                for (int i = 0; i < totalCells; i++) {
                    if (i == 0 || passageIndices.contains(i) || escalierIndices.contains(i)) continue;
                    int d = bfsDist[i];
                    if (d > bestDist) {
                        bestDist = d;
                        bestManhattan = (i % gridW) + (i / gridW);
                        bestCell = i;
                    } else if (d == bestDist) {
                        int manhattan = (i % gridW) + (i / gridW);
                        if (manhattan > bestManhattan) { bestManhattan = manhattan; bestCell = i; }
                    }
                }
                if (bestCell >= 0) { exitIdx = bestCell; exitOnUpper = false; }
                else { exitIdx = farthestIdx % totalCells; exitOnUpper = farthestIdx >= totalCells; }
            }
        }

        if (!exitOnUpper && exitIdx != 0 && !reservedCells.contains(exitIdx)) {
            reservedCells.add(exitIdx);
            visited[exitIdx % gridW][exitIdx / gridW] = true;
        }

        int exitGx = exitIdx % gridW;
        int exitGz = exitIdx / gridW;
        int exitRotation = 0;
        for (int[] bd : bfsDirs) {
            int px = exitGx + bd[0];
            int pz = exitGz + bd[1];
            if (px >= 0 && px < gridW && pz >= 0 && pz < gridH) {
                boolean wallOpen = false;
                if (exitOnUpper) {
                    if (bd[2] == 0 && exitGx < gridW - 1) wallOpen = !vWallsUpper[exitGx][exitGz];
                    else if (bd[2] == 1 && exitGz < gridH - 1) wallOpen = !hWallsUpper[exitGx][exitGz];
                    else if (bd[2] == 2 && exitGx > 0) wallOpen = !vWallsUpper[px][exitGz];
                    else if (bd[2] == 3 && exitGz > 0) wallOpen = !hWallsUpper[exitGx][pz];
                } else {
                    if (bd[2] == 0 && exitGx < gridW - 1) wallOpen = !vWalls[exitGx][exitGz];
                    else if (bd[2] == 1 && exitGz < gridH - 1) wallOpen = !hWalls[exitGx][exitGz];
                    else if (bd[2] == 2 && exitGx > 0) wallOpen = !vWalls[px][exitGz];
                    else if (bd[2] == 3 && exitGz > 0) wallOpen = !hWalls[exitGx][pz];
                }
                if (wallOpen) {
                    exitRotation = (bd[2] + 2) % 4;
                    break;
                }
            }
        }

        for (int passageIdx : passageIndices) {
            int pgx = passageIdx % gridW;
            int pgz = passageIdx / gridW;
            int escIdx = passageToEscalier.get(passageIdx);
            int egx = escIdx % gridW;
            int egz = escIdx / gridW;
            int dir = 0;
            if (egx > pgx) dir = 0;
            else if (egz > pgz) dir = 1;
            else if (egx < pgx) dir = 2;
            else if (egz < pgz) dir = 3;
            Room r = rooms.get(passageIdx);
            rooms.set(passageIdx, new Room(r.gx, r.gz, r.pos, dir, RoomType.PASSAGE_SECRET));
        }

        for (int escIdx : escalierIndices) {
            Room r = rooms.get(escIdx);
            int passageIdx = -1;
            for (var e : passageToEscalier.entrySet()) {
                if (e.getValue() == escIdx) { passageIdx = e.getKey(); break; }
            }
            int dir = 0;
            if (passageIdx >= 0) {
                dir = rooms.get(passageIdx).rotation;
            }
            rooms.set(escIdx, new Room(r.gx, r.gz, r.pos, dir, RoomType.ESCALIER));
        }

        if (!exitOnUpper && exitIdx != 0) {
            Room er = rooms.get(exitIdx);
            rooms.set(exitIdx, new Room(er.gx, er.gz, er.pos, exitRotation, RoomType.EXIT));
        }

        int upperLevel = origin.getY() + 5;
        for (int z = 0; z < gridH; z++) {
            for (int x = 0; x < gridW; x++) {
                int idx = z * gridW + x;
                BlockPos upperPos = new BlockPos(
                    origin.getX() + x * ROOM_SIZE, upperLevel, origin.getZ() + z * ROOM_SIZE
                );
                RoomType uType = RoomType.EMPTY;
                int uRot = rng.nextInt(4);
                if (upperActiveCells.contains(idx)) {
                    if (exitOnUpper && idx == exitIdx) {
                        uType = RoomType.EXIT;
                        uRot = exitRotation;
                    } else if (escalierIndices.contains(idx)) {
                        uType = RoomType.FINESCALIER;
                        uRot = rooms.get(idx).rotation;
                    } else if (room2Idx >= 0 && idx == room2Idx) {
                        uType = RoomType.FINESCALIER;
                        uRot = rooms.get(idx).rotation;
                    } else {
                        uType = RoomType.NORMAL;
                    }
                }
                rooms.add(new Room(x, z, upperPos, uRot, uType));
                extraPositions.add(upperPos);
            }
        }

        placeRoom(world, rooms.get(0).pos, entryData, entryRotation);

        for (int i = 1; i < rooms.size(); i++) {
            Room r = rooms.get(i);
            switch (r.type) {
                case PASSAGE_SECRET:
                    placeRoom(world, r.pos, passageSecretData, r.rotation);
                    break;
                case ESCALIER:
                    boolean stairHasLoot = stairLootMap.getOrDefault(i, rng.nextBoolean());
                    placeRoom(world, r.pos, stairHasLoot ? escalierAvecLootData : escalierSansLootData, r.rotation);
                    break;
                case FINESCALIER:
                    for (int cx = 0; cx < ROOM_SIZE; cx++) {
                        for (int cy = 0; cy < sizeY; cy++) {
                            for (int cz = 0; cz < ROOM_SIZE; cz++) {
                                world.setBlockState(r.pos.add(cx, cy, cz), Blocks.AIR.getDefaultState(), 2);
                            }
                        }
                    }
                    int finIdx = i - totalCells;
                    boolean finHasLoot = finLootMap.getOrDefault(finIdx, rng.nextBoolean());
                    placeRoom(world, r.pos, finHasLoot ? finescalierAvecLootData : finescalierSansLootData, r.rotation);
                    if (room2Idx >= 0 && finIdx == room2Idx) {
                        for (int dy = 0; dy < 2; dy++) {
                            for (int dx2 = 3; dx2 <= 6; dx2++) {
                                for (int dz2 = 3; dz2 <= 6; dz2++) {
                                    world.setBlockState(r.pos.add(dx2, -dy, dz2), Blocks.AIR.getDefaultState(), 2);
                                }
                            }
                        }
                    }
                    break;
                case EXIT:
                    int diffIdx = Math.min(difficulty, 5) - 1;
                    placeRoom(world, r.pos, sortieDifficultData[diffIdx], r.rotation);
                    break;
                case EMPTY:
                    break;
                default:
                    placeRoom(world, r.pos, roomData, r.rotation);
                    break;
            }
        }

        for (int escIdx : escalierIndices) {
            int finIdx = -1;
            for (int i = 0; i < rooms.size(); i++) {
                Room rr = rooms.get(i);
                if (rr.type != RoomType.FINESCALIER) continue;
                int cIdx = i - totalCells;
                if (cIdx == escIdx) { finIdx = i; break; }
            }
            if (finIdx < 0) continue;
            Room finRoom = rooms.get(finIdx);
            boolean finHL = finLootMap.getOrDefault(escIdx, true);
            if (!finHL) {
                int direction = finRoom.rotation;
                BlockPos upperPassagePos = new BlockPos(
                    finRoom.pos.getX() + dx[direction] * ROOM_SIZE,
                    finRoom.pos.getY(),
                    finRoom.pos.getZ() + dz[direction] * ROOM_SIZE
                );
                placeRoom(world, upperPassagePos, passageSecretData, (direction + 2) % 4);
                rooms.add(new Room(-1, -1, upperPassagePos, (direction + 2) % 4, RoomType.PASSAGE_SECRET));
            }
        }

        trackPilePositions(world, rooms.get(0).pos, entryRotation);

        for (Map.Entry<Integer, Integer> pair : passageToEscalier.entrySet()) {
            int passageIdx = pair.getKey();
            int escIdx = pair.getValue();
            Room passageRoom = rooms.get(passageIdx);
            Room escRoom = rooms.get(escIdx);
            int dir = passageRoom.rotation;
            switch (dir) {
                case 0: clearWallX(world, escRoom.pos, 0); break;
                case 1: clearWallZ(world, escRoom.pos, 0); break;
                case 2: clearWallX(world, escRoom.pos, ROOM_SIZE - 1); break;
                case 3: clearWallZ(world, escRoom.pos, ROOM_SIZE - 1); break;
            }
        }

        if (room2Idx >= 0 && room2Idx < rooms.size() && rooms.get(room2Idx).type == RoomType.PASSAGE_SECRET) {
            int r2Dir = rooms.get(room2Idx).rotation;
            int r2nx = (room2Idx % gridW) + dx[r2Dir];
            int r2nz = (room2Idx / gridW) + dz[r2Dir];
            if (r2nx >= 0 && r2nx < gridW && r2nz >= 0 && r2nz < gridH) {
                int nIdx = r2nz * gridW + r2nx;
                if (nIdx < rooms.size()) {
                    carvePassage(world, rooms.get(room2Idx).pos, rooms.get(nIdx).pos, dx[r2Dir], dz[r2Dir]);
                }
            }
        }

        for (int x = 0; x < gridW; x++) {
            for (int z = 0; z < gridH - 1; z++) {
                if (!hWalls[x][z]) {
                    int aIdx = z * gridW + x;
                    int bIdx = (z + 1) * gridW + x;
                    if (reservedCells.contains(aIdx) || reservedCells.contains(bIdx)) continue;
                    if (!exitOnUpper && (aIdx == exitIdx || bIdx == exitIdx)) continue;
                    Room a = rooms.get(aIdx);
                    Room b = rooms.get(bIdx);
                    carveBetween(world, a, b, 0, 1);
                }
            }
        }
        for (int x = 0; x < gridW - 1; x++) {
            for (int z = 0; z < gridH; z++) {
                if (!vWalls[x][z]) {
                    int aIdx = z * gridW + x;
                    int bIdx = z * gridW + (x + 1);
                    if (reservedCells.contains(aIdx) || reservedCells.contains(bIdx)) continue;
                    if (!exitOnUpper && (aIdx == exitIdx || bIdx == exitIdx)) continue;
                    Room a = rooms.get(aIdx);
                    Room b = rooms.get(bIdx);
                    carveBetween(world, a, b, 1, 0);
                }
            }
        }

        if (entryNeighborDir >= 0) {
            int enx = dx[entryNeighborDir];
            int enz = dz[entryNeighborDir];
            int enIdx = enz * gridW + enx;
            if (enIdx >= 0 && enIdx < rooms.size()) {
                Room neighborRoom = rooms.get(enIdx);
                if (enx == 1) clearWallX(world, neighborRoom.pos, 0);
                else if (enx == -1) clearWallX(world, neighborRoom.pos, ROOM_SIZE - 1);
                else if (enz == 1) clearWallZ(world, neighborRoom.pos, 0);
                else clearWallZ(world, neighborRoom.pos, ROOM_SIZE - 1);
            }
        }

        for (int x = 0; x < gridW; x++) {
            for (int z = 0; z < gridH - 1; z++) {
                if (!hWallsUpper[x][z]) {
                    int aCell = z * gridW + x;
                    int bCell = (z + 1) * gridW + x;
                    if (aCell == 0 || bCell == 0) continue;
                    int aIdx = totalCells + aCell;
                    int bIdx = totalCells + bCell;
                    if (aIdx >= rooms.size() || bIdx >= rooms.size()) continue;
                    carvePassage(world, rooms.get(aIdx).pos, rooms.get(bIdx).pos, 0, 1);
                }
            }
        }
        for (int x = 0; x < gridW - 1; x++) {
            for (int z = 0; z < gridH; z++) {
                if (!vWallsUpper[x][z]) {
                    int aCell = z * gridW + x;
                    int bCell = z * gridW + (x + 1);
                    if (aCell == 0 || bCell == 0) continue;
                    int aIdx = totalCells + aCell;
                    int bIdx = totalCells + bCell;
                    if (aIdx >= rooms.size() || bIdx >= rooms.size()) continue;
                    carvePassage(world, rooms.get(aIdx).pos, rooms.get(bIdx).pos, 1, 0);
                }
            }
        }

        if (exitIdx > 0) {
            int exitRoomIdx = exitOnUpper ? totalCells + exitIdx : exitIdx;
            if (exitRoomIdx < rooms.size()) {
                Room exitRoom = rooms.get(exitRoomIdx);
                boolean[][] eWalls = exitOnUpper ? vWallsUpper : vWalls;
                boolean[][] eHWalls = exitOnUpper ? hWallsUpper : hWalls;
                for (int d = 0; d < 4; d++) {
                    int nx = exitGx + dx[d];
                    int nz = exitGz + dz[d];
                    if (nx >= 0 && nx < gridW && nz >= 0 && nz < gridH) {
                        boolean wallOpen = false;
                        if (d == 0 && exitGx < gridW - 1) wallOpen = !eWalls[exitGx][exitGz];
                        else if (d == 1 && exitGz < gridH - 1) wallOpen = !eHWalls[exitGx][exitGz];
                        else if (d == 2 && exitGx > 0) wallOpen = !eWalls[nx][exitGz];
                        else if (d == 3 && exitGz > 0) wallOpen = !eHWalls[exitGx][nz];
                        if (wallOpen) {
                            int nCell = nz * gridW + nx;
                            int nRoomIdx = exitOnUpper ? totalCells + nCell : nCell;
                            if (nRoomIdx >= 0 && nRoomIdx < rooms.size()) {
                                carvePassage(world, exitRoom.pos, rooms.get(nRoomIdx).pos, dx[d], dz[d]);
                                break;
                            }
                        }
                    }
                }
            }
        }

        for (int passageIdx : passageIndices) {
            int wallMask = passagePreopenedWalls.getOrDefault(passageIdx, 0);
            Room passageRoom = rooms.get(passageIdx);
            for (int d = 0; d < 4; d++) {
                if ((wallMask & (1 << d)) != 0) {
                    int nx = passageRoom.gx + dx[d];
                    int nz = passageRoom.gz + dz[d];
                    if (nx >= 0 && nx < gridW && nz >= 0 && nz < gridH) {
                        Room neighbor = rooms.get(nz * gridW + nx);
                        carvePassage(world, passageRoom.pos, neighbor.pos, dx[d], dz[d]);
                    }
                }
            }
        }

        player.changeGameMode(net.minecraft.world.GameMode.ADVENTURE);

        dungeonRooms = rooms;
        DungeonMod.addDungeon(dungeonName, origin, gridW, gridH, world.getRegistryKey().getValue(), entryRotation, extraPositions);

        applyDungeonGamerules(world);
    }

    private static final java.util.Map<GameRules.Key<GameRules.BooleanRule>, Boolean> savedBooleanRules = new java.util.HashMap<>();
    private static final java.util.Map<GameRules.Key<GameRules.IntRule>, Integer> savedIntRules = new java.util.HashMap<>();

    private static void applyDungeonGamerules(ServerWorld world) {
        GameRules rules = world.getGameRules();
        savedBooleanRules.clear();
        savedIntRules.clear();

        saveAndSet(rules, GameRules.DO_MOB_SPAWNING, false);
        saveAndSet(rules, GameRules.DO_MOB_LOOT, false);
        saveAndSet(rules, GameRules.DO_TILE_DROPS, false);
        saveAndSet(rules, GameRules.DO_ENTITY_DROPS, false);
        saveAndSet(rules, GameRules.DO_DAYLIGHT_CYCLE, false);
        saveAndSet(rules, GameRules.DO_WEATHER_CYCLE, false);
        saveAndSet(rules, GameRules.DO_INSOMNIA, false);
        saveAndSet(rules, GameRules.DO_MOB_GRIEFING, false);
        saveAndSet(rules, GameRules.DO_FIRE_TICK, false);
        saveAndSet(rules, GameRules.DO_VINES_SPREAD, false);
        saveAndSet(rules, GameRules.DROWNING_DAMAGE, false);
        saveAndSet(rules, GameRules.FALL_DAMAGE, false);
        saveAndSet(rules, GameRules.FIRE_DAMAGE, false);
        saveAndSet(rules, GameRules.FREEZE_DAMAGE, false);
        saveAndSet(rules, GameRules.KEEP_INVENTORY, true);
        saveAndSet(rules, GameRules.NATURAL_REGENERATION, true);
        saveAndSet(rules, GameRules.DISABLE_RAIDS, true);
        saveAndSet(rules, GameRules.DO_PATROL_SPAWNING, false);
        saveAndSet(rules, GameRules.DO_TRADER_SPAWNING, false);
        saveAndSet(rules, GameRules.DO_WARDEN_SPAWNING, false);
    }

    public static void restoreDungeonGamerules(ServerWorld world) {
        if (savedBooleanRules.isEmpty() && savedIntRules.isEmpty()) return;
        GameRules rules = world.getGameRules();
        for (var e : savedBooleanRules.entrySet()) {
            rules.get(e.getKey()).set(e.getValue(), null);
        }
        for (var e : savedIntRules.entrySet()) {
            rules.get(e.getKey()).set(e.getValue(), null);
        }
        savedBooleanRules.clear();
        savedIntRules.clear();
    }

    private static void saveAndSet(GameRules rules, GameRules.Key<GameRules.BooleanRule> key, boolean value) {
        savedBooleanRules.put(key, rules.getBoolean(key));
        rules.get(key).set(value, null);
    }

    private static void addLoopsToMaze(boolean[][] hWalls, boolean[][] vWalls,
                                        int gridW, int gridH, int difficulty, Random rng,
                                        Set<Integer> reservedCells) {
        int totalWalls = 0;
        for (int x = 0; x < gridW; x++)
            for (int z = 0; z < gridH - 1; z++)
                if (hWalls[x][z]) totalWalls++;
        for (int x = 0; x < gridW - 1; x++)
            for (int z = 0; z < gridH; z++)
                if (vWalls[x][z]) totalWalls++;

        int loopsToAdd;
        switch (difficulty) {
            case 1: loopsToAdd = totalWalls / 4; break;
            case 2: loopsToAdd = totalWalls / 6; break;
            case 3: loopsToAdd = totalWalls / 10; break;
            case 4: loopsToAdd = totalWalls / 18; break;
            default: loopsToAdd = 0; break;
        }

        int added = 0;
        int attempts = 0;
        while (added < loopsToAdd && attempts < loopsToAdd * 10) {
            attempts++;
            if (rng.nextBoolean()) {
                int x = rng.nextInt(gridW);
                int z = rng.nextInt(gridH - 1);
                if (hWalls[x][z]) {
                    int a = z * gridW + x;
                    int b = (z + 1) * gridW + x;
                    if (reservedCells.contains(a) || reservedCells.contains(b)) continue;
                    hWalls[x][z] = false;
                    added++;
                }
            } else {
                int x = rng.nextInt(gridW - 1);
                int z = rng.nextInt(gridH);
                if (vWalls[x][z]) {
                    int a = z * gridW + x;
                    int b = z * gridW + (x + 1);
                    if (reservedCells.contains(a) || reservedCells.contains(b)) continue;
                    vWalls[x][z] = false;
                    added++;
                }
            }
        }
    }

    private static void trackPilePositions(ServerWorld world, BlockPos entryPos, int rotation) {
        Set<BlockPos> breakable = new HashSet<>();
        for (int[] local : pileLocalPositions) {
            int rx = rotateX(local[0], local[2], rotation);
            int rz = rotateZ(local[0], local[2], rotation);
            BlockPos worldPos = entryPos.add(rx, local[1], rz);
            BlockState state = world.getBlockState(worldPos);
            if (!state.isAir()) {
                breakable.add(worldPos.toImmutable());
            }
        }
        int wallX = rotation == 0 ? 0 : rotation == 2 ? ROOM_SIZE - 1 : -1;
        int wallZ = rotation == 1 ? 0 : rotation == 3 ? ROOM_SIZE - 1 : -1;
        if (wallX >= 0) {
            for (int y = 1; y <= 3; y++) {
                for (int z = PASSAGE_MIN; z <= PASSAGE_MAX; z++) {
                    for (int dx = 0; dx <= 1; dx++) {
                        int wx = wallX == 0 ? dx : ROOM_SIZE - 1 - dx;
                        BlockPos bp = entryPos.add(wx, y, z);
                        if (!world.getBlockState(bp).isAir()) {
                            breakable.add(bp.toImmutable());
                        }
                    }
                }
            }
        } else if (wallZ >= 0) {
            for (int y = 1; y <= 3; y++) {
                for (int x = PASSAGE_MIN; x <= PASSAGE_MAX; x++) {
                    for (int dz = 0; dz <= 1; dz++) {
                        int wz = wallZ == 0 ? dz : ROOM_SIZE - 1 - dz;
                        BlockPos bp = entryPos.add(x, y, wz);
                        if (!world.getBlockState(bp).isAir()) {
                            breakable.add(bp.toImmutable());
                        }
                    }
                }
            }
        }
        DungeonPickaxe.setBreakablePositions(breakable);
        DungeonMod.LOGGER.info("Pile breakable positions: {}", breakable.size());
    }

    private static void generateMaze(boolean[][] visited, boolean[][] hWalls, boolean[][] vWalls,
                                      int sx, int sz, int gridW, int gridH, Random rng) {
        int startX = -1, startZ = -1;
        for (int x = 0; x < gridW && startX < 0; x++) {
            for (int z = 0; z < gridH && startX < 0; z++) {
                if (!visited[x][z]) { startX = x; startZ = z; }
            }
        }
        if (startX < 0) return;

        visited[startX][startZ] = true;
        int[] stack = new int[gridW * gridH * 2];
        int sp = 0;
        stack[sp++] = startX;
        stack[sp++] = startZ;

        int[][] dirs = {{1, 0}, {-1, 0}, {0, 1}, {0, -1}};

        while (sp > 0) {
            int cz = stack[sp - 1];
            int cx = stack[sp - 2];
            sp -= 2;

            List<int[]> unvisited = new ArrayList<>();
            for (int[] d : dirs) {
                int nx = cx + d[0];
                int nz = cz + d[1];
                if (nx >= 0 && nx < gridW && nz >= 0 && nz < gridH && !visited[nx][nz]) {
                    unvisited.add(new int[]{nx, nz});
                }
            }

            if (!unvisited.isEmpty()) {
                stack[sp++] = cx;
                stack[sp++] = cz;

                int[] next = unvisited.get(rng.nextInt(unvisited.size()));
                int nx = next[0], nz = next[1];

                if (nx == cx + 1) vWalls[cx][cz] = false;
                else if (nx == cx - 1) vWalls[nx][cz] = false;
                else if (nz == cz + 1) hWalls[cx][cz] = false;
                else if (nz == cz - 1) hWalls[cx][nz] = false;

                visited[nx][nz] = true;
                stack[sp++] = nx;
                stack[sp++] = nz;
            }
        }
    }

    private static void carveBetween(ServerWorld world, Room a, Room b, int dx, int dz) {
        if (a.gx == 0 && a.gz == 0) {
            carveAdjacentOnly(world, a.pos, b.pos, dx, dz);
        } else if (b.gx == 0 && b.gz == 0) {
            carveAdjacentOnly(world, b.pos, a.pos, -dx, -dz);
        } else {
            carvePassage(world, a.pos, b.pos, dx, dz);
        }
    }

    private static void carveAdjacentOnly(ServerWorld world, BlockPos entryPos, BlockPos adjPos, int dx, int dz) {
        if (dx == 1) { clearWallX(world, entryPos, ROOM_SIZE - 1); clearWallX(world, adjPos, 0); }
        else if (dx == -1) { clearWallX(world, entryPos, 0); clearWallX(world, adjPos, ROOM_SIZE - 1); }
        else if (dz == 1) { clearWallZ(world, entryPos, ROOM_SIZE - 1); clearWallZ(world, adjPos, 0); }
        else { clearWallZ(world, entryPos, 0); clearWallZ(world, adjPos, ROOM_SIZE - 1); }
    }

    public static boolean clear(ServerWorld world) {
        if (dungeonRooms == null) return false;
        for (Room room : dungeonRooms) {
            for (int x = 0; x < sizeX; x++) {
                for (int y = 0; y < sizeY; y++) {
                    for (int z = 0; z < sizeZ; z++) {
                        world.setBlockState(room.pos.add(x, y, z), Blocks.AIR.getDefaultState(), 2);
                    }
                }
            }
        }
        DungeonPickaxe.clearBreakablePositions();
        dungeonRooms = null;
        if (DungeonMod.dungeons.isEmpty()) {
            restoreDungeonGamerules(world);
        }
        return true;
    }

    public static boolean clearDungeon(ServerWorld world, DungeonMod.DungeonData dungeon) {
        int gx = dungeon.origin.getX();
        int gy = dungeon.origin.getY();
        int gz = dungeon.origin.getZ();
        for (int rx = 0; rx < dungeon.gridW; rx++) {
            for (int rz = 0; rz < dungeon.gridH; rz++) {
                BlockPos roomOrigin = new BlockPos(gx + rx * ROOM_SIZE, gy, gz + rz * ROOM_SIZE);
                for (int x = 0; x < ROOM_SIZE; x++) {
                    for (int y = 0; y < 5; y++) {
                        for (int z = 0; z < ROOM_SIZE; z++) {
                            world.setBlockState(roomOrigin.add(x, y, z), Blocks.AIR.getDefaultState(), 2);
                        }
                    }
                }
            }
        }
        for (BlockPos extra : dungeon.extraPositions) {
            for (int x = 0; x < ROOM_SIZE; x++) {
                for (int y = 0; y < 5; y++) {
                    for (int z = 0; z < ROOM_SIZE; z++) {
                        world.setBlockState(extra.add(x, y, z), Blocks.AIR.getDefaultState(), 2);
                    }
                }
            }
        }
        return true;
    }

    public static BlockPos getSpawnPos() {
        if (dungeonRooms == null || dungeonRooms.isEmpty()) return null;
        Room entry = dungeonRooms.get(0);
        return new BlockPos(entry.pos.getX() + sizeX / 2, entry.pos.getY() + 2, entry.pos.getZ() + sizeZ / 2);
    }

    public static float getSpawnYaw() {
        if (dungeonRooms == null || dungeonRooms.isEmpty()) return 0f;
        int r = dungeonRooms.get(0).rotation;
        switch (r) {
            case 0: return 90f;
            case 1: return 180f;
            case 2: return 270f;
            case 3: return 0f;
            default: return 0f;
        }
    }

    private static void placeRoom(ServerWorld world, BlockPos origin, BlockState[][][] data, int rotation) {
        for (int x = 0; x < sizeX; x++) {
            for (int y = 0; y < sizeY; y++) {
                for (int z = 0; z < sizeZ; z++) {
                    BlockState state = data[x][y][z];
                    if (state != null && !state.isAir()) {
                        int rx = rotateX(x, z, rotation);
                        int rz = rotateZ(x, z, rotation);
                        BlockState rotated = rotateBlockState(state, rotation);
                        world.setBlockState(origin.add(rx, y, rz), rotated, 2);
                    }
                }
            }
        }
    }

    private static int rotateX(int x, int z, int r) {
        switch (r) { case 1: return ROOM_SIZE - 1 - z; case 2: return ROOM_SIZE - 1 - x; case 3: return z; default: return x; }
    }

    private static int rotateZ(int x, int z, int r) {
        switch (r) { case 1: return x; case 2: return ROOM_SIZE - 1 - z; case 3: return ROOM_SIZE - 1 - x; default: return z; }
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static BlockState rotateBlockState(BlockState state, int rotation) {
        if (rotation == 0) return state;
        Property facingProp = state.getBlock().getStateManager().getProperty("facing");
        if (facingProp != null) {
            String val = String.valueOf(state.get(facingProp));
            state = applyProperty(state, facingProp, rotateFacing(val, rotation));
        }
        Property axisProp = state.getBlock().getStateManager().getProperty("axis");
        if (axisProp != null) {
            String val = String.valueOf(state.get(axisProp));
            state = applyProperty(state, axisProp, rotateAxis(val, rotation));
        }
        Property rotProp = state.getBlock().getStateManager().getProperty("rotation");
        if (rotProp != null && rotProp.getName().equals("rotation")) {
            int cur = (int) state.get(rotProp);
            int newRot = (cur + rotation * 4) % 16;
            state = applyProperty(state, rotProp, String.valueOf(newRot));
        }
        Property hingeProp = state.getBlock().getStateManager().getProperty("hinge");
        if (hingeProp != null) {
            String val = String.valueOf(state.get(hingeProp));
            if (rotation == 1 || rotation == 3) {
                state = applyProperty(state, hingeProp, val.equals("left") ? "right" : "left");
            }
        }
        return state;
    }

    private static String rotateFacing(String facing, int rotation) {
        String[] dirs = {"north", "east", "south", "west"};
        int idx = -1;
        for (int i = 0; i < dirs.length; i++) { if (dirs[i].equals(facing)) { idx = i; break; } }
        if (idx < 0) return facing;
        return dirs[(idx + rotation) % 4];
    }

    private static String rotateAxis(String axis, int rotation) {
        if (rotation == 0 || rotation == 2) return axis;
        if (axis.equals("x")) return "z";
        if (axis.equals("z")) return "x";
        return axis;
    }

    private static void carvePassage(ServerWorld world, BlockPos a, BlockPos b, int dx, int dz) {
        if (dx == 1) { clearWallX(world, a, ROOM_SIZE - 1); clearWallX(world, b, 0); }
        else if (dx == -1) { clearWallX(world, a, 0); clearWallX(world, b, ROOM_SIZE - 1); }
        else if (dz == 1) { clearWallZ(world, a, ROOM_SIZE - 1); clearWallZ(world, b, 0); }
        else { clearWallZ(world, a, 0); clearWallZ(world, b, ROOM_SIZE - 1); }
    }

    private static void clearWallX(ServerWorld world, BlockPos origin, int x) {
        int adjX = (x == 0) ? 1 : (x == ROOM_SIZE - 1) ? ROOM_SIZE - 2 : -1;
        for (int y = 1; y <= 3; y++) {
            for (int z = PASSAGE_MIN; z <= PASSAGE_MAX; z++) {
                world.setBlockState(origin.add(x, y, z), Blocks.AIR.getDefaultState(), 2);
                if (adjX >= 0) world.setBlockState(origin.add(adjX, y, z), Blocks.AIR.getDefaultState(), 2);
            }
        }
    }

    private static void clearWallZ(ServerWorld world, BlockPos origin, int z) {
        int adjZ = (z == 0) ? 1 : (z == ROOM_SIZE - 1) ? ROOM_SIZE - 2 : -1;
        for (int y = 1; y <= 3; y++) {
            for (int x = PASSAGE_MIN; x <= PASSAGE_MAX; x++) {
                world.setBlockState(origin.add(x, y, z), Blocks.AIR.getDefaultState(), 2);
                if (adjZ >= 0) world.setBlockState(origin.add(x, y, adjZ), Blocks.AIR.getDefaultState(), 2);
            }
        }
    }

    private static void loadStructures() {
        entryData = loadNbt("/structures/entre1withpile.nbt");
        BlockState[][][] baseData = loadNbt("/structures/entre1.nbt");
        roomData = loadNbt("/structures/entre2.nbt");
        passageSecretData = loadNbt("/structures/entre2avecpassagesecret.nbt");
        escalierAvecLootData = loadNbt("/structures/escalieravecloot.nbt");
        escalierSansLootData = loadNbt("/structures/escaliersansloot.nbt");
        finescalierAvecLootData = loadNbt("/structures/finescalieravecloot.nbt");
        finescalierSansLootData = loadNbt("/structures/finescaliersansloot.nbt");
        sortieDifficultData = new BlockState[5][][][];
        sortieDifficultData[0] = loadNbt("/structures/sortiedifficult1.nbt");
        sortieDifficultData[1] = loadNbt("/structures/sortiedifficult2.nbt");
        sortieDifficultData[2] = loadNbt("/structures/sortiedifficult3.nbt");
        sortieDifficultData[3] = loadNbt("/structures/sortiedifficult4.nbt");
        sortieDifficultData[4] = loadNbt("/structures/sortiedifficult5.nbt");
        DungeonMod.LOGGER.info("Structures chargees: {}x{}x{}", sizeX, sizeY, sizeZ);

        pileLocalPositions.clear();
        for (int x = 0; x < sizeX; x++) {
            for (int y = 0; y < sizeY; y++) {
                for (int z = 0; z < sizeZ; z++) {
                    BlockState pileState = entryData[x][y][z];
                    BlockState baseState = baseData[x][y][z];
                    boolean pileIsAir = pileState == null || pileState.isAir();
                    boolean baseIsAir = baseState == null || baseState.isAir();
                    if (!pileIsAir && (baseIsAir || !pileState.equals(baseState))) {
                        pileLocalPositions.add(new int[]{x, y, z});
                    }
                }
            }
        }
        DungeonMod.LOGGER.info("Pile positions detected: {}", pileLocalPositions.size());
    }

    private static BlockState[][][] loadNbt(String path) {
        try {
            URL url = DungeonGenerator.class.getResource(path);
            if (url == null) throw new RuntimeException("Fichier introuvable: " + path);
            InputStream is = url.openStream();
            NbtCompound nbt = NbtIo.readCompressed(is, NbtSizeTracker.ofUnlimitedBytes());
            is.close();

            if (sizeX == 0) {
                int[] sizeArr = nbt.getIntArray("size");
                if (sizeArr.length == 0) {
                    NbtList sizeList = nbt.getList("size", 3);
                    sizeX = sizeList.getInt(0); sizeY = sizeList.getInt(1); sizeZ = sizeList.getInt(2);
                } else { sizeX = sizeArr[0]; sizeY = sizeArr[1]; sizeZ = sizeArr[2]; }
            }

            NbtList paletteList = nbt.getList("palette", 10);
            List<BlockState> palette = new ArrayList<>();
            for (int i = 0; i < paletteList.size(); i++) palette.add(parseBlockState(paletteList.getCompound(i)));

            BlockState[][][] data = new BlockState[sizeX][sizeY][sizeZ];
            NbtList blocksList = nbt.getList("blocks", 10);
            for (int i = 0; i < blocksList.size(); i++) {
                NbtCompound entry = blocksList.getCompound(i);
                int[] pos = entry.getIntArray("pos");
                int x, y, z;
                if (pos.length == 0) {
                    NbtList posList = entry.getList("pos", 3);
                    x = posList.getInt(0); y = posList.getInt(1); z = posList.getInt(2);
                } else { x = pos[0]; y = pos[1]; z = pos[2]; }
                int stateIdx = entry.getInt("state");
                if (x >= 0 && x < sizeX && y >= 0 && y < sizeY && z >= 0 && z < sizeZ)
                    data[x][y][z] = palette.get(stateIdx);
            }
            return data;
        } catch (Exception e) { throw new RuntimeException("Impossible de charger " + path, e); }
    }

    private static BlockState parseBlockState(NbtCompound entry) {
        Identifier id = Identifier.of(entry.getString("Name"));
        Block block = Registries.BLOCK.get(id);
        BlockState state = block.getDefaultState();
        if (entry.contains("Properties")) {
            NbtCompound props = entry.getCompound("Properties");
            for (String key : props.getKeys()) {
                Property<?> prop = state.getBlock().getStateManager().getProperty(key);
                if (prop != null) state = applyProperty(state, prop, props.getString(key));
            }
        }
        return state;
    }

    @SuppressWarnings("unchecked")
    private static <T extends Comparable<T>> BlockState applyProperty(BlockState state, Property<T> prop, String value) {
        return prop.parse(value).map(v -> state.with(prop, v)).orElse(state);
    }

    private static class Room {
        final int gx, gz;
        final BlockPos pos;
        final int rotation;
        final RoomType type;

        Room(int gx, int gz, BlockPos pos, int rotation, RoomType type) {
            this.gx = gx; this.gz = gz; this.pos = pos; this.rotation = rotation; this.type = type;
        }

        Room(int gx, int gz, BlockPos pos, int rotation) {
            this(gx, gz, pos, rotation, RoomType.NORMAL);
        }
    }
}
