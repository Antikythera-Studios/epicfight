package yesman.epicfight.api.client.model.rebake;

import java.util.Map;
import java.util.Set;

import javax.annotation.Nullable;

import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

import net.minecraft.client.model.HumanoidModel;
import yesman.epicfight.api.client.model.SkinnedMesh;
import yesman.epicfight.gameasset.Armatures;
import yesman.epicfight.main.EpicFightMod;

public final class ModelRebaker {
	private ModelRebaker() {
	}

	private static final Map<String, RebakedMesh> CACHE = Maps.newHashMap();
	private static final Set<String> FAILED = Sets.newHashSet();

	public static void clear() {
		CACHE.values().forEach(SkinnedMesh::destroy);
		CACHE.clear();
		FAILED.clear();
	}

	@Nullable
	public static RebakedMesh bakeHumanoid(String cacheKey, HumanoidModel<?> model) {
		RebakedMesh cached = CACHE.get(cacheKey);

		if (cached != null) {
			return cached;
		}

		if (FAILED.contains(cacheKey)) {
			return null;
		}

		try {
			RebakedMesh baked = bakeUncached(model);

			if (baked == null) {
				FAILED.add(cacheKey);

				return null;
			}

			CACHE.put(cacheKey, baked);

			return baked;
		} catch (Throwable t) {
			FAILED.add(cacheKey);

			return null;
		}
	}

	@Nullable
	private static RebakedMesh bakeUncached(HumanoidModel<?> model) {
		TemplateRig rig = TemplateRig.get(Armatures.BIPED.get().getJointNumber());

		if (rig == null) {
			return null;
		}

		java.util.List<LiveSource> liveSources = com.google.common.collect.Lists.newArrayList();
		RawMesh mesh = MeshCapture.captureHumanoid(model, liveSources);

		if (mesh == null || mesh.isEmpty()) {
			return null;
		}

		mesh.repairNormals();

		PartSubstitution.Result substituted = PartSubstitution.substitute(mesh, rig, Armatures.BIPED.get().getJointNumber(), null);

		if (substituted != null) {
			RebakedMesh built = MeshAssembler.assemble(substituted.mesh(), substituted.weights(), "body", null);
			built.addLiveSources(liveSources);
			PartSubstitution.logSubstitution("part tree", substituted);

			return built;
		}

		WeightSolver.Report report = new WeightSolver.Report();
		WeightSolver.Refined refined = WeightSolver.solveRefined(mesh, rig, report);

		if (refined == null) {
			return null;
		}


		RebakedMesh assembled = MeshAssembler.assemble(refined.mesh(), refined.weights(), "part tree", null);
		assembled.addLiveSources(liveSources);
		SurfaceBinder.bind(assembled, refined.mesh());

		return assembled;
	}
}
