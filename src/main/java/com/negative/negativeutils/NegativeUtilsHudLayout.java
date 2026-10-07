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

        // En resoluciones muy pequeñas no intentamos encajar un espacio fijo
        // debajo del minimapa: eso desplaza los HUD demasiado hacia abajo.
        // En su lugar, mantenemos el anclaje en la esquina superior izquierda.
        if (height < 300 || width < 300) {
            return 0;
        }

        // Xaero's minimap is normally anchored to a screen corner and its
        // default footprint is roughly 128x128 at GUI scale. Keep our HUD
        // below that area instead of desplazándolo según la altura disponible.
        return XAERO_MINIMAP_RESERVED_HEIGHT;
    }

    public static boolean topLeftAreaReserved(Minecraft minecraft, int width, int height) {
        return topLeftOffset(minecraft, width, height) > 0
                && width >= XAERO_MINIMAP_RESERVED_WIDTH;
    }
}
