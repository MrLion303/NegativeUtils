package com.negative.negativeutils;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid="negativeutils",value=Dist.CLIENT,bus=Mod.EventBusSubscriber.Bus.FORGE)
public final class TrailRenderer {
    private static final double FOOTPRINT_SPACING=0.65, SIDE_OFFSET=0.14, HEIGHT_OFFSET=0.035;
    private TrailRenderer(){}
    @SubscribeEvent public static void onRenderLevel(RenderLevelStageEvent event){
        if(event.getStage()!=RenderLevelStageEvent.Stage.AFTER_TRANSLUCENT_BLOCKS)return;
        Minecraft mc=Minecraft.getInstance();if(mc.level==null||mc.player==null)return;
        String dimension=mc.level.dimension().location().toString();
        PoseStack pose=event.getPoseStack();Vec3 camera=event.getCamera().getPosition();
        MultiBufferSource.BufferSource buffers=mc.renderBuffers().bufferSource();
        pose.pushPose();pose.translate(-camera.x,-camera.y,-camera.z);
        for(TrailSavedData.Trail trail:TrailClientData.getTrails()){
            if(!trail.visible()||trail.points().size()<2)continue;
            int color=0xFF000000|(trail.red()<<16)|(trail.green()<<8)|trail.blue();
            var points=trail.points();
            for(int i=0;i<points.size()-1;i++){
                var a=points.get(i);var b=points.get(i+1);
                if(!dimension.equals(a.dimension())||!a.dimension().equals(b.dimension()))continue;
                drawFootprints(pose,buffers,mc.font,new Vec3(a.x(),a.y(),a.z()),new Vec3(b.x(),b.y(),b.z()),color);
            }
        }
        buffers.endBatch();pose.popPose();
    }
    private static void drawFootprints(PoseStack pose,MultiBufferSource.BufferSource buffers,Font font,Vec3 a,Vec3 b,int color){
        double dx=b.x-a.x,dz=b.z-a.z,length=Math.sqrt(dx*dx+dz*dz);if(length<0.01)return;
        double sideX=-dz/length,sideZ=dx/length;int count=Math.max(1,(int)Math.floor(length/FOOTPRINT_SPACING));
        for(int i=0;i<=count;i++){
            double progress=Math.min(1.0,i*FOOTPRINT_SPACING/length);
            double x=a.x+dx*progress,y=a.y+(b.y-a.y)*progress+HEIGHT_OFFSET,z=a.z+dz*progress;
            double side=(i%2==0)?SIDE_OFFSET:-SIDE_OFFSET;x+=sideX*side;z+=sideZ*side;
            drawFootprint(pose,buffers,font,x,y,z,(i%2==0)?0.20:-0.20,color);
        }
    }
    private static void drawFootprint(PoseStack pose,MultiBufferSource.BufferSource buffers,Font font,double x,double y,double z,double rotation,int color){
        pose.pushPose();pose.translate(x,y,z);pose.mulPose(Axis.YP.rotation((float)rotation));pose.mulPose(Axis.XP.rotationDegrees(90));pose.scale(0.032F,-0.032F,0.032F);
        String mark="◆";font.drawInBatch(mark,-font.width(mark)/2.0F,-font.lineHeight/2.0F,color,false,pose.last().pose(),buffers,Font.DisplayMode.SEE_THROUGH,0,LightTexture.FULL_BRIGHT);
        pose.popPose();
    }
}