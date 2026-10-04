package com.negative.negativeutils;

import java.time.DateTimeException;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Locale;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public class AdminPanelScreen extends Screen {
    private static final String[] POSITIONS = {
            "BOSSBAR",
            "ACTIONBAR",
            "SCOREBOARD",
            "TITLE"
    };

    private static final String[] POSITION_LABELS = {
            "Arriba, estilo bossbar",
            "Barra de acción",
            "Marcador lateral",
            "Centro, estilo título"
    };

    private static final int WHEEL_RADIUS = 48;
    private static final int WHEEL_CELL_SIZE = 3;
    private static final int WHEEL_CENTER_Y = 111;

    private EditBox dayInput;
    private EditBox monthInput;
    private EditBox yearInput;
    private EditBox hourInput;
    private EditBox minuteInput;
    private EditBox secondInput;
    private EditBox displayTextInput;
    private EditBox colorInput;

    private Component statusMessage = Component.empty();
    private int statusColor = 0xFFFFFF;
    private long selectedEndTimeMillis;
    private int selectedPosition;
    private int selectedWheelColor = 0xFFFFFF;
    private int wheelDraftColor = 0xFFFFFF;
    private boolean colorPickerOpen;

    public AdminPanelScreen() {
        super(Component.literal("Panel de administración"));
    }

    @Override
    protected void init() {
        int fieldWidth = 58;
        int fieldGap = 10;
        int rowWidth = fieldWidth * 3 + fieldGap * 2;
        int startX = (this.width - rowWidth) / 2;

        dayInput = createNumberInput(startX, 48, fieldWidth, 2);
        monthInput = createNumberInput(
                startX + fieldWidth + fieldGap, 48, fieldWidth, 2
        );
        yearInput = createNumberInput(
                startX + (fieldWidth + fieldGap) * 2, 48, fieldWidth, 4
        );

        hourInput = createNumberInput(startX, 88, fieldWidth, 2);
        minuteInput = createNumberInput(
                startX + fieldWidth + fieldGap, 88, fieldWidth, 2
        );
        secondInput = createNumberInput(
                startX + (fieldWidth + fieldGap) * 2, 88, fieldWidth, 2
        );

        this.addRenderableWidget(dayInput);
        this.addRenderableWidget(monthInput);
        this.addRenderableWidget(yearInput);
        this.addRenderableWidget(hourInput);
        this.addRenderableWidget(minuteInput);
        this.addRenderableWidget(secondInput);

        displayTextInput = new EditBox(
                this.font,
                this.width / 2 - 140,
                142,
                280,
                20,
                Component.literal("Texto bajo el contador")
        );
        displayTextInput.setMaxLength(100);
        this.addRenderableWidget(displayTextInput);

        colorInput = new EditBox(
                this.font,
                this.width / 2 - 45,
                177,
                90,
                20,
                Component.literal("Color hexadecimal")
        );
        colorInput.setMaxLength(6);
        colorInput.setValue("FFFFFF");
        colorInput.setFilter(text ->
                text.length() <= 6
                        && text.chars().allMatch(
                                character -> Character.digit(character, 16) >= 0
                        )
        );
        this.addRenderableWidget(colorInput);

        this.addRenderableWidget(
                Button.builder(
                                Component.literal("Elegir color"),
                                button -> openColorPicker()
                        )
                        .bounds(this.width / 2 + 52, 177, 90, 20)
                        .build()
        );

        this.addRenderableWidget(
                Button.builder(
                                Component.literal(getPositionLabel()),
                                button -> {
                                    selectedPosition =
                                            (selectedPosition + 1) % POSITIONS.length;
                                    button.setMessage(
                                            Component.literal(getPositionLabel())
                                    );
                                }
                        )
                        .bounds(this.width / 2 - 105, 199, 210, 20)
                        .build()
        );

        this.addRenderableWidget(
                Button.builder(
                                Component.literal("Aplicar"),
                                button -> startCountdown()
                        )
                        .bounds(this.width / 2 - 155, 220, 95, 20)
                        .build()
        );

        this.addRenderableWidget(
                Button.builder(
                                Component.literal("Quitar contador"),
                                button -> {
                                    CountdownNetwork.resetCountdown();
                                    setStatus(
                                            "Contador eliminado para todos.",
                                            0xFFAA55
                                    );
                                }
                        )
                        .bounds(this.width / 2 - 50, 220, 100, 20)
                        .build()
        );

        this.addRenderableWidget(
                Button.builder(
                                Component.literal("Cerrar"),
                                button -> this.onClose()
                        )
                        .bounds(this.width / 2 + 60, 220, 95, 20)
                        .build()
        );
    }

    private EditBox createNumberInput(
            int x,
            int y,
            int width,
            int maxLength
    ) {
        EditBox input = new EditBox(
                this.font,
                x,
                y,
                width,
                20,
                Component.empty()
        );

        input.setMaxLength(maxLength);
        input.setFilter(text ->
                text.isEmpty() || text.chars().allMatch(Character::isDigit)
        );

        return input;
    }

    private String getPositionLabel() {
        return "Posición: " + POSITION_LABELS[selectedPosition];
    }

    private void openColorPicker() {
        try {
            wheelDraftColor = Integer.parseInt(colorInput.getValue(), 16);
        } catch (NumberFormatException exception) {
            wheelDraftColor = selectedWheelColor;
        }

        colorPickerOpen = true;
    }

    private boolean updateColorFromWheel(double mouseX, double mouseY) {
        double dx = mouseX - this.width / 2.0;
        double dy = mouseY - WHEEL_CENTER_Y;
        double distance = Math.sqrt(dx * dx + dy * dy);

        if (distance > WHEEL_RADIUS) {
            return false;
        }

        float hue = (float) (
                (Math.atan2(dy, dx) / (Math.PI * 2.0) + 1.0) % 1.0
        );
        float saturation = (float) Math.min(1.0, distance / WHEEL_RADIUS);

        wheelDraftColor =
                java.awt.Color.HSBtoRGB(hue, saturation, 1.0F) & 0xFFFFFF;
        return true;
    }

    private boolean validateDate() {
        try {
            int day = Integer.parseInt(dayInput.getValue());
            int month = Integer.parseInt(monthInput.getValue());
            int year = Integer.parseInt(yearInput.getValue());
            int hour = Integer.parseInt(hourInput.getValue());
            int minute = Integer.parseInt(minuteInput.getValue());
            int second = Integer.parseInt(secondInput.getValue());

            LocalDateTime dateTime = LocalDateTime.of(
                    year, month, day, hour, minute, second
            );

            selectedEndTimeMillis = dateTime
                    .atZone(ZoneId.systemDefault())
                    .toInstant()
                    .toEpochMilli();

            if (selectedEndTimeMillis <= System.currentTimeMillis()) {
                setStatus("La fecha y hora deben ser futuras.", 0xFF5555);
                return false;
            }

            return true;
        } catch (NumberFormatException | DateTimeException exception) {
            setStatus(
                    "Introduce una fecha y hora válidas en todos los campos.",
                    0xFF5555
            );
            return false;
        }
    }

    private void startCountdown() {
        if (!validateDate()) {
            return;
        }

        String colorValue = colorInput.getValue().trim();

        if (colorValue.length() != 6) {
            setStatus(
                    "El color debe tener 6 caracteres hexadecimales.",
                    0xFF5555
            );
            return;
        }

        int displayColor;

        try {
            displayColor = Integer.parseInt(colorValue, 16);
        } catch (NumberFormatException exception) {
            setStatus("Color no válido. Ejemplo: 66CCFF", 0xFF5555);
            return;
        }

        CountdownNetwork.startCountdown(
                selectedEndTimeMillis,
                displayTextInput.getValue().trim(),
                displayColor,
                POSITIONS[selectedPosition]
        );

        setStatus("Ajustes enviados al servidor.", 0x55FF55);
    }

    private void setStatus(String message, int color) {
        statusMessage = Component.literal(message);
        statusColor = color;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (colorPickerOpen) {
            if (button == 0) {
                if (updateColorFromWheel(mouseX, mouseY)) {
                    return true;
                }

                int centerX = this.width / 2;
                if (mouseX >= centerX - 96
                        && mouseX <= centerX - 4
                        && mouseY >= 193
                        && mouseY <= 216) {
                    selectedWheelColor = wheelDraftColor;
                    colorInput.setValue(
                            String.format(Locale.ROOT, "%06X", wheelDraftColor)
                    );
                    colorPickerOpen = false;
                    return true;
                }
                if (mouseX >= centerX + 4
                        && mouseX <= centerX + 96
                        && mouseY >= 193
                        && mouseY <= 216) {
                    colorPickerOpen = false;
                    return true;
                }
            }

            return true;
        }

        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (colorPickerOpen && keyCode == 256) {
            colorPickerOpen = false;
            return true;
        }

        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    private void renderColorWheel(GuiGraphics graphics) {
        int centerX = this.width / 2;
        int centerY = WHEEL_CENTER_Y;

        int panelLeft = centerX - WHEEL_RADIUS - 22;
        int panelRight = centerX + WHEEL_RADIUS + 22;
        int panelTop = 12;
        int panelBottom = 228;
        graphics.fill(0, 0, this.width, this.height, 0xA0000000);
        graphics.fill(
                panelLeft - 2,
                panelTop - 2,
                panelRight + 2,
                panelBottom + 2,
                0xFF7A7A7A
        );
        graphics.fill(
                panelLeft,
                panelTop,
                panelRight,
                panelBottom,
                0xFF20242A
        );
        graphics.drawCenteredString(
                this.font, "Elige un color", centerX, 24, 0xFFFFFF
        );

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
                        centerY + y,
                        centerX + x + WHEEL_CELL_SIZE,
                        centerY + y + WHEEL_CELL_SIZE,
                        0xFF000000 | (color & 0xFFFFFF)
                );
            }
        }

        float[] hsv = java.awt.Color.RGBtoHSB(
                wheelDraftColor >>> 16 & 0xFF,
                wheelDraftColor >>> 8 & 0xFF,
                wheelDraftColor & 0xFF,
                null
        );

        double markerAngle = hsv[0] * Math.PI * 2.0;
        double markerRadius = hsv[1] * WHEEL_RADIUS;
        int markerX = centerX + (int) (Math.cos(markerAngle) * markerRadius);
        int markerY = centerY + (int) (Math.sin(markerAngle) * markerRadius);

        graphics.fill(markerX - 4, markerY - 1, markerX + 5, markerY + 2, 0xFF000000);
        graphics.fill(markerX - 1, markerY - 4, markerX + 2, markerY + 5, 0xFF000000);
        graphics.fill(markerX - 3, markerY, markerX + 4, markerY + 1, 0xFFFFFFFF);
        graphics.fill(markerX, markerY - 3, markerX + 1, markerY + 4, 0xFFFFFFFF);

        graphics.fill(
                centerX - 30, 169, centerX + 30, 188,
                0xFF000000 | wheelDraftColor
        );
        graphics.drawCenteredString(
                this.font,
                String.format(Locale.ROOT, "#%06X", wheelDraftColor),
                centerX,
                174,
                0xFFFFFFFF
        );

        graphics.fill(centerX - 96, 193, centerX - 4, 216, 0xFF397A45);
        graphics.drawCenteredString(
                this.font,
                "Aplicar",
                centerX - 50,
                200,
                0xFFFFFFFF
        );
        graphics.fill(centerX + 4, 193, centerX + 96, 216, 0xFF6B3B3B);
        graphics.drawCenteredString(
                this.font,
                "Cancelar",
                centerX + 50,
                200,
                0xFFFFFFFF
        );
    }

    @Override
    public void render(
            GuiGraphics graphics,
            int mouseX,
            int mouseY,
            float partialTick
    ) {
        this.renderBackground(graphics);
        NegativeUtilsGuiStyle.renderFrame(graphics, width, height);
        super.render(graphics, mouseX, mouseY, partialTick);

        graphics.drawCenteredString(
                this.font,
                "Panel de administración",
                this.width / 2,
                8,
                0xFFFFFF
        );

        graphics.drawCenteredString(
                this.font,
                statusMessage,
                this.width / 2,
                25,
                statusColor
        );

        int fieldWidth = 58;
        int fieldGap = 10;
        int rowWidth = fieldWidth * 3 + fieldGap * 2;
        int startX = (this.width - rowWidth) / 2;

        graphics.drawCenteredString(this.font, "Día", startX + fieldWidth / 2, 36, 0xFFFFFF);
        graphics.drawCenteredString(this.font, "Mes", startX + fieldWidth + fieldGap + fieldWidth / 2, 36, 0xFFFFFF);
        graphics.drawCenteredString(this.font, "Año", startX + (fieldWidth + fieldGap) * 2 + fieldWidth / 2, 36, 0xFFFFFF);

        graphics.drawCenteredString(this.font, "Hora", startX + fieldWidth / 2, 76, 0xFFFFFF);
        graphics.drawCenteredString(this.font, "Minuto", startX + fieldWidth + fieldGap + fieldWidth / 2, 76, 0xFFFFFF);
        graphics.drawCenteredString(this.font, "Segundo", startX + (fieldWidth + fieldGap) * 2 + fieldWidth / 2, 76, 0xFFFFFF);

        graphics.drawCenteredString(
                this.font,
                "Texto que aparecerá debajo del contador",
                this.width / 2,
                130,
                0xFFFFFF
        );

        graphics.drawCenteredString(
                this.font,
                "Color hexadecimal",
                this.width / 2,
                165,
                0xFFFFFF
        );

        if (colorPickerOpen) {
            renderColorWheel(graphics);
        }
    }

    public long getSelectedEndTimeMillis() {
        return selectedEndTimeMillis;
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}