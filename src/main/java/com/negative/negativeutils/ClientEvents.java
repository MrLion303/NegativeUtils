package com.negative.negativeutils;

import net.minecraft.client.Minecraft;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(
        modid = EnciclopediaMod.MOD_ID,
        value = Dist.CLIENT
)
public final class ClientEvents {
    private ClientEvents() {
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }

        Minecraft minecraft = Minecraft.getInstance();

        while (ModKeyMappings.OPEN_ENCYCLOPEDIA.consumeClick()) {
            if (minecraft.player != null
                    && FeatureToggleClientState.isEncyclopediaEnabled()) {
                minecraft.setScreen(new EncyclopediaScreen());
            }
        }

        while (ModKeyMappings.OPEN_GUILDS.consumeClick()) {
            if (minecraft.player != null
                    && FeatureToggleClientState.areGuildsEnabled()) {
                minecraft.setScreen(new GuildScreen());
            }
        }

        while (TrailKeyMappings.TOGGLE_TRAIL.consumeClick()) {
            TrailClientState.toggleVisible();
        }
    }
}