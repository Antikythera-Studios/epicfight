package yesman.epicfight.api.client.model.rebake;

public final class SurfaceBinding {
	private static final int ANCHORS = 4;

	private final int[] triangle;
	private final float[] bary;
	private final float[] offset;
	private final float[] influence;
	private final boolean[] bound;
	private final float[] normalLocal;
	private int boundCount;

	private SurfaceBinding(int vertexCount) {
		this.triangle = new int[vertexCount * ANCHORS];
		this.bary = new float[vertexCount * ANCHORS * 3];
		this.offset = new float[vertexCount * ANCHORS * 3];
		this.influence = new float[vertexCount * ANCHORS];
		this.bound = new boolean[vertexCount];
		this.normalLocal = new float[vertexCount * ANCHORS * 3];
	}

	public int boundCount() {
		return this.boundCount;
	}

	public boolean isBound(int vertex) {
		return this.bound[vertex];
	}

	public static SurfaceBinding bind(RawMesh mesh, float[] restCorners, int triangleCount) {
		SurfaceBinding binding = new SurfaceBinding(mesh.vertexCount());
		float limit = RebakeConfig.SURFACE_BIND_DISTANCE;
		float[] scratch = new float[3];
		float[] frame = new float[9];

		int[] bestTriangle = new int[ANCHORS];
		float[] bestDistSq = new float[ANCHORS];
		float[] bestBary = new float[ANCHORS * 3];

		for (int vertex = 0; vertex < mesh.vertexCount(); vertex++) {
			float px = mesh.px(vertex), py = mesh.py(vertex), pz = mesh.pz(vertex);
			java.util.Arrays.fill(bestTriangle, -1);
			java.util.Arrays.fill(bestDistSq, limit * limit);

			for (int t = 0; t < triangleCount; t++) {
				float distSq = closestOnTriangle(restCorners, t, px, py, pz, scratch);

				if (distSq >= bestDistSq[ANCHORS - 1]) {
					continue;
				}

				int slot = ANCHORS - 1;

				while (slot > 0 && distSq < bestDistSq[slot - 1]) {
					bestDistSq[slot] = bestDistSq[slot - 1];
					bestTriangle[slot] = bestTriangle[slot - 1];
					System.arraycopy(bestBary, (slot - 1) * 3, bestBary, slot * 3, 3);
					slot--;
				}

				bestDistSq[slot] = distSq;
				bestTriangle[slot] = t;
				System.arraycopy(scratch, 0, bestBary, slot * 3, 3);
			}

			if (bestTriangle[0] < 0) {
				continue;
			}

			float total = 0.0F;

			for (int k = 0; k < ANCHORS; k++) {
				int t = bestTriangle[k];

				if (t < 0 || !buildFrame(restCorners, t, frame)) {
					binding.influence[vertex * ANCHORS + k] = 0.0F;

					continue;
				}

				int base = vertex * ANCHORS + k;
				int b3 = base * 3;
				float u = bestBary[k * 3], v = bestBary[k * 3 + 1], w = bestBary[k * 3 + 2];
				float ox = restCorners[t * 9] * u + restCorners[t * 9 + 3] * v + restCorners[t * 9 + 6] * w;
				float oy = restCorners[t * 9 + 1] * u + restCorners[t * 9 + 4] * v + restCorners[t * 9 + 7] * w;
				float oz = restCorners[t * 9 + 2] * u + restCorners[t * 9 + 5] * v + restCorners[t * 9 + 8] * w;
				float dx = px - ox, dy = py - oy, dz = pz - oz;

				binding.triangle[base] = t;
				System.arraycopy(bestBary, k * 3, binding.bary, b3, 3);
				binding.offset[b3] = dx * frame[0] + dy * frame[1] + dz * frame[2];
				binding.offset[b3 + 1] = dx * frame[3] + dy * frame[4] + dz * frame[5];
				binding.offset[b3 + 2] = dx * frame[6] + dy * frame[7] + dz * frame[8];

				float nx = mesh.nx(vertex), ny = mesh.ny(vertex), nz = mesh.nz(vertex);
				binding.normalLocal[b3] = nx * frame[0] + ny * frame[1] + nz * frame[2];
				binding.normalLocal[b3 + 1] = nx * frame[3] + ny * frame[4] + nz * frame[5];
				binding.normalLocal[b3 + 2] = nx * frame[6] + ny * frame[7] + nz * frame[8];

				float weight = 1.0F / (float) (Math.sqrt(bestDistSq[k]) + 1.0E-3F);
				binding.influence[base] = weight;
				total += weight;
			}

			if (total <= 0.0F) {
				continue;
			}

			for (int k = 0; k < ANCHORS; k++) {
				binding.influence[vertex * ANCHORS + k] /= total;
			}

			binding.bound[vertex] = true;
			binding.boundCount++;
		}

		return binding;
	}

	public boolean evaluate(int vertex, float[] deformedCorners, float[] dest, float[] normalDest) {
		if (!this.bound[vertex]) {
			return false;
		}

		float[] frame = new float[9];
		float x = 0.0F, y = 0.0F, z = 0.0F;
		float nx = 0.0F, ny = 0.0F, nz = 0.0F;
		float applied = 0.0F;

		for (int k = 0; k < ANCHORS; k++) {
			int base = vertex * ANCHORS + k;
			float influence = this.influence[base];
			int t = this.triangle[base];

			if (influence <= 0.0F || t < 0 || !buildFrame(deformedCorners, t, frame)) {
				continue;
			}

			int b3 = base * 3;
			float u = this.bary[b3], v = this.bary[b3 + 1], w = this.bary[b3 + 2];
			float ox = deformedCorners[t * 9] * u + deformedCorners[t * 9 + 3] * v + deformedCorners[t * 9 + 6] * w;
			float oy = deformedCorners[t * 9 + 1] * u + deformedCorners[t * 9 + 4] * v + deformedCorners[t * 9 + 7] * w;
			float oz = deformedCorners[t * 9 + 2] * u + deformedCorners[t * 9 + 5] * v + deformedCorners[t * 9 + 8] * w;
			float du = this.offset[b3], dv = this.offset[b3 + 1], dn = this.offset[b3 + 2];

			x += (ox + frame[0] * du + frame[3] * dv + frame[6] * dn) * influence;
			y += (oy + frame[1] * du + frame[4] * dv + frame[7] * dn) * influence;
			z += (oz + frame[2] * du + frame[5] * dv + frame[8] * dn) * influence;

			float lu = this.normalLocal[b3], lv = this.normalLocal[b3 + 1], ln = this.normalLocal[b3 + 2];
			nx += (frame[0] * lu + frame[3] * lv + frame[6] * ln) * influence;
			ny += (frame[1] * lu + frame[4] * lv + frame[7] * ln) * influence;
			nz += (frame[2] * lu + frame[5] * lv + frame[8] * ln) * influence;
			applied += influence;
		}

		if (applied < 0.5F) {
			return false;
		}

		dest[0] = x / applied;
		dest[1] = y / applied;
		dest[2] = z / applied;
		normalDest[0] = nx;
		normalDest[1] = ny;
		normalDest[2] = nz;

		return true;
	}

	private static boolean buildFrame(float[] corners, int triangle, float[] dest) {
		int base = triangle * 9;
		float ax = corners[base], ay = corners[base + 1], az = corners[base + 2];
		float ux = corners[base + 3] - ax, uy = corners[base + 4] - ay, uz = corners[base + 5] - az;
		float vx = corners[base + 6] - ax, vy = corners[base + 7] - ay, vz = corners[base + 8] - az;

		float nx = uy * vz - uz * vy;
		float ny = uz * vx - ux * vz;
		float nz = ux * vy - uy * vx;
		float nLength = (float) Math.sqrt(nx * nx + ny * ny + nz * nz);
		float uLength = (float) Math.sqrt(ux * ux + uy * uy + uz * uz);

		if (nLength < 1.0E-9F || uLength < 1.0E-9F) {
			return false;
		}

		nx /= nLength;
		ny /= nLength;
		nz /= nLength;
		ux /= uLength;
		uy /= uLength;
		uz /= uLength;

		dest[0] = ux;
		dest[1] = uy;
		dest[2] = uz;
		dest[3] = ny * uz - nz * uy;
		dest[4] = nz * ux - nx * uz;
		dest[5] = nx * uy - ny * ux;
		dest[6] = nx;
		dest[7] = ny;
		dest[8] = nz;

		return true;
	}

	private static float closestOnTriangle(float[] corners, int triangle, float px, float py, float pz, float[] baryOut) {
		int base = triangle * 9;
		float ax = corners[base], ay = corners[base + 1], az = corners[base + 2];
		float bx = corners[base + 3], by = corners[base + 4], bz = corners[base + 5];
		float cx = corners[base + 6], cy = corners[base + 7], cz = corners[base + 8];

		float abx = bx - ax, aby = by - ay, abz = bz - az;
		float acx = cx - ax, acy = cy - ay, acz = cz - az;
		float apx = px - ax, apy = py - ay, apz = pz - az;
		float d1 = abx * apx + aby * apy + abz * apz;
		float d2 = acx * apx + acy * apy + acz * apz;
		float u = 0.0F, v = 0.0F;

		if (!(d1 <= 0.0F && d2 <= 0.0F)) {
			float bpx = px - bx, bpy = py - by, bpz = pz - bz;
			float d3 = abx * bpx + aby * bpy + abz * bpz;
			float d4 = acx * bpx + acy * bpy + acz * bpz;
			float cpx = px - cx, cpy = py - cy, cpz = pz - cz;
			float d5 = abx * cpx + aby * cpy + abz * cpz;
			float d6 = acx * cpx + acy * cpy + acz * cpz;
			float vc = d1 * d4 - d3 * d2;
			float vb = d5 * d2 - d1 * d6;
			float va = d3 * d6 - d5 * d4;

			if (vc <= 0.0F && d1 >= 0.0F && d3 <= 0.0F) {
				u = d1 / (d1 - d3);
			} else if (vb <= 0.0F && d2 >= 0.0F && d6 <= 0.0F) {
				v = d2 / (d2 - d6);
			} else if (va <= 0.0F && (d4 - d3) >= 0.0F && (d5 - d6) >= 0.0F) {
				float t = (d4 - d3) / ((d4 - d3) + (d5 - d6));
				u = 1.0F - t;
				v = t;
			} else if (d3 >= 0.0F && d4 <= d3) {
				u = 1.0F;
			} else if (d6 >= 0.0F && d5 <= d6) {
				v = 1.0F;
			} else {
				float denom = 1.0F / (va + vb + vc);
				u = vb * denom;
				v = vc * denom;
			}
		}

		baryOut[0] = 1.0F - u - v;
		baryOut[1] = u;
		baryOut[2] = v;

		float qx = ax + abx * u + acx * v;
		float qy = ay + aby * u + acy * v;
		float qz = az + abz * u + acz * v;
		float dx = px - qx, dy = py - qy, dz = pz - qz;

		return dx * dx + dy * dy + dz * dz;
	}
}
