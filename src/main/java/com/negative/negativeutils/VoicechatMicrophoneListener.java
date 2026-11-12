package com.negative.negativeutils;

import de.maxhenkel.voicechat.api.events.MicrophonePacketEvent;

/**
 * Listener del flujo de micrófono de Simple Voice Chat.
 * Cuando muteall está activo, cancela cada paquete de voz que llega al servidor.
 */
public final class VoicechatMicrophoneListener {
    private VoicechatMicrophoneListener() {
    }

    public static void onMicrophonePacket(MicrophonePacketEvent event) {
        if (VoicechatGlobalMuteManager.isGloballyMuted()) {
            event.cancel();
        }
    }
}
