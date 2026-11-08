package com.negative.negativeutils;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;

/**
 * Configuración global del spawneo natural de rifts.
 * Se guarda en el Overworld para compartirla entre dimensiones.
 */
public final class RiftSpawnSettingsSavedData extends SavedData {
    private static final String DATA_NAME = "negativeutils_rift_spawn_settings";

    private boolean enabled = true;

    public static RiftSpawnSettingsSavedData get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(
                RiftSpawnSettingsSavedData::load,
                RiftSpawnSettingsSavedData::new,
                DATA_NAME
        );
    }

    public static RiftSpawnSettingsSavedData load(CompoundTag tag) {
        RiftSpawnSettingsSavedData data = new RiftSpawnSettingsSavedData();
        // Los mundos existentes mantienen el comportamiento anterior por defecto.
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
