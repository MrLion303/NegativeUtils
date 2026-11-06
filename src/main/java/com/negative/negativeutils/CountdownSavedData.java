package com.negative.negativeutils;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;
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
        long duration = Math.max(0, durationMillis);
        Countdown countdown = new Countdown(UUID.randomUUID(), clean, duration > 0, duration == 0,
                System.currentTimeMillis() + duration, duration, text, color, position);
        countdowns.add(countdown);
        setDirty();
        return countdown;
    }

    public Countdown createMinuteCountdown(String id, int minutes, String completionCommand) {
        String clean = sanitizeName(id);
        String cleanCommand = sanitizeMinuteFinishCommand(completionCommand);
        if (clean.isBlank() || getByName(clean) != null || minutes < 1
                || cleanCommand == null) return null;
        long duration = minutes * 60_000L;
        Countdown countdown = new Countdown(UUID.randomUUID(), clean, false, false,
                0L, duration, "", 0xFFFFFF, "BOSSBAR");
        countdown.minuteMode = true;
        countdown.durationMillis = duration;
        countdown.completionCommand = cleanCommand;
        countdown.displayed = false;
        countdowns.add(countdown);
        setDirty();
        return countdown;
    }

    public static String sanitizeMinuteFinishCommand(String command) {
        if (command == null) return "";
        String clean = command.trim();
        // greedyString conserva todo el texto, incluidas comillas internas.
        // Si se usó el formato anterior con comillas exteriores, retirarlas
        // sin alterar las comillas que pertenecen al comando.
        if (clean.length() >= 2 && clean.charAt(0) == '"' && clean.charAt(clean.length() - 1) == '"') {
            clean = clean.substring(1, clean.length() - 1).trim();
        }
        if (clean.startsWith("/")) clean = clean.substring(1).trim();
        if (clean.length() > 1024
                || clean.indexOf('\n') >= 0
                || clean.indexOf('\r') >= 0
                || clean.indexOf('\0') >= 0) {
            return null;
        }
        return clean;
    }

    public boolean setMinuteCountdownDisplayed(UUID id, boolean displayed) {
        Countdown countdown = getById(id);
        if (countdown == null || !countdown.minuteMode) return false;
        if (displayed) {
            countdown.finished = false;
            countdown.completionCommandExecuted = false;
            countdown.pausedRemainingMillis = countdown.durationMillis;
            countdown.endTimeMillis = System.currentTimeMillis() + countdown.durationMillis;
            countdown.running = true;
            countdown.displayed = true;
        } else {
            countdown.running = false;
            countdown.finished = false;
            countdown.completionCommandExecuted = false;
            countdown.pausedRemainingMillis = countdown.durationMillis;
            countdown.endTimeMillis = 0L;
            countdown.displayed = false;
        }
        setDirty();
        return true;
    }

    public boolean setHologramPosition(UUID id, String dimension, Vec3 position, String imageName) {
        Countdown countdown = getById(id);
        ResourceLocation parsedDimension = ResourceLocation.tryParse(dimension);
        if (countdown == null || parsedDimension == null || position == null) return false;

        String cleanImage = imageName == null ? null : sanitizeHologramImage(imageName);
        if (imageName != null && cleanImage == null) return false;

        countdown.hologramDimension = parsedDimension.toString();
        countdown.hologramX = position.x;
        countdown.hologramY = position.y;
        countdown.hologramZ = position.z;
        if (cleanImage != null) countdown.hologramImage = cleanImage;
        setDirty();
        return true;
    }

    public static String sanitizeHologramImage(String imageName) {
        if (imageName == null) return "";
        String clean = imageName.trim();
        if (clean.isEmpty()) return "";
        if (clean.length() > 64 || clean.contains("..")
                || !clean.matches("[a-z0-9_-][a-z0-9_.-]*\\.png")) {
            return null;
        }
        return clean;
    }

    public boolean clearHologramPosition(UUID id) {
        Countdown countdown = getById(id);
        if (countdown == null) return false;
        countdown.hologramDimension = "";
        countdown.hologramX = 0.0;
        countdown.hologramY = 0.0;
        countdown.hologramZ = 0.0;
        countdown.hologramImage = "";
        setDirty();
        return true;
    }

    public boolean removeByName(String name) {
        Countdown countdown = getByName(name);
        return countdown != null && remove(countdown.id());
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

    public boolean updateFinished(MinecraftServer server) {
        boolean changed = false;
        List<String> commandsToRun = new ArrayList<>();
        long now = System.currentTimeMillis();

        for (Countdown c : countdowns) {
            if (c.running && c.endTimeMillis <= now) {
                c.running = false;
                if (c.minuteMode) {
                    c.finished = false;
                    c.pausedRemainingMillis = c.durationMillis;
                    c.endTimeMillis = 0L;
                    c.displayed = false;

                    if (!c.completionCommandExecuted) {
                        c.completionCommandExecuted = true;
                        if (!c.completionCommand.isBlank()) {
                            commandsToRun.add(c.completionCommand);
                        }
                    }
                } else {
                    c.finished = true;
                    c.pausedRemainingMillis = 0;
                }
                changed = true;
            }
        }

        // Guardar el estado de finalización antes de ejecutar los comandos para evitar
        // repetirlos en los siguientes ticks o modificar la lista durante su iteración.
        if (changed) setDirty();

        for (String command : commandsToRun) {
            try {
                server.getCommands().performPrefixedCommand(
                        server.createCommandSourceStack(),
                        command
                );
            } catch (RuntimeException exception) {
                com.mojang.logging.LogUtils.getLogger().error(
                        "No se pudo ejecutar el comando de finalización de contadorminus: {}",
                        command,
                        exception
                );
            }
        }
        return changed;
    }

    public String nextAutomaticName() {
        int n = 1;
        while (getByName("Contador " + n) != null) n++;
        return "Contador " + n;
    }

    public Countdown getByName(String name) {
        String clean = sanitizeName(name);
        for (Countdown c : countdowns) {
            if (c.name().equalsIgnoreCase(clean)) return c;
        }
        return null;
    }

    public boolean setDisplayed(UUID id, boolean displayed) {
        Countdown countdown = getById(id);
        if (countdown == null) return false;
        if (countdown.displayed != displayed) {
            countdown.displayed = displayed;
            setDirty();
        }
        return true;
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
        private boolean displayed = true;
        private String hologramDimension = "";
        private String hologramImage = "";
        private double hologramX, hologramY, hologramZ;
        private boolean minuteMode;
        private String completionCommand = "";
        private boolean completionCommandExecuted;
        private long durationMillis;
        private long endTimeMillis, pausedRemainingMillis;
        private int displayColor;

        public Countdown(UUID id, String name, boolean running, boolean finished,
                         long endTimeMillis, long pausedRemainingMillis, String text,
                         int color, String position) {
            this.id=id; this.name=sanitizeName(name); this.running=running; this.finished=finished;
            this.endTimeMillis=endTimeMillis; this.pausedRemainingMillis=Math.max(0, pausedRemainingMillis);
            this.durationMillis=Math.max(0, pausedRemainingMillis);
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
            c.displayed = !t.contains("Displayed") || t.getBoolean("Displayed");
            ResourceLocation hologramDimension = ResourceLocation.tryParse(t.getString("HologramDimension"));
            if (hologramDimension != null) {
                c.hologramDimension = hologramDimension.toString();
                c.hologramX = t.getDouble("HologramX");
                c.hologramY = t.getDouble("HologramY");
                c.hologramZ = t.getDouble("HologramZ");
                String savedImage = sanitizeHologramImage(t.getString("HologramImage"));
                c.hologramImage = savedImage == null ? "" : savedImage;
            }
            c.minuteMode = t.getBoolean("MinuteMode");
            c.completionCommand = sanitizeMinuteFinishCommand(t.getString("CompletionCommand"));
            if (c.completionCommand == null) c.completionCommand = "";
            c.completionCommandExecuted = t.getBoolean("CompletionCommandExecuted");
            c.durationMillis = t.contains("Duration") ? Math.max(0, t.getLong("Duration")) : c.pausedRemainingMillis;
            if (c.running && c.endTimeMillis <= System.currentTimeMillis()
                    && !c.minuteMode) {
                c.running = false;
                c.finished = true;
                c.pausedRemainingMillis = 0;
            }
            return c;
        }

        private CompoundTag save() {
            CompoundTag t=new CompoundTag();
            t.putUUID("Id",id); t.putString("Name",name); t.putBoolean("Running",running);
            t.putBoolean("Finished",finished); t.putBoolean("Displayed",displayed);
            t.putBoolean("MinuteMode", minuteMode);
            t.putString("CompletionCommand", completionCommand);
            t.putBoolean("CompletionCommandExecuted", completionCommandExecuted);
            t.putLong("Duration", durationMillis);
            t.putLong("EndTime",endTimeMillis); t.putLong("Remaining",getRemainingMillis()); t.putString("Text",displayText);
            t.putString("HologramDimension", hologramDimension);
            t.putString("HologramImage", hologramImage);
            t.putDouble("HologramX", hologramX); t.putDouble("HologramY", hologramY); t.putDouble("HologramZ", hologramZ);
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
        public boolean displayed(){return displayed;} public boolean minuteMode(){return minuteMode;}
        public String completionCommand(){return completionCommand;}
        public boolean completionCommandExecuted(){return completionCommandExecuted;}
        public long durationMillis(){return durationMillis;}
        public String hologramDimension(){return hologramDimension;}
        public String hologramImage(){return hologramImage;}
        public double hologramX(){return hologramX;}
        public double hologramY(){return hologramY;}
        public double hologramZ(){return hologramZ;}
        public boolean hasHologram(){return !hologramDimension.isBlank();}
    }
}