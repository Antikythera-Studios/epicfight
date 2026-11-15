package yesman.epicfight.api.animation.types.procedural;

import net.minecraft.world.phys.Vec3;
import yesman.epicfight.api.animation.AnimationManager.AnimationAccessor;
import yesman.epicfight.api.animation.AnimationPlayer;
import yesman.epicfight.api.animation.Keyframe;
import yesman.epicfight.api.animation.TransformSheet;
import yesman.epicfight.api.animation.property.AnimationProperty.StaticAnimationProperty;
import yesman.epicfight.api.animation.types.ActionAnimation;
import yesman.epicfight.api.asset.AssetAccessor;
import yesman.epicfight.api.model.Armature;
import yesman.epicfight.api.physics.ik.InverseKinematicsSimulatable;
import yesman.epicfight.api.physics.ik.InverseKinematicsSimulator;
import yesman.epicfight.api.physics.ik.InverseKinematicsSimulator.BakedInverseKinematicsDefinition;
import yesman.epicfight.api.utils.math.OpenMatrix4f;
import yesman.epicfight.api.utils.math.Vec3f;
import yesman.epicfight.world.capabilities.entitypatch.LivingEntityPatch;

public class EnderDragonDynamicActionAnimation extends ActionAnimation {
	public EnderDragonDynamicActionAnimation(float transitionTime, AnimationAccessor<? extends EnderDragonDynamicActionAnimation> accessor, AssetAccessor<? extends Armature> armature) {
		super(transitionTime, accessor, armature);
	}
	
	@Override
	public void putOnPlayer(AnimationPlayer animationPlayer, LivingEntityPatch<?> entitypatch) {
		super.putOnPlayer(animationPlayer, entitypatch);
		
		if (entitypatch instanceof InverseKinematicsSimulatable ikSimulatable) {
			Vec3 entitypos = ikSimulatable.toEntity().position();
			OpenMatrix4f toWorld = OpenMatrix4f.mul(OpenMatrix4f.createTranslation((float)entitypos.x, (float)entitypos.y, (float)entitypos.z), ikSimulatable.getModelMatrix(1.0F), null);
			//ikSimulatable.resetTipAnimations();
			TransformSheet movementAnimation = entitypatch.getAnimator().getVariables().getOrDefaultSharedVariable(ACTION_ANIMATION_COORD);
			
			this.getProperty(StaticAnimationProperty.BAKED_IK_DEFINITION).ifPresent((ikDefinitions) -> {
				for (BakedInverseKinematicsDefinition bakedIKInfo : ikDefinitions) {
					TransformSheet tipAnim = this.clipAnimation(bakedIKInfo.terminalBoneTransform(), bakedIKInfo);
					Keyframe[] keyframes = tipAnim.getKeyframes();
					Vec3f startpos = movementAnimation.getInterpolatedTranslation(0.0F);
					
					for (int i = 0; i < keyframes.length; i++) {
						Keyframe kf = keyframes[i];
						Vec3f dynamicpos = movementAnimation.getInterpolatedTranslation(kf.time()).sub(startpos);
						OpenMatrix4f.transform3v(OpenMatrix4f.createRotatorDeg(-90.0F, Vec3f.X_AXIS), dynamicpos, dynamicpos).multiply(-1.0F, 1.0F, -1.0F);
						Vec3f finalTargetpos;
						
						if (!bakedIKInfo.clipAnimation() || bakedIKInfo.touchingGround()[i]) {
							Vec3f clipStart = kf.transform().translation().copy().multiply(-1.0F, 1.0F, -1.0F).add(dynamicpos).add(0.0F, 2.5F, 0.0F);
							finalTargetpos = this.getRayCastedTipPosition(ikSimulatable, clipStart, toWorld, 2.5F, bakedIKInfo.rayLeastHeight());
						} else {
							Vec3f start = kf.transform().translation().copy().multiply(-1.0F, 1.0F, -1.0F).add(dynamicpos);
							finalTargetpos = OpenMatrix4f.transform3v(toWorld, start, null);
						}
						
						kf.transform().translation().set(finalTargetpos);
					}
					
					ikSimulatable.getIKSimulator().runUntil(
						  bakedIKInfo.endJoint()
						, this
						, InverseKinematicsSimulator.InverseKinematicsBuilder.create(
							  keyframes[0].transform().translation()
							, tipAnim
							, bakedIKInfo)
						, () -> entitypatch.getAnimator().getPlayer(this.getAccessor()).isPresent()
					);
				}
			});
		}
	}
	
	@Override
	public void tick(LivingEntityPatch<?> entitypatch) {
		super.tick(entitypatch);
		
		if (entitypatch instanceof InverseKinematicsSimulatable ikSimulatable) {
			float elapsedTime = entitypatch.getAnimator().getPlayerFor(this.getAccessor()).getElapsedTime();
			
			this.getProperty(StaticAnimationProperty.BAKED_IK_DEFINITION).ifPresent((ikDefinitions) -> {
				for (BakedInverseKinematicsDefinition bakedIKInfo : ikDefinitions) {
					if (ikSimulatable.getIKSimulator().isRunning(bakedIKInfo.endJoint()) && bakedIKInfo.clipAnimation()) {
						Keyframe[] keyframes = this.getTransfroms().get(bakedIKInfo.endJoint().getName()).getKeyframes();
						float startTime = keyframes[bakedIKInfo.startFrame()].time();
						float endTime = keyframes[bakedIKInfo.endFrame() - 1].time();
						
						if (startTime <= elapsedTime && elapsedTime < endTime) {
							InverseKinematicsSimulator.InverseKinematicsObject tipAnim = ikSimulatable.getIKSimulator().getRunningObject(bakedIKInfo.endJoint()).get();
							
							if (!tipAnim.isOnWorking()) {
								this.startSimple(tipAnim);
							}
						}
					}
				}
			});
		}
	}
}