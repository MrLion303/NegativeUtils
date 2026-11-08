package com.negative.negativeutils;

import java.util.Random;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraftforge.event.level.ChunkEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Genera rifts naturales de forma determinista al cargar el chunk elegido
 * para cada región de 300x300 bloques.
 */
@Mod.EventBusSubscriber(
        modid = NegativeUtilsMod.MOD_ID,
        bus = Mod.EventBusSubscriber.Bus.FORGE
)
public final class RiftNaturalSpawnEvents {
    private static final int REGION_SIZE = 300;
    private static final float SPAWN_CHANCE = 0.10F;
    private static final int MINIMUM_DISTANCE = 300;

    private RiftNaturalSpawnEvents() {
    }

    @SubscribeEvent
    public static void onChunkLoad(ChunkEvent.Load event) {
        if (!(event.getLevel() instanceof ServerLevel level)
                || !(event.getChunk() instanceof LevelChunk chunk)) {
            return;
        }

        // El interruptor es global y se guarda en los datos del Overworld.
        if (!RiftSpawnSettingsSavedData.get(level.getServer()).isEnabled()) {
            return;
        }

        ChunkPos chunkPos = chunk.getPos();
        int minBlockX = chunkPos.getMinBlockX();
        int minBlockZ = chunkPos.getMinBlockZ();
        int minRegionX = Math.floorDiv(minBlockX, REGION_SIZE);
        int maxRegionX = Math.floorDiv(minBlockX + 15, REGION_SIZE);
        int minRegionZ = Math.floorDiv(minBlockZ, REGION_SIZE);
        int maxRegionZ = Math.floorDiv(minBlockZ + 15, REGION_SIZE);

        RiftSpawnSavedData savedData = RiftSpawnSavedData.get(level);

        // Un chunk puede cruzar el límite entre dos regiones.
        for (int regionX = minRegionX; regionX <= maxRegionX; regionX++) {
            for (int regionZ = minRegionZ; regionZ <= maxRegionZ; regionZ++) {
                trySpawnInRegion(level, chunkPos, savedData, regionX, regionZ);
            }
        }
    }

    private static void trySpawnInRegion(
            ServerLevel level,
            ChunkPos loadedChunk,
            RiftSpawnSavedData savedData,
            int regionX,
            int regionZ
    ) {
        if (savedData.hasSpawnedInRegion(regionX, regionZ)) {
            return;
        }

        Random random = new Random(createRegionSeed(level.getSeed(), regionX, regionZ));
        if (random.nextFloat() >= SPAWN_CHANCE) {
            return;
        }

        int x = regionX * REGION_SIZE + random.nextInt(REGION_SIZE);
        int z = regionZ * REGION_SIZE + random.nextInt(REGION_SIZE);

        // Cada región tiene un único chunk candidato: no se repite la tirada al recargar.
        if (Math.floorDiv(x, 16) != loadedChunk.x
                || Math.floorDiv(z, 16) != loadedChunk.z) {
            return;
        }

        // La distancia mínima también se comprueba entre regiones vecinas.
        if (savedData.isWithinSpawnDistance(x, z)) {
            return;
        }

        int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
        if (y <= level.getMinBuildHeight() + 1 || y >= level.getMaxBuildHeight() - 2) {
            return;
        }

        BlockPos feet = new BlockPos(x, y, z);
        BlockPos floor = feet.below();
        if (!level.getBlockState(floor).isFaceSturdy(level, floor, Direction.UP)
                || !level.getBlockState(feet).isAir()
                || !level.getBlockState(feet.above()).isAir()) {
            return;
        }

        RiftEntity rift = ModEntities.RIFT.get().create(level);
        if (rift == null) {
            return;
        }

        float yaw = random.nextFloat() * 360.0F;
        rift.moveTo(x + 0.5D, y, z + 0.5D, yaw, 0.0F);
        rift.setHealth(rift.getMaxHealth());

        if (!level.noCollision(rift, rift.getBoundingBox()) || !level.addFreshEntity(rift)) {
            return;
        }

        // El registro sobrevive a la destrucción del rift y evita que vuelva a aparecer.
        savedData.markSpawned(regionX, regionZ, x, z);
    }

    private static long createRegionSeed(long worldSeed, int regionX, int regionZ) {
        long seed = worldSeed
                ^ ((long) regionX * 341873128712L)
                ^ ((long) regionZ * 132897987541L);

        seed = (seed ^ (seed >>> 33)) * 0xff51afd7ed558ccdL;
        seed = (seed ^ (seed >>> 33)) * 0xc4ceb9fe1a85ec53L;
        return seed ^ (seed >>> 33);
    }
}
