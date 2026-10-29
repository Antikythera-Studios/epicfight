package yesman.epicfight.mixin.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import yesman.epicfight.api.client.event.EpicFightClientEventHooks;
import yesman.epicfight.api.client.event.types.render.RenderLivingPreEvent;

/// Class mixin for [LivingEntityRenderer] that intercepts [render] at HEAD to fire [RenderLivingPreEvent].
///
/// This is separate from [MixinLivingEntityRenderer] (the interface accessor mixin) because
/// `@Inject` requires a class mixin, while `@Invoker` methods work in interface mixins.
@Mixin(value = LivingEntityRenderer.class)
public abstract class MixinLivingEntityRendererInject {
	@Inject(
		method = "render(Lnet/minecraft/world/entity/LivingEntity;FFLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)V",
		at = @At("HEAD"),
		cancellable = true
	)
	private void epicfight$renderLivingPre(LivingEntity entity, float entityYaw, float partialTick, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight, CallbackInfo callbackInfo) {
		LivingEntityRenderer<?, ?> self = (LivingEntityRenderer<?, ?>)(Object)this;
		RenderLivingPreEvent event = new RenderLivingPreEvent(entity, self, partialTick, poseStack, bufferSource, packedLight);

		if (EpicFightClientEventHooks.Render.RENDER_LIVING_PRE.post(event).isCanceled()) {
			callbackInfo.cancel();
		}
	}
}
