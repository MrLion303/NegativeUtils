package com.negative.negativeutils;

import com.mojang.brigadier.Command;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(
        modid = EnciclopediaMod.MOD_ID,
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
                                        .then(
                                                Commands.literal("clear")
                                                        .executes(context -> {
                                                            MinecraftServer server =
                                                                    context.getSource()
                                                                            .getServer();
                                                            WaypointSavedData data =
                                                                    WaypointSavedData
                                                                            .get(server);
                                                            int removed =
                                                                    data.removeAll();
                                                            WaypointNetwork.syncAll(
                                                                    data.getWaypoints()
                                                            );
                                                            context.getSource()
                                                                    .sendSuccess(
                                                                            () -> Component.literal(
                                                                                    "Se eliminaron "
                                                                                            + removed
                                                                                            + " waypoints."
                                                                            ),
                                                                            true
                                                                    );
                                                            return Command.SINGLE_SUCCESS;
                                                        })
                                        )
                        )
        );
    }
}
