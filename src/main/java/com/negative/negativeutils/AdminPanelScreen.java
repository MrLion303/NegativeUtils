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
    private boolean monterreyTime;

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

        int panelWidth = Math.min(760, Math.max(560, width - 24));
        int panelLeft = (width - panelWidth) / 2;
        int dividerX = panelLeft + 190;
        int editorLeft = dividerX + 18;
        int fieldWidth = panelWidth - 190 - 36;

        addRenderableWidget(Button.builder(Component.literal("Nuevo contador"), button -> {
            selected = -1;
            clearEditor();
            buildWidgets();
        }).bounds(panelLeft + 14, 58, 162, 20).build());

        for (int i = 0; i < entries.size() && i < 7; i++) {
            CountdownClientData.Entry entry = entries.get(i);
            int index = i;
            int y = 86 + i * 24;
            addRenderableWidget(Button.builder(
                    Component.literal((entry.running() ? "● " : "○ ") + trimToWidth(entry.name(), 145)),
                    button -> {
                        selected = index;
                        buildWidgets();
                    }).bounds(panelLeft + 14, y, 162, 20).build());
        }

        nameInput = field(editorLeft, 56, fieldWidth, "", 32);
        addRenderableWidget(nameInput);

        int smallWidth = Math.max(36, (fieldWidth - 12) / 3);
        int dateY = 106;
        dayInput = numberField(editorLeft, dateY, smallWidth, 2);
        monthInput = numberField(editorLeft + smallWidth + 6, dateY, smallWidth, 2);
        yearInput = numberField(editorLeft + (smallWidth + 6) * 2, dateY,
                fieldWidth - (smallWidth + 6) * 2, 4);

        int timeY = 154;
        hourInput = numberField(editorLeft, timeY, smallWidth, 2);
        minuteInput = numberField(editorLeft + smallWidth + 6, timeY, smallWidth, 2);
        secondInput = numberField(editorLeft + (smallWidth + 6) * 2, timeY,
                fieldWidth - (smallWidth + 6) * 2, 2);

        displayTextInput = field(editorLeft, 192, fieldWidth, "", 100);
        int colorWidth = Math.min(86, Math.max(70, fieldWidth - 74));
        colorInput = field(editorLeft, 224, colorWidth, "", 6);
        colorInput.setValue("FFFFFF");
        colorInput.setFilter(text -> text.length() <= 6
                && text.chars().allMatch(character -> Character.digit(character, 16) >= 0));
        addRenderableWidget(dayInput);
        addRenderableWidget(monthInput);
        addRenderableWidget(yearInput);
        addRenderableWidget(hourInput);
        addRenderableWidget(minuteInput);
        addRenderableWidget(secondInput);
        addRenderableWidget(displayTextInput);
        addRenderableWidget(colorInput);

        addRenderableWidget(Button.builder(Component.literal("Elegir color"),
                button -> openColorPicker())
                .bounds(editorLeft + colorWidth + 6, 224, fieldWidth - colorWidth - 6, 20)
                .build());

        addRenderableWidget(Button.builder(
                Component.literal(monterreyTime ? "Zona: Monterrey (UTC-6)" : "Zona: UTC"),
                button -> {
                    long target = readTargetMillis();
                    monterreyTime = !monterreyTime;
                    setDateFields(target);
                    button.setMessage(Component.literal(
                            monterreyTime ? "Zona: Monterrey (UTC-6)" : "Zona: UTC"));
                }).bounds(editorLeft, 256, fieldWidth, 20).build());

        addRenderableWidget(Button.builder(Component.literal(POSITION_LABELS[selectedPosition]),
                button -> {
                    selectedPosition = (selectedPosition + 1) % POSITIONS.length;
                    button.setMessage(Component.literal(POSITION_LABELS[selectedPosition]));
                }).bounds(editorLeft, 288, fieldWidth, 20).build());

        int actionY = height - 52;
        addRenderableWidget(Button.builder(Component.literal("Guardar"),
                button -> saveCountdown())
                .bounds(editorLeft, actionY, 86, 20).build());
        addRenderableWidget(Button.builder(Component.literal(
                selected >= 0 && selected < entries.size() && entries.get(selected).running()
                        ? "Desactivar" : "Activar"), button -> toggleCountdown())
                .bounds(editorLeft + 92, actionY, 86, 20).build());

        addRenderableWidget(Button.builder(Component.literal("Eliminar"),
                button -> deleteCountdown())
                .bounds(panelLeft + 14, height - 52, 162, 20).build());
        addRenderableWidget(Button.builder(Component.literal("Cerrar"),
                button -> onClose())
                .bounds(width / 2 - 45, height - 28, 90, 20).build());

        if (selected >= 0 && selected < entries.size()) {
            loadSelected();
        } else {
            clearEditor();
        }
    }

    private String trimToWidth(String text, int maxWidth) {
        if (font.width(text) <= maxWidth) return text;
        String suffix = "...";
        int limit = Math.max(0, maxWidth - font.width(suffix));
        while (!text.isEmpty() && font.width(text) > limit) {
            text = text.substring(0, text.length() - 1);
        }
        return text + suffix;
    }

    private EditBox field(int x, int y, int w, String hint, int maxLength) {
        EditBox input = new EditBox(font, x, y, w, 20, Component.literal(hint));
        input.setMaxLength(maxLength);
        NegativeUtilsGuiStyle.styleField(input);
        return input;
    }

    private EditBox numberField(int x, int y, int w, int maxLength) {
        EditBox input = field(x, y, w, "", maxLength);
        input.setFilter(text -> text.isEmpty() || text.chars().allMatch(Character::isDigit));
        return input;
    }

    private void setDateFields(long epochMillis) {
        ZoneOffset offset = monterreyTime ? ZoneOffset.ofHours(-6) : ZoneOffset.UTC;
        LocalDateTime local = LocalDateTime.ofEpochSecond(epochMillis / 1000L, 0, offset);
        dayInput.setValue(String.format(Locale.ROOT, "%02d", local.getDayOfMonth()));
        monthInput.setValue(String.format(Locale.ROOT, "%02d", local.getMonthValue()));
        yearInput.setValue(String.format(Locale.ROOT, "%04d", local.getYear()));
        hourInput.setValue(String.format(Locale.ROOT, "%02d", local.getHour()));
        minuteInput.setValue(String.format(Locale.ROOT, "%02d", local.getMinute()));
        secondInput.setValue(String.format(Locale.ROOT, "%02d", local.getSecond()));
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

    private long readTargetMillis() {
        try {
            ZoneOffset offset = monterreyTime ? ZoneOffset.ofHours(-6) : ZoneOffset.UTC;
            LocalDateTime local = LocalDateTime.of(
                    Integer.parseInt(yearInput.getValue()),
                    Integer.parseInt(monthInput.getValue()),
                    Integer.parseInt(dayInput.getValue()),
                    Integer.parseInt(hourInput.getValue()),
                    Integer.parseInt(minuteInput.getValue()),
                    Integer.parseInt(secondInput.getValue()));
            return local.toInstant(offset).toEpochMilli();
        } catch (Exception exception) {
            return System.currentTimeMillis() + 3_600_000L;
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
            targetMillis = readTargetMillis();
            color = Integer.parseInt(colorInput.getValue().trim(), 16);
        } catch (NumberFormatException | DateTimeException exception) {
            setStatus("Introduce una fecha y un color hexadecimal válidos.", 0xFFFF5555);
            return;
        }

        long remainingMillis = targetMillis - System.currentTimeMillis();
        if (remainingMillis <= 0) {
            setStatus("La fecha y hora deben ser futuras.", 0xFFFF5555);
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
        setStatus("Contador guardado. Zona: " + (monterreyTime ? "Monterrey (UTC-6)." : "UTC."), 0xFF55FF55);
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
        graphics.pose().pushPose();
        graphics.pose().translate(0, 0, 1000);
        graphics.fill(0, 0, width, height, 0xB0101318);
        int centerX = width / 2;
        int centerY = WHEEL_CENTER_Y;
        int panelLeft = centerX - WHEEL_RADIUS - 22;
        int panelRight = centerX + WHEEL_RADIUS + 22;
        graphics.fill(0, 0, width, height, 0xFF101318);
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
        graphics.pose().popPose();
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics);
        NegativeUtilsGuiStyle.renderFrame(graphics, width, height);

        int panelWidth = Math.min(760, Math.max(560, width - 24));
        int panelLeft = (width - panelWidth) / 2;
        int panelRight = panelLeft + panelWidth;
        int dividerX = panelLeft + 190;
        int editorLeft = dividerX + 18;
        int editorRight = panelRight - 18;

        graphics.fill(panelLeft, 8, panelRight, height - 8, 0xE8141B24);
        graphics.fill(panelLeft, 8, panelRight, 10, 0xFF54D6FF);

        graphics.drawCenteredString(font, title, width / 2, 16, 0xFFFFFFFF);
        if (!statusMessage.getString().isBlank()) {
            graphics.drawCenteredString(font, statusMessage, width / 2, 28, statusColor);
        }

        graphics.drawString(font, "Contadores", panelLeft + 14, 46, 0xFFFFFFFF);
        graphics.drawString(font, "Editor", editorLeft, 46, 0xFFFFFFFF);

        graphics.fill(dividerX, 40, dividerX + 1, height - 24, 0x554C5968);

        graphics.drawString(font, "Nombre", editorLeft, 42, 0xFFD8E1EA);
        graphics.drawString(font, "Fecha", editorLeft, 84, 0xFFD8E1EA);
        graphics.drawString(font,
                monterreyTime ? "Monterrey (UTC-6)" : "UTC",
                editorLeft + 78, 84, 0xFF9FAAB8);
        graphics.drawString(font, "Día", editorLeft, 98, 0xFF9FAAB8);
        graphics.drawString(font, "Mes", editorLeft + 49, 98, 0xFF9FAAB8);
        graphics.drawString(font, "Año", editorLeft + 98, 98, 0xFF9FAAB8);

        graphics.drawString(font, "Hora", editorLeft, 132, 0xFFD8E1EA);
        graphics.drawString(font, "H", editorLeft, 146, 0xFF9FAAB8);
        graphics.drawString(font, "Min", editorLeft + 49, 146, 0xFF9FAAB8);
        graphics.drawString(font, "Seg", editorLeft + 98, 146, 0xFF9FAAB8);

        graphics.drawString(font, "Texto debajo del contador", editorLeft, 184, 0xFFD8E1EA);
        graphics.drawString(font, "Color", editorLeft, 216, 0xFFD8E1EA);
        graphics.drawString(font, "Posición", editorLeft, 248, 0xFFD8E1EA);

        super.render(graphics, mouseX, mouseY, partialTick);

        if (colorPickerOpen) {
            renderColorWheel(graphics);
        }
    }
    @Override
    public boolean isPauseScreen() {
        return false;
    }
}