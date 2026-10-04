package com.negative.negativeutils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;

public class EncyclopediaData extends SavedData {
    private static final String DATA_NAME = "negativeutils_entries";

    private final Map<String, Entry> entries = new LinkedHashMap<>();

    public static EncyclopediaData get(MinecraftServer server) {
        return server.overworld()
                .getDataStorage()
                .computeIfAbsent(
                        EncyclopediaData::load,
                        EncyclopediaData::new,
                        DATA_NAME
                );
    }

    public static EncyclopediaData load(CompoundTag tag) {
        EncyclopediaData data = new EncyclopediaData();
        ListTag list = tag.getList("entries", Tag.TAG_COMPOUND);

        for (int i = 0; i < list.size(); i++) {
            CompoundTag savedEntry = list.getCompound(i);

            try {
                Entry entry = new Entry(
                        savedEntry.getString("targetId"),
                        Category.valueOf(savedEntry.getString("category")),
                        savedEntry.getString("description"),
                        savedEntry.getString("notificationText")
                );

                data.entries.put(data.key(entry), entry);
            } catch (IllegalArgumentException ignored) {
                // Ignora entradas con categoría inválida.
            }
        }

        return data;
    }

    @Override
    public CompoundTag save(CompoundTag tag) {
        ListTag list = new ListTag();

        for (Entry entry : entries.values()) {
            CompoundTag savedEntry = new CompoundTag();
            savedEntry.putString("targetId", entry.targetId());
            savedEntry.putString("category", entry.category().name());
            savedEntry.putString("description", entry.description());
            savedEntry.putString("notificationText", entry.notificationText());
            list.add(savedEntry);
        }

        tag.put("entries", list);
        return tag;
    }

    public Collection<Entry> getEntries() {
        return new ArrayList<>(entries.values());
    }

    public void addOrUpdate(Entry entry) {
        entries.put(key(entry), entry);
        setDirty();
    }

    public boolean removeEntry(Entry entry) {
        boolean removed = entries.remove(key(entry)) != null;

        if (removed) {
            setDirty();
        }

        return removed;
    }

    private String key(Entry entry) {
        return entry.category().name() + ":" + entry.targetId();
    }

    public enum Category {
        ITEM,
        BLOCK,
        MOB
    }

    public record Entry(
            String targetId,
            Category category,
            String description,
            String notificationText
    ) {
    }
}