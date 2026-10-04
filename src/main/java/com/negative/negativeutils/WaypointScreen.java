package com.negative.negativeutils;

import java.util.List;
import java.util.Locale;
import java.util.UUID;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public class WaypointScreen extends Screen {
    private static final int WHEEL_RADIUS = 48;
    private static final int WHEEL_CENTER_Y = 106;
    private static final List<String> ICONS = List.of("◆", "★", "●", "♥", "♣", "♠", "✦", "✚", "▲", "■", "☀", "☠", "✿", "⚑");
    private static final String[] CORNERS = {"TOP_LEFT", "TOP_RIGHT", "BOTTOM_LEFT", "BOTTOM_RIGHT"};
    private static final String[] CORNER_LABELS = {"Esquina: arriba izquierda", "Esquina: arriba derecha",
            "Esquina: abajo izquierda", "Esquina: abajo derecha"};

    private final UUID id;
    private final String initialCommandId, initialName, initialDimension, initialIcon, initialCorner;
    private final double initialX, initialY, initialZ;
    private EditBox commandIdInput, nameInput, xInput, yInput, zInput, dimensionInput, colorInput;
    private int color, draftColor;
    private String selectedIcon, selectedCorner;
    private boolean colorPickerOpen;

    public WaypointScreen(UUID id, String commandId, String name, String dimension,
                          double x, double y, double z, int color, String icon, String corner) {
        super(Component.literal(id == null ? "Crear waypoint" : "Editar waypoint"));
        this.id = id;
        this.initialCommandId = commandId;
        this.initialName = name;
        this.initialDimension = dimension;
        this.initialX = x; this.initialY = y; this.initialZ = z;
        this.color = color & 0xFFFFFF;
        this.selectedIcon = WaypointSavedData.sanitizeIcon(icon);
        this.initialIcon = this.selectedIcon;
        this.selectedCorner = WaypointSavedData.sanitizeCorner(corner);
        this.initialCorner = this.selectedCorner;
        this.draftColor = this.color;
    }

    @Override
    protected void init() {
        int cx = width / 2;
        int left = cx - 100;
        commandIdInput = field(left, 39, 200, "Identificador (ej. waypoint_1)", 48, initialCommandId);
        nameInput = field(left, 65, 200, "Nombre visible", 32, initialName);
        xInput = field(left, 91, 62, "X", 16, String.valueOf(initialX));
        yInput = field(left + 69, 91, 62, "Y", 16, String.valueOf(initialY));
        zInput = field(left + 138, 91, 62, "Z", 16, String.valueOf(initialZ));
        dimensionInput = field(left, 117, 200, "Dimensión", 256, initialDimension);
        colorInput = field(left + 80, 144, 72, "HEX", 6, String.format(Locale.ROOT, "%06X", color));
        colorInput.setFilter(value -> value.length() <= 6
                && value.chars().allMatch(character -> Character.digit(character, 16) >= 0));
        addRenderableWidget(commandIdInput); addRenderableWidget(nameInput);
        addRenderableWidget(xInput); addRenderableWidget(yInput); addRenderableWidget(zInput);
        addRenderableWidget(dimensionInput); addRenderableWidget(colorInput);
        addRenderableWidget(Button.builder(Component.literal("Icono: " + selectedIcon), button -> {
            int index = ICONS.indexOf(selectedIcon);
            selectedIcon = ICONS.get((index + 1) % ICONS.size());
            button.setMessage(Component.literal("Icono: " + selectedIcon));
        }).bounds(left, 143, 75, 20).build());
        addRenderableWidget(Button.builder(Component.literal("Elegir color"), button -> openColorPicker())
                .bounds(left + 155, 143, 75, 20).build());
        addRenderableWidget(Button.builder(Component.literal(cornerLabel(selectedCorner)), button -> {
            int index = cornerIndex(selectedCorner);
            selectedCorner = CORNERS[(index + 1) % CORNERS.length];
            button.setMessage(Component.literal(cornerLabel(selectedCorner)));
        }).bounds(left, 169, 200, 20).build());

        addRenderableWidget(Button.builder(Component.literal("Guardar"), button -> save())
                .bounds(cx - 100, 195, 95, 20).build());
        addRenderableWidget(Button.builder(Component.literal("Activar / Desactivar"), button -> {
            if (id != null) WaypointNetwork.toggle(id);
        }).bounds(cx + 5, 195, 95, 20).build());
        addRenderableWidget(Button.builder(Component.literal("Eliminar"), button -> {
            if (id != null) { WaypointNetwork.delete(id); onClose(); }
        }).bounds(cx - 100, 219, 95, 20).build());
        addRenderableWidget(Button.builder(Component.literal("Cerrar"), button -> onClose())
                .bounds(cx + 5, 219, 95, 20).build());
    }

    private String cornerLabel(String corner) {
        for (int i = 0; i < CORNERS.length; i++) if (CORNERS[i].equals(corner)) return CORNER_LABELS[i];
        return CORNER_LABELS[0];
    }

    private int cornerIndex(String corner) {
        for (int i = 0; i < CORNERS.length; i++) if (CORNERS[i].equals(corner)) return i;
        return 0;
    }

    private EditBox field(int x, int y, int w, String hint, int max, String value) {
        EditBox input = new EditBox(font, x, y, w, 18, Component.literal(hint));
        input.setMaxLength(max);
        input.setHint(Component.literal(hint));
        input.setValue(value == null ? "" : value);
        return input;
    }

    private void save() {
        try {
            int selectedColor = Integer.parseInt(colorInput.getValue().trim(), 16);
            String commandId = WaypointSavedData.sanitizeCommandId(commandIdInput.getValue());
            if (commandId.isBlank() || nameInput.getValue().trim().isBlank()) return;
            WaypointNetwork.save(id, commandId, nameInput.getValue(), dimensionInput.getValue(),
                    Double.parseDouble(xInput.getValue()), Double.parseDouble(yInput.getValue()),
                    Double.parseDouble(zInput.getValue()), selectedColor, selectedIcon, selectedCorner);
            onClose();
        } catch (Exception ignored) {
        }
    }

    private void openColorPicker() {
        try { draftColor = Integer.parseInt(colorInput.getValue(), 16) & 0xFFFFFF; }
        catch (NumberFormatException ignored) { draftColor = color; }
        colorPickerOpen = true;
    }

    private boolean updateColor(double mouseX, double mouseY) {
        double dx = mouseX - width / 2.0;
        double dy = mouseY - WHEEL_CENTER_Y;
        double distance = Math.sqrt(dx * dx + dy * dy);
        if (distance > WHEEL_RADIUS) return false;
        float hue = (float) ((Math.atan2(dy, dx) / (Math.PI * 2.0) + 1.0) % 1.0);
        float saturation = (float) Math.min(1.0, distance / WHEEL_RADIUS);
        draftColor = java.awt.Color.HSBtoRGB(hue, saturation, 1.0F) & 0xFFFFFF;
        return true;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (colorPickerOpen) {
            if (button == 0) {
                if (updateColor(mouseX, mouseY)) return true;
                int cx = width / 2;
                if (mouseX >= cx - 96 && mouseX <= cx - 4 && mouseY >= 176 && mouseY <= 199) {
                    color = draftColor;
                    colorInput.setValue(String.format(Locale.ROOT, "%06X", color));
                    colorPickerOpen = false;
                    return true;
                }
                if (mouseX >= cx + 4 && mouseX <= cx + 96 && mouseY >= 176 && mouseY <= 199) {
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
        if (colorPickerOpen && keyCode == 256) { colorPickerOpen = false; return true; }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    private void renderColorPicker(GuiGraphics g) {
        int cx = width / 2, cy = WHEEL_CENTER_Y;
        g.fill(0, 0, width, height, 0xFF101318);
        g.fill(cx - 70, 12, cx + 70, 214, 0xFF20242A);
        g.fill(cx - 72, 10, cx + 72, 216, 0xFF707780);
        g.fill(cx - 70, 12, cx + 70, 214, 0xFF20242A);
        g.drawCenteredString(font, "Elige un color", cx, 22, 0xFFFFFFFF);
        for (int y = -WHEEL_RADIUS; y < WHEEL_RADIUS; y += 3) {
            for (int x = -WHEEL_RADIUS; x < WHEEL_RADIUS; x += 3) {
                double sx = x + 1.5, sy = y + 1.5, distance = Math.sqrt(sx * sx + sy * sy);
                if (distance > WHEEL_RADIUS) continue;
                float hue = (float) ((Math.atan2(sy, sx) / (Math.PI * 2.0) + 1.0) % 1.0);
                int wheelColor = java.awt.Color.HSBtoRGB(hue, (float) (distance / WHEEL_RADIUS), 1.0F);
                g.fill(cx + x, cy + y, cx + x + 3, cy + y + 3, 0xFF000000 | (wheelColor & 0xFFFFFF));
            }
        }
        g.fill(cx - 30, 160, cx + 30, 172, 0xFF000000 | draftColor);
        g.drawCenteredString(font, String.format(Locale.ROOT, "#%06X", draftColor), cx, 162, 0xFFFFFFFF);
        g.fill(cx - 96, 176, cx - 4, 199, 0xFF397A45);
        g.drawCenteredString(font, "Aplicar", cx - 50, 183, 0xFFFFFFFF);
        g.fill(cx + 4, 176, cx + 96, 199, 0xFF6B3B3B);
        g.drawCenteredString(font, "Cancelar", cx + 50, 183, 0xFFFFFFFF);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics);
        NegativeUtilsGuiStyle.renderFrame(graphics, width, height);
        int cx = width / 2;
        graphics.fill(cx - 115, 17, cx + 115, 243, 0xE8141B24);
        graphics.fill(cx - 115, 17, cx + 115, 19, 0xFF54D6FF);
        graphics.drawCenteredString(font, title, cx, 24, 0xFFFFFFFF);
        graphics.drawString(font, "ID de comandos", cx - 100, 29, 0xFFD8E1EA);
        graphics.drawString(font, "Nombre visible", cx - 100, 55, 0xFFD8E1EA);
        graphics.drawString(font, "Coordenadas", cx - 100, 81, 0xFFD8E1EA);
        graphics.drawString(font, "Dimensión", cx - 100, 107, 0xFFD8E1EA);
        graphics.drawString(font, "Color HEX", cx - 20, 137, 0xFFD8E1EA);
        super.render(graphics, mouseX, mouseY, partialTick);
        if (colorPickerOpen) renderColorPicker(graphics);
    }

    @Override public boolean isPauseScreen() { return false; }
}