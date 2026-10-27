package yesman.epicfight.mixin.client;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

import net.minecraft.client.model.EntityModel;
import net.minecraft.client.renderer.entity.layers.RenderLayer;

@Mixin(RenderLayer.class)
public interface MixinRenderLayer {
	@Invoker("getParentModel")
	EntityModel<?> epicfight$getParentModel();
}
