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
        modid = "negativeutils",
        value = Dist.CLIENT,
        bus = Mod.EventBusSubscriber.Bus.FORGE
)
public final class WaypointRenderer {
    private static final float MARKER_SCALE = 0.05F;
    private static final float LABEL_SCALE = 0.02F;

    private WaypointRenderer() {
    }

    @SubscribeEvent
    public static void onRenderHud(RenderGuiOverlayEvent.Post event) {
        if (event.getOverlay() != VanillaGuiOverlay.HOTBAR.type()) return;
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null || minecraft.player == null) return;

        String dimension = minecraft.level.dimension().location().toString();
        List<WaypointSavedData.Waypoint> waypoints = WaypointClientData.getWaypoints().stream()
                .filter(WaypointSavedData.Waypoint::visible)
                .filter(waypoint -> waypoint.dimension().equals(dimension))
                .sorted(Comparator.comparingDouble(waypoint -> minecraft.player.position().distanceToSqr(
                        new Vec3(waypoint.x(), waypoint.y(), waypoint.z()))))
                .toList();
        if (waypoints.isEmpty()) return;

        GuiGraphics graphics = event.getGuiGraphics();
        Font font = minecraft.font;
        int margin = 8;
        int[] rows = new int[4];
        int shown = Math.min(waypoints.size(), 32);
        for (int index = 0; index < shown; index++) {
            WaypointSavedData.Waypoint waypoint = waypoints.get(index);
            double distance = minecraft.player.position().distanceTo(
                    new Vec3(waypoint.x(), waypoint.y(), waypoint.z()));
            String distanceLabel = Math.round(distance) + " m";
            int rowWidth = Math.max(font.width(waypoint.name()), font.width(distanceLabel)) + 25;
            int corner = switch (waypoint.corner()) {
                case "TOP_RIGHT" -> 1;
                case "BOTTOM_LEFT" -> 2;
                case "BOTTOM_RIGHT" -> 3;
                default -> 0;
            };
            int row = rows[corner]++;
            boolean right = corner == 1 || corner == 3;
            boolean bottom = corner == 2 || corner == 3;
            int x = right ? graphics.guiWidth() - margin - rowWidth : margin;
            int y = bottom ? graphics.guiHeight() - margin - 34 - row * 36 : margin + row * 36;
            graphics.fill(x - 3, y - 2, x + rowWidth, y + 34, 0x90000000);
            graphics.drawString(font, waypoint.icon(), x, y + 11, 0xFF000000 | waypoint.color(), true);
            if (!waypoint.name().isBlank()) {
                graphics.drawString(font, waypoint.name(), x + 13, y, 0xFFFFFFFF, true);
            }
            graphics.drawString(font, distanceLabel, x + 13, y + 22, 0xFFFFFFFF, true);
        }
        if (waypoints.size() > shown) {
            graphics.drawString(font, "+" + (waypoints.size() - shown) + " waypoints",
                    margin, margin + 36 * 8, 0xFFCCCCCC, true);
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
            if (!waypoint.visible()
                    || !waypoint.dimension().equals(dimension)) {
                continue;
            }

            Vec3 anchor = new Vec3(
                    waypoint.x(),
                    waypoint.y(),
                    waypoint.z()
            );
            double distance = minecraft.player.position().distanceTo(anchor);

            float yaw = horizontalFacingYaw(anchor, camera);
            float distanceScale = (float) Math.max(0.001, distance / 8.0);
            float markerScale = MARKER_SCALE * distanceScale;
            float labelScale = LABEL_SCALE * distanceScale;
            int color = 0xFF000000 | waypoint.color();

            drawVerticalText(
                    minecraft.font,
                    buffers,
                    poseStack,
                    anchor.x,
                    anchor.y,
                    anchor.z,
                    yaw,
                    waypoint.icon(),
                    markerScale,
                    color,
                    Font.DisplayMode.NORMAL,
                    0x55000000
            );

            if (!waypoint.name().isBlank()) {
                drawVerticalText(
                        minecraft.font,
                        buffers,
                        poseStack,
                        anchor.x,
                        anchor.y + 0.72 * distanceScale,
                        anchor.z,
                        yaw,
                        waypoint.name(),
                        labelScale,
                        0xFFFFFFFF,
                        Font.DisplayMode.NORMAL,
                        0x65000000
                );
            }

            drawVerticalText(
                    minecraft.font,
                    buffers,
                    poseStack,
                    anchor.x,
                    anchor.y - 0.48 * distanceScale,
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

    private static float horizontalFacingYaw(
            Vec3 waypointPosition,
            Vec3 playerPosition
    ) {
        double dx = playerPosition.x - waypointPosition.x;
        double dz = playerPosition.z - waypointPosition.z;
        return (float) Math.toDegrees(Math.atan2(dx, dz));
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
}
