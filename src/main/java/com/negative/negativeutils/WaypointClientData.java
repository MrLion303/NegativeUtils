package com.negative.negativeutils;

import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

public final class WaypointClientData {
    private static List<WaypointSavedData.Waypoint> waypoints = List.of();

    private WaypointClientData() {
    }

    public static void setWaypoints(
            List<WaypointSavedData.Waypoint> updatedWaypoints
    ) {
        waypoints = List.copyOf(updatedWaypoints);
    }

    public static void appendWaypoints(
            List<WaypointSavedData.Waypoint> additionalWaypoints
    ) {
        java.util.ArrayList<WaypointSavedData.Waypoint> combined =
                new java.util.ArrayList<>(waypoints.size()
                        + additionalWaypoints.size());
        combined.addAll(waypoints);
        combined.addAll(additionalWaypoints);
        waypoints = List.copyOf(combined);
    }

    public static List<WaypointSavedData.Waypoint> getWaypoints() {
        return waypoints;
    }

    public static Vec3 resolvePosition(WaypointSavedData.Waypoint waypoint) {
        if (!waypoint.tracksPlayer()) return new Vec3(waypoint.x(), waypoint.y(), waypoint.z());
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return null;
        for (Entity entity : mc.level.entitiesForRendering()) {
            if (entity.getUUID().equals(waypoint.trackedPlayer())) return entity.position();
        }
        return null;
    }

    public static void clear() {
        waypoints = List.of();
    }
}
