package com.negative.negativeutils;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.level.saveddata.SavedData;

public class WaypointSavedData extends SavedData {
    private static final String DATA_NAME = "negativeutils_waypoints";
    private final List<Waypoint> waypoints = new ArrayList<>();

    public static WaypointSavedData get(MinecraftServer server) {
        return server.overworld()
                .getDataStorage()
                .computeIfAbsent(
                        WaypointSavedData::load,
                        WaypointSavedData::new,
                        DATA_NAME
                );
    }

    public static WaypointSavedData load(CompoundTag tag) {
        WaypointSavedData data = new WaypointSavedData();
        ListTag savedWaypoints = tag.getList("Waypoints", Tag.TAG_COMPOUND);

        for (int i = 0; i < savedWaypoints.size(); i++) {
            CompoundTag saved = savedWaypoints.getCompound(i);
            ResourceLocation dimension = ResourceLocation.tryParse(
                    saved.getString("Dimension")
            );
            try {
                if (dimension == null
                        || !saved.hasUUID("Id")
                        || !saved.hasUUID("Owner")) {
                    continue;
                }
                String name = sanitizeName(saved.getString("Name"));
                int color = saved.getInt("Color") & 0xFFFFFF;
                data.waypoints.add(new Waypoint(
                        saved.getUUID("Id"),
                        saved.getUUID("Owner"),
                        name,
                        dimension.toString(),
                        saved.getDouble("X"),
                        saved.getDouble("Y"),
                        saved.getDouble("Z"),
                        color,
                        1
                ));
            } catch (IllegalArgumentException ignored) {
                // Ignore malformed entries without preventing other saved waypoints from loading.
            }
        }

        return data;
    }

    @Override
    public CompoundTag save(CompoundTag tag) {
        ListTag savedWaypoints = new ListTag();
        for (Waypoint waypoint : waypoints) {
            CompoundTag saved = new CompoundTag();
            saved.putUUID("Id", waypoint.id());
            saved.putUUID("Owner", waypoint.owner());
            saved.putString("Name", waypoint.name());
            saved.putString("Dimension", waypoint.dimension());
            saved.putDouble("X", waypoint.x());
            saved.putDouble("Y", waypoint.y());
            saved.putDouble("Z", waypoint.z());
            saved.putInt("Color", waypoint.color());
            saved.putInt("Shape", waypoint.shape());
            savedWaypoints.add(saved);
        }
        tag.put("Waypoints", savedWaypoints);
        return tag;
    }

    public Waypoint add(
            UUID owner,
            String name,
            String dimension,
            Vec3 position,
            int color
    ) {
        String cleanName = sanitizeName(name);
        ResourceLocation dimensionId = ResourceLocation.tryParse(dimension);
        if (owner == null
                || dimensionId == null
                || position == null
                || !Double.isFinite(position.x)
                || !Double.isFinite(position.y)
                || !Double.isFinite(position.z)) {
            return null;
        }

        Waypoint waypoint = new Waypoint(
                UUID.randomUUID(),
                owner,
                cleanName,
                dimensionId.toString(),
                position.x,
                position.y,
                position.z,
                color & 0xFFFFFF,
                1
        );
        waypoints.add(waypoint);
        setDirty();
        return waypoint;
    }

    public Waypoint removeById(
            UUID id,
            UUID actor,
            boolean mayDeleteAny,
            String dimension
    ) {
        for (int i = 0; i < waypoints.size(); i++) {
            Waypoint waypoint = waypoints.get(i);
            if (!waypoint.id().equals(id)
                    || !waypoint.dimension().equals(dimension)
                    || (!mayDeleteAny && !waypoint.owner().equals(actor))) {
                continue;
            }

            setDirty();
            return waypoints.remove(i);
        }
        return null;
    }

    public int removeAll() {
        int removed = waypoints.size();
        if (removed > 0) {
            waypoints.clear();
            setDirty();
        }
        return removed;
    }

    public List<Waypoint> getWaypoints() {
        return List.copyOf(waypoints);
    }

    public static String sanitizeName(String name) {
        if (name == null) {
            return "";
        }
        String clean = name.replaceAll("[\\p{Cntrl}§]", "").trim();
        return clean.length() > 32 ? clean.substring(0, 32) : clean;
    }

    public record Waypoint(
            UUID id,
            UUID owner,
            String name,
            String dimension,
            double x,
            double y,
            double z,
            int color,
            int shape
    ) {
    }
}
