package com.negative.negativeutils;

import java.util.List;
import java.util.UUID;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;

public final class WaypointListScreen extends Screen {
    private static final int ROW_HEIGHT = 46;
    private int scroll;
    private List<WaypointSavedData.Waypoint> entries = List.of();

    public WaypointListScreen() {
        super(Component.literal("Waypoints"));
    }

    @Override
    protected void init() {
        refresh();
    }

    private int panelWidth() {
        return Math.min(700, Math.max(420, width - 24));
    }

    private int panelLeft() {
        return (width - panelWidth()) / 2;
    }

    private int listTop() {
        return 50;
    }

    private int visibleRows() {
        return Math.max(1, (height - 104) / ROW_HEIGHT);
    }

    private void refresh() {
        clearWidgets();
        entries = WaypointClientData.getWaypoints();

        int rows = visibleRows();
        int maxScroll = Math.max(0, entries.size() - rows);
        scroll = Math.max(0, Math.min(scroll, maxScroll));

        int left = panelLeft();
        int right = left + panelWidth() - 10;
        int top = listTop();

        for (int i = 0; i < rows; i++) {
            int index = scroll + i;
            if (index >= entries.size()) break;

            WaypointSavedData.Waypoint waypoint = entries.get(index);
            int y = top + i * ROW_HEIGHT;
            int actionWidth = 76;
            int gap = 4;

            addRenderableWidget(Button.builder(
                            Component.literal(waypoint.visible() ? "Ocultar" : "Mostrar"),
                            button -> WaypointNetwork.toggle(waypoint.id()))
                    .bounds(right - actionWidth * 3 - gap * 2, y + 12, actionWidth, 20)
                    .build());

            addRenderableWidget(Button.builder(
                            Component.literal("Editar"),
                            button -> openEditor(waypoint))
                    .bounds(right - actionWidth * 2 - gap, y + 12, actionWidth, 20)
                    .build());

            addRenderableWidget(Button.builder(
                            Component.literal("Eliminar"),
                            button -> WaypointNetwork.delete(waypoint.id()))
                    .bounds(right - actionWidth, y + 12, actionWidth, 20)
                    .build());
        }

        addRenderableWidget(Button.builder(Component.literal("Cerrar"), button -> onClose())
                .bounds(width / 2 - 50, height - 30, 100, 20)
                .build());
    }

    private void openEditor(WaypointSavedData.Waypoint waypoint) {
        String trackedPlayerName = "";
        if (waypoint.trackedPlayer() != null && minecraft != null && minecraft.level != null) {
            for (Entity entity : minecraft.level.entitiesForRendering()) {
                if (entity.getUUID().equals(waypoint.trackedPlayer())
                        && entity instanceof net.minecraft.world.entity.player.Player player) {
                    trackedPlayerName = player.getGameProfile().getName();
                    break;
                }
            }
        }

        minecraft.setScreen(new WaypointScreen(
                waypoint.id(),
                waypoint.commandId(),
                waypoint.name(),
                waypoint.dimension(),
                waypoint.x(),
                waypoint.y(),
                waypoint.z(),
                waypoint.color(),
                waypoint.icon(),
                waypoint.corner(),
                waypoint.tracksPlayer(),
                trackedPlayerName
        ));
    }

    @Override
    public void tick() {
        super.tick();
        List<WaypointSavedData.Waypoint> latest = WaypointClientData.getWaypoints();
        if (!latest.equals(entries)) refresh();
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        int top = listTop();
        int bottom = height - 48;
        if (mouseY >= top && mouseY <= bottom) {
            int maxScroll = Math.max(0, entries.size() - visibleRows());
            int direction = delta > 0 ? -1 : 1;
            int next = Math.max(0, Math.min(maxScroll, scroll + direction));
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

        int left = panelLeft();
        int right = left + panelWidth();
        graphics.fill(left, 16, right, height - 16, 0xE8141B24);
        graphics.fill(left, 16, right, 18, 0xFF54D6FF);
        graphics.drawCenteredString(font, title, width / 2, 25, 0xFFFFFFFF);
        graphics.drawCenteredString(
                font,
                entries.isEmpty() ? "No hay waypoints creados." : "Desplázate para ver más",
                width / 2,
                36,
                0xFF9FAAB8
        );

        int top = listTop();
        int rows = visibleRows();
        int actionSpace = 76 * 3 + 8 + 16;
        int textWidth = panelWidth() - actionSpace;

        for (int i = 0; i < rows; i++) {
            int index = scroll + i;
            if (index >= entries.size()) break;

            WaypointSavedData.Waypoint waypoint = entries.get(index);
            int y = top + i * ROW_HEIGHT;
            int rowLeft = left + 8;

            graphics.fill(
                    rowLeft,
                    y,
                    right - 8,
                    y + ROW_HEIGHT - 4,
                    i % 2 == 0 ? 0x661B232C : 0x55202731
            );

            String type = waypoint.tracksPlayer() ? "Tracker" : "Waypoint";
            String state = waypoint.visible() ? "Activo" : "Oculto";
            String titleText = trimToWidth(
                    waypoint.name() + "  •  " + type + "  •  " + state,
                    textWidth
            );
            graphics.drawString(font, titleText, rowLeft + 6, y + 5, 0xFFFFFFFF);

            String details = waypoint.dimension() + "  •  "
                    + Math.round(waypoint.x()) + ", "
                    + Math.round(waypoint.y()) + ", "
                    + Math.round(waypoint.z());
            graphics.drawString(
                    font,
                    trimToWidth(details, textWidth),
                    rowLeft + 6,
                    y + 21,
                    0xFF9FAAB8
            );
        }

        super.render(graphics, mouseX, mouseY, partialTick);
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

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
