package com.negative.negativeutils;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(
        modid = "negativeutils",
        bus = Mod.EventBusSubscriber.Bus.FORGE
)
public final class WaypointCommands {
    private WaypointCommands() {
    }

    @SubscribeEvent
    public static void registerCommands(RegisterCommandsEvent event) {
        event.getDispatcher().register(
                Commands.literal("negativeutils")
                        .requires(source -> source.hasPermission(2))
                        .then(
                                Commands.literal("waypoints")
                                        .then(Commands.literal("mostrar")
                                                .then(Commands.argument(
                                                        "nombre",
                                                        StringArgumentType.string()
                                                ).executes(context ->
                                                        setVisible(context.getSource(), context.getArgument("nombre", String.class), true)
                                                )))
                                        .then(Commands.literal("ocultar")
                                                .then(Commands.argument(
                                                        "nombre",
                                                        StringArgumentType.string()
                                                ).executes(context ->
                                                        setVisible(context.getSource(), context.getArgument("nombre", String.class), false)
                                                )))
                                        .then(Commands.literal("lista")
                                                .executes(context -> list(context.getSource())))
                                        .then(Commands.literal("clear")
                                                .executes(context -> {
                                                    MinecraftServer server =
                                                            context.getSource().getServer();
                                                    WaypointSavedData data =
                                                            WaypointSavedData.get(server);
                                                    int removed = data.removeAll();
                                                    WaypointNetwork.syncAll(data.getWaypoints());
                                                    context.getSource().sendSuccess(
                                                            () -> Component.literal(
                                                                    "Se eliminaron "
                                                                            + removed
                                                                            + " waypoints."
                                                            ),
                                                            true
                                                    );
                                                    return Command.SINGLE_SUCCESS;
                                                }))
                        )
        );
    }

    private static int setVisible(
            CommandSourceStack source,
            String name,
            boolean visible
    ) {
        WaypointSavedData data = WaypointSavedData.get(source.getServer());
        WaypointSavedData.Waypoint waypoint = data.getByName(name);

        if (waypoint == null) {
            source.sendFailure(Component.literal("No existe un waypoint con ese nombre."));
            return 0;
        }

        data.setVisible(waypoint.id(), visible);
        WaypointNetwork.syncAll(data.getWaypoints());

        source.sendSuccess(
                () -> Component.literal(
                        visible
                                ? "Waypoint '" + waypoint.name() + "' activado."
                                : "Waypoint '" + waypoint.name() + "' desactivado."
                ),
                true
        );
        return Command.SINGLE_SUCCESS;
    }

    private static int list(CommandSourceStack source) {
        WaypointSavedData data = WaypointSavedData.get(source.getServer());

        if (data.getWaypoints().isEmpty()) {
            source.sendSuccess(
                    () -> Component.literal("No hay waypoints creados."),
                    false
            );
            return Command.SINGLE_SUCCESS;
        }

        for (WaypointSavedData.Waypoint waypoint : data.getWaypoints()) {
            source.sendSuccess(
                    () -> Component.literal(
                            (waypoint.name().isBlank() ? "(sin nombre)" : waypoint.name())
                                    + " | "
                                    + (waypoint.visible() ? "visible" : "oculto")
                    ),
                    false
            );
        }

        return Command.SINGLE_SUCCESS;
    }
}
