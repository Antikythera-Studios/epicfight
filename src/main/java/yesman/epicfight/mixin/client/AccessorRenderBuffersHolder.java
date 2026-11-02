package yesman.epicfight.mixin.client;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.gen.Accessor;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.RenderBuffers;

public final class AccessorRenderBuffersHolder {
	private AccessorRenderBuffersHolder() {
	}

	@Mixin(LevelRenderer.class)
	public interface OfLevelRenderer {
		@Accessor("renderBuffers")
		RenderBuffers epicfight$getRenderBuffers();

		@Mutable
		@Accessor("renderBuffers")
		void epicfight$setRenderBuffers(RenderBuffers buffers);
	}

	@Mixin(Minecraft.class)
	public interface OfMinecraft {
		@Accessor("renderBuffers")
		RenderBuffers epicfight$getRenderBuffers();

		@Mutable
		@Accessor("renderBuffers")
		void epicfight$setRenderBuffers(RenderBuffers buffers);
	}
}
