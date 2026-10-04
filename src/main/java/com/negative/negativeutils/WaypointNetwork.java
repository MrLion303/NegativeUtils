package com.negative.negativeutils;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.function.Supplier;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;

public final class WaypointNetwork {
    private static final String PROTOCOL = "3";
    private static final int MAX_WAYPOINTS_PER_PACKET = 256;

    private static final SimpleChannel CHANNEL =
            NetworkRegistry.newSimpleChannel(
                    ResourceLocation.fromNamespaceAndPath(
                            "negativeutils",
                            "waypoints"
                    ),
                    () -> PROTOCOL,
                    PROTOCOL::equals,
                    PROTOCOL::equals
            );

    private static boolean registered;

    private WaypointNetwork() {
    }

    public static void register() {
        if (registered) {
            return;
        }

        registered = true;

        CHANNEL.registerMessage(
                0,
                CreateWaypointPacket.class,
                CreateWaypointPacket::encode,
                CreateWaypointPacket::decode,
                CreateWaypointPacket::handle
        );
        CHANNEL.registerMessage(
                1,
                SyncWaypointsPacket.class,
                SyncWaypointsPacket::encode,
                SyncWaypointsPacket::decode,
                SyncWaypointsPacket::handle
        );
        CHANNEL.registerMessage(
                2,
                DeleteWaypointPacket.class,
                DeleteWaypointPacket::encode,
                DeleteWaypointPacket::decode,
                DeleteWaypointPacket::handle
        );
    }

    public static void create(
            BlockPos blockPos,
            String dimension,
            String name,
            int color
    ) {
        CHANNEL.sendToServer(
                new CreateWaypointPacket(
                        blockPos,
                        dimension,
                        name,
                        color
                )
        );
    }

    public static void delete(UUID waypointId) {
        CHANNEL.sendToServer(new DeleteWaypointPacket(waypointId));
    }

    public static void syncAll(List<WaypointSavedData.Waypoint> waypoints) {
        sendSyncPackets(waypoints, PacketDistributor.ALL.noArg());
    }

    public static void syncToPlayer(
            ServerPlayer player,
            List<WaypointSavedData.Waypoint> waypoints
    ) {
        sendSyncPackets(
                waypoints,
                PacketDistributor.PLAYER.with(() -> player)
        );
    }

    private static void sendSyncPackets(
            List<WaypointSavedData.Waypoint> waypoints,
            net.minecraftforge.network.PacketDistributor.PacketTarget target
    ) {
        int packetCount = Math.max(
                1,
                (waypoints.size() + MAX_WAYPOINTS_PER_PACKET - 1)
                        / MAX_WAYPOINTS_PER_PACKET
        );

        for (int packetIndex = 0; packetIndex < packetCount; packetIndex++) {
            int start = packetIndex * MAX_WAYPOINTS_PER_PACKET;
            int end = Math.min(
                    waypoints.size(),
                    start + MAX_WAYPOINTS_PER_PACKET
            );

            CHANNEL.send(
                    target,
                    new SyncWaypointsPacket(
                            waypoints.subList(start, end),
                            packetIndex == 0
                    )
            );
        }
    }

    private record CreateWaypointPacket(
            BlockPos blockPos,
            String dimension,
            String name,
            int color
    ) {
        private static void encode(
                CreateWaypointPacket packet,
                FriendlyByteBuf buffer
        ) {
            buffer.writeBlockPos(packet.blockPos);
            buffer.writeUtf(packet.dimension, 256);
            buffer.writeUtf(packet.name, 128);
            buffer.writeInt(packet.color);
        }

        private static CreateWaypointPacket decode(FriendlyByteBuf buffer) {
            return new CreateWaypointPacket(
                    buffer.readBlockPos(),
                    buffer.readUtf(256),
                    buffer.readUtf(128),
                    buffer.readInt()
            );
        }

        private static void handle(
                CreateWaypointPacket packet,
                Supplier<NetworkEvent.Context> contextSupplier
        ) {
            NetworkEvent.Context context = contextSupplier.get();
            context.enqueueWork(() -> {
                ServerPlayer player = context.getSender();
                if (player == null) {
                    return;
                }

                String cleanName = WaypointSavedData.sanitizeName(packet.name);
                if (cleanName.length() > 32
                        || !player.getMainHandItem().is(ModItems.WAYPOINT_WAND.get())
                        || !player.level().dimension().location().toString()
                                .equals(packet.dimension)
                        || !player.serverLevel().getWorldBorder()
                                .isWithinBounds(packet.blockPos)
                        || packet.blockPos.getY()
                                < player.serverLevel().getMinBuildHeight()
                        || packet.blockPos.getY()
                                >= player.serverLevel().getMaxBuildHeight() - 4
                        || player.distanceToSqr(
                                Vec3.atCenterOf(packet.blockPos)
                        ) > 256.0) {
                    player.displayClientMessage(
                            net.minecraft.network.chat.Component.literal(
                                    "No se pudo crear el waypoint: revisa el nombre, el objeto y que sigas cerca del lugar."
                            ),
                            true
                    );
                    return;
                }

                WaypointSavedData data =
                        WaypointSavedData.get(player.getServer());
                Vec3 markerPosition = new Vec3(
                        packet.blockPos.getX() + 0.5,
                        packet.blockPos.getY() + 4.0,
                        packet.blockPos.getZ() + 0.5
                );

                WaypointSavedData.Waypoint waypoint = data.add(
                        player.getUUID(),
                        cleanName,
                        packet.dimension,
                        markerPosition,
                        packet.color
                );

                if (waypoint == null) {
                    player.displayClientMessage(
                            net.minecraft.network.chat.Component.literal(
                                    "No se pudo guardar el waypoint."
                            ),
                            true
                    );
                    return;
                }

                syncAll(data.getWaypoints());
                player.displayClientMessage(
                        net.minecraft.network.chat.Component.literal(
                                waypoint.name().isBlank()
                                        ? "Waypoint creado."
                                        : "Waypoint '" + waypoint.name() + "' creado."
                        ),
                        true
                );
            });
            context.setPacketHandled(true);
        }
    }

    private record DeleteWaypointPacket(UUID waypointId) {
        private static void encode(
                DeleteWaypointPacket packet,
                FriendlyByteBuf buffer
        ) {
            buffer.writeUUID(packet.waypointId);
        }

        private static DeleteWaypointPacket decode(FriendlyByteBuf buffer) {
            return new DeleteWaypointPacket(buffer.readUUID());
        }

        private static void handle(
                DeleteWaypointPacket packet,
                Supplier<NetworkEvent.Context> contextSupplier
        ) {
            NetworkEvent.Context context = contextSupplier.get();
            context.enqueueWork(() -> {
                ServerPlayer player = context.getSender();
                if (player == null) {
                    return;
                }

                if (!player.getMainHandItem().is(ModItems.WAYPOINT_WAND.get())) {
                    player.displayClientMessage(
                            net.minecraft.network.chat.Component.literal(
                                    "Equipa la varita de waypoints para borrarlo."
                            ),
                            true
                    );
                    return;
                }

                WaypointSavedData data =
                        WaypointSavedData.get(player.getServer());
                WaypointSavedData.Waypoint waypoint = data.getWaypoints()
                        .stream()
                        .filter(candidate ->
                                candidate.id().equals(packet.waypointId))
                        .findFirst()
                        .orElse(null);

                if (waypoint == null
                        || !waypoint.dimension().equals(
                                player.level().dimension().location().toString()
                        )
                        || player.distanceToSqr(
                                new Vec3(
                                        waypoint.x(),
                                        waypoint.y(),
                                        waypoint.z()
                                )
                        ) > 512.0 * 512.0) {
                    player.displayClientMessage(
                            net.minecraft.network.chat.Component.literal(
                                    "No se encontró un waypoint alcanzable."
                            ),
                            true
                    );
                    return;
                }

                WaypointSavedData.Waypoint removed = data.removeById(
                        packet.waypointId,
                        player.getUUID(),
                        player.hasPermissions(2),
                        waypoint.dimension()
                );

                if (removed == null) {
                    player.displayClientMessage(
                            net.minecraft.network.chat.Component.literal(
                                    "No hay un waypoint tuyo cerca de ese bloque."
                            ),
                            true
                    );
                    return;
                }

                syncAll(data.getWaypoints());
                player.displayClientMessage(
                        net.minecraft.network.chat.Component.literal(
                                "Waypoint '" + removed.name() + "' eliminado."
                        ),
                        true
                );
            });
            context.setPacketHandled(true);
        }
    }

    private record SyncWaypointsPacket(
            List<WaypointSavedData.Waypoint> waypoints,
            boolean startsBatch
    ) {
        private SyncWaypointsPacket {
            waypoints = List.copyOf(waypoints);
        }

        private static void encode(
                SyncWaypointsPacket packet,
                FriendlyByteBuf buffer
        ) {
            buffer.writeBoolean(packet.startsBatch);
            buffer.writeVarInt(packet.waypoints.size());

            for (WaypointSavedData.Waypoint waypoint : packet.waypoints) {
                buffer.writeUUID(waypoint.id());
                buffer.writeUUID(waypoint.owner());
                buffer.writeUtf(waypoint.name(), 32);
                buffer.writeUtf(waypoint.dimension(), 256);
                buffer.writeDouble(waypoint.x());
                buffer.writeDouble(waypoint.y());
                buffer.writeDouble(waypoint.z());
                buffer.writeInt(waypoint.color());
                buffer.writeByte(waypoint.shape());
                buffer.writeBoolean(waypoint.visible());
            }
        }

        private static SyncWaypointsPacket decode(FriendlyByteBuf buffer) {
            boolean startsBatch = buffer.readBoolean();
            int count = buffer.readVarInt();

            if (count < 0 || count > MAX_WAYPOINTS_PER_PACKET) {
                throw new IllegalArgumentException(
                        "Cantidad de waypoints sincronizados no válida: " + count
                );
            }

            List<WaypointSavedData.Waypoint> waypoints =
                    new ArrayList<>(count);

            for (int i = 0; i < count; i++) {
                waypoints.add(new WaypointSavedData.Waypoint(
                        buffer.readUUID(),
                        buffer.readUUID(),
                        buffer.readUtf(32),
                        buffer.readUtf(256),
                        buffer.readDouble(),
                        buffer.readDouble(),
                        buffer.readDouble(),
                        buffer.readInt() & 0xFFFFFF,
                        buffer.readByte(),
                        buffer.readBoolean()
                ));
            }

            return new SyncWaypointsPacket(waypoints, startsBatch);
        }

        private static void handle(
                SyncWaypointsPacket packet,
                Supplier<NetworkEvent.Context> contextSupplier
        ) {
            NetworkEvent.Context context = contextSupplier.get();
            context.enqueueWork(() ->
                    DistExecutor.unsafeRunWhenOn(
                            Dist.CLIENT,
                            () -> () -> {
                                if (packet.startsBatch) {
                                    WaypointClientData.setWaypoints(
                                            packet.waypoints
                                    );
                                } else {
                                    WaypointClientData.appendWaypoints(
                                            packet.waypoints
                                    );
                                }
                            }
                    )
            );
            context.setPacketHandled(true);
        }
    }
}
