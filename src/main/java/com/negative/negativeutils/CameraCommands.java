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

@Mod.EventBusSubscriber(modid = NegativeUtilsMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class CameraCommands {
    private CameraCommands() {}

    @SubscribeEvent
    public static void registerCommands(RegisterCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("negativeutils")
                .requires(source -> source.hasPermission(2))
                .then(Commands.literal("camara")
                        .then(Commands.literal("crear")
                                .then(Commands.argument("id", StringArgumentType.word())
                                        .executes(c -> create(c))))
                        .then(Commands.literal("modificar")
                                .then(Commands.argument("id", StringArgumentType.word())
                                        .suggests(CameraCommands::suggestIds)
                                        .executes(c -> edit(c))))
                        .then(Commands.literal("borrar")
                                .then(Commands.argument("id", StringArgumentType.word())
                                        .suggests(CameraCommands::suggestIds)
                                        .executes(c -> delete(c))))
                        .then(Commands.literal("mostrar")
                                .then(Commands.argument("id", StringArgumentType.word())
                                        .suggests(CameraCommands::suggestIds)
                                        .executes(c -> show(c))))
                        .then(Commands.literal("ocultar")
                                .executes(c -> hide(c))));
    }

    private static int create(CommandContext<CommandSourceStack> context)
            throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer player = context.getSource().getPlayerOrException();
        String id = CameraSavedData.sanitizeId(StringArgumentType.getString(context, "id"));
        if (id.isBlank()) {
            context.getSource().sendFailure(Component.literal("El ID no es válido."));
            return 0;
        }
        if (CameraSavedData.get(player.getServer()).getByCommandId(id) != null) {
            context.getSource().sendFailure(Component.literal("Ya existe una cámara con ese ID."));
            return 0;
        }
        CameraNetwork.openCreate(player, id);
        return Command.SINGLE_SUCCESS;
    }

    private static int edit(CommandContext<CommandSourceStack> context)
            throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer player = context.getSource().getPlayerOrException();
        CameraSavedData.Camera camera = CameraSavedData.get(player.getServer())
                .getByCommandId(StringArgumentType.getString(context, "id"));
        if (camera == null) {
            context.getSource().sendFailure(Component.literal("No existe esa cámara."));
            return 0;
        }
        CameraNetwork.openEdit(player, camera);
        return Command.SINGLE_SUCCESS;
    }

    private static int delete(CommandContext<CommandSourceStack> context) {
        CameraSavedData data = CameraSavedData.get(context.getSource().getServer());
        CameraSavedData.Camera camera = data.getByCommandId(StringArgumentType.getString(context, "id"));
        if (camera == null) {
            context.getSource().sendFailure(Component.literal("No existe esa cámara."));
            return 0;
        }
        data.remove(camera.id());
        context.getSource().sendSuccess(() -> Component.literal(
                "Cámara eliminada: " + camera.commandId()), true);
        return Command.SINGLE_SUCCESS;
    }

    private static int show(CommandContext<CommandSourceStack> context)
            throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer player = context.getSource().getPlayerOrException();
        CameraSavedData.Camera camera = CameraSavedData.get(player.getServer()).getByCommandId(
                StringArgumentType.getString(context, "id"));
        if (camera == null) {
            context.getSource().sendFailure(Component.literal("No existe esa cámara."));
            return 0;
        }
        CameraNetwork.show(player, camera);
        return Command.SINGLE_SUCCESS;
    }

    private static int hide(CommandContext<CommandSourceStack> context)
            throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        CameraNetwork.hide(context.getSource().getPlayerOrException());
        return Command.SINGLE_SUCCESS;
    }

    private static CompletableFuture<Suggestions> suggestIds(
            CommandContext<CommandSourceStack> context,
            com.mojang.brigadier.suggestion.SuggestionsBuilder builder) {
        return SharedSuggestionProvider.suggest(
                CameraSavedData.get(context.getSource().getServer()).getCameras().stream()
                        .map(CameraSavedData.Camera::commandId).toList(), builder);
    }
}
