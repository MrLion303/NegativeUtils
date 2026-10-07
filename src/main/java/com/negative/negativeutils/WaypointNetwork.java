package com.negative.negativeutils;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;

public final class WaypointNetwork {
    private static final String PROTOCOL = "5";
    private static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
            ResourceLocation.fromNamespaceAndPath("negativeutils", "waypoints"),
            () -> PROTOCOL, PROTOCOL::equals, PROTOCOL::equals);
    private static boolean registered;

    private WaypointNetwork() {}

    public static void register() {
        if (registered) return;
        registered = true;
        CHANNEL.registerMessage(0, SavePacket.class, SavePacket::encode, SavePacket::decode, SavePacket::handle);
        CHANNEL.registerMessage(1, SyncPacket.class, SyncPacket::encode, SyncPacket::decode, SyncPacket::handle);
        CHANNEL.registerMessage(2, TogglePacket.class, TogglePacket::encode, TogglePacket::decode, TogglePacket::handle);
        CHANNEL.registerMessage(3, DeletePacket.class, DeletePacket::encode, DeletePacket::decode, DeletePacket::handle);
        CHANNEL.registerMessage(4, OpenPacket.class, OpenPacket::encode, OpenPacket::decode, OpenPacket::handle);
        CHANNEL.registerMessage(5, OpenListPacket.class, OpenListPacket::encode, OpenListPacket::decode, OpenListPacket::handle);
    }

    public static void openCreate(ServerPlayer player, String commandId) {
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), OpenPacket.create(player, commandId));
    }

    public static void openEdit(ServerPlayer player, WaypointSavedData.Waypoint waypoint) {
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), OpenPacket.edit(waypoint));
    }

    public static void openList(ServerPlayer player) {
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), new OpenListPacket());
    }

    public static void save(UUID id, String commandId, String name, String dimension,
                            double x, double y, double z, int color, String icon, String corner) {
        CHANNEL.sendToServer(new SavePacket(id, commandId, name, dimension, x, y, z, color, icon, corner));
    }

    public static void toggle(UUID id) { CHANNEL.sendToServer(new TogglePacket(id)); }
    public static void delete(UUID id) { CHANNEL.sendToServer(new DeletePacket(id)); }
    public static void syncAll(List<WaypointSavedData.Waypoint> waypoints) {
        sync(waypoints, PacketDistributor.ALL.noArg());
    }
    public static void syncToPlayer(ServerPlayer player, List<WaypointSavedData.Waypoint> waypoints) {
        sync(waypoints, PacketDistributor.PLAYER.with(() -> player));
    }
    private static void sync(List<WaypointSavedData.Waypoint> waypoints, PacketDistributor.PacketTarget target) {
        CHANNEL.send(target, new SyncPacket(waypoints));
    }

    private record SavePacket(UUID id, String commandId, String name, String dimension,
                              double x, double y, double z, int color, String icon, String corner) {
        static void encode(SavePacket p, FriendlyByteBuf b) {
            b.writeBoolean(p.id != null);
            if (p.id != null) b.writeUUID(p.id);
            b.writeUtf(p.commandId, 48); b.writeUtf(p.name, 32); b.writeUtf(p.dimension, 256);
            b.writeDouble(p.x); b.writeDouble(p.y); b.writeDouble(p.z);
            b.writeInt(p.color); b.writeUtf(p.icon, 4); b.writeUtf(p.corner, 16);
        }
        static SavePacket decode(FriendlyByteBuf b) {
            return new SavePacket(b.readBoolean() ? b.readUUID() : null, b.readUtf(48), b.readUtf(32),
                    b.readUtf(256), b.readDouble(), b.readDouble(), b.readDouble(), b.readInt(),
                    b.readUtf(4), b.readUtf(16));
        }
        static void handle(SavePacket p, Supplier<NetworkEvent.Context> supplier) {
            NetworkEvent.Context context = supplier.get();
            context.enqueueWork(() -> {
                ServerPlayer player = context.getSender();
                if (player == null || !player.hasPermissions(2)) return;
                WaypointSavedData data = WaypointSavedData.get(player.getServer());
                Vec3 position = new Vec3(p.x, p.y, p.z);
                WaypointSavedData.Waypoint waypoint;
                if (p.id == null) {
                    waypoint = data.add(player.getUUID(), p.commandId, p.name, p.dimension,
                            position, p.color, p.icon, p.corner);
                } else {
                    boolean updated = data.update(p.id, p.commandId, p.name, p.dimension,
                            position, p.color, p.icon, p.corner);
                    waypoint = updated ? data.getById(p.id) : null;
                }
                if (waypoint == null) {
                    player.sendSystemMessage(net.minecraft.network.chat.Component.literal(
                            "No se pudo guardar: el identificador está vacío o ya existe."));
                    return;
                }
                syncAll(data.getWaypoints());
                player.sendSystemMessage(net.minecraft.network.chat.Component.literal(
                        "Waypoint guardado: " + waypoint.name() + " (ID: " + waypoint.commandId() + ")"));
            });
            context.setPacketHandled(true);
        }
    }

    private record TogglePacket(UUID id) {
        static void encode(TogglePacket p, FriendlyByteBuf b) { b.writeUUID(p.id); }
        static TogglePacket decode(FriendlyByteBuf b) { return new TogglePacket(b.readUUID()); }
        static void handle(TogglePacket p, Supplier<NetworkEvent.Context> supplier) {
            NetworkEvent.Context context = supplier.get();
            context.enqueueWork(() -> {
                ServerPlayer player = context.getSender();
                if (player == null || !player.hasPermissions(2)) return;
                WaypointSavedData data = WaypointSavedData.get(player.getServer());
                WaypointSavedData.Waypoint waypoint = data.getById(p.id);
                if (waypoint != null) {
                    data.setVisible(waypoint.id(), !waypoint.visible());
                    syncAll(data.getWaypoints());
                }
            });
            context.setPacketHandled(true);
        }
    }

    private record DeletePacket(UUID id) {
        static void encode(DeletePacket p, FriendlyByteBuf b) { b.writeUUID(p.id); }
        static DeletePacket decode(FriendlyByteBuf b) { return new DeletePacket(b.readUUID()); }
        static void handle(DeletePacket p, Supplier<NetworkEvent.Context> supplier) {
            NetworkEvent.Context context = supplier.get();
            context.enqueueWork(() -> {
                ServerPlayer player = context.getSender();
                if (player == null || !player.hasPermissions(2)) return;
                WaypointSavedData data = WaypointSavedData.get(player.getServer());
                if (data.removeById(p.id)) syncAll(data.getWaypoints());
            });
            context.setPacketHandled(true);
        }
    }

    private static class SyncPacket {
        final List<WaypointSavedData.Waypoint> list;
        SyncPacket(List<WaypointSavedData.Waypoint> list) { this.list = List.copyOf(list); }
        static void encode(SyncPacket p, FriendlyByteBuf b) {
            b.writeVarInt(p.list.size());
            for (WaypointSavedData.Waypoint w : p.list) {
                b.writeUUID(w.id()); b.writeUUID(w.owner());
                b.writeUtf(w.commandId(), 48); b.writeUtf(w.name(), 32); b.writeUtf(w.dimension(), 256);
                b.writeDouble(w.x()); b.writeDouble(w.y()); b.writeDouble(w.z());
                b.writeInt(w.color()); b.writeByte(w.shape()); b.writeBoolean(w.visible());
                b.writeUtf(w.icon(), 4); b.writeUtf(w.corner(), 16);
            }
        }
        static SyncPacket decode(FriendlyByteBuf b) {
            int count = b.readVarInt();
            if (count < 0 || count > 512) throw new IllegalArgumentException("Cantidad inválida");
            List<WaypointSavedData.Waypoint> list = new ArrayList<>();
            for (int i = 0; i < count; i++) {
                list.add(new WaypointSavedData.Waypoint(b.readUUID(), b.readUUID(), b.readUtf(48),
                        b.readUtf(32), b.readUtf(256), b.readDouble(), b.readDouble(), b.readDouble(),
                        b.readInt() & 0xFFFFFF, b.readByte(), b.readBoolean(), b.readUtf(4), b.readUtf(16)));
            }
            return new SyncPacket(list);
        }
        static void handle(SyncPacket p, Supplier<NetworkEvent.Context> supplier) {
            NetworkEvent.Context context = supplier.get();
            context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                    () -> () -> WaypointClientData.setWaypoints(p.list)));
            context.setPacketHandled(true);
        }
    }

    private record OpenListPacket() {
        static void encode(OpenListPacket p, FriendlyByteBuf b) { }
        static OpenListPacket decode(FriendlyByteBuf b) { return new OpenListPacket(); }
        static void handle(OpenListPacket p, Supplier<NetworkEvent.Context> supplier) {
            NetworkEvent.Context context = supplier.get();
            context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                    () -> () -> NegativeUtilsClientPacketHandler.openWaypointList()));
            context.setPacketHandled(true);
        }
    }

    private record OpenPacket(boolean edit, UUID id, String commandId, String name, String dimension,
                              double x, double y, double z, int color, String icon, String corner) {
        static OpenPacket create(ServerPlayer player, String commandId) {
            return new OpenPacket(false, null, commandId, "", player.level().dimension().location().toString(),
                    player.getX(), player.getY(), player.getZ(), 0x40D8FF, "◆", "TOP_LEFT");
        }
        static OpenPacket edit(WaypointSavedData.Waypoint w) {
            return new OpenPacket(true, w.id(), w.commandId(), w.name(), w.dimension(),
                    w.x(), w.y(), w.z(), w.color(), w.icon(), w.corner());
        }
        static void encode(OpenPacket p, FriendlyByteBuf b) {
            b.writeBoolean(p.edit); b.writeBoolean(p.id != null);
            if (p.id != null) b.writeUUID(p.id);
            b.writeUtf(p.commandId, 48); b.writeUtf(p.name, 32); b.writeUtf(p.dimension, 256);
            b.writeDouble(p.x); b.writeDouble(p.y); b.writeDouble(p.z);
            b.writeInt(p.color); b.writeUtf(p.icon, 4); b.writeUtf(p.corner, 16);
        }
        static OpenPacket decode(FriendlyByteBuf b) {
            return new OpenPacket(b.readBoolean(), b.readBoolean() ? b.readUUID() : null,
                    b.readUtf(48), b.readUtf(32), b.readUtf(256), b.readDouble(), b.readDouble(),
                    b.readDouble(), b.readInt(), b.readUtf(4), b.readUtf(16));
        }
        static void handle(OpenPacket p, Supplier<NetworkEvent.Context> supplier) {
            NetworkEvent.Context context = supplier.get();
            context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () ->
                    () -> NegativeUtilsClientPacketHandler.openWaypointEditor(p.id, p.commandId, p.name,
                            p.dimension, p.x, p.y, p.z, p.color, p.icon, p.corner)));
            context.setPacketHandled(true);
        }
    }
}