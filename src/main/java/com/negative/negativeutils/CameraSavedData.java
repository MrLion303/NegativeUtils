package com.negative.negativeutils;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;

public final class CameraSavedData extends SavedData {
    private static final String DATA_NAME = "negativeutils_cameras";
    private final List<Camera> cameras = new ArrayList<>();

    public static CameraSavedData get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(
                CameraSavedData::load, CameraSavedData::new, DATA_NAME);
    }

    public static CameraSavedData load(CompoundTag tag) {
        CameraSavedData data = new CameraSavedData();
        ListTag list = tag.getList("Cameras", Tag.TAG_COMPOUND);
        for (int i = 0; i < list.size(); i++) {
            CompoundTag t = list.getCompound(i);
            if (!t.hasUUID("Id")) continue;
            String id = sanitizeId(t.getString("CommandId"));
            if (id.isBlank()) continue;
            String name = sanitizeName(t.getString("Name"));
            if (name.isBlank()) name = id;
            data.cameras.add(new Camera(
                    t.getUUID("Id"),
                    t.getUUID("Owner"),
                    id,
                    name,
                    t.getString("TargetType"),
                    t.getString("Dimension"),
                    t.getDouble("X"), t.getDouble("Y"), t.getDouble("Z"),
                    t.getInt("EntityId"),
                    t.getString("PlayerName"),
                    t.hasUUID("PlayerUuid") ? t.getUUID("PlayerUuid") : null,
                    t.getBoolean("ForceLook")
            ));
        }
        return data;
    }

    @Override
    public CompoundTag save(CompoundTag tag) {
        ListTag list = new ListTag();
        for (Camera c : cameras) {
            CompoundTag t = new CompoundTag();
            t.putUUID("Id", c.id());
            t.putUUID("Owner", c.owner());
            t.putString("CommandId", c.commandId());
            t.putString("Name", c.name());
            t.putString("TargetType", c.targetType());
            t.putString("Dimension", c.dimension());
            t.putDouble("X", c.x());
            t.putDouble("Y", c.y());
            t.putDouble("Z", c.z());
            t.putInt("EntityId", c.entityId());
            t.putString("PlayerName", c.playerName());
            if (c.playerUuid() != null) t.putUUID("PlayerUuid", c.playerUuid());
            t.putBoolean("ForceLook", c.forceLook());
            list.add(t);
        }
        tag.put("Cameras", list);
        return tag;
    }

    public Camera add(UUID owner, String commandId, String name, String targetType,
                      String dimension, double x, double y, double z, int entityId,
                      String playerName, UUID playerUuid, boolean forceLook) {
        String cleanId = sanitizeId(commandId);
        if (owner == null || cleanId.isBlank() || getByCommandId(cleanId) != null) return null;
        Camera camera = new Camera(UUID.randomUUID(), owner, cleanId,
                sanitizeName(name).isBlank() ? cleanId : sanitizeName(name),
                sanitizeTargetType(targetType), dimension == null ? "" : dimension,
                x, y, z, entityId, playerName == null ? "" : playerName.trim(),
                playerUuid, forceLook);
        cameras.add(camera);
        setDirty();
        return camera;
    }

    public boolean update(UUID id, String commandId, String name, String targetType,
                          String dimension, double x, double y, double z, int entityId,
                          String playerName, UUID playerUuid, boolean forceLook) {
        Camera current = getById(id);
        if (current == null) return false;
        String cleanId = sanitizeId(commandId);
        if (cleanId.isBlank()) return false;
        Camera duplicate = getByCommandId(cleanId);
        if (duplicate != null && !duplicate.id().equals(id)) return false;
        String cleanName = sanitizeName(name);
        if (cleanName.isBlank()) cleanName = current.name();
        cameras.set(cameras.indexOf(current), new Camera(
                current.id(), current.owner(), cleanId, cleanName,
                sanitizeTargetType(targetType), dimension == null ? "" : dimension,
                x, y, z, entityId, playerName == null ? "" : playerName.trim(),
                playerUuid, forceLook));
        setDirty();
        return true;
    }

    public Camera getById(UUID id) {
        for (Camera camera : cameras) if (camera.id().equals(id)) return camera;
        return null;
    }

    public Camera getByCommandId(String id) {
        String clean = sanitizeId(id);
        for (Camera camera : cameras) if (camera.commandId().equals(clean)) return camera;
        return null;
    }

    public List<Camera> getCameras() {
        return List.copyOf(cameras);
    }

    public boolean remove(UUID id) {
        boolean removed = cameras.removeIf(camera -> camera.id().equals(id));
        if (removed) setDirty();
        return removed;
    }

    public static String sanitizeId(String id) {
        if (id == null) return "";
        String clean = id.trim().toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9_-]", "_");
        return clean.length() > 48 ? clean.substring(0, 48) : clean;
    }

    public static String sanitizeName(String name) {
        if (name == null) return "";
        String clean = name.replaceAll("[\\p{Cntrl}§]", "").trim();
        return clean.length() > 48 ? clean.substring(0, 48) : clean;
    }

    public static String sanitizeTargetType(String type) {
        return switch (type == null ? "" : type.toUpperCase(Locale.ROOT)) {
            case "ENTITY", "PLAYER" -> type.toUpperCase(Locale.ROOT);
            default -> "COORDS";
        };
    }

    public record Camera(
            UUID id, UUID owner, String commandId, String name,
            String targetType, String dimension,
            double x, double y, double z, int entityId,
            String playerName, UUID playerUuid, boolean forceLook) {
    }
}
