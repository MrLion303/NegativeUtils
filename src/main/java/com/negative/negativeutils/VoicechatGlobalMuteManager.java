package com.negative.negativeutils;

/**
 * Estado compartido del mute global, siguiendo el modelo de muteall de SVC-Mute.
 * Se mantiene en memoria y se actualiza desde los comandos y los datos del mundo.
 */
public final class VoicechatGlobalMuteManager {
    private static volatile boolean globallyMuted;

    private VoicechatGlobalMuteManager() {
    }

    public static void setGloballyMuted(boolean muted) {
        globallyMuted = muted;
    }

    public static boolean isGloballyMuted() {
        return globallyMuted;
    }
}
