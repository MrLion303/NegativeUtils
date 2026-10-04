package com.negative.negativeutils;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(
        modid = EnciclopediaMod.MOD_ID,
        bus = Mod.EventBusSubscriber.Bus.FORGE
)
public final class GuildMotdCommands {
    private GuildMotdCommands() {
    }

    @SubscribeEvent
    public static void registerCommands(RegisterCommandsEvent event) {
        event.getDispatcher().register(
                Commands.literal("guild")
                        .then(Commands.literal("motd")
                                .then(Commands.argument(
                                                "texto",
                                                StringArgumentType.greedyString()
                                        )
                                        .executes(context -> {
                                            ServerPlayer player = context
                                                    .getSource()
                                                    .getPlayerOrException();
                                            GuildSavedData data =
                                                    GuildSavedData.get(
                                                            player.getServer()
                                                    );
                                            GuildSavedData.Guild guild =
                                                    data.getGuildForPlayer(
                                                            player.getUUID()
                                                    );
                                            if (guild == null) {
                                                context.getSource().sendFailure(
                                                        Component.literal(
                                                                "No perteneces a una hermandad."
                                                        )
                                                );
                                                return 0;
                                            }

                                            String text = StringArgumentType
                                                    .getString(context, "texto");
                                            if (!data.setMotd(
                                                    guild.id(),
                                                    player.getUUID(),
                                                    text
                                            )) {
                                                context.getSource().sendFailure(
                                                        Component.literal(
                                                                "Solo el líder o un oficial puede cambiar el MOTD."
                                                        )
                                                );
                                                return 0;
                                            }

                                            GuildActionsNetwork.syncGuild(
                                                    player,
                                                    guild.id()
                                            );
                                            context.getSource().sendSuccess(
                                                    () -> Component.literal(
                                                            "MOTD de la hermandad actualizado."
                                                    ),
                                                    false
                                            );
                                            return Command.SINGLE_SUCCESS;
                                        })
                                )
                        )
        );
    }
}
