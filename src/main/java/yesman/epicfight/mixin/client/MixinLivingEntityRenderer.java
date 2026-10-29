package yesman.epicfight.mixin.client;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.world.entity.LivingEntity;

@Mixin(value = LivingEntityRenderer.class)
public interface MixinLivingEntityRenderer {
	@Invoker("isBodyVisible")
	public boolean invokeIsBodyVisible(LivingEntity entity);

	@Invoker("getRenderType")
	public RenderType invokeGetRenderType(LivingEntity entity, boolean flag1, boolean flag2, boolean flag3);

	@Invoker("getBob")
	public float invokeGetBob(LivingEntity entity, float partialTicks);
}
