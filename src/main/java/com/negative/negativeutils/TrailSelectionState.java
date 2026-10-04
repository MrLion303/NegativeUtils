package com.negative.negativeutils;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class TrailSelectionState {
    private static final Map<UUID, UUID> SELECTED = new HashMap<>();

    private TrailSelectionState() {
    }

    public static void select(UUID playerId, UUID trailId) {
        SELECTED.put(playerId, trailId);
    }

    public static UUID get(UUID playerId) {
        return SELECTED.get(playerId);
    }

    public static void clear(UUID playerId) {
        SELECTED.remove(playerId);
    }
}
