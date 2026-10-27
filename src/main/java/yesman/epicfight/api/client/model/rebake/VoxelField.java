package yesman.epicfight.api.client.model.rebake;

import java.util.Arrays;

import javax.annotation.Nullable;

import it.unimi.dsi.fastutil.ints.IntArrayList;

public final class VoxelField {
	private final float originX, originY, originZ;
	private final float cellSize;
	private final int sizeX, sizeY, sizeZ;
	private final int jointCount;

	private final int[] slotOf;
	private final IntArrayList slotCells = new IntArrayList();

	private float[] values;
	private float[] scratch;
	private boolean[] fixed;
	private boolean[] known;

	public VoxelField(float[] bounds, float padding, int targetResolution, int jointCount) {
		float spanX = bounds[3] - bounds[0] + padding * 2.0F;
		float spanY = bounds[4] - bounds[1] + padding * 2.0F;
		float spanZ = bounds[5] - bounds[2] + padding * 2.0F;
		float longest = Math.max(spanX, Math.max(spanY, spanZ));

		this.cellSize = Math.max(longest / Math.max(1, targetResolution), 1.0E-4F);
		this.originX = bounds[0] - padding;
		this.originY = bounds[1] - padding;
		this.originZ = bounds[2] - padding;
		this.sizeX = Math.max(1, (int) Math.ceil(spanX / this.cellSize) + 1);
		this.sizeY = Math.max(1, (int) Math.ceil(spanY / this.cellSize) + 1);
		this.sizeZ = Math.max(1, (int) Math.ceil(spanZ / this.cellSize) + 1);
		this.jointCount = jointCount;

		this.slotOf = new int[this.sizeX * this.sizeY * this.sizeZ];
		Arrays.fill(this.slotOf, -1);
	}

	public float cellSize() {
		return this.cellSize;
	}

	private int cellAt(float x, float y, float z) {
		int ix = (int) Math.floor((x - this.originX) / this.cellSize);
		int iy = (int) Math.floor((y - this.originY) / this.cellSize);
		int iz = (int) Math.floor((z - this.originZ) / this.cellSize);

		if (ix < 0 || iy < 0 || iz < 0 || ix >= this.sizeX || iy >= this.sizeY || iz >= this.sizeZ) {
			return -1;
		}

		return (iz * this.sizeY + iy) * this.sizeX + ix;
	}

	private int occupy(int cell) {
		if (cell < 0) {
			return -1;
		}

		int slot = this.slotOf[cell];

		if (slot < 0) {
			slot = this.slotCells.size();
			this.slotOf[cell] = slot;
			this.slotCells.add(cell);
		}

		return slot;
	}

	public void markOccupied(float x, float y, float z) {
		this.occupy(this.cellAt(x, y, z));
	}

	public void dilate(int layers) {
		for (int pass = 0; pass < layers; pass++) {
			int[] seedCells = this.slotCells.toIntArray();

			for (int cell : seedCells) {
				int ix = cell % this.sizeX;
				int iy = (cell / this.sizeX) % this.sizeY;
				int iz = cell / (this.sizeX * this.sizeY);

				for (int axis = 0; axis < 3; axis++) {
					for (int step = -1; step <= 1; step += 2) {
						int nx = ix + (axis == 0 ? step : 0);
						int ny = iy + (axis == 1 ? step : 0);
						int nz = iz + (axis == 2 ? step : 0);

						if (nx < 0 || ny < 0 || nz < 0 || nx >= this.sizeX || ny >= this.sizeY || nz >= this.sizeZ) {
							continue;
						}

						this.occupy((nz * this.sizeY + ny) * this.sizeX + nx);
					}
				}
			}
		}
	}

	public void allocate() {
		int slots = this.slotCells.size();
		this.values = new float[slots * this.jointCount];
		this.scratch = new float[slots * this.jointCount];
		this.fixed = new boolean[slots];
		this.known = new boolean[slots];
	}

	public void seed(float x, float y, float z, float[] weights) {
		int cell = this.cellAt(x, y, z);

		if (cell < 0) {
			return;
		}

		int slot = this.slotOf[cell];

		if (slot < 0) {
			return;
		}

		int base = slot * this.jointCount;

		if (this.fixed[slot]) {
			for (int j = 0; j < this.jointCount; j++) {
				this.values[base + j] = (this.values[base + j] + weights[j]) * 0.5F;
			}
		} else {
			System.arraycopy(weights, 0, this.values, base, this.jointCount);
			this.fixed[slot] = true;
			this.known[slot] = true;
		}
	}

	public boolean hasSeeds() {
		if (this.fixed == null) {
			return false;
		}

		for (boolean f : this.fixed) {
			if (f) {
				return true;
			}
		}

		return false;
	}

	public void diffuse(int iterations) {
		if (this.values == null || this.slotCells.isEmpty()) {
			return;
		}

		int slots = this.slotCells.size();

		for (int iteration = 0; iteration < iterations; iteration++) {
			System.arraycopy(this.values, 0, this.scratch, 0, this.values.length);
			boolean changed = false;

			for (int slot = 0; slot < slots; slot++) {
				if (this.fixed[slot]) {
					continue;
				}

				int cell = this.slotCells.getInt(slot);
				int ix = cell % this.sizeX;
				int iy = (cell / this.sizeX) % this.sizeY;
				int iz = cell / (this.sizeX * this.sizeY);
				int contributors = 0;
				int base = slot * this.jointCount;
				Arrays.fill(this.scratch, base, base + this.jointCount, 0.0F);

				for (int axis = 0; axis < 3; axis++) {
					for (int step = -1; step <= 1; step += 2) {
						int nx = ix + (axis == 0 ? step : 0);
						int ny = iy + (axis == 1 ? step : 0);
						int nz = iz + (axis == 2 ? step : 0);

						if (nx < 0 || ny < 0 || nz < 0 || nx >= this.sizeX || ny >= this.sizeY || nz >= this.sizeZ) {
							continue;
						}

						int neighbourSlot = this.slotOf[(nz * this.sizeY + ny) * this.sizeX + nx];

						if (neighbourSlot < 0 || !this.known[neighbourSlot]) {
							continue;
						}

						int neighbourBase = neighbourSlot * this.jointCount;

						for (int j = 0; j < this.jointCount; j++) {
							this.scratch[base + j] += this.values[neighbourBase + j];
						}

						contributors++;
					}
				}

				if (contributors == 0) {
					continue;
				}

				float inverse = 1.0F / contributors;

				for (int j = 0; j < this.jointCount; j++) {
					this.scratch[base + j] *= inverse;
				}

				if (!this.known[slot]) {
					this.known[slot] = true;
					changed = true;
				}
			}

			float[] swap = this.values;
			this.values = this.scratch;
			this.scratch = swap;

			if (!changed && iteration > 8) {
				break;
			}
		}
	}

	@Nullable
	public float[] sample(float x, float y, float z) {
		int cell = this.cellAt(x, y, z);

		if (cell < 0 || this.values == null) {
			return null;
		}

		int slot = this.slotOf[cell];

		if (slot < 0 || !this.known[slot]) {
			return null;
		}

		float[] result = new float[this.jointCount];
		System.arraycopy(this.values, slot * this.jointCount, result, 0, this.jointCount);

		return result;
	}
}
