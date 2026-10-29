package yesman.epicfight.api.client.model;

import java.util.List;
import java.util.function.Supplier;

import javax.annotation.Nullable;

import org.joml.Vector4f;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;

import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import yesman.epicfight.api.utils.math.OpenMatrix4f;
import yesman.epicfight.client.renderer.EpicFightRenderTypes;

public abstract class MeshPart {
	protected final List<VertexBuilder> verticies;
	protected final Mesh.RenderProperties renderProperties;
	protected final Supplier<OpenMatrix4f> vanillaPartTracer;
	protected boolean isHidden;
	
	public MeshPart(List<VertexBuilder> vertices, @Nullable Mesh.RenderProperties renderProperties, @Nullable Supplier<OpenMatrix4f> vanillaPartTracer) {
		this.verticies = vertices;
		this.renderProperties = renderProperties;
		this.vanillaPartTracer = vanillaPartTracer;
	}
	
	public abstract void draw(PoseStack poseStack, VertexConsumer bufferbuilder, Mesh.DrawingFunction drawingFunction, int packedLight, float r, float g, float b, float a, int overlay);
	
	public void setHidden(boolean hidden) {
		this.isHidden = hidden;
	}
	
	public boolean isHidden() {
		return this.isHidden;
	}
	
	public List<VertexBuilder> getVertices() {
		return this.verticies;
	}
	
	public OpenMatrix4f getVanillaPartTransform() {
		if (this.vanillaPartTracer == null) {
			return null;
		}
		
		return this.vanillaPartTracer.get();
	}
	
	public VertexConsumer getBufferBuilder(RenderType renderType, MultiBufferSource bufferSource) {
		if (this.renderProperties.customTexturePath() != null) {
			return bufferSource.getBuffer(EpicFightRenderTypes.replaceTexture(this.renderProperties.customTexturePath(), renderType));
		}
		
		return bufferSource.getBuffer(renderType);
	}
	
	protected static final Vector4f COLOR = new Vector4f();
	
	public Vector4f getColor(float r, float g, float b, float a) {
		if (this.renderProperties != null && this.renderProperties.customColor() != null) {
			COLOR.set(
				  this.renderProperties.customColor().x
				, this.renderProperties.customColor().y
				, this.renderProperties.customColor().z
				, a
			);
			
			return COLOR;
		} else {
			COLOR.set(
				  r
				, g
				, b
				, a
			);
			
			return COLOR;
		}
	}
}