package com.negative.negativeutils;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;

public class FeatureToggleSavedData extends SavedData {
    private static final String DATA_NAME = "negativeutils_feature_toggles";

    private boolean guildsEnabled = true;
    private boolean encyclopediaEnabled = true;

    public FeatureToggleSavedData() {
    }

    public static FeatureToggleSavedData get(MinecraftServer server) {
        return server.overworld()
                .getDataStorage()
                .computeIfAbsent(
                        FeatureToggleSavedData::load,
                        FeatureToggleSavedData::new,
                        DATA_NAME
                );
    }

    public static FeatureToggleSavedData load(CompoundTag tag) {
        FeatureToggleSavedData data = new FeatureToggleSavedData();

        // En mundos antiguos, las funciones empiezan activadas.
        data.guildsEnabled = !tag.contains("GuildsEnabled")
                || tag.getBoolean("GuildsEnabled");
        data.encyclopediaEnabled = !tag.contains("EncyclopediaEnabled")
                || tag.getBoolean("EncyclopediaEnabled");

        return data;
    }

    @Override
    public CompoundTag save(CompoundTag tag) {
        tag.putBoolean("GuildsEnabled", guildsEnabled);
        tag.putBoolean("EncyclopediaEnabled", encyclopediaEnabled);
        return tag;
    }

    public boolean areGuildsEnabled() {
        return guildsEnabled;
    }

    public boolean isEncyclopediaEnabled() {
        return encyclopediaEnabled;
    }

    public void setGuildsEnabled(boolean enabled) {
        guildsEnabled = enabled;
        setDirty();
    }

    public void setEncyclopediaEnabled(boolean enabled) {
        encyclopediaEnabled = enabled;
        setDirty();
    }
}