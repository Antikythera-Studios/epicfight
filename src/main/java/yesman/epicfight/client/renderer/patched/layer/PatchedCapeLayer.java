package yesman.epicfight.client.renderer.patched.layer;

import java.util.function.Function;

import org.joml.Matrix4f;

import com.mojang.blaze3d.vertex.PoseStack;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.EffectRenderingInventoryScreen;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.layers.CapeLayer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.PlayerModelPart;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.Vec3;
import yesman.epicfight.api.client.model.Mesh;
import yesman.epicfight.client.online.EpicSkins;
import yesman.epicfight.api.client.physics.cloth.ClothSimulator;
import yesman.epicfight.api.client.physics.cloth.ClothSimulator.ClothObject;
import yesman.epicfight.api.utils.math.MathUtils;
import yesman.epicfight.api.utils.math.OpenMatrix4f;
import yesman.epicfight.api.utils.math.QuaternionUtils;
import yesman.epicfight.api.utils.math.Vec3f;
import yesman.epicfight.client.ClientEngine;
import yesman.epicfight.client.renderer.EpicFightRenderTypes;
import yesman.epicfight.client.world.capabilites.entitypatch.player.AbstractClientPlayerPatch;
import yesman.epicfight.config.ClientConfig;
import yesman.epicfight.gameasset.Armatures;

public class PatchedCapeLayer extends PatchedLayer<AbstractClientPlayer, AbstractClientPlayerPatch<AbstractClientPlayer>, PlayerModel<AbstractClientPlayer>, CapeLayer> {
	@SuppressWarnings("unchecked")
	@Override
	protected void renderLayer(AbstractClientPlayerPatch<AbstractClientPlayer> entitypatch, AbstractClientPlayer entityliving, CapeLayer vanillaLayer, PoseStack poseStack, MultiBufferSource buffer, int packedLight, OpenMatrix4f[] poses, float bob, float yRot, float xRot, float partialTick) {
		boolean simulated = false;

		if (ClientConfig.enableCosmetics) {
			// Prevent simulating cape in inventory screen
			if (Minecraft.getInstance().screen instanceof EffectRenderingInventoryScreen && entityliving == Minecraft.getInstance().player && partialTick == 1.0F) {
				return;
			}
			
			simulated = entitypatch.getClothSimulator().getRunningObject(ClothSimulator.PLAYER_CLOAK).map(clothObj -> {
				ResourceLocation capeTexture = entitypatch.isEpicSkinsLoaded() ? entitypatch.getEpicSkinsInformation().cloakTexture().get() : entityliving.getCloakTextureLocation();
				
				if (capeTexture != null) {
					Function<Float, OpenMatrix4f> partialColliderTransformProvider = (partialFrame) -> {
						Vec3 pos = entitypatch.getOriginal().getPosition(partialFrame);
						float yRotLerp = Mth.rotLerp(partialFrame, entitypatch.getYRotO(), entitypatch.getYRot());
						
						return OpenMatrix4f.createTranslation((float)pos.x, (float)pos.y, (float)pos.z).rotateDeg(180.0F - yRotLerp, Vec3f.Y_AXIS);
			        };
			        
					clothObj.tick(entitypatch, partialColliderTransformProvider, partialTick, entitypatch.getArmature(), poses);
					
					double entityX = Mth.lerp((double)partialTick, entityliving.xOld, entityliving.getX());
					double entityY = Mth.lerp((double)partialTick, entityliving.yOld, entityliving.getY());
					double entityZ = Mth.lerp((double)partialTick, entityliving.zOld, entityliving.getZ());
					
					PoseStack posestack$2 = new PoseStack();
					var renderer = ClientEngine.getInstance().renderEngine.getEntityRenderer(EntityType.PLAYER);
					renderer.mulPoseStack(posestack$2, entitypatch.getArmature(), entitypatch.getOriginal(), entitypatch, partialTick);
					Matrix4f renderLocalPose = posestack$2.last().pose();
					float bodyYRot = Mth.rotLerp(partialTick, entitypatch.getYRotO(), entitypatch.getYRot());
					
					if (entitypatch.isEpicSkinsLoaded()) {
						EpicSkins epicskinsInfo = entitypatch.getEpicSkinsInformation();
						renderSimulatingCape(poseStack, buffer, RenderType.entityCutoutNoCull(capeTexture), Mesh.DrawingFunction.NEW_ENTITY, clothObj, entityX, entityY, entityZ, epicskinsInfo.r(), epicskinsInfo.g(), epicskinsInfo.b(), 1.0F, entitypatch, poses, packedLight, renderLocalPose, bodyYRot);
					} else {
						renderSimulatingCape(poseStack, buffer, RenderType.entityCutoutNoCull(capeTexture), Mesh.DrawingFunction.NEW_ENTITY, clothObj, entityX, entityY, entityZ, 1.0F, 1.0F, 1.0F, 1.0F, entitypatch, poses, packedLight, renderLocalPose, bodyYRot);
					}
					
					return Boolean.TRUE;
				}
				
				return Boolean.FALSE;
			}).orElse(Boolean.FALSE);
		}

		if (!simulated) {
			if (!entityliving.isInvisible() && entityliving.isModelPartShown(PlayerModelPart.CAPE) && entityliving.getCloakTextureLocation() != null) {
				ItemStack itemstack = entityliving.getItemBySlot(EquipmentSlot.CHEST);
				
				if (itemstack.getItem() != Items.ELYTRA) {
					OpenMatrix4f modelMatrix = new OpenMatrix4f();
					modelMatrix.scale(new Vec3f(-1.0F, -1.0F, 1.0F)).mulFront(poses[8]);
					poseStack.pushPose();
					MathUtils.mulStack(poseStack, modelMatrix);
					poseStack.translate(0.0D, -0.4D, -0.025D);
					vanillaLayer.render(poseStack, buffer, packedLight, entityliving, entityliving.walkAnimation.position(), entityliving.walkAnimation.speed(), partialTick, entityliving.tickCount, yRot, xRot);
					poseStack.popPose();
				}
			}
		}
	}
	
	public static void renderSimulatingCape(
		PoseStack poseStack,
		MultiBufferSource buffers,
		RenderType rendertype,
		Mesh.DrawingFunction drawFunction,
		ClothObject clothObj,
		double x,
		double y,
		double z,
		float r,
		float g,
		float b,
		float a,
		AbstractClientPlayerPatch<?> entitypatch,
		OpenMatrix4f[] poses,
		int packedLight,
		Matrix4f renderLocalMatrix,
		float yBodyRot
	) {
		poseStack.pushPose();
		float scaler = entitypatch.getScale();
		poseStack.scale(scaler, scaler, scaler);
		
		if (entitypatch.getOriginal().hasItemInSlot(EquipmentSlot.CHEST)) {
			OpenMatrix4f poseMat = poses[Armatures.BIPED.get().chest.getId()];
			poseStack.translate(poseMat.m30, poseMat.m31, poseMat.m32);
			poseStack.scale(1.17F, 1.17F, 1.17F);
			poseStack.translate(-poseMat.m30, -poseMat.m31, -poseMat.m32);
		}
		
		clothObj.scaleFromPose(poseStack, poses);
		poseStack.pushPose();
		renderLocalMatrix = renderLocalMatrix.invert(new Matrix4f());
		
		poseStack.mulPoseMatrix(renderLocalMatrix);
		poseStack.translate(-renderLocalMatrix.m30() - x, -renderLocalMatrix.m31() - y, -renderLocalMatrix.m32() - z);
		poseStack.last().normal().rotate(QuaternionUtils.YP.rotationDegrees(yBodyRot));
		
		clothObj.drawPosed(poseStack, buffers.getBuffer(EpicFightRenderTypes.getTriangulated(rendertype)), drawFunction, packedLight, r, g, b, a, OverlayTexture.NO_OVERLAY, entitypatch.getArmature(), poses);
		
		poseStack.popPose();
		poseStack.popPose();
	}
}
