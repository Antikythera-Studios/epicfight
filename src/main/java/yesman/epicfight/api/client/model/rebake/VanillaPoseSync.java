package yesman.epicfight.api.client.model.rebake;

import com.mojang.blaze3d.vertex.PoseStack;

import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.util.Mth;
import yesman.epicfight.api.animation.Joint;
import yesman.epicfight.api.model.Armature;
import yesman.epicfight.api.utils.math.OpenMatrix4f;

public final class VanillaPoseSync {
	private VanillaPoseSync() {
	}

	private static final OpenMatrix4f MODEL_TO_PART = buildBridge();

	private static OpenMatrix4f buildBridge() {
		OpenMatrix4f matrix = new OpenMatrix4f();
		matrix.m00 = -1.0F;
		matrix.m11 = -1.0F;
		matrix.m22 = 1.0F;
		matrix.m31 = VanillaPartCapture.MODEL_Y_OFFSET;

		return matrix;
	}

	private static final String[] JOINT_FOR_PART = {
		"Head", "Chest", "Arm_R", "Arm_L", "Thigh_R", "Thigh_L"
	};

	public static void apply(HumanoidModel<?> model, Armature armature, OpenMatrix4f[] poses) {
		ModelPart[] parts = { model.head, model.body, model.rightArm, model.leftArm, model.rightLeg, model.leftLeg };

		for (int i = 0; i < parts.length; i++) {
			applyOne(parts[i], armature, poses, JOINT_FOR_PART[i]);
		}

		copyPose(model.head, model.hat);
	}

	private static void applyOne(ModelPart part, Armature armature, OpenMatrix4f[] poses, String jointName) {
		if (part == null || !armature.hasJoint(jointName)) {
			return;
		}

		Joint joint = armature.searchJointByName(jointName);
		int id = joint.getId();

		if (id < 0 || id >= poses.length || poses[id] == null) {
			return;
		}

		OpenMatrix4f live = OpenMatrix4f.mul(MODEL_TO_PART, poses[id], null);
		live = OpenMatrix4f.mul(live, joint.getToOrigin(), null);
		live = OpenMatrix4f.mul(live, MODEL_TO_PART, null);
		live = OpenMatrix4f.mul(live, bindOf(part), null);

		decomposeInto(live, part);
	}

	private static OpenMatrix4f bindOf(ModelPart part) {
		PartPose bind = part.getInitialPose();
		OpenMatrix4f matrix = new OpenMatrix4f();
		matrix.setIdentity();

		OpenMatrix4f rotation = rotationZYX(bind.zRot, bind.yRot, bind.xRot);
		rotation.m30 = bind.x / 16.0F;
		rotation.m31 = bind.y / 16.0F;
		rotation.m32 = bind.z / 16.0F;

		return rotation;
	}

	private static OpenMatrix4f rotationZYX(float angleZ, float angleY, float angleX) {
		float sa = Mth.sin(angleX), ca = Mth.cos(angleX);
		float sb = Mth.sin(angleY), cb = Mth.cos(angleY);
		float sc = Mth.sin(angleZ), cc = Mth.cos(angleZ);

		OpenMatrix4f matrix = new OpenMatrix4f();
		matrix.m00 = cb * cc;
		matrix.m01 = cb * sc;
		matrix.m02 = -sb;
		matrix.m10 = cc * sb * sa - sc * ca;
		matrix.m11 = sc * sb * sa + cc * ca;
		matrix.m12 = cb * sa;
		matrix.m20 = cc * sb * ca + sc * sa;
		matrix.m21 = sc * sb * ca - cc * sa;
		matrix.m22 = cb * ca;
		matrix.m33 = 1.0F;

		return matrix;
	}

	private static void decomposeInto(OpenMatrix4f matrix, ModelPart part) {
		float scaleX = length(matrix.m00, matrix.m01, matrix.m02);
		float scaleY = length(matrix.m10, matrix.m11, matrix.m12);
		float scaleZ = length(matrix.m20, matrix.m21, matrix.m22);

		if (scaleX < 1.0E-5F || scaleY < 1.0E-5F || scaleZ < 1.0E-5F) {
			return;
		}

		float m00 = matrix.m00 / scaleX, m01 = matrix.m01 / scaleX, m02 = matrix.m02 / scaleX;
		float m11 = matrix.m11 / scaleY, m12 = matrix.m12 / scaleY;
		float m21 = matrix.m21 / scaleZ, m22 = matrix.m22 / scaleZ;

		float yRot = (float) Math.asin(-Mth.clamp(m02, -1.0F, 1.0F));
		float xRot;
		float zRot;

		if (Math.abs(m02) < 0.9999F) {
			xRot = (float) Math.atan2(m12, m22);
			zRot = (float) Math.atan2(m01, m00);
		} else {
			xRot = (float) Math.atan2(-m21, m11);
			zRot = 0.0F;
		}

		part.x = matrix.m30 * 16.0F;
		part.y = matrix.m31 * 16.0F;
		part.z = matrix.m32 * 16.0F;
		part.xRot = xRot;
		part.yRot = yRot;
		part.zRot = zRot;
		part.xScale = scaleX;
		part.yScale = scaleY;
		part.zScale = scaleZ;
	}

	private static float length(float x, float y, float z) {
		return (float) Math.sqrt(x * x + y * y + z * z);
	}

	private static void copyPose(ModelPart from, ModelPart to) {
		if (from == null || to == null) {
			return;
		}

		to.x = from.x;
		to.y = from.y;
		to.z = from.z;
		to.xRot = from.xRot;
		to.yRot = from.yRot;
		to.zRot = from.zRot;
		to.xScale = from.xScale;
		to.yScale = from.yScale;
		to.zScale = from.zScale;
	}

	public static void pushPartSpace(PoseStack poseStack) {
		poseStack.translate(0.0F, VanillaPartCapture.MODEL_Y_OFFSET, 0.0F);
		poseStack.scale(-1.0F, -1.0F, 1.0F);
	}
}
