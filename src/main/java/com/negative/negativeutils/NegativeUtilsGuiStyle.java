package com.negative.negativeutils;

import net.minecraft.client.gui.GuiGraphics;

public final class NegativeUtilsGuiStyle {
    private static final int ACCENT = 0xFF54D6FF;

    private NegativeUtilsGuiStyle() {
    }

    public static void renderFrame(
            GuiGraphics graphics,
            int width,
            int height
    ) {
        graphics.fill(0, 0, width, height, 0x160B111A);
        graphics.fill(0, 0, width, 2, ACCENT);
        graphics.fill(0, 2, 2, height, 0xAA54D6FF);
        graphics.fill(width - 2, 2, width, height, 0xAA54D6FF);
        graphics.fill(2, height - 2, width - 2, height, 0xAA54D6FF);
    }
}
