package yesman.epicfight.client.renderer.patched.layer;

import javax.annotation.Nullable;

import com.mojang.blaze3d.vertex.PoseStack;

import net.minecraft.client.model.EntityModel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.world.entity.LivingEntity;
import yesman.epicfight.api.utils.math.OpenMatrix4f;
import yesman.epicfight.world.capabilities.entitypatch.LivingEntityPatch;

public abstract class PatchedLayer<E extends LivingEntity, T extends LivingEntityPatch<E>, M extends EntityModel<E>, R extends RenderLayer<E, M>> {
	public void renderLayer(E entityliving, T entitypatch, RenderLayer<E, M> vanillaLayer, PoseStack poseStack, MultiBufferSource buffer, int packedLight, OpenMatrix4f[] poses, float bob, float yRot, float xRot, float partialTicks) {
		this.renderLayer(entitypatch, entityliving, this.castLayer(vanillaLayer), poseStack, buffer, packedLight, poses, bob, yRot, xRot, partialTicks);
	}
	
	protected abstract void renderLayer(T entitypatch, E entityliving, @Nullable R vanillaLayer, PoseStack poseStack, MultiBufferSource buffer, int packedLight, OpenMatrix4f[] poses, float bob, float yRot, float xRot, float partialTicks);
	
	@SuppressWarnings("unchecked")
	protected R castLayer(RenderLayer<E, M> layer) {
		return (R)layer;
	}
}