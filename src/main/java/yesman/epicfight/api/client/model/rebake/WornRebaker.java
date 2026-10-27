package yesman.epicfight.api.client.model.rebake;

import java.util.Map;
import java.util.Set;

import javax.annotation.Nullable;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.mojang.blaze3d.vertex.PoseStack;

import net.minecraft.client.Minecraft;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import yesman.epicfight.api.animation.Pose;
import yesman.epicfight.api.model.Armature;
import yesman.epicfight.main.EpicFightMod;

public final class WornRebaker {
	private static final Map<String, java.util.List<Baked>> CACHE = Maps.newHashMap();
	private static final Set<String> UNSUITABLE = Sets.newHashSet();

	private static final Map<String, Long> QUIET = Maps.newHashMap();

	private static final Set<String> DRAWS_ITSELF = Sets.newHashSet();

	private static final Map<String, Integer> SHAPE_WHEN_BAKED = Maps.newHashMap();

	private static final Map<String, Long> LOOK_AGAIN_AT = Maps.newHashMap();

	private static final long LOOK_AGAIN_INTERVAL_MS = 2000L;

	private static final long QUIET_RETRY_MS = 500L;

	private static final float SEPARATE_OBJECT_SHARE = 0.25F;

	private static final float PROBE_NUDGE = 8.0F;

	private static final float PROBE_MOVED = 0.1F;

	private WornRebaker() {
	}

	public record Baked(RebakedMesh mesh, net.minecraft.client.renderer.RenderType renderType) {
	}

	public static void clear() {
		CACHE.clear();
		UNSUITABLE.clear();
		QUIET.clear();
		DRAWS_ITSELF.clear();
		SHAPE_WHEN_BAKED.clear();
		LOOK_AGAIN_AT.clear();
		DREW_NOTHING.clear();
		WONT_SAY.clear();
		MOVED_ONCE.clear();
	}

	@Nullable
	public static java.util.List<Baked> bake(String rawKey, LivingEntity entity, Armature armature, LayerAttachment.AtRest render) {
		boolean slim = TemplateRig.isSlim(entity);
		String key = slim ? rawKey + "#slim" : rawKey;
		if (DRAWS_ITSELF.contains(key)) {
			return null;
		}

		long now = System.currentTimeMillis();
		java.util.List<Baked> cached = CACHE.get(key);

		if (cached != null) {
			return stillTheSameShape(key, entity, armature, render, now) ? cached : null;
		}

		if (skipFor(key, now)) {
			return null;
		}

		EntityRenderer<?> renderer = Minecraft.getInstance().getEntityRenderDispatcher().getRenderer(entity);
		boolean[] drewNothing = new boolean[1];
		java.util.List<Baked> baked = renderer instanceof LivingEntityRenderer<?, ?> living
			&& living.getModel() instanceof HumanoidModel<?> humanoid
				? bakeWith(key, humanoid, armature, render, drewNothing, slim) : null;

		if (baked == null || baked.isEmpty()) {
			remember(key, drewNothing[0], now);

			return null;
		}

		QUIET.remove(key);
		CACHE.put(key, baked);
		LOOK_AGAIN_AT.put(key, now + LOOK_AGAIN_INTERVAL_MS);

		return baked;
	}

	private static boolean stillTheSameShape(String key, LivingEntity entity, Armature armature,
			LayerAttachment.AtRest render, long now) {
		Long lookAgainAt = LOOK_AGAIN_AT.get(key);

		if (lookAgainAt == null || now < lookAgainAt) {
			return true;
		}

		LOOK_AGAIN_AT.put(key, now + LOOK_AGAIN_INTERVAL_MS);

		EntityRenderer<?> renderer = Minecraft.getInstance().getEntityRenderDispatcher().getRenderer(entity);

		if (!(renderer instanceof LivingEntityRenderer<?, ?> living)
				|| !(living.getModel() instanceof HumanoidModel<?> humanoid)) {
			return true;
		}

		RawMesh now2 = captureAtRest(humanoid, armature, render);

		if (now2 == null || now2.isEmpty()) {
			return true;
		}

		Integer wasShaped = SHAPE_WHEN_BAKED.get(key);
		int isShaped = shapeSignature(now2);

		if (wasShaped == null) {
			SHAPE_WHEN_BAKED.put(key, isShaped);

			return true;
		}

		if (wasShaped == isShaped) {
			MOVED_ONCE.remove(key);

			return true;
		}

		if (MOVED_ONCE.add(key)) {
			CACHE.remove(key);
			SHAPE_WHEN_BAKED.remove(key);
			LOOK_AGAIN_AT.remove(key);

			return false;
		}

		CACHE.remove(key);
		SHAPE_WHEN_BAKED.remove(key);
		LOOK_AGAIN_AT.remove(key);
		MOVED_ONCE.remove(key);
		DRAWS_ITSELF.add(key);

		return false;
	}

	static net.minecraft.client.renderer.RenderType asEntityFormat(net.minecraft.client.renderer.RenderType type) {
		if (type == null || type.format() == com.mojang.blaze3d.vertex.DefaultVertexFormat.NEW_ENTITY) {
			return type;
		}

		ResourceLocation sheet = CapturingVertexConsumer.textureOf(type);

		if (sheet == null) {
			return type;
		}

		boolean blends = type.toString().contains("translucent");
		net.minecraft.client.renderer.RenderType entity = blends
			? net.minecraft.client.renderer.RenderType.entityTranslucentCull(sheet)
			: net.minecraft.client.renderer.RenderType.entityCutoutNoCull(sheet);


		return entity;
	}


	static int shapeSignature(RawMesh mesh) {
		int signature = mesh.vertexCount();

		for (int v = 0; v < mesh.vertexCount(); v++) {
			signature = signature * 31 + Float.floatToIntBits(Math.round(mesh.px(v) * 4096.0F) / 4096.0F);
			signature = signature * 31 + Float.floatToIntBits(Math.round(mesh.py(v) * 4096.0F) / 4096.0F);
			signature = signature * 31 + Float.floatToIntBits(Math.round(mesh.pz(v) * 4096.0F) / 4096.0F);
		}

		return signature;
	}

	@Nullable
	private static final int GONE_FOR_GOOD = 3;

	private static final java.util.Map<String, Integer> DREW_NOTHING = com.google.common.collect.Maps.newHashMap();

	private static final java.util.Set<String> MOVED_ONCE = com.google.common.collect.Sets.newHashSet();

	private static final java.util.Set<String> WONT_SAY = com.google.common.collect.Sets.newHashSet();

	public static int verticesDrawnBy(LayerAttachment.AtRest render) {
		RawMesh drawn = new RawMesh();
		CapturingVertexConsumer.Buffers buffers = new CapturingVertexConsumer.Buffers(drawn);

		try (OffscreenCapture.Scope scope = OffscreenCapture.begin(buffers)) {
			render.render(new PoseStack(), buffers);
		} catch (Throwable t) {
			return -1;
		} finally {
			buffers.finish();
		}

		return drawn.vertexCount();
	}

	private static boolean drawsWhenLeftAlone(LayerAttachment.AtRest render) {
		RawMesh drawn = new RawMesh();
		CapturingVertexConsumer.Buffers buffers = new CapturingVertexConsumer.Buffers(drawn);

		try (OffscreenCapture.Scope scope = OffscreenCapture.begin(buffers)) {
			render.render(new PoseStack(), buffers);
		} catch (Throwable t) {
			return false;
		} finally {
			buffers.finish();
		}

		return drawn.vertexCount() > 0;
	}

	public static java.util.List<net.minecraft.client.renderer.RenderType> typesNow(String key,
			java.util.List<Baked> baked, LayerAttachment.AtRest render) {
		java.util.Set<net.minecraft.client.renderer.RenderType> kinds =
			java.util.Collections.newSetFromMap(new java.util.IdentityHashMap<> ());

		for (Baked pass : baked) {
			kinds.add(pass.renderType());
		}

		if (WONT_SAY.contains(key)) {
			return baked.stream().map(Baked::renderType).map(WornRebaker::asEntityFormat).toList();
		}

		java.util.List<net.minecraft.client.renderer.RenderType> asked = TypeProbe.askedFor(kinds.size(), render);

		if (asked == null) {
			DREW_NOTHING.remove(key);

			return baked.stream().map(Baked::renderType).map(WornRebaker::asEntityFormat).toList();
		}

		if (asked.isEmpty()) {
			int missed = DREW_NOTHING.merge(key, 1, Integer::sum);

			if (missed < GONE_FOR_GOOD) {
				return baked.stream().map(Baked::renderType).map(WornRebaker::asEntityFormat).toList();
			}

			if (missed == GONE_FOR_GOOD) {
				if (drawsWhenLeftAlone(render)) {
					WONT_SAY.add(key);

					return baked.stream().map(Baked::renderType).map(WornRebaker::asEntityFormat).toList();
				}

			}

			return null;
		}

		DREW_NOTHING.remove(key);

		java.util.List<net.minecraft.client.renderer.RenderType> now = Lists.newArrayList();

		for (int i = 0; i < baked.size(); i++) {
			now.add(asEntityFormat(refreshed(baked, i, asked)));
		}

		return now;
	}

	private static net.minecraft.client.renderer.RenderType refreshed(java.util.List<Baked> baked, int index,
			java.util.List<net.minecraft.client.renderer.RenderType> asked) {
		net.minecraft.client.renderer.RenderType was = baked.get(index).renderType();
		ResourceLocation sheet = CapturingVertexConsumer.textureOf(was);
		int rank = 0;

		for (int i = 0; i < index; i++) {
			if (java.util.Objects.equals(CapturingVertexConsumer.textureOf(baked.get(i).renderType()), sheet)) {
				rank++;
			}
		}

		java.util.List<net.minecraft.client.renderer.RenderType> onSheet = asked.stream()
			.filter(type -> java.util.Objects.equals(CapturingVertexConsumer.textureOf(type), sheet)).toList();

		if (onSheet.isEmpty()) {
			ResourceLocation drawnOn = asked.size() == 1 ? CapturingVertexConsumer.textureOf(asked.get(0)) : null;
			boolean contradicts = sheet != null && drawnOn != null && !sheet.equals(drawnOn);

			return asked.size() == 1 && !contradicts ? asked.get(0) : was;
		}

		return onSheet.get(Math.min(rank, onSheet.size() - 1));
	}

	public static String keyed(String base, int tint) {
		return tint == 0 ? base : base + "/" + Integer.toHexString(tint);
	}

	static boolean skipFor(String key, long now) {
		if (UNSUITABLE.contains(key)) {
			return true;
		}

		Long quietUntil = QUIET.get(key);

		return quietUntil != null && now < quietUntil;
	}

	static void remember(String key, boolean drewNothing, long now) {
		if (drewNothing) {
			QUIET.put(key, now + QUIET_RETRY_MS);
		} else {
			UNSUITABLE.add(key);
		}
	}

	@Nullable
	public static java.util.List<Baked> bakeWith(String key, HumanoidModel<?> humanoid, Armature armature, LayerAttachment.AtRest render) {
		return bakeWith(key, humanoid, armature, render, new boolean[1]);
	}

	@Nullable
	private static java.util.List<Baked> bakeWith(String key, HumanoidModel<?> humanoid, Armature armature,
			LayerAttachment.AtRest render, boolean[] drewNothing) {
		return bakeWith(key, humanoid, armature, render, drewNothing, false);
	}

	@Nullable
	private static java.util.List<Baked> bakeWith(String key, HumanoidModel<?> humanoid, Armature armature,
			LayerAttachment.AtRest render, boolean[] drewNothing, boolean slim) {
		try {
			TemplateRig rig = TemplateRig.get(armature.getJointNumber(), slim);

			if (rig == null) {
				return null;
			}

			RawMesh captured = new RawMesh();
			CapturingVertexConsumer.Buffers buffers = new CapturingVertexConsumer.Buffers(captured);
			ModelSnapshot snapshot = ModelSnapshot.of(humanoid);

			try (OffscreenCapture.Scope scope = OffscreenCapture.begin(buffers)) {
				ModelSnapshot.restToBindPose(humanoid);
				VanillaPoseSync.apply(humanoid, armature, armature.getPoseAsTransformMatrix(Pose.EMPTY_POSE, false));
				PoseStack atRest = new PoseStack();
				VanillaPoseSync.pushPartSpace(atRest);
				render.render(atRest, buffers);
			} catch (Throwable t) {

				return null;
			} finally {
				buffers.finish();

				if (snapshot != null) {
					snapshot.restore();
				}
			}

			if (captured.isEmpty()) {
				drewNothing[0] = true;

				return null;
			}

			java.util.List<Baked> shell = asShell(captured, rig, armature, key);

			if (shell != null) {

				return shell;
			}

			JointNames.Region[] probed = limbPerVertexByProbing(humanoid, armature, render, captured);

			if (probed == null) {

				return null;
			}

			float[][] weightOfVertex = bindByPiece(captured, rig, armature, probed);

			if (weightOfVertex == null) {

				return null;
			}

			if (!changesAnything(weightOfVertex, armature)) {
				return null;
			}

			java.util.List<Baked> baked = Lists.newArrayList();

			for (net.minecraft.client.renderer.RenderType type : renderTypesOf(captured)) {
				it.unimi.dsi.fastutil.ints.IntArrayList cameFrom = new it.unimi.dsi.fastutil.ints.IntArrayList();
				RawMesh subset = subsetFor(captured, type, cameFrom);
				SkinWeights weights = weightsFor(subset, cameFrom, weightOfVertex, armature.getJointNumber());

				if (subset.isEmpty() || weights == null) {
					continue;
				}

				baked.add(new Baked(MeshAssembler.assemble(subset, weights, "worn", null), type));
			}

			if (baked.isEmpty()) {
				return null;
			}

			float lowX = Float.MAX_VALUE, lowY = Float.MAX_VALUE, lowZ = Float.MAX_VALUE;
			float highX = -Float.MAX_VALUE, highY = -Float.MAX_VALUE, highZ = -Float.MAX_VALUE;
			java.util.TreeSet<String> bones = new java.util.TreeSet<> ();

			for (int v = 0; v < captured.vertexCount(); v++) {
				lowX = Math.min(lowX, captured.px(v)); highX = Math.max(highX, captured.px(v));
				lowY = Math.min(lowY, captured.py(v)); highY = Math.max(highY, captured.py(v));
				lowZ = Math.min(lowZ, captured.pz(v)); highZ = Math.max(highZ, captured.pz(v));

				float[] weights = weightOfVertex[v];
				int strongest = -1;

				for (int joint = 0; weights != null && joint < weights.length; joint++) {
					if (weights[joint] > 0.0F && (strongest < 0 || weights[joint] > weights[strongest])) {
						strongest = joint;
					}
				}

				if (strongest >= 0) {
					bones.add(armature.searchJointById(strongest) == null ? String.valueOf(strongest)
						: armature.searchJointById(strongest).getName());
				}
			}


			return baked;
		} catch (Throwable t) {

			return null;
		}
	}

	@Nullable
	private static float[][] bindByPiece(RawMesh mesh, TemplateRig rig, Armature armature, @Nullable JointNames.Region[] probed) {
		int[] piece = connectedPieces(mesh);
		Map<Integer, Boolean> byPiece = Maps.newLinkedHashMap();
		float[][] weightOfVertex = new float[mesh.vertexCount()][];

		for (int t = 0; t < mesh.triangleCount(); t++) {
			byPiece.putIfAbsent(piece[t], Boolean.TRUE);
		}

		Map<JointNames.Region, java.util.List<Integer>> byLimb = Maps.newLinkedHashMap();
		Map<JointNames.Region, Integer> paintPerLimb = Maps.newHashMap();
		int paintTotal = 0;

		for (Map.Entry<Integer, Boolean> entry : byPiece.entrySet()) {
			RawMesh only = copyTriangles(mesh, t -> piece[t] == entry.getKey(), null);
			JointNames.Region limb = probedLimbOf(mesh, piece, entry.getKey(), probed);

			int paint = paintedAmount(only);
			byLimb.computeIfAbsent(limb, key -> Lists.newArrayList()).add(entry.getKey());
			paintPerLimb.merge(limb, paint, Integer::sum);
			paintTotal += paint;
		}

		JointNames.Region held = null;
		int mostPaint = -1;

		for (Map.Entry<JointNames.Region, Integer> entry : paintPerLimb.entrySet()) {
			if (entry.getValue() > mostPaint) {
				mostPaint = entry.getValue();
				held = entry.getKey();
			}
		}

		if (probed == null && held != null && paintTotal > 0) {
			for (JointNames.Region limb : Lists.newArrayList(byLimb.keySet())) {
				if (limb == held || (float) paintPerLimb.getOrDefault(limb, 0) / paintTotal >= SEPARATE_OBJECT_SHARE) {
					continue;
				}

				byLimb.get(held).addAll(byLimb.remove(limb));
			}
		}

		for (Map.Entry<JointNames.Region, java.util.List<Integer>> entry : byLimb.entrySet()) {
			JointNames.Region limb = entry.getKey();
			java.util.List<Integer> pieces = entry.getValue();
			RawMesh together = copyTriangles(mesh, t -> pieces.contains(piece[t]), null);
			float[] weights = restingWeightsIn(together, rig, limb, armature.getJointNumber());

			if (weights == null) {
				weights = restingWeightsIn(together, rig, JointNames.Region.UNKNOWN, armature.getJointNumber());
			}

			if (weights == null) {

				return null;
			}

			for (int t = 0; t < mesh.triangleCount(); t++) {
				if (!pieces.contains(piece[t])) {
					continue;
				}

				for (int corner = 0; corner < 3; corner++) {
					weightOfVertex[mesh.triangleVertex(t, corner)] = weights;
				}
			}
		}

		for (float[] weights : weightOfVertex) {
			if (weights == null) {
				return null;
			}
		}

		return weightOfVertex;
	}

	@Nullable
	private static JointNames.Region[] limbPerVertexByProbing(HumanoidModel<?> humanoid, Armature armature,
			LayerAttachment.AtRest render, RawMesh atRest) {
		record Probe(String limbName, ModelPart part, JointNames.Region limb) {
		}

		Probe[] probes = {
			new Probe("head", humanoid.head, JointNames.Region.HEAD_REGION),
			new Probe("body", humanoid.body, JointNames.Region.TORSO_REGION),
			new Probe("rightArm", humanoid.rightArm, JointNames.Region.ARM_RIGHT),
			new Probe("leftArm", humanoid.leftArm, JointNames.Region.ARM_LEFT),
			new Probe("rightLeg", humanoid.rightLeg, JointNames.Region.LEG_RIGHT),
			new Probe("leftLeg", humanoid.leftLeg, JointNames.Region.LEG_LEFT),
		};

		JointNames.Region[] limbs = new JointNames.Region[atRest.vertexCount()];
		boolean anyMoved = false;

		RawMesh again = captureAtRest(humanoid, armature, render);

		if (again == null || again.vertexCount() != atRest.vertexCount() || !sameGeometry(atRest, again)) {

			return null;
		}

		for (Probe probe : probes) {
			if (probe.part() == null) {
				continue;
			}

			RawMesh moved = new RawMesh();
			CapturingVertexConsumer.Buffers buffers = new CapturingVertexConsumer.Buffers(moved);
			ModelSnapshot snapshot = ModelSnapshot.of(humanoid);

			try (OffscreenCapture.Scope scope = OffscreenCapture.begin(buffers)) {
				VanillaPoseSync.apply(humanoid, armature, armature.getPoseAsTransformMatrix(Pose.EMPTY_POSE, false));
				probe.part().x += PROBE_NUDGE;
				PoseStack stack = new PoseStack();
				VanillaPoseSync.pushPartSpace(stack);
				render.render(stack, buffers);
			} catch (Throwable t) {
				return null;
			} finally {
				buffers.finish();

				if (snapshot != null) {
					snapshot.restore();
				}
			}

			if (moved.vertexCount() != atRest.vertexCount()) {
				return null;
			}

			for (int v = 0; v < limbs.length; v++) {
				float dx = moved.px(v) - atRest.px(v);
				float dy = moved.py(v) - atRest.py(v);
				float dz = moved.pz(v) - atRest.pz(v);

				if (dx * dx + dy * dy + dz * dz > PROBE_MOVED * PROBE_MOVED && limbs[v] == null) {
					limbs[v] = probe.limb();
					anyMoved = true;
				}
			}
		}

		return anyMoved ? limbs : null;
	}

	private static boolean isRigid(float[] weights) {
		float strongest = 0.0F;

		for (float weight : weights) {
			strongest = Math.max(strongest, weight);
		}

		return strongest >= RebakeConfig.PURE_BAND_WEIGHT;
	}

	@Nullable
	private static RawMesh captureAtRest(HumanoidModel<?> humanoid, Armature armature, LayerAttachment.AtRest render) {
		RawMesh out = new RawMesh();
		CapturingVertexConsumer.Buffers buffers = new CapturingVertexConsumer.Buffers(out);
		ModelSnapshot snapshot = ModelSnapshot.of(humanoid);

		try (OffscreenCapture.Scope scope = OffscreenCapture.begin(buffers)) {
			VanillaPoseSync.apply(humanoid, armature, armature.getPoseAsTransformMatrix(Pose.EMPTY_POSE, false));
			PoseStack stack = new PoseStack();
			VanillaPoseSync.pushPartSpace(stack);
			render.render(stack, buffers);
		} catch (Throwable t) {
			return null;
		} finally {
			buffers.finish();

			if (snapshot != null) {
				snapshot.restore();
			}
		}

		return out;
	}

	private static boolean sameGeometry(RawMesh first, RawMesh second) {
		if (first.vertexCount() != second.vertexCount()) {
			return false;
		}

		for (int v = 0; v < first.vertexCount(); v++) {
			if (Math.abs(first.px(v) - second.px(v)) > 1.0E-4F
					|| Math.abs(first.py(v) - second.py(v)) > 1.0E-4F
					|| Math.abs(first.pz(v) - second.pz(v)) > 1.0E-4F) {
				return false;
			}
		}

		return true;
	}

	@Nullable
	private static java.util.List<Baked> asShell(RawMesh captured, TemplateRig rig, Armature armature, String key) {
		java.util.List<Baked> baked = Lists.newArrayList();
		boolean anyBends = false;

		for (net.minecraft.client.renderer.RenderType type : renderTypesOf(captured)) {
			RawMesh subset = subsetFor(captured, type, null);

			if (subset.isEmpty()) {
				continue;
			}

			int[] piece = connectedPieces(subset);
			Set<Integer> ids = Sets.newLinkedHashSet();

			for (int t = 0; t < subset.triangleCount(); t++) {
				ids.add(piece[t]);
			}

			TextureCoverage coverage = TextureCoverage.of(firstTextureOf(subset));

			try {
				for (int id : ids) {
					RawMesh one = copyTriangles(subset, t -> piece[t] == id, null);
					PartSubstitution.Result rebuilt = PartSubstitution.substitute(one, rig, armature.getJointNumber(), coverage);

					if (rebuilt == null) {

						return null;
					}

					anyBends |= bends(rebuilt.weights(), armature.getJointNumber());
					baked.add(new Baked(MeshAssembler.assemble(rebuilt.mesh(), rebuilt.weights(), "worn shell", null), type));
				}
			} finally {
				if (coverage != null) {
					coverage.close();
				}
			}
		}

		return baked.isEmpty() || !anyBends ? null : baked;
	}

	private static boolean bends(SkinWeights weights, int jointCount) {
		Set<Integer> joints = Sets.newHashSet();

		for (int vertex = 0; vertex < weights.vertexCount; vertex++) {
			int base = vertex * TemplateRig.MAX_INFLUENCES;

			for (int i = 0; i < weights.counts[vertex]; i++) {
				joints.add(weights.jointIds[base + i]);
			}
		}

		Map<JointNames.Region, Integer> perLimb = Maps.newHashMap();

		for (int joint : joints) {
			for (JointNames.Region region : JointNames.Region.values()) {
				if (region != JointNames.Region.UNKNOWN && region.permits(joint)) {
					perLimb.merge(region, 1, Integer::sum);

					break;
				}
			}
		}

		return perLimb.values().stream().anyMatch(count -> count > 1);
	}

	private static boolean changesAnything(float[][] weightOfVertex, Armature armature) {
		Set<Integer> placing = Sets.newHashSet();

		for (String name : new String[] { "Head", "Chest", "Arm_R", "Arm_L", "Thigh_R", "Thigh_L" }) {
			yesman.epicfight.api.animation.Joint joint = armature.searchJointByName(name);

			if (joint != null) {
				placing.add(joint.getId());
			}
		}

		for (float[] dense : weightOfVertex) {
			int carrying = 0;
			int only = -1;

			for (int joint = 0; joint < dense.length; joint++) {
				if (dense[joint] > 1.0E-4F) {
					carrying++;
					only = joint;
				}
			}

			if (carrying != 1 || !placing.contains(only)) {
				return true;
			}
		}

		return false;
	}

	@Nullable
	private static JointNames.Region probedLimbOf(RawMesh mesh, int[] piece, int id, JointNames.Region[] probed) {
		Map<JointNames.Region, Integer> tally = Maps.newHashMap();

		for (int t = 0; t < mesh.triangleCount(); t++) {
			if (piece[t] != id) {
				continue;
			}

			for (int corner = 0; corner < 3; corner++) {
				JointNames.Region limb = probed[mesh.triangleVertex(t, corner)];

				if (limb != null) {
					tally.merge(limb, 1, Integer::sum);
				}
			}
		}

		JointNames.Region best = null;
		int most = 0;

		for (Map.Entry<JointNames.Region, Integer> entry : tally.entrySet()) {
			if (entry.getValue() > most) {
				most = entry.getValue();
				best = entry.getKey();
			}
		}

		return best;
	}

	private static int paintedAmount(RawMesh piece) {
		int[] painted = { 0 };

		for (RawMesh.Group group : piece.groups()) {
			TextureCoverage coverage = TextureCoverage.of(group.texture);

			if (coverage == null) {
				continue;
			}

			try {
				coverage.forEachPaintedSample(piece, group.firstTriangle, group.triangleCount, (x, y, z) -> painted[0]++);
			} finally {
				coverage.close();
			}
		}

		return painted[0];
	}

	private static int[] connectedPieces(RawMesh mesh) {
		int triangles = mesh.triangleCount();
		int[] parent = new int[triangles];

		for (int t = 0; t < triangles; t++) {
			parent[t] = t;
		}

		Map<Long, Integer> seen = Maps.newHashMap();

		for (int t = 0; t < triangles; t++) {
			for (int corner = 0; corner < 3; corner++) {
				int v = mesh.triangleVertex(t, corner);
				Integer other = seen.putIfAbsent(positionKey(mesh.px(v), mesh.py(v), mesh.pz(v)), t);

				if (other != null) {
					int a = find(parent, t), b = find(parent, other);

					if (a != b) {
						parent[b] = a;
					}
				}
			}
		}

		int[] piece = new int[triangles];

		for (int t = 0; t < triangles; t++) {
			piece[t] = find(parent, t);
		}

		return piece;
	}

	private static long positionKey(float x, float y, float z) {
		long qx = Math.round(x * 8192.0F) & 0x1FFFFFL;
		long qy = Math.round(y * 8192.0F) & 0x1FFFFFL;
		long qz = Math.round(z * 8192.0F) & 0x1FFFFFL;

		return qx | (qy << 21) | (qz << 42);
	}

	private static int find(int[] parent, int i) {
		while (parent[i] != i) {
			parent[i] = parent[parent[i]];
			i = parent[i];
		}

		return i;
	}

	@Nullable
	private static float[] restingWeightsIn(RawMesh piece, TemplateRig rig, JointNames.Region limb, int jointCount) {
		if (limb == null) {
			return null;
		}

		TemplateRig.Match scratch = rig.newMatch();
		float[] total = new float[jointCount];
		int[] taken = { 0 };
		int[] rigidUnderneath = { 0 };

		for (RawMesh.Group group : piece.groups()) {
			TextureCoverage coverage = TextureCoverage.of(group.texture);

			if (coverage == null) {
				continue;
			}

			try {
				coverage.forEachPaintedSample(piece, group.firstTriangle, group.triangleCount, (x, y, z) -> {
					if (rig.closestPoint(x, y, z, limb, scratch)) {
						for (int joint = 0; joint < jointCount && joint < scratch.weights.length; joint++) {
							total[joint] += scratch.weights[joint];
						}

						if (isRigid(scratch.weights)) {
							rigidUnderneath[0]++;
						}

						taken[0]++;
					}
				});
			} finally {
				coverage.close();
			}
		}

		if (taken[0] == 0) {
			for (int v = 0; v < piece.vertexCount(); v++) {
				if (rig.closestPoint(piece.px(v), piece.py(v), piece.pz(v), limb, scratch)) {
					for (int joint = 0; joint < jointCount && joint < scratch.weights.length; joint++) {
						total[joint] += scratch.weights[joint];
					}

					if (isRigid(scratch.weights)) {
						rigidUnderneath[0]++;
					}

					taken[0]++;
				}
			}
		}

		if (taken[0] == 0) {
			return null;
		}

		float sum = 0.0F;

		for (float weight : total) {
			sum += weight;
		}

		if (sum <= 1.0E-5F) {
			return null;
		}

		for (int joint = 0; joint < jointCount; joint++) {
			total[joint] /= sum;
		}

		if (rigidUnderneath[0] * 2 >= taken[0]) {
			int strongest = 0;

			for (int joint = 1; joint < jointCount; joint++) {
				if (total[joint] > total[strongest]) {
					strongest = joint;
				}
			}

			java.util.Arrays.fill(total, 0.0F);
			total[strongest] = 1.0F;
		}

		return total;
	}

	private static java.util.List<net.minecraft.client.renderer.RenderType> renderTypesOf(RawMesh mesh) {
		java.util.List<net.minecraft.client.renderer.RenderType> types = Lists.newArrayList();

		for (RawMesh.Group group : mesh.groups()) {
			if (!group.isEmpty() && group.renderType != null && !types.contains(group.renderType)) {
				types.add(group.renderType);
			}
		}

		return types;
	}

	private static RawMesh subsetFor(RawMesh mesh, net.minecraft.client.renderer.RenderType type, it.unimi.dsi.fastutil.ints.IntArrayList cameFrom) {
		Set<Integer> keep = Sets.newHashSet();

		for (RawMesh.Group group : mesh.groups()) {
			if (type.equals(group.renderType)) {
				for (int t = group.firstTriangle; t < group.firstTriangle + group.triangleCount; t++) {
					keep.add(t);
				}
			}
		}

		return copyTriangles(mesh, keep::contains, cameFrom);
	}

	private static RawMesh copyTriangles(RawMesh mesh, java.util.function.IntPredicate wanted,
			@Nullable it.unimi.dsi.fastutil.ints.IntArrayList cameFrom) {
		RawMesh into = new RawMesh();
		into.beginGroup("piece").texture = firstTextureOf(mesh);
		int[] triangle = new int[3];

		for (int t = 0; t < mesh.triangleCount(); t++) {
			if (!wanted.test(t)) {
				continue;
			}

			for (int corner = 0; corner < 3; corner++) {
				int v = mesh.triangleVertex(t, corner);
				triangle[corner] = into.addVertex(mesh.px(v), mesh.py(v), mesh.pz(v),
					mesh.nx(v), mesh.ny(v), mesh.nz(v), mesh.u(v), mesh.v(v),
					mesh.red(v), mesh.green(v), mesh.blue(v), mesh.alpha(v), mesh.emissive(v));

				if (cameFrom != null) {
					cameFrom.add(v);
				}
			}

			into.addTriangle(triangle[0], triangle[1], triangle[2]);
		}

		into.finish();

		return into;
	}

	@Nullable
	private static SkinWeights weightsFor(RawMesh subset, it.unimi.dsi.fastutil.ints.IntArrayList cameFrom,
			float[][] weightOfVertex, int jointCount) {
		if (subset.vertexCount() == 0 || cameFrom.size() != subset.vertexCount()) {
			return null;
		}

		SkinWeights weights = new SkinWeights(subset.vertexCount());

		for (int vertex = 0; vertex < subset.vertexCount(); vertex++) {
			float[] dense = weightOfVertex[cameFrom.getInt(vertex)];

			if (dense == null) {
				return null;
			}

			weights.store(vertex, dense);
		}

		return weights;
	}

	@Nullable
	private static ResourceLocation firstTextureOf(RawMesh mesh) {
		for (RawMesh.Group group : mesh.groups()) {
			if (group.texture != null) {
				return group.texture;
			}
		}

		return null;
	}

}
