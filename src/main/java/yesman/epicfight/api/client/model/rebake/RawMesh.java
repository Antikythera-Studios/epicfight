package yesman.epicfight.api.client.model.rebake;

import java.util.List;
import java.util.function.Supplier;

import javax.annotation.Nullable;

import com.google.common.collect.Lists;

import it.unimi.dsi.fastutil.floats.FloatArrayList;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import net.minecraft.resources.ResourceLocation;
import yesman.epicfight.api.utils.math.OpenMatrix4f;

public final class RawMesh {
	public static final class Group {
		public final String name;
		public final int firstTriangle;
		public int triangleCount;
		public final int firstFace;
		public int faceCount;

		@Nullable
		public Supplier<OpenMatrix4f> liveTransform;

		@Nullable
		public ResourceLocation texture;

		@Nullable
		public net.minecraft.client.renderer.RenderType renderType;

		Group(String name, int firstTriangle, int firstFace) {
			this.name = name;
			this.firstTriangle = firstTriangle;
			this.firstFace = firstFace;
		}

		public boolean isEmpty() {
			return this.triangleCount == 0;
		}

		@Override
		public String toString() {
			return this.name + "[" + this.triangleCount + " tris]";
		}
	}

	private final FloatArrayList positions = new FloatArrayList();
	private final FloatArrayList normals = new FloatArrayList();
	private final FloatArrayList uvs = new FloatArrayList();
	private final FloatArrayList colours = new FloatArrayList();
	private final IntArrayList triangles = new IntArrayList();
	private final IntArrayList faces = new IntArrayList();
	private final List<Group> groups = Lists.newArrayList();

	private Group current;

	public Group beginGroup(String name) {
		this.closeEmptyCurrent();
		this.current = new Group(name, this.triangleCount(), this.faceCount());
		this.groups.add(this.current);

		return this.current;
	}

	private void closeEmptyCurrent() {
		if (this.current != null && this.current.isEmpty()) {
			this.groups.remove(this.groups.size() - 1);
			this.current = null;
		}
	}

	public void finish() {
		this.closeEmptyCurrent();
		this.current = null;
	}

	public int addVertex(float px, float py, float pz, float nx, float ny, float nz, float u, float v) {
		return this.addVertex(px, py, pz, nx, ny, nz, u, v, 1.0F, 1.0F, 1.0F, 1.0F, 0.0F);
	}

	public int addVertex(float px, float py, float pz, float nx, float ny, float nz, float u, float v,
			float red, float green, float blue) {
		return this.addVertex(px, py, pz, nx, ny, nz, u, v, red, green, blue, 1.0F, 0.0F);
	}

	public int addVertex(float px, float py, float pz, float nx, float ny, float nz, float u, float v,
			float red, float green, float blue, float alpha) {
		return this.addVertex(px, py, pz, nx, ny, nz, u, v, red, green, blue, alpha, 0.0F);
	}

	public int addVertex(float px, float py, float pz, float nx, float ny, float nz, float u, float v,
			float red, float green, float blue, float alpha, float emissive) {
		int index = this.vertexCount();
		this.positions.add(px);
		this.positions.add(py);
		this.positions.add(pz);
		this.normals.add(nx);
		this.normals.add(ny);
		this.normals.add(nz);
		this.uvs.add(u);
		this.uvs.add(v);
		this.colours.add(red);
		this.colours.add(green);
		this.colours.add(blue);
		this.colours.add(alpha);
		this.colours.add(emissive);

		return index;
	}

	public float red(int vertex) {
		return this.colours.getFloat(vertex * 5);
	}

	public float green(int vertex) {
		return this.colours.getFloat(vertex * 5 + 1);
	}

	public float blue(int vertex) {
		return this.colours.getFloat(vertex * 5 + 2);
	}

	public float alpha(int vertex) {
		return this.colours.getFloat(vertex * 5 + 3);
	}

	public float emissive(int vertex) {
		return this.colours.getFloat(vertex * 5 + 4);
	}

	public boolean anyTinted() {
		for (int v = 0; v < this.vertexCount(); v++) {
			if (this.red(v) < 0.999F || this.green(v) < 0.999F || this.blue(v) < 0.999F
					|| this.alpha(v) < 0.999F || this.emissive(v) > 0.001F) {
				return true;
			}
		}

		return false;
	}

	public float[] colourArray() {
		return this.colours.toFloatArray();
	}

	public void addTriangle(int a, int b, int c) {
		this.recordFace(a, b, c, -1);
		this.emitTriangle(a, b, c);
	}

	public void addQuad(int a, int b, int c, int d) {
		this.recordFace(a, b, c, d);
		this.emitTriangle(a, b, c);
		this.emitTriangle(a, c, d);
	}

	private void recordFace(int a, int b, int c, int d) {
		this.faces.add(a);
		this.faces.add(b);
		this.faces.add(c);
		this.faces.add(d);

		if (this.current != null) {
			this.current.faceCount++;
		}
	}

	private void emitTriangle(int a, int b, int c) {
		this.triangles.add(a);
		this.triangles.add(b);
		this.triangles.add(c);

		if (this.current != null) {
			this.current.triangleCount++;
		}
	}

	public int faceCount() {
		return this.faces.size() / 4;
	}

	public int vertexCount() {
		return this.positions.size() / 3;
	}

	public int triangleCount() {
		return this.triangles.size() / 3;
	}

	public boolean isEmpty() {
		return this.triangles.isEmpty();
	}

	public List<Group> groups() {
		return this.groups;
	}

	public float px(int vertex) {
		return this.positions.getFloat(vertex * 3);
	}

	public float py(int vertex) {
		return this.positions.getFloat(vertex * 3 + 1);
	}

	public float pz(int vertex) {
		return this.positions.getFloat(vertex * 3 + 2);
	}

	public float nx(int vertex) {
		return this.normals.getFloat(vertex * 3);
	}

	public float ny(int vertex) {
		return this.normals.getFloat(vertex * 3 + 1);
	}

	public float nz(int vertex) {
		return this.normals.getFloat(vertex * 3 + 2);
	}

	public float u(int vertex) {
		return this.uvs.getFloat(vertex * 2);
	}

	public float v(int vertex) {
		return this.uvs.getFloat(vertex * 2 + 1);
	}

	public int triangleVertex(int triangle, int corner) {
		return this.triangles.getInt(triangle * 3 + corner);
	}

	public float[] positionArray() {
		return this.positions.toFloatArray();
	}

	public float[] normalArray() {
		return this.normals.toFloatArray();
	}

	public float[] uvArray() {
		return this.uvs.toFloatArray();
	}

	public void flipVanillaOrientation(float yFlip) {
		for (int i = 0; i < this.vertexCount(); i++) {
			this.positions.set(i * 3, -this.positions.getFloat(i * 3));
			this.positions.set(i * 3 + 1, yFlip - this.positions.getFloat(i * 3 + 1));
			this.normals.set(i * 3, -this.normals.getFloat(i * 3));
			this.normals.set(i * 3 + 1, -this.normals.getFloat(i * 3 + 1));
		}
	}

	public interface EdgeCutter {
		void cut(RawMesh source, int groupIndex, int from, int to, it.unimi.dsi.fastutil.floats.FloatCollection into);
	}

	public static RawMesh concat(RawMesh first, RawMesh second) {
		RawMesh out = new RawMesh();

		for (RawMesh source : new RawMesh[] { first, second }) {
			int offset = out.vertexCount();

			for (int v = 0; v < source.vertexCount(); v++) {
				out.copyVertex(source, v);
			}

			for (Group group : source.groups) {
				if (group.isEmpty()) {
					continue;
				}

				Group copy = out.beginGroup(group.name);
				copy.texture = group.texture;
				copy.renderType = group.renderType;
				copy.liveTransform = group.liveTransform;

				for (int f = group.firstFace; f < group.firstFace + group.faceCount; f++) {
					int a = source.faces.getInt(f * 4);
					int b = source.faces.getInt(f * 4 + 1);
					int c = source.faces.getInt(f * 4 + 2);
					int d = source.faces.getInt(f * 4 + 3);

					if (d < 0) {
						out.addTriangle(a + offset, b + offset, c + offset);
					} else {
						out.addQuad(a + offset, b + offset, c + offset, d + offset);
					}
				}
			}
		}

		out.finish();

		return out;
	}

	public RawMesh selectGroups(java.util.function.Predicate<String> keep) {
		RawMesh out = new RawMesh();

		for (Group group : this.groups) {
			if (group.isEmpty() || !keep.test(group.name)) {
				continue;
			}

			Group copy = out.beginGroup(group.name);
			copy.texture = group.texture;
			copy.renderType = group.renderType;
			copy.liveTransform = group.liveTransform;

			for (int f = group.firstFace; f < group.firstFace + group.faceCount; f++) {
				int a = this.faces.getInt(f * 4);
				int b = this.faces.getInt(f * 4 + 1);
				int c = this.faces.getInt(f * 4 + 2);
				int d = this.faces.getInt(f * 4 + 3);

				if (d < 0) {
					out.addTriangle(out.copyVertex(this, a), out.copyVertex(this, b), out.copyVertex(this, c));
				} else {
					out.addQuad(out.copyVertex(this, a), out.copyVertex(this, b), out.copyVertex(this, c), out.copyVertex(this, d));
				}
			}
		}

		out.finish();

		return out;
	}

	public RawMesh splitConnectedPieces() {
		int[] parent = new int[this.vertexCount()];

		for (int v = 0; v < parent.length; v++) {
			parent[v] = v;
		}

		it.unimi.dsi.fastutil.longs.Long2IntOpenHashMap first = new it.unimi.dsi.fastutil.longs.Long2IntOpenHashMap();
		first.defaultReturnValue(-1);

		for (int v = 0; v < parent.length; v++) {
			long key = positionKey(this.px(v), this.py(v), this.pz(v));
			int seen = first.get(key);

			if (seen < 0) {
				first.put(key, v);
			} else {
				union(parent, v, seen);
			}
		}

		for (int f = 0; f < this.faces.size() / 4; f++) {
			int a = this.faces.getInt(f * 4);

			for (int corner = 1; corner < 4; corner++) {
				int other = this.faces.getInt(f * 4 + corner);

				if (other >= 0) {
					union(parent, a, other);
				}
			}
		}

		int[] component = new int[parent.length];

		for (int v = 0; v < parent.length; v++) {
			component[v] = find(parent, v);
		}

		java.util.HashMap<Long, Integer> layers = new java.util.HashMap<> ();

		return this.regrouped((groupName, a, b, c, d) -> {
			long key = 0L;

			for (int vertex : new int[] { a, b, c, d }) {
				key += vertex < 0 ? 0L : positionKey(this.px(vertex), this.py(vertex), this.pz(vertex));
			}

			return groupName + "/piece" + component[a] + "layer" + layers.merge(key, 0, (had, ignored) -> had + 1);
		});
	}

	private static void union(int[] parent, int a, int b) {
		int ra = find(parent, a);
		int rb = find(parent, b);

		if (ra != rb) {
			parent[ra] = rb;
		}
	}

	private static int find(int[] parent, int i) {
		while (parent[i] != i) {
			parent[i] = parent[parent[i]];
			i = parent[i];
		}

		return i;
	}

	private static long positionKey(float x, float y, float z) {
		long qx = Math.round(x * 8192.0F) & 0x1FFFFFL;
		long qy = Math.round(y * 8192.0F) & 0x1FFFFFL;
		long qz = Math.round(z * 8192.0F) & 0x1FFFFFL;

		return qx | (qy << 21) | (qz << 42);
	}

	public interface FaceNamer {
		@Nullable
		String nameOf(String groupName, int a, int b, int c, int d);
	}

	public RawMesh regrouped(FaceNamer namer) {
		RawMesh out = new RawMesh();

		for (int g = 0; g < this.groups.size(); g++) {
			Group group = this.groups.get(g);
			java.util.LinkedHashMap<String, it.unimi.dsi.fastutil.ints.IntArrayList> buckets = new java.util.LinkedHashMap<> ();

			for (int f = group.firstFace; f < group.firstFace + group.faceCount; f++) {
				int a = this.faces.getInt(f * 4);
				int b = this.faces.getInt(f * 4 + 1);
				int c = this.faces.getInt(f * 4 + 2);
				int d = this.faces.getInt(f * 4 + 3);
				String name = namer.nameOf(group.name, a, b, c, d);

				buckets.computeIfAbsent(name == null ? group.name : name, key -> new it.unimi.dsi.fastutil.ints.IntArrayList()).add(f);
			}

			for (java.util.Map.Entry<String, it.unimi.dsi.fastutil.ints.IntArrayList> bucket : buckets.entrySet()) {
				Group copy = out.beginGroup(bucket.getKey());
				copy.texture = group.texture;
				copy.renderType = group.renderType;
				copy.liveTransform = group.liveTransform;

				for (int i = 0; i < bucket.getValue().size(); i++) {
					int f = bucket.getValue().getInt(i);
					int a = this.faces.getInt(f * 4);
					int b = this.faces.getInt(f * 4 + 1);
					int c = this.faces.getInt(f * 4 + 2);
					int d = this.faces.getInt(f * 4 + 3);

					if (d < 0) {
						out.addTriangle(out.copyVertex(this, a), out.copyVertex(this, b), out.copyVertex(this, c));
					} else {
						out.addQuad(out.copyVertex(this, a), out.copyVertex(this, b), out.copyVertex(this, c), out.copyVertex(this, d));
					}
				}
			}
		}

		out.finish();

		return out;
	}

	public RawMesh tessellate(float[] stepPerGroup, int maxSegments) {
		return this.tessellate(stepPerGroup, maxSegments, null);
	}

	public RawMesh tessellate(float[] stepPerGroup, int maxSegments, @Nullable EdgeCutter cutter) {
		RawMesh out = new RawMesh();

		for (int g = 0; g < this.groups.size(); g++) {
			Group group = this.groups.get(g);
			RawMesh.Group copy = out.beginGroup(group.name);
			copy.liveTransform = group.liveTransform;
			copy.texture = group.texture;
			copy.renderType = group.renderType;

			float step = g < stepPerGroup.length ? stepPerGroup[g] : 0.0F;

			for (int f = group.firstFace; f < group.firstFace + group.faceCount; f++) {
				int a = this.faces.getInt(f * 4);
				int b = this.faces.getInt(f * 4 + 1);
				int c = this.faces.getInt(f * 4 + 2);
				int d = this.faces.getInt(f * 4 + 3);

				if (d < 0) {
					out.addTriangle(out.copyVertex(this, a), out.copyVertex(this, b), out.copyVertex(this, c));

					continue;
				}

				float[] cutsU = this.gridCrossings(a, b, step, maxSegments, cutter, g);
				float[] cutsV = this.gridCrossings(a, d, step, maxSegments, cutter, g);
				out.emitGrid(this, a, b, c, d, cutsU, cutsV);
			}
		}

		out.finish();

		return out;
	}

	private float[] gridCrossings(int p0, int p1, float step, int maxSegments, @Nullable EdgeCutter cutter, int groupIndex) {
		float length = this.distance(p0, p1);
		it.unimi.dsi.fastutil.floats.FloatArrayList candidates = new it.unimi.dsi.fastutil.floats.FloatArrayList();

		if (cutter != null) {
			cutter.cut(this, groupIndex, p0, p1, candidates);
		}

		for (int axis = 0; RebakeConfig.WORLD_GRID_CUTS && step > 0.0F && axis < 3; axis++) {
			float from = this.coordinate(p0, axis);
			float to = this.coordinate(p1, axis);
			float span = to - from;

			if (Math.abs(span) < 1.0E-5F) {
				continue;
			}

			int first = (int) Math.floor(Math.min(from, to) / step) + 1;
			int last = (int) Math.ceil(Math.max(from, to) / step) - 1;

			if (last - first > maxSegments) {
				continue;
			}

			for (int k = first; k <= last; k++) {
				candidates.add((k * step - from) / span);
			}
		}

		if (step > 0.0F && candidates.isEmpty()) {
			int segments = Math.max(1, Math.min(maxSegments, Math.round(length / step)));

			for (int i = 1; i < segments; i++) {
				candidates.add((float) i / segments);
			}
		}

		float minimumGap = length > 1.0E-5F ? RebakeConfig.MIN_EDGE_LENGTH / length : 1.0F;
		it.unimi.dsi.fastutil.floats.FloatAVLTreeSet accepted = new it.unimi.dsi.fastutil.floats.FloatAVLTreeSet();
		accepted.add(0.0F);
		accepted.add(1.0F);

		for (float candidate : candidates) {
			if (candidate <= 1.0E-4F || candidate >= 1.0F - 1.0E-4F) {
				continue;
			}

			boolean crowded = false;

			for (float kept : accepted) {
				if (Math.abs(kept - candidate) < minimumGap) {
					crowded = true;

					break;
				}
			}

			if (!crowded) {
				accepted.add(candidate);
			}
		}

		return accepted.toFloatArray();
	}

	private float coordinate(int vertex, int axis) {
		return switch (axis) {
			case 0 -> this.px(vertex);
			case 1 -> this.py(vertex);
			default -> this.pz(vertex);
		};
	}

	public float distanceBetween(int i, int j) {
		return this.distance(i, j);
	}

	private float distance(int i, int j) {
		float dx = this.px(i) - this.px(j);
		float dy = this.py(i) - this.py(j);
		float dz = this.pz(i) - this.pz(j);

		return (float) Math.sqrt(dx * dx + dy * dy + dz * dz);
	}

	int copyVertex(RawMesh source, int vertex) {
		return this.addVertex(source.px(vertex), source.py(vertex), source.pz(vertex),
			source.nx(vertex), source.ny(vertex), source.nz(vertex), source.u(vertex), source.v(vertex),
			source.red(vertex), source.green(vertex), source.blue(vertex), source.alpha(vertex),
			source.emissive(vertex));
	}

	private void emitGrid(RawMesh source, int a, int b, int c, int d, float[] cutsU, float[] cutsV) {
		int segmentsU = cutsU.length - 1;
		int segmentsV = cutsV.length - 1;
		int[] ring = new int[(segmentsU + 1) * (segmentsV + 1)];

		for (int j = 0; j <= segmentsV; j++) {
			float v = cutsV[j];

			for (int i = 0; i <= segmentsU; i++) {
				float u = cutsU[i];
				float wa = (1.0F - u) * (1.0F - v);
				float wb = u * (1.0F - v);
				float wc = u * v;
				float wd = (1.0F - u) * v;

				ring[j * (segmentsU + 1) + i] = this.addVertex(
					source.px(a) * wa + source.px(b) * wb + source.px(c) * wc + source.px(d) * wd,
					source.py(a) * wa + source.py(b) * wb + source.py(c) * wc + source.py(d) * wd,
					source.pz(a) * wa + source.pz(b) * wb + source.pz(c) * wc + source.pz(d) * wd,
					source.nx(a) * wa + source.nx(b) * wb + source.nx(c) * wc + source.nx(d) * wd,
					source.ny(a) * wa + source.ny(b) * wb + source.ny(c) * wc + source.ny(d) * wd,
					source.nz(a) * wa + source.nz(b) * wb + source.nz(c) * wc + source.nz(d) * wd,
					source.u(a) * wa + source.u(b) * wb + source.u(c) * wc + source.u(d) * wd,
					source.v(a) * wa + source.v(b) * wb + source.v(c) * wc + source.v(d) * wd,
					source.red(a) * wa + source.red(b) * wb + source.red(c) * wc + source.red(d) * wd,
					source.green(a) * wa + source.green(b) * wb + source.green(c) * wc + source.green(d) * wd,
					source.blue(a) * wa + source.blue(b) * wb + source.blue(c) * wc + source.blue(d) * wd,
					source.alpha(a) * wa + source.alpha(b) * wb + source.alpha(c) * wc + source.alpha(d) * wd,
					source.emissive(a) * wa + source.emissive(b) * wb + source.emissive(c) * wc
						+ source.emissive(d) * wd);
			}
		}

		for (int j = 0; j < segmentsV; j++) {
			for (int i = 0; i < segmentsU; i++) {
				int row = j * (segmentsU + 1);
				int next = (j + 1) * (segmentsU + 1);
				this.addQuad(ring[row + i], ring[row + i + 1], ring[next + i + 1], ring[next + i]);
			}
		}
	}

	public void repairNormals() {
		int vertices = this.vertexCount();
		boolean[] broken = new boolean[vertices];
		boolean any = false;

		for (int i = 0; i < vertices; i++) {
			float x = this.normals.getFloat(i * 3);
			float y = this.normals.getFloat(i * 3 + 1);
			float z = this.normals.getFloat(i * 3 + 2);

			if (x * x + y * y + z * z < 1.0E-8F) {
				broken[i] = true;
				any = true;
			}
		}

		if (!any) {
			return;
		}

		for (int t = 0; t < this.triangleCount(); t++) {
			int a = this.triangles.getInt(t * 3);
			int b = this.triangles.getInt(t * 3 + 1);
			int c = this.triangles.getInt(t * 3 + 2);

			float ux = this.px(b) - this.px(a), uy = this.py(b) - this.py(a), uz = this.pz(b) - this.pz(a);
			float vx = this.px(c) - this.px(a), vy = this.py(c) - this.py(a), vz = this.pz(c) - this.pz(a);
			float fx = uy * vz - uz * vy;
			float fy = uz * vx - ux * vz;
			float fz = ux * vy - uy * vx;

			for (int corner : new int[] { a, b, c }) {
				if (!broken[corner]) {
					continue;
				}

				this.normals.set(corner * 3, this.normals.getFloat(corner * 3) + fx);
				this.normals.set(corner * 3 + 1, this.normals.getFloat(corner * 3 + 1) + fy);
				this.normals.set(corner * 3 + 2, this.normals.getFloat(corner * 3 + 2) + fz);
			}
		}

		for (int i = 0; i < vertices; i++) {
			if (!broken[i]) {
				continue;
			}

			float x = this.normals.getFloat(i * 3);
			float y = this.normals.getFloat(i * 3 + 1);
			float z = this.normals.getFloat(i * 3 + 2);
			float length = (float) Math.sqrt(x * x + y * y + z * z);

			if (length > 1.0E-6F) {
				this.normals.set(i * 3, x / length);
				this.normals.set(i * 3 + 1, y / length);
				this.normals.set(i * 3 + 2, z / length);
			} else {
				this.normals.set(i * 3 + 1, 1.0F);
			}
		}
	}

	@Nullable
	public float[] bounds() {
		if (this.vertexCount() == 0) {
			return null;
		}

		float[] b = new float[] {
			Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY,
			Float.NEGATIVE_INFINITY, Float.NEGATIVE_INFINITY, Float.NEGATIVE_INFINITY
		};

		for (int i = 0; i < this.vertexCount(); i++) {
			for (int axis = 0; axis < 3; axis++) {
				float value = this.positions.getFloat(i * 3 + axis);
				b[axis] = Math.min(b[axis], value);
				b[axis + 3] = Math.max(b[axis + 3], value);
			}
		}

		return b;
	}

	@Override
	public String toString() {
		return String.format("RawMesh[%d verts, %d tris, %d groups]", this.vertexCount(), this.triangleCount(), this.groups.size());
	}
}
