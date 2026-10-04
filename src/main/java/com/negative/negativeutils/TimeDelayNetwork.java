package com.negative.negativeutils;

import java.util.function.Supplier;

import net.minecraft.core.BlockPos;
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

public final class TimeDelayNetwork {
    private static final String PROTOCOL_VERSION = "1";

    private static final SimpleChannel CHANNEL =
            NetworkRegistry.newSimpleChannel(
                    ResourceLocation.fromNamespaceAndPath(
                            "negativeutils",
                            "time_delay"
                    ),
                    () -> PROTOCOL_VERSION,
                    PROTOCOL_VERSION::equals,
                    PROTOCOL_VERSION::equals
            );

    private static int nextMessageId;
    private static boolean registered;

    private TimeDelayNetwork() {
    }

    public static void register() {
        if (registered) {
            return;
        }

        registered = true;
        CHANNEL.registerMessage(
                nextMessageId++,
                SetDelayPacket.class,
                SetDelayPacket::encode,
                SetDelayPacket::decode,
                SetDelayPacket::handle
        );
        CHANNEL.registerMessage(
                nextMessageId++,
                RequestEditorPacket.class,
                RequestEditorPacket::encode,
                RequestEditorPacket::decode,
                RequestEditorPacket::handle
        );
        CHANNEL.registerMessage(
                nextMessageId++,
                OpenEditorPacket.class,
                OpenEditorPacket::encode,
                OpenEditorPacket::decode,
                OpenEditorPacket::handle,
                java.util.Optional.of(NetworkDirection.PLAY_TO_CLIENT)
        );
    }

    public static void setDelay(BlockPos pos, int seconds) {
        CHANNEL.sendToServer(new SetDelayPacket(pos, seconds));
    }

    public static void requestEditor(BlockPos pos) {
        CHANNEL.sendToServer(new RequestEditorPacket(pos));
    }

    private static boolean canAccess(ServerPlayer player, BlockPos pos) {
        return player.hasPermissions(2)
                && player.serverLevel().hasChunkAt(pos)
                && player.distanceToSqr(
                        pos.getX() + 0.5,
                        pos.getY() + 0.5,
                        pos.getZ() + 0.5
                ) <= 64.0;
    }

    private static class SetDelayPacket {
        private final BlockPos pos;
        private final int seconds;

        private SetDelayPacket(BlockPos pos, int seconds) {
            this.pos = pos;
            this.seconds = seconds;
        }

        private static void encode(
                SetDelayPacket packet,
                FriendlyByteBuf buffer
        ) {
            buffer.writeBlockPos(packet.pos);
            buffer.writeVarInt(packet.seconds);
        }

        private static SetDelayPacket decode(FriendlyByteBuf buffer) {
            return new SetDelayPacket(
                    buffer.readBlockPos(),
                    buffer.readVarInt()
            );
        }

        private static void handle(
                SetDelayPacket packet,
                Supplier<NetworkEvent.Context> contextSupplier
        ) {
            NetworkEvent.Context context = contextSupplier.get();

            context.enqueueWork(() -> {
                ServerPlayer player = context.getSender();

                if (player == null || !canAccess(player, packet.pos)) {
                    return;
                }

                if (player.serverLevel().getBlockEntity(packet.pos)
                        instanceof TimeDelayBlockEntity timer) {
                    timer.setDelaySeconds(packet.seconds);
                    player.displayClientMessage(
                            net.minecraft.network.chat.Component.literal(
                                    "Demora configurada: "
                                            + timer.getDelaySeconds()
                                            + " segundos."
                            ),
                            true
                    );
                }
            });

            context.setPacketHandled(true);
        }
    }

    private static class RequestEditorPacket {
        private final BlockPos pos;

        private RequestEditorPacket(BlockPos pos) {
            this.pos = pos;
        }

        private static void encode(
                RequestEditorPacket packet,
                FriendlyByteBuf buffer
        ) {
            buffer.writeBlockPos(packet.pos);
        }

        private static RequestEditorPacket decode(FriendlyByteBuf buffer) {
            return new RequestEditorPacket(buffer.readBlockPos());
        }

        private static void handle(
                RequestEditorPacket packet,
                Supplier<NetworkEvent.Context> contextSupplier
        ) {
            NetworkEvent.Context context = contextSupplier.get();
            context.enqueueWork(() -> {
                ServerPlayer player = context.getSender();
                if (player == null) {
                    return;
                }
                if (!canAccess(player, packet.pos)) {
                    player.displayClientMessage(
                            net.minecraft.network.chat.Component.literal(
                                    "No tienes permiso para editar este bloque."
                            ),
                            true
                    );
                    return;
                }
                if (player.serverLevel().getBlockEntity(packet.pos)
                        instanceof TimeDelayBlockEntity timer) {
                    CHANNEL.send(
                            PacketDistributor.PLAYER.with(() -> player),
                            new OpenEditorPacket(packet.pos, timer.getDelaySeconds())
                    );
                }
            });
            context.setPacketHandled(true);
        }
    }

    private static class OpenEditorPacket {
        private final BlockPos pos;
        private final int seconds;

        private OpenEditorPacket(BlockPos pos, int seconds) {
            this.pos = pos;
            this.seconds = seconds;
        }

        private static void encode(
                OpenEditorPacket packet,
                FriendlyByteBuf buffer
        ) {
            buffer.writeBlockPos(packet.pos);
            buffer.writeVarInt(packet.seconds);
        }

        private static OpenEditorPacket decode(FriendlyByteBuf buffer) {
            return new OpenEditorPacket(
                    buffer.readBlockPos(),
                    buffer.readVarInt()
            );
        }

        private static void handle(
                OpenEditorPacket packet,
                Supplier<NetworkEvent.Context> contextSupplier
        ) {
            NetworkEvent.Context context = contextSupplier.get();
            context.enqueueWork(() ->
                    DistExecutor.unsafeRunWhenOn(
                            Dist.CLIENT,
                            () -> () -> NegativeUtilsClientPacketHandler
                                    .openTimeDelayEditor(
                                            packet.pos,
                                            packet.seconds
                                    )
                    )
            );
            context.setPacketHandled(true);
        }
    }
}