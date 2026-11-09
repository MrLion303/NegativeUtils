package com.negative.negativeutils;

import de.maxhenkel.voicechat.api.ForgeVoicechatPlugin;
import de.maxhenkel.voicechat.api.VoicechatApi;
import de.maxhenkel.voicechat.api.VoicechatPlugin;
import de.maxhenkel.voicechat.api.VoicechatServerApi;
import de.maxhenkel.voicechat.api.VoicechatConnection;
import de.maxhenkel.voicechat.api.events.EventRegistration;
import de.maxhenkel.voicechat.api.events.PlayerForcedStateEvent;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

@ForgeVoicechatPlugin
public final class VoicechatPluginIntegration implements VoicechatPlugin {
    private static volatile boolean globallyMuted;
    private static volatile VoicechatServerApi serverApi;

    @Override
    public String getPluginId() {
        return "negativeutils_global_mute";
    }

    @Override
    public void initialize(VoicechatApi api) {
        if (api instanceof VoicechatServerApi voicechatServerApi) {
            serverApi = voicechatServerApi;
        }
    }

    @Override
    public void registerEvents(EventRegistration registration) {
        registration.registerEvent(
                PlayerForcedStateEvent.class,
                event -> {
                    if (globallyMuted) {
                        event.forceMute();
                    }
                },
                1000
        );
    }

    public static void setGloballyMuted(boolean muted) {
        globallyMuted = muted;
    }

    /**
     * Reevalúa el estado forzado de los jugadores que ya tienen conexión de voz.
     * Los demás quedarán silenciados cuando Simple Voice Chat establezca su conexión.
     */
    public static void refreshConnections(MinecraftServer server) {
        VoicechatServerApi api = serverApi;
        if (api == null) {
            return;
        }

        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            VoicechatConnection connection = api.getConnectionOf(player.getUUID());
            if (connection != null) {
                connection.evaluateForcedState();
            }
        }
    }
}
