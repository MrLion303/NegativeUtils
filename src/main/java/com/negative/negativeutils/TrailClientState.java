package com.negative.negativeutils;

public final class TrailClientState {
    private static boolean visible = true;

    private TrailClientState() {
    }

    public static boolean isVisible() {
        return visible;
    }

    public static void toggleVisible() {
        visible = !visible;
    }
}