package com.negative.negativeutils;

import java.util.Collection;
import java.util.List;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** Comandos para permitir o restringir el cambio de perspectiva de los jugadores. */
@Mod.EventBusSubscriber(
        modid = NegativeUtilsMod.MOD_ID,
        bus = Mod.EventBusSubscriber.Bus.FORGE
)
public final class F5PerspectiveCommands {
    private F5PerspectiveCommands() {}

    @SubscribeEvent
    public static void registerCommands(RegisterCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("negativeutils")
                .requires(source -> source.hasPermission(2))
                .then(Commands.literal("f5")
                        .then(Commands.literal("disable")
                                .executes(context -> setForSender(context, true))
                                .then(Commands.argument("jugadores", EntityArgument.players())
                                        .executes(context -> setForTargets(
                                                context,
                                                EntityArgument.getPlayers(context, "jugadores"),
                                                true
                                        ))))
                        .then(Commands.literal("enable")
                                .executes(context -> setForSender(context, false))
                                .then(Commands.argument("jugadores", EntityArgument.players())
                                        .executes(context -> setForTargets(
                                                context,
                                                EntityArgument.getPlayers(context, "jugadores"),
                                                false
                                        ))))));
    }

    private static int setForSender(
            CommandContext<CommandSourceStack> context,
            boolean disabled
    ) throws CommandSyntaxException {
        return setForTargets(
                context,
                List.of(context.getSource().getPlayerOrException()),
                disabled
        );
    }

    private static int setForTargets(
            CommandContext<CommandSourceStack> context,
            Collection<ServerPlayer> players,
            boolean disabled
    ) {
        if (players.isEmpty()) {
            context.getSource().sendFailure(Component.literal(
                    "No se encontraron jugadores para cambiar la perspectiva."
            ));
            return 0;
        }

        F5PerspectiveSavedData data = F5PerspectiveSavedData.get(
                context.getSource().getServer()
        );
        for (ServerPlayer player : players) {
            data.setDisabled(player.getUUID(), disabled);
            F5PerspectiveNetwork.syncToPlayer(player, disabled);
        }

        String action = disabled
                ? "Cambio de perspectiva bloqueado en primera persona para "
                : "Cambio de perspectiva habilitado para ";
        String message = action + players.size()
                + (players.size() == 1 ? " jugador." : " jugadores.");
        context.getSource().sendSuccess(() -> Component.literal(message), true);
        return Command.SINGLE_SUCCESS;
    }
}
