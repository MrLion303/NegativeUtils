package com.negative.negativeutils;

import net.minecraft.nbt.CompoundTag;

public final class GuildClientData {
    private static CompoundTag snapshot = new CompoundTag();

    private GuildClientData() {
    }

    public static void setSnapshot(CompoundTag newSnapshot) {
        snapshot = newSnapshot == null
                ? new CompoundTag()
                : newSnapshot.copy();
    }

    public static CompoundTag getSnapshot() {
        return snapshot.copy();
    }

    public static void clear() {
        snapshot = new CompoundTag();
    }
}