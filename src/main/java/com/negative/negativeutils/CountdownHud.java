package com.negative.negativeutils;

import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderGuiOverlayEvent;
import net.minecraftforge.client.gui.overlay.VanillaGuiOverlay;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid="negativeutils", value=Dist.CLIENT)
public class CountdownHud {
    private CountdownHud() {}
    @SubscribeEvent
    public static void onRenderOverlay(RenderGuiOverlayEvent.Post event) {
        if (event.getOverlay() != VanillaGuiOverlay.BOSS_EVENT_PROGRESS.type()) return;
        Minecraft mc=Minecraft.getInstance();
        if (mc.player==null) return;
        var graphics=event.getGuiGraphics();
        int w=event.getWindow().getGuiScaledWidth(), h=event.getWindow().getGuiScaledHeight();
        int[] offsets={0,0,0,0};
        for (CountdownClientData.Entry c: CountdownClientData.getAll()) {
            long ms=c.remainingMillis();
            if (ms<=0) continue;
            long s=(ms+999)/1000, d=s/86400, hr=(s%86400)/3600, min=(s%3600)/60, sec=s%60;
            String timer=d>0?String.format("%d:%02d:%02d:%02d",d,hr,min,sec):String.format("%02d:%02d:%02d",hr,min,sec);
            String text=c.displayText();
            Component timerC=Component.literal(timer), textC=Component.literal(text);
            int idx=switch(c.displayPosition()){case "ACTIONBAR"->1;case "SCOREBOARD"->2;case "TITLE"->3;default->0;};
            int y;
            if(idx==0) y=10+offsets[idx]*24;
            else if(idx==1) y=h-48-offsets[idx]*24;
            else if(idx==2) y=h/2-12-offsets[idx]*24;
            else y=h/2-15+offsets[idx]*24;
            offsets[idx]++;
            if(idx==2){
                graphics.drawString(mc.font,timerC,w-6-mc.font.width(timerC),y,c.displayColor(),false);
                if(!text.isBlank()) graphics.drawString(mc.font,textC,w-6-mc.font.width(textC),y+12,c.displayColor(),false);
            } else {
                graphics.drawCenteredString(mc.font,timerC,w/2,y,c.displayColor());
                if(!text.isBlank()) graphics.drawCenteredString(mc.font,textC,w/2,y+13,c.displayColor());
            }
        }
    }
}