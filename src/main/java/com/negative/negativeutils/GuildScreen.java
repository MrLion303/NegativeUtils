package com.negative.negativeutils;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.Locale;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.client.resources.DefaultPlayerSkin;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

public class GuildScreen extends Screen {
    private static final DateTimeFormatter MESSAGE_TIME =
            DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")
                    .withZone(ZoneId.systemDefault());
    private static final int DARK = 0xFF303030;
    private static final int BLACK = 0xFF17191D;
    private static final int BORDER = 0xFFB8BCC6;
    private static final int ACCENT = 0xFF72CDE5;

    private enum Tab {
        CHAT("Chat", Items.WRITABLE_BOOK),
        BOARD("Tablón", Items.OAK_SIGN),
        LOCATIONS("Ubicaciones", Items.COMPASS),
        BANNER("Estandarte", Items.WHITE_BANNER),
        MEMBERS("Miembros", Items.PLAYER_HEAD),
        PLAYERS("Jugadores online", Items.SPYGLASS);

        private final String label;
        private final ItemStack icon;

        Tab(String label, net.minecraft.world.item.Item item) {
            this.label = label;
            this.icon = new ItemStack(item);
        }
    }

    private Tab selectedTab = Tab.CHAT;

    private EditBox guildNameInput;
    private EditBox searchInput;
    private EditBox chatInput;
    private EditBox announcementTitleInput;
    private EditBox announcementInput;
    private EditBox locationInput;
    private EditBox playerSearchInput;

    private int selectedColor = 0x72CDE5;
    private int listScroll;
    private String statusMessage = "";
    private boolean previousHasGuild;
    private boolean confirmDelete;
    private int announcementPage;
    private int playerScroll;
    private int invitationScroll;
    private int rosterRefreshTicks;

    private int frameX;
    private int frameY;
    private int frameWidth;
    private int frameHeight;
    private int mainX;
    private int mainY;
    private int mainWidth;
    private int mainHeight;
    private int membersX;
    private int membersWidth;
    private int wheelX;
    private int wheelY;

    private static final int WHEEL_RADIUS = 38;

    public GuildScreen() {
        super(Component.literal("Hermandades"));
    }

    @Override
    protected void init() {
        super.init();

        frameWidth = Math.min(width - 24, 820);
        frameHeight = Math.min(height - 28, 460);
        frameWidth = Math.max(300, frameWidth);
        frameHeight = Math.max(220, frameHeight);

        frameX = (width - frameWidth) / 2;
        frameY = (height - frameHeight) / 2;

        CompoundTag snapshot = GuildClientData.getSnapshot();
        previousHasGuild = snapshot.getBoolean("HasGuild");

        if (previousHasGuild) {
            setupGuildLayout();
            addGuildControls(snapshot);
        } else {
            addNoGuildControls();
        }

        addRenderableWidget(
                Button.builder(
                                Component.literal("Cerrar"),
                                button -> onClose()
                        )
                        .bounds(
                                frameX + frameWidth - 82,
                                frameY + frameHeight - 28,
                                70,
                                20
                        )
                        .build()
        );

        GuildNetwork.requestPanelData();
    }

    private void setupGuildLayout() {
        mainX = frameX + 18;
        mainY = frameY + 86;
        mainWidth = frameWidth - 220;
        mainHeight = frameHeight - 104;
        membersX = mainX + mainWidth + 10;
        membersWidth = frameX + frameWidth - 18 - membersX;
    }

    private void addNoGuildControls() {
        int gap = 16;
        int panelWidth = (frameWidth - 48) / 2;
        int leftX = frameX + 16;
        int rightX = leftX + panelWidth + gap;
        int panelY = frameY + 82;
        int panelHeight = frameHeight - 98;

        guildNameInput = new EditBox(
                font,
                leftX + 12,
                panelY + 42,
                panelWidth - 24,
                20,
                Component.literal("Nombre de la hermandad")
        );
        guildNameInput.setMaxLength(24);
        guildNameInput.setHint(Component.literal("Nombre de la hermandad"));
        addRenderableWidget(guildNameInput);

        wheelX = leftX + 20;
        wheelY = panelY + 112;

        searchInput = new EditBox(
                font,
                rightX + 12,
                panelY + 42,
                panelWidth - 24,
                20,
                Component.literal("Buscar hermandad")
        );
        searchInput.setMaxLength(24);
        searchInput.setHint(Component.literal("Buscar por nombre..."));
        searchInput.setResponder(value -> listScroll = 0);
        addRenderableWidget(searchInput);

        addRenderableWidget(
                Button.builder(
                                Component.literal("Crear hermandad"),
                                button -> createGuild()
                        )
                        .bounds(
                                leftX + 12,
                                panelY + panelHeight - 30,
                                panelWidth - 24,
                                20
                        )
                        .build()
        );
    }

    private void addGuildControls(CompoundTag snapshot) {
        GuildSavedData.Guild guild = null;
        CompoundTag ownGuild = snapshot.getCompound("OwnGuild");
        UUID localId = Minecraft.getInstance().player == null
                ? null
                : Minecraft.getInstance().player.getUUID();

        boolean isManager = false;
        boolean isLeader = false;

        if (localId != null) {
            ListTag members = ownGuild.getList(
                    "Members",
                    CompoundTag.TAG_COMPOUND
            );

            for (int i = 0; i < members.size(); i++) {
                CompoundTag member = members.getCompound(i);

                if (!localId.toString().equals(member.getString("Id"))) {
                    continue;
                }

                String rank = member.getString("Rank");
                isManager = rank.equals("LEADER")
                        || rank.equals("OFFICER");
                isLeader = rank.equals("LEADER");
                break;
            }
        }

        int fieldX = mainX + 10;
        int fieldWidth = Math.max(80, mainWidth - 112);
        int buttonX = fieldX + fieldWidth + 6;

        switch (selectedTab) {
            case CHAT -> {
                chatInput = new EditBox(
                        font,
                        fieldX,
                        mainY + mainHeight - 39,
                        fieldWidth,
                        20,
                        Component.literal("Escribe a la hermandad...")
                );
                chatInput.setMaxLength(240);
                chatInput.setHint(Component.literal("Escribe un mensaje para el chat de la hermandad..."));
                addRenderableWidget(chatInput);

                addRenderableWidget(
                        Button.builder(
                                        Component.literal("Enviar"),
                                        button -> sendChat()
                                )
                                .bounds(
                                        buttonX,
                                        mainY + mainHeight - 39,
                                        96,
                                        20
                                )
                                .build()
                );
            }

            case BOARD -> {
                if (isManager) {
                    announcementTitleInput = new EditBox(
                            font,
                            fieldX,
                            mainY + mainHeight - 64,
                            fieldWidth,
                            20,
                            Component.literal("Título del anuncio")
                    );
                    announcementTitleInput.setMaxLength(64);
                    announcementTitleInput.setHint(Component.literal("Título breve del anuncio"));
                    addRenderableWidget(announcementTitleInput);

                    announcementInput = new EditBox(
                            font,
                            fieldX,
                            mainY + mainHeight - 39,
                            fieldWidth,
                            20,
                            Component.literal("Escribe el mensaje del anuncio")
                    );
                    announcementInput.setMaxLength(500);
                    announcementInput.setHint(Component.literal("Escribe aquí el mensaje del anuncio"));
                    addRenderableWidget(announcementInput);

                    addRenderableWidget(
                            Button.builder(
                                            Component.literal("Enviar"),
                                            button -> saveAnnouncement()
                                    )
                                    .bounds(
                                            buttonX,
                                            mainY + mainHeight - 39,
                                            96,
                                            20
                                    )
                                    .build()
                    );
                }
            }

            case LOCATIONS -> {
                if (isManager) {
                    locationInput = new EditBox(
                            font,
                            fieldX,
                            mainY + mainHeight - 39,
                            fieldWidth,
                            20,
                            Component.literal("Nombre de la ubicación")
                    );
                    locationInput.setMaxLength(32);
                    locationInput.setHint(Component.literal("Nombre de la ubicación que guardarás"));
                    addRenderableWidget(locationInput);

                    addRenderableWidget(
                            Button.builder(
                                            Component.literal("Guardar aquí"),
                                            button -> saveLocation()
                                    )
                                    .bounds(
                                            buttonX,
                                            mainY + mainHeight - 39,
                                            96,
                                            20
                                    )
                                    .build()
                    );
                }
            }

            case BANNER -> {
                if (isManager) {
                    addRenderableWidget(
                            Button.builder(
                                            Component.literal(
                                                    "Guardar estandarte en mano"
                                            ),
                                            button -> GuildActionsNetwork
                                                    .saveBannerFromHand()
                                    )
                                    .bounds(
                                            mainX + 10,
                                            mainY + mainHeight - 30,
                                            mainWidth - 20,
                                            20
                                    )
                                    .build()
                    );
                }
            }

            case MEMBERS -> {
                if (isLeader) {
                    String label = confirmDelete
                            ? "Confirmar borrar hermandad"
                            : "Borrar hermandad";

                    addRenderableWidget(
                            Button.builder(
                                            Component.literal(label),
                                            button -> {
                                                if (confirmDelete) {
                                                    GuildActionsNetwork
                                                            .deleteGuild();
                                                } else {
                                                    confirmDelete = true;
                                                    clearWidgets();
                                                    init();
                                                }
                                            }
                                    )
                                    .bounds(
                                            mainX + 10,
                                            mainY + mainHeight - 30,
                                            mainWidth - 20,
                                            20
                                    )
                                    .build()
                    );
                } else {
                    addRenderableWidget(
                            Button.builder(
                                            Component.literal(
                                                    "Salir de la hermandad"
                                            ),
                                            button -> GuildActionsNetwork
                                                    .leaveGuild()
                                    )
                                    .bounds(
                                            mainX + 10,
                                            mainY + mainHeight - 30,
                                            mainWidth - 20,
                                            20
                                    )
                                    .build()
                    );
                }
            }

            case PLAYERS -> {
                playerSearchInput = new EditBox(
                        font,
                        mainX + 10,
                        mainY + mainHeight - 31,
                        mainWidth - 20,
                        20,
                        Component.literal("Buscar jugador online")
                );
                playerSearchInput.setMaxLength(16);
                playerSearchInput.setHint(Component.literal("Buscar jugador online..."));
                playerSearchInput.setResponder(value -> playerScroll = 0);
                addRenderableWidget(playerSearchInput);
            }
        }
    }

    private void createGuild() {
        if (guildNameInput == null) {
            return;
        }

        String name = guildNameInput.getValue().trim();

        if (name.isBlank()) {
            statusMessage = "Escribe un nombre para la hermandad.";
            return;
        }

        GuildNetwork.sendCreateRequest(name, selectedColor);
        statusMessage = "Creando hermandad...";
    }

    private void sendChat() {
        if (chatInput == null) {
            return;
        }

        String text = chatInput.getValue().trim();

        if (text.isBlank()) {
            return;
        }

        GuildActionsNetwork.sendChat(text);
        chatInput.setValue("");
    }

    private void saveAnnouncement() {
        if (announcementTitleInput == null || announcementInput == null) {
            return;
        }

        String title = announcementTitleInput.getValue().trim();
        String message = announcementInput.getValue().trim();
        if (title.isBlank() || message.isBlank()) {
            statusMessage = "Escribe el título y el mensaje del anuncio.";
            return;
        }

        GuildActionsNetwork.saveAnnouncement(
                title,
                message
        );
        announcementTitleInput.setValue("");
        announcementInput.setValue("");
        announcementPage = 0;
        statusMessage = "Enviando anuncio...";
    }

    private void saveLocation() {
        if (locationInput == null) {
            return;
        }

        String name = locationInput.getValue().trim();

        if (name.isBlank()) {
            statusMessage = "Escribe un nombre para la ubicación.";
            return;
        }

        GuildActionsNetwork.saveLocation(name);
        locationInput.setValue("");
        statusMessage = "Guardando ubicación...";
    }

    private void selectTab(Tab tab) {
        selectedTab = tab;
        statusMessage = "";
        clearWidgets();
        init();
    }

    @Override
    public void tick() {
        super.tick();

        if (selectedTab == Tab.PLAYERS
                && GuildClientData.getSnapshot().getBoolean("HasGuild")
                && ++rosterRefreshTicks >= 60) {
            rosterRefreshTicks = 0;
            GuildNetwork.requestPanelData();
        }

        boolean hasGuild = GuildClientData.getSnapshot()
                .getBoolean("HasGuild");

        if (hasGuild != previousHasGuild) {
            clearWidgets();
            init();
        }
    }

    @Override
    public void render(
            GuiGraphics graphics,
            int mouseX,
            int mouseY,
            float partialTick
    ) {
        renderBackground(graphics);
        NegativeUtilsGuiStyle.renderFrame(graphics, width, height);
        drawOuterFrame(graphics);

        CompoundTag snapshot = GuildClientData.getSnapshot();

        if (snapshot.getBoolean("HasGuild")) {
            drawGuildPanel(graphics, snapshot);
        } else {
            drawNoGuildPanel(graphics);
        }

        if (!statusMessage.isBlank()) {
            graphics.drawCenteredString(
                    font,
                    statusMessage,
                    width / 2,
                    frameY + frameHeight - 8,
                    0xFFFFFF55
            );
        }

        super.render(graphics, mouseX, mouseY, partialTick);
    }

    private void drawOuterFrame(GuiGraphics graphics) {
        graphics.fill(
                frameX,
                frameY,
                frameX + frameWidth,
                frameY + frameHeight,
                0xFFB9BEC9
        );
        graphics.renderOutline(
                frameX,
                frameY,
                frameWidth,
                frameHeight,
                0xFF15171B
        );

        graphics.fill(
                frameX + 5,
                frameY + 5,
                frameX + frameWidth - 5,
                frameY + frameHeight - 5,
                0xFF363A43
        );
        graphics.renderOutline(
                frameX + 5,
                frameY + 5,
                frameWidth - 10,
                frameHeight - 10,
                0xFF696F7B
        );

        graphics.fill(
                frameX + 12,
                frameY + 68,
                frameX + frameWidth - 12,
                frameY + frameHeight - 14,
                0xFF181A1F
        );

        graphics.fill(
                frameX + 18,
                frameY + 4,
                frameX + 116,
                frameY + 8,
                0xFFCE202A
        );
        graphics.fill(
                frameX + frameWidth - 116,
                frameY + 4,
                frameX + frameWidth - 18,
                frameY + 8,
                0xFFCE202A
        );
    }

    private void drawNoGuildPanel(GuiGraphics graphics) {
        int panelWidth = (frameWidth - 48) / 2;
        int leftX = frameX + 16;
        int rightX = leftX + panelWidth + 16;
        int panelY = frameY + 82;
        int panelHeight = frameHeight - 98;

        drawPanel(graphics, leftX, panelY, panelWidth, panelHeight);
        drawPanel(graphics, rightX, panelY, panelWidth, panelHeight);

        graphics.drawCenteredString(
                font,
                "Crear hermandad",
                leftX + panelWidth / 2,
                panelY + 12,
                ACCENT
        );

        graphics.drawString(
                font,
                "Líder:",
                leftX + 14,
                panelY + 70,
                0xFFCCCCCC
        );

        String playerName = Minecraft.getInstance().player == null
                ? ""
                : Minecraft.getInstance()
                        .player
                        .getGameProfile()
                        .getName();

        graphics.drawString(
                font,
                playerName,
                leftX + 52,
                panelY + 70,
                0xFFFFFFFF
        );

        graphics.drawString(
                font,
                "Color:",
                leftX + 14,
                panelY + 91,
                0xFFCCCCCC
        );

        drawColorWheel(graphics, leftX);
        drawIncomingInvitations(graphics, leftX, panelY, panelWidth);

        graphics.drawCenteredString(
                font,
                "Buscar hermandad",
                rightX + panelWidth / 2,
                panelY + 12,
                ACCENT
        );

        drawGuildCatalog(graphics, rightX, panelY, panelWidth, panelHeight);
    }

    private void drawIncomingInvitations(
            GuiGraphics graphics,
            int x,
            int panelY,
            int panelWidth
    ) {
        ListTag invitations = GuildClientData.getSnapshot()
                .getCompound("InvitationInbox")
                .getList("Invitations", CompoundTag.TAG_COMPOUND);
        if (invitations.isEmpty()) return;
        graphics.drawString(font, "Invitaciones recibidas", x + 12, panelY + 199, ACCENT);
        int visible = Math.min(3, invitations.size());
        int maxScroll = Math.max(0, invitations.size() - visible);
        invitationScroll = Math.max(0, Math.min(invitationScroll, maxScroll));
        for (int row = 0; row < visible; row++) {
            int index = invitationScroll + row;
            CompoundTag invitation = invitations.getCompound(index);
            int rowY = panelY + 214 + row * 26;
            graphics.fill(x + 8, rowY, x + panelWidth - 8, rowY + 23, BLACK);
            graphics.drawString(
                    font,
                    font.plainSubstrByWidth("<" + invitation.getString("GuildName") + ">", panelWidth - 132),
                    x + 14,
                    rowY + 7,
                    invitation.getInt("Color") | 0xFF000000
            );
            drawSmallAction(graphics, x + panelWidth - 82, rowY + 2, "Sí", 0xFF8DD694);
            drawSmallAction(graphics, x + panelWidth - 43, rowY + 2, "No", 0xFFFF8E8E);
        }
    }

    private void drawSmallAction(
            GuiGraphics graphics,
            int x,
            int y,
            String label,
            int color
    ) {
        graphics.fill(x, y, x + 34, y + 19, 0xFF353B44);
        graphics.renderOutline(x, y, 34, 19, 0xFF858B94);
        graphics.drawCenteredString(font, label, x + 17, y + 6, color);
    }

    private void drawGuildPanel(
            GuiGraphics graphics,
            CompoundTag snapshot
    ) {
        CompoundTag guild = snapshot.getCompound("OwnGuild");
        int guildColor = guild.getInt("Color") & 0xFFFFFF;

        drawTabs(graphics);

        int badgeWidth = Math.min(180, frameWidth / 3);
        int badgeX = frameX + (frameWidth - badgeWidth) / 2;
        int badgeY = frameY + 38;

        drawHeaderBanner(graphics, guild);

        graphics.fill(
                badgeX,
                badgeY,
                badgeX + badgeWidth,
                badgeY + 26,
                0xFF000000 | lightenColor(guildColor, 0.72F)
        );
        graphics.drawCenteredString(
                font,
                "<" + guild.getString("Name") + ">",
                badgeX + badgeWidth / 2,
                badgeY + 8,
                guildColor
        );

        graphics.fill(
                frameX + frameWidth - 156,
                frameY + 47,
                frameX + frameWidth - 28,
                frameY + 66,
                0xFF252930
        );
        graphics.renderOutline(
                frameX + frameWidth - 156,
                frameY + 47,
                128,
                19,
                0xFF777E89
        );
        graphics.drawCenteredString(
                font,
                "En línea: " + countOnline(guild) + "/"
                        + guild.getList(
                                "Members",
                                CompoundTag.TAG_COMPOUND
                        ).size(),
                frameX + frameWidth - 92,
                frameY + 53,
                0xFFFFFFFF
        );

        drawPanel(graphics, mainX, mainY, mainWidth, mainHeight);
        drawPanel(graphics, membersX, mainY, membersWidth, mainHeight);

        drawTabContent(graphics, guild);
        drawMembers(graphics, guild);
    }

    private void drawTabs(GuiGraphics graphics) {
        int size = 34;
        int gap = 7;
        int startX = frameX + 18;
        int y = frameY + 20;

        for (int i = 0; i < Tab.values().length; i++) {
            Tab tab = Tab.values()[i];
            int x = tab == Tab.PLAYERS
                    ? frameX + frameWidth - size - 18
                    : startX + i * (size + gap);

            graphics.fill(
                    x,
                    y,
                    x + size,
                    y + size,
                    tab == selectedTab ? 0xFFB8D6E5 : 0xFF747B87
            );
            graphics.renderOutline(
                    x,
                    y,
                    size,
                    size,
                    0xFF1B1D22
            );
            graphics.fill(
                    x + 4,
                    y + 4,
                    x + size - 4,
                    y + size - 4,
                    0xFF252930
            );
            graphics.renderItem(tab.icon, x + 9, y + 9);
        }
    }

    private void drawHeaderBanner(GuiGraphics graphics, CompoundTag guild) {
        CompoundTag bannerTag = guild.getCompound("Banner");
        if (bannerTag.isEmpty()) {
            return;
        }

        ItemStack banner = ItemStack.of(bannerTag);
        if (banner.isEmpty()) {
            return;
        }

        int centerX = frameX + frameWidth / 2;
        int x = centerX - 31;
        int y = frameY - 30;
        graphics.fill(x, y, x + 62, y + 66, 0xFF20242A);
        graphics.renderOutline(x, y, 62, 66, 0xFFB8BCC6);
        graphics.pose().pushPose();
        graphics.pose().translate(centerX - 24, frameY - 22, 0.0D);
        graphics.pose().scale(3.0F, 3.0F, 1.0F);
        graphics.renderItem(banner, 0, 0);
        graphics.pose().popPose();
    }

    private void drawTabContent(
            GuiGraphics graphics,
            CompoundTag guild
    ) {
        switch (selectedTab) {
            case CHAT -> drawChat(graphics, guild);
            case BOARD -> drawBoard(graphics, guild);
            case LOCATIONS -> drawLocations(graphics, guild);
            case BANNER -> drawBanner(graphics, guild);
            case MEMBERS -> drawMemberSummary(graphics, guild);
            case PLAYERS -> drawOnlinePlayers(graphics);
        }
    }

    private int lightenColor(int color, float amount) {
        int red = (color >> 16) & 0xFF;
        int green = (color >> 8) & 0xFF;
        int blue = color & 0xFF;
        red += Math.round((255 - red) * amount);
        green += Math.round((255 - green) * amount);
        blue += Math.round((255 - blue) * amount);
        return (Math.min(red, 255) << 16)
                | (Math.min(green, 255) << 8)
                | Math.min(blue, 255);
    }

    private void drawOnlinePlayers(GuiGraphics graphics) {
        graphics.drawCenteredString(font, "Jugadores online", mainX + mainWidth / 2, mainY + 10, ACCENT);
        ListTag players = getFilteredOnlinePlayers();
        int listY = mainY + 32;
        int rowHeight = 32;
        int bottom = mainY + mainHeight - 42;
        int visibleRows = Math.max(1, (bottom - listY) / rowHeight);
        int maxScroll = Math.max(0, players.size() - visibleRows);
        playerScroll = Math.max(0, Math.min(playerScroll, maxScroll));
        if (players.isEmpty()) {
            graphics.drawString(font, "No hay jugadores que coincidan con la búsqueda.", mainX + 14, listY + 8, 0xFFCCCCCC);
            return;
        }

        int end = Math.min(players.size(), playerScroll + visibleRows);
        CompoundTag snapshot = GuildClientData.getSnapshot();
        boolean canManage = snapshot.getBoolean("CanManageGuild");
        boolean isLeader = snapshot.getBoolean("IsGuildLeader");
        for (int index = playerScroll; index < end; index++) {
            CompoundTag player = players.getCompound(index);
            int rowY = listY + (index - playerScroll) * rowHeight;
            boolean member = player.getBoolean("Member");
            graphics.fill(mainX + 10, rowY, mainX + mainWidth - 10, rowY + 29,
                    member ? 0xFF25282D : DARK);
            try {
                drawPlayerHead(graphics, UUID.fromString(player.getString("Id")), mainX + 15, rowY + 5);
            } catch (IllegalArgumentException ignored) {
                // Continúa mostrando el nombre del jugador.
            }
            graphics.drawString(font, player.getString("Name"), mainX + 41, rowY + 4,
                    member ? 0xFFAAAAAA : 0xFFFFFFFF);

            String state;
            if (member) {
                state = player.getBoolean("Self")
                        ? "Es un miembro · este eres tú"
                        : "Es un miembro";
            } else if (player.getBoolean("Requested")) {
                state = isLeader ? "Ha solicitado entrar" : "Solicitud enviada al líder";
            } else if (player.getBoolean("Invited")) {
                state = "Ya ha sido invitado";
            } else {
                state = canManage ? "" : "Solo líderes y oficiales pueden invitar";
            }
            graphics.drawString(font, font.plainSubstrByWidth(state, mainWidth - 185),
                    mainX + 41, rowY + 16, member ? 0xFF9A9A9A : 0xFFBFC5CC);

            int acceptX = mainX + mainWidth - 112;
            int rejectX = mainX + mainWidth - 58;
            if (!member && player.getBoolean("Requested") && isLeader) {
                drawWideAction(graphics, acceptX, rowY + 5, 50, "Aceptar", 0xFF8DD694);
                drawWideAction(graphics, rejectX, rowY + 5, 50, "Rechazar", 0xFFFF8E8E);
            } else if (!member && !player.getBoolean("Requested")
                    && !player.getBoolean("Invited") && canManage) {
                drawWideAction(graphics, mainX + mainWidth - 94, rowY + 5, 80, "Invitar", 0xFF8DD694);
            }
        }
    }

    private void drawWideAction(
            GuiGraphics graphics,
            int x,
            int y,
            int buttonWidth,
            String label,
            int color
    ) {
        graphics.fill(x, y, x + buttonWidth, y + 19, 0xFF353B44);
        graphics.renderOutline(x, y, buttonWidth, 19, 0xFF858B94);
        graphics.drawCenteredString(font, label, x + buttonWidth / 2, y + 6, color);
    }

    private ListTag getFilteredOnlinePlayers() {
        ListTag source = GuildClientData.getSnapshot().getList("OnlinePlayers", CompoundTag.TAG_COMPOUND);
        String filter = playerSearchInput == null
                ? ""
                : playerSearchInput.getValue().trim().toLowerCase(Locale.ROOT);
        ListTag filtered = new ListTag();
        for (int i = 0; i < source.size(); i++) {
            CompoundTag player = source.getCompound(i);
            if (player.getString("Name").toLowerCase(Locale.ROOT).contains(filter)) {
                filtered.add(player.copy());
            }
        }
        return filtered;
    }

    private boolean clickIncomingInvitations(double mouseX, double mouseY) {
        ListTag invitations = GuildClientData.getSnapshot()
                .getCompound("InvitationInbox")
                .getList("Invitations", CompoundTag.TAG_COMPOUND);
        int panelWidth = (frameWidth - 48) / 2;
        int leftX = frameX + 16;
        int panelY = frameY + 82;
        int visible = Math.min(3, invitations.size());
        for (int row = 0; row < visible; row++) {
            int index = invitationScroll + row;
            if (index >= invitations.size()) break;
            int rowY = panelY + 214 + row * 26;
            if (mouseY < rowY || mouseY >= rowY + 23) continue;
            CompoundTag invite = invitations.getCompound(index);
            try {
                UUID guildId = UUID.fromString(invite.getString("GuildId"));
                if (mouseX >= leftX + panelWidth - 82 && mouseX < leftX + panelWidth - 48) {
                    GuildActionsNetwork.acceptInvitation(guildId);
                    return true;
                }
                if (mouseX >= leftX + panelWidth - 43 && mouseX < leftX + panelWidth - 9) {
                    GuildActionsNetwork.rejectInvitation(guildId);
                    return true;
                }
            } catch (IllegalArgumentException ignored) {
                return true;
            }
        }
        return false;
    }

    private boolean clickOnlinePlayer(double mouseX, double mouseY) {
        ListTag players = getFilteredOnlinePlayers();
        int listY = mainY + 32;
        int rowHeight = 32;
        int bottom = mainY + mainHeight - 42;
        int visibleRows = Math.max(1, (bottom - listY) / rowHeight);
        int end = Math.min(players.size(), playerScroll + visibleRows);
        CompoundTag snapshot = GuildClientData.getSnapshot();
        boolean canManage = snapshot.getBoolean("CanManageGuild");
        boolean isLeader = snapshot.getBoolean("IsGuildLeader");
        for (int index = playerScroll; index < end; index++) {
            int rowY = listY + (index - playerScroll) * rowHeight;
            if (mouseY < rowY || mouseY >= rowY + 29) continue;
            CompoundTag target = players.getCompound(index);
            if (target.getBoolean("Member")) return true;
            try {
                UUID targetId = UUID.fromString(target.getString("Id"));
                int acceptX = mainX + mainWidth - 112;
                int rejectX = mainX + mainWidth - 58;
                if (target.getBoolean("Requested") && isLeader) {
                    UUID guildId = UUID.fromString(snapshot.getCompound("OwnGuild").getString("Id"));
                    if (mouseX >= acceptX && mouseX < acceptX + 50) {
                        GuildNetwork.acceptJoinRequest(guildId, targetId);
                        return true;
                    }
                    if (mouseX >= rejectX && mouseX < rejectX + 50) {
                        GuildNetwork.rejectJoinRequest(guildId, targetId);
                        return true;
                    }
                } else if (!target.getBoolean("Requested")
                        && !target.getBoolean("Invited") && canManage
                        && mouseX >= mainX + mainWidth - 94
                        && mouseX < mainX + mainWidth - 14) {
                    GuildActionsNetwork.invitePlayer(targetId);
                    statusMessage = "Invitación enviada.";
                    return true;
                }
            } catch (IllegalArgumentException ignored) {
                return true;
            }
        }
        return false;
    }

    private void drawChat(
            GuiGraphics graphics,
            CompoundTag guild
    ) {
        int x = mainX + 10;
        int y = mainY + 10;
        int w = mainWidth - 20;
        int h = mainHeight - 62;

        graphics.fill(x, y, x + w, y + 38, DARK);
        graphics.renderOutline(x, y, w, 38, BORDER);

        graphics.drawString(font, "MOTD:", x + 8, y + 13, ACCENT);

        String motd = guild.getString("Motd");
        graphics.drawString(
                font,
                motd.isBlank() ? "Bienvenidos a la hermandad" : motd,
                x + 54,
                y + 13,
                0xFFFFFFFF
        );

        int chatY = y + 47;
        int chatHeight = Math.max(40, h - 47);

        graphics.fill(
                x,
                chatY,
                x + w,
                chatY + chatHeight,
                DARK
        );
        graphics.renderOutline(x, chatY, w, chatHeight, BORDER);

        ListTag messages = guild.getList(
                "ChatMessages",
                CompoundTag.TAG_COMPOUND
        );

        if (messages.isEmpty()) {
            graphics.drawString(
                    font,
                    "El chat de la hermandad aparecerá aquí.",
                    x + 8,
                    chatY + 8,
                    0xFFCCCCCC
            );
            return;
        }

        int maxLines = Math.max(1, (chatHeight - 12) / 12);
        int firstLine = Math.max(0, messages.size() - maxLines);
        int lineY = chatY + 6;

        for (int i = firstLine; i < messages.size(); i++) {
            CompoundTag message = messages.getCompound(i);
            String line = "[" + formatMessageTime(message.getLong("Timestamp")) + "] "
                    + message.getString("Sender")
                    + ": "
                    + message.getString("Text");

            graphics.drawString(
                    font,
                    font.plainSubstrByWidth(line, w - 14),
                    x + 7,
                    lineY,
                    0xFFFFFFFF
            );

            lineY += 12;
        }
    }

    private void drawBoard(
            GuiGraphics graphics,
            CompoundTag guild
    ) {
        graphics.drawCenteredString(
                font,
                "Tablón de anuncios",
                mainX + mainWidth / 2,
                mainY + 14,
                ACCENT
        );

        ListTag announcements = guild.getList(
                "Announcements",
                CompoundTag.TAG_COMPOUND
        );
        if (announcements.isEmpty()) {
            graphics.drawString(
                    font,
                    "Todavía no hay anuncios.",
                    mainX + 14,
                    mainY + 42,
                    0xFFCCCCCC
            );
            return;
        }

        announcementPage = Math.max(
                0,
                Math.min(announcementPage, announcements.size() - 1)
        );
        int announcementIndex = announcements.size() - 1 - announcementPage;
        CompoundTag announcement = announcements.getCompound(announcementIndex);
        int cardX = mainX + 10;
        int cardY = mainY + 36;
        int cardW = mainWidth - 20;
        int cardH = Math.max(70, mainHeight - 150);
        graphics.fill(cardX, cardY, cardX + cardW, cardY + cardH, DARK);
        graphics.renderOutline(cardX, cardY, cardW, cardH, BORDER);
        graphics.drawString(
                font,
                font.plainSubstrByWidth(
                        announcement.getString("Title"),
                        cardW - 20
                ),
                cardX + 10,
                cardY + 9,
                0xFFFFFFFF
        );
        graphics.fill(cardX + 8, cardY + 23, cardX + cardW - 8, cardY + 24, 0xFF60656E);
        int bodyY = cardY + 31;
        int bodyBottom = cardY + cardH - 40;
        for (String line : wrapText(announcement.getString("Text"), cardW - 20)) {
            if (bodyY + 9 > bodyBottom) {
                break;
            }
            graphics.drawString(font, line, cardX + 10, bodyY, 0xFFE4E4E4);
            bodyY += 12;
        }
        String byline = announcement.getString("Author")
                + "  ·  "
                + formatMessageTime(announcement.getLong("Timestamp"));
        graphics.drawString(
                font,
                byline,
                cardX + 10,
                cardY + cardH - 28,
                0xFFFFB347
        );

        int pageY = cardY + cardH + 4;
        drawPageButton(graphics, cardX + cardW / 2 - 64, pageY, "<", announcementPage < announcements.size() - 1);
        graphics.drawCenteredString(
                font,
                "Anuncio " + (announcements.size() - announcementPage)
                        + " / " + announcements.size(),
                cardX + cardW / 2,
                pageY + 5,
                ACCENT
        );
        drawPageButton(graphics, cardX + cardW / 2 + 48, pageY, ">", announcementPage > 0);
    }

    private List<String> wrapText(String text, int maxWidth) {
        List<String> lines = new ArrayList<>();
        String remaining = text == null ? "" : text.trim();
        while (!remaining.isEmpty()) {
            String line = font.plainSubstrByWidth(remaining, maxWidth);
            if (line.isEmpty()) {
                break;
            }
            lines.add(line);
            remaining = remaining.substring(line.length()).stripLeading();
        }
        return lines;
    }

    private void drawPageButton(
            GuiGraphics graphics,
            int x,
            int y,
            String label,
            boolean enabled
    ) {
        graphics.fill(x, y, x + 20, y + 18, enabled ? 0xFF434A54 : 0xFF252930);
        graphics.renderOutline(x, y, 20, 18, 0xFF777E89);
        graphics.drawCenteredString(
                font,
                label,
                x + 10,
                y + 5,
                enabled ? 0xFFFFFFFF : 0xFF777777
        );
    }

    private boolean clickAnnouncementPage(double mouseX, double mouseY) {
        CompoundTag guild = GuildClientData.getSnapshot().getCompound("OwnGuild");
        ListTag announcements = guild.getList("Announcements", CompoundTag.TAG_COMPOUND);
        if (announcements.size() < 2) {
            return false;
        }

        int cardX = mainX + 10;
        int cardY = mainY + 36;
        int cardW = mainWidth - 20;
        int cardH = Math.max(70, mainHeight - 150);
        int pageY = cardY + cardH + 4;
        int leftX = cardX + cardW / 2 - 64;
        int rightX = cardX + cardW / 2 + 48;
        if (mouseY < pageY || mouseY >= pageY + 18) {
            return false;
        }
        if (mouseX >= leftX && mouseX < leftX + 20
                && announcementPage < announcements.size() - 1) {
            announcementPage++;
            return true;
        }
        if (mouseX >= rightX && mouseX < rightX + 20
                && announcementPage > 0) {
            announcementPage--;
            return true;
        }
        return false;
    }

    private String formatMessageTime(long timestamp) {
        if (timestamp <= 0L) {
            return "--/--/---- --:--";
        }
        return MESSAGE_TIME.format(Instant.ofEpochMilli(timestamp));
    }

    private void drawLocations(
            GuiGraphics graphics,
            CompoundTag guild
    ) {
        graphics.drawCenteredString(
                font,
                "Ubicaciones",
                mainX + mainWidth / 2,
                mainY + 14,
                ACCENT
        );

        ListTag locations = guild.getList(
                "Locations",
                CompoundTag.TAG_COMPOUND
        );

        if (locations.isEmpty()) {
            graphics.drawString(
                    font,
                    "Todavía no hay ubicaciones guardadas.",
                    mainX + 14,
                    mainY + 42,
                    0xFFCCCCCC
            );
            return;
        }

        int rowY = mainY + 40;

        for (int i = 0; i < locations.size(); i++) {
            CompoundTag location = locations.getCompound(i);

            graphics.fill(
                    mainX + 10,
                    rowY,
                    mainX + mainWidth - 10,
                    rowY + 34,
                    DARK
            );

            graphics.drawString(
                    font,
                    location.getString("Name"),
                    mainX + 18,
                    rowY + 5,
                    ACCENT
            );

            graphics.drawString(
                    font,
                    location.getString("Dimension")
                            + " X:" + location.getInt("X")
                            + " Y:" + location.getInt("Y")
                            + " Z:" + location.getInt("Z"),
                    mainX + 18,
                    rowY + 19,
                    0xFFDDDDDD
            );

            rowY += 39;
        }
    }

    private void drawBanner(
            GuiGraphics graphics,
            CompoundTag guild
    ) {
        graphics.drawCenteredString(
                font,
                "Colección de Estandartes",
                mainX + mainWidth / 2,
                mainY + 14,
                ACCENT
        );

        CompoundTag bannerTag = guild.getCompound("Banner");

        if (bannerTag.isEmpty()) {
            graphics.drawCenteredString(
                    font,
                    "No hay estandarte guardado.",
                    mainX + mainWidth / 2,
                    mainY + 52,
                    0xFFCCCCCC
            );
            return;
        }

        ItemStack banner = ItemStack.of(bannerTag);
        graphics.renderItem(
                banner,
                mainX + mainWidth / 2 - 8,
                mainY + 48
        );
        graphics.drawCenteredString(
                font,
                banner.getHoverName(),
                mainX + mainWidth / 2,
                mainY + 72,
                0xFFFFFFFF
        );
    }

    private void drawMemberSummary(
            GuiGraphics graphics,
            CompoundTag guild
    ) {
        ListTag members = guild.getList(
                "Members",
                CompoundTag.TAG_COMPOUND
        );

        graphics.drawString(
                font,
                "Líder:",
                mainX + 14,
                mainY + 14,
                0xFFFFFFFF
        );

        String leader = "—";

        for (int i = 0; i < members.size(); i++) {
            CompoundTag member = members.getCompound(i);

            if ("LEADER".equals(member.getString("Rank"))) {
                leader = member.getString("Name");
                break;
            }
        }

        graphics.drawString(
                font,
                leader,
                mainX + 60,
                mainY + 14,
                0xFFFFFF55
        );

        graphics.drawString(
                font,
                "Integrantes: " + members.size(),
                mainX + 14,
                mainY + 34,
                0xFFFFFFFF
        );

        drawInbox(graphics, guild);
    }

    private void drawInbox(
            GuiGraphics graphics,
            CompoundTag guild
    ) {
        CompoundTag inbox = GuildClientData.getSnapshot()
                .getCompound("Inbox");

        if (!inbox.getBoolean("HasGuildInbox")) {
            return;
        }

        ListTag requests = inbox.getList(
                "Requests",
                CompoundTag.TAG_COMPOUND
        );

        graphics.drawString(
                font,
                "Solicitudes pendientes:",
                mainX + 14,
                mainY + 62,
                ACCENT
        );

        int rowY = mainY + 82;

        for (int i = 0; i < requests.size(); i++) {
            CompoundTag request = requests.getCompound(i);

            graphics.fill(
                    mainX + 10,
                    rowY,
                    mainX + mainWidth - 10,
                    rowY + 25,
                    BLACK
            );

            graphics.drawString(
                    font,
                    request.getString("PlayerName"),
                    mainX + 16,
                    rowY + 8,
                    0xFFFFFFFF
            );

            graphics.drawString(
                    font,
                    "Aceptar",
                    mainX + mainWidth - 112,
                    rowY + 8,
                    0xFF8DD694
            );

            graphics.drawString(
                    font,
                    "Rechazar",
                    mainX + mainWidth - 58,
                    rowY + 8,
                    0xFFFF8E8E
            );

            rowY += 29;
        }
    }

    private void drawMembers(
            GuiGraphics graphics,
            CompoundTag guild
    ) {
        graphics.drawCenteredString(
                font,
                "Integrantes",
                membersX + membersWidth / 2,
                mainY + 10,
                ACCENT
        );

        ListTag members = guild.getList(
                "Members",
                CompoundTag.TAG_COMPOUND
        );

        int rowY = mainY + 32;

        for (int i = 0; i < members.size(); i++) {
            CompoundTag member = members.getCompound(i);

            graphics.fill(
                    membersX + 6,
                    rowY,
                    membersX + membersWidth - 6,
                    rowY + 30,
                    BLACK
            );

            String rank = member.getString("Rank");
            String symbol = switch (rank) {
                case "LEADER" -> "★ ";
                case "OFFICER" -> "◆ ";
                default -> "";
            };

            try {
                drawPlayerHead(
                        graphics,
                        UUID.fromString(member.getString("Id")),
                        membersX + 9,
                        rowY + 5
                );
            } catch (IllegalArgumentException ignored) {
                // Continúa mostrando el nombre aunque el UUID no sea válido.
            }

            graphics.drawString(
                    font,
                    symbol + member.getString("Name"),
                    membersX + 32,
                    rowY + 3,
                    "LEADER".equals(rank)
                            ? 0xFFFFFF55
                            : 0xFFFFFFFF
            );

            graphics.drawString(
                    font,
                    "<" + guild.getString("Name") + ">",
                    membersX + 32,
                    rowY + 15,
                    guild.getInt("Color") & 0xFFFFFF
            );

            rowY += 33;
        }
    }

    private void drawPlayerHead(
            GuiGraphics graphics,
            UUID playerId,
            int x,
            int y
    ) {
        ResourceLocation skin =
                DefaultPlayerSkin.getDefaultSkin(playerId);

        if (Minecraft.getInstance().getConnection() != null) {
            PlayerInfo info = Minecraft.getInstance()
                    .getConnection()
                    .getPlayerInfo(playerId);

            if (info != null) {
                skin = info.getSkinLocation();
            }
        }

        graphics.blit(
                skin,
                x,
                y,
                20,
                20,
                8.0F,
                8.0F,
                8,
                8,
                64,
                64
        );
        graphics.blit(
                skin,
                x,
                y,
                20,
                20,
                40.0F,
                8.0F,
                8,
                8,
                64,
                64
        );
    }

    private void drawGuildCatalog(
            GuiGraphics graphics,
            int rightX,
            int panelY,
            int panelWidth,
            int panelHeight
    ) {
        List<CatalogEntry> entries = getFilteredGuilds();

        int x = rightX + 12;
        int y = panelY + 72;
        int rowWidth = panelWidth - 24;
        int bottom = panelY + panelHeight - 12;
        int visibleRows = Math.max(1, (bottom - y) / 27);

        if (entries.isEmpty()) {
            graphics.drawString(
                    font,
                    "No se encontraron hermandades.",
                    x + 6,
                    y + 8,
                    0xFFCCCCCC
            );
            return;
        }

        int maxScroll = Math.max(0, entries.size() - visibleRows);
        listScroll = Math.max(0, Math.min(listScroll, maxScroll));

        int end = Math.min(entries.size(), listScroll + visibleRows);

        for (int i = listScroll; i < end; i++) {
            CatalogEntry entry = entries.get(i);
            int rowY = y + (i - listScroll) * 27;

            graphics.fill(
                    x,
                    rowY,
                    x + rowWidth,
                    rowY + 24,
                    DARK
            );

            graphics.drawString(
                    font,
                    "<" + entry.name() + ">",
                    x + 7,
                    rowY + 3,
                    entry.color()
            );

            graphics.drawString(
                    font,
                    entry.memberCount() + " integrantes",
                    x + 7,
                    rowY + 13,
                    0xFFCCCCCC
            );

            graphics.drawString(
                    font,
                    "Solicitar",
                    x + rowWidth - 54,
                    rowY + 7,
                    ACCENT
            );
        }
    }

    private List<CatalogEntry> getFilteredGuilds() {
        CompoundTag snapshot = GuildClientData.getSnapshot();
        ListTag catalog = snapshot.getList(
                "Catalog",
                CompoundTag.TAG_COMPOUND
        );

        String filter = searchInput == null
                ? ""
                : searchInput.getValue().trim().toLowerCase();

        List<CatalogEntry> entries = new ArrayList<>();

        for (int i = 0; i < catalog.size(); i++) {
            CompoundTag entry = catalog.getCompound(i);
            String name = entry.getString("Name");

            if (!name.toLowerCase().contains(filter)) {
                continue;
            }

            try {
                entries.add(
                        new CatalogEntry(
                                UUID.fromString(entry.getString("Id")),
                                name,
                                entry.getInt("Color") & 0xFFFFFF,
                                entry.getInt("MemberCount")
                        )
                );
            } catch (IllegalArgumentException ignored) {
                // Omite una entrada con UUID inválido.
            }
        }

        return entries;
    }

    private void drawColorWheel(
            GuiGraphics graphics,
            int leftX
    ) {
        int centerX = wheelX + WHEEL_RADIUS;
        int centerY = wheelY + WHEEL_RADIUS;

        for (int y = -WHEEL_RADIUS; y <= WHEEL_RADIUS; y += 2) {
            for (int x = -WHEEL_RADIUS; x <= WHEEL_RADIUS; x += 2) {
                float distance = (float) Math.sqrt(x * x + y * y);

                if (distance > WHEEL_RADIUS) {
                    continue;
                }

                float hue = (float) (
                        Math.atan2(y, x) / (Math.PI * 2.0)
                );

                if (hue < 0.0F) {
                    hue += 1.0F;
                }

                float saturation = Math.min(
                        1.0F,
                        distance / WHEEL_RADIUS
                );

                int color = Color.HSBtoRGB(
                        hue,
                        saturation,
                        1.0F
                );

                graphics.fill(
                        centerX + x,
                        centerY + y,
                        centerX + x + 2,
                        centerY + y + 2,
                        color
                );
            }
        }

        graphics.renderOutline(
                centerX - WHEEL_RADIUS,
                centerY - WHEEL_RADIUS,
                WHEEL_RADIUS * 2,
                WHEEL_RADIUS * 2,
                0xFFFFFFFF
        );

        graphics.fill(
                leftX + 112,
                wheelY + 20,
                leftX + 143,
                wheelY + 51,
                0xFF000000 | selectedColor
        );
        graphics.renderOutline(
                leftX + 112,
                wheelY + 20,
                31,
                31,
                0xFFFFFFFF
        );

        graphics.drawString(
                font,
                String.format("#%06X", selectedColor),
                leftX + 112,
                wheelY + 57,
                0xFFFFFFFF
        );
    }

    private void drawPanel(
            GuiGraphics graphics,
            int x,
            int y,
            int panelWidth,
            int panelHeight
    ) {
        graphics.fill(
                x,
                y,
                x + panelWidth,
                y + panelHeight,
                DARK
        );
        graphics.renderOutline(
                x,
                y,
                panelWidth,
                panelHeight,
                BORDER
        );
    }

    private int countOnline(CompoundTag guild) {
        ListTag members = guild.getList(
                "Members",
                CompoundTag.TAG_COMPOUND
        );

        int online = 0;
        Minecraft minecraft = Minecraft.getInstance();

        if (minecraft.getConnection() == null) {
            return 0;
        }

        for (int i = 0; i < members.size(); i++) {
            try {
                UUID id = UUID.fromString(
                        members.getCompound(i).getString("Id")
                );

                if (minecraft.getConnection().getPlayerInfo(id) != null) {
                    online++;
                }
            } catch (IllegalArgumentException ignored) {
                // Omite UUID inválidos.
            }
        }

        return online;
    }

    @Override
    public boolean mouseClicked(
            double mouseX,
            double mouseY,
            int button
    ) {
        if (button == 0) {
            CompoundTag snapshot = GuildClientData.getSnapshot();

            if (snapshot.getBoolean("HasGuild")) {
                if (clickTab(mouseX, mouseY)) {
                    return true;
                }

                if (selectedTab == Tab.MEMBERS
                        && clickInbox(mouseX, mouseY)) {
                    return true;
                }
                if (selectedTab == Tab.BOARD
                        && clickAnnouncementPage(mouseX, mouseY)) {
                    return true;
                }
                if (selectedTab == Tab.PLAYERS
                        && clickOnlinePlayer(mouseX, mouseY)) {
                    return true;
                }
            } else {
                if (clickIncomingInvitations(mouseX, mouseY)) {
                    return true;
                }
                if (clickColorWheel(mouseX, mouseY)) {
                    return true;
                }

                if (clickCatalog(mouseX, mouseY)) {
                    return true;
                }
            }
        }

        return super.mouseClicked(mouseX, mouseY, button);
    }

    private boolean clickTab(double mouseX, double mouseY) {
        int size = 34;
        int gap = 7;
        int startX = frameX + 18;
        int y = frameY + 20;

        for (int i = 0; i < Tab.values().length; i++) {
            Tab tab = Tab.values()[i];
            int x = tab == Tab.PLAYERS
                    ? frameX + frameWidth - size - 18
                    : startX + i * (size + gap);

            if (mouseX >= x
                    && mouseX < x + size
                    && mouseY >= y
                    && mouseY < y + size) {
                selectTab(tab);
                return true;
            }
        }

        return false;
    }

    private boolean clickInbox(double mouseX, double mouseY) {
        CompoundTag inbox = GuildClientData.getSnapshot()
                .getCompound("Inbox");

        if (!inbox.getBoolean("HasGuildInbox")) {
            return false;
        }

        ListTag requests = inbox.getList(
                "Requests",
                CompoundTag.TAG_COMPOUND
        );

        int rowY = mainY + 82;

        for (int i = 0; i < requests.size(); i++) {
            int currentY = rowY + i * 29;

            if (mouseY < currentY || mouseY >= currentY + 25) {
                continue;
            }

            UUID guildId;
            UUID applicantId;

            try {
                guildId = UUID.fromString(inbox.getString("GuildId"));
                applicantId = UUID.fromString(
                        requests.getCompound(i).getString("PlayerId")
                );
            } catch (IllegalArgumentException ignored) {
                return true;
            }

            if (mouseX >= mainX + mainWidth - 112
                    && mouseX < mainX + mainWidth - 62) {
                GuildNetwork.acceptJoinRequest(guildId, applicantId);
                return true;
            }

            if (mouseX >= mainX + mainWidth - 60
                    && mouseX < mainX + mainWidth - 10) {
                GuildNetwork.rejectJoinRequest(guildId, applicantId);
                return true;
            }
        }

        return false;
    }

    private boolean clickCatalog(double mouseX, double mouseY) {
        int panelWidth = (frameWidth - 48) / 2;
        int leftX = frameX + 16;
        int rightX = leftX + panelWidth + 16;
        int panelY = frameY + 82;
        int panelHeight = frameHeight - 98;
        int x = rightX + 12;
        int y = panelY + 72;
        int rowWidth = panelWidth - 24;
        int bottom = panelY + panelHeight - 12;

        if (mouseX < x
                || mouseX >= x + rowWidth
                || mouseY < y
                || mouseY >= bottom) {
            return false;
        }

        List<CatalogEntry> entries = getFilteredGuilds();
        int visibleIndex = (int) ((mouseY - y) / 27);
        int selectedIndex = listScroll + visibleIndex;

        if (selectedIndex >= 0
                && selectedIndex < entries.size()
                && mouseX >= x + rowWidth - 60) {
            GuildNetwork.requestToJoin(
                    entries.get(selectedIndex).id()
            );
            statusMessage = "Solicitud enviada al líder.";
        }

        return true;
    }

    private boolean clickColorWheel(
            double mouseX,
            double mouseY
    ) {
        int centerX = wheelX + WHEEL_RADIUS;
        int centerY = wheelY + WHEEL_RADIUS;
        double dx = mouseX - centerX;
        double dy = mouseY - centerY;

        if (dx * dx + dy * dy > WHEEL_RADIUS * WHEEL_RADIUS) {
            return false;
        }

        float hue = (float) (
                Math.atan2(dy, dx) / (Math.PI * 2.0)
        );

        if (hue < 0.0F) {
            hue += 1.0F;
        }

        float saturation = (float) Math.min(
                1.0,
                Math.sqrt(dx * dx + dy * dy) / WHEEL_RADIUS
        );

        selectedColor = Color.HSBtoRGB(
                hue,
                saturation,
                1.0F
        ) & 0xFFFFFF;

        return true;
    }

    @Override
    public boolean mouseScrolled(
            double mouseX,
            double mouseY,
            double delta
    ) {
        if (GuildClientData.getSnapshot().getBoolean("HasGuild")
                && selectedTab == Tab.PLAYERS) {
            ListTag players = getFilteredOnlinePlayers();
            int listY = mainY + 32;
            int bottom = mainY + mainHeight - 42;
            int visibleRows = Math.max(1, (bottom - listY) / 32);
            int maxScroll = Math.max(0, players.size() - visibleRows);
            playerScroll -= (int) Math.signum(delta);
            playerScroll = Math.max(0, Math.min(playerScroll, maxScroll));
            return true;
        }

        if (!GuildClientData.getSnapshot().getBoolean("HasGuild")) {
            List<CatalogEntry> entries = getFilteredGuilds();
            int panelY = frameY + 82;
            int panelHeight = frameHeight - 98;
            int listY = panelY + 72;
            int bottom = panelY + panelHeight - 12;
            int visibleRows = Math.max(1, (bottom - listY) / 27);
            int maxScroll = Math.max(0, entries.size() - visibleRows);

            listScroll -= (int) Math.signum(delta);
            listScroll = Math.max(0, Math.min(listScroll, maxScroll));
            int panelWidth = (frameWidth - 48) / 2;
            int leftX = frameX + 16;
            if (mouseX >= leftX && mouseX < leftX + panelWidth
                    && mouseY >= panelY + 214) {
                int invitationCount = GuildClientData.getSnapshot()
                        .getCompound("InvitationInbox")
                        .getList("Invitations", CompoundTag.TAG_COMPOUND)
                        .size();
                invitationScroll -= (int) Math.signum(delta);
                invitationScroll = Math.max(
                        0,
                        Math.min(invitationScroll, Math.max(0, invitationCount - 3))
                );
            }
            return true;
        }

        return super.mouseScrolled(mouseX, mouseY, delta);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    private record CatalogEntry(
            UUID id,
            String name,
            int color,
            int memberCount
    ) {
    }
}
