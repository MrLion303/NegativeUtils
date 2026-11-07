package com.negative.negativeutils;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.CommandSuggestions;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;

public class CommandSequenceScreen extends Screen {
    private static final int LINE_HEIGHT = 12;
    private static final int GUTTER_WIDTH = 34;

    private final BlockPos blockPos;
    private final boolean communicator;
    private final List<String> lines = new ArrayList<>();
    private final List<Integer> cursorPositions = new ArrayList<>();
    private final List<CommandLineEditBox> visibleEditors = new ArrayList<>();

    private String currentCommands;
    private CommandLineEditBox activeEditor;
    private CommandSuggestions suggestions;

    private int editorX;
    private int editorY;
    private int editorWidth;
    private int editorHeight;
    private int firstVisibleLine;
    private int activeLineIndex = -1;

    private boolean suppressLineChanged;
    private String lastSuggestionValue;
    private int lastSuggestionCursor = -1;

    public CommandSequenceScreen(BlockPos blockPos, String commands) {
        this(blockPos, commands, false);
    }

    public CommandSequenceScreen(BlockPos blockPos, String commands, boolean communicator) {
        super(Component.literal(communicator ? "Editor del comunicador" : "Editor de secuencias"));
        this.blockPos = blockPos;
        this.communicator = communicator;
        this.currentCommands = normalize(commands);

        for (String line : currentCommands.split("\n", -1)) {
            lines.add(line);
            cursorPositions.add(line.length());
        }
        if (lines.isEmpty()) {
            lines.add("");
            cursorPositions.add(0);
        }
        activeLineIndex = 0;
    }

    private static String normalize(String value) {
        if (value == null) {
            return "";
        }
        return value.replace("\r\n", "\n").replace('\r', '\n');
    }

    @Override
    protected void init() {
        int panelWidth = Math.min(560, this.width - 32);
        editorX = (this.width - panelWidth) / 2;
        editorY = 78;
        editorWidth = panelWidth;
        editorHeight = Math.max(80, this.height - 144);

        visibleEditors.clear();
        int visibleRows = Math.max(1, editorHeight / LINE_HEIGHT);
        int inputX = editorX + GUTTER_WIDTH + 6;
        int inputWidth = Math.max(40, editorWidth - GUTTER_WIDTH - 12);

        for (int row = 0; row < visibleRows; row++) {
            CommandLineEditBox input = new CommandLineEditBox(
                    this.font,
                    inputX,
                    editorY + row * LINE_HEIGHT + 1,
                    inputWidth,
                    LINE_HEIGHT,
                    this
            );
            input.setMaxLength(CommandSequenceBlockEntity.MAX_COMMANDS_LENGTH);
            input.setBordered(false);
            input.setTextColor(0xFFE6EDF3);
            input.setTextColorUneditable(0xFFE6EDF3);
            visibleEditors.add(this.addRenderableWidget(input));
        }

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

        if (lines.isEmpty()) {
            lines.add("");
            cursorPositions.add(0);
        }
        activeLineIndex = Math.max(0, Math.min(activeLineIndex, lines.size() - 1));
        bindVisibleEditors(activeLineIndex, cursorPositions.get(activeLineIndex), true);
    }

    private void bindVisibleEditors(int requestedLine, int requestedCursor, boolean forceFocus) {
        boolean hadFocus = activeEditor != null && activeEditor.isFocused();
        saveActiveCursor();

        if (requestedLine >= 0) {
            activeLineIndex = Math.max(0, Math.min(requestedLine, lines.size() - 1));
            if (requestedCursor >= 0) {
                cursorPositions.set(
                        activeLineIndex,
                        Math.max(0, Math.min(requestedCursor, lines.get(activeLineIndex).length()))
                );
            }
        }

        int maxFirstLine = Math.max(0, lines.size() - visibleEditors.size());
        if (forceFocus) {
            if (activeLineIndex < firstVisibleLine) {
                firstVisibleLine = activeLineIndex;
            } else if (activeLineIndex >= firstVisibleLine + visibleEditors.size()) {
                firstVisibleLine = activeLineIndex - visibleEditors.size() + 1;
            }
        }
        firstVisibleLine = Math.max(0, Math.min(firstVisibleLine, maxFirstLine));

        if (activeEditor != null) {
            activeEditor.setFocused(false);
        }
        if (suggestions != null) {
            suggestions.hide();
            suggestions = null;
        }
        activeEditor = null;

        suppressLineChanged = true;
        CommandLineEditBox target = null;
        for (int row = 0; row < visibleEditors.size(); row++) {
            CommandLineEditBox input = visibleEditors.get(row);
            int lineIndex = firstVisibleLine + row;
            input.setY(editorY + row * LINE_HEIGHT + 1);

            if (lineIndex < lines.size()) {
                input.bindLine(lineIndex, lines.get(lineIndex));
                input.setVisible(true);
                int cursor = cursorPositions.get(lineIndex);
                input.setCursorPosition(Math.max(0, Math.min(cursor, lines.get(lineIndex).length())));
                if (lineIndex == activeLineIndex) {
                    target = input;
                }
            } else {
                input.bindLine(-1, "");
                input.setVisible(false);
            }
        }
        suppressLineChanged = false;

        if (target != null) {
            activeEditor = target;
            if (forceFocus || hadFocus) {
                this.setFocused(target);
                refreshSuggestions(target);
            }
        }
    }

    private void saveActiveCursor() {
        if (activeEditor == null || activeLineIndex < 0 || activeLineIndex >= cursorPositions.size()) {
            return;
        }
        cursorPositions.set(
                activeLineIndex,
                Math.max(0, Math.min(activeEditor.getCursorPosition(), lines.get(activeLineIndex).length()))
        );
    }

    private void onLineChanged(CommandLineEditBox editor, String value) {
        if (suppressLineChanged) {
            return;
        }

        int lineIndex = editor.getLineIndex();
        if (lineIndex < 0 || lineIndex >= lines.size()) {
            return;
        }

        String previous = lines.get(lineIndex);
        lines.set(lineIndex, value);
        String updated = String.join("\n", lines);

        if (updated.length() > CommandSequenceBlockEntity.MAX_COMMANDS_LENGTH) {
            lines.set(lineIndex, previous);
            int cursor = editor.getCursorPosition();
            suppressLineChanged = true;
            editor.setValue(previous);
            editor.setCursorPosition(Math.min(cursor, previous.length()));
            suppressLineChanged = false;
            return;
        }

        currentCommands = updated;
        cursorPositions.set(lineIndex, Math.min(editor.getCursorPosition(), value.length()));
        updateSuggestionsIfNeeded();
    }

    private void onEditorFocused(CommandLineEditBox editor) {
        int lineIndex = editor.getLineIndex();
        if (lineIndex < 0 || lineIndex >= lines.size()) {
            return;
        }
        if (activeEditor == editor && activeLineIndex == lineIndex) {
            return;
        }

        saveActiveCursor();
        activeEditor = editor;
        activeLineIndex = lineIndex;
        cursorPositions.set(lineIndex, Math.min(editor.getCursorPosition(), lines.get(lineIndex).length()));
        refreshSuggestions(editor);
    }

    private void refreshSuggestions(CommandLineEditBox editor) {
        if (suggestions != null) {
            suggestions.hide();
        }
        if (editor == null || this.minecraft == null) {
            suggestions = null;
            return;
        }

        suggestions = new CommandSuggestions(
                this.minecraft,
                this,
                editor,
                this.font,
                true,
                false,
                0,
                7,
                false,
                0xF0100010
        );
        suggestions.setAllowSuggestions(true);
        lastSuggestionValue = null;
        lastSuggestionCursor = -1;
        updateSuggestionsIfNeeded();
    }

    private void updateSuggestionsIfNeeded() {
        if (suggestions == null || activeEditor == null || activeEditor.getLineIndex() < 0) {
            return;
        }

        String value = activeEditor.getValue();
        int cursor = activeEditor.getCursorPosition();
        if (Objects.equals(value, lastSuggestionValue) && cursor == lastSuggestionCursor) {
            return;
        }

        lastSuggestionValue = value;
        lastSuggestionCursor = cursor;
        suggestions.setAllowSuggestions(true);
        suggestions.updateCommandInfo();

        // "wait" es una instrucción propia de las secuencias, no un comando vanilla.
        String candidate = value.startsWith("/") ? value.substring(1) : value;
        if (cursor == value.length() && candidate.indexOf(' ') < 0) {
            String lower = candidate.toLowerCase(Locale.ROOT);
            if ("wait".startsWith(lower) && !lower.isEmpty()) {
                activeEditor.setSuggestion("wait".substring(lower.length()) + " ");
            } else if (lower.equals("wait")) {
                activeEditor.setSuggestion(" ");
            }
        } else if (candidate.equalsIgnoreCase("wait ")) {
            activeEditor.setSuggestion("segundos");
        }
    }

    private boolean completeWaitDirective() {
        if (activeEditor == null) {
            return false;
        }

        String value = activeEditor.getValue();
        if (activeEditor.getCursorPosition() != value.length()) {
            return false;
        }

        boolean slash = value.startsWith("/");
        String candidate = slash ? value.substring(1) : value;
        if (!candidate.equalsIgnoreCase("wa")
                && !candidate.equalsIgnoreCase("wai")
                && !candidate.equalsIgnoreCase("wait")) {
            return false;
        }

        String completed = (slash ? "/" : "") + "wait ";
        activeEditor.setValue(completed);
        activeEditor.setCursorPosition(completed.length());
        return true;
    }

    private void insertLineBreak() {
        if (activeEditor == null || activeLineIndex < 0 || activeLineIndex >= lines.size()) {
            return;
        }

        String value = activeEditor.getValue();
        int cursor = Math.max(0, Math.min(activeEditor.getCursorPosition(), value.length()));
        String before = value.substring(0, cursor);
        String after = value.substring(cursor);

        lines.set(activeLineIndex, before);
        lines.add(activeLineIndex + 1, after);
        cursorPositions.set(activeLineIndex, before.length());
        cursorPositions.add(activeLineIndex + 1, 0);
        currentCommands = String.join("\n", lines);

        bindVisibleEditors(activeLineIndex + 1, 0, true);
    }

    private void mergeLineBackward() {
        if (activeEditor == null || activeLineIndex <= 0
                || activeEditor.getCursorPosition() != 0
                || !activeEditor.getHighlighted().isEmpty()) {
            return;
        }

        int previousIndex = activeLineIndex - 1;
        String previous = lines.get(previousIndex);
        String current = activeEditor.getValue();
        int cursor = previous.length();

        lines.set(previousIndex, previous + current);
        lines.remove(activeLineIndex);
        cursorPositions.set(previousIndex, cursor);
        cursorPositions.remove(activeLineIndex);
        currentCommands = String.join("\n", lines);

        bindVisibleEditors(previousIndex, cursor, true);
    }

    private void mergeLineForward() {
        if (activeEditor == null || activeLineIndex < 0
                || activeLineIndex >= lines.size() - 1
                || activeEditor.getCursorPosition() != activeEditor.getValue().length()
                || !activeEditor.getHighlighted().isEmpty()) {
            return;
        }

        int currentIndex = activeLineIndex;
        String current = activeEditor.getValue();
        String next = lines.get(currentIndex + 1);
        int cursor = current.length();

        lines.set(currentIndex, current + next);
        lines.remove(currentIndex + 1);
        cursorPositions.set(currentIndex, cursor);
        cursorPositions.remove(currentIndex + 1);
        currentCommands = String.join("\n", lines);

        bindVisibleEditors(currentIndex, cursor, true);
    }

    private void focusAdjacentLine(int offset) {
        if (activeEditor == null || !activeEditor.getHighlighted().isEmpty()) {
            return;
        }

        int targetLine = activeLineIndex + offset;
        if (targetLine < 0 || targetLine >= lines.size()) {
            return;
        }

        int cursor = Math.min(activeEditor.getCursorPosition(), lines.get(targetLine).length());
        bindVisibleEditors(targetLine, cursor, true);
    }

    private void addBlankLinesThrough(int lineIndex) {
        while (lines.size() <= lineIndex
                && String.join("\n", lines).length()
                < CommandSequenceBlockEntity.MAX_COMMANDS_LENGTH) {
            lines.add("");
            cursorPositions.add(0);
        }
        currentCommands = String.join("\n", lines);
    }

    private void saveCommands() {
        currentCommands = String.join("\n", lines);
        if (communicator) {
            CommunicatorNetwork.saveCommands(blockPos, currentCommands);
        } else {
            CommandSequenceNetwork.saveCommands(blockPos, currentCommands);
        }
        onClose();
    }

    @Override
    public void tick() {
        super.tick();
        for (CommandLineEditBox editor : visibleEditors) {
            if (editor.visible) {
                editor.tick();
            }
        }
        saveActiveCursor();
        updateSuggestionsIfNeeded();
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == GLFW.GLFW_KEY_ENTER && hasControlDown()) {
            saveCommands();
            return true;
        }

        if (suggestions != null && suggestions.keyPressed(keyCode, scanCode, modifiers)) {
            updateSuggestionsIfNeeded();
            return true;
        }

        if (activeEditor != null) {
            if (keyCode == GLFW.GLFW_KEY_TAB && completeWaitDirective()) {
                return true;
            }
            if (keyCode == GLFW.GLFW_KEY_ENTER) {
                insertLineBreak();
                return true;
            }
            if (keyCode == GLFW.GLFW_KEY_UP) {
                focusAdjacentLine(-1);
                return true;
            }
            if (keyCode == GLFW.GLFW_KEY_DOWN) {
                focusAdjacentLine(1);
                return true;
            }
            if (keyCode == GLFW.GLFW_KEY_BACKSPACE) {
                int cursor = activeEditor.getCursorPosition();
                if (cursor == 0 && activeEditor.getHighlighted().isEmpty()) {
                    mergeLineBackward();
                    return true;
                }
            }
            if (keyCode == GLFW.GLFW_KEY_DELETE) {
                if (activeEditor.getCursorPosition() == activeEditor.getValue().length()
                        && activeEditor.getHighlighted().isEmpty()) {
                    mergeLineForward();
                    return true;
                }
            }
        }

        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (suggestions != null && suggestions.mouseClicked(mouseX, mouseY, button)) {
            updateSuggestionsIfNeeded();
            return true;
        }

        boolean insideEditor = mouseX >= editorX && mouseX <= editorX + editorWidth
                && mouseY >= editorY && mouseY <= editorY + editorHeight;
        if (button == 0 && insideEditor) {
            int row = Math.max(0, Math.min(
                    visibleEditors.size() - 1,
                    (int) ((mouseY - editorY) / LINE_HEIGHT)
            ));
            int requestedLine = firstVisibleLine + row;

            if (mouseX < editorX + GUTTER_WIDTH || requestedLine >= lines.size()) {
                addBlankLinesThrough(requestedLine);
                bindVisibleEditors(requestedLine, 0, true);
                return true;
            }
        }

        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        if (suggestions != null && suggestions.mouseScrolled(delta)) {
            return true;
        }

        boolean insideEditor = mouseX >= editorX && mouseX <= editorX + editorWidth
                && mouseY >= editorY && mouseY <= editorY + editorHeight;
        if (insideEditor && lines.size() > visibleEditors.size()) {
            int maxFirstLine = Math.max(0, lines.size() - visibleEditors.size());
            int movement = delta > 0 ? -3 : 3;
            int nextFirstLine = Math.max(0, Math.min(maxFirstLine, firstVisibleLine + movement));
            if (nextFirstLine != firstVisibleLine) {
                firstVisibleLine = nextFirstLine;
                bindVisibleEditors(-1, -1, false);
            }
            return true;
        }

        return super.mouseScrolled(mouseX, mouseY, delta);
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
                communicator ? "Comunicador" : "Bloque de secuencias",
                this.width / 2,
                24,
                0xFFFFFF
        );
        graphics.drawCenteredString(
                this.font,
                communicator
                        ? "Dale clic derecho normal para ejecutar la secuencia."
                        : "Dale energía de redstone para ejecutar la secuencia.",
                this.width / 2,
                42,
                0xBBBBBB
        );
        graphics.drawCenteredString(
                this.font,
                "wait <segundos> pausa; las líneas restantes se ejecutan al terminar",
                this.width / 2,
                58,
                0xBBBBBB
        );
        graphics.drawCenteredString(
                this.font,
                "Escribe comandos normales. Usa Tab para completar sugerencias.",
                this.width / 2,
                70,
                0xBBBBBB
        );

        graphics.fill(
                editorX,
                editorY,
                editorX + editorWidth,
                editorY + editorHeight,
                0xFF111318
        );
        graphics.fill(
                editorX,
                editorY,
                editorX + GUTTER_WIDTH,
                editorY + editorHeight,
                0xFF202124
        );
        graphics.fill(
                editorX + GUTTER_WIDTH - 1,
                editorY,
                editorX + GUTTER_WIDTH,
                editorY + editorHeight,
                0xFF444444
        );

        for (int row = 0; row < visibleEditors.size(); row++) {
            int lineIndex = firstVisibleLine + row;
            if (lineIndex >= lines.size()) {
                break;
            }

            int y = editorY + row * LINE_HEIGHT + 2;
            if (lineIndex == activeLineIndex) {
                graphics.fill(
                        editorX + GUTTER_WIDTH,
                        editorY + row * LINE_HEIGHT,
                        editorX + editorWidth,
                        editorY + (row + 1) * LINE_HEIGHT,
                        0xFF1B2633
                );
            }
            graphics.drawString(
                    this.font,
                    Integer.toString(lineIndex + 1),
                    editorX + 8,
                    y,
                    0xFF718096,
                    false
            );
        }

        super.render(graphics, mouseX, mouseY, partialTick);
        if (suggestions != null) {
            suggestions.render(graphics, mouseX, mouseY);
        }
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    private static final class CommandLineEditBox extends EditBox {
        private final CommandSequenceScreen owner;
        private int lineIndex = -1;

        private CommandLineEditBox(
                net.minecraft.client.gui.Font font,
                int x,
                int y,
                int width,
                int height,
                CommandSequenceScreen owner
        ) {
            super(font, x, y, width, height, Component.empty());
            this.owner = owner;
            setResponder(value -> owner.onLineChanged(this, value));
        }

        private void bindLine(int lineIndex, String value) {
            this.lineIndex = lineIndex;
            setValue(value);
        }

        private int getLineIndex() {
            return lineIndex;
        }

        @Override
        public void onClick(double mouseX, double mouseY) {
            super.onClick(mouseX, mouseY);
            owner.onEditorFocused(this);
        }

        @Override
        public void setFocused(boolean focused) {
            super.setFocused(focused);
            if (focused) {
                owner.onEditorFocused(this);
            }
        }
    }
}
