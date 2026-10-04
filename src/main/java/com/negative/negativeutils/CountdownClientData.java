package com.negative.negativeutils;

public final class CountdownClientData {
    private static boolean configured;
    private static boolean running;
    private static boolean finished;
    private static long endTimeMillis;
    private static long pausedRemainingMillis;

    private static String displayText = "";
    private static int displayColor = 0xFFFFFF;
    private static String displayPosition = "BOSSBAR";

    private CountdownClientData() {
    }

    public static void update(
            boolean newConfigured,
            boolean newRunning,
            boolean newFinished,
            long newEndTimeMillis,
            long newPausedRemainingMillis
    ) {
        update(
                newConfigured,
                newRunning,
                newFinished,
                newEndTimeMillis,
                newPausedRemainingMillis,
                "",
                0xFFFFFF,
                "BOSSBAR"
        );
    }

    public static void update(
            boolean newConfigured,
            boolean newRunning,
            boolean newFinished,
            long newEndTimeMillis,
            long newPausedRemainingMillis,
            String newDisplayText,
            int newDisplayColor,
            String newDisplayPosition
    ) {
        configured = newConfigured;
        running = newRunning;
        finished = newFinished;
        endTimeMillis = newEndTimeMillis;
        pausedRemainingMillis = newPausedRemainingMillis;
        displayText = newDisplayText == null ? "" : newDisplayText;
        displayColor = newDisplayColor & 0xFFFFFF;
        displayPosition = newDisplayPosition == null
                ? "BOSSBAR"
                : newDisplayPosition;
    }

    public static boolean isConfigured() {
        return configured;
    }

    public static boolean isRunning() {
        return running;
    }

    public static boolean isFinished() {
        return finished;
    }

    public static long getEndTimeMillis() {
        return endTimeMillis;
    }

    public static long getRemainingMillis() {
        if (!configured || finished) {
            return 0;
        }

        if (running) {
            return Math.max(0, endTimeMillis - System.currentTimeMillis());
        }

        return Math.max(0, pausedRemainingMillis);
    }

    public static String getDisplayText() {
        return displayText;
    }

    public static int getDisplayColor() {
        return displayColor;
    }

    public static String getDisplayPosition() {
        return displayPosition;
    }
}