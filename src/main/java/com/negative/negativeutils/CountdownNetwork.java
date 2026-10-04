package com.negative.negativeutils;

import java.util.function.Supplier;

import net.minecraft.network.FriendlyByteBuf;
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

public final class CountdownNetwork {
    private static final String PROTOCOL_VERSION = "1";

    public static final SimpleChannel CHANNEL =
            NetworkRegistry.newSimpleChannel(
                    new ResourceLocation("negativeutils", "countdown"),
                    () -> PROTOCOL_VERSION,
                    PROTOCOL_VERSION::equals,
                    PROTOCOL_VERSION::equals
            );

    private static int nextMessageId = 0;

    private CountdownNetwork() {
    }

    @Mod.EventBusSubscriber(
            modid = "negativeutils",
            bus = Mod.EventBusSubscriber.Bus.MOD
    )
    public static class Registration {
        @SubscribeEvent
        public static void onCommonSetup(FMLCommonSetupEvent event) {
            event.enqueueWork(() -> {
                CHANNEL.registerMessage(
                        nextMessageId++,
                        StartCountdownPacket.class,
                        StartCountdownPacket::encode,
                        StartCountdownPacket::decode,
                        StartCountdownPacket::handle
                );

                CHANNEL.registerMessage(
                        nextMessageId++,
                        SyncCountdownPacket.class,
                        SyncCountdownPacket::encode,
                        SyncCountdownPacket::decode,
                        SyncCountdownPacket::handle
                );

                CHANNEL.registerMessage(
                        nextMessageId++,
                        OpenAdminPanelPacket.class,
                        OpenAdminPanelPacket::encode,
                        OpenAdminPanelPacket::decode,
                        OpenAdminPanelPacket::handle
                );

                CHANNEL.registerMessage(
                        nextMessageId++,
                        ResetCountdownPacket.class,
                        ResetCountdownPacket::encode,
                        ResetCountdownPacket::decode,
                        ResetCountdownPacket::handle
                );
            });
        }
    }

    public static void startCountdown(
            long targetTimeMillis,
            String displayText,
            int displayColor,
            String displayPosition
    ) {
        CHANNEL.sendToServer(
                new StartCountdownPacket(
                        targetTimeMillis,
                        displayText,
                        displayColor,
                        displayPosition
                )
        );
    }

    public static void resetCountdown() {
        CHANNEL.sendToServer(new ResetCountdownPacket());
    }

    public static void openAdminPanel(ServerPlayer player) {
        CHANNEL.send(
                PacketDistributor.PLAYER.with(() -> player),
                new OpenAdminPanelPacket()
        );
    }

    public static void syncToPlayer(
            ServerPlayer player,
            CountdownSavedData data
    ) {
        CHANNEL.send(
                PacketDistributor.PLAYER.with(() -> player),
                createSyncPacket(data)
        );
    }

    public static void syncToEveryone(CountdownSavedData data) {
        CHANNEL.send(
                PacketDistributor.ALL.noArg(),
                createSyncPacket(data)
        );
    }

    private static SyncCountdownPacket createSyncPacket(
            CountdownSavedData data
    ) {
        return new SyncCountdownPacket(
                data.isConfigured(),
                data.isRunning(),
                data.isFinished(),
                data.getEndTimeMillis(),
                data.isRunning() ? 0 : data.getRemainingMillis(),
                data.getDisplayText(),
                data.getDisplayColor(),
                data.getDisplayPosition()
        );
    }

    private static boolean isValidPosition(String position) {
        return "BOSSBAR".equals(position)
                || "ACTIONBAR".equals(position)
                || "SCOREBOARD".equals(position)
                || "TITLE".equals(position);
    }

    private static class StartCountdownPacket {
        private final long targetTimeMillis;
        private final String displayText;
        private final int displayColor;
        private final String displayPosition;

        private StartCountdownPacket(
                long targetTimeMillis,
                String displayText,
                int displayColor,
                String displayPosition
        ) {
            this.targetTimeMillis = targetTimeMillis;
            this.displayText = displayText == null
                    ? ""
                    : displayText.substring(0, Math.min(displayText.length(), 100));
            this.displayColor = displayColor & 0xFFFFFF;
            this.displayPosition = isValidPosition(displayPosition)
                    ? displayPosition
                    : "BOSSBAR";
        }

        private static void encode(
                StartCountdownPacket packet,
                FriendlyByteBuf buffer
        ) {
            buffer.writeLong(packet.targetTimeMillis);
            buffer.writeUtf(packet.displayText, 100);
            buffer.writeInt(packet.displayColor);
            buffer.writeUtf(packet.displayPosition, 16);
        }

        private static StartCountdownPacket decode(FriendlyByteBuf buffer) {
            return new StartCountdownPacket(
                    buffer.readLong(),
                    buffer.readUtf(100),
                    buffer.readInt(),
                    buffer.readUtf(16)
            );
        }

        private static void handle(
                StartCountdownPacket packet,
                Supplier<NetworkEvent.Context> contextSupplier
        ) {
            NetworkEvent.Context context = contextSupplier.get();

            context.enqueueWork(() -> {
                ServerPlayer player = context.getSender();

                if (player == null || !player.hasPermissions(2)) {
                    return;
                }

                long durationMillis = Math.max(
                        0,
                        packet.targetTimeMillis - System.currentTimeMillis()
                );

                CountdownSavedData data =
                        CountdownSavedData.get(player.getServer());

                data.start(
                        durationMillis,
                        packet.displayText,
                        packet.displayColor,
                        packet.displayPosition
                );

                syncToEveryone(data);
            });

            context.setPacketHandled(true);
        }
    }

    private static class ResetCountdownPacket {
        private ResetCountdownPacket() {
        }

        private static void encode(
                ResetCountdownPacket packet,
                FriendlyByteBuf buffer
        ) {
            // Este paquete no necesita datos.
        }

        private static ResetCountdownPacket decode(FriendlyByteBuf buffer) {
            return new ResetCountdownPacket();
        }

        private static void handle(
                ResetCountdownPacket packet,
                Supplier<NetworkEvent.Context> contextSupplier
        ) {
            NetworkEvent.Context context = contextSupplier.get();

            context.enqueueWork(() -> {
                ServerPlayer player = context.getSender();

                if (player == null || !player.hasPermissions(2)) {
                    return;
                }

                CountdownSavedData data =
                        CountdownSavedData.get(player.getServer());

                data.reset();
                syncToEveryone(data);
            });

            context.setPacketHandled(true);
        }
    }

    private static class SyncCountdownPacket {
        private final boolean configured;
        private final boolean running;
        private final boolean finished;
        private final long endTimeMillis;
        private final long pausedRemainingMillis;
        private final String displayText;
        private final int displayColor;
        private final String displayPosition;

        private SyncCountdownPacket(
                boolean configured,
                boolean running,
                boolean finished,
                long endTimeMillis,
                long pausedRemainingMillis,
                String displayText,
                int displayColor,
                String displayPosition
        ) {
            this.configured = configured;
            this.running = running;
            this.finished = finished;
            this.endTimeMillis = endTimeMillis;
            this.pausedRemainingMillis = pausedRemainingMillis;
            this.displayText = displayText == null
                    ? ""
                    : displayText.substring(0, Math.min(displayText.length(), 100));
            this.displayColor = displayColor & 0xFFFFFF;
            this.displayPosition = isValidPosition(displayPosition)
                    ? displayPosition
                    : "BOSSBAR";
        }

        private static void encode(
                SyncCountdownPacket packet,
                FriendlyByteBuf buffer
        ) {
            buffer.writeBoolean(packet.configured);
            buffer.writeBoolean(packet.running);
            buffer.writeBoolean(packet.finished);
            buffer.writeLong(packet.endTimeMillis);
            buffer.writeLong(packet.pausedRemainingMillis);
            buffer.writeUtf(packet.displayText, 100);
            buffer.writeInt(packet.displayColor);
            buffer.writeUtf(packet.displayPosition, 16);
        }

        private static SyncCountdownPacket decode(FriendlyByteBuf buffer) {
            return new SyncCountdownPacket(
                    buffer.readBoolean(),
                    buffer.readBoolean(),
                    buffer.readBoolean(),
                    buffer.readLong(),
                    buffer.readLong(),
                    buffer.readUtf(100),
                    buffer.readInt(),
                    buffer.readUtf(16)
            );
        }

        private static void handle(
                SyncCountdownPacket packet,
                Supplier<NetworkEvent.Context> contextSupplier
        ) {
            NetworkEvent.Context context = contextSupplier.get();

            context.enqueueWork(() ->
                    DistExecutor.unsafeRunWhenOn(
                            Dist.CLIENT,
                            () -> () -> NegativeUtilsClientPacketHandler
                                    .updateCountdown(
                                            packet.configured,
                                            packet.running,
                                            packet.finished,
                                            packet.endTimeMillis,
                                            packet.pausedRemainingMillis,
                                            packet.displayText,
                                            packet.displayColor,
                                            packet.displayPosition
                                    )
                    )
            );

            context.setPacketHandled(true);
        }
    }

    private static class OpenAdminPanelPacket {
        private OpenAdminPanelPacket() {
        }

        private static void encode(
                OpenAdminPanelPacket packet,
                FriendlyByteBuf buffer
        ) {
            // Este paquete no necesita datos.
        }

        private static OpenAdminPanelPacket decode(FriendlyByteBuf buffer) {
            return new OpenAdminPanelPacket();
        }

        private static void handle(
                OpenAdminPanelPacket packet,
                Supplier<NetworkEvent.Context> contextSupplier
        ) {
            NetworkEvent.Context context = contextSupplier.get();

            context.enqueueWork(() ->
                    DistExecutor.unsafeRunWhenOn(
                            Dist.CLIENT,
                            () -> () -> NegativeUtilsClientPacketHandler
                                    .openAdminPanel()
                    )
            );

            context.setPacketHandled(true);
        }
    }
}