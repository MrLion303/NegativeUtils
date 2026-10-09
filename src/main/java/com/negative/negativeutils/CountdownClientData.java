package com.negative.negativeutils;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public final class CountdownClientData {
    private static final UUID LEGACY_COUNTDOWN_ID =
            UUID.fromString("00000000-0000-0000-0000-000000000001");
    private static List<Entry> countdowns = List.of();

    private CountdownClientData() {}

    public static boolean set(List<Entry> entries) {
        List<Entry> next = List.copyOf(entries);
        boolean metadataChanged = countdowns.size() != next.size();
        if (!metadataChanged) {
            for (int i = 0; i < next.size(); i++) {
                Entry oldEntry = countdowns.get(i);
                Entry newEntry = next.get(i);
                if (!oldEntry.id().equals(newEntry.id())
                        || !oldEntry.name().equals(newEntry.name())
                        || oldEntry.running() != newEntry.running()
                        || oldEntry.finished() != newEntry.finished()
                        || oldEntry.displayed() != newEntry.displayed()
                        || oldEntry.displayColor() != newEntry.displayColor()
                        || !oldEntry.displayText().equals(newEntry.displayText())
                        || !oldEntry.displayPosition().equals(newEntry.displayPosition())) {
                    metadataChanged = true;
                    break;
                }
            }
        }
        countdowns = next;
        return metadataChanged;
    }

    /**
     * Compatibilidad con los paquetes del contador antiguo.
     * El sistema nuevo sincroniza la lista completa mediante set().
     */
    public static void update(
            boolean configured,
            boolean running,
            boolean finished,
            long endTimeMillis,
            long pausedRemainingMillis,
            String displayText,
            int displayColor,
            String displayPosition
    ) {
        List<Entry> updated = new ArrayList<>(countdowns);
        updated.removeIf(entry -> entry.id().equals(LEGACY_COUNTDOWN_ID));
        if (configured) {
            updated.add(new Entry(
                    LEGACY_COUNTDOWN_ID,
                    "Contador",
                    running,
                    finished,
                    endTimeMillis,
                    pausedRemainingMillis,
                    displayText == null ? "" : displayText,
                    displayColor,
                    displayPosition == null ? "ACTIONBAR" : displayPosition,
                    true
            ));
        }
        countdowns = List.copyOf(updated);
    }

    public static List<Entry> getAll() {
        return countdowns;
    }

    public record Entry(
            UUID id,
            String name,
            boolean running,
            boolean finished,
            long endTimeMillis,
            long pausedRemainingMillis,
            String displayText,
            int displayColor,
            String displayPosition,
            boolean displayed
    ) {
        public long remainingMillis() {
            if (finished) return 0;
            return running
                    ? Math.max(0, endTimeMillis - System.currentTimeMillis())
                    : Math.max(0, pausedRemainingMillis);
        }
    }
}
