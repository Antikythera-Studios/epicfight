package yesman.epicfight.compat.skinlayer3d;
import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
import yesman.epicfight.EpicFight;

import com.google.common.collect.Maps;
import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.vertex.PoseStack;
import dev.tr7zw.skinlayers.SkinLayersModBase;
import dev.tr7zw.skinlayers.SkinUtil;
import dev.tr7zw.skinlayers.accessor.PlayerEntityModelAccessor;
import dev.tr7zw.skinlayers.accessor.PlayerSettings;
import dev.tr7zw.skinlayers.api.Mesh;
import dev.tr7zw.skinlayers.render.CustomizableCubeListBuilder;
import dev.tr7zw.skinlayers.render.CustomizableModelPart;
import dev.tr7zw.skinlayers.renderlayers.CustomLayerFeatureRenderer;
import dev.tr7zw.skinlayers.util.NMSWrapper.WrappedNativeImage;
import dev.tr7zw.skinlayers.versionless.util.wrapper.SolidPixelWrapper;

import net.minecraft.client.Minecraft;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.player.PlayerModelPart;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.AbstractSkullBlock;



import yesman.epicfight.api.client.event.EpicFightClientEventHooks;
import yesman.epicfight.api.client.model.SkinnedMesh;
import yesman.epicfight.api.event.EpicFightEventHooks;
import yesman.epicfight.api.utils.math.OpenMatrix4f;
import yesman.epicfight.client.events.engine.RenderEngine;
import yesman.epicfight.client.mesh.HumanoidMesh;
import yesman.epicfight.client.renderer.patched.entity.PPlayerRenderer;
import yesman.epicfight.client.renderer.patched.layer.ModelRenderLayer;
import yesman.epicfight.client.world.capabilites.entitypatch.player.AbstractClientPlayerPatch;
import yesman.epicfight.compat.ICompatModule;
import yesman.epicfight.compat.skinlayer3d.client.SkinLayer3DTransformer;
import yesman.epicfight.main.EpicFightMod;

import java.util.Collections;
import java.util.Map;
import java.util.function.Function;

public class SkinLayer3DCompat implements ICompatModule {
	public static final AttachmentType<SkinLayer3DMeshes> SKINLAYER_MESH = AttachmentRegistry.createDefaulted(
            ResourceLocation.fromNamespaceAndPath(EpicFight.MODID, "skinlayer_mesh"),
            SkinLayer3DMeshes::new
    );
	
	@Override
	public void onInitialize() {
	}

	@Override
	public void onInitializeServer() {
	}
	
	@Override
	public void onInitializeClient() {
        EpicFightClientEventHooks.Registry.MODIFY_PATCHED_ENTITY.registerEvent(event -> {
            if (event.get(EntityType.PLAYER) instanceof PPlayerRenderer playerrenderer) {
                playerrenderer.addPatchedLayerAlways(CustomLayerFeatureRenderer.class, new EpicFight3DSkinLayerRenderer());
            }
        });
	}
	
	@Override
	public void onInitializeClientServer() {
        EpicFightEventHooks.Entity.ON_REMOVED.registerEvent(event -> {
            SkinLayer3DMeshes skinlayerMesh = event.getEntityPatch().getOriginal().getAttached(SKINLAYER_MESH);
            if (skinlayerMesh != null) {
                skinlayerMesh.partMeshes.forEach((k, v) -> { if (v != null) v.destroy(); });
                skinlayerMesh.partMeshes.clear();
                event.getEntityPatch().getOriginal().removeAttached(SKINLAYER_MESH);
            }
        });
	}
	
	public static final class SkinLayer3DMeshes {
		private final Map<PlayerModelPart, SkinnedMesh> partMeshes = Maps.newHashMap();

		public SkinLayer3DMeshes() {
		}

		public void put(PlayerModelPart playerModelPart, SkinnedMesh animatedMesh) {
			if (animatedMesh == null) {
				return;
			}

			SkinnedMesh oldMesh = this.partMeshes.get(playerModelPart);
			if (oldMesh != null && oldMesh != animatedMesh) {
				oldMesh.destroy();
			}

			this.partMeshes.put(playerModelPart, animatedMesh);
		}
	}
	
	public static class EpicFight3DSkinLayerRenderer extends ModelRenderLayer<AbstractClientPlayer, AbstractClientPlayerPatch<AbstractClientPlayer>, PlayerModel<AbstractClientPlayer>, CustomLayerFeatureRenderer, HumanoidMesh> {
		private final Map<PlayerModelPart, Function<Player, Boolean>> partVisibilities = Maps.newHashMap();

		public EpicFight3DSkinLayerRenderer() {
			super(null);
			
			this.partVisibilities.put(PlayerModelPart.HAT, (player) -> {
				Item item = player.getItemBySlot(EquipmentSlot.HEAD).getItem();
				return !(item instanceof BlockItem blockItem && blockItem.getBlock() instanceof AbstractSkullBlock) && SkinLayersModBase.config.enableHat;
			});
			this.partVisibilities.put(PlayerModelPart.LEFT_PANTS_LEG, (player) -> SkinLayersModBase.config.enableLeftPants);
			this.partVisibilities.put(PlayerModelPart.RIGHT_PANTS_LEG, (player) -> SkinLayersModBase.config.enableRightPants);
			this.partVisibilities.put(PlayerModelPart.LEFT_SLEEVE, (player) -> SkinLayersModBase.config.enableLeftSleeve);
			this.partVisibilities.put(PlayerModelPart.RIGHT_SLEEVE, (player) -> SkinLayersModBase.config.enableRightSleeve);
			this.partVisibilities.put(PlayerModelPart.JACKET, (player) -> SkinLayersModBase.config.enableJacket);
		}
		
		@Override
		protected void renderLayer(AbstractClientPlayerPatch<AbstractClientPlayer> entitypatch, AbstractClientPlayer player, CustomLayerFeatureRenderer vanillaLayer, PoseStack poseStack, MultiBufferSource buffer, int packedLight, OpenMatrix4f[] poses, float bob, float yRot, float xRot, float partialTicks) {
			if (SkinLayersModBase.config.compatibilityMode || player.isInvisible()) {
				return;
	        }

			if (Minecraft.getInstance().player.distanceToSqr(player) > SkinLayersModBase.config.renderDistanceLOD * SkinLayersModBase.config.renderDistanceLOD) {
	            return;
			}
			
			SkinLayer3DMeshes skin3dlayerMeshes = player.getAttachedOrCreate(SKINLAYER_MESH);
			int overlay = LivingEntityRenderer.getOverlayCoords(player, 0.0f);
			
			boolean firstPerson = RenderEngine.isLocalPlayerInFirstPerson(player);

			for (PlayerModelPart playerModelPart : PlayerModelPart.values()) {
				if (playerModelPart == PlayerModelPart.CAPE) {
					continue;
				}

				if (firstPerson && playerModelPart == PlayerModelPart.HAT) {
					continue;
				}
				
				boolean noModel = !skin3dlayerMeshes.partMeshes.containsKey(playerModelPart);
				
				if (noModel || RenderEngine.getInstance().shouldRenderVanillaModel()) {
					if (player instanceof PlayerSettings playerSettings) {
						switch (playerModelPart) {
						case JACKET -> {
							skin3dlayerMeshes.put(playerModelPart, createEpicFight3DSkinLayer(player, playerModelPart, playerSettings.getTorsoMesh(), vanillaLayer.getParentModel().body, 8, 12, 4, 16, 32, true, 0));
						}
						case LEFT_SLEEVE -> {
							int armWidth = ((PlayerEntityModelAccessor)vanillaLayer.getParentModel()).hasThinArms() ? 3 : 4;
							skin3dlayerMeshes.put(playerModelPart, createEpicFight3DSkinLayer(player, playerModelPart, playerSettings.getLeftArmMesh(), vanillaLayer.getParentModel().leftArm, armWidth, 12, 4, 48, 48, true, -2f));
						}
						case RIGHT_SLEEVE -> {
							int armWidth = ((PlayerEntityModelAccessor)vanillaLayer.getParentModel()).hasThinArms() ? 3 : 4;
							skin3dlayerMeshes.put(playerModelPart, createEpicFight3DSkinLayer(player, playerModelPart, playerSettings.getRightArmMesh(), vanillaLayer.getParentModel().rightArm, armWidth, 12, 4, 40, 32, true, -2F));
						}
						case LEFT_PANTS_LEG -> {
							skin3dlayerMeshes.put(playerModelPart, createEpicFight3DSkinLayer(player, playerModelPart, playerSettings.getLeftLegMesh(), vanillaLayer.getParentModel().leftLeg, 4, 12, 4, 0, 48, true, 0f));
						}
						case RIGHT_PANTS_LEG -> {
							skin3dlayerMeshes.put(playerModelPart, createEpicFight3DSkinLayer(player, playerModelPart, playerSettings.getRightLegMesh(), vanillaLayer.getParentModel().rightLeg, 4, 12, 4, 0, 32, true, 0f));
						}
						case HAT -> {
							skin3dlayerMeshes.put(playerModelPart, createEpicFight3DSkinLayer(player, playerModelPart, playerSettings.getHeadMesh(), vanillaLayer.getParentModel().head, 8, 8, 8, 32, 0, false, 0.6F));
						}
						default -> {}
						}
					}
					
					// Initialize timer
					if (noModel) {
						RenderEngine.getInstance().setModelInitializerTimer(20);
					}
				}
				
				if (this.partVisibilities.get(playerModelPart).apply(player)) {
					SkinnedMesh mesh = skin3dlayerMeshes.partMeshes.get(playerModelPart);
					
					if (mesh != null) {
						mesh.draw(poseStack, buffer, RenderType.entityTranslucent(player.getSkin().texture(), true), packedLight, 1.0F, 1.0F, 1.0F, 1.0F, overlay, entitypatch.getArmature(), poses);
					}
				}
			}
		}
		
		private static SkinnedMesh createEpicFight3DSkinLayer(AbstractClientPlayer player, PlayerModelPart playerModelPart, Mesh skinlayerModelPart, ModelPart vanillaModelPart, int width, int height, int depth, int textureU, int textureV, boolean topPivot, float rotationOffset) {
			ResourceLocation skinLocation = player.getSkin().texture();
			NativeImage skinImage = SkinUtil.getTexture(skinLocation, null);

			if (skinImage == null) {
				return null;
			}

			CustomizableCubeListBuilder builder = new CustomizableCubeListBuilder();

			if (SolidPixelWrapper.wrapBox(builder, new WrappedNativeImage(skinImage), width, height, depth, textureU, textureV, topPivot, rotationOffset) != null) {
				return SkinLayer3DTransformer.transformMesh(
						player
					 , (skinlayerModelPart == null) ? new CustomizableModelPart(builder.getVanillaCubes(), builder.getCubes(), Collections.emptyMap()) : (CustomizableModelPart)skinlayerModelPart
					 , vanillaModelPart
					 , playerModelPart
					 , builder.getVanillaCubes()
					 , builder.getCubes()
				);
			}

			return null;
		}
	}
}