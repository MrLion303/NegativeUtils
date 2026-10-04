package com.negative.negativeutils;

import java.util.UUID;
import java.util.function.Supplier;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;

public final class GuildNetwork {
    private static final String PROTOCOL_VERSION = "1";

    private static final SimpleChannel CHANNEL =
            NetworkRegistry.newSimpleChannel(
                    ResourceLocation.fromNamespaceAndPath(
                            EnciclopediaMod.MOD_ID,
                            "guilds"
                    ),
                    () -> PROTOCOL_VERSION,
                    PROTOCOL_VERSION::equals,
                    PROTOCOL_VERSION::equals
            );

    private static boolean registered;

    private GuildNetwork() {
    }

    public static void register() {
        if (registered) {
            return;
        }

        registered = true;

        CHANNEL.registerMessage(
                0,
                PanelDataRequest.class,
                PanelDataRequest::encode,
                PanelDataRequest::decode,
                PanelDataRequest::handle
        );

        CHANNEL.registerMessage(
                1,
                CreateGuildRequest.class,
                CreateGuildRequest::encode,
                CreateGuildRequest::decode,
                CreateGuildRequest::handle
        );

        CHANNEL.registerMessage(
                2,
                JoinGuildRequest.class,
                JoinGuildRequest::encode,
                JoinGuildRequest::decode,
                JoinGuildRequest::handle
        );

        CHANNEL.registerMessage(
                3,
                AcceptJoinRequest.class,
                AcceptJoinRequest::encode,
                AcceptJoinRequest::decode,
                AcceptJoinRequest::handle
        );

        CHANNEL.registerMessage(
                4,
                RejectJoinRequest.class,
                RejectJoinRequest::encode,
                RejectJoinRequest::decode,
                RejectJoinRequest::handle
        );

        CHANNEL.registerMessage(
                5,
                PanelSnapshot.class,
                PanelSnapshot::encode,
                PanelSnapshot::decode,
                PanelSnapshot::handle
        );
    }

    @Mod.EventBusSubscriber(
            modid = EnciclopediaMod.MOD_ID,
            bus = Mod.EventBusSubscriber.Bus.MOD
    )
    public static class Registration {
        @SubscribeEvent
        public static void onCommonSetup(FMLCommonSetupEvent event) {
            event.enqueueWork(GuildNetwork::register);
        }
    }

    /**
     * La tecla K y la GUI usarán esto para pedir el catálogo
     * y el buzón al servidor.
     */
    public static void requestPanelData() {
        CHANNEL.sendToServer(new PanelDataRequest());
    }

    /**
     * La GUI de creación usará esto.
     */
    public static void sendCreateRequest(String name, int color) {
        CHANNEL.sendToServer(
                new CreateGuildRequest(name, color)
        );
    }

    /**
     * La lista de búsqueda usará esto al seleccionar una hermandad.
     */
    public static void requestToJoin(UUID guildId) {
        CHANNEL.sendToServer(new JoinGuildRequest(guildId));
    }

    /**
     * El líder usará esto desde el buzón de solicitudes.
     */
    public static void acceptJoinRequest(
            UUID guildId,
            UUID applicantId
    ) {
        CHANNEL.sendToServer(
                new AcceptJoinRequest(guildId, applicantId)
        );
    }

    /**
     * El líder usará esto desde el buzón de solicitudes.
     */
    public static void rejectJoinRequest(
            UUID guildId,
            UUID applicantId
    ) {
        CHANNEL.sendToServer(
                new RejectJoinRequest(guildId, applicantId)
        );
    }

    private static void sendPanelSnapshot(
            ServerPlayer player,
            GuildSavedData data
    ) {
        CHANNEL.send(
                PacketDistributor.PLAYER.with(() -> player),
                new PanelSnapshot(
                        GuildActionsNetwork.createPanelSnapshot(player, data)
                )
        );
    }

    private static void sendError(
            ServerPlayer player,
            String message
    ) {
        player.sendSystemMessage(Component.literal(message));
    }

    private static class PanelDataRequest {
        private static void encode(
                PanelDataRequest packet,
                FriendlyByteBuf buffer
        ) {
        }

        private static PanelDataRequest decode(
                FriendlyByteBuf buffer
        ) {
            return new PanelDataRequest();
        }

        private static void handle(
                PanelDataRequest packet,
                Supplier<NetworkEvent.Context> contextSupplier
        ) {
            NetworkEvent.Context context = contextSupplier.get();

            context.enqueueWork(() -> {
                ServerPlayer player = context.getSender();

                if (player == null) {
                    return;
                }

                GuildSavedData data =
                        GuildSavedData.get(player.getServer());

                sendPanelSnapshot(player, data);
            });

            context.setPacketHandled(true);
        }
    }

    private static class CreateGuildRequest {
        private final String name;
        private final int color;

        private CreateGuildRequest(String name, int color) {
            this.name = name == null ? "" : name.strip();
            this.color = color & 0xFFFFFF;
        }

        private static void encode(
                CreateGuildRequest packet,
                FriendlyByteBuf buffer
        ) {
            buffer.writeUtf(packet.name, 24);
            buffer.writeInt(packet.color);
        }

        private static CreateGuildRequest decode(
                FriendlyByteBuf buffer
        ) {
            return new CreateGuildRequest(
                    buffer.readUtf(24),
                    buffer.readInt()
            );
        }

        private static void handle(
                CreateGuildRequest packet,
                Supplier<NetworkEvent.Context> contextSupplier
        ) {
            NetworkEvent.Context context = contextSupplier.get();

            context.enqueueWork(() -> {
                ServerPlayer player = context.getSender();

                if (player == null) {
                    return;
                }

                GuildSavedData data =
                        GuildSavedData.get(player.getServer());

                GuildSavedData.Guild guild = data.createGuild(
                        player.getUUID(),
                        player.getGameProfile().getName(),
                        packet.name,
                        packet.color
                );

                if (guild == null) {
                    sendError(
                            player,
                            "No se pudo crear la hermandad. "
                                    + "Comprueba el nombre y que no "
                                    + "pertenezcas ya a otra."
                    );
                }

                sendPanelSnapshot(player, data);
            });

            context.setPacketHandled(true);
        }
    }

    private static class JoinGuildRequest {
        private final UUID guildId;

        private JoinGuildRequest(UUID guildId) {
            this.guildId = guildId;
        }

        private static void encode(
                JoinGuildRequest packet,
                FriendlyByteBuf buffer
        ) {
            buffer.writeUUID(packet.guildId);
        }

        private static JoinGuildRequest decode(
                FriendlyByteBuf buffer
        ) {
            return new JoinGuildRequest(buffer.readUUID());
        }

        private static void handle(
                JoinGuildRequest packet,
                Supplier<NetworkEvent.Context> contextSupplier
        ) {
            NetworkEvent.Context context = contextSupplier.get();

            context.enqueueWork(() -> {
                ServerPlayer applicant = context.getSender();

                if (applicant == null) {
                    return;
                }

                GuildSavedData data =
                        GuildSavedData.get(applicant.getServer());

                GuildSavedData.RequestResult result =
                        data.requestToJoin(
                                packet.guildId,
                                applicant.getUUID(),
                                applicant.getGameProfile().getName()
                        );

                switch (result) {
                    case SUCCESS -> {
                        GuildSavedData.Guild guild =
                                data.getGuild(packet.guildId);

                        if (guild != null) {
                            ServerPlayer leader =
                                    applicant.getServer()
                                            .getPlayerList()
                                            .getPlayer(guild.leaderId());

                            if (leader != null) {
                                sendPanelSnapshot(leader, data);
                            }
                        }
                    }

                    case GUILD_NOT_FOUND ->
                            sendError(
                                    applicant,
                                    "Esa hermandad ya no existe."
                            );

                    case INVALID_PLAYER ->
                            sendError(
                                    applicant,
                                    "No se pudo enviar la solicitud."
                            );

                    case ALREADY_IN_GUILD ->
                            sendError(
                                    applicant,
                                    "Ya perteneces a una hermandad."
                            );

                    case ALREADY_REQUESTED ->
                            sendError(
                                    applicant,
                                    "Ya enviaste una solicitud a esa hermandad."
                            );
                }

                sendPanelSnapshot(applicant, data);
            });

            context.setPacketHandled(true);
        }
    }

    private static class AcceptJoinRequest {
        private final UUID guildId;
        private final UUID applicantId;

        private AcceptJoinRequest(UUID guildId, UUID applicantId) {
            this.guildId = guildId;
            this.applicantId = applicantId;
        }

        private static void encode(
                AcceptJoinRequest packet,
                FriendlyByteBuf buffer
        ) {
            buffer.writeUUID(packet.guildId);
            buffer.writeUUID(packet.applicantId);
        }

        private static AcceptJoinRequest decode(
                FriendlyByteBuf buffer
        ) {
            return new AcceptJoinRequest(
                    buffer.readUUID(),
                    buffer.readUUID()
            );
        }

        private static void handle(
                AcceptJoinRequest packet,
                Supplier<NetworkEvent.Context> contextSupplier
        ) {
            NetworkEvent.Context context = contextSupplier.get();

            context.enqueueWork(() -> {
                ServerPlayer leader = context.getSender();

                if (leader == null) {
                    return;
                }

                GuildSavedData data =
                        GuildSavedData.get(leader.getServer());

                boolean accepted = data.acceptJoinRequest(
                        packet.guildId,
                        leader.getUUID(),
                        packet.applicantId
                );

                if (!accepted) {
                    sendError(
                            leader,
                            "No se pudo aceptar esa solicitud."
                    );
                } else {
                    ServerPlayer applicant =
                            leader.getServer()
                                    .getPlayerList()
                                    .getPlayer(packet.applicantId);

                    if (applicant != null) {
                        applicant.sendSystemMessage(
                                Component.literal(
                                        "Han aceptado tu solicitud "
                                                + "para entrar a la hermandad."
                                )
                        );
                        sendPanelSnapshot(applicant, data);
                    }
                }

                sendPanelSnapshot(leader, data);
            });

            context.setPacketHandled(true);
        }
    }

    private static class RejectJoinRequest {
        private final UUID guildId;
        private final UUID applicantId;

        private RejectJoinRequest(UUID guildId, UUID applicantId) {
            this.guildId = guildId;
            this.applicantId = applicantId;
        }

        private static void encode(
                RejectJoinRequest packet,
                FriendlyByteBuf buffer
        ) {
            buffer.writeUUID(packet.guildId);
            buffer.writeUUID(packet.applicantId);
        }

        private static RejectJoinRequest decode(
                FriendlyByteBuf buffer
        ) {
            return new RejectJoinRequest(
                    buffer.readUUID(),
                    buffer.readUUID()
            );
        }

        private static void handle(
                RejectJoinRequest packet,
                Supplier<NetworkEvent.Context> contextSupplier
        ) {
            NetworkEvent.Context context = contextSupplier.get();

            context.enqueueWork(() -> {
                ServerPlayer leader = context.getSender();

                if (leader == null) {
                    return;
                }

                GuildSavedData data =
                        GuildSavedData.get(leader.getServer());

                boolean rejected = data.rejectJoinRequest(
                        packet.guildId,
                        leader.getUUID(),
                        packet.applicantId
                );

                if (!rejected) {
                    sendError(
                            leader,
                            "No se pudo rechazar esa solicitud."
                    );
                }

                sendPanelSnapshot(leader, data);
            });

            context.setPacketHandled(true);
        }
    }

    private static class PanelSnapshot {
        private final CompoundTag snapshot;

        private PanelSnapshot(CompoundTag snapshot) {
            this.snapshot = snapshot.copy();
        }

        private static void encode(
                PanelSnapshot packet,
                FriendlyByteBuf buffer
        ) {
            buffer.writeNbt(packet.snapshot);
        }

        private static PanelSnapshot decode(
                FriendlyByteBuf buffer
        ) {
            CompoundTag tag = buffer.readNbt();

            return new PanelSnapshot(
                    tag == null ? new CompoundTag() : tag
            );
        }

        private static void handle(
                PanelSnapshot packet,
                Supplier<NetworkEvent.Context> contextSupplier
        ) {
            NetworkEvent.Context context = contextSupplier.get();

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
