package com.negative.negativeutils;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
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
                                Commands.literal("contador")
                                        .executes(context -> {
                                            ServerPlayer player =
                                                    context.getSource()
                                                            .getPlayerOrException();
                                            CountdownNetwork.openAdminPanel(player);
                                            return Command.SINGLE_SUCCESS;
                                        })
                                        .then(Commands.literal("mostrar")
                                                .then(Commands.argument("nombre", StringArgumentType.string())
                                                        .executes(context -> setDisplayed(context, true))))
                                        .then(Commands.literal("activar")
                                                .then(Commands.argument("nombre", StringArgumentType.string())
                                                        .executes(context -> setDisplayed(context, true))))
                                        .then(Commands.literal("ocultar")
                                                .then(Commands.argument("nombre", StringArgumentType.string())
                                                        .executes(context -> setDisplayed(context, false))))
                                        .then(Commands.literal("desactivar")
                                                .then(Commands.argument("nombre", StringArgumentType.string())
                                                        .executes(context -> setDisplayed(context, false))))
                        )
        );
    }

    private static int setDisplayed(
            CommandContext<CommandSourceStack> context,
            boolean displayed
    ) {
        String name = StringArgumentType.getString(context, "nombre");
        var data = CountdownSavedData.get(context.getSource().getServer());
        CountdownSavedData.Countdown countdown = data.getByName(name);
        if (countdown == null) {
            context.getSource().sendFailure(
                    Component.literal("No existe una cuenta regresiva llamada \"" + name + "\".")
            );
            return 0;
        }

        data.setDisplayed(countdown.id(), displayed);
        CountdownNetwork.syncAll(data);
        String state = displayed ? "visible" : "oculta";
        context.getSource().sendSuccess(
                () -> Component.literal("La cuenta regresiva \"" + countdown.name()
                        + "\" ahora está " + state + "."),
                false
        );
        return Command.SINGLE_SUCCESS;
    }
}
