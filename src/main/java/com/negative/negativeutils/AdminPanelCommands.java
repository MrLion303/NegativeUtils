package com.negative.negativeutils;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.coordinates.Vec3Argument;
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
                                        .then(Commands.literal("posicionar")
                                                .then(Commands.argument("nombre", StringArgumentType.string())
                                                        .then(Commands.argument("posicion", Vec3Argument.vec3())
                                                                .executes(context -> setHologramPosition(context, null))
                                                                .then(Commands.argument("imagen", StringArgumentType.string())
                                                                        .executes(context -> setHologramPosition(context,
                                                                                StringArgumentType.getString(context, "imagen")))))))
                                        .then(Commands.literal("pocisionar")
                                                .then(Commands.argument("nombre", StringArgumentType.string())
                                                        .then(Commands.argument("posicion", Vec3Argument.vec3())
                                                                .executes(context -> setHologramPosition(context, null))
                                                                .then(Commands.argument("imagen", StringArgumentType.string())
                                                                        .executes(context -> setHologramPosition(context,
                                                                                StringArgumentType.getString(context, "imagen")))))))
                                        .then(Commands.literal("desposicionar")
                                                .then(Commands.argument("nombre", StringArgumentType.string())
                                                        .executes(AdminPanelCommands::clearHologramPosition)))
                        )
                        .then(Commands.literal("contadorminus")
                                .then(Commands.literal("crear")
                                        .then(Commands.argument("id", StringArgumentType.word())
                                                .then(Commands.argument("minutos", IntegerArgumentType.integer(1, 525600))
                                                        .executes(AdminPanelCommands::createMinuteCountdown))))
                                .then(Commands.literal("mostrar")
                                        .then(Commands.argument("id", StringArgumentType.word())
                                                .executes(context -> setMinuteCountdownDisplayed(context, true))))
                                .then(Commands.literal("ocultar")
                                        .then(Commands.argument("id", StringArgumentType.word())
                                                .executes(context -> setMinuteCountdownDisplayed(context, false))))
                                .then(Commands.literal("borrar")
                                        .then(Commands.argument("id", StringArgumentType.word())
                                                .executes(AdminPanelCommands::deleteMinuteCountdown)))
                        )
        );
    }

    private static int createMinuteCountdown(CommandContext<CommandSourceStack> context) {
        String id = StringArgumentType.getString(context, "id");
        int minutes = IntegerArgumentType.getInteger(context, "minutos");
        var data = CountdownSavedData.get(context.getSource().getServer());
        if (data.getByName(id) != null) {
            context.getSource().sendFailure(Component.literal("Ya existe un contador con el ID \"" + id + "\"."));
            return 0;
        }
        var countdown = data.createMinuteCountdown(id, minutes);
        if (countdown == null) {
            context.getSource().sendFailure(Component.literal("No se pudo crear el contador. Comprueba el ID y el tiempo."));
            return 0;
        }
        CountdownNetwork.syncAll(data);
        context.getSource().sendSuccess(
                () -> Component.literal("Contador \"" + countdown.name() + "\" creado para " + minutes
                        + " minuto(s). Usa /negativeutils contadorminus mostrar " + countdown.name() + " para iniciarlo."),
                false
        );
        return Command.SINGLE_SUCCESS;
    }

    private static int setMinuteCountdownDisplayed(CommandContext<CommandSourceStack> context, boolean displayed) {
        String id = StringArgumentType.getString(context, "id");
        var data = CountdownSavedData.get(context.getSource().getServer());
        var countdown = data.getByName(id);
        if (countdown == null || !countdown.minuteMode()) {
            context.getSource().sendFailure(Component.literal("No existe un contador contadorminus con el ID \"" + id + "\"."));
            return 0;
        }
        data.setMinuteCountdownDisplayed(countdown.id(), displayed);
        CountdownNetwork.syncAll(data);
        context.getSource().sendSuccess(
                () -> Component.literal(displayed
                        ? "Contador \"" + countdown.name() + "\" iniciado desde " + (countdown.durationMillis() / 60000L) + " minuto(s)."
                        : "Contador \"" + countdown.name() + "\" ocultado y reiniciado."),
                false
        );
        return Command.SINGLE_SUCCESS;
    }

    private static int deleteMinuteCountdown(CommandContext<CommandSourceStack> context) {
        String id = StringArgumentType.getString(context, "id");
        var data = CountdownSavedData.get(context.getSource().getServer());
        var countdown = data.getByName(id);
        if (countdown == null || !countdown.minuteMode()) {
            context.getSource().sendFailure(Component.literal("No existe un contador contadorminus con el ID \"" + id + "\"."));
            return 0;
        }
        data.remove(countdown.id());
        CountdownNetwork.syncAll(data);
        context.getSource().sendSuccess(() -> Component.literal("Contador \"" + id + "\" eliminado."), false);
        return Command.SINGLE_SUCCESS;
    }

    private static int setHologramPosition(CommandContext<CommandSourceStack> context, String imageName) {
        String name = StringArgumentType.getString(context, "nombre");
        var data = CountdownSavedData.get(context.getSource().getServer());
        CountdownSavedData.Countdown countdown = data.getByName(name);
        if (countdown == null) {
            context.getSource().sendFailure(
                    Component.literal("No existe una cuenta regresiva llamada \"" + name + "\".")
            );
            return 0;
        }

        ServerPlayer player = context.getSource().getPlayer();
        if (player == null) {
            context.getSource().sendFailure(Component.literal("Este comando solo puede usarse dentro del juego."));
            return 0;
        }
        var position = Vec3Argument.getVec3(context, "posicion");
        String dimension = player.level().dimension().location().toString();
        if (!data.setHologramPosition(countdown.id(), dimension, position, imageName)) {
            context.getSource().sendFailure(Component.literal(
                    "Nombre de imagen no válido. Usa un archivo PNG, por ejemplo \"hola.png\" (sin carpetas)."
            ));
            return 0;
        }

        CountdownNetwork.syncAll(data);
        String imageMessage = imageName == null
                ? (countdown.hologramImage().isBlank() ? "" : " con imagen \"" + countdown.hologramImage() + "\"")
                : (imageName.isBlank() ? "" : " con imagen \"" + imageName + "\"");
        context.getSource().sendSuccess(
                () -> Component.literal("Holograma del contador \"" + countdown.name()
                        + "\" colocado en " + String.format(java.util.Locale.ROOT, "%.2f, %.2f, %.2f",
                        position.x, position.y, position.z) + imageMessage + "."),
                false
        );
        return Command.SINGLE_SUCCESS;
    }

    private static int clearHologramPosition(CommandContext<CommandSourceStack> context) {
        String name = StringArgumentType.getString(context, "nombre");
        var data = CountdownSavedData.get(context.getSource().getServer());
        CountdownSavedData.Countdown countdown = data.getByName(name);
        if (countdown == null) {
            context.getSource().sendFailure(
                    Component.literal("No existe una cuenta regresiva llamada \"" + name + "\".")
            );
            return 0;
        }
        if (!data.clearHologramPosition(countdown.id())) {
            context.getSource().sendFailure(Component.literal("No se pudo retirar el holograma."));
            return 0;
        }
        CountdownNetwork.syncAll(data);
        context.getSource().sendSuccess(
                () -> Component.literal("Holograma del contador \"" + countdown.name() + "\" retirado."),
                false
        );
        return Command.SINGLE_SUCCESS;
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
