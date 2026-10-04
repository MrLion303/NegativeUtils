package com.negative.negativeutils;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.MultiLineEditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;

public class CommandSequenceScreen extends Screen {
    private final BlockPos blockPos;
    private String currentCommands;
    private CodeEditor editor;
    private int editorX;
    private int editorY;
    private int editorHeight;

    public CommandSequenceScreen(BlockPos blockPos, String commands) {
        super(Component.literal("Editor de secuencias"));
        this.blockPos = blockPos;
        this.currentCommands = commands;
    }

    @Override
    protected void init() {
        int panelWidth = Math.min(560, this.width - 32);
        editorX = (this.width - panelWidth) / 2;
        editorY = 78;
        editorHeight = Math.max(80, this.height - 144);
        int gutterWidth = 34;

        editor = new CodeEditor(
                this.font,
                editorX + gutterWidth,
                editorY,
                panelWidth - gutterWidth,
                editorHeight,
                Component.literal("Comandos"),
                Component.literal(
                        "say \"Hola\"\n"
                                + "wait 2\n"
                                + "say \"Han pasado dos segundos\""
                )
        );
        editor.setCharacterLimit(
                CommandSequenceBlockEntity.MAX_COMMANDS_LENGTH
        );
        editor.setValue(currentCommands);
        editor.setValueListener(value -> currentCommands = value);
        this.addRenderableWidget(editor);

        this.addRenderableWidget(
                Button.builder(
                                Component.literal("Guardar"),
                                button -> saveCommands()
                        )
                        .bounds(this.width / 2 - 105, this.height - 48, 100, 20)
                        .build()
        );
        this.addRenderableWidget(
                Button.builder(
                                Component.literal("Cancelar"),
                                button -> this.onClose()
                        )
                        .bounds(this.width / 2 + 5, this.height - 48, 100, 20)
                        .build()
        );
        this.setInitialFocus(editor);
    }

    private void saveCommands() {
        CommandSequenceNetwork.saveCommands(blockPos, editor.getValue());
        onClose();
    }

    @Override
    public void tick() {
        super.tick();
        if (editor != null) {
            editor.tick();
        }
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
        graphics.drawCenteredString(
                this.font,
                "Bloque de secuencias",
                this.width / 2,
                24,
                0xFFFFFF
        );
        graphics.drawCenteredString(
                this.font,
                "Dale energía de redstone para ejecutar la secuencia.",
                this.width / 2,
                42,
                0xBBBBBB
        );
        graphics.drawCenteredString(
                this.font,
                "wait <segundos> pausa; las líneas sin wait se ejecutan a la vez",
                this.width / 2,
                58,
                0xBBBBBB
        );
        graphics.drawCenteredString(
                this.font,
                "Usa comandos normales y wait <segundos> para pausas.",
                this.width / 2,
                70,
                0xBBBBBB
        );

        graphics.fill(
                editorX,
                editorY,
                editorX + 34,
                editorY + editorHeight,
                0xFF202124
        );
        graphics.fill(
                editorX + 33,
                editorY,
                editorX + 34,
                editorY + editorHeight,
                0xFF444444
        );
        renderLineNumbers(graphics);
        super.render(graphics, mouseX, mouseY, partialTick);
    }

    private void renderLineNumbers(GuiGraphics graphics) {
        if (editor == null) {
            return;
        }
        String[] lines = editor.getValue().split("\\n", -1);
        int firstVisibleLine = (int) (editor.scrollOffset() / this.font.lineHeight);
        int visibleRows = editorHeight / this.font.lineHeight + 1;
        for (int row = 0; row < visibleRows; row++) {
            int lineNumber = firstVisibleLine + row;
            if (lineNumber >= lines.length) {
                break;
            }
            int y = editorY + 4 + row * this.font.lineHeight;
            graphics.drawString(
                    this.font,
                    Integer.toString(lineNumber + 1),
                    editorX + 8,
                    y,
                    0xFF718096,
                    false
            );
        }
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == 257 && hasControlDown()) {
            saveCommands();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    private static final class CodeEditor extends MultiLineEditBox {
        private CodeEditor(
                net.minecraft.client.gui.Font font,
                int x,
                int y,
                int width,
                int height,
                Component title,
                Component placeholder
        ) {
            super(font, x, y, width, height, title, placeholder);
        }

        private double scrollOffset() {
            return scrollAmount();
        }
    }
}
