package com.negative.negativeutils;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraftforge.fml.ModList;

public final class NegativeUtilsHudLayout {
    private static final int XAERO_MINIMAP_RESERVED_HEIGHT = 132;
    private static final int XAERO_MINIMAP_RESERVED_WIDTH = 132;

    private NegativeUtilsHudLayout() {
    }

    public static int topLeftOffset(Minecraft minecraft, int width, int height) {
        if (!ModList.get().isLoaded("xaerominimap")) {
            return 0;
        }

        // Xaero's minimap is normally anchored to a screen corner and its
        // default footprint is roughly 128x128 at GUI scale. Keep our HUD
        // below that area instead of painting over it.
        return Math.min(XAERO_MINIMAP_RESERVED_HEIGHT, Math.max(0, height - 48));
    }

    public static boolean topLeftAreaReserved(Minecraft minecraft, int width, int height) {
        return topLeftOffset(minecraft, width, height) > 0
                && width >= XAERO_MINIMAP_RESERVED_WIDTH;
    }
}
