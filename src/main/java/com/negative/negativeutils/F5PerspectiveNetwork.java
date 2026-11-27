package com.negative.negativeutils;

import java.util.Optional;
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

/** Envía al cliente el estado de bloqueo de perspectiva de cada jugador. */
public final class F5PerspectiveNetwork {
    private static final String PROTOCOL_VERSION = "1";
    private static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
            new ResourceLocation(NegativeUtilsMod.MOD_ID, "f5_perspective"),
            () -> PROTOCOL_VERSION,
            PROTOCOL_VERSION::equals,
            PROTOCOL_VERSION::equals
    );

    private static boolean registered;

    private F5PerspectiveNetwork() {}

    public static synchronized void register() {
        if (registered) return;
        registered = true;
        CHANNEL.registerMessage(
                0,
                SyncPacket.class,
                SyncPacket::encode,
                SyncPacket::decode,
                SyncPacket::handle,
                Optional.of(NetworkDirection.PLAY_TO_CLIENT)
        );
    }

    public static void syncToPlayer(ServerPlayer player, boolean disabled) {
        CHANNEL.send(
                PacketDistributor.PLAYER.with(() -> player),
                new SyncPacket(disabled)
        );
    }

    private record SyncPacket(boolean disabled) {
        private static void encode(SyncPacket packet, FriendlyByteBuf buffer) {
            buffer.writeBoolean(packet.disabled);
        }

        private static SyncPacket decode(FriendlyByteBuf buffer) {
            return new SyncPacket(buffer.readBoolean());
        }

        private static void handle(
                SyncPacket packet,
                Supplier<NetworkEvent.Context> contextSupplier
        ) {
            NetworkEvent.Context context = contextSupplier.get();
            context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(
                    Dist.CLIENT,
                    () -> () -> F5PerspectiveClientState.apply(packet.disabled)
            ));
            context.setPacketHandled(true);
        }
    }
}
