package com.negative.negativeutils;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.Locale;

@Mod.EventBusSubscriber(
        modid = "negativeutils",
        value = Dist.CLIENT,
        bus = Mod.EventBusSubscriber.Bus.FORGE
)
public final class CountdownHologramRenderer {
    private static final float TIMER_SCALE = 0.15F;
    private static final float LABEL_SCALE = 0.055F;
    private static final double MAX_RENDER_DISTANCE = 256.0D;

    private CountdownHologramRenderer() {
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
        Vec3 camera = event.getCamera().getPosition();
        PoseStack poseStack = event.getPoseStack();
        MultiBufferSource.BufferSource buffers = minecraft.renderBuffers().bufferSource();

        RenderSystem.disableDepthTest();
        RenderSystem.enableBlend();
        poseStack.pushPose();
        poseStack.translate(-camera.x, -camera.y, -camera.z);

        for (CountdownClientData.Entry countdown : CountdownClientData.getAll()) {
            if (!countdown.displayed()
                    || !countdown.hasHologram()
                    || !countdown.hologramDimension().equals(dimension)
                    || countdown.remainingMillis() <= 0L) {
                continue;
            }

            Vec3 anchor = new Vec3(
                    countdown.hologramX(),
                    countdown.hologramY(),
                    countdown.hologramZ()
            );
            if (minecraft.player.position().distanceToSqr(anchor)
                    > MAX_RENDER_DISTANCE * MAX_RENDER_DISTANCE) {
                continue;
            }

            float yaw = horizontalFacingYaw(anchor, camera);
            long remainingMillis = countdown.remainingMillis();
            String timer = formatRemaining(remainingMillis);
            int timerColor = 0xFF000000 | (countdown.displayColor() & 0xFFFFFF);

            if (countdown.hasHologramImage()) {
                CountdownHologramTextures.TextureInfo image =
                        CountdownHologramTextures.get(countdown.hologramImage());
                if (image != null) {
                    drawImage(buffers, poseStack, anchor, yaw, image);
                    drawText(
                            minecraft.font, buffers, poseStack,
                            anchor.x, anchor.y - 0.32D, anchor.z,
                            yaw, timer, TIMER_SCALE, timerColor
                    );
                    drawText(
                            minecraft.font, buffers, poseStack,
                            anchor.x, anchor.y - 0.92D, anchor.z,
                            yaw, countdown.name(),
                            fitLabelScale(minecraft.font, countdown.name()), 0xFFFFFFFF
                    );
                    if (!countdown.displayText().isBlank()) {
                        drawText(
                                minecraft.font, buffers, poseStack,
                                anchor.x, anchor.y - 1.38D, anchor.z,
                                yaw, countdown.displayText(),
                                fitLabelScale(minecraft.font, countdown.displayText()), 0xFFE8F4FF
                        );
                    }
                    continue;
                }
            }

            // Sin imagen (o si el archivo no está en el paquete), conserva el holograma de texto.
            drawText(
                    minecraft.font, buffers, poseStack,
                    anchor.x, anchor.y + 0.10D, anchor.z,
                    yaw, timer, TIMER_SCALE, timerColor
            );

            drawText(
                    minecraft.font, buffers, poseStack,
                    anchor.x, anchor.y + 1.12D, anchor.z,
                    yaw, countdown.name(), fitLabelScale(minecraft.font, countdown.name()), 0xFFFFFFFF
            );

            if (!countdown.displayText().isBlank()) {
                drawText(
                        minecraft.font, buffers, poseStack,
                        anchor.x, anchor.y - 0.95D, anchor.z,
                        yaw, countdown.displayText(), fitLabelScale(minecraft.font, countdown.displayText()), 0xFFE8F4FF
                );
            }
        }

        buffers.endBatch();
        poseStack.popPose();
        RenderSystem.disableBlend();
        RenderSystem.enableDepthTest();
    }

    private static void drawImage(
            MultiBufferSource.BufferSource buffers,
            PoseStack poseStack,
            Vec3 anchor,
            float yaw,
            CountdownHologramTextures.TextureInfo texture
    ) {
        float imageHeight = 2.8F;
        float imageWidth = imageHeight * ((float) texture.width() / (float) texture.height());
        if (imageWidth > 5.5F) {
            float reduction = 5.5F / imageWidth;
            imageWidth *= reduction;
            imageHeight *= reduction;
        }

        poseStack.pushPose();
        poseStack.translate(anchor.x, anchor.y + 0.12D, anchor.z);
        poseStack.mulPose(Axis.YP.rotationDegrees(yaw));

        VertexConsumer vertices = buffers.getBuffer(RenderType.entityTranslucent(texture.location()));
        PoseStack.Pose pose = poseStack.last();
        float left = -imageWidth / 2.0F;
        float right = imageWidth / 2.0F;
        int light = LightTexture.FULL_BRIGHT;

        vertices.vertex(pose.pose(), left, imageHeight, 0.0F)
                .color(255, 255, 255, 255).uv(0.0F, 0.0F)
                .overlayCoords(OverlayTexture.NO_OVERLAY).uv2(light)
                .normal(pose, 0.0F, 0.0F, 1.0F).endVertex();
        vertices.vertex(pose.pose(), left, 0.0F, 0.0F)
                .color(255, 255, 255, 255).uv(0.0F, 1.0F)
                .overlayCoords(OverlayTexture.NO_OVERLAY).uv2(light)
                .normal(pose, 0.0F, 0.0F, 1.0F).endVertex();
        vertices.vertex(pose.pose(), right, 0.0F, 0.0F)
                .color(255, 255, 255, 255).uv(1.0F, 1.0F)
                .overlayCoords(OverlayTexture.NO_OVERLAY).uv2(light)
                .normal(pose, 0.0F, 0.0F, 1.0F).endVertex();
        vertices.vertex(pose.pose(), right, imageHeight, 0.0F)
                .color(255, 255, 255, 255).uv(1.0F, 0.0F)
                .overlayCoords(OverlayTexture.NO_OVERLAY).uv2(light)
                .normal(pose, 0.0F, 0.0F, 1.0F).endVertex();

        poseStack.popPose();
    }

    private static String formatRemaining(long remainingMillis) {
        long totalSeconds = Math.max(0L, (remainingMillis + 999L) / 1000L);
        long days = totalSeconds / 86400L;
        long hours = (totalSeconds % 86400L) / 3600L;
        long minutes = (totalSeconds % 3600L) / 60L;
        long seconds = totalSeconds % 60L;

        if (days > 0L) {
            return String.format(Locale.ROOT, "%d:%02d:%02d:%02d",
                    days, hours, minutes, seconds);
        }
        return String.format(Locale.ROOT, "%02d:%02d:%02d",
                hours, minutes, seconds);
    }

    private static float fitLabelScale(Font font, String text) {
        return Math.min(LABEL_SCALE, 12.0F / Math.max(1, font.width(text)));
    }

    private static float horizontalFacingYaw(Vec3 hologramPosition, Vec3 cameraPosition) {
        double dx = cameraPosition.x - hologramPosition.x;
        double dz = cameraPosition.z - hologramPosition.z;
        return (float) Math.toDegrees(Math.atan2(dx, dz));
    }

    private static void drawText(
            Font font,
            MultiBufferSource.BufferSource buffers,
            PoseStack poseStack,
            double x,
            double y,
            double z,
            float yaw,
            String text,
            float scale,
            int color
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
                Font.DisplayMode.SEE_THROUGH,
                0x55000000,
                LightTexture.FULL_BRIGHT
        );
        poseStack.popPose();
    }
}
