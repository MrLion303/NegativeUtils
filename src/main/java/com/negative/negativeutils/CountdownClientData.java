package com.negative.negativeutils;

import java.util.List;
import java.util.UUID;

public final class CountdownClientData {
    private static List<Entry> countdowns = List.of();
    private CountdownClientData() {}
    public static void set(List<Entry> entries) { countdowns = List.copyOf(entries); }
    public static List<Entry> getAll() { return countdowns; }
    public record Entry(UUID id, String name, boolean running, boolean finished,
                        long endTimeMillis, long pausedRemainingMillis,
                        String displayText, int displayColor, String displayPosition) {
        public long remainingMillis() {
            if (finished) return 0;
            return running ? Math.max(0, endTimeMillis-System.currentTimeMillis()) : Math.max(0, pausedRemainingMillis);
        }
    }
}