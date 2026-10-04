package com.negative.negativeutils;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;

public class CountdownSavedData extends SavedData {
    private static final String DATA_NAME = "negativeutils_countdown";
    private final List<Countdown> countdowns = new ArrayList<>();

    public static CountdownSavedData get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(
                CountdownSavedData::load, CountdownSavedData::new, DATA_NAME
        );
    }

    public static CountdownSavedData load(CompoundTag tag) {
        CountdownSavedData data = new CountdownSavedData();
        ListTag list = tag.getList("Countdowns", Tag.TAG_COMPOUND);
        for (int i = 0; i < list.size(); i++) {
            Countdown countdown = Countdown.load(list.getCompound(i));
            if (countdown != null) data.countdowns.add(countdown);
        }
        // Migración sencilla del formato anterior de un solo contador.
        if (data.countdowns.isEmpty() && tag.getBoolean("Configured")) {
            Countdown old = new Countdown(
                    UUID.randomUUID(), "Contador 1",
                    tag.getBoolean("Running"), tag.getBoolean("Finished"),
                    tag.getLong("EndTimeMillis"), tag.getLong("PausedRemainingMillis"),
                    tag.getString("DisplayText"), tag.contains("DisplayColor") ? tag.getInt("DisplayColor") : 0xFFFFFF,
                    tag.contains("DisplayPosition") ? tag.getString("DisplayPosition") : "BOSSBAR"
            );
            data.countdowns.add(old);
        }
        return data;
    }

    @Override
    public CompoundTag save(CompoundTag tag) {
        ListTag list = new ListTag();
        for (Countdown countdown : countdowns) list.add(countdown.save());
        tag.put("Countdowns", list);
        return tag;
    }

    public List<Countdown> getCountdowns() {
        return List.copyOf(countdowns);
    }

    public Countdown getById(UUID id) {
        for (Countdown countdown : countdowns) if (countdown.id().equals(id)) return countdown;
        return null;
    }

    public Countdown create(String name, long durationMillis, String text, int color, String position) {
        String clean = sanitizeName(name);
        if (clean.isBlank()) clean = nextAutomaticName();
        Countdown countdown = new Countdown(UUID.randomUUID(), clean, false, false,
                0, Math.max(0, durationMillis), text, color, position);
        countdowns.add(countdown);
        setDirty();
        return countdown;
    }

    public boolean update(UUID id, String name, long durationMillis, String text, int color, String position) {
        Countdown c = getById(id);
        if (c == null) return false;
        String clean = sanitizeName(name);
        if (clean.isBlank()) clean = c.name();
        c.name = clean;
        c.pausedRemainingMillis = Math.max(0, durationMillis);
        c.displayText = sanitizeText(text);
        c.displayColor = color & 0xFFFFFF;
        c.displayPosition = validPosition(position) ? position : "BOSSBAR";
        if (c.running) c.endTimeMillis = System.currentTimeMillis() + c.pausedRemainingMillis;
        c.finished = c.pausedRemainingMillis == 0;
        setDirty();
        return true;
    }

    public boolean setRunning(UUID id, boolean running) {
        Countdown c = getById(id);
        if (c == null) return false;
        if (running) c.resume(); else c.pause();
        setDirty();
        return true;
    }

    public boolean remove(UUID id) {
        boolean removed = countdowns.removeIf(c -> c.id().equals(id));
        if (removed) setDirty();
        return removed;
    }

    public boolean updateFinished() {
        boolean changed = false;
        for (Countdown c : countdowns) {
            if (c.running && c.endTimeMillis <= System.currentTimeMillis()) {
                c.running = false;
                c.finished = true;
                c.pausedRemainingMillis = 0;
                changed = true;
            }
        }
        if (changed) setDirty();
        return changed;
    }

    public String nextAutomaticName() {
        int n = 1;
        while (getByName("Contador " + n) != null) n++;
        return "Contador " + n;
    }

    private Countdown getByName(String name) {
        String clean = sanitizeName(name);
        for (Countdown c : countdowns) if (c.name().equals(clean)) return c;
        return null;
    }

    public static String sanitizeName(String name) {
        if (name == null) return "";
        String clean = name.replaceAll("[\\p{Cntrl}§]", "").trim();
        return clean.length() > 32 ? clean.substring(0, 32) : clean;
    }

    private static String sanitizeText(String text) {
        if (text == null) return "";
        return text.length() > 100 ? text.substring(0, 100) : text;
    }

    public static boolean validPosition(String position) {
        return "BOSSBAR".equals(position) || "ACTIONBAR".equals(position)
                || "SCOREBOARD".equals(position) || "TITLE".equals(position);
    }

    public static final class Countdown {
        private final UUID id;
        private String name, displayText, displayPosition;
        private boolean running, finished;
        private long endTimeMillis, pausedRemainingMillis;
        private int displayColor;

        public Countdown(UUID id, String name, boolean running, boolean finished,
                         long endTimeMillis, long pausedRemainingMillis, String text,
                         int color, String position) {
            this.id=id; this.name=sanitizeName(name); this.running=running; this.finished=finished;
            this.endTimeMillis=endTimeMillis; this.pausedRemainingMillis=Math.max(0, pausedRemainingMillis);
            this.displayText=sanitizeText(text); this.displayColor=color & 0xFFFFFF;
            this.displayPosition=validPosition(position) ? position : "BOSSBAR";
        }

        private static Countdown load(CompoundTag t) {
            if (!t.hasUUID("Id")) return null;
            Countdown c = new Countdown(t.getUUID("Id"), t.getString("Name"),
                    t.getBoolean("Running"), t.getBoolean("Finished"),
                    t.getLong("EndTime"), t.getLong("Remaining"),
                    t.getString("Text"), t.getInt("Color"),
                    t.getString("Position"));
            if (c.running && c.endTimeMillis <= System.currentTimeMillis()) {
                c.running=false; c.finished=true; c.pausedRemainingMillis=0;
            }
            return c;
        }

        private CompoundTag save() {
            CompoundTag t=new CompoundTag();
            t.putUUID("Id",id); t.putString("Name",name); t.putBoolean("Running",running);
            t.putBoolean("Finished",finished); t.putLong("EndTime",endTimeMillis);
            t.putLong("Remaining",getRemainingMillis()); t.putString("Text",displayText);
            t.putInt("Color",displayColor); t.putString("Position",displayPosition);
            return t;
        }

        public void pause() {
            if (!running) return;
            pausedRemainingMillis=getRemainingMillis(); running=false;
            if (pausedRemainingMillis<=0) { pausedRemainingMillis=0; finished=true; }
        }
        public void resume() {
            if (finished || pausedRemainingMillis<=0) return;
            endTimeMillis=System.currentTimeMillis()+pausedRemainingMillis; running=true;
        }
        public long getRemainingMillis() {
            if (finished) return 0;
            return running ? Math.max(0,endTimeMillis-System.currentTimeMillis()) : Math.max(0,pausedRemainingMillis);
        }
        public UUID id(){return id;} public String name(){return name;} public boolean running(){return running;}
        public boolean finished(){return finished;} public long endTimeMillis(){return endTimeMillis;}
        public long pausedRemainingMillis(){return pausedRemainingMillis;} public String displayText(){return displayText;}
        public int displayColor(){return displayColor;} public String displayPosition(){return displayPosition;}
    }
}