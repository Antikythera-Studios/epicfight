package yesman.epicfight.api.client.model;
import yesman.epicfight.EpicFight;

import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

import javax.annotation.Nullable;

import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.joml.Vector4f;

import com.google.common.collect.Maps;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;

import yesman.epicfight.api.client.model.ClassicMesh.ClassicMeshPart;
import yesman.epicfight.api.model.Armature;
import yesman.epicfight.api.utils.math.OpenMatrix4f;
import yesman.epicfight.main.EpicFightMod;

public class ClassicMesh extends StaticMesh<ClassicMeshPart> {
	public ClassicMesh(Map<String, Number[]> arrayMap, Map<MeshPartDefinition, List<VertexBuilder>> partBuilders, ClassicMesh parent, RenderProperties properties) {
		super(arrayMap, partBuilders, parent, properties);
	}
	
	@Override
	protected Map<String, ClassicMeshPart> createModelPart(Map<MeshPartDefinition, List<VertexBuilder>> partBuilders) {
		Map<String, ClassicMeshPart> parts = Maps.newHashMap();
		
		partBuilders.forEach((partDefinition, vertexBuilder) -> {
			parts.put(partDefinition.partName(), new ClassicMeshPart(vertexBuilder, partDefinition.renderProperties(), partDefinition.getModelPartAnimationProvider()));
		});
		
		return parts;
	}
	
	@Override
	protected ClassicMeshPart getOrLogException(Map<String, ClassicMeshPart> parts, String name) {
		if (!parts.containsKey(name)) {
			EpicFight.LOGGER.debug("Can not find the mesh part named " + name + " in " + this.getClass().getCanonicalName());
			return null;
		}
		
		return parts.get(name);
	}
	
	@Override
	public void draw(PoseStack poseStack, VertexConsumer vertexConsumer, Mesh.DrawingFunction drawingFunction, int packedLight, float r, float g, float b, float a, int overlay) {
		for (ClassicMeshPart part : this.parts.values()) {
			part.draw(poseStack, vertexConsumer, drawingFunction, packedLight, r, g, b, a, overlay);
		}
	}
	
	@Override
	public void drawPosed(PoseStack poseStack, VertexConsumer vertexConsumer, Mesh.DrawingFunction drawingFunction, int packedLight, float r, float g, float b, float a, int overlay, Armature armature, OpenMatrix4f[] poses) {
		this.draw(poseStack, vertexConsumer, drawingFunction, packedLight, r, g, b, a, overlay);
	}
	
	public class ClassicMeshPart extends MeshPart {
		public ClassicMeshPart(List<VertexBuilder> verticies, @Nullable Mesh.RenderProperties renderProperties, @Nullable Supplier<OpenMatrix4f> vanillaPartTracer) {
			super(verticies, renderProperties, vanillaPartTracer);
		}
		
		protected static final Vector4f POSITION = new Vector4f();
		protected static final Vector3f NORMAL = new Vector3f();
		
		@Override
		public void draw(PoseStack poseStack, VertexConsumer bufferbuilder, Mesh.DrawingFunction drawingFunction, int packedLight, float r, float g, float b, float a, int overlay) {
			if (this.isHidden()) {
				return;
			}
			
			Vector4f color = this.getColor(r, g, b, a);
			poseStack.pushPose();
			OpenMatrix4f transform = this.getVanillaPartTransform();
			
			if (transform != null) {
				poseStack.mulPose(OpenMatrix4f.exportToMojangMatrix(transform));
			}
			
			Matrix4f matrix4f = poseStack.last().pose();
			Matrix3f matrix3f = poseStack.last().normal();
			
			for (VertexBuilder vi : this.getVertices()) {
				getVertexPosition(vi.position, POSITION);
				getVertexNormal(vi.normal, NORMAL);
				POSITION.mul(matrix4f);
				NORMAL.mul(matrix3f);
				
				drawingFunction.draw(bufferbuilder, POSITION.x(), POSITION.y(), POSITION.z(), NORMAL.x(), NORMAL.y(), NORMAL.z(), packedLight, color.x, color.y, color.z, color.w, uvs[vi.uv * 2], uvs[vi.uv * 2 + 1], overlay);
			}
			
			poseStack.popPose();
		}
	}
}