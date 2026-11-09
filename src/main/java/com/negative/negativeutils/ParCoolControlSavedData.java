package com.negative.negativeutils;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;

/**
 * Estado global del control de ParCool.
 * Se almacena en el Overworld para compartirlo entre dimensiones.
 */
public final class ParCoolControlSavedData extends SavedData {
    private static final String DATA_NAME = "negativeutils_parcool_control";

    private boolean enabled = true;

    public static ParCoolControlSavedData get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(
                ParCoolControlSavedData::load,
                ParCoolControlSavedData::new,
                DATA_NAME
        );
    }

    public static ParCoolControlSavedData load(CompoundTag tag) {
        ParCoolControlSavedData data = new ParCoolControlSavedData();
        data.enabled = !tag.contains("Enabled") || tag.getBoolean("Enabled");
        return data;
    }

    @Override
    public CompoundTag save(CompoundTag tag) {
        tag.putBoolean("Enabled", enabled);
        return tag;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        if (this.enabled == enabled) {
            return;
        }
        this.enabled = enabled;
        setDirty();
    }
}
