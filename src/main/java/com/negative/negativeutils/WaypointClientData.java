package com.negative.negativeutils;

import java.util.List;

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

    public static void clear() {
        waypoints = List.of();
    }
}
