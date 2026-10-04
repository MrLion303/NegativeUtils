package com.negative.negativeutils;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.network.chat.Style;

public final class DiscordTokenScreen extends Screen {
    private EditBox tokenInput;

    public DiscordTokenScreen() {
        super(Component.literal("Configurar bot de Discord"));
    }

    @Override
    protected void init() {
        tokenInput = new EditBox(
                font,
                width / 2 - 140,
                height / 2 - 4,
                280,
                20,
                Component.literal("Token del bot")
        );
        tokenInput.setMaxLength(256);
        tokenInput.setFormatter((value, cursor) ->
                FormattedCharSequence.forward(
                        "*".repeat(value.length()),
                        Style.EMPTY
                )
        );
        addRenderableWidget(tokenInput);
        addRenderableWidget(
                Button.builder(
                                Component.literal("Guardar"),
                                button -> {
                                    DiscordEmoteNetwork.setToken(
                                            tokenInput.getValue()
                                    );
                                    onClose();
                                }
                        )
                        .bounds(width / 2 - 105, height / 2 + 28, 100, 20)
                        .build()
        );
        addRenderableWidget(
                Button.builder(
                                Component.literal("Cancelar"),
                                button -> onClose()
                        )
                        .bounds(width / 2 + 5, height / 2 + 28, 100, 20)
                        .build()
        );
        setInitialFocus(tokenInput);
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
        graphics.drawCenteredString(
                font,
                "Pega aquí el token del bot de Discord.",
                width / 2,
                height / 2 - 36,
                0xFFFFFF
        );
        graphics.drawCenteredString(
                font,
                "Solo se envía al servidor y se guarda en su carpeta config.",
                width / 2,
                height / 2 - 22,
                0xFFAA55
        );
        super.render(graphics, mouseX, mouseY, partialTick);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
