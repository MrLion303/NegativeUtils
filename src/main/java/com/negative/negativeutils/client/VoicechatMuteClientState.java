package com.negative.negativeutils.client;

/**
 * Estado de mute global recibido desde el servidor.
 */
public final class VoicechatMuteClientState {
    private static volatile boolean globallyMuted;

    private VoicechatMuteClientState() {
    }

    public static void setGloballyMuted(boolean muted) {
        globallyMuted = muted;
    }

    public static boolean isGloballyMuted() {
        return globallyMuted;
    }
}
