package yesman.epicfight.client.renderer.patched.layer;

import com.mojang.blaze3d.vertex.PoseStack;

import net.minecraft.client.model.DrownedModel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.layers.DrownedOuterLayer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.monster.Drowned;
import yesman.epicfight.api.client.model.Meshes;
import yesman.epicfight.api.utils.math.OpenMatrix4f;
import yesman.epicfight.client.mesh.HumanoidMesh;
import yesman.epicfight.world.capabilities.entitypatch.mob.DrownedPatch;

public class OuterLayerRenderer extends ModelRenderLayer<Drowned, DrownedPatch, DrownedModel<Drowned>, DrownedOuterLayer<Drowned>, HumanoidMesh> {
	public static final ResourceLocation DROWNED_OUTER_LAYER = ResourceLocation.withDefaultNamespace("textures/entity/zombie/drowned_outer_layer.png");
	
	public OuterLayerRenderer() {
		super(Meshes.BIPED_OUTLAYER);
	}
	
	@Override
	protected void renderLayer(DrownedPatch entitypatch, Drowned entityliving, DrownedOuterLayer<Drowned> vanillaLayer, PoseStack poseStack, MultiBufferSource buffer, int packedLight,
			OpenMatrix4f[] poses, float bob, float yRot, float xRot, float partialTicks) {
		this.mesh.get().draw(poseStack, buffer, RenderType.entityCutoutNoCull(DROWNED_OUTER_LAYER), packedLight, 1.0F, 1.0F, 1.0F, 1.0F, LivingEntityRenderer.getOverlayCoords(entityliving, 0.0F), entitypatch.getArmature(), poses);
	}
	
	@Override
	protected void initMesh() {
		this.mesh.get().initialize();
	}
}