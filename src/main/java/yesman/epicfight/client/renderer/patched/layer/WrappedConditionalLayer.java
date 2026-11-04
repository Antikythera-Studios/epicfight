package yesman.epicfight.client.renderer.patched.layer;

import java.util.function.Function;

import javax.annotation.Nullable;

import com.mojang.blaze3d.vertex.PoseStack;

import net.minecraft.client.model.EntityModel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.world.entity.LivingEntity;
import yesman.epicfight.api.utils.math.OpenMatrix4f;
import yesman.epicfight.world.capabilities.entitypatch.LivingEntityPatch;

public class WrappedConditionalLayer<E extends LivingEntity, T extends LivingEntityPatch<E>, M extends EntityModel<E>, R extends RenderLayer<E, M>> extends PatchedLayer<E, T, M, R> {
	private final PatchedLayer<E, T, M, R> layer;
	private final Function<LivingEntityPatch<?>, Boolean> renderCondition;
	
	public WrappedConditionalLayer(PatchedLayer<E, T, M, R> layer, Function<LivingEntityPatch<?>, Boolean> renderCondition) {
		this.layer = layer;
		this.renderCondition = renderCondition;
	}
	
	@Override
	protected void renderLayer(T entitypatch, E entityliving, @Nullable R vanillaLayer, PoseStack poseStack, MultiBufferSource buffer, int packedLight, OpenMatrix4f[] poses, float bob, float yRot, float xRot, float partialTicks) {
		if (this.renderCondition.apply(entitypatch)) {
			this.layer.renderLayer(entitypatch, entityliving, vanillaLayer, poseStack, buffer, packedLight, poses, bob, yRot, xRot, partialTicks);
		}
	}
}