package yesman.epicfight.api.client.model;

import java.nio.ByteBuffer;
import java.nio.IntBuffer;

import javax.annotation.Nullable;

import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.joml.Vector4f;
import org.lwjgl.system.MemoryStack;

import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;

import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.core.Vec3i;
import net.minecraft.resources.ResourceLocation;

import yesman.epicfight.api.model.Armature;
import yesman.epicfight.api.utils.math.OpenMatrix4f;
import yesman.epicfight.api.utils.math.Vec3f;
import yesman.epicfight.client.renderer.EpicFightRenderTypes;

public interface Mesh {
	
	void initialize();
	
	/* Draw wihtout mesh deformation */
	void draw(PoseStack poseStack, VertexConsumer vertexConsumer, Mesh.DrawingFunction drawingFunction, int packedLight, float r, float g, float b, float a, int overlay);
	
	/* Draw with mesh deformation */
	void drawPosed(PoseStack poseStack, VertexConsumer vertexConsumer, Mesh.DrawingFunction drawingFunction, int packedLight, float r, float g, float b, float a, int overlay, @Nullable Armature armature, OpenMatrix4f[] poses);
	
	/* Universal method */
	default void draw(PoseStack poseStack, MultiBufferSource bufferSources, RenderType renderType, Mesh.DrawingFunction drawingFunction, int packedLight, float r, float g, float b, float a, int overlay, @Nullable Armature armature, OpenMatrix4f[] poses) {
		this.drawPosed(poseStack, bufferSources.getBuffer(EpicFightRenderTypes.getTriangulated(renderType)), drawingFunction, packedLight, r, g, b, a, overlay, armature, poses);
	}
	
	public static record RenderProperties(ResourceLocation customTexturePath, Vec3f customColor, boolean isTransparent) {
		public static class Builder {
			protected String customTexturePath;
			protected Vec3f customColor = new Vec3f();
			protected boolean isTransparent;
			
			public RenderProperties.Builder customTexturePath(String path) {
				this.customTexturePath = path;
				return this;
			}
			
			public RenderProperties.Builder transparency(boolean isTransparent) {
				this.isTransparent = isTransparent;
				return this;
			}
			
			public RenderProperties.Builder customColor(float r, float g, float b) {
				this.customColor.x = r;
				this.customColor.y = g;
				this.customColor.z = b;
				return this;
			}
			
			public RenderProperties build() {
				return new RenderProperties(this.customTexturePath == null ? null : ResourceLocation.parse(this.customTexturePath), this.customColor, this.isTransparent);
			}
			
			public static RenderProperties.Builder create() {
				return new RenderProperties.Builder();
			}
		}
	}
	
	@FunctionalInterface
	public interface DrawingFunction {
		public static final DrawingFunction NEW_ENTITY = (builder, posX, posY, posZ, normX, normY, normZ, packedLight, r, g, b, a, u, v, overlay) -> {
			builder.addVertex(posX, posY, posZ)
			        .setColor(r, g, b, a)
			        .setUv(u, v)
			        .setOverlay(overlay)
			        .setLight(packedLight)
			        .setNormal(normX, normY, normZ);
		};
		
		public static final DrawingFunction POSITION_TEX = (builder, posX, posY, posZ, normX, normY, normZ, packedLight, r, g, b, a, u, v, overlay) -> {
			builder.addVertex(posX, posY, posZ)
					.setUv(u, v);
		};
		
		public static final DrawingFunction POSITION_TEX_COLOR_NORMAL = (builder, posX, posY, posZ, normX, normY, normZ, packedLight, r, g, b, a, u, v, overlay) -> {
			builder.addVertex(posX, posY, posZ)
					.setUv(u, v)
					.setColor(r, g, b, a)
					.setNormal(normX, normY, normZ);
		};
		
		public static final DrawingFunction POSITION_TEX_COLOR_LIGHTMAP = (builder, posX, posY, posZ, normX, normY, normZ, packedLight, r, g, b, a, u, v, overlay) -> {
			builder.addVertex(posX, posY, posZ)
					.setUv(u, v)
					.setColor(r, g, b, a)
					.setLight(packedLight);
		};
		
		public static final DrawingFunction POSITION_COLOR_LIGHTMAP = (builder, posX, posY, posZ, normX, normY, normZ, packedLight, r, g, b, a, u, v, overlay) -> {
			builder.addVertex(posX, posY, posZ)
					.setColor(r, g, b, a)
					.setLight(packedLight);
		};
		
		public static final DrawingFunction POSITION_COLOR_NORMAL = (builder, posX, posY, posZ, normX, normY, normZ, packedLight, r, g, b, a, u, v, overlay) -> {
			builder.addVertex(posX, posY, posZ)
					.setColor(r, g, b, a)
					.setNormal(normX, normY, normZ);
		};
		
		public static final DrawingFunction POSITION_COLOR_TEX_LIGHTMAP = (builder, posX, posY, posZ, normX, normY, normZ, packedLight, r, g, b, a, u, v, overlay) -> {
			builder.addVertex(posX, posY, posZ)
					.setColor(r, g, b, a)
					.setUv(u, v)
					.setLight(packedLight);
		};
		
		public void draw(VertexConsumer vertexConsumer, float posX, float posY, float posZ, float normX, float normY, float normZ, int packedLight, float r, float g, float b, float a, float u, float v, int overlay);
		
	    default void putBulkData(PoseStack.Pose pose, BakedQuad bakedQuad, VertexConsumer vertexConsumer, float red, float green, float blue, float alpha, int packedLight, int packedOverlay, boolean readExistingColor) {
	    	putBulkDataWithDrawingFunction(this, vertexConsumer, pose, bakedQuad, new float[] { 1.0F, 1.0F, 1.0F, 1.0F }, red, green, blue, alpha, new int[] { packedLight, packedLight, packedLight, packedLight }, packedOverlay, readExistingColor);
	    }
	    
		static void putBulkDataWithDrawingFunction(DrawingFunction drawingFunction, VertexConsumer builder, PoseStack.Pose pPoseEntry, BakedQuad pQuad, float[] pColorMuls, float pRed, float pGreen, float pBlue, float alpha, int[] pCombinedLights, int pCombinedOverlay, boolean pMulColor) {
			float[] afloat = new float[] { pColorMuls[0], pColorMuls[1], pColorMuls[2], pColorMuls[3] };
			int[] aint1 = pQuad.getVertices();
			Vec3i vec3i = pQuad.getDirection().getNormal();
			Matrix4f matrix4f = pPoseEntry.pose();
			Vector3f vector3f = pPoseEntry.normal().transform(new Vector3f((float) vec3i.getX(), (float) vec3i.getY(), (float) vec3i.getZ()));
			int j = aint1.length / 8;

			try (MemoryStack memorystack = MemoryStack.stackPush()) {
				ByteBuffer bytebuffer = memorystack.malloc(DefaultVertexFormat.BLOCK.getVertexSize());
				IntBuffer intbuffer = bytebuffer.asIntBuffer();

				for (int k = 0; k < j; ++k) {
					intbuffer.clear();
					intbuffer.put(aint1, k * 8, 8);
					float f = bytebuffer.getFloat(0);
					float f1 = bytebuffer.getFloat(4);
					float f2 = bytebuffer.getFloat(8);
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
					Vector4f vector4f = matrix4f.transform(new Vector4f(f, f1, f2, 1.0F));
					applyBakedNormals(vector3f, bytebuffer, pPoseEntry.normal());
					float vertexAlpha = pMulColor ? alpha * (float) (bytebuffer.get(15) & 255) / 255.0F : alpha;
					drawingFunction.draw(builder, vector4f.x(), vector4f.y(), vector4f.z(), vector3f.x(), vector3f.y(), vector3f.z(), l, f3, f4, f5, vertexAlpha, f9, f10, pCombinedOverlay);
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
	        if (nx != 0 || ny != 0 || nz != 0)
	        {
	            generated.set(nx / 127f, ny / 127f, nz / 127f);
	            generated.mul(normalTransform);
	        }
	    }
	}
}