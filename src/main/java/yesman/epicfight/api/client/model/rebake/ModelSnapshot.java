package yesman.epicfight.api.client.model.rebake;

import java.util.List;
import java.util.Map;

import javax.annotation.Nullable;

import com.google.common.collect.Lists;

import net.minecraft.client.model.Model;
import net.minecraft.client.model.geom.ModelPart;

public final class ModelSnapshot {
	private final List<ModelPart> parts = Lists.newArrayList();
	private final List<float[]> transforms = Lists.newArrayList();
	private final List<boolean[]> flags = Lists.newArrayList();

	@Nullable
	private net.minecraft.client.model.HumanoidModel<?> humanoid;
	private boolean young;
	private boolean crouching;
	private boolean riding;
	private float attackTime;
	private net.minecraft.client.model.HumanoidModel.ArmPose leftArmPose;
	private net.minecraft.client.model.HumanoidModel.ArmPose rightArmPose;

	private ModelSnapshot() {
	}

	@Nullable
	public static ModelSnapshot of(@Nullable Model model) {
		if (!(model instanceof net.minecraft.client.model.HumanoidModel<?> humanoid)) {
			return null;
		}

		ModelSnapshot snapshot = new ModelSnapshot();
		snapshot.humanoid = humanoid;
		snapshot.young = humanoid.young;
		snapshot.crouching = humanoid.crouching;
		snapshot.riding = humanoid.riding;
		snapshot.attackTime = humanoid.attackTime;
		snapshot.leftArmPose = humanoid.leftArmPose;
		snapshot.rightArmPose = humanoid.rightArmPose;

		for (ModelPart root : new ModelPart[] { humanoid.head, humanoid.hat, humanoid.body,
				humanoid.rightArm, humanoid.leftArm, humanoid.rightLeg, humanoid.leftLeg }) {
			snapshot.record(root);
		}

		return snapshot.parts.isEmpty() ? null : snapshot;
	}

	

	private void record(@Nullable ModelPart part) {
		if (part == null) {
			return;
		}

		this.parts.add(part);
		this.transforms.add(new float[] {
			part.x, part.y, part.z, part.xRot, part.yRot, part.zRot, part.xScale, part.yScale, part.zScale,
		});
		this.flags.add(new boolean[] { part.visible, part.skipDraw });

		for (Map.Entry<String, ModelPart> child : part.children.entrySet()) {
			this.record(child.getValue());
		}
	}

	public static void restToBindPose(net.minecraft.client.model.HumanoidModel<?> model) {
		model.young = false;
		model.crouching = false;
		model.riding = false;
		model.attackTime = 0.0F;
		model.leftArmPose = net.minecraft.client.model.HumanoidModel.ArmPose.EMPTY;
		model.rightArmPose = net.minecraft.client.model.HumanoidModel.ArmPose.EMPTY;

		for (ModelPart part : new ModelPart[] { model.head, model.hat, model.body,
				model.rightArm, model.leftArm, model.rightLeg, model.leftLeg }) {
			if (part != null) {
				part.loadPose(part.getInitialPose());
			}
		}
	}

	public void restore() {
		if (this.humanoid != null) {
			this.humanoid.young = this.young;
			this.humanoid.crouching = this.crouching;
			this.humanoid.riding = this.riding;
			this.humanoid.attackTime = this.attackTime;
			this.humanoid.leftArmPose = this.leftArmPose;
			this.humanoid.rightArmPose = this.rightArmPose;
		}

		for (int i = 0; i < this.parts.size(); i++) {
			ModelPart part = this.parts.get(i);
			float[] transform = this.transforms.get(i);
			boolean[] flag = this.flags.get(i);

			part.x = transform[0];
			part.y = transform[1];
			part.z = transform[2];
			part.xRot = transform[3];
			part.yRot = transform[4];
			part.zRot = transform[5];
			part.xScale = transform[6];
			part.yScale = transform[7];
			part.zScale = transform[8];
			part.visible = flag[0];
			part.skipDraw = flag[1];
		}
	}
}
