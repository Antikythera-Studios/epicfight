package yesman.epicfight.api.client.model.rebake;

import javax.annotation.Nullable;

import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelPart;

public final class MeshCapture {
	private MeshCapture() {
	}

	private record NamedPart(String name, ModelPart part) {
	}

	@Nullable
	public static RawMesh captureHumanoid(HumanoidModel<?> model) {
		return captureHumanoid(model, null);
	}

	@Nullable
	public static RawMesh captureHumanoid(HumanoidModel<?> model, @Nullable java.util.List<LiveSource> sources) {
		return captureHumanoid(model, sources, null);
	}

	@Nullable
	public static RawMesh captureHumanoid(HumanoidModel<?> model, @Nullable java.util.List<LiveSource> sources,
			@Nullable HumanoidModel<?> wearer) {
		NamedPart[] parts = new NamedPart[] {
			new NamedPart("head", model.head),
			new NamedPart("hat", model.hat),
			new NamedPart("body", model.body),
			new NamedPart("right_arm", model.rightArm),
			new NamedPart("left_arm", model.leftArm),
			new NamedPart("right_leg", model.rightLeg),
			new NamedPart("left_leg", model.leftLeg),
		};

		boolean anyGeometry = false;

		for (NamedPart part : parts) {
			if (part.part() != null && VanillaPartCapture.hasGeometry(part.part())) {
				anyGeometry = true;

				break;
			}
		}

		if (!anyGeometry) {
			return null;
		}

		RawMesh mesh = new RawMesh();

		for (NamedPart part : parts) {
			if (part.part() == null || !part.part().visible) {
				continue;
			}

			LiveSource source = new LiveSource(part.name());
			source.bindFrom(model);

			if (sources != null) {
				sources.add(source);
			}

			VanillaPartCapture.capture(mesh, part.part(), "/" + part.name(), 0, source, anchorOn(wearer, part.name()));
		}

		mesh.finish();

		return mesh.isEmpty() ? null : mesh;
	}

	@Nullable
	private static net.minecraft.client.model.geom.PartPose anchorOn(@Nullable HumanoidModel<?> wearer, String name) {
		if (wearer == null) {
			return null;
		}

		ModelPart part = switch (name) {
			case "head" -> wearer.head;
			case "hat" -> wearer.hat;
			case "body" -> wearer.body;
			case "right_arm" -> wearer.rightArm;
			case "left_arm" -> wearer.leftArm;
			case "right_leg" -> wearer.rightLeg;
			case "left_leg" -> wearer.leftLeg;
			default -> null;
		};

		return part == null ? null : part.getInitialPose();
	}

}
