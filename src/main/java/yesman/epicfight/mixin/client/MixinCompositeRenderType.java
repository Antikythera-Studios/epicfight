package yesman.epicfight.mixin.client;

import java.util.Optional;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.mojang.blaze3d.vertex.VertexFormat;

import net.minecraft.client.renderer.RenderStateShard.TextureStateShard;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.RenderType.CompositeRenderType;
import yesman.epicfight.client.renderer.EpicFightRenderTypes;

@Mixin(value = CompositeRenderType.class)
public class MixinCompositeRenderType {
	@Shadow
	private Optional<RenderType> outline;
	
	@Inject(at = @At(value = "RETURN"), method = "<init>")
	private void epicfight_renderTypeInit(CallbackInfo info) {
		CompositeRenderType self = (CompositeRenderType)(Object)this;
		
		if (self.mode() == VertexFormat.Mode.TRIANGLES && self.state.textureState instanceof TextureStateShard texStateShard && texStateShard.texture.isPresent()) {
			EpicFightRenderTypes.addRenderType(self.name, texStateShard.texture.get(), self);
		}
	}
}