package com.negative.negativeutils;

import java.util.UUID;

import net.minecraft.client.Minecraft;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
final class TrailClientPacketHandler {
    private TrailClientPacketHandler() {
    }

    static void openSettings(
            UUID trailId,
            String name,
            int red,
            int green,
            int blue,
            int opacityPercent
    ) {
        Minecraft.getInstance().setScreen(
                new TrailScreen(
                        trailId,
                        name,
                        red,
                        green,
                        blue,
                        opacityPercent
                )
        );
    }
}
