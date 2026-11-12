package com.negative.negativeutils;

import com.mojang.brigadier.Command;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;
import net.minecraftforge.event.server.ServerStartedEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.common.Mod;

/**
 * Puente entre los comandos, el estado persistente del mundo y el listener de SVC.
 * No carga clases de la API de Simple Voice Chat cuando el mod no está instalado.
 */
@Mod.EventBusSubscriber(
        modid = NegativeUtilsMod.MOD_ID,
        bus = Mod.EventBusSubscriber.Bus.FORGE
)
public final class VoicechatMuteControl {
    private static final String VOICECHAT_MOD_ID = "voicechat";

    private VoicechatMuteControl() {
    }

    @SubscribeEvent
    public static void onServerStarted(ServerStartedEvent event) {
        boolean muted = VoicechatMuteSavedData.get(event.getServer()).isMuted();
        VoicechatGlobalMuteManager.setGloballyMuted(muted);
    }

    public static int setMuted(CommandSourceStack source, boolean muted) {
        VoicechatMuteSavedData data = VoicechatMuteSavedData.get(source.getServer());
        boolean changed = data.isMuted() != muted;

        data.setMuted(muted);
        VoicechatGlobalMuteManager.setGloballyMuted(muted);

        if (!ModList.get().isLoaded(VOICECHAT_MOD_ID)) {
            source.sendSuccess(
                    () -> Component.literal(
                            "Simple Voice Chat no está instalado en el servidor. El estado se guardó y se aplicará cuando esté instalado."
                    ),
                    true
            );
            return Command.SINGLE_SUCCESS;
        }

        source.sendSuccess(
                () -> Component.literal(
                        muted
                                ? (changed
                                ? "Silencio global activado: todos los micrófonos están bloqueados."
                                : "El silencio global ya estaba activado.")
                                : (changed
                                ? "Silencio global desactivado: los jugadores pueden volver a hablar."
                                : "El silencio global ya estaba desactivado.")
                ),
                true
        );
        return Command.SINGLE_SUCCESS;
    }
}
