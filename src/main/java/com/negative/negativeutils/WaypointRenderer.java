package com.negative.negativeutils;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.AABB;
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
    private static final float MARKER_SCALE = 0.11F;
    private static final float LABEL_SCALE = 0.027F;
    private static final float NAME_SCALE = 0.039F;
    private static final float DISTANCE_SCALE = 0.032F;

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
                .sorted(Comparator.comparingDouble(waypoint -> {
                    Vec3 position = WaypointClientData.resolvePosition(waypoint);
                    return position == null ? Double.MAX_VALUE : minecraft.player.position().distanceToSqr(position);
                }))
                .toList();
        if (waypoints.isEmpty()) return;

        GuiGraphics graphics = event.getGuiGraphics();
        Font font = minecraft.font;
        int margin = 8;
        int[] rows = new int[4];
        int xaeroOffset = NegativeUtilsHudLayout.topLeftOffset(minecraft, graphics.guiWidth(), graphics.guiHeight());
        int shown = Math.min(waypoints.size(), 32);
        for (int index = 0; index < shown; index++) {
            WaypointSavedData.Waypoint waypoint = waypoints.get(index);
            Vec3 resolved = WaypointClientData.resolvePosition(waypoint);
            if (resolved == null) continue;
            double distance = minecraft.player.position().distanceTo(resolved);
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
            int y = bottom ? graphics.guiHeight() - margin - 34 - row * 36 : margin + xaeroOffset + row * 36;
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
    public static void onRenderTrackedPlayer(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_ENTITIES) return;
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null || minecraft.player == null) return;

        Vec3 camera = event.getCamera().getPosition();
        MultiBufferSource.BufferSource buffers = minecraft.renderBuffers().bufferSource();
        VertexConsumer lines = buffers.getBuffer(RenderType.lines());
        boolean throughBlocks = Config.trailsThroughBlocks;
        if (throughBlocks) RenderSystem.depthMask(false);

        for (WaypointSavedData.Waypoint waypoint : WaypointClientData.getWaypoints()) {
            if (!waypoint.visible() || !waypoint.tracksPlayer()) continue;
            Entity target = null;
            for (Entity candidate : minecraft.level.entitiesForRendering()) {
                if (candidate.getUUID().equals(waypoint.trackedPlayer())) {
                    target = candidate;
                    break;
                }
            }
            if (target == null) continue;

            float r = ((waypoint.color() >> 16) & 255) / 255.0F;
            float g = ((waypoint.color() >> 8) & 255) / 255.0F;
            float b = (waypoint.color() & 255) / 255.0F;
            AABB box = target.getBoundingBox().inflate(0.08D);
            event.getPoseStack().pushPose();
            event.getPoseStack().translate(-camera.x, -camera.y, -camera.z);
            LevelRenderer.renderLineBox(event.getPoseStack(), lines, box, r, g, b, 1.0F);
            event.getPoseStack().popPose();
        }

        buffers.endBatch(RenderType.lines());
        if (throughBlocks) RenderSystem.depthMask(true);
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

            Vec3 anchor = WaypointClientData.resolvePosition(waypoint);
            if (anchor == null) continue;
            double distance = minecraft.player.position().distanceTo(anchor);

            float yaw = horizontalFacingYaw(anchor, camera);
            float distanceScale = (float) Math.max(0.001, distance / 8.0);
            float markerScale = MARKER_SCALE * distanceScale;
            float labelScale = LABEL_SCALE * distanceScale;
            float nameScale = NAME_SCALE * distanceScale;
            float distanceTextScale = DISTANCE_SCALE * distanceScale;
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
                        nameScale,
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
                    anchor.y - 0.78 * distanceScale,
                    anchor.z,
                    yaw,
                    Math.round(distance) + " m",
                    distanceTextScale,
                    0xFFFFFFFF,
                    Font.DisplayMode.SEE_THROUGH,
                    0x65000000
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
