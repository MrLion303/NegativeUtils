package com.negative.negativeutils;

import java.util.List;

public final class TrailClientData {
    private static List<TrailSavedData.TrailPoint> points = List.of();

    private static int red = 255;
    private static int green = 199;
    private static int blue = 31;
    private static int alpha = 230;

    private TrailClientData() {
    }

    public static void setPoints(List<TrailSavedData.TrailPoint> newPoints) {
        points = List.copyOf(newPoints);
    }

    public static List<TrailSavedData.TrailPoint> getPoints() {
        return points;
    }

    public static void setColor(int newRed, int newGreen, int newBlue, int newAlpha) {
        red = clamp(newRed);
        green = clamp(newGreen);
        blue = clamp(newBlue);
        alpha = clamp(newAlpha);
    }

    public static int getRed() {
        return red;
    }

    public static int getGreen() {
        return green;
    }

    public static int getBlue() {
        return blue;
    }

    public static int getAlpha() {
        return alpha;
    }

    public static void clear() {
        points = List.of();
    }

    private static int clamp(int value) {
        return Math.max(0, Math.min(255, value));
    }
}