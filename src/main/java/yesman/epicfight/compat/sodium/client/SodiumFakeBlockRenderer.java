package yesman.epicfight.compat.sodium.client;

import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.util.List;

import org.joml.Matrix3f;
import org.joml.Vector3f;
import org.lwjgl.system.MemoryStack;

import com.mojang.blaze3d.shaders.Uniform;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;

import net.caffeinemc.mods.sodium.client.model.quad.BakedQuadView;
import net.caffeinemc.mods.sodium.client.render.chunk.region.RenderRegion;
import net.irisshaders.batchedentityrendering.impl.FullyBufferedMultiBufferSource;
import net.irisshaders.iris.vertices.sodium.terrain.XHFPModelVertexType;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Vec3i;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

import yesman.epicfight.client.renderer.EpicFightRenderTypes;
import yesman.epicfight.client.renderer.FakeBlockRenderer;

public class SodiumFakeBlockRenderer implements FakeBlockRenderer {
	private static final Direction[] DIRECTIONS = Direction.values();
	
	@Override
	public void render(Camera camera, PoseStack poseStack, MultiBufferSource buffers, Level level, BlockPos bp, float r, float g, float b, float a) {
		BlockState bs = level.getBlockState(bp);
		
		if (bs.getRenderShape() != RenderShape.MODEL) {
			return;
		}
		
		RandomSource randomsource = RandomSource.create();
		long seed = bs.getSeed(bp);
		randomsource.setSeed(seed);
		
		Vec3 offset = bs.hasOffsetFunction() ? bs.getOffset(level, bp) : Vec3.ZERO;
		VertexConsumer buffer = buffers.getBuffer(EpicFightRenderTypes.blockHighlight());
		BlockRenderDispatcher blockrenderdispatcher = Minecraft.getInstance().getBlockRenderer();
		var model = blockrenderdispatcher.getBlockModel(bs);
		BlockPos.MutableBlockPos mutablepos = bp.mutable();
		int originX = (bp.getX() & 15);
		int originY = (bp.getY() & 15);
		int originZ = (bp.getZ() & 15);
		
		for (Direction d : DIRECTIONS) {
			List<BakedQuad> culledFaces = model.getQuads(bs, d, randomsource);
			mutablepos.setWithOffset(bp, d);
			
			if (Block.shouldRenderFace(bs, level, bp, d, mutablepos)) {
				this.renderPreviewBlocks(buffer, level, culledFaces, originX, originY, originZ, offset, r, g, b, a);
			}
		}
		
		this.renderPreviewBlocks(buffer, level, model.getQuads(bs, null, randomsource), originX, originY, originZ, offset, r, g, b, a);
		
		RenderSystem.getModelViewStack().pushMatrix();
		RenderSystem.getModelViewStack().mul(poseStack.last().pose());
		RenderSystem.applyModelViewMatrix();
		
		Uniform uniform = GameRenderer.getRendertypeTranslucentShader().CHUNK_OFFSET;
		Vec3 camPos = camera.getPosition();
		int cameraBpX = (int)camPos.x;
		int cameraBpY = (int)camPos.y;
		int cameraBpZ = (int)camPos.z;
		
		/**
		 * Calculates block chunk origin and follow type convert order to avoid z-fighting
		 * Sodium's model view matrix setup for chunk render: {@link DefaultChunkRenderer#setModelMatrixUniforms}
		 */
		uniform.set(
			((bp.getX() >> 4 << 4) - cameraBpX) - fractional(camPos.x),
			((bp.getY() >> 4 << 4) - cameraBpY) - fractional(camPos.y),
			((bp.getZ() >> 4 << 4) - cameraBpZ) - fractional(camPos.z)
		);
		
		if (buffers instanceof FullyBufferedMultiBufferSource irisBuffer) {
			irisBuffer.endBatch();
		} else if (buffers instanceof MultiBufferSource.BufferSource vanillaBuffer) {
			vanillaBuffer.endLastBatch();
		}
		
		uniform.set(0.0F, 0.0F, 0.0F);
		RenderSystem.getModelViewStack().popMatrix();
		RenderSystem.applyModelViewMatrix();
	}
	
	private void renderPreviewBlocks(VertexConsumer consumer, BlockAndTintGetter level, List<BakedQuad> quads, int originX, int originY, int originZ, Vec3 offset, float r, float g, float b, float a) {
		for (BakedQuad bakedquad : quads) {
			float f = level.getShade(bakedquad.getDirection(), bakedquad.isShade());
			putBulkDataWithoutPose(consumer, bakedquad, originX, originY, originZ, offset, new float[] {f, f, f, f}, r, g, b, a, new int[] {16777215, 16777215, 16777215, 16777215}, OverlayTexture.NO_OVERLAY, false);
		}
	}

	private static void putBulkDataWithoutPose(VertexConsumer vertexConsumer, BakedQuad pQuad, int originX, int originY, int originZ, Vec3 offset, float[] pColorMuls, float pRed, float pGreen, float pBlue, float alpha, int[] pCombinedLights, int pCombinedOverlay, boolean pMulColor) {
		float[] afloat = new float[] { pColorMuls[0], pColorMuls[1], pColorMuls[2], pColorMuls[3] };
		int[] aint1 = pQuad.getVertices();
		Vec3i vec3i = pQuad.getDirection().getNormal();
		Vector3f vector3f = new Vector3f((float) vec3i.getX(), (float) vec3i.getY(), (float) vec3i.getZ());
		int j = pQuad.getVertices().length / 8;
		
		BakedQuadView bakedQuadView = (BakedQuadView)pQuad;
		
		try (MemoryStack memorystack = MemoryStack.stackPush()) {
			ByteBuffer bytebuffer = memorystack.malloc(DefaultVertexFormat.BLOCK.getVertexSize());
			IntBuffer intbuffer = bytebuffer.asIntBuffer();
			
			for (int k = 0; k < j; ++k) {
				intbuffer.clear();
				intbuffer.put(aint1, k * 8, 8);
				
				/**
				 * Sodium's block quad vertices upload:
				 * {@link me.jellysquid.mods.sodium.client.render.chunk.compile.pipeline.BlockRenderer#writeGeometry}
				 */
				float outX = decodePosition(encodePosition(originX + bakedQuadView.getX(k) + (float)offset.x()));
				float outY = decodePosition(encodePosition(originY + bakedQuadView.getY(k) + (float)offset.y()));
				float outZ = decodePosition(encodePosition(originZ + bakedQuadView.getZ(k) + (float)offset.z()));
				
				float f3;
				float f4;
				float f5;
				if (pMulColor) {
					float f6 = (float) (bytebuffer.get(12) & 255) / 255.0F;
					float f7 = (float) (bytebuffer.get(13) & 255) / 255.0F;
					float f8 = (float) (bytebuffer.get(14) & 255) / 255.0F;
					f3 = f6 * afloat[k] * pRed;
					f4 = f7 * afloat[k] * pGreen;
					f5 = f8 * afloat[k] * pBlue;
				} else {
					f3 = afloat[k] * pRed;
					f4 = afloat[k] * pGreen;
					f5 = afloat[k] * pBlue;
				}

				int l = applyBakedLighting(pCombinedLights[k], bytebuffer);
				float f9 = bytebuffer.getFloat(16);
				float f10 = bytebuffer.getFloat(20);
				applyBakedNormals(vector3f, bytebuffer, new Matrix3f());
				float vertexAlpha = pMulColor ? alpha * (float) (bytebuffer.get(15) & 255) / 255.0F : alpha;
				
				vertexConsumer.addVertex(outX, outY, outZ)
			        .setColor(f3, f4, f5, vertexAlpha)
			        .setUv(f9, f10)
			        .setOverlay(pCombinedOverlay)
			        .setLight(l)
			        .setNormal(vector3f.x(), vector3f.y(), vector3f.z());
			}
		}
	}

	/**
	 * Code copy from {@link IForgeVertexConsumer#applyBakedLighting}
	 */
	static int applyBakedLighting(int packedLight, ByteBuffer data) {
		int bl = packedLight & 0xFFFF;
		int sl = (packedLight >> 16) & 0xFFFF;
		// DefaultVertexFormat.BLOCK is Position, Color, UV0, UV2, Normal, so the lightmap (UV2)
		// starts at int 6 (byte 24) - this is NeoForge's IQuadTransformer.UV2, which has no
		// Fabric equivalent. Reading int 4 here would decode the UV0 texture floats as light.
		int offset = 6 * 4;
		int blBaked = Short.toUnsignedInt(data.getShort(offset));
		int slBaked = Short.toUnsignedInt(data.getShort(offset + 2));
		bl = Math.max(bl, blBaked);
		sl = Math.max(sl, slBaked);
		return bl | (sl << 16);
	}

	/**
	 * Code copy from {@link IForgeVertexConsumer#applyBakedNormals}
	 */
	static void applyBakedNormals(Vector3f generated, ByteBuffer data, Matrix3f normalTransform) {
		byte nx = data.get(28);
		byte ny = data.get(29);
		byte nz = data.get(30);
		if (nx != 0 || ny != 0 || nz != 0) {
			generated.set(nx / 127f, ny / 127f, nz / 127f);
			generated.mul(normalTransform);
		}
	}

	private static float fractional(double value) {
		float fullPrecision = (float) (value - (int)value);
		float modifier = Math.copySign(RenderRegion.REGION_WIDTH * 16, fullPrecision);
		return (fullPrecision + modifier) - modifier;
	}

	/**
	 * copies from {@link XHFPModelVertexType}
	 */
	private static final int POSITION_MAX_VALUE = 65536;
	private static final float MODEL_ORIGIN = 8.0f;
	private static final float MODEL_RANGE = 32.0f;
	private static final float MODEL_SCALE = MODEL_RANGE / POSITION_MAX_VALUE;
	private static final float MODEL_SCALE_INV = POSITION_MAX_VALUE / MODEL_RANGE;
	
	static short encodePosition(float v) {
		return (short) ((MODEL_ORIGIN + v) * MODEL_SCALE_INV);
	}
	
	static float decodePosition(short raw) {
		return (raw & 0xFFFF) * MODEL_SCALE - MODEL_ORIGIN;
	}
}
