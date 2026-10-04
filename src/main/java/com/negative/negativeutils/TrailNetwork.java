package com.negative.negativeutils;

import java.util.ArrayList;
import java.util.List;
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
    private static final String PROTOCOL_VERSION = "1";

    public static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
            new ResourceLocation("negativeutils", "trail"),
            () -> PROTOCOL_VERSION,
            PROTOCOL_VERSION::equals,
            PROTOCOL_VERSION::equals
    );

    private static int packetId = 0;

    // Valores iniciales: dorado y opacidad de 90 %.
    private static int red = 255;
    private static int green = 199;
    private static int blue = 31;
    private static int alpha = 230;

    private TrailNetwork() {
    }

    public static void register() {
        CHANNEL.registerMessage(
                packetId++,
                TrailSyncPacket.class,
                TrailSyncPacket::encode,
                TrailSyncPacket::decode,
                TrailSyncPacket::handle
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

    public static void openSettings(ServerPlayer player) {
        CHANNEL.send(
                PacketDistributor.PLAYER.with(() -> player),
                new OpenSettingsPacket(
                        red,
                        green,
                        blue,
                        Math.round(alpha * 100.0F / 255.0F)
                )
        );
    }

    public static void saveSettings(int newRed, int newGreen, int newBlue, int opacityPercent) {
        CHANNEL.sendToServer(
                new SaveSettingsPacket(
                        newRed,
                        newGreen,
                        newBlue,
                        opacityPercent
                )
        );
    }

    public static void sendToAll(List<TrailSavedData.TrailPoint> points) {
        CHANNEL.send(
                PacketDistributor.ALL.noArg(),
                new TrailSyncPacket(points, red, green, blue, alpha)
        );
    }

    public static void sendToPlayer(
            ServerPlayer player,
            List<TrailSavedData.TrailPoint> points
    ) {
        CHANNEL.send(
                PacketDistributor.PLAYER.with(() -> player),
                new TrailSyncPacket(points, red, green, blue, alpha)
        );
    }

    public static void setColor(
            List<TrailSavedData.TrailPoint> points,
            int newRed,
            int newGreen,
            int newBlue,
            int newAlpha
    ) {
        red = clamp(newRed);
        green = clamp(newGreen);
        blue = clamp(newBlue);
        alpha = clamp(newAlpha);

        sendToAll(points);
    }

    private static int clamp(int value) {
        return Math.max(0, Math.min(255, value));
    }

    private record OpenSettingsPacket(
            int red,
            int green,
            int blue,
            int opacityPercent
    ) {
        private static void encode(
                OpenSettingsPacket packet,
                FriendlyByteBuf buffer
        ) {
            buffer.writeByte(packet.red);
            buffer.writeByte(packet.green);
            buffer.writeByte(packet.blue);
            buffer.writeByte(packet.opacityPercent);
        }

        private static OpenSettingsPacket decode(FriendlyByteBuf buffer) {
            return new OpenSettingsPacket(
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
            int red,
            int green,
            int blue,
            int opacityPercent
    ) {
        private static void encode(
                SaveSettingsPacket packet,
                FriendlyByteBuf buffer
        ) {
            buffer.writeByte(packet.red);
            buffer.writeByte(packet.green);
            buffer.writeByte(packet.blue);
            buffer.writeByte(packet.opacityPercent);
        }

        private static SaveSettingsPacket decode(FriendlyByteBuf buffer) {
            return new SaveSettingsPacket(
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
                setColor(
                        data.getPoints(),
                        packet.red,
                        packet.green,
                        packet.blue,
                        Math.round(packet.opacityPercent * 255.0F / 100.0F)
                );
                player.displayClientMessage(
                        net.minecraft.network.chat.Component.literal(
                                "Color de la guía actualizado."
                        ),
                        true
                );
            });
            context.setPacketHandled(true);
        }
    }

    public static class TrailSyncPacket {
        private final List<TrailSavedData.TrailPoint> points;
        private final int red;
        private final int green;
        private final int blue;
        private final int alpha;

        public TrailSyncPacket(
                List<TrailSavedData.TrailPoint> points,
                int red,
                int green,
                int blue,
                int alpha
        ) {
            this.points = List.copyOf(points);
            this.red = red;
            this.green = green;
            this.blue = blue;
            this.alpha = alpha;
        }

        public static void encode(TrailSyncPacket packet, FriendlyByteBuf buffer) {
            buffer.writeVarInt(packet.points.size());

            for (TrailSavedData.TrailPoint point : packet.points) {
                buffer.writeUtf(point.dimension());
                buffer.writeDouble(point.x());
                buffer.writeDouble(point.y());
                buffer.writeDouble(point.z());
            }

            buffer.writeByte(packet.red);
            buffer.writeByte(packet.green);
            buffer.writeByte(packet.blue);
            buffer.writeByte(packet.alpha);
        }

        public static TrailSyncPacket decode(FriendlyByteBuf buffer) {
            int count = buffer.readVarInt();

            if (count < 0 || count > 10000) {
                count = 0;
            }

            List<TrailSavedData.TrailPoint> points = new ArrayList<>(count);

            for (int i = 0; i < count; i++) {
                points.add(new TrailSavedData.TrailPoint(
                        buffer.readUtf(256),
                        buffer.readDouble(),
                        buffer.readDouble(),
                        buffer.readDouble()
                ));
            }

            int red = buffer.readUnsignedByte();
            int green = buffer.readUnsignedByte();
            int blue = buffer.readUnsignedByte();
            int alpha = buffer.readUnsignedByte();

            return new TrailSyncPacket(points, red, green, blue, alpha);
        }

        public static void handle(
                TrailSyncPacket packet,
                Supplier<NetworkEvent.Context> contextSupplier
        ) {
            NetworkEvent.Context context = contextSupplier.get();

            context.enqueueWork(() -> {
                TrailClientData.setPoints(packet.points);
                TrailClientData.setColor(
                        packet.red,
                        packet.green,
                        packet.blue,
                        packet.alpha
                );
            });

            context.setPacketHandled(true);
        }
    }
}