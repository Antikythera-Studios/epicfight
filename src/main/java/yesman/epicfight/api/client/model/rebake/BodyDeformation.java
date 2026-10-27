package yesman.epicfight.api.client.model.rebake;

import java.util.List;
import java.util.Map;

import javax.annotation.Nullable;

import com.google.common.collect.Lists;

import it.unimi.dsi.fastutil.floats.FloatArrayList;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import yesman.epicfight.api.client.model.MeshPart;
import yesman.epicfight.api.client.model.SkinnedMesh;
import yesman.epicfight.api.client.model.VertexBuilder;
import yesman.epicfight.api.model.Armature;
import yesman.epicfight.api.utils.math.OpenMatrix4f;

public final class BodyDeformation {
	private BodyDeformation() {
	}

	@Nullable
	private static SkinnedMesh source;
	private static float[] restCorners = new float[0];
	private static int[] influenceStart = new int[0];
	private static IntArrayList influenceJoint = new IntArrayList();
	private static FloatArrayList influenceWeight = new FloatArrayList();
	private static int cornerCount;

	private static float[] deformed = new float[0];
	private static boolean valid;

	public static void invalidateRest() {
		source = null;
		valid = false;
	}

	public static float[] restCorners() {
		return restCorners;
	}

	public static int triangleCount() {
		return cornerCount / 3;
	}

	@Nullable
	public static float[] deformedCorners() {
		return valid ? deformed : null;
	}

	public static void prepare(SkinnedMesh body) {
		if (source == body) {
			return;
		}

		source = body;
		FloatArrayList corners = new FloatArrayList();
		IntArrayList starts = new IntArrayList();
		influenceJoint = new IntArrayList();
		influenceWeight = new FloatArrayList();

		float[] positions = body.positions();
		int[] counts = body.affectingJointCounts();

		for (Map.Entry<String, ? extends MeshPart> entry : body.getPartEntry()) {
			List<VertexBuilder> vertices = entry.getValue().getVertices();

			for (int i = 0; i + 2 < vertices.size(); i += 3) {
				for (int corner = 0; corner < 3; corner++) {
					int p = vertices.get(i + corner).position;
					corners.add(positions[p * 3]);
					corners.add(positions[p * 3 + 1]);
					corners.add(positions[p * 3 + 2]);
					starts.add(influenceJoint.size());

					for (int k = 0; k < counts[p]; k++) {
						influenceJoint.add(body.affectingJointIndices()[p][k]);
						influenceWeight.add(body.weights()[body.affectingWeightIndices()[p][k]]);
					}
				}
			}
		}

		starts.add(influenceJoint.size());
		restCorners = corners.toFloatArray();
		influenceStart = starts.toIntArray();
		cornerCount = restCorners.length / 3;
		deformed = new float[restCorners.length];
		valid = false;
	}

	public static void beginFrame(Armature armature, OpenMatrix4f[] poses) {
		if (source == null || cornerCount == 0) {
			valid = false;

			return;
		}

		List<OpenMatrix4f> skinning = Lists.newArrayListWithCapacity(poses.length);

		for (int joint = 0; joint < poses.length; joint++) {
			skinning.add(OpenMatrix4f.mul(poses[joint], armature.searchJointById(joint).getToOrigin(), null));
		}

		for (int corner = 0; corner < cornerCount; corner++) {
			float px = restCorners[corner * 3];
			float py = restCorners[corner * 3 + 1];
			float pz = restCorners[corner * 3 + 2];
			float x = 0.0F, y = 0.0F, z = 0.0F;

			for (int i = influenceStart[corner]; i < influenceStart[corner + 1]; i++) {
				int joint = influenceJoint.getInt(i);

				if (joint < 0 || joint >= skinning.size()) {
					continue;
				}

				OpenMatrix4f m = skinning.get(joint);
				float weight = influenceWeight.getFloat(i);
				x += (m.m00 * px + m.m10 * py + m.m20 * pz + m.m30) * weight;
				y += (m.m01 * px + m.m11 * py + m.m21 * pz + m.m31) * weight;
				z += (m.m02 * px + m.m12 * py + m.m22 * pz + m.m32) * weight;
			}

			deformed[corner * 3] = x;
			deformed[corner * 3 + 1] = y;
			deformed[corner * 3 + 2] = z;
		}

		valid = true;
	}

	public static void endFrame() {
		valid = false;
	}
}
