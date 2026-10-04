package com.negative.negativeutils;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;

public class PlayerDiscoveryData extends SavedData {
    private static final String DATA_NAME = "negativeutils_discoveries";

    private final Map<UUID, Set<String>> discoveries = new HashMap<>();

    public static PlayerDiscoveryData get(MinecraftServer server) {
        return server.overworld()
                .getDataStorage()
                .computeIfAbsent(
                        PlayerDiscoveryData::load,
                        PlayerDiscoveryData::new,
                        DATA_NAME
                );
    }

    public static PlayerDiscoveryData load(CompoundTag tag) {
        PlayerDiscoveryData data = new PlayerDiscoveryData();
        CompoundTag players = tag.getCompound("players");

        for (String playerId : players.getAllKeys()) {
            try {
                UUID uuid = UUID.fromString(playerId);
                ListTag list = players.getList(playerId, Tag.TAG_STRING);
                Set<String> unlocked = new HashSet<>();

                for (int i = 0; i < list.size(); i++) {
                    unlocked.add(list.getString(i));
                }

                data.discoveries.put(uuid, unlocked);
            } catch (IllegalArgumentException ignored) {
                // Ignora UUID con formato incorrecto.
            }
        }

        return data;
    }

    @Override
    public CompoundTag save(CompoundTag tag) {
        CompoundTag players = new CompoundTag();

        for (Map.Entry<UUID, Set<String>> playerEntry : discoveries.entrySet()) {
            ListTag list = new ListTag();

            for (String discovery : playerEntry.getValue()) {
                list.add(StringTag.valueOf(discovery));
            }

            players.put(playerEntry.getKey().toString(), list);
        }

        tag.put("players", players);
        return tag;
    }

    public boolean discover(UUID playerId, EncyclopediaData.Entry entry) {
        Set<String> unlocked = discoveries.computeIfAbsent(
                playerId,
                ignored -> new HashSet<>()
        );

        boolean isNew = unlocked.add(key(entry));

        if (isNew) {
            setDirty();
        }

        return isNew;
    }

    public boolean hasDiscovered(
            UUID playerId,
            EncyclopediaData.Entry entry
    ) {
        return discoveries
                .getOrDefault(playerId, Set.of())
                .contains(key(entry));
    }

    public Set<String> getDiscoveries(UUID playerId) {
        return Set.copyOf(
                discoveries.getOrDefault(playerId, Set.of())
        );
    }

    public void forgetEntry(EncyclopediaData.Entry entry) {
        String entryKey = key(entry);
        boolean changed = false;

        for (Set<String> unlocked : discoveries.values()) {
            changed |= unlocked.remove(entryKey);
        }

        if (changed) {
            setDirty();
        }
    }

    public static String key(EncyclopediaData.Entry entry) {
        return entry.category().name() + ":" + entry.targetId();
    }
}