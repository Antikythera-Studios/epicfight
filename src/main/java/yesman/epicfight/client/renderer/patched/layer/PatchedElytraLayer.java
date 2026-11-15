package yesman.epicfight.client.renderer.patched.layer;

import com.mojang.blaze3d.vertex.PoseStack;

import net.minecraft.client.model.EntityModel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.layers.ElytraLayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import yesman.epicfight.api.utils.math.MathUtils;
import yesman.epicfight.api.utils.math.OpenMatrix4f;
import yesman.epicfight.api.utils.math.Vec3f;
import yesman.epicfight.world.capabilities.entitypatch.LivingEntityPatch;

public class PatchedElytraLayer<E extends LivingEntity, T extends LivingEntityPatch<E>, M extends EntityModel<E>> extends PatchedLayer<E, T, M, ElytraLayer<E, M>> {
	@Override
	protected void renderLayer(T entitypatch, E livingentity, ElytraLayer<E, M> vanillaLayer, PoseStack poseStack, MultiBufferSource buffer, int packedLight, OpenMatrix4f[] poses, float bob, float yRot, float xRot, float partialTicks) {
		if (livingentity.getItemBySlot(EquipmentSlot.CHEST).is(Items.ELYTRA)) {
			vanillaLayer.getParentModel().copyPropertiesTo((net.minecraft.client.model.EntityModel) ((yesman.epicfight.mixin.client.ElytraLayerAccessor) vanillaLayer).epicfight$getElytraModel());
			OpenMatrix4f modelMatrix = new OpenMatrix4f();
			modelMatrix.scale(new Vec3f(-0.9F, -0.9F, 0.9F)).translate(new Vec3f(0.0F, -0.5F, -0.1F)).mulFront(poses[8]);
			poseStack.pushPose();
			MathUtils.mulStack(poseStack, modelMatrix);
			vanillaLayer.render(poseStack, buffer, packedLight, livingentity, livingentity.walkAnimation.position(), livingentity.walkAnimation.speed(), partialTicks, bob, yRot, xRot);
			poseStack.popPose();
		}
	}
}