package com.negative.negativeutils;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Supplier;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;
import net.minecraftforge.registries.ForgeRegistries;

public final class EncyclopediaNetwork {
    private static final String PROTOCOL_VERSION = "1";

    private static final SimpleChannel CHANNEL =
            NetworkRegistry.newSimpleChannel(
                    ResourceLocation.fromNamespaceAndPath(
                            EnciclopediaMod.MOD_ID,
                            "main"
                    ),
                    () -> PROTOCOL_VERSION,
                    PROTOCOL_VERSION::equals,
                    PROTOCOL_VERSION::equals
            );

    private static boolean registered;

    private EncyclopediaNetwork() {
    }

    public static void register() {
        if (registered) {
            return;
        }

        registered = true;

        CHANNEL.registerMessage(
                0,
                RequestEntries.class,
                RequestEntries::encode,
                RequestEntries::decode,
                RequestEntries::handle,
                Optional.of(NetworkDirection.PLAY_TO_SERVER)
        );

        CHANNEL.registerMessage(
                1,
                SyncEntries.class,
                SyncEntries::encode,
                SyncEntries::decode,
                SyncEntries::handle,
                Optional.of(NetworkDirection.PLAY_TO_CLIENT)
        );

        CHANNEL.registerMessage(
                2,
                CreateEntry.class,
                CreateEntry::encode,
                CreateEntry::decode,
                CreateEntry::handle,
                Optional.of(NetworkDirection.PLAY_TO_SERVER)
        );

        CHANNEL.registerMessage(
                3,
                SyncDiscoveries.class,
                SyncDiscoveries::encode,
                SyncDiscoveries::decode,
                SyncDiscoveries::handle,
                Optional.of(NetworkDirection.PLAY_TO_CLIENT)
        );

        CHANNEL.registerMessage(
                4,
                DeleteEntry.class,
                DeleteEntry::encode,
                DeleteEntry::decode,
                DeleteEntry::handle,
                Optional.of(NetworkDirection.PLAY_TO_SERVER)
        );
    }

    public static void requestEntries() {
        CHANNEL.sendToServer(new RequestEntries());
    }

    public static void submitEntry(EncyclopediaData.Entry entry) {
        CHANNEL.sendToServer(new CreateEntry(entry));
    }

    public static void deleteEntry(EncyclopediaData.Entry entry) {
        CHANNEL.sendToServer(new DeleteEntry(entry));
    }

    public static void syncDiscovery(
            ServerPlayer player,
            String notificationText
    ) {
        Set<String> discoveries = PlayerDiscoveryData
                .get(player.server)
                .getDiscoveries(player.getUUID());

        CHANNEL.send(
                PacketDistributor.PLAYER.with(() -> player),
                new SyncDiscoveries(
                        new ArrayList<>(discoveries),
                        notificationText
                )
        );
    }

    private static void sendEntries(ServerPlayer player) {
        List<EncyclopediaData.Entry> entries =
                new ArrayList<>(EncyclopediaData.get(player.server).getEntries());

        CHANNEL.send(
                PacketDistributor.PLAYER.with(() -> player),
                new SyncEntries(entries, player.hasPermissions(2))
        );

        syncDiscovery(player, "");
    }

    private static void broadcastEntries(MinecraftServer server) {
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            sendEntries(player);
        }
    }

    private static boolean isValidTarget(EncyclopediaData.Entry entry) {
        ResourceLocation id = ResourceLocation.tryParse(entry.targetId());

        if (id == null) {
            return false;
        }

        return switch (entry.category()) {
            case ITEM -> ForgeRegistries.ITEMS.containsKey(id);
            case BLOCK -> ForgeRegistries.BLOCKS.containsKey(id);
            case MOB -> ForgeRegistries.ENTITY_TYPES.containsKey(id);
        };
    }

    private static void writeEntry(
            FriendlyByteBuf buffer,
            EncyclopediaData.Entry entry
    ) {
        buffer.writeUtf(entry.targetId(), 256);
        buffer.writeUtf(entry.category().name(), 16);
        buffer.writeUtf(entry.description(), 8192);
        buffer.writeUtf(entry.notificationText(), 256);
    }

    private static EncyclopediaData.Entry readEntry(FriendlyByteBuf buffer) {
        String targetId = buffer.readUtf(256);
        EncyclopediaData.Category category =
                EncyclopediaData.Category.valueOf(buffer.readUtf(16));
        String description = buffer.readUtf(8192);
        String notificationText = buffer.readUtf(256);

        return new EncyclopediaData.Entry(
                targetId,
                category,
                description,
                notificationText
        );
    }

    private static class RequestEntries {
        private static void encode(
                RequestEntries message,
                FriendlyByteBuf buffer
        ) {
        }

        private static RequestEntries decode(FriendlyByteBuf buffer) {
            return new RequestEntries();
        }

        private static void handle(
                RequestEntries message,
                Supplier<NetworkEvent.Context> contextSupplier
        ) {
            NetworkEvent.Context context = contextSupplier.get();
            ServerPlayer player = context.getSender();

            if (player != null) {
                context.enqueueWork(() -> sendEntries(player));
            }

            context.setPacketHandled(true);
        }
    }

    private static class SyncEntries {
        private final List<EncyclopediaData.Entry> entries;
        private final boolean canEdit;

        private SyncEntries(
                List<EncyclopediaData.Entry> entries,
                boolean canEdit
        ) {
            this.entries = List.copyOf(entries);
            this.canEdit = canEdit;
        }

        private static void encode(
                SyncEntries message,
                FriendlyByteBuf buffer
        ) {
            buffer.writeBoolean(message.canEdit);
            buffer.writeVarInt(message.entries.size());

            for (EncyclopediaData.Entry entry : message.entries) {
                writeEntry(buffer, entry);
            }
        }

        private static SyncEntries decode(FriendlyByteBuf buffer) {
            boolean canEdit = buffer.readBoolean();
            int count = buffer.readVarInt();

            if (count < 0 || count > 4096) {
                throw new IllegalArgumentException(
                        "Invalid encyclopedia entry count"
                );
            }

            List<EncyclopediaData.Entry> entries = new ArrayList<>();

            for (int i = 0; i < count; i++) {
                entries.add(readEntry(buffer));
            }

            return new SyncEntries(entries, canEdit);
        }

        private static void handle(
                SyncEntries message,
                Supplier<NetworkEvent.Context> contextSupplier
        ) {
            NetworkEvent.Context context = contextSupplier.get();

            context.enqueueWork(
                    () -> EncyclopediaClientData.setEntries(
                            message.entries,
                            message.canEdit
                    )
            );

            context.setPacketHandled(true);
        }
    }

    private static class CreateEntry {
        private final EncyclopediaData.Entry entry;

        private CreateEntry(EncyclopediaData.Entry entry) {
            this.entry = entry;
        }

        private static void encode(
                CreateEntry message,
                FriendlyByteBuf buffer
        ) {
            writeEntry(buffer, message.entry);
        }

        private static CreateEntry decode(FriendlyByteBuf buffer) {
            return new CreateEntry(readEntry(buffer));
        }

        private static void handle(
                CreateEntry message,
                Supplier<NetworkEvent.Context> contextSupplier
        ) {
            NetworkEvent.Context context = contextSupplier.get();
            ServerPlayer player = context.getSender();

            if (player != null) {
                context.enqueueWork(() -> {
                    if (!player.hasPermissions(2)
                            || !isValidTarget(message.entry)) {
                        return;
                    }

                    EncyclopediaData.get(player.server)
                            .addOrUpdate(message.entry);

                    broadcastEntries(player.server);
                });
            }

            context.setPacketHandled(true);
        }
    }

    private static class SyncDiscoveries {
        private final List<String> discoveries;
        private final String notificationText;

        private SyncDiscoveries(
                List<String> discoveries,
                String notificationText
        ) {
            this.discoveries = List.copyOf(discoveries);
            this.notificationText = notificationText;
        }

        private static void encode(
                SyncDiscoveries message,
                FriendlyByteBuf buffer
        ) {
            buffer.writeVarInt(message.discoveries.size());

            for (String discovery : message.discoveries) {
                buffer.writeUtf(discovery, 512);
            }

            buffer.writeUtf(message.notificationText, 256);
        }

        private static SyncDiscoveries decode(FriendlyByteBuf buffer) {
            int count = buffer.readVarInt();

            if (count < 0 || count > 4096) {
                throw new IllegalArgumentException("Invalid discovery count");
            }

            List<String> discoveries = new ArrayList<>();

            for (int i = 0; i < count; i++) {
                discoveries.add(buffer.readUtf(512));
            }

            String notificationText = buffer.readUtf(256);
            return new SyncDiscoveries(discoveries, notificationText);
        }

        private static void handle(
                SyncDiscoveries message,
                Supplier<NetworkEvent.Context> contextSupplier
        ) {
            NetworkEvent.Context context = contextSupplier.get();

            context.enqueueWork(() -> {
                EncyclopediaClientData.setDiscoveries(
                        new HashSet<>(message.discoveries)
                );

                if (!message.notificationText.isBlank()) {
                    DistExecutor.unsafeRunWhenOn(
                            Dist.CLIENT,
                            () -> () -> NegativeUtilsClientPacketHandler
                                    .showDiscoveryToast(
                                            message.notificationText
                                    )
                    );
                }
            });

            context.setPacketHandled(true);
        }
    }

    private static class DeleteEntry {
        private final EncyclopediaData.Entry entry;

        private DeleteEntry(EncyclopediaData.Entry entry) {
            this.entry = entry;
        }

        private static void encode(
                DeleteEntry message,
                FriendlyByteBuf buffer
        ) {
            writeEntry(buffer, message.entry);
        }

        private static DeleteEntry decode(FriendlyByteBuf buffer) {
            return new DeleteEntry(readEntry(buffer));
        }

        private static void handle(
                DeleteEntry message,
                Supplier<NetworkEvent.Context> contextSupplier
        ) {
            NetworkEvent.Context context = contextSupplier.get();
            ServerPlayer player = context.getSender();

            if (player != null) {
                context.enqueueWork(() -> {
                    if (!player.hasPermissions(2)) {
                        return;
                    }

                    boolean removed = EncyclopediaData.get(player.server)
                            .removeEntry(message.entry);

                    if (removed) {
                        PlayerDiscoveryData.get(player.server)
                                .forgetEntry(message.entry);

                        broadcastEntries(player.server);
                    }
                });
            }

            context.setPacketHandled(true);
        }
    }
}