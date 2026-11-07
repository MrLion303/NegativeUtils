package com.negative.negativeutils.client.renderer;

import com.negative.negativeutils.NegativeUtilsMod;
import com.negative.negativeutils.RiftEntity;
import com.negative.negativeutils.client.model.RiftModel;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.ResourceLocation;

public final class RiftRenderer extends MobRenderer<RiftEntity, RiftModel> {
    private static final ResourceLocation TEXTURE =
            new ResourceLocation(NegativeUtilsMod.MOD_ID, "textures/entity/rift.png");

    public RiftRenderer(EntityRendererProvider.Context context) {
        super(context, new RiftModel(context.bakeLayer(RiftModel.LAYER_LOCATION)), 0.0F);
    }

    @Override
    public ResourceLocation getTextureLocation(RiftEntity entity) {
        return TEXTURE;
    }

    @Override
    protected void setupRotations(RiftEntity entity, PoseStack poseStack,
                                  float ageInTicks, float rotationYaw, float partialTicks) {
        super.setupRotations(entity, poseStack, ageInTicks, rotationYaw, partialTicks);

        if (entity.isExitOnly()) {
            // Antes medía 4x; 12x triplica exactamente el tamaño actual.
            // La transformación conserva el centro de la abertura al girarla sobre el suelo.
            poseStack.translate(0.0D, RiftEntity.MODEL_CENTER_OFFSET, 0.0D);
            poseStack.mulPose(Axis.XP.rotationDegrees(90.0F));
            poseStack.scale(12.0F, 12.0F, 12.0F);
            poseStack.translate(0.0D, -RiftEntity.MODEL_CENTER_OFFSET, 0.0D);
        }
    }

    @Override
    protected boolean shouldShowName(RiftEntity entity) {
        return false;
    }
}
