package yesman.epicfight.api.client.model.rebake;

import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.annotation.Nullable;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

import it.unimi.dsi.fastutil.floats.FloatArrayList;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import yesman.epicfight.api.client.model.Mesh;
import yesman.epicfight.api.client.model.MeshPartDefinition;
import yesman.epicfight.api.client.model.VertexBuilder;
import yesman.epicfight.api.client.model.rebake.JointNames.Region;

public final class MeshAssembler {
	private MeshAssembler() {
	}

	public static RebakedMesh assemble(RawMesh mesh, SkinWeights weights, String provenance, @Nullable Mesh.RenderProperties renderProperties) {
		int vertexCount = mesh.vertexCount();

		Float[] positions = box(mesh.positionArray());
		Float[] normals = box(mesh.normalArray());
		Float[] uvs = box(mesh.uvArray());

		FloatArrayList weightPool = new FloatArrayList();
		IntArrayList influences = new IntArrayList();
		Integer[] counts = new Integer[vertexCount];

		for (int vertex = 0; vertex < vertexCount; vertex++) {
			int count = weights.counts[vertex];
			counts[vertex] = count;
			int base = vertex * TemplateRig.MAX_INFLUENCES;

			for (int i = 0; i < count; i++) {
				influences.add(weights.jointIds[base + i]);
				weightPool.add(weights.weights[base + i]);
				influences.add(weightPool.size() - 1);
			}
		}

		Map<String, Number[]> arrayMap = Maps.newHashMap();
		arrayMap.put("positions", positions);
		arrayMap.put("normals", normals);
		arrayMap.put("uvs", uvs);
		arrayMap.put("weights", box(weightPool.toFloatArray()));
		arrayMap.put("vcounts", counts);
		arrayMap.put("vindices", box(influences.toIntArray()));

		Map<MeshPartDefinition, List<VertexBuilder>> partBuilders = Maps.newLinkedHashMap();
		Map<String, Region> regions = Maps.newHashMap();
		Map<String, net.minecraft.resources.ResourceLocation> textures = Maps.newLinkedHashMap();
		Map<String, net.minecraft.client.renderer.RenderType> renderTypes = Maps.newLinkedHashMap();
		Set<String> usedNames = Sets.newHashSet();

		for (RawMesh.Group group : mesh.groups()) {
			if (group.isEmpty()) {
				continue;
			}

			String name = uniqueName(group.name, usedNames);
			List<VertexBuilder> builders = Lists.newArrayListWithCapacity(group.triangleCount * 3);

			for (int t = group.firstTriangle; t < group.firstTriangle + group.triangleCount; t++) {
				for (int corner = 0; corner < 3; corner++) {
					int vertex = mesh.triangleVertex(t, corner);
					builders.add(new VertexBuilder(vertex, vertex, vertex));
				}
			}

			partBuilders.put(new RebakedPartDefinition(name, null, group.liveTransform), builders);
			regions.put(name, JointNames.regionOf(group.name));

			if (group.texture != null) {
				textures.put(name, group.texture);
			}

			if (group.renderType != null) {
				renderTypes.put(name, group.renderType);
			}
		}

		RebakedMesh result = new RebakedMesh(arrayMap, partBuilders, null, renderProperties);

		if (mesh.anyTinted()) {
			result.setVertexTints(mesh.colourArray());
		}
		regions.forEach(result::putRegion);
		textures.forEach(result::putTexture);
		renderTypes.forEach(result::putRenderType);
		result.setProvenance(provenance);

		return result;
	}

	private static String uniqueName(String base, Set<String> used) {
		if (used.add(base)) {
			return base;
		}

		for (int suffix = 2; ; suffix++) {
			String candidate = base + "#" + suffix;

			if (used.add(candidate)) {
				return candidate;
			}
		}
	}

	private static Float[] box(float[] values) {
		Float[] boxed = new Float[values.length];

		for (int i = 0; i < values.length; i++) {
			boxed[i] = values[i];
		}

		return boxed;
	}

	private static Integer[] box(int[] values) {
		Integer[] boxed = new Integer[values.length];

		for (int i = 0; i < values.length; i++) {
			boxed[i] = values[i];
		}

		return boxed;
	}
}
