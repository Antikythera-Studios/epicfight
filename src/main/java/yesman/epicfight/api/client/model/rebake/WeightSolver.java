package yesman.epicfight.api.client.model.rebake;

import java.util.Arrays;
import java.util.Map;

import javax.annotation.Nullable;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

import java.util.List;

import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import yesman.epicfight.api.client.model.rebake.JointNames.Region;
import yesman.epicfight.main.EpicFightMod;

public final class WeightSolver {
	private WeightSolver() {
	}

	public record Refined(RawMesh mesh, SkinWeights weights) {
	}

	public static final class Report {
		public int vertices;
		public int clusters;
		public int matchedDirect;
		public int matchedFlipped;
		public int inpainted;
		public int fallback;
		public int missNoSample, missDistance, missOrientation, missRegion;
		public int inheritedFrom;
		public final StringBuilder missExamples = new StringBuilder();
		public int triangles;
		public int tessellatedGroups;
		public int addedTriangles;

		public float matchRate() {
			return this.clusters == 0 ? 0.0F : (float) (this.matchedDirect + this.matchedFlipped) / this.clusters;
		}

		void copyFrom(Report other) {
			this.vertices = other.vertices;
			this.clusters = other.clusters;
			this.matchedDirect = other.matchedDirect;
			this.matchedFlipped = other.matchedFlipped;
			this.inpainted = other.inpainted;
			this.fallback = other.fallback;
			this.missNoSample = other.missNoSample;
			this.missDistance = other.missDistance;
			this.missOrientation = other.missOrientation;
			this.missRegion = other.missRegion;
			this.inheritedFrom = other.inheritedFrom;
		}

		@Override
		public String toString() {
			return String.format("%d tris (+%d from cuts), %d groups subdivided; %d verts / %d welded - %d matched (%d via inner surface), %d inpainted, %d fell back (%d of those inherited from neighbouring geometry) (%.0f%% direct)",
				this.triangles, this.addedTriangles, this.tessellatedGroups, this.vertices, this.clusters, this.matchedDirect + this.matchedFlipped, this.matchedFlipped, this.inpainted, this.fallback, this.inheritedFrom, this.matchRate() * 100.0F)
				+ (this.missNoSample + this.missDistance + this.missOrientation + this.missRegion > 0
					? String.format("%n    misses: %d no template, %d too far, %d wrong facing, %d out of region; at %s",
						this.missNoSample, this.missDistance, this.missOrientation, this.missRegion, this.missExamples.toString().trim())
					: "");
		}
	}

	@Nullable
	public static Refined solveRefined(RawMesh mesh, TemplateRig rig, Report report) {
		SkinWeights coarse = solve(mesh, rig, report);

		if (coarse == null) {
			return null;
		}

		Region[] coarseRegions = resolveRegions(mesh);
		float[] steps = Tessellation.chooseSteps(mesh, coarse, rig.jointCount);
		int tessellated = 0;

		for (float step : steps) {
			if (step > 0.0F) {
				tessellated++;
			}
		}

		if (tessellated == 0 && !RebakeConfig.BAND_CUTS) {
			report.triangles = mesh.triangleCount();

			return new Refined(mesh, coarse);
		}

		Region[] regionOfVertex = coarseRegions;
		TemplateRig.Match cutScratch = rig.newMatch();
		RawMesh.EdgeCutter cutter = !RebakeConfig.BAND_CUTS ? null : (source, groupIndex, from, to, into) -> {
			boolean flipped = higherFirst(source, from, to);
			int lower = flipped ? to : from;
			int upper = flipped ? from : to;
			float found = rig.findBandCrossing(source.px(lower), source.py(lower), source.pz(lower),
				source.px(upper), source.py(upper), source.pz(upper), regionOfVertex[lower], cutScratch);
			float crossing = found < 0.0F ? found : flipped ? 1.0F - found : found;

			if (crossing < 0.0F) {
				return;
			}

			if (!RebakeConfig.FOLD_BANDS) {
				float[] interval = new float[2];

				if (RebakeConfig.HANDOVER_TWO_ROWS && rig.findBandInterval(source.px(from), source.py(from), source.pz(from),
						source.px(to), source.py(to), source.pz(to), regionOfVertex[from], cutScratch, interval)) {
					into.add(interval[0]);
					into.add(interval[1]);
				} else {
					into.add(crossing);
				}

				return;
			}

			float across = snapAcross(source, from, to, crossing);
			into.add(across);

			float span = source.py(to) - source.py(from);

			if (Math.abs(span) > 1.0E-5F) {
				float half = (1.0F / 32.0F) / Math.abs(span);

				if (across - half > 0.02F) {
					into.add(across - half);
				}

				if (across + half < 0.98F) {
					into.add(across + half);
				}
			}
		};

		RawMesh fine = mesh.tessellate(steps, RebakeConfig.MAX_SEGMENTS, cutter);
		Report fineReport = new Report();
		SkinWeights weights = solve(fine, rig, fineReport, true);

		if (weights == null) {
			report.triangles = mesh.triangleCount();

			return new Refined(mesh, coarse);
		}

		report.copyFrom(fineReport);
		report.triangles = fine.triangleCount();
		report.tessellatedGroups = tessellated;
		report.addedTriangles = fine.triangleCount() - mesh.triangleCount();

		return new Refined(fine, weights);
	}

	@Nullable
	public static SkinWeights solve(RawMesh mesh, TemplateRig rig, Report report) {
		return solve(mesh, rig, report, false);
	}

	@Nullable
	public static SkinWeights solve(RawMesh mesh, TemplateRig rig, Report report, boolean smooth) {
		int vertexCount = mesh.vertexCount();

		if (vertexCount == 0) {
			return null;
		}

		int jointCount = rig.jointCount;
		Region[] regionOfVertex = resolveRegions(mesh);

		int[] clusterOf = new int[vertexCount];
		Map<Long, Integer> lookup = Maps.newHashMap();
		IntArrayList representative = new IntArrayList();

		for (int vertex = 0; vertex < vertexCount; vertex++) {
			long key = weldKey(mesh.px(vertex), mesh.py(vertex), mesh.pz(vertex), regionOfVertex[vertex]);
			Integer existing = lookup.get(key);

			if (existing == null) {
				existing = representative.size();
				lookup.put(key, existing);
				representative.add(vertex);
			}

			clusterOf[vertex] = existing;
		}

		int clusterCount = representative.size();
		report.vertices = vertexCount;
		report.clusters = clusterCount;

		int[] memberStart = new int[clusterCount + 1];

		for (int vertex = 0; vertex < vertexCount; vertex++) {
			memberStart[clusterOf[vertex] + 1]++;
		}

		for (int cluster = 0; cluster < clusterCount; cluster++) {
			memberStart[cluster + 1] += memberStart[cluster];
		}

		int[] members = new int[vertexCount];
		int[] cursor = memberStart.clone();

		for (int vertex = 0; vertex < vertexCount; vertex++) {
			members[cursor[clusterOf[vertex]]++] = vertex;
		}

		float[][] clusterWeights = new float[clusterCount][];
		TemplateRig.Match match = rig.newMatch();
		TemplateRig.Match scratch = rig.newMatch();

		for (int cluster = 0; cluster < clusterCount; cluster++) {
			int representativeVertex = representative.getInt(cluster);
			Region region = regionOfVertex[representativeVertex];

			float outX = 0.0F, outY = 0.0F, outZ = 0.0F;

			for (int m = memberStart[cluster]; m < memberStart[cluster + 1]; m++) {
				outX += mesh.nx(members[m]);
				outY += mesh.ny(members[m]);
				outZ += mesh.nz(members[m]);
			}

			if (!rig.sampleAlongNormal(mesh.px(representativeVertex), mesh.py(representativeVertex), mesh.pz(representativeVertex), outX, outY, outZ, region, match, scratch)) {
				report.missNoSample++;
				noteMiss(report, mesh, representativeVertex, "no template in region");
				continue;
			}

			if (match.distance > RebakeConfig.MATCH_DISTANCE) {
				report.missDistance++;
				noteMiss(report, mesh, representativeVertex, String.format("%.3f away", match.distance));
				continue;
			}

			boolean anyDirect = match.distance <= RebakeConfig.COINCIDENT_DISTANCE;
			boolean anyFlipped = false;

			for (int m = memberStart[cluster]; m < memberStart[cluster + 1]; m++) {
				int vertex = members[m];
				float nx = mesh.nx(vertex), ny = mesh.ny(vertex), nz = mesh.nz(vertex);
				float length = (float) Math.sqrt(nx * nx + ny * ny + nz * nz);

				if (length < 1.0E-6F) {
					continue;
				}

				float dot = (nx * match.normalX + ny * match.normalY + nz * match.normalZ) / length;

				if (dot >= RebakeConfig.MATCH_NORMAL_COS) {
					anyDirect = true;
				} else if (-dot >= RebakeConfig.MATCH_NORMAL_COS) {
					anyFlipped = true;
				}

			}

			if (!anyDirect && !anyFlipped) {
				report.missOrientation++;
				noteMiss(report, mesh, representativeVertex, "normal off by >" + (int) RebakeConfig.MATCH_NORMAL_DEGREES + " deg");
				continue;
			}

			float[] masked = applyRegion(match.weights, region, jointCount);

			if (masked == null) {
				report.missRegion++;
				noteMiss(report, mesh, representativeVertex, "match outside region " + region);
				continue;
			}

			float[] fold = foldWeightsAt(rig, mesh.px(representativeVertex), mesh.py(representativeVertex), mesh.pz(representativeVertex), region, jointCount, scratch);

			if (fold == null) {
				fold = snapAtStep(rig, mesh.px(representativeVertex), mesh.py(representativeVertex), mesh.pz(representativeVertex), region, masked, jointCount, scratch);
			}

			clusterWeights[cluster] = fold != null ? fold : masked;

			if (anyDirect) {
				report.matchedDirect++;
			} else {
				report.matchedFlipped++;
			}
		}

		float[] bounds = mesh.bounds();

		if (bounds != null) {
			VoxelField field = new VoxelField(bounds, RebakeConfig.MATCH_DISTANCE, RebakeConfig.VOXEL_RESOLUTION, jointCount);

			for (int cluster = 0; cluster < clusterCount; cluster++) {
				int vertex = representative.getInt(cluster);
				field.markOccupied(mesh.px(vertex), mesh.py(vertex), mesh.pz(vertex));
			}

			field.dilate(RebakeConfig.VOXEL_DILATION);
			field.allocate();

			for (int cluster = 0; cluster < clusterCount; cluster++) {
				if (clusterWeights[cluster] != null) {
					int vertex = representative.getInt(cluster);
					field.seed(mesh.px(vertex), mesh.py(vertex), mesh.pz(vertex), clusterWeights[cluster]);
				}
			}

			if (field.hasSeeds()) {
				field.diffuse(RebakeConfig.DIFFUSION_ITERATIONS);

				for (int cluster = 0; cluster < clusterCount; cluster++) {
					if (clusterWeights[cluster] != null) {
						continue;
					}

					int vertex = representative.getInt(cluster);
					float[] sampled = field.sample(mesh.px(vertex), mesh.py(vertex), mesh.pz(vertex));

					if (sampled == null) {
						continue;
					}

					float[] masked = applyRegion(sampled, regionOfVertex[vertex], jointCount);

					if (masked != null) {
						float[] atStep = snapAtStep(rig, mesh.px(vertex), mesh.py(vertex), mesh.pz(vertex),
							regionOfVertex[vertex], masked, jointCount, match);

						clusterWeights[cluster] = atStep != null ? atStep
							: snapIfRigid(rig, mesh.px(vertex), mesh.py(vertex), mesh.pz(vertex),
								regionOfVertex[vertex], masked, jointCount, match);
						report.inpainted++;
					}
				}
			}
		}

		int solvedCount = 0;

		for (float[] weights : clusterWeights) {
			if (weights != null) {
				solvedCount++;
			}
		}

		if (solvedCount > 0) {
			for (int cluster = 0; cluster < clusterCount; cluster++) {
				if (clusterWeights[cluster] != null) {
					continue;
				}

				int vertex = representative.getInt(cluster);
				Region region = regionOfVertex[vertex];
				int nearest = nearestSolved(mesh, representative, clusterWeights, clusterCount, regionOfVertex, vertex, region, true);

				if (nearest < 0) {
					nearest = nearestSolved(mesh, representative, clusterWeights, clusterCount, regionOfVertex, vertex, region, false);
				}

				if (nearest >= 0) {
					float[] inherited = applyRegion(clusterWeights[nearest], region, jointCount);

					if (inherited != null) {
						clusterWeights[cluster] = inherited;
						report.fallback++;
						report.inheritedFrom++;
					}
				}
			}
		}

		for (int cluster = 0; cluster < clusterCount; cluster++) {
			if (clusterWeights[cluster] != null) {
				continue;
			}

			int vertex = representative.getInt(cluster);
			Region region = regionOfVertex[vertex];
			float[] resolved = null;

			if (rig.closestPoint(mesh.px(vertex), mesh.py(vertex), mesh.pz(vertex), region, match)
					|| rig.closestPoint(mesh.px(vertex), mesh.py(vertex), mesh.pz(vertex), Region.UNKNOWN, match)) {
				resolved = applyRegion(match.weights, region, jointCount);
			}

			if (resolved == null) {
				resolved = new float[jointCount];
				resolved[region.fallbackJoint] = 1.0F;
			}

			clusterWeights[cluster] = resolved;
			report.fallback++;
		}

		if (smooth) {
			blendBandBoundaries(mesh, clusterOf, clusterWeights, regionOfVertex, jointCount);
		}

		SkinWeights result = new SkinWeights(vertexCount);

		for (int vertex = 0; vertex < vertexCount; vertex++) {
			result.store(vertex, clusterWeights[clusterOf[vertex]]);
		}

		return result;
	}

	private static boolean higherFirst(RawMesh source, int from, int to) {
		if (source.py(from) != source.py(to)) {
			return source.py(from) > source.py(to);
		}

		if (source.px(from) != source.px(to)) {
			return source.px(from) > source.px(to);
		}

		return source.pz(from) > source.pz(to);
	}

	private static float snapAcross(RawMesh source, int from, int to, float crossing) {
		float y0 = source.py(from);
		float y1 = source.py(to);
		float span = y1 - y0;

		if (Math.abs(span) < 1.0E-5F) {
			return crossing;
		}

		float snapped = Math.round((y0 + span * crossing) * ROW_GRID) / ROW_GRID;
		float moved = (snapped - y0) / span;

		return moved > 1.0E-4F && moved < 1.0F - 1.0E-4F ? moved : crossing;
	}

	private static final float ROW_GRID = 4096.0F;

	private static void blendBandBoundaries(RawMesh mesh, int[] clusterOf, float[][] clusterWeights, Region[] regionOfVertex, int jointCount) {
		int clusterCount = clusterWeights.length;

		if (clusterCount == 0 || !RebakeConfig.BLEND_BAND_BOUNDARIES) {
			return;
		}

		List<IntOpenHashSet> neighbours = Lists.newArrayListWithCapacity(clusterCount);

		for (int i = 0; i < clusterCount; i++) {
			neighbours.add(new IntOpenHashSet());
		}

		for (int t = 0; t < mesh.triangleCount(); t++) {
			for (int edge = 0; edge < 3; edge++) {
				int va = mesh.triangleVertex(t, edge);
				int vb = mesh.triangleVertex(t, (edge + 1) % 3);

				if (regionOfVertex[va] != regionOfVertex[vb]) {
					continue;
				}

				int ca = clusterOf[va];
				int cb = clusterOf[vb];

				if (ca != cb) {
					neighbours.get(ca).add(cb);
					neighbours.get(cb).add(ca);
				}
			}
		}

		int[] dominant = new int[clusterCount];

		for (int cluster = 0; cluster < clusterCount; cluster++) {
			dominant[cluster] = argmax(clusterWeights[cluster]);
		}

		float[][] updated = new float[clusterCount][];

		for (int cluster = 0; cluster < clusterCount; cluster++) {
			float[] blended = null;
			int contributors = 1;

			for (int neighbour : neighbours.get(cluster)) {
				if (dominant[neighbour] == dominant[cluster]) {
					continue;
				}

				if (blended == null) {
					blended = clusterWeights[cluster].clone();
				}

				for (int joint = 0; joint < jointCount; joint++) {
					blended[joint] += clusterWeights[neighbour][joint];
				}

				contributors++;
			}

			if (blended == null) {
				continue;
			}

			float total = 0.0F;

			for (int joint = 0; joint < jointCount; joint++) {
				blended[joint] /= contributors;
				total += blended[joint];
			}

			if (total > 1.0E-5F) {
				for (int joint = 0; joint < jointCount; joint++) {
					blended[joint] /= total;
				}

				updated[cluster] = blended;
			}
		}

		for (int cluster = 0; cluster < clusterCount; cluster++) {
			if (updated[cluster] != null) {
				clusterWeights[cluster] = updated[cluster];
			}
		}
	}

	private static int argmax(float[] values) {
		int best = 0;

		for (int i = 1; i < values.length; i++) {
			if (values[i] > values[best]) {
				best = i;
			}
		}

		return best;
	}

	@Nullable

	private static void noteMiss(Report report, RawMesh mesh, int vertex, String reason) {
		if (report.missExamples.length() > 400) {
			return;
		}

		report.missExamples.append(String.format("(%+.3f %+.3f %+.3f %s) ", mesh.px(vertex), mesh.py(vertex), mesh.pz(vertex), reason));
	}

	private static float[] snapIfRigid(TemplateRig rig, float x, float y, float z, Region region, float[] diffused, int jointCount, TemplateRig.Match scratch) {
		if (rig.pureJointAt(x, y, z, region, scratch) < 0) {
			return diffused;
		}

		int strongest = 0;

		for (int joint = 1; joint < jointCount; joint++) {
			if (diffused[joint] > diffused[strongest]) {
				strongest = joint;
			}
		}

		float[] snapped = new float[jointCount];
		snapped[strongest] = 1.0F;

		return snapped;
	}

	private static float[] snapAtStep(TemplateRig rig, float x, float y, float z, Region region, float[] interpolated, int jointCount, TemplateRig.Match scratch) {
		float probe = RebakeConfig.STEP_PROBE_DISTANCE;

		int above = rig.pureJointAt(x, y + probe, z, region, scratch);
		int below = rig.pureJointAt(x, y - probe, z, region, scratch);

		if (above < 0 || below < 0 || above == below) {
			return null;
		}

		int strongest = 0;

		for (int joint = 1; joint < jointCount; joint++) {
			if (interpolated[joint] > interpolated[strongest]) {
				strongest = joint;
			}
		}

		float[] snapped = new float[jointCount];
		snapped[strongest] = 1.0F;

		return snapped;
	}

	@Nullable
	private static float[] foldWeightsAt(TemplateRig rig, float x, float y, float z, Region region, int jointCount, TemplateRig.Match scratch) {
		if (!RebakeConfig.FOLD_BANDS) {
			return null;
		}

		float half = RebakeConfig.FOLD_BAND_HALF_WIDTH;
		int above = rig.dominantJointAt(x, y + half, z, region, scratch);
		int below = rig.dominantJointAt(x, y - half, z, region, scratch);

		if (above < 0 || below < 0 || above == below) {
			return null;
		}

		if (!rig.opensOnBend(x, y, z, above, below)) {
			return null;
		}

		int fold = rig.foldJointBetween(above, below);

		if (fold < 0) {
			fold = rig.foldJointBetween(below, above);
		}

		if (fold < 0 || fold >= jointCount || !region.permits(fold)) {
			return null;
		}

		float[] weights = new float[jointCount];
		weights[fold] = 1.0F;

		return weights;
	}

	@Nullable

	private static int nearestSolved(RawMesh mesh, IntArrayList representative, float[][] clusterWeights,
			int clusterCount, Region[] regionOfVertex, int vertex, Region region, boolean sameRegionOnly) {
		float x = mesh.px(vertex), y = mesh.py(vertex), z = mesh.pz(vertex);
		int best = -1;
		float bestDistance = Float.MAX_VALUE;

		for (int other = 0; other < clusterCount; other++) {
			if (clusterWeights[other] == null) {
				continue;
			}

			int otherVertex = representative.getInt(other);

			if (sameRegionOnly && regionOfVertex[otherVertex] != region) {
				continue;
			}

			float dx = mesh.px(otherVertex) - x;
			float dy = mesh.py(otherVertex) - y;
			float dz = mesh.pz(otherVertex) - z;
			float distance = dx * dx + dy * dy + dz * dz;

			if (distance < bestDistance) {
				bestDistance = distance;
				best = other;
			}
		}

		return best;
	}

	private static float[] applyRegion(float[] dense, Region region, int jointCount) {
		float[] masked = new float[jointCount];
		float total = 0.0F;

		for (int joint = 0; joint < jointCount && joint < dense.length; joint++) {
			if (dense[joint] > 0.0F && region.permits(joint)) {
				masked[joint] = dense[joint];
				total += dense[joint];
			}
		}

		if (total <= 1.0E-5F) {
			return null;
		}

		for (int joint = 0; joint < jointCount; joint++) {
			masked[joint] /= total;
		}

		return masked;
	}

	private static Region[] resolveRegions(RawMesh mesh) {
		Region[] regions = new Region[mesh.vertexCount()];
		Arrays.fill(regions, null);

		for (RawMesh.Group group : mesh.groups()) {
			Region region = JointNames.regionOf(group.name);

			if (region == Region.UNKNOWN) {
				continue;
			}

			for (int t = group.firstTriangle; t < group.firstTriangle + group.triangleCount; t++) {
				for (int corner = 0; corner < 3; corner++) {
					regions[mesh.triangleVertex(t, corner)] = region;
				}
			}
		}

		for (int vertex = 0; vertex < regions.length; vertex++) {
			if (regions[vertex] == null) {
				regions[vertex] = Region.UNKNOWN;
			}
		}

		return regions;
	}

	private static String f(float value) {
		return String.format("%.3f", value);
	}

	private static long weldKey(float x, float y, float z, Region region) {
		long qx = Math.round(x / RebakeConfig.WELD_EPSILON) & 0xFFFFFL;
		long qy = Math.round(y / RebakeConfig.WELD_EPSILON) & 0xFFFFFL;
		long qz = Math.round(z / RebakeConfig.WELD_EPSILON) & 0xFFFFFL;

		return qx | (qy << 20) | (qz << 40) | ((long) region.ordinal() << 60);
	}
}
