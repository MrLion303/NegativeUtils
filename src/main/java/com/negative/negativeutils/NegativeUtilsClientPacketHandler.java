package com.negative.negativeutils;

import java.util.Map;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
final class NegativeUtilsClientPacketHandler {
    private NegativeUtilsClientPacketHandler() {
    }

    static void openTimeDelayEditor(BlockPos pos, int seconds) {
        Minecraft.getInstance().setScreen(new TimeDelayScreen(pos, seconds));
    }

    static void openCommandSequenceEditor(BlockPos pos, String commands) {
        Minecraft.getInstance().setScreen(
                new CommandSequenceScreen(pos, commands)
        );
    }

    static void openDiscordTokenScreen() {
        Minecraft.getInstance().setScreen(new DiscordTokenScreen());
    }

    static void openAdminPanel() {
        Minecraft.getInstance().setScreen(new AdminPanelScreen());
    }

    static void refreshAdminPanel() {
        if (Minecraft.getInstance().screen instanceof AdminPanelScreen screen) {
            screen.refreshData();
        }
    }

    static void openWaypointEditor(java.util.UUID id, String name, String dimension,
                                   double x, double y, double z, int color, String icon) {
        Minecraft.getInstance().setScreen(
                new WaypointScreen(id, name, dimension, x, y, z, color, icon)
        );
    }

    static void acceptDiscordEmotes(Map<String, Integer> mapping, byte[] atlas) {
        DiscordEmoteClientData.accept(mapping, atlas);
    }

    static void updateCountdown(
            boolean configured,
            boolean running,
            boolean finished,
            long endTimeMillis,
            long pausedRemainingMillis,
            String displayText,
            int displayColor,
            String displayPosition
    ) {
        CountdownClientData.update(
                configured,
                running,
                finished,
                endTimeMillis,
                pausedRemainingMillis,
                displayText,
                displayColor,
                displayPosition
        );
    }

    static void showDiscoveryToast(String notificationText) {
        ClientToastHandler.showDiscovery(notificationText);
    }
}
