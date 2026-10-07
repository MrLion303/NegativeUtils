package com.negative.negativeutils;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;

public class TimeDelayScreen extends Screen {
    private final BlockPos blockPos;
    private final int currentDelaySeconds;

    private EditBox secondsInput;
    private Component status = Component.empty();
    private int statusColor = 0xFFFFFF;

    public TimeDelayScreen(BlockPos blockPos, int currentDelaySeconds) {
        super(Component.literal("Configurar bloque de tiempo"));
        this.blockPos = blockPos;
        this.currentDelaySeconds = currentDelaySeconds;
    }

    @Override
    protected void init() {
        secondsInput = new EditBox(
                this.font,
                this.width / 2 - 60,
                this.height / 2 - 20,
                120,
                20,
                Component.literal("Segundos")
        );

        secondsInput.setMaxLength(5);
        NegativeUtilsGuiStyle.styleField(secondsInput);
        secondsInput.setValue(Integer.toString(currentDelaySeconds));
        secondsInput.setFilter(text ->
                text.isEmpty() || text.chars().allMatch(Character::isDigit)
        );

        this.addRenderableWidget(secondsInput);

        this.addRenderableWidget(
                Button.builder(
                                Component.literal("Guardar"),
                                button -> saveDelay()
                        )
                        .bounds(this.width / 2 - 105, this.height / 2 + 20, 100, 20)
                        .build()
        );

        this.addRenderableWidget(
                Button.builder(
                                Component.literal("Cerrar"),
                                button -> this.onClose()
                        )
                        .bounds(this.width / 2 + 5, this.height / 2 + 20, 100, 20)
                        .build()
        );

        this.setInitialFocus(secondsInput);
    }

    private void saveDelay() {
        try {
            int seconds = Integer.parseInt(secondsInput.getValue());

            if (seconds < 1 || seconds > 86400) {
                setStatus("Elige entre 1 y 86400 segundos.", 0xFF5555);
                return;
            }

            TimeDelayNetwork.setDelay(blockPos, seconds);
            setStatus("Se envió la demora al servidor.", 0x55FF55);
        } catch (NumberFormatException exception) {
            setStatus("Escribe una cantidad válida de segundos.", 0xFF5555);
        }
    }

    private void setStatus(String text, int color) {
        status = Component.literal(text);
        statusColor = color;
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
                "Bloque de tiempo",
                this.width / 2,
                this.height / 2 - 65,
                0xFFFFFF
        );

        graphics.drawCenteredString(
                this.font,
                "Demora en segundos (1-86400)",
                this.width / 2,
                this.height / 2 - 40,
                0xAAAAAA
        );

        graphics.drawCenteredString(
                this.font,
                status,
                this.width / 2,
                this.height / 2 + 50,
                statusColor
        );
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}