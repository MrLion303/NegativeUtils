package com.negative.negativeutils;

import java.util.Locale;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;

public class WaypointScreen extends Screen {
    private static final int WHEEL_RADIUS = 40;
    private static final int WHEEL_CELL_SIZE = 4;
    private static final int WHEEL_CENTER_Y = 135;

    private final BlockPos blockPos;
    private final String dimension;
    private EditBox nameInput;
    private int selectedColor = 0x40D8FF;

    public WaypointScreen(BlockPos blockPos, String dimension) {
        super(Component.literal("Crear waypoint"));
        this.blockPos = blockPos;
        this.dimension = dimension;
    }

    @Override
    protected void init() {
        int centerX = width / 2;
        nameInput = new EditBox(
                font,
                centerX - 100,
                54,
                200,
                20,
                Component.literal("Nombre del waypoint")
        );
        nameInput.setMaxLength(32);
        nameInput.setHint(Component.literal("Nombre (opcional)"));
        addRenderableWidget(nameInput);
        setInitialFocus(nameInput);

        addRenderableWidget(
                Button.builder(
                        Component.literal("Listo"),
                        button -> saveWaypoint()
                )
                .bounds(centerX - 50, height - 36, 100, 20)
                .build()
        );
    }

    private void saveWaypoint() {
        String name = nameInput.getValue().trim();
        WaypointNetwork.create(
                blockPos,
                dimension,
                name,
                selectedColor
        );
        Minecraft.getInstance().setScreen(null);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        double dx = mouseX - width / 2.0;
        double dy = mouseY - WHEEL_CENTER_Y;
        if (button == 0
                && dx * dx + dy * dy <= WHEEL_RADIUS * WHEEL_RADIUS) {
            float hue = (float) (
                    (Math.atan2(dy, dx) / (Math.PI * 2.0) + 1.0) % 1.0
            );
            float saturation = (float) (
                    Math.sqrt(dx * dx + dy * dy) / WHEEL_RADIUS
            );
            selectedColor = java.awt.Color.HSBtoRGB(
                    hue,
                    saturation,
                    1.0F
            ) & 0xFFFFFF;
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public void render(
            GuiGraphics graphics,
            int mouseX,
            int mouseY,
            float partialTick
    ) {
        renderBackground(graphics);
        NegativeUtilsGuiStyle.renderFrame(graphics, width, height);
        int centerX = width / 2;
        graphics.fill(
                centerX - 120,
                20,
                centerX + 120,
                height - 25,
                0xE8141B24
        );
        graphics.fill(
                centerX - 120,
                20,
                centerX + 120,
                21,
                0xFF54D6FF
        );
        graphics.drawCenteredString(
                font,
                "Crear waypoint",
                centerX,
                30,
                0xFFFFFFFF
        );
        graphics.drawString(
                font,
                "Nombre",
                centerX - 100,
                43,
                0xFFD8E1EA
        );

        renderColorWheel(graphics);
        graphics.drawCenteredString(
                font,
                String.format(Locale.ROOT, "#%06X", selectedColor),
                centerX,
                WHEEL_CENTER_Y + WHEEL_RADIUS + 5,
                0xFFFFFFFF
        );

        super.render(graphics, mouseX, mouseY, partialTick);
    }

    private void renderColorWheel(GuiGraphics graphics) {
        int centerX = width / 2;
        for (int y = -WHEEL_RADIUS; y < WHEEL_RADIUS; y += WHEEL_CELL_SIZE) {
            for (int x = -WHEEL_RADIUS; x < WHEEL_RADIUS; x += WHEEL_CELL_SIZE) {
                double sampleX = x + WHEEL_CELL_SIZE / 2.0;
                double sampleY = y + WHEEL_CELL_SIZE / 2.0;
                double distance = Math.sqrt(sampleX * sampleX + sampleY * sampleY);
                if (distance > WHEEL_RADIUS) {
                    continue;
                }

                float hue = (float) (
                        (Math.atan2(sampleY, sampleX) / (Math.PI * 2.0) + 1.0) % 1.0
                );
                float saturation = (float) (distance / WHEEL_RADIUS);
                int color = java.awt.Color.HSBtoRGB(hue, saturation, 1.0F);
                graphics.fill(
                        centerX + x,
                        WHEEL_CENTER_Y + y,
                        centerX + x + WHEEL_CELL_SIZE,
                        WHEEL_CENTER_Y + y + WHEEL_CELL_SIZE,
                        0xFF000000 | color & 0xFFFFFF
                );
            }
        }

        float[] hsv = java.awt.Color.RGBtoHSB(
                selectedColor >>> 16 & 0xFF,
                selectedColor >>> 8 & 0xFF,
                selectedColor & 0xFF,
                null
        );
        double markerAngle = hsv[0] * Math.PI * 2.0;
        double markerRadius = hsv[1] * WHEEL_RADIUS;
        int markerX = centerX + (int) (Math.cos(markerAngle) * markerRadius);
        int markerY = WHEEL_CENTER_Y
                + (int) (Math.sin(markerAngle) * markerRadius);
        graphics.fill(markerX - 3, markerY - 3, markerX + 4, markerY + 4, 0xFF000000);
        graphics.fill(markerX - 1, markerY - 1, markerX + 2, markerY + 2, 0xFFFFFFFF);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
