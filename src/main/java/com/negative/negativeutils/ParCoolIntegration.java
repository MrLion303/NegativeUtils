package com.negative.negativeutils;

import com.mojang.brigadier.Command;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraftforge.event.server.ServerStartedEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.common.Mod;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.lang.reflect.Method;
import java.util.List;

/**
 * Control global de ParCool 1.20.1 mediante su API de Limitations.
 * Se utiliza la limitación global oficial para que también afecte a los jugadores
 * que entren después de ejecutar el comando.
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

    private ParCoolIntegration() {
    }

    @SubscribeEvent
    public static void onServerStarted(ServerStartedEvent event) {
        boolean enabled = ParCoolControlSavedData.get(event.getServer()).isEnabled();
        if (ModList.get().isLoaded(PARCOOL_MOD_ID)) {
            applyGlobalState(event.getServer(), enabled);
        }
    }

    public static int setEnabled(CommandSourceStack source, boolean enabled) {
        ParCoolControlSavedData.get(source.getServer()).setEnabled(enabled);

        if (!ModList.get().isLoaded(PARCOOL_MOD_ID)) {
            source.sendSuccess(
                    () -> Component.literal(
                            "ParCool no está instalado. El estado se guardó y se aplicará cuando esté instalado."
                    ),
                    true
            );
            return Command.SINGLE_SUCCESS;
        }

        if (!applyGlobalState(source.getServer(), enabled)) {
            source.sendFailure(Component.literal(
                    "No se pudo aplicar el control global de ParCool. Revisa el registro del servidor."
            ));
            return 0;
        }

        source.sendSuccess(
                () -> Component.literal(
                        enabled
                                ? "ParCool se ha habilitado globalmente."
                                : "ParCool se ha deshabilitado globalmente: todas sus acciones están bloqueadas."
                ),
                true
        );
        return Command.SINGLE_SUCCESS;
    }

    private static boolean applyGlobalState(MinecraftServer server, boolean enabled) {
        if (!ModList.get().isLoaded(PARCOOL_MOD_ID)) {
            return false;
        }

        try {
            ClassLoader loader = ParCoolIntegration.class.getClassLoader();
            Class<?> limitationType = Class.forName(LIMITATION_CLASS, true, loader);
            Object limitation = limitationType
                    .getMethod("getGlobal", MinecraftServer.class)
                    .invoke(null, server);

            if (enabled) {
                // Quita únicamente la limitación global de NegativeUtils.
                limitationType.getMethod("disable").invoke(limitation);
            } else {
                Class<?> actionListType = Class.forName(ACTION_LIST_CLASS, true, loader);
                List<?> actions = (List<?>) actionListType.getField("ACTIONS").get(null);
                Method permit = limitationType.getMethod("permit", Class.class, boolean.class);

                for (Object action : actions) {
                    permit.invoke(limitation, (Class<?>) action, false);
                }
                limitationType.getMethod("enable").invoke(limitation);
            }

            // ParCool sincroniza la limitación global con todos los jugadores conectados.
            limitationType.getMethod("apply").invoke(limitation);
            LOGGER.info(
                    "Control global de ParCool aplicado: {}",
                    enabled ? "habilitado" : "deshabilitado"
            );
            return true;
        } catch (ReflectiveOperationException | LinkageError | RuntimeException exception) {
            LOGGER.error("No se pudo aplicar el control global de ParCool.", exception);
            return false;
        }
    }
}
