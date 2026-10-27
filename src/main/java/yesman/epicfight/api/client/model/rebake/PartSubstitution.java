package yesman.epicfight.api.client.model.rebake;

import java.util.List;
import com.google.common.collect.Lists;
import java.util.Map;

import javax.annotation.Nullable;

import com.google.common.collect.Maps;

import yesman.epicfight.api.client.model.MeshPart;
import yesman.epicfight.api.client.model.SkinnedMesh;
import yesman.epicfight.api.client.model.VertexBuilder;
import yesman.epicfight.api.client.model.Meshes;
import yesman.epicfight.main.EpicFightMod;

public final class PartSubstitution {
	private static final String[] BASE_PARTS = { "head", "torso", "rightArm", "leftArm", "rightLeg", "leftLeg" };

	private static final float CONGRUENCE_TOLERANCE = 1.0F / 64.0F;

	private static final float MAX_INFLATION = 4.0F / 16.0F;

	private static final int LIMB_AXIS = 1;

	private static final float MAX_UV_ERROR_TEXELS = 0.5F;

	private static final float UV_TOLERANCE = 1.0F / 128.0F;

	private static final float SLIVER_THICKNESS = 0.005F;

	private static final float MINORITY_SHARE = 0.15F;

	private PartSubstitution() {
	}

	private static float[] uvBoundsOfGroup(RawMesh mesh, RawMesh.Group group) {
		float[] bounds = { Float.MAX_VALUE, -Float.MAX_VALUE, Float.MAX_VALUE, -Float.MAX_VALUE };

		for (int t = group.firstTriangle; t < group.firstTriangle + group.triangleCount; t++) {
			for (int corner = 0; corner < 3; corner++) {
				int vertex = mesh.triangleVertex(t, corner);
				bounds[0] = Math.min(bounds[0], mesh.u(vertex));
				bounds[1] = Math.max(bounds[1], mesh.u(vertex));
				bounds[2] = Math.min(bounds[2], mesh.v(vertex));
				bounds[3] = Math.max(bounds[3], mesh.v(vertex));
			}
		}

		return bounds;
	}

	private static float[] uvBoundsOfRange(RawMesh mesh, int from, int to) {
		float[] bounds = { Float.MAX_VALUE, -Float.MAX_VALUE, Float.MAX_VALUE, -Float.MAX_VALUE };

		for (int vertex = from; vertex < to; vertex++) {
			bounds[0] = Math.min(bounds[0], mesh.u(vertex));
			bounds[1] = Math.max(bounds[1], mesh.u(vertex));
			bounds[2] = Math.min(bounds[2], mesh.v(vertex));
			bounds[3] = Math.max(bounds[3], mesh.v(vertex));
		}

		return bounds;
	}

	public record Result(RawMesh mesh, SkinWeights weights, int groups, java.util.Set<String> heldRigid) {
	}

	@Nullable
	private static boolean fillsItsBounds(RawMesh mesh, RawMesh.Group group, Bounds bounds) {
		for (int t = group.firstTriangle; t < group.firstTriangle + group.triangleCount; t++) {
			for (int corner = 0; corner < 3; corner++) {
				int vertex = mesh.triangleVertex(t, corner);
				float nx = mesh.nx(vertex), ny = mesh.ny(vertex), nz = mesh.nz(vertex);
				float length = (float) Math.sqrt(nx * nx + ny * ny + nz * nz);

				if (length < 1.0E-5F) {
					return false;
				}

				nx /= length;
				ny /= length;
				nz /= length;

				int axis = Math.abs(nx) > Math.abs(ny) ? (Math.abs(nx) > Math.abs(nz) ? 0 : 2)
					: (Math.abs(ny) > Math.abs(nz) ? 1 : 2);
				float along = axis == 0 ? nx : axis == 1 ? ny : nz;

				if (Math.abs(along) < AXIS_ALIGNED_COS) {
					return false;
				}

				float coordinate = axis == 0 ? mesh.px(vertex) : axis == 1 ? mesh.py(vertex) : mesh.pz(vertex);
				float face = along > 0.0F ? bounds.max[axis] : bounds.min[axis];

				if (Math.abs(coordinate - face) > CONGRUENCE_TOLERANCE) {
					return false;
				}
			}
		}

		return true;
	}

	private static final float AXIS_ALIGNED_COS = 0.99F;

	private static String[] partsFor(String groupName) {
		return switch (JointNames.regionOf(groupName)) {
			case HEAD_REGION -> new String[] { "head" };
			case TORSO_REGION -> new String[] { "torso" };
			case ARM_RIGHT -> new String[] { "rightArm" };
			case ARM_LEFT -> new String[] { "leftArm" };
			case LEG_RIGHT -> new String[] { "rightLeg" };
			case LEG_LEFT -> new String[] { "leftLeg" };
			default -> BASE_PARTS;
		};
	}

	public static java.util.Set<String> congruentGroups(RawMesh captured) {
		return congruentGroups(captured, null);
	}

	public static java.util.Set<String> congruentGroups(RawMesh captured, @Nullable TemplateRig rig) {
		java.util.Set<String> congruent = new java.util.LinkedHashSet<> ();
		SkinnedMesh body;

		try {
			body = bodyOf(rig);
		} catch (Exception e) {
			return congruent;
		}

		if (body == null) {
			return congruent;
		}

		Map<String, Bounds> bodyBounds = Maps.newHashMap();

		for (String name : BASE_PARTS) {
			if (!body.hasPart(name)) {
				return congruent;
			}

			Bounds bounds = boundsOfPart(body, name);

			if (bounds == null) {
				return congruent;
			}

			bodyBounds.put(name, bounds);
		}

		for (RawMesh.Group group : captured.groups()) {
			if (group.isEmpty()) {
				continue;
			}

			Bounds box = boundsOfGroup(captured, group);

			if (!fillsItsBounds(captured, group, box)) {
				continue;
			}

			for (String name : partsFor(group.name)) {
				float delta = bodyBounds.get(name).inflationTo(box);

				if (delta >= -CONGRUENCE_TOLERANCE && delta <= MAX_INFLATION && BoxUv.fit(captured, group) != null) {
					congruent.add(group.name);

					break;
				}
			}
		}

		return congruent;
	}

	@Nullable
	private static SkinnedMesh bodyOf(@Nullable TemplateRig rig) {
		SkinnedMesh body = rig == null ? null : rig.body();

		return body != null ? body : Meshes.BIPED.get();
	}

	public static Result substitute(RawMesh captured, TemplateRig rig, int jointCount, @Nullable TextureCoverage coverage) {
		SkinnedMesh body;

		try {
			body = bodyOf(rig);
		} catch (Exception e) {
			return null;
		}

		if (body == null || captured.groups().isEmpty()) {
			return null;
		}

		Map<String, Bounds> bodyBounds = Maps.newHashMap();

		for (String name : BASE_PARTS) {
			if (!body.hasPart(name)) {

				return null;
			}

			Bounds bounds = boundsOfPart(body, name);

			if (bounds == null) {
				return null;
			}

			bodyBounds.put(name, bounds);
		}

		RawMesh out = new RawMesh();
		java.util.Set<String> heldRigid = new java.util.LinkedHashSet<> ();
		java.util.List<float[]> dense = new java.util.ArrayList<> ();
		int substituted = 0;

		for (RawMesh.Group group : captured.groups()) {
			if (group.isEmpty()) {
				continue;
			}

			Bounds box = boundsOfGroup(captured, group);
			String match = null;
			float inflation = 0.0F;

			if (!fillsItsBounds(captured, group, box)) {

				return null;
			}

			for (String name : partsFor(group.name)) {
				float delta = bodyBounds.get(name).inflationTo(box);

				if (delta >= -CONGRUENCE_TOLERANCE && delta <= MAX_INFLATION) {
					match = name;
					inflation = Math.max(0.0F, delta);

					break;
				}
			}

			if (match == null) {
				StringBuilder why = new StringBuilder();

				for (String name : BASE_PARTS) {
					why.append(String.format("%s d=%.4f  ", name, bodyBounds.get(name).inflationTo(box)));
				}


				return null;
			}

			BoxUv uv = BoxUv.fit(captured, group);

			if (uv == null) {
				return null;
			}

			int before = out.vertexCount();
			Bounds inflated = box;
			emit(out, dense, body, rig, match, bodyBounds.get(match), box, uv, captured, group, jointCount);

			float offBox = 0.0F;
			float[] outside = null;

			for (int v = before; v < out.vertexCount(); v++) {
				float d = inflated.distanceToSurface(out.px(v), out.py(v), out.pz(v));

				if (d > offBox) {
					offBox = d;
					outside = new float[] { out.px(v), out.py(v), out.pz(v) };
				}
			}


			if (holdRigidIfItNeverReachesTheBend(out, dense, before, out.vertexCount(), coverage, group.name, jointCount)) {
				heldRigid.add(group.name);
			}

			float[] want = uvBoundsOfGroup(captured, group);
			float[] got = uvBoundsOfRange(out, before, out.vertexCount());

			if (got[0] < want[0] - UV_TOLERANCE || got[1] > want[1] + UV_TOLERANCE
					|| got[2] < want[2] - UV_TOLERANCE || got[3] > want[3] + UV_TOLERANCE) {

				return null;
			}

			substituted++;
		}

		out.finish();

		if (substituted == 0 || out.vertexCount() == 0) {
			return null;
		}

		SkinWeights weights = new SkinWeights(out.vertexCount());

		for (int vertex = 0; vertex < out.vertexCount(); vertex++) {
			weights.store(vertex, dense.get(vertex));
		}

		return new Result(out, weights, substituted, heldRigid);
	}

	private static float[] averageTint(RawMesh captured, RawMesh.Group source) {
		float red = 0.0F, green = 0.0F, blue = 0.0F, alpha = 0.0F, emissive = 0.0F;
		int counted = 0;

		for (int t = source.firstTriangle; t < source.firstTriangle + source.triangleCount; t++) {
			for (int corner = 0; corner < 3; corner++) {
				int vertex = captured.triangleVertex(t, corner);
				red += captured.red(vertex);
				green += captured.green(vertex);
				blue += captured.blue(vertex);
				alpha += captured.alpha(vertex);
				emissive += captured.emissive(vertex);
				counted++;
			}
		}

		if (counted == 0) {
			return new float[] { 1.0F, 1.0F, 1.0F, 1.0F, 0.0F };
		}

		return new float[] { red / counted, green / counted, blue / counted, alpha / counted, emissive / counted };
	}

	private static List<float[][][]> clipAlongLimb(float[][] point, float[][] normal, float[][] weight,
			float[][] texture, Bounds target) {
		List<float[][][]> out = Lists.newArrayList();
		List<float[][]> polygon = Lists.newArrayList();

		for (int corner = 0; corner < 3; corner++) {
			polygon.add(new float[][] { point[corner], normal[corner], weight[corner], texture[corner] });
		}

		polygon = clipAgainst(polygon, target.min[LIMB_AXIS], true);
		polygon = clipAgainst(polygon, target.max[LIMB_AXIS], false);

		for (int i = 1; i + 1 < polygon.size(); i++) {
			float[][][] triangle = new float[4][3][];

			for (int corner = 0; corner < 3; corner++) {
				float[][] vertex = polygon.get(corner == 0 ? 0 : i + corner - 1);

				for (int attribute = 0; attribute < 4; attribute++) {
					triangle[attribute][corner] = vertex[attribute];
				}
			}

			out.add(triangle);
		}

		return out;
	}

	private static List<float[][]> clipAgainst(List<float[][]> polygon, float plane, boolean keepAbove) {
		List<float[][]> out = Lists.newArrayList();

		for (int i = 0; i < polygon.size(); i++) {
			float[][] current = polygon.get(i);
			float[][] next = polygon.get((i + 1) % polygon.size());
			boolean currentIn = keepAbove ? current[0][LIMB_AXIS] >= plane - CONGRUENCE_TOLERANCE
				: current[0][LIMB_AXIS] <= plane + CONGRUENCE_TOLERANCE;
			boolean nextIn = keepAbove ? next[0][LIMB_AXIS] >= plane - CONGRUENCE_TOLERANCE
				: next[0][LIMB_AXIS] <= plane + CONGRUENCE_TOLERANCE;

			if (currentIn) {
				out.add(current);
			}

			if (currentIn != nextIn) {
				float span = next[0][LIMB_AXIS] - current[0][LIMB_AXIS];

				if (Math.abs(span) > 1.0E-6F) {
					out.add(mix(current, next, (plane - current[0][LIMB_AXIS]) / span));
				}
			}
		}

		return out;
	}

	private static float[][] mix(float[][] a, float[][] b, float t) {
		float[][] out = new float[a.length][];

		for (int attribute = 0; attribute < a.length; attribute++) {
			float[] blended = new float[a[attribute].length];

			for (int i = 0; i < blended.length; i++) {
				blended[i] = a[attribute][i] + (b[attribute][i] - a[attribute][i]) * t;
			}

			out[attribute] = blended;
		}

		return out;
	}

	private static void emit(RawMesh out, List<float[]> dense, SkinnedMesh body, TemplateRig rig, String partName, Bounds bounds,
			Bounds target, BoxUv uv, RawMesh captured, RawMesh.Group source, int jointCount) {
		MeshPart part = body.getPart(partName);
		List<VertexBuilder> corners = part.getVertices();
		float[] positions = body.positions();
		float[] normals = body.normals();
		float[] pool = body.weights();
		int[] counts = body.affectingJointCounts();
		int[][] jointIndices = body.affectingJointIndices();
		int[][] weightIndices = body.affectingWeightIndices();

		RawMesh.Group group = out.beginGroup(source.name);
		group.texture = source.texture;
		group.renderType = source.renderType;
		group.liveTransform = source.liveTransform;
		int[] triangle = new int[3];

		float[] tint = averageTint(captured, source);

		for (int i = 0; i + 2 < corners.size(); i += 3) {
			float[][] point = new float[3][];
			float[][] weight = new float[3][];
			float[][] normal = new float[3][];
			float[][] texture = new float[3][];

			for (int corner = 0; corner < 3; corner++) {
				VertexBuilder builder = corners.get(i + corner);
				int p = builder.position;
				int n = builder.normal;

				float nx = 0.0F, ny = 1.0F, nz = 0.0F;

				if (n >= 0 && n * 3 + 2 < normals.length) {
					nx = normals[n * 3];
					ny = normals[n * 3 + 1];
					nz = normals[n * 3 + 2];
				}

				float px = bounds.push(0, positions[p * 3], target);
				float py = bounds.push(1, positions[p * 3 + 1], target);
				float pz = bounds.push(2, positions[p * 3 + 2], target);

				point[corner] = new float[] { px, py, pz };
				normal[corner] = new float[] { nx, ny, nz };
				texture[corner] = uv.sample(px, py, pz, nx, ny, nz);
				weight[corner] = denseWeights(p, counts, jointIndices, weightIndices, pool, jointCount);
			}

			if (alwaysDegenerate(point, weight)) {
				continue;
			}

			if (!uv.draws(normal[0][0], normal[0][1], normal[0][2])) {
				continue;
			}

			for (float[][][] clipped : clipAlongLimb(point, normal, weight, texture, target)) {
				for (int corner = 0; corner < 3; corner++) {
					float[] p = clipped[0][corner];
					float[] n = clipped[1][corner];
					float[] w = clipped[2][corner];
					float[] t = clipped[3][corner];

					triangle[corner] = out.addVertex(p[0], p[1], p[2], n[0], n[1], n[2], t[0], t[1],
						tint[0], tint[1], tint[2], tint[3], tint[4]);
					dense.add(w);
				}

				out.addTriangle(triangle[0], triangle[1], triangle[2]);
			}
		}
	}

	private static boolean alwaysDegenerate(float[][] point, float[][] weight) {
		if (!java.util.Arrays.equals(weight[0], weight[1]) || !java.util.Arrays.equals(weight[1], weight[2])) {
			return false;
		}

		float ux = point[1][0] - point[0][0], uy = point[1][1] - point[0][1], uz = point[1][2] - point[0][2];
		float vx = point[2][0] - point[0][0], vy = point[2][1] - point[0][1], vz = point[2][2] - point[0][2];
		float nx = uy * vz - uz * vy, ny = uz * vx - ux * vz, nz = ux * vy - uy * vx;
		float area2 = (float) Math.sqrt(nx * nx + ny * ny + nz * nz);

		float longest = (float) Math.sqrt(Math.max(ux * ux + uy * uy + uz * uz,
			Math.max(vx * vx + vy * vy + vz * vz,
				(point[1][0] - point[2][0]) * (point[1][0] - point[2][0])
					+ (point[1][1] - point[2][1]) * (point[1][1] - point[2][1])
					+ (point[1][2] - point[2][2]) * (point[1][2] - point[2][2]))));

		return longest > 1.0E-6F && area2 / longest < SLIVER_THICKNESS;
	}

	private static float[] denseWeights(int vertex, int[] counts, int[][] jointIndices, int[][] weightIndices, float[] pool, int jointCount) {
		float[] weights = new float[jointCount];

		if (vertex < 0 || vertex >= counts.length) {
			return weights;
		}

		float total = 0.0F;

		for (int i = 0; i < counts[vertex]; i++) {
			int joint = jointIndices[vertex][i];
			int slot = weightIndices[vertex][i];

			if (joint < 0 || joint >= jointCount || slot < 0 || slot >= pool.length) {
				continue;
			}

			weights[joint] += pool[slot];
			total += pool[slot];
		}

		if (total > 1.0E-5F) {
			for (int joint = 0; joint < jointCount; joint++) {
				weights[joint] /= total;
			}
		}

		return weights;
	}

	@Nullable
	private static Bounds boundsOfPart(SkinnedMesh body, String name) {
		MeshPart part = body.getPart(name);

		if (part == null) {
			return null;
		}

		float[] positions = body.positions();
		Bounds bounds = new Bounds();

		for (VertexBuilder builder : part.getVertices()) {
			int p = builder.position;

			if (p >= 0 && p * 3 + 2 < positions.length) {
				bounds.include(positions[p * 3], positions[p * 3 + 1], positions[p * 3 + 2]);
			}
		}

		return bounds.valid() ? bounds : null;
	}

	private static Bounds boundsOfGroup(RawMesh mesh, RawMesh.Group group) {
		Bounds bounds = new Bounds();

		for (int t = group.firstTriangle; t < group.firstTriangle + group.triangleCount; t++) {
			for (int corner = 0; corner < 3; corner++) {
				int vertex = mesh.triangleVertex(t, corner);
				bounds.include(mesh.px(vertex), mesh.py(vertex), mesh.pz(vertex));
			}
		}

		return bounds;
	}

	private static final class Bounds {
		private final float[] min = { Float.MAX_VALUE, Float.MAX_VALUE, Float.MAX_VALUE };
		private final float[] max = { -Float.MAX_VALUE, -Float.MAX_VALUE, -Float.MAX_VALUE };

		void include(float x, float y, float z) {
			float[] p = { x, y, z };

			for (int axis = 0; axis < 3; axis++) {
				this.min[axis] = Math.min(this.min[axis], p[axis]);
				this.max[axis] = Math.max(this.max[axis], p[axis]);
			}
		}

		@Override
		public String toString() {
			return String.format("x[%.3f %.3f] y[%.3f %.3f] z[%.3f %.3f]", this.min[0], this.max[0], this.min[1], this.max[1], this.min[2], this.max[2]);
		}

		boolean valid() {
			return this.min[0] <= this.max[0];
		}

		float inflationTo(Bounds other) {
			float worst = 0.0F;

			for (int axis = 0; axis < 3; axis++) {
				float over = other.max[axis] - this.max[axis];
				float under = this.min[axis] - other.min[axis];

				if (axis != LIMB_AXIS && (over < -CONGRUENCE_TOLERANCE || under < -CONGRUENCE_TOLERANCE)) {
					return Float.NaN;
				}

				worst = Math.max(worst, Math.max(over, under));
			}

			return worst;
		}

		float distanceToSurface(float x, float y, float z) {
			float[] p = { x, y, z };
			float outside = 0.0F;
			float deepest = Float.MAX_VALUE;

			for (int axis = 0; axis < 3; axis++) {
				outside = Math.max(outside, Math.max(this.min[axis] - p[axis], p[axis] - this.max[axis]));
				deepest = Math.min(deepest, Math.min(p[axis] - this.min[axis], this.max[axis] - p[axis]));
			}

			return outside > 0.0F ? outside : Math.max(0.0F, deepest);
		}

		float push(int axis, float value, Bounds target) {
			if (value >= this.max[axis] - CONGRUENCE_TOLERANCE) {
				return target.max[axis];
			}

			if (value <= this.min[axis] + CONGRUENCE_TOLERANCE) {
				return target.min[axis];
			}

			return value;
		}
	}

	private static final class BoxUv {
		private static final String[] FACE_NAMES = { "+x", "-x", "+y", "-y", "+z", "-z" };

		private final float[][] maps = new float[6][];
		private final boolean[] drawn = new boolean[6];

		@Nullable
		static BoxUv fit(RawMesh mesh, RawMesh.Group group) {
			BoxUv box = new BoxUv();
			double[][] normal = new double[6][9];
			double[][] rhs = new double[6][6];

			for (int t = group.firstTriangle; t < group.firstTriangle + group.triangleCount; t++) {
				for (int corner = 0; corner < 3; corner++) {
					int vertex = mesh.triangleVertex(t, corner);
					int face = faceOf(mesh.nx(vertex), mesh.ny(vertex), mesh.nz(vertex));

					if (face < 0) {
						continue;
					}

					box.drawn[face] = true;
					int axis = face >> 1;
					float p1 = component(mesh, vertex, (axis + 1) % 3);
					float p2 = component(mesh, vertex, (axis + 2) % 3);
					double[] row = { p1, p2, 1.0 };

					for (int i = 0; i < 3; i++) {
						for (int j = 0; j < 3; j++) {
							normal[face][i * 3 + j] += row[i] * row[j];
						}

						rhs[face][i] += row[i] * mesh.u(vertex);
						rhs[face][i + 3] += row[i] * mesh.v(vertex);
					}
				}
			}

			boolean any = false;
			StringBuilder missing = new StringBuilder();

			for (int face = 0; face < 6; face++) {
				float[] solved = solve(normal[face], rhs[face]);

				if (solved != null) {
					box.maps[face] = solved;
					any = true;
				} else {
					missing.append(FACE_NAMES[face]).append(' ');
				}
			}

			if (!any) {
				return null;
			}

			float worst = 0.0F;

			for (int t = group.firstTriangle; t < group.firstTriangle + group.triangleCount; t++) {
				for (int corner = 0; corner < 3; corner++) {
					int vertex = mesh.triangleVertex(t, corner);
					float[] got = box.sample(mesh.px(vertex), mesh.py(vertex), mesh.pz(vertex),
						mesh.nx(vertex), mesh.ny(vertex), mesh.nz(vertex));
					worst = Math.max(worst, Math.max(Math.abs(got[0] - mesh.u(vertex)), Math.abs(got[1] - mesh.v(vertex))));
				}
			}


			if (worst * 64.0F > MAX_UV_ERROR_TEXELS) {

				return null;
			}

			return box;
		}

		boolean draws(float nx, float ny, float nz) {
			int face = faceOf(nx, ny, nz);

			return face >= 0 && this.drawn[face];
		}

		float[] sample(float x, float y, float z, float nx, float ny, float nz) {
			int face = faceOf(nx, ny, nz);

			if (face < 0 || this.maps[face] == null) {
				return new float[2];
			}

			int axis = face >> 1;
			float[] p = { x, y, z };
			float p1 = p[(axis + 1) % 3];
			float p2 = p[(axis + 2) % 3];
			float[] m = this.maps[face];

			return new float[] { m[0] * p1 + m[1] * p2 + m[2], m[3] * p1 + m[4] * p2 + m[5] };
		}

		private static float component(RawMesh mesh, int vertex, int axis) {
			return axis == 0 ? mesh.px(vertex) : axis == 1 ? mesh.py(vertex) : mesh.pz(vertex);
		}

		private static int faceOf(float nx, float ny, float nz) {
			float ax = Math.abs(nx), ay = Math.abs(ny), az = Math.abs(nz);

			if (ax >= ay && ax >= az) {
				return ax < 1.0E-4F ? -1 : (nx > 0.0F ? 0 : 1);
			}

			if (ay >= az) {
				return ay < 1.0E-4F ? -1 : (ny > 0.0F ? 2 : 3);
			}

			return az < 1.0E-4F ? -1 : (nz > 0.0F ? 4 : 5);
		}

		@Nullable
		private static float[] solve(double[] matrix, double[] rhs) {
			double[][] a = new double[3][5];

			for (int i = 0; i < 3; i++) {
				for (int j = 0; j < 3; j++) {
					a[i][j] = matrix[i * 3 + j];
				}

				a[i][3] = rhs[i];
				a[i][4] = rhs[i + 3];
			}

			for (int col = 0; col < 3; col++) {
				int pivot = col;

				for (int row = col + 1; row < 3; row++) {
					if (Math.abs(a[row][col]) > Math.abs(a[pivot][col])) {
						pivot = row;
					}
				}

				if (Math.abs(a[pivot][col]) < 1.0E-9) {
					return null;
				}

				double[] swap = a[col];
				a[col] = a[pivot];
				a[pivot] = swap;

				for (int row = 0; row < 3; row++) {
					if (row == col) {
						continue;
					}

					double factor = a[row][col] / a[col][col];

					for (int j = col; j < 5; j++) {
						a[row][j] -= factor * a[col][j];
					}
				}
			}

			return new float[] {
				(float) (a[0][3] / a[0][0]), (float) (a[1][3] / a[1][1]), (float) (a[2][3] / a[2][2]),
				(float) (a[0][4] / a[0][0]), (float) (a[1][4] / a[1][1]), (float) (a[2][4] / a[2][2]),
			};
		}
	}

	private static boolean holdRigidIfItNeverReachesTheBend(RawMesh mesh, List<float[]> dense, int from, int to,
			@Nullable TextureCoverage coverage, String name, int jointCount) {
		if (coverage == null || to <= from) {
			return false;
		}

		int lowest = from;
		int highest = from;

		for (int v = from; v < to; v++) {
			if (mesh.py(v) < mesh.py(lowest)) {
				lowest = v;
			}

			if (mesh.py(v) > mesh.py(highest)) {
				highest = v;
			}
		}

		int below = dominantOf(dense.get(lowest), jointCount);
		float boundary = Float.NaN;

		for (int v = from; v < to; v++) {
			if (dominantOf(dense.get(v), jointCount) != below && (Float.isNaN(boundary) || mesh.py(v) < boundary)) {
				boundary = mesh.py(v);
			}
		}

		if (Float.isNaN(boundary)) {
			return false;
		}

		float[] split = coverage.paintedSplit(mesh, from / 3, (to - from) / 3, boundary);
		float total = split[0] + split[1];

		if (total <= 0.0F) {
			return false;
		}

		float minority = Math.min(split[0], split[1]) / total;

		if (minority > MINORITY_SHARE) {
			return false;
		}

		int rides = split[0] >= split[1] ? below : dominantOf(dense.get(highest), jointCount);
		float[] rigid = new float[jointCount];
		rigid[rides] = 1.0F;

		for (int v = from; v < to; v++) {
			dense.set(v, rigid);
		}


		return true;
	}

	private static int dominantOf(float[] weights, int jointCount) {
		int best = 0;

		for (int joint = 1; joint < jointCount; joint++) {
			if (weights[joint] > weights[best]) {
				best = joint;
			}
		}

		return best;
	}

	public static void logSubstitution(String provenance, Result result) {
	}
}
