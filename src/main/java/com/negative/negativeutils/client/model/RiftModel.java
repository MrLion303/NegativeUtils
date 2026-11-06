package com.negative.negativeutils.client.model;

import com.negative.negativeutils.NegativeUtilsMod;
import com.negative.negativeutils.RiftEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

public final class RiftModel extends EntityModel<RiftEntity> {
    public static final ModelLayerLocation LAYER_LOCATION =
            new ModelLayerLocation(new ResourceLocation(NegativeUtilsMod.MOD_ID, "rift"), "main");

    private final ModelPart brazo;
    private final ModelPart minibrazo;
    private final ModelPart minibrazo2;
    private final ModelPart minibrazo3;
    private final ModelPart brazo2;
    private final ModelPart brazo3;

    private final float brazoX, brazoY, brazoZ;
    private final float minibrazoX, minibrazoY, minibrazoZ;
    private final float minibrazo2X, minibrazo2Y, minibrazo2Z;
    private final float minibrazo3X, minibrazo3Y, minibrazo3Z;
    private final float brazo2X, brazo2Y, brazo2Z;
    private final float brazo3X, brazo3Y, brazo3Z;

    public RiftModel(ModelPart root) {
        brazo = root.getChild("brazo");
        minibrazo = root.getChild("minibrazo");
        minibrazo2 = root.getChild("minibrazo2");
        minibrazo3 = root.getChild("minibrazo3");
        brazo2 = root.getChild("brazo2");
        brazo3 = root.getChild("brazo3");

        brazoX = brazo.x; brazoY = brazo.y; brazoZ = brazo.z;
        minibrazoX = minibrazo.x; minibrazoY = minibrazo.y; minibrazoZ = minibrazo.z;
        minibrazo2X = minibrazo2.x; minibrazo2Y = minibrazo2.y; minibrazo2Z = minibrazo2.z;
        minibrazo3X = minibrazo3.x; minibrazo3Y = minibrazo3.y; minibrazo3Z = minibrazo3.z;
        brazo2X = brazo2.x; brazo2Y = brazo2.y; brazo2Z = brazo2.z;
        brazo3X = brazo3.x; brazo3Y = brazo3.y; brazo3Z = brazo3.z;
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition meshdefinition = new MeshDefinition();
        PartDefinition partdefinition = meshdefinition.getRoot();

        partdefinition.addOrReplaceChild("brazo", CubeListBuilder.create()
                .texOffs(0, 12).addBox(-0.875F, -6.75F, -0.5F, 1.0F, 11.0F, 1.0F, new CubeDeformation(0.0F))
                .texOffs(12, 19).addBox(-2.875F, -4.75F, -0.5F, 2.0F, 7.0F, 1.0F, new CubeDeformation(0.0F))
                .texOffs(8, 12).addBox(0.125F, -3.75F, -0.5F, 1.0F, 10.0F, 1.0F, new CubeDeformation(0.0F))
                .texOffs(4, 24).addBox(1.125F, -0.75F, -0.5F, 1.0F, 4.0F, 1.0F, new CubeDeformation(0.0F)),
                PartPose.offset(1.875F, 14.75F, -0.5F));

        partdefinition.addOrReplaceChild("minibrazo", CubeListBuilder.create()
                .texOffs(20, 10).addBox(-0.5F, -3.5F, -0.5F, 1.0F, 7.0F, 1.0F, new CubeDeformation(0.0F))
                .texOffs(24, 0).addBox(0.5F, -2.5F, -0.5F, 1.0F, 5.0F, 1.0F, new CubeDeformation(0.0F))
                .texOffs(24, 16).addBox(-1.5F, -1.5F, -0.5F, 1.0F, 3.0F, 1.0F, new CubeDeformation(0.0F)),
                PartPose.offsetAndRotation(5.8782F, 10.3267F, -0.5F, 0.0F, 0.0F, -0.6109F));

        partdefinition.addOrReplaceChild("minibrazo2", CubeListBuilder.create()
                .texOffs(20, 0).addBox(-0.5F, -4.0F, -0.5F, 1.0F, 9.0F, 1.0F, new CubeDeformation(0.0F))
                .texOffs(8, 23).addBox(0.5F, -3.0F, -0.5F, 1.0F, 6.0F, 1.0F, new CubeDeformation(0.0F))
                .texOffs(24, 20).addBox(-1.5F, -2.0F, -0.5F, 1.0F, 3.0F, 1.0F, new CubeDeformation(0.0F)),
                PartPose.offsetAndRotation(-5.7362F, 10.165F, -0.5F, 0.0F, 0.0F, 0.9599F));

        partdefinition.addOrReplaceChild("minibrazo3", CubeListBuilder.create()
                .texOffs(0, 24).addBox(-1.0F, -2.75F, -0.5F, 1.0F, 6.0F, 1.0F, new CubeDeformation(0.0F))
                .texOffs(24, 24).addBox(0.0F, -1.75F, -0.5F, 1.0F, 3.0F, 1.0F, new CubeDeformation(0.0F)),
                PartPose.offsetAndRotation(0.6364F, -0.5539F, -0.5F, 0.0F, 0.0F, -0.1309F));

        partdefinition.addOrReplaceChild("brazo2", CubeListBuilder.create()
                .texOffs(0, 0).addBox(-1.5F, -6.75F, -0.5F, 2.0F, 11.0F, 1.0F, new CubeDeformation(0.0F))
                .texOffs(18, 19).addBox(-3.5F, -4.75F, -0.5F, 2.0F, 7.0F, 1.0F, new CubeDeformation(0.0F))
                .texOffs(12, 8).addBox(0.5F, -3.75F, -0.5F, 1.0F, 10.0F, 1.0F, new CubeDeformation(0.0F))
                .texOffs(24, 6).addBox(1.5F, -0.75F, -0.5F, 1.0F, 4.0F, 1.0F, new CubeDeformation(0.0F)),
                PartPose.offsetAndRotation(-5.773F, 1.6915F, -0.5F, 0.0F, 0.0F, -0.7854F));

        partdefinition.addOrReplaceChild("brazo3", CubeListBuilder.create()
                .texOffs(6, 0).addBox(-2.0F, -7.1F, -0.5F, 2.0F, 11.0F, 1.0F, new CubeDeformation(0.0F))
                .texOffs(12, 0).addBox(-5.0F, -5.1F, -0.5F, 3.0F, 7.0F, 1.0F, new CubeDeformation(0.0F))
                .texOffs(4, 12).addBox(0.0F, -4.1F, -0.5F, 1.0F, 11.0F, 1.0F, new CubeDeformation(0.0F))
                .texOffs(16, 8).addBox(1.0F, -4.1F, -0.5F, 1.0F, 10.0F, 1.0F, new CubeDeformation(0.0F))
                .texOffs(24, 11).addBox(2.0F, -1.1F, -0.5F, 1.0F, 4.0F, 1.0F, new CubeDeformation(0.0F)),
                PartPose.offsetAndRotation(7.7462F, 0.5773F, -0.5F, 0.0F, 0.0F, 0.8727F));

        return LayerDefinition.create(meshdefinition, 32, 32);
    }

    @Override
    public void setupAnim(RiftEntity entity, float limbSwing, float limbSwingAmount,
                          float ageInTicks, float netHeadYaw, float headPitch) {
        // Bucle idle de 2 segundos extraído de los keyframes del .bbmodel.
        float progress = 0.5F - 0.5F * Mth.cos(ageInTicks * ((float) Math.PI / 20.0F));

        brazo.setPos(brazoX, brazoY - 2.0F * progress, brazoZ);
        brazo2.setPos(brazo2X + progress, brazo2Y + progress, brazo2Z);
        brazo3.setPos(brazo3X - progress, brazo3Y + progress, brazo3Z);
        minibrazo.setPos(minibrazoX - progress, minibrazoY - progress, minibrazoZ);
        minibrazo2.setPos(minibrazo2X + progress, minibrazo2Y - progress, minibrazo2Z);
        minibrazo3.setPos(minibrazo3X, minibrazo3Y + progress, minibrazo3Z);
    }

    @Override
    public void renderToBuffer(PoseStack poseStack, VertexConsumer vertexConsumer,
                               int packedLight, int packedOverlay,
                               float red, float green, float blue, float alpha) {
        brazo.render(poseStack, vertexConsumer, packedLight, packedOverlay, red, green, blue, alpha);
        minibrazo.render(poseStack, vertexConsumer, packedLight, packedOverlay, red, green, blue, alpha);
        minibrazo2.render(poseStack, vertexConsumer, packedLight, packedOverlay, red, green, blue, alpha);
        minibrazo3.render(poseStack, vertexConsumer, packedLight, packedOverlay, red, green, blue, alpha);
        brazo2.render(poseStack, vertexConsumer, packedLight, packedOverlay, red, green, blue, alpha);
        brazo3.render(poseStack, vertexConsumer, packedLight, packedOverlay, red, green, blue, alpha);
    }
}
