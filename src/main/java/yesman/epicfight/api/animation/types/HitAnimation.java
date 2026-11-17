package yesman.epicfight.api.animation.types;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import yesman.epicfight.api.animation.AnimationManager.AnimationAccessor;
import yesman.epicfight.api.animation.JointTransform;
import yesman.epicfight.api.animation.Keyframe;
import yesman.epicfight.api.animation.Pose;
import yesman.epicfight.api.animation.TransformSheet;
import yesman.epicfight.api.asset.AssetAccessor;
import yesman.epicfight.api.model.Armature;
import yesman.epicfight.world.capabilities.entitypatch.LivingEntityPatch;

public class HitAnimation extends MainFrameAnimation {
	public HitAnimation(float transitionTime, AnimationAccessor<? extends HitAnimation> accessor, AssetAccessor<? extends Armature> armature) {
		super(transitionTime, accessor, armature);
		
		this.stateSpectrumBlueprint.clear()
			.newTimePair(0.0F, Float.MAX_VALUE)
			.addState(EntityState.TURNING_LOCKED, true)
			.addState(EntityState.MOVEMENT_LOCKED, true)
			.addState(EntityState.UPDATE_LIVING_MOTION, false)
			.addState(EntityState.COMBO_ATTACKS_DOABLE, false)
			.addState(EntityState.SKILL_EXECUTABLE, false)
			.addState(EntityState.INACTION, true)
			.addState(EntityState.HURT_LEVEL, 1);
	}
	
	public HitAnimation(float transitionTime, String path, AssetAccessor<? extends Armature> armature) {
		super(transitionTime, path, armature);
		
		this.stateSpectrumBlueprint.clear()
			.newTimePair(0.0F, Float.MAX_VALUE)
			.addState(EntityState.TURNING_LOCKED, true)
			.addState(EntityState.MOVEMENT_LOCKED, true)
			.addState(EntityState.UPDATE_LIVING_MOTION, false)
			.addState(EntityState.COMBO_ATTACKS_DOABLE, false)
			.addState(EntityState.SKILL_EXECUTABLE, false)
			.addState(EntityState.INACTION, true)
			.addState(EntityState.HURT_LEVEL, 1);
	}
	
	@Override
	public void begin(LivingEntityPatch<?> entitypatch) {
		entitypatch.cancelItemUse();
		
		super.begin(entitypatch);
	}
	
	@Override
	public void setLinkAnimation(AssetAccessor<? extends DynamicAnimation> fromAnimation, Pose startPose, boolean isOnSameLayer, float transitionTimeModifier, LivingEntityPatch<?> entitypatch, LinkAnimation dest) {
		dest.resetNextStartTime();
		dest.getAnimationClip().reset();
		dest.setTotalTime(transitionTimeModifier + this.transitionTime);
		dest.setConnectedAnimations(fromAnimation, this.getAccessor());
		
		Map<String, JointTransform> data1 = startPose.getJointTransformData();
		Map<String, JointTransform> data2 = super.getPoseByTime(entitypatch, 0.0F, 0.0F).getJointTransformData();
		Map<String, JointTransform> data3 = super.getPoseByTime(entitypatch, this.getTotalTime(), 0.0F).getJointTransformData();
		
		Set<String> joint1 = new HashSet<> (data1.keySet());
		joint1.removeIf((jointName) -> !fromAnimation.get().hasTransformFor(jointName));
		Set<String> joint2 = new HashSet<> (data2.keySet());
		joint2.removeIf((jointName) -> !this.hasTransformFor(jointName));
		joint1.addAll(joint2);
		
		for (String jointName : joint1) {
			if (data1.containsKey(jointName) && data2.containsKey(jointName)) {
				Keyframe[] keyframes = new Keyframe[4];
				keyframes[0] = new Keyframe(0, data1.get(jointName).copy());
				keyframes[1] = new Keyframe(this.transitionTime, data2.get(jointName).copy());
				keyframes[2] = new Keyframe(this.transitionTime + 0.033F, data3.get(jointName).copy());
				keyframes[3] = new Keyframe(transitionTimeModifier + this.transitionTime, data3.get(jointName).copy());
				TransformSheet sheet = new TransformSheet(keyframes);
				dest.getAnimationClip().addJointTransform(jointName, sheet);
			}
		}
	}

	@Override
	public Pose getPoseByTime(LivingEntityPatch<?> entitypatch, float time, float partialTicks) {
		return super.getPoseByTime(entitypatch, this.getTotalTime() - 0.000001F, 0.0F);
	}

	@Override
	public Pose getPoseByTime(LivingEntityPatch<?> entitypatch, float time, float partialTicks, Pose pose) {
		return super.getPoseByTime(entitypatch, this.getTotalTime() - 0.000001F, 0.0F, pose);
	}
}