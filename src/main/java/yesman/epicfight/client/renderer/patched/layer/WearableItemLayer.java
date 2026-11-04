package yesman.epicfight.client.renderer.patched.layer;
import yesman.epicfight.platform.neoforged.event.EventHooks;
import yesman.epicfight.platform.neoforged.client.ClientHooks;
import yesman.epicfight.platform.neoforged.client.extensions.common.IClientItemExtensions;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.Nullable;

import org.joml.Vector4f;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;

import net.minecraft.client.Minecraft;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.Model;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.client.renderer.entity.layers.HumanoidArmorLayer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.ModelManager;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.armortrim.ArmorTrim;


import yesman.epicfight.api.asset.AssetAccessor;
import yesman.epicfight.api.asset.JsonAssetLoader;
import yesman.epicfight.api.client.event.EpicFightClientEventHooks;
import yesman.epicfight.api.client.event.types.render.AnimatedArmorTextureEvent;
import yesman.epicfight.api.client.model.Mesh.DrawingFunction;
import yesman.epicfight.api.client.model.SkinnedMesh;
import yesman.epicfight.api.client.model.transformer.HumanoidModelBaker;
import yesman.epicfight.api.exception.AssetLoadingException;
import yesman.epicfight.api.model.Armature;
import yesman.epicfight.api.utils.ColorUtil;
import yesman.epicfight.api.utils.ParseUtil;
import yesman.epicfight.api.utils.math.OpenMatrix4f;
import yesman.epicfight.client.events.engine.RenderEngine;
import yesman.epicfight.client.mesh.HumanoidMesh;
import yesman.epicfight.client.renderer.EpicFightRenderTypes;
import yesman.epicfight.world.capabilities.entitypatch.LivingEntityPatch;

public class WearableItemLayer<E extends LivingEntity, T extends LivingEntityPatch<E>, M extends HumanoidModel<E>, AM extends HumanoidMesh> extends ModelRenderLayer<E, T, M, HumanoidArmorLayer<E, M, M>, AM> {
	private static final Map<ResourceLocation, SkinnedMesh> ARMOR_MODELS = new HashMap<> ();
	private static final Map<String, ResourceLocation> EPICFIGHT_OVERRIDING_TEXTURES = new HashMap<> ();
	
	public static void clearModels() {
		ARMOR_MODELS.values().stream().filter(v -> v != null).forEach(SkinnedMesh::destroy);
		ARMOR_MODELS.clear();
		EPICFIGHT_OVERRIDING_TEXTURES.clear();
	}
	
	public static void putModel(ResourceLocation rl, SkinnedMesh skinnedMesh) {
		ARMOR_MODELS.computeIfPresent(rl, (key, mesh) -> {
			if (mesh != skinnedMesh) mesh.destroy();
			return mesh;
		});
		
		ARMOR_MODELS.put(rl, skinnedMesh);
	}
	
	public static SkinnedMesh getCachedModel(Item item) {
		ResourceLocation key = BuiltInRegistries.ITEM.getKey(item);
		return ARMOR_MODELS.get(key);
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
	
	private void renderTrim(PoseStack poseStack, MultiBufferSource multiBufferSource, int packedLight, SkinnedMesh model, Armature armature, Holder<ArmorMaterial> armorMaterial, ArmorTrim armorTrim, EquipmentSlot slot, OpenMatrix4f[] poses) {
		TextureAtlasSprite textureatlassprite = this.armorTrimAtlas.getSprite(innerModel(slot) ? armorTrim.innerTexture(armorMaterial) : armorTrim.outerTexture(armorMaterial));
		VertexConsumer vertexConsumer = textureatlassprite.wrap(multiBufferSource.getBuffer(EpicFightRenderTypes.getTriangulated(Sheets.armorTrimsSheet(armorTrim.pattern().value().decal()))));
		model.drawPosed(poseStack, vertexConsumer, DrawingFunction.NEW_ENTITY, packedLight, 1.0F, 1.0F, 1.0F, 1.0F, OverlayTexture.NO_OVERLAY, armature, poses);
	}
	
	@SuppressWarnings("unchecked")
	@Override
	public void renderLayer(T entitypatch, E livingentity, HumanoidArmorLayer<E, M, M> vanillaLayer, PoseStack poseStack, MultiBufferSource buffers, int packedLight, OpenMatrix4f[] poses, float bob, float yRot, float xRot, float partialTicks) {
		for (EquipmentSlot slot : EquipmentSlot.values()) {
			if (slot.getType() != EquipmentSlot.Type.HUMANOID_ARMOR) {
				continue;
			}
			
			boolean firstPersonChest = false;
			
			if (entitypatch.isFirstPerson() && this.firstPersonModel) {
				if (slot != EquipmentSlot.CHEST) {
					continue;
				} else {
					firstPersonChest = true;
				}
			}
			
			if (slot == EquipmentSlot.HEAD && this.firstPersonModel) {
				continue;
			}
			
			ItemStack itemstack = livingentity.getItemBySlot(slot);
			Item item = itemstack.getItem();
			
			if (item instanceof ArmorItem armorItem) {
				if (slot != armorItem.getEquipmentSlot()) {
					return;
				}
				
				poseStack.pushPose();
				float head = 0.0F;
				
				if (slot == EquipmentSlot.HEAD) {
					poseStack.translate(0.0D, head * 0.055D, 0.0D);
				}
				
				M vanillaModel = vanillaLayer.getArmorModel(slot);
				Model armorModel = ClientHooks.getArmorModel(livingentity, itemstack, slot, vanillaModel);
				SkinnedMesh armorMesh = this.getArmorModel(vanillaLayer, vanillaModel, armorModel, livingentity, armorItem, itemstack, slot);
				
				if (armorMesh == null) {
					poseStack.popPose();
					return;
				}
				
				if (armorModel instanceof HumanoidModel humanoidModel) {
					boolean shouldSit = livingentity.isPassenger(); // NeoForge's Entity.shouldRiderSit() defaults to true in vanilla
					float f8 = 0.0F;
					float f5 = 0.0F;
					
					if (!shouldSit && livingentity.isAlive()) {
						f8 = livingentity.walkAnimation.speed(partialTicks);
						f5 = livingentity.walkAnimation.position(partialTicks);
						
						if (livingentity.isBaby()) {
							f5 *= 3.0F;
						}
						
						if (f8 > 1.0F) {
							f8 = 1.0F;
						}
					}
					
					try {
						// Fix: Crash with better nether by unknown cause
						humanoidModel.setupAnim(livingentity, f8, f5, bob, yRot, xRot);
					} catch (ClassCastException e) {
					}
					
					humanoidModel.head.loadPose(humanoidModel.head.getInitialPose());
					humanoidModel.hat.loadPose(humanoidModel.hat.getInitialPose());
					humanoidModel.body.loadPose(humanoidModel.body.getInitialPose());
					humanoidModel.leftArm.loadPose(humanoidModel.leftArm.getInitialPose());
					humanoidModel.rightArm.loadPose(humanoidModel.rightArm.getInitialPose());
					humanoidModel.leftLeg.loadPose(humanoidModel.leftLeg.getInitialPose());
					humanoidModel.rightLeg.loadPose(humanoidModel.rightLeg.getInitialPose());
				}
				
				armorMesh.initialize();
				
				if (firstPersonChest) {
					armorMesh.getAllParts().forEach(part -> part.setHidden(true));
					
					if (armorMesh.hasPart("leftArm")) {
						armorMesh.getPart("leftArm").setHidden(false);
					}
					
					if (armorMesh.hasPart("rightArm")) {
						armorMesh.getPart("rightArm").setHidden(false);
					}
				}
				
				/**
				 * Copy from {@link HumanoidArmorLayer#renderArmorPiece}
				 */
				ArmorMaterial armormaterial = armorItem.getMaterial().value();
				IClientItemExtensions extensions = IClientItemExtensions.of(itemstack);
				int fallbackColor = extensions.getDefaultDyeColor(itemstack);
				boolean innerModel = innerModel(slot);

				AnimatedArmorTextureEvent textureEvent = EpicFightClientEventHooks.Render.ANIMATED_ARMOR_TEXTURE.post(new AnimatedArmorTextureEvent(livingentity, itemstack, slot, vanillaModel));
				ResourceLocation overriddenTexture = textureEvent.getResultLocation();

				for (int layerIdx = 0; layerIdx < armormaterial.layers().size(); layerIdx++) {
					ArmorMaterial.Layer armormaterial$layer = armormaterial.layers().get(layerIdx);
					int packedColor = extensions.getArmorLayerTintColor(itemstack, livingentity, armormaterial$layer, layerIdx, fallbackColor);

					if (packedColor != 0) {
						Vector4f color = ColorUtil.unpackToARGBF(packedColor);
						ResourceLocation texture = overriddenTexture != null
								? overriddenTexture
								: ParseUtil.tryGetOr(() -> armorMesh.getRenderProperties().customTexturePath(), () -> ClientHooks.getArmorTexture(livingentity, itemstack, armormaterial$layer, innerModel, slot));
						this.renderArmor(poseStack, buffers, packedLight, armorMesh, entitypatch.getArmature(), color.x, color.y, color.z, texture, poses);
					}
				}
				
				ArmorTrim armorTrim = itemstack.get(DataComponents.TRIM);
				
				if (armorTrim != null) {
					this.renderTrim(poseStack, buffers, packedLight, armorMesh, entitypatch.getArmature(), armorItem.getMaterial(), armorTrim, slot, poses);
				}
				
				if (itemstack.hasFoil()) {
					this.renderGlint(poseStack, buffers, packedLight, armorMesh, entitypatch.getArmature(), poses);
				}
				
				poseStack.popPose();
			}
		}
	}
	
	private SkinnedMesh getArmorModel(HumanoidArmorLayer<E, M, M> originalRenderer, M originalModel, Model forgeHooksArmorModel, E entityliving, ArmorItem armorItem, ItemStack itemstack, EquipmentSlot slot) {
		ResourceLocation registryName = BuiltInRegistries.ITEM.getKey(armorItem);
		
		if (ARMOR_MODELS.containsKey(registryName) && !RenderEngine.getInstance().shouldRenderVanillaModel()) {
			return ARMOR_MODELS.get(registryName);
		} else {
			ResourceManager resourceManager = Minecraft.getInstance().getResourceManager();
			ResourceLocation rl = ResourceLocation.fromNamespaceAndPath(BuiltInRegistries.ITEM.getKey(armorItem).getNamespace(), "animmodels/armor/" + BuiltInRegistries.ITEM.getKey(armorItem).getPath() + ".json");
			SkinnedMesh skinnedMesh = null;
			
			if (resourceManager.getResource(rl).isPresent()) {
				try {
					JsonAssetLoader modelLoader = new JsonAssetLoader(resourceManager, rl);
					skinnedMesh = modelLoader.loadSkinnedMesh(SkinnedMesh::new);
				} catch (AssetLoadingException e) {
					e.printStackTrace();
					skinnedMesh = null;
				}
			} else {
				Iterable<ItemStack> armorItems = entityliving.getArmorSlots();
				ItemStack head = entityliving.getItemBySlot(EquipmentSlot.HEAD);
				ItemStack chest = entityliving.getItemBySlot(EquipmentSlot.CHEST);
				ItemStack legs = entityliving.getItemBySlot(EquipmentSlot.LEGS);
				ItemStack feet = entityliving.getItemBySlot(EquipmentSlot.FEET);
				
				if (armorItems instanceof List) {
					List<ItemStack> armorItemList = (List<ItemStack>) armorItems;
					armorItemList.set(0, ItemStack.EMPTY);
					armorItemList.set(1, ItemStack.EMPTY);
					armorItemList.set(2, ItemStack.EMPTY);
					armorItemList.set(3, ItemStack.EMPTY);
					armorItemList.set(slot.getIndex(), itemstack);
				}

				PoseStack ps = new PoseStack();
				ps.translate(0, 0, 10000);
				
				if (forgeHooksArmorModel instanceof HumanoidModel<?> humanoidModel) {
					//Setup default visibility
					switch (slot) {
					case FEET -> {
						humanoidModel.rightLeg.visible = true;
						humanoidModel.leftLeg.visible = true;
					}
					case LEGS -> {
						humanoidModel.body.visible = true;
						humanoidModel.rightLeg.visible = true;
						humanoidModel.leftLeg.visible = true;
					}
					case CHEST -> {
						humanoidModel.body.visible = true;
						humanoidModel.rightArm.visible = true;
						humanoidModel.leftArm.visible = true;
					}
					case HEAD -> {
						humanoidModel.head.visible = true;
						humanoidModel.hat.visible = true;
					}
					default -> {}
					}
				}
				
				//Render armor to get the visibility of each part
				originalRenderer.render(ps, Minecraft.getInstance().renderBuffers().bufferSource(), 0, entityliving, 0, 0, 0, 0, 0, 0);
				
				if (armorItems instanceof List<ItemStack> armorItemList) {
					armorItemList.set(0, feet);
					armorItemList.set(1, legs);
					armorItemList.set(2, chest);
					armorItemList.set(3, head);
				}
				
				skinnedMesh = HumanoidModelBaker.bakeArmor(entityliving, itemstack, armorItem, slot, originalModel, forgeHooksArmorModel, originalRenderer.getParentModel(), this.mesh.get());
			}
			
			putModel(registryName, skinnedMesh);
			
			return skinnedMesh;
		}
	}
	
	/**
	 * Copy from {@link HumanoidArmorLayer#usesInnerModel}
	 */
	private static boolean innerModel(EquipmentSlot slot) {
		return slot == EquipmentSlot.LEGS;
	}
	
	/**
	 * Code copy from {@link HumanoidArmorLayer#getArmorResource(Entity, ItemStack, EquipmentSlot, String)} since it's not a static
	 */
	public static ResourceLocation getArmorResource(Entity entity, ItemStack itemstack, EquipmentSlot slot, @Nullable String type) {
		ArmorItem item = (ArmorItem) itemstack.getItem();
		String texture = item.getMaterial().getRegisteredName();
		String domain = "minecraft";
		int idx = texture.indexOf(':');
		
		if (idx != -1) {
			domain = texture.substring(0, idx);
			texture = texture.substring(idx + 1);
		}
		
		ResourceLocation rl = ResourceLocation.parse(String.format(java.util.Locale.ROOT, "%s:textures/models/armor/%s_layer_%d%s.png", domain, texture, (innerModel(slot) ? 2 : 1), type == null ? "" : String.format(java.util.Locale.ROOT, "_%s", type)));
		
		return rl;
	}
}
