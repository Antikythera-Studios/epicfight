package yesman.epicfight.mixin.client;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderBuffers;
import yesman.epicfight.api.client.model.rebake.OffscreenCapture;

@Mixin(RenderBuffers.class)
public class MixinRenderBuffers {
	@Inject(method = "bufferSource", at = @At("HEAD"), cancellable = true)
	private void epicfight$redirectToOffscreenCapture(CallbackInfoReturnable<MultiBufferSource.BufferSource> cir) {
		MultiBufferSource.BufferSource redirect = OffscreenCapture.redirect();

		if (redirect != null) {
			cir.setReturnValue(redirect);
		}
	}
}
