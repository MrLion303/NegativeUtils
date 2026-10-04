package com.negative.negativeutils;

import net.minecraft.client.Minecraft;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
final class TrailClientPacketHandler {
    private TrailClientPacketHandler() {
    }

    static void openSettings(
            int red,
            int green,
            int blue,
            int opacityPercent
    ) {
        Minecraft.getInstance().setScreen(
                new TrailScreen(red, green, blue, opacityPercent)
        );
    }
}
