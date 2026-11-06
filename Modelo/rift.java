// Made with Blockbench 5.2.2
// Exported for Minecraft version 1.17 or later with Mojang mappings
// Paste this class into your mod and generate all required imports


public class rift<T extends Entity> extends EntityModel<T> {
	// This layer location should be baked with EntityRendererProvider.Context in the entity renderer and passed into this model's constructor
	public static final ModelLayerLocation LAYER_LOCATION = new ModelLayerLocation(new ResourceLocation("modid", "rift"), "main");
	private final ModelPart brazo;
	private final ModelPart minibrazo;
	private final ModelPart minibrazo2;
	private final ModelPart minibrazo3;
	private final ModelPart brazo2;
	private final ModelPart brazo3;

	public rift(ModelPart root) {
		this.brazo = root.getChild("brazo");
		this.minibrazo = root.getChild("minibrazo");
		this.minibrazo2 = root.getChild("minibrazo2");
		this.minibrazo3 = root.getChild("minibrazo3");
		this.brazo2 = root.getChild("brazo2");
		this.brazo3 = root.getChild("brazo3");
	}

	public static LayerDefinition createBodyLayer() {
		MeshDefinition meshdefinition = new MeshDefinition();
		PartDefinition partdefinition = meshdefinition.getRoot();

		PartDefinition brazo = partdefinition.addOrReplaceChild("brazo", CubeListBuilder.create().texOffs(0, 12).addBox(-0.875F, -6.75F, -0.5F, 1.0F, 11.0F, 1.0F, new CubeDeformation(0.0F))
		.texOffs(12, 19).addBox(-2.875F, -4.75F, -0.5F, 2.0F, 7.0F, 1.0F, new CubeDeformation(0.0F))
		.texOffs(8, 12).addBox(0.125F, -3.75F, -0.5F, 1.0F, 10.0F, 1.0F, new CubeDeformation(0.0F))
		.texOffs(4, 24).addBox(1.125F, -0.75F, -0.5F, 1.0F, 4.0F, 1.0F, new CubeDeformation(0.0F)), PartPose.offset(1.875F, 14.75F, -0.5F));

		PartDefinition minibrazo = partdefinition.addOrReplaceChild("minibrazo", CubeListBuilder.create().texOffs(20, 10).addBox(-0.5F, -3.5F, -0.5F, 1.0F, 7.0F, 1.0F, new CubeDeformation(0.0F))
		.texOffs(24, 0).addBox(0.5F, -2.5F, -0.5F, 1.0F, 5.0F, 1.0F, new CubeDeformation(0.0F))
		.texOffs(24, 16).addBox(-1.5F, -1.5F, -0.5F, 1.0F, 3.0F, 1.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(5.8782F, 10.3267F, -0.5F, 0.0F, 0.0F, -0.6109F));

		PartDefinition minibrazo2 = partdefinition.addOrReplaceChild("minibrazo2", CubeListBuilder.create().texOffs(20, 0).addBox(-0.5F, -4.0F, -0.5F, 1.0F, 9.0F, 1.0F, new CubeDeformation(0.0F))
		.texOffs(8, 23).addBox(0.5F, -3.0F, -0.5F, 1.0F, 6.0F, 1.0F, new CubeDeformation(0.0F))
		.texOffs(24, 20).addBox(-1.5F, -2.0F, -0.5F, 1.0F, 3.0F, 1.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-5.7362F, 10.165F, -0.5F, 0.0F, 0.0F, 0.9599F));

		PartDefinition minibrazo3 = partdefinition.addOrReplaceChild("minibrazo3", CubeListBuilder.create().texOffs(0, 24).addBox(-1.0F, -2.75F, -0.5F, 1.0F, 6.0F, 1.0F, new CubeDeformation(0.0F))
		.texOffs(24, 24).addBox(0.0F, -1.75F, -0.5F, 1.0F, 3.0F, 1.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.6364F, -0.5539F, -0.5F, 0.0F, 0.0F, -0.1309F));

		PartDefinition brazo2 = partdefinition.addOrReplaceChild("brazo2", CubeListBuilder.create().texOffs(0, 0).addBox(-1.5F, -6.75F, -0.5F, 2.0F, 11.0F, 1.0F, new CubeDeformation(0.0F))
		.texOffs(18, 19).addBox(-3.5F, -4.75F, -0.5F, 2.0F, 7.0F, 1.0F, new CubeDeformation(0.0F))
		.texOffs(12, 8).addBox(0.5F, -3.75F, -0.5F, 1.0F, 10.0F, 1.0F, new CubeDeformation(0.0F))
		.texOffs(24, 6).addBox(1.5F, -0.75F, -0.5F, 1.0F, 4.0F, 1.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-5.773F, 1.6915F, -0.5F, 0.0F, 0.0F, -0.7854F));

		PartDefinition brazo3 = partdefinition.addOrReplaceChild("brazo3", CubeListBuilder.create().texOffs(6, 0).addBox(-2.0F, -7.1F, -0.5F, 2.0F, 11.0F, 1.0F, new CubeDeformation(0.0F))
		.texOffs(12, 0).addBox(-5.0F, -5.1F, -0.5F, 3.0F, 7.0F, 1.0F, new CubeDeformation(0.0F))
		.texOffs(4, 12).addBox(0.0F, -4.1F, -0.5F, 1.0F, 11.0F, 1.0F, new CubeDeformation(0.0F))
		.texOffs(16, 8).addBox(1.0F, -4.1F, -0.5F, 1.0F, 10.0F, 1.0F, new CubeDeformation(0.0F))
		.texOffs(24, 11).addBox(2.0F, -1.1F, -0.5F, 1.0F, 4.0F, 1.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(7.7462F, 0.5773F, -0.5F, 0.0F, 0.0F, 0.8727F));

		return LayerDefinition.create(meshdefinition, 32, 32);
	}

	@Override
	public void setupAnim(Entity entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch) {

	}

	@Override
	public void renderToBuffer(PoseStack poseStack, VertexConsumer vertexConsumer, int packedLight, int packedOverlay, float red, float green, float blue, float alpha) {
		brazo.render(poseStack, vertexConsumer, packedLight, packedOverlay, red, green, blue, alpha);
		minibrazo.render(poseStack, vertexConsumer, packedLight, packedOverlay, red, green, blue, alpha);
		minibrazo2.render(poseStack, vertexConsumer, packedLight, packedOverlay, red, green, blue, alpha);
		minibrazo3.render(poseStack, vertexConsumer, packedLight, packedOverlay, red, green, blue, alpha);
		brazo2.render(poseStack, vertexConsumer, packedLight, packedOverlay, red, green, blue, alpha);
		brazo3.render(poseStack, vertexConsumer, packedLight, packedOverlay, red, green, blue, alpha);
	}
}