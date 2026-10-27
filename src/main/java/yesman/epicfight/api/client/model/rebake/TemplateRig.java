package yesman.epicfight.api.client.model.rebake;

import java.util.List;

import javax.annotation.Nullable;

import com.google.common.collect.Lists;

import it.unimi.dsi.fastutil.floats.FloatArrayList;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import yesman.epicfight.api.asset.AssetAccessor;
import yesman.epicfight.api.client.model.Mesh;
import yesman.epicfight.api.client.model.MeshPart;
import yesman.epicfight.api.client.model.Meshes;
import yesman.epicfight.api.client.model.SkinnedMesh;
import yesman.epicfight.api.client.model.VertexBuilder;
import yesman.epicfight.api.client.model.rebake.JointNames.Region;
import yesman.epicfight.api.utils.math.OpenMatrix4f;
import yesman.epicfight.main.EpicFightMod;

public final class TemplateRig {
	public static final int MAX_INFLUENCES = 4;

	private static TemplateRig instance;
	private static TemplateRig slimInstance;

	public static void invalidate() {
		instance = null;
		slimInstance = null;
	}

	public static boolean isSlim(@Nullable net.minecraft.world.entity.LivingEntity entity) {
		return entity instanceof net.minecraft.client.player.AbstractClientPlayer player
			&& "slim".equals(player.getModelName());
	}

	public int foldJointBetween(int upper, int lower) {
		if (this.armature == null || upper < 0 || lower < 0) {
			return -1;
		}

		yesman.epicfight.api.animation.Joint parent = this.armature.searchJointById(upper);

		if (parent == null) {
			return -1;
		}

		boolean adjacent = false;
		int found = -1;

		for (yesman.epicfight.api.animation.Joint child : parent.getSubJoints()) {
			if (child.getId() == lower) {
				adjacent = true;

				continue;
			}

			if (found >= 0) {
				return -1;
			}

			found = child.getId();
		}

		return adjacent ? found : -1;
	}

	public boolean findBandInterval(float x0, float y0, float z0, float x1, float y1, float z1, Region region, Match scratch, float[] out) {
		int startJoint = this.pureJointAt(x0, y0, z0, region, scratch);
		int endJoint = this.pureJointAt(x1, y1, z1, region, scratch);

		if (startJoint < 0 || endJoint < 0 || startJoint == endJoint) {
			return false;
		}

		out[0] = this.bisectPurity(x0, y0, z0, x1, y1, z1, region, scratch, startJoint, true);
		out[1] = this.bisectPurity(x0, y0, z0, x1, y1, z1, region, scratch, endJoint, false);

		if (out[0] > out[1]) {
			float swap = out[0];
			out[0] = out[1];
			out[1] = swap;
		}

		return out[1] - out[0] > 1.0E-4F || out[0] > 1.0E-3F;
	}

	private float bisectPurity(float x0, float y0, float z0, float x1, float y1, float z1, Region region, Match scratch, int joint, boolean forward) {
		float known = forward ? 0.0F : 1.0F;
		float unknown = forward ? 1.0F : 0.0F;

		for (int i = 0; i < 12; i++) {
			float mid = (known + unknown) * 0.5F;

			if (this.pureJointAt(x0 + (x1 - x0) * mid, y0 + (y1 - y0) * mid, z0 + (z1 - z0) * mid, region, scratch) == joint) {
				known = mid;
			} else {
				unknown = mid;
			}
		}

		return known;
	}

	public int pureJointAt(float x, float y, float z, Region region, Match scratch) {
		if (!this.closestPoint(x, y, z, region, scratch)) {
			return -1;
		}

		int best = -1;
		float bestWeight = 0.0F;

		for (int joint = 0; joint < this.jointCount; joint++) {
			if (scratch.weights[joint] > bestWeight) {
				bestWeight = scratch.weights[joint];
				best = joint;
			}
		}

		return bestWeight >= RebakeConfig.PURE_BAND_WEIGHT ? best : -1;
	}

	public boolean opensOnBend(float x, float y, float z, int upper, int lower) {
		if (this.armature == null) {
			return true;
		}

		yesman.epicfight.api.animation.Joint upperJoint = this.armature.searchJointById(upper);
		yesman.epicfight.api.animation.Joint lowerJoint = this.armature.searchJointById(lower);

		if (upperJoint == null || lowerJoint == null) {
			return true;
		}

		OpenMatrix4f lowerBind = OpenMatrix4f.invert(lowerJoint.getToOrigin(), null);
		OpenMatrix4f upperBind = OpenMatrix4f.invert(upperJoint.getToOrigin(), null);

		float ox = lowerBind.m30, oy = lowerBind.m31, oz = lowerBind.m32;
		float ax = lowerBind.m00, ay = lowerBind.m01, az = lowerBind.m02;
		float rx = x - ox, ry = y - oy, rz = z - oz;

		float dx = ay * rz - az * ry;
		float dy = az * rx - ax * rz;
		float dz = ax * ry - ay * rx;

		float towardUpperX = upperBind.m30 - x;
		float towardUpperY = upperBind.m31 - y;
		float towardUpperZ = upperBind.m32 - z;

		return dx * towardUpperX + dy * towardUpperY + dz * towardUpperZ <= 0.0F;
	}

	@Nullable
	public static TemplateRig get(int jointCount) {
		return get(jointCount, false);
	}

	@Nullable
	public static TemplateRig get(int jointCount, boolean slim) {
		TemplateRig cached = slim ? slimInstance : instance;

		if (cached != null && cached.jointCount == jointCount) {
			return cached;
		}

		try {
			TemplateRig built = build(jointCount, slim);

			if (built.triangleCount == 0) {

				return null;
			}

			if (slim) {
				slimInstance = built;
			} else {
				instance = built;
			}

			return built;
		} catch (Exception e) {

			return null;
		}
	}

	private static TemplateRig build(int jointCount, boolean slim) {
		AssetAccessor<? extends SkinnedMesh> source = slim ? Meshes.ALEX : Meshes.BIPED;
		TemplateRig rig = buildFrom(jointCount, source);
		rig.armature = yesman.epicfight.gameasset.Armatures.BIPED.get();
		rig.body = source.get();

		return rig;
	}

	@SafeVarargs
	public static TemplateRig buildFrom(int jointCount, AssetAccessor<? extends SkinnedMesh>... sources) {
		TemplateRig rig = new TemplateRig(jointCount);

		for (AssetAccessor<? extends SkinnedMesh> source : sources) {
			rig.append(source);
		}

		rig.finish();

		return rig;
	}

	public final int jointCount;

	@Nullable
	private yesman.epicfight.api.model.Armature armature;

	@Nullable
	private SkinnedMesh body;

	@Nullable
	public SkinnedMesh body() {
		return this.body;
	}

	private final FloatArrayList cornerPos = new FloatArrayList();
	private final FloatArrayList cornerNormal = new FloatArrayList();
	private final FloatArrayList cornerWeights = new FloatArrayList();
	private final IntArrayList triangleJoint = new IntArrayList();
	private int triangleCount;

	private float minX, minY, minZ, cellSize;
	private int gridX, gridY, gridZ;
	private int[] cellStart;
	private int[] cellTriangles;

	private TemplateRig(int jointCount) {
		this.jointCount = jointCount;
	}

	private void append(AssetAccessor<? extends SkinnedMesh> accessor) {
		if (accessor == null) {
			return;
		}

		SkinnedMesh mesh;

		try {
			mesh = accessor.get();
		} catch (Exception e) {
			return;
		}

		if (mesh == null) {
			return;
		}

		float[] positions = mesh.positions();
		float[] normals = mesh.normals();
		float[] weights = mesh.weights();
		int[] counts = mesh.affectingJointCounts();
		int[][] jointIndices = mesh.affectingJointIndices();
		int[][] weightIndices = mesh.affectingWeightIndices();

		for (MeshPart part : mesh.getAllParts()) {
			List<VertexBuilder> vertices = part.getVertices();

			for (int i = 0; i + 2 < vertices.size(); i += 3) {
				for (int corner = 0; corner < 3; corner++) {
					VertexBuilder vertex = vertices.get(i + corner);
					int p = vertex.position;

					if (p < 0 || p * 3 + 2 >= positions.length || p >= counts.length) {
						return;
					}

					this.cornerPos.add(positions[p * 3]);
					this.cornerPos.add(positions[p * 3 + 1]);
					this.cornerPos.add(positions[p * 3 + 2]);

					int n = vertex.normal;

					if (n >= 0 && n * 3 + 2 < normals.length) {
						this.cornerNormal.add(normals[n * 3]);
						this.cornerNormal.add(normals[n * 3 + 1]);
						this.cornerNormal.add(normals[n * 3 + 2]);
					} else {
						this.cornerNormal.add(0.0F);
						this.cornerNormal.add(1.0F);
						this.cornerNormal.add(0.0F);
					}

					this.appendWeights(counts[p], jointIndices[p], weightIndices[p], weights);
				}

				this.triangleJoint.add(this.dominantJointOfLastTriangle());
				this.triangleCount++;
			}
		}
	}

	private int dominantJointOfLastTriangle() {
		int base = this.cornerWeights.size() - this.jointCount * 3;
		int best = 0;
		float bestWeight = -1.0F;

		for (int joint = 0; joint < this.jointCount; joint++) {
			float total = this.cornerWeights.getFloat(base + joint)
				+ this.cornerWeights.getFloat(base + this.jointCount + joint)
				+ this.cornerWeights.getFloat(base + this.jointCount * 2 + joint);

			if (total > bestWeight) {
				bestWeight = total;
				best = joint;
			}
		}

		return best;
	}

	private void appendWeights(int count, int[] joints, int[] weightSlots, float[] weightPool) {
		float[] dense = new float[this.jointCount];
		float total = 0.0F;

		for (int i = 0; i < count && i < joints.length; i++) {
			int joint = joints[i];
			int slot = weightSlots[i];

			if (joint < 0 || joint >= this.jointCount || slot < 0 || slot >= weightPool.length) {
				continue;
			}

			float weight = weightPool[slot];

			if (weight > 0.0F) {
				dense[joint] += weight;
				total += weight;
			}
		}

		if (total <= 0.0F) {
			dense[0] = 1.0F;
			total = 1.0F;
		}

		for (int j = 0; j < this.jointCount; j++) {
			this.cornerWeights.add(dense[j] / total);
		}
	}

	private void finish() {
		if (this.triangleCount == 0) {
			return;
		}

		float maxX = Float.NEGATIVE_INFINITY, maxY = Float.NEGATIVE_INFINITY, maxZ = Float.NEGATIVE_INFINITY;
		this.minX = Float.POSITIVE_INFINITY;
		this.minY = Float.POSITIVE_INFINITY;
		this.minZ = Float.POSITIVE_INFINITY;

		for (int i = 0; i < this.triangleCount * 3; i++) {
			float x = this.cornerPos.getFloat(i * 3);
			float y = this.cornerPos.getFloat(i * 3 + 1);
			float z = this.cornerPos.getFloat(i * 3 + 2);
			this.minX = Math.min(this.minX, x);
			this.minY = Math.min(this.minY, y);
			this.minZ = Math.min(this.minZ, z);
			maxX = Math.max(maxX, x);
			maxY = Math.max(maxY, y);
			maxZ = Math.max(maxZ, z);
		}

		float span = Math.max(Math.max(maxX - this.minX, maxY - this.minY), maxZ - this.minZ);
		this.cellSize = Math.max(span / 24.0F, 1.0E-4F);
		this.gridX = Math.max(1, (int) Math.ceil((maxX - this.minX) / this.cellSize) + 1);
		this.gridY = Math.max(1, (int) Math.ceil((maxY - this.minY) / this.cellSize) + 1);
		this.gridZ = Math.max(1, (int) Math.ceil((maxZ - this.minZ) / this.cellSize) + 1);

		int cells = this.gridX * this.gridY * this.gridZ;
		int[] counts = new int[cells + 1];
		int[] cellOf = new int[this.triangleCount];

		for (int t = 0; t < this.triangleCount; t++) {
			cellOf[t] = this.cellIndex(this.centroid(t, 0), this.centroid(t, 1), this.centroid(t, 2));
			counts[cellOf[t] + 1]++;
		}

		for (int c = 0; c < cells; c++) {
			counts[c + 1] += counts[c];
		}

		this.cellStart = counts;
		this.cellTriangles = new int[this.triangleCount];
		int[] cursor = counts.clone();

		for (int t = 0; t < this.triangleCount; t++) {
			this.cellTriangles[cursor[cellOf[t]]++] = t;
		}

	}

	private static String fmt(float value) {
		return String.format("%.3f", value);
	}

	private float centroid(int triangle, int axis) {
		int base = triangle * 9 + axis;

		return (this.cornerPos.getFloat(base) + this.cornerPos.getFloat(base + 3) + this.cornerPos.getFloat(base + 6)) / 3.0F;
	}

	private int cellIndex(float x, float y, float z) {
		int ix = clamp((int) ((x - this.minX) / this.cellSize), this.gridX);
		int iy = clamp((int) ((y - this.minY) / this.cellSize), this.gridY);
		int iz = clamp((int) ((z - this.minZ) / this.cellSize), this.gridZ);

		return (iz * this.gridY + iy) * this.gridX + ix;
	}

	private static int clamp(int value, int size) {
		return value < 0 ? 0 : (value >= size ? size - 1 : value);
	}

	public int dominantJointAt(float x, float y, float z, Region region, Match scratch) {
		if (!this.closestPoint(x, y, z, region, scratch)) {
			return -1;
		}

		int best = -1;
		float bestWeight = 0.0F;

		for (int joint = 0; joint < this.jointCount; joint++) {
			if (scratch.weights[joint] > bestWeight) {
				bestWeight = scratch.weights[joint];
				best = joint;
			}
		}

		return best;
	}

	public float findBandCrossing(float x0, float y0, float z0, float x1, float y1, float z1, Region region, Match scratch) {
		int startJoint = this.dominantJointAt(x0, y0, z0, region, scratch);
		int endJoint = this.dominantJointAt(x1, y1, z1, region, scratch);

		if (startJoint < 0 || endJoint < 0 || startJoint == endJoint) {
			return -1.0F;
		}

		float low = 0.0F;
		float high = 1.0F;

		for (int i = 0; i < 12; i++) {
			float mid = (low + high) * 0.5F;
			int here = this.dominantJointAt(x0 + (x1 - x0) * mid, y0 + (y1 - y0) * mid, z0 + (z1 - z0) * mid, region, scratch);

			if (here == startJoint) {
				low = mid;
			} else {
				high = mid;
			}
		}

		float crossing = (low + high) * 0.5F;

		return (crossing > 1.0E-3F && crossing < 1.0F - 1.0E-3F) ? crossing : -1.0F;
	}

	public static final class Match {
		public float distance = Float.POSITIVE_INFINITY;
		public float normalX, normalY, normalZ;
		public final float[] weights;

		Match(int jointCount) {
			this.weights = new float[jointCount];
		}

		public boolean found() {
			return this.distance < Float.POSITIVE_INFINITY;
		}
	}

	public Match newMatch() {
		return new Match(this.jointCount);
	}

	public boolean closestPoint(float x, float y, float z, Region region, Match dest) {
		dest.distance = Float.POSITIVE_INFINITY;

		if (this.triangleCount == 0) {
			return false;
		}

		int ix = clamp((int) ((x - this.minX) / this.cellSize), this.gridX);
		int iy = clamp((int) ((y - this.minY) / this.cellSize), this.gridY);
		int iz = clamp((int) ((z - this.minZ) / this.cellSize), this.gridZ);
		int maxRing = Math.max(this.gridX, Math.max(this.gridY, this.gridZ));

		int best = -1;
		float bestDistSq = Float.POSITIVE_INFINITY;
		float[] bary = new float[3];
		float[] bestBary = new float[3];

		for (int ring = 0; ring <= maxRing; ring++) {
			if (best >= 0) {
				float shellDistance = (ring - 1) * this.cellSize;

				if (shellDistance > 0 && bestDistSq < shellDistance * shellDistance) {
					break;
				}
			}

			for (int dz = -ring; dz <= ring; dz++) {
				for (int dy = -ring; dy <= ring; dy++) {
					for (int dx = -ring; dx <= ring; dx++) {
						if (ring > 0 && Math.abs(dx) != ring && Math.abs(dy) != ring && Math.abs(dz) != ring) {
							continue;
						}

						int cx = ix + dx, cy = iy + dy, cz = iz + dz;

						if (cx < 0 || cy < 0 || cz < 0 || cx >= this.gridX || cy >= this.gridY || cz >= this.gridZ) {
							continue;
						}

						int cell = (cz * this.gridY + cy) * this.gridX + cx;

						for (int i = this.cellStart[cell]; i < this.cellStart[cell + 1]; i++) {
							int triangle = this.cellTriangles[i];

							if (!region.permits(this.triangleJoint.getInt(triangle))) {
								continue;
							}

							float distSq = this.closestOnTriangle(triangle, x, y, z, bary);

							if (distSq < bestDistSq) {
								bestDistSq = distSq;
								best = triangle;
								System.arraycopy(bary, 0, bestBary, 0, 3);
							}
						}
					}
				}
			}
		}

		if (best < 0) {
			return false;
		}

		dest.distance = (float) Math.sqrt(bestDistSq);

		int base = best * 3;
		float tnx = 0.0F, tny = 0.0F, tnz = 0.0F;
		java.util.Arrays.fill(dest.weights, 0.0F);

		for (int corner = 0; corner < 3; corner++) {
			float w = bestBary[corner];
			int c = base + corner;
			tnx += this.cornerNormal.getFloat(c * 3) * w;
			tny += this.cornerNormal.getFloat(c * 3 + 1) * w;
			tnz += this.cornerNormal.getFloat(c * 3 + 2) * w;
			int wBase = c * this.jointCount;

			for (int j = 0; j < this.jointCount; j++) {
				dest.weights[j] += this.cornerWeights.getFloat(wBase + j) * w;
			}
		}

		if (this.spansBandStep(base)) {
			int winner = bestBary[0] >= bestBary[1] ? (bestBary[0] >= bestBary[2] ? 0 : 2) : (bestBary[1] >= bestBary[2] ? 1 : 2);
			int wBase = (base + winner) * this.jointCount;

			for (int j = 0; j < this.jointCount; j++) {
				dest.weights[j] = this.cornerWeights.getFloat(wBase + j);
			}
		}

		float tLen = (float) Math.sqrt(tnx * tnx + tny * tny + tnz * tnz);

		if (tLen > 1.0E-6F) {
			dest.normalX = tnx / tLen;
			dest.normalY = tny / tLen;
			dest.normalZ = tnz / tLen;
		} else {
			dest.normalX = 0.0F;
			dest.normalY = 1.0F;
			dest.normalZ = 0.0F;
		}

		return true;
	}

	public boolean sampleAlongNormal(float x, float y, float z, float nx, float ny, float nz, Region region, Match dest, Match scratch) {
		if (!this.closestPoint(x, y, z, region, dest)) {
			return false;
		}

		float length = (float) Math.sqrt(nx * nx + ny * ny + nz * nz);

		if (length < 1.0E-6F || dest.distance <= 1.0E-6F) {
			return true;
		}

		float step = dest.distance / length;

		if (!this.closestPoint(x - nx * step, y - ny * step, z - nz * step, region, scratch)) {
			return true;
		}

		if (scratch.distance < dest.distance) {
			dest.distance = scratch.distance;
			dest.normalX = scratch.normalX;
			dest.normalY = scratch.normalY;
			dest.normalZ = scratch.normalZ;
			System.arraycopy(scratch.weights, 0, dest.weights, 0, dest.weights.length);
		}

		return true;
	}

	public boolean sampleFiltered(float x, float y, float z, Region region, float radius, Match dest, Match scratch) {
		if (!this.closestPoint(x, y, z, region, dest)) {
			return false;
		}

		if (radius <= 0.0F) {
			return true;
		}

		float total = 2.0F;

		for (int j = 0; j < this.jointCount; j++) {
			dest.weights[j] *= total;
		}

		for (int axis = 0; axis < 3; axis++) {
			for (int step = -1; step <= 1; step += 2) {
				float offset = step * radius;

				if (!this.closestPoint(x + (axis == 0 ? offset : 0.0F), y + (axis == 1 ? offset : 0.0F), z + (axis == 2 ? offset : 0.0F), region, scratch)) {
					continue;
				}

				for (int j = 0; j < this.jointCount; j++) {
					dest.weights[j] += scratch.weights[j];
				}

				total += 1.0F;
			}
		}

		for (int j = 0; j < this.jointCount; j++) {
			dest.weights[j] /= total;
		}

		return true;
	}

	private boolean spansBandStep(int base) {
		int shared = -1;

		for (int corner = 0; corner < 3; corner++) {
			int wBase = (base + corner) * this.jointCount;
			int dominant = -1;
			float best = 0.0F;

			for (int j = 0; j < this.jointCount; j++) {
				float w = this.cornerWeights.getFloat(wBase + j);

				if (w > best) {
					best = w;
					dominant = j;
				}
			}

			if (best < RebakeConfig.PURE_BAND_WEIGHT) {
				return false;
			}

			if (shared < 0) {
				shared = dominant;
			} else if (shared != dominant) {
				return true;
			}
		}

		return false;
	}

	private float closestOnTriangle(int triangle, float px, float py, float pz, float[] bary) {
		int base = triangle * 9;
		float ax = this.cornerPos.getFloat(base), ay = this.cornerPos.getFloat(base + 1), az = this.cornerPos.getFloat(base + 2);
		float bx = this.cornerPos.getFloat(base + 3), by = this.cornerPos.getFloat(base + 4), bz = this.cornerPos.getFloat(base + 5);
		float cx = this.cornerPos.getFloat(base + 6), cy = this.cornerPos.getFloat(base + 7), cz = this.cornerPos.getFloat(base + 8);

		float abx = bx - ax, aby = by - ay, abz = bz - az;
		float acx = cx - ax, acy = cy - ay, acz = cz - az;
		float apx = px - ax, apy = py - ay, apz = pz - az;

		float d1 = abx * apx + aby * apy + abz * apz;
		float d2 = acx * apx + acy * apy + acz * apz;

		if (d1 <= 0.0F && d2 <= 0.0F) {
			return set(bary, 1.0F, 0.0F, 0.0F, px - ax, py - ay, pz - az);
		}

		float bpx = px - bx, bpy = py - by, bpz = pz - bz;
		float d3 = abx * bpx + aby * bpy + abz * bpz;
		float d4 = acx * bpx + acy * bpy + acz * bpz;

		if (d3 >= 0.0F && d4 <= d3) {
			return set(bary, 0.0F, 1.0F, 0.0F, px - bx, py - by, pz - bz);
		}

		float vc = d1 * d4 - d3 * d2;

		if (vc <= 0.0F && d1 >= 0.0F && d3 <= 0.0F) {
			float v = d1 / (d1 - d3);

			return set(bary, 1.0F - v, v, 0.0F, px - (ax + abx * v), py - (ay + aby * v), pz - (az + abz * v));
		}

		float cpx = px - cx, cpy = py - cy, cpz = pz - cz;
		float d5 = abx * cpx + aby * cpy + abz * cpz;
		float d6 = acx * cpx + acy * cpy + acz * cpz;

		if (d6 >= 0.0F && d5 <= d6) {
			return set(bary, 0.0F, 0.0F, 1.0F, px - cx, py - cy, pz - cz);
		}

		float vb = d5 * d2 - d1 * d6;

		if (vb <= 0.0F && d2 >= 0.0F && d6 <= 0.0F) {
			float w = d2 / (d2 - d6);

			return set(bary, 1.0F - w, 0.0F, w, px - (ax + acx * w), py - (ay + acy * w), pz - (az + acz * w));
		}

		float va = d3 * d6 - d5 * d4;

		if (va <= 0.0F && (d4 - d3) >= 0.0F && (d5 - d6) >= 0.0F) {
			float w = (d4 - d3) / ((d4 - d3) + (d5 - d6));

			return set(bary, 0.0F, 1.0F - w, w, px - (bx + (cx - bx) * w), py - (by + (cy - by) * w), pz - (bz + (cz - bz) * w));
		}

		float denom = 1.0F / (va + vb + vc);
		float v = vb * denom;
		float w = vc * denom;

		return set(bary, 1.0F - v - w, v, w, px - (ax + abx * v + acx * w), py - (ay + aby * v + acy * w), pz - (az + abz * v + acz * w));
	}

	private static float set(float[] bary, float a, float b, float c, float dx, float dy, float dz) {
		bary[0] = a;
		bary[1] = b;
		bary[2] = c;

		return dx * dx + dy * dy + dz * dz;
	}
}
