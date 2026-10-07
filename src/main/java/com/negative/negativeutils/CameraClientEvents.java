package com.negative.negativeutils;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.CameraType;
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
    private static CameraType previousCameraType = CameraType.FIRST_PERSON;
    private static CameraType transitionFrom;
    private static boolean transitioning;
    private static boolean transitionToFirstPerson;
    private static int transitionTick;
    private static int transitionLength;
    private static boolean cameraViewOwned;

    private CameraClientEvents() {}

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        Minecraft mc = Minecraft.getInstance();
        CameraClientData.Active camera = CameraClientData.getActive();
        if (mc.player == null || mc.level == null || mc.screen != null) {
            rightHeld = false;
            return;
        }

        if (camera == null) {
            if (cameraViewOwned && !transitioning) {
                beginTransition(mc, false);
            }
            if (transitioning) {
                transitionTick++;
                float progress = Math.min(1.0F, transitionTick / (float) transitionLength);
                if (progress >= 0.5F && !transitionToFirstPerson
                        && transitionFrom != null) {
                    mc.options.setCameraType(transitionFrom);
                }
                if (progress >= 1.0F) {
                    transitioning = false;
                    cameraViewOwned = false;
                }
            }
            return;
        }

        long window = mc.getWindow().getWindow();
        rightHeld = GLFW.glfwGetMouseButton(window, GLFW.GLFW_MOUSE_BUTTON_RIGHT) == GLFW.GLFW_PRESS;
        boolean shouldFix = camera.forceLook() || rightHeld;

        if (shouldFix && !cameraViewOwned && !transitioning) {
            beginTransition(mc, true);
        } else if (!shouldFix && cameraViewOwned && !transitioning) {
            beginTransition(mc, false);
        }

        if (transitioning) {
            transitionTick++;
            float progress = Math.min(1.0F, transitionTick / (float) transitionLength);
            if (progress >= 0.5F && transitionToFirstPerson
                    && mc.options.getCameraType() != CameraType.FIRST_PERSON) {
                mc.options.setCameraType(CameraType.FIRST_PERSON);
            }
            if (progress >= 1.0F) {
                transitioning = false;
                cameraViewOwned = transitionToFirstPerson;
                if (!transitionToFirstPerson && transitionFrom != null) {
                    mc.options.setCameraType(transitionFrom);
                }
            }
        }

        if (!shouldFix && !transitioning) return;
        if (transitioning && !transitionToFirstPerson) return;

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

    private static void beginTransition(Minecraft mc, boolean toFirstPerson) {
        transitionFrom = mc.options.getCameraType();
        transitionToFirstPerson = toFirstPerson;
        transitionTick = 0;

        if (toFirstPerson && transitionFrom == CameraType.FIRST_PERSON) {
            cameraViewOwned = true;
            return;
        }
        if (!toFirstPerson && transitionFrom == CameraType.FIRST_PERSON) {
            cameraViewOwned = false;
            return;
        }

        // La tercera persona trasera entra de frente hacia el jugador;
        // la tercera persona frontal necesita un poco más de tiempo para cruzar la posición.
        transitionLength = transitionFrom == CameraType.THIRD_PERSON_FRONT ? 16 : 12;
        transitioning = true;
    }

    @SubscribeEvent
    public static void onRenderHud(RenderGuiOverlayEvent.Post event) {
        if (event.getOverlay() != VanillaGuiOverlay.HOTBAR.type()) return;
        if (CameraClientData.getActive() == null) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;

        GuiGraphics graphics = event.getGuiGraphics();
        String text = "Click derecho para fijar";
        graphics.drawCenteredString(mc.font, text, graphics.guiWidth() / 2,
                graphics.guiHeight() - 59, 0xFFFFFFFF);

        if (transitioning) {
            float progress = transitionTick / (float) Math.max(1, transitionLength);
            float fade = progress < 0.5F ? progress * 2.0F : (1.0F - progress) * 2.0F;
            int alpha = (int) (Math.max(0.0F, Math.min(1.0F, fade)) * 170.0F);
            if (alpha > 0) {
                graphics.fill(0, 0, graphics.guiWidth(), graphics.guiHeight(),
                        (alpha << 24));
            }
        }
    }
}
