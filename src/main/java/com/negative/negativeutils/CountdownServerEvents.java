package com.negative.negativeutils;

import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid="negativeutils",bus=Mod.EventBusSubscriber.Bus.FORGE)
public class CountdownServerEvents {
    private static int tickCounter;
    @SubscribeEvent public static void onPlayerLogin(PlayerEvent.PlayerLoggedInEvent event){
        if(event.getEntity() instanceof ServerPlayer player) CountdownNetwork.syncToPlayer(player);
    }
    @SubscribeEvent public static void onServerTick(TickEvent.ServerTickEvent event){
        if(event.phase!=TickEvent.Phase.END)return;
        if(++tickCounter<20)return;
        tickCounter=0;
        CountdownSavedData data=CountdownSavedData.get(event.getServer());
        if(data.updateFinished()) CountdownNetwork.syncAll(data);
    }
}