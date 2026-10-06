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

@Mod.EventBusSubscriber(
        modid = "negativeutils",
        value = Dist.CLIENT,
        bus = Mod.EventBusSubscriber.Bus.FORGE
)
public final class TrailRenderer {
    private static final double FOOTPRINT_SPACING = 0.65;
    private static final double SIDE_OFFSET = 0.14;
    private static final double HEIGHT_OFFSET = 0.025;

    private TrailRenderer() {
    }

    @SubscribeEvent
    public static void onRenderLevel(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_TRANSLUCENT_BLOCKS) {
            return;
        }

        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null || minecraft.player == null) {
            return;
        }

        String dimension = minecraft.level.dimension().location().toString();
        PoseStack poseStack = event.getPoseStack();
        Vec3 camera = event.getCamera().getPosition();
        MultiBufferSource.BufferSource buffers = minecraft.renderBuffers().bufferSource();

        poseStack.pushPose();
        poseStack.translate(-camera.x, -camera.y, -camera.z);

        for (TrailSavedData.Trail trail : TrailClientData.getTrails()) {
            if (!trail.visible() || trail.points().size() < 2) {
                continue;
            }

            int color = (trail.opacity() << 24)
                    | (trail.red() << 16)
                    | (trail.green() << 8)
                    | trail.blue();

            var points = trail.points();
            for (int i = 0; i < points.size() - 1; i++) {
                var first = points.get(i);
                var second = points.get(i + 1);
                if (!first.dimension().equals(second.dimension())
                        || !dimension.equals(first.dimension())) {
                    continue;
                }

                drawFootprints(
                        poseStack, buffers, minecraft.font,
                        new Vec3(first.x(), first.y(), first.z()),
                        new Vec3(second.x(), second.y(), second.z()),
                        color);
            }
        }

        buffers.endBatch();
        poseStack.popPose();
    }

    private static void drawFootprints(
            PoseStack poseStack,
            MultiBufferSource.BufferSource buffers,
            Font font,
            Vec3 start,
            Vec3 end,
            int color) {
        double dx = end.x - start.x;
        double dz = end.z - start.z;
        double length = Math.sqrt(dx * dx + dz * dz);
        if (length < 0.01) {
            return;
        }

        double sideX = -dz / length;
        double sideZ = dx / length;
        float direction = (float) Math.atan2(dx, dz);
        int count = Math.max(1, (int) Math.floor(length / FOOTPRINT_SPACING));

        for (int i = 0; i <= count; i++) {
            double progress = Math.min(1.0, i * FOOTPRINT_SPACING / length);
            double x = start.x + dx * progress;
            double y = start.y + (end.y - start.y) * progress + HEIGHT_OFFSET;
            double z = start.z + dz * progress;
            double side = (i % 2 == 0) ? SIDE_OFFSET : -SIDE_OFFSET;
            x += sideX * side;
            z += sideZ * side;

            float footRotation = direction + (i % 2 == 0 ? 0.20F : -0.20F);
            drawFootprint(
                    poseStack, buffers, font, x, y, z, footRotation, color);
        }
    }

    private static void drawFootprint(
            PoseStack poseStack,
            MultiBufferSource.BufferSource buffers,
            Font font,
            double x,
            double y,
            double z,
            float rotation,
            int color) {
        poseStack.pushPose();
        poseStack.translate(x, y, z);
        poseStack.mulPose(Axis.YP.rotation(rotation));
        poseStack.mulPose(Axis.XP.rotationDegrees(90.0F));
        poseStack.scale(0.045F, -0.045F, 0.045F);

        drawGlyph(poseStack, buffers, font, "●", 0.0F, 1.0F, color);
        drawGlyph(poseStack, buffers, font, "•", -2.5F, -3.0F, color);
        drawGlyph(poseStack, buffers, font, "•", 0.0F, -4.0F, color);
        drawGlyph(poseStack, buffers, font, "•", 2.5F, -3.0F, color);

        poseStack.mulPose(Axis.XP.rotationDegrees(180.0F));
        drawGlyph(poseStack, buffers, font, "●", 0.0F, 1.0F, color);
        drawGlyph(poseStack, buffers, font, "•", -2.5F, -3.0F, color);
        drawGlyph(poseStack, buffers, font, "•", 0.0F, -4.0F, color);
        drawGlyph(poseStack, buffers, font, "•", 2.5F, -3.0F, color);
        poseStack.popPose();
    }

    private static void drawGlyph(
            PoseStack poseStack,
            MultiBufferSource.BufferSource buffers,
            Font font,
            String glyph,
            float x,
            float y,
            int color) {
        font.drawInBatch(
                glyph,
                x - font.width(glyph) / 2.0F,
                y,
                color,
                false,
                poseStack.last().pose(),
                buffers,
                Config.trailsThroughBlocks
                        ? Font.DisplayMode.SEE_THROUGH
                        : Font.DisplayMode.NORMAL,
                0,
                LightTexture.FULL_BRIGHT);
    }
}
