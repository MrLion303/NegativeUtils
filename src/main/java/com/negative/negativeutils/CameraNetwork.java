package com.negative.negativeutils;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.function.Supplier;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;

public final class CameraNetwork {
    private static final String PROTOCOL = "1";
    private static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
            ResourceLocation.fromNamespaceAndPath("negativeutils", "cameras"),
            () -> PROTOCOL, PROTOCOL::equals, PROTOCOL::equals);
    private static boolean registered;

    private CameraNetwork() {}

    public static void register() {
        if (registered) return;
        registered = true;
        CHANNEL.registerMessage(0, SavePacket.class, SavePacket::encode, SavePacket::decode, SavePacket::handle);
        CHANNEL.registerMessage(1, OpenPacket.class, OpenPacket::encode, OpenPacket::decode, OpenPacket::handle);
        CHANNEL.registerMessage(2, ShowPacket.class, ShowPacket::encode, ShowPacket::decode, ShowPacket::handle);
        CHANNEL.registerMessage(3, HidePacket.class, HidePacket::encode, HidePacket::decode, HidePacket::handle);
    }

    public static void openCreate(ServerPlayer player, String id) {
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> player),
                OpenPacket.create(id, player.level().dimension().location().toString(),
                        player.getX(), player.getY(), player.getZ()));
    }

    public static void openEdit(ServerPlayer player, CameraSavedData.Camera camera) {
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), OpenPacket.edit(camera));
    }

    public static void show(ServerPlayer player, CameraSavedData.Camera camera) {
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), ShowPacket.from(camera));
    }

    public static void hide(ServerPlayer player) {
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), new HidePacket());
    }

    public static void save(UUID id, String commandId, String name, String targetType,
                            String dimension, double x, double y, double z, int entityId,
                            String playerName, boolean forceLook) {
        CHANNEL.sendToServer(new SavePacket(id, commandId, name, targetType, dimension,
                x, y, z, entityId, playerName == null ? "" : playerName, forceLook));
    }

    private record SavePacket(UUID id, String commandId, String name, String targetType,
                              String dimension, double x, double y, double z,
                              int entityId, String playerName, boolean forceLook) {
        static void encode(SavePacket p, FriendlyByteBuf b) {
            b.writeBoolean(p.id != null);
            if (p.id != null) b.writeUUID(p.id);
            b.writeUtf(p.commandId, 48);
            b.writeUtf(p.name, 48);
            b.writeUtf(p.targetType, 8);
            b.writeUtf(p.dimension, 256);
            b.writeDouble(p.x); b.writeDouble(p.y); b.writeDouble(p.z);
            b.writeInt(p.entityId);
            b.writeUtf(p.playerName, 32);
            b.writeBoolean(p.forceLook);
        }

        static SavePacket decode(FriendlyByteBuf b) {
            return new SavePacket(
                    b.readBoolean() ? b.readUUID() : null,
                    b.readUtf(48), b.readUtf(48), b.readUtf(8), b.readUtf(256),
                    b.readDouble(), b.readDouble(), b.readDouble(), b.readInt(),
                    b.readUtf(32), b.readBoolean());
        }

        static void handle(SavePacket p, Supplier<NetworkEvent.Context> supplier) {
            NetworkEvent.Context context = supplier.get();
            context.enqueueWork(() -> {
                ServerPlayer sender = context.getSender();
                if (sender == null || !sender.hasPermissions(2)) return;

                CameraSavedData data = CameraSavedData.get(sender.getServer());
                String type = CameraSavedData.sanitizeTargetType(p.targetType);
                String playerName = p.playerName.trim();
                UUID playerUuid = null;

                if (type.equals("PLAYER")) {
                    ServerPlayer target = sender.getServer().getPlayerList().getPlayerByName(playerName);
                    if (target == null) {
                        sender.sendSystemMessage(Component.literal(
                                "El jugador no está conectado: " + playerName));
                        return;
                    }
                    playerUuid = target.getUUID();
                } else if (type.equals("ENTITY")) {
                    Entity target = sender.level().getEntity(p.entityId);
                    if (target == null) {
                        sender.sendSystemMessage(Component.literal(
                                "No existe una entidad con ID " + p.entityId + " en tu dimensión."));
                        return;
                    }
                }

                CameraSavedData.Camera camera;
                if (p.id == null) {
                    camera = data.add(sender.getUUID(), p.commandId, p.name, type, p.dimension,
                            p.x, p.y, p.z, p.entityId, playerName, playerUuid, p.forceLook);
                } else {
                    camera = data.getById(p.id);
                    boolean updated = data.update(p.id, p.commandId, p.name, type, p.dimension,
                            p.x, p.y, p.z, p.entityId, playerName, playerUuid, p.forceLook);
                    camera = updated ? data.getById(p.id) : null;
                }

                if (camera == null) {
                    sender.sendSystemMessage(Component.literal(
                            "No se pudo guardar la cámara. Comprueba el ID."));
                    return;
                }
                sender.sendSystemMessage(Component.literal(
                        "Cámara guardada: " + camera.name() + " (ID: " + camera.commandId() + ")"));
            });
            context.setPacketHandled(true);
        }
    }

    private record OpenPacket(boolean edit, UUID id, String commandId, String name,
                              String targetType, String dimension, double x, double y, double z,
                              int entityId, String playerName, boolean forceLook) {
        static OpenPacket create(String commandId, String dimension, double x, double y, double z) {
            return new OpenPacket(false, null, commandId, "", "COORDS", dimension,
                    x, y, z, 0, "", false);
        }

        static OpenPacket edit(CameraSavedData.Camera c) {
            return new OpenPacket(true, c.id(), c.commandId(), c.name(), c.targetType(),
                    c.dimension(), c.x(), c.y(), c.z(), c.entityId(), c.playerName(), c.forceLook());
        }

        static void encode(OpenPacket p, FriendlyByteBuf b) {
            b.writeBoolean(p.edit);
            b.writeBoolean(p.id != null);
            if (p.id != null) b.writeUUID(p.id);
            b.writeUtf(p.commandId, 48); b.writeUtf(p.name, 48); b.writeUtf(p.targetType, 8);
            b.writeUtf(p.dimension, 256);
            b.writeDouble(p.x); b.writeDouble(p.y); b.writeDouble(p.z);
            b.writeInt(p.entityId); b.writeUtf(p.playerName, 32); b.writeBoolean(p.forceLook);
        }

        static OpenPacket decode(FriendlyByteBuf b) {
            return new OpenPacket(
                    b.readBoolean(), b.readBoolean() ? b.readUUID() : null,
                    b.readUtf(48), b.readUtf(48), b.readUtf(8), b.readUtf(256),
                    b.readDouble(), b.readDouble(), b.readDouble(), b.readInt(),
                    b.readUtf(32), b.readBoolean());
        }

        static void handle(OpenPacket p, Supplier<NetworkEvent.Context> supplier) {
            NetworkEvent.Context context = supplier.get();
            context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                    () -> () -> NegativeUtilsClientPacketHandler.openCameraEditor(
                            p.id, p.commandId, p.name, p.targetType, p.dimension,
                            p.x, p.y, p.z, p.entityId, p.playerName, p.forceLook)));
            context.setPacketHandled(true);
        }
    }

    private record ShowPacket(UUID id, String commandId, String name, String targetType,
                              String dimension, double x, double y, double z,
                              int entityId, String playerName, boolean forceLook) {
        static ShowPacket from(CameraSavedData.Camera c) {
            return new ShowPacket(c.id(), c.commandId(), c.name(), c.targetType(), c.dimension(),
                    c.x(), c.y(), c.z(), c.entityId(), c.playerName(), c.forceLook());
        }

        static void encode(ShowPacket p, FriendlyByteBuf b) {
            b.writeUUID(p.id); b.writeUtf(p.commandId, 48); b.writeUtf(p.name, 48);
            b.writeUtf(p.targetType, 8); b.writeUtf(p.dimension, 256);
            b.writeDouble(p.x); b.writeDouble(p.y); b.writeDouble(p.z);
            b.writeInt(p.entityId); b.writeUtf(p.playerName, 32); b.writeBoolean(p.forceLook);
        }

        static ShowPacket decode(FriendlyByteBuf b) {
            return new ShowPacket(b.readUUID(), b.readUtf(48), b.readUtf(48), b.readUtf(8),
                    b.readUtf(256), b.readDouble(), b.readDouble(), b.readDouble(), b.readInt(),
                    b.readUtf(32), b.readBoolean());
        }

        static void handle(ShowPacket p, Supplier<NetworkEvent.Context> supplier) {
            NetworkEvent.Context context = supplier.get();
            context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                    () -> () -> CameraClientData.show(new CameraClientData.Active(
                            p.id(), p.commandId(), p.name(), p.targetType(), p.dimension(),
                            p.x(), p.y(), p.z(), p.entityId(), p.playerName(), p.forceLook))));
            context.setPacketHandled(true);
        }
    }

    private record HidePacket() {
        static void encode(HidePacket p, FriendlyByteBuf b) {}
        static HidePacket decode(FriendlyByteBuf b) { return new HidePacket(); }

        static void handle(HidePacket p, Supplier<NetworkEvent.Context> supplier) {
            NetworkEvent.Context context = supplier.get();
            context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                    () -> CameraClientData::hide));
            context.setPacketHandled(true);
        }
    }
}
