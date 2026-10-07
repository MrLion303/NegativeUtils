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
    private static final int WHEEL_CENTER_Y = 112;
    private static final List<String> ICONS = List.of("◆", "★", "●", "♥", "♣", "♠", "✦", "✚", "▲", "■", "☀", "☠", "✿", "⚑");
    private static final String[] CORNERS = {"TOP_LEFT", "TOP_RIGHT", "BOTTOM_LEFT", "BOTTOM_RIGHT"};
    private static final String[] CORNER_LABELS = {
            "Arriba izquierda", "Arriba derecha", "Abajo izquierda", "Abajo derecha"
    };

    private final UUID id;
    private final String initialCommandId;
    private final String initialName;
    private final String initialDimension;
    private final String initialIcon;
    private final String initialCorner;
    private final String initialTrackedPlayerName;
    private final boolean trackerMode;
    private final double initialX;
    private final double initialY;
    private final double initialZ;

    private EditBox commandIdInput;
    private EditBox nameInput;
    private EditBox trackedPlayerInput;
    private EditBox xInput;
    private EditBox yInput;
    private EditBox zInput;
    private EditBox dimensionInput;
    private EditBox colorInput;

    private Button iconButton;
    private String selectedIcon;
    private String selectedCorner;
    private int color;
    private int draftColor;
    private boolean colorPickerOpen;
    private boolean iconPickerOpen;
    private int panelTop;

    public WaypointScreen(UUID id, String commandId, String name, String dimension,
                          double x, double y, double z, int color, String icon, String corner) {
        this(id, commandId, name, dimension, x, y, z, color, icon, corner, false, "");
    }

    public WaypointScreen(UUID id, String commandId, String name, String dimension,
                          double x, double y, double z, int color, String icon, String corner,
                          boolean trackerMode, String trackedPlayerName) {
        super(Component.literal(id == null
                ? (trackerMode ? "Crear tracker" : "Crear waypoint")
                : (trackerMode ? "Editar tracker" : "Editar waypoint")));
        this.id = id;
        this.initialCommandId = commandId == null ? "" : commandId;
        this.initialName = name == null ? "" : name;
        this.initialDimension = dimension == null ? "" : dimension;
        this.initialX = x;
        this.initialY = y;
        this.initialZ = z;
        this.color = color & 0xFFFFFF;
        this.selectedIcon = WaypointSavedData.sanitizeIcon(icon);
        this.initialIcon = this.selectedIcon;
        this.selectedCorner = WaypointSavedData.sanitizeCorner(corner);
        this.initialCorner = this.selectedCorner;
        this.trackerMode = trackerMode;
        this.initialTrackedPlayerName = trackedPlayerName == null ? "" : trackedPlayerName;
        this.draftColor = this.color;
    }

    @Override
    protected void init() {
        int panelWidth = Math.min(360, Math.max(280, width - 20));
        int contentWidth = panelWidth - 32;
        panelTop = Math.max(8, (height - (trackerMode ? 328 : 300)) / 2);
        int left = (width - panelWidth) / 2;
        int fieldLeft = left + 16;
        int fieldWidth = contentWidth;
        int y = panelTop + 34;

        commandIdInput = field(fieldLeft, y + 12, fieldWidth, "ID de comandos", 48, initialCommandId);
        nameInput = field(fieldLeft, y + 46, fieldWidth, "Nombre visible", 32, initialName);
        addRenderableWidget(commandIdInput);
        addRenderableWidget(nameInput);

        y += 80;
        if (trackerMode) {
            trackedPlayerInput = field(fieldLeft, y + 12, fieldWidth, "Nombre del jugador", 16, initialTrackedPlayerName);
            addRenderableWidget(trackedPlayerInput);
            y += 34;
        }

        int coordinateWidth = (fieldWidth - 12) / 3;
        xInput = field(fieldLeft, y + 12, coordinateWidth, "X", 16, String.valueOf(initialX));
        yInput = field(fieldLeft + coordinateWidth + 6, y + 12, coordinateWidth, "Y", 16, String.valueOf(initialY));
        zInput = field(fieldLeft + (coordinateWidth + 6) * 2, y + 12, coordinateWidth, "Z", 16, String.valueOf(initialZ));
        addRenderableWidget(xInput);
        addRenderableWidget(yInput);
        addRenderableWidget(zInput);

        dimensionInput = field(fieldLeft, y + 46, fieldWidth, "Dimensión", 256, initialDimension);
        addRenderableWidget(dimensionInput);

        colorInput = field(fieldLeft, y + 80, 90, "HEX", 6,
                String.format(Locale.ROOT, "%06X", color));
        colorInput.setFilter(value -> value.length() <= 6
                && value.chars().allMatch(character -> Character.digit(character, 16) >= 0));
        addRenderableWidget(colorInput);

        iconButton = Button.builder(Component.literal("Icono: " + selectedIcon),
                        button -> iconPickerOpen = true)
                .bounds(fieldLeft + 96, y + 79, 86, 20)
                .build();
        addRenderableWidget(iconButton);

        addRenderableWidget(Button.builder(Component.literal("Elegir color"),
                        button -> openColorPicker())
                .bounds(fieldLeft + 188, y + 79, fieldWidth - 188, 20)
                .build());

        int actionY = y + 113;
        addRenderableWidget(Button.builder(
                        Component.literal("Esquina: " + cornerLabel(selectedCorner)),
                        button -> {
                            selectedCorner = CORNERS[(cornerIndex(selectedCorner) + 1) % CORNERS.length];
                            button.setMessage(Component.literal("Esquina: " + cornerLabel(selectedCorner)));
                        })
                .bounds(fieldLeft, actionY, fieldWidth, 20)
                .build());

        int buttonsY = actionY + 28;
        addRenderableWidget(Button.builder(Component.literal("Guardar"), button -> save())
                .bounds(width / 2 - 100, buttonsY, 96, 20)
                .build());
        addRenderableWidget(Button.builder(Component.literal("Cerrar"), button -> onClose())
                .bounds(width / 2 + 4, buttonsY, 96, 20)
                .build());

        if (id != null) {
            addRenderableWidget(Button.builder(Component.literal("Eliminar"), button -> {
                        WaypointNetwork.delete(id);
                        onClose();
                    })
                    .bounds(width / 2 - 100, buttonsY + 26, 96, 20)
                    .build());
            addRenderableWidget(Button.builder(Component.literal("Activar / Desactivar"),
                            button -> WaypointNetwork.toggle(id))
                    .bounds(width / 2 + 4, buttonsY + 26, 96, 20)
                    .build());
        }
    }

    private EditBox field(int x, int y, int width, String hint, int maxLength, String value) {
        EditBox input = new EditBox(font, x, y, width, 18, Component.literal(hint));
        input.setMaxLength(maxLength);
        input.setHint(Component.empty());
        input.setValue(value == null ? "" : value);
        return input;
    }

    private String cornerLabel(String corner) {
        int index = cornerIndex(corner);
        return CORNER_LABELS[index];
    }

    private int cornerIndex(String corner) {
        for (int i = 0; i < CORNERS.length; i++) {
            if (CORNERS[i].equals(corner)) return i;
        }
        return 0;
    }

    private void save() {
        try {
            int selectedColor = Integer.parseInt(colorInput.getValue().trim(), 16) & 0xFFFFFF;
            String commandId = WaypointSavedData.sanitizeCommandId(commandIdInput.getValue());
            String name = nameInput.getValue().trim();

            if (commandId.isBlank() || name.isBlank()) {
                return;
            }

            String trackedName = trackerMode ? trackedPlayerInput.getValue().trim() : "";
            if (trackerMode && trackedName.isBlank()) {
                return;
            }

            WaypointNetwork.save(
                    id,
                    commandId,
                    name,
                    dimensionInput.getValue(),
                    Double.parseDouble(xInput.getValue()),
                    Double.parseDouble(yInput.getValue()),
                    Double.parseDouble(zInput.getValue()),
                    selectedColor,
                    selectedIcon,
                    selectedCorner,
                    trackerMode,
                    trackedName
            );
            onClose();
        } catch (Exception ignored) {
        }
    }

    private void openColorPicker() {
        try {
            draftColor = Integer.parseInt(colorInput.getValue(), 16) & 0xFFFFFF;
        } catch (NumberFormatException ignored) {
            draftColor = color;
        }
        colorPickerOpen = true;
        iconPickerOpen = false;
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
            if (button != 0) return true;
            if (updateColor(mouseX, mouseY)) return true;

            int cx = width / 2;
            if (mouseX >= cx - 96 && mouseX <= cx - 4 && mouseY >= 178 && mouseY <= 202) {
                color = draftColor;
                colorInput.setValue(String.format(Locale.ROOT, "%06X", color));
                colorPickerOpen = false;
                return true;
            }
            if (mouseX >= cx + 4 && mouseX <= cx + 96 && mouseY >= 178 && mouseY <= 202) {
                colorPickerOpen = false;
                return true;
            }
            return true;
        }

        if (iconPickerOpen) {
            if (button != 0) return true;

            int cx = width / 2;
            int gridLeft = cx - 105;
            int gridTop = 76;
            if (mouseX >= gridLeft && mouseX < gridLeft + 210
                    && mouseY >= gridTop && mouseY < gridTop + 60) {
                int column = (int) ((mouseX - gridLeft) / 30);
                int row = (int) ((mouseY - gridTop) / 30);
                int index = row * 7 + column;
                if (index >= 0 && index < ICONS.size()) {
                    selectedIcon = ICONS.get(index);
                    iconButton.setMessage(Component.literal("Icono: " + selectedIcon));
                    iconPickerOpen = false;
                    return true;
                }
            }

            if (mouseX >= cx - 45 && mouseX <= cx + 45 && mouseY >= 154 && mouseY <= 178) {
                iconPickerOpen = false;
            }
            return true;
        }

        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if ((colorPickerOpen || iconPickerOpen) && keyCode == 256) {
            colorPickerOpen = false;
            iconPickerOpen = false;
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    private void renderIconPicker(GuiGraphics graphics) {
        graphics.pose().pushPose();
        graphics.pose().translate(0, 0, 1000);
        graphics.fill(0, 0, width, height, 0xD0101318);

        int cx = width / 2;
        int left = cx - 125;
        int top = 42;
        int right = cx + 125;
        int bottom = 190;

        graphics.fill(left, top, right, bottom, 0xFF707780);
        graphics.fill(left + 2, top + 2, right - 2, bottom - 2, 0xFF20242A);
        graphics.drawCenteredString(font, "Elige un icono", cx, top + 10, 0xFFFFFFFF);

        int gridLeft = cx - 105;
        int gridTop = 76;
        for (int i = 0; i < ICONS.size(); i++) {
            int column = i % 7;
            int row = i / 7;
            int x = gridLeft + column * 30;
            int y = gridTop + row * 30;
            int background = ICONS.get(i).equals(selectedIcon)
                    ? 0xFF397A45
                    : 0xFF343B45;
            graphics.fill(x + 1, y + 1, x + 29, y + 29, background);
            graphics.drawCenteredString(font, ICONS.get(i), x + 15, y + 10, 0xFFFFFFFF);
        }

        graphics.fill(cx - 45, 154, cx + 45, 178, 0xFF6B3B3B);
        graphics.drawCenteredString(font, "Cancelar", cx, 161, 0xFFFFFFFF);
        graphics.pose().popPose();
    }

    private void renderColorPicker(GuiGraphics graphics) {
        graphics.pose().pushPose();
        graphics.pose().translate(0, 0, 1000);
        graphics.fill(0, 0, width, height, 0xD0101318);

        int cx = width / 2;
        int panelLeft = cx - 92;
        int panelRight = cx + 92;
        int panelTop = 8;
        int panelBottom = 218;

        graphics.fill(panelLeft, panelTop, panelRight, panelBottom, 0xFF707780);
        graphics.fill(panelLeft + 2, panelTop + 2, panelRight - 2, panelBottom - 2, 0xFF20242A);
        graphics.drawCenteredString(font, "Elige un color", cx, 20, 0xFFFFFFFF);

        for (int y = -WHEEL_RADIUS; y < WHEEL_RADIUS; y += 3) {
            for (int x = -WHEEL_RADIUS; x < WHEEL_RADIUS; x += 3) {
                double sx = x + 1.5;
                double sy = y + 1.5;
                double distance = Math.sqrt(sx * sx + sy * sy);
                if (distance > WHEEL_RADIUS) continue;

                float hue = (float) ((Math.atan2(sy, sx) / (Math.PI * 2.0) + 1.0) % 1.0);
                int wheelColor = java.awt.Color.HSBtoRGB(
                        hue, (float) (distance / WHEEL_RADIUS), 1.0F);
                graphics.fill(
                        cx + x, WHEEL_CENTER_Y + y,
                        cx + x + 3, WHEEL_CENTER_Y + y + 3,
                        0xFF000000 | (wheelColor & 0xFFFFFF)
                );
            }
        }

        graphics.fill(cx - 30, 164, cx + 30, 176, 0xFF000000 | draftColor);
        graphics.drawCenteredString(
                font, String.format(Locale.ROOT, "#%06X", draftColor),
                cx, 166, 0xFFFFFFFF);

        graphics.fill(cx - 96, 182, cx - 4, 206, 0xFF397A45);
        graphics.drawCenteredString(font, "Aplicar", cx - 50, 189, 0xFFFFFFFF);
        graphics.fill(cx + 4, 182, cx + 96, 206, 0xFF6B3B3B);
        graphics.drawCenteredString(font, "Cancelar", cx + 50, 189, 0xFFFFFFFF);
        graphics.pose().popPose();
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics);
        NegativeUtilsGuiStyle.renderFrame(graphics, width, height);

        int panelWidth = Math.min(360, Math.max(280, width - 20));
        int panelHeight = trackerMode ? 328 : 300;
        int left = (width - panelWidth) / 2;

        graphics.fill(left, panelTop, left + panelWidth, panelTop + panelHeight, 0xE8141B24);
        graphics.fill(left, panelTop, left + panelWidth, panelTop + 2, 0xFF54D6FF);
        graphics.drawCenteredString(font, title, width / 2, panelTop + 10, 0xFFFFFFFF);

        int labelLeft = left + 16;
        graphics.drawString(font, "ID de comandos", labelLeft, panelTop + 23, 0xFFD8E1EA);
        graphics.drawString(font, "Nombre visible", labelLeft, panelTop + 57, 0xFFD8E1EA);

        int labelY = panelTop + 91;
        if (trackerMode) {
            graphics.drawString(font, "Jugador a rastrear", labelLeft, labelY, 0xFF54D6FF);
            labelY += 34;
        }

        graphics.drawString(font, "Coordenadas", labelLeft, labelY, 0xFFD8E1EA);
        graphics.drawString(font, "Dimensión", labelLeft, labelY + 34, 0xFFD8E1EA);
        graphics.drawString(font, "Color e icono", labelLeft, labelY + 68, 0xFFD8E1EA);

        super.render(graphics, mouseX, mouseY, partialTick);

        if (colorPickerOpen) {
            renderColorPicker(graphics);
        } else if (iconPickerOpen) {
            renderIconPicker(graphics);
        }
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
