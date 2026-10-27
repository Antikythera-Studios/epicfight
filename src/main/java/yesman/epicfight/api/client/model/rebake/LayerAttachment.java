package yesman.epicfight.api.client.model.rebake;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.annotation.Nullable;

import com.mojang.blaze3d.vertex.PoseStack;

import net.minecraft.client.Minecraft;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.world.entity.LivingEntity;
import yesman.epicfight.api.animation.Joint;
import yesman.epicfight.api.animation.Pose;
import yesman.epicfight.api.client.model.rebake.JointNames.Region;
import yesman.epicfight.api.model.Armature;
import yesman.epicfight.api.utils.math.OpenMatrix4f;
import yesman.epicfight.main.EpicFightMod;

public final class LayerAttachment {
	public static final int[] NONE = { -1, -1 };

	private static final float LOCALISED_SHARE = 0.8F;

	private static final float WORN_LIMB_SHARE = 0.6F;

	private static final Map<Region, String> PLACED_BY = Map.of(
		Region.HEAD_REGION, "Head",
		Region.TORSO_REGION, "Chest",
		Region.ARM_RIGHT, "Arm_R",
		Region.ARM_LEFT, "Arm_L",
		Region.LEG_RIGHT, "Thigh_R",
		Region.LEG_LEFT, "Thigh_L");

	private static final Map<Object, int[]> CACHE = new ConcurrentHashMap<> ();

	private LayerAttachment() {
	}

	public interface AtRest {
		void render(PoseStack poseStack, MultiBufferSource buffers) throws Exception;
	}

	public static void clear() {
		CACHE.clear();
	}

	public static int[] of(Object key, LivingEntity entity, Armature armature, AtRest render) {
		int[] known = CACHE.get(key);

		if (known != null) {
			return known;
		}

		int[] decided = decide(entity, armature, render);
		CACHE.put(key, decided);

		return decided;
	}

	private static int[] decide(LivingEntity entity, Armature armature, AtRest render) {
		try {
			TemplateRig rig = TemplateRig.get(armature.getJointNumber());
			EntityRenderer<?> renderer = Minecraft.getInstance().getEntityRenderDispatcher().getRenderer(entity);

			if (rig == null || !(renderer instanceof LivingEntityRenderer<?, ?> living)
					|| !(living.getModel() instanceof HumanoidModel<?> humanoid)) {
				return NONE;
			}

			RawMesh captured = new RawMesh();
			CapturingVertexConsumer.Buffers buffers = new CapturingVertexConsumer.Buffers(captured);
			ModelSnapshot snapshot = ModelSnapshot.of(humanoid);

			try {
				VanillaPoseSync.apply(humanoid, armature, armature.getPoseAsTransformMatrix(Pose.EMPTY_POSE, false));
				PoseStack atRest = new PoseStack();
				VanillaPoseSync.pushPartSpace(atRest);
				render.render(atRest, buffers);
			} catch (Throwable t) {
				return NONE;
			} finally {
				buffers.finish();

				if (snapshot != null) {
					snapshot.restore();
				}
			}

			return attachmentOf(captured, rig, armature);
		} catch (Throwable t) {
			return NONE;
		}
	}

	public static Region limbOf(RawMesh captured, TemplateRig rig) {
		if (captured.vertexCount() == 0) {
			return Region.UNKNOWN;
		}

		Tally tally = new Tally(rig);

		for (RawMesh.Group group : captured.groups()) {
			TextureCoverage coverage = TextureCoverage.of(group.texture);

			if (coverage == null) {
				continue;
			}

			try {
				coverage.forEachPaintedSample(captured, group.firstTriangle, group.triangleCount, tally::add);
			} finally {
				coverage.close();
			}
		}

		if (tally.placed == 0) {
			for (int v = 0; v < captured.vertexCount(); v++) {
				tally.add(captured.px(v), captured.py(v), captured.pz(v));
			}
		}

		return tally.limb();
	}

	public static Region wornLimbOf(RawMesh captured, TemplateRig rig) {
		if (captured.vertexCount() == 0) {
			return Region.UNKNOWN;
		}

		Tally tally = new Tally(rig);

		for (RawMesh.Group group : captured.groups()) {
			TextureCoverage coverage = TextureCoverage.of(group.texture);

			if (coverage == null) {
				continue;
			}

			try {
				coverage.forEachPaintedSample(captured, group.firstTriangle, group.triangleCount, tally::add);
			} finally {
				coverage.close();
			}
		}

		if (tally.placed == 0) {
			for (int v = 0; v < captured.vertexCount(); v++) {
				tally.add(captured.px(v), captured.py(v), captured.pz(v));
			}
		}

		return tally.wornLimb();
	}

	public static int[] attachmentOf(RawMesh captured, TemplateRig rig, Armature armature) {
		if (captured.vertexCount() == 0) {
			return NONE;
		}

		Tally tally = new Tally(rig);

		for (RawMesh.Group group : captured.groups()) {
			TextureCoverage coverage = TextureCoverage.of(group.texture);

			if (coverage == null) {
				continue;
			}

			try {
				coverage.forEachPaintedSample(captured, group.firstTriangle, group.triangleCount, tally::add);
			} finally {
				coverage.close();
			}
		}

		if (tally.placed == 0) {
			for (int v = 0; v < captured.vertexCount(); v++) {
				tally.add(captured.px(v), captured.py(v), captured.pz(v));
			}
		}

		return tally.resolve(armature);
	}

	private static final class Tally {
		private final TemplateRig rig;
		private final TemplateRig.Match scratch;
		private final Map<Integer, Integer> byJoint = new java.util.HashMap<> ();
		private double cx, cy, cz;
		private int placed;

		private static Region regionOf(int joint) {
			for (Region region : Region.values()) {
				if (region != Region.UNKNOWN && region.permits(joint)) {
					return region;
				}
			}

			return Region.UNKNOWN;
		}

		private Tally(TemplateRig rig) {
			this.rig = rig;
			this.scratch = rig.newMatch();
		}

		void add(float x, float y, float z) {
			int joint = this.rig.dominantJointAt(x, y, z, Region.UNKNOWN, this.scratch);

			if (joint < 0) {
				return;
			}

			this.byJoint.merge(joint, 1, Integer::sum);
			this.cx += x;
			this.cy += y;
			this.cz += z;
			this.placed++;
		}

		Region wornLimb() {
			if (this.placed == 0) {
				return Region.UNKNOWN;
			}

			Region centred = JointNames.regionByPosition((float) (this.cx / this.placed),
				(float) (this.cy / this.placed), (float) (this.cz / this.placed));

			if (centred == Region.UNKNOWN) {
				centred = this.limb();
			}

			if (centred == Region.UNKNOWN) {
				return Region.UNKNOWN;
			}

			int inLimb = 0;

			for (Map.Entry<Integer, Integer> entry : this.byJoint.entrySet()) {
				if (regionOf(entry.getKey()) == centred) {
					inLimb += entry.getValue();
				}
			}

			return (float) inLimb / this.placed < WORN_LIMB_SHARE ? Region.UNKNOWN : centred;
		}

		Region limb() {
			if (this.placed == 0) {
				return Region.UNKNOWN;
			}

			Map<Region, Integer> byRegion = new java.util.HashMap<> ();

			for (Map.Entry<Integer, Integer> entry : this.byJoint.entrySet()) {
				byRegion.merge(regionOf(entry.getKey()), entry.getValue(), Integer::sum);
			}

			Region best = Region.UNKNOWN;
			int most = 0;

			for (Map.Entry<Region, Integer> entry : byRegion.entrySet()) {
				if (entry.getValue() > most) {
					most = entry.getValue();
					best = entry.getKey();
				}
			}

			return (float) most / this.placed < LOCALISED_SHARE ? Region.UNKNOWN : best;
		}

		int[] resolve(Armature armature) {
			if (this.placed == 0) {
				return NONE;
			}

			Map<Region, Integer> byRegion = new java.util.HashMap<> ();

			for (Map.Entry<Integer, Integer> entry : this.byJoint.entrySet()) {
				byRegion.merge(regionOf(entry.getKey()), entry.getValue(), Integer::sum);
			}

			Region limb = null;
			int inLimb = 0;

			for (Map.Entry<Region, Integer> entry : byRegion.entrySet()) {
				if (entry.getValue() > inLimb) {
					inLimb = entry.getValue();
					limb = entry.getKey();
				}
			}

			if (limb == null || limb == Region.UNKNOWN || (float) inLimb / this.placed < LOCALISED_SHARE) {
				return NONE;
			}

			int owner = -1;
			int best = 0;

			for (Map.Entry<Integer, Integer> entry : this.byJoint.entrySet()) {
				if (regionOf(entry.getKey()) == limb && entry.getValue() > best) {
					best = entry.getValue();
					owner = entry.getKey();
				}
			}

			if (owner < 0) {
				return NONE;
			}

			String placedByName = PLACED_BY.get(limb);

			if (placedByName == null) {
				return NONE;
			}

			Joint placedBy = armature.searchJointByName(placedByName);

			if (placedBy == null || placedBy.getId() == owner) {
				return NONE;
			}

			Joint ownerJoint = armature.searchJointById(owner);

			return new int[] { placedBy.getId(), owner };
		}
	}

	public static void apply(PoseStack poseStack, @Nullable int[] attachment, OpenMatrix4f[] poses) {
		if (attachment == null || attachment[0] < 0 || attachment[0] >= poses.length || attachment[1] >= poses.length) {
			return;
		}

		OpenMatrix4f correction = OpenMatrix4f.mul(poses[attachment[1]], OpenMatrix4f.invert(poses[attachment[0]], null), null);
		poseStack.mulPoseMatrix(OpenMatrix4f.exportToMojangMatrix(correction));
	}
}
