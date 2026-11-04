package yesman.epicfight.client.particle;

import org.joml.Quaternionf;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;

import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import yesman.epicfight.api.asset.AssetAccessor;
import yesman.epicfight.api.client.model.Mesh;
import yesman.epicfight.api.utils.math.QuaternionUtils;

public abstract class CustomModelParticle<M extends Mesh> extends Particle {
	protected final AssetAccessor<M> particleMeshProvider;
	protected float pitch;
	protected float pitchO;
	protected float yaw;
	protected float yawO;
	protected float scale = 1.0F;
	protected float scaleO = 1.0F;
	
	public CustomModelParticle(ClientLevel level, double x, double y, double z, double xd, double yd, double zd, AssetAccessor<M> particleMesh) {
		super(level, x, y, z, xd, yd, zd);
		this.particleMeshProvider = particleMesh;
	}
	
	@Override
	public void render(VertexConsumer vertexConsumer, Camera camera, float partialTicks) {
		PoseStack poseStack = new PoseStack();
		poseStack.pushPose();
		this.setupPoseStack(poseStack, camera, partialTicks);
		this.prepareDraw(poseStack, partialTicks);
		this.particleMeshProvider.get().draw(poseStack, vertexConsumer, Mesh.DrawingFunction.POSITION_TEX_COLOR_LIGHTMAP, this.getLightColor(partialTicks), this.rCol, this.gCol, this.bCol, this.alpha, OverlayTexture.NO_OVERLAY);
		this.revert(poseStack);
		poseStack.popPose();
	}
	
	@Override
	public void tick() {
		if (this.age++ >= this.lifetime) {
			this.remove();
		} else {
			this.pitchO = this.pitch;
			this.yawO = this.yaw;
			this.oRoll = this.roll;
			this.scaleO = this.scale;
		}
	}
	
	public void prepareDraw(PoseStack poseStack, float partialTicks) {
	}
	
	protected void setupPoseStack(PoseStack poseStack, Camera camera, float partialTicks) {
		poseStack.pushPose();
		
		Vec3 cameraPosition = camera.getPosition();
		float x = (float)(Mth.lerp(partialTicks, this.xo, this.x) - cameraPosition.x());
		float y = (float)(Mth.lerp(partialTicks, this.yo, this.y) - cameraPosition.y());
		float z = (float)(Mth.lerp(partialTicks, this.zo, this.z) - cameraPosition.z());
		poseStack.translate(x, y, z);
		
		Quaternionf rotation = new Quaternionf(0.0F, 0.0F, 0.0F, 1.0F);
		float roll = Mth.lerp(partialTicks, this.oRoll, this.roll);
		float pitch = Mth.lerp(partialTicks, this.pitchO, this.pitch);
		float yaw = Mth.lerp(partialTicks, this.yawO, this.yaw);
		rotation.mul(QuaternionUtils.YP.rotationDegrees(180.0F - yaw));
		rotation.mul(QuaternionUtils.XP.rotationDegrees(pitch));
		rotation.mul(QuaternionUtils.ZP.rotationDegrees(roll));
		poseStack.mulPose(rotation);
		
		float scale = (float)Mth.lerp(partialTicks, this.scaleO, this.scale);
		poseStack.scale(scale, scale, scale);
	}
	
	protected void revert(PoseStack poseStack) {
		poseStack.popPose();
	}
}