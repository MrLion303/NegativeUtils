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
    private static final String PROTOCOL="4";
    private static final SimpleChannel CHANNEL=NetworkRegistry.newSimpleChannel(ResourceLocation.fromNamespaceAndPath("negativeutils","waypoints"),()->PROTOCOL,PROTOCOL::equals,PROTOCOL::equals);
    private static boolean registered;
    private WaypointNetwork(){}
    public static void register(){
        if(registered)return;registered=true;
        CHANNEL.registerMessage(0,SavePacket.class,SavePacket::encode,SavePacket::decode,SavePacket::handle);
        CHANNEL.registerMessage(1,SyncPacket.class,SyncPacket::encode,SyncPacket::decode,SyncPacket::handle);
        CHANNEL.registerMessage(2,TogglePacket.class,TogglePacket::encode,TogglePacket::decode,TogglePacket::handle);
        CHANNEL.registerMessage(3,DeletePacket.class,DeletePacket::encode,DeletePacket::decode,DeletePacket::handle);
        CHANNEL.registerMessage(4,OpenPacket.class,OpenPacket::encode,OpenPacket::decode,OpenPacket::handle);
    }
    public static void openCreate(ServerPlayer p){CHANNEL.send(PacketDistributor.PLAYER.with(()->p),OpenPacket.create(p));}
    public static void openEdit(ServerPlayer p,WaypointSavedData.Waypoint w){CHANNEL.send(PacketDistributor.PLAYER.with(()->p),OpenPacket.edit(w));}
    public static void save(UUID id,String name,String dimension,double x,double y,double z,int color,String icon){CHANNEL.sendToServer(new SavePacket(id,name,dimension,x,y,z,color,icon));}
    public static void toggle(UUID id){CHANNEL.sendToServer(new TogglePacket(id));}
    public static void delete(UUID id){CHANNEL.sendToServer(new DeletePacket(id));}
    public static void syncAll(List<WaypointSavedData.Waypoint> ws){sync(ws,PacketDistributor.ALL.noArg());}
    public static void syncToPlayer(ServerPlayer p,List<WaypointSavedData.Waypoint> ws){sync(ws,PacketDistributor.PLAYER.with(()->p));}
    private static void sync(List<WaypointSavedData.Waypoint> ws,PacketDistributor.PacketTarget target){CHANNEL.send(target,new SyncPacket(ws));}

    private record SavePacket(UUID id,String name,String dimension,double x,double y,double z,int color,String icon){
        static void encode(SavePacket p,FriendlyByteBuf b){b.writeBoolean(p.id!=null);if(p.id!=null)b.writeUUID(p.id);b.writeUtf(p.name,32);b.writeUtf(p.dimension,256);b.writeDouble(p.x);b.writeDouble(p.y);b.writeDouble(p.z);b.writeInt(p.color);b.writeUtf(p.icon,4);}
        static SavePacket decode(FriendlyByteBuf b){return new SavePacket(b.readBoolean()?b.readUUID():null,b.readUtf(32),b.readUtf(256),b.readDouble(),b.readDouble(),b.readDouble(),b.readInt(),b.readUtf(4));}
        static void handle(SavePacket p,Supplier<NetworkEvent.Context> s){var c=s.get();c.enqueueWork(()->{ServerPlayer pl=c.getSender();if(pl==null||!pl.hasPermissions(2))return;var d=WaypointSavedData.get(pl.getServer());Vec3 pos=new Vec3(p.x,p.y,p.z);WaypointSavedData.Waypoint w;if(p.id==null)w=d.add(pl.getUUID(),p.name,p.dimension,pos,p.color,p.icon);else{d.update(p.id,p.name,p.dimension,pos,p.color,p.icon);w=d.getById(p.id);}if(w!=null){syncAll(d.getWaypoints());pl.sendSystemMessage(net.minecraft.network.chat.Component.literal("Waypoint guardado: "+w.name()));}});c.setPacketHandled(true);}
    }
    private record TogglePacket(UUID id){
        static void encode(TogglePacket p,FriendlyByteBuf b){b.writeUUID(p.id);}static TogglePacket decode(FriendlyByteBuf b){return new TogglePacket(b.readUUID());}
        static void handle(TogglePacket p,Supplier<NetworkEvent.Context> s){var c=s.get();c.enqueueWork(()->{ServerPlayer pl=c.getSender();if(pl==null||!pl.hasPermissions(2))return;var d=WaypointSavedData.get(pl.getServer());var w=d.getById(p.id);if(w!=null){d.setVisible(w.id(),!w.visible());syncAll(d.getWaypoints());}});c.setPacketHandled(true);}
    }
    private record DeletePacket(UUID id){
        static void encode(DeletePacket p,FriendlyByteBuf b){b.writeUUID(p.id);}static DeletePacket decode(FriendlyByteBuf b){return new DeletePacket(b.readUUID());}
        static void handle(DeletePacket p,Supplier<NetworkEvent.Context> s){var c=s.get();c.enqueueWork(()->{ServerPlayer pl=c.getSender();if(pl==null||!pl.hasPermissions(2))return;var d=WaypointSavedData.get(pl.getServer());if(d.removeById(p.id))syncAll(d.getWaypoints());});c.setPacketHandled(true);}
    }
    private static class SyncPacket{
        final List<WaypointSavedData.Waypoint> list;SyncPacket(List<WaypointSavedData.Waypoint> l){list=List.copyOf(l);}
        static void encode(SyncPacket p,FriendlyByteBuf b){b.writeVarInt(p.list.size());for(var w:p.list){b.writeUUID(w.id());b.writeUUID(w.owner());b.writeUtf(w.name(),32);b.writeUtf(w.dimension(),256);b.writeDouble(w.x());b.writeDouble(w.y());b.writeDouble(w.z());b.writeInt(w.color());b.writeByte(w.shape());b.writeBoolean(w.visible());b.writeUtf(w.icon(),4);}}
        static SyncPacket decode(FriendlyByteBuf b){int n=b.readVarInt();if(n<0||n>512)throw new IllegalArgumentException("Cantidad inválida");List<WaypointSavedData.Waypoint> l=new ArrayList<>();for(int i=0;i<n;i++)l.add(new WaypointSavedData.Waypoint(b.readUUID(),b.readUUID(),b.readUtf(32),b.readUtf(256),b.readDouble(),b.readDouble(),b.readDouble(),b.readInt()&0xFFFFFF,b.readByte(),b.readBoolean(),b.readUtf(4)));return new SyncPacket(l);}
        static void handle(SyncPacket p,Supplier<NetworkEvent.Context> s){var c=s.get();c.enqueueWork(()->DistExecutor.unsafeRunWhenOn(Dist.CLIENT,()->()->WaypointClientData.setWaypoints(p.list)));c.setPacketHandled(true);}
    }
    private record OpenPacket(boolean edit,UUID id,String name,String dimension,double x,double y,double z,int color,String icon){
        static OpenPacket create(ServerPlayer p){return new OpenPacket(false,null,"",p.level().dimension().location().toString(),p.getX(),p.getY(),p.getZ(),0x40D8FF,"◆");}
        static OpenPacket edit(WaypointSavedData.Waypoint w){return new OpenPacket(true,w.id(),w.name(),w.dimension(),w.x(),w.y(),w.z(),w.color(),w.icon());}
        static void encode(OpenPacket p,FriendlyByteBuf b){b.writeBoolean(p.edit);b.writeBoolean(p.id!=null);if(p.id!=null)b.writeUUID(p.id);b.writeUtf(p.name,32);b.writeUtf(p.dimension,256);b.writeDouble(p.x);b.writeDouble(p.y);b.writeDouble(p.z);b.writeInt(p.color);b.writeUtf(p.icon,4);}
        static OpenPacket decode(FriendlyByteBuf b){return new OpenPacket(b.readBoolean(),b.readBoolean()?b.readUUID():null,b.readUtf(32),b.readUtf(256),b.readDouble(),b.readDouble(),b.readDouble(),b.readInt(),b.readUtf(4));}
        static void handle(OpenPacket p,Supplier<NetworkEvent.Context> s){var c=s.get();c.enqueueWork(()->DistExecutor.unsafeRunWhenOn(Dist.CLIENT,()->()->net.minecraft.client.Minecraft.getInstance().setScreen(new WaypointScreen(p.id,p.name,p.dimension,p.x,p.y,p.z,p.color,p.icon))));c.setPacketHandled(true);}
    }
}