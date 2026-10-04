package com.negative.negativeutils;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.world.level.saveddata.SavedData;

public class CountdownSavedData extends SavedData {
    private static final String DATA_NAME = "negativeutils_countdown";

    private boolean configured;
    private boolean running;
    private boolean finished;
    private long endTimeMillis;
    private long pausedRemainingMillis;

    private String displayText = "";
    private int displayColor = 0xFFFFFF;
    private String displayPosition = "BOSSBAR";

    public CountdownSavedData() {
    }

    public static CountdownSavedData get(MinecraftServer server) {
        return server.overworld()
                .getDataStorage()
                .computeIfAbsent(
                        CountdownSavedData::load,
                        CountdownSavedData::new,
                        DATA_NAME
                );
    }

    public static CountdownSavedData load(CompoundTag tag) {
        CountdownSavedData data = new CountdownSavedData();

        data.configured = tag.getBoolean("Configured");
        data.running = tag.getBoolean("Running");
        data.finished = tag.getBoolean("Finished");
        data.endTimeMillis = tag.getLong("EndTimeMillis");
        data.pausedRemainingMillis = tag.getLong("PausedRemainingMillis");

        if (tag.contains("DisplayText")) {
            data.displayText = tag.getString("DisplayText");
        }
        if (tag.contains("DisplayColor")) {
            data.displayColor = tag.getInt("DisplayColor");
        }
        if (tag.contains("DisplayPosition")) {
            data.displayPosition = tag.getString("DisplayPosition");
        }

        if (data.running && data.endTimeMillis <= System.currentTimeMillis()) {
            data.running = false;
            data.finished = true;
            data.pausedRemainingMillis = 0;
            data.setDirty();
        }

        return data;
    }

    @Override
    public CompoundTag save(CompoundTag tag) {
        tag.putBoolean("Configured", configured);
        tag.putBoolean("Running", running);
        tag.putBoolean("Finished", finished);
        tag.putLong("EndTimeMillis", endTimeMillis);
        tag.putLong("PausedRemainingMillis", pausedRemainingMillis);
        tag.putString("DisplayText", displayText);
        tag.putInt("DisplayColor", displayColor);
        tag.putString("DisplayPosition", displayPosition);
        return tag;
    }

    public void start(
            long durationMillis,
            String text,
            int color,
            String position
    ) {
        long duration = Math.max(0, durationMillis);

        configured = true;
        finished = duration == 0;
        running = duration > 0;
        pausedRemainingMillis = duration;
        endTimeMillis = System.currentTimeMillis() + duration;

        displayText = text == null ? "" : text;
        if (displayText.length() > 100) {
            displayText = displayText.substring(0, 100);
        }

        displayColor = color & 0xFFFFFF;
        displayPosition = isValidPosition(position) ? position : "BOSSBAR";

        setDirty();
    }

    /** Compatibilidad con llamadas antiguas que solo iniciaban el contador. */
    public void start(long durationMillis) {
        start(durationMillis, displayText, displayColor, displayPosition);
    }

    public void pause() {
        if (!configured || !running) {
            return;
        }

        pausedRemainingMillis = getRemainingMillis();
        running = false;

        if (pausedRemainingMillis <= 0) {
            pausedRemainingMillis = 0;
            finished = true;
        }

        setDirty();
    }

    public void resume() {
        if (!configured || running || finished || pausedRemainingMillis <= 0) {
            return;
        }

        endTimeMillis = System.currentTimeMillis() + pausedRemainingMillis;
        running = true;
        setDirty();
    }

    public void reset() {
        configured = false;
        running = false;
        finished = false;
        endTimeMillis = 0;
        pausedRemainingMillis = 0;
        setDirty();
    }

    public boolean updateFinished() {
        if (!configured || !running || endTimeMillis > System.currentTimeMillis()) {
            return false;
        }

        running = false;
        finished = true;
        pausedRemainingMillis = 0;
        setDirty();
        return true;
    }

    public long getRemainingMillis() {
        if (!configured || finished) {
            return 0;
        }

        if (running) {
            return Math.max(0, endTimeMillis - System.currentTimeMillis());
        }

        return Math.max(0, pausedRemainingMillis);
    }

    public long getEndTimeMillis() {
        return endTimeMillis;
    }

    public boolean isConfigured() {
        return configured;
    }

    public boolean isRunning() {
        return running;
    }

    public boolean isFinished() {
        return finished;
    }

    public String getDisplayText() {
        return displayText;
    }

    public int getDisplayColor() {
        return displayColor;
    }

    public String getDisplayPosition() {
        return displayPosition;
    }

    private static boolean isValidPosition(String position) {
        return "BOSSBAR".equals(position)
                || "ACTIONBAR".equals(position)
                || "SCOREBOARD".equals(position)
                || "TITLE".equals(position);
    }
}