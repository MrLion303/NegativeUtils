package com.negative.negativeutils;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.network.chat.Component;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(
        modid = "negativeutils",
        bus = Mod.EventBusSubscriber.Bus.FORGE
)
public final class TrailCommands {
    private TrailCommands() {
    }

    @SubscribeEvent
    public static void registerCommands(RegisterCommandsEvent event) {
        event.getDispatcher().register(
                Commands.literal("negativeutils")
                        .requires(source -> source.hasPermission(2))
                        .then(Commands.literal("senderos")
                                .then(Commands.literal("crear")
                                        .then(Commands.argument(
                                                "nombre",
                                                StringArgumentType.string()
                                        ).suggests((context, builder) ->
                                                SharedSuggestionProvider.suggest(
                                                        TrailSavedData.get(context.getSource().getServer())
                                                                .getTrails()
                                                                .stream()
                                                                .map(TrailSavedData.Trail::name)
                                                                .toList(),
                                                        builder
                                                )
                                        ).executes(context -> {
                                            String name = StringArgumentType.getString(
                                                    context, "nombre"
                                            );
                                            TrailSavedData data = TrailSavedData.get(
                                                    context.getSource().getServer()
                                            );
                                            if (data.nameExists(name)) {
                                                context.getSource().sendFailure(
                                                        Component.literal(
                                                                "Ya existe un sendero con ese nombre."
                                                        )
                                                );
                                                return 0;
                                            }

                                            TrailSavedData.Trail trail = data.create(name);
                                            if (trail == null) {
                                                return 0;
                                            }

                                            ServerPlayer player =
                                                    context.getSource().getPlayerOrException();
                                            TrailSelectionState.select(
                                                    player.getUUID(),
                                                    trail.id()
                                            );
                                            TrailNetwork.syncAll(data.getTrails());
                                            context.getSource().sendSuccess(
                                                    () -> Component.literal(
                                                            "Sendero '" + trail.name()
                                                                    + "' creado y seleccionado."
                                                    ),
                                                    true
                                            );
                                            return Command.SINGLE_SUCCESS;
                                        })
                                ))
                                .then(Commands.literal("select")
                                        .then(Commands.argument(
                                                "nombre",
                                                StringArgumentType.string()
                                        ).suggests((context, builder) ->
                                                SharedSuggestionProvider.suggest(
                                                        TrailSavedData.get(context.getSource().getServer())
                                                                .getTrails()
                                                                .stream()
                                                                .map(TrailSavedData.Trail::name)
                                                                .toList(),
                                                        builder
                                                )
                                        ).executes(context -> {
                                            String name = StringArgumentType.getString(
                                                    context, "nombre"
                                            );
                                            TrailSavedData data = TrailSavedData.get(
                                                    context.getSource().getServer()
                                            );
                                            TrailSavedData.Trail trail = data.getByName(name);
                                            if (trail == null) {
                                                context.getSource().sendFailure(
                                                        Component.literal(
                                                                "No existe ese sendero."
                                                        )
                                                );
                                                return 0;
                                            }

                                            ServerPlayer player =
                                                    context.getSource().getPlayerOrException();
                                            TrailSelectionState.select(
                                                    player.getUUID(),
                                                    trail.id()
                                            );
                                            TrailNetwork.openSettings(player, trail);
                                            return Command.SINGLE_SUCCESS;
                                        })
                                ))
                                .then(Commands.literal("mostrar")
                                        .then(Commands.argument(
                                                "nombre",
                                                StringArgumentType.string()
                                        ).suggests((context, builder) ->
                                                SharedSuggestionProvider.suggest(
                                                        TrailSavedData.get(context.getSource().getServer())
                                                                .getTrails()
                                                                .stream()
                                                                .map(TrailSavedData.Trail::name)
                                                                .toList(),
                                                        builder
                                                )
                                        ).executes(context ->
                                                setVisible(context, true)
                                        ))
                                )
                                .then(Commands.literal("ocultar")
                                        .then(Commands.argument(
                                                "nombre",
                                                StringArgumentType.string()
                                        ).suggests((context, builder) ->
                                                SharedSuggestionProvider.suggest(
                                                        TrailSavedData.get(context.getSource().getServer())
                                                                .getTrails()
                                                                .stream()
                                                                .map(TrailSavedData.Trail::name)
                                                                .toList(),
                                                        builder
                                                )
                                        ).executes(context ->
                                                setVisible(context, false)
                                        ))
                                )
                                .then(Commands.literal("eliminar")
                                        .then(Commands.argument(
                                                "nombre",
                                                StringArgumentType.string()
                                        ).suggests((context, builder) ->
                                                SharedSuggestionProvider.suggest(
                                                        TrailSavedData.get(context.getSource().getServer())
                                                                .getTrails()
                                                                .stream()
                                                                .map(TrailSavedData.Trail::name)
                                                                .toList(),
                                                        builder
                                                )
                                        ).executes(context ->
                                                remove(context)
                                        ))
                                )
                                .then(Commands.literal("color")
                                        .then(Commands.argument("nombre", StringArgumentType.string())
                                                .suggests((context, builder) ->
                                                        SharedSuggestionProvider.suggest(
                                                                TrailSavedData.get(context.getSource().getServer())
                                                                        .getTrails().stream()
                                                                        .map(TrailSavedData.Trail::name).toList(), builder))
                                                .then(Commands.argument("hex", StringArgumentType.word())
                                                        .executes(context -> {
                                                            String name = StringArgumentType.getString(context, "nombre");
                                                            String hex = StringArgumentType.getString(context, "hex").replace("#", "");
                                                            if (hex.length() != 6 || !hex.chars().allMatch(ch -> Character.digit(ch, 16) >= 0)) {
                                                                context.getSource().sendFailure(Component.literal("Usa un color hexadecimal de 6 dígitos, por ejemplo FF8800."));
                                                                return 0;
                                                            }
                                                            TrailSavedData data = TrailSavedData.get(context.getSource().getServer());
                                                            TrailSavedData.Trail trail = data.getByName(name);
                                                            if (trail == null) {
                                                                context.getSource().sendFailure(Component.literal("No existe ese sendero."));
                                                                return 0;
                                                            }
                                                            int color = Integer.parseInt(hex, 16);
                                                            data.setSettings(trail.id(), (color >> 16) & 255, (color >> 8) & 255,
                                                                    color & 255, trail.opacity());
                                                            TrailNetwork.syncAll(data.getTrails());
                                                            context.getSource().sendSuccess(() -> Component.literal(
                                                                    "Color del sendero '" + trail.name() + "' actualizado a #" + hex.toUpperCase(java.util.Locale.ROOT) + "."), true);
                                                            return Command.SINGLE_SUCCESS;
                                                        })))
                                .then(Commands.literal("lista")
                                        .executes(context -> list(context)))
                                .then(Commands.literal("deseleccionar")
                                        .executes(context -> {
                                            ServerPlayer player =
                                                    context.getSource().getPlayerOrException();
                                            TrailSelectionState.clear(player.getUUID());
                                            context.getSource().sendSuccess(
                                                    () -> Component.literal(
                                                            "Sendero deseleccionado."
                                                    ),
                                                    false
                                            );
                                            return Command.SINGLE_SUCCESS;
                                        })
                                )
                        )
        );
    }

    private static int setVisible(
            com.mojang.brigadier.context.CommandContext<net.minecraft.commands.CommandSourceStack> context,
            boolean visible
    ) {
        String name = StringArgumentType.getString(context, "nombre");
        TrailSavedData data = TrailSavedData.get(context.getSource().getServer());
        TrailSavedData.Trail trail = data.getByName(name);
        if (trail == null) {
            context.getSource().sendFailure(Component.literal("No existe ese sendero."));
            return 0;
        }

        data.setVisible(trail.id(), visible);
        TrailNetwork.syncAll(data.getTrails());
        context.getSource().sendSuccess(
                () -> Component.literal(
                        visible
                                ? "Sendero '" + trail.name() + "' visible."
                                : "Sendero '" + trail.name() + "' oculto."
                ),
                true
        );
        return Command.SINGLE_SUCCESS;
    }

    private static int remove(
            com.mojang.brigadier.context.CommandContext<net.minecraft.commands.CommandSourceStack> context
    ) {
        String name = StringArgumentType.getString(context, "nombre");
        TrailSavedData data = TrailSavedData.get(context.getSource().getServer());
        TrailSavedData.Trail trail = data.getByName(name);
        if (trail == null) {
            context.getSource().sendFailure(Component.literal("No existe ese sendero."));
            return 0;
        }

        data.remove(trail.id());
        TrailNetwork.syncAll(data.getTrails());
        context.getSource().sendSuccess(
                () -> Component.literal(
                        "Sendero '" + trail.name() + "' eliminado."
                ),
                true
        );
        return Command.SINGLE_SUCCESS;
    }

    private static int list(
            com.mojang.brigadier.context.CommandContext<net.minecraft.commands.CommandSourceStack> context
    ) {
        TrailSavedData data = TrailSavedData.get(context.getSource().getServer());
        if (data.getTrails().isEmpty()) {
            context.getSource().sendSuccess(
                    () -> Component.literal("No hay senderos creados."),
                    false
            );
            return Command.SINGLE_SUCCESS;
        }

        for (TrailSavedData.Trail trail : data.getTrails()) {
            context.getSource().sendSuccess(
                    () -> Component.literal(
                            trail.name() + " | "
                                    + (trail.visible() ? "visible" : "oculto")
                                    + " | puntos: " + trail.points().size()
                    ),
                    false
            );
        }
        return Command.SINGLE_SUCCESS;
    }
}
