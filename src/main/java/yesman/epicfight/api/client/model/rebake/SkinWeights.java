package yesman.epicfight.api.client.model.rebake;

public final class SkinWeights {
	public final int vertexCount;
	public final int[] jointIds;
	public final float[] weights;
	public final int[] counts;

	SkinWeights(int vertexCount) {
		this.vertexCount = vertexCount;
		this.jointIds = new int[vertexCount * TemplateRig.MAX_INFLUENCES];
		this.weights = new float[vertexCount * TemplateRig.MAX_INFLUENCES];
		this.counts = new int[vertexCount];
	}

	void store(int vertex, float[] dense) {
		int base = vertex * TemplateRig.MAX_INFLUENCES;
		int stored = 0;

		for (int joint = 0; joint < dense.length; joint++) {
			float weight = dense[joint];

			if (weight <= 1.0E-4F) {
				continue;
			}

			if (stored < TemplateRig.MAX_INFLUENCES) {
				this.jointIds[base + stored] = joint;
				this.weights[base + stored] = weight;
				stored++;

				continue;
			}

			int weakest = 0;

			for (int i = 1; i < TemplateRig.MAX_INFLUENCES; i++) {
				if (this.weights[base + i] < this.weights[base + weakest]) {
					weakest = i;
				}
			}

			if (weight > this.weights[base + weakest]) {
				this.jointIds[base + weakest] = joint;
				this.weights[base + weakest] = weight;
			}
		}

		if (stored == 0) {
			this.jointIds[base] = JointNames.ROOT;
			this.weights[base] = 1.0F;
			stored = 1;
		}

		float total = 0.0F;

		for (int i = 0; i < stored; i++) {
			total += this.weights[base + i];
		}

		if (total > 0.0F) {
			for (int i = 0; i < stored; i++) {
				this.weights[base + i] /= total;
			}
		} else {
			this.weights[base] = 1.0F;
		}

		this.counts[vertex] = stored;
	}
}
