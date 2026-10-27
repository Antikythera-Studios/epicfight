package yesman.epicfight.api.client.model.rebake;

import javax.annotation.Nullable;

import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelPart;

public final class LiveSource {
	private final String canonicalName;

	@Nullable
	private ModelPart current;

	LiveSource(String canonicalName) {
		this.canonicalName = canonicalName;
	}

	public String canonicalName() {
		return this.canonicalName;
	}

	@Nullable
	public ModelPart current() {
		return this.current;
	}

	void bind(@Nullable ModelPart part) {
		this.current = part;
	}

	public void bindFrom(HumanoidModel<?> model) {
		this.bind(canonicalPart(model, this.canonicalName));
	}

	@Nullable
	public static ModelPart canonicalPart(HumanoidModel<?> model, String name) {
		return switch (name) {
			case "head" -> model.head;
			case "hat" -> model.hat;
			case "body" -> model.body;
			case "right_arm" -> model.rightArm;
			case "left_arm" -> model.leftArm;
			case "right_leg" -> model.rightLeg;
			case "left_leg" -> model.leftLeg;
			default -> null;
		};
	}
}
