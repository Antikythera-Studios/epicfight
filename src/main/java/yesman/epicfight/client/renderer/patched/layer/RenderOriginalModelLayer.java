package yesman.epicfight.client.renderer.patched.layer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;

import net.minecraft.client.model.EntityModel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.world.entity.LivingEntity;
import yesman.epicfight.api.utils.math.MathUtils;
import yesman.epicfight.api.utils.math.OpenMatrix4f;
import yesman.epicfight.api.utils.math.Vec3f;
import yesman.epicfight.world.capabilities.entitypatch.LivingEntityPatch;

public class RenderOriginalModelLayer<E extends LivingEntity, T extends LivingEntityPatch<E>, M extends EntityModel<E>> extends PatchedLayer<E, T, M, RenderLayer<E, M>> {
	private final String parentJoint;
	private final Vec3f vec;
	private final Vec3f rot;
	
	public RenderOriginalModelLayer(String parentJoint, Vec3f vec, Vec3f rot) {
		this.parentJoint = parentJoint;
		this.vec = vec;
		this.rot = rot;
	}
	
	@Override
	protected void renderLayer(T entitypatch, E entityliving, RenderLayer<E, M> vanillaLayer, PoseStack poseStack, MultiBufferSource buffer, int packedLight, OpenMatrix4f[] poses, float bob, float yRot, float xRot, float partialTicks) {
		OpenMatrix4f modelMatrix = poses[entitypatch.getArmature().searchJointByName(this.parentJoint).getId()];
		
		poseStack.pushPose();
		MathUtils.mulStack(poseStack, modelMatrix);
		poseStack.translate(this.vec.x, this.vec.y, this.vec.z);
		poseStack.mulPose(Axis.YP.rotationDegrees(this.rot.y));
		poseStack.mulPose(Axis.XP.rotationDegrees(this.rot.x));
		poseStack.mulPose(Axis.ZP.rotationDegrees(this.rot.z));
		poseStack.scale(-1.0F, -1.0F, 1.0F);
		
		vanillaLayer.render(poseStack, buffer, packedLight, entityliving, entityliving.walkAnimation.position(), entityliving.walkAnimation.speed(), partialTicks, bob, yRot, xRot);
		
		poseStack.popPose();
	}
}
