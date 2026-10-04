package com.negative.negativeutils;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.function.Supplier;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;

public final class TrailNetwork {
    private static final String PROTOCOL_VERSION = "2";

    public static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
            new ResourceLocation("negativeutils", "trail"),
            () -> PROTOCOL_VERSION,
            PROTOCOL_VERSION::equals,
            PROTOCOL_VERSION::equals
    );

    private static int packetId;

    private TrailNetwork() {
    }

    public static void register() {
        CHANNEL.registerMessage(
                packetId++,
                SyncTrailsPacket.class,
                SyncTrailsPacket::encode,
                SyncTrailsPacket::decode,
                SyncTrailsPacket::handle
        );
        CHANNEL.registerMessage(
                packetId++,
                OpenSettingsPacket.class,
                OpenSettingsPacket::encode,
                OpenSettingsPacket::decode,
                OpenSettingsPacket::handle,
                java.util.Optional.of(NetworkDirection.PLAY_TO_CLIENT)
        );
        CHANNEL.registerMessage(
                packetId++,
                SaveSettingsPacket.class,
                SaveSettingsPacket::encode,
                SaveSettingsPacket::decode,
                SaveSettingsPacket::handle,
                java.util.Optional.of(NetworkDirection.PLAY_TO_SERVER)
        );
    }

    public static void syncAll(List<TrailSavedData.Trail> trails) {
        CHANNEL.send(
                PacketDistributor.ALL.noArg(),
                new SyncTrailsPacket(trails)
        );
    }

    public static void syncToPlayer(
            ServerPlayer player,
            List<TrailSavedData.Trail> trails
    ) {
        CHANNEL.send(
                PacketDistributor.PLAYER.with(() -> player),
                new SyncTrailsPacket(trails)
        );
    }

    public static void openSettings(
            ServerPlayer player,
            TrailSavedData.Trail trail
    ) {
        CHANNEL.send(
                PacketDistributor.PLAYER.with(() -> player),
                new OpenSettingsPacket(
                        trail.id(),
                        trail.name(),
                        trail.red(),
                        trail.green(),
                        trail.blue(),
                        Math.round(trail.opacity() * 100.0F / 255.0F)
                )
        );
    }

    public static void saveSettings(
            UUID trailId,
            int red,
            int green,
            int blue,
            int opacityPercent
    ) {
        CHANNEL.sendToServer(
                new SaveSettingsPacket(
                        trailId,
                        red,
                        green,
                        blue,
                        opacityPercent
                )
        );
    }

    private static int clamp(int value) {
        return Math.max(0, Math.min(255, value));
    }

    private record OpenSettingsPacket(
            UUID trailId,
            String name,
            int red,
            int green,
            int blue,
            int opacityPercent
    ) {
        private static void encode(
                OpenSettingsPacket packet,
                FriendlyByteBuf buffer
        ) {
            buffer.writeUUID(packet.trailId);
            buffer.writeUtf(packet.name, 32);
            buffer.writeByte(packet.red);
            buffer.writeByte(packet.green);
            buffer.writeByte(packet.blue);
            buffer.writeByte(packet.opacityPercent);
        }

        private static OpenSettingsPacket decode(FriendlyByteBuf buffer) {
            return new OpenSettingsPacket(
                    buffer.readUUID(),
                    buffer.readUtf(32),
                    buffer.readUnsignedByte(),
                    buffer.readUnsignedByte(),
                    buffer.readUnsignedByte(),
                    buffer.readUnsignedByte()
            );
        }

        private static void handle(
                OpenSettingsPacket packet,
                Supplier<NetworkEvent.Context> contextSupplier
        ) {
            NetworkEvent.Context context = contextSupplier.get();
            context.enqueueWork(() ->
                    DistExecutor.unsafeRunWhenOn(
                            Dist.CLIENT,
                            () -> () -> TrailClientPacketHandler.openSettings(
                                    packet.trailId,
                                    packet.name,
                                    packet.red,
                                    packet.green,
                                    packet.blue,
                                    packet.opacityPercent
                            )
                    )
            );
            context.setPacketHandled(true);
        }
    }

    private record SaveSettingsPacket(
            UUID trailId,
            int red,
            int green,
            int blue,
            int opacityPercent
    ) {
        private static void encode(
                SaveSettingsPacket packet,
                FriendlyByteBuf buffer
        ) {
            buffer.writeUUID(packet.trailId);
            buffer.writeByte(packet.red);
            buffer.writeByte(packet.green);
            buffer.writeByte(packet.blue);
            buffer.writeByte(packet.opacityPercent);
        }

        private static SaveSettingsPacket decode(FriendlyByteBuf buffer) {
            return new SaveSettingsPacket(
                    buffer.readUUID(),
                    buffer.readUnsignedByte(),
                    buffer.readUnsignedByte(),
                    buffer.readUnsignedByte(),
                    buffer.readUnsignedByte()
            );
        }

        private static void handle(
                SaveSettingsPacket packet,
                Supplier<NetworkEvent.Context> contextSupplier
        ) {
            NetworkEvent.Context context = contextSupplier.get();
            context.enqueueWork(() -> {
                ServerPlayer player = context.getSender();
                if (player == null || !player.hasPermissions(2)) {
                    return;
                }

                TrailSavedData data = TrailSavedData.get(player.getServer());
                if (!data.setSettings(
                        packet.trailId,
                        packet.red,
                        packet.green,
                        packet.blue,
                        Math.round(packet.opacityPercent * 255.0F / 100.0F)
                )) {
                    return;
                }

                TrailNetwork.syncAll(data.getTrails());
                TrailSavedData.Trail trail = data.getById(packet.trailId);
                if (trail != null) {
                    player.displayClientMessage(
                            net.minecraft.network.chat.Component.literal(
                                    "Ajustes del sendero guardados."
                            ),
                            true
                    );
                }
            });
            context.setPacketHandled(true);
        }
    }

    public static class SyncTrailsPacket {
        private final List<TrailSavedData.Trail> trails;

        public SyncTrailsPacket(List<TrailSavedData.Trail> trails) {
            this.trails = List.copyOf(trails);
        }

        public static void encode(
                SyncTrailsPacket packet,
                FriendlyByteBuf buffer
        ) {
            buffer.writeVarInt(packet.trails.size());
            for (TrailSavedData.Trail trail : packet.trails) {
                buffer.writeUUID(trail.id());
                buffer.writeUtf(trail.name(), 32);
                buffer.writeBoolean(trail.visible());
                buffer.writeByte(trail.red());
                buffer.writeByte(trail.green());
                buffer.writeByte(trail.blue());
                buffer.writeByte(trail.opacity());

                List<TrailSavedData.TrailPoint> points = trail.points();
                buffer.writeVarInt(points.size());
                for (TrailSavedData.TrailPoint point : points) {
                    buffer.writeUtf(point.dimension(), 256);
                    buffer.writeDouble(point.x());
                    buffer.writeDouble(point.y());
                    buffer.writeDouble(point.z());
                }
            }
        }

        public static SyncTrailsPacket decode(FriendlyByteBuf buffer) {
            int trailCount = buffer.readVarInt();
            if (trailCount < 0 || trailCount > 256) {
                throw new IllegalArgumentException("Cantidad de senderos no válida.");
            }

            List<TrailSavedData.Trail> trails = new ArrayList<>(trailCount);
            for (int i = 0; i < trailCount; i++) {
                UUID id = buffer.readUUID();
                String name = buffer.readUtf(32);
                boolean visible = buffer.readBoolean();
                int red = buffer.readUnsignedByte();
                int green = buffer.readUnsignedByte();
                int blue = buffer.readUnsignedByte();
                int opacity = buffer.readUnsignedByte();

                TrailSavedData.Trail trail = new TrailSavedData.Trail(
                        id,
                        name,
                        visible,
                        red,
                        green,
                        blue,
                        opacity
                );

                int pointCount = buffer.readVarInt();
                if (pointCount < 0 || pointCount > 10000) {
                    throw new IllegalArgumentException("Cantidad de puntos no válida.");
                }

                for (int pointIndex = 0; pointIndex < pointCount; pointIndex++) {
                    trail.pointsInternalAdd(new TrailSavedData.TrailPoint(
                            buffer.readUtf(256),
                            buffer.readDouble(),
                            buffer.readDouble(),
                            buffer.readDouble()
                    ));
                }
                trails.add(trail);
            }

            return new SyncTrailsPacket(trails);
        }

        public static void handle(
                SyncTrailsPacket packet,
                Supplier<NetworkEvent.Context> contextSupplier
        ) {
            NetworkEvent.Context context = contextSupplier.get();
            context.enqueueWork(() ->
                    DistExecutor.unsafeRunWhenOn(
                            Dist.CLIENT,
                            () -> () -> TrailClientData.setTrails(packet.trails)
                    )
            );
            context.setPacketHandled(true);
        }
    }
}
