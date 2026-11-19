package yesman.epicfight.api.animation.types;

import net.minecraft.resources.ResourceLocation;
import yesman.epicfight.api.animation.*;
import yesman.epicfight.api.animation.AnimationManager.AnimationAccessor;
import yesman.epicfight.api.animation.types.EntityState.StateFactor;
import yesman.epicfight.api.asset.AssetAccessor;
import yesman.epicfight.api.client.animation.property.JointMaskEntry;
import yesman.epicfight.api.utils.datastructure.ParameterizedHashMap;
import yesman.epicfight.api.utils.side.ClientOnly;
import yesman.epicfight.world.capabilities.entitypatch.LivingEntityPatch;

import java.util.Map;
import java.util.Optional;

public class LinkAnimation extends DynamicAnimation implements AnimationAccessor<LinkAnimation> {
	protected TransformSheet coord;
	protected AssetAccessor<? extends DynamicAnimation> fromAnimation;
	protected AssetAccessor<? extends StaticAnimation> toAnimation;
	protected float nextStartTime;
	
	public LinkAnimation() {
		this.animationClip = new AnimationClip();
	}
	
	@Override
	public void tick(LivingEntityPatch<?> entitypatch) {
		this.toAnimation.get().linkTick(entitypatch, this);
	}
	
	@Override
	public void end(LivingEntityPatch<?> entitypatch, AssetAccessor<? extends DynamicAnimation> nextAnimation, boolean isEnd) {
		if (!isEnd) {
			this.toAnimation.get().end(entitypatch, nextAnimation, isEnd);
		} else {
			if (this.nextStartTime > 0.0F) {
				entitypatch.getAnimator().getPlayer(this).ifPresent(player -> {
					player.setElapsedTime(this.nextStartTime);
					player.markDoNotResetTime();
				});
			}
		}
	}
	
	@Override
	public ParameterizedHashMap<StateFactor<?>> getStatesMap(LivingEntityPatch<?> entitypatch, float time) {
		float timeInRealAnimation = Math.max(time - (this.getTotalTime() - this.nextStartTime), 0.0F);
		ParameterizedHashMap<StateFactor<?>> map = this.toAnimation.get().getStatesMap(entitypatch, timeInRealAnimation);
		
		for (Map.Entry<StateFactor<?>, Object> entry : map.entrySet()) {
			Object val = this.toAnimation.get().getModifiedLinkState(entry.getKey(), entry.getValue(), entitypatch, time);
			map.put(entry.getKey(), val);
		}
		
		return map;
	}
	
	@Override
	public EntityState getState(LivingEntityPatch<?> entitypatch, float time) {
		float timeInRealAnimation = Math.max(time - (this.getTotalTime() - this.nextStartTime), 0.0F);
		
		EntityState state = this.toAnimation.get().getState(entitypatch, timeInRealAnimation);
		ParameterizedHashMap<StateFactor<?>> map = state.getStateMap();
		
		for (Map.Entry<StateFactor<?>, Object> entry : map.entrySet()) {
			Object val = this.toAnimation.get().getModifiedLinkState(entry.getKey(), entry.getValue(), entitypatch, time);
			map.put(entry.getKey(), val);
		}
		
		return state;
	}
	
	@SuppressWarnings("unchecked")
	@Override
	public <T> T getState(StateFactor<T> stateFactor, LivingEntityPatch<?> entitypatch, float time) {
		float timeInRealAnimation = Math.max(time - (this.getTotalTime() - this.nextStartTime), 0.0F);
		T state = this.toAnimation.get().getState(stateFactor, entitypatch, timeInRealAnimation);
		
		return (T)this.toAnimation.get().getModifiedLinkState(stateFactor, state, entitypatch, time);
	}
	
	private final ThreadLocal<Pose> nextPoseScratch = ThreadLocal.withInitial(Pose::new);

	@Override
	public Pose getPoseByTime(LivingEntityPatch<?> entitypatch, float time, float partialTicks) {
		return this.getPoseByTime(entitypatch, time, partialTicks, new Pose());
	}

	@Override
	public Pose getPoseByTime(LivingEntityPatch<?> entitypatch, float time, float partialTicks, Pose pose) {
		Pose scratch = this.nextPoseScratch.get();
		// loadDeep only adds joints; drop leftovers from earlier animations so the loop
		// below iterates exactly the joints the destination pose actually has.
		scratch.getJointTransformData().clear();
		Pose nextStartingPose = scratch.loadDeep(this.toAnimation.get().getPoseByTime(entitypatch, this.nextStartTime, partialTicks));

		/**
		 * Update dest pose
		 */
		for (Map.Entry<String, JointTransform> entry : nextStartingPose.getJointTransformData().entrySet()) {
			if (this.animationClip.hasJointTransform(entry.getKey())) {
				Keyframe[] keyframe = this.animationClip.getJointTransform(entry.getKey()).getKeyframes();
				JointTransform jt = keyframe[keyframe.length - 1].transform();
				JointTransform newJt = entry.getValue();

				if (jt == null || newJt == null) {
					continue;
				}

				newJt.translation().set(jt.translation());
				jt.copyFromDeep(newJt);
			}
		}

		// Compute the base pose directly: delegating to super.getPoseByTime(4-arg) routes back
		// into this.getPoseByTime(3-arg) and recurses forever.
		this.getRawPose(time, pose);
		this.modifyPose(this, pose, entitypatch, time, partialTicks);

		return pose;
	}
	
	@Override
	public void modifyPose(DynamicAnimation animation, Pose pose, LivingEntityPatch<?> entitypatch, float time, float partialTicks) {
		// Bad implementation: AddEntity root joint as coord in loading animation
		if (this.toAnimation.get() instanceof ActionAnimation actionAnimation) {
			if (!this.getTransfroms().containsKey("Coord")) {
				actionAnimation.correctRootJoint(this, pose, entitypatch, time, partialTicks);
			}
		}
	}
	
	@Override
	public float getPlaySpeed(LivingEntityPatch<?> entitypatch, DynamicAnimation animation) {
		return this.toAnimation.get().getPlaySpeed(entitypatch, animation);
	}
	
	public void setConnectedAnimations(AssetAccessor<? extends DynamicAnimation> from, AssetAccessor<? extends StaticAnimation> to) {
		this.fromAnimation = from.get().getRealAnimation();
		this.toAnimation = to;
	}
	
	public AssetAccessor<? extends StaticAnimation> getNextAnimation() {
		return this.toAnimation;
	}
	
	@Override
	public TransformSheet getCoord() {
		if (this.coord != null) {
			return this.coord;
		} else if (this.getTransfroms().containsKey("Root")) {
			return this.getTransfroms().get("Root");
		}
		
		return TransformSheet.EMPTY_SHEET;
	}

    @ClientOnly
	public Optional<JointMaskEntry> getJointMaskEntry(LivingEntityPatch<?> entitypatch, boolean useCurrentMotion) {
		return useCurrentMotion ? this.toAnimation.get().getJointMaskEntry(entitypatch, true) : this.fromAnimation.get().getJointMaskEntry(entitypatch, false);
	}
	
	@Override
	public boolean isMainFrameAnimation() {
		return this.toAnimation.get().isMainFrameAnimation();
	}
	
	@Override
	public boolean isReboundAnimation() {
		return this.toAnimation.get().isReboundAnimation();
	}
	
	@Override
	public boolean doesHeadRotFollowEntityHead() {
		return this.fromAnimation.get().doesHeadRotFollowEntityHead() && this.toAnimation.get().doesHeadRotFollowEntityHead();
	}
	
	@Override
	public AssetAccessor<? extends StaticAnimation> getRealAnimation() {
		return this.toAnimation;
	}
		
	public AssetAccessor<? extends DynamicAnimation> getFromAnimation() {
		return this.fromAnimation;
	} 
	
	@Override
	public AnimationAccessor<? extends DynamicAnimation> getAccessor() {
		return this;
	}
	
	public void copyTo(LinkAnimation dest) {
		dest.setConnectedAnimations(this.fromAnimation, this.toAnimation);
		dest.setTotalTime(this.getTotalTime());
		dest.getAnimationClip().reset();
		this.getTransfroms().forEach((jointName, transformSheet) -> dest.getAnimationClip().addJointTransform(jointName, transformSheet.copyAll()));
	}
	
	public void loadCoord(TransformSheet coord) {
		this.coord = coord;
	}
	
	public float getNextStartTime() {
		return this.nextStartTime;
	}
	
	public void setNextStartTime(float nextStartTime) {
		this.nextStartTime = nextStartTime;
	}
	
	public void resetNextStartTime() {
		this.nextStartTime = 0.0F;
	}
	
	@Override
	public boolean isLinkAnimation() {
		return true;
	}
	
	@Override
	public String toString() {
		return "From " + this.fromAnimation + " to " + this.toAnimation;
	}

	@Override
	public AnimationClip getAnimationClip() {
		return this.animationClip;
	}
	
	@Override
	public LinkAnimation get() {
		return this;
	}

	@Override
	public ResourceLocation registryName() {
		return null;
	}

	@Override
	public boolean isPresent() {
		return true;
	}

	@Override
	public int id() {
		return -1;
	}
	
	@Override
	public boolean inRegistry() {
		return false;
	}
}