package com.negative.negativeutils;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.context.CommandContext;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(
        modid = EnciclopediaMod.MOD_ID,
        bus = Mod.EventBusSubscriber.Bus.FORGE
)
public final class AdminPanelCommands {
    private AdminPanelCommands() {
    }

    @SubscribeEvent
    public static void registerCommands(RegisterCommandsEvent event) {
        event.getDispatcher().register(
                Commands.literal("adminpanel")
                        .requires(source ->
                                source.hasPermission(2)
                                        && source.getEntity()
                                        instanceof ServerPlayer
                        )
                        .executes(context -> {
                            ServerPlayer player = context.getSource()
                                    .getPlayerOrException();

                            CountdownNetwork.openAdminPanel(player);
                            return Command.SINGLE_SUCCESS;
                        })
        );

        event.getDispatcher().register(
                Commands.literal("negativeutils")
                        .requires(source -> source.hasPermission(2))
                        .then(
                                Commands.literal("desactivar")
                                        .then(
                                                Commands.literal("hermandades")
                                                        .executes(context ->
                                                                setGuilds(
                                                                        context,
                                                                        false
                                                                )
                                                        )
                                        )
                                        .then(
                                                Commands.literal("enciclopedia")
                                                        .executes(context ->
                                                                setEncyclopedia(
                                                                        context,
                                                                        false
                                                                )
                                                        )
                                        )
                        )
                        .then(
                                Commands.literal("activar")
                                        .then(
                                                Commands.literal("hermandades")
                                                        .executes(context ->
                                                                setGuilds(
                                                                        context,
                                                                        true
                                                                )
                                                        )
                                        )
                                        .then(
                                                Commands.literal("enciclopedia")
                                                        .executes(context ->
                                                                setEncyclopedia(
                                                                        context,
                                                                        true
                                                                )
                                                        )
                                        )
                        )
        );
    }

    private static int setGuilds(
            CommandContext<CommandSourceStack> context,
            boolean enabled
    ) {
        MinecraftServer server = context.getSource().getServer();
        FeatureToggleSavedData data = FeatureToggleSavedData.get(server);
        data.setGuildsEnabled(enabled);
        FeatureToggleNetwork.broadcast(server);

        String message = enabled
                ? "Hermandades activadas para todos."
                : "Hermandades desactivadas para todos.";

        context.getSource().sendSuccess(
                () -> Component.literal(message),
                true
        );

        return Command.SINGLE_SUCCESS;
    }

    private static int setEncyclopedia(
            CommandContext<CommandSourceStack> context,
            boolean enabled
    ) {
        MinecraftServer server = context.getSource().getServer();
        FeatureToggleSavedData data = FeatureToggleSavedData.get(server);
        data.setEncyclopediaEnabled(enabled);
        FeatureToggleNetwork.broadcast(server);

        String message = enabled
                ? "Enciclopedia activada para todos."
                : "Enciclopedia desactivada para todos.";

        context.getSource().sendSuccess(
                () -> Component.literal(message),
                true
        );

        return Command.SINGLE_SUCCESS;
    }
}