package com.negative.negativeutils;

import java.util.Locale;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public class WaypointScreen extends Screen {
    private final java.util.UUID id;
    private EditBox name,xInput,yInput,zInput,dimensionInput,iconInput,colorInput;
    private int color;
    public WaypointScreen(java.util.UUID id,String name,String dimension,double x,double y,double z,int color,String icon){
        super(Component.literal(id==null?"Crear waypoint":"Editar waypoint"));
        this.id=id;this.color=color&0xFFFFFF;this.initialName=name;this.initialDimension=dimension;
        this.initialX=x;this.initialY=y;this.initialZ=z;this.initialIcon=icon;
    }
    private final String initialName,initialDimension,initialIcon;private final double initialX,initialY,initialZ;

    @Override protected void init(){
        int cx=width/2;int left=cx-100;
        name=field(left,42,200,"Nombre",32,initialName);
        xInput=field(left,78,62,"X",16,String.valueOf(initialX));
        yInput=field(left+69,78,62,"Y",16,String.valueOf(initialY));
        zInput=field(left+138,78,62,"Z",16,String.valueOf(initialZ));
        dimensionInput=field(left,114,200,"Dimensión",256,initialDimension);
        iconInput=field(left,150,70,"Icono",4,initialIcon);
        colorInput=field(left+80,150,120,"Color HEX",6,String.format(Locale.ROOT,"%06X",color));
        addRenderableWidget(name);addRenderableWidget(xInput);addRenderableWidget(yInput);addRenderableWidget(zInput);addRenderableWidget(dimensionInput);addRenderableWidget(iconInput);addRenderableWidget(colorInput);
        addRenderableWidget(Button.builder(Component.literal("Guardar"),b->save()).bounds(cx-100,190,95,21).build());
        addRenderableWidget(Button.builder(Component.literal("Activar / Desactivar"),b->{if(id!=null)WaypointNetwork.toggle(id);}).bounds(cx+5,190,95,21).build());
        addRenderableWidget(Button.builder(Component.literal("Eliminar"),b->{if(id!=null){WaypointNetwork.delete(id);onClose();}}).bounds(cx-100,216,95,21).build());
        addRenderableWidget(Button.builder(Component.literal("Cerrar"),b->onClose()).bounds(cx+5,216,95,21).build());
    }
    private EditBox field(int x,int y,int w,String hint,int max,String value){EditBox e=new EditBox(font,x,y,w,20,Component.literal(hint));e.setMaxLength(max);e.setHint(Component.literal(hint));e.setValue(value);return e;}
    private void save(){
        try{
            int c=Integer.parseInt(colorInput.getValue().trim(),16);
            WaypointNetwork.save(id,name.getValue(),dimensionInput.getValue(),Double.parseDouble(xInput.getValue()),Double.parseDouble(yInput.getValue()),Double.parseDouble(zInput.getValue()),c,iconInput.getValue());
            onClose();
        }catch(Exception ignored){}
    }
    @Override public void render(GuiGraphics g,int mx,int my,float pt){
        renderBackground(g);NegativeUtilsGuiStyle.renderFrame(g,width,height);
        int cx=width/2;
        g.fill(cx-115,20,cx+115,244,0xE8141B24);g.fill(cx-115,20,cx+115,22,0xFF54D6FF);
        g.drawCenteredString(font,title,cx,28,0xFFFFFFFF);
        g.drawString(font,"Nombre",cx-100,34,0xFFD8E1EA);
        g.drawString(font,"Coordenadas",cx-100,70,0xFFD8E1EA);
        g.drawString(font,"Dimensión",cx-100,106,0xFFD8E1EA);
        g.drawString(font,"Icono",cx-100,142,0xFFD8E1EA);
        g.drawString(font,"Color",cx-20,142,0xFFD8E1EA);
        g.fill(cx-20,173,cx+20,187,0xFF000000|color);
        super.render(g,mx,my,pt);
    }
    @Override public boolean isPauseScreen(){return false;}
}