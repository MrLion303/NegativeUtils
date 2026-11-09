package com.negative.negativeutils;

import com.mojang.brigadier.Command;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.common.Mod;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.lang.reflect.Method;
import java.util.List;

/**
 * Integración opcional con ParCool 1.20.1.
 *
 * Toda referencia a clases de ParCool se resuelve por reflexión para que
 * NegativeUtils pueda cargarse y funcionar sin tener ParCool instalado.
 */
@Mod.EventBusSubscriber(
        modid = NegativeUtilsMod.MOD_ID,
        bus = Mod.EventBusSubscriber.Bus.FORGE
)
public final class ParCoolIntegration {
    private static final Logger LOGGER = LogManager.getLogger();
    private static final String PARCOOL_MOD_ID = "parcool";
    private static final String LIMITATION_CLASS =
            "com.alrex.parcool.api.unstable.Limitation";
    private static final String ACTION_LIST_CLASS =
            "com.alrex.parcool.common.action.ActionList";
    private static final String LIMITATION_GROUP = "negativeutils";
    private static final String LIMITATION_NAME = "parkour_control";

    private ParCoolIntegration() {
    }

    @SubscribeEvent
    public static void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }

        ParCoolControlSavedData data = ParCoolControlSavedData.get(player.getServer());
        applyPlayerState(player, data.isEnabled());
    }

    public static int setEnabled(CommandSourceStack source, boolean enabled) {
        ParCoolControlSavedData data = ParCoolControlSavedData.get(source.getServer());
        data.setEnabled(enabled);

        if (!ModList.get().isLoaded(PARCOOL_MOD_ID)) {
            source.sendSuccess(
                    () -> Component.literal(
                            "ParCool no está instalado en el servidor. El estado se guardó y se aplicará cuando esté instalado."
                    ),
                    true
            );
            return Command.SINGLE_SUCCESS;
        }

        int updated = 0;
        int failed = 0;
        for (ServerPlayer player : source.getServer().getPlayerList().getPlayers()) {
            if (applyPlayerState(player, enabled)) {
                updated++;
            } else {
                failed++;
            }
        }

        final int updatedPlayers = updated;
        final int failedPlayers = failed;
        source.sendSuccess(
                () -> Component.literal(
                        (enabled
                                ? "ParCool habilitado globalmente."
                                : "ParCool deshabilitado globalmente.")
                                + " Jugadores actualizados: " + updatedPlayers + "."
                                + (failedPlayers > 0
                                ? " No se pudo actualizar a " + failedPlayers + " jugador(es); revisa el registro."
                                : "")
                ),
                true
        );
        return Command.SINGLE_SUCCESS;
    }

    private static boolean applyPlayerState(ServerPlayer player, boolean enabled) {
        if (!ModList.get().isLoaded(PARCOOL_MOD_ID)) {
            return true;
        }

        try {
            ClassLoader loader = ParCoolIntegration.class.getClassLoader();
            Class<?> limitationType = Class.forName(LIMITATION_CLASS, true, loader);
            Class<?> limitationIdType = Class.forName(LIMITATION_CLASS + "$ID", true, loader);
            Class<?> actionListType = Class.forName(ACTION_LIST_CLASS, true, loader);

            Object limitationId = limitationIdType
                    .getConstructor(String.class, String.class)
                    .newInstance(LIMITATION_GROUP, LIMITATION_NAME);

            Object limitation = limitationType
                    .getMethod("get", ServerPlayer.class, limitationIdType)
                    .invoke(null, player, limitationId);

            if (!enabled) {
                List<?> actions = (List<?>) actionListType.getField("ACTIONS").get(null);
                Method permit = limitationType.getMethod("permit", Class.class, boolean.class);
                for (Object action : actions) {
                    permit.invoke(limitation, (Class<?>) action, false);
                }
                limitationType.getMethod("enable").invoke(limitation);
            } else {
                limitationType.getMethod("disable").invoke(limitation);
            }

            limitationType.getMethod("apply").invoke(limitation);
            limitationType.getMethod("save").invoke(limitation);
            return true;
        } catch (ReflectiveOperationException | LinkageError | RuntimeException exception) {
            LOGGER.error(
                    "No se pudo aplicar el control global de ParCool al jugador {}.",
                    player.getGameProfile().getName(),
                    exception
            );
            return false;
        }
    }
}
