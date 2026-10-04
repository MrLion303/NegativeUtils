package com.negative.negativeutils;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.world.level.saveddata.SavedData;

public class TrailSavedData extends SavedData {
    private static final String DATA_NAME = "negativeutils_trail";

    private final List<TrailPoint> points = new ArrayList<>();

    public TrailSavedData() {
    }

    public static TrailSavedData get(MinecraftServer server) {
        return server.overworld()
                .getDataStorage()
                .computeIfAbsent(TrailSavedData::load, TrailSavedData::new, DATA_NAME);
    }

    public void addPoint(ServerLevel level, int blockX, int blockY, int blockZ) {
        double x = blockX + 0.5;
        double y = blockY + 1.05;
        double z = blockZ + 0.5;

        points.add(new TrailPoint(
                level.dimension().location().toString(),
                x,
                y,
                z
        ));

        setDirty();
    }

    /**
     * Quita el punto más cercano, si está a dos bloques o menos
     * del bloque que ha seleccionado el administrador.
     */
    public boolean removeNearestPoint(
            ServerLevel level,
            double blockX,
            double blockY,
            double blockZ
    ) {
        String dimension = level.dimension().location().toString();

        double targetX = blockX + 0.5;
        double targetY = blockY + 1.05;
        double targetZ = blockZ + 0.5;

        int nearestIndex = -1;
        double nearestDistanceSquared = 4.0;

        for (int i = 0; i < points.size(); i++) {
            TrailPoint point = points.get(i);

            if (!point.dimension().equals(dimension)) {
                continue;
            }

            double dx = point.x() - targetX;
            double dy = point.y() - targetY;
            double dz = point.z() - targetZ;
            double distanceSquared = dx * dx + dy * dy + dz * dz;

            if (distanceSquared <= nearestDistanceSquared) {
                nearestDistanceSquared = distanceSquared;
                nearestIndex = i;
            }
        }

        if (nearestIndex < 0) {
            return false;
        }

        points.remove(nearestIndex);
        setDirty();
        return true;
    }

    public List<TrailPoint> getPoints() {
        return List.copyOf(points);
    }

    public void clearPoints() {
        if (!points.isEmpty()) {
            points.clear();
            setDirty();
        }
    }

    public static TrailSavedData load(CompoundTag tag) {
        TrailSavedData data = new TrailSavedData();
        ListTag savedPoints = tag.getList("Points", Tag.TAG_COMPOUND);

        for (int i = 0; i < savedPoints.size(); i++) {
            CompoundTag pointTag = savedPoints.getCompound(i);

            data.points.add(new TrailPoint(
                    pointTag.getString("Dimension"),
                    pointTag.getDouble("X"),
                    pointTag.getDouble("Y"),
                    pointTag.getDouble("Z")
            ));
        }

        return data;
    }

    @Override
    public CompoundTag save(CompoundTag tag) {
        ListTag savedPoints = new ListTag();

        for (TrailPoint point : points) {
            CompoundTag pointTag = new CompoundTag();
            pointTag.putString("Dimension", point.dimension());
            pointTag.putDouble("X", point.x());
            pointTag.putDouble("Y", point.y());
            pointTag.putDouble("Z", point.z());
            savedPoints.add(pointTag);
        }

        tag.put("Points", savedPoints);
        return tag;
    }

    public record TrailPoint(
            String dimension,
            double x,
            double y,
            double z
    ) {
        public ResourceLocation dimensionId() {
            return ResourceLocation.tryParse(dimension);
        }
    }
}