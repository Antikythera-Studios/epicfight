package yesman.epicfight.client.renderer.patched.layer;

import java.util.HashMap;
import java.util.Map;

import javax.annotation.Nullable;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;

import net.minecraft.client.Minecraft;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.Model;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.client.renderer.entity.layers.HumanoidArmorLayer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.ModelManager;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.DyeableLeatherItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.armortrim.ArmorTrim;
import net.minecraftforge.client.ForgeHooksClient;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.registries.ForgeRegistries;
import yesman.epicfight.api.asset.AssetAccessor;
import yesman.epicfight.api.asset.JsonAssetLoader;
import yesman.epicfight.api.client.forgeevent.AnimatedArmorTextureEvent;
import yesman.epicfight.api.client.model.Mesh.DrawingFunction;
import yesman.epicfight.api.client.model.SkinnedMesh;
import yesman.epicfight.api.client.model.Meshes;
import yesman.epicfight.api.client.model.rebake.ArmorRebaker;
import yesman.epicfight.api.client.model.rebake.BodyDeformation;
import yesman.epicfight.api.client.model.rebake.JointNames;
import yesman.epicfight.api.client.model.rebake.RebakedMesh;
import yesman.epicfight.api.exception.AssetLoadingException;
import yesman.epicfight.api.model.Armature;
import yesman.epicfight.api.utils.math.OpenMatrix4f;
import yesman.epicfight.client.ClientEngine;
import yesman.epicfight.client.mesh.HumanoidMesh;
import yesman.epicfight.client.renderer.EpicFightRenderTypes;
import yesman.epicfight.main.EpicFightMod;
import yesman.epicfight.world.capabilities.entitypatch.LivingEntityPatch;

public class WearableItemLayer<E extends LivingEntity, T extends LivingEntityPatch<E>, M extends HumanoidModel<E>, AM extends HumanoidMesh> extends ModelRenderLayer<E, T, M, HumanoidArmorLayer<E, M, M>, AM> {
	private record ArmorKey(ResourceLocation item, EquipmentSlot slot, int tint, String layer, int shape,
			String sheet) {
	}

	private static String layerNameOf(@Nullable net.minecraft.client.renderer.entity.layers.RenderLayer<?, ?> layer) {
		return layer == null ? "-" : layer.getClass().getName();
	}
	
	private static final EquipmentSlot[] ARMOR_DRAW_ORDER = {
		EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET, EquipmentSlot.HEAD
	};
	
	private static final Map<ArmorKey, SkinnedMesh> ARMOR_MODELS = new HashMap<> ();
	private static final Map<String, ResourceLocation> EPICFIGHT_OVERRIDING_TEXTURES = new HashMap<> ();

	private static final Map<ResourceLocation, Boolean> HAND_RIGGED = new HashMap<> ();
	
	public static void clearModels() {
		ARMOR_MODELS.values().forEach(mesh -> {
			if (mesh != null) {
				mesh.destroy();
			}
		});
		
		ARMOR_MODELS.clear();
		EPICFIGHT_OVERRIDING_TEXTURES.clear();
		HAND_RIGGED.clear();
		ArmorRebaker.clear();
	}
	
	public static void putModel(ResourceLocation rl, EquipmentSlot slot, int tint, SkinnedMesh skinnedMesh) {
		putModel(rl, slot, tint, "-", 0, "-", skinnedMesh);
	}

	public static void putModel(ResourceLocation rl, EquipmentSlot slot, int tint, String layer, int shape, String sheet, SkinnedMesh skinnedMesh) {
		ArmorKey key = new ArmorKey(rl, slot, tint, layer, shape, sheet);
		SkinnedMesh previous = ARMOR_MODELS.put(key, skinnedMesh);
		
		if (previous != null && previous != skinnedMesh) {
			previous.destroy();
		}
	}
	
	@Nullable
	public static SkinnedMesh getCachedModel(Item item, EquipmentSlot slot) {
		ResourceLocation key = ForgeRegistries.ITEMS.getKey(item);
		
		if (key == null) {
			return null;
		}

		for (Map.Entry<ArmorKey, SkinnedMesh> entry : ARMOR_MODELS.entrySet()) {
			if (entry.getKey().item().equals(key) && entry.getKey().slot() == slot) {
				return entry.getValue();
			}
		}

		return null;
	}
	
	private final boolean firstPersonModel;
	private final TextureAtlas armorTrimAtlas;
	
	public WearableItemLayer(AssetAccessor<AM> meshProvider, boolean firstPersonModel, ModelManager modelManager) {
		super(meshProvider);
		
		this.firstPersonModel = firstPersonModel;
		this.armorTrimAtlas = modelManager.getAtlas(Sheets.ARMOR_TRIMS_SHEET);
	}
	
	private void renderArmor(PoseStack poseStack, MultiBufferSource multiBufferSource, int packedLight, SkinnedMesh model, Armature armature, float r, float g, float b, ResourceLocation armorTexture, OpenMatrix4f[] poses) {
		model.draw(poseStack, multiBufferSource, RenderType.armorCutoutNoCull(armorTexture), packedLight, r, g, b, 1.0F, OverlayTexture.NO_OVERLAY, armature, poses);
	}
	
	private void renderGlint(PoseStack poseStack, MultiBufferSource multiBufferSource, int packedLight, SkinnedMesh model, Armature armature, OpenMatrix4f[] poses) {
		model.draw(poseStack, multiBufferSource, RenderType.armorEntityGlint(), packedLight, 1.0F, 1.0F, 1.0F, 1.0F, OverlayTexture.NO_OVERLAY, armature, poses);
	}
	
	private void renderTrim(PoseStack poseStack, MultiBufferSource multiBufferSource, int packedLight, SkinnedMesh model, Armature armature, ArmorMaterial armorMaterial, ArmorTrim armorTrim, EquipmentSlot slot, OpenMatrix4f[] poses) {
		TextureAtlasSprite textureatlassprite = this.armorTrimAtlas.getSprite(innerModel(slot) ? armorTrim.innerTexture(armorMaterial) : armorTrim.outerTexture(armorMaterial));
		VertexConsumer vertexConsumer = textureatlassprite.wrap(multiBufferSource.getBuffer(EpicFightRenderTypes.getTriangulated(Sheets.armorTrimsSheet())));
		model.drawPosed(poseStack, vertexConsumer, DrawingFunction.NEW_ENTITY, packedLight, 1.0F, 1.0F, 1.0F, 1.0F, OverlayTexture.NO_OVERLAY, armature, poses);
	}
	

	@Override
	public void renderLayer(T entitypatch, E entityliving, HumanoidArmorLayer<E, M, M> vanillaLayer, PoseStack poseStack, MultiBufferSource buf, int packedLight, OpenMatrix4f[] poses, float bob, float yRot, float xRot, float partialTicks) {
		try {
			BodyDeformation.prepare(Meshes.BIPED.get());
			BodyDeformation.beginFrame(entitypatch.getArmature(), poses);
		} catch (Exception e) {
			BodyDeformation.endFrame();
		}
		
		for (EquipmentSlot slot : ARMOR_DRAW_ORDER) {
			
			boolean firstPersonChest = false;
			
			if (entitypatch.isFirstPerson() && this.firstPersonModel) {
				if (slot != EquipmentSlot.CHEST) {
					continue;
				}
				
				firstPersonChest = true;
			}
			
			if (slot == EquipmentSlot.HEAD && this.firstPersonModel) {
				continue;
			}
			
			ItemStack itemstack = entityliving.getItemBySlot(slot);
			
			if (!(itemstack.getItem() instanceof ArmorItem armorItem)) {
				continue;
			}
			
			if (slot != armorItem.getEquipmentSlot()) {
				continue;
			}
			
			try {
				this.renderPiece(entitypatch, entityliving, vanillaLayer, poseStack, buf, packedLight, poses, bob, yRot, xRot, partialTicks, slot, itemstack, armorItem, firstPersonChest);
			} catch (Exception e) {
				EpicFightMod.LOGGER.debug("{} in {} failed to draw: {}", ForgeRegistries.ITEMS.getKey(armorItem), slot, e.toString());
			}
		}
		
		BodyDeformation.endFrame();
	}
	
	private void renderPiece(T entitypatch, E entityliving, HumanoidArmorLayer<E, M, M> vanillaLayer, PoseStack poseStack, MultiBufferSource buf, int packedLight,
			OpenMatrix4f[] poses, float bob, float yRot, float xRot, float partialTicks, EquipmentSlot slot, ItemStack itemstack, ArmorItem armorItem, boolean firstPersonChest) {
		M defaultModel = vanillaLayer.getArmorModel(slot);
		Model armorModel = ForgeHooksClient.getArmorModel(entityliving, itemstack, slot, defaultModel);
		SkinnedMesh armorMesh = this.getArmorModel(vanillaLayer, defaultModel, armorModel, entityliving, armorItem, itemstack, slot);
		
		if (armorMesh == null) {
			return;
		}
		
		yesman.epicfight.api.client.model.rebake.ModelSnapshot snapshot =
			yesman.epicfight.api.client.model.rebake.ModelSnapshot.of(armorModel);
		
		driveSourceAnimation(armorModel, entityliving, vanillaLayer, bob, yRot, xRot, partialTicks);
		
		if (armorMesh instanceof RebakedMesh rebaked && armorModel instanceof HumanoidModel<?> animationSource) {
			rebaked.bindAnimationSource(animationSource);
		}
		
		poseStack.pushPose();
		
		try {
			armorMesh.initialize();
			
			if (firstPersonChest) {
				hideAllButArms(armorMesh);
			}
			
			float r = 1.0F;
			float g = 1.0F;
			float b = 1.0F;
			
			boolean coloursItself = armorMesh instanceof RebakedMesh watched && watched.coloursObserved();
			
			if (armorItem instanceof DyeableLeatherItem dyeableItem && !coloursItself) {
				int colour = dyeableItem.getColor(itemstack);
				r = (float) (colour >> 16 & 255) / 255.0F;
				g = (float) (colour >> 8 & 255) / 255.0F;
				b = (float) (colour & 255) / 255.0F;
			}

			ResourceLocation plainSheet = withEpicFightOverride(getArmorResource(entityliving, itemstack, slot, "overlay"));
			
			if (armorMesh instanceof RebakedMesh rebaked && rebaked.hasCapturedPasses()) {
				for (net.minecraft.client.renderer.RenderType pass : rebaked.capturedPasses()) {
					boolean[] visibility = rebaked.restrictToPass(pass);
					boolean dyed = takesTheDye(plainSheet, pass);
					float pr = dyed ? r : 1.0F;
					float pg = dyed ? g : 1.0F;
					float pb = dyed ? b : 1.0F;
					
					try {
						armorMesh.draw(poseStack, buf, pass, packedLight, pr, pg, pb, 1.0F,
							OverlayTexture.NO_OVERLAY, entitypatch.getArmature(), poses);
					} finally {
						rebaked.restoreVisibility(visibility);
					}
				}
				
				if (rebaked.hasUnrecordedParts()) {
					drawUnrecorded(poseStack, buf, packedLight, rebaked, entitypatch.getArmature(), r, g, b, poses,
						itemstack, entityliving, slot, defaultModel);
				}
			} else if (armorMesh instanceof RebakedMesh rebaked && !rebaked.extraPasses().isEmpty()) {
				drawUnrecorded(poseStack, buf, packedLight, rebaked, entitypatch.getArmature(), r, g, b, poses,
					itemstack, entityliving, slot, defaultModel);
			} else {
				ResourceLocation texture = this.getArmorTexture(itemstack, entityliving, armorMesh, slot, null, defaultModel);
				
				this.renderArmor(poseStack, buf, packedLight, armorMesh, entitypatch.getArmature(), r, g, b, texture, poses);
				
				if (armorItem instanceof DyeableLeatherItem && !coloursItself) {
					this.renderArmor(poseStack, buf, packedLight, armorMesh, entitypatch.getArmature(), 1.0F, 1.0F, 1.0F, this.getArmorTexture(itemstack, entityliving, armorMesh, slot, "overlay", defaultModel), poses);
				}
			}
			
			ArmorTrim.getTrim(entityliving.level().registryAccess(), itemstack).ifPresent((armorTrim) -> {
				this.renderTrim(poseStack, buf, packedLight, armorMesh, entitypatch.getArmature(), armorItem.getMaterial(), armorTrim, slot, poses);
			});
			
			if (itemstack.hasFoil()) {
				this.renderGlint(poseStack, buf, packedLight, armorMesh, entitypatch.getArmature(), poses);
			}
		} finally {
			poseStack.popPose();
			
			if (snapshot != null) {
				snapshot.restore();
			}
		}
	}
	
	private static void driveSourceAnimation(Model armorModel, LivingEntity entityliving,
			net.minecraft.client.renderer.entity.layers.RenderLayer<?, ?> vanillaLayer,
			float bob, float yRot, float xRot, float partialTicks) {
		if (!(armorModel instanceof HumanoidModel<?> humanoidModel)) {
			return;
		}
		
		HumanoidModel<?> wearer = yesman.epicfight.api.client.model.rebake.ArmorRebaker.parentModelOf(vanillaLayer);
		
		if (wearer != null) {
			try {
				((HumanoidModel) wearer).copyPropertiesTo((HumanoidModel) humanoidModel);
			} catch (Exception e) {
				EpicFightMod.LOGGER.debug("Could not copy the wearer's pose onto the piece: {}", e.toString());
			}
		}
		
		boolean shouldSit = entityliving.isPassenger() && entityliving.getVehicle() != null && entityliving.getVehicle().shouldRiderSit();
		float limbSwingAmount = 0.0F;
		float limbSwing = 0.0F;
		
		if (!shouldSit && entityliving.isAlive()) {
			limbSwingAmount = Math.min(1.0F, entityliving.walkAnimation.speed(partialTicks));
			limbSwing = entityliving.walkAnimation.position(partialTicks);
			
			if (entityliving.isBaby()) {
				limbSwing *= 3.0F;
			}
		}
		
		try {
			setupAnimUnchecked(humanoidModel, entityliving, limbSwing, limbSwingAmount, bob, yRot, xRot);
		} catch (Exception e) {
			return;
		}
		
		for (ModelPart part : new ModelPart[] { humanoidModel.head, humanoidModel.hat, humanoidModel.body,
				humanoidModel.rightArm, humanoidModel.leftArm, humanoidModel.rightLeg, humanoidModel.leftLeg }) {
			if (part != null) {
				part.loadPose(part.getInitialPose());
			}
		}
	}
	
	@SuppressWarnings("unchecked")
	private static void setupAnimUnchecked(HumanoidModel<?> model, LivingEntity entity, float limbSwing, float limbSwingAmount, float bob, float yRot, float xRot) {
		((HumanoidModel<LivingEntity>) model).setupAnim(entity, limbSwing, limbSwingAmount, bob, yRot, xRot);
	}
	
	private static void hideAllButArms(SkinnedMesh mesh) {
		if (mesh instanceof RebakedMesh rebaked) {
			rebaked.showOnlyRegions(JointNames.Region.ARM_LEFT, JointNames.Region.ARM_RIGHT);
			
			return;
		}
		
		mesh.getAllParts().forEach(part -> part.setHidden(true));
		
		for (String name : new String[] { "leftArm", "rightArm" }) {
			if (mesh.hasPart(name)) {
				mesh.getPart(name).setHidden(false);
			}
		}
	}
	
	@Nullable
	private SkinnedMesh getArmorModel(HumanoidArmorLayer<E, M, M> vanillaLayer, M defaultModel, Model armorModel, E entityliving, ArmorItem armorItem, ItemStack itemstack, EquipmentSlot slot) {
		ResourceLocation registryName = ForgeRegistries.ITEMS.getKey(armorItem);
		
		if (registryName == null) {
			return null;
		}
		
		ArmorKey key = new ArmorKey(registryName, slot,
			yesman.epicfight.api.client.model.rebake.ArmorRebaker.tintOf(itemstack), layerNameOf(vanillaLayer),
			yesman.epicfight.api.client.model.rebake.ArmorRebaker.shapeOf(armorModel),
			yesman.epicfight.api.client.model.rebake.ArmorRebaker.sheetOf(entityliving, itemstack, slot));
		
		if (!ClientEngine.getInstance().renderEngine.shouldRenderVanillaModel()) {
			SkinnedMesh cached = ARMOR_MODELS.get(key);
			
			if (cached != null) {
				return cached;
			}
		}
		
		Resolved resolved = this.resolveArmorModel(vanillaLayer, defaultModel, armorModel, entityliving, armorItem, itemstack, slot, registryName);
		
		if (resolved == null) {
			return null;
		}
		
		if (!resolved.provisional()) {
			putModel(registryName, slot, yesman.epicfight.api.client.model.rebake.ArmorRebaker.tintOf(itemstack),
				layerNameOf(vanillaLayer), yesman.epicfight.api.client.model.rebake.ArmorRebaker.shapeOf(armorModel),
				yesman.epicfight.api.client.model.rebake.ArmorRebaker.sheetOf(entityliving, itemstack, slot),
				resolved.mesh());
		}
		
		return resolved.mesh();
	}
	
	private record Resolved(SkinnedMesh mesh, boolean provisional) {
	}
	
	@Nullable
	private Resolved resolveArmorModel(HumanoidArmorLayer<E, M, M> vanillaLayer, M defaultModel, Model armorModel, E entityliving, ArmorItem armorItem, ItemStack itemstack, EquipmentSlot slot, ResourceLocation registryName) {
		ResourceManager resourceManager = Minecraft.getInstance().getResourceManager();
		ResourceLocation handRigged = ResourceLocation.fromNamespaceAndPath(registryName.getNamespace(), "animmodels/armor/" + registryName.getPath() + ".json");
		
		if (HAND_RIGGED.computeIfAbsent(handRigged, at -> resourceManager.getResource(at).isPresent())) {
			try {
				return new Resolved(new JsonAssetLoader(resourceManager, handRigged).loadSkinnedMesh(SkinnedMesh::new), false);
			} catch (AssetLoadingException e) {
				EpicFightMod.LOGGER.warn("Could not load {}: {}", handRigged, e.toString());
			}
		}
		
		RebakedMesh rebaked = ArmorRebaker.bake(entityliving, itemstack, slot, defaultModel, armorModel, vanillaLayer);
		
		if (rebaked != null) {
			return new Resolved(rebaked, false);
		}
		
		if (vanillaLayer.getClass() != HumanoidArmorLayer.class) {
			return null;
		}

		AssetAccessor<? extends SkinnedMesh> builtIn = this.mesh.get().getHumanoidArmorModel(slot);
		
		if (builtIn == null) {
			return null;
		}
		
		return new Resolved(builtIn.get(), !ArmorRebaker.hasGivenUp(entityliving, itemstack, slot, vanillaLayer));
	}
	
	private ResourceLocation getArmorTexture(ItemStack itemstack, LivingEntity entity, SkinnedMesh armorMesh, EquipmentSlot slot, String type, M originalModel) {
		ResourceLocation base = getArmorResource(entity, itemstack, slot, type);
		ResourceLocation overridden = withEpicFightOverride(base);
		
		if (overridden != base) {
			return overridden;
		}
		
		String s1 = base.toString();
		
		AnimatedArmorTextureEvent animatedArmorTextureEvent = new AnimatedArmorTextureEvent(entity, itemstack, slot, originalModel);
		MinecraftForge.EVENT_BUS.post(animatedArmorTextureEvent);
		ResourceLocation extensionTexturePath = animatedArmorTextureEvent.getResultLocation();
		
		if (armorMesh.getRenderProperties() != null && armorMesh.getRenderProperties().customTexturePath() != null) {
			s1 = armorMesh.getRenderProperties().customTexturePath().toString();
			extensionTexturePath = null;
		}
		
		if (extensionTexturePath != null) {
			return extensionTexturePath;
		}
		
		ResourceLocation resourcelocation = HumanoidArmorLayer.ARMOR_LOCATION_CACHE.get(s1);
		
		if (resourcelocation == null) {
			resourcelocation = ResourceLocation.parse(s1);
			HumanoidArmorLayer.ARMOR_LOCATION_CACHE.put(s1, resourcelocation);
		}
		
		return resourcelocation;
	}
	
	private static ResourceLocation withEpicFightOverride(ResourceLocation base) {
		String path = base.toString();
		int lastSlash = path.lastIndexOf('/');
		
		if (lastSlash < 0) {
			return base;
		}
		
		String candidate = String.format("%s/epicfight/%s", path.substring(0, lastSlash), path.substring(lastSlash + 1));
		ResourceLocation known = EPICFIGHT_OVERRIDING_TEXTURES.get(candidate);
		
		if (known != null) {
			return known;
		}
		
		if (!EPICFIGHT_OVERRIDING_TEXTURES.containsKey(candidate)) {
			ResourceLocation resolved = ResourceLocation.parse(candidate);
			ResourceManager resources = Minecraft.getInstance().getResourceManager();
			
			if (resources.getResource(resolved).isPresent()) {
				EPICFIGHT_OVERRIDING_TEXTURES.put(candidate, resolved);
				
				return resolved;
			}
			
			EPICFIGHT_OVERRIDING_TEXTURES.put(candidate, null);
		}
		
		return base;
	}
	
	private void drawUnrecorded(PoseStack poseStack, MultiBufferSource buf, int packedLight, RebakedMesh rebaked,
			Armature armature, float r, float g, float b, OpenMatrix4f[] poses, ItemStack itemstack,
			E entityliving, EquipmentSlot slot, M defaultModel) {
		boolean[] visibility = rebaked.hasUnrecordedParts() ? rebaked.restrictToUnrecorded() : null;
		
		ResourceLocation plainSheet = withEpicFightOverride(getArmorResource(entityliving, itemstack, slot, "overlay"));
		
		try {
			if (rebaked.extraPasses().isEmpty()) {
				this.renderArmor(poseStack, buf, packedLight, rebaked, armature, r, g, b,
					this.getArmorTexture(itemstack, entityliving, rebaked, slot, null, defaultModel), poses);
				
				return;
			}
			
			for (net.minecraft.client.renderer.RenderType pass : rebaked.extraPasses()) {
				boolean dyed = takesTheDye(plainSheet, pass);
				
				rebaked.draw(poseStack, buf, pass, packedLight, dyed ? r : 1.0F, dyed ? g : 1.0F,
					dyed ? b : 1.0F, 1.0F, OverlayTexture.NO_OVERLAY, armature, poses);
			}
		} finally {
			if (visibility != null) {
				rebaked.restoreVisibility(visibility);
			}
		}
	}
	
	private static boolean innerModel(EquipmentSlot slot) {
		return slot == EquipmentSlot.LEGS;
	}
	
	/**
	 * Code copy from {@link HumanoidArmorLayer#getArmorResource(Entity, ItemStack, EquipmentSlot, String)} since it's not a static
	 */
	public static boolean takesTheDye(@Nullable ResourceLocation plainSheet, @Nullable net.minecraft.client.renderer.RenderType pass) {
		if (plainSheet == null || pass == null) {
			return true;
		}

		return !plainSheet.equals(yesman.epicfight.api.client.model.rebake.CapturingVertexConsumer.textureOf(pass));
	}

	public static ResourceLocation getArmorResource(Entity entity, ItemStack stack, EquipmentSlot slot, @Nullable String type) {
		ArmorItem item = (ArmorItem) stack.getItem();
		String texture = item.getMaterial().getName();
		String domain = "minecraft";
		int idx = texture.indexOf(':');
		
		if (idx != -1) {
			domain = texture.substring(0, idx);
			texture = texture.substring(idx + 1);
		}
		
		String s1 = String.format(java.util.Locale.ROOT, "%s:textures/models/armor/%s_layer_%d%s.png", domain, texture, (innerModel(slot) ? 2 : 1), type == null ? "" : String.format(java.util.Locale.ROOT, "_%s", type));
		
		s1 = net.minecraftforge.client.ForgeHooksClient.getArmorTexture(entity, stack, s1, slot, type);
		ResourceLocation resourcelocation = HumanoidArmorLayer.ARMOR_LOCATION_CACHE.get(s1);
		
		if (resourcelocation == null) {
			resourcelocation = ResourceLocation.parse(s1);
			HumanoidArmorLayer.ARMOR_LOCATION_CACHE.put(s1, resourcelocation);
		}
		
		return resourcelocation;
	}
}
