package com.negative.negativeutils;

import java.util.UUID;
import java.util.function.Supplier;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.BannerItem;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;

public final class GuildActionsNetwork {
    private static final String PROTOCOL = "1";

    private static final byte CHAT = 0;
    private static final byte MOTD = 1;
    private static final byte ANNOUNCEMENT = 2;
    private static final byte LOCATION = 3;
    private static final byte SAVE_BANNER = 4;
    private static final byte LEAVE = 5;
    private static final byte DELETE = 6;
    private static final byte INVITE_PLAYER = 7;
    private static final byte ACCEPT_INVITATION = 8;
    private static final byte REJECT_INVITATION = 9;

    private static final SimpleChannel CHANNEL =
            NetworkRegistry.newSimpleChannel(
                    ResourceLocation.fromNamespaceAndPath(
                            EnciclopediaMod.MOD_ID,
                            "guild_actions"
                    ),
                    () -> PROTOCOL,
                    PROTOCOL::equals,
                    PROTOCOL::equals
            );

    private static boolean registered;

    private GuildActionsNetwork() {
    }

    public static void register() {
        if (registered) {
            return;
        }

        registered = true;

        CHANNEL.registerMessage(
                0,
                ActionRequest.class,
                ActionRequest::encode,
                ActionRequest::decode,
                ActionRequest::handle
        );

        CHANNEL.registerMessage(
                1,
                SnapshotPacket.class,
                SnapshotPacket::encode,
                SnapshotPacket::decode,
                SnapshotPacket::handle
        );
    }

    public static void sendChat(String text) {
        send(CHAT, text);
    }

    public static void saveMotd(String text) {
        send(MOTD, text);
    }

    public static void saveAnnouncement(String title, String text) {
        send(ANNOUNCEMENT, title, text);
    }

    public static void saveLocation(String name) {
        send(LOCATION, name);
    }

    public static void saveBannerFromHand() {
        send(SAVE_BANNER, "");
    }

    public static void leaveGuild() {
        send(LEAVE, "");
    }

    public static void deleteGuild() {
        send(DELETE, "");
    }

    public static void invitePlayer(UUID playerId) {
        send(INVITE_PLAYER, playerId.toString());
    }

    public static void acceptInvitation(UUID guildId) {
        send(ACCEPT_INVITATION, guildId.toString());
    }

    public static void rejectInvitation(UUID guildId) {
        send(REJECT_INVITATION, guildId.toString());
    }

    public static void syncGuild(ServerPlayer actor, UUID guildId) {
        GuildSavedData data = GuildSavedData.get(actor.getServer());
        refreshGuildMembers(actor, data, guildId);
    }

    private static void send(byte action, String text) {
        send(action, text, "");
    }

    private static void send(byte action, String text, String extraText) {
        CHANNEL.sendToServer(
                new ActionRequest(action, text, extraText)
        );
    }

    private static void refreshGuildMembers(
            ServerPlayer actor,
            GuildSavedData data,
            UUID guildId
    ) {
        for (ServerPlayer online
                : actor.getServer().getPlayerList().getPlayers()) {
            GuildSavedData.Guild guild =
                    data.getGuildForPlayer(online.getUUID());

            if (guild != null && guild.id().equals(guildId)) {
                sendSnapshot(online, data);
            }
        }

        // Actualiza también al jugador que acaba de salir o borrar.
        if (data.getGuildForPlayer(actor.getUUID()) == null) {
            sendSnapshot(actor, data);
        }
    }

    private static void sendSnapshot(
            ServerPlayer player,
            GuildSavedData data
    ) {
        CHANNEL.send(
                PacketDistributor.PLAYER.with(() -> player),
                new SnapshotPacket(
                        createPanelSnapshot(player, data)
                )
        );
    }

    public static CompoundTag createPanelSnapshot(
            ServerPlayer player,
            GuildSavedData data
    ) {
        CompoundTag snapshot = data.savePanelSnapshot(player.getUUID());
        GuildSavedData.Guild guild = data.getGuildForPlayer(player.getUUID());
        if (guild == null) {
            return snapshot;
        }

        GuildSavedData.Member localMember = guild.members().get(player.getUUID());
        boolean leader = guild.leaderId().equals(player.getUUID());
        boolean manager = localMember != null
                && (localMember.rank() == GuildSavedData.Rank.LEADER
                || localMember.rank() == GuildSavedData.Rank.OFFICER);
        snapshot.putBoolean("IsGuildLeader", leader);
        snapshot.putBoolean("CanManageGuild", manager);

        ListTag onlinePlayers = new ListTag();
        for (ServerPlayer online : player.getServer()
                .getPlayerList().getPlayers()) {
            CompoundTag entry = new CompoundTag();
            UUID onlineId = online.getUUID();
            GuildSavedData.Guild currentGuild = data.getGuildForPlayer(onlineId);
            boolean sameGuild = currentGuild != null
                    && currentGuild.id().equals(guild.id());
            entry.putString("Id", onlineId.toString());
            entry.putString("Name", online.getGameProfile().getName());
            entry.putBoolean("Self", onlineId.equals(player.getUUID()));
            entry.putBoolean("Member", sameGuild);
            entry.putBoolean(
                    "Requested",
                    guild.joinRequests().containsKey(onlineId)
            );
            entry.putBoolean(
                    "Invited",
                    guild.invitations().containsKey(onlineId)
            );
            onlinePlayers.add(entry);
        }
        snapshot.put("OnlinePlayers", onlinePlayers);
        return snapshot;
    }

    private static void error(ServerPlayer player, String message) {
        player.sendSystemMessage(Component.literal(message));
    }

    @Mod.EventBusSubscriber(
            modid = EnciclopediaMod.MOD_ID,
            bus = Mod.EventBusSubscriber.Bus.MOD
    )
    public static class Registration {
        private Registration() {
        }

        @SubscribeEvent
        public static void onCommonSetup(FMLCommonSetupEvent event) {
            event.enqueueWork(GuildActionsNetwork::register);
        }
    }

    private static class ActionRequest {
        private final byte action;
        private final String text;
        private final String extraText;

        private ActionRequest(byte action, String text, String extraText) {
            this.action = action;
            this.text = text == null ? "" : text.strip();
            this.extraText = extraText == null ? "" : extraText.strip();
        }

        private static void encode(
                ActionRequest packet,
                FriendlyByteBuf buffer
        ) {
            buffer.writeByte(packet.action);
            buffer.writeUtf(packet.text, 500);
            buffer.writeUtf(packet.extraText, 500);
        }

        private static ActionRequest decode(FriendlyByteBuf buffer) {
            return new ActionRequest(
                    buffer.readByte(),
                    buffer.readUtf(500),
                    buffer.readUtf(500)
            );
        }

        private static void handle(
                ActionRequest packet,
                Supplier<NetworkEvent.Context> supplier
        ) {
            NetworkEvent.Context context = supplier.get();

            context.enqueueWork(() -> {
                ServerPlayer player = context.getSender();

                if (player == null) {
                    return;
                }

                GuildSavedData data =
                        GuildSavedData.get(player.getServer());

                if (packet.action == ACCEPT_INVITATION
                        || packet.action == REJECT_INVITATION) {
                    UUID invitationGuildId = parseUuid(packet.text);
                    if (invitationGuildId == null) {
                        sendSnapshot(player, data);
                        return;
                    }
                    boolean accepted = packet.action == ACCEPT_INVITATION
                            ? data.acceptInvitation(
                                    invitationGuildId,
                                    player.getUUID(),
                                    player.getGameProfile().getName()
                            )
                            : data.rejectInvitation(
                                    invitationGuildId,
                                    player.getUUID()
                            );
                    if (!accepted) {
                        error(player, "La invitación ya no está disponible.");
                    }
                    refreshGuildMembers(player, data, invitationGuildId);
                    return;
                }

                GuildSavedData.Guild guild =
                        data.getGuildForPlayer(player.getUUID());

                if (guild == null) {
                    error(player, "No perteneces a una hermandad.");
                    sendSnapshot(player, data);
                    return;
                }

                UUID guildId = guild.id();
                boolean success;

                switch (packet.action) {
                    case CHAT -> {
                        success = data.addChatMessage(
                                guildId,
                                player.getUUID(),
                                player.getGameProfile().getName(),
                                packet.text
                        );

                        if (!success) {
                            error(player, "No se pudo enviar el mensaje.");
                        }
                    }

                    case MOTD -> {
                        success = data.setMotd(
                                guildId,
                                player.getUUID(),
                                packet.text
                        );

                        if (!success) {
                            error(
                                    player,
                                    "Solo el líder o un oficial puede "
                                            + "cambiar el MOTD."
                            );
                        }
                    }

                    case ANNOUNCEMENT -> {
                        success = data.setAnnouncement(
                                guildId,
                                player.getUUID(),
                                packet.text,
                                packet.extraText
                        );

                        if (!success) {
                            error(
                                    player,
                                    "Solo el líder o un oficial puede "
                                            + "editar el tablón."
                            );
                        }
                    }

                    case LOCATION -> {
                        success = data.addLocation(
                                guildId,
                                player.getUUID(),
                                packet.text,
                                player.serverLevel()
                                        .dimension()
                                        .location()
                                        .toString(),
                                player.blockPosition().getX(),
                                player.blockPosition().getY(),
                                player.blockPosition().getZ()
                        );

                        if (!success) {
                            error(
                                    player,
                                    "No se pudo guardar la ubicación. "
                                            + "Comprueba tu rango y el límite."
                            );
                        }
                    }

                    case SAVE_BANNER -> {
                        ItemStack heldItem = player.getMainHandItem();

                        if (!(heldItem.getItem() instanceof BannerItem)) {
                            success = false;
                            error(
                                    player,
                                    "Sujeta un estandarte en la mano "
                                            + "principal para guardarlo."
                            );
                        } else {
                            success = data.setBanner(
                                    guildId,
                                    player.getUUID(),
                                    heldItem
                            );

                            if (!success) {
                                error(
                                        player,
                                        "Solo el líder o un oficial "
                                                + "puede cambiar el estandarte."
                                );
                            }
                        }
                    }

                    case LEAVE -> {
                        success = data.leaveGuild(player.getUUID());

                        if (!success) {
                            error(
                                    player,
                                    "El líder debe transferir el liderazgo "
                                            + "o borrar la hermandad antes "
                                            + "de salir."
                            );
                        }
                    }

                    case DELETE -> {
                        success = data.deleteGuild(
                                guildId,
                                player.getUUID()
                        );

                        if (!success) {
                            error(
                                    player,
                                    "Solo el líder puede borrar "
                                            + "la hermandad."
                            );
                        }
                    }

                    case INVITE_PLAYER -> {
                        UUID targetId = parseUuid(packet.text);
                        success = targetId != null
                                && data.invitePlayer(
                                        guildId,
                                        player.getUUID(),
                                        targetId
                                );
                        if (!success) {
                            error(
                                    player,
                                    "No se pudo invitar. Comprueba que no "
                                            + "sea miembro, no tenga otra "
                                            + "invitación y que tengas rango."
                            );
                        } else {
                            ServerPlayer target = player.getServer()
                                    .getPlayerList().getPlayer(targetId);
                            if (target != null) {
                                target.sendSystemMessage(Component.literal(
                                        "Tienes una invitación a la hermandad "
                                                + guild.name()
                                                + ". Ábrela en el panel K."
                                ));
                                sendSnapshot(target, data);
                            }
                        }
                    }

                    default -> {
                        success = false;
                        error(player, "Acción desconocida.");
                    }
                }

                refreshGuildMembers(player, data, guildId);
            });

            context.setPacketHandled(true);
        }
    }

    private static UUID parseUuid(String value) {
        try {
            return UUID.fromString(value);
        } catch (IllegalArgumentException exception) {
            return null;
        }
    }

    private static class SnapshotPacket {
        private final CompoundTag snapshot;

        private SnapshotPacket(CompoundTag snapshot) {
            this.snapshot = snapshot.copy();
        }

        private static void encode(
                SnapshotPacket packet,
                FriendlyByteBuf buffer
        ) {
            buffer.writeNbt(packet.snapshot);
        }

        private static SnapshotPacket decode(FriendlyByteBuf buffer) {
            CompoundTag tag = buffer.readNbt();

            return new SnapshotPacket(
                    tag == null ? new CompoundTag() : tag
            );
        }

        private static void handle(
                SnapshotPacket packet,
                Supplier<NetworkEvent.Context> supplier
        ) {
            NetworkEvent.Context context = supplier.get();

            context.enqueueWork(() ->
                    DistExecutor.unsafeRunWhenOn(
                            Dist.CLIENT,
                            () -> () -> GuildClientData.setSnapshot(
                                    packet.snapshot
                            )
                    )
            );

            context.setPacketHandled(true);
        }
    }
}
