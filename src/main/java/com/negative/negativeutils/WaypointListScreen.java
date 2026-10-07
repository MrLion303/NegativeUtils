package com.negative.negativeutils;

import java.util.List;
import java.util.UUID;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public final class WaypointListScreen extends Screen {
    private static final int ROW_HEIGHT = 28;
    private int scroll;
    private List<WaypointSavedData.Waypoint> entries = List.of();

    public WaypointListScreen() {
        super(Component.literal("Waypoints"));
    }

    @Override
    protected void init() {
        refresh();
    }

    private void refresh() {
        clearWidgets();
        entries = WaypointClientData.getWaypoints();
        int panelWidth = Math.min(620, Math.max(300, width - 24));
        int left = (width - panelWidth) / 2;
        int top = 42;
        int visibleRows = Math.max(1, (height - 92) / ROW_HEIGHT);
        int maxScroll = Math.max(0, entries.size() - visibleRows);
        scroll = Math.max(0, Math.min(scroll, maxScroll));

        for (int i = 0; i < visibleRows; i++) {
            int index = scroll + i;
            if (index >= entries.size()) break;
            WaypointSavedData.Waypoint waypoint = entries.get(index);
            int y = top + i * ROW_HEIGHT;
            int actionWidth = 92;
            int deleteWidth = 64;
            int editWidth = 52;
            int gap = 4;
            int right = left + panelWidth - 8;

            addRenderableWidget(Button.builder(
                    Component.literal(waypoint.visible() ? "Desactivar" : "Activar"),
                    button -> WaypointNetwork.toggle(waypoint.id()))
                    .bounds(right - actionWidth - deleteWidth - editWidth - gap * 2, y, actionWidth, 20).build());

            addRenderableWidget(Button.builder(Component.literal("Editar"),
                    button -> openEditor(waypoint))
                    .bounds(right - actionWidth - deleteWidth - editWidth - gap, y, editWidth, 20).build());

            addRenderableWidget(Button.builder(Component.literal("Eliminar"),
                    button -> WaypointNetwork.delete(waypoint.id()))
                    .bounds(right - deleteWidth, y, deleteWidth, 20).build());
        }

        addRenderableWidget(Button.builder(Component.literal("Cerrar"), button -> onClose())
                .bounds(width / 2 - 50, height - 28, 100, 20).build());
    }

    private void openEditor(WaypointSavedData.Waypoint waypoint) {
        minecraft.setScreen(new WaypointScreen(
                waypoint.id(), waypoint.commandId(), waypoint.name(), waypoint.dimension(),
                waypoint.x(), waypoint.y(), waypoint.z(), waypoint.color(), waypoint.icon(), waypoint.corner()));
    }

    @Override
    public void tick() {
        super.tick();
        List<WaypointSavedData.Waypoint> latest = WaypointClientData.getWaypoints();
        if (!latest.equals(entries)) refresh();
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        int panelHeight = height - 92;
        if (mouseY >= 42 && mouseY <= 42 + panelHeight) {
            int rows = Math.max(1, panelHeight / ROW_HEIGHT);
            int maxScroll = Math.max(0, entries.size() - rows);
            int next = Math.max(0, Math.min(maxScroll, scroll - (int) Math.signum(delta)));
            if (next != scroll) {
                scroll = next;
                refresh();
            }
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, delta);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics);
        NegativeUtilsGuiStyle.renderFrame(graphics, width, height);

        int panelWidth = Math.min(620, Math.max(300, width - 24));
        int left = (width - panelWidth) / 2;
        graphics.fill(left, 16, left + panelWidth, height - 16, 0xE8141B24);
        graphics.fill(left, 16, left + panelWidth, 18, 0xFF54D6FF);
        graphics.drawCenteredString(font, title, width / 2, 24, 0xFFFFFFFF);

        int top = 42;
        int visibleRows = Math.max(1, (height - 92) / ROW_HEIGHT);
        for (int i = 0; i < visibleRows; i++) {
            int index = scroll + i;
            if (index >= entries.size()) break;
            WaypointSavedData.Waypoint waypoint = entries.get(index);
            int y = top + i * ROW_HEIGHT;
            String state = waypoint.visible() ? "Activo" : "Oculto";
            String label = waypoint.name() + "  [" + state + "]  " + waypoint.dimension()
                    + "  " + Math.round(waypoint.x()) + ", " + Math.round(waypoint.y())
                    + ", " + Math.round(waypoint.z());
            int reserved = 92 + 64 + 52 + 12;
            graphics.drawString(font, trimToWidth(label, panelWidth - reserved - 16),
                    left + 8, y + 6, 0xFFFFFFFF);
        }

        if (entries.isEmpty()) {
            graphics.drawCenteredString(font, "No hay waypoints creados.", width / 2, height / 2, 0xFFBFC7D5);
        }
        super.render(graphics, mouseX, mouseY, partialTick);
    }

    private String trimToWidth(String text, int maxWidth) {
        if (font.width(text) <= maxWidth) return text;
        String suffix = "...";
        int limit = Math.max(0, maxWidth - font.width(suffix));
        while (!text.isEmpty() && font.width(text) > limit) text = text.substring(0, text.length() - 1);
        return text + suffix;
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
