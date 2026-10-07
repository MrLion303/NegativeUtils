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

        // El minimapa de Xaero también ocupa espacio en resoluciones pequeñas.
        // Reducimos la reserva de forma proporcional, pero nunca la eliminamos.
        int proportional = Math.min(132, Math.max(84, height / 3));
        return Math.min(proportional, Math.max(0, height - 42));
    }

    public static boolean topLeftAreaReserved(Minecraft minecraft, int width, int height) {
        return topLeftOffset(minecraft, width, height) > 0
                && width >= XAERO_MINIMAP_RESERVED_WIDTH;
    }
}
