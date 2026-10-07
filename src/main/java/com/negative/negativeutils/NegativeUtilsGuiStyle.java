package com.negative.negativeutils;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.network.chat.Component;

public final class NegativeUtilsGuiStyle {
    private static final int ACCENT = 0xFF54D6FF;
    private static final int FIELD_HEIGHT = 22;

    private NegativeUtilsGuiStyle() {
    }

    public static void styleField(EditBox field) {
        field.setHeight(FIELD_HEIGHT);
        field.setHint(Component.empty());
        field.setTextColor(0xFFEAF7FF);
        field.setTextColorUneditable(0xFF9FAAB8);
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
