package com.negative.negativeutils;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
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
    private static final String PROTOCOL="4";
    private static final SimpleChannel CHANNEL=NetworkRegistry.newSimpleChannel(
            ResourceLocation.fromNamespaceAndPath("negativeutils","countdown"),
            ()->PROTOCOL,PROTOCOL::equals,PROTOCOL::equals);
    private static int id=0;
    private CountdownNetwork(){}
    @Mod.EventBusSubscriber(modid="negativeutils",bus=Mod.EventBusSubscriber.Bus.MOD)
    public static class Registration {
        @SubscribeEvent public static void onCommonSetup(FMLCommonSetupEvent e) { e.enqueueWork(()->{
            CHANNEL.registerMessage(id++,SavePacket.class,SavePacket::encode,SavePacket::decode,SavePacket::handle);
            CHANNEL.registerMessage(id++,TogglePacket.class,TogglePacket::encode,TogglePacket::decode,TogglePacket::handle);
            CHANNEL.registerMessage(id++,DeletePacket.class,DeletePacket::encode,DeletePacket::decode,DeletePacket::handle);
            CHANNEL.registerMessage(id++,SyncPacket.class,SyncPacket::encode,SyncPacket::decode,SyncPacket::handle);
            CHANNEL.registerMessage(id++,OpenPacket.class,OpenPacket::encode,OpenPacket::decode,OpenPacket::handle);
        });}
    }
    public static void saveCountdown(UUID uuid,String name,long seconds,String text,int color,String position){CHANNEL.sendToServer(new SavePacket(uuid,name,seconds,text,color,position));}
    public static void toggleCountdown(UUID uuid){CHANNEL.sendToServer(new TogglePacket(uuid));}
    public static void deleteCountdown(UUID uuid){CHANNEL.sendToServer(new DeletePacket(uuid));}
    public static void openAdminPanel(ServerPlayer player){syncToPlayer(player);CHANNEL.send(PacketDistributor.PLAYER.with(()->player),new OpenPacket());}
    public static void syncToPlayer(ServerPlayer player){sync(CountdownSavedData.get(player.getServer()),PacketDistributor.PLAYER.with(()->player));}
    public static void syncAll(CountdownSavedData data){sync(data,PacketDistributor.ALL.noArg());}
    private static void sync(CountdownSavedData d,PacketDistributor.PacketTarget target){
        List<SyncEntry> list=new ArrayList<>();
        for(var c:d.getCountdowns()) list.add(new SyncEntry(c.id(),c.name(),c.running(),c.finished(),c.endTimeMillis(),c.pausedRemainingMillis(),c.displayText(),c.displayColor(),c.displayPosition(),c.displayed(),c.minuteMode(),c.durationMillis()));
        CHANNEL.send(target,new SyncPacket(list));
    }
    private static class SavePacket {
        final UUID uuid; final String name,text,position; final long seconds; final int color;
        SavePacket(UUID u,String n,long s,String t,int c,String p){uuid=u;name=n;text=t;seconds=s;color=c;position=p;}
        static void encode(SavePacket p,FriendlyByteBuf b){b.writeBoolean(p.uuid!=null);if(p.uuid!=null)b.writeUUID(p.uuid);b.writeUtf(p.name,32);b.writeLong(p.seconds);b.writeUtf(p.text,100);b.writeInt(p.color);b.writeUtf(p.position,16);}
        static SavePacket decode(FriendlyByteBuf b){UUID u=b.readBoolean()?b.readUUID():null;return new SavePacket(u,b.readUtf(32),b.readLong(),b.readUtf(100),b.readInt(),b.readUtf(16));}
        static void handle(SavePacket p,Supplier<NetworkEvent.Context> s){var c=s.get();c.enqueueWork(()->{ServerPlayer player=c.getSender();if(player==null||!player.hasPermissions(2))return;var d=CountdownSavedData.get(player.getServer());long sec=Math.max(1,Math.min(p.seconds,315360000L));if(p.uuid==null)d.create(p.name,sec*1000L,p.text,p.color,p.position);else d.update(p.uuid,p.name,sec*1000L,p.text,p.color,p.position);sync(d,PacketDistributor.ALL.noArg());});c.setPacketHandled(true);}
    }
    private static class TogglePacket {
        final UUID uuid; TogglePacket(UUID u){uuid=u;}
        static void encode(TogglePacket p,FriendlyByteBuf b){b.writeUUID(p.uuid);} static TogglePacket decode(FriendlyByteBuf b){return new TogglePacket(b.readUUID());}
        static void handle(TogglePacket p,Supplier<NetworkEvent.Context> s){var c=s.get();c.enqueueWork(()->{ServerPlayer pl=c.getSender();if(pl==null||!pl.hasPermissions(2))return;var d=CountdownSavedData.get(pl.getServer());var x=d.getById(p.uuid);if(x!=null)d.setRunning(x.id(),!x.running());sync(d,PacketDistributor.ALL.noArg());});c.setPacketHandled(true);}
    }
    private static class DeletePacket {
        final UUID uuid; DeletePacket(UUID u){uuid=u;}
        static void encode(DeletePacket p,FriendlyByteBuf b){b.writeUUID(p.uuid);} static DeletePacket decode(FriendlyByteBuf b){return new DeletePacket(b.readUUID());}
        static void handle(DeletePacket p,Supplier<NetworkEvent.Context> s){var c=s.get();c.enqueueWork(()->{ServerPlayer pl=c.getSender();if(pl==null||!pl.hasPermissions(2))return;var d=CountdownSavedData.get(pl.getServer());d.remove(p.uuid);sync(d,PacketDistributor.ALL.noArg());});c.setPacketHandled(true);}
    }
    private record SyncEntry(UUID id,String name,boolean running,boolean finished,long end,long remaining,String text,int color,String position,boolean displayed,boolean minuteMode,long durationMillis){}
    private static class SyncPacket {
        final List<SyncEntry> list; SyncPacket(List<SyncEntry> l){list=List.copyOf(l);}
        static void encode(SyncPacket p,FriendlyByteBuf b){b.writeVarInt(p.list.size());for(var x:p.list){b.writeUUID(x.id());b.writeUtf(x.name(),32);b.writeBoolean(x.running());b.writeBoolean(x.finished());b.writeLong(x.end());b.writeLong(x.remaining());b.writeUtf(x.text(),100);b.writeInt(x.color());b.writeUtf(x.position(),16);b.writeBoolean(x.displayed());b.writeBoolean(x.minuteMode());b.writeLong(x.durationMillis());}}
        static SyncPacket decode(FriendlyByteBuf b){int n=b.readVarInt();if(n<0||n>512)throw new IllegalArgumentException("Cantidad inválida");List<SyncEntry> l=new ArrayList<>();for(int i=0;i<n;i++)l.add(new SyncEntry(b.readUUID(),b.readUtf(32),b.readBoolean(),b.readBoolean(),b.readLong(),b.readLong(),b.readUtf(100),b.readInt(),b.readUtf(16),b.readBoolean(),b.readBoolean(),b.readLong()));return new SyncPacket(l);}
        static void handle(SyncPacket p,Supplier<NetworkEvent.Context> s){var c=s.get();c.enqueueWork(()->DistExecutor.unsafeRunWhenOn(Dist.CLIENT,()->()->{List<CountdownClientData.Entry> l=new ArrayList<>();for(var x:p.list)l.add(new CountdownClientData.Entry(x.id(),x.name(),x.running(),x.finished(),x.end(),x.remaining(),x.text(),x.color(),x.position(),x.displayed(),x.minuteMode(),x.durationMillis()));if (CountdownClientData.set(l)) NegativeUtilsClientPacketHandler.refreshAdminPanel();}));c.setPacketHandled(true);}
    }
    private static class OpenPacket {
        static void encode(OpenPacket p,FriendlyByteBuf b){} static OpenPacket decode(FriendlyByteBuf b){return new OpenPacket();}
        static void handle(OpenPacket p,Supplier<NetworkEvent.Context> s){var c=s.get();c.enqueueWork(()->DistExecutor.unsafeRunWhenOn(Dist.CLIENT,()->()->NegativeUtilsClientPacketHandler.openAdminPanel()));c.setPacketHandled(true);}
    }
}