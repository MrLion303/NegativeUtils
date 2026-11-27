package com.negative.negativeutils;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;

/** Guarda qué jugadores tienen bloqueado el cambio normal de perspectiva. */
public final class F5PerspectiveSavedData extends SavedData {
    private static final String DATA_NAME = "negativeutils_f5_perspective";
    private static final String PLAYERS_TAG = "DisabledPlayers";

    private final Set<UUID> disabledPlayers = new HashSet<>();

    public static F5PerspectiveSavedData get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(
                F5PerspectiveSavedData::load,
                F5PerspectiveSavedData::new,
                DATA_NAME
        );
    }

    public static F5PerspectiveSavedData load(CompoundTag tag) {
        F5PerspectiveSavedData data = new F5PerspectiveSavedData();
        ListTag players = tag.getList(PLAYERS_TAG, Tag.TAG_COMPOUND);
        for (int i = 0; i < players.size(); i++) {
            CompoundTag entry = players.getCompound(i);
            if (entry.hasUUID("Player")) {
                data.disabledPlayers.add(entry.getUUID("Player"));
            }
        }
        return data;
    }

    @Override
    public CompoundTag save(CompoundTag tag) {
        ListTag players = new ListTag();
        for (UUID playerId : disabledPlayers) {
            CompoundTag entry = new CompoundTag();
            entry.putUUID("Player", playerId);
            players.add(entry);
        }
        tag.put(PLAYERS_TAG, players);
        return tag;
    }

    public boolean isDisabled(UUID playerId) {
        return disabledPlayers.contains(playerId);
    }

    public void setDisabled(UUID playerId, boolean disabled) {
        boolean changed = disabled
                ? disabledPlayers.add(playerId)
                : disabledPlayers.remove(playerId);
        if (changed) {
            setDirty();
        }
    }
}
