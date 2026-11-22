package com.negative.negativeutils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.stream.Stream;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/**
 * Explorador de archivos integrado en Minecraft para importar secuencias .txt.
 * No utiliza AWT/Swing ni abre ventanas nativas externas.
 */
public final class TextFilePickerScreen extends Screen {
    private static final int ROW_HEIGHT = 24;

    private final CommandSequenceScreen parent;
    private Path currentDirectory;
    private final List<Path> entries = new ArrayList<>();
    private int scroll;
    private String status = "";

    public TextFilePickerScreen(CommandSequenceScreen parent) {
        super(Component.literal("Importar archivo de texto"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        Path home;
        try {
            home = Path.of(System.getProperty("user.home", ".")).toAbsolutePath().normalize();
        } catch (Exception exception) {
            home = minecraft.gameDirectory.toPath().toAbsolutePath().normalize();
        }

        if (!Files.isDirectory(home)) {
            home = minecraft.gameDirectory.toPath().toAbsolutePath().normalize();
        }

        currentDirectory = home;
        loadEntries();
        rebuildWidgets();
    }

    private int panelWidth() {
        return Math.min(640, Math.max(360, width - 24));
    }

    private int panelLeft() {
        return (width - panelWidth()) / 2;
    }

    private int listTop() {
        return 76;
    }

    private int visibleRows() {
        return Math.max(1, (height - 144) / ROW_HEIGHT);
    }

    private void loadEntries() {
        entries.clear();
        if (currentDirectory == null || !Files.isDirectory(currentDirectory)) {
            status = "La carpeta seleccionada no está disponible.";
            return;
        }

        try (Stream<Path> children = Files.list(currentDirectory)) {
            children
                    .filter(path -> Files.isDirectory(path) || isTextFile(path))
                    .sorted(Comparator
                            .comparing((Path path) -> !Files.isDirectory(path))
                            .thenComparing(path -> path.getFileName().toString(),
                                    String.CASE_INSENSITIVE_ORDER))
                    .forEach(entries::add);

            status = entries.isEmpty() ? "Esta carpeta no contiene archivos .txt ni subcarpetas." : "";
        } catch (IOException | SecurityException exception) {
            status = "No se puede leer esta carpeta.";
        }

        int maxScroll = Math.max(0, entries.size() - visibleRows());
        scroll = Math.max(0, Math.min(scroll, maxScroll));
    }

    private boolean isTextFile(Path path) {
        Path name = path.getFileName();
        return name != null && name.toString().toLowerCase(Locale.ROOT).endsWith(".txt");
    }

    private void rebuildWidgets() {
        clearWidgets();

        int left = panelLeft() + 12;
        int rowWidth = panelWidth() - 24;
        int rows = visibleRows();
        int maxScroll = Math.max(0, entries.size() - rows);
        scroll = Math.max(0, Math.min(scroll, maxScroll));

        for (int row = 0; row < rows; row++) {
            int index = scroll + row;
            if (index >= entries.size()) {
                break;
            }

            Path entry = entries.get(index);
            boolean directory = Files.isDirectory(entry);
            String prefix = directory ? "[Carpeta]  " : "[TXT]  ";
            String label = trimToWidth(prefix + entry.getFileName(), rowWidth - 14);

            addRenderableWidget(Button.builder(Component.literal(label), button -> selectEntry(entry))
                    .bounds(left, listTop() + row * ROW_HEIGHT, rowWidth, 20)
                    .build());
        }

        Path parentDirectory = currentDirectory == null ? null : currentDirectory.getParent();
        Button upButton = Button.builder(Component.literal("Carpeta superior"), button -> goToParent())
                .bounds(width / 2 - 104, height - 38, 126, 20)
                .build();
        upButton.active = parentDirectory != null;
        addRenderableWidget(upButton);

        addRenderableWidget(Button.builder(Component.literal("Cancelar"), button -> returnToEditor())
                .bounds(width / 2 + 28, height - 38, 76, 20)
                .build());
    }

    private void selectEntry(Path entry) {
        if (Files.isDirectory(entry)) {
            currentDirectory = entry.toAbsolutePath().normalize();
            scroll = 0;
            loadEntries();
            rebuildWidgets();
            return;
        }

        if (!isTextFile(entry) || !Files.isRegularFile(entry)) {
            status = "Selecciona un archivo .txt válido.";
            return;
        }

        returnToEditor();
        parent.loadImportedTextFile(entry);
    }

    private void goToParent() {
        if (currentDirectory == null || currentDirectory.getParent() == null) {
            return;
        }

        currentDirectory = currentDirectory.getParent();
        scroll = 0;
        loadEntries();
        rebuildWidgets();
    }

    private void returnToEditor() {
        if (minecraft != null) {
            minecraft.setScreen(parent);
        }
    }

    private String trimToWidth(String text, int maxWidth) {
        if (font.width(text) <= maxWidth) {
            return text;
        }

        String suffix = "...";
        int limit = Math.max(0, maxWidth - font.width(suffix));
        while (!text.isEmpty() && font.width(text) > limit) {
            text = text.substring(0, text.length() - 1);
        }
        return text + suffix;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        if (mouseY >= listTop() && mouseY <= height - 48 && entries.size() > visibleRows()) {
            int maxScroll = Math.max(0, entries.size() - visibleRows());
            int movement = delta > 0 ? -2 : 2;
            int nextScroll = Math.max(0, Math.min(maxScroll, scroll + movement));
            if (nextScroll != scroll) {
                scroll = nextScroll;
                rebuildWidgets();
            }
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, delta);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics);
        NegativeUtilsGuiStyle.renderFrame(graphics, width, height);

        int left = panelLeft();
        int right = left + panelWidth();
        graphics.fill(left, 12, right, height - 12, 0xEC141B24);
        graphics.fill(left, 12, right, 14, 0xFF54D6FF);
        graphics.drawCenteredString(font, title, width / 2, 24, 0xFFFFFFFF);
        graphics.drawString(font, "Carpeta:", left + 12, 43, 0xFF9FAAB8);
        graphics.drawString(font, trimToWidth(currentDirectory == null
                ? "" : currentDirectory.toString(), panelWidth() - 76),
                left + 54, 43, 0xFFEAF7FF);
        graphics.drawCenteredString(font, "Selecciona una carpeta o un archivo .txt",
                width / 2, 61, 0xFF9FAAB8);

        if (entries.isEmpty()) {
            graphics.drawCenteredString(font, status.isEmpty() ? "Carpeta vacía" : status,
                    width / 2, listTop() + 12, 0xFFFFCC66);
        } else if (!status.isEmpty()) {
            graphics.drawCenteredString(font, status, width / 2, height - 54, 0xFFFFCC66);
        }

        super.render(graphics, mouseX, mouseY, partialTick);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
