package com.negative.negativeutils;

import com.negative.negativeutils.client.VoicechatMuteClientState;
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

import java.util.Optional;
import java.util.function.Supplier;

/**
 * Sincroniza el estado de mute global para mostrar el indicador de micrófono
 * silenciado junto a los nombres de los jugadores.
 */
public final class VoicechatMuteStateNetwork {
    private static final String PROTOCOL_VERSION = "1";

    private static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
            new ResourceLocation(NegativeUtilsMod.MOD_ID, "voicechat_mute"),
            () -> PROTOCOL_VERSION,
            PROTOCOL_VERSION::equals,
            PROTOCOL_VERSION::equals
    );

    private VoicechatMuteStateNetwork() {
    }

    public static void register() {
        CHANNEL.registerMessage(
                0,
                SyncMuteStatePacket.class,
                SyncMuteStatePacket::encode,
                SyncMuteStatePacket::decode,
                SyncMuteStatePacket::handle,
                Optional.of(NetworkDirection.PLAY_TO_CLIENT)
        );
    }

    public static void syncAll(boolean muted) {
        CHANNEL.send(PacketDistributor.ALL.noArg(), new SyncMuteStatePacket(muted));
    }

    public static void syncToPlayer(ServerPlayer player, boolean muted) {
        CHANNEL.send(
                PacketDistributor.PLAYER.with(() -> player),
                new SyncMuteStatePacket(muted)
        );
    }

    private record SyncMuteStatePacket(boolean muted) {
        private static void encode(SyncMuteStatePacket packet, FriendlyByteBuf buffer) {
            buffer.writeBoolean(packet.muted);
        }

        private static SyncMuteStatePacket decode(FriendlyByteBuf buffer) {
            return new SyncMuteStatePacket(buffer.readBoolean());
        }

        private static void handle(
                SyncMuteStatePacket packet,
                Supplier<NetworkEvent.Context> contextSupplier
        ) {
            NetworkEvent.Context context = contextSupplier.get();
            context.enqueueWork(() ->
                    DistExecutor.unsafeRunWhenOn(
                            Dist.CLIENT,
                            () -> () -> VoicechatMuteClientState.setGloballyMuted(packet.muted)
                    )
            );
            context.setPacketHandled(true);
        }
    }
}
