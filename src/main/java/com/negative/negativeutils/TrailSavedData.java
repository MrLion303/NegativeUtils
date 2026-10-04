package com.negative.negativeutils;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;

public class TrailSavedData extends SavedData {
    private static final String DATA_NAME = "negativeutils_trails";
    private final List<Trail> trails = new ArrayList<>();

    public static TrailSavedData get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(
                TrailSavedData::load,
                TrailSavedData::new,
                DATA_NAME
        );
    }

    public static TrailSavedData load(CompoundTag tag) {
        TrailSavedData data = new TrailSavedData();
        ListTag savedTrails = tag.getList("Trails", Tag.TAG_COMPOUND);

        for (int i = 0; i < savedTrails.size(); i++) {
            CompoundTag saved = savedTrails.getCompound(i);
            Trail trail = Trail.load(saved);
            if (trail != null) {
                data.trails.add(trail);
            }
        }

        // Conserva el sistema anterior si existe un único camino antiguo.
        if (data.trails.isEmpty()) {
            ListTag oldPoints = tag.getList("Points", Tag.TAG_COMPOUND);
            if (!oldPoints.isEmpty()) {
                Trail legacy = new Trail(
                        UUID.randomUUID(),
                        "Sendero 1",
                        true,
                        255,
                        199,
                        31,
                        230
                );
                for (int i = 0; i < oldPoints.size(); i++) {
                    CompoundTag point = oldPoints.getCompound(i);
                    legacy.points.add(new TrailPoint(
                            point.getString("Dimension"),
                            point.getDouble("X"),
                            point.getDouble("Y"),
                            point.getDouble("Z")
                    ));
                }
                data.trails.add(legacy);
            }
        }

        return data;
    }

    @Override
    public CompoundTag save(CompoundTag tag) {
        ListTag savedTrails = new ListTag();
        for (Trail trail : trails) {
            savedTrails.add(trail.save());
        }
        tag.put("Trails", savedTrails);
        return tag;
    }

    public Trail create(String name) {
        String cleanName = sanitizeName(name);
        if (cleanName.isBlank()) {
            return null;
        }

        Trail trail = new Trail(
                UUID.randomUUID(),
                cleanName,
                true,
                255,
                199,
                31,
                230
        );
        trails.add(trail);
        setDirty();
        return trail;
    }

    public Trail getById(UUID id) {
        for (Trail trail : trails) {
            if (trail.id.equals(id)) {
                return trail;
            }
        }
        return null;
    }

    public Trail getByName(String name) {
        String cleanName = sanitizeName(name);
        for (Trail trail : trails) {
            if (trail.name.equals(cleanName)) {
                return trail;
            }
        }
        return null;
    }

    public boolean nameExists(String name) {
        return getByName(name) != null;
    }

    public String nextAutomaticName() {
        int number = 1;
        while (nameExists("Sendero " + number)) {
            number++;
        }
        return "Sendero " + number;
    }

    public boolean remove(UUID id) {
        Trail trail = getById(id);
        if (trail == null) {
            return false;
        }
        trails.remove(trail);
        setDirty();
        return true;
    }

    public List<Trail> getTrails() {
        return List.copyOf(trails);
    }

    public boolean setVisible(UUID id, boolean visible) {
        Trail trail = getById(id);
        if (trail == null || trail.visible == visible) {
            return trail != null;
        }
        trail.visible = visible;
        setDirty();
        return true;
    }

    public boolean setSettings(
            UUID id,
            int red,
            int green,
            int blue,
            int opacity
    ) {
        Trail trail = getById(id);
        if (trail == null) {
            return false;
        }
        trail.red = clamp(red);
        trail.green = clamp(green);
        trail.blue = clamp(blue);
        trail.opacity = clamp(opacity);
        setDirty();
        return true;
    }

    public boolean addPoint(
            UUID id,
            ServerLevel level,
            int blockX,
            int blockY,
            int blockZ
    ) {
        Trail trail = getById(id);
        if (trail == null) {
            return false;
        }

        trail.points.add(new TrailPoint(
                level.dimension().location().toString(),
                blockX + 0.5,
                blockY + 1.05,
                blockZ + 0.5
        ));
        setDirty();
        return true;
    }

    public boolean resetPoints(
            UUID id,
            ServerLevel level,
            int blockX,
            int blockY,
            int blockZ
    ) {
        Trail trail = getById(id);
        if (trail == null) {
            return false;
        }

        trail.points.clear();
        return addPoint(id, level, blockX, blockY, blockZ);
    }

    public static String sanitizeName(String name) {
        if (name == null) {
            return "";
        }
        String clean = name.replaceAll("[\\p{Cntrl}§]", "").trim();
        return clean.length() > 32 ? clean.substring(0, 32) : clean;
    }

    private static int clamp(int value) {
        return Math.max(0, Math.min(255, value));
    }

    public static final class Trail {
        private final UUID id;
        private String name;
        private boolean visible;
        private int red;
        private int green;
        private int blue;
        private int opacity;
        private final List<TrailPoint> points = new ArrayList<>();

        public Trail(
                UUID id,
                String name,
                boolean visible,
                int red,
                int green,
                int blue,
                int opacity
        ) {
            this.id = id;
            this.name = sanitizeName(name);
            this.visible = visible;
            this.red = clamp(red);
            this.green = clamp(green);
            this.blue = clamp(blue);
            this.opacity = clamp(opacity);
        }

        private static Trail load(CompoundTag tag) {
            if (!tag.hasUUID("Id")) {
                return null;
            }

            String name = sanitizeName(tag.getString("Name"));
            if (name.isBlank()) {
                return null;
            }

            Trail trail = new Trail(
                    tag.getUUID("Id"),
                    name,
                    !tag.contains("Visible") || tag.getBoolean("Visible"),
                    tag.contains("Red") ? tag.getInt("Red") : 255,
                    tag.contains("Green") ? tag.getInt("Green") : 199,
                    tag.contains("Blue") ? tag.getInt("Blue") : 31,
                    tag.contains("Opacity") ? tag.getInt("Opacity") : 230
            );

            ListTag savedPoints = tag.getList("Points", Tag.TAG_COMPOUND);
            for (int i = 0; i < savedPoints.size(); i++) {
                CompoundTag point = savedPoints.getCompound(i);
                ResourceLocation dimension =
                        ResourceLocation.tryParse(point.getString("Dimension"));
                if (dimension == null) {
                    continue;
                }

                trail.points.add(new TrailPoint(
                        dimension.toString(),
                        point.getDouble("X"),
                        point.getDouble("Y"),
                        point.getDouble("Z")
                ));
            }

            return trail;
        }

        private CompoundTag save() {
            CompoundTag tag = new CompoundTag();
            tag.putUUID("Id", id);
            tag.putString("Name", name);
            tag.putBoolean("Visible", visible);
            tag.putInt("Red", red);
            tag.putInt("Green", green);
            tag.putInt("Blue", blue);
            tag.putInt("Opacity", opacity);

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

        public UUID id() {
            return id;
        }

        public String name() {
            return name;
        }

        public boolean visible() {
            return visible;
        }

        public int red() {
            return red;
        }

        public int green() {
            return green;
        }

        public int blue() {
            return blue;
        }

        public int opacity() {
            return opacity;
        }

        public List<TrailPoint> points() {
            return List.copyOf(points);
        }
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
