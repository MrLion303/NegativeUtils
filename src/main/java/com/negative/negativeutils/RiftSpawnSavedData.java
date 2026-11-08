package com.negative.negativeutils;

import java.util.HashSet;
import java.util.Set;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;

/**
 * Registra las regiones y coordenadas donde ya apareció un rift natural.
 * Los registros no se eliminan aunque la entidad sea destruida.
 */
public final class RiftSpawnSavedData extends SavedData {
    private static final String DATA_NAME = "negativeutils_rift_spawned_positions";
    private static final long MIN_DISTANCE_SQUARED = 300L * 300L;

    private final Set<Long> spawnedRegions = new HashSet<>();
    private final Set<Long> spawnedPositions = new HashSet<>();

    public static RiftSpawnSavedData get(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(
                RiftSpawnSavedData::load,
                RiftSpawnSavedData::new,
                DATA_NAME
        );
    }

    public static RiftSpawnSavedData load(CompoundTag tag) {
        RiftSpawnSavedData data = new RiftSpawnSavedData();

        for (long region : tag.getLongArray("SpawnedRegions")) {
            data.spawnedRegions.add(region);
        }
        for (long position : tag.getLongArray("SpawnedPositions")) {
            data.spawnedPositions.add(position);
        }

        return data;
    }

    @Override
    public CompoundTag save(CompoundTag tag) {
        tag.putLongArray("SpawnedRegions", toLongArray(spawnedRegions));
        tag.putLongArray("SpawnedPositions", toLongArray(spawnedPositions));
        return tag;
    }

    public boolean hasSpawnedInRegion(int regionX, int regionZ) {
        return spawnedRegions.contains(pack(regionX, regionZ));
    }

    public boolean isWithinSpawnDistance(int x, int z) {
        for (long packedPosition : spawnedPositions) {
            int previousX = (int) (packedPosition >> 32);
            int previousZ = (int) packedPosition;
            long dx = (long) x - previousX;
            long dz = (long) z - previousZ;

            if (dx * dx + dz * dz < MIN_DISTANCE_SQUARED) {
                return true;
            }
        }
        return false;
    }

    public void markSpawned(int regionX, int regionZ, int x, int z) {
        boolean regionAdded = spawnedRegions.add(pack(regionX, regionZ));
        boolean positionAdded = spawnedPositions.add(pack(x, z));
        if (regionAdded || positionAdded) {
            setDirty();
        }
    }

    private static long pack(int x, int z) {
        return ((long) x << 32) | (z & 0xffffffffL);
    }

    private static long[] toLongArray(Set<Long> values) {
        long[] result = new long[values.size()];
        int index = 0;
        for (long value : values) {
            result[index++] = value;
        }
        return result;
    }
}
