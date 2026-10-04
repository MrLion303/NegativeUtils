package com.negative.negativeutils;

import java.util.List;

public final class TrailClientData {
    private static List<TrailSavedData.Trail> trails = List.of();

    private TrailClientData() {
    }

    public static void setTrails(List<TrailSavedData.Trail> updatedTrails) {
        trails = List.copyOf(updatedTrails);
    }

    public static List<TrailSavedData.Trail> getTrails() {
        return trails;
    }

    public static void clear() {
        trails = List.of();
    }
}
