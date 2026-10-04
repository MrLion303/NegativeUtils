package com.negative.negativeutils;

import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.phys.Vec3;

public class MomentoSavedData extends SavedData {
    private static final String DATA_NAME = "negativeutils_momento";

    private boolean configured;
    private String dimension = Level.OVERWORLD.location().toString();
    private double x;
    private double y;
    private double z;

    public static MomentoSavedData get(MinecraftServer server) {
        return server.overworld()
                .getDataStorage()
                .computeIfAbsent(
                        MomentoSavedData::load,
                        MomentoSavedData::new,
                        DATA_NAME
                );
    }

    public static MomentoSavedData load(CompoundTag tag) {
        MomentoSavedData data = new MomentoSavedData();
        data.configured = tag.getBoolean("Configured");

        if (data.configured) {
            String savedDimension = tag.getString("Dimension");
            if (ResourceLocation.tryParse(savedDimension) == null) {
                data.configured = false;
                return data;
            }

            data.dimension = savedDimension;
            data.x = tag.getDouble("X");
            data.y = tag.getDouble("Y");
            data.z = tag.getDouble("Z");
        }

        return data;
    }

    @Override
    public CompoundTag save(CompoundTag tag) {
        tag.putBoolean("Configured", configured);
        if (configured) {
            tag.putString("Dimension", dimension);
            tag.putDouble("X", x);
            tag.putDouble("Y", y);
            tag.putDouble("Z", z);
        }
        return tag;
    }

    public void setLocation(ServerLevel level, Vec3 position) {
        configured = true;
        dimension = level.dimension().location().toString();
        x = position.x;
        y = position.y;
        z = position.z;
        setDirty();
    }

    public boolean isConfigured() {
        return configured;
    }

    public ResourceKey<Level> getDimension() {
        ResourceLocation location = ResourceLocation.tryParse(dimension);
        if (location == null) {
            throw new IllegalStateException(
                    "La dimensión guardada para /yo no es válida."
            );
        }
        return ResourceKey.create(Registries.DIMENSION, location);
    }

    public Vec3 getPosition() {
        return new Vec3(x, y, z);
    }
}
