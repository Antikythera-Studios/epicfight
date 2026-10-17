package yesman.epicfight.client.gui;

import com.google.common.collect.Lists;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import yesman.epicfight.api.client.camera.EpicFightCameraAPI;
import yesman.epicfight.client.renderer.EpicFightRenderTypes;
import yesman.epicfight.client.world.capabilites.entitypatch.player.LocalPlayerPatch;
import yesman.epicfight.EpicFight;
import yesman.epicfight.world.capabilities.entitypatch.LivingEntityPatch;

import javax.annotation.Nullable;
import java.util.List;

public abstract class EntityUI {
	public static final List<EntityUI> ENTITY_UI_LIST = Lists.newArrayList();
	public static final TargetIndicator TARGET_INDICATOR = new TargetIndicator();
	public static final HealthBar HEALTH_BAR  = new HealthBar();
    public static final ResourceLocation BATTLE_ICON = EpicFight.identifier("textures/gui/battle_icons.png");
	
	public EntityUI() {
		ENTITY_UI_LIST.add(this);
	}
	
	public static void setupPoseStack(PoseStack poseStack, LivingEntity entity, float uiX, float uiY, float uiZ, boolean lockRotation, float partialTick) {
        EpicFightCameraAPI cameraApi = EpicFightCameraAPI.getInstance();
		float xRot = -cameraApi.getCameraXRot();
		float yRot = -cameraApi.getCameraYRot() + 180.0F;
		poseStack.translate(uiX, uiY, uiZ);
		poseStack.mulPose(Axis.YP.rotationDegrees(yRot));
		poseStack.mulPose(Axis.XP.rotationDegrees(xRot));
	}
	
	public static void drawUIAsLevelModel(PoseStack.Pose posestack$pose, ResourceLocation textureLocation, MultiBufferSource buffer, float minX, float minY, float maxX, float maxY, int minU, int minV, int maxU, int maxV, int uvSize) {
		float uvSizeInvert = 1.0F / uvSize;
		
		drawUIAsLevelModel(posestack$pose, textureLocation, buffer, minX, minY, maxX, maxY, minU * uvSizeInvert, minV * uvSizeInvert, maxU * uvSizeInvert, maxV * uvSizeInvert);
	}
	
	public static void drawUIAsLevelModel(PoseStack.Pose posestack$pose, ResourceLocation textureLocation, MultiBufferSource buffer, float minX, float minY, float maxX, float maxY, float minU, float minV, float maxU, float maxV) {
		VertexConsumer vertexConsumer = buffer.getBuffer(EpicFightRenderTypes.entityUITexture(textureLocation));
		
		vertexConsumer.addVertex(posestack$pose, minX, minY, 0).setUv(minU, maxV);
        vertexConsumer.addVertex(posestack$pose, maxX, minY, 0).setUv(maxU, maxV);
        vertexConsumer.addVertex(posestack$pose, maxX, maxY, 0).setUv(maxU, minV);
        vertexConsumer.addVertex(posestack$pose, minX, maxY, 0).setUv(minU, minV);
	}
	
	public static void drawColoredQuadAsLevelModel(PoseStack.Pose posestack$pose, MultiBufferSource buffer, float minX, float minY, float maxX, float maxY, int packedColor) {
		VertexConsumer vertexConsumer = buffer.getBuffer(EpicFightRenderTypes.entityUIColor());
		
		vertexConsumer.addVertex(posestack$pose, minX, minY, 0).setColor(packedColor);
        vertexConsumer.addVertex(posestack$pose, maxX, minY, 0).setColor(packedColor);
        vertexConsumer.addVertex(posestack$pose, maxX, maxY, 0).setColor(packedColor);
        vertexConsumer.addVertex(posestack$pose, minX, maxY, 0).setColor(packedColor);
	}
	
	public static void drawColoredQuadAsLevelModel(PoseStack.Pose posestack$pose, MultiBufferSource buffer, float minX, float minY, float maxX, float maxY, int r, int g, int b, int a) {
		VertexConsumer vertexConsumer = buffer.getBuffer(EpicFightRenderTypes.entityUIColor());
		
		vertexConsumer.addVertex(posestack$pose, minX, minY, 0).setColor(r, g, b, a);
        vertexConsumer.addVertex(posestack$pose, maxX, minY, 0).setColor(r, g, b, a);
        vertexConsumer.addVertex(posestack$pose, maxX, maxY, 0).setColor(r, g, b, a);
        vertexConsumer.addVertex(posestack$pose, minX, maxY, 0).setColor(r, g, b, a);
	}
	
	public abstract boolean shouldDraw(LivingEntity entity, @Nullable LivingEntityPatch<?> entitypatch, LocalPlayerPatch playerpatch, float partialTick);
	
	public abstract void draw(LivingEntity entity, @Nullable LivingEntityPatch<?> entitypatch, LocalPlayerPatch playerpatch, PoseStack poseStack, MultiBufferSource multiBufferSource, float partialTick);
}
