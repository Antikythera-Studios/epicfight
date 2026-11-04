package yesman.epicfight.client.renderer.patched.item;

import com.google.gson.JsonElement;
import com.mojang.blaze3d.vertex.PoseStack;

import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import yesman.epicfight.api.utils.math.MathUtils;
import yesman.epicfight.api.utils.math.OpenMatrix4f;
import yesman.epicfight.world.capabilities.entitypatch.LivingEntityPatch;

public class RenderTwoHandedRangedWeapon extends RenderItemBase {
	public RenderTwoHandedRangedWeapon(JsonElement jsonElement) {
		super(jsonElement);
	}
	
	@Override
	public void renderItemInHand(ItemStack stack, LivingEntityPatch<?> entitypatch, InteractionHand hand, OpenMatrix4f[] poses, MultiBufferSource buffer, PoseStack poseStack, int packedLight, float partialTicks) {
		OpenMatrix4f modelMatrix = this.getCorrectionMatrix(entitypatch, InteractionHand.OFF_HAND, poses);
		
		poseStack.pushPose();
		MathUtils.mulStack(poseStack, modelMatrix);
		itemInHandRenderer.renderItem(entitypatch.getOriginal(), stack, ItemDisplayContext.THIRD_PERSON_RIGHT_HAND, false, poseStack, buffer, packedLight);
		poseStack.popPose();
	}
}