package com.negative.negativeutils;

import com.mojang.brigadier.Command;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraftforge.event.server.ServerStartedEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.common.Mod;

import java.lang.reflect.Method;

/**
 * Control opcional de Simple Voice Chat sin referencias directas a su API.
 */
@Mod.EventBusSubscriber(
        modid = NegativeUtilsMod.MOD_ID,
        bus = Mod.EventBusSubscriber.Bus.FORGE
)
public final class VoicechatMuteControl {
    private static final String VOICECHAT_MOD_ID = "voicechat";
    private static final String PLUGIN_CLASS =
            "com.negative.negativeutils.VoicechatPluginIntegration";

    private VoicechatMuteControl() {
    }

    @SubscribeEvent
    public static void onServerStarted(ServerStartedEvent event) {
        boolean muted = VoicechatMuteSavedData.get(event.getServer()).isMuted();
        updatePlugin(event.getServer(), muted);
    }

    public static int setMuted(CommandSourceStack source, boolean muted) {
        MinecraftServer server = source.getServer();
        VoicechatMuteSavedData data = VoicechatMuteSavedData.get(server);
        boolean changed = data.isMuted() != muted;
        data.setMuted(muted);

        if (!ModList.get().isLoaded(VOICECHAT_MOD_ID)) {
            source.sendSuccess(
                    () -> Component.literal(
                            "Simple Voice Chat no está instalado en el servidor. El estado se guardó y se aplicará cuando esté instalado."
                    ),
                    true
            );
            return Command.SINGLE_SUCCESS;
        }

        updatePlugin(server, muted);
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

    private static void updatePlugin(MinecraftServer server, boolean muted) {
        if (!ModList.get().isLoaded(VOICECHAT_MOD_ID)) {
            return;
        }

        try {
            Class<?> plugin = Class.forName(PLUGIN_CLASS, true, VoicechatMuteControl.class.getClassLoader());
            Method setMuted = plugin.getMethod("setGloballyMuted", boolean.class);
            setMuted.invoke(null, muted);
            Method refresh = plugin.getMethod("refreshConnections", MinecraftServer.class);
            refresh.invoke(null, server);
        } catch (ReflectiveOperationException | LinkageError exception) {
            NegativeUtilsMod.LOGGER.error(
                    "No se pudo actualizar el silencio global de Simple Voice Chat.",
                    exception
            );
        }
    }
}
