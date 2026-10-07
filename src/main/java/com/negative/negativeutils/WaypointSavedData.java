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
import net.minecraft.world.phys.Vec3;

public class WaypointSavedData extends SavedData {
    private static final String DATA_NAME = "negativeutils_waypoints";
    private final List<Waypoint> waypoints = new ArrayList<>();

    public static WaypointSavedData get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(
                WaypointSavedData::load, WaypointSavedData::new, DATA_NAME);
    }

    public static WaypointSavedData load(CompoundTag tag) {
        WaypointSavedData data = new WaypointSavedData();
        ListTag list = tag.getList("Waypoints", Tag.TAG_COMPOUND);
        for (int i = 0; i < list.size(); i++) {
            CompoundTag t = list.getCompound(i);
            ResourceLocation dimension = ResourceLocation.tryParse(t.getString("Dimension"));
            if (dimension == null || !t.hasUUID("Id") || !t.hasUUID("Owner")) continue;
            String name = sanitizeName(t.getString("Name"));
            String commandId = sanitizeCommandId(t.contains("CommandId") ? t.getString("CommandId") : name.toLowerCase(Locale.ROOT).replace(' ', '_'));
            if (name.isBlank()) name = "Waypoint";
            if (commandId.isBlank()) commandId = "waypoint_" + (i + 1);
            data.waypoints.add(new Waypoint(
                    t.getUUID("Id"), t.getUUID("Owner"), commandId, name, dimension.toString(),
                    t.getDouble("X"), t.getDouble("Y"), t.getDouble("Z"),
                    t.getInt("Color") & 0xFFFFFF,
                    t.contains("Shape") ? t.getInt("Shape") : 1,
                    !t.contains("Visible") || t.getBoolean("Visible"),
                    t.contains("Icon") ? sanitizeIcon(t.getString("Icon")) : "◆",
                    t.contains("Corner") ? sanitizeCorner(t.getString("Corner")) : "TOP_LEFT",
                    t.hasUUID("TrackedPlayer") ? t.getUUID("TrackedPlayer") : null));
        }
        return data;
    }

    @Override
    public CompoundTag save(CompoundTag tag) {
        ListTag list = new ListTag();
        for (Waypoint w : waypoints) {
            CompoundTag t = new CompoundTag();
            t.putUUID("Id", w.id()); t.putUUID("Owner", w.owner());
            t.putString("CommandId", w.commandId()); t.putString("Name", w.name());
            t.putString("Dimension", w.dimension());
            t.putDouble("X", w.x()); t.putDouble("Y", w.y()); t.putDouble("Z", w.z());
            t.putInt("Color", w.color()); t.putInt("Shape", w.shape());
            t.putBoolean("Visible", w.visible()); t.putString("Icon", w.icon());
            t.putString("Corner", w.corner());
            if (w.trackedPlayer() != null) t.putUUID("TrackedPlayer", w.trackedPlayer());
            list.add(t);
        }
        tag.put("Waypoints", list);
        return tag;
    }

    public Waypoint add(UUID owner, String commandId, String name, String dimension,
                        Vec3 pos, int color, String icon, String corner) {
        ResourceLocation dim = ResourceLocation.tryParse(dimension);
        if (owner == null || dim == null || pos == null) return null;
        String cleanName = sanitizeName(name);
        if (cleanName.isBlank()) cleanName = nextAutomaticName();
        String cleanId = sanitizeCommandId(commandId);
        if (cleanId.isBlank()) cleanId = nextAutomaticId();
        if (getByCommandId(cleanId) != null) return null;
        Waypoint waypoint = new Waypoint(UUID.randomUUID(), owner, cleanId, cleanName,
                dim.toString(), pos.x, pos.y, pos.z, color & 0xFFFFFF, 1, true,
                sanitizeIcon(icon), sanitizeCorner(corner), null);
        waypoints.add(waypoint);
        setDirty();
        return waypoint;
    }

    public Waypoint addTracked(UUID owner, String commandId, String name, String dimension,
                               Vec3 pos, int color, String icon, String corner, UUID target) {
        ResourceLocation dim = ResourceLocation.tryParse(dimension);
        if (owner == null || target == null || dim == null || pos == null) return null;
        String cleanName = sanitizeName(name);
        if (cleanName.isBlank()) cleanName = "Jugador_" + target.toString().substring(0, 8);
        String cleanId = sanitizeCommandId(commandId);
        if (cleanId.isBlank()) cleanId = nextAutomaticId();
        if (getByCommandId(cleanId) != null) return null;
        Waypoint waypoint = new Waypoint(UUID.randomUUID(), owner, cleanId, cleanName,
                dim.toString(), pos.x, pos.y, pos.z, color & 0xFFFFFF, 1, true,
                sanitizeIcon(icon), sanitizeCorner(corner), target);
        waypoints.add(waypoint);
        setDirty();
        return waypoint;
    }

    public Waypoint replaceTrackedTarget(UUID id, net.minecraft.server.level.ServerPlayer target) {
        Waypoint w = getById(id);
        if (w == null || target == null) return null;
        Waypoint updated = new Waypoint(w.id(), w.owner(), w.commandId(), w.name(),
                target.level().dimension().location().toString(),
                target.getX(), target.getY(), target.getZ(), w.color(), w.shape(),
                w.visible(), w.icon(), w.corner(), target.getUUID());
        waypoints.set(waypoints.indexOf(w), updated);
        setDirty();
        return updated;
    }

    public Waypoint getById(UUID id) {
        for (Waypoint waypoint : waypoints) if (waypoint.id().equals(id)) return waypoint;
        return null;
    }

    public Waypoint getByName(String name) {
        String clean = sanitizeName(name);
        for (Waypoint waypoint : waypoints) if (waypoint.name().equals(clean)) return waypoint;
        return null;
    }

    public Waypoint getByCommandId(String commandId) {
        String clean = sanitizeCommandId(commandId);
        for (Waypoint waypoint : waypoints) if (waypoint.commandId().equals(clean)) return waypoint;
        return null;
    }

    public boolean update(UUID id, String commandId, String name, String dimension, Vec3 pos,
                          int color, String icon, String corner) {
        for (int i = 0; i < waypoints.size(); i++) {
            Waypoint w = waypoints.get(i);
            if (!w.id().equals(id)) continue;
            ResourceLocation dim = ResourceLocation.tryParse(dimension);
            String cleanId = sanitizeCommandId(commandId);
            if (dim == null || pos == null || cleanId.isBlank()) return false;
            Waypoint duplicate = getByCommandId(cleanId);
            if (duplicate != null && !duplicate.id().equals(id)) return false;
            String cleanName = sanitizeName(name);
            if (cleanName.isBlank()) cleanName = w.name();
            waypoints.set(i, new Waypoint(w.id(), w.owner(), cleanId, cleanName, dim.toString(),
                    pos.x, pos.y, pos.z, color & 0xFFFFFF, w.shape(), w.visible(),
                    sanitizeIcon(icon), sanitizeCorner(corner), w.trackedPlayer()));
            setDirty();
            return true;
        }
        return false;
    }

    public boolean setVisible(UUID id, boolean visible) {
        Waypoint w = getById(id);
        if (w == null) return false;
        if (w.visible() == visible) return true;
        waypoints.set(waypoints.indexOf(w), new Waypoint(w.id(), w.owner(), w.commandId(),
                w.name(), w.dimension(), w.x(), w.y(), w.z(), w.color(), w.shape(),
                visible, w.icon(), w.corner(), w.trackedPlayer()));
        setDirty();
        return true;
    }

    public boolean removeById(UUID id) {
        boolean removed = waypoints.removeIf(w -> w.id().equals(id));
        if (removed) setDirty();
        return removed;
    }

    public int removeAll() {
        int count = waypoints.size();
        if (count > 0) { waypoints.clear(); setDirty(); }
        return count;
    }

    public List<Waypoint> getWaypoints() { return List.copyOf(waypoints); }

    private String nextAutomaticName() {
        int n = 1;
        while (getByName("Waypoint_" + n) != null) n++;
        return "Waypoint_" + n;
    }

    private String nextAutomaticId() {
        int n = 1;
        while (getByCommandId("waypoint_" + n) != null) n++;
        return "waypoint_" + n;
    }

    public static String sanitizeName(String name) {
        if (name == null) return "";
        String clean = name.replaceAll("[\\p{Cntrl}§]", "").trim().replaceAll("\\s+", "_");
        return clean.length() > 32 ? clean.substring(0, 32) : clean;
    }

    public static String sanitizeCommandId(String id) {
        if (id == null) return "";
        String clean = id.trim().toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9_-]", "_");
        return clean.length() > 48 ? clean.substring(0, 48) : clean;
    }

    public static String sanitizeIcon(String icon) {
        if (icon == null || icon.isBlank()) return "◆";
        String clean = icon.trim();
        return clean.length() > 4 ? clean.substring(0, 4) : clean;
    }

    public static String sanitizeCorner(String corner) {
        if (corner == null) return "TOP_LEFT";
        return switch (corner.toUpperCase(Locale.ROOT)) {
            case "TOP_RIGHT", "BOTTOM_LEFT", "BOTTOM_RIGHT" -> corner.toUpperCase(Locale.ROOT);
            default -> "TOP_LEFT";
        };
    }

    public record Waypoint(UUID id, UUID owner, String commandId, String name, String dimension,
                          double x, double y, double z, int color, int shape, boolean visible,
                          String icon, String corner, UUID trackedPlayer) {
        public boolean tracksPlayer() { return trackedPlayer != null; }
    }
}