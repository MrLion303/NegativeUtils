package com.negative.negativeutils;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.phys.Vec3;

public class WaypointSavedData extends SavedData {
    private static final String DATA_NAME="negativeutils_waypoints";
    private final List<Waypoint> waypoints=new ArrayList<>();

    public static WaypointSavedData get(MinecraftServer server){return server.overworld().getDataStorage().computeIfAbsent(WaypointSavedData::load,WaypointSavedData::new,DATA_NAME);}
    public static WaypointSavedData load(CompoundTag tag){
        WaypointSavedData data=new WaypointSavedData(); ListTag list=tag.getList("Waypoints",Tag.TAG_COMPOUND);
        for(int i=0;i<list.size();i++){CompoundTag t=list.getCompound(i);ResourceLocation dim=ResourceLocation.tryParse(t.getString("Dimension"));try{
            if(dim==null||!t.hasUUID("Id")||!t.hasUUID("Owner"))continue;
            data.waypoints.add(new Waypoint(t.getUUID("Id"),t.getUUID("Owner"),sanitizeName(t.getString("Name")),dim.toString(),t.getDouble("X"),t.getDouble("Y"),t.getDouble("Z"),t.getInt("Color")&0xFFFFFF,t.contains("Shape")?t.getInt("Shape"):1,t.contains("Visible")?t.getBoolean("Visible"):true,t.contains("Icon")?t.getString("Icon"):"◆"));
        }catch(IllegalArgumentException ignored){}}
        return data;
    }
    @Override public CompoundTag save(CompoundTag tag){
        ListTag list=new ListTag(); for(Waypoint w:waypoints){CompoundTag t=new CompoundTag();t.putUUID("Id",w.id());t.putUUID("Owner",w.owner());t.putString("Name",w.name());t.putString("Dimension",w.dimension());t.putDouble("X",w.x());t.putDouble("Y",w.y());t.putDouble("Z",w.z());t.putInt("Color",w.color());t.putInt("Shape",w.shape());t.putBoolean("Visible",w.visible());t.putString("Icon",w.icon());list.add(t);}tag.put("Waypoints",list);return tag;
    }
    public Waypoint add(UUID owner,String name,String dimension,Vec3 pos,int color,String icon){
        ResourceLocation dim=ResourceLocation.tryParse(dimension);if(owner==null||dim==null||pos==null)return null;
        String clean=sanitizeName(name);if(clean.isBlank())clean=nextAutomaticName();
        Waypoint w=new Waypoint(UUID.randomUUID(),owner,clean,dim.toString(),pos.x,pos.y,pos.z,color&0xFFFFFF,1,true,sanitizeIcon(icon));waypoints.add(w);setDirty();return w;
    }
    public Waypoint getById(UUID id){for(Waypoint w:waypoints)if(w.id().equals(id))return w;return null;}
    public Waypoint getByName(String name){String clean=sanitizeName(name);for(Waypoint w:waypoints)if(w.name().equals(clean))return w;return null;}
    public boolean update(UUID id,String name,String dimension,Vec3 pos,int color,String icon){
        for(int i=0;i<waypoints.size();i++){Waypoint w=waypoints.get(i);if(!w.id().equals(id))continue;ResourceLocation dim=ResourceLocation.tryParse(dimension);if(dim==null||pos==null)return false;
            waypoints.set(i,new Waypoint(w.id(),w.owner(),sanitizeName(name).isBlank()?w.name():sanitizeName(name),dim.toString(),pos.x,pos.y,pos.z,color&0xFFFFFF,w.shape(),w.visible(),sanitizeIcon(icon)));setDirty();return true;}
        return false;
    }
    public boolean setVisible(UUID id,boolean visible){Waypoint w=getById(id);if(w==null)return false;if(w.visible()==visible)return true;waypoints.set(waypoints.indexOf(w),new Waypoint(w.id(),w.owner(),w.name(),w.dimension(),w.x(),w.y(),w.z(),w.color(),w.shape(),visible,w.icon()));setDirty();return true;}
    public boolean removeById(UUID id){boolean removed=waypoints.removeIf(w->w.id().equals(id));if(removed)setDirty();return removed;}
    public int removeAll(){int n=waypoints.size();if(n>0){waypoints.clear();setDirty();}return n;}
    public List<Waypoint> getWaypoints(){return List.copyOf(waypoints);}
    private String nextAutomaticName(){int n=1;while(getByName("Waypoint "+n)!=null)n++;return "Waypoint "+n;}
    public static String sanitizeName(String name){if(name==null)return "";String c=name.replaceAll("[\\p{Cntrl}§]","").trim();return c.length()>32?c.substring(0,32):c;}
    public static String sanitizeIcon(String icon){if(icon==null||icon.isBlank())return "◆";String c=icon.trim();return c.length()>4?c.substring(0,4):c;}
    public record Waypoint(UUID id,UUID owner,String name,String dimension,double x,double y,double z,int color,int shape,boolean visible,String icon){}
}