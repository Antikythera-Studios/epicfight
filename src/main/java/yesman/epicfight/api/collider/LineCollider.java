package yesman.epicfight.api.collider;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import yesman.epicfight.api.animation.Joint;
import yesman.epicfight.api.animation.Pose;
import yesman.epicfight.api.model.Armature;
import yesman.epicfight.api.utils.math.MathUtils;
import yesman.epicfight.api.utils.math.OpenMatrix4f;
import yesman.epicfight.api.utils.side.ClientOnly;
import yesman.epicfight.client.renderer.EpicFightRenderTypes;

public class LineCollider extends Collider {
	protected Vec3 modelVec;
	protected Vec3 worldVec;
	
	public LineCollider(double posX, double posY, double posZ, double vecX, double vecY, double vecZ) {
		this(getInitialAABB(posX, posY, posZ, vecX, vecY, vecZ), posX, posY, posZ, vecX, vecY, vecZ);
	}
	
	protected LineCollider(AABB outerAABB, double posX, double posY, double posZ, double vecX, double vecY, double vecZ) {
		super(new Vec3(posX, posY, posZ), outerAABB);
		this.modelVec = new Vec3(vecX, vecY, vecZ);
		this.worldVec = new Vec3(0.0D, 0.0D, 0.0D);
	}
	
	static AABB getInitialAABB(double posX, double posY, double posZ, double vecX, double vecY, double vecZ) {
		Vec3 start = new Vec3(posX, posY, posZ);
		Vec3 end = new Vec3(vecX + posX, vecY + posY, vecZ + posZ);
		double length = Math.max(start.length(), end.length());
		return new AABB(length, length, length, -length, -length, -length);
	}
	
	@Override
	public void transform(OpenMatrix4f mat) {
		this.worldVec = OpenMatrix4f.transform(OpenMatrix4f.removeTranslation(mat, Armature.evalScratch()[0]), this.modelVec);
		super.transform(mat);
	}
	
	@Override
	public boolean isCollide(Entity entity) {
		AABB opponent = entity.getBoundingBox();
		double maxStart;
		double minEnd;
		double startX;
		double startY;
		double startZ;
		double endX;
		double endY;
		double endZ;
		
		if (this.worldVec.x == 0) {
			if (this.worldCenter.x < opponent.minX || this.worldCenter.x > opponent.maxX) {
				return false;
			}
		}
		
		startX = Mth.clamp((opponent.minX + this.worldCenter.x) / -this.worldVec.x, 0, 1);
		endX = Mth.clamp((opponent.maxX + this.worldCenter.x) / -this.worldVec.x, 0, 1);

		if (startX > endX) {
			double temp = startX;
			startX = endX;
			endX = temp;
		}
		
		maxStart = startX;
		minEnd = endX;
		
		if (minEnd == maxStart) {
			return false;
		}
		
		if (this.worldVec.y == 0) {
			if (this.worldCenter.y < opponent.minY || this.worldCenter.y > opponent.maxY) {
				return false;
			}
		}
		
		startY = Mth.clamp((float)(opponent.minY - this.worldCenter.y) / this.worldVec.y, 0, 1);
		endY = Mth.clamp((float)(opponent.maxY - this.worldCenter.y) / this.worldVec.y, 0, 1);
		
		if (startY > endY) {
			double temp = startY;
			startY = endY;
			endY = temp;
		}
		
		maxStart = maxStart < startY ? startY : maxStart;
		minEnd = minEnd > endY ? endY : minEnd;
		
		if (maxStart >= minEnd) {
			return false;
		}
		
		if (this.worldVec.z == 0) {
			if (this.worldCenter.z < opponent.minZ || this.worldCenter.z > opponent.maxZ) {
				return false;
			}
		}
		
		startZ = Mth.clamp((float)(opponent.minZ + this.worldCenter.z) / -this.worldVec.z, 0, 1);
		endZ = Mth.clamp((float)(opponent.maxZ + this.worldCenter.z) / -this.worldVec.z, 0, 1);
		
		if (startZ > endZ) {
			double temp = startZ;
			startZ = endZ;
			endZ = temp;
		}
		
		maxStart = maxStart < startZ ? startZ : maxStart;
		minEnd = minEnd > endZ ? endZ : minEnd;

		return !(maxStart >= minEnd);
	}
	
	@Override
	public LineCollider deepCopy() {
		return new LineCollider(this.modelCenter.x, this.modelCenter.y, this.modelCenter.z, this.modelVec.x, this.modelVec.y, this.modelVec.z);
	}
	
	@Override @ClientOnly
	public RenderType getRenderType() {
		return EpicFightRenderTypes.debugCollider();
	}
	
	@Override @ClientOnly
	public void drawInternal(PoseStack poseStack, VertexConsumer vertexConsumer, Armature armature, Joint joint, Pose pose1, Pose pose2, float partialTicks, int color) {
		OpenMatrix4f poseMatrix;
		Pose interpolatedPose = Pose.interpolatePose(pose1, pose2, partialTicks);
		
		if (armature.rootJoint.equals(joint)) {
			poseMatrix = interpolatedPose.orElseEmpty("Root").getAnimationBoundMatrix(armature.rootJoint, new OpenMatrix4f()).removeTranslation();
		} else {
			poseMatrix = armature.getBoundTransformFor(interpolatedPose, joint);
		}
		
		MathUtils.mulStack(poseStack, poseMatrix);
        Matrix4f matrix = poseStack.last().pose();
        float startX = (float)this.modelCenter.x;
        float startY = (float)this.modelCenter.y;
        float startZ = (float)this.modelCenter.z;
        float endX = (float)(this.modelCenter.x + this.modelVec.x);
        float endY = (float)(this.modelCenter.y + this.modelVec.y);
        float endZ = (float)(this.modelCenter.z + this.modelVec.z);
        vertexConsumer.addVertex(matrix, startX, startY, startZ).setColor(color);
        vertexConsumer.addVertex(matrix, endX, endY, endZ).setColor(color);
	}
}