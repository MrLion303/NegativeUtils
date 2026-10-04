package com.negative.negativeutils;

import java.util.Locale;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public final class TrailScreen extends Screen {
    private static final int WHEEL_RADIUS = 66;
    private static final int WHEEL_CELL_SIZE = 3;
    private static final int SLIDER_WIDTH = 176;

    private int red;
    private int green;
    private int blue;
    private int opacityPercent;
    private int wheelCenterX;
    private int wheelCenterY;
    private int sliderLeft;
    private int sliderY;
    private boolean draggingOpacity;

    public TrailScreen(
            int red,
            int green,
            int blue,
            int opacityPercent
    ) {
        super(Component.literal("Ajustes de la guía"));
        this.red = clamp(red, 0, 255);
        this.green = clamp(green, 0, 255);
        this.blue = clamp(blue, 0, 255);
        this.opacityPercent = clamp(opacityPercent, 0, 100);
    }

    @Override
    protected void init() {
        wheelCenterX = width / 2;
        wheelCenterY = height / 2 - 47;
        sliderLeft = width / 2 - SLIDER_WIDTH / 2;
        sliderY = wheelCenterY + WHEEL_RADIUS + 36;

        addRenderableWidget(
                Button.builder(
                                Component.literal("Aplicar"),
                                button -> {
                                    TrailNetwork.saveSettings(
                                            red,
                                            green,
                                            blue,
                                            opacityPercent
                                    );
                                    onClose();
                                }
                        )
                        .bounds(width / 2 - 96, sliderY + 30, 90, 20)
                        .build()
        );
        addRenderableWidget(
                Button.builder(
                                Component.literal("Cancelar"),
                                button -> onClose()
                        )
                        .bounds(width / 2 + 6, sliderY + 30, 90, 20)
                        .build()
        );
    }

    private boolean updateColorFromWheel(double mouseX, double mouseY) {
        double dx = mouseX - wheelCenterX;
        double dy = mouseY - wheelCenterY;
        double distance = Math.sqrt(dx * dx + dy * dy);
        if (distance > WHEEL_RADIUS) {
            return false;
        }

        float hue = (float) (
                (Math.atan2(dy, dx) / (Math.PI * 2.0) + 1.0) % 1.0
        );
        float saturation = (float) (distance / WHEEL_RADIUS);
        int color = java.awt.Color.HSBtoRGB(hue, saturation, 1.0F);
        red = color >>> 16 & 0xFF;
        green = color >>> 8 & 0xFF;
        blue = color & 0xFF;
        return true;
    }

    private void updateOpacity(double mouseX) {
        double progress = (mouseX - sliderLeft) / SLIDER_WIDTH;
        opacityPercent = clamp((int) Math.round(progress * 100), 0, 100);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0 && updateColorFromWheel(mouseX, mouseY)) {
            return true;
        }
        if (button == 0
                && mouseY >= sliderY - 5
                && mouseY <= sliderY + 9
                && mouseX >= sliderLeft - 6
                && mouseX <= sliderLeft + SLIDER_WIDTH + 6) {
            draggingOpacity = true;
            updateOpacity(mouseX);
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(
            double mouseX,
            double mouseY,
            int button,
            double dragX,
            double dragY
    ) {
        if (button == 0 && updateColorFromWheel(mouseX, mouseY)) {
            return true;
        }
        if (button == 0 && draggingOpacity) {
            updateOpacity(mouseX);
            return true;
        }
        return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    @Override
    public boolean mouseReleased(
            double mouseX,
            double mouseY,
            int button
    ) {
        if (button == 0 && draggingOpacity) {
            draggingOpacity = false;
            return true;
        }
        return super.mouseReleased(mouseX, mouseY, button);
    }

    private void renderWheel(GuiGraphics graphics) {
        for (int y = -WHEEL_RADIUS; y < WHEEL_RADIUS; y += WHEEL_CELL_SIZE) {
            for (int x = -WHEEL_RADIUS; x < WHEEL_RADIUS; x += WHEEL_CELL_SIZE) {
                double sampleX = x + WHEEL_CELL_SIZE / 2.0;
                double sampleY = y + WHEEL_CELL_SIZE / 2.0;
                double distance = Math.sqrt(
                        sampleX * sampleX + sampleY * sampleY
                );
                if (distance > WHEEL_RADIUS) {
                    continue;
                }

                float hue = (float) (
                        (Math.atan2(sampleY, sampleX) / (Math.PI * 2.0) + 1.0)
                                % 1.0
                );
                float saturation = (float) (distance / WHEEL_RADIUS);
                int color = java.awt.Color.HSBtoRGB(hue, saturation, 1.0F);
                graphics.fill(
                        wheelCenterX + x,
                        wheelCenterY + y,
                        wheelCenterX + x + WHEEL_CELL_SIZE,
                        wheelCenterY + y + WHEEL_CELL_SIZE,
                        0xFF000000 | color & 0xFFFFFF
                );
            }
        }

        float[] hsv = java.awt.Color.RGBtoHSB(red, green, blue, null);
        double angle = hsv[0] * Math.PI * 2.0;
        double radius = hsv[1] * WHEEL_RADIUS;
        int markerX = wheelCenterX + (int) (Math.cos(angle) * radius);
        int markerY = wheelCenterY + (int) (Math.sin(angle) * radius);
        graphics.fill(markerX - 4, markerY - 1, markerX + 5, markerY + 2, 0xFF000000);
        graphics.fill(markerX - 1, markerY - 4, markerX + 2, markerY + 5, 0xFF000000);
        graphics.fill(markerX - 3, markerY, markerX + 4, markerY + 1, 0xFFFFFFFF);
        graphics.fill(markerX, markerY - 3, markerX + 1, markerY + 4, 0xFFFFFFFF);
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

        int panelLeft = width / 2 - 112;
        int panelRight = width / 2 + 112;
        int panelTop = wheelCenterY - WHEEL_RADIUS - 30;
        int panelBottom = sliderY + 58;
        graphics.fill(panelLeft, panelTop, panelRight, panelBottom, 0xE8141B24);
        graphics.fill(panelLeft, panelTop, panelRight, panelTop + 2, 0xFF54D6FF);
        graphics.drawCenteredString(
                font,
                "Ajustes de la guía",
                width / 2,
                panelTop + 8,
                0xFFFFFFFF
        );

        renderWheel(graphics);
        String hex = String.format(Locale.ROOT, "#%02X%02X%02X", red, green, blue);
        graphics.drawCenteredString(
                font,
                hex,
                width / 2,
                wheelCenterY + WHEEL_RADIUS + 5,
                0xFFFFFFFF
        );
        graphics.fill(
                width / 2 - 10,
                wheelCenterY + WHEEL_RADIUS + 2,
                width / 2 + 10,
                wheelCenterY + WHEEL_RADIUS + 18,
                ((opacityPercent * 255 / 100) << 24)
                        | red << 16
                        | green << 8
                        | blue
        );

        graphics.drawString(
                font,
                "Opacidad",
                sliderLeft,
                sliderY - 15,
                0xFFFFFFFF
        );
        graphics.drawString(
                font,
                opacityPercent + "%",
                sliderLeft + SLIDER_WIDTH - font.width(opacityPercent + "%"),
                sliderY - 15,
                0xFFFFFFFF
        );
        graphics.fill(
                sliderLeft,
                sliderY,
                sliderLeft + SLIDER_WIDTH,
                sliderY + 4,
                0xFF343A42
        );
        int filledWidth = SLIDER_WIDTH * opacityPercent / 100;
        graphics.fill(
                sliderLeft,
                sliderY,
                sliderLeft + filledWidth,
                sliderY + 4,
                0xFF54D6FF
        );
        int handleX = sliderLeft + filledWidth;
        graphics.fill(handleX - 3, sliderY - 3, handleX + 4, sliderY + 8, 0xFFFFFFFF);
        graphics.drawString(font, "0", sliderLeft, sliderY + 8, 0xFFBBBBBB);
        String maximum = "100";
        graphics.drawString(
                font,
                maximum,
                sliderLeft + SLIDER_WIDTH - font.width(maximum),
                sliderY + 8,
                0xFFBBBBBB
        );

        super.render(graphics, mouseX, mouseY, partialTick);
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
