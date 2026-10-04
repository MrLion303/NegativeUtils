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
                                                .then(Commands.argument("color", StringArgumentType.word())
                                                        .suggests((context, builder) ->
                                                                SharedSuggestionProvider.suggest(
                                                                        java.util.List.of(
                                                                                "rojo", "azul", "verde", "amarillo", "naranja",
                                                                                "morado", "violeta", "rosa", "cian", "turquesa",
                                                                                "blanco", "negro", "gris", "marron", "dorado",
                                                                                "lima", "magenta", "coral", "rojo_oscuro",
                                                                                "azul_marino", "red", "blue", "green", "yellow",
                                                                                "orange", "purple", "pink", "cyan", "white",
                                                                                "black", "gray", "brown", "gold", "lime",
                                                                                "magenta", "navy", "teal", "FF8800"), builder))
                                                        .executes(context -> {
                                                            String name = StringArgumentType.getString(context, "nombre");
                                                            String requestedColor = StringArgumentType.getString(context, "color");
                                                            Integer color = parseColor(requestedColor);
                                                            if (color == null) {
                                                                context.getSource().sendFailure(Component.literal(
                                                                        "Color no reconocido. Usa un nombre como rojo, azul, verde o un hexadecimal como FF8800."));
                                                                return 0;
                                                            }
                                                            TrailSavedData data = TrailSavedData.get(context.getSource().getServer());
                                                            TrailSavedData.Trail trail = data.getByName(name);
                                                            if (trail == null) {
                                                                context.getSource().sendFailure(Component.literal(
                                                                        "No existe ese sendero. Usa su nombre con guiones bajos, por ejemplo Sendero_1."));
                                                                return 0;
                                                            }
                                                            data.setSettings(trail.id(), (color >> 16) & 255, (color >> 8) & 255,
                                                                    color & 255, trail.opacity());
                                                            TrailNetwork.syncAll(data.getTrails());
                                                            context.getSource().sendSuccess(() -> Component.literal(
                                                                    "Color del sendero '" + trail.name() + "' actualizado a "
                                                                            + requestedColor + "."), true);
                                                            return Command.SINGLE_SUCCESS;
                                                        }))))
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

    private static Integer parseColor(String input) {
        if (input == null || input.isBlank()) return null;
        String value = input.trim().toLowerCase(java.util.Locale.ROOT)
                .replace("á", "a").replace("é", "e").replace("í", "i")
                .replace("ó", "o").replace("ú", "u").replace("ü", "u");
        if (value.startsWith("#")) value = value.substring(1);
        if (value.matches("[0-9a-f]{6}")) {
            try { return Integer.parseInt(value, 16); }
            catch (NumberFormatException ignored) { return null; }
        }
        return switch (value.replace(' ', '_').replace('-', '_')) {
            case "rojo", "red" -> 0xFF0000;
            case "rojo_oscuro", "dark_red" -> 0xAA0000;
            case "azul", "blue" -> 0x0000FF;
            case "azul_marino", "navy" -> 0x000080;
            case "celeste", "light_blue" -> 0x55AAFF;
            case "verde", "green" -> 0x00AA00;
            case "verde_oscuro", "dark_green" -> 0x006400;
            case "amarillo", "yellow" -> 0xFFFF00;
            case "naranja", "orange" -> 0xFF8800;
            case "morado", "purpura", "purple" -> 0x8000FF;
            case "violeta", "violet" -> 0x8F00FF;
            case "rosa", "pink" -> 0xFF69B4;
            case "cian", "cyan", "aqua" -> 0x00FFFF;
            case "turquesa", "teal" -> 0x008080;
            case "blanco", "white" -> 0xFFFFFF;
            case "negro", "black" -> 0x000000;
            case "gris", "gray", "grey" -> 0x808080;
            case "marron", "brown" -> 0x8B4513;
            case "dorado", "oro", "gold" -> 0xFFD700;
            case "lima", "lime" -> 0x00FF00;
            case "magenta", "fucsia", "fuchsia" -> 0xFF00FF;
            case "coral" -> 0xFF7F50;
            case "salmon" -> 0xFA8072;
            case "oliva", "olive" -> 0x808000;
            case "lavanda", "lavender" -> 0xE6E6FA;
            default -> null;
        };
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
