package com.negative.negativeutils;

import de.maxhenkel.voicechat.api.ForgeVoicechatPlugin;
import de.maxhenkel.voicechat.api.VoicechatApi;
import de.maxhenkel.voicechat.api.VoicechatPlugin;
import de.maxhenkel.voicechat.api.events.EventRegistration;
import de.maxhenkel.voicechat.api.events.MicrophonePacketEvent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Bloquea los paquetes de micrófono en el servidor mientras el mute global está activo.
 */
@ForgeVoicechatPlugin
public final class VoicechatPluginIntegration implements VoicechatPlugin {
    private static final Logger LOGGER = LogManager.getLogger();
    private static volatile boolean globallyMuted;

    @Override
    public String getPluginId() {
        return "negativeutils_global_mute";
    }

    @Override
    public void initialize(VoicechatApi api) {
        LOGGER.info("Integración de silencio global de NegativeUtils cargada en Simple Voice Chat.");
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
        LOGGER.info("Evento de bloqueo de micrófono registrado.");
    }

    public static void setGloballyMuted(boolean muted) {
        globallyMuted = muted;
        LOGGER.info(
                "Silencio global de Simple Voice Chat: {}",
                muted ? "activado" : "desactivado"
        );
    }
}
