package com.negative.negativeutils.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderNameTagEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.common.Mod;

/**
 * Muestra el icono de micrófono silenciado de Simple Voice Chat junto
 * al nombre de cada jugador mientras el mute global está activo.
 */
@Mod.EventBusSubscriber(
        modid = "negativeutils",
        bus = Mod.EventBusSubscriber.Bus.FORGE,
        value = Dist.CLIENT
)
public final class VoicechatMuteIconEvents {
    private static final ResourceLocation MUTED_MICROPHONE =
            new ResourceLocation("voicechat", "textures/icons/microphone_off.png");

    private VoicechatMuteIconEvents() {
    }

    @SubscribeEvent
    public static void onRenderNameTag(RenderNameTagEvent event) {
        if (!VoicechatMuteClientState.isGloballyMuted()) {
            return;
        }
        if (!ModList.get().isLoaded("voicechat")) {
            return;
        }

        Entity entity = event.getEntity();
        if (!(entity instanceof Player player)) {
            return;
        }

        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || player == minecraft.player || minecraft.options.hideGui) {
            return;
        }

        PoseStack poseStack = event.getPoseStack();
        MultiBufferSource buffers = event.getMultiBufferSource();
        float offset = (float) (minecraft.font.width(event.getContent()) / 2 + 2);

        poseStack.pushPose();
        poseStack.translate(0.0D, player.getBbHeight() + 0.5D, 0.0D);
        poseStack.mulPose(minecraft.getEntityRenderDispatcher().cameraOrientation());
        poseStack.scale(-0.025F, -0.025F, 0.025F);
        poseStack.translate(0.0D, -1.0D, 0.0D);

        VertexConsumer builder = buffers.getBuffer(RenderType.text(MUTED_MICROPHONE));
        PoseStack.Pose pose = poseStack.last();

        addVertex(builder, pose, offset, 10.0F, 0.0F, 0.0F, 1.0F, event.getPackedLight());
        addVertex(builder, pose, offset + 10.0F, 10.0F, 0.0F, 1.0F, 1.0F, event.getPackedLight());
        addVertex(builder, pose, offset + 10.0F, 0.0F, 0.0F, 1.0F, 0.0F, event.getPackedLight());
        addVertex(builder, pose, offset, 0.0F, 0.0F, 0.0F, 0.0F, event.getPackedLight());

        poseStack.popPose();
    }

    private static void addVertex(
            VertexConsumer builder,
            PoseStack.Pose pose,
            float x,
            float y,
            float z,
            float u,
            float v,
            int light
    ) {
        builder.vertex(pose.pose(), x, y, z)
                .color(255, 255, 255, 255)
                .uv(u, v)
                .overlayCoords(OverlayTexture.NO_OVERLAY)
                .uv2(light)
                .normal(pose.normal(), 0.0F, 0.0F, -1.0F)
                .endVertex();
    }
}
