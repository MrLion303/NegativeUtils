package com.negative.negativeutils;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public class AdminPanelScreen extends Screen {
    private final List<CountdownClientData.Entry> entries=new ArrayList<>();
    private EditBox nameInput,durationInput,textInput,colorInput;
    private int selected=-1, position=0;
    private static final String[] POS={"BOSSBAR","ACTIONBAR","SCOREBOARD","TITLE"};
    private static final String[] POS_LABEL={"Arriba","Barra de acción","Marcador lateral","Centro"};
    private Component status=Component.empty();
    private int statusColor=0xFFFFFF;

    public AdminPanelScreen(){super(Component.literal("Contadores"));}

    @Override protected void init(){
        entries.clear(); entries.addAll(CountdownClientData.getAll());
        buildWidgets();
    }
    private void buildWidgets(){
        clearWidgets();
        int left=width/2-150, right=width/2+15;
        addRenderableWidget(Button.builder(Component.literal("Nuevo contador"),b->{selected=-1;clearEditor();buildWidgets();})
                .bounds(left,35,150,21).build());
        int y=61;
        for(int i=0;i<entries.size() && i<6;i++){
            CountdownClientData.Entry e=entries.get(i); int index=i;
            addRenderableWidget(Button.builder(Component.literal((e.running()?"● ":"○ ")+e.name()),
                    b->{selected=index;buildWidgets();}).bounds(left,y+i*25,150,21).build());
        }
        nameInput=field(right,35,155,"Nombre",32);
        durationInput=field(right,75,155,"Duración (segundos)",9);
        textInput=field(right,115,155,"Texto",100);
        colorInput=field(right,155,90,"Color HEX",6);
        addRenderableWidget(nameInput);addRenderableWidget(durationInput);addRenderableWidget(textInput);addRenderableWidget(colorInput);
        addRenderableWidget(Button.builder(Component.literal(POS_LABEL[position]),b->{position=(position+1)%POS.length;b.setMessage(Component.literal(POS_LABEL[position]));})
                .bounds(right,190,155,21).build());
        addRenderableWidget(Button.builder(Component.literal("Guardar"),b->save()).bounds(right,216,74,21).build());
        addRenderableWidget(Button.builder(Component.literal(selected>=0&&selected<entries.size()&&entries.get(selected).running()?"Desactivar":"Activar"),
                b->toggle()).bounds(right+81,216,74,21).build());
        addRenderableWidget(Button.builder(Component.literal("Eliminar"),b->remove()).bounds(right,242,155,21).build());
        addRenderableWidget(Button.builder(Component.literal("Cerrar"),b->onClose()).bounds(width/2-50,height-24,100,20).build());
        if(selected>=0&&selected<entries.size())loadSelected(); else clearEditor();
    }
    private EditBox field(int x,int y,int w,String hint,int max){
        EditBox e=new EditBox(font,x,y,w,20,Component.literal(hint));e.setMaxLength(max);e.setHint(Component.literal(hint));
        if(hint.startsWith("Color")){e.setValue("FFFFFF");e.setFilter(s->s.length()<=6&&s.chars().allMatch(c->Character.digit(c,16)>=0));}
        if(hint.startsWith("Dur"))e.setFilter(s->s.isEmpty()||s.chars().allMatch(Character::isDigit));
        return e;
    }
    private void clearEditor(){if(nameInput==null)return;nameInput.setValue("");durationInput.setValue("60");textInput.setValue("");colorInput.setValue("FFFFFF");position=0;}
    private void loadSelected(){
        var e=entries.get(selected);nameInput.setValue(e.name());durationInput.setValue(String.valueOf(Math.max(1,(e.remainingMillis()+999)/1000)));
        textInput.setValue(e.displayText());colorInput.setValue(String.format(Locale.ROOT,"%06X",e.displayColor()));
        position=0;for(int i=0;i<POS.length;i++)if(POS[i].equals(e.displayPosition()))position=i;
    }
    private void save(){
        if(nameInput.getValue().trim().isEmpty()){setStatus("Escribe un nombre.",0xFF5555);return;}
        long seconds;int color;try{seconds=Long.parseLong(durationInput.getValue());color=Integer.parseInt(colorInput.getValue(),16);}
        catch(Exception e){setStatus("Duración o color no válido.",0xFF5555);return;}
        if(seconds<=0){setStatus("La duración debe ser mayor que 0.",0xFF5555);return;}
        CountdownNetwork.saveCountdown(selected>=0&&selected<entries.size()?entries.get(selected).id():null,nameInput.getValue(),seconds,textInput.getValue(),color,POS[position]);
        setStatus("Cambios guardados.",0x55FF55);
    }
    private void toggle(){if(selected>=0&&selected<entries.size())CountdownNetwork.toggleCountdown(entries.get(selected).id());else setStatus("Selecciona un contador.",0xFFAA55);}
    private void remove(){if(selected>=0&&selected<entries.size())CountdownNetwork.deleteCountdown(entries.get(selected).id());else setStatus("Selecciona un contador.",0xFFAA55);}
    private void setStatus(String s,int c){status=Component.literal(s);statusColor=c;}
    @Override public void render(GuiGraphics g,int mx,int my,float pt){
        renderBackground(g);NegativeUtilsGuiStyle.renderFrame(g,width,height);
        g.drawCenteredString(font,title,width/2,10,0xFFFFFF);
        g.drawString(font,"Contadores",width/2-150,22,0xFFFFFF);g.drawString(font,"Editor",width/2+15,22,0xFFFFFF);
        g.drawCenteredString(font,status,width/2,height-40,statusColor);
        super.render(g,mx,my,pt);
    }
    @Override public boolean isPauseScreen(){return false;}
}