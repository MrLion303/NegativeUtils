package com.negative.negativeutils;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;

/**
 * Estado persistente del silencio global de Simple Voice Chat.
 */
public final class VoicechatMuteSavedData extends SavedData {
    private static final String DATA_NAME = "negativeutils_voicechat_mute";

    private boolean muted;

    public static VoicechatMuteSavedData get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(
                VoicechatMuteSavedData::load,
                VoicechatMuteSavedData::new,
                DATA_NAME
        );
    }

    public static VoicechatMuteSavedData load(CompoundTag tag) {
        VoicechatMuteSavedData data = new VoicechatMuteSavedData();
        data.muted = tag.getBoolean("Muted");
        return data;
    }

    @Override
    public CompoundTag save(CompoundTag tag) {
        tag.putBoolean("Muted", muted);
        return tag;
    }

    public boolean isMuted() {
        return muted;
    }

    public void setMuted(boolean muted) {
        if (this.muted == muted) {
            return;
        }
        this.muted = muted;
        setDirty();
    }
}
