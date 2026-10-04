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

public final class CommandSequenceNetwork {
    private static final String PROTOCOL = "1";
    private static final SimpleChannel CHANNEL =
            NetworkRegistry.newSimpleChannel(
                    ResourceLocation.fromNamespaceAndPath(
                            "negativeutils",
                            "command_sequence"
                    ),
                    () -> PROTOCOL,
                    PROTOCOL::equals,
                    PROTOCOL::equals
            );

    private static boolean registered;

    private CommandSequenceNetwork() {
    }

    public static void register() {
        if (registered) {
            return;
        }
        registered = true;
        CHANNEL.registerMessage(
                0,
                SaveCommandsPacket.class,
                SaveCommandsPacket::encode,
                SaveCommandsPacket::decode,
                SaveCommandsPacket::handle
        );
        CHANNEL.registerMessage(
                1,
                RequestCommandsPacket.class,
                RequestCommandsPacket::encode,
                RequestCommandsPacket::decode,
                RequestCommandsPacket::handle
        );
        CHANNEL.registerMessage(
                2,
                OpenEditorPacket.class,
                OpenEditorPacket::encode,
                OpenEditorPacket::decode,
                OpenEditorPacket::handle,
                java.util.Optional.of(NetworkDirection.PLAY_TO_CLIENT)
        );
    }

    public static void saveCommands(BlockPos pos, String commands) {
        CHANNEL.sendToServer(new SaveCommandsPacket(pos, commands));
    }

    public static void requestEditor(BlockPos pos) {
        CHANNEL.sendToServer(new RequestCommandsPacket(pos));
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

    private record SaveCommandsPacket(BlockPos pos, String commands) {
        private static void encode(
                SaveCommandsPacket packet,
                FriendlyByteBuf buffer
        ) {
            buffer.writeBlockPos(packet.pos);
            buffer.writeUtf(
                    packet.commands,
                    CommandSequenceBlockEntity.MAX_COMMANDS_LENGTH
            );
        }

        private static SaveCommandsPacket decode(FriendlyByteBuf buffer) {
            return new SaveCommandsPacket(
                    buffer.readBlockPos(),
                    buffer.readUtf(
                            CommandSequenceBlockEntity.MAX_COMMANDS_LENGTH
                    )
            );
        }

        private static void handle(
                SaveCommandsPacket packet,
                Supplier<NetworkEvent.Context> contextSupplier
        ) {
            NetworkEvent.Context context = contextSupplier.get();
            context.enqueueWork(() -> {
                ServerPlayer player = context.getSender();
                if (player == null || !canAccess(player, packet.pos)) {
                    return;
                }

                if (player.serverLevel().getBlockEntity(packet.pos)
                        instanceof CommandSequenceBlockEntity sequence) {
                    sequence.setCommands(packet.commands);
                    player.displayClientMessage(
                            net.minecraft.network.chat.Component.literal(
                                    "Secuencia guardada."
                            ),
                            true
                    );
                }
            });
            context.setPacketHandled(true);
        }
    }

    private record RequestCommandsPacket(BlockPos pos) {
        private static void encode(
                RequestCommandsPacket packet,
                FriendlyByteBuf buffer
        ) {
            buffer.writeBlockPos(packet.pos);
        }

        private static RequestCommandsPacket decode(FriendlyByteBuf buffer) {
            return new RequestCommandsPacket(buffer.readBlockPos());
        }

        private static void handle(
                RequestCommandsPacket packet,
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
                        instanceof CommandSequenceBlockEntity sequence) {
                    CHANNEL.send(
                            PacketDistributor.PLAYER.with(() -> player),
                            new OpenEditorPacket(packet.pos, sequence.getCommands())
                    );
                }
            });
            context.setPacketHandled(true);
        }
    }

    private record OpenEditorPacket(BlockPos pos, String commands) {
        private static void encode(
                OpenEditorPacket packet,
                FriendlyByteBuf buffer
        ) {
            buffer.writeBlockPos(packet.pos);
            buffer.writeUtf(
                    packet.commands,
                    CommandSequenceBlockEntity.MAX_COMMANDS_LENGTH
            );
        }

        private static OpenEditorPacket decode(FriendlyByteBuf buffer) {
            return new OpenEditorPacket(
                    buffer.readBlockPos(),
                    buffer.readUtf(CommandSequenceBlockEntity.MAX_COMMANDS_LENGTH)
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
                                    .openCommandSequenceEditor(
                                            packet.pos,
                                            packet.commands
                                    )
                    )
            );
            context.setPacketHandled(true);
        }
    }
}
