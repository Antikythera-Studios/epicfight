package yesman.epicfight.api.client.model.rebake;

import java.util.List;
import java.util.Map;

import com.google.common.collect.Lists;
import com.mojang.blaze3d.vertex.PoseStack;

import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.joml.Vector4f;

import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import yesman.epicfight.api.utils.math.OpenMatrix4f;

public final class VanillaPartCapture {
	private VanillaPartCapture() {
	}

	public static final float MODEL_Y_OFFSET = 1.5F;

	public static void applyBaseTransform(PoseStack poseStack) {
		poseStack.mulPose(new Quaternionf().rotationY((float) Math.PI));
		poseStack.mulPose(new Quaternionf().rotationX((float) Math.PI));
		poseStack.translate(0.0F, -MODEL_Y_OFFSET, 0.0F);
	}

	public static boolean hasGeometry(ModelPart root) {
		if (!root.cubes.isEmpty()) {
			return true;
		}

		for (ModelPart child : root.children.values()) {
			if (hasGeometry(child)) {
				return true;
			}
		}

		return false;
	}

	public static void capture(RawMesh out, ModelPart root, String rootName, int anchorDepth, @javax.annotation.Nullable LiveSource source) {
		capture(out, root, rootName, anchorDepth, source, null);
	}

	public static void capture(RawMesh out, ModelPart root, String rootName, int anchorDepth, @javax.annotation.Nullable LiveSource source,
			@javax.annotation.Nullable PartPose anchor) {
		List<PartState> saved = Lists.newArrayList();
		collectState(root, saved);

		try {
			for (PartState state : saved) {
				state.resetToBind();
			}

			if (anchor != null) {
				root.loadPose(anchor);
			}

			PoseStack poseStack = new PoseStack();
			applyBaseTransform(poseStack);

			List<String> path = Lists.newArrayList();
			descend(out, poseStack, root, rootName, path, 0, anchorDepth, source);
		} finally {
			for (PartState state : saved) {
				state.restore();
			}
		}

		out.finish();
	}

	private static void descend(RawMesh out, PoseStack poseStack, ModelPart part, String path, List<String> childPath, int depth, int anchorDepth, @javax.annotation.Nullable LiveSource source) {
		if (part.cubes.isEmpty() && part.children.isEmpty()) {
			return;
		}

		poseStack.pushPose();
		part.translateAndRotate(poseStack);

		if (part.visible && !part.skipDraw && !part.cubes.isEmpty()) {
			RawMesh.Group group = out.beginGroup(path);

			for (ModelPart.Cube cube : part.cubes) {
				emitCube(out, poseStack.last(), cube);
			}

			if (depth > anchorDepth && source != null) {
				group.liveTransform = liveTransformOf(poseStack.last().pose(), source, Lists.newArrayList(childPath));
			}
		}

		for (Map.Entry<String, ModelPart> child : part.children.entrySet()) {
			childPath.add(child.getKey());
			descend(out, poseStack, child.getValue(), path + "/" + child.getKey(), childPath, depth + 1, anchorDepth, source);
			childPath.remove(childPath.size() - 1);
		}

		poseStack.popPose();
	}

	private static void emitCube(RawMesh out, PoseStack.Pose pose, ModelPart.Cube cube) {
		Matrix4f matrix = pose.pose();
		Matrix3f normalMatrix = pose.normal();

		for (ModelPart.Polygon polygon : cube.polygons) {
			if (polygon == null) {
				continue;
			}

			Vector3f normal = normalMatrix.transform(new Vector3f(polygon.normal));
			int[] corners = new int[4];

			for (int i = 0; i < 4; i++) {
				ModelPart.Vertex vertex = polygon.vertices[i];
				Vector4f position = matrix.transform(new Vector4f(vertex.pos.x() / 16.0F, vertex.pos.y() / 16.0F, vertex.pos.z() / 16.0F, 1.0F));
				corners[i] = out.addVertex(position.x(), position.y(), position.z(), normal.x(), normal.y(), normal.z(), vertex.u, vertex.v);
			}

			out.addQuad(corners[0], corners[1], corners[2], corners[3]);
		}
	}

	private static java.util.function.Supplier<OpenMatrix4f> liveTransformOf(Matrix4f bindMatrix, LiveSource source, List<String> childPath) {
		OpenMatrix4f inverseBind = OpenMatrix4f.importFromMojangMatrix(new Matrix4f(bindMatrix));
		inverseBind.invert();

		return () -> {
			ModelPart part = source.current();

			if (part == null) {
				return null;
			}

			PoseStack poseStack = new PoseStack();
			applyBaseTransform(poseStack);
			part.translateAndRotate(poseStack);

			for (String name : childPath) {
				if (!part.hasChild(name)) {
					return null;
				}

				part = part.getChild(name);
				part.translateAndRotate(poseStack);
			}

			OpenMatrix4f live = OpenMatrix4f.importFromMojangMatrix(new Matrix4f(poseStack.last().pose()));

			return OpenMatrix4f.mul(live, inverseBind, null);
		};
	}

	private static void collectState(ModelPart part, List<PartState> out) {
		out.add(new PartState(part));

		for (ModelPart child : part.children.values()) {
			collectState(child, out);
		}
	}

	private static final class PartState {
		private final ModelPart part;
		private final float x, y, z, xRot, yRot, zRot, xScale, yScale, zScale;

		PartState(ModelPart part) {
			this.part = part;
			this.x = part.x;
			this.y = part.y;
			this.z = part.z;
			this.xRot = part.xRot;
			this.yRot = part.yRot;
			this.zRot = part.zRot;
			this.xScale = part.xScale;
			this.yScale = part.yScale;
			this.zScale = part.zScale;
		}

		void resetToBind() {
			PartPose bind = this.part.getInitialPose();
			this.part.loadPose(bind);
			this.part.xScale = 1.0F;
			this.part.yScale = 1.0F;
			this.part.zScale = 1.0F;
		}

		void restore() {
			this.part.x = this.x;
			this.part.y = this.y;
			this.part.z = this.z;
			this.part.xRot = this.xRot;
			this.part.yRot = this.yRot;
			this.part.zRot = this.zRot;
			this.part.xScale = this.xScale;
			this.part.yScale = this.yScale;
			this.part.zScale = this.zScale;
		}
	}
}
