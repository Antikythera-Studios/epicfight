package yesman.epicfight.api.client.model.rebake;

public final class Tessellation {
	private Tessellation() {
	}

	public static float[] chooseSteps(RawMesh mesh, SkinWeights weights, int jointCount) {
		float[] steps = new float[mesh.groups().size()];

		for (int g = 0; g < mesh.groups().size(); g++) {
			steps[g] = varies(mesh, weights, mesh.groups().get(g), jointCount) ? RebakeConfig.TESSELLATION_STEP : 0.0F;
		}

		return steps;
	}

	private static boolean varies(RawMesh mesh, SkinWeights weights, RawMesh.Group group, int jointCount) {
		float[] reference = null;
		float[] scratch = new float[jointCount];

		for (int t = group.firstTriangle; t < group.firstTriangle + group.triangleCount; t++) {
			for (int corner = 0; corner < 3; corner++) {
				dense(weights, mesh.triangleVertex(t, corner), scratch);

				if (reference == null) {
					reference = scratch.clone();

					continue;
				}

				float difference = 0.0F;

				for (int joint = 0; joint < jointCount; joint++) {
					difference += Math.abs(reference[joint] - scratch[joint]);
				}

				if (difference * 0.5F > RebakeConfig.RIGID_WEIGHT_TOLERANCE) {
					return true;
				}
			}
		}

		return false;
	}

	private static void dense(SkinWeights weights, int vertex, float[] dest) {
		java.util.Arrays.fill(dest, 0.0F);
		int base = vertex * TemplateRig.MAX_INFLUENCES;

		for (int i = 0; i < weights.counts[vertex]; i++) {
			int joint = weights.jointIds[base + i];

			if (joint >= 0 && joint < dest.length) {
				dest[joint] += weights.weights[base + i];
			}
		}
	}
}
