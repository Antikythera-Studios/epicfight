package yesman.epicfight.api.client.model.rebake;

import java.util.List;
import java.util.Map;

import javax.annotation.Nullable;

import com.google.common.collect.Maps;

import yesman.epicfight.api.client.model.MeshPart;
import yesman.epicfight.api.client.model.MeshPartDefinition;
import yesman.epicfight.api.client.model.SkinnedMesh;
import yesman.epicfight.api.client.model.VertexBuilder;
import yesman.epicfight.api.client.model.rebake.JointNames.Region;

public class RebakedMesh extends SkinnedMesh {
	private final Map<String, Region> regions = Maps.newHashMap();
	private final Map<String, net.minecraft.resources.ResourceLocation> textures = Maps.newLinkedHashMap();
	private final Map<String, net.minecraft.client.renderer.RenderType> renderTypes = Maps.newLinkedHashMap();

	@Nullable
	private SurfaceBinding binding;
	private final java.util.List<LiveSource> liveSources = com.google.common.collect.Lists.newArrayList();
	private String provenance = "unknown";

	@Nullable
	private float[] tints;
	private boolean coloursObserved;
	private java.util.List<net.minecraft.client.renderer.RenderType> extraPasses = com.google.common.collect.Lists.newArrayList();

	public RebakedMesh(Map<String, Number[]> arrayMap, Map<MeshPartDefinition, List<VertexBuilder>> partBuilders, @Nullable SkinnedMesh parent, RenderProperties properties) {
		super(arrayMap, partBuilders, parent, properties);
	}

	void setVertexTints(float[] tints) {
		this.tints = tints;
	}

	public boolean hasVertexTints() {
		return this.tints != null;
	}

	public boolean coloursObserved() {
		return this.coloursObserved;
	}

	void setColoursObserved(boolean observed) {
		this.coloursObserved = observed;
	}

	void setBinding(@Nullable SurfaceBinding binding) {
		this.binding = binding;
	}

	@Nullable
	public SurfaceBinding binding() {
		return this.binding;
	}

	@Override
	public void drawPosed(com.mojang.blaze3d.vertex.PoseStack poseStack, com.mojang.blaze3d.vertex.VertexConsumer buffer,
			yesman.epicfight.api.client.model.Mesh.DrawingFunction drawingFunction, int packedLight,
			float red, float green, float blue, float alpha, int overlay,
			@Nullable yesman.epicfight.api.model.Armature armature, yesman.epicfight.api.utils.math.OpenMatrix4f[] poses) {
		float[] corners = this.binding == null ? null : BodyDeformation.deformedCorners();

		if (corners == null && this.tints == null) {
			super.drawPosed(poseStack, buffer, drawingFunction, packedLight, red, green, blue, alpha, overlay, armature, poses);

			return;
		}

		org.joml.Matrix4f pose = poseStack.last().pose();
		org.joml.Matrix3f normalMatrix = poseStack.last().normal();
		float[] point = new float[3];
		float[] normal = new float[3];
		org.joml.Vector4f position = new org.joml.Vector4f();
		org.joml.Vector3f direction = new org.joml.Vector3f();

		boolean posesReady = false;

		for (Map.Entry<String, ? extends MeshPart> entry : this.getPartEntry()) {
			MeshPart part = entry.getValue();

			if (part.isHidden()) {
				continue;
			}

			for (yesman.epicfight.api.client.model.VertexBuilder vertex : part.getVertices()) {
				if (corners != null && this.binding != null && this.binding.evaluate(vertex.position, corners, point, normal)) {
					position.set(point[0], point[1], point[2], 1.0F);
					direction.set(normal[0], normal[1], normal[2]);
				} else {
					if (!posesReady) {
						prepareJointMatrices(armature, poses, part);
						posesReady = true;
					}

					this.getVertexPosition(vertex.position, position, yesman.epicfight.client.renderer.shader.compute.ComputeShaderSetup.TOTAL_POSES);
					this.getVertexNormal(vertex.position, vertex.normal, direction, yesman.epicfight.client.renderer.shader.compute.ComputeShaderSetup.TOTAL_NORMALS);
				}

				position.mul(pose);
				direction.mul(normalMatrix);

				float vr = red;
				float vg = green;
				float vb = blue;

				float va = alpha;
				int vertexLight = packedLight;

				if (this.tints != null && vertex.position * 5 + 4 < this.tints.length) {
					vr *= this.tints[vertex.position * 5];
					vg *= this.tints[vertex.position * 5 + 1];
					vb *= this.tints[vertex.position * 5 + 2];

					va *= this.tints[vertex.position * 5 + 3];

					if (this.tints[vertex.position * 5 + 4] > 0.5F) {
						vertexLight = net.minecraft.client.renderer.LightTexture.FULL_BRIGHT;
					}
				}

				drawingFunction.draw(buffer, position.x(), position.y(), position.z(), direction.x(), direction.y(), direction.z(),
					vertexLight, vr, vg, vb, va, this.uvs[vertex.uv * 2], this.uvs[vertex.uv * 2 + 1], overlay);
			}
		}
	}

	private static void prepareJointMatrices(@Nullable yesman.epicfight.api.model.Armature armature, yesman.epicfight.api.utils.math.OpenMatrix4f[] poses, MeshPart part) {
		yesman.epicfight.api.utils.math.OpenMatrix4f transform = part.getVanillaPartTransform();

		for (int i = 0; i < poses.length; i++) {
			yesman.epicfight.client.renderer.shader.compute.ComputeShaderSetup.TOTAL_POSES[i].load(poses[i]);

			if (armature != null) {
				yesman.epicfight.client.renderer.shader.compute.ComputeShaderSetup.TOTAL_POSES[i].mulBack(armature.searchJointById(i).getToOrigin());
			}

			if (transform != null) {
				yesman.epicfight.client.renderer.shader.compute.ComputeShaderSetup.TOTAL_POSES[i].mulBack(transform);
			}

			yesman.epicfight.client.renderer.shader.compute.ComputeShaderSetup.TOTAL_NORMALS[i] = yesman.epicfight.client.renderer.shader.compute.ComputeShaderSetup.TOTAL_POSES[i].removeTranslation();
		}
	}

	@Override
	public void draw(com.mojang.blaze3d.vertex.PoseStack poseStack, net.minecraft.client.renderer.MultiBufferSource bufferSources,
			net.minecraft.client.renderer.RenderType renderType, yesman.epicfight.api.client.model.Mesh.DrawingFunction drawingFunction,
			int packedLight, float red, float green, float blue, float alpha, int overlay,
			@Nullable yesman.epicfight.api.model.Armature armature, yesman.epicfight.api.utils.math.OpenMatrix4f[] poses) {
		if (this.tints != null || (this.binding != null && BodyDeformation.deformedCorners() != null)) {
			this.drawPosed(poseStack, bufferSources.getBuffer(yesman.epicfight.client.renderer.EpicFightRenderTypes.getTriangulated(renderType)),
				drawingFunction, packedLight, red, green, blue, alpha, overlay, armature, poses);

			return;
		}

		super.draw(poseStack, bufferSources, renderType, drawingFunction, packedLight, red, green, blue, alpha, overlay, armature, poses);
	}

	void addLiveSources(java.util.List<LiveSource> sources) {
		this.liveSources.addAll(sources);
	}

	public void bindAnimationSource(net.minecraft.client.model.HumanoidModel<?> model) {
		for (LiveSource source : this.liveSources) {
			source.bindFrom(model);
		}
	}

	void putTexture(String partName, net.minecraft.resources.ResourceLocation texture) {
		this.textures.put(partName, texture);
	}

	void putRenderType(String partName, net.minecraft.client.renderer.RenderType renderType) {
		this.renderTypes.put(partName, renderType);
	}

	public java.util.List<net.minecraft.client.renderer.RenderType> capturedPasses() {
		java.util.List<net.minecraft.client.renderer.RenderType> distinct = com.google.common.collect.Lists.newArrayList();

		for (net.minecraft.client.renderer.RenderType type : this.renderTypes.values()) {
			if (!distinct.contains(type)) {
				distinct.add(type);
			}
		}

		return distinct;
	}

	public boolean hasCapturedPasses() {
		return !this.renderTypes.isEmpty();
	}

	public java.util.List<net.minecraft.client.renderer.RenderType> extraPasses() {
		return this.extraPasses;
	}

	void setExtraPasses(java.util.List<net.minecraft.client.renderer.RenderType> passes) {
		this.extraPasses = passes;
	}

	public boolean hasUnrecordedParts() {
		for (Map.Entry<String, ? extends MeshPart> entry : this.getPartEntry()) {
			if (this.renderTypes.get(entry.getKey()) == null) {
				return true;
			}
		}

		return false;
	}

	public boolean[] restrictToUnrecorded() {
		java.util.List<Boolean> saved = com.google.common.collect.Lists.newArrayList();

		for (Map.Entry<String, ? extends MeshPart> entry : this.getPartEntry()) {
			MeshPart part = entry.getValue();
			saved.add(part.isHidden());

			if (this.renderTypes.get(entry.getKey()) != null) {
				part.setHidden(true);
			}
		}

		boolean[] flags = new boolean[saved.size()];

		for (int i = 0; i < flags.length; i++) {
			flags[i] = saved.get(i);
		}

		return flags;
	}

	public boolean[] restrictToPass(net.minecraft.client.renderer.RenderType pass) {
		java.util.List<Boolean> saved = com.google.common.collect.Lists.newArrayList();

		for (Map.Entry<String, ? extends MeshPart> entry : this.getPartEntry()) {
			MeshPart part = entry.getValue();
			saved.add(part.isHidden());

			if (!pass.equals(this.renderTypes.get(entry.getKey()))) {
				part.setHidden(true);
			}
		}

		boolean[] flags = new boolean[saved.size()];

		for (int i = 0; i < flags.length; i++) {
			flags[i] = saved.get(i);
		}

		return flags;
	}

	public boolean hasCapturedTextures() {
		return !this.textures.isEmpty();
	}

	public java.util.List<net.minecraft.resources.ResourceLocation> capturedTextures() {
		java.util.List<net.minecraft.resources.ResourceLocation> distinct = com.google.common.collect.Lists.newArrayList();

		for (net.minecraft.resources.ResourceLocation texture : this.textures.values()) {
			if (!distinct.contains(texture)) {
				distinct.add(texture);
			}
		}

		return distinct;
	}

	public boolean[] restrictToTexture(net.minecraft.resources.ResourceLocation texture) {
		java.util.List<Boolean> saved = com.google.common.collect.Lists.newArrayList();

		for (Map.Entry<String, ? extends MeshPart> entry : this.getPartEntry()) {
			MeshPart part = entry.getValue();
			saved.add(part.isHidden());

			if (!texture.equals(this.textures.get(entry.getKey()))) {
				part.setHidden(true);
			}
		}

		boolean[] flags = new boolean[saved.size()];

		for (int i = 0; i < flags.length; i++) {
			flags[i] = saved.get(i);
		}

		return flags;
	}

	public void restoreVisibility(boolean[] saved) {
		int i = 0;

		for (Map.Entry<String, ? extends MeshPart> entry : this.getPartEntry()) {
			if (i >= saved.length) {
				return;
			}

			entry.getValue().setHidden(saved[i++]);
		}
	}

	void putRegion(String partName, Region region) {
		this.regions.put(partName, region);
	}

	void setProvenance(String provenance) {
		this.provenance = provenance;
	}

	public String provenance() {
		return this.provenance;
	}

	public Region regionOf(String partName) {
		return this.regions.getOrDefault(partName, Region.UNKNOWN);
	}

	public void showOnlyRegions(Region... visible) {
		for (Map.Entry<String, ? extends MeshPart> entry : this.getPartEntry()) {
			boolean show = false;
			Region region = this.regionOf(entry.getKey());

			for (Region candidate : visible) {
				if (region == candidate) {
					show = true;

					break;
				}
			}

			entry.getValue().setHidden(!show);
		}
	}
}
