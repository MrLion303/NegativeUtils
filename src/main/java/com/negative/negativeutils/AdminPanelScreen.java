package com.negative.negativeutils;

import java.time.DateTimeException;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public class AdminPanelScreen extends Screen {
    private static final String[] POSITIONS = {"BOSSBAR", "ACTIONBAR", "SCOREBOARD", "TITLE"};
    private static final String[] POSITION_LABELS = {
            "Arriba, estilo bossbar", "Barra de acción", "Marcador lateral", "Centro, estilo título"
    };
    private static final int WHEEL_RADIUS = 48;
    private static final int WHEEL_CELL_SIZE = 3;
    private static final int WHEEL_CENTER_Y = 111;

    private final List<CountdownClientData.Entry> entries = new ArrayList<>();
    private EditBox nameInput, dayInput, monthInput, yearInput, hourInput, minuteInput, secondInput;
    private EditBox displayTextInput, colorInput;
    private Component statusMessage = Component.empty();
    private int statusColor = 0xFFFFFF;
    private int selected = -1;
    private int selectedPosition;
    private int selectedWheelColor = 0xFFFFFF;
    private int wheelDraftColor = 0xFFFFFF;
    private boolean colorPickerOpen;

    public AdminPanelScreen() {
        super(Component.literal("Panel de contadores"));
    }

    @Override
    protected void init() {
        entries.clear();
        entries.addAll(CountdownClientData.getAll());
        buildWidgets();
    }

    private void buildWidgets() {
        clearWidgets();
        int left = width / 2 - 195;
        int right = width / 2 + 15;

        addRenderableWidget(Button.builder(Component.literal("Nuevo contador"), button -> {
            selected = -1;
            clearEditor();
            buildWidgets();
        }).bounds(left, 34, 150, 20).build());

        for (int i = 0; i < entries.size() && i < 5; i++) {
            CountdownClientData.Entry entry = entries.get(i);
            int index = i;
            addRenderableWidget(Button.builder(
                    Component.literal((entry.running() ? "● " : "○ ") + entry.name()),
                    button -> {
                        selected = index;
                        buildWidgets();
                    }).bounds(left, 58 + i * 23, 150, 20).build());
        }

        nameInput = field(right, 30, 155, "Nombre del contador", 32);
        int dateWidth = 43;
        int dateGap = 4;
        int dateStart = right + 1;
        dayInput = numberField(dateStart, 64, dateWidth, 2);
        monthInput = numberField(dateStart + dateWidth + dateGap, 64, dateWidth, 2);
        yearInput = numberField(dateStart + (dateWidth + dateGap) * 2, 64, dateWidth + 7, 4);
        hourInput = numberField(dateStart, 95, dateWidth, 2);
        minuteInput = numberField(dateStart + dateWidth + dateGap, 95, dateWidth, 2);
        secondInput = numberField(dateStart + (dateWidth + dateGap) * 2, 95, dateWidth + 7, 2);

        displayTextInput = field(right, 128, 155, "Texto debajo del contador", 100);
        colorInput = field(right, 161, 88, "Color HEX", 6);
        colorInput.setValue("FFFFFF");
        colorInput.setFilter(text -> text.length() <= 6
                && text.chars().allMatch(character -> Character.digit(character, 16) >= 0));

        addRenderableWidget(nameInput);
        addRenderableWidget(dayInput);
        addRenderableWidget(monthInput);
        addRenderableWidget(yearInput);
        addRenderableWidget(hourInput);
        addRenderableWidget(minuteInput);
        addRenderableWidget(secondInput);
        addRenderableWidget(displayTextInput);
        addRenderableWidget(colorInput);

        addRenderableWidget(Button.builder(Component.literal("Elegir color"), button -> openColorPicker())
                .bounds(right + 92, 161, 63, 20).build());
        addRenderableWidget(Button.builder(Component.literal(POSITION_LABELS[selectedPosition]), button -> {
            selectedPosition = (selectedPosition + 1) % POSITIONS.length;
            button.setMessage(Component.literal(POSITION_LABELS[selectedPosition]));
        }).bounds(right, 185, 155, 20).build());

        int actionY = height - 49;
        addRenderableWidget(Button.builder(Component.literal("Guardar"), button -> saveCountdown())
                .bounds(right, actionY, 74, 20).build());
        addRenderableWidget(Button.builder(Component.literal(
                selected >= 0 && selected < entries.size() && entries.get(selected).running()
                        ? "Desactivar" : "Activar"), button -> toggleCountdown())
                .bounds(right + 80, actionY, 75, 20).build());
        addRenderableWidget(Button.builder(Component.literal("Eliminar"), button -> deleteCountdown())
                .bounds(left, height - 25, 72, 20).build());
        addRenderableWidget(Button.builder(Component.literal("Cerrar"), button -> onClose())
                .bounds(width / 2 + 80, height - 25, 75, 20).build());

        if (selected >= 0 && selected < entries.size()) {
            loadSelected();
        } else {
            clearEditor();
        }
    }

    private EditBox field(int x, int y, int w, String hint, int maxLength) {
        EditBox input = new EditBox(font, x, y, w, 20, Component.literal(hint));
        input.setMaxLength(maxLength);
        input.setHint(Component.literal(hint));
        return input;
    }

    private EditBox numberField(int x, int y, int w, int maxLength) {
        EditBox input = field(x, y, w, "", maxLength);
        input.setFilter(text -> text.isEmpty() || text.chars().allMatch(Character::isDigit));
        return input;
    }

    private void setDateFields(long epochMillis) {
        LocalDateTime utc = LocalDateTime.ofEpochSecond(epochMillis / 1000L, 0, ZoneOffset.UTC);
        dayInput.setValue(String.format(Locale.ROOT, "%02d", utc.getDayOfMonth()));
        monthInput.setValue(String.format(Locale.ROOT, "%02d", utc.getMonthValue()));
        yearInput.setValue(String.format(Locale.ROOT, "%04d", utc.getYear()));
        hourInput.setValue(String.format(Locale.ROOT, "%02d", utc.getHour()));
        minuteInput.setValue(String.format(Locale.ROOT, "%02d", utc.getMinute()));
        secondInput.setValue(String.format(Locale.ROOT, "%02d", utc.getSecond()));
    }

    private void clearEditor() {
        if (nameInput == null) return;
        nameInput.setValue("");
        setDateFields(System.currentTimeMillis() + 3_600_000L);
        displayTextInput.setValue("");
        colorInput.setValue("FFFFFF");
        selectedPosition = 0;
        selectedWheelColor = 0xFFFFFF;
    }

    private void loadSelected() {
        CountdownClientData.Entry entry = entries.get(selected);
        nameInput.setValue(entry.name());
        long target = entry.running()
                ? entry.endTimeMillis()
                : System.currentTimeMillis() + entry.remainingMillis();
        setDateFields(target);
        displayTextInput.setValue(entry.displayText());
        colorInput.setValue(String.format(Locale.ROOT, "%06X", entry.displayColor() & 0xFFFFFF));
        selectedWheelColor = entry.displayColor() & 0xFFFFFF;
        selectedPosition = 0;
        for (int i = 0; i < POSITIONS.length; i++) {
            if (POSITIONS[i].equals(entry.displayPosition())) selectedPosition = i;
        }
    }

    private void saveCountdown() {
        if (nameInput.getValue().trim().isEmpty()) {
            setStatus("Escribe un nombre para el contador.", 0xFFFF5555);
            return;
        }

        final long targetMillis;
        final int color;
        try {
            LocalDateTime utc = LocalDateTime.of(
                    Integer.parseInt(yearInput.getValue()),
                    Integer.parseInt(monthInput.getValue()),
                    Integer.parseInt(dayInput.getValue()),
                    Integer.parseInt(hourInput.getValue()),
                    Integer.parseInt(minuteInput.getValue()),
                    Integer.parseInt(secondInput.getValue()));
            targetMillis = utc.toInstant(ZoneOffset.UTC).toEpochMilli();
            color = Integer.parseInt(colorInput.getValue().trim(), 16);
        } catch (NumberFormatException | DateTimeException exception) {
            setStatus("Introduce una fecha UTC y un color hexadecimal válidos.", 0xFFFF5555);
            return;
        }

        long remainingMillis = targetMillis - System.currentTimeMillis();
        if (remainingMillis <= 0) {
            setStatus("La fecha y hora UTC deben ser futuras.", 0xFFFF5555);
            return;
        }
        if (colorInput.getValue().trim().length() != 6) {
            setStatus("El color debe tener 6 caracteres hexadecimales.", 0xFFFF5555);
            return;
        }

        long seconds = Math.max(1L, (remainingMillis + 999L) / 1000L);
        CountdownNetwork.saveCountdown(
                selected >= 0 && selected < entries.size() ? entries.get(selected).id() : null,
                nameInput.getValue().trim(),
                seconds,
                displayTextInput.getValue(),
                color & 0xFFFFFF,
                POSITIONS[selectedPosition]);
        setStatus("Contador guardado. La fecha se interpreta en UTC.", 0xFF55FF55);
    }

    private void toggleCountdown() {
        if (selected >= 0 && selected < entries.size()) {
            CountdownNetwork.toggleCountdown(entries.get(selected).id());
        } else {
            setStatus("Selecciona un contador de la lista.", 0xFFFFAA55);
        }
    }

    private void deleteCountdown() {
        if (selected >= 0 && selected < entries.size()) {
            CountdownNetwork.deleteCountdown(entries.get(selected).id());
            selected = -1;
            setStatus("Solicitud de eliminación enviada.", 0xFFFFAA55);
        } else {
            setStatus("Selecciona un contador de la lista.", 0xFFFFAA55);
        }
    }

    public void refreshData() {
        entries.clear();
        entries.addAll(CountdownClientData.getAll());
        if (selected >= entries.size()) selected = -1;
        buildWidgets();
    }

    private void setStatus(String message, int color) {
        statusMessage = Component.literal(message);
        statusColor = color;
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
        double dx = mouseX - width / 2.0;
        double dy = mouseY - WHEEL_CENTER_Y;
        double distance = Math.sqrt(dx * dx + dy * dy);
        if (distance > WHEEL_RADIUS) return false;

        float hue = (float) ((Math.atan2(dy, dx) / (Math.PI * 2.0) + 1.0) % 1.0);
        float saturation = (float) Math.min(1.0, distance / WHEEL_RADIUS);
        wheelDraftColor = java.awt.Color.HSBtoRGB(hue, saturation, 1.0F) & 0xFFFFFF;
        return true;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (colorPickerOpen) {
            if (button == 0) {
                if (updateColorFromWheel(mouseX, mouseY)) return true;
                int centerX = width / 2;
                if (mouseX >= centerX - 96 && mouseX <= centerX - 4
                        && mouseY >= 193 && mouseY <= 216) {
                    selectedWheelColor = wheelDraftColor;
                    colorInput.setValue(String.format(Locale.ROOT, "%06X", wheelDraftColor));
                    colorPickerOpen = false;
                    return true;
                }
                if (mouseX >= centerX + 4 && mouseX <= centerX + 96
                        && mouseY >= 193 && mouseY <= 216) {
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
        int centerX = width / 2;
        int centerY = WHEEL_CENTER_Y;
        int panelLeft = centerX - WHEEL_RADIUS - 22;
        int panelRight = centerX + WHEEL_RADIUS + 22;
        graphics.fill(0, 0, width, height, 0xA0000000);
        graphics.fill(panelLeft - 2, 10, panelRight + 2, 230, 0xFF7A7A7A);
        graphics.fill(panelLeft, 12, panelRight, 228, 0xFF20242A);
        graphics.drawCenteredString(font, "Elige un color", centerX, 24, 0xFFFFFFFF);

        for (int y = -WHEEL_RADIUS; y < WHEEL_RADIUS; y += WHEEL_CELL_SIZE) {
            for (int x = -WHEEL_RADIUS; x < WHEEL_RADIUS; x += WHEEL_CELL_SIZE) {
                double sampleX = x + WHEEL_CELL_SIZE / 2.0;
                double sampleY = y + WHEEL_CELL_SIZE / 2.0;
                double distance = Math.sqrt(sampleX * sampleX + sampleY * sampleY);
                if (distance > WHEEL_RADIUS) continue;
                float hue = (float) ((Math.atan2(sampleY, sampleX) / (Math.PI * 2.0) + 1.0) % 1.0);
                float saturation = (float) (distance / WHEEL_RADIUS);
                int color = java.awt.Color.HSBtoRGB(hue, saturation, 1.0F);
                graphics.fill(centerX + x, centerY + y,
                        centerX + x + WHEEL_CELL_SIZE, centerY + y + WHEEL_CELL_SIZE,
                        0xFF000000 | (color & 0xFFFFFF));
            }
        }

        float[] hsv = java.awt.Color.RGBtoHSB(
                wheelDraftColor >>> 16 & 0xFF, wheelDraftColor >>> 8 & 0xFF,
                wheelDraftColor & 0xFF, null);
        int markerX = centerX + (int) (Math.cos(hsv[0] * Math.PI * 2.0) * hsv[1] * WHEEL_RADIUS);
        int markerY = centerY + (int) (Math.sin(hsv[0] * Math.PI * 2.0) * hsv[1] * WHEEL_RADIUS);
        graphics.fill(markerX - 4, markerY - 1, markerX + 5, markerY + 2, 0xFF000000);
        graphics.fill(markerX - 1, markerY - 4, markerX + 2, markerY + 5, 0xFF000000);
        graphics.fill(markerX - 3, markerY, markerX + 4, markerY + 1, 0xFFFFFFFF);
        graphics.fill(markerX, markerY - 3, markerX + 1, markerY + 4, 0xFFFFFFFF);
        graphics.fill(centerX - 30, 169, centerX + 30, 188, 0xFF000000 | wheelDraftColor);
        graphics.drawCenteredString(font, String.format(Locale.ROOT, "#%06X", wheelDraftColor),
                centerX, 174, 0xFFFFFFFF);
        graphics.fill(centerX - 96, 193, centerX - 4, 216, 0xFF397A45);
        graphics.drawCenteredString(font, "Aplicar", centerX - 50, 200, 0xFFFFFFFF);
        graphics.fill(centerX + 4, 193, centerX + 96, 216, 0xFF6B3B3B);
        graphics.drawCenteredString(font, "Cancelar", centerX + 50, 200, 0xFFFFFFFF);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics);
        NegativeUtilsGuiStyle.renderFrame(graphics, width, height);
        super.render(graphics, mouseX, mouseY, partialTick);
        graphics.drawCenteredString(font, title, width / 2, 8, 0xFFFFFF);
        graphics.drawCenteredString(font, statusMessage, width / 2, 20, statusColor);

        int left = width / 2 - 195;
        int right = width / 2 + 15;
        graphics.drawString(font, "Contadores", left, 22, 0xFFFFFF);
        graphics.drawString(font, "Editor", right, 21, 0xFFFFFF);
        graphics.drawString(font, "Fecha UTC: día / mes / año", right, 53, 0xFFFFFF);
        graphics.drawString(font, "Hora UTC: h / min / seg", right, 84, 0xFFFFFF);
        graphics.drawString(font, "Texto debajo del contador", right, 118, 0xFFFFFF);
        graphics.drawString(font, "Color", right, 151, 0xFFFFFF);

        if (colorPickerOpen) renderColorWheel(graphics);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}