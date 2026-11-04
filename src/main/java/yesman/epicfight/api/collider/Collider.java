package yesman.epicfight.api.collider;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import yesman.epicfight.api.animation.Joint;
import yesman.epicfight.api.animation.JointTransform;
import yesman.epicfight.api.animation.Pose;
import yesman.epicfight.api.animation.PoseMirror;
import yesman.epicfight.api.animation.types.AttackAnimation;
import yesman.epicfight.api.animation.types.EntityState;
import yesman.epicfight.api.model.Armature;
import yesman.epicfight.api.utils.math.OpenMatrix4f;
import yesman.epicfight.api.utils.side.ClientOnly;
import yesman.epicfight.world.capabilities.entitypatch.LivingEntityPatch;

import javax.annotation.Nullable;
import java.util.List;

public abstract class Collider {
	protected final Vec3 modelCenter;
	protected final AABB outerAABB;
	protected Vec3 worldCenter;
	
	public Collider(Vec3 center, @Nullable AABB outerAABB) {
		this.modelCenter = center;
		this.outerAABB = outerAABB;
		this.worldCenter = new Vec3(0.0D, 0.0D, 0.0D);
	}
	
	protected void transform(OpenMatrix4f mat) {
		this.worldCenter = OpenMatrix4f.transform(mat, this.modelCenter);
	}
	
	/** Multiplier applied to the collider's local frame when the attacker is mounted expands the
	 *  hitbox so the rider's strike still reaches the ground tile under their vehicle. The scale is
	 *  embedded in the transform matrix so it propagates through {@code transform()} and shows up
	 *  in the F3+B debug render. */
	private static final float MOUNTED_COLLIDER_SCALE = 1.75F;

	public List<Entity> updateAndSelectCollideEntity(LivingEntityPatch<?> entitypatch, AttackAnimation attackAnimation, float prevElapsedTime, float elapsedTime, Joint joint, float attackSpeed) {
		OpenMatrix4f transformMatrix;
		Armature armature = entitypatch.getArmature();
		boolean mirror = entitypatch.isMirrorMode() && !armature.rootJoint.equals(joint);

		if (armature.rootJoint.equals(joint)) {
			Pose rootPose = new Pose();
			rootPose.putJointData("Root", JointTransform.empty());
			attackAnimation.modifyPose(attackAnimation, rootPose, entitypatch, elapsedTime, 1.0F);
			transformMatrix = rootPose.orElseEmpty("Root").getAnimationBoundMatrix(armature.rootJoint, new OpenMatrix4f()).removeTranslation();
		} else if (mirror) {
			// Match the renderer's mirror exactly: PoseMirror.mirrorPose swaps R/L joint data
			// (with X-translation negated and qy/qz inverted), and the rendered weapon is the
			// vanilla offhand attachment, which displays at the mirrored joint (example: Tool_R ->
			// Tool_L). Walk the bound chain on the SAME mirrored pose for the SAME mirrored
			// joint so the collider lands exactly where the visible weapon does. The previous
			// "MirrorX matrix in front of the bound transform" approximation drifted because
			// bind(Tool_L) is not the X-reflection of bind(Tool_R) once you account for parent
			// joints' bind orientations.
			Pose mirrored = PoseMirror.mirrorPose(attackAnimation.getPoseByTime(entitypatch, elapsedTime, 1.0F));
			Joint mirroredJoint = armature.searchJointByName(PoseMirror.mirrorJointName(joint.getName()));
			if (mirroredJoint == null) mirroredJoint = joint;
			transformMatrix = armature.getBoundTransformFor(mirrored, mirroredJoint);
		} else {
			transformMatrix = armature.getBoundTransformFor(attackAnimation.getPoseByTime(entitypatch, elapsedTime, 1.0F), joint);
		}

		OpenMatrix4f toWorldCoord = OpenMatrix4f.createTranslation(-(float)entitypatch.getOriginal().getX(), (float)entitypatch.getOriginal().getY(), -(float)entitypatch.getOriginal().getZ());
		transformMatrix.mulFront(toWorldCoord.mulBack(entitypatch.getModelMatrix(1.0F)));

		// When the attacker is a passenger (chicken jockey, skeleton trap, etc) the rider's hand
		// sits above ground by the vehicle's height, so the unscaled hitbox can't reach a target on
		// foot. Embed an extra scale into the transform matrix so the OBB's rotatedVertices grow
		// (the debug render draws those, so F3+B shows the enlarged box) and getHitboxAABB inflates
		// proportionally for the broad-phase lookup.
		if (entitypatch.getOriginal().isPassenger()) {
			transformMatrix.scale(MOUNTED_COLLIDER_SCALE, MOUNTED_COLLIDER_SCALE, MOUNTED_COLLIDER_SCALE);
		}

		this.transform(transformMatrix);

		return this.getCollideEntities(entitypatch.getOriginal());
	}
	
	public List<Entity> getCollideEntities(Entity entity) {
		List<Entity> list = entity.level().getEntities(entity, this.getHitboxAABB(), (e) -> {
			if (e instanceof net.minecraft.world.entity.boss.EnderDragonPart enderDragonPart) {
				if (enderDragonPart.parentMob.is(entity)) {
					return false;
				}
			}

			if (e.isSpectator()) {
				return false;
			}

			return this.isCollide(e);
		});

		return list;
	}
	
	/** Display on debug mode **/
    @ClientOnly
	public abstract void drawInternal(PoseStack poseStack, VertexConsumer vertexConsumer, Armature armature, Joint joint, Pose pose1, Pose pose2, float partialTicks, int colliderColor);
	
	/** Display on debug mode **/
    @ClientOnly
	public void draw(PoseStack poseStack, MultiBufferSource buffer, LivingEntityPatch<?> entitypatch, AttackAnimation animation, Joint joint, float prevElapsedTime, float elapsedTime, float partialTicks, float attackSpeed) {
		Armature armature = entitypatch.getArmature();
		EntityState state = animation.getState(entitypatch, elapsedTime);
		EntityState prevState = animation.getState(entitypatch, prevElapsedTime);
		boolean attacking = prevState.attacking() || state.attacking() || (prevState.getLevel() < 2 && state.getLevel() > 2);
		Pose prevPose;
		Pose currentPose;
		
		boolean mirror = entitypatch.isMirrorMode() && !joint.getName().equals(armature.rootJoint.getName());
		Joint drawJoint = joint;

		if (joint.getName().equals(armature.rootJoint.getName())) {
			prevPose = new Pose();
			currentPose = new Pose();
			prevPose.putJointData("Root", JointTransform.empty());
			currentPose.putJointData("Root", JointTransform.empty());
			animation.modifyPose(animation, prevPose, entitypatch, prevElapsedTime, 0.0F);
			animation.modifyPose(animation, currentPose, entitypatch, elapsedTime, 1.0F);
		} else if (mirror) {
			// Match the live collider math: mirror the pose and look up the mirrored joint so
			// the F3+B debug box draws on top of the actual rendered weapon.
			prevPose = PoseMirror.mirrorPose(animation.getPoseByTime(entitypatch, prevElapsedTime, 0.0F));
			currentPose = PoseMirror.mirrorPose(animation.getPoseByTime(entitypatch, elapsedTime, 1.0F));
			Joint mirroredJoint = armature.searchJointByName(PoseMirror.mirrorJointName(joint.getName()));
			if (mirroredJoint != null) drawJoint = mirroredJoint;
		} else {
			prevPose = animation.getPoseByTime(entitypatch, prevElapsedTime, 0.0F);
			currentPose = animation.getPoseByTime(entitypatch, elapsedTime, 1.0F);
		}

		this.drawInternal(poseStack, buffer.getBuffer(this.getRenderType()), armature, drawJoint, prevPose, currentPose, partialTicks, attacking ? 0xFFFF0000 : -1);
	}
	
	public abstract Collider deepCopy();
	
	public abstract boolean isCollide(Entity opponent);

    @ClientOnly
	public abstract RenderType getRenderType();
	
	protected AABB getHitboxAABB() {
		return this.outerAABB.move(-this.worldCenter.x, this.worldCenter.y, -this.worldCenter.z);
	}
	
	public CompoundTag serialize(CompoundTag resultTag) {
		return resultTag;
	}
	
	@Override
	public String toString() {
		return this.getClass().getSimpleName() + " center: " + this.modelCenter;
	}
}
