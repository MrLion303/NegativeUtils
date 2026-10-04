package com.negative.negativeutils;

import java.util.Map;
import java.io.IOException;
import java.util.function.Supplier;
import net.minecraft.network.chat.Component;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;

public final class DiscordEmoteNetwork {
    private static final String PROTOCOL = "1";
    private static final SimpleChannel CHANNEL =
            NetworkRegistry.newSimpleChannel(
                    ResourceLocation.fromNamespaceAndPath(
                            "negativeutils",
                            "discord_emotes"
                    ),
                    () -> PROTOCOL,
                    PROTOCOL::equals,
                    PROTOCOL::equals
            );
    private static boolean registered;

    private DiscordEmoteNetwork() {
    }

    public static void register() {
        if (registered) {
            return;
        }
        registered = true;
        CHANNEL.registerMessage(
                0,
                SyncEmotesPacket.class,
                SyncEmotesPacket::encode,
                SyncEmotesPacket::decode,
                SyncEmotesPacket::handle
        );
        CHANNEL.registerMessage(
                1,
                SetTokenPacket.class,
                SetTokenPacket::encode,
                SetTokenPacket::decode,
                SetTokenPacket::handle
        );
        CHANNEL.registerMessage(
                2,
                OpenTokenScreenPacket.class,
                OpenTokenScreenPacket::encode,
                OpenTokenScreenPacket::decode,
                OpenTokenScreenPacket::handle
        );
    }

    public static void setToken(String token) {
        CHANNEL.sendToServer(new SetTokenPacket(token));
    }

    public static void openTokenScreen(ServerPlayer player) {
        CHANNEL.send(
                PacketDistributor.PLAYER.with(() -> player),
                new OpenTokenScreenPacket()
        );
    }

    public static void syncAll(Map<String, Integer> mapping, byte[] atlas) {
        SyncEmotesPacket packet = new SyncEmotesPacket(mapping, atlas);
        CHANNEL.send(PacketDistributor.ALL.noArg(), packet);
    }

    public static void syncToPlayer(
            ServerPlayer player,
            Map<String, Integer> mapping,
            byte[] atlas
    ) {
        CHANNEL.send(
                PacketDistributor.PLAYER.with(() -> player),
                new SyncEmotesPacket(mapping, atlas)
        );
    }

    private record SyncEmotesPacket(
            Map<String, Integer> mapping,
            byte[] atlas
    ) {
        private SyncEmotesPacket {
            mapping = Map.copyOf(mapping);
            atlas = atlas.clone();
        }

        private static void encode(
                SyncEmotesPacket packet,
                FriendlyByteBuf buffer
        ) {
            buffer.writeVarInt(packet.mapping.size());
            packet.mapping.forEach((name, codePoint) -> {
                buffer.writeUtf(name, 32);
                buffer.writeShort(codePoint);
            });
            buffer.writeByteArray(packet.atlas);
        }

        private static SyncEmotesPacket decode(FriendlyByteBuf buffer) {
            int count = buffer.readVarInt();
            if (count < 0 || count > DiscordEmoteService.MAX_EMOTES) {
                throw new IllegalArgumentException(
                        "Cantidad de emotes no válida: " + count
                );
            }
            Map<String, Integer> mapping = new java.util.LinkedHashMap<>();
            for (int i = 0; i < count; i++) {
                mapping.put(buffer.readUtf(32), buffer.readUnsignedShort());
            }
            byte[] atlas = buffer.readByteArray(2_000_000);
            return new SyncEmotesPacket(mapping, atlas);
        }

        private static void handle(
                SyncEmotesPacket packet,
                Supplier<NetworkEvent.Context> contextSupplier
        ) {
            NetworkEvent.Context context = contextSupplier.get();
            context.enqueueWork(() ->
                    DistExecutor.unsafeRunWhenOn(
                            Dist.CLIENT,
                            () -> () -> NegativeUtilsClientPacketHandler
                                    .acceptDiscordEmotes(
                                            packet.mapping,
                                            packet.atlas
                                    )
                    )
            );
            context.setPacketHandled(true);
        }
    }

    private record SetTokenPacket(String token) {
        private static void encode(SetTokenPacket packet, FriendlyByteBuf buffer) {
            buffer.writeUtf(packet.token, 256);
        }

        private static SetTokenPacket decode(FriendlyByteBuf buffer) {
            return new SetTokenPacket(buffer.readUtf(256));
        }

        private static void handle(
                SetTokenPacket packet,
                Supplier<NetworkEvent.Context> contextSupplier
        ) {
            NetworkEvent.Context context = contextSupplier.get();
            context.enqueueWork(() -> {
                ServerPlayer player = context.getSender();
                if (player == null
                        || !player.hasPermissions(2)
                        || player.getServer() == null) {
                    return;
                }

                try {
                    DiscordEmoteService.setToken(
                            player.getServer(),
                            packet.token
                    );
                    player.displayClientMessage(
                            Component.literal(
                                    "Token guardado. Si DiscordSRV está activo se usará su conexión; si no, usa /negativeutils discord sync."
                            ),
                            true
                    );
                } catch (IllegalArgumentException | IOException exception) {
                    player.displayClientMessage(
                            Component.literal(
                                    "No se pudo guardar el token: "
                                            + exception.getMessage()
                            ),
                            true
                    );
                }
            });
            context.setPacketHandled(true);
        }
    }

    private record OpenTokenScreenPacket() {
        private static void encode(
                OpenTokenScreenPacket packet,
                FriendlyByteBuf buffer
        ) {
        }

        private static OpenTokenScreenPacket decode(FriendlyByteBuf buffer) {
            return new OpenTokenScreenPacket();
        }

        private static void handle(
                OpenTokenScreenPacket packet,
                Supplier<NetworkEvent.Context> contextSupplier
        ) {
            NetworkEvent.Context context = contextSupplier.get();
            context.enqueueWork(() ->
                    DistExecutor.unsafeRunWhenOn(
                            Dist.CLIENT,
                            () -> () -> NegativeUtilsClientPacketHandler
                                    .openDiscordTokenScreen()
                    )
            );
            context.setPacketHandled(true);
        }
    }
}
