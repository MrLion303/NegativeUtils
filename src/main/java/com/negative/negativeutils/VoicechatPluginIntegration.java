package com.negative.negativeutils;

import de.maxhenkel.voicechat.api.ForgeVoicechatPlugin;
import de.maxhenkel.voicechat.api.VoicechatApi;
import de.maxhenkel.voicechat.api.VoicechatPlugin;
import de.maxhenkel.voicechat.api.events.EventRegistration;
import de.maxhenkel.voicechat.api.events.MicrophonePacketEvent;

@ForgeVoicechatPlugin
public final class VoicechatPluginIntegration implements VoicechatPlugin {
    private static volatile boolean globallyMuted;

    @Override
    public String getPluginId() {
        return "negativeutils_global_mute";
    }

    @Override
    public void initialize(VoicechatApi api) {
        // No se necesita conservar la API: el bloqueo se aplica a cada paquete entrante.
    }

    @Override
    public void registerEvents(EventRegistration registration) {
        registration.registerEvent(
                MicrophonePacketEvent.class,
                event -> {
                    if (globallyMuted) {
                        event.cancel();
                    }
                },
                1000
        );
    }

    public static void setGloballyMuted(boolean muted) {
        globallyMuted = muted;
    }
}
