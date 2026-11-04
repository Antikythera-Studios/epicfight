package yesman.epicfight.client.renderer.entity;

import net.minecraft.client.renderer.entity.EntityRendererProvider.Context;
import net.minecraft.client.renderer.entity.WitherSkeletonRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.monster.WitherSkeleton;
import yesman.epicfight.EpicFight;

public class WitherSkeletonMinionRenderer extends WitherSkeletonRenderer {
    private static final ResourceLocation WITHER_SKELETON_LOCATION = EpicFight.identifier("textures/entity/wither_skeleton_minion.png");

	public WitherSkeletonMinionRenderer(Context context) {
		super(context);
	}
	
	@Override
	public ResourceLocation getTextureLocation(WitherSkeleton entity) {
		return WITHER_SKELETON_LOCATION;
	}
}