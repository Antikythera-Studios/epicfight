package yesman.epicfight.client.renderer.entity;

import net.minecraft.client.renderer.entity.EntityRendererProvider.Context;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import yesman.epicfight.client.renderer.patched.entity.PWitherRenderer;
import yesman.epicfight.world.entity.WitherGhostClone;

public class WitherGhostRenderer extends NoopLivingEntityRenderer<WitherGhostClone> {
	public WitherGhostRenderer(Context context) {
		super(context, 1.0F);
	}
	
	@Override
	protected int getBlockLightLevel(WitherGhostClone witherBoss, BlockPos blockpos) {
		return 15;
	}
	
	@Override
	public ResourceLocation getTextureLocation(WitherGhostClone entity) {
		return PWitherRenderer.WITHER_INVULNERABLE_LOCATION;
	}
}