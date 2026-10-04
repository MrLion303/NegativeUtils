package com.negative.negativeutils;

import com.mojang.brigadier.Command;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.coordinates.Vec3Argument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(
        modid = "negativeutils",
        bus = Mod.EventBusSubscriber.Bus.FORGE
)
public final class YoCommands {
    private YoCommands() {
    }

    @SubscribeEvent
    public static void registerCommands(RegisterCommandsEvent event) {
        event.getDispatcher().register(
                Commands.literal("yo")
                        .executes(context ->
                                teleport(context.getSource().getPlayerOrException())
                        )
        );

        event.getDispatcher().register(
                Commands.literal("momento")
                        .requires(source -> source.hasPermission(2))
                        .then(
                                Commands.literal("set")
                                        .then(
                                                Commands.literal("ubicacion")
                                                        .then(
                                                                Commands.argument(
                                                                                "coordenadas",
                                                                                Vec3Argument.vec3()
                                                                        )
                                                                        .executes(context -> {
                                                                            Vec3 position =
                                                                                    Vec3Argument.getVec3(
                                                                                            context,
                                                                                            "coordenadas"
                                                                                    );
                                                                            ServerLevel level =
                                                                                    context.getSource()
                                                                                            .getLevel();
                                                                            MomentoSavedData data =
                                                                                    MomentoSavedData.get(
                                                                                            context.getSource()
                                                                                                    .getServer()
                                                                                    );
                                                                            data.setLocation(
                                                                                    level,
                                                                                    position
                                                                            );
                                                                            context.getSource()
                                                                                    .sendSuccess(
                                                                                            () -> Component.literal(
                                                                                                    "Ubicación de /yo guardada en "
                                                                                                            + format(position)
                                                                                                            + " ("
                                                                                                            + level.dimension()
                                                                                                                    .location()
                                                                                                            + ")."
                                                                                            ),
                                                                                            true
                                                                                    );
                                                                            return Command.SINGLE_SUCCESS;
                                                                        })
                                                        )
                                        )
                        )
        );
    }

    private static int teleport(ServerPlayer player) {
        MomentoSavedData data = MomentoSavedData.get(player.getServer());
        if (!data.isConfigured()) {
            player.sendSystemMessage(
                    Component.literal(
                            "La ubicación de /yo todavía no está configurada."
                    )
            );
            return 0;
        }

        ServerLevel destination = player.getServer().getLevel(data.getDimension());
        if (destination == null) {
            player.sendSystemMessage(
                    Component.literal(
                            "La dimensión guardada para /yo no está disponible."
                    )
            );
            return 0;
        }

        Vec3 position = data.getPosition();
        player.teleportTo(
                destination,
                position.x,
                position.y,
                position.z,
                player.getYRot(),
                player.getXRot()
        );
        player.sendSystemMessage(
                Component.literal(
                        "Teletransportado a " + format(position) + "."
                )
        );
        return Command.SINGLE_SUCCESS;
    }

    private static String format(Vec3 position) {
        return String.format(
                java.util.Locale.ROOT,
                "%.2f, %.2f, %.2f",
                position.x,
                position.y,
                position.z
        );
    }
}
