package com.negative.negativeutils;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid="negativeutils",bus=Mod.EventBusSubscriber.Bus.FORGE)
public final class TrailWandEvents {
    private TrailWandEvents(){}
    private static void message(ServerPlayer player,String text){player.sendSystemMessage(Component.literal(text).withStyle(ChatFormatting.GOLD));}

    @SubscribeEvent public static void onLeftClickBlock(PlayerInteractEvent.LeftClickBlock event){
        if(!event.getItemStack().is(ModItems.TRAIL_WAND.get()))return;
        event.setCanceled(true);event.setCancellationResult(InteractionResult.SUCCESS);
        if(event.getLevel().isClientSide()||!(event.getEntity() instanceof ServerPlayer player)||!player.hasPermissions(2))return;
        ServerLevel level=(ServerLevel)event.getLevel();TrailSavedData data=TrailSavedData.get(level.getServer());
        java.util.UUID selected=TrailSelectionState.get(player.getUUID());
        TrailSavedData.Trail trail=selected==null?null:data.getById(selected);
        if(trail==null){
            trail=data.create(data.nextAutomaticName());
            if(trail==null){message(player,"No se pudo crear el sendero.");return;}
            TrailSelectionState.select(player.getUUID(),trail.id());
            message(player,"Sendero '"+trail.name()+"' creado y seleccionado.");
        }
        data.resetPoints(trail.id(),level,event.getPos().getX(),event.getPos().getY(),event.getPos().getZ());
        TrailNetwork.syncAll(data.getTrails());
        message(player,"Punto A marcado en '"+trail.name()+"'. Clic derecho para añadir los siguientes puntos.");
    }

    @SubscribeEvent public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event){
        if(!event.getItemStack().is(ModItems.TRAIL_WAND.get()))return;
        event.setCanceled(true);event.setCancellationResult(InteractionResult.SUCCESS);
        if(event.getLevel().isClientSide()||!(event.getEntity() instanceof ServerPlayer player)||!player.hasPermissions(2))return;
        ServerLevel level=(ServerLevel)event.getLevel();TrailSavedData data=TrailSavedData.get(level.getServer());
        java.util.UUID selected=TrailSelectionState.get(player.getUUID());TrailSavedData.Trail trail=selected==null?null:data.getById(selected);
        if(trail==null){message(player,"Primero crea o selecciona un sendero.");return;}
        data.addPoint(trail.id(),level,event.getPos().getX(),event.getPos().getY(),event.getPos().getZ());
        TrailNetwork.syncAll(data.getTrails());
        message(player,"Punto añadido a '"+trail.name()+"' ("+trail.points().size()+").");
    }
}