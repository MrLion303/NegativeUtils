package com.negative.negativeutils;

import java.util.function.Supplier;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;

public final class FeatureToggleNetwork {
    private static final String PROTOCOL = "1";

    private static final SimpleChannel CHANNEL =
            NetworkRegistry.newSimpleChannel(
                    ResourceLocation.fromNamespaceAndPath(
                            EnciclopediaMod.MOD_ID,
                            "feature_toggles"
                    ),
                    () -> PROTOCOL,
                    PROTOCOL::equals,
                    PROTOCOL::equals
            );

    private static boolean registered;

    private FeatureToggleNetwork() {
    }

    public static void register() {
        if (registered) {
            return;
        }

        registered = true;

        CHANNEL.registerMessage(
                0,
                StatePacket.class,
                StatePacket::encode,
                StatePacket::decode,
                StatePacket::handle
        );
    }

    public static void syncTo(ServerPlayer player) {
        FeatureToggleSavedData data =
                FeatureToggleSavedData.get(player.getServer());

        CHANNEL.send(
                PacketDistributor.PLAYER.with(() -> player),
                new StatePacket(
                        data.areGuildsEnabled(),
                        data.isEncyclopediaEnabled()
                )
        );
    }

    public static void broadcast(MinecraftServer server) {
        FeatureToggleSavedData data =
                FeatureToggleSavedData.get(server);

        CHANNEL.send(
                PacketDistributor.ALL.noArg(),
                new StatePacket(
                        data.areGuildsEnabled(),
                        data.isEncyclopediaEnabled()
                )
        );
    }

    private static class StatePacket {
        private final boolean guildsEnabled;
        private final boolean encyclopediaEnabled;

        private StatePacket(
                boolean guildsEnabled,
                boolean encyclopediaEnabled
        ) {
            this.guildsEnabled = guildsEnabled;
            this.encyclopediaEnabled = encyclopediaEnabled;
        }

        private static void encode(
                StatePacket packet,
                FriendlyByteBuf buffer
        ) {
            buffer.writeBoolean(packet.guildsEnabled);
            buffer.writeBoolean(packet.encyclopediaEnabled);
        }

        private static StatePacket decode(FriendlyByteBuf buffer) {
            return new StatePacket(
                    buffer.readBoolean(),
                    buffer.readBoolean()
            );
        }

        private static void handle(
                StatePacket packet,
                Supplier<NetworkEvent.Context> supplier
        ) {
            NetworkEvent.Context context = supplier.get();

            context.enqueueWork(() ->
                    DistExecutor.unsafeRunWhenOn(
                            Dist.CLIENT,
                            () -> () -> FeatureToggleClientState.apply(
                                    packet.guildsEnabled,
                                    packet.encyclopediaEnabled
                            )
                    )
            );

            context.setPacketHandled(true);
        }
    }
}