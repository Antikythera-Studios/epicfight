package yesman.epicfight.api.client.model.rebake;

import java.util.List;
import java.util.Map;

import javax.annotation.Nullable;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.mojang.blaze3d.vertex.PoseStack;

import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.Model;
import net.minecraft.client.renderer.entity.layers.HumanoidArmorLayer;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.ForgeRegistries;
import yesman.epicfight.api.client.model.SkinnedMesh;
import yesman.epicfight.gameasset.Armatures;
import yesman.epicfight.main.EpicFightMod;

public final class ArmorRebaker {
	private ArmorRebaker() {
	}
	
	record CacheKey(ResourceLocation item, EquipmentSlot slot, int tint, boolean slim, String layer, int shape,
			String sheet) {
	}

	public static String sheetOf(LivingEntity entity, ItemStack stack, EquipmentSlot slot) {
		try {
			return String.valueOf(net.minecraftforge.client.ForgeHooksClient.getArmorTexture(entity, stack,
				"minecraft:textures/models/armor/" + slot.getName() + ".png", slot, null));
		} catch (Exception e) {
			return "-";
		}
	}

	public static int shapeOf(@Nullable Model armorModel) {
		if (!(armorModel instanceof HumanoidModel<?> humanoid)) {
			return 0;
		}

		int shape = 1;

		for (net.minecraft.client.model.geom.ModelPart part : new net.minecraft.client.model.geom.ModelPart[] {
				humanoid.head, humanoid.hat, humanoid.body, humanoid.rightArm, humanoid.leftArm,
				humanoid.rightLeg, humanoid.leftLeg }) {
			shape = shape * 31 + shapeOfPart(part);
		}

		return shape;
	}

	private static int shapeOfPart(@Nullable net.minecraft.client.model.geom.ModelPart part) {
		if (part == null) {
			return 0;
		}

		int shape = 17;

		for (net.minecraft.client.model.geom.ModelPart.Cube cube : part.cubes) {
			for (net.minecraft.client.model.geom.ModelPart.Polygon polygon : cube.polygons) {
				if (polygon == null) {
					continue;
				}

				for (net.minecraft.client.model.geom.ModelPart.Vertex vertex : polygon.vertices) {
					shape = shape * 31 + Float.floatToIntBits(vertex.pos.x());
					shape = shape * 31 + Float.floatToIntBits(vertex.pos.y());
					shape = shape * 31 + Float.floatToIntBits(vertex.pos.z());
				}
			}
		}

		for (Map.Entry<String, net.minecraft.client.model.geom.ModelPart> child : part.children.entrySet()) {
			shape = shape * 31 + child.getKey().hashCode();
			shape = shape * 31 + shapeOfPart(child.getValue());
		}

		return shape;
	}

	private static String layerNameOf(@Nullable RenderLayer<?, ?> layer) {
		return layer == null ? "-" : layer.getClass().getName();
	}

	public static int tintOf(ItemStack stack) {
		return stack.getItem() instanceof net.minecraft.world.item.DyeableLeatherItem dyeable ? dyeable.getColor(stack) : 0;
	}
	
	private static final Map<CacheKey, RebakedMesh> CACHE = Maps.newHashMap();
	
	private static final Map<CacheKey, Attempt> FAILURES = Maps.newHashMap();

	private static final java.util.Set<CacheKey> NOT_DRAWN = com.google.common.collect.Sets.newHashSet();


	private record Attempt(int count, long lastTried) {
	}

	private static final int EAGER_ATTEMPTS = 6;

	private static final long RETRY_INTERVAL_MS = 750L;

	private static final long SLOW_RETRY_INTERVAL_MS = 4000L;

	private static long retryDelay(int attempts) {
		if (attempts < EAGER_ATTEMPTS) {
			return RETRY_INTERVAL_MS;
		}

		return SLOW_RETRY_INTERVAL_MS;
	}
	
	private static boolean capturing;
	
	public static void exportModels(java.io.File resourcePackDirectory) throws java.io.IOException {
		java.io.File zipFile = new java.io.File(resourcePackDirectory, "epicfight_custom_armors.zip");

		try (java.util.zip.ZipOutputStream out = new java.util.zip.ZipOutputStream(new java.io.FileOutputStream(zipFile))) {
			com.google.gson.Gson gson = new com.google.gson.GsonBuilder().create();

			for (Map.Entry<CacheKey, RebakedMesh> entry : CACHE.entrySet()) {
				CacheKey baked = entry.getKey();
				String name = baked.item().getPath() + "_" + baked.slot().getName();
				out.putNextEntry(new java.util.zip.ZipEntry(String.format("assets/%s/animmodels/armor/%s.json",
					baked.item().getNamespace(), name)));
				out.write(gson.toJson(entry.getValue().toJsonObject()).getBytes());
				out.closeEntry();
			}

			com.google.gson.JsonObject pack = new com.google.gson.JsonObject();
			pack.addProperty("description", "epicfight_custom_armor_models");
			pack.addProperty("pack_format", net.minecraft.SharedConstants.getCurrentVersion()
				.getPackVersion(net.minecraft.server.packs.PackType.CLIENT_RESOURCES));

			com.google.gson.JsonObject root = new com.google.gson.JsonObject();
			root.add("pack", pack);
			out.putNextEntry(new java.util.zip.ZipEntry("pack.mcmeta"));
			out.write(new com.google.gson.GsonBuilder().setPrettyPrinting().create().toJson(root).getBytes());
			out.closeEntry();
		}
	}

	public static void clear() {
		CACHE.values().forEach(SkinnedMesh::destroy);
		CACHE.clear();
		NOT_DRAWN.clear();
		FAILURES.clear();
		TemplateRig.invalidate();
	}

	public static boolean hasGivenUp(LivingEntity entity, ItemStack stack, EquipmentSlot slot, RenderLayer<?, ?> vanillaLayer) {
		ResourceLocation id = ForgeRegistries.ITEMS.getKey(stack.getItem());

		if (id == null) {
			return true;
		}

		return false;
	}
	
	@Nullable
	public static RebakedMesh cached(LivingEntity entity, ItemStack stack, EquipmentSlot slot, RenderLayer<?, ?> vanillaLayer,
			@Nullable Model armorModel) {
		ResourceLocation id = ForgeRegistries.ITEMS.getKey(stack.getItem());
		
		return id == null ? null : CACHE.get(new CacheKey(id, slot, tintOf(stack), TemplateRig.isSlim(entity),
			layerNameOf(vanillaLayer), shapeOf(armorModel), sheetOf(entity, stack, slot)));
	}
	
	@Nullable
	public static RebakedMesh bake(LivingEntity entity, ItemStack stack, EquipmentSlot slot,
			HumanoidModel<?> defaultModel, Model armorModel, RenderLayer<?, ?> vanillaLayer) {
		ResourceLocation id = ForgeRegistries.ITEMS.getKey(stack.getItem());
		
		if (id == null) {
			return null;
		}
		
		CacheKey key = new CacheKey(id, slot, tintOf(stack), TemplateRig.isSlim(entity), layerNameOf(vanillaLayer),
			shapeOf(armorModel), sheetOf(entity, stack, slot));

		if (NOT_DRAWN.contains(key)) {
			return null;
		}

		RebakedMesh existing = CACHE.get(key);
		
		if (existing != null) {
			return existing;
		}
		
		Attempt attempt = FAILURES.get(key);

		if (capturing || (attempt != null
				&& System.currentTimeMillis() - attempt.lastTried() < retryDelay(attempt.count()))) {
			return null;
		}
		
		try {
			capturing = true;
			RebakedMesh baked = bakeUncached(key, entity, stack, slot, defaultModel, armorModel, vanillaLayer);
			
			if (baked != null) {
				CACHE.put(key, baked);
				FAILURES.remove(key);
			} else {
				noteFailure(key, id, slot, "produced no geometry");
			}
			
			return baked;
		} catch (Throwable t) {
			noteFailure(key, id, slot, t.toString());
			
			return null;
		} finally {
			capturing = false;
		}
	}

	private static void noteFailure(CacheKey key, ResourceLocation id, EquipmentSlot slot, String reason) {
		Attempt previous = FAILURES.get(key);
		int count = (previous == null ? 0 : previous.count()) + 1;
		FAILURES.put(key, new Attempt(count, System.currentTimeMillis()));

	}
	
	@Nullable
	private static RebakedMesh bakeUncached(CacheKey key, LivingEntity entity, ItemStack stack, EquipmentSlot slot,
			HumanoidModel<?> defaultModel, Model armorModel, RenderLayer<?, ?> vanillaLayer) {
		int jointCount = Armatures.BIPED.get().getJointNumber();
		TemplateRig rig = TemplateRig.get(jointCount, key.slim());
		
		if (rig == null) {
			return null;
		}
		
		HumanoidModel<?> parent = parentModelOf(vanillaLayer);
		RawMesh captured = captureLayer(vanillaLayer, entity, stack, slot, armorModel, parent, false);

		List<LiveSource> liveSources = Lists.newArrayList();
		RawMesh partTree = null;

		if (armorModel instanceof HumanoidModel<?> humanoidModel) {
			partTree = MeshCapture.captureHumanoid(humanoidModel, liveSources, parent);
		}

		RawMesh again = captureLayer(vanillaLayer, entity, stack, slot, armorModel, parent, false);
		boolean steady = sameGeometry(captured, again);


		java.util.Set<String> moving = partsThatMove(armorModel, entity);


		RawMesh drawn = captured.isEmpty() ? null : captured;
		String provenance;
		RawMesh mesh;
		boolean watchedItDraw;
		RawMesh silenced = null;

		if (drawn == null) {
			if (vanillaLayer != null && vanillaLayer.getClass() != HumanoidArmorLayer.class) {
				NOT_DRAWN.add(key);

				return null;
			}

			if (partTree == null) {
				return null;
			}

			mesh = partTree;
			provenance = "part tree";
			watchedItDraw = false;
		} else if (partTree == null) {
			mesh = drawn;
			provenance = "vertex capture";
			watchedItDraw = true;
		} else {
			silenced = captureLayer(vanillaLayer, entity, stack, slot, armorModel, parent, true);
			boolean treeDrawsIt = drawsTheTree(captured, silenced);

			if (treeDrawsIt) {
				mesh = partTree;
				provenance = "part tree";
				watchedItDraw = false;
			} else {
				mesh = drawn;
				provenance = "vertex capture";
				watchedItDraw = true;
				liveSources.clear();

			}
		}

		if (provenance.equals("vertex capture") && steady) {
			JointNames.Region[] regions = regionPerVertexByProbing(vanillaLayer, entity, stack, slot, armorModel, parent, mesh);

			if (regions != null) {
				mesh = regroupedByRegion(mesh, regions);
			} else {
			}
		}

		mesh.repairNormals();
		
		java.util.Set<ResourceLocation> sheets = new java.util.LinkedHashSet<> ();
		sheets.add(yesman.epicfight.client.renderer.patched.layer.WearableItemLayer.getArmorResource(entity, stack, slot, null));
		sheets.add(yesman.epicfight.client.renderer.patched.layer.WearableItemLayer.getArmorResource(entity, stack, slot, "overlay"));

		for (RawMesh.Group group : captured.groups()) {
			if (group.texture != null) {
				sheets.add(group.texture);
			}
		}

		sheets.remove(null);

		TextureCoverage coverage = TextureCoverage.of(sheets);

		try {
			mesh = mesh.splitConnectedPieces();

		int carryingMotion = 0;

		for (RawMesh.Group group : mesh.groups()) {
			if (group.liveTransform != null) {
				carryingMotion++;
			}
		}


		float lowestAlpha = 1.0F;
		int selfLit = 0;

		for (int v = 0; v < mesh.vertexCount(); v++) {
			lowestAlpha = Math.min(lowestAlpha, mesh.alpha(v));

			if (mesh.emissive(v) > 0.5F) {
				selfLit++;
			}
		}



		java.util.Set<String> congruent = PartSubstitution.congruentGroups(mesh, rig);
			RawMesh bodyShaped = mesh.selectGroups(congruent::contains);
			RawMesh remainder = mesh.selectGroups(name -> !congruent.contains(name));

			if (!bodyShaped.isEmpty()) {
				PartSubstitution.Result substituted = PartSubstitution.substitute(bodyShaped, rig, jointCount, coverage);

				if (substituted != null && remainder.isEmpty()) {
					RebakedMesh built = MeshAssembler.assemble(substituted.mesh(), substituted.weights(), provenance, null);
					built.setColoursObserved(watchedItDraw);
					built.setExtraPasses(passesOf(drawn));
					built.addLiveSources(liveSources);
					bindToBody(built, substituted.mesh());
					PartSubstitution.logSubstitution(key.item() + " (" + key.slot() + ")", substituted);

					return built;
				}

				if (substituted != null) {
					WeightSolver.Report rest = new WeightSolver.Report();
					WeightSolver.Refined solved = WeightSolver.solveRefined(remainder, rig, rest);

					if (solved != null) {
						holdSmallPiecesRigid(solved.mesh(), solved.weights(), jointCount, moving);
						RawMesh combined = RawMesh.concat(substituted.mesh(), solved.mesh());
						SkinWeights weights = concat(substituted.weights(), solved.weights(), jointCount);
						RebakedMesh built = MeshAssembler.assemble(combined, weights, "part tree + body", null);
						built.setColoursObserved(watchedItDraw);
						built.setExtraPasses(passesOf(drawn));
						built.addLiveSources(liveSources);
						bindToBody(built, combined);


						return built;
					}
				}
			}
		} finally {
			if (coverage != null) {
				coverage.close();
			}
		}

		WeightSolver.Report report = new WeightSolver.Report();
		WeightSolver.Refined refined = WeightSolver.solveRefined(mesh, rig, report);

		if (refined == null) {
			return null;
		}

		holdSmallPiecesRigid(refined.mesh(), refined.weights(), jointCount, moving);

		RebakedMesh result = MeshAssembler.assemble(refined.mesh(), refined.weights(), provenance, null);
		result.setColoursObserved(watchedItDraw);
		result.setExtraPasses(passesOf(drawn));
		result.addLiveSources(liveSources);
		bindToBody(result, refined.mesh());

		return result;
	}

	@Nullable
	public static HumanoidModel<?> parentModelOf(RenderLayer<?, ?> vanillaLayer) {
		try {
			if (vanillaLayer instanceof yesman.epicfight.mixin.client.MixinRenderLayer accessor
					&& accessor.epicfight$getParentModel() instanceof HumanoidModel<?> humanoid) {
				return humanoid;
			}
		} catch (Throwable t) {
			EpicFightMod.LOGGER.debug("Could not read the layer's parent model: {}", t.toString());
		}

		if (!warnedAboutParent) {
			warnedAboutParent = true;
		}

		return null;
	}

	private static boolean warnedAboutParent;

	private static SkinWeights concat(SkinWeights first, SkinWeights second, int jointCount) {
		SkinWeights out = new SkinWeights(first.vertexCount + second.vertexCount);
		int at = 0;

		for (SkinWeights source : new SkinWeights[] { first, second }) {
			for (int v = 0; v < source.vertexCount; v++) {
				float[] dense = new float[jointCount];

				for (int i = 0; i < source.counts[v]; i++) {
					int slot = v * TemplateRig.MAX_INFLUENCES + i;

					if (source.jointIds[slot] < jointCount) {
						dense[source.jointIds[slot]] += source.weights[slot];
					}
				}

				out.store(at++, dense);
			}
		}

		return out;
	}

	private static final java.util.Set<String> ANCHORS = java.util.Set.of(
		"/head", "/hat", "/body", "/right_arm", "/left_arm", "/right_leg", "/left_leg");

	static java.util.Set<String> partsThatMove(Model armorModel, LivingEntity entity) {
		java.util.Set<String> moving = com.google.common.collect.Sets.newHashSet();

		if (!(armorModel instanceof HumanoidModel<?> humanoid)) {
			return moving;
		}

		ModelSnapshot before = ModelSnapshot.of(humanoid);

		try {
			Map<String, float[]> first = Maps.newHashMap();
			Map<String, float[]> second = Maps.newHashMap();

			poseAt(humanoid, entity, 0.0F, 0.0F, 0.0F);
			recordPoses(humanoid, first);
			poseAt(humanoid, entity, 37.3F, 7.3F, 0.9F);
			recordPoses(humanoid, second);

			for (Map.Entry<String, float[]> entry : first.entrySet()) {
				float[] later = second.get(entry.getKey());

				if (later == null || ANCHORS.contains(entry.getKey())) {
					continue;
				}

				for (int i = 0; i < entry.getValue().length; i++) {
					if (Math.abs(entry.getValue()[i] - later[i]) > 1.0E-4F) {
						moving.add(entry.getKey());

						break;
					}
				}
			}
		} catch (Throwable t) {
			EpicFightMod.LOGGER.debug("Could not determine which parts animate: {}", t.toString());
		} finally {
			if (before != null) {
				before.restore();
			}
		}

		return moving;
	}

	@SuppressWarnings("unchecked")
	private static void poseAt(HumanoidModel<?> model, LivingEntity entity, float ageInTicks, float limbSwing, float limbSwingAmount) {
		((HumanoidModel<LivingEntity>) model).setupAnim(entity, limbSwing, limbSwingAmount, ageInTicks, 0.0F, 0.0F);
	}

	private static void recordPoses(HumanoidModel<?> model, Map<String, float[]> into) {
		record Named(String name, net.minecraft.client.model.geom.ModelPart part) {
		}

		for (Named root : new Named[] {
				new Named("/head", model.head), new Named("/hat", model.hat), new Named("/body", model.body),
				new Named("/right_arm", model.rightArm), new Named("/left_arm", model.leftArm),
				new Named("/right_leg", model.rightLeg), new Named("/left_leg", model.leftLeg) }) {
			if (root.part() != null) {
				recordBranch(root.name(), root.part(), into);
			}
		}
	}

	private static void recordBranch(String path, net.minecraft.client.model.geom.ModelPart part, Map<String, float[]> into) {
		into.put(path, new float[] { part.x, part.y, part.z, part.xRot, part.yRot, part.zRot,
			part.xScale, part.yScale, part.zScale });

		for (Map.Entry<String, net.minecraft.client.model.geom.ModelPart> child : part.children.entrySet()) {
			recordBranch(path + "/" + child.getKey(), child.getValue(), into);
		}
	}

	private static final float CROSSING_SHARE = 0.25F;

	private static final float CONGRUENT = 1.0F / 64.0F;

	private static final float SEAM = 2.0F / 16.0F;

	private record Fold(float below, float above, int jointBelow, int jointAbove, float boundary) {
	}

	@Nullable
	private static Fold foldWithin(RawMesh mesh, SkinWeights weights, RawMesh.Group group, int jointCount) {
		int lowest = -1;
		int highest = -1;

		for (int t = group.firstTriangle; t < group.firstTriangle + group.triangleCount; t++) {
			for (int corner = 0; corner < 3; corner++) {
				int vertex = mesh.triangleVertex(t, corner);

				if (lowest < 0 || mesh.py(vertex) < mesh.py(lowest)) {
					lowest = vertex;
				}

				if (highest < 0 || mesh.py(vertex) > mesh.py(highest)) {
					highest = vertex;
				}
			}
		}

		if (lowest < 0) {
			return null;
		}

		int below = dominantJoint(weights, lowest, jointCount);
		int above = dominantJoint(weights, highest, jointCount);

		if (below < 0 || above < 0 || below == above) {
			return null;
		}

		float boundary = Float.NaN;

		for (int t = group.firstTriangle; t < group.firstTriangle + group.triangleCount; t++) {
			for (int corner = 0; corner < 3; corner++) {
				int vertex = mesh.triangleVertex(t, corner);

				if (dominantJoint(weights, vertex, jointCount) != below
						&& (Float.isNaN(boundary) || mesh.py(vertex) < boundary)) {
					boundary = mesh.py(vertex);
				}
			}
		}

		if (Float.isNaN(boundary)) {
			return null;
		}

		return new Fold(boundary - mesh.py(lowest), mesh.py(highest) - boundary, below, above, boundary);
	}

	private static int dominantJoint(SkinWeights weights, int vertex, int jointCount) {
		int dominant = -1;
		float strongest = 0.0F;
		int base = vertex * TemplateRig.MAX_INFLUENCES;

		for (int i = 0; i < weights.counts[vertex]; i++) {
			if (weights.jointIds[base + i] < jointCount && weights.weights[base + i] > strongest) {
				strongest = weights.weights[base + i];
				dominant = weights.jointIds[base + i];
			}
		}

		return dominant;
	}

	private static final float CREASE_BAND = 1.0F / 32.0F;

	private static void articulate(RawMesh mesh, SkinWeights weights, RawMesh.Group group, int jointCount, Fold fold) {

		int crease = JointNames.creaseJoint(fold.jointBelow(), fold.jointAbove());
		boolean gusseted = crease >= 0 && crease < jointCount;

		for (int t = group.firstTriangle; t < group.firstTriangle + group.triangleCount; t++) {
			for (int corner = 0; corner < 3; corner++) {
				int vertex = mesh.triangleVertex(t, corner);
				float fromCrease = (mesh.py(vertex) - fold.boundary()) / SEAM;
				float[] dense = new float[jointCount];

				if (gusseted) {
					float across = Math.max(-1.0F, Math.min(1.0F, (mesh.py(vertex) - fold.boundary()) / CREASE_BAND));

					if (across <= 0.0F) {
						dense[fold.jointBelow()] += -across;
						dense[crease] += 1.0F + across;
					} else {
						dense[crease] += 1.0F - across;
						dense[fold.jointAbove()] += across;
					}
				} else {
					float toAbove = Math.max(0.0F, Math.min(1.0F, 0.5F + 0.5F * fromCrease));

					dense[fold.jointAbove()] += toAbove;
					dense[fold.jointBelow()] += 1.0F - toAbove;
				}

				weights.store(vertex, dense);
			}
		}
	}

	private static void bindTo(RawMesh mesh, SkinWeights weights, RawMesh.Group group, int jointCount, int joint) {
		float[] dense = new float[jointCount];
		dense[joint] = 1.0F;


		for (int t = group.firstTriangle; t < group.firstTriangle + group.triangleCount; t++) {
			for (int corner = 0; corner < 3; corner++) {
				weights.store(mesh.triangleVertex(t, corner), dense);
			}
		}
	}

	static void holdSmallPiecesRigid(RawMesh mesh, SkinWeights weights, int jointCount) {
		holdSmallPiecesRigid(mesh, weights, jointCount, java.util.Set.of());
	}

	static void holdSmallPiecesRigid(RawMesh mesh, SkinWeights weights, int jointCount, java.util.Set<String> moving) {
		Map<JointNames.Region, Fold> creases = Maps.newHashMap();
		Map<JointNames.Region, java.util.List<Fold>> folds = Maps.newHashMap();

		for (RawMesh.Group group : mesh.groups()) {
			if (group.isEmpty() || animates(group.name, moving)) {
				continue;
			}

			Fold fold = foldWithin(mesh, weights, group, jointCount);

			if (fold != null) {
				folds.computeIfAbsent(JointNames.regionOf(group.name), region -> Lists.newArrayList()).add(fold);
			}
		}

		folds.forEach((region, found) -> {
			Map<Long, Integer> votes = Maps.newHashMap();

			for (Fold fold : found) {
				votes.merge((long) fold.jointBelow() << 32 | fold.jointAbove(), 1, Integer::sum);
			}

			long agreed = votes.entrySet().stream().max(Map.Entry.comparingByValue()).get().getKey();
			java.util.List<Float> boundaries = Lists.newArrayList();

			for (Fold fold : found) {
				if (((long) fold.jointBelow() << 32 | fold.jointAbove()) == agreed) {
					boundaries.add(fold.boundary());
				}
			}

			java.util.Collections.sort(boundaries);

			creases.put(region, new Fold(0.0F, 0.0F, (int) (agreed >> 32), (int) agreed,
				boundaries.get(boundaries.size() / 2)));
		});

		for (RawMesh.Group group : mesh.groups()) {
			if (group.isEmpty()) {
				continue;
			}

			float lowest = Float.MAX_VALUE;
			float highest = -Float.MAX_VALUE;

			for (int t = group.firstTriangle; t < group.firstTriangle + group.triangleCount; t++) {
				for (int corner = 0; corner < 3; corner++) {
					float y = mesh.py(mesh.triangleVertex(t, corner));
					lowest = Math.min(lowest, y);
					highest = Math.max(highest, y);
				}
			}

			if (!animates(group.name, moving)) {
				Fold limb = creases.get(JointNames.regionOf(group.name));


				if (limb != null && lowest < limb.boundary() - CONGRUENT && highest > limb.boundary() + CONGRUENT) {
					float below = limb.boundary() - lowest;
					float above = highest - limb.boundary();

					if (highest - lowest > RebakeConfig.BENDABLE_LENGTH
							&& Math.min(below, above) >= (below + above) * CROSSING_SHARE) {
						articulate(mesh, weights, group, jointCount,
							new Fold(below, above, limb.jointBelow(), limb.jointAbove(), limb.boundary()));

						continue;
					}

					bindTo(mesh, weights, group, jointCount,
						below >= above ? limb.jointBelow() : limb.jointAbove());

					continue;
				}

				Fold fold = foldWithin(mesh, weights, group, jointCount);

				if (fold != null) {
					if (highest - lowest > RebakeConfig.BENDABLE_LENGTH
							&& Math.min(fold.below(), fold.above()) >= (fold.below() + fold.above()) * CROSSING_SHARE) {
						articulate(mesh, weights, group, jointCount, fold);

						continue;
					}

					bindTo(mesh, weights, group, jointCount,
						fold.below() >= fold.above() ? fold.jointBelow() : fold.jointAbove());

					continue;
				}

				if (highest - lowest > RebakeConfig.BENDABLE_LENGTH) {
					continue;
				}
			}


			int top = -1;
			float highestPoint = -Float.MAX_VALUE;

			for (int t = group.firstTriangle; t < group.firstTriangle + group.triangleCount; t++) {
				for (int corner = 0; corner < 3; corner++) {
					int vertex = mesh.triangleVertex(t, corner);

					if (mesh.py(vertex) > highestPoint) {
						highestPoint = mesh.py(vertex);
						top = vertex;
					}
				}
			}

			if (top < 0) {
				continue;
			}

			int strongest = -1;
			float strongestWeight = 0.0F;
			int base = top * TemplateRig.MAX_INFLUENCES;

			for (int i = 0; i < weights.counts[top]; i++) {
				if (weights.jointIds[base + i] < jointCount && weights.weights[base + i] > strongestWeight) {
					strongestWeight = weights.weights[base + i];
					strongest = weights.jointIds[base + i];
				}
			}

			if (strongest < 0) {
				continue;
			}

			float[] dense = new float[jointCount];
			dense[strongest] = 1.0F;


			for (int t = group.firstTriangle; t < group.firstTriangle + group.triangleCount; t++) {
				for (int corner = 0; corner < 3; corner++) {
					weights.store(mesh.triangleVertex(t, corner), dense);
				}
			}
		}
	}

	private static boolean animates(String groupName, java.util.Set<String> moving) {
		for (String path : moving) {
			if (groupName.startsWith(path)) {
				return true;
			}
		}

		return false;
	}

	private static java.util.List<net.minecraft.client.renderer.RenderType> passesOf(@Nullable RawMesh captured) {
		java.util.List<net.minecraft.client.renderer.RenderType> passes = Lists.newArrayList();

		if (captured == null) {
			return passes;
		}

		for (RawMesh.Group group : captured.groups()) {
			if (group.renderType != null && !passes.contains(group.renderType)) {
				passes.add(group.renderType);
			}
		}

		return passes;
	}

	private static void bindToBody(RebakedMesh result, RawMesh source) {
		SurfaceBinder.bind(result, source);
	}
	
	private static RawMesh captureLayer(RenderLayer<?, ?> vanillaLayer, LivingEntity entity, ItemStack stack,
			EquipmentSlot slot, Model armorModel, @Nullable HumanoidModel<?> parent, boolean muteTree) {
		return captureLayer(vanillaLayer, entity, stack, slot, armorModel, parent, muteTree, null);
	}

	private static RawMesh captureLayer(RenderLayer<?, ?> vanillaLayer, LivingEntity entity, ItemStack stack,
			EquipmentSlot slot, Model armorModel, @Nullable HumanoidModel<?> parent, boolean muteTree,
			@Nullable java.util.function.Consumer<HumanoidModel<?>> displace) {
		RawMesh out = new RawMesh();
		CapturingVertexConsumer.Buffers buffers = new CapturingVertexConsumer.Buffers(out);

		SlotIsolation isolation = SlotIsolation.begin(entity, stack, slot);
		ModelSnapshot armorBefore = ModelSnapshot.of(armorModel);
		ModelSnapshot parentBefore = parent == null ? null : ModelSnapshot.of(parent);

		if (parent != null) {
			ModelSnapshot.restToBindPose(parent);

			if (displace != null) {
				displace.accept(parent);
			}
		}

		if (muteTree && armorModel instanceof HumanoidModel<?> humanoid) {
			muteTree(humanoid);
		}

		try (OffscreenCapture.Scope scope = OffscreenCapture.begin(buffers)) {
			PoseStack poseStack = new PoseStack();

			VanillaPoseSync.pushPartSpace(poseStack);

			((RenderLayer<LivingEntity, ?>) vanillaLayer).render(poseStack, buffers, 0, entity, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F);
		} catch (Throwable t) {
			EpicFightMod.LOGGER.warn("Failed to capture {} in {}: {}",
				ForgeRegistries.ITEMS.getKey(stack.getItem()), slot, t.toString());
		} finally {
			buffers.finish();
			isolation.end();

			if (parentBefore != null) {
				parentBefore.restore();
			}

			if (muteTree && armorBefore != null) {
				armorBefore.restore();
			}
		}

		return out;
	}

	private static final float PROBE_NUDGE = 8.0F;

	private static final float PROBE_MOVED = 0.1F;

	@Nullable
	private static JointNames.Region[] regionPerVertexByProbing(RenderLayer<?, ?> vanillaLayer, LivingEntity entity,
			ItemStack stack, EquipmentSlot slot, Model armorModel, @Nullable HumanoidModel<?> parent, RawMesh atRest) {
		if (parent == null || atRest.vertexCount() == 0) {
			return null;
		}

		record Probe(JointNames.Region region, java.util.function.Consumer<HumanoidModel<?>> displace) {
		}

		Probe[] probes = {
			new Probe(JointNames.Region.HEAD_REGION, model -> model.head.x += PROBE_NUDGE),
			new Probe(JointNames.Region.TORSO_REGION, model -> model.body.x += PROBE_NUDGE),
			new Probe(JointNames.Region.ARM_RIGHT, model -> model.rightArm.x += PROBE_NUDGE),
			new Probe(JointNames.Region.ARM_LEFT, model -> model.leftArm.x += PROBE_NUDGE),
			new Probe(JointNames.Region.LEG_RIGHT, model -> model.rightLeg.x += PROBE_NUDGE),
			new Probe(JointNames.Region.LEG_LEFT, model -> model.leftLeg.x += PROBE_NUDGE),
		};

		JointNames.Region[] regions = new JointNames.Region[atRest.vertexCount()];
		boolean anyMoved = false;

		for (Probe probe : probes) {
			RawMesh moved = captureLayer(vanillaLayer, entity, stack, slot, armorModel, parent, false, probe.displace());

			if (moved.vertexCount() != atRest.vertexCount()) {
				return null;
			}

			for (int v = 0; v < regions.length; v++) {
				if (regions[v] != null) {
					continue;
				}

				float dx = moved.px(v) - atRest.px(v);
				float dy = moved.py(v) - atRest.py(v);
				float dz = moved.pz(v) - atRest.pz(v);

				if (dx * dx + dy * dy + dz * dz > PROBE_MOVED * PROBE_MOVED) {
					regions[v] = probe.region();
					anyMoved = true;
				}
			}
		}

		return anyMoved ? regions : null;
	}

	private static RawMesh regroupedByRegion(RawMesh captured, JointNames.Region[] regions) {
		return captured.regrouped((groupName, a, b, c, d) -> {
			JointNames.Region region = regions[a];

			if (region == null || regions[b] != region || regions[c] != region) {
				region = majorityRegion(regions, a, b, c, d);
			}

			String segment = segmentFor(region);

			return segment == null ? groupName : "/" + segment + "/" + groupName;
		});
	}

	private static String describe(JointNames.Region[] regions) {
		Map<String, Integer> tally = Maps.newLinkedHashMap();

		for (JointNames.Region region : regions) {
			String segment = segmentFor(region);
			tally.merge(segment == null ? "nothing" : segment, 1, Integer::sum);
		}

		return "follows the body as " + tally;
	}

	@Nullable
	private static JointNames.Region majorityRegion(JointNames.Region[] regions, int a, int b, int c, int d) {
		Map<JointNames.Region, Integer> tally = Maps.newLinkedHashMap();

		for (int vertex : new int[] { a, b, c, d }) {
			if (vertex >= 0 && vertex < regions.length && regions[vertex] != null) {
				tally.merge(regions[vertex], 1, Integer::sum);
			}
		}

		JointNames.Region best = null;
		int bestCount = 0;

		for (Map.Entry<JointNames.Region, Integer> entry : tally.entrySet()) {
			if (entry.getValue() > bestCount) {
				best = entry.getKey();
				bestCount = entry.getValue();
			}
		}

		return best;
	}

	@Nullable
	private static String segmentFor(@Nullable JointNames.Region region) {
		if (region == null) {
			return null;
		}

		return switch (region) {
			case HEAD_REGION -> "head";
			case TORSO_REGION -> "body";
			case ARM_RIGHT -> "right_arm";
			case ARM_LEFT -> "left_arm";
			case LEG_RIGHT -> "right_leg";
			case LEG_LEFT -> "left_leg";
			default -> null;
		};
	}

	static void muteTree(HumanoidModel<?> model) {
		for (net.minecraft.client.model.geom.ModelPart root : new net.minecraft.client.model.geom.ModelPart[] {
				model.head, model.hat, model.body, model.rightArm, model.leftArm, model.rightLeg, model.leftLeg }) {
			muteBranch(root);
		}
	}

	private static void muteBranch(@Nullable net.minecraft.client.model.geom.ModelPart part) {
		if (part == null) {
			return;
		}

		part.skipDraw = true;

		for (net.minecraft.client.model.geom.ModelPart child : part.children.values()) {
			muteBranch(child);
		}
	}

	static boolean sameGeometry(RawMesh first, RawMesh second) {
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

	static boolean drawsTheTree(RawMesh captured, RawMesh withoutTree) {
		if (captured.vertexCount() == 0) {
			return true;
		}

		return withoutTree.vertexCount() * 10 < captured.vertexCount();
	}

	private static final class SlotIsolation {
		@Nullable
		private final List<ItemStack> slots;
		private final ItemStack[] saved = new ItemStack[4];
		
		private SlotIsolation(@Nullable List<ItemStack> slots) {
			this.slots = slots;
		}
		
		static SlotIsolation begin(LivingEntity entity, ItemStack keep, EquipmentSlot slot) {
			Iterable<ItemStack> armor = entity.getArmorSlots();
			
			if (!(armor instanceof List<?>)) {
				return new SlotIsolation(null);
			}
			
			@SuppressWarnings("unchecked")
			List<ItemStack> list = (List<ItemStack>) armor;
			
			if (list.size() < 4) {
				return new SlotIsolation(null);
			}
			
			SlotIsolation isolation = new SlotIsolation(list);
			
			for (int i = 0; i < 4; i++) {
				isolation.saved[i] = list.get(i);
				list.set(i, ItemStack.EMPTY);
			}
			
			if (slot.getType() == EquipmentSlot.Type.ARMOR) {
				list.set(slot.getIndex(), keep);
			}
			
			return isolation;
		}
		
		void end() {
			if (this.slots == null) {
				return;
			}
			
			for (int i = 0; i < 4; i++) {
				this.slots.set(i, this.saved[i]);
			}
		}
	}
}
