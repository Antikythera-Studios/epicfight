package yesman.epicfight.api.client.model;
import yesman.epicfight.EpicFight;

import com.google.common.collect.Maps;
import com.google.gson.JsonObject;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntList;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.joml.Vector4f;
import yesman.epicfight.api.asset.JsonAssetLoader;
import yesman.epicfight.api.client.model.SkinnedMesh.SkinnedMeshPart;
import yesman.epicfight.api.model.Armature;
import yesman.epicfight.api.utils.ParseUtil;
import yesman.epicfight.api.utils.math.OpenMatrix4f;
import yesman.epicfight.api.utils.math.Vec4f;
import yesman.epicfight.client.renderer.EpicFightRenderTypes;
import yesman.epicfight.client.renderer.shader.compute.ComputeShaderSetup;
import yesman.epicfight.client.renderer.shader.compute.loader.ComputeShaderProvider;
import yesman.epicfight.config.ClientConfig;
import yesman.epicfight.main.EpicFightMod;
import yesman.epicfight.main.EpicFightSharedConstants;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

public class SkinnedMesh extends StaticMesh<SkinnedMeshPart> {
	protected final float[] weights;
	protected final int[] affectingJointCounts;
	protected final int[][] affectingWeightIndices;
	protected final int[][] affectingJointIndices;
	
	private final int maxJointCount;
	
	@Nullable
	private ComputeShaderSetup computerShaderSetup;
	
	public SkinnedMesh(@Nullable Map<String, Number[]> arrayMap, @Nullable Map<MeshPartDefinition, List<VertexBuilder>> partBuilders, @Nullable SkinnedMesh parent, RenderProperties properties) {
		super(arrayMap, partBuilders, parent, properties);
		
		this.weights = parent == null ? ParseUtil.unwrapFloatWrapperArray(arrayMap.get("weights")) : parent.weights;
		this.affectingJointCounts = parent == null ? ParseUtil.unwrapIntWrapperArray(arrayMap.get("vcounts")) : parent.affectingJointCounts;
		
		if (parent != null) {
			this.affectingJointIndices = parent.affectingJointIndices;
			this.affectingWeightIndices = parent.affectingWeightIndices;
		} else {
			int[] vindices = ParseUtil.unwrapIntWrapperArray(arrayMap.get("vindices"));
			this.affectingJointIndices = new int[this.affectingJointCounts.length][];
			this.affectingWeightIndices = new int[this.affectingJointCounts.length][];
			int idx = 0;
			
			for (int i = 0; i < this.affectingJointCounts.length; i++) {
				int count = this.affectingJointCounts[i];
				int[] jointId = new int[count];
				int[] weights = new int[count];
				
				for (int j = 0; j < count; j++) {
					jointId[j] = vindices[idx * 2];
					weights[j] = vindices[idx * 2 + 1];
					idx++;
				}
				
				this.affectingJointIndices[i] = jointId;
				this.affectingWeightIndices[i] = weights;
			}
		}
		
		int maxJointId = 0;
		
		for (int[] i : this.affectingJointIndices) {
			for (int j : i) {
				if (maxJointId < j) {
					maxJointId = j;
				}
			}
		}
		
		this.maxJointCount = maxJointId;

		if (ComputeShaderProvider.supportComputeShader()) {
			if (RenderSystem.isOnRenderThread()) {
				this.computerShaderSetup = ComputeShaderProvider.getComputeShaderSetup(this);
			} else {
				RenderSystem.recordRenderCall(() -> {
					this.computerShaderSetup = ComputeShaderProvider.getComputeShaderSetup(this);
				});
			}
		}
	}
	
	public void destroy() {
		if (RenderSystem.isOnRenderThread()) {
			if (this.computerShaderSetup != null) {
				this.computerShaderSetup.destroyBuffers();
			}
		} else {
			RenderSystem.recordRenderCall(() -> {
				if (this.computerShaderSetup != null) {
					this.computerShaderSetup.destroyBuffers();
				}
			});
		}
	}
	
	@Override
	protected Map<String, SkinnedMeshPart> createModelPart(Map<MeshPartDefinition, List<VertexBuilder>> partBuilders) {
		Map<String, SkinnedMeshPart> parts = Maps.newHashMap();
		
		partBuilders.forEach((partDefinition, vertexBuilder) -> {
			parts.put(partDefinition.partName(), new SkinnedMeshPart(vertexBuilder, partDefinition.renderProperties(), partDefinition.getModelPartAnimationProvider()));
		});
		
		return parts;
	}
	
	@Override
	protected SkinnedMeshPart getOrLogException(Map<String, SkinnedMeshPart> parts, String name) {
		if (!parts.containsKey(name)) {
			if (EpicFightSharedConstants.IS_DEV_ENV) {
				EpicFight.LOGGER.debug("Cannot find the mesh part named " + name + " in " + this.getClass().getCanonicalName());
			}
			return null;
		}
		return parts.get(name);
	}
	
	private static final Vec4f TRANSFORM = new Vec4f();
	private static final Vec4f POS = new Vec4f();
	private static final Vec4f TOTAL_POS = new Vec4f();
	
	@Override
	public void getVertexPosition(int positionIndex, Vector4f dest, @Nullable OpenMatrix4f[] poses) {
		int index = positionIndex * 3;
		
		POS.set(this.positions[index], this.positions[index + 1], this.positions[index + 2], 1.0F);
		TOTAL_POS.set(0.0F, 0.0F, 0.0F, 0.0F);
		
		for (int i = 0; i < this.affectingJointCounts[positionIndex]; i++) {
			int jointIndex = this.affectingJointIndices[positionIndex][i];
			int weightIndex = this.affectingWeightIndices[positionIndex][i];
			float weight = this.weights[weightIndex];
			
			Vec4f.add(OpenMatrix4f.transform(poses[jointIndex], POS, TRANSFORM).scale(weight), TOTAL_POS, TOTAL_POS);
		}
		
		dest.set(TOTAL_POS.x, TOTAL_POS.y, TOTAL_POS.z, 1.0F);
	}
	
	private static final Vec4f NORM = new Vec4f();
	private static final Vec4f TOTAL_NORM = new Vec4f();
	
	@Override
	public void getVertexNormal(int positionIndex, int normalIndex, Vector3f dest, @Nullable OpenMatrix4f[] poses) {
		int index = normalIndex * 3;
		NORM.set(this.normals[index], this.normals[index + 1], this.normals[index + 2], 1.0F);
		TOTAL_NORM.set(0.0F, 0.0F, 0.0F, 0.0F);
		
		for (int i = 0; i < this.affectingJointCounts[positionIndex]; i++) {
			int jointIndex = this.affectingJointIndices[positionIndex][i];
			int weightIndex = this.affectingWeightIndices[positionIndex][i];
			float weight = this.weights[weightIndex];
			Vec4f.add(OpenMatrix4f.transform(poses[jointIndex], NORM, TRANSFORM).scale(weight), TOTAL_NORM, TOTAL_NORM);
		}
		
		dest.set(TOTAL_NORM.x, TOTAL_NORM.y, TOTAL_NORM.z);
	}
	
	/**
	 * Draws the model without applying animation
	 */
	@Override
	public void draw(PoseStack poseStack, VertexConsumer bufferbuilder, Mesh.DrawingFunction drawingFunction, int packedLight, float r, float g, float b, float a, int overlay) {
		for (SkinnedMeshPart part : this.parts.values()) {
			part.draw(poseStack, bufferbuilder, drawingFunction, packedLight, r, g, b, a, overlay);
		}
	}

	protected static final Vector4f POSITION = new Vector4f();
	protected static final Vector3f NORMAL = new Vector3f();
	
	/**
	 * Draws the model to vanilla buffer
	 */
	@Override
	public void drawPosed(PoseStack poseStack, VertexConsumer bufferbuilder, Mesh.DrawingFunction drawingFunction, int packedLight, float r, float g, float b, float a, int overlay, @Nullable Armature armature, OpenMatrix4f[] poses) {
		Matrix4f pose = poseStack.last().pose();
		Matrix3f normal = poseStack.last().normal();
		
		for (SkinnedMeshPart part : this.parts.values()) {
			if (!part.isHidden()) {
				OpenMatrix4f transform = part.getVanillaPartTransform();
				
				for (int i = 0; i < poses.length; i++) {
					ComputeShaderSetup.TOTAL_POSES[i].load(poses[i]);
					
					if (armature != null) {
						ComputeShaderSetup.TOTAL_POSES[i].mulBack(armature.searchJointById(i).getToOrigin());
					}
					
					if (transform != null) {
						ComputeShaderSetup.TOTAL_POSES[i].mulBack(transform);
					}
					
					ComputeShaderSetup.TOTAL_NORMALS[i] = ComputeShaderSetup.TOTAL_POSES[i].removeTranslation();
				}
				
				for (VertexBuilder vi : part.getVertices()) {
					this.getVertexPosition(vi.position, POSITION, ComputeShaderSetup.TOTAL_POSES);
					this.getVertexNormal(vi.position, vi.normal, NORMAL, ComputeShaderSetup.TOTAL_NORMALS);
					
					POSITION.mul(pose);
					NORMAL.mul(normal);
					
					drawingFunction.draw(bufferbuilder, POSITION.x, POSITION.y, POSITION.z, NORMAL.x, NORMAL.y, NORMAL.z, packedLight, r, g, b, a, this.uvs[vi.uv * 2], this.uvs[vi.uv * 2 + 1], overlay);
				}
			}
		}
	}
	
	/**
	 * Draws the model depending on animation shader option
	 * @param armature give this parameter as null if @param poses already bound origin translation
	 * @param poses
	 */
	public void draw(PoseStack poseStack, MultiBufferSource bufferSources, RenderType renderType, int packedLight, float r, float g, float b, float a, int overlay, @Nullable Armature armature, OpenMatrix4f[] poses) {
		this.draw(poseStack, bufferSources, renderType, Mesh.DrawingFunction.NEW_ENTITY, packedLight, r, g, b, a, overlay, armature, poses);
	}

	@Override
	public void draw(PoseStack poseStack, MultiBufferSource bufferSources, RenderType renderType, Mesh.DrawingFunction drawingFunction, int packedLight, float r, float g, float b, float a, int overlay, @Nullable Armature armature, OpenMatrix4f[] poses) {
        if (ClientConfig.activateComputeShader && this.computerShaderSetup != null) {
			this.computerShaderSetup.drawWithShader(this, poseStack, bufferSources, EpicFightRenderTypes.getTriangulated(renderType), packedLight, r, g, b, a, overlay, armature, poses);
		} else {
			this.drawPosed(poseStack, bufferSources.getBuffer(EpicFightRenderTypes.getTriangulated(renderType)), drawingFunction, packedLight, r, g, b, a, overlay, armature, poses);
		}
	}
	
	public int getMaxJointCount() {
		return this.maxJointCount;
	}
	
	public float[] weights() {
		return this.weights;
	}
	
	public int[] affectingJointCounts() {
		return this.affectingJointCounts;
	}
	
	public int[][] affectingWeightIndices() {
		return this.affectingWeightIndices;
	}
	
	public int[][] affectingJointIndices() {
		return this.affectingJointIndices;
	}
	
	public class SkinnedMeshPart extends MeshPart {
		private ComputeShaderSetup.MeshPartBuffer partVBO;

		public SkinnedMeshPart(List<VertexBuilder> animatedMeshPartList, @Nullable Mesh.RenderProperties renderProperties, @Nullable Supplier<OpenMatrix4f> vanillaPartTracer) {
			super(animatedMeshPartList, renderProperties, vanillaPartTracer);
		}
		
		public void initVBO(ComputeShaderSetup.MeshPartBuffer partVBO) {
			this.partVBO = partVBO;
		}
		
		public ComputeShaderSetup.MeshPartBuffer getPartVBO() {
			return this.partVBO;
		}
		
		@Override
		public void draw(PoseStack poseStack, VertexConsumer bufferBuilder, Mesh.DrawingFunction drawingFunction, int packedLight, float r, float g, float b, float a, int overlay) {
			if (this.isHidden()) {
				return;
			}
			
			Vector4f color = this.getColor(r, g, b, a);
			Matrix4f pose = poseStack.last().pose();
			Matrix3f normal = poseStack.last().normal();
			
			for (VertexBuilder vi : this.getVertices()) {
				getVertexPosition(vi.position, POSITION);
				getVertexNormal(vi.normal, NORMAL);
				POSITION.mul(pose);
				NORMAL.mul(normal);
				drawingFunction.draw(bufferBuilder, POSITION.x(), POSITION.y(), POSITION.z(), NORMAL.x(), NORMAL.y(), NORMAL.z(), packedLight, color.x, color.y, color.z, color.w, uvs[vi.uv * 2], uvs[vi.uv * 2 + 1], overlay);
			}
		}
	}
	
	/**
	 * Export this model as Json format
	 */
	public JsonObject toJsonObject() {
		JsonObject root = new JsonObject();
		JsonObject vertices = new JsonObject();
		float[] positions = this.positions.clone();
		float[] normals = this.normals.clone();
		
		for (int i = 0; i < positions.length / 3; i++) {
			int k = i * 3;
			Vec4f posVector = new Vec4f(positions[k], positions[k+1], positions[k+2], 1.0F);
			posVector.transform(JsonAssetLoader.MINECRAFT_TO_BLENDER_COORD);
			positions[k] = posVector.x;
			positions[k+1] = posVector.y;
			positions[k+2] = posVector.z;
		}
		
		for (int i = 0; i < normals.length / 3; i++) {
			int k = i * 3;
			Vec4f normVector = new Vec4f(normals[k], normals[k+1], normals[k+2], 1.0F);
			normVector.transform(JsonAssetLoader.MINECRAFT_TO_BLENDER_COORD);
			normals[k] = normVector.x;
			normals[k+1] = normVector.y;
			normals[k+2] = normVector.z;
		}
		
		IntList affectingJointAndWeightIndices = new IntArrayList();
		
		for (int i = 0; i < this.affectingJointCounts.length; i++) {
			for (int j = 0; j < this.affectingJointCounts[j]; j++) {
				affectingJointAndWeightIndices.add(this.affectingJointIndices[i][j]);
				affectingJointAndWeightIndices.add(this.affectingWeightIndices[i][j]);
			}
		}
		
		vertices.add("positions", ParseUtil.farrayToJsonObject(positions, 3));
		vertices.add("uvs", ParseUtil.farrayToJsonObject(this.uvs, 2));
		vertices.add("normals", ParseUtil.farrayToJsonObject(normals, 3));
		vertices.add("vcounts", ParseUtil.iarrayToJsonObject(this.affectingJointCounts, 1));
		vertices.add("weights", ParseUtil.farrayToJsonObject(this.weights, 1));
		vertices.add("vindices", ParseUtil.iarrayToJsonObject(affectingJointAndWeightIndices.toIntArray(), 1));
		
		if (!this.parts.isEmpty()) {
			JsonObject parts = new JsonObject();
			
			for (Map.Entry<String, SkinnedMeshPart> partEntry : this.parts.entrySet()) {
				IntList indicesArray = new IntArrayList();
				
				for (VertexBuilder vertexIndicator : partEntry.getValue().getVertices()) {
					indicesArray.add(vertexIndicator.position);
					indicesArray.add(vertexIndicator.uv);
					indicesArray.add(vertexIndicator.normal);
				}
				
				parts.add(partEntry.getKey(), ParseUtil.iarrayToJsonObject(indicesArray.toIntArray(), 3));
			}
			
			vertices.add("parts", parts);
		} else {
			int i = 0;
			int[] indices = new int[this.vertexCount * 3];
			
			for (SkinnedMeshPart part : this.parts.values()) {
				for (VertexBuilder vertexIndicator : part.getVertices()) {
					indices[i * 3] = vertexIndicator.position;
					indices[i * 3 + 1] = vertexIndicator.uv;
					indices[i * 3 + 2] = vertexIndicator.normal;
					i++;
				}
			}
			
			vertices.add("indices", ParseUtil.iarrayToJsonObject(indices, 3));
		}
		
		root.add("vertices", vertices);
		
		if (this.renderProperties != null) {
			JsonObject renderProperties = new JsonObject();
			renderProperties.addProperty("texture_path", this.renderProperties.customTexturePath().toString());
			renderProperties.addProperty("transparent", this.renderProperties.isTransparent());
			root.add("render_properties", renderProperties);
		}
		
		return root;
	}
}
