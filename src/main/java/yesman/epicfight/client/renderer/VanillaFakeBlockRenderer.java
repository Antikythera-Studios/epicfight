package yesman.epicfight.client.renderer;

import java.util.List;

import com.mojang.blaze3d.shaders.Uniform;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;

import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;


public class VanillaFakeBlockRenderer implements FakeBlockRenderer {
	private static final Direction[] DIRECTIONS = Direction.values();
	
	public void render(Camera camera, PoseStack poseStack, MultiBufferSource buffers, Level level, BlockPos bp, float r, float g, float b, float a) {
		BlockState bs = level.getBlockState(bp);
		
		if (bs.getRenderShape() != RenderShape.MODEL) {
			return;
		}
		
		RandomSource randomsource = RandomSource.create();
		long seed = bs.getSeed(bp);
		randomsource.setSeed(seed);
		
		PoseStack poseStack2 = new PoseStack();
		Vec3 vec = bs.getOffset(level, bp);
		
		poseStack2.pushPose();
		poseStack2.translate((float)(bp.getX() & 15), (float)(bp.getY() & 15), (float)(bp.getZ() & 15));
		poseStack2.translate(vec.x, vec.y, vec.z);
		
		Vec3 camPos = camera.getPosition();
		
		VertexConsumer buffer = buffers.getBuffer(EpicFightRenderTypes.blockHighlight());
		
		BlockRenderDispatcher blockrenderdispatcher = Minecraft.getInstance().getBlockRenderer();
		var model = blockrenderdispatcher.getBlockModel(bs);
		BlockPos.MutableBlockPos mutablepos = bp.mutable();
		
		for (Direction d : DIRECTIONS) {
			List<BakedQuad> culledFaces = model.getQuads(bs, d, randomsource);
			mutablepos.setWithOffset(bp, d);
			
			if (Block.shouldRenderFace(bs, level, bp, d, mutablepos)) {
				this.renderPreviewBlocks(poseStack2, buffer, level, culledFaces, r, g, b, a);
			}
		}
		
		this.renderPreviewBlocks(poseStack2, buffer, level, model.getQuads(bs, null, randomsource), r, g, b, a);
		
		poseStack2.popPose();
		
		RenderSystem.getModelViewStack().pushMatrix();
		RenderSystem.getModelViewStack().mul(poseStack.last().pose());
		RenderSystem.applyModelViewMatrix();
		
		Uniform uniform = GameRenderer.getRendertypeTranslucentShader().CHUNK_OFFSET;
		
		// Calculates block chunk origin and follow type convert order to avoid z-fighting
		uniform.set(
			(float)((double)(bp.getX() >> 4 << 4) - camPos.x()),
			(float)((double)(bp.getY() >> 4 << 4) - camPos.y()),
			(float)((double)(bp.getZ() >> 4 << 4) - camPos.z())
		);
		
		if (buffers instanceof MultiBufferSource.BufferSource vanillaBuffer) {
			vanillaBuffer.endLastBatch();
		}
		
		uniform.set(0.0F, 0.0F, 0.0F);
		
		poseStack2.popPose();
		
		RenderSystem.getModelViewStack().popMatrix();
		RenderSystem.applyModelViewMatrix();
	}
	
	private void renderPreviewBlocks(PoseStack poseStack, VertexConsumer consumer, BlockAndTintGetter level, List<BakedQuad> quads, float r, float g, float b, float a) {
		for (BakedQuad bakedquad : quads) {
			float f = level.getShade(bakedquad.getDirection(), bakedquad.isShade());
			consumer.putBulkData(poseStack.last(), bakedquad, new float[] {f, f, f, f}, r, g, b, a, new int[] {16777215, 16777215, 16777215, 16777215}, OverlayTexture.NO_OVERLAY, false);
		}
	}
}
