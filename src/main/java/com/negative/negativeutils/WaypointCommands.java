package com.negative.negativeutils;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid="negativeutils",bus=Mod.EventBusSubscriber.Bus.FORGE)
public final class WaypointCommands {
    private WaypointCommands(){}
    @SubscribeEvent public static void registerCommands(RegisterCommandsEvent event){
        event.getDispatcher().register(Commands.literal("negativeutils").requires(s->s.hasPermission(2)).then(Commands.literal("waypoints")
            .then(Commands.literal("crear").executes(c->{ServerPlayer p=c.getSource().getPlayerOrException();WaypointNetwork.openCreate(p);return Command.SINGLE_SUCCESS;}))
            .then(Commands.literal("editar").then(Commands.argument("nombre",StringArgumentType.string()).suggests((c,b)->SharedSuggestionProvider.suggest(WaypointSavedData.get(c.getSource().getServer()).getWaypoints().stream().map(WaypointSavedData.Waypoint::name).toList(),b)).executes(c->{var w=WaypointSavedData.get(c.getSource().getServer()).getByName(StringArgumentType.getString(c,"nombre"));if(w==null){c.getSource().sendFailure(Component.literal("No existe ese waypoint."));return 0;}WaypointNetwork.openEdit(c.getSource().getPlayerOrException(),w);return Command.SINGLE_SUCCESS;})))
            .then(Commands.literal("mostrar").then(Commands.argument("nombre",StringArgumentType.string()).suggests((c,b)->suggest(c,b)).executes(c->setVisible(c,true))))
            .then(Commands.literal("ocultar").then(Commands.argument("nombre",StringArgumentType.string()).suggests((c,b)->suggest(c,b)).executes(c->setVisible(c,false))))
            .then(Commands.literal("eliminar").then(Commands.argument("nombre",StringArgumentType.string()).suggests((c,b)->suggest(c,b)).executes(c->{var d=WaypointSavedData.get(c.getSource().getServer());var w=d.getByName(StringArgumentType.getString(c,"nombre"));if(w==null)return 0;d.removeById(w.id());WaypointNetwork.syncAll(d.getWaypoints());c.getSource().sendSuccess(()->Component.literal("Waypoint eliminado."),true);return Command.SINGLE_SUCCESS;})))
            .then(Commands.literal("lista").executes(c->{var ws=WaypointSavedData.get(c.getSource().getServer()).getWaypoints();if(ws.isEmpty())c.getSource().sendSuccess(()->Component.literal("No hay waypoints."),false);for(var w:ws)c.getSource().sendSuccess(()->Component.literal(w.name()+" | "+(w.visible()?"visible":"oculto")+" | "+w.dimension()+" | "+Math.round(w.x())+", "+Math.round(w.y())+", "+Math.round(w.z())),false);return Command.SINGLE_SUCCESS;}))
            .then(Commands.literal("clear").executes(c->{var d=WaypointSavedData.get(c.getSource().getServer());int n=d.removeAll();WaypointNetwork.syncAll(d.getWaypoints());c.getSource().sendSuccess(()->Component.literal("Se eliminaron "+n+" waypoints."),true);return Command.SINGLE_SUCCESS;}))));
    }
    private static com.mojang.brigadier.suggestion.Suggestions suggest(com.mojang.brigadier.context.CommandContext<net.minecraft.commands.CommandSourceStack> c,com.mojang.brigadier.suggestions.SuggestionsBuilder b){return SharedSuggestionProvider.suggest(WaypointSavedData.get(c.getSource().getServer()).getWaypoints().stream().map(WaypointSavedData.Waypoint::name).toList(),b);}
    private static int setVisible(com.mojang.brigadier.context.CommandContext<net.minecraft.commands.CommandSourceStack> c,boolean visible){var d=WaypointSavedData.get(c.getSource().getServer());var w=d.getByName(StringArgumentType.getString(c,"nombre"));if(w==null){c.getSource().sendFailure(Component.literal("No existe ese waypoint."));return 0;}d.setVisible(w.id(),visible);WaypointNetwork.syncAll(d.getWaypoints());c.getSource().sendSuccess(()->Component.literal(visible?"Waypoint activado.":"Waypoint desactivado."),true);return Command.SINGLE_SUCCESS;}
}