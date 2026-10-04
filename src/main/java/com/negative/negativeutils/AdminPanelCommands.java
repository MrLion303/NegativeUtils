package com.negative.negativeutils;

import com.mojang.brigadier.Command;
import net.minecraft.commands.Commands;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(
        modid = "negativeutils",
        bus = Mod.EventBusSubscriber.Bus.FORGE
)
public final class AdminPanelCommands {
    private AdminPanelCommands() {
    }

    @SubscribeEvent
    public static void registerCommands(RegisterCommandsEvent event) {
        event.getDispatcher().register(
                Commands.literal("negativeutils")
                        .requires(source ->
                                source.hasPermission(2)
                                        && source.getEntity() instanceof ServerPlayer
                        )
                        .then(
                                Commands.literal("cuenta")
                                        .executes(context -> {
                                            ServerPlayer player =
                                                    context.getSource()
                                                            .getPlayerOrException();
                                            CountdownNetwork.openAdminPanel(player);
                                            return Command.SINGLE_SUCCESS;
                                        })
                        )
        );
    }
}
