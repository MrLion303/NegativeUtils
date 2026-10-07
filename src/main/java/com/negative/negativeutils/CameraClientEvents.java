package com.negative.negativeutils;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderGuiOverlayEvent;
import net.minecraftforge.client.gui.overlay.VanillaGuiOverlay;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.lwjgl.glfw.GLFW;

@Mod.EventBusSubscriber(
        modid = NegativeUtilsMod.MOD_ID,
        value = Dist.CLIENT,
        bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class CameraClientEvents {
    private static boolean rightHeld;

    private CameraClientEvents() {}

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        Minecraft mc = Minecraft.getInstance();
        CameraClientData.Active camera = CameraClientData.getActive();
        if (mc.player == null || mc.level == null || camera == null) {
            rightHeld = false;
            return;
        }

        long window = mc.getWindow().getWindow();
        rightHeld = GLFW.glfwGetMouseButton(window, GLFW.GLFW_MOUSE_BUTTON_RIGHT) == GLFW.GLFW_PRESS;

        if (!camera.forceLook() && !rightHeld) return;
        Vec3 target = CameraClientData.resolveTarget(camera);
        if (target == null) return;

        Vec3 eye = mc.player.getEyePosition();
        double dx = target.x - eye.x;
        double dy = target.y - eye.y;
        double dz = target.z - eye.z;
        double horizontal = Math.sqrt(dx * dx + dz * dz);
        float targetYaw = (float)(Math.toDegrees(Math.atan2(dz, dx)) - 90.0D);
        float targetPitch = (float)-Math.toDegrees(Math.atan2(dy, horizontal));
        targetPitch = Mth.clamp(targetPitch, -90.0F, 90.0F);

        float yaw = Mth.rotLerp(0.12F, mc.player.getYRot(), targetYaw);
        float pitch = Mth.lerp(0.12F, mc.player.getXRot(), targetPitch);
        mc.player.setYRot(yaw);
        mc.player.setXRot(pitch);
        mc.player.yRotO = yaw;
        mc.player.xRotO = pitch;
        mc.player.setYHeadRot(yaw);
        mc.player.yHeadRotO = yaw;
    }

    @SubscribeEvent
    public static void onRenderHud(RenderGuiOverlayEvent.Post event) {
        if (event.getOverlay() != VanillaGuiOverlay.HOTBAR.type()) return;
        if (CameraClientData.getActive() == null) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;

        GuiGraphics graphics = event.getGuiGraphics();
        String text = CameraClientData.getActive().forceLook()
                ? "Cámara fijada"
                : "Click derecho para fijar";
        graphics.drawCenteredString(mc.font, text, graphics.guiWidth() / 2,
                graphics.guiHeight() - 59, 0xFFFFFFFF);
    }
}
