package com.negative.negativeutils;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderGuiOverlayEvent;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.client.gui.overlay.VanillaGuiOverlay;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.Comparator;
import java.util.List;

@Mod.EventBusSubscriber(
        modid = EnciclopediaMod.MOD_ID,
        value = Dist.CLIENT,
        bus = Mod.EventBusSubscriber.Bus.FORGE
)
public final class WaypointRenderer {
    private static final double MAX_RENDER_DISTANCE = 512.0;
    private static final float MARKER_SCALE = 0.05F;
    private static final float LABEL_SCALE = 0.02F;

    private WaypointRenderer() {
    }

    @SubscribeEvent
    public static void onRenderHud(RenderGuiOverlayEvent.Post event) {
        if (event.getOverlay() != VanillaGuiOverlay.HOTBAR.type()) {
            return;
        }

        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null || minecraft.player == null) {
            return;
        }

        String dimension = minecraft.level.dimension().location().toString();
        List<WaypointSavedData.Waypoint> waypoints =
                WaypointClientData.getWaypoints().stream()
                        .filter(waypoint -> waypoint.dimension().equals(dimension))
                        .sorted(Comparator.comparingDouble(waypoint ->
                                minecraft.player.position().distanceToSqr(
                                        new Vec3(
                                                waypoint.x(),
                                                waypoint.y(),
                                                waypoint.z()
                                        )
                                )))
                        .toList();
        if (waypoints.isEmpty()) {
            return;
        }

        GuiGraphics graphics = event.getGuiGraphics();
        Font font = minecraft.font;
        int x = 8;
        int y = 8;
        int shown = Math.min(waypoints.size(), 8);
        for (int index = 0; index < shown; index++) {
            WaypointSavedData.Waypoint waypoint = waypoints.get(index);
            double distance = minecraft.player.position().distanceTo(
                    new Vec3(waypoint.x(), waypoint.y(), waypoint.z())
            );
            String distanceLabel = Math.round(distance) + " m";
            int rowWidth = Math.max(
                    font.width(waypoint.name()),
                    font.width(distanceLabel)
            ) + 18;
            graphics.fill(x - 3, y - 2, x + rowWidth, y + 34, 0x90000000);
            if (!waypoint.name().isBlank()) {
                graphics.drawString(
                        font,
                        waypoint.name(),
                        x,
                        y,
                        0xFFFFFFFF,
                        true
                );
            }
            drawHudMarker(
                    graphics,
                    x,
                    y + 12,
                    0xFF000000 | waypoint.color()
            );
            graphics.drawString(
                    font,
                    distanceLabel,
                    x,
                    y + 22,
                    0xFFFFFFFF,
                    true
            );
            y += 36;
        }

        if (waypoints.size() > shown) {
            graphics.drawString(
                    font,
                    "+" + (waypoints.size() - shown) + " waypoints",
                    x + 12,
                    y,
                    0xFFCCCCCC,
                    true
            );
        }
    }

    private static void drawHudMarker(
            GuiGraphics graphics,
            int x,
            int y,
            int color
    ) {
        graphics.fill(x, y, x + 8, y + 8, color);
    }

    @SubscribeEvent
    public static void onRenderLevel(RenderLevelStageEvent event) {
        if (event.getStage()
                != RenderLevelStageEvent.Stage.AFTER_TRANSLUCENT_BLOCKS) {
            return;
        }

        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null || minecraft.player == null) {
            return;
        }

        String dimension = minecraft.level.dimension().location().toString();
        Vec3 camera = event.getCamera().getPosition();
        PoseStack poseStack = event.getPoseStack();
        MultiBufferSource.BufferSource buffers =
                minecraft.renderBuffers().bufferSource();

        poseStack.pushPose();
        poseStack.translate(-camera.x, -camera.y, -camera.z);
        for (WaypointSavedData.Waypoint waypoint
                : WaypointClientData.getWaypoints()) {
            if (!waypoint.dimension().equals(dimension)) {
                continue;
            }

            Vec3 anchor = new Vec3(waypoint.x(), waypoint.y(), waypoint.z());
            double distance = minecraft.player.position().distanceTo(anchor);
            if (distance > MAX_RENDER_DISTANCE) {
                continue;
            }

            float yaw = horizontalFacingYaw(anchor, camera);
            int color = 0xFF000000 | waypoint.color();
            drawVerticalText(
                    minecraft.font,
                    buffers,
                    poseStack,
                    anchor.x,
                    anchor.y,
                    anchor.z,
                    yaw,
                    "■",
                    MARKER_SCALE,
                    color,
                    Font.DisplayMode.NORMAL,
                    0
            );
            if (!waypoint.name().isBlank()) {
                drawVerticalText(
                        minecraft.font,
                        buffers,
                        poseStack,
                        anchor.x,
                        anchor.y + 0.72,
                        anchor.z,
                        yaw,
                        waypoint.name(),
                        LABEL_SCALE,
                        0xFFFFFFFF,
                        Font.DisplayMode.NORMAL,
                        0
                );
            }
            drawVerticalText(
                    minecraft.font,
                    buffers,
                    poseStack,
                    anchor.x,
                    anchor.y - 0.48,
                    anchor.z,
                    yaw,
                    Math.round(distance) + " m",
                    LABEL_SCALE,
                    0xFFFFFFFF,
                    Font.DisplayMode.NORMAL,
                    0
            );
        }
        buffers.endBatch();
        poseStack.popPose();
    }

    private static void drawVerticalText(
            Font font,
            MultiBufferSource.BufferSource buffers,
            PoseStack poseStack,
            double x,
            double y,
            double z,
            float yaw,
            String text,
            float scale,
            int color,
            Font.DisplayMode displayMode,
            int backgroundColor
    ) {
        poseStack.pushPose();
        poseStack.translate(x, y, z);
        poseStack.mulPose(Axis.YP.rotationDegrees(yaw));
        poseStack.scale(scale, -scale, scale);
        font.drawInBatch(
                text,
                -font.width(text) / 2.0F,
                -font.lineHeight / 2.0F,
                color,
                true,
                poseStack.last().pose(),
                buffers,
                displayMode,
                backgroundColor,
                LightTexture.FULL_BRIGHT
        );
        poseStack.popPose();
    }

    private static float horizontalFacingYaw(
            Vec3 waypointPosition,
            Vec3 playerPosition
    ) {
        double dx = playerPosition.x - waypointPosition.x;
        double dz = playerPosition.z - waypointPosition.z;
        return (float) Math.toDegrees(Math.atan2(dx, dz));
    }
}
