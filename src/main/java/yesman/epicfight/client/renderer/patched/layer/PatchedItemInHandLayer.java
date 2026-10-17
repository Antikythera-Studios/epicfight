package yesman.epicfight.client.renderer.patched.layer;

import com.mojang.blaze3d.vertex.PoseStack;

import net.minecraft.client.model.EntityModel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import yesman.epicfight.api.utils.math.OpenMatrix4f;
import yesman.epicfight.client.events.engine.RenderEngine;
import yesman.epicfight.client.renderer.patched.item.RenderItemBase;
import yesman.epicfight.world.capabilities.entitypatch.LivingEntityPatch;

public class PatchedItemInHandLayer<E extends LivingEntity, T extends LivingEntityPatch<E>, M extends EntityModel<E>> extends PatchedLayer<E, T, M, RenderLayer<E, M>> {
	@Override
	protected void renderLayer(T entitypatch, E entityliving, RenderLayer<E, M> vanillaLayer, PoseStack postStack, MultiBufferSource buffer, int packedLight, OpenMatrix4f[] poses, float bob, float yRot, float xRot, float partialTicks) {
		ItemStack mainHandStack = entitypatch.getOriginal().getMainHandItem();

		if (mainHandStack.getItem() != Items.AIR && entitypatch.isMainhandItemValid()) {
			RenderItemBase renderer = RenderEngine.getInstance().getItemRenderer(mainHandStack);
			if (renderer != null) {
				renderer.renderItemInHand(mainHandStack, entitypatch, InteractionHand.MAIN_HAND, poses, buffer, postStack, packedLight, partialTicks);
			}
		}

		ItemStack offHandStack = entitypatch.getOriginal().getOffhandItem();

		if (entitypatch.isOffhandItemValid()) {
			RenderItemBase renderer = RenderEngine.getInstance().getItemRenderer(offHandStack);
			if (renderer != null) {
				renderer.renderItemInHand(offHandStack, entitypatch, InteractionHand.OFF_HAND, poses, buffer, postStack, packedLight, partialTicks);
			}
		}
	}
}