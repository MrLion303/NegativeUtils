package com.negative.negativeutils;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.Suggestions;
import java.util.concurrent.CompletableFuture;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = "negativeutils", bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class WaypointCommands {
    private WaypointCommands() {}

    @SubscribeEvent
    public static void registerCommands(RegisterCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("negativeutils")
                .requires(source -> source.hasPermission(2))
                .then(Commands.literal("waypoints")
                        .then(Commands.literal("crear")
                                .then(Commands.argument("id", StringArgumentType.word())
                                        .executes(context -> {
                                            String id = WaypointSavedData.sanitizeCommandId(
                                                    StringArgumentType.getString(context, "id"));
                                            if (id.isBlank()) {
                                                context.getSource().sendFailure(Component.literal("El ID no es válido."));
                                                return 0;
                                            }
                                            if (WaypointSavedData.get(context.getSource().getServer()).getByCommandId(id) != null) {
                                                context.getSource().sendFailure(Component.literal("Ya existe un waypoint con ese ID."));
                                                return 0;
                                            }
                                            ServerPlayer player = context.getSource().getPlayerOrException();
                                            WaypointNetwork.openCreate(player, id);
                                            return Command.SINGLE_SUCCESS;
                                        })))
                        .then(Commands.literal("editar").then(Commands.argument("id", StringArgumentType.word())
                                .suggests((c, b) -> suggestIds(c, b))
                                .executes(context -> openEdit(context))))
                        .then(Commands.literal("mostrar").then(Commands.argument("id", StringArgumentType.word())
                                .suggests((c, b) -> suggestIds(c, b))
                                .executes(context -> setVisible(context, true))))
                        .then(Commands.literal("ocultar").then(Commands.argument("id", StringArgumentType.word())
                                .suggests((c, b) -> suggestIds(c, b))
                                .executes(context -> setVisible(context, false))))
                        .then(Commands.literal("eliminar").then(Commands.argument("id", StringArgumentType.word())
                                .suggests((c, b) -> suggestIds(c, b))
                                .executes(context -> {
                                    WaypointSavedData data = WaypointSavedData.get(context.getSource().getServer());
                                    WaypointSavedData.Waypoint waypoint = find(context);
                                    if (waypoint == null) return missing(context);
                                    data.removeById(waypoint.id());
                                    WaypointNetwork.syncAll(data.getWaypoints());
                                    context.getSource().sendSuccess(() -> Component.literal(
                                            "Waypoint '" + waypoint.name() + "' (" + waypoint.commandId() + ") eliminado."), true);
                                    return Command.SINGLE_SUCCESS;
                                })))
                        .then(Commands.literal("lista").executes(context -> {
                            var waypoints = WaypointSavedData.get(context.getSource().getServer()).getWaypoints();
                            if (waypoints.isEmpty()) {
                                context.getSource().sendSuccess(() -> Component.literal("No hay waypoints."), false);
                            }
                            for (var waypoint : waypoints) {
                                context.getSource().sendSuccess(() -> Component.literal(
                                        waypoint.commandId() + " | " + waypoint.name() + " | "
                                                + (waypoint.visible() ? "visible" : "oculto") + " | "
                                                + waypoint.dimension() + " | "
                                                + Math.round(waypoint.x()) + ", "
                                                + Math.round(waypoint.y()) + ", "
                                                + Math.round(waypoint.z())), false);
                            }
                            return Command.SINGLE_SUCCESS;
                        }))
                        .then(Commands.literal("clear").executes(context -> {
                            WaypointSavedData data = WaypointSavedData.get(context.getSource().getServer());
                            int count = data.removeAll();
                            WaypointNetwork.syncAll(data.getWaypoints());
                            context.getSource().sendSuccess(() -> Component.literal(
                                    "Se eliminaron " + count + " waypoints."), true);
                            return Command.SINGLE_SUCCESS;
                        }))));
    }

    private static CompletableFuture<Suggestions> suggestIds(
            CommandContext<CommandSourceStack> context,
            com.mojang.brigadier.suggestion.SuggestionsBuilder builder) {
        return SharedSuggestionProvider.suggest(
                WaypointSavedData.get(context.getSource().getServer()).getWaypoints().stream()
                        .map(WaypointSavedData.Waypoint::commandId).toList(), builder);
    }

    private static WaypointSavedData.Waypoint find(CommandContext<CommandSourceStack> context) {
        return WaypointSavedData.get(context.getSource().getServer()).getByCommandId(
                StringArgumentType.getString(context, "id"));
    }

    private static int missing(CommandContext<CommandSourceStack> context) {
        context.getSource().sendFailure(Component.literal("No existe un waypoint con ese ID."));
        return 0;
    }

    private static int openEdit(CommandContext<CommandSourceStack> context) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        WaypointSavedData.Waypoint waypoint = find(context);
        if (waypoint == null) return missing(context);
        WaypointNetwork.openEdit(context.getSource().getPlayerOrException(), waypoint);
        return Command.SINGLE_SUCCESS;
    }

    private static int setVisible(CommandContext<CommandSourceStack> context, boolean visible) {
        WaypointSavedData data = WaypointSavedData.get(context.getSource().getServer());
        WaypointSavedData.Waypoint waypoint = find(context);
        if (waypoint == null) return missing(context);
        data.setVisible(waypoint.id(), visible);
        WaypointNetwork.syncAll(data.getWaypoints());
        context.getSource().sendSuccess(() -> Component.literal(
                visible ? "Waypoint activado: " + waypoint.commandId() : "Waypoint desactivado: " + waypoint.commandId()), true);
        return Command.SINGLE_SUCCESS;
    }
}