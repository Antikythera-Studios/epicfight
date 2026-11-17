package yesman.epicfight.api.collider;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.DoubleTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import yesman.epicfight.api.animation.Joint;
import yesman.epicfight.api.animation.JointTransform;
import yesman.epicfight.api.animation.Pose;
import yesman.epicfight.api.model.Armature;
import yesman.epicfight.api.utils.math.MathUtils;
import yesman.epicfight.api.utils.math.OpenMatrix4f;
import yesman.epicfight.api.utils.math.Vec3f;
import yesman.epicfight.api.utils.side.ClientOnly;

public class OBBCollider extends Collider {
	protected final Vec3[] modelVertices;
	protected final Vec3[] modelNormals;
	protected Vec3[] rotatedVertices;
	protected Vec3[] rotatedNormals;
	protected Vec3f scale;
	private OBBCollider queryCollider;
	
	/**
	 * 
	 * @param vertexX - vertex vector starting from center of OBB
	 * @param vertexY - vertex vector starting from center of OBB
	 * @param vertexZ - vertex vector starting from center of OBB
	 * @param centerX - center of OBB
	 * @param centerY - center of OBB
	 * @param centerZ - center of OBB
	 */
	public OBBCollider(double vertexX, double vertexY, double vertexZ, double centerX, double centerY, double centerZ) {
		this(getInitialAABB(vertexX, vertexY, vertexZ, centerX, centerY, centerZ), vertexX, vertexY, vertexZ, centerX, centerY, centerZ);
	}
	
	protected OBBCollider(AABB outerAABB, double vertexX, double vertexY, double vertexZ, double centerX, double centerY, double centerZ) {
		super(new Vec3(centerX, centerY, centerZ), outerAABB);
		
		this.modelVertices = new Vec3[4];
		this.modelNormals = new Vec3[3];
		this.rotatedVertices = new Vec3[4];
		this.rotatedNormals = new Vec3[3];
		this.modelVertices[0] = new Vec3(vertexX, vertexY, -vertexZ);
		this.modelVertices[1] = new Vec3(vertexX, vertexY, vertexZ);
		this.modelVertices[2] = new Vec3(-vertexX, vertexY, vertexZ);
		this.modelVertices[3] = new Vec3(-vertexX, vertexY, -vertexZ);
		this.modelNormals[0] = new Vec3(1, 0, 0);
		this.modelNormals[1] = new Vec3(0, 1, 0);
		this.modelNormals[2] = new Vec3(0, 0, 1);
		this.rotatedVertices[0] = new Vec3(0.0D, 0.0D, 0.0D);
		this.rotatedVertices[1] = new Vec3(0.0D, 0.0D, 0.0D);
		this.rotatedVertices[2] = new Vec3(0.0D, 0.0D, 0.0D);
		this.rotatedVertices[3] = new Vec3(0.0D, 0.0D, 0.0D);
		this.rotatedNormals[0] = new Vec3(0.0D, 0.0D, 0.0D);
		this.rotatedNormals[1] = new Vec3(0.0D, 0.0D, 0.0D);
		this.rotatedNormals[2] = new Vec3(0.0D, 0.0D, 0.0D);
	}
	
	static AABB getInitialAABB(double posX, double posY, double posZ, double center_x, double center_y, double center_z) {
		double xLength = Math.abs(posX) + Math.abs(center_x);
		double yLength = Math.abs(posY) + Math.abs(center_y);
		double zLength = Math.abs(posZ) + Math.abs(center_z);
		double maxLength = Math.max(xLength, Math.max(yLength, zLength));
		return new AABB(maxLength, maxLength, maxLength, -maxLength, -maxLength, -maxLength);
	}
	
	/**
	 * make 2d obb
	 */
	public OBBCollider(
		  AABB entityCallAABB
		, double pos1_x, double pos1_y, double pos1_z
		, double pos2_x, double pos2_y, double pos2_z
		, double norm1_x, double norm1_y, double norm1_z
		, double norm2_x, double norm2_y, double norm2_z
		, double center_x, double center_y, double center_z
	) {
		super(new Vec3(center_x, center_y, center_z), entityCallAABB);
		this.modelVertices = new Vec3[2];
		this.modelNormals = new Vec3[2];
		this.rotatedVertices = new Vec3[2];
		this.rotatedNormals = new Vec3[2];
		this.modelVertices[0] = new Vec3(pos1_x, pos1_y, pos1_z);
		this.modelVertices[1] = new Vec3(pos2_x, pos2_y, pos2_z);
		this.modelNormals[0] = new Vec3(norm1_x, norm1_y, norm1_z);
		this.modelNormals[1] = new Vec3(norm2_x, norm2_y, norm2_z);
		this.rotatedVertices[0] = new Vec3(0.0D, 0.0D, 0.0D);
		this.rotatedVertices[1] = new Vec3(0.0D, 0.0D, 0.0D);
		this.rotatedNormals[0] = new Vec3(0.0D, 0.0D, 0.0D);
		this.rotatedNormals[1] = new Vec3(0.0D, 0.0D, 0.0D);
	}
	
	/**
	 * convert an AABB to OBB
	 * @param aabb
	 */
	public OBBCollider(AABB aabb) {
		super(null, null);
		this.modelVertices = null;
		this.modelNormals = null;
		this.rotatedVertices = new Vec3[4];
		this.rotatedNormals = new Vec3[3];
		this.setFromAABB(aabb);
	}
	
	/** Re-derives this collider's world-space data from {@code aabb}, reusing the vertex/normal
	 *  arrays so repeated {@link #isCollide(Entity)} queries stay cheap. */
	private OBBCollider setFromAABB(AABB aabb) {
		double xSize = (aabb.maxX - aabb.minX) / 2;
		double ySize = (aabb.maxY - aabb.minY) / 2;
		double zSize = (aabb.maxZ - aabb.minZ) / 2;
		this.worldCenter = new Vec3(-((float)aabb.minX + xSize), (float)aabb.minY + ySize, -((float)aabb.minZ + zSize));
		this.rotatedVertices[0] = new Vec3(-xSize, ySize, -zSize);
		this.rotatedVertices[1] = new Vec3(-xSize, ySize, zSize);
		this.rotatedVertices[2] = new Vec3(xSize, ySize, zSize);
		this.rotatedVertices[3] = new Vec3(xSize, ySize, -zSize);
		this.rotatedNormals[0] = new Vec3(1, 0, 0);
		this.rotatedNormals[1] = new Vec3(0, 1, 0);
		this.rotatedNormals[2] = new Vec3(0, 0, 1);
		return this;
	}
	
	/**
	 * Transform the bounding box
	 **/
	@Override
	public void transform(OpenMatrix4f modelMatrix) {
		OpenMatrix4f noTranslation = OpenMatrix4f.removeTranslation(modelMatrix, Armature.evalScratch()[0]);
		
		for (int i = 0; i < this.modelVertices.length; i++) {
			this.rotatedVertices[i] = OpenMatrix4f.transform(noTranslation, this.modelVertices[i]);
		}
		
		for (int i = 0; i < this.modelNormals.length; i++) {
			this.rotatedNormals[i] = OpenMatrix4f.transform(noTranslation, this.modelNormals[i]);
		}
		
		if (this.scale == null) {
			this.scale = new Vec3f();
		}
		
		noTranslation.toScaleVector(this.scale);
		
		super.transform(modelMatrix);
	}
	
	@Override
	protected AABB getHitboxAABB() {
		return this.outerAABB.inflate(
			  (this.outerAABB.maxX - this.outerAABB.minX) * this.scale.x
			, (this.outerAABB.maxY - this.outerAABB.minY) * this.scale.y
			, (this.outerAABB.maxZ - this.outerAABB.minZ) * this.scale.z
		).move(-this.worldCenter.x, this.worldCenter.y, -this.worldCenter.z);
	}
	
	public boolean isCollide(OBBCollider opponent) {
		Vec3 toOpponent = opponent.worldCenter.subtract(this.worldCenter);
		
		for (Vec3 seperateAxis : this.rotatedNormals) {
			if (!checkSeparateAxisOverlap(seperateAxis, toOpponent, this, opponent)) {
				return false;
			}
		}
		
		for (Vec3 seperateAxis : opponent.rotatedNormals) {
			if (!checkSeparateAxisOverlap(seperateAxis, toOpponent, this, opponent)) {
				return false;
			}
		}
		
		//Detects if lines are collide to opponent obb lines, but disabled for better performance
		/** 
		for(Vector3f norm1 : this.rotatedNormal)
		{
			for(Vector3f norm2 : opponent.rotatedNormal)
			{
				Vector3f seperateAxis = Vector3f.cross(norm1, norm2, null);
				
				if(seperateAxis.x + seperateAxis.y + seperateAxis.z == 0)
				{
					continue;
				}
				
				if(!collisionLogic(seperateAxis, toOpponent, this, opponent))
				{
					return false;
				}
			}
		}
		**/
		
		return true;
	}
	
	@Override
	public boolean isCollide(Entity entity) {
		if (this.queryCollider == null) {
			this.queryCollider = new OBBCollider(new AABB(0.0D, 0.0D, 0.0D, 0.0D, 0.0D, 0.0D));
		}
		
		return this.isCollide(this.queryCollider.setFromAABB(entity.getBoundingBox()));
	}
	
	@Override
	public OBBCollider deepCopy() {
		Vec3 xyzVec = this.modelVertices[1];
		return new OBBCollider(xyzVec.x, xyzVec.y, xyzVec.z, this.modelCenter.x, this.modelCenter.y, this.modelCenter.z);
	}
	
	private static boolean checkSeparateAxisOverlap(Vec3 seperateAxis, Vec3 toOpponent, OBBCollider box1, OBBCollider box2) {
		Vec3 maxProj1 = null, maxProj2 = null;//, distance;
		double maxDot1 = -1, maxDot2 = -1;
		
		if (seperateAxis.dot(toOpponent) < 0.0F) {
			seperateAxis.scale(-1.0D);
		}
		
		for (Vec3 vertexVector : box1.rotatedVertices) {
			Vec3 temp = seperateAxis.dot(vertexVector) > 0.0F ? vertexVector : vertexVector.scale(-1.0D);
			double dot = seperateAxis.dot(temp);
			
			if (dot > maxDot1) {
				maxDot1 = dot;
				maxProj1 = temp;
			}
		}
		
		for (Vec3 vertexVector : box2.rotatedVertices) {
			Vec3 temp = seperateAxis.dot(vertexVector) > 0.0F ? vertexVector : vertexVector.scale(-1.0D);
			double dot = seperateAxis.dot(temp);
			
			if (dot > maxDot2) {
				maxDot2 = dot;
				maxProj2 = temp;
			}
		}
		
		return MathUtils.projectVector(toOpponent, seperateAxis).length() < MathUtils.projectVector(maxProj1, seperateAxis).length() + MathUtils.projectVector(maxProj2, seperateAxis).length();
	}
	
	@Override
	public String toString() {
		return super.toString() + " worldCenter : " + this.worldCenter + " world direction : " + this.rotatedVertices[0];
	}
	
	@Override @ClientOnly
	public RenderType getRenderType() {
		return RenderType.lines();
	}

    @ClientOnly
	public void draw(PoseStack poseStack, MultiBufferSource buffer, int color) {
		VertexConsumer vertexConsumer = buffer.getBuffer(this.getRenderType());
		Matrix4f matrix = poseStack.last().pose();
		
		float v1x = (float) (this.worldCenter.x + this.rotatedVertices[0].x);
		float v1y = (float) (this.worldCenter.y + this.rotatedVertices[0].y);
		float v1z = (float) (this.worldCenter.z + this.rotatedVertices[0].z);
		
		float v2x = (float) (this.worldCenter.x + this.rotatedVertices[1].x);
		float v2y = (float) (this.worldCenter.y + this.rotatedVertices[1].y);
		float v2z = (float) (this.worldCenter.z + this.rotatedVertices[1].z);
		
		float v3x = (float) (this.worldCenter.x + this.rotatedVertices[2].x);
		float v3y = (float) (this.worldCenter.y + this.rotatedVertices[2].y);
		float v3z = (float) (this.worldCenter.z + this.rotatedVertices[2].z);

		float v4x = (float) (this.worldCenter.x + this.rotatedVertices[3].x);
		float v4y = (float) (this.worldCenter.y + this.rotatedVertices[3].y);
		float v4z = (float) (this.worldCenter.z + this.rotatedVertices[3].z);
		
		float v5x = (float) (this.worldCenter.x + -this.rotatedVertices[2].x);
		float v5y = (float) (this.worldCenter.y + -this.rotatedVertices[2].y);
		float v5z = (float) (this.worldCenter.z + -this.rotatedVertices[2].z);
		
		float v6x = (float) (this.worldCenter.x + -this.rotatedVertices[3].x);
		float v6y = (float) (this.worldCenter.y + -this.rotatedVertices[3].y);
		float v6z = (float) (this.worldCenter.z + -this.rotatedVertices[3].z);
		
		float v7x = (float) (this.worldCenter.x + -this.rotatedVertices[0].x);
		float v7y = (float) (this.worldCenter.y + -this.rotatedVertices[0].y);
		float v7z = (float) (this.worldCenter.z + -this.rotatedVertices[0].z);
		
		float v8x = (float) (this.worldCenter.x + -this.rotatedVertices[1].x);
		float v8y = (float) (this.worldCenter.y + -this.rotatedVertices[1].y);
		float v8z = (float) (this.worldCenter.z + -this.rotatedVertices[1].z);
		
		vertexConsumer.addVertex(matrix, v1x, v1y, v1z).setColor(color).setNormal(v2x - v1x, v2y - v1y, v2z - v1z);
		vertexConsumer.addVertex(matrix, v2x, v2y, v2z).setColor(color).setNormal(0.0F, 0.0F, 0.0F);
		
		vertexConsumer.addVertex(matrix, v2x, v2y, v2z).setColor(color).setNormal(v3x - v2x, v3y - v2y, v3z - v2z);
		vertexConsumer.addVertex(matrix, v3x, v3y, v3z).setColor(color).setNormal(0.0F, 0.0F, 0.0F);
		
		vertexConsumer.addVertex(matrix, v3x, v3y, v3z).setColor(color).setNormal(v4x - v3x, v4y - v3y, v4z - v3z);
		vertexConsumer.addVertex(matrix, v4x, v4y, v4z).setColor(color).setNormal(0.0F, 0.0F, 0.0F);
		
		vertexConsumer.addVertex(matrix, v4x, v4y, v4z).setColor(color).setNormal(v1x - v4x, v1y - v4y, v1z - v4z);
		vertexConsumer.addVertex(matrix, v1x, v1y, v1z).setColor(color).setNormal(0.0F, 0.0F, 0.0F);
		
		vertexConsumer.addVertex(matrix, v1x, v1y, v1z).setColor(color).setNormal(v5x - v1x, v5y - v1y, v5z - v1z);
		vertexConsumer.addVertex(matrix, v5x, v5y, v5z).setColor(color).setNormal(0.0F, 0.0F, 0.0F);
		
		vertexConsumer.addVertex(matrix, v2x, v2y, v2z).setColor(color).setNormal(v6x - v2x, v6y - v2y, v6z - v2z);
		vertexConsumer.addVertex(matrix, v6x, v6y, v6z).setColor(color).setNormal(0.0F, 0.0F, 0.0F);
		
		vertexConsumer.addVertex(matrix, v3x, v3y, v3z).setColor(color).setNormal(v7x - v3x, v7y - v3y, v7z - v3z);
		vertexConsumer.addVertex(matrix, v7x, v7y, v7z).setColor(color).setNormal(0.0F, 0.0F, 0.0F);
		
		vertexConsumer.addVertex(matrix, v4x, v4y, v4z).setColor(color).setNormal(v8x - v4x, v8y - v4y, v8z - v4z);
		vertexConsumer.addVertex(matrix, v8x, v8y, v8z).setColor(color).setNormal(0.0F, 0.0F, 0.0F);
		
		vertexConsumer.addVertex(matrix, v5x, v5y, v5z).setColor(color).setNormal(v6x - v5x, v6y - v5y, v6z - v5z);
		vertexConsumer.addVertex(matrix, v6x, v6y, v6z).setColor(color).setNormal(0.0F, 0.0F, 0.0F);
		
		vertexConsumer.addVertex(matrix, v6x, v6y, v6z).setColor(color).setNormal(v7x - v6x, v7y - v6y, v7z - v6z);
		vertexConsumer.addVertex(matrix, v7x, v7y, v7z).setColor(color).setNormal(0.0F, 0.0F, 0.0F);
		
		vertexConsumer.addVertex(matrix, v7x, v7y, v7z).setColor(color).setNormal(v8x - v7x, v8y - v7y, v8z - v7z);
		vertexConsumer.addVertex(matrix, v8x, v8y, v8z).setColor(color).setNormal(0.0F, 0.0F, 0.0F);
		
		vertexConsumer.addVertex(matrix, v8x, v8y, v8z).setColor(color).setNormal(v5x - v8x, v5y - v8y, v5z - v8z);
		vertexConsumer.addVertex(matrix, v5x, v5y, v5z).setColor(color).setNormal(0.0F, 0.0F, 0.0F);
	}
	
	@Override @ClientOnly
	public void drawInternal(PoseStack poseStack, VertexConsumer vertexConsumer, Armature armature, Joint joint, Pose pose1, Pose pose2, float partialTicks, int color) {
		OpenMatrix4f poseMatrix;
		Pose interpolatedPose = Pose.interpolatePose(pose1, pose2, partialTicks);
		
		if (armature.rootJoint.equals(joint)) {
			JointTransform jt = interpolatedPose.orElseEmpty("Root");
			jt.rotation().x = 0.0F;
			jt.rotation().y = 0.0F;
			jt.rotation().z = 0.0F;
			jt.rotation().w = 1.0F;
			
			poseMatrix = jt.getAnimationBoundMatrix(armature.rootJoint, new OpenMatrix4f()).removeTranslation();
		} else {
			poseMatrix = armature.getBoundTransformFor(interpolatedPose, joint);
		}
		
		poseStack.pushPose();
        MathUtils.mulStack(poseStack, poseMatrix);
        Matrix4f matrix = poseStack.last().pose();
        Vec3 vec = this.modelVertices[1];
        float maxX = (float)(this.modelCenter.x + vec.x);
        float maxY = (float)(this.modelCenter.y + vec.y);
        float maxZ = (float)(this.modelCenter.z + vec.z);
        float minX = (float)(this.modelCenter.x - vec.x);
        float minY = (float)(this.modelCenter.y - vec.y);
        float minZ = (float)(this.modelCenter.z - vec.z);
        
        vertexConsumer.addVertex(matrix, minX, maxY, minZ).setColor(color).setNormal(0.0F, 0.0F, 1.0F);
        vertexConsumer.addVertex(matrix, minX, maxY, maxZ).setColor(color).setNormal(0.0F, 0.0F, 1.0F);
        vertexConsumer.addVertex(matrix, minX, maxY, maxZ).setColor(color).setNormal(1.0F, 0.0F, 0.0F);
        vertexConsumer.addVertex(matrix, maxX, maxY, maxZ).setColor(color).setNormal(1.0F, 0.0F, 0.0F);
        vertexConsumer.addVertex(matrix, maxX, maxY, maxZ).setColor(color).setNormal(0.0F, 0.0F, -1.0F);
        vertexConsumer.addVertex(matrix, maxX, maxY, minZ).setColor(color).setNormal(0.0F, 0.0F, -1.0F);
        vertexConsumer.addVertex(matrix, maxX, maxY, minZ).setColor(color).setNormal(-1.0F, 0.0F, 0.0F);
        vertexConsumer.addVertex(matrix, minX, maxY, minZ).setColor(color).setNormal(-1.0F, 0.0F, 0.0F);
        vertexConsumer.addVertex(matrix, maxX, maxY, maxZ).setColor(color).setNormal(0.0F, -1.0F, 0.0F);
        vertexConsumer.addVertex(matrix, maxX, minY, maxZ).setColor(color).setNormal(0.0F, -1.0F, 0.0F);
        vertexConsumer.addVertex(matrix, minX, maxY, maxZ).setColor(color).setNormal(0.0F, -1.0F, 0.0F);
        vertexConsumer.addVertex(matrix, minX, minY, maxZ).setColor(color).setNormal(0.0F, -1.0F, 0.0F);
        vertexConsumer.addVertex(matrix, maxX, maxY, minZ).setColor(color).setNormal(0.0F, -1.0F, 0.0F);
        vertexConsumer.addVertex(matrix, maxX, minY, minZ).setColor(color).setNormal(0.0F, -1.0F, 0.0F);
        vertexConsumer.addVertex(matrix, minX, maxY, minZ).setColor(color).setNormal(0.0F, -1.0F, 0.0F);
        vertexConsumer.addVertex(matrix, minX, minY, minZ).setColor(color).setNormal(0.0F, -1.0F, 0.0F);
        vertexConsumer.addVertex(matrix, minX, minY, minZ).setColor(color).setNormal(0.0F, 0.0F, 1.0F);
        vertexConsumer.addVertex(matrix, minX, minY, maxZ).setColor(color).setNormal(0.0F, 0.0F, 1.0F);
        vertexConsumer.addVertex(matrix, minX, minY, maxZ).setColor(color).setNormal(1.0F, 0.0F, 0.0F);
        vertexConsumer.addVertex(matrix, maxX, minY, maxZ).setColor(color).setNormal(1.0F, 0.0F, 0.0F);
        vertexConsumer.addVertex(matrix, maxX, minY, maxZ).setColor(color).setNormal(0.0F, 0.0F, -1.0F);
        vertexConsumer.addVertex(matrix, maxX, minY, minZ).setColor(color).setNormal(0.0F, 0.0F, -1.0F);
        vertexConsumer.addVertex(matrix, maxX, minY, minZ).setColor(color).setNormal(-1.0F, 0.0F, 0.0F);
        vertexConsumer.addVertex(matrix, minX, minY, minZ).setColor(color).setNormal(-1.0F, 0.0F, 0.0F);
        
        poseStack.popPose();
	}
	
	public CompoundTag serialize(CompoundTag resultTag) {
		if (resultTag == null) {
			resultTag = new CompoundTag();
		}
		
		resultTag.putInt("number", 1);
		
		ListTag center = new ListTag();
		center.add(DoubleTag.valueOf(this.modelCenter.x));
		center.add(DoubleTag.valueOf(this.modelCenter.y));
		center.add(DoubleTag.valueOf(this.modelCenter.z));
		
		resultTag.put("center", center);
		
		ListTag size = new ListTag();
		size.add(DoubleTag.valueOf(this.modelVertices[1].x));
		size.add(DoubleTag.valueOf(this.modelVertices[1].y));
		size.add(DoubleTag.valueOf(this.modelVertices[1].z));
		
		resultTag.put("size", size);
		
		return resultTag;
	}
}