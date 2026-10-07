package com.negative.negativeutils;

import java.util.UUID;
import java.util.Locale;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public final class CameraScreen extends Screen {
    private final UUID cameraId;
    private final String commandId;
    private EditBox nameInput;
    private EditBox dimensionInput;
    private EditBox xInput;
    private EditBox yInput;
    private EditBox zInput;
    private EditBox entityIdInput;
    private EditBox playerInput;
    private String targetType;
    private boolean forceLook;
    private Component status = Component.empty();
    private int statusColor = 0xFFFFFFFF;

    public CameraScreen(UUID cameraId, String commandId, String name, String targetType,
                        String dimension, double x, double y, double z, int entityId,
                        String playerName, UUID playerUuid, boolean forceLook) {
        super(Component.literal(cameraId == null ? "Crear cámara" : "Modificar cámara"));
        this.cameraId = cameraId;
        this.commandId = commandId;
        this.targetType = CameraSavedData.sanitizeTargetType(targetType);
        this.forceLook = forceLook;
        this.initialName = name;
        this.initialDimension = dimension;
        this.initialX = x;
        this.initialY = y;
        this.initialZ = z;
        this.initialEntityId = entityId;
        this.initialPlayerName = playerName;
    }

    private final String initialName;
    private final String initialDimension;
    private final double initialX, initialY, initialZ;
    private final int initialEntityId;
    private final String initialPlayerName;

    @Override
    protected void init() {
        clearWidgets();

        int panelWidth = Math.min(520, Math.max(360, width - 24));
        int left = (width - panelWidth) / 2;
        int top = Math.max(12, (height - 330) / 2);
        int contentLeft = left + 18;
        int contentWidth = panelWidth - 36;

        nameInput = field(contentLeft, top + 48, contentWidth, 48);
        dimensionInput = field(contentLeft, top + 104, contentWidth, 256);
        xInput = numberField(contentLeft, top + 160, 105, 24);
        yInput = numberField(contentLeft + 111, top + 160, 105, 24);
        zInput = numberField(contentLeft + 222, top + 160, contentWidth - 222, 24);
        entityIdInput = numberField(contentLeft, top + 216, contentWidth, 12);
        playerInput = field(contentLeft, top + 216, contentWidth, 32);

        nameInput.setValue(initialName == null ? "" : initialName);
        dimensionInput.setValue(initialDimension == null ? "" : initialDimension);
        xInput.setValue(format(initialX));
        yInput.setValue(format(initialY));
        zInput.setValue(format(initialZ));
        entityIdInput.setValue(Integer.toString(initialEntityId));
        playerInput.setValue(initialPlayerName == null ? "" : initialPlayerName);

        addRenderableWidget(nameInput);
        addRenderableWidget(dimensionInput);
        addRenderableWidget(xInput);
        addRenderableWidget(yInput);
        addRenderableWidget(zInput);

        addRenderableWidget(entityIdInput);
        addRenderableWidget(playerInput);
        entityIdInput.visible = "ENTITY".equals(targetType);
        playerInput.visible = "PLAYER".equals(targetType);

        addRenderableWidget(Button.builder(Component.literal("Coordenadas"), b -> setTarget("COORDS"))
                .bounds(contentLeft, top + 188, 120, 20).build());
        addRenderableWidget(Button.builder(Component.literal("Entidad"), b -> setTarget("ENTITY"))
                .bounds(contentLeft + 126, top + 188, 100, 20).build());
        addRenderableWidget(Button.builder(Component.literal("Jugador"), b -> setTarget("PLAYER"))
                .bounds(contentLeft + 232, top + 188, contentWidth - 232, 20).build());

        addRenderableWidget(Button.builder(Component.literal(forceLook
                ? "Fijación: Siempre" : "Fijación: Manual (clic derecho)"), b -> {
            forceLook = !forceLook;
            b.setMessage(Component.literal(forceLook
                    ? "Fijación: Siempre" : "Fijación: Manual (clic derecho)"));
        }).bounds(contentLeft, top + 244, contentWidth, 20).build());

        addRenderableWidget(Button.builder(Component.literal("Guardar"), b -> save())
                .bounds(contentLeft, top + 278, 100, 20).build());
        addRenderableWidget(Button.builder(Component.literal("Cerrar"), b -> onClose())
                .bounds(contentLeft + contentWidth - 100, top + 278, 100, 20).build());
    }

    private EditBox field(int x, int y, int w, int max) {
        EditBox box = new EditBox(font, x, y, w, 20, Component.empty());
        box.setMaxLength(max);
        NegativeUtilsGuiStyle.styleField(box);
        return box;
    }

    private EditBox numberField(int x, int y, int w, int max) {
        EditBox box = field(x, y, w, max);
        box.setFilter(value -> value.isEmpty() || value.matches("-?\\d*(\\.\\d*)?"));
        return box;
    }

    private void setTarget(String type) {
        targetType = type;
        entityIdInput.visible = "ENTITY".equals(type);
        playerInput.visible = "PLAYER".equals(type);
        status = Component.empty();
    }

    private void save() {
        String name = nameInput.getValue().trim();
        String dimension = dimensionInput.getValue().trim();
        if (name.isBlank()) {
            status = Component.literal("Escribe un nombre para la cámara.");
            statusColor = 0xFFFF5555;
            return;
        }
        if (dimension.isBlank()) {
            status = Component.literal("Escribe la dimensión del objetivo.");
            statusColor = 0xFFFF5555;
            return;
        }

        try {
            double x = Double.parseDouble(xInput.getValue());
            double y = Double.parseDouble(yInput.getValue());
            double z = Double.parseDouble(zInput.getValue());
            int entityId = Integer.parseInt(entityIdInput.getValue().isBlank()
                    ? "0" : entityIdInput.getValue());
            String player = playerInput.getValue().trim();

            CameraNetwork.save(cameraId, commandId, name, targetType, dimension,
                    x, y, z, entityId, player, forceLook);
            status = Component.literal("Cámara enviada al servidor.");
            statusColor = 0xFF55FF55;
        } catch (NumberFormatException exception) {
            status = Component.literal("Revisa las coordenadas o el ID de entidad.");
            statusColor = 0xFFFF5555;
        }
    }

    private String format(double value) {
        return String.format(Locale.ROOT, "%.3f", value);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics);
        NegativeUtilsGuiStyle.renderFrame(graphics, width, height);

        int panelWidth = Math.min(520, Math.max(360, width - 24));
        int left = (width - panelWidth) / 2;
        int top = Math.max(12, (height - 330) / 2);
        int right = left + panelWidth;

        graphics.fill(left, top, right, top + 316, 0xEC141B24);
        graphics.fill(left, top, right, top + 2, 0xFF54D6FF);
        graphics.drawCenteredString(font, title, width / 2, top + 14, 0xFFFFFFFF);

        graphics.drawString(font, "Nombre", left + 18, top + 36, 0xFFD8E1EA);
        graphics.drawString(font, "Dimensión", left + 18, top + 92, 0xFFD8E1EA);
        graphics.drawString(font, "Objetivo", left + 18, top + 148, 0xFFD8E1EA);
        graphics.drawString(font, "X", left + 18, top + 176, 0xFF9FAAB8);
        graphics.drawString(font, "Y", left + 129, top + 176, 0xFF9FAAB8);
        graphics.drawString(font, "Z", left + 240, top + 176, 0xFF9FAAB8);

        if ("ENTITY".equals(targetType)) {
            graphics.drawString(font, "ID de entidad", left + 18, top + 204, 0xFF9FAAB8);
        } else if ("PLAYER".equals(targetType)) {
            graphics.drawString(font, "Jugador", left + 18, top + 204, 0xFF9FAAB8);
        }

        graphics.drawString(font, "Modo de fijación", left + 18, top + 232, 0xFFD8E1EA);
        if (!status.getString().isBlank()) {
            graphics.drawCenteredString(font, status, width / 2, top + 302, statusColor);
        }

        super.render(graphics, mouseX, mouseY, partialTick);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
