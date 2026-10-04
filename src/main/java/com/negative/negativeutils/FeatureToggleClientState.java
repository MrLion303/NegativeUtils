package com.negative.negativeutils;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(
        modid = EnciclopediaMod.MOD_ID,
        value = Dist.CLIENT
)
public final class FeatureToggleClientState {
    private static boolean guildsEnabled = true;
    private static boolean encyclopediaEnabled = true;

    private FeatureToggleClientState() {
    }

    public static boolean areGuildsEnabled() {
        return guildsEnabled;
    }

    public static boolean isEncyclopediaEnabled() {
        return encyclopediaEnabled;
    }

    public static void apply(
            boolean newGuildsEnabled,
            boolean newEncyclopediaEnabled
    ) {
        guildsEnabled = newGuildsEnabled;
        encyclopediaEnabled = newEncyclopediaEnabled;

        Minecraft minecraft = Minecraft.getInstance();
        Screen currentScreen = minecraft.screen;

        if (!guildsEnabled && currentScreen instanceof GuildScreen) {
            minecraft.setScreen(null);
            return;
        }

        if (!encyclopediaEnabled
                && (currentScreen instanceof EncyclopediaScreen
                || currentScreen instanceof EncyclopediaAdminScreen)) {
            minecraft.setScreen(null);
        }
    }

    @SubscribeEvent
    public static void onDisconnect(
            ClientPlayerNetworkEvent.LoggingOut event
    ) {
        // Evita que la configuración de un servidor afecte al siguiente.
        guildsEnabled = true;
        encyclopediaEnabled = true;
    }
}