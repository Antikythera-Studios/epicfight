package yesman.epicfight.client.renderer.blockentity;

import org.joml.Quaternionf;
import org.joml.Vector3f;

import com.mojang.blaze3d.vertex.PoseStack;

import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

import yesman.epicfight.api.utils.math.MathUtils;
import yesman.epicfight.world.level.block.entity.FractureBlockEntity;

public class FractureBlockRenderer implements BlockEntityRenderer<FractureBlockEntity> {
	private final BlockRenderDispatcher blockRenderDispatcher;
	
	public FractureBlockRenderer(BlockEntityRendererProvider.Context context) {
		this.blockRenderDispatcher = context.getBlockRenderDispatcher();
	}
	
	@Override
	public boolean shouldRender(FractureBlockEntity p_173568_, Vec3 p_173569_) {
		return Vec3.atCenterOf(p_173568_.getBlockPos()).closerThan(p_173569_, this.getViewDistance());
	}
	
	@Override
	public void render(FractureBlockEntity blockEntity, float partialTicks, PoseStack poseStack, MultiBufferSource multiBufferSource, int lightColor, int overlayColor) {
		float turnBackTime = 5.0F;
		float lerpAmount = Mth.clamp(partialTicks * (1.0F / turnBackTime) + (turnBackTime - (blockEntity.getMaxLifeTime() - blockEntity.getLifeTime())) * (1.0F / turnBackTime), 0.0F, 1.0F);
		
		Vector3f translate = blockEntity.getMaxLifeTime() > blockEntity.getLifeTime() + turnBackTime ? blockEntity.getTranslate() : MathUtils.lerpMojangVector(blockEntity.getTranslate(), new Vector3f(), lerpAmount);
		Quaternionf rotate = blockEntity.getMaxLifeTime() > blockEntity.getLifeTime() + turnBackTime ? blockEntity.getRotation() : MathUtils.lerpQuaternion(blockEntity.getRotation(), new Quaternionf(), lerpAmount);
		
		double BOUNCE_MAX_HEIGHT = blockEntity.getBouncing();
		double TIME = Math.max(BOUNCE_MAX_HEIGHT * 8.0D, 8.0D);
		double EXTENDER = 1 / Math.pow(TIME * 0.5, 2);
		double MOVE_GRAPH = Math.sqrt(BOUNCE_MAX_HEIGHT / EXTENDER);
		double bouncingAnimation = Math.max(-EXTENDER * Math.pow(blockEntity.getLifeTime() + partialTicks - MOVE_GRAPH, 2.0D) + BOUNCE_MAX_HEIGHT, 0.0D);
		
		poseStack.pushPose();
		poseStack.translate(0.5D, 0.5D, 0.5D);
		poseStack.mulPose(rotate);
		poseStack.translate(translate.x(), translate.y() + bouncingAnimation, translate.z());
		poseStack.translate(-0.5D, -0.5D, -0.5D);	
		
		this.blockRenderDispatcher.renderBreakingTexture(blockEntity.getOriginalBlockState(), blockEntity.getBlockPos().above(), blockEntity.getLevel(), poseStack, multiBufferSource.getBuffer(RenderType.cutout()));
		
		poseStack.popPose();
	}
}