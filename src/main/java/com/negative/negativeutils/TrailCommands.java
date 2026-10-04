package com.negative.negativeutils;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(
        modid = "negativeutils",
        bus = Mod.EventBusSubscriber.Bus.FORGE
)
public class TrailCommands {

    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        event.getDispatcher().register(
                Commands.literal("trail")
                        .requires(source -> source.hasPermission(2))
                        .executes(context -> {
                            TrailNetwork.openSettings(
                                    context.getSource().getPlayerOrException()
                            );
                            return 1;
                        })
                        .then(Commands.literal("color")
                                .then(Commands.argument(
                                                "rojo",
                                                IntegerArgumentType.integer(0, 255)
                                        )
                                        .then(Commands.argument(
                                                        "verde",
                                                        IntegerArgumentType.integer(0, 255)
                                                )
                                                .then(Commands.argument(
                                                                "azul",
                                                                IntegerArgumentType.integer(0, 255)
                                                        )
                                                        .then(Commands.argument(
                                                                        "opacidad",
                                                                        IntegerArgumentType.integer(0, 255)
                                                                )
                                                                .executes(context -> {
                                                                    int red = IntegerArgumentType.getInteger(
                                                                            context,
                                                                            "rojo"
                                                                    );
                                                                    int green = IntegerArgumentType.getInteger(
                                                                            context,
                                                                            "verde"
                                                                    );
                                                                    int blue = IntegerArgumentType.getInteger(
                                                                            context,
                                                                            "azul"
                                                                    );
                                                                    int alpha = IntegerArgumentType.getInteger(
                                                                            context,
                                                                            "opacidad"
                                                                    );

                                                                    TrailSavedData data =
                                                                            TrailSavedData.get(
                                                                                    context.getSource()
                                                                                            .getServer()
                                                                            );

                                                                    TrailNetwork.setColor(
                                                                            data.getPoints(),
                                                                            red,
                                                                            green,
                                                                            blue,
                                                                            alpha
                                                                    );

                                                                    context.getSource().sendSuccess(
                                                                            () -> Component.literal(
                                                                                    "Color de la guía actualizado: "
                                                                                            + red + ", "
                                                                                            + green + ", "
                                                                                            + blue
                                                                                            + " | opacidad: "
                                                                                            + alpha
                                                                            ),
                                                                            true
                                                                    );

                                                                    return 1;
                                                                })
                                                        )
                                                )
                                        )
                                )
                        )
        );
    }
}