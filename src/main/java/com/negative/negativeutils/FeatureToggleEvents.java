package com.negative.negativeutils;

import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(
        modid = EnciclopediaMod.MOD_ID
)
public final class FeatureToggleEvents {
    private FeatureToggleEvents() {
    }

    @SubscribeEvent
    public static void onPlayerLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            FeatureToggleNetwork.syncTo(player);
        }
    }
}