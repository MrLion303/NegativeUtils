package com.negative.negativeutils;

import de.maxhenkel.voicechat.api.ForgeVoicechatPlugin;
import de.maxhenkel.voicechat.api.VoicechatApi;
import de.maxhenkel.voicechat.api.VoicechatPlugin;
import de.maxhenkel.voicechat.api.events.EventRegistration;
import de.maxhenkel.voicechat.api.events.MicrophonePacketEvent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Registra NegativeUtils como plugin de Forge para Simple Voice Chat.
 */
@ForgeVoicechatPlugin
public final class VoicechatPluginIntegration implements VoicechatPlugin {
    private static final Logger LOGGER = LogManager.getLogger();

    @Override
    public String getPluginId() {
        return "negativeutils_global_mute";
    }

    @Override
    public void initialize(VoicechatApi api) {
        LOGGER.info("Plugin de mute global de NegativeUtils conectado a Simple Voice Chat.");
    }

    @Override
    public void registerEvents(EventRegistration registration) {
        // SVC-Mute usa el mismo evento de la API: MicrophonePacketEvent#cancel().
        registration.registerEvent(
                MicrophonePacketEvent.class,
                VoicechatMicrophoneListener::onMicrophonePacket,
                100
        );
        LOGGER.info("Listener de muteall registrado en Simple Voice Chat.");
    }
}
